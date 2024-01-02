/*
 * (c) 2014-2021 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Programmstart fuer JavaFX
 */

package org.jens_mueller.jtcemu.platform.fx;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.MenuBar;
import javafx.scene.control.SelectionModel;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.jens_mueller.jtcemu.base.AppContext;
import org.jens_mueller.jtcemu.base.ErrorViewer;
import org.jens_mueller.jtcemu.base.JTCSys;
import org.jens_mueller.jtcemu.base.JTCUtil;
import org.jens_mueller.jtcemu.platform.fx.base.*;
import org.jens_mueller.jtcemu.platform.fx.settings.SettingsNode;
import org.jens_mueller.jtcemu.platform.fx.tools.ReassNode;
import org.jens_mueller.z8.Z8;
import org.jens_mueller.z8.Z8Listener;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;


public class JTCEMUApplication extends Application implements ErrorViewer, Z8Listener
{
  public static final String PROP_WINDOW_X = "window.x";
  public static final String PROP_WINDOW_Y = "window.y";

  private static final String APP_NAME    = "JTCemuFX";
  private static final String APP_VERSION = "1.1";

  private static final String pathsFileName = "jtcemufx_paths.xml";
  private static final String propsFileName = "jtcemufx_config.xml";

  private static final String[] windowIcons = {
					"/images/window/jtcemu_20x20.png",
					"/images/window/jtcemu_32x32.png",
					"/images/window/jtcemu_48x48.png" };

  private static File pathsFile        = null;
  private static File propsFile        = null;
  private static java.util.Timer timer = null;

  private Stage           stage          = null;
  private Scene           scene          = null;
  private BorderPane      borderPane     = null;
  private MenuBar         defaultMenuBar = null;
  private TabPane         tabPane        = null;
  private Map<String,Tab> tabMap         = null;
  private JTCNode jtcNode        = null;
  private JTCSys jtcSys         = null;
  private Thread          emuThread      = null;


  public static void main( String[] args )
  {
    launch();
  }


  public static void addIconsTo( Stage stage )
  {
    for( String icon : windowIcons ) {
      URL url = JTCEMUApplication.class.getResource( icon );
      if( url != null ) {
	Image image = new Image( url.toString() );
	if( !image.isError() ) {
	  stage.getIcons().add( image );
	}
      }
    }
  }


  public void doCloseTab( Event e )
  {
    Tab tab = this.tabPane.getSelectionModel().getSelectedItem();
    if( tab != null ) {
      boolean state = tab.isClosable();
      if( state ) {
	Node content = tab.getContent();
	if( content != null ) {
	  if( content instanceof AppTab ) {
	    state = ((AppTab) content).tabCloseRequest( e );
	  }
	}
      }
      if( state ) {
	this.tabPane.getTabs().remove( tab );
	tabClosed( tab );
      }
    }
  }


  public boolean doQuit()
  {
    boolean state = true;
    if( AppContext.getBooleanProperty(
				JTCUtil.PROP_CONFIRM_QUIT,
				true ) )
    {
      if( !MsgDlg.showYesNoDlg(
		this.stage,
		"M\u00F6chten Sie " + AppContext.getAppName() + " beenden?",
		"Best\u00E4tigung" ) )
      {
	state = false;
      }
    }
    while( state ) {
      Tab                 tab  = null;
      java.util.List<Tab> tabs = this.tabPane.getTabs();
      synchronized( this ) {
	for( Tab t : tabs ) {
	  if( t.isClosable() ) {
	    tab = t;
	  }
	}
      }
      if( tab == null ) {
	break;
      }
      EventHandler<Event> handler = tab.getOnCloseRequest();
      if( handler != null ) {
	Event e = new Event( Tab.TAB_CLOSE_REQUEST_EVENT );
        handler.handle( e );
	if( e.isConsumed() ) {
	  state = false;
	  break;
	}
      }
      tabs.remove( tab );
    }
    if( state ) {
      Platform.exit();
    }
    return state;
  }


  public JTCSys getJTCSys()
  {
    return this.jtcSys;
  }


  public static File getPathsFile()
  {
    return pathsFile;
  }


  public static File getPropertiesFile()
  {
    return propsFile;
  }


  public ScreenNode getScreen()
  {
    return this.jtcNode != null ? this.jtcNode.getScreenNode() : null;
  }


  public Stage getStage()
  {
    return this.stage;
  }


  public static java.util.Timer getTimer()
  {
    synchronized( JTCEMUApplication.class ) {
      if( timer == null ) {
	timer = new java.util.Timer( "Timer", false );
      }
    }
    return timer;
  }


  public boolean isCurMenuBar( MenuBar mnuBar )
  {
    return (this.borderPane.getTop() == mnuBar);
  }


  public boolean isTabContentSelected( Node content )
  {
    boolean rv = false;
    if( content != null ) {
      Tab tab = this.tabPane.getSelectionModel().getSelectedItem();
      if( tab != null ) {
	if( tab.getContent() == content ) {
	  rv = true;
	}
      }
    }
    return rv;
  }


  public void settingsChanged()
  {
    if( this.jtcNode != null ) {
      this.jtcNode.settingsChanged();
    }
  }


  public void showError( Exception ex )
  {
    MsgDlg.showErrorMsg( this.stage, ex );
  }


  public Tab showTab(
		final String text,
		final Node   content,
		boolean      closable )
  {
    Tab tab = null;
    if( this.tabPane != null ) {
      if( this.tabMap == null ) {
	this.tabMap = new HashMap<String,Tab>();
      }
      tab = this.tabMap.get( text );
      if( tab == null ) {
	tab = new Tab( text );
	tab.setClosable( closable );
	tab.setContent( content );
	if( content instanceof AppTab) {
	  ((AppTab) content).setTab( tab );
	  tab.setOnCloseRequest(
			e->((AppTab) content).tabCloseRequest( e ) );
	}
	final Tab t = tab;
	tab.setOnClosed( e->tabClosed( t ) );
	tab.setOnSelectionChanged( e->tabSelectionChanged() );
	this.tabMap.put( text, tab );
	this.tabPane.getTabs().add( tab );
      }
      selectTab( tab );
    }
    return tab;
  }


  public void selectTab( final Tab tab )
  {
    if( (tab != null) && (this.tabPane != null) ) {
      final SelectionModel<Tab> sm = this.tabPane.getSelectionModel();
      if( sm != null ) {
	sm.select( tab );
      }
    }
  }


  public void updWindowTitle()
  {
    String  title  = null;
    Tab     tab    = this.tabPane.getSelectionModel().getSelectedItem();
    if( tab != null ) {
      Node content = tab.getContent();
      if( content != null ) {
	if( content instanceof AppTab ) {
	  title = ((AppTab) content).getTitle();
	}
      }
    }
    this.stage.setTitle( title != null ? title : APP_NAME );
  }


	/* --- ErrorViewer --- */

  @Override
  public void showError( final String msg )
  {
    Platform.runLater( ()->showErrorInternal( msg ) );
  }


	/* --- Z8Listener --- */

  @Override
  public void z8Update(Z8 z8, Reason reason )
  {
    switch( reason ) {
      case POWER_ON:
      case RESET:
	Platform.runLater( ()-> ReassNode.reset() );
	break;
    }
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void init()
  {
    AppContext.setAppInfo( APP_NAME, APP_VERSION );
    AppContext.setPropPrefix( "jtcemu." );
  }


  @Override
  public void start( Stage stage )
  {
    this.stage = stage;
    this.stage.setOnCloseRequest( e->handleStageCloseRequest( e ) );
    Platform.setImplicitExit( true );

    // Icons
    addIconsTo( stage );

    // Fensterinhalt
    this.borderPane = new BorderPane();
    this.tabPane    = new TabPane();
    this.tabPane.setSide( Side.TOP );
    this.tabPane.setTabClosingPolicy( TabPane.TabClosingPolicy.ALL_TABS );
    this.tabPane.setCenterShape( true );
    this.borderPane.setCenter( this.tabPane );

    // Standard-Menue
    this.defaultMenuBar = GUIUtil.completeMenuBar( this, null, true );

    // Emulator-Tab
    this.jtcNode = new JTCNode( this );
    Tab jtcTab   = showTab( "JU+TE-Computer", this.jtcNode, false );

    // Konfigurationsdatei und Datei fuer letzte Zugriffspfade
    boolean propsLoaded = false;
    try {
      String appData = System.getenv( "APPDATA" );
      if( appData != null ) {
	if( !appData.isEmpty() ) {
	  pathsFile = new File( appData, pathsFileName );
	  propsFile = new File( appData, propsFileName );
	}
      }
      String homeDir = System.getProperty( "user.home" );
      if( homeDir != null ) {
	if( !homeDir.isEmpty() ) {
	  if( pathsFile == null ) {
	    if( File.separatorChar == '/' ) {
	      pathsFile = new File( homeDir, "." + pathsFileName );
	    } else {
	      pathsFile = new File( homeDir, pathsFileName );
	    }
	  }
	  if( propsFile == null ) {
	    if( File.separatorChar == '/' ) {
	      propsFile = new File( homeDir, "." + propsFileName );
	    } else {
	      propsFile = new File( homeDir, propsFileName );
	    }
	  }
	}
      }
    }
    catch( SecurityException ex ) {}
    if( pathsFile == null ) {
      pathsFile = new File( pathsFileName );
    }
    AppContext.setLastPathsFile( pathsFile );
    if( propsFile == null ) {
      propsFile = new File( propsFileName );
    }
    if( propsFile.exists() ) {
      try {
	InputStream in = null;
	try {
	  in               = new FileInputStream( propsFile );
	  Properties props = new Properties();
	  props.loadFromXML( in );
	  AppContext.putProperties( props );
	  propsLoaded = true;
	}
	finally {
	  JTCUtil.closeSilently( in );
	}
      }
      catch( IOException ex ) {
	showError( ex );
      }
    }

    // Emulation aufsetzen
    try {

      // Emulation der JTC-Hardware aufsetzen
      this.jtcSys = new JTCSys();
      this.jtcSys.setScreen( this.jtcNode.getScreenNode() );
      this.jtcNode.setJTCSys( this.jtcSys );

      // Fehlermeldungen
      String errText = this.jtcSys.getErrorText();
      if( errText != null ) {
	showError( errText );
      }
      this.jtcSys.setErrorViewer( this );

      /*
       * Tastaturereignisse an Emulator weiterreichen,
       * auch wenn diese standardmaessig von der TabPane
       * selbst verarbeitet werden wuerden (z.B. Cursor-Tasten)
       */
      this.tabPane.setEventDispatcher(
		new JTCKeyEventExtractor( this.tabPane, this.jtcNode ) );

      // Emulation starten
      Z8 z8 = this.jtcSys.getZ8();
      this.emuThread = new Thread( z8, "Z8 emulation" );
      this.emuThread.start();

      /*
       * Wenn keine Einstellungen geladen wurden, koennte JTCemuFX
       * das erste mal auf dem Rechner gestartet worden sein.
       * Um in dem Fall die Bedienung fuer den Anwender etwas ersichtlicher
       * zu gestalten, wird der Einstellungs-Tab gleich mit angezeigt,
       * aber nicht ausgewaehlt.
       */
      if( !propsLoaded ) {
	SettingsNode.showTab( this );
        selectTab( jtcTab );
        tabSelectionChanged();
      }

      // Fenster anzeigen
      this.scene = new Scene( this.borderPane );
      stage.setResizable( true );
      stage.setScene( this.scene );
      double x = getDoubleProperty( PROP_WINDOW_X );
      double y = getDoubleProperty( PROP_WINDOW_Y );
      if( (x >= 0.0) && (y >= 0.0) ) {
	stage.setX( x );
	stage.setY( y );
	stage.sizeToScene();
      } else {
	stage.sizeToScene();
	stage.centerOnScreen();
      }
      stage.show();
      Platform.runLater( ()->updJTCLayout() );
      /*
       * bei Aenderung der Fenstergroesse
       * Skalierung der Bildschirmausgabe anpassen
       */
      stage.heightProperty().addListener( (ov,o,n)->updJTCLayout() );
      stage.widthProperty().addListener( (ov,o,n)->updJTCLayout() );
      stage.maximizedProperty().addListener( (ov,o,n)->updJTCLayout() );

      // ResetListener
      this.jtcSys.setResetListener( this );
    }
    catch( IOException ex ) {
      showError( ex );
    }
  }


  @Override
  public void stop()
  {
    if( this.timer != null ) {
      this.timer.cancel();
    }
    if( this.jtcSys != null ) {
      this.jtcSys.getZ8().fireQuit();
    }
    if( this.emuThread != null ) {
      try {
	this.emuThread.join( 500 );
      }
      catch( InterruptedException ex ) {}
    }
  }


	/* --- private Methoden --- */

  private static double getDoubleProperty( String keyword )
  {
    double rv = -1.0;
    String text = AppContext.getProperty( keyword );
    if( text != null ) {
      try {
	rv = Double.parseDouble( text );
      }
      catch( NumberFormatException ex ) {}
    }
    return rv;
  }


  private void handleStageCloseRequest( WindowEvent e )
  {
    if( !doQuit() ) {
      e.consume();
    }
  }


  private void showErrorInternal( String msg )
  {
    MsgDlg.showErrorMsg(
		this.stage,
		msg != null ? msg : "Unbekannter Fehler" );
  }


  private void tabClosed( Tab tab )
  {
    Node content = tab.getContent();
    if( content != null ) {
      if( content instanceof AppTab ) {
	((AppTab) content).tabClosed();
      }
    }
    if( this.tabMap != null ) {
      String text = tab.getText();
      if( text != null ) {
	this.tabMap.remove( text );
      }
    }
  }


  private void tabSelectionChanged()
  {
    MenuBar mnuBar = null;
    String  title  = null;
    Tab     tab    = this.tabPane.getSelectionModel().getSelectedItem();
    if( tab != null ) {
      Node content = tab.getContent();
      if( content != null ) {
	if( content instanceof AppTab ) {
	  mnuBar = ((AppTab) content).getMenuBar();
	  title  = ((AppTab) content).getTitle();
	  Platform.runLater( ()->((AppTab) content).tabSelected() );
	}
	content.requestFocus();
      }
    }
    if( mnuBar == null ) {
      mnuBar = this.defaultMenuBar;
    }
    this.borderPane.setTop( mnuBar );
    this.stage.setTitle( title != null ? title : APP_NAME );
  }


  private void updJTCLayout()
  {
    if( this.jtcNode != null ) {
      final JTCNode jtcNode = this.jtcNode;
      Platform.runLater( ()->jtcNode.updLayout() );
    }
  }
}
