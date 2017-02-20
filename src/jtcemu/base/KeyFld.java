/*
 * (c) 2007-2010 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Komponente fuer eine Taste der Bildschirmtastatur
 */

package jtcemu.base;

import java.awt.*;
import java.awt.event.*;
import java.lang.*;
import javax.swing.*;
import javax.swing.border.Border;


public class KeyFld extends JComponent implements MouseListener
{
  private static final Border borderSelected
			= BorderFactory.createLoweredBevelBorder();

  private static final Border borderUnselected
			= BorderFactory.createRaisedBevelBorder();

  private static final Font keyFont = new Font( "SansSerif", Font.BOLD, 12 );
  private static final Font cmdFont = new Font( "SansSerif", Font.PLAIN, 8 );
  private static final int  MARGIN  = 3;

  private KeyboardFrm keyboardFrm;
  private Image       baseImage;
  private String      baseText;
  private String      cmdText;
  private Image       shiftImage;
  private String      shiftText;
  private String      shiftCmdText;
  private Dimension   prefSize;
  private int         wBase;
  private int         wCmd;
  private int         wShift;
  private boolean     clicked;
  private boolean     selected;


  public KeyFld(
		KeyboardFrm keyboardFrm,
		Image       baseImage,
		String      baseText,
		String      cmdText,
		Image       shiftImage,
		String      shiftText,
		String      shiftCmdText )
  {
    this.keyboardFrm = keyboardFrm;
    setValues(
	baseImage,
	baseText,
	cmdText,
	shiftImage,
	shiftText,
	shiftCmdText );

    this.clicked  = false;
    this.selected = false;
    setBorder( borderUnselected );
    setFocusable( false );
    addMouseListener( this );
  }


  public void setSelected( boolean state )
  {
    if( state != this.selected ) {
      this.selected = state;
      setBorder( this.selected ? borderSelected : borderUnselected );
      setFocusable( false );
      this.keyboardFrm.keyStatusChanged( this, this.selected );
      repaint();
    }
  }


  public void setValues(
		Image  baseImage,
		String baseText,
		String cmdText,
		Image  shiftImage,
		String shiftText,
		String shiftCmdText )
  {
    this.baseImage    = baseImage;
    this.baseText     = baseText;
    this.cmdText      = cmdText;
    this.shiftImage   = shiftImage;
    this.shiftText    = shiftText;
    this.shiftCmdText = shiftCmdText;
    this.prefSize     = null;
    this.wBase        = -1;
    this.wCmd         = -1;
    this.wShift       = -1;

    String toolTipText = null;
    if( (this.baseImage != null) && (this.baseText != null) ) {
      if( (shiftImage != null) && (this.shiftText != null) ) {
	toolTipText = this.baseText + " / " + this.shiftText;
      } else {
	toolTipText = this.baseText;
      }
    }
    setToolTipText( toolTipText );
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
    if( (e.getComponent() == this)
	&& this.clicked
	&& !this.keyboardFrm.getKeepKeysPressed() )
    {
      setSelected( true );
    }
  }


  @Override
  public void mouseExited( MouseEvent e )
  {
    if( (e.getComponent() == this)
	&& this.clicked
	&& !this.keyboardFrm.getKeepKeysPressed() )
    {
      setSelected( false );
    }
  }


  @Override
  public void mousePressed( MouseEvent e )
  {
    if( e.getComponent() == this ) {
      if( this.keyboardFrm.getKeepKeysPressed() ) {
	setSelected( !this.selected );
      } else {
	this.clicked = true;
	setSelected( true );
      }
    }
  }


  @Override
  public void mouseReleased( MouseEvent e )
  {
    if( (e.getComponent() == this)
	&& !this.keyboardFrm.getKeepKeysPressed() )
    {
      this.clicked = false;
      setSelected( false );
    }
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public Dimension getPreferredSize()
  {
    if( this.prefSize == null ) {
      ensureWidthsCalculated();
      this.prefSize = new Dimension(
				(3 * MARGIN) + this.wBase + this.wCmd,
				(3 * MARGIN) + 12 + 12 );
    }
    return this.prefSize;
  }


  @Override
  protected void paintComponent( Graphics g )
  {
    ensureWidthsCalculated();
    g.setColor( this.selected ? Color.gray : Color.lightGray );
    g.fillRect( 0, 0, getWidth(), getHeight() );
    g.setColor( Color.black );
    if( this.shiftCmdText != null ) {
      g.setFont( cmdFont );
      g.drawString(
		this.shiftCmdText,
		getWidth() - this.wShift - MARGIN,
		MARGIN + 12 );
    }
    else if( this.shiftImage != null ) {
      g.drawImage( this.shiftImage, MARGIN, MARGIN, this );
    }
    else if( this.shiftText != null ) {
      g.setFont( keyFont );
      g.drawString( this.shiftText, MARGIN, MARGIN + 12 );
    }
    if( this.baseImage != null ) {
      g.drawImage( this.baseImage, MARGIN, MARGIN + 12 + MARGIN, this );
    }
    else if( this.baseText != null ) {
      g.setFont( keyFont );
      g.drawString( this.baseText, MARGIN, MARGIN + 12 + MARGIN + 12 );
    }
    if( this.cmdText != null ) {
      g.setFont( cmdFont );
      g.drawString(
		this.cmdText,
		getWidth() - this.wCmd - MARGIN,
		MARGIN + 12 + MARGIN + 12 );
    }
  }


	/* --- private Methoden --- */

  private void ensureWidthsCalculated()
  {
    if( this.wBase < 0 ) {
      if( this.baseImage != null ) {
	this.wBase = getImageWidth( this.baseImage );
      } else {
	this.wBase = textWidth( this.baseText, keyFont );
      }
    }
    if( this.wCmd < 0 ) {
      this.wCmd = textWidth( this.cmdText, cmdFont );
    }
    if( this.wShift < 0 ) {
      if( this.shiftCmdText != null ) {
	this.wShift = textWidth( this.shiftCmdText, cmdFont );
      }
      else if( this.shiftImage != null ) {
	this.wShift = getImageWidth( this.shiftImage );
      }
      else if( this.shiftText != null ) {
	this.wShift = textWidth( this.shiftText, keyFont );
      }
    }
  }


  private int getImageWidth( Image img )
  {
    MediaTracker mt = new MediaTracker( this );
    mt.addImage( img, 1 );
    try {
      mt.waitForAll();
    }
    catch( InterruptedException ex ) {}
    return img.getWidth( this );
  }


  private int textWidth( String text, Font font )
  {
    int w = 0;
    if( text != null ) {
      FontMetrics fm = getFontMetrics( font );
      if( fm != null )
	w = fm.stringWidth( text );
    }
    return w;
  }
}
