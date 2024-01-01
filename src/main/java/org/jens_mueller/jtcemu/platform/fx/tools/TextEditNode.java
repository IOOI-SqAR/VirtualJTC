/*
 * (c) 2014-2021 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Inhalt des Texteditor-Tabs
 */

package org.jens_mueller.jtcemu.platform.fx.tools;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.util.Properties;
import java.util.TimerTask;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.IndexRange;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Separator;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Skin;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToolBar;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import org.jens_mueller.jtcemu.base.AppContext;
import org.jens_mueller.jtcemu.base.JTCSys;
import org.jens_mueller.jtcemu.base.JTCUtil;
import org.jens_mueller.jtcemu.platform.fx.JTCEMUApplication;
import org.jens_mueller.jtcemu.platform.fx.base.AppTab;
import org.jens_mueller.jtcemu.platform.fx.base.DropFileHandler;
import org.jens_mueller.jtcemu.platform.fx.base.GUIUtil;
import org.jens_mueller.jtcemu.platform.fx.base.MsgDlg;
import org.jens_mueller.jtcemu.platform.fx.base.ReplyDlg;
import org.jens_mueller.jtcemu.platform.fx.tools.assembler.AsmOptionsDlg;
import org.jens_mueller.jtcemu.tools.BasicParser;
import org.jens_mueller.jtcemu.tools.BasicUtil;
import org.jens_mueller.jtcemu.tools.TextOutput;
import org.jens_mueller.jtcemu.tools.ToolUtil;
import org.jens_mueller.jtcemu.tools.assembler.AsmOptions;
import org.jens_mueller.jtcemu.tools.assembler.AsmUtil;


public class TextEditNode
		extends BorderPane
		implements AppTab, DropFileHandler
{
  private static final String DEFAULT_STATUS_TEXT    = "Bereit";
  private static final long   STATUS_SHOW_MSG_MILLIS = 5000;
  private static final int    DEFAULT_FONT_SIZE      = 13;
  private static final String PROP_FONT_SIZE         = "textedit.font.size";
  private static final String FILE_GROUP_TEXT        = "text";
  private static final String FILE_GROUP_PROJECT     = "project";
  private static final String LABEL_BEG_ADDR_OF_BASIC_PRG =
				"Anfangsadresse des BASIC-Programms:";

  private static final String ACTION_NEW   = "Neuer Text";
  private static final String ACTION_OPEN  = "\u00D6ffnen...";
  private static final String ACTION_SAVE  = "Speichern";
  private static final String ACTION_PRINT = "Drucken";
  private static final String ACTION_CUT   = "Ausschnieden";
  private static final String ACTION_COPY  = "Kopieren";
  private static final String ACTION_PASTE = "Einf\u00FCgen";
  private static final String ACTION_FIND  = "Suchen...";

  private static TextEditNode instance = null;

  private JTCEMUApplication JTCEMUApplication;
  private Tab               tab;
  private File              prjFile;
  private File              textFile;
  private TextFinder        textFinder;
  private Node              contextClickNode;
  private Point2D           contextClickPoint;
  private String            eol;
  private AsmOptions        asmOptions;
  private Integer           basicAddrTransfer;
  private int               basicAddrFetch;
  private boolean           dataChanged;
  private boolean           focusListenerMissing;
  private Button            btnSave;
  private Button            btnCut;
  private Button            btnCopy;
  private ComboBox<Integer> comboFontSize;
  private MenuItem          mnuContextLogCopy;
  private MenuItem          mnuContextLogToEditLine;
  private MenuItem          mnuSave;
  private MenuItem          mnuCut;
  private MenuItem          mnuCopy;
  private MenuItem          mnuPaste;
  private MenuItem          mnuReplace;
  private MenuItem          mnuFindNext;
  private MenuItem          mnuFindPrev;
  private MenuBar           mnuBar;
  private TextArea          fldEdit;
  private TextArea          fldLog;
  private Label             statusBar;
  private long              statusTimerMillis;
  private TimerTask         statusTimerTask;


  public static void showTab( JTCEMUApplication JTCEMUApplication)
  {
    if( instance == null ) {
      instance = new TextEditNode(JTCEMUApplication);
    }
    JTCEMUApplication.showTab( "Texteditor", instance, true );
  }


	/* --- Konstruktor --- */

  private TextEditNode( JTCEMUApplication JTCEMUApplication)
  {
    this.JTCEMUApplication = JTCEMUApplication;
    this.textFinder           = new TextFinder();
    this.tab                  = null;
    this.prjFile              = null;
    this.textFile             = null;
    this.contextClickNode     = null;
    this.contextClickPoint    = null;
    this.eol                  = null;
    this.asmOptions           = null;
    this.basicAddrTransfer    = null;
    this.basicAddrFetch       = JTCSys.DEFAULT_BASIC_ADDR;
    this.dataChanged          = false;
    this.focusListenerMissing = true;

    // Menue Datei
    MenuItem mnuNew = GUIUtil.createShortcutMenuItem(
						ACTION_NEW,
						"N",
						false );
    mnuNew.setOnAction( e->doNew() );

    MenuItem mnuOpen = GUIUtil.createShortcutMenuItem(
						ACTION_OPEN,
						"O",
						false );
    mnuOpen.setOnAction( e->doOpen() );

    MenuItem mnuLoadBasicFromMem = new MenuItem(
		String.format(
			"BASIC-Programm aus Arbeitsspeicher ab %%%04X laden",
			JTCSys.DEFAULT_BASIC_ADDR ) );
    mnuLoadBasicFromMem.setOnAction(
		e->doLoadBasicMem( JTCSys.DEFAULT_BASIC_ADDR ) );

    MenuItem mnuLoadBasicFromMemWith = new MenuItem(
			"BASIC-Programm aus Arbeitsspeicher laden..." );
    mnuLoadBasicFromMemWith.setOnAction( e->doLoadBasicMem( null ) );

    this.mnuSave = GUIUtil.createShortcutMenuItem(
						ACTION_SAVE,
						"S",
						false );
    this.mnuSave.setOnAction( e->doSave( false ) );

    MenuItem mnuSaveAs = GUIUtil.createShortcutMenuItem(
						"Speichern unter...",
						"S",
						true );
    mnuSaveAs.setOnAction( e->doSave( true ) );

    MenuItem mnuPrint = GUIUtil.createShortcutMenuItem(
						ACTION_PRINT,
						"P",
						false );
    mnuPrint.setOnAction( e->doPrint() );

    Menu mnuFile = new Menu( "Datei" );
    mnuFile.getItems().addAll(
			mnuNew,
			mnuOpen,
			new SeparatorMenuItem(),
			mnuLoadBasicFromMem,
			mnuLoadBasicFromMemWith,
			new SeparatorMenuItem(),
			this.mnuSave,
			mnuSaveAs,
			new SeparatorMenuItem(),
			mnuPrint );

    // Menue Bearbeiten
    this.mnuCut = GUIUtil.createShortcutMenuItem(
						ACTION_CUT,
						"X",
						false );
    this.mnuCut.setOnAction( e->doCut() );

    this.mnuCopy = GUIUtil.createShortcutMenuItem(
						ACTION_COPY,
						"C",
						false );
    this.mnuCopy.setOnAction( e->doCopy() );

    this.mnuPaste = GUIUtil.createShortcutMenuItem(
						ACTION_PASTE,
						"V",
						false );
    this.mnuPaste.setOnAction( e->doPaste() );

    MenuItem mnuFind = GUIUtil.createShortcutMenuItem(
						ACTION_FIND,
						"F",
						false );
    mnuFind.setOnAction( e->doFindAndReplace() );

    this.mnuReplace = GUIUtil.createShortcutMenuItem(
						"Ersetzen",
						"R",
						false );
    this.mnuReplace.setDisable( true );
    this.mnuReplace.setOnAction( e->doReplace() );

    this.mnuFindNext = GUIUtil.createShortcutMenuItem(
						"Weitersuchen",
						"F",
						true );
    this.mnuFindNext.setDisable( true );
    this.mnuFindNext.setOnAction( e->doFindNext() );

    this.mnuFindPrev = new MenuItem( "R\u00FCckw\u00E4rts suchen" );
    this.mnuFindPrev.setDisable( true );
    this.mnuFindPrev.setOnAction( e->doFindPrev() );

    MenuItem mnuGoto = GUIUtil.createShortcutMenuItem(
						"Gehe zu Zeile...",
						"G",
						false );
    mnuGoto.setOnAction( e->doGoto() );

    MenuItem mnuSelectAll = new MenuItem( "Alles markieren" );
    mnuSelectAll.setOnAction( e->doSelectAll() );

    Menu mnuEdit = new Menu( "Bearbeiten" );
    mnuEdit.getItems().addAll(
			this.mnuCut,
			this.mnuCopy,
			this.mnuPaste,
			new SeparatorMenuItem(),
			mnuFind,
			this.mnuReplace,
			this.mnuFindNext,
			this.mnuFindPrev,
			new SeparatorMenuItem(),
			mnuGoto,
			mnuSelectAll );

    // Programmierung
    MenuItem mnuAssemble = GUIUtil.createShortcutMenuItem(
						"Assembliere",
						"M",
						false );
    mnuAssemble.setOnAction( e->doPrgAssemble( false ) );

    MenuItem mnuAssembleWith = GUIUtil.createShortcutMenuItem(
						"Assembliere mit...",
						"M",
						true );
    mnuAssembleWith.setOnAction( e->doPrgAssemble( true ) );

    MenuItem mnuAsmPrjOpen = new MenuItem(
			"Assembler-Projekt \u00F6ffnen..." );
    mnuAsmPrjOpen.setOnAction( e->doPrgAsmPrjOpen() );

    MenuItem mnuAsmPrjSave = new MenuItem( "Assembler-Projekt speichern" );
    mnuAsmPrjSave.setOnAction( e->doPrgAsmPrjSave( false ) );

    MenuItem mnuAsmPrjSaveAs = new MenuItem(
			"Assembler-Projekt speichern unter..." );
    mnuAsmPrjSaveAs.setOnAction( e->doPrgAsmPrjSave( true ) );

    MenuItem mnuBasicParse = GUIUtil.createShortcutMenuItem(
			"BASIC-Programm syntaktisch pr\u00FCfen",
			"B",
			false );
    mnuBasicParse.setOnAction( e->doPrgBasic( false, false ) );

    MenuItem mnuBasicIntoEmu = GUIUtil.createShortcutMenuItem(
			"BASIC-Programm in Arbeitsspeicher laden",
			"B",
			true );
    mnuBasicIntoEmu.setOnAction( e->doPrgBasic( true, false ) );

    MenuItem mnuBasicIntoEmuOpt = new MenuItem(
			"BASIC-Programm in Arbeitsspeicher laden mit..." );
    mnuBasicIntoEmuOpt.setOnAction( e->doPrgBasic( true, true ) );

    Menu mnuPrg = new Menu( "Programmierung" );
    mnuPrg.getItems().addAll(
			mnuAssemble,
			mnuAssembleWith,
			new SeparatorMenuItem(),
			mnuAsmPrjOpen,
			mnuAsmPrjSave,
			mnuAsmPrjSaveAs,
			new SeparatorMenuItem(),
			mnuBasicParse,
			mnuBasicIntoEmu,
			mnuBasicIntoEmuOpt );

    // Menueleiste
    this.mnuBar = new MenuBar();
    this.mnuBar.getMenus().addAll( mnuFile, mnuEdit, mnuPrg );
    GUIUtil.completeMenuBar(JTCEMUApplication, this.mnuBar, true );


    // Werkzeugleiste
    Button btnNew = GUIUtil.createToolBarButton(
					"/images/file/new.png",
					ACTION_NEW,
					e->doNew() );
    Button btnOpen = GUIUtil.createToolBarButton(
					"/images/file/open.png",
					ACTION_OPEN,
					e->doOpen() );
    this.btnSave = GUIUtil.createToolBarButton(
					"/images/file/save.png",
					ACTION_SAVE,
					e->doSave( false ) );
    Button btnPrint = GUIUtil.createToolBarButton(
					"/images/file/print.png",
					ACTION_PRINT,
					e->doPrint() );
    this.btnCut = GUIUtil.createToolBarButton(
					"/images/edit/cut.png",
					ACTION_CUT,
					e->doCut() );
    this.btnCopy = GUIUtil.createToolBarButton(
					"/images/edit/copy.png",
					ACTION_COPY,
					e->doCopy() );
    Button btnPaste = GUIUtil.createToolBarButton(
					"/images/edit/paste.png",
					ACTION_PASTE,
					e->doPaste() );
    Button btnFind = GUIUtil.createToolBarButton(
					"/images/edit/find.png",
					ACTION_FIND,
					e->doFindAndReplace() );
    this.comboFontSize = new ComboBox<>();
    this.comboFontSize.setEditable( false );
    this.comboFontSize.getItems().addAll(
		10, 11, 12, 13, 14, 15, 16, 18, 20, 22, 24 );

    ToolBar toolBar = new ToolBar(
			btnNew,
			btnOpen,
			this.btnSave,
			new Separator(),
			btnPrint,
			new Separator(),
			this.btnCut,
			this.btnCopy,
			btnPaste,
			new Separator(),
			btnFind,
			new Separator(),
			new Label( "Schriftgr\u00F4\u00DFe:" ),
			comboFontSize );
    setTop( toolBar );


    // Fensterinhalt
    this.fldEdit = new TextArea();
    this.fldEdit.setPrefColumnCount( 1 );
    this.fldEdit.setPrefRowCount( 1 );
    this.fldEdit.setWrapText( false );

    this.fldLog = new TextArea();
    this.fldLog.setEditable( false );
    this.fldLog.setPrefColumnCount( 1 );
    this.fldLog.setPrefRowCount( 1 );
    this.fldLog.setWrapText( false );
    this.fldLog.setOnMouseClicked( e->mouseOnLogClicked( e ) );

    SplitPane splitPane = new SplitPane( this.fldEdit, this.fldLog );
    splitPane.setOrientation( Orientation.VERTICAL );
    splitPane.setResizableWithParent( this.fldEdit, true );
    splitPane.setResizableWithParent( this.fldLog, false );
    splitPane.setDividerPositions( 0.8 );
    setCenter( splitPane );


    // Statuszeile
    this.statusBar = new Label( DEFAULT_STATUS_TEXT );
    this.statusBar.setAlignment( Pos.CENTER_LEFT );
    this.statusBar.setPadding( new Insets( 5 ) );
    setBottom( this.statusBar );
    installStatusTimerTask( STATUS_SHOW_MSG_MILLIS );


    // Kontextmenue
    this.mnuContextLogCopy = new MenuItem( "Kopieren" );
    this.mnuContextLogCopy.setOnAction( e->this.fldLog.copy() );

    this.mnuContextLogToEditLine = new MenuItem(
					"Gehe zur betreffenden Zeile" );
    this.mnuContextLogToEditLine.setOnAction(
					e->contextLogGotoEditLine() );

    MenuItem mnuContextLogSelectAll = new MenuItem( "Alles markieren" );
    mnuContextLogSelectAll.setOnAction( e->this.fldLog.selectAll() );

    this.fldLog.setContextMenu( new ContextMenu(
					this.mnuContextLogCopy,
					new SeparatorMenuItem(),
					this.mnuContextLogToEditLine,
					new SeparatorMenuItem(),
					mnuContextLogSelectAll ) );

    this.fldLog.setOnContextMenuRequested(
			e->contextLogRequested( e, this.fldLog ) );


    // Schriftgroesse einstellen
    int fontSize = AppContext.getIntProperty( PROP_FONT_SIZE, -1 );
    if( fontSize < 10 ) {
      Font font = this.fldEdit.getFont();
      if( font != null ) {
	fontSize = (int) font.getSize();
	if( this.comboFontSize.getItems().contains( fontSize ) ) {
	  fontSize = -1;
	}
      }
    }
    if( fontSize < 10 ) {
      fontSize = DEFAULT_FONT_SIZE;
    }
    this.comboFontSize.setValue( fontSize );
    setMonospacedFont( fontSize );
    this.comboFontSize.setOnAction( e->fontSizeChanged() );

    // Laden einer Datei ueber Drag&Drop ermoeglichen
    this.fldEdit.setOnDragOver( e->GUIUtil.handleFileDragOver( e ) );
    this.fldEdit.setOnDragDropped(
			e->GUIUtil.handleFileDragDropped( e, this ) );

    // Listener, wenn sich die Cursor-Position geaendefrt hat
    this.fldEdit.caretPositionProperty().addListener(
				(ov,o,n)->updStatusText() );

    // Listener, wenn sich der markierte Text geaendert hat
    this.fldEdit.selectedTextProperty().addListener(
				(ov,o,n)->selectedTextChanged() );
    this.fldLog.selectedTextProperty().addListener(
				(ov,o,n)->selectedTextChanged() );
    selectedTextChanged();

    // Listener, wenn der Text geaendert wird
    this.fldEdit.textProperty().addListener(
				(ov,o,n)->setDataChanged( true ) );
    fireDataUnchanged();
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
    StringBuilder buf = new StringBuilder( 256 );
    buf.append( AppContext.getAppName() );
    buf.append( " Texteditor" );

    if( this.textFile != null ) {
      String fileName = this.textFile.getName();
      if( fileName != null ) {
	if( !fileName.isEmpty() ) {
	  buf.append( ": " );
	  buf.append( fileName );
	}
      }
    } else {
      buf.append( ": Neuer Text" );
    }
    return buf.toString();
  }


  @Override
  public void setTab( Tab tab )
  {
    this.tab = tab;
  }


  @Override
  public void tabClosed()
  {
    instance = null;
  }


  @Override
  public boolean tabCloseRequest( Event e )
  {
    boolean rv = confirmDataSaved();
    if( !rv && (e != null) ) {
      e.consume();
    }
    return rv;
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
	scene.focusOwnerProperty().addListener( (ov,o,n)->focusChanged() );
	focusChanged();
	this.focusListenerMissing = false;
      }
    }
    if( GUIUtil.hasSelection( this.fldLog ) ) {
      this.fldLog.requestFocus();
    } else {
      this.fldEdit.requestFocus();
    }
  }


	/* --- DropFileHandler --- */

  @Override
  public boolean handleDroppedFile( Object target, final File file )
  {
    boolean success = false;
    if( target == this.fldEdit ) {
      // nicht auf Benutzereingabe warten
      Platform.runLater( ()->checkDataSavedAndLoadFile( file ) );
      success = true;
    }
    return success;
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
    if( this.JTCEMUApplication.isCurMenuBar( this.mnuBar ) ) {
      if( this.fldEdit.isFocused() ) {
	this.fldEdit.copy();
      } else if( this.fldLog.isFocused() ) {
	this.fldLog.copy();
      }
    }
  }


  private void doCut()
  {
    if( this.JTCEMUApplication.isCurMenuBar( this.mnuBar ) ) {
      if( this.fldEdit.isFocused() ) {
	this.fldEdit.cut();
      }
    }
  }


  private void doFindAndReplace()
  {
    if( this.JTCEMUApplication.isCurMenuBar( this.mnuBar ) ) {
      if( this.fldLog.isFocused() ) {
	this.textFinder.openFindDlg( this.JTCEMUApplication.getStage(), this.fldLog );
      } else {
	this.textFinder.openFindAndReplaceDlg(
					this.JTCEMUApplication.getStage(),
					this.fldEdit );
      }
      if( textFinder.hasSearchText() ) {
	this.mnuFindPrev.setDisable( false );
	this.mnuFindNext.setDisable( false );
      }
    }
  }


  private void doFindNext()
  {
    if( this.JTCEMUApplication.isCurMenuBar( this.mnuBar ) ) {
      this.textFinder.findNext(
		this.JTCEMUApplication.getStage(),
		this.fldLog.isFocused() ? this.fldLog : this.fldEdit );
    }
  }


  private void doFindPrev()
  {
    if( this.JTCEMUApplication.isCurMenuBar( this.mnuBar ) ) {
      this.textFinder.findPrev(
		this.JTCEMUApplication.getStage(),
		this.fldLog.isFocused() ? this.fldLog : this.fldEdit );
    }
  }


  private void doGoto()
  {
    if( this.JTCEMUApplication.isCurMenuBar( this.mnuBar ) ) {
      int    curLineNum = 1;
      String text       = this.fldEdit.getText();
      if( text != null ) {
	int e = Math.min( this.fldEdit.getCaretPosition(), text.length() );
	for( int i = 0; i < e; i++ ) {
	  if( text.charAt( i ) == '\n' ) {
	    curLineNum++;
	  }
	}
      }
      Integer lineNum = ReplyDlg.showReplyDecDlg(
				this.JTCEMUApplication.getStage(),
				"Zeile:",
				1,
				curLineNum );
      if( lineNum != null ) {
	gotoLine( lineNum.intValue() );
      }
    }
  }


  private void doLoadBasicMem( Integer addr )
  {
    if( this.JTCEMUApplication.isCurMenuBar( this.mnuBar ) && confirmDataSaved() ) {
      if( addr == null ) {
	addr = ReplyDlg.showReplyHex4Dlg(
			this.JTCEMUApplication.getStage(),
			LABEL_BEG_ADDR_OF_BASIC_PRG,
			this.basicAddrFetch );
      }
      if( addr != null ) {
	String text = BasicUtil.getBasicProgramTextFromMemory(
						this.JTCEMUApplication.getJTCSys(),
						addr.intValue() );
	if( text != null ) {
	  clear();
	  setEditText( text );
	  this.basicAddrFetch = addr;
	  showStatusText( "BASIC-Programm aus Arbeitsspeicher geladen" );
	  fireDataUnchanged();
	} else {
	  this.JTCEMUApplication.showError(
		"An der angegebenen Adresse im Arbeitsspeicher\n"
			+ "befindet sich kein BASIC-Programm." );
	}
      }
    }
  }


  private void doNew()
  {
    if( this.JTCEMUApplication.isCurMenuBar( this.mnuBar ) && confirmDataSaved() ) {
      clear();
      fireDataUnchanged();
    }
  }


  private void doOpen()
  {
    if( this.JTCEMUApplication.isCurMenuBar( this.mnuBar ) && confirmDataSaved() ) {
      FileChooser fileChooser = prepareTextFileChooser(
						"Datei \u00F6ffnen",
						null );
      loadFile( fileChooser.showOpenDialog( this.JTCEMUApplication.getStage() ) );
    }
  }


  private void doPaste()
  {
    if( this.JTCEMUApplication.isCurMenuBar( this.mnuBar ) && this.fldEdit.isFocused() )
      this.fldEdit.paste();
  }


  private void doPrgAssemble( boolean askOpt )
  {
    if( this.JTCEMUApplication.isCurMenuBar( this.mnuBar ) ) {
      AsmOptions options = this.asmOptions;
      if( askOpt || (options == null) ) {
	options = AsmOptionsDlg.open( this.JTCEMUApplication, options );
      }
      if( options != null ) {
	this.asmOptions = options;
	this.fldLog.setText( "" );
	AsmUtil.assemble(
			this.fldEdit.getText(),
			null,
			options,
			this.JTCEMUApplication.getJTCSys(),
			createTextOutput( this.fldLog ) );
      }
    }
  }


  private void doPrgAsmPrjOpen()
  {
    if( this.JTCEMUApplication.isCurMenuBar( this.mnuBar ) && confirmDataSaved() ) {
      FileChooser fileChooser = preparePrjFileChooser(
					"Assembler-Pojekt \u00F6ffnen" );
      File dirFile = AppContext.getLastDirFile( FILE_GROUP_PROJECT );
      if( dirFile != null ) {
	fileChooser.setInitialDirectory( dirFile );
      }
      File file = fileChooser.showOpenDialog( this.JTCEMUApplication.getStage() );
      if( file != null ) {
	try {
	  loadProjectFile( file );
	}
	catch( IOException ex ) {
	  this.JTCEMUApplication.showError( ex );
	}
      }
    }
  }


  private boolean doPrgAsmPrjSave( boolean forceFileDlg )
  {
    boolean rv = false;
    if( this.JTCEMUApplication.isCurMenuBar( this.mnuBar ) ) {
      try {
	AsmUtil.ensureSourceFileExists( this.textFile );

	File prjFile = this.prjFile;
	if( forceFileDlg || (prjFile == null) ) {
	  FileChooser fileChooser = preparePrjFileChooser(
					"Assembler-Pojekt speichern" );
	  File preSelection = AsmUtil.getProjectFilePreSelection(
							prjFile,
							this.textFile );
	  if( preSelection != null ) {
	    File dirFile = preSelection.getParentFile();
	    if( dirFile != null ) {
	      fileChooser.setInitialDirectory( dirFile );
	    }
	    String fileName = preSelection.getName();
	    if( fileName != null ) {
	      fileChooser.setInitialFileName( fileName );
	    }
	  }
	  prjFile = fileChooser.showSaveDialog( this.JTCEMUApplication.getStage() );
	}
	if( prjFile != null ) {
	  if( saveEditTextToFile( this.textFile ) ) {
	    AsmUtil.saveProject( prjFile, this.textFile, this.asmOptions );
	    this.prjFile = prjFile;
	    AppContext.setLastFile( FILE_GROUP_PROJECT, prjFile );
	    showStatusText( "Projekt gespeichert" );
	    fireDataUnchanged();
	    rv = true;
	  }
	}
      }
      catch( IOException ex ) {
	this.JTCEMUApplication.showError( ex );
      }
    }
    return rv;
  }


  private void doPrgBasic( boolean intoEmu, boolean askOpt )
  {
    if( this.JTCEMUApplication.isCurMenuBar( this.mnuBar ) ) {
      JTCSys jtcSys = this.JTCEMUApplication.getJTCSys();
      if( jtcSys != null ) {
	Integer addr = null;
	if( intoEmu ) {
	  addr = this.basicAddrTransfer;
	  if( askOpt || (addr == null) ) {
	    addr = ReplyDlg.showReplyHex4Dlg(
			this.JTCEMUApplication.getStage(),
			LABEL_BEG_ADDR_OF_BASIC_PRG,
			addr != null ? addr : JTCSys.DEFAULT_BASIC_ADDR );
	  }
	} else {
	  addr = Integer.valueOf( JTCSys.DEFAULT_BASIC_ADDR );
	}
	if( addr != null ) {
	  this.fldLog.setText( "" );
	  byte[] codeBytes = BasicParser.parse(
				this.JTCEMUApplication.getJTCSys(),
				addr.intValue(),
				this.fldEdit.getText(),
				createTextOutput( this.fldLog ) );
	  if( intoEmu && (codeBytes != null) ) {
	    if( codeBytes.length > 0 ) {
	      int a = addr.intValue();
	      for( int i = 0; i < codeBytes.length; i++ ) {
		jtcSys.setMemByte( a++, false, codeBytes[ i ] );
	      }
	      this.fldLog.appendText(
			String.format(
				"BASIC-Programm in Speicherbereich"
					+ " %04X-%04X geladen\n",
				addr,
				a - 1 ) );
	      this.basicAddrTransfer = addr;
	    }
	  }
	}
      }
    }
  }


  private void doReplace()
  {
    if( this.JTCEMUApplication.isCurMenuBar( this.mnuBar ) ) {
      String replaceText = this.textFinder.getReplaceText();
      if( replaceText != null ) {
	IndexRange range = this.fldEdit.getSelection();
	if( range != null ) {
	  int begPos = range.getStart();
	  int endPos = range.getEnd();
	  if( (begPos >= 0) && (begPos < endPos) ) {
	    this.fldEdit.replaceText( range, replaceText );
	  }
	}
      }
    }
  }


  private void doPrint()
  {
    if( this.JTCEMUApplication.isCurMenuBar( this.mnuBar ) ) {
      PlainTextPrintDlg.showAndWait(
		this.JTCEMUApplication,
		this.fldEdit.getText(),
		this.textFile != null ? this.textFile.getName() : null );
    }
  }


  private void doSave( boolean saveAs )
  {
    if( this.JTCEMUApplication.isCurMenuBar( this.mnuBar ) ) {
      File file = null;
      if( (file == null) || saveAs ) {
	FileChooser fileChooser = prepareTextFileChooser(
						"Textdatei speichern",
						file );
	if( file == null ) {
	  file = AppContext.getLastDirFile( FILE_GROUP_TEXT );
	}
	if( file != null ) {
	  String fileName = null;
	  if( !file.isDirectory() ) {
	    fileName = file.getName();
	    file     = file.getParentFile();
	  }
	  if( file != null ) {
	    fileChooser.setInitialDirectory( file );
	  }
	  if( fileName != null ) {
	    fileChooser.setInitialFileName( fileName );
	  }
	}
	file = fileChooser.showSaveDialog( this.JTCEMUApplication.getStage() );
      }
      if( file != null ) {
	saveEditTextToFile( file );
	showStatusText( "Datei gespeichert" );
      }
    }
  }


  private void doSelectAll()
  {
    if( this.JTCEMUApplication.isCurMenuBar( this.mnuBar ) ) {
      if( this.fldEdit.isFocused() ) {
	this.fldEdit.selectAll();
      } else if( this.fldLog.isFocused() ) {
	this.fldLog.selectAll();
      }
    }
  }


	/* --- private Methoden --- */

  private void checkDataSavedAndLoadFile( File file )
  {
    if( confirmDataSaved() ) {
      loadFile( file );
    }
  }

  private void clear()
  {
    this.fldEdit.clear();
    this.fldLog.clear();
    this.textFile = null;
    this.prjFile  = null;
    this.eol      = null;
    fireUpdTitle();
  }


  public boolean confirmDataSaved()
  {
    boolean rv = true;
    if( this.dataChanged ) {
      if( this.tab != null ) {
	this.JTCEMUApplication.selectTab( this.tab );
      }
      rv = MsgDlg.showYesNoWarningDlg(
		this.JTCEMUApplication.getStage(),
		"Der Text wurde ge\u00E4ndert, aber nicht gespeichert.\n"
			+ "Soll die Aktion trotzdem ausgef\u00FChrt"
			+ " werden?" );
    }
    return rv;
  }


  private void contextLogGotoEditLine()
  {
    if( (this.contextClickNode == this.fldLog)
	&& (this.contextClickPoint != null) )
    {
      int lineNum = getCorrespondingEditLineNum(
				this.contextClickPoint.getX(),
				this.contextClickPoint.getY() );
      if( lineNum > 0 ) {
	gotoLine( lineNum );
      }
    }
  }


  /*
   * Die Methode wird bei der Anforderung des Kontextmenus aufgerufen,
   * und dient dazu, nicht relevante Menueeintraege zu deaktivieren.
   * Die eigentlichen Anzeige wird dem System ueberlassen,
   * weshalb das Event nicht konsumiert werden darf.
   */
  private void contextLogRequested(
				ContextMenuEvent e,
				Node             node  )
  {
    this.contextClickNode  = node;
    this.contextClickPoint = new Point2D( e.getX(), e.getY() );

    if( mnuContextLogCopy != null ) {
      IndexRange selection = this.fldLog.getSelection();
      if( selection != null ) {
	this.mnuContextLogCopy.setDisable( selection.getLength() == 0 );
      } else {
	this.mnuContextLogCopy.setDisable( true);
      }
    }
    if( this.mnuContextLogToEditLine != null ) {
      this.mnuContextLogToEditLine.setDisable(
		getCorrespondingEditLineNum( e.getX(), e.getY() ) < 1 );
    }
  }


  private TextOutput createTextOutput( final TextArea textArea )
  {
    return new TextOutput()
		{
		  @Override
		  public void print( String text )
		  {
		    textArea.appendText( text );
		  }
		  @Override
		  public void println()
		  {
		    textArea.appendText( "\n" );
		  }
		};
  }


  private void fireDataUnchanged()
  {
    Platform.runLater( ()->setDataChanged( false ) );
  }


  private void fireUpdStatusText()
  {
    Platform.runLater( ()->updStatusText() );
  }


  private void fireUpdTitle()
  {
    Platform.runLater( ()->this.JTCEMUApplication.updWindowTitle() );
  }


  private void focusChanged()
  {
    selectedTextChanged();
    updStatusText();
    this.mnuPaste.setDisable( !this.fldEdit.isFocused() );
  }


  private void fontSizeChanged()
  {
    Integer fontSize = this.comboFontSize.getValue();
    if( fontSize != null ) {
      if( setMonospacedFont( fontSize.intValue() ) ) {
	AppContext.setProperty( PROP_FONT_SIZE, String.valueOf( fontSize ) );
      }
    }
  }


  private int getCorrespondingEditLineNum( double xClick, double yClick )
  {
    int    rv   = -1;
    String text = this.fldLog.getText();
    if( text != null ) {
      Skin<?> skin = this.fldLog.getSkin();
      if( skin != null ) {
	double  x    = xClick + this.fldLog.getScrollLeft();
	double  y    = yClick + this.fldLog.getScrollTop();
	Object  idx  = null;
	boolean done = false;

	/*
	 * zuerst die Methode fuer Java 9 und hoeher probieren:
	 *
	 *   if( skin instanceof TextAreaSkin ) {
	 *     HitInfo hitInfo = ((TextAreaSkin) skin).getIndex( x, y );
	 *     if( hitInfo != null ) {
	 *	 idx = hitInfo.getInsertionIndex();
	 *     }
	 *   }
	 */
	try {
	  Object hitInfo = skin.getClass().getMethod(
					"getIndex",
					double.class,
					double.class ).invoke( skin, x, y );
	  if( hitInfo != null ) {
	    idx  = hitInfo.getClass().getMethod(
				"getInsertionIndex" ).invoke( hitInfo );
	    done = true;
	  }
	}
	catch( Exception ex ) {}

	if( !done ) {
	  /*
	   * Wenn die Methode fuer Java 9 und hoeher nicht funktioniert,
	   * dann die nicht offiziell dokumentierte Methode fuer Java 8
	   * versuchen:
	   *
	   *   import com.sun.javafx.scene.control.skin.TextAreaSkin;
	   *
	   *   if( skin instanceof TextAreaSkin ) {
	   *     idx = ((TextAreaSkin) skin).getInsertionPoint( x, y );
	   *   }
	   */
	  try {
	    idx = skin.getClass().getMethod(
					"getInsertionPoint",
					double.class,
					double.class ).invoke( skin, x, y );
	  }
	  catch( Exception ex ) {}
	}
	if( idx != null ) {
	  if( idx instanceof Number ) {
	    int lineNum = ToolUtil.getLineNumFromLineNumMsg(
					text,
					((Number) idx).intValue() );
	    if( lineNum > 0 ) {
	      rv = lineNum;
	    }
	  }
	}
      }
    }
    return rv;
  }


  private void gotoLine( int lineNum )
  {
    int    pos        = 0;
    int    tmpLineNum = 1;
    String text       = this.fldEdit.getText();
    if( text != null ) {
      int endPos = text.length();
      while( (tmpLineNum < lineNum) && (pos < endPos) ) {
	if( text.charAt( pos ) == '\n' ) {
	  tmpLineNum++;
	}
	pos++;
      }
    }
    this.fldEdit.positionCaret( pos );
    this.fldEdit.requestFocus();
  }


  private void installStatusTimerTask( long millis )
  {
    this.statusTimerMillis = millis;
    this.statusTimerTask   = new TimerTask()
					{
					  @Override
					  public void run()
					  {
					    fireUpdStatusText();
					  }
					};
    JTCEMUApplication.getTimer().scheduleAtFixedRate(
				this.statusTimerTask,
				millis,
				millis );
  }


  private boolean loadFile( File file )
  {
    boolean done = false;
    if( file != null ) {
      try {
	String fName = file.getName();
	if( fName != null ) {
	  fName = fName.toLowerCase();
	  if( fName.endsWith( ".prj" ) ) {
	    try {
	      loadProjectFile( file );
	      done = false;
	    }
	    catch( IOException ex ) {}
	  }
	}
	if( !done ) {
	  loadTextFile( file );
	  showStatusText( "Datei geladen" );
	  done = true;
	}
      }
      catch( IOException ex ) {
	this.JTCEMUApplication.showError( ex );
      }
    }
    return done;
  }


  private void loadProjectFile( File file ) throws IOException
  {
    Properties props   = AsmUtil.loadProject( file );
    String     asmFile = props.getProperty( AsmUtil.PROP_SOURCE_FILE );
    if( asmFile == null ) {
      AsmUtil.throwNoPrjFile( file );
    }
    if( asmFile.isEmpty() ) {
      AsmUtil.throwNoPrjFile( file );
    }
    loadTextFile( new File( asmFile ) );
    this.asmOptions = AsmOptions.createOf( props );
    AppContext.setLastFile( FILE_GROUP_PROJECT, file );
    showStatusText( "Projekt geladen" );
  }


  private void loadTextFile( File file ) throws IOException
  {
    long len = file.length();
    if( len > 0x1000000 ) {
      throw new IOException( "Datei ist zu gro\u00DF!" );
    }
    try( Reader in = new FileReader( file ) ) {
      clear();

      String        eol = null;
      StringBuilder buf = new StringBuilder( (int) Math.max( len, 1024 ) );
      boolean       cr  = false;
      int           ch  = in.read();
      while( ch >= 0 ) {
	if( ch == '\n' ) {
	  if( eol == null ) {
	    eol = (cr ? "\r\n" : "\n");
	  }
	  cr = false;
	  buf.append( (char) ch );
	} else {
	  if( cr ) {
	    buf.append( (char) '\r' );
	    cr = true;
	  }
	  if( ch == '\r' ) {
	    cr = true;
	  } else {
	    buf.append( (char) ch );
	  }
	}
	ch = in.read();
      }
      setEditText( buf.toString() );
      this.eol      = eol;
      this.textFile = file;
      this.prjFile  = file;
      AppContext.setLastFile( FILE_GROUP_TEXT, file );
      fireDataUnchanged();
      fireUpdTitle();
    }
  }


  private void mouseOnLogClicked( MouseEvent e )
  {
    if( e.getClickCount() > 1 ) {
      int lineNum = getCorrespondingEditLineNum( e.getX(), e.getY() );
      if( lineNum > 0 ) {
	gotoLine( lineNum );
      }
      e.consume();
    }
  }


  private FileChooser preparePrjFileChooser( String title )
  {
    FileChooser fileChooser = GUIUtil.prepareFileChooser(
						title,
						this.prjFile,
						FILE_GROUP_PROJECT );
    fileChooser.getExtensionFilters().add(
			new FileChooser.ExtensionFilter(
				"Projektdateien (*.prj)", "*.prj" ) );
    return fileChooser;
  }


  private static FileChooser prepareTextFileChooser(
						String title,
						File   presetFile )
  {
    FileChooser fileChooser = GUIUtil.prepareFileChooser(
						title,
						presetFile,
						FILE_GROUP_TEXT );
    fileChooser.getExtensionFilters().addAll(
			GUIUtil.createTextFileFilter(),
			GUIUtil.createAsmFileFilter(),
			GUIUtil.createLstFileFilter(),
			new FileChooser.ExtensionFilter(
				"BASIC-Dateien (*.bas)", "*.bas" ) );
    return fileChooser;
  }


  private synchronized void restartStatusTimer( long millis )
  {
    if( this.statusTimerTask != null ) {
      this.statusTimerTask.cancel();
      JTCEMUApplication.getTimer().purge();
    }
    installStatusTimerTask( millis );
  }


  private boolean saveEditTextToFile( File file )
  {
    boolean rv = false;
    try {
      BufferedWriter out = null;
      try {
	out = new BufferedWriter( new FileWriter( file ) );

	String text = this.fldEdit.getText();
	if( text != null ) {
	  int len = text.length();
	  for( int i = 0; i < len; i++ ) {
	    char ch = text.charAt( i );
	    if( ch == '\n' ) {
	      if( eol != null ) {
		out.write( eol );
	      } else {
		out.newLine();
	      }
	    } else {
	      out.write( ch );
	    }
	  }
	}
	out.close();
	out = null;
	rv  = true;
	AppContext.setLastFile( FILE_GROUP_TEXT, file );
      }
      finally {
	JTCUtil.closeSilently( out );
      }
    }
    catch( IOException ex ) {
      this.JTCEMUApplication.showError( ex );
    }
    return rv;
  }


  private void selectedTextChanged()
  {
    IndexRange range = null;
    if( this.fldEdit.isFocused() ) {
      range = this.fldEdit.getSelection();
    } else if( this.fldLog.isFocused() ) {
      range = this.fldEdit.getSelection();
    }
    boolean state = !GUIUtil.hasSelection( range );
    this.btnCut.setDisable( state || !this.fldEdit.isFocused() );
    this.mnuCut.setDisable( state || !this.fldEdit.isFocused() );
    this.btnCopy.setDisable( state );
    this.mnuCopy.setDisable( state );
    this.mnuReplace.setDisable(
		state || !this.textFinder.hasReplaceText() );
  }


  public void showStatusText( String text )
  {
    if( text != null ) {
      if( !text.isEmpty() ) {
	this.statusBar.setText( text );
	restartStatusTimer( STATUS_SHOW_MSG_MILLIS );
      }
    }
  }


  private void setDataChanged( boolean state )
  {
    this.dataChanged = state;
    this.btnSave.setDisable( !state );
    this.mnuSave.setDisable( !state );
  }


  private void setEditText( String text )
  {
    this.fldEdit.setText( text != null ? text : "" );
    this.fldEdit.positionCaret( 0 );
  }


  private boolean setMonospacedFont( int fontSize )
  {
    boolean rv   = false;
    Font    font = Font.font( "Monospaced", (double) fontSize );
    if( font != null ) {
      this.fldEdit.setFont( font );
      this.fldLog.setFont( font );
      rv = true;
    }
    return rv;
  }


  private void updStatusText()
  {
    String statusText = DEFAULT_STATUS_TEXT;
    if( this.fldEdit.isFocused() ) {
      int caretPos = this.fldEdit.getCaretPosition();
      if( caretPos >= 0 ) {
	int    col      = 1;
	int    row      = 1;
	String editText = this.fldEdit.getText();
	if( editText != null ) {
	  int len = editText.length();
	  for( int i = 0; i < len; i++ ) {
	    if( i >= caretPos ) {
	      break;
	    }
	    if( editText.charAt( i ) == '\n' ) {
	      col = 1;
	      row++;
	    } else {
	      col++;
	    }
	  }
	}
	statusText = String.format( "Z:%d S:%d", row, col );
      }
    }
    this.statusBar.setText( statusText );
  }
}
