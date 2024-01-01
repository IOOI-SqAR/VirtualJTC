/*
 * (c) 2007-2021 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Wrapper-Klasse und Document fuer ein hexadezimales
 * Eingabefeld mit Erkennung manueller Aenderungen
 */

package org.jens_mueller.jtcemu.platform.se.tools.debugger;

import org.jens_mueller.jtcemu.platform.se.base.GUIUtil;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;
import java.awt.*;
import java.awt.event.*;
import java.util.Arrays;


public class HexFld extends PlainDocument
			implements
				ActionListener,
				FocusListener,
				MouseListener
{
  private boolean          dataChanged;
  private boolean          editMode;
  private boolean          readOnly;
  private boolean          marked;
  private String           orgText;
  private int              orgValue;
  private int              value;
  private int              inputMask;
  private ChangeListener[] changeListeners;
  private char[]           digitBuf;
  private JPopupMenu       popup;
  private JMenuItem        mnuCopy;
  private JMenuItem        mnuPaste;
  private JMenuItem        mnuReset;
  private JTextField       textFld;
  private Color            bgColor;


  public HexFld( int maxDigits )
  {
    this.dataChanged = false;
    this.editMode    = false;
    this.readOnly    = false;
    this.marked      = false;
    this.orgText     = "";
    this.orgValue    = 0;
    this.value       = 0;
    this.inputMask   = 0;
    for( int i = 0; i < maxDigits; i++ ) {
      this.inputMask = (this.inputMask << 4) | 0x0F;
    }
    this.changeListeners = null;
    this.digitBuf        = new char[ maxDigits ];
    this.popup           = new JPopupMenu();
    this.mnuCopy         = this.popup.add( "Kopieren" );
    this.mnuPaste        = this.popup.add( "Einf\u00FCgen" );
    this.popup.addSeparator();
    this.mnuReset = this.popup.add( "Zur\u00FCcksetzen auf Ursprungswert" );

    final HexFld hexFld = this;
    this.textFld  = new JTextField( this, this.orgText, maxDigits )
				{
				  @Override
				  public void copy()
				  {
				    hexFld.copy();
				  }

				  @Override
				  public void paste()
				  {
				    hexFld.paste();
				  }
				};
    this.bgColor = this.textFld.getBackground();
    if( this.bgColor == null ) {
      this.bgColor = Color.WHITE;
    }
    this.textFld.setEditable( false );
    this.textFld.addActionListener( this );
    this.textFld.addFocusListener( this );
    this.textFld.addMouseListener( this );
    this.mnuCopy.addActionListener( this );
    this.mnuPaste.addActionListener( this );
    this.mnuReset.addActionListener( this );
  }


  public void addChangeListener( ChangeListener listener )
  {
    if( this.changeListeners != null ) {
      int len              = this.changeListeners.length;
      this.changeListeners = Arrays.copyOf( this.changeListeners, len + 1 );
      this.changeListeners[ len ] = listener;
    } else {
      this.changeListeners      = new ChangeListener[ 1 ];
      this.changeListeners[ 0 ] = listener;
    }
  }


  public void clearValue()
  {
    this.value       = 0;
    this.orgValue    = 0;
    this.dataChanged = false;
    this.editMode    = false;
    this.textFld.setText( "" );
    this.textFld.setEditable( false );
    updBackground();
  }


  public void copy()
  {
    String text = this.textFld.getSelectedText();
    if( text == null ) {
      text = "";
    }
    if( text.isEmpty() ) {
      text = this.textFld.getText();
    }
    GUIUtil.copyToClipboard( this.textFld, text );
  }


  public Component getComponent()
  {
    return this.textFld;
  }


  public int getOrgValue()
  {
    return this.orgValue;
  }


  public int getValue()
  {
    return this.editMode && this.dataChanged ? this.value : this.orgValue;
  }


  public boolean isReadOnly()
  {
    return this.readOnly;
  }


  public void paste()
  {
    if( this.textFld.isEditable() ) {
      String text = GUIUtil.getClipboardText( this.textFld );
      if( text != null ) {
	this.textFld.setText( text );
	checkText();
      }
    }
  }


  public void resetValue()
  {
    if( this.editMode && !this.readOnly && this.textFld.isEditable() ) {
      this.value = this.orgValue;
      checkValue();
    }
  }


  public void setEditBit( int mask, boolean value )
  {
    if( this.editMode ) {
      if( !this.dataChanged ) {
	this.value = this.orgValue;
      }
      if( value ) {
	this.value |= mask;
      } else {
	this.value &= ~mask;
      }
      checkValue();
    }
  }


  public void setInputMask( int mask )
  {
    this.inputMask = mask;
  }


  public void setMarked( boolean state )
  {
    this.marked = state;
    updBackground();
  }


  public void setReadOnly( boolean state )
  {
    this.readOnly = state;
    if( this.editMode ) {
      this.dataChanged = false;
      this.editMode    = false;
      this.textFld.setText( this.orgText );
      this.textFld.setEditable( false );
    }
  }


  public void setValue( int value )
  {
    this.dataChanged = false;
    this.value       = value;
    this.orgValue    = value;
    this.orgText     = createHexString( value );
    this.textFld.setText( this.orgText );
    if( !this.readOnly ) {
      this.editMode = true;
      this.textFld.setEditable( true );
    }
  }


  public boolean wasDataChanged()
  {
    return this.dataChanged;
  }


	/* --- ActionListener --- */

  @Override
  public void actionPerformed( ActionEvent e )
  {
    Object src = e.getSource();
    if( src == this.mnuCopy ) {
      this.textFld.copy();
    } else if( (src == this.mnuPaste)
	       && this.editMode
	       && !this.readOnly
	       && this.textFld.isEditable() )
    {
      this.textFld.paste();
    } else if( src == this.mnuReset ) {
      resetValue();
    } else if( src == this.textFld ) {
      checkText();
    }
  }


	/* --- FocusListener --- */

  @Override
  public void focusGained( FocusEvent e )
  {
    // leer
  }


  @Override
  public void focusLost( FocusEvent e )
  {
    if( e.getSource() == this.textFld )
      checkText();
  }


	/* --- MouseListener --- */

  @Override
  public void mouseClicked( MouseEvent e )
  {
    checkOpenPopupMenu( e );
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
    checkOpenPopupMenu( e );
  }


  @Override
  public void mouseReleased( MouseEvent e )
  {
    checkOpenPopupMenu( e );
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void insertString(
			int          offs,
			String       text,
			AttributeSet a ) throws BadLocationException
  {
    if( text != null ) {
      int len = text.length();
      int pos = 0;
      while( (pos < len) && (getLength() < this.digitBuf.length) ) {
	char ch = Character.toUpperCase( text.charAt( pos++ ) );
	if( ((ch >= 0) && (ch <= '9')) || ((ch >= 'A') && (ch <= 'F')) ) {
	  super.insertString( offs++, Character.toString( ch ), a );
	}
      }
    }
  }


	/* --- private Methoden --- */

  private void checkOpenPopupMenu( MouseEvent e )
  {
    if( e.isPopupTrigger() ) {
      this.mnuCopy.setEnabled(
		this.textFld.isEnabled() && (getLength() > 0) );
      this.mnuPaste.setEnabled( this.editMode && !this.readOnly );
      this.mnuReset.setEnabled(
		this.editMode
			&& !this.readOnly
			&& (this.value != this.orgValue) );
      this.popup.show( e.getComponent(), e.getX(), e.getY() );
      e.consume();
    }
  }


  private void checkText()
  {
    if( this.editMode ) {
      this.value  = -1;
      String text = this.textFld.getText();
      if( text != null ) {
	if( !text.isEmpty() ) {
	  try {
	    this.value = Integer.parseInt( text, 16 ) & this.inputMask;
	  }
	  catch( NumberFormatException ex ) {}
	} else {
	  this.value = this.orgValue;
	}
      } else {
	this.value = this.orgValue;
      }
      checkValue();
    }
  }


  private void checkValue()
  {
    if( this.value != this.orgValue ) {
      this.dataChanged = true;
      this.textFld.setText( createHexString( this.value ) );
    } else {
      this.dataChanged = false;
      this.textFld.setText( this.orgText );
    }
    updBackground();
    if( this.changeListeners != null ) {
      ChangeEvent e = new ChangeEvent( this );
      for( int i = 0; i < this.changeListeners.length; i++ ) {
	this.changeListeners[ i ].stateChanged( e );
      }
    }
  }


  private String createHexString( int value )
  {
    for( int i = this.digitBuf.length - 1; i >= 0; --i ) {
      int v = value & 0x0F;
      this.digitBuf[ i ] = (char) (v < 10 ? (v + '0') : (v - 10 + 'A'));
      value >>= 4;
    }
    return new String( this.digitBuf );
  }


  private void updBackground()
  {
    if( this.editMode && this.dataChanged ) {
      this.textFld.setBackground( Color.ORANGE );
    } else {
      this.textFld.setBackground( this.marked ? Color.YELLOW : this.bgColor );
    }
  }
}
