/*
 * (c) 2007-2010 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Wrapper-Klasse und Document fuer ein hexadezimales
 * Eingabefeld mit Erkennung manueller Aenderungen
 */

package jtcemu.tools;

import java.awt.*;
import java.awt.event.*;
import java.lang.*;
import java.util.Arrays;
import javax.swing.JTextField;
import javax.swing.event.*;
import javax.swing.text.*;


public class HexFld extends PlainDocument
			implements
				ActionListener,
				FocusListener
{
  private boolean          dataChanged;
  private boolean          editMode;
  private boolean          readOnly;
  private String           orgText;
  private int              orgValue;
  private int              value;
  private ChangeListener[] changeListeners;
  private char[]           digitBuf;
  private JTextField       textFld;
  private Color            bgColor;


  public HexFld( int maxDigits )
  {
    this.dataChanged     = false;
    this.editMode        = false;
    this.readOnly        = false;
    this.orgText         = "";
    this.orgValue        = 0;
    this.value           = 0;
    this.changeListeners = null;
    this.digitBuf        = new char[ maxDigits ];
    this.textFld         = new JTextField( this, this.orgText, maxDigits );
    this.bgColor         = this.textFld.getBackground();
    if( this.bgColor == null ) {
      this.bgColor = Color.white;
    }
    this.textFld.setEditable( false );
    this.textFld.addActionListener( this );
    this.textFld.addFocusListener( this );
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
    this.textFld.setText( "" );
    this.textFld.setEditable( false );
    this.textFld.setBackground( this.bgColor );
    this.editMode = false;
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


  public boolean isEditMode()
  {
    return this.editMode;
  }


  public boolean isReadOnly()
  {
    return this.readOnly;
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
    if( e.getSource() == this.textFld )
      checkText();
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
	if( ((ch >= 0) && (ch <= '9')) || ((ch >= 'A') && (ch <= 'F')) )
	  super.insertString( offs++, Character.toString( ch ), a );
      }
    }
  }


	/* --- private Methoden --- */

  private void checkText()
  {
    if( this.editMode ) {
      this.value  = -1;
      String text = this.textFld.getText();
      if( text != null ) {
	if( !text.isEmpty() ) {
	  try {
	    this.value = Integer.parseInt( text, 16 );
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
      this.textFld.setBackground( Color.yellow );
      this.textFld.setText( createHexString( this.value ) );
      this.dataChanged = true;
    } else {
      this.textFld.setBackground( this.bgColor );
      this.textFld.setText( this.orgText );
      this.dataChanged = false;
    }
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
}
