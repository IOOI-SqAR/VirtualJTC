/*
 * (c) 2014-2020 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Hilfsfunktionen fuer Oberflaechenprogrammierung
 */

package jtcemu.platform.fx.base;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.IndexRange;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Dragboard;
import javafx.scene.input.DragEvent;
import javafx.scene.input.KeyCharacterCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import jtcemu.base.AppContext;
import jtcemu.base.JTCUtil;
import jtcemu.platform.fx.Main;
import jtcemu.platform.fx.tools.ReassNode;
import jtcemu.platform.fx.tools.TextEditNode;


public class GUIUtil
{
  public static FileChooser createMemFileChooser( String title )
  {
    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle( title );
    fileChooser.getExtensionFilters().addAll(
	new FileChooser.ExtensionFilter( "Alle Dateien", "*" ),
	new FileChooser.ExtensionFilter(
				"Bin\u00E4rdateien (*.bin)",
				"*.bin" ),
	new FileChooser.ExtensionFilter( "HEX-Dateien (*.hex)", "*.hex" ),
	new FileChooser.ExtensionFilter( "JTC-Dateien (*.jtc)", "*.jtc" ),
	new FileChooser.ExtensionFilter( "TAP-Dateien (*.tap)", "*.tap" ) );
    return fileChooser;
  }


  public static Button createImageButton(
				String                    resource,
				String                    text,
				EventHandler<ActionEvent> onAction )
  {
    boolean     done = false;
    Button      btn  = new Button();
    InputStream in   = Main.class.getResourceAsStream( resource );
    if( in != null ) {
      Image image = new Image( in );
      if( !image.isError() ) {
	btn.setGraphic( new ImageView( image ) );
	btn.setTooltip( new Tooltip( text ) );
	done = true;
      }
      JTCUtil.closeSilently( in );
    }
    if( !done ) {
      btn.setText( text );
    }
    btn.setOnAction( onAction );
    return btn;
  }


  public static Button createToolBarButton(
				String                    resource,
				String                    text,
				EventHandler<ActionEvent> onAction )
  {
    Button btn = createImageButton( resource, text, onAction );
    btn.setFocusTraversable( false );
    btn.addEventHandler(
		MouseEvent.MOUSE_ENTERED,
		e->btn.setEffect( new DropShadow() ) );
    btn.addEventHandler(
		MouseEvent.MOUSE_EXITED,
		e->btn.setEffect( null ) );
    btn.setOnAction( onAction );
    return btn;
  }


  public static MenuItem createNonControlShortcutMenuItem(
						String  text,
						String  acceleratorChar,
						boolean shiftDown )
  {
    MenuItem item = new MenuItem( text );
    item.setAccelerator(
	new KeyCharacterCombination(
		acceleratorChar,
		shiftDown ? KeyCombination.ModifierValue.DOWN
				: KeyCombination.ModifierValue.UP,
		KeyCombination.ModifierValue.UP,
		JTCUtil.isMacOS() ? KeyCombination.ModifierValue.UP
				: KeyCombination.ModifierValue.DOWN,
		KeyCombination.ModifierValue.UP,
		JTCUtil.isMacOS() ? KeyCombination.ModifierValue.DOWN
				: KeyCombination.ModifierValue.UP ) );
    return item;
  }


  public static MenuItem createShortcutMenuItem(
						String  text,
						String  acceleratorChar,
						boolean shiftDown )
  {
    MenuItem item = new MenuItem( text );
    item.setAccelerator(
	new KeyCharacterCombination(
		acceleratorChar,
		shiftDown ? KeyCombination.ModifierValue.DOWN	// Shift
				: KeyCombination.ModifierValue.UP,
		KeyCombination.ModifierValue.UP,		// Control
		KeyCombination.ModifierValue.UP,		// Alt
		KeyCombination.ModifierValue.UP,		// Meta
		KeyCombination.ModifierValue.DOWN ) );		// Shortcut
    return item;
  }


  public static MenuBar completeMenuBar(
				final Main main,
				MenuBar    mnuBar,
				boolean    withCloseItem )
  {
    // Menue Werkzeuge
    MenuItem mnuTextEditor = createNonControlShortcutMenuItem(
						"Texteditor",
						"T",
						false );
    mnuTextEditor.setOnAction( e->TextEditNode.showTab( main ) );

    MenuItem mnuReassembler = createNonControlShortcutMenuItem(
						"Reassembler",
						"R",
						true );
    mnuReassembler.setOnAction( e->ReassNode.showTab( main ) );


    Menu mnuTools = new Menu( "Werkzeuge" );
    mnuTools.getItems().addAll( mnuTextEditor, mnuReassembler );


    // Menue Hilfe
    Menu mnuHelp = new Menu( "Hilfe" );

    MenuItem mnuHelpHome = new MenuItem(
				AppContext.getAppName() + "-Hilfe..." );
    mnuHelpHome.setOnAction( e->doHelp( main ) );

    MenuItem mnuAbout = new MenuItem(
		"\u00DCber " + AppContext.getAppName() + "..." );
    mnuAbout.setOnAction( e->AboutNode.showTab( main ) );

    mnuHelp.getItems().addAll(
			mnuHelpHome,
			new SeparatorMenuItem(),
			mnuAbout );


    // Menueleiste
    if( mnuBar == null ) {
      mnuBar = new MenuBar();
    }
    ObservableList<Menu> menus = mnuBar.getMenus();
    if( menus.isEmpty() ) {
      Menu mnuFile = new Menu( "Datei" );
      menus.add( mnuFile );
    }
    ObservableList<MenuItem> fileItems = menus.get( 0 ).getItems();
    if( withCloseItem ) {
      if( !fileItems.isEmpty() ) {
	fileItems.add( new SeparatorMenuItem() );
      }
      MenuItem mnuClose = new MenuItem( "Schlie\u00DFen" );
      mnuClose.setOnAction( e->main.doCloseTab( e ) );
      fileItems.add( mnuClose );
    }
    if( !fileItems.isEmpty() ) {
      fileItems.add( new SeparatorMenuItem() );
    }
    MenuItem mnuQuit = new MenuItem( "Beenden" );
    mnuQuit.setOnAction( e->main.doQuit() );
    fileItems.add( mnuQuit );
    menus.addAll( mnuTools, mnuHelp );

    return mnuBar;
  }


  public static void centerStageOnOwner( Stage s )
  {
    Window o = s.getOwner();
    if( o != null ) {
      s.setX( Math.max(
		0,
		o.getX() + ((o.getWidth() - s.getWidth()) / 2) ) );
      s.setY( Math.max(
		0,
		o.getY() + ((o.getHeight() - s.getHeight()) / 2) ) );
    }
  }


  public static FileChooser.ExtensionFilter createAsmFileFilter()
  {
    return new FileChooser.ExtensionFilter(
		"Assembler-Dateien (*.asm; *.s)", "*.asm", "*.s" );
  }


  public static FileChooser.ExtensionFilter createLstFileFilter()
  {
    return new FileChooser.ExtensionFilter(
		"Assembler-Listing-Dateien (*.lst)", "*.lst" );
  }


  public static FileChooser.ExtensionFilter createTextFileFilter()
  {
    return new FileChooser.ExtensionFilter(
			"Textdateien (*.asc; *.log; *.txt)",
			"*.asc", "*.log", "*.txt" );
  }


  public static void handleFileDragOver( DragEvent e )
  {
    Dragboard dragboard = e.getDragboard();
    if( dragboard != null ) {
      if( dragboard.hasFiles() ) {
	e.acceptTransferModes( TransferMode.COPY );
      }
    }
    e.consume();
  }


  public static void handleFileDragDropped(
				DragEvent       e,
				DropFileHandler handler )
  {
    boolean   success   = false;
    Dragboard dragboard = e.getDragboard();
    if( dragboard != null ) {
      java.util.List<File> files = dragboard.getFiles();
      if( files != null ) {
	if( !files.isEmpty() ) {
	  File file = files.get( 0 );
	  if( file != null ) {
	    success = handler.handleDroppedFile(
					e.getGestureTarget(),
					file );
	  }
	}
      }
    }
    e.setDropCompleted( success );
    e.consume();
  }


  public static boolean hasSelection( IndexRange range )
  {
    boolean rv = false;
    if( range != null ) {
      int e = range.getEnd();
      if( (range.getStart() < e) && (e > 0) ) {
	rv = true;
      }
    }
    return rv;
  }


  public static boolean hasSelection( TextInputControl tic )
  {
    return hasSelection( tic.getSelection() );
  }


  public static FileChooser prepareFileChooser(
					String title,
					File   presetFile,
					String fileGroup )
  {
    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle( title );
    if( presetFile == null ) {
      presetFile = AppContext.getLastDirFile( fileGroup );
    }
    if( presetFile != null ) {
      String fileName = null;
      if( !presetFile.isDirectory() ) {
	fileName   = presetFile.getName();
	presetFile = presetFile.getParentFile();
      }
      if( presetFile != null ) {
	fileChooser.setInitialDirectory( presetFile );
      }
      if( fileName != null ) {
	fileChooser.setInitialFileName( fileName );
      }
    }
    fileChooser.getExtensionFilters().add(
	new FileChooser.ExtensionFilter( "Alle Dateien", "*" ) );
    return fileChooser;
  }


	/* --- private Methoden --- */

  private static void doHelp( final Main main )
  {
    /*
     * Das Hilfesystem wird dynamisch geladen,
     * da auf manchen Plattformen das JavaFX-WebView
     * nicht unterstuetzt wird.
     * Duch das dynamische Laden wird so ein Fall abgefangen
     * und es erscheint eine entsprechende Fehlermeldung.
     */
    try {
      Class.forName( "jtcemu.platform.fx.help.HelpNode" )
		.getMethod( "showTab", Main.class )
		.invoke( null, main );
    }
    catch( Exception | LinkageError e ) {
      Throwable t = e;
      String    s = "Die Hilfe steht auf der hier verwendeten\n"
			+ "Java-Plattform nicht zur Verf\u00FCgung.";
      if( e instanceof InvocationTargetException ) {
	t = e.getCause();
      }
      if( t != null ) {
	if( t instanceof IOException ) {
	  s = t.getMessage();
	}
      }
      main.showError( s );
    }
  }
}
