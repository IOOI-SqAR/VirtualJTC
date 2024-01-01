/*
 * (c) 2007-2020 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Hilfsfunktionen fuer Oberflaechenprogrammierung
 */

package org.jens_mueller.jtcemu.platform.se.base;

import org.jens_mueller.jtcemu.base.AppContext;
import org.jens_mueller.jtcemu.base.JTCUtil;
import org.jens_mueller.jtcemu.base.UserInputException;
import org.jens_mueller.jtcemu.platform.se.Main;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.net.URL;
import java.util.Collection;
import java.util.Iterator;


public class GUIUtil
{
  public static final String PROP_WINDOW_X = "window.x";
  public static final String PROP_WINDOW_Y = "window.y";
  public static final String PROP_WINDOW_W = "window.width";
  public static final String PROP_WINDOW_H = "window.height";

  public static FileFilter asmFileFilter = new FileNameExtensionFilter(
					"Assembler-Dateien (*.asm; *.s)",
					"asm", "s" );

  public static FileFilter basicFileFilter = new FileNameExtensionFilter(
					"BASIC-Dateien (*.bas)",
					"bas" );

  public static FileFilter binaryFileFilter = new FileNameExtensionFilter(
					"Bin\u00E4rdateien (*.bin)",
					"bin" );

  public static FileFilter hexFileFilter = new FileNameExtensionFilter(
					"HEX-Dateien (*.hex)",
					"hex" );

  public static FileFilter prjFileFilter = new FileNameExtensionFilter(
					"Projektdateien (*.prj)",
					"prj" );

  public static FileFilter jtcFileFilter = new FileNameExtensionFilter(
					"JTC-Dateien (*.jtc)",
					"jtc" );

  public static FileFilter romFileFilter = new FileNameExtensionFilter(
					"ROM-Dateien (*.bin; *.rom)",
					"bin", "rom" );

  public static FileFilter tapFileFilter = new FileNameExtensionFilter(
					"TAP-Dateien (*.tap)",
					"tap" );

  public static FileFilter textFileFilter = new FileNameExtensionFilter(
					"Textdateien (*.asc, *.log, *.txt)",
					"asc", "log", "txt" );


  private static final Cursor defaultCursor = Cursor.getDefaultCursor();
  private static final Cursor waitCursor = new Cursor( Cursor.WAIT_CURSOR );

  private static Integer menuShortcutKeyMask = null;


  public static boolean applyWindowSettings( BaseFrm frm )
  {
    boolean rv     = false;
    String  prefix = frm.getPropPrefix();
    Integer xNew   = AppContext.getIntegerProperty( prefix + PROP_WINDOW_X );
    Integer yNew   = AppContext.getIntegerProperty( prefix + PROP_WINDOW_Y );
    if( (xNew != null) && (yNew != null) ) {
      Integer wNew = null;
      Integer hNew = null;
      if( frm.isResizable() ) {
	wNew = AppContext.getIntegerProperty( prefix + PROP_WINDOW_W );
	hNew = AppContext.getIntegerProperty( prefix + PROP_WINDOW_H );
      }

      /*
       * Eigenschaften nur anwenden, wenn das Fenster
       * auf einem Bildschirm mit mindestens 48x48 Pixel sichtbar ist
       */
      int x = xNew.intValue();
      int y = yNew.intValue();
      int w = frm.getWidth();
      int h = frm.getHeight();
      if( wNew != null ) {
	w = wNew.intValue();
      }
      if( hNew != null ) {
	h = hNew.intValue();
      }
      if( (w > 0) && (h > 0) ) {
	boolean   visible      = false;
	Rectangle windowBounds = new Rectangle( x, y, w, h );
	for( GraphicsDevice gd : GraphicsEnvironment
					.getLocalGraphicsEnvironment()
					.getScreenDevices() )
	{
	  for( GraphicsConfiguration gc : gd.getConfigurations() ) {
	    Rectangle deviceBounds = gc.getBounds();
	    if( deviceBounds != null ) {
	      Rectangle2D intersection = deviceBounds.createIntersection(
							windowBounds );
	      if( intersection != null ) {
		if( (intersection.getWidth() > 48.0)
		    && (intersection.getHeight() > 48.0) )
		{
		  visible = true;
		  break;
		}
	      }
	    }
	  }
	  if( visible ) {
	    break;
	  }
	}
	if( visible ) {
	  if( (wNew != null) && (hNew != null) ) {
	    frm.setBounds( x, y, w, h );
	  } else {
	    frm.setLocation( x, y );
	  }
	  rv = true;
	}
      }
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
	if( !text.isEmpty() ) {
	  try {
	    rv = Integer.valueOf( text );
	    if( (rv != null) && (minValue != null) ) {
	      if( rv.intValue() < minValue.intValue() ) {
		Main.showError( owner, "Zahl zu klein!" );
		rv = null;
	      }
	    }
	  }
	  catch( NumberFormatException ex ) {
	    Main.showError( owner, "Ung\u00FCltige Eingabe" );
	  }
	} else {
	  Main.showError( owner, "Eingabe erwartet" );
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
	  rv = JTCUtil.parseHex4( text, msg );
	}
	catch( UserInputException ex ) {
	  Main.showError( owner, ex );
	}
      } else {
	break;
      }
    }
    return rv;
  }


  public static boolean copyToClipboard( Component c, String text )
  {
    boolean done = false;
    if( text != null ) {
      if( !text.isEmpty() ) {
	try {
	  Clipboard clipboard = c.getToolkit().getSystemClipboard();
	  if( clipboard != null ) {
	    StringSelection ss = new StringSelection( text );
	    clipboard.setContents( ss, ss );
	    done = true;
	  }
	}
	catch( IllegalStateException ex ) {}
      }
    }
    return done;
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
		    if( o instanceof File ) {
		      file = (File) o;
		    } else {
		      String s = o.toString();
		      if( s != null ) {
			if( !s.isEmpty() ) {
			  file = new File( s );
			  break;
			}
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


  public static String getClipboardText( Component c )
  {
    String text = null;
    try {
      Clipboard clipboard = c.getToolkit().getSystemClipboard();
      if( clipboard != null ) {
	if( clipboard.isDataFlavorAvailable( DataFlavor.stringFlavor ) ) {
	  Object o = clipboard.getData( DataFlavor.stringFlavor );
	  if( o != null ) {
	    text = o.toString();
	  }
	}
      }
    }
    catch( Exception ex ) {}
    return text;
  }


  public static int getMenuShortcutKeyMask( Component c )
  {
    if( menuShortcutKeyMask == null ) {
      Toolkit tk = c.getToolkit();
      for( String methodName : new String[] {
					"getMenuShortcutKeyMaskEx",
					"getMenuShortcutKeyMask" } )
      {
	try {
	  Object v = tk.getClass().getMethod( methodName ).invoke( tk );
	  if( v != null ) {
	    if( v instanceof Number ) {
	      menuShortcutKeyMask = Integer.valueOf(
					((Number) v).intValue() );
	      break;
	    }
	  }
	}
	catch( Exception ex ) {}
      }
    }
    if( menuShortcutKeyMask == null ) {
      menuShortcutKeyMask = Integer.valueOf( InputEvent.CTRL_DOWN_MASK );
    }
    return menuShortcutKeyMask.intValue();
  }


  public static Font getMonospacedFont( Component c )
  {
    int style = Font.BOLD;
    int size  = 12;
    if( c != null ) {
      Font font = c.getFont();
      if( font != null ) {
	style = font.getStyle();
	size  = font.getSize();
      }
    }
    return new Font( Font.MONOSPACED, style, size );
  }


  public static Window getWindow( Component c )
  {
    while( c != null ) {
      if( c instanceof Window ) {
        return (Window) c;
      }
      c = c.getParent();
    }
    return null;
  }


  public static boolean hasSelection( JTextComponent textComp )
  {
    boolean rv = false;
    if( textComp != null ) {
      int e = textComp.getSelectionEnd();
      rv    = (textComp.getSelectionStart() < e) && (e > 0);
    }
    return rv;
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


  public static void memorizeWindowSettings( BaseFrm frm )
  {
    if( frm.isVisible() ) {
      String prefix = frm.getPropPrefix();
      AppContext.setProperty( prefix + PROP_WINDOW_X, frm.getX() );
      AppContext.setProperty( prefix + PROP_WINDOW_Y, frm.getY() );
      AppContext.setProperty( prefix + PROP_WINDOW_W, frm.getWidth() );
      AppContext.setProperty( prefix + PROP_WINDOW_H, frm.getHeight() );
    }
  }


  public static int parseHex4(
			JTextField fld,
			String     fldName ) throws UserInputException
  {
    int value = JTCUtil.parseHex4( fld.getText(), fldName );
    fld.setText( String.format( "%04X", value ) );
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


  public static void toFront( final Frame frame )
  {
    int state = frame.getExtendedState();
    if( (state & Frame.ICONIFIED) != 0 ) {
      frame.setExtendedState( state & ~Frame.ICONIFIED );
    }
    if( !frame.isShowing() ) {
      frame.setVisible( true );
    }
    EventQueue.invokeLater(
		new Runnable()
		{
		  @Override
		  public void run()
		  {
		    frame.toFront();
		  }
		} );
  }
}
