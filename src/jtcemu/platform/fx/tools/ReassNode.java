/*
 * (c) 2014-2020 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Inhalt des Reassembler-Tabs
 */

package jtcemu.platform.fx.tools;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javafx.event.Event;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.IndexRange;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.Clipboard;
import javafx.scene.input.DataFormat;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import jtcemu.base.AppContext;
import jtcemu.base.JTCUtil;
import jtcemu.base.UserInputException;
import jtcemu.tools.assembler.AsmUtil;
import jtcemu.platform.fx.Main;
import jtcemu.platform.fx.base.AppTab;
import jtcemu.platform.fx.base.GUIUtil;
import jtcemu.platform.fx.base.ReplyDlg;
import z8.Z8Memory;
import z8.Z8Reassembler;


public class ReassNode extends VBox implements AppTab
{
  private static final String FILE_GROUP_REASS  = "reass";
  private static final String FILE_GROUP_SOURCE = "reass_to_source";
  private static final String LABEL_BEG_ADDR    = "Anfangsadresse:";
  private static final String LABEL_END_ADDR    = "Endadresse:";

  private static ReassNode instance = null;

  private Main          main;
  private File          file;
  private File          sourceFile;
  private String        labelPrefix;
  private TextFinder    textFinder;
  private Z8Reassembler z8Reass;
  private boolean       focusListenerMissing;
  private int           begAddr;
  private int           endAddr;
  private MenuBar       mnuBar;
  private MenuItem      mnuSaveAs;
  private MenuItem      mnuPrint;
  private MenuItem      mnuSourceCopy;
  private MenuItem      mnuSourceSaveAs;
  private MenuItem      mnuCopy;
  private MenuItem      mnuFind;
  private MenuItem      mnuFindNext;
  private MenuItem      mnuFindPrev;
  private MenuItem      mnuSelectAll;
  private TextField     fldBegAddr;
  private TextField     fldEndAddr;
  private TextArea      fldResult;


  public static void reset()
  {
    if( instance != null )
      instance.updView();
  }


  public static void showTab( Main main )
  {
    if( instance == null ) {
      instance = new ReassNode( main );
    }
    main.showTab( "Reassembler", instance, true );
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
    /*
     * Da im Konstruktor die Scene noch nicht gesetzt ist,
     * wird der Focus-Listener erst hier angehaengt.
     */
    if( this.focusListenerMissing ) {
      Scene scene = getScene();
      if( scene != null ) {
	scene.focusOwnerProperty().addListener(
					(ov,o,n)->selectedTextChanged() );
	selectedTextChanged();
	this.focusListenerMissing = false;
      }
    }
    if( GUIUtil.hasSelection( this.fldResult ) ) {
      this.fldResult.requestFocus();
    } else {
      this.fldBegAddr.requestFocus();
    }
  }


	/* --- Konstruktor --- */

  private ReassNode( Main main )
  {
    this.main                 = main;
    this.labelPrefix          = "M_";
    this.textFinder           = new TextFinder();
    this.file                 = null;
    this.sourceFile           = null;
    this.z8Reass              = null;
    this.focusListenerMissing = true;
    this.begAddr              = -1;
    this.endAddr              = -1;


    // Menue Datei
    MenuItem mnuReass = GUIUtil.createShortcutMenuItem(
						"Reassemblieren",
						"R",
						false );

    this.mnuSaveAs = GUIUtil.createShortcutMenuItem(
						"Speichern unter...",
						"S",
						false );
    this.mnuSaveAs.setDisable( true );

    this.mnuPrint = GUIUtil.createShortcutMenuItem(
						"Drucken...",
						"P",
						false );
    this.mnuPrint.setDisable( true );

    this.mnuSourceCopy = new MenuItem( "kopieren" );
    this.mnuSourceCopy.setDisable( true );

    this.mnuSourceSaveAs = new MenuItem( "speichern unter..." );
    this.mnuSourceSaveAs.setDisable( true );

    Menu mnuSource = new Menu( "Als Assembler-Quelltext" );
    mnuSource.getItems().addAll(
				this.mnuSourceCopy,
				this.mnuSourceSaveAs );

    Menu mnuFile = new Menu( "Datei" );
    mnuFile.getItems().addAll(
			mnuReass,
			this.mnuSaveAs,
			new SeparatorMenuItem(),
			this.mnuPrint,
			mnuSource );


    // Menue Bearbeiten
    this.mnuCopy = GUIUtil.createShortcutMenuItem(
						"Kopieren",
						"C",
						false );
    this.mnuCopy.setDisable( true );

    this.mnuFind = GUIUtil.createShortcutMenuItem(
						"Suchen...",
						"F",
						false );
    this.mnuFind.setDisable( true );

    this.mnuFindNext = GUIUtil.createShortcutMenuItem(
						"Weitersuchen",
						"F",
						true );
    this.mnuFindNext.setDisable( true );

    this.mnuFindPrev = new MenuItem( "R\u00FCckw\u00E4rts suchen" );
    this.mnuFindPrev.setDisable( true );

    this.mnuSelectAll = new MenuItem( "Alles markieren" );
    this.mnuSelectAll.setDisable( true );

    Menu mnuEdit = new Menu( "Bearbeiten" );
    mnuEdit.getItems().addAll(
			this.mnuCopy,
			new SeparatorMenuItem(),
			this.mnuFind,
			this.mnuFindNext,
			this.mnuFindPrev,
			new SeparatorMenuItem(),
			this.mnuSelectAll );


    // Menueleiste
    this.mnuBar = new MenuBar();
    this.mnuBar.getMenus().addAll( mnuFile, mnuEdit );
    GUIUtil.completeMenuBar( main, this.mnuBar, true );


    // Fensterinhalt
    this.fldBegAddr = new TextField();
    this.fldBegAddr.setPrefColumnCount( 4 );
    this.fldBegAddr.setPadding( new Insets( 0.0, 10.0, 0.0, 0.0 ) );

    this.fldEndAddr = new TextField();
    this.fldEndAddr.setPrefColumnCount( 4 );
    this.fldEndAddr.setPadding( new Insets( 0.0, 10.0, 0.0, 0.0 ) );

    HBox hbox = new HBox(
			5,
			new Label( "Anfangsadresse:" ),
			this.fldBegAddr,
			new Label( "Endadresse:" ),
			this.fldEndAddr,
			GUIUtil.createImageButton(
					"/images/file/reload.png",
					"Reassemblieren",
					e->doReassemble() ) );
    hbox.setAlignment( Pos.CENTER_LEFT );
    hbox.setFillHeight( false );

    this.fldResult = new TextArea();
    this.fldResult.setPrefColumnCount( 1 );
    this.fldResult.setPrefRowCount( 1 );
    this.fldResult.setEditable( false );
    this.fldResult.setWrapText( false );
    Font font = Font.font( "Monospaced" );
    if( font != null ) {
      this.fldResult.setFont( font );
    }
    getChildren().addAll( hbox, this.fldResult );

    setMargin( hbox, new Insets( 5 ) );
    setPadding( new Insets( 5 ) );
    setFillWidth( true );
    setVgrow( this.fldResult, Priority.ALWAYS );


    // Aktionen
    mnuReass.setOnAction( e->doReassemble() );
    this.mnuSaveAs.setOnAction( e->doSaveAs() );
    this.mnuPrint.setOnAction( e->doPrint() );
    this.mnuSourceCopy.setOnAction( e->doSourceCopy() );
    this.mnuSourceSaveAs.setOnAction( e->doSourceSaveAs() );
    this.mnuCopy.setOnAction( e->doCopy() );
    this.mnuFind.setOnAction( e->doFind() );
    this.mnuFindNext.setOnAction( e->doFindNext() );
    this.mnuFindPrev.setOnAction( e->doFindPrev() );
    this.mnuSelectAll.setOnAction( e->doSelectAll() );
    this.fldBegAddr.setOnAction( e->requestFocusToEndAddr() );
    this.fldBegAddr.selectionProperty().addListener(
					(ov,o,n)->selectedTextChanged() );
    this.fldEndAddr.setOnAction( e->doReassemble() );
    this.fldEndAddr.selectionProperty().addListener(
					(ov,o,n)->selectedTextChanged() );
    this.fldResult.selectionProperty().addListener(
					(ov,o,n)->selectedTextChanged() );
  }


	/* --- Aktionen --- */

  /*
   * Da nach dem Umschalten der Menueleiste die alten Tastenkombinationen,
   * sofern sie durch die neue Menuleiste nicht ueberschrieben wurden,
   * weiterhin aktiv sind, wird vor jeder Aktion geprueft,
   * ob die eigene Menueleiste aktiv ist.
   */
  private void doCopy()
  {
    if( this.main.isCurMenuBar( this.mnuBar ) ) {
      TextInputControl tic = getFocusedTextInputControl();
      if( tic != null ) {
	tic.copy();
      }
    }
  }


  private void doFind()
  {
    if( this.main.isCurMenuBar( this.mnuBar ) ) {
      this.textFinder.openFindDlg( this.main.getStage(), this.fldResult );
      if( textFinder.hasSearchText() ) {
	this.mnuFindPrev.setDisable( false );
	this.mnuFindNext.setDisable( false );
      }
    }
  }


  private void doFindNext()
  {
    if( this.main.isCurMenuBar( this.mnuBar ) )
      this.textFinder.findNext( this.main.getStage(), this.fldResult );
  }


  private void doFindPrev()
  {
    if( this.main.isCurMenuBar( this.mnuBar ) )
      this.textFinder.findPrev( this.main.getStage(), this.fldResult );
  }


  private void doPrint()
  {
    if( this.main.isCurMenuBar( this.mnuBar ) ) {
      PlainTextPrintDlg.showAndWait(
			this.main,
			this.fldResult.getText(),
			"Reassembler Listing" );
    }
  }


  private void doReassemble()
  {
    if( this.main.isCurMenuBar( this.mnuBar ) && (this.z8Reass == null) ) {
      Z8Memory memory = this.main.getJTCSys();
      if( memory != null ) {
	this.z8Reass = new Z8Reassembler( memory );
      }
    }
    if( this.z8Reass != null ) {
      try {
	this.begAddr = JTCUtil.parseHex4(
				this.fldBegAddr.getText(),
				LABEL_BEG_ADDR );
	this.fldBegAddr.setText( String.format( "%04X", this.begAddr ) );
	this.endAddr = JTCUtil.parseHex4(
				this.fldEndAddr.getText(),
				LABEL_END_ADDR );
	this.fldEndAddr.setText( String.format( "%04X", this.endAddr ) );
	if( this.endAddr < this.begAddr ) {
	  throw new UserInputException(
			"Endadresse ist kleiner als Anfangsadresse." );
	}
	updView();
      }
      catch( UserInputException ex ) {
	this.main.showError( ex );
      }
    }
  }


  private void doSaveAs()
  {
    if( this.main.isCurMenuBar( this.mnuBar ) ) {
      File file = saveTextFileAs(
			this.fldResult.getText(),
			"Reassembler-Listing speichern",
			this.file,
			FILE_GROUP_REASS,
			GUIUtil.createLstFileFilter(),
			GUIUtil.createTextFileFilter() );
      if( file != null ) {
	this.file = file;
      }
    }
  }


  private void doSelectAll()
  {
    if( this.main.isCurMenuBar( this.mnuBar ) ) {
      TextInputControl tic = getFocusedTextInputControl();
      if( tic != null ) {
	tic.selectAll();
      }
    }
  }

  private void doSourceCopy()
  {
    if( this.main.isCurMenuBar( this.mnuBar ) ) {
      String sourceText = createSourceText();
      if( sourceText != null ) {
	Map<DataFormat,Object> content = new HashMap<>();
	content.put( DataFormat.PLAIN_TEXT, sourceText );
	if( !Clipboard.getSystemClipboard().setContent( content ) ) {
	  this.main.showError(
		"Der erzeugte Assembler-Quelltext konnte nicht\n"
			+ "in die Zwischenablage kopiert werden." );
	}
      }
    }
  }


  private void doSourceSaveAs()
  {
    if( this.main.isCurMenuBar( this.mnuBar ) ) {
      String sourceText = createSourceText();
      if( sourceText != null ) {
	File file = saveTextFileAs(
			sourceText,
			"Assembler-Quelltext speichern",
			this.sourceFile,
			FILE_GROUP_SOURCE,
			GUIUtil.createAsmFileFilter(),
			GUIUtil.createTextFileFilter() );
	if( file != null ) {
	  this.sourceFile = file;
	}
      }
    }
  }


	/* --- private Methoden --- */

  private String createSourceText()
  {
    String text = null;
    if( (this.z8Reass != null)
        && (this.begAddr >= 0) && (this.begAddr < this.endAddr) )
    {
      String labelPrefix = ReplyDlg.showReplyTextDlg(
					this.main.getStage(),
					"Prefix f\u00FCr Marken:",
					this.labelPrefix );
      if( labelPrefix != null ) {
	boolean prefixOK = false;
	int     len      = labelPrefix.length();
	if( len > 0 ) {
	  prefixOK = AsmUtil.isIdentifierStart( labelPrefix.charAt( 0 ) );
	  for( int i = 1; i < len; i++ ) {
	    prefixOK &= AsmUtil.isIdentifierPart( labelPrefix.charAt( i ) );
	  }
	}
	if( prefixOK ) {
	  this.labelPrefix = labelPrefix;
	  text             = this.z8Reass.reassembleToSource(
							this.begAddr,
							this.endAddr,
							labelPrefix );
	} else {
	  this.main.showError(
		"Der Pr\u00E4fix entspricht nicht den Namenskonventionen"
			+ " f\u00FCr Assembler-Marken." );
	}
      }
    }
    return text;
  }


  private TextInputControl getFocusedTextInputControl()
  {
    TextInputControl tic   = null;
    Scene            scene = getScene();
    if( scene != null ) {
      Node node = scene.getFocusOwner();
      if( node != null ) {
	if( node instanceof TextInputControl ) {
	  tic = (TextInputControl) node;
	}
      }
    }
    return tic;
  }


  private void requestFocusToEndAddr()
  {
    if( this.fldEndAddr != null )
      this.fldEndAddr.requestFocus();
  }


  private File saveTextFileAs(
			String                         text,
			String                         title,
			File                           oldFile,
			String                         fileGroup,
			FileChooser.ExtensionFilter... fileFilters )
  {
    FileChooser fileChooser = GUIUtil.prepareFileChooser(
							title,
							oldFile,
							fileGroup );
    fileChooser.getExtensionFilters().addAll( fileFilters );
    file = fileChooser.showSaveDialog( this.main.getStage() );
    if( file != null ) {
      try {
	BufferedWriter out = null;
	try {
	  out     = new BufferedWriter( new FileWriter( file ) );
	  int len = text.length();
	  for( int i = 0; i < len; i++ ) {
	    char ch = text.charAt( i );
	    if( ch == '\n' ) {
	      out.newLine();
	    } else {
	      out.write( ch );
	    }
	  }
	  out.close();
	  out = null;
	  AppContext.setLastFile( fileGroup, file );
	}
	finally {
	  JTCUtil.closeSilently( out );
	}
      }
      catch( IOException ex ) {
	this.main.showError( ex );
	file = null;
      }
    }
    return file;
  }


  private void selectedTextChanged()
  {
    IndexRange range = null;
    if( this.fldBegAddr.isFocused() ) {
      range = this.fldBegAddr.getSelection();
    } else if( this.fldEndAddr.isFocused() ) {
      range = this.fldEndAddr.getSelection();
    } else if( this.fldResult.isFocused() ) {
      range = this.fldResult.getSelection();
    }
    this.mnuCopy.setDisable( !GUIUtil.hasSelection( range ) );
  }


  private void updView()
  {
    if( (this.begAddr >= 0) && (this.begAddr <= this.endAddr) ) {
      String text = this.z8Reass.reassemble( this.begAddr, this.endAddr );
      this.fldResult.setText( text );
      this.fldResult.positionCaret( 0 );
      this.fldResult.requestFocus();
      if( !text.isEmpty() ) {
	this.mnuSaveAs.setDisable( false );
	this.mnuPrint.setDisable( false );
	this.mnuSourceCopy.setDisable( false );
	this.mnuSourceSaveAs.setDisable( false );
	this.mnuFind.setDisable( false );
	this.mnuSelectAll.setDisable( false );
      }
    }
  }
}
