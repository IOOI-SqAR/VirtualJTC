/*
 * (c) 2014-2021 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Dialog zum Speichern eines Adressbereichs
 */

package jtcemu.platform.fx.base;

import java.io.File;
import java.io.IOException;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import jtcemu.base.AppContext;
import jtcemu.base.FileInfo;
import jtcemu.base.FileSaver;
import jtcemu.base.JTCSys;
import jtcemu.base.JTCUtil;
import jtcemu.platform.fx.Main;
import jtcemu.tools.ToolUtil;


public class SaveDlg extends Stage
{
  private static final String LABEL_BEG_ADDR   = "Anfangsadresse:";
  private static final String LABEL_END_ADDR   = "Endadresse:";
  private static final String LABEL_START_ADDR = "Startadresse:";

  private JTCNode     jtcNode;
  private JTCSys      jtcSys;
  private TextField   fldBegAddr;
  private TextField   fldEndAddr;
  private TextField   fldFileBegAddr;
  private TextField   fldFileStartAddr;
  private TextField   fldFileDesc;
  private Label       labelFileBegAddr;
  private Label       labelFileBegAddrOpt;
  private Label       labelFileStartAddr;
  private Label       labelFileStartAddrOpt;
  private Label       labelFileDesc;
  private RadioButton btnJTC;
  private RadioButton btnTAP;
  private RadioButton btnBIN;
  private RadioButton btnHEX;
  private Button      btnSave;


  public static void showSaveDlg(
				Window  owner,
				JTCNode jtcNode,
				JTCSys  jtcSys )
  {
    (new SaveDlg( owner, jtcNode, jtcSys )).showAndWait();
  }


	/* --- Konstruktor --- */

  private SaveDlg( Window owner, JTCNode jtcNode, JTCSys jtcSys )
  {
    initOwner( owner );
    initModality( Modality.WINDOW_MODAL );
    setOnShown( e->GUIUtil.centerStageOnOwner( this ) );
    setResizable( false );
    setTitle( "Datei speichern" );
    Main.addIconsTo( this );
    this.jtcNode = jtcNode;
    this.jtcSys  = jtcSys;


    // Zu speichernder Bereich
    this.fldBegAddr = new TextField();
    this.fldBegAddr.setPrefColumnCount( 4 );

    Label labelEndAddr = new Label( LABEL_END_ADDR );
    HBox.setMargin( labelEndAddr, new Insets( 0.0, 0.0, 0.0, 15.0 ) );

    this.fldEndAddr = new TextField();
    this.fldEndAddr.setPrefColumnCount( 4 );

    HBox addrNode = new HBox(
			5.0,
			new Label( LABEL_BEG_ADDR ),
			this.fldBegAddr,
			labelEndAddr,
			this.fldEndAddr );
    addrNode.setAlignment( Pos.CENTER_LEFT );

    TitledPane addrPane = new TitledPane(
				"Zu speichernder Bereich",
				addrNode );
    addrPane.setCollapsible( false );


    // Dateiformat
    ToggleGroup grpFmt = new ToggleGroup();

    this.btnJTC = new RadioButton( "JTC" );
    this.btnJTC.setOnAction( e->fileFormatChanged() );
    this.btnJTC.setToggleGroup( grpFmt );

    this.btnTAP = new RadioButton( "KC-TAP" );
    this.btnTAP.setOnAction( e->fileFormatChanged() );
    this.btnTAP.setToggleGroup( grpFmt );

    this.btnBIN = new RadioButton( "BIN" );
    this.btnBIN.setOnAction( e->fileFormatChanged() );
    this.btnBIN.setToggleGroup( grpFmt );

    this.btnHEX = new RadioButton( "HEX" );
    this.btnHEX.setOnAction( e->fileFormatChanged() );
    this.btnHEX.setToggleGroup( grpFmt );

    HBox fileFmtNode = new HBox(
			15.0,
			this.btnJTC,
			this.btnTAP,
			this.btnBIN,
			this.btnHEX );
    fileFmtNode.setAlignment( Pos.CENTER_LEFT );

    TitledPane fileFmtPane = new TitledPane(
				"Zu speichernder Bereich",
				fileFmtNode );
    fileFmtPane.setCollapsible( false );


    // Angaben in der Datei
    GridPane fileHeadNode = new GridPane();
    fileHeadNode.setHgap( 5.0 );
    fileHeadNode.setVgap( 5.0 );

    this.labelFileBegAddr = new Label( LABEL_BEG_ADDR );
    fileHeadNode.add( this.labelFileBegAddr, 0, 0 );

    this.fldFileBegAddr = new TextField();
    this.fldFileBegAddr.setPrefColumnCount( 4 );
    fileHeadNode.add( this.fldFileBegAddr, 1, 0 );

    this.labelFileBegAddrOpt = new Label( "(nur wenn abweichend)" );
    fileHeadNode.add( this.labelFileBegAddrOpt, 2, 0 );

    this.labelFileStartAddr = new Label( LABEL_START_ADDR );
    fileHeadNode.add( this.labelFileStartAddr, 0, 1 );

    this.fldFileStartAddr = new TextField();
    this.fldFileStartAddr.setPrefColumnCount( 4 );
    fileHeadNode.add( this.fldFileStartAddr, 1, 1 );

    this.labelFileStartAddrOpt = new Label( "(optional)" );
    fileHeadNode.add( this.labelFileStartAddrOpt, 2, 1 );

    this.labelFileDesc = new Label( "Bezeichnung:" );
    fileHeadNode.add( this.labelFileDesc, 0, 2 );

    this.fldFileDesc = new TextField();
    this.fldFileDesc.setMaxWidth( Double.MAX_VALUE );
    GridPane.setColumnSpan( this.fldFileDesc, 2 );
    GridPane.setFillWidth( this.fldFileDesc, true );
    fileHeadNode.add( this.fldFileDesc, 1, 2 );

    TitledPane fileHeadPane = new TitledPane(
				"Angaben in der Datei",
				fileHeadNode );
    fileHeadPane.setCollapsible( false );


    // Schaltflaechen
    this.btnSave = new Button( "Speichern..." );
    this.btnSave.setMaxWidth( Double.MAX_VALUE );
    this.btnSave.setOnAction( e->doSave() );

    Button btnCancel = new Button( "Abbrechen" );
    btnCancel.setMaxWidth( Double.MAX_VALUE );
    btnCancel.setOnAction( e->close() );

    TilePane buttonPane = new TilePane(
				Orientation.HORIZONTAL,
				10.0, 0.0,
				this.btnSave, btnCancel );
    buttonPane.setAlignment( Pos.CENTER );
    buttonPane.setPrefColumns( 2 );


    // Gesamt-Layout
    VBox rootNode = new VBox(
			10.0,
			addrPane,
			fileFmtPane,
			fileHeadPane,
			buttonPane );
    rootNode.setFillWidth( true );
    rootNode.setPadding( new Insets( 10.0 ) );
    setScene( new Scene( rootNode ) );

    final Stage stage = this;
    Platform.runLater( ()->sizeToScene() );


    // Fokussteuerung
    this.fldBegAddr.setOnKeyPressed(
		e->transferFocusOnEnter( e, this.fldEndAddr ) );
    this.fldEndAddr.setOnKeyPressed(
		e->transferFocusOnEnter(
				e,
				this.btnJTC,
				this.btnTAP,
				this.btnBIN,
				this.btnHEX ) );
    this.btnJTC.setOnKeyPressed(
		e->fileFormatOnEnter( e, this.fldFileBegAddr ) );
    this.btnTAP.setOnKeyPressed(
		e->fileFormatOnEnter( e, this.fldFileBegAddr ) );
    this.btnBIN.setOnKeyPressed(
		e->fileFormatOnEnter( e, this.btnSave ) );
    this.btnHEX.setOnKeyPressed(
		e->fileFormatOnEnter( e, this.fldFileBegAddr ) );
    this.fldFileBegAddr.setOnKeyPressed(
		e->transferFocusOnEnter( e, this.fldFileStartAddr ) );
    this.fldFileStartAddr.setOnKeyPressed(
		e->transferFocusOnEnter( e, this.fldFileDesc ) );
    this.fldFileDesc.setOnKeyPressed( e->saveOnEnter( e ) );

    // ggf. Adressen eines BASIC-Programms eintragen
    int endAddr = ToolUtil.getBasicEndAddress( jtcSys );
    if( endAddr > 0xE002 ) {
      this.fldBegAddr.setText( "E000" );
      this.fldEndAddr.setText( String.format( "%04X", endAddr ) );
    }

    // Vorbelegung Dateiformat
    this.btnJTC.setSelected( true );
    fileFormatChanged();
  }


	/* --- private Methoden --- */

  private void fileFormatChanged()
  {
    boolean disableBegAddr = this.btnBIN.isSelected();
    this.labelFileBegAddr.setDisable( disableBegAddr );
    this.fldFileBegAddr.setDisable( disableBegAddr );
    this.labelFileBegAddrOpt.setDisable( disableBegAddr );

    disableBegAddr |= this.btnHEX.isSelected();
    this.labelFileStartAddr.setDisable( disableBegAddr );
    this.fldFileStartAddr.setDisable( disableBegAddr );
    this.labelFileStartAddrOpt.setDisable( disableBegAddr );

    boolean disableDesc = (this.btnBIN.isSelected()
					|| this.btnHEX.isSelected());
    this.labelFileDesc.setDisable( disableDesc );
    this.fldFileDesc.setDisable( disableDesc );
  }


  private void doSave()
  {
    try {
      int begAddr = JTCUtil.parseHex4(
				this.fldBegAddr.getText(),
				LABEL_BEG_ADDR );
      int endAddr = JTCUtil.parseHex4(
				this.fldEndAddr.getText(),
				LABEL_END_ADDR );
      if( endAddr < begAddr ) {
	throw new IOException( "Die Endadresse kann nicht vor der"
					+ " Anfangsadresse liegen!" );
      }

      int fileBegAddr = begAddr;
      if( !this.fldFileBegAddr.isDisabled() ) {
	String text = this.fldFileBegAddr.getText();
	if( text != null ) {
	  text = text.trim();
	  if( !text.isEmpty() ) {
	    fileBegAddr = JTCUtil.parseHex4(
					text,
					"Anfangsadresse in der Datei:" );
	  }
	}
      }

      int fileStartAddr = -1;
      if( !this.fldFileStartAddr.isDisabled() ) {
	String text = this.fldFileStartAddr.getText();
	if( text != null ) {
	  text = text.trim();
	  if( !text.isEmpty() ) {
	    fileStartAddr = JTCUtil.parseHex4(
					text,
					LABEL_START_ADDR );
	  }
	}
      }

      boolean jtcSelected = this.btnJTC.isSelected();
      boolean tapSelected = this.btnTAP.isSelected();
      boolean hexSelected = this.btnHEX.isSelected();

      FileSaver.Format format      = FileSaver.Format.BIN;
      FileChooser      fileChooser = new FileChooser();
      if( jtcSelected ) {
	format = FileSaver.Format.JTC;
	fileChooser.getExtensionFilters().add(
		new FileChooser.ExtensionFilter(
					"JTC-Dateien (*.jtc)", "*.jtc" ) );
      } else if( tapSelected ) {
	format = FileSaver.Format.TAP;
	fileChooser.getExtensionFilters().add(
		new FileChooser.ExtensionFilter(
					"TAP-Dateien (*.tap)", "*.tab" ) );
      } else if( hexSelected ) {
	format = FileSaver.Format.HEX;
	fileChooser.getExtensionFilters().add(
		new FileChooser.ExtensionFilter(
					"HEX-Dateien (*.hex)", "*.hex" ) );
      } else {
	fileChooser.getExtensionFilters().addAll(
		new FileChooser.ExtensionFilter(
					"BIN-Dateien (*.bin)", "*.bin" ),
		new FileChooser.ExtensionFilter(
					"ROM-Dateien (*.rom)", "*.rom" ) );
      }
      fileChooser.getExtensionFilters().add(
		new FileChooser.ExtensionFilter( "Alle Dateien", "*" ) );
      File dirFile = AppContext.getLastDirFile(
					FileInfo.FILE_GROUP_SOFTWARE );
      if( dirFile != null ) {
	fileChooser.setInitialDirectory( dirFile );
      }
      File file = fileChooser.showSaveDialog( this );
      if( file != null ) {
	String statusText = FileSaver.save(
					this.jtcSys,
					begAddr,
					endAddr,
					fileStartAddr,
					file,
					format,
					fileBegAddr,
					this.fldFileDesc.getText() );
	if( statusText != null ) {
	  close();
	  this.jtcNode.showStatusText( statusText );
	}
      }
    }
    catch( Exception ex ) {
      MsgDlg.showErrorMsg( this, ex );
    }
  }


  private void fileFormatOnEnter( KeyEvent e, Control c )
  {
    if( e.getCode() == KeyCode.ENTER ) {
      e.consume();
      fileFormatChanged();
      c.requestFocus();
    }
  }


  private void saveOnEnter( KeyEvent e )
  {
    if( e.getCode() == KeyCode.ENTER ) {
      e.consume();
      doSave();
    }
  }


  private void transferFocusOnEnter( KeyEvent e, Control c )
  {
    if( e.getCode() == KeyCode.ENTER ) {
      e.consume();
      c.requestFocus();
    }
  }


  private void transferFocusOnEnter( KeyEvent e, RadioButton... radioBtns )
  {
    if( e.getCode() == KeyCode.ENTER ) {
      e.consume();
      for( RadioButton rb : radioBtns ) {
	if( rb.isSelected() ) {
	  rb.requestFocus();
	  break;
	}
      }
    }
  }
}
