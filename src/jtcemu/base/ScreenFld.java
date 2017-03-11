/*
 * (c) 2007-2010 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Komponente fuer die Darstellung des Bildschirminhaltes
 */

package jtcemu.base;

import java.awt.*;
import java.awt.image.*;
import javax.swing.JComponent;


public class ScreenFld extends JComponent
{
  public enum Mode { M64X64, M128X128, M320X192 };

  private JTCSys           jtcSys;
  private volatile int     screenScale;
  private volatile int     margin;
  private volatile Mode    mode;
  private volatile boolean dirty;
  private Color[]          colors;


  public ScreenFld( JTCSys jtcSys, int screenScale )
  {
    this.jtcSys      = jtcSys;
    this.screenScale = screenScale;
    this.margin      = 0;
    this.mode        = Mode.M64X64;
    this.dirty       = false;
    this.colors      = new Color[ 16 ];
    for( int i = 0; i < colors.length; i++ ) {
      int r = ((i & 8) == 0 ? 155 : 0);
      int g = ((i & 4) == 0 ? 155 : 0);
      int b = ((i & 2) == 0 ? 155 : 0);
      if( (i & 1) == 0 ) {
        r += 100;
        g += 100;
        b += 100;
      }
      this.colors[ i ] = new Color( r, g, b );
    }
    updPreferredSize();
    this.jtcSys.setScreenFld( this );
  }


  public BufferedImage createBufferedImage()
  {
    BufferedImage img = null;
    int           w   = getWidth();
    int           h   = getHeight();
    if( (w > 0) && (h > 0) ) {
      IndexColorModel cm = null;
      if( this.mode == Mode.M320X192 ) {
        byte[] r = new byte[ this.colors.length ];
        byte[] g = new byte[ this.colors.length ];
        byte[] b = new byte[ this.colors.length ];
        for( int i = 0; i < this.colors.length; i++ ) {
          r[ i ] = (byte) this.colors[ i ].getRed();
          g[ i ] = (byte) this.colors[ i ].getGreen();
          b[ i ] = (byte) this.colors[ i ].getBlue();
        }
        cm = new IndexColorModel( 4, this.colors.length, r, g, b );
      } else {
        byte[] r = { (byte) 255, (byte) 0 };
        byte[] g = { (byte) 255, (byte) 0 };
        byte[] b = { (byte) 255, (byte) 0 };
        cm = new IndexColorModel( 1, 2, r, g, b );
      }
      img = new BufferedImage( w, h, BufferedImage.TYPE_BYTE_INDEXED, cm );
      Graphics g = img.createGraphics();
      paint( g, w, h );
      g.dispose();
    }
    return img;
  }


  public int getMargin()
  {
    return this.margin;
  }


  public Mode getMode()
  {
    return this.mode;
  }


  public int getScreenScale()
  {
    return this.screenScale;
  }


  public boolean isDirty()
  {
    return this.dirty;
  }


  public void setDirty()
  {
    this.dirty = true;
  }


  public void setMargin( int margin )
  {
    this.margin = margin;
    updPreferredSize();
  }


  public void setMode( Mode mode, boolean updScale )
  {
    if( (mode != null) && (mode != this.mode) ) {
      if( updScale ) {
        int hOld  = getPixelHeight( this.mode );
        int hNew  = getPixelHeight( mode );
        int scale = (int) Math.round( hOld * this.screenScale / hNew );
        if( (hNew < hOld)
            && (hNew * scale) > (hOld * this.screenScale) )
        {
          --scale;
        }
        else if( (hNew > hOld)
                 && (hNew * scale) < (hOld * this.screenScale) )
        {
          scale++;
        }
        if( scale < 1 ) {
          scale = 1;
        }
        else if( scale > 8 ) {
          scale = 8;
        }
        this.screenScale = scale;
      }
      this.mode = mode;
      updPreferredSize();
    }
  }


  public void setScreenScale( int screenScale )
  {
    if( (screenScale > 0) && (screenScale != this.screenScale) ) {
      this.screenScale = screenScale;
      updPreferredSize();
    }
  }


        /* --- ueberschriebene Methoden --- */

  @Override
  public boolean isFocusable()
  {
    return true;
  }


  @Override
  public void paint( Graphics g )
  {
    this.dirty = false;
    paint( g, getWidth(), getHeight() );
  }


  /*
   * update(...) wird ueberschrieben,
   * da paint(...) die Komponente vollstaendig fuellt
   * und somit das standardmaessige Fuellen mit der Hintegrundfarbe
   * entfallen kann.
   */
  @Override
  public void update( Graphics g )
  {
    paint( g );
  }


        /* --- private Methoden --- */

  private Color getColor( int value )
  {
    Color rv = Color.black;
    if( (this.mode == Mode.M320X192)
        && (value >= 0) && (value < this.colors.length) )
    {
      rv = this.colors[ value ];
    } else {
      if( value > 0 ) {
        rv = Color.white;
      }
    }
    return rv;
  }


  private Point getOffset( int wNorm, int hNorm )
  {
    int w = getWidth();
    if( w < 1 ) {
      w = wNorm * this.screenScale;
    }

    int h = getHeight();
    if( h < 1 ) {
      h = hNorm * this.screenScale;
    }

    int xOffset = (w - (wNorm * this.screenScale)) / 2;
    if( xOffset < 0 ) {
      xOffset = 0;
    }
    int yOffset = (h - (hNorm * this.screenScale)) / 2;
    if( yOffset < 0 ) {
      yOffset = 0;
    }
    return new Point( xOffset, yOffset );
  }


  private static int getPixelHeight( Mode mode )
  {
    int rv = 64;
    switch( mode ) {
      case M128X128:
        rv = 128;
        break;

      case M320X192:
        rv = 192;
        break;
    }
    return rv;
  }



  private void paint( Graphics g, int w, int h )
  {
    int wNorm = 64;
    int hNorm = 64;
    if( this.mode == Mode.M128X128 ) {
      wNorm = 128;
      hNorm = 128;
    }
    else if( this.mode == Mode.M320X192 ) {
      wNorm = 320;
      hNorm = 192;
    }

    // Hintergrund
    Color bgColor = Color.black;
    g.setColor( bgColor );
    g.fillRect( 0, 0, w, h );

    // Vordergrund zentrieren
    Point offset = getOffset( wNorm, hNorm );
    if( (offset.x > 0) && (offset.y > 0) ) {
      g.translate( offset.x, offset.y );
    }

    if( this.jtcSys.isScreenOutputEnabled() ) {

      /*
       * Aus Greunden der Performance werden nebeneinander liegende
       * Punkte zusammengefasst und als Linie gezeichnet.
       */
      for( int y = 0; y < hNorm; y++ ) {
        Color lastColor = null;
        int   xColorBeg = -1;
        for( int x = 0; x < wNorm; x++ ) {
          Color color = getColor( this.jtcSys.getPixel( x, y ) );
          if( (color != null) && (color != lastColor) ) {
            if( (lastColor != null)
                && (lastColor != bgColor)
                && (xColorBeg >= 0) )
            {
              g.setColor( lastColor );
              g.fillRect(
                        xColorBeg * this.screenScale,
                        y * this.screenScale,
                        (x - xColorBeg) * this.screenScale,
                        this.screenScale );
            }
            xColorBeg = x;
            lastColor = color;
          }
        }
        if( (lastColor != null)
             && (lastColor != bgColor)
            && (xColorBeg >= 0) )
        {
          g.setColor( lastColor );
          g.fillRect(
                xColorBeg * this.screenScale,
                y * this.screenScale,
                (wNorm - xColorBeg) * this.screenScale,
                this.screenScale );
        }
      }
    }
  }


  private void updPreferredSize()
  {
    int margin = this.margin;
    if( margin < 0 ) {
      margin = 0;
    }
    if( this.mode == Mode.M320X192 ) {
      setPreferredSize( new Dimension(
                                (2 * margin) + (320 * this.screenScale),
                                (2 * margin) + (192 * this.screenScale) ) );
    } else if( this.mode == Mode.M128X128 ) {
      setPreferredSize( new Dimension(
                                (2 * margin) + (128 * this.screenScale),
                                (2 * margin) + (128 * this.screenScale) ) );
    } else {
      setPreferredSize( new Dimension(
                                (2 * margin) + (64 * this.screenScale),
                                (2 * margin) + (64 * this.screenScale) ) );
    }
    Container parent = getParent();
    if( parent != null ) {
      parent.invalidate();
    } else {
      invalidate();
    }
  }
}
