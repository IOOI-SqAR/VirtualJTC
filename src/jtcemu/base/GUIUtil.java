/*
 * (c) 2007-2010 Jens Mueller
 * (c) 2017 Lars Sonchocky-Helldorf
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Hilfsfunktionen fuer Oberflaechenprogrammierung
 */

package jtcemu.base;

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.ActionListener;
import java.awt.dnd.*;
import java.io.File;
import java.net.URL;
import java.text.ParseException;
import java.util.*;
import javax.swing.*;
import javax.swing.filechooser.*;
import jtcemu.Main;


public class GUIUtil
{
  private static Locale locale = Locale.getDefault();
  private static ResourceBundle guiUtilResourceBundle = ResourceBundle.getBundle("resources.GUIUtil", locale);

  public static FileFilter basicFileFilter = new FileNameExtensionFilter(
                                        guiUtilResourceBundle.getString("basicFileFilter.description"),
                                        "bas" );

  public static FileFilter binaryFileFilter = new FileNameExtensionFilter(
                                        guiUtilResourceBundle.getString("binaryFileFilter.description"),
                                        "bin" );

  public static FileFilter hexFileFilter = new FileNameExtensionFilter(
                                        guiUtilResourceBundle.getString("hexFileFilter.description"),
                                        "hex" );

  public static FileFilter jtcFileFilter = new FileNameExtensionFilter(
                                        guiUtilResourceBundle.getString("jtcFileFilter.description"),
                                        "jtc" );

  public static FileFilter romFileFilter = new FileNameExtensionFilter(
                                        guiUtilResourceBundle.getString("romFileFilter.description"),
                                        "bin", "rom" );

  public static FileFilter tapFileFilter = new FileNameExtensionFilter(
                                        guiUtilResourceBundle.getString("tapFileFilter.description"),
                                        "tap" );

  public static FileFilter textFileFilter = new FileNameExtensionFilter(
                                        guiUtilResourceBundle.getString("textFileFilter.description"),
                                        "asc", "log", "txt" );


  private static final Cursor defaultCursor = Cursor.getDefaultCursor();
  private static final Cursor waitCursor    = new Cursor( Cursor.WAIT_CURSOR );


  public static boolean applyWindowSettings( Window window )
  {
    boolean rv = false;
    String  p  = window.getClass().getName();
    int     x  = Main.getIntProperty( p + ".window.x", -1 );
    int     y  = Main.getIntProperty( p + ".window.y", -1 );
    if( (x >= 0) && (y >= 0) ) {
      int w = -1;
      int h = -1;
      if( window instanceof Dialog ) {
        if( ((Dialog) window).isResizable() ) {
          w = Main.getIntProperty( p + ".window.width", -1 );
          h = Main.getIntProperty( p + ".window.height", -1 );
        }
      }
      else if( window instanceof Frame ) {
        if( ((Frame) window).isResizable() ) {
          w = Main.getIntProperty( p + ".window.width", -1 );
          h = Main.getIntProperty( p + ".window.height", -1 );
        }
      }
      if( (w > 0) && (h > 0) ) {
        window.setBounds( x, y, w, h );
      } else {
        window.setLocation( x, y );
      }
      rv = true;
    }
    return rv;
  }


  public static Integer askDecimal(
                                Component owner,
                                String    msg,
                                Integer   preSelection,
                                Integer   minValue )
  {
    Integer rv = null;
    while( rv == null ) {
      String errMsg = null;
      String text   = null;
      if( preSelection != null ) {
        text = JOptionPane.showInputDialog(
                                owner,
                                msg + ":",
                                preSelection.toString() );
      } else {
        text = JOptionPane.showInputDialog( owner, msg + ":" );
      }
      if( text != null ) {
        if( text.length() > 0 ) {
          try {
            rv = Integer.valueOf( text );
            if( (rv != null) && (minValue != null) ) {
              if( rv.intValue() < minValue.intValue() ) {
                Main.showError( owner, guiUtilResourceBundle.getString("error.askDecimal.numberToSmall.message") );
                rv = null;
              }
            }
          }
          catch( NumberFormatException ex ) {
            Main.showError( owner, guiUtilResourceBundle.getString("error.askDecimal.numberFormatException.message") );
          }
        } else {
          Main.showError( owner, guiUtilResourceBundle.getString("error.askDecimal.inputToShort.message") );
        }
      } else {
        break;
      }
    }
    return rv;
  }


  public static Integer askHex4(
                                Component owner,
                                String    msg,
                                Integer   preSelection )
  {
    Integer rv = null;
    while( rv == null ) {
      String text = null;
      if( preSelection != null ) {
        text = JOptionPane.showInputDialog(
                                owner,
                                msg + ":",
                                String.format( "%04X", preSelection ) );
      } else {
        text = JOptionPane.showInputDialog( owner, msg + ":" );
      }
      if( text != null ) {
        text = text.trim();
        if( (text.length() > 1) && text.startsWith( "%" ) ) {
          text = text.substring( 1 );
        }
        try {
          rv = new Integer( parseHex4( text, msg ) );
        }
        catch( ParseException ex ) {
          Main.showError( owner, ex );
        }
      } else {
        break;
      }
    }
    return rv;
  }


  public static JButton createImageButton(
                                Component owner,
                                String    imgName,
                                String    text,
                                String    actionCmd )
  {
    JButton btn = null;
    Image   img = readImage( owner, imgName );
    if( img != null ) {
      btn = new JButton( new ImageIcon( img ) );
      btn.setToolTipText( text );
    } else {
      btn = new JButton( text );
    }
    btn.setFocusable( false );
    if( owner instanceof ActionListener ) {
      btn.addActionListener( (ActionListener) owner );
    }
    if( actionCmd != null ) {
      btn.setActionCommand( actionCmd );
    }
    return btn;
  }


  public static JButton createImageButton(
                                Component owner,
                                String    imgName,
                                String    text )
  {
    return createImageButton( owner, imgName, text, null );
  }


  public static File fileDrop( Component owner, DropTargetDropEvent e )
  {
    File file = null;
    if( isFileDrop( e ) ) {
      e.acceptDrop( DnDConstants.ACTION_COPY );    // Quelle nicht loeschen
      Transferable t = e.getTransferable();
      if( t != null ) {
        try {
          Object o = t.getTransferData( DataFlavor.javaFileListFlavor );
          if( o != null ) {
            if( o instanceof Collection ) {
              Iterator iter = ((Collection) o).iterator();
              if( iter != null ) {
                while( iter.hasNext() ) {
                  o = iter.next();
                  if( o != null ) {
                    File tmpFile = null;
                    if( o instanceof File ) {
                      String path = ((File) o).getPath();
                      if( path != null ) {
                        if( !path.isEmpty() ) {
                          tmpFile = (File) o;
                        }
                      }
                    }
                    else if( o instanceof String ) {
                      String s = (String) o;
                      if( !s.isEmpty() ) {
                        tmpFile = new File( s );
                      }
                    }
                    if( tmpFile != null ) {
                      if( file == null ) {
                        file = tmpFile;
                      } else {
                        Main.showError(
                                owner,
                                guiUtilResourceBundle.getString("error.fileDrop.moreThanOneFileDropped.message") );
                        file = null;
                        break;
                      }
                    }
                  }
                }
              }
            }
          }
        }
        catch( Exception ex ) {}
      }
      e.dropComplete( true );
    } else {
      e.rejectDrop();
    }
    return file;
  }


  public static boolean isFileDrop( DropTargetDragEvent e )
  {
    boolean rv = false;
    int action = e.getDropAction();
    if( (action == DnDConstants.ACTION_COPY)
        || (action == DnDConstants.ACTION_MOVE)
        || (action == DnDConstants.ACTION_COPY_OR_MOVE)
        || (action == DnDConstants.ACTION_LINK) )
    {
      rv = e.isDataFlavorSupported( DataFlavor.javaFileListFlavor );
    }
    return rv;
  }


  public static boolean isFileDrop( DropTargetDropEvent e )
  {
    boolean rv = false;
    int action = e.getDropAction();
    if( (action == DnDConstants.ACTION_COPY)
        || (action == DnDConstants.ACTION_MOVE)
        || (action == DnDConstants.ACTION_COPY_OR_MOVE)
        || (action == DnDConstants.ACTION_LINK) )
    {
      rv = e.isDataFlavorSupported( DataFlavor.javaFileListFlavor );
    }
    return rv;
  }


  public static int parseHex4(
                        JTextField fld,
                        String     fldName ) throws ParseException
  {
    int value = parseHex4( fld.getText(), fldName );
    fld.setText( String.format( "%04X", value ) );
    return value;
  }


  public static int parseHex4(
                        String text,
                        String fldName ) throws ParseException
  {
    int     value = 0;
    boolean done  = false;
    if( text != null ) {
      text    = text.trim();
      int len = text.length();
      for( int i = 0; i < len; i++ ) {
        char ch = Character.toUpperCase( text.charAt( i ) );
        if( (ch >= '0') && (ch <= '9') ) {
          value = (value << 4) | (ch - '0');
          done  = true;
        } else if( (ch >= 'A') && (ch <= 'F') ) {
          value = (value << 4) | (ch - 'A' + 10);
          done  = true;
        } else {
          throw new ParseException(
                        fldName +  guiUtilResourceBundle.getString("error.parseHex4.parseException.notHex.message"),
                        i );
        }
        if( (value & ~0xFFFF) != 0 ) {
          throw new ParseException( fldName + guiUtilResourceBundle.getString("error.parseHex4.parseException.overflow.message"), i );
        }
      }
    }
    if( !done ) {
      throw new ParseException( fldName + guiUtilResourceBundle.getString("error.parseHex4.parseException.empty.message"), 0 );
    }
    return value;
  }


  public static Image readImage( Component owner, String resource )
  {
    Image img = null;
    URL   url = GUIUtil.class.getResource( resource );
    if( url != null ) {
      img = owner.getToolkit().createImage( url );
    }
    return img;
  }


  public static void setWaitCursor( RootPaneContainer c, boolean state )
  {
    Component glassPane = c.getGlassPane();
    if( state ) {
      glassPane.setCursor( waitCursor );
      glassPane.setVisible( true );
    } else {
      glassPane.setCursor( defaultCursor );
      glassPane.setVisible( false );
    }
  }
}

