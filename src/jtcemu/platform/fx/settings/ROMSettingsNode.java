/*
 * (c) 2014-2020 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Node fuer eingebundene ROM-Dateien
 */

package jtcemu.platform.fx.settings;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Properties;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.Priority;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import jtcemu.base.AppContext;
import jtcemu.base.ExtROM;
import jtcemu.base.JTCSys;
import jtcemu.base.JTCUtil;
import jtcemu.base.UserInputException;
import jtcemu.platform.fx.Main;
import jtcemu.platform.fx.base.DropFileHandler;
import jtcemu.platform.fx.base.GUIUtil;
import jtcemu.platform.fx.base.ReplyDlg;


public class ROMSettingsNode extends ScrollPane implements DropFileHandler
{
  private static final String FILE_GROUP_ROM     = "rom";
  private static final String FILE_GROUP_ROMBANK = "rombank";

  private SettingsNode           settingsNode;
  private Main                   main;
  private java.util.List<ExtROM> extROMs;
  private ExtROM                 romBank;
  private ListView<ExtROM>       listView;
  private CheckBox               btnReload;
  private CheckBox               btnRomBank;
  private Button                 btnRomAdd;
  private Button                 btnRomRemove;
  private Button                 btnRomBankFileSelect;
  private Button                 btnRomBankFileRemove;
  private Label                  labelRomBankFile;
  private TextField              fldRomBankFile;


  public ROMSettingsNode( final SettingsNode settingsNode, Main main )
  {
    this.settingsNode = settingsNode;
    this.main         = main;
    this.extROMs      = new ArrayList<ExtROM>();
    this.romBank      = null;

    setFitToHeight( true );
    setFitToWidth( true );

    VBox vbox = new VBox();
    vbox.setAlignment( Pos.CENTER_LEFT );
    vbox.setFillWidth( true );
    vbox.setPadding( new Insets( 10.0 ) );
    setContent( vbox );
    vbox.getChildren().add(
		new Label( "Zus\u00E4tzliche oder mit anderem Inhalt"
				+ " belegte ROM-Bereiche:" ) );

    this.listView = new ListView<>();
    this.listView.setOrientation( Orientation.VERTICAL );
    this.listView.setPrefHeight( 100.0 );
    vbox.getChildren().add( this.listView );
    VBox.setVgrow( this.listView, Priority.ALWAYS );

    this.btnRomAdd = new Button( "Hinzuf\u00FCgen..." );
    this.btnRomAdd.setMaxWidth( Double.MAX_VALUE );

    this.btnRomRemove = new Button( "Entfernen" );
    this.btnRomRemove.setMaxWidth( Double.MAX_VALUE );

    TilePane romBtnPane = new TilePane(
				Orientation.HORIZONTAL,
				10, 0,
				this.btnRomAdd, this.btnRomRemove );
    romBtnPane.setAlignment( Pos.CENTER_LEFT );
    VBox.setMargin( romBtnPane, new Insets( 5.0, 0.0, 0.0, 0.0 ) );
    vbox.getChildren().add( romBtnPane );

    this.btnRomBank = new CheckBox(
		"ROM-Bank im Bereich %2000-%3FFF emulieren (nur ES 4.0)" );
    this.btnRomBank.setOnAction( e->doRomBank() );
    VBox.setMargin( this.btnRomBank, new Insets( 20.0, 0.0, 0.0, 0.0 ) );
    vbox.getChildren().add( btnRomBank );

    this.labelRomBankFile = new Label( "Inhalt der ROM-Bank:" );
    VBox.setMargin(
		this.labelRomBankFile,
		new Insets( 0.0, 0.0, 0.0, 50.0 ) );
    vbox.getChildren().add( this.labelRomBankFile );

    this.fldRomBankFile = new TextField();
    this.fldRomBankFile.setEditable( false );
    VBox.setMargin(
		this.fldRomBankFile,
		new Insets( 0.0, 0.0, 0.0, 50.0 ) );
    vbox.getChildren().add( this.fldRomBankFile );

    this.btnRomBankFileSelect = new Button( "Ausw\u00E4hlen..." );
    this.btnRomBankFileSelect.setMaxWidth( Double.MAX_VALUE );
    this.btnRomBankFileSelect.setOnAction( e->doRomBankFileSelect() );

    this.btnRomBankFileRemove = new Button( "Entfernen" );
    this.btnRomBankFileRemove.setMaxWidth( Double.MAX_VALUE );
    this.btnRomBankFileRemove.setDisable( true );
    this.btnRomBankFileRemove.setOnAction( e->doRomBankFileRemove() );

    TilePane romBankBtnPane = new TilePane(
				Orientation.HORIZONTAL,
				10, 0,
				this.btnRomBankFileSelect,
				this.btnRomBankFileRemove );
    romBankBtnPane.setAlignment( Pos.CENTER_LEFT );
    VBox.setMargin(
		romBankBtnPane,
		new Insets( 5.0, 0.0, 0.0, 50.0 ) );
    vbox.getChildren().add( romBankBtnPane );

    this.btnReload = new CheckBox( "ROM-Dateien und ROM-Bank"
			+ " bei jedem \"Einschalten\" neu laden" );
    VBox.setMargin( this.btnReload, new Insets( 20, 0, 0, 0 ) );
    vbox.getChildren().add( this.btnReload );

    this.listView.getSelectionModel().selectedItemProperty().addListener(
					(ov,o,v)->updRomRemoveBtnState() );
    updSettings();

    JTCSys jtcSys = this.main.getJTCSys();
    if( jtcSys != null ) {
      this.romBank = jtcSys.getROMBank();
    }
    if( this.romBank != null ) {
      this.btnRomBank.setSelected( true );
      this.fldRomBankFile.setText( this.romBank.getFile().getPath() );
    } else {
      this.btnRomBank.setSelected( false );

      /*
       * Wenn in den Einstellungen trotzdem eine ROM-Bank-Datei
       * angegeben ist, dann diese lesen
       */
      try {
	String fileName = AppContext.getProperty( JTCSys.PROP_ROMBANK_FILE );

	if( fileName != null ) {
	  if( !fileName.isEmpty() ) {
	    this.romBank = new ExtROM(
				new File( fileName ),
				JTCSys.MAX_ROMBANK_SIZE );
	    this.fldRomBankFile.setText( fileName );
	  }
	}
      }
      catch( IOException ex ) {}
    }
    updRomBankFieldsEnabled();
    updRomRemoveBtnState();
    updReloadBtnEnabled();
    this.btnRomAdd.setOnAction( e->doRomAdd() );
    this.btnRomRemove.setOnAction( e->doRomRemove() );
    this.btnReload.setOnAction( e->settingsNode.setDataChanged() );

    // Laden einer ROM-Datei ueber Drag&Drop ermoeglichen
    this.listView.setOnDragOver( e->GUIUtil.handleFileDragOver( e ) );
    this.listView.setOnDragDropped(
			e->GUIUtil.handleFileDragDropped( e, this ) );

    // Laden der ROM-Bank-Datei ueber Drag&Drop ermoeglichen
    this.fldRomBankFile.setOnDragOver( e->GUIUtil.handleFileDragOver( e ) );
    this.fldRomBankFile.setOnDragDropped(
			e->GUIUtil.handleFileDragDropped( e, this ) );

    /*
     * Damit dieser Tab nicht groesser als der
     * durch die anderen Tabs benoetigte Platz wird,
     * wird das ListView erst einmal nicht gemanaged und spaeter,
     * wenn die Fenstergroesse berechnet ist,
     * in die Groessenverwaltung einbezogen.
     */
    final ListView listView = this.listView;
    listView.setManaged( false );
    Platform.runLater( ()->listView.setManaged( true ) );
  }


  public ExtROM[] applySettingsTo(
			Properties    props,
			JTCSys.OSType osType ) throws UserInputException
  {
    props.setProperty(
		AppContext.getPropPrefix() + JTCSys.PROP_ROM_RELOAD,
		Boolean.toString( this.btnReload.isSelected()
				&& JTCSys.supportsROMBank( osType ) ) );

    boolean romBankState = JTCSys.supportsROMBank( osType )
				&& this.btnRomBank.isSelected();
    JTCUtil.applyROMBankSettings(
				props,
				romBankState,
				this.fldRomBankFile.getText() );
    ExtROM[] extROMs = null;
    try {
      extROMs = this.extROMs.toArray( new ExtROM[ this.extROMs.size() ] );
      JTCUtil.applyROMSettings( props, extROMs, romBankState );
    }
    catch( ArrayStoreException ex ) {}
    return extROMs;
  }


  public void osSelectionChanged( JTCSys.OSType osType )
  {
    this.btnRomBank.setDisable( !JTCSys.supportsROMBank( osType ) );
    updRomBankFieldsEnabled();
  }


  public ExtROM getROMBank( JTCSys.OSType osType )
  {
    return JTCSys.supportsROMBank( osType ) && this.btnRomBank.isSelected() ?
							this.romBank
							: null;
  }


  public void updSettings()
  {
    JTCSys jtcSys = this.main.getJTCSys();
    if( jtcSys != null ) {
      this.extROMs.clear();
      ExtROM[] roms = jtcSys.getExtROMs();
      if( roms != null ) {
	for( ExtROM rom : roms ) {
	  this.extROMs.add( rom );
	}
      }
      updListView();
    }
    this.btnReload.setSelected(
		AppContext.getBooleanProperty( "rom.reload", false ) );
  }


	/* --- DropFileHandler --- */

  @Override
  public boolean handleDroppedFile( Object target, final File file )
  {
    boolean success = false;
    if( target == this.listView ) {
      // nicht auf Benutzereingabe warten
      Platform.runLater( ()->addRomFile( file ) );
      success = true;
    } else if( target == this.fldRomBankFile ) {
      // nicht auf Benutzereingabe warten
      Platform.runLater( ()->loadRomBankFile( file ) );
      success = true;
    }
    return success;
  }


	/* --- private Methoden --- */

  private boolean addRomFile( File file )
  {
    boolean done = false;
    try {
      ExtROM  rom  = new ExtROM( file, JTCSys.MAX_ROM_SIZE );
      Integer addr = ReplyDlg.showReplyHex4Dlg(
				this.main.getStage(),
				"Anfangsadresse:",
				JTCUtil.getBegAddrFromFilename( file ) );
      if( addr != null ) {
	rom.setBegAddr( addr.intValue() );
	this.extROMs.add( rom );
	updListView();
	updReloadBtnEnabled();
	this.settingsNode.setDataChanged();
	done = true;
      }
    }
    catch( Exception ex ) {
      this.main.showError( ex );
    }
    return done;
  }


  private void doRomAdd()
  {
    File file = openRomFileChooser( FILE_GROUP_ROM );
    if( file != null ) {
      if( addRomFile( file ) ) {
	AppContext.setLastFile( FILE_GROUP_ROM, file );
      }
    }
  }


  private void doRomRemove()
  {
    Collection<ExtROM> selectedROMs = this.listView.getSelectionModel()
							.getSelectedItems();
    if( selectedROMs != null ) {
      if( !selectedROMs.isEmpty() ) {
	this.extROMs.removeAll( selectedROMs );
	updListView();
	updReloadBtnEnabled();
	this.settingsNode.setDataChanged();
      }
    }
  }


  private void doRomBank()
  {
    updRomBankFieldsEnabled();
    updReloadBtnEnabled();
    this.settingsNode.setDataChanged();
  }


  private void doRomBankFileSelect()
  {
    if( !this.btnRomBank.isDisabled() && this.btnRomBank.isSelected() ) {
      File file = openRomFileChooser( FILE_GROUP_ROMBANK );
      if( file != null ) {
	if( loadRomBankFile( file ) ) {
	  AppContext.setLastFile( FILE_GROUP_ROMBANK, file );
	}
      }
    }
  }


  private void doRomBankFileRemove()
  {
    this.romBank = null;
    this.fldRomBankFile.setText( "" );
    this.btnRomBankFileRemove.setDisable( true );
    updReloadBtnEnabled();
    this.settingsNode.setDataChanged();
  }


  private boolean loadRomBankFile( File file )
  {
    boolean done = false;
    try {
      this.romBank = new ExtROM( file, JTCSys.MAX_ROMBANK_SIZE );
      this.fldRomBankFile.setText( file.getPath() );
      this.btnRomBankFileRemove.setDisable( false );
      updReloadBtnEnabled();
      this.settingsNode.setDataChanged();
      done = true;
    }
    catch( Exception ex ) {
      this.main.showError( ex );
    }
    return done;
  }


  private File openRomFileChooser( String fileGroup )
  {
    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle( "ROM-Datei laden" );
    fileChooser.getExtensionFilters().addAll(
	new FileChooser.ExtensionFilter( "Alle Dateien", "*" ),
	new FileChooser.ExtensionFilter(
				"Bin\u00E4rdateien (*.bin)",
				"*.bin" ),
	new FileChooser.ExtensionFilter( "ROM-Dateien (*.rom)", "*.rom" ) );
    File dirFile = AppContext.getLastDirFile( fileGroup );
    if( dirFile != null ) {
      fileChooser.setInitialDirectory( dirFile );
    }
    return fileChooser.showOpenDialog( this.main.getStage() );
  }


  private void updRomRemoveBtnState()
  {
    this.btnRomRemove.setDisable(
		this.listView.getSelectionModel().isEmpty() );
  }


  private void updListView()
  {
    try {
      Collections.sort( this.extROMs );
    }
    catch( ClassCastException ex ) {}
    this.listView.setItems( FXCollections.observableList( this.extROMs ) );
  }


  private void updReloadBtnEnabled()
  {
    this.btnReload.setDisable(
		this.extROMs.isEmpty() && !this.btnRomBank.isSelected() );
  }


  private void updRomBankFieldsEnabled()
  {
    boolean disabled = this.btnRomBank.isDisabled()
				|| !this.btnRomBank.isSelected();
    this.labelRomBankFile.setDisable( disabled );
    this.fldRomBankFile.setDisable( disabled );
    this.btnRomBankFileSelect.setDisable( disabled );

    String fileName = this.fldRomBankFile.getText();
    if( fileName != null ) {
      if( fileName.trim().isEmpty() ) {
	fileName = null;
      }
    }
    this.btnRomBankFileRemove.setDisable( disabled || (fileName == null) );
  }
}
