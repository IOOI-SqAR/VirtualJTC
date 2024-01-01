/*
 * (c) 2014-2020 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Node fuer die Einstellungen
 */

package org.jens_mueller.jtcemu.platform.fx.settings;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.control.Button;
import javafx.scene.control.Separator;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import org.jens_mueller.jtcemu.base.AppContext;
import org.jens_mueller.jtcemu.base.ExtROM;
import org.jens_mueller.jtcemu.base.JTCSys;
import org.jens_mueller.jtcemu.base.JTCUtil;
import org.jens_mueller.jtcemu.base.UserInputException;
import org.jens_mueller.jtcemu.platform.fx.Main;
import org.jens_mueller.jtcemu.platform.fx.base.ScreenNode;


public class SettingsNode extends BorderPane
{
  private static SettingsNode instance = null;

  private Main                main;
  private JTCSys              jtcSys;
  private TabPane             tabPane;
  private Tab                 systemTab;
  private Tab                 ramTab;
  private Tab                 romTab;
  private Tab                 confirmTab;
  private Tab                 etcTab;
  private SystemSettingsNode  systemNode;
  private RAMSettingsNode     ramNode;
  private ROMSettingsNode     romNode;
  private ConfirmSettingsNode confirmNode;
  private EtcSettingsNode     etcNode;
  private Button              btnApply;
  private Button              btnSave;


  public void osSelectionChanged( JTCSys.OSType osType )
  {
    setDataChanged();
    this.romNode.osSelectionChanged( osType );
  }


  public static void showTab( Main main )
  {
    boolean created = false;
    if( instance == null ) {
      instance = new SettingsNode( main );
      created  = true;
    }
    main.showTab( "Einstellungen", instance, true );
    if( created ) {
      instance.updSettings();
    }
  }


  public void setDataChanged()
  {
    if( this.btnApply != null ) {
      this.btnApply.setDisable( false );
    }
    if( this.btnSave != null ) {
      this.btnSave.setDisable( true );
    }
  }


  public void updSaveButtonEnabled()
  {
    if( (this.btnApply != null) && (this.btnSave != null) ) {
      this.btnSave.setDisable( !this.btnApply.isDisable() );
    }
  }


  public void updSettings()
  {
    this.systemNode.updSettings();
    this.ramNode.updSettings();
    this.romNode.updSettings();
    this.romNode.osSelectionChanged( this.systemNode.getSelectedOS() );
    this.confirmNode.updSettings();
    this.btnApply.setDisable( true );
    this.btnSave.setDisable( false );
  }


	/* --- Konstruktor --- */

  private SettingsNode( Main main )
  {
    this.main   = main;
    this.jtcSys = main.getJTCSys();

    this.tabPane = new TabPane();
    this.tabPane.setSide( Side.TOP );
    this.tabPane.setTabClosingPolicy( TabPane.TabClosingPolicy.UNAVAILABLE );
    setCenter( this.tabPane );

    this.systemNode = new SystemSettingsNode( this, this.jtcSys );
    this.systemTab  = new Tab( "System" );
    this.systemTab.setClosable( false );
    this.systemTab.setContent( this.systemNode );
    this.tabPane.getTabs().add( this.systemTab );

    this.ramNode = new RAMSettingsNode( this, this.jtcSys );
    this.ramTab  = new Tab( "RAM" );
    this.ramTab.setClosable( false );
    this.ramTab.setContent( this.ramNode );
    this.tabPane.getTabs().add( this.ramTab );

    this.romNode = new ROMSettingsNode( this, this.main );
    this.romTab  = new Tab( "ROM" );
    this.romTab.setClosable( false );
    this.romTab.setContent( this.romNode );
    this.tabPane.getTabs().add( this.romTab );

    this.confirmNode = new ConfirmSettingsNode( this );
    this.confirmTab  = new Tab( "Best\u00E4tigungen" );
    this.confirmTab.setClosable( false );
    this.confirmTab.setContent( this.confirmNode );
    this.tabPane.getTabs().add( this.confirmTab );

    this.etcNode = new EtcSettingsNode( this, this.main );
    this.etcTab  = new Tab( "Sonstiges" );
    this.etcTab.setClosable( false );
    this.etcTab.setContent( this.etcNode );
    this.tabPane.getTabs().add( this.etcTab );


    // Trennlinie
    VBox vbox = new VBox();
    vbox.setAlignment( Pos.CENTER );
    vbox.setPadding( Insets.EMPTY );
    vbox.setFillWidth( true );
    vbox.getChildren().add( new Separator( Orientation.HORIZONTAL ) );
    setBottom( vbox );


    // Buttons
    this.btnApply = new Button( "\u00DCbernehmen" );
    this.btnApply.setMaxWidth( Double.MAX_VALUE );

    this.btnSave = new Button( "Speichern" );
    this.btnSave.setMaxWidth( Double.MAX_VALUE );

    TilePane btnPane = new TilePane(
				Orientation.HORIZONTAL,
				this.btnApply,
				this.btnSave );
    btnPane.setTileAlignment( Pos.CENTER_LEFT );
    btnPane.setPadding( new Insets( 10 ) );
    btnPane.setHgap( 10 );
    btnPane.setPrefRows( 1 );
    vbox.getChildren().add( btnPane );

    this.btnApply.setOnAction( e->doApply() );
    this.btnSave.setOnAction( e->doSave() );
  }


	/* --- private Methoden --- */

  private void doApply()
  {
    Tab tab = null;
    try {
      boolean    status = true;
      Properties props  = new Properties();

      // Einstellungen im Tab System uebernehmen
      tab                   = this.systemTab;
      JTCSys.OSType osType  = this.systemNode.applySettingsTo( props );

      // Einstellungen im Tab RAM uebernehmen
      tab = this.ramTab;
      this.ramNode.applySettingsTo( props, osType );

      // Einstellungen im Tab ROM uebernehmen
      tab              = this.romTab;
      ExtROM[] extROMs = this.romNode.applySettingsTo( props, osType );

      // Einstellungen im Tab Bestaetigungen uebernehmen
      tab = this.confirmTab;
      this.confirmNode.applySettingsTo( props );

      // Einstellungen im Tab Sonstiges uebernehmen
      tab = this.etcTab;
      this.etcNode.applySettingsTo( props );

      // uebernommene Einstellungen aktivieren
      AppContext.putProperties( props );

      // Schaltflaechen aktualisieren
      this.btnApply.setDisable( true );
      this.btnSave.setDisable( Main.getPropertiesFile() == null );

      // JTCSys aktualisieren
      this.jtcSys.settingsChanged(
			extROMs,
			this.romNode.getROMBank( osType ) );

      // zum Schluss sonstige Interessierte informieren
      Platform.runLater( ()->this.main.settingsChanged() );
    }
    catch( UserInputException ex ) {
      if( tab != null ) {
	this.tabPane.getSelectionModel().select( tab );
      }
      this.main.showError( ex );
    }
  }


  private void doSave()
  {
    File file = Main.getPropertiesFile();
    if( file != null ) {
      Properties props = AppContext.getProperties();

      // Fensterpostion und Skalierung merken
      Window window = main.getStage();
      if( window != null ) {
	props.setProperty(
		AppContext.getPropPrefix() + Main.PROP_WINDOW_X,
		Double.toString( window.getX() ) );
	props.setProperty(
		AppContext.getPropPrefix() + Main.PROP_WINDOW_Y,
		Double.toString( window.getY() ) );

	JTCSys.OSType osType = this.jtcSys.getOSType();
	int           scale  = ScreenNode.getDefaultScreenScale( osType );
	ScreenNode    screen = this.main.getScreen();
	if( screen != null ) {
	  scale = screen.getScreenScale();
	}
	props.setProperty(
		AppContext.getPropPrefix() + ScreenNode.PROP_SCREEN_SCALE,
		Integer.toString( scale ) );
      }

      // eigentlches speichern
      try {
	OutputStream out = null;
	try {
	  out = new FileOutputStream( file );
	  AppContext.getProperties().storeToXML(
				out,
				AppContext.getAppName() + " settings" );
	  out.close();
	  out = null;
	  this.btnSave.setDisable( true );
	}
	finally {
	  JTCUtil.closeSilently( out );
	}
      }
      catch( IOException ex ) {
	this.main.showError( ex );
      }
      this.etcNode.updPropsFileDeleteButtonEnabled();
    }
  }
}
