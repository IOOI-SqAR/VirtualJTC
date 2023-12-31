/*
 * (c) 2014-2021 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Dialog zur Eingabe der Ladeadressen
 */

package jtcemu.platform.fx.base;

import java.io.File;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import jtcemu.base.AppContext;
import jtcemu.base.FileInfo;
import jtcemu.base.FileLoader;
import jtcemu.base.JTCUtil;
import jtcemu.platform.fx.Main;
import z8.Z8Memory;


public class LoadDlg extends Stage
{
  private static final String LABEL_BEG_ADDR = "Laden ab Adresse:";
  private static final String LABEL_END_ADDR = "Bis max. Adresse:";

  private JTCNode         jtcNode;
  private Z8Memory        memory;
  private File            file;
  private FileInfo.Format fileFmt;
  private TextField       fldBegAddr;
  private TextField       fldEndAddr;
  private CheckBox        btnLoadAsBIN;


  public static void showLoadDlg(
			Window   owner,
			JTCNode  jtcNode,
			Z8Memory memory,
			File     file,
			FileInfo fileInfo )
  {
    (new LoadDlg( owner, jtcNode, memory, file, fileInfo )).showAndWait();
  }


  public static boolean loadFile(
				Window          owner,
				JTCNode         jtcNode,
				Z8Memory        memory,
				File            file,
				FileInfo.Format fmt,
				int             begAddr,
				int             endAddr,
				int             startAddr )
  {
    boolean done = false;
    if( memory != null ) {
      FileLoader fileLoader = new FileLoader( memory );
      String statusText = fileLoader.loadFile(
					file,
					fmt,
					begAddr,
					endAddr,
					startAddr );
      if( statusText != null ) {
	AppContext.setLastFile( FileInfo.FILE_GROUP_SOFTWARE, file );
	done = true;
      }
      String msg = fileLoader.getMessage();
      if( !done && (msg == null) ) {
	msg = "Datei konnte nicht geladen werden.";
      }
      if( msg != null ) {
	if( done) {
	  MsgDlg.showInfoMsg( owner, msg );
	} else {
	  MsgDlg.showErrorMsg( owner, msg );
	}
      }
      if( statusText != null ) {
	jtcNode.showStatusText( statusText );
      }
    }
    return done;
  }


	/* --- Konstruktor --- */

  private LoadDlg(
		Window   owner,
		JTCNode  jtcNode,
		Z8Memory memory,
		File     file,
		FileInfo fileInfo )
  {
    initOwner( owner );
    initModality( Modality.WINDOW_MODAL );
    setOnShown( e->GUIUtil.centerStageOnOwner( this ) );
    setResizable( true );
    setTitle( "Datei laden" );
    Main.addIconsTo( this );
    this.jtcNode = jtcNode;
    this.memory  = memory;
    this.file    = file;
    this.fileFmt = fileInfo.getFormat();


    // Eingabebereich
    GridPane dataPane = new GridPane();
    dataPane.setHgap( 5.0 );
    dataPane.setVgap( 5.0 );
    dataPane.setPadding( new Insets( 10 ) );

    int y = 0;
    dataPane.add( new Label( "Dateiname:" ), 0, y );

    TextField fldName = new TextField();
    fldName.setAlignment( Pos.CENTER_LEFT );
    fldName.setEditable( false );
    fldName.setMaxWidth( Double.MAX_VALUE );
    String fName = file.getName();
    if( fName != null ) {
      fldName.setText( fName );
      fldName.setPrefColumnCount( fName.length() );
    }
    dataPane.add( fldName, 1, y++ );
    GridPane.setFillWidth( fldName, true );

    dataPane.add( new Label( "Info:" ), 0, y );

    TextField fldInfo = new TextField();
    fldInfo.setAlignment( Pos.CENTER_LEFT );
    fldInfo.setEditable( false );
    fldInfo.setMaxWidth( Double.MAX_VALUE );
    String infoText = fileInfo.getInfoText();
    if( infoText != null ) {
      fldInfo.setText( infoText );
      fldInfo.setPrefColumnCount( infoText.length() );
    }
    dataPane.add( fldInfo, 1, y++ );
    GridPane.setFillWidth( fldInfo, true );

    this.btnLoadAsBIN = null;
    if( !this.fileFmt.equals( FileInfo.Format.BIN ) ) {
      this.btnLoadAsBIN = new CheckBox( "Als BIN-Datei laden" );
      this.btnLoadAsBIN.setAlignment( Pos.CENTER_LEFT );
      dataPane.add( this.btnLoadAsBIN, 1, y++ );
    }

    Label labelBegAddr = new Label( LABEL_BEG_ADDR );
    dataPane.add( labelBegAddr, 0, y );
    GridPane.setMargin( labelBegAddr, new Insets( 10.0, 0.0, 0.0, 0.0 ) );

    this.fldBegAddr = new TextField();
    this.fldBegAddr.setAlignment( Pos.CENTER_LEFT );
    this.fldBegAddr.setPrefColumnCount( 4 );
    int begAddr = fileInfo.getBegAddr();
    if( begAddr < 0 ) {
      begAddr = FileLoader.DEFAULt_LOAD_ADDR;
    }
    this.fldBegAddr.setText( String.format( "%04X", begAddr ) );

    dataPane.add( this.fldBegAddr, 1, y++ );
    GridPane.setFillWidth( this.fldBegAddr, false );
    GridPane.setMargin( this.fldBegAddr, new Insets( 10.0, 0.0, 0.0, 0.0 ) );

    dataPane.add( new Label( LABEL_END_ADDR ), 0, y );

    this.fldEndAddr = new TextField();
    this.fldEndAddr.setAlignment( Pos.CENTER_LEFT );
    this.fldEndAddr.setPrefColumnCount( 4 );
    dataPane.add( this.fldEndAddr, 1, y++ );
    GridPane.setFillWidth( this.fldEndAddr, false );


    // Schaltflaechen
    Button btnLoad = new Button( "Laden" );
    btnLoad.setMaxWidth( Double.MAX_VALUE );
    btnLoad.setOnAction( e->doLoad() );

    Button btnCancel = new Button( "Abbrechen" );
    btnCancel.setMaxWidth( Double.MAX_VALUE );
    btnCancel.setOnAction( e->close() );

    TilePane buttonPane = new TilePane(
				Orientation.HORIZONTAL,
				10.0, 0.0,
				btnLoad, btnCancel );
    buttonPane.setAlignment( Pos.CENTER );
    buttonPane.setPrefColumns( 2 );


    // Gesamt-Layout
    VBox rootNode = new VBox( 10.0, dataPane, buttonPane );
    rootNode.setFillWidth( true );
    rootNode.setPadding( new Insets( 10.0 ) );
    setScene( new Scene( rootNode ) );

    final Stage stage = this;
    Platform.runLater( ()->sizeToScene() );


    // Fokussteuerung
    this.fldBegAddr.setOnKeyPressed(
		e->transferFocusOnEnter( e, this.fldEndAddr ) );
    this.fldEndAddr.setOnKeyPressed( e->loadOnEnter( e ) );
    this.fldBegAddr.requestFocus();
  }


	/* --- private Methoden --- */

  private void doLoad()
  {
    try {
      int endAddr = -1;
      int begAddr = JTCUtil.parseHex4(
				this.fldBegAddr.getText(),
				LABEL_BEG_ADDR );

      String text = this.fldEndAddr.getText();
      if( text != null ) {
	text = text.trim();
	if( !text.isEmpty() ) {
	  endAddr = JTCUtil.parseHex4( text, LABEL_END_ADDR );
	}
      }
      FileInfo.Format fmt = this.fileFmt;
      if( this.btnLoadAsBIN != null ) {
	if( this.btnLoadAsBIN.isSelected() ) {
	  fmt = FileInfo.Format.BIN;
	}
      }
      if( fmt != null ) {
	if( loadFile(
		this,
		this.jtcNode,
		this.memory,
		this.file,
		fmt,
		begAddr,
		endAddr,
		-1 ) )
	{
	  close();
	}
      }
    }
    catch( Exception ex ) {
      MsgDlg.showErrorMsg( this, ex );
    }
  }


  private void loadOnEnter( KeyEvent e )
  {
    if( e.getCode() == KeyCode.ENTER ) {
      e.consume();
      doLoad();
    }
  }


  private void transferFocusOnEnter( KeyEvent e, Control c )
  {
    if( e.getCode() == KeyCode.ENTER ) {
      e.consume();
      c.requestFocus();
    }
  }
}
