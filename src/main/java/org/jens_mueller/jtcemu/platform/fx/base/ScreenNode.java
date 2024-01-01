/*
 * (c) 2014-2019 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Node zur Darstellung des Bildschirms
 */

package org.jens_mueller.jtcemu.platform.fx.base;

import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.scene.paint.Color;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import org.jens_mueller.jtcemu.base.AppContext;
import org.jens_mueller.jtcemu.base.CharRaster;
import org.jens_mueller.jtcemu.base.JTCScreen;
import org.jens_mueller.jtcemu.base.JTCSys;
import org.jens_mueller.jtcemu.platform.fx.JTCEMUApplication;


public class ScreenNode extends Canvas implements JTCScreen
{
  public static final String PROP_SCREEN_MARGIN     = "screen.margin";
  public static final String PROP_SCREEN_REFRESH_MS = "screen.refresh.ms";
  public static final String PROP_SCREEN_SCALE      = "screen.scale";

  public static final int DEFAULT_SCREEN_MARGIN     = 20;
  public static final int DEFAULT_SCREEN_REFRESH_MS = 50;

  private JTCEMUApplication JTCEMUApplication;
  private JTCNode             jtcNode;
  private JTCSys              jtcSys;
  private Color[]             colors;
  private Color               markColor;
  private CharRaster          charRaster;
  private Point2D             dragStart;
  private Point2D             dragEnd;
  private boolean             textSelected;
  private int                 selectionCharX1;
  private int                 selectionCharX2;
  private int                 selectionCharY1;
  private int                 selectionCharY2;
  private java.util.TimerTask refreshTask;
  private long                refreshMillis;
  private volatile int        pendingUpdScreenEvents;
  private int                 margin;
  private int                 screenScale;
  private boolean             useMinPrefSize;
  private boolean             screenOutputEnabled;
  private volatile boolean    dirty;


  public ScreenNode(JTCEMUApplication JTCEMUApplication, JTCNode jtcNode )
  {
    this.JTCEMUApplication = JTCEMUApplication;
    this.jtcNode                = jtcNode;
    this.jtcSys                 = null;
    this.colors                 = null;
    this.refreshTask            = null;
    this.markColor              = new Color( 0.75, 0.5, 0.0, 0.5 );
    this.charRaster             = null;
    this.dragStart              = null;
    this.dragEnd                = null;
    this.textSelected           = false;
    this.selectionCharX1        = -1;
    this.selectionCharY1        = -1;
    this.selectionCharX2        = -1;
    this.selectionCharY2        = -1;
    this.pendingUpdScreenEvents = 0;
    this.margin                 = getMargin();
    this.screenScale            = 1;
    this.screenOutputEnabled    = false;
    this.useMinPrefSize         = false;
    this.dirty                  = true;
    updRefreshMillis();
    setOnMouseDragged( e->mouseDragged( e ) );
    setOnMousePressed( e->mousePressed( e ) );
  }


  public void clearSelection()
  {
    this.dragStart       = null;
    this.dragEnd         = null;
    this.selectionCharX1 = -1;
    this.selectionCharY1 = -1;
    this.selectionCharX2 = -1;
    this.selectionCharY2 = -1;
    setScreenDirty();
  }


  public static int getDefaultScreenScale( JTCSys.OSType osType )
  {
    int screenScale = 1;
    switch( osType ) {
      case OS2K:
      case ES1988:
	screenScale = 4;
      case ES23:
	screenScale = 2;
	break;
      case ES40:
	screenScale = 1;
	break;
    }
    return screenScale;
  }


  public long getRefreshMillis()
  {
    return this.refreshMillis;
  }


  public int getScreenScale()
  {
    return this.screenScale;
  }


  public String getSelectedText()
  {
    String text = null;
    if( (this.jtcSys != null)
	&& (this.selectionCharX1 >= 0)
	&& (this.selectionCharY1 >= 0)
	&& (this.selectionCharX2 >= 0)
	&& (this.selectionCharY2 >= 0) )
    {
      text = this.jtcSys.getScreenText(
				this.selectionCharX1,
				this.selectionCharY1,
				this.selectionCharX2,
				this.selectionCharY2 );
    }
    return text;
  }


  public synchronized void reinstallRefreshTimer()
  {
    if( this.refreshTask != null ) {
      this.refreshTask.cancel();
      JTCEMUApplication.getTimer().purge();
      installRefreshTask();
    }
  }


  public void settingsChanged()
  {
    this.margin        = getMargin();
    this.refreshMillis = AppContext.getIntProperty(
					PROP_SCREEN_REFRESH_MS,
					DEFAULT_SCREEN_REFRESH_MS );
    reinstallRefreshTimer();
  }


  public void setJTCSys( JTCSys jtcSys )
  {
    this.jtcSys = jtcSys;

    int[] rgbs  = jtcSys.getColorModeRGBs();
    this.colors = new Color[ rgbs.length ];
    for( int i = 0; i < colors.length; i++ ) {
      int rgb          = rgbs[ i ];
      this.colors[ i ] = new Color(
				(double) ((rgb >> 16) & 0xFF) / 255.0,
				(double) ((rgb >> 8) & 0xFF) / 255.0,
				(double) (rgb & 0xFF) / 255.0,
				1F );
    }
    int screenScale = AppContext.getIntProperty( PROP_SCREEN_SCALE, 0 );
    if( screenScale > 0 ) {
      this.screenScale = screenScale;
    } else {
      this.screenScale = getDefaultScreenScale( jtcSys.getOSType() );
    }
    installRefreshTask();
  }


  public void setVisibleScreenArea( double w, double h )
  {
    this.useMinPrefSize = true;
    setWidth( w );
    setHeight( h );
    setScreenDirty();
  }


	/* --- JTCScreen --- */

  @Override
  public void screenConfigChanged()
  {
    // leer
  }


  @Override
  public void setScreenDirty()
  {
    this.dirty = true;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public boolean isResizable()
  {
    return true;
  }


  @Override
  public double prefHeight( double height )
  {
    int h = 0;
    if( this.jtcSys != null ) {
      h = this.jtcSys.getScreenHeight();
      if( !this.useMinPrefSize )  {
	h = (2 * this.margin) + (h * this.screenScale);
      }
    }
    return (double) h;
  }


  @Override
  public double prefWidth( double width )
  {
    int w = 0;
    if( this.jtcSys != null ) {
      w = this.jtcSys.getScreenWidth();
      if( !this.useMinPrefSize )  {
	w = (2 * this.margin) + (w * this.screenScale);
      }
    }
    return (double) w;
  }


	/* --- private Methoden --- */

  private void fireUpdScreen()
  {
    /*
     * Wenn die Rechenleistung nicht ausreicht, kann es vorkommen,
     * dass die Events langsamer abgearbeitet werden,
     * als die Refresh-Rate ist und
     * es dadurch zum einer Ueberflutung der Event-Queue kommt.
     * Aus diesem Grund wird geprueft, dass nicht zuviele 
     * Events Refresh-Anforderungen in der Queue sind.
     * Es werden aber trotzdem mehrere Refresh-Anforderungen
     * in der Queue zugelassen, damit die Anzeige nicht
     * wegen einem verloren gegangenen Event stehen bleibt.
     */
    if( this.pendingUpdScreenEvents < 10 ) {
      Platform.runLater( ()->updScreen() );
      this.pendingUpdScreenEvents++;
    }
  }


  public int getMargin()
  {
    int m = AppContext.getIntProperty(
				PROP_SCREEN_MARGIN,
				DEFAULT_SCREEN_MARGIN );
    return m >= 0 ? m : 0;
  }


  private synchronized void installRefreshTask()
  {
    this.refreshTask = new java.util.TimerTask()
					{
					  @Override
					  public void run()
					  {
					    fireUpdScreen();
					  }
					};
    updRefreshMillis();
    JTCEMUApplication.getTimer().scheduleAtFixedRate(
				this.refreshTask,
				this.refreshMillis,
				this.refreshMillis );
  }


  private void mouseDragged( MouseEvent e )
  {
    if( this.dragStart == null ) {
      this.charRaster = this.jtcSys.getScreenCharRaster();
      if( this.charRaster != null ) {
	this.dragStart = new Point2D( e.getX(), e.getY() );
	this.dragEnd   = null;
	setScreenDirty();
      }
    } else {
      if( this.charRaster != null ) {
	this.dragEnd = new Point2D( e.getX(), e.getY() );
      } else {
	this.dragEnd   = null;
	this.dragStart = null;
      }
      setScreenDirty();
    }
    e.consume();
  }


  private void mousePressed( MouseEvent e )
  {
    if( e.getButton() == MouseButton.PRIMARY ) {
      clearSelection();
    }
  }


  private void updRefreshMillis()
  {
    this.refreshMillis = AppContext.getIntProperty(
						PROP_SCREEN_REFRESH_MS,
						DEFAULT_SCREEN_REFRESH_MS );
  }


  private void updScreen()
  {
    boolean screenOutputEnabled = this.jtcSys.isScreenOutputEnabled();
    if( (screenOutputEnabled != this.screenOutputEnabled) || this.dirty ) {
      this.screenOutputEnabled = screenOutputEnabled;

      GraphicsContext gc = getGraphicsContext2D();
      double          w  = getWidth();
      double          h  = getHeight();
      if( (gc != null) && (this.jtcSys != null) && (this.colors != null)
	  && (w > 1.0) && (h > 1.0) )
      {
	this.dirty = false;

	// Hintergrund
	Color bgColor = Color.BLACK;
	gc.setFill( bgColor );
	gc.fillRect( 0.0, 0.0, w, h );
	if( screenOutputEnabled ) {

	  // Umwandlung in Integer-Werte und Skalierung ermitteln
	  int scale = 1;
	  int wNode = (int) w;
	  int hNode = (int) h;
	  int wBase = this.jtcSys.getScreenWidth();
	  int hBase = this.jtcSys.getScreenHeight();
	  if( (wBase > 0) && (hBase > 0) ) {
	    if( ((wNode + this.margin) > wBase)
		&& ((hNode + this.margin) > hBase) )
	    {
	      scale = Math.min(
			(wNode - this.margin) / wBase,
			(hNode - this.margin) / hBase );
	      if( scale < 1 ) {
		scale = 1;
	      }
	    }
	  }
	  this.screenScale = scale;

	  // Bildausgabe zentrieren
	  int xOffs = (wNode - (wBase * scale)) / 2;
	  if( xOffs < 0 ) {
	    xOffs = 0;
	  }
	  int yOffs = (hNode - (hBase * scale)) / 2;
	  if( yOffs < 0 ) {
	    yOffs = 0;
	  }

	  /*
	   * Aus Gruenden der Performance werden nebeneinander liegende
	   * Punkte zusammengefasst und als Linie gezeichnet.
	   */
	  for( int y = 0; y < hBase; y++ ) {
	    Color lastColor = null;
	    int   xColorBeg = -1;
	    for( int x = 0; x < wBase; x++ ) {
	      int   pixel = this.jtcSys.getPixelColorNum( x, y );
	      Color color = (this.jtcSys.isMonochrome() ?
				(pixel > 0 ? Color.WHITE : Color.BLACK)
				: this.colors[ pixel & 0x0F ]);
	      if( (color != null) && (color != lastColor) ) {
		if( (lastColor != null)
		    && (lastColor != bgColor)
		    && (xColorBeg >= 0) )
		{
		  gc.setFill( lastColor );
		  gc.fillRect(
			xOffs + (xColorBeg * scale),
			yOffs + (y * scale),
			(x - xColorBeg) * scale,
			scale );
		}
		xColorBeg = x;
		lastColor = color;
	      }
	    }
	    if( (lastColor != null)
		 && (lastColor != bgColor)
		&& (xColorBeg >= 0) )
	    {
	      gc.setFill( lastColor );
	      gc.fillRect(
			xOffs + (xColorBeg * scale),
			yOffs + (y * scale),
			(wBase - xColorBeg) * scale,
			scale );
	    }
	  }

	  // Markierter Text
	  boolean    textSelected = false;
	  CharRaster charRaster   = this.charRaster;
	  Point2D    dragStart    = this.dragStart;
	  Point2D    dragEnd      = this.dragEnd;
	  if( (charRaster != null)
	      && (dragStart != null)
	      && (dragEnd != null)
	      && (scale > 0) )
	  {
	    int nCols = this.charRaster.getColCount();
	    int nRows = this.charRaster.getRowCount();
	    int wChar = this.charRaster.getCharWidth();
	    int hChar = this.charRaster.getCharHeight();
	    if( (nCols > 0) && (nRows > 0)
		&& (wChar > 0) && (hChar > 0) )
	    {
	      int x1 = (int) Math.round( dragStart.getX() );
	      int y1 = (int) Math.round( dragStart.getY() );
	      int x2 = (int) Math.round( dragEnd.getX() );
	      int y2 = (int) Math.round( dragEnd.getY() );

	      xOffs += (this.charRaster.getXOffset() * scale);
	      yOffs += (this.charRaster.getYOffset() * scale);

	      // Zeichenpositionen berechnen
	      this.selectionCharX1 = Math.max(
			(x1 - xOffs) / scale, 0 ) / wChar;
	      this.selectionCharY1 = Math.max(
			(y1 - yOffs) / scale, 0 ) / hChar;
	      this.selectionCharX2 = Math.max(
			(x2 - xOffs) / scale, 0 ) / wChar;
	      this.selectionCharY2 = Math.max(
			(y2 - yOffs) / scale , 0 ) / hChar;
	      if( this.selectionCharX1 >= nCols ) {
		this.selectionCharX1 = nCols - 1;
	      }
	      if( this.selectionCharY1 >= nRows ) {
		this.selectionCharY1 = nRows - 1;
	      }
	      if( this.selectionCharX2 >= nCols ) {
		this.selectionCharX2 = nCols - 1;
	      }
	      if( this.selectionCharY2 >= nRows ) {
		this.selectionCharY2 = nRows - 1;
	      }

	      // Koordinaten tauschen, wenn Endpunkt vor Startpunkt liegt
	      if( (this.selectionCharY1 > this.selectionCharY2)
		  || ((this.selectionCharY1 == this.selectionCharY2)
		      && (this.selectionCharX1 > this.selectionCharX2)) )
	      {
		int m = this.selectionCharX1;
		this.selectionCharX1 = this.selectionCharX2;
		this.selectionCharX2 = m;

		m = this.selectionCharY1;
		this.selectionCharY1 = this.selectionCharY2;
		this.selectionCharY2 = m;

		m  = x1;
		x1 = x2;
		x2 = m;

		m  = y1;
		y1 = y2;
		y2 = m;
	      }

	      /*
	       * Koordinaten anpassen,
	       * wenn Endpunkt ausserhalb der Bildschirmausgabe liegt
	       */
	      if( y1 < yOffs ) {
		this.selectionCharX1 = 0;
		this.selectionCharY1 = 0;
	      } else {
		if( x1 > (xOffs + (scale * nCols * wChar)) ) {
		  this.selectionCharX1 = 0;
		  this.selectionCharY1++;
		}
	      }
	      if( y2 > (yOffs + (scale * (nRows * hChar))) ) {
		this.selectionCharX2 = nCols - 1;
		this.selectionCharY2 = nRows - 1;
	      } else {
		if( x2 < xOffs ) {
		  this.selectionCharX2 = nCols - 1;
		  --this.selectionCharY2;
		}
	      }

	      // Markierter Text visualisieren
	      gc.setFill( this.markColor );
	      if( this.selectionCharY1 == this.selectionCharY2 ) {
		gc.fillRect(
			xOffs + (scale * this.selectionCharX1 * wChar),
			yOffs + (scale * this.selectionCharY1 * hChar),
			scale * (this.selectionCharX2
					- this.selectionCharX1 + 1) * wChar,
			scale * hChar );
	      } else {
		gc.fillRect(
			xOffs + (scale * this.selectionCharX1 * wChar),
			yOffs + (scale * this.selectionCharY1 * hChar),
			scale * (nCols - this.selectionCharX1) * wChar,
			scale * hChar );
		if( this.selectionCharY1 + 1 < this.selectionCharY2 ) {
		  gc.fillRect(
			xOffs,
			yOffs + (scale * (this.selectionCharY1 + 1) * hChar),
			scale * nCols * wChar,
			scale * (this.selectionCharY2
					- this.selectionCharY1 - 1) * hChar );
		}
		gc.fillRect(
			xOffs,
			yOffs + (scale * this.selectionCharY2 * hChar),
			scale * (this.selectionCharX2 + 1) * wChar,
			scale * hChar );
	      }
	      textSelected = true;
	    }
	  }
	  if( textSelected != this.textSelected ) {
	    this.textSelected = textSelected;
	    this.jtcNode.setScreenTextSelected( textSelected );
	  }
	}
      }
    }
    --this.pendingUpdScreenEvents;
  }
}
