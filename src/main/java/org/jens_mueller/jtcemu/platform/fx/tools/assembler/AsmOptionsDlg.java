/*
 * (c) 2019 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Dialog fuer Assembler-Optionen
 */

package org.jens_mueller.jtcemu.platform.fx.tools.assembler;

import java.io.File;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.jens_mueller.jtcemu.base.AppContext;
import org.jens_mueller.jtcemu.platform.fx.Main;
import org.jens_mueller.jtcemu.platform.fx.base.DropFileHandler;
import org.jens_mueller.jtcemu.platform.fx.base.GUIUtil;
import org.jens_mueller.jtcemu.tools.assembler.AsmOptions;


public class AsmOptionsDlg extends Stage implements DropFileHandler
{
  private static final String FILE_GROUP = "asm";

  private Main       main;
  private AsmOptions oldOptions;
  private AsmOptions approvedOptions;
  private File       codeFile;
  private Button     btnSelectCodeFile;
  private CheckBox   btnCodeToEmu;
  private CheckBox   btnCodeToFile;
  private CheckBox   btnLabelsIgnoreCase;
  private CheckBox   btnListLabels;
  private CheckBox   btnWarnNonAsciiChars;
  private Label      labelCodeFile;
  private TextField  fldCodeFile;


  public static AsmOptions open( Main main, AsmOptions options )
  {
    AsmOptionsDlg dlg = new AsmOptionsDlg( main, options );
    dlg.showAndWait();
    return dlg.approvedOptions;
  }


	/* --- DropFileHandler --- */

  @Override
  public boolean handleDroppedFile( Object target, File file )
  {
    // nicht auf Benutzereingaben warten
    Platform.runLater( ()->codeFileSelected( file ) );
    return true;
  }


	/* --- Konstruktor --- */

  private AsmOptionsDlg(
		Main       main,
		AsmOptions options )
  {
    initOwner( main.getStage() );
    initModality( Modality.WINDOW_MODAL );
    setOnShown( e->GUIUtil.centerStageOnOwner( this ) );
    setResizable( true );
    setTitle( "Assembler-Optionen" );
    Main.addIconsTo( this );
    this.main            = main;
    this.oldOptions      = options;
    this.approvedOptions = null;


    // Bereich Marken
    this.btnLabelsIgnoreCase = new CheckBox(
			"Gro\u00DF-/Kleinschreibung bei Marken ignorieren" );
    this.btnLabelsIgnoreCase.setAlignment( Pos.CENTER_LEFT );

    this.btnListLabels = new CheckBox( "Markentabelle ausgeben" );
    this.btnListLabels.setAlignment( Pos.CENTER_LEFT );

    TitledPane labelsPane = new TitledPane(
		"Marken",
		new HBox(
			10.0,
			this.btnLabelsIgnoreCase,
			this.btnListLabels ) );
    labelsPane.setCollapsible( false );
    labelsPane.setMaxWidth( Double.MAX_VALUE );


    // Bereich erzeugter Programmcode
    this.btnCodeToEmu = new CheckBox(
				"Programmcode in Emulator laden" );
    this.btnCodeToEmu.setAlignment( Pos.CENTER_LEFT );

    this.btnCodeToFile = new CheckBox(
				"Programmcode in Datei speichern" );
    this.btnCodeToFile.setAlignment( Pos.CENTER_LEFT );
    this.btnCodeToFile.setOnAction( e->updCodeFileFieldsEnabled() );

    this.labelCodeFile = new Label( "Dateiname:" );
    this.labelCodeFile.setAlignment( Pos.CENTER_LEFT );
    this.labelCodeFile.setPadding( new Insets( 0.0, 0.0, 0.0, 50.0 ) );

    this.fldCodeFile = new TextField();
    this.fldCodeFile.setEditable( false );
    this.fldCodeFile.setAlignment( Pos.CENTER_LEFT );
    this.fldCodeFile.setMaxWidth( Double.MAX_VALUE );
    this.fldCodeFile.setOnDragOver( e->GUIUtil.handleFileDragOver( e ) );
    this.fldCodeFile.setOnDragDropped(
		e->GUIUtil.handleFileDragDropped( e, this ) );
    HBox.setHgrow( this.fldCodeFile, Priority.ALWAYS );

    this.btnSelectCodeFile = new Button( "..." );
    this.btnSelectCodeFile.setAlignment( Pos.CENTER_LEFT );
    this.btnSelectCodeFile.setOnAction( e->doSelectCodeFile() );

    TitledPane codePane = new TitledPane(
		"Erzeugter Programmcode",
		new VBox(
			5.0,
			this.btnCodeToEmu,
			this.btnCodeToFile,
			new HBox(
				5.0,
				this.labelCodeFile,
				this.fldCodeFile,
				this.btnSelectCodeFile ) ) );
    codePane.setCollapsible( false );
    codePane.setMaxWidth( Double.MAX_VALUE );


    // Bereich Sonstiges
    this.btnWarnNonAsciiChars = new CheckBox(
					"Bei Nicht-ASCII-Zeichen warnen" );
    this.btnWarnNonAsciiChars.setAlignment( Pos.CENTER_LEFT );

    TitledPane etcPane = new TitledPane(
		"Sonstiges",
		new VBox(
			5.0,
			this.btnWarnNonAsciiChars ) );
    etcPane.setCollapsible( false );
    etcPane.setMaxWidth( Double.MAX_VALUE );


    // Schaltflaechen
    final Button btnAssemble = new Button( "Assemblieren" );
    btnAssemble.setMaxWidth( Double.MAX_VALUE );
    btnAssemble.setOnAction( e->doApprove() );

    Button btnCancel = new Button( "Abbrechen" );
    btnCancel.setMaxWidth( Double.MAX_VALUE );
    btnCancel.setOnAction( e->close() );

    TilePane buttonPane = new TilePane(
				Orientation.HORIZONTAL,
				10.0, 0.0,
				btnAssemble, btnCancel );
    buttonPane.setAlignment( Pos.CENTER );
    buttonPane.setPrefColumns( 2 );


    // Gesamt-Layout
    VBox rootNode = new VBox(
			10.0,
			labelsPane,
			codePane,
			etcPane,
			buttonPane );
    rootNode.setFillWidth( true );
    rootNode.setPadding( new Insets( 10.0 ) );
    setScene( new Scene( rootNode ) );

    final Stage stage = this;
    Platform.runLater( ()->sizeToScene() );


    // Vorbelegungen
    if( this.oldOptions == null ) {
      this.oldOptions = new AsmOptions();
    }
    this.btnLabelsIgnoreCase.setSelected(
			this.oldOptions.getLabelsIgnoreCase() );

    this.btnListLabels.setSelected( this.oldOptions.getListLabels() );
    this.btnCodeToEmu.setSelected( this.oldOptions.getCodeToEmu() );
    this.btnCodeToFile.setSelected( this.oldOptions.getCodeToFile() );

    this.codeFile = this.oldOptions.getCodeFile();
    if( this.codeFile != null ) {
      this.fldCodeFile.setText( this.codeFile.getPath() );
    }
    updCodeFileFieldsEnabled();

    this.btnWarnNonAsciiChars.setSelected(
			this.oldOptions.getWarnNonAsciiChars() );

    setOnShown( e->btnAssemble.requestFocus() );
  }


	/* --- private Methoden --- */

  private void codeFileSelected( File file )
  {
    this.codeFile = file;
    this.fldCodeFile.setText( file.getPath() );
  }


  protected void doApprove()
  {
    boolean codeToFile = this.btnCodeToFile.isSelected();
    if( codeToFile && (this.codeFile == null) ) {
      this.main.showError(
		"Erzeugter Programmcode in Datei speichern:\n"
			+ "Es wurde keine Datei angegeben." );
    } else {
      this.approvedOptions = new AsmOptions( this.oldOptions );
      this.approvedOptions.setCodeToEmu( this.btnCodeToEmu.isSelected() );
      this.approvedOptions.setCodeToFile( codeToFile, this.codeFile );
      this.approvedOptions.setLabelsIgnoreCase(
			this.btnLabelsIgnoreCase.isSelected() );
      this.approvedOptions.setListLabels(
			this.btnListLabels.isSelected() );
      this.approvedOptions.setWarnNonAsciiChars(
			this.btnWarnNonAsciiChars.isSelected() );
      close();
    }
  }


  private void doSelectCodeFile()
  {
    FileChooser fileChooser = GUIUtil.createMemFileChooser(
					"Ausgabedatei festlegen" );
    if( this.codeFile != null ) {
      File dirFile = this.codeFile.getParentFile();
      if( dirFile != null ) {
	fileChooser.setInitialDirectory( dirFile );
	String fileName = this.codeFile.getName();
	if( fileName != null ) {
	  fileChooser.setInitialFileName( fileName );
	}
      }
    } else {
      File dirFile = AppContext.getLastDirFile( FILE_GROUP );
      if( dirFile != null ) {
	fileChooser.setInitialDirectory( dirFile );
      }
    }
    File file = fileChooser.showSaveDialog( this.main.getStage() );
    if( file != null ) {
      codeFileSelected( file );
      AppContext.setLastFile( FILE_GROUP, file );
    }
  }


  private void updCodeFileFieldsEnabled()
  {
    boolean disable = !this.btnCodeToFile.isSelected();
    this.labelCodeFile.setDisable( disable );
    this.fldCodeFile.setDisable( disable );
    this.btnSelectCodeFile.setDisable( disable );
  }
}
