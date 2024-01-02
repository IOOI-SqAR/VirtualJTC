/*
 * (c) 2019 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Inhalt des Hilfe-Tabs
 */

package org.jens_mueller.jtcemu.platform.fx.help;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker.State;
import javafx.event.Event;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebHistory;
import javafx.scene.web.WebView;
import org.jens_mueller.jtcemu.base.AppContext;
import org.jens_mueller.jtcemu.base.JTCUtil;
import org.jens_mueller.jtcemu.platform.fx.JTCEMUApplication;
import org.jens_mueller.jtcemu.platform.fx.base.AppTab;
import org.jens_mueller.jtcemu.platform.fx.base.GUIUtil;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;


public class HelpNode extends BorderPane implements AppTab
{
  private static final String ACTION_BACK   = "Zur\u00FCck";
  private static final String ACTION_HOME   = "Startseite";
  private static final String HOME_RESOURCE = "/help/fx/home.htm";
  private static final String PROP_ZOOM     = "help.zoom";

  private static HelpNode instance = null;

  private final JTCEMUApplication JTCEMUApplication;
  private String   baseUrl;
  private String   homeUrl;
  private final MenuBar  mnuBar;
  private final MenuItem mnuBack;
  private final MenuItem mnuHome;
  private final Button   btnBack;
  private final Button   btnHome;
  private final Slider   sliderZoom;
  private final WebView  webView;


  public static void showTab( JTCEMUApplication JTCEMUApplication) throws IOException
  {
    if( instance == null ) {
      instance = new HelpNode(JTCEMUApplication);
    }
    JTCEMUApplication.showTab( "Hilfe", instance, true );
  }


	/* --- AppTab --- */

  @Override
  public MenuBar getMenuBar()
  {
    return this.mnuBar;
  }


  @Override
  public String getTitle()
  {
    return AppContext.getAppName();
  }


  @Override
  public void setTab( Tab tab )
  {
    // leer
  }


  @Override
  public void tabClosed()
  {
    instance = null;
  }


  @Override
  public boolean tabCloseRequest( Event e )
  {
    return true;
  }


  @Override
  public void tabSelected()
  {
    // leer
  }


	/* --- Konstruktor --- */

  private HelpNode( JTCEMUApplication JTCEMUApplication) throws IOException
  {
    this.JTCEMUApplication = JTCEMUApplication;
    this.baseUrl = null;
    this.homeUrl = null;


    // Menu
    MenuItem mnuCopyPage = new MenuItem(
			"Hilfeseite ohne Hypertext-Links kopieren" );
    mnuCopyPage.setOnAction( e->doCopyPage( true ) );

    MenuItem mnuCopyPageWithLinks = new MenuItem(
			"Hilfeseite mit Hypertext-Links kopieren" );
    mnuCopyPageWithLinks.setOnAction( e->doCopyPage( false ) );

    Menu mnuFile = new Menu( "Datei" );
    mnuFile.getItems().addAll( mnuCopyPage, mnuCopyPageWithLinks );

    this.mnuBack = GUIUtil.createShortcutMenuItem(
						ACTION_BACK,
						"B",
						false );
    this.mnuBack.setDisable( true );
    this.mnuBack.setOnAction( e->doBack() );

    this.mnuHome = GUIUtil.createShortcutMenuItem(
						ACTION_HOME,
						"H",
						false );
    this.mnuHome.setDisable( true );
    this.mnuHome.setOnAction( e->doHome() );

    Menu mnuNav = new Menu( "Navigation" );
    mnuNav.getItems().addAll( this.mnuBack, this.mnuHome );

    this.mnuBar = new MenuBar();
    this.mnuBar.getMenus().addAll( mnuFile, mnuNav );
    GUIUtil.completeMenuBar(JTCEMUApplication, this.mnuBar, true );


    // Werkzeugleiste
    this.btnBack = GUIUtil.createToolBarButton(
				"/images/nav/back.png",
				ACTION_BACK,
				e->doBack() );
    this.btnBack.setDisable( true );

    this.btnHome = GUIUtil.createToolBarButton(
				"/images/nav/home.png",
				ACTION_HOME,
				e->doHome() );
    this.btnHome.setDisable( true );

    this.sliderZoom = new Slider( 0.5, 1.5, 1.0 );

    double zoom = 1.0;
    String text = AppContext.getProperty( PROP_ZOOM );
    if( text != null ) {
      try {
	double v = Double.parseDouble( text );
	v = Math.max( v, this.sliderZoom.getMin() );
	v = Math.min( v, this.sliderZoom.getMax() );
	this.sliderZoom.setValue( v );
      }
      catch( NumberFormatException ignored) {}
    }

    Button btnDefaultZoom = new Button( "100%" );

    setTop(
	new ToolBar(
		this.btnBack,
		this.btnHome,
		new Separator(),
		new Label( "Gr\u00F6\u00DFe:" ),
		this.sliderZoom,
		btnDefaultZoom ) );


    // Fensterinhalt
    this.webView = new WebView();
    this.webView.setContextMenuEnabled( false );
    this.webView.setZoom( 2.0 );
    setCenter( this.webView );


    // Startseite laden
    URL url = getClass().getResource( HOME_RESOURCE );
    if( url != null ) {
      String urlText = url.toExternalForm();
      if( urlText.endsWith( HOME_RESOURCE ) ) {
	this.homeUrl = urlText;
	this.baseUrl = urlText.substring(
		0,
		urlText.length() - HOME_RESOURCE.length() );
      }
    }
    if( (this.baseUrl == null) || (this.homeUrl == null) ) {
      throw new IOException( "Startseite der Hilfe nicht gefunden" );
    }

    /*
     * Nachdem ein Dokument geladen wurde,
     * sollen Clicks auf Links abgefangen werden.
     * Dazu muss erst einmal das Ereignis abgefangen werden,
     * wenn das Dokument geladen wurde.
     */
    final WebEngine webEngine = this.webView.getEngine();
    webEngine.getLoadWorker().stateProperty().addListener(
		new ChangeListener<State>() {
			public void changed(
					ObservableValue ov,
					State           oldState,
					State           newState )
			{
			  if( newState == State.SUCCEEDED ) {
			    contentLoaded( webEngine );
			  }
			}
		} );

    webEngine.load( this.homeUrl );


    // Aktionen
    btnDefaultZoom.setOnAction( e->this.sliderZoom.setValue( 1.0 ) );
    this.sliderZoom.valueProperty().addListener( (ov,o,n)->updZoom() );
    updZoom();
  }


	/* --- Aktionen --- */

  /*
   * Da nach dem Umschalten der Menueleiste die alten Tastenkombinationen,
   * sofern sie durch die neue Menuleiste nicht ueberschrieben wurden,
   * weiterhin aktiv sind, wird vor jeder Aktion geprueft,
   * ob die eigene Menueleiste aktiv ist.
   */
  private void doBack()
  {
    if( this.JTCEMUApplication.isCurMenuBar( this.mnuBar ) ) {
      WebHistory history = this.webView.getEngine().getHistory();
      if( history != null ) {
	try {
	  if( history.getCurrentIndex() > 0 ) {
	    history.go( -1 );
	  }
	}
	catch( IndexOutOfBoundsException ignored) {}
      }
      updNavButtons();
    }
  }


  private void doCopyPage( boolean removeLinks )
  {
    if( this.JTCEMUApplication.isCurMenuBar( this.mnuBar ) ) {
      String urlText = this.webView.getEngine().getLocation();
      if( urlText != null ) {
	if( !urlText.isEmpty() ) {
	  try {
	    String text = JTCUtil.loadHtml(
					new URI( urlText ).toURL(),
					removeLinks );
	    if( text != null ) {
	      final ClipboardContent content = new ClipboardContent();
	      content.putHtml( text );
	      Clipboard.getSystemClipboard().setContent( content );
	    }
	  }
	  catch( IOException ex ) {
	    this.JTCEMUApplication.showError( "Die Seite konnte nicht nicht"
				+ " die Zwischenablage kopiert werden." );
	  } catch (URISyntaxException e) {
          throw new RuntimeException(e);
      }
    }
      }
    }
  }


  private void doHome()
  {
    if( this.JTCEMUApplication.isCurMenuBar( this.mnuBar ) ) {
      boolean done = false;
      try {
	WebHistory history = this.webView.getEngine().getHistory();
	if( history != null ) {
	  int idx = history.getCurrentIndex();
	  if( idx > 0 ) {
	    history.go( -idx );
	    done = true;
	  }
	}
      }
      catch( IndexOutOfBoundsException ignored) {}
      if( !done ) {
	this.webView.getEngine().load( this.homeUrl );
      }
      this.mnuBack.setDisable( true );
      this.mnuHome.setDisable( true );
      this.btnBack.setDisable( true );
      this.btnHome.setDisable( true );
    }
  }


	/* --- private Methoden --- */

  /*
   * Die Methode haengt an alle a-Tags im Dokument einen Click-Listener an,
   * der dann das entsprechende Ziel laedt.
   */
  private void contentLoaded( final WebEngine webEngine )
  {
    org.w3c.dom.Document doc = webEngine.getDocument();
    if( doc != null ) {
      org.w3c.dom.NodeList aNodes = doc.getElementsByTagName( "a" );
      int                  n      = aNodes.getLength();
      for( int i = 0; i < n; i++ ) {
	org.w3c.dom.Node node = aNodes.item( i );
	if( node instanceof org.w3c.dom.events.EventTarget ) {
	  final String fixBaseUrl = this.baseUrl;
	  final String fixHomeUrl = this.homeUrl;
	  ((org.w3c.dom.events.EventTarget) node).addEventListener(
		"click",
		new org.w3c.dom.events.EventListener()
		{
		  /*
		   * Die Auswertung des Ziels und daraus folgend
		   * die Aufbereitung der neuen URL sind hier
		   * nicht allgemeingueltig implementiert.
		   * Es werden nur die Link-Typen unterstuetzt,
		   * die in den konkreten Hilfedateien auch vorkommen.
		   */
		  @Override
		  public void handleEvent( org.w3c.dom.events.Event e )
		  {
		    org.w3c.dom.events.EventTarget t = e.getCurrentTarget();
		    if( t != null ) {
		      String s = t.toString();
		      if( s != null ) {
			/*
			 * Der String enthaelt die URL des Click-Ziels.
			 * Wenn die href-Angabe im HTML-Dokument keine URL
			 * ist, sondern nur einen Dateinamen enthaelt,
			 * wird die URL von der WebEngine aufbereitet.
			 * Das funktioniert seltsamerweise aber nicht,
			 * wenn die aktuelle URL in eine JAR-Datei zeigt.
			 * In dem Fall enthaelt EventTarget nur
			 * die href-Angabe.
			 * Aus diesem Grund wird hier der Einfachheit halber
			 * durch Vorhandensein eines Doppelpunktes
			 * auf eine URL getestet.
			 * Ist es keine, wird die URL hier selbst
			 * aufbereitet.
			 * Ist das nicht moeglich,
			 * wird die Startseite angezeigt.
			 */
			if(!s.contains(":")) {
			  try {
			    if( s.startsWith( "/" ) ) {
			      /*
			       * Da die Hilfe-Dateien keine Links
			       * zu externen Seiten enthalten,
			       * muss die aktuelle URL hier nicht
			       * ausgewertet werden.
			       */
			      s = fixBaseUrl + s;
			    } else {
			      String curLoc = webEngine.getLocation();
			      if( curLoc == null ) {
				curLoc = "";
			      }
			      int pos = curLoc.lastIndexOf( "/" );
			      if( pos < 0 ) {
				throw new IllegalStateException();
			      }
			      curLoc = curLoc.substring( 0, pos );
			      while( s.startsWith( "../" ) ) {
				pos = curLoc.lastIndexOf( "/" );
				if( pos < 0 ) {
				  throw new IllegalStateException();
				}
				curLoc = curLoc.substring( 0, pos );
				s      = s.substring( 3 );
			      }
			      s = curLoc + "/" + s;
			    }
			  }
			  catch( IllegalStateException ex ) {
			    s = fixHomeUrl;
			  }
			}
			final String s1 = s;
			Platform.runLater( ()->webEngine.load( s1 ) );
		      }
		    }
		    e.preventDefault();
		  }
		},
		false );
	}
      }
    }
    updNavButtons();
  }


  private void updNavButtons()
  {
    boolean    disable = false;
    WebHistory history = this.webView.getEngine().getHistory();
    if( history != null ) {
      if( history.getCurrentIndex() < 1 ) {
	disable = true;
      }
    }
    this.mnuBack.setDisable( disable );
    this.mnuHome.setDisable( disable );
    this.btnBack.setDisable( disable );
    this.btnHome.setDisable( disable );
  }


  private void updZoom()
  {
    double v = this.sliderZoom.getValue();
    AppContext.setProperty( PROP_ZOOM, String.valueOf( v ) );
    this.webView.setZoom( v * v );
  }
}
