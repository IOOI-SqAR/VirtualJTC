/*
 * (c) 2007-2010 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Fenster fuer Bildschirmtastatur
 */

package jtcemu.base;

import java.awt.*;
import java.awt.event.*;
import java.lang.*;
import java.util.*;
import javax.swing.*;
import jtcemu.*;


public class KeyboardFrm extends BaseFrm
                        implements
                                ActionListener,
                                KeyListener
{
  private static KeyboardFrm instance = null;

  private Map<Character,KeyFld> char2Key = new Hashtable<Character,KeyFld>();
  private Map<Integer,KeyFld>   code2Key = new Hashtable<Integer,KeyFld>();

  private KeyFld[] keys0 = {
        createKeyCodeFld(
                        GUIUtil.readImage( this, "/images/key/space.png" ),
                        "Leer",
                        null,
                        null,
                        null,
                        null,
                        KeyEvent.VK_SPACE ),
        createKeyCharFld( 'Y', null,   null, null ),
        createKeyCharFld( 'X', null,   null, null ),
        createKeyCharFld( 'C', "CALL", null, null ),
        createKeyCharFld( 'V', null,   null, null ),
        createKeyCharFld( 'B', null,   null, null ),
        createKeyCharFld( 'N', null,   null, null ),
        createKeyCharFld( 'M', "REM",  "]",  null ),
        createKeyCharFld( ',', "THEN", "<",  null ),
        createKeyCharFld( '.', null,   ">",  null ),
        createKeyCharFld( '/', "TOFF", "?",  null ),
        createKeyCodeFld(
                        null,
                        "Enter",
                        null,
                        null,
                        null,
                        "OFF",
                        KeyEvent.VK_ENTER ) };

  private KeyFld[] keys1 = {
        createKeyCodeFld(
                        GUIUtil.readImage( this, "/images/key/shift.png" ),
                        "Shift",
                        null,
                        null,
                        null,
                        null,
                        KeyEvent.VK_CONTROL ),
        createKeyCharFld( 'A', null,    null, null ),
        createKeyCharFld( 'S', "GOSUB", null, null ),
        createKeyCharFld( 'D', null,    null, null ),
        createKeyCharFld( 'F', "IF",    null, null ),
        createKeyCharFld( 'G', "GOTO",  null, null ),
        createKeyCharFld( 'H', "PTH",   null, null ),
        createKeyCharFld( 'J', null,    null, null ),
        createKeyCharFld( 'K', null,    "[",  null ),
        createKeyCharFld( 'L', "LET",   null, null ),
        createKeyCharFld( '*', null,    ":",  null ),
        createKeyCharFld( '-', "LIST",  null,  null ) };

  private KeyFld[] keys2 = {
        new KeyFld( this, null, null, null, null, null, null ),
        createKeyCharFld( 'Q', null,     null, null ),
        createKeyCharFld( 'W', "WAIT",   null, null ),
        createKeyCharFld( 'E', "END",    null, null ),
        createKeyCharFld( 'R', "RETURN", null, null ),
        createKeyCharFld( 'T', "STOP",   null, null ),
        createKeyCharFld( 'Z', null,     null, null ),
        createKeyCharFld( 'U', null,     null, null ),
        createKeyCharFld( 'I', "INPUT",  null, null ),
        createKeyCharFld( 'O', "PROC",   null, "LOAD" ),
        createKeyCharFld( 'P', "PRINT",  null, "SAVE" ),
        createKeyCharFld( '+', "RUN",    ";",  null ) };

  private KeyFld[] keys3 = {
        createKeyCodeFld(
                        GUIUtil.readImage( this, "/images/key/down.png" ),
                        "Runter",
                        null,
                        GUIUtil.readImage( this, "/images/key/up.png" ),
                        "Hoch",
                        null,
                        KeyEvent.VK_DOWN,
                        KeyEvent.VK_UP ),
        createKeyCharFld( '1', "TRAP", "!",  null ),
        createKeyCharFld( '2', null,   "\"", null ),
        createKeyCharFld( '3', null,   "#",  null ),
        createKeyCharFld( '4', null,   "$",  null ),
        createKeyCharFld( '5', null,   "%",  null ),
        createKeyCharFld( '6', null,   "&",  null ),
        createKeyCharFld( '7', null,   "\'", null ),
        createKeyCharFld( '8', null,   "(",  null ),
        createKeyCharFld( '9', null,   ")",  null ),
        createKeyCharFld( '0', null,   null, null ),
        createKeyCodeFld(
                        GUIUtil.readImage( this, "/images/key/left.png" ),
                        "Backspace",
                        "NEW",
                        null,
                        null,
                        null,
                        KeyEvent.VK_BACK_SPACE ) };

  private TopFrm            topFrm;
  private JTCSys            jtcSys;
  private boolean           ignoreKeyChar;
  private boolean           keepKeysPressed;
  private JMenuItem         mnuReset;
  private JMenuItem         mnuClose;
  private JCheckBoxMenuItem mnuKeepKeysPressed;
  private JPanel            panelKey;


  public static void close()
  {
    if( instance != null )
      instance.doClose();
  }


  public boolean getKeepKeysPressed()
  {
    return this.keepKeysPressed;
  }


  public void keyStatusChanged( KeyFld keyFld, boolean state )
  {
    if( state || this.keepKeysPressed ) {
      if( !checkMatrixKey( this.keys0, keyFld, state, 1 ) ) {
        if( !checkMatrixKey( this.keys1, keyFld, state, 2 ) ) {
          if( !checkMatrixKey( this.keys2, keyFld, state, 4 ) )
            checkMatrixKey( this.keys3, keyFld, state, 8 );
        }
      }
    } else {
      this.jtcSys.resetKeyMatrixStatus();
    }
  }


  public static void open( TopFrm topFrm, JTCSys jtcSys )
  {
    if( instance != null ) {
      instance.setState( Frame.NORMAL );
      instance.toFront();
    } else {
      instance = new KeyboardFrm( topFrm, jtcSys );
      instance.setVisible( true );
    }
  }


  public static void reset()
  {
    if( instance != null )
      instance.doClear();
  }


        /* --- ActionListener --- */

  @Override
  public void actionPerformed( ActionEvent e )
  {
    Object src = e.getSource();
    if( src != null ) {
      if( src == this.mnuReset ) {
        this.topFrm.doReset();
      }
      else if( src == this.mnuClose ) {
        doClose();
      }
      else if( src == this.mnuKeepKeysPressed ) {
        this.keepKeysPressed = this.mnuKeepKeysPressed.isSelected();
        if( !this.keepKeysPressed ) {
          doClear();
        }
        Main.setProperty(
                "jtcemu.keyboard_window.keep_keys_pressed",
                Boolean.toString( this.keepKeysPressed ) );
      }
    }
  }


        /* --- KeyListener --- */

  @Override
  public void keyPressed( KeyEvent e )
  {
    KeyFld keyFld = this.code2Key.get( new Integer( e.getKeyCode() ) );
    if( keyFld != null ) {
      keyFld.setSelected( true );
      this.ignoreKeyChar = true;
    }
  }


  @Override
  public void keyReleased( KeyEvent e )
  {
    doClear();
  }


  @Override
  public void keyTyped( KeyEvent e )
  {
    if( !this.ignoreKeyChar ) {
      KeyFld keyFld = this.char2Key.get( new Character( e.getKeyChar() ) );
      if( keyFld != null ) {
        keyFld.setSelected( true );
      }
    } else {
      this.ignoreKeyChar = false;
    }
    e.consume();
  }


        /* --- ueberschriebene Methoden --- */

  @Override
  protected boolean doClose()
  {
    boolean rv = super.doClose();
    if( rv ) {
      instance = null;
    }
    return rv;
  }


  @Override
  public void lafChanged()
  {
    pack();
  }


  @Override
  public void settingsChanged()
  {
    this.updKeys();
    this.keys1[ 11 ].repaint();
    this.keys2[ 0 ].repaint();
  }


        /* --- private Konstruktoren und Methoden --- */

  private KeyboardFrm( TopFrm topFrm, JTCSys jtcSys )
  {
    setTitle( "JTCEMU Tastatur" );
    this.topFrm          = topFrm;
    this.jtcSys          = jtcSys;
    this.ignoreKeyChar   = false;
    this.keepKeysPressed = Main.getBooleanProperty(
                                "jtcemu.keyboard_window.keep_keys_pressed",
                                false );


    // Menu
    JMenuBar mnuBar = new JMenuBar();
    setJMenuBar( mnuBar );


    // Menu Datei
    JMenu mnuFile = new JMenu( "Datei" );
    mnuFile.setMnemonic( 'D' );
    mnuBar.add( mnuFile );

    this.mnuReset = new JMenuItem( "Zur\u00FCcksetzen (RESET)" );
    this.mnuReset.setAccelerator(
                        KeyStroke.getKeyStroke( KeyEvent.VK_F7, 0 ) );
    this.mnuReset.addActionListener( this );
    mnuFile.add( this.mnuReset );
    mnuFile.addSeparator();

    this.mnuClose = new JMenuItem( "Schlie\u00DFen" );
    this.mnuClose.addActionListener( this );
    mnuFile.add( this.mnuClose );


    // Menu Einstellungen
    JMenu mnuSettings = new JMenu( "Einstellungen" );
    mnuSettings.setMnemonic( 'E' );
    mnuBar.add( mnuSettings );

    this.mnuKeepKeysPressed = new JCheckBoxMenuItem(
                                "Tasten gedr\u00FCckt halten",
                                this.keepKeysPressed );
    this.mnuKeepKeysPressed.addActionListener( this );
    mnuSettings.add( this.mnuKeepKeysPressed );


    // Fensterinhalt
    setLayout( new BorderLayout() );

    this.panelKey = new JPanel( new GridLayout( 4, 12, 5, 5 ) );
    this.panelKey.setBorder( BorderFactory.createEmptyBorder( 5, 5, 5, 5 ) );
    this.panelKey.setFocusable( false );
    add( this.panelKey, BorderLayout.CENTER );
    addKeys( this.keys3 );
    addKeys( this.keys2 );
    addKeys( this.keys1 );
    addKeys( this.keys0 );
    updKeys();


    // Fenstergroesse und -position
    pack();
    setFocusable( true );
    setResizable( false );
    if( !GUIUtil.applyWindowSettings( this ) ) {
      setLocationByPlatform( true );
    }
    addKeyListener( this );
  }


  private void addKeys( KeyFld[] fields )
  {
    for( int i = 0; i < fields.length; i++ )
      this.panelKey.add( fields[ i ] );
  }


  private boolean checkMatrixKey(
                        KeyFld[] keys,
                        KeyFld   pressedKey,
                        boolean  state,
                        int      value )
  {
    boolean rv = false;
    for( int i = 0; i < keys.length; i++ ) {
      if( keys[ i ] == pressedKey ) {
        if( state ) {
          this.jtcSys.setKeyMatrixColBits( i + 1, value );
        } else {
          this.jtcSys.resetKeyMatrixColBits( i + 1, value );
        }
        rv = true;
        break;
      }
    }
    return rv;
  }


  private KeyFld createKeyCharFld(
                        char    baseChar,
                        String  cmdText,
                        String  shiftText,
                        String  shiftCmdText )
  {
    KeyFld keyFld = createKeyFld(
                                null,
                                Character.toString( baseChar ),
                                cmdText,
                                null,
                                shiftText,
                                shiftCmdText );
    this.char2Key.put( new Character( baseChar ), keyFld );
    if( (baseChar >= 'A') && (baseChar <= 'Z') ) {
      this.char2Key.put(
                new Character( Character.toLowerCase( baseChar ) ),
                keyFld );
    }
    if( shiftText != null ) {
      if( shiftText.length() > 0 )
        this.char2Key.put( new Character( shiftText.charAt( 0 ) ), keyFld );
    }
    return keyFld;
  }


  private KeyFld createKeyCodeFld(
                        Image  baseImage,
                        String baseText,
                        String cmdText,
                        Image  shiftImage,
                        String shiftText,
                        String shiftCmdText,
                        int... keyCodes )
  {
    KeyFld keyFld = createKeyFld(
                        baseImage,
                        baseText,
                        cmdText,
                        shiftImage,
                        shiftText,
                        shiftCmdText );
    if( keyCodes != null ) {
      for( int i = 0; i < keyCodes.length; i++ )
        this.code2Key.put( new Integer( keyCodes[ i ] ), keyFld );
    }
    return keyFld;
  }


  private KeyFld createKeyFld(
                        Image  baseImage,
                        String baseText,
                        String cmdText,
                        Image  shiftImage,
                        String shiftText,
                        String shiftCmdText )
  {
    return new KeyFld(
                this,
                baseImage,
                baseText,
                cmdText,
                shiftImage,
                shiftText,
                shiftCmdText );
  }


  private void doClear()
  {
    setKeysUnselected( this.keys0 );
    setKeysUnselected( this.keys1 );
    setKeysUnselected( this.keys2 );
    setKeysUnselected( this.keys3 );
    this.ignoreKeyChar = false;
  }


  private void setKeysUnselected( KeyFld[] keys )
  {
    for( int i = 0; i < keys.length; i++ )
      keys[ i ].setSelected( false );
  }


  private void updKeys()
  {
    if( this.jtcSys.getOSType() == JTCSys.OSType.OS2K ) {
      this.keys1[ 11 ].setValues( null, "-", "LIST", null, "=", null );
      this.keys2[ 0 ].setValues(
                GUIUtil.readImage( this, "/images/key/left.png" ),
                "Links",
                null,
                GUIUtil.readImage( this, "/images/key/right.png" ),
                "Rechts",
                null );
      this.char2Key.put( new Character( '=' ), this.keys1[ 11 ] );
      this.code2Key.put( new Integer( KeyEvent.VK_LEFT ), this.keys2[ 0 ] );
      this.code2Key.put( new Integer( KeyEvent.VK_RIGHT ), this.keys2[ 0 ] );
    } else {
      this.keys1[ 11 ].setValues( null, "-", "LIST", null, null, null );
      this.keys2[ 0 ].setValues( null, "=", null, null, null, null );
      this.char2Key.put( new Character( '=' ), this.keys2[ 0 ] );
      this.code2Key.remove( new Integer( KeyEvent.VK_LEFT ) );
      this.code2Key.remove( new Integer( KeyEvent.VK_RIGHT ) );
    }
  }
}
