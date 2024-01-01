/*
 * (c) 2021 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Basisklasse fuer die Bildschirmtastatur
 */

package org.jens_mueller.jtcemu.platform.se.keyboard;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.net.URL;
import java.util.Arrays;
import javax.swing.JPanel;
import org.jens_mueller.jtcemu.base.JTCSys;
import org.jens_mueller.jtcemu.platform.se.Main;


public abstract class AbstractKeyboardFld
				extends JPanel
				implements MouseListener
{
  private JTCSys      jtcSys;
  private int         cols;
  private int         firstColIdx;
  private int         wKey;
  private int         hKey;
  private int         keyIdx;
  private int         keyColIdx;
  private int         keyColValue;
  private int[]       keyMatrixCols;
  private KeyFld[]    keys;
  private KeyboardFrm keyboardFrm;
  private boolean     notified;


  protected AbstractKeyboardFld(
			JTCSys jtcSys,
			int    firstColIdx,
			int    cols,
			int    wKey,
			int    hKey )
  {
    this.jtcSys        = jtcSys;
    this.firstColIdx   = firstColIdx;
    this.cols          = cols;
    this.wKey          = wKey;
    this.hKey          = hKey;
    this.keyIdx        = 0;
    this.keyColIdx     = 0;
    this.keyColValue   = 0x08;
    this.keyMatrixCols = new int[ jtcSys.getKeyMatrixCols().length ];
    this.keys          = new KeyFld[ 4 * cols ];
    this.keyboardFrm   = null;
    this.notified      = false;
    setLayout( new GridLayout( 4, cols, 5, 5 ) );
  }


  /*
   * Die Tasten muessen von links oben nach rechts unten hinzugefuegt werden.
   */
  protected KeyFld addKey( KeyFld keyFld )
  {
    this.keys[ this.keyIdx++ ] = keyFld;
    this.keyColIdx++;
    if( this.keyColIdx >= this.cols ) {
      this.keyColIdx = 0;
      this.keyColValue >>= 1;
    }

    Component c = keyFld;
    if( c == null ) {
      c = new JPanel();
    }
    c.setPreferredSize( new Dimension( this.wKey, this.hKey ) );
    add( c );

    return keyFld;
  }


  protected KeyFld addKey(
			Object baseObj,
			Object shift1Obj,
			Object shift2Obj,
			String shift3Text )
  {
    return addKey( new KeyFld(
			this.keyColIdx + this.firstColIdx,
			this.keyColValue,
			baseObj,
			shift1Obj,
			shift2Obj,
			shift3Text ) );
  }


  protected KeyFld addKey(
			Object baseObj,
			Object shift1Obj,
			Object shift2Obj )
  {
    return addKey( baseObj, shift1Obj, shift2Obj, null );
  }


  protected KeyFld addKey( Object baseObj, Object shift1Obj )
  {
    return addKey( baseObj, shift1Obj, null, null );
  }


  protected KeyFld addKey( Object baseObj )
  {
    return addKey( baseObj, null, null, null );
  }


  protected static void drawRight( Graphics g, String text, int x, int y )
  {
    if( text != null ) {
      FontMetrics fm = g.getFontMetrics();
      if( fm != null ) {
	g.drawString( text, x - fm.stringWidth( text ), y );
      }
    }
  }


  public String getHoldShiftKeysText()
  {
    return null;
  }


  public Object getImageDown()
  {
    Image img = getImage( "/images/key/down.png" );
    return img != null ? img : "\u2193";
  }


  public Object getImageLeft()
  {
    Image img = getImage( "/images/key/left.png" );
    return img != null ? img : "\u2190";
  }


  public Object getImageRight()
  {
    Image img = getImage( "/images/key/right.png" );
    return img != null ? img : "\u2192";
  }


  public Object getImageShift()
  {
    Image img = getImage( "/images/key/shift.png" );
    return img != null ? img : "SHT";
  }


  public Object getImageSpace()
  {
    return getImage( "/images/key/space.png" );
  }


  public Object getImageUp()
  {
    Image img = getImage( "/images/key/up.png" );
    return img != null ? img : "\u2191";
  }


  public abstract JTCSys.OSType getOSType();


  /*
   * Die Implementierung der Basisklasse malt nur den Hintergrund.
   */
  public void paintKeyFld( Graphics g, KeyFld keyFld )
  {
    int w = keyFld.getWidth();
    int h = keyFld.getHeight();
    if( (w > 0) && (h > 0) ) {
      g.setColor( keyFld.isPressed() ? Color.GRAY : Color.LIGHT_GRAY );
      g.fillRect( 0, 0, w, h );
    }
  }


  public void reset()
  {
    boolean changed = false;
    for( KeyFld keyFld : this.keys ) {
      if( keyFld != null ) {
	if( keyFld.isPressed() ) {
	  keyFld.setPressed( false, false );
	  changed = true;
	}
      }
    }
    if( changed ) {
      updKeyMatrix();
    }
  }


  public void setKeyboardFrm( KeyboardFrm keyboardFrm )
  {
    this.keyboardFrm = keyboardFrm;
  }


  public void updKeyFields()
  {
    int[] keyMatrixCols = this.jtcSys.getKeyMatrixCols();
    int   len = Math.min(
			keyMatrixCols.length,
			this.keyMatrixCols.length );
    System.arraycopy( keyMatrixCols, 0, this.keyMatrixCols, 0, len );
    for( KeyFld keyFld : this.keys ) {
      if( keyFld != null ) {
	int col = keyFld.getColIdx();
	if( (col >= 0) && (col < len) ) {
	  boolean pressed = ((this.keyMatrixCols[ col ]
					& keyFld.getColValue()) != 0);
	  if( pressed != keyFld.isPressed() ) {
	    keyFld.setPressed( pressed, false );
	  }
	}
      }
    }
  }


	/* --- MouseListener --- */

  @Override
  public void mouseClicked( MouseEvent e )
  {
    // leer
  }

  @Override
  public void mouseEntered( MouseEvent e )
  {
    // leer
  }

  @Override
  public void mouseExited( MouseEvent e )
  {
    // leer
  }

  @Override
  public void mousePressed( MouseEvent e )
  {
    Component c = e.getComponent();
    if( c != null ) {
      if( !(c instanceof KeyFld) ) {
	c = c.getComponentAt( e.getPoint() );
      }
    }
    if( c != null ) {
      if( c instanceof KeyFld ) {
	KeyFld keyFld = (KeyFld) c;
	if( keyFld.isPressed() ) {
	  keyFld.setPressed( false, false );
	  updKeyMatrix();
	} else {
	  boolean snapped = (e.isShiftDown() || e.isControlDown());
	  if( !snapped ) {
	    int b = e.getButton();
	    if( (b == MouseEvent.BUTTON2) || (b == MouseEvent.BUTTON3) ) {
	      snapped = true;
	    }
	  }
	  keyFld.setPressed( true, snapped );
	  int[] matrix = this.jtcSys.getKeyMatrixCols();
	  int   col    = keyFld.getColIdx();
	  if( (col >= 0) && (col < matrix.length) ) {
	    matrix[ col ] |= keyFld.getColValue();
	  }
	}
      }
    }
  }

  @Override
  public void mouseReleased( MouseEvent e )
  {
    Component c = e.getComponent();
    if( c != null ) {
      if( !(c instanceof KeyFld) ) {
	c = c.getComponentAt( e.getPoint() );
      }
    }
    boolean holdShiftKeys = true;
    if( this.keyboardFrm != null ) {
      holdShiftKeys = this.keyboardFrm.getHoldShiftKeys();
    }
    boolean changed = false;
    for( KeyFld keyFld : this.keys ) {
      if( keyFld != null ) {
	if( keyFld.isPressed()
	    && !keyFld.isSnapped()
	    && (!keyFld.isShiftKey() || !holdShiftKeys || (keyFld != c)) )
	{
	  keyFld.setPressed( false, false );
	  changed = true;
	}
      }
    }
    if( changed ) {
      updKeyMatrix();
    }
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void addNotify()
  {
    super.addNotify();
    if( !this.notified ) {
      this.notified = true;
      addMouseListener( this );
    }
  }


  @Override
  public void removeNotify()
  {
    if( this.notified ) {
      this.notified = false;
      removeMouseListener( this );
    }
  }


	/* --- private Methoden --- */

  private Image getImage( String resource )
  {
    Image img = null;
    URL   url = Main.class.getResource( resource );
    if( url != null ) {
      img = Main.getTopFrm().getToolkit().createImage( url );
    }
    return img;
  }


  private void updKeyMatrix()
  {
    int[] keyMatrixCols = this.jtcSys.getKeyMatrixCols();
    synchronized( this.keyMatrixCols ) {
      Arrays.fill( keyMatrixCols, 0 );
      for( KeyFld keyFld : this.keys ) {
	if( keyFld != null ) {
	  if( keyFld.isPressed() ) {
	    int col = keyFld.getColIdx();
	    if( (col >= 0) && (col < keyMatrixCols.length) ) {
	      keyMatrixCols[ col ] |= keyFld.getColValue();
	    }
	  }
	}
      }
    }
  }
}
