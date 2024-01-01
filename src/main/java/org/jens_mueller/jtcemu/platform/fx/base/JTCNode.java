/*
 * (c) 2014-2021 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Inhalt des Tabs fuer Bildschirm und Tastatur
 */

package jtcemu.platform.fx.base;

import java.io.File;
import java.util.TimerTask;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.event.EventType;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Separator;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.ToolBar;
import javafx.scene.image.Image;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import jtcemu.base.AppContext;
import jtcemu.base.FileInfo;
import jtcemu.base.FileSaver;
import jtcemu.base.JTCSys;
import jtcemu.base.JTCUtil;
import jtcemu.base.PasteObserver;
import jtcemu.platform.fx.Main;
import jtcemu.platform.fx.settings.SettingsNode;
import jtcemu.platform.fx.tools.TextEditNode;
import z8.Z8;
import z8.Z8Listener;


public class JTCNode
		extends BorderPane
		implements
			AppTab,
			DropFileHandler,
			PasteObserver,
			Z8Listener
{
  private static final String ACTION_GO_ON      = "Fortsetzen";
  private static final String ACTION_LOAD       = "Laden...";
  private static final String ACTION_MAX_SPEED  = "Maximale Geschwindigkeit";
  private static final String ACTION_NORM_SPEED = "Normale Geschwindigkeit";
  private static final String ACTION_PAUSE      = "Pause";
  private static final String ACTION_SAVE       = "Speichern...";
  private static final String ACTION_SETTINGS   = "Einstellungen...";
  private static final String ACTION_RESET = "Zur\u00FCcksetzen (RESET)";

  private static final String DEFAULT_STATUS_TEXT = "Emulator l\u00E4uft...";

  private static final long STATUS_REFRESH_MILLIS  = 750;
  private static final long STATUS_SHOW_MSG_MILLIS = 5000;

  private Main       main;
  private JTCSys     jtcSys;
  private Z8         z8;
  private MenuBar    mnuBar;
  private MenuItem   mnuCopy;
  private MenuItem   mnuPasteCancel;
  private MenuItem   mnuSpeed;
  private MenuItem   mnuPause;
  private ScreenNode screenNode;
  private Label      statusBar;
  private long       statusTimerMillis;
  private TimerTask  statusTimerTask;


  public JTCNode( final jtcemu.platform.fx.Main main )
  {
    this.main   = main;
    this.jtcSys = null;
    this.z8     = null;
    setCenterShape( true );


    // Menue Datei
    MenuItem mnuLoad = GUIUtil.createNonControlShortcutMenuItem(
						ACTION_LOAD,
						"L",
						false );
    mnuLoad.setOnAction( e->doLoad( false ) );

    MenuItem mnuLoadWith = GUIUtil.createNonControlShortcutMenuItem(
						"Laden mit Optionen...",
						"L",
						true );
    mnuLoadWith.setOnAction( e->doLoad( true ) );

    MenuItem mnuSave = GUIUtil.createNonControlShortcutMenuItem(
						ACTION_SAVE,
						"S",
						false );
    mnuSave.setOnAction( e->doSave() );

    Menu mnuFile = new Menu( "Datei" );
    mnuFile.getItems().addAll( mnuLoad, mnuLoadWith, mnuSave );


    // Menue Bearbeiten
    this.mnuCopy = GUIUtil.createNonControlShortcutMenuItem(
						"Kopieren",
						"C",
						false );
    this.mnuCopy.setOnAction( e->doCopy() );
    this.mnuCopy.setDisable( true );

    MenuItem mnuPaste = GUIUtil.createNonControlShortcutMenuItem(
						"Einf\u00FCgen",
						"V",
						false );
    mnuPaste.setOnAction( e->doPaste() );

    this.mnuPasteCancel = new MenuItem( "Einf\u00FCgen abbrechen" );
    this.mnuPasteCancel.setOnAction( e->doPasteCancel() );
    this.mnuPasteCancel.setDisable( true );

    Menu mnuEdit = new Menu( "Bearbeiten" );
    mnuEdit.getItems().addAll(
			this.mnuCopy,
			mnuPaste,
			new SeparatorMenuItem(),
			this.mnuPasteCancel );


    // Menu Extra
    MenuItem mnuScreenCopyText = new MenuItem( "als Text kopieren" );
    mnuScreenCopyText.setOnAction( e->doCopyScreenText() );

    MenuItem mnuScreenCopyImage = new MenuItem( "als Bild kopieren" );
    mnuScreenCopyImage.setOnAction( e->doCopyScreenImage() );

    Menu mnuScreenOutput = new Menu( "Bildschirmausgabe" );
    mnuScreenOutput.getItems().addAll(
				mnuScreenCopyText,
				mnuScreenCopyImage );

    MenuItem mnuSettings = GUIUtil.createNonControlShortcutMenuItem(
						ACTION_SETTINGS,
						"E",
						false );
    mnuSettings.setOnAction( e->SettingsNode.showTab( main ) );

    this.mnuSpeed = GUIUtil.createNonControlShortcutMenuItem(
						ACTION_MAX_SPEED,
						"G",
						false );
    this.mnuSpeed.setOnAction( e->doSpeed() );

    this.mnuPause = GUIUtil.createNonControlShortcutMenuItem(
						ACTION_PAUSE,
						"P",
						false );
    this.mnuPause.setOnAction( e->doPause() );

    MenuItem mnuReset = GUIUtil.createNonControlShortcutMenuItem(
						ACTION_RESET,
						"R",
						false );
    mnuReset.setOnAction( e->doReset() );

    MenuItem mnuPowerOn = GUIUtil.createNonControlShortcutMenuItem(
				"Einschalten (Speicher l\u00F6schen)",
				"I",
				false );
    mnuPowerOn.setOnAction( e->doPowerOn() );

    Menu mnuExtra = new Menu( "Extra" );
    mnuExtra.getItems().addAll(
			mnuScreenOutput,
			mnuSettings,
			new SeparatorMenuItem(),
			this.mnuSpeed,
			this.mnuPause,
			new SeparatorMenuItem(),
			mnuReset,
			mnuPowerOn );


    // Menueleisete
    this.mnuBar = new MenuBar();
    this.mnuBar.getMenus().addAll( mnuFile, mnuEdit, mnuExtra );
    GUIUtil.completeMenuBar( main, this.mnuBar, false );


    // Werkzeugleiste
    Button btnLoad = GUIUtil.createToolBarButton(
					"/images/file/open.png",
					ACTION_LOAD,
					e->doLoad( false ) );
    Button btnSave = GUIUtil.createToolBarButton(
					"/images/file/save.png",
					ACTION_SAVE,
					e->doSave() );
    Button btnTextEdit = GUIUtil.createToolBarButton(
					"/images/file/edit.png",
					"Texteditor...",
					e->TextEditNode.showTab( main ) );
    Button btnSettings = GUIUtil.createToolBarButton(
					"/images/edit/settings.png",
					ACTION_SETTINGS,
					e->SettingsNode.showTab( main ) );
    Button btnReset = GUIUtil.createToolBarButton(
					"/images/file/reset.png",
					ACTION_RESET,
					e->doReset() );
    ToolBar toolBar = new ToolBar(
			btnLoad,
			btnSave,
			new Separator(),
			btnTextEdit,
			btnSettings,
			new Separator(),
			btnReset );
    setTop( toolBar );


    // Bildschirm
    this.screenNode = new ScreenNode( main, this );
    setCenter( this.screenNode );


    // Statuszeile
    this.statusBar = new Label( DEFAULT_STATUS_TEXT );
    this.statusBar.setAlignment( Pos.CENTER_LEFT );
    this.statusBar.setPadding( new Insets( 5 ) );
    setBottom( this.statusBar );


    // Laden einer Datei ueber Drag&Drop ermoeglichen
    setOnDragOver( e->GUIUtil.handleFileDragOver( e ) );
    setOnDragDropped( e->GUIUtil.handleFileDragDropped( e, this ) );


    // Timer fuer Aktualisierung der Statuszeile
    installStatusTimerTask( STATUS_SHOW_MSG_MILLIS );


    /*
     * Die Tastaturereignisse sollten zwar schon durch den
     * durch den JTCKeyEventExtractor zugestellt werden,
     * aber zur Sicherheit wird hier auch auf die Events gehoert.
     * Da nur nicht konsumierte KeyEvents verarbeitet werden,
     * wird eine Doppelverarbeitung verhindert.
     */
    setOnKeyPressed( e->handleKeyEvent( e ) );
    setOnKeyReleased( e->handleKeyEvent( e ) );
    setOnKeyTyped( e->handleKeyEvent( e ) );
  }


  public ScreenNode getScreenNode()
  {
    return this.screenNode;
  }


  public void handleKeyEvent( KeyEvent e )
  {
    if( !e.isConsumed() ) {
      EventType<KeyEvent> eventType = e.getEventType();
      if( eventType == KeyEvent.KEY_PRESSED ) {
	JTCSys.Key key = null;
	switch( e.getCode() ) {
	  case LEFT:
	    key = JTCSys.Key.LEFT;
	    break;
	  case RIGHT:
	    key = JTCSys.Key.RIGHT;
	    break;
	  case UP:
	    key = JTCSys.Key.UP;
	    break;
	  case DOWN:
	    key = JTCSys.Key.DOWN;
	    break;
	  case BACK_SPACE:
	    key = JTCSys.Key.BACK_SPACE;
	    break;
	  case HOME:
	    key = JTCSys.Key.HOME;
	    break;
	  case SPACE:
	    key = JTCSys.Key.SPACE;
	    break;
	  case ENTER:
	    key = JTCSys.Key.ENTER;
	    break;
	  case ESCAPE:
	    key = JTCSys.Key.ESCAPE;
	    break;
	  case INSERT:
	    key = JTCSys.Key.INSERT;
	    break;
	  case DELETE:
	    key = JTCSys.Key.DELETE;
	    break;
	  case CLEAR:
	    key = JTCSys.Key.CLEAR;
	    break;
	  case F1:
	    key = JTCSys.Key.F1;
	    break;
	  case F2:
	    key = JTCSys.Key.F2;
	    break;
	  case F3:
	    key = JTCSys.Key.F3;
	    break;
	  case F4:
	    key = JTCSys.Key.F4;
	    break;
	  case F5:
	    key = JTCSys.Key.F5;
	    break;
	  case F6:
	    key = JTCSys.Key.F6;
	    break;
	  case F7:
	    key = JTCSys.Key.F7;
	    break;
	  case F8:
	    key = JTCSys.Key.F8;
	    break;
	}
	if( key != null ) {
	  if( this.jtcSys.keyPressed( key, e.isShiftDown() ) ) {
	    e.consume();
	  }
	} else {
	  checkKeyTyped( e );
	}
      } else if( eventType == KeyEvent.KEY_RELEASED ) {
	this.jtcSys.keyReleased();
      } else if( eventType == KeyEvent.KEY_TYPED ) {
	checkKeyTyped( e );
      }
    }
  }


  public void setJTCSys( JTCSys jtcSys )
  {
    this.jtcSys = jtcSys;
    this.screenNode.setJTCSys( jtcSys );
    this.z8 = this.jtcSys.getZ8();
    this.z8.setStatusListener( this );
  }


  public void setScreenTextSelected( boolean state )
  {
    this.mnuCopy.setDisable( !state );
  }


  public void settingsChanged()
  {
    this.screenNode.settingsChanged();
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


  public void updLayout()
  {
    double hDiff = 0;
    Node   node  = getTop();
    if( node != null ) {
      if( node instanceof Region ) {
	hDiff += ((Region) node).getHeight();
      }
    }
    node = getBottom();
    if( node != null ) {
      if( node instanceof Region ) {
	hDiff += ((Region) node).getHeight();
      }
    }
    this.screenNode.setVisibleScreenArea( getWidth(), getHeight() - hDiff );
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
    // leer
  }


  @Override
  public boolean tabCloseRequest( Event e )
  {
    return false;
  }


  @Override
  public void tabSelected()
  {
    // leer
  }


	/* --- DropFileHandler --- */

  @Override
  public boolean handleDroppedFile( Object target, final File file )
  {
    boolean success = false;
    if( target == this ) {
      // nicht auf Benutzereingabe warten
      Platform.runLater( ()->loadFile( file, false ) );
      success = true;
    }
    return success;
  }


	/* --- PasteObserver --- */

  @Override
  public void pastingFinished()
  {
    Platform.runLater( ()->setPasting( false ) );
  }


	/* --- Z8Listener --- */

  @Override
  public void z8Update( Z8 z8, Reason reason )
  {
    if( reason == Z8Listener.Reason.STATUS_CHANGED )
      fireUpdStatusText();
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
      String text = this.screenNode.getSelectedText();
      if( text != null ) {
	ClipboardContent content = new ClipboardContent();
	content.putString( text );
	Clipboard.getSystemClipboard().setContent( content );
      }
    }
  }


  private void doCopyScreenImage()
  {
    if( this.main.isCurMenuBar( this.mnuBar ) ) {
      Image image = snapshot( null, null );
      if( image != null ) {
	ClipboardContent content = new ClipboardContent();
	content.putImage( image );
	Clipboard.getSystemClipboard().setContent( content );
      }
    }
  }


  private void doCopyScreenText()
  {
    if( this.main.isCurMenuBar( this.mnuBar ) && (this.jtcSys != null) ) {
      String text = this.jtcSys.getScreenText();
      if( text != null ) {
	Clipboard        clipboard = Clipboard.getSystemClipboard();
	ClipboardContent content   = new ClipboardContent();
	content.putString( text );
	clipboard.setContent( content );
      }
    }
  }


  private void doLoad( boolean withOptions )
  {
    if( this.main.isCurMenuBar( this.mnuBar ) ) {
      FileChooser fileChooser = GUIUtil.createMemFileChooser(
			withOptions ?
				"Datei laden mit Optionen"
				: "Datei laden" );
      File dirFile = AppContext.getLastDirFile(
					FileInfo.FILE_GROUP_SOFTWARE );
      if( dirFile != null ) {
	fileChooser.setInitialDirectory( dirFile );
      }
      File file = fileChooser.showOpenDialog( this.main.getStage() );
      if( file != null ) {
	loadFile( file, withOptions );
      }
    }
  }


  private void doPaste()
  {
    if( this.main.isCurMenuBar( this.mnuBar ) && (this.jtcSys != null) ) {
      boolean done = false;
      String  text = Clipboard.getSystemClipboard().getString();
      if( text != null ) {
	if( !text.isEmpty() ) {
	  this.jtcSys.startPastingText( text, this );
	  setPasting( true );
	  done = true;
	}
      }
      if( !done ) {
	this.main.showError( "Kein Text in der Zwischenablage enthalten" );
      }
    }
  }


  private void doPasteCancel()
  {
    if( this.main.isCurMenuBar( this.mnuBar ) && (this.jtcSys != null) ) {
      this.jtcSys.cancelPastingText();
      setPasting( false );
    }
  }


  private void doPause()
  {
    if( this.main.isCurMenuBar( this.mnuBar ) && (this.z8 != null) )
      this.z8.setPause( !this.z8.isPause() );
  }


  public void doPowerOn()
  {
    if( this.main.isCurMenuBar( this.mnuBar ) ) {
      boolean state = true;
      if( AppContext.getBooleanProperty(
				JTCUtil.PROP_CONFIRM_POWER_ON,
				true ) )
      {
	if( !MsgDlg.showYesNoDlg(
		this.main.getStage(),
		"M\u00F6chten Sie das Aus- und wieder Einschalten emulieren\n"
			+ "und den Emulator initialisieren?\n"
			+ "Dabei wird der Arbeitsspeicher gel\u00F6scht.",
		"Best\u00E4tigung" ) )
	{
	  state = false;
	}
      }
      if( state ) {
	resetEmu( true );
      }
    }
  }


  private void doReset()
  {
    if( this.main.isCurMenuBar( this.mnuBar ) && (this.jtcSys != null) ) {
      boolean state = true;
      if( AppContext.getBooleanProperty(
				JTCUtil.PROP_CONFIRM_RESET,
				true ) )
      {
	String msg = "M\u00F6chten Sie jetzt ein RESET ausl\u00F6sen\n"
			+ "und den Emulator zur\u00FCcksetzen?";
	if( (this.jtcSys.getOSType() == JTCSys.OSType.OS2K)
	    || (this.jtcSys.getOSType() == JTCSys.OSType.ES1988) )
	{
	  msg += "\nWenn der A-Cursor zu sehen ist,\n"
			+ "sollten Sie kein RESET ausl\u00F6sen!";
	}
	if( !MsgDlg.showYesNoDlg(
			this.main.getStage(),
			msg,
			"Best\u00E4tigung" ) )
	{
	  state = false;
	}
      }
      if( state ) {
	resetEmu( false );
      }
    }
  }


  private void doSave()
  {
    if( this.main.isCurMenuBar( this.mnuBar ) && (this.jtcSys != null) )
      SaveDlg.showSaveDlg( this.main.getStage(), this, this.jtcSys );
  }


  private void doSpeed()
  {
    if( this.main.isCurMenuBar( this.mnuBar ) && (this.z8 != null) ) {
      if( this.z8.getCyclesPerSecond() > 0 ) {
	this.mnuSpeed.setText( ACTION_NORM_SPEED );
	this.z8.setCyclesPerSecond( 0 );
	ensureStatusTimerMillis( STATUS_REFRESH_MILLIS );
      } else {
	this.mnuSpeed.setText( ACTION_MAX_SPEED );
	this.z8.setCyclesPerSecond( JTCSys.DEFAULT_Z8_CYCLES_PER_SECOND );
      }
      updStatusText();
    }
  }


	/* --- private Methoden --- */

  private void checkKeyTyped( KeyEvent e )
  {
    String s = e.getCharacter();
    if( s != null ) {
      if( s.length() == 1 ) {
	if( this.jtcSys.keyTyped( s.charAt( 0 ) ) ) {
	  e.consume();
	}
      }
    }
  }


  private void ensureStatusTimerMillis( long millis )
  {
    if( millis != this.statusTimerMillis )
      restartStatusTimer( millis );
  }


  private void fireUpdStatusText()
  {
    Platform.runLater( ()->updStatusText() );
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
    Main.getTimer().scheduleAtFixedRate(
				this.statusTimerTask,
				millis,
				millis );
  }


  private void loadFile( File file, boolean withOptions )
  {
    if( (this.jtcSys != null) && (file != null) ) {
      int      begAddr  = -1;
      FileInfo fileInfo = FileInfo.analyzeFile( file );
      if( fileInfo != null ) {
	begAddr = fileInfo.getBegAddr();
      }
      if( !withOptions && (fileInfo != null) && (begAddr >= 0) ) {
	LoadDlg.loadFile(
			this.main.getStage(),
			this,
			this.main.getJTCSys(),
			file,
			fileInfo.getFormat(),
			begAddr,
			fileInfo.getEndAddr(),
			fileInfo.getStartAddr() );
      } else {
	LoadDlg.showLoadDlg(
			this.main.getStage(),
			this,
			this.main.getJTCSys(),
			file,
			fileInfo );
      }
    }
  }


  private void resetEmu( boolean initRAM )
  {
    if( this.jtcSys != null ) {

      // eigentliches RESET
      this.screenNode.clearSelection();
      this.jtcSys.fireReset( initRAM );

      /*
       * Die Timer werden neu gestartet fuer den Fall,
       * dass sie sich aufgehaengt haben sollten,
       * und man moechte sie mit RESET reaktivieren.
       */
      this.screenNode.reinstallRefreshTimer();
      this.statusBar.setText( DEFAULT_STATUS_TEXT );
      restartStatusTimer( STATUS_REFRESH_MILLIS );
    }
  }


  private synchronized void restartStatusTimer( long millis )
  {
    if( this.statusTimerTask != null ) {
      this.statusTimerTask.cancel();
      Main.getTimer().purge();
    }
    installStatusTimerTask( millis );
  }


  private void setPasting( boolean state )
  {
    this.mnuPasteCancel.setDisable( !state );
  }


  private void updStatusText()
  {
    long   millis = STATUS_SHOW_MSG_MILLIS;
    String text   = null;
    if( this.z8 != null ) {
      if( this.z8.isPause() ) {
	this.mnuPause.setText( ACTION_GO_ON );
	this.mnuPause.setDisable( false );
	text = "Emulator angehalten";
      } else {
	this.mnuPause.setText( ACTION_PAUSE );
	this.mnuPause.setDisable( false );
	Double mhz = this.z8.getEmulatedMHz();
	if( this.z8.getCyclesPerSecond() > 0 ) {
	  if( mhz != null ) {
	    if( (mhz.doubleValue() >= 3.8)
		&& (mhz.doubleValue() <= 4.2) )
	    {
	      mhz = null;
	    } else {
	      millis = STATUS_REFRESH_MILLIS;
	    }
	  }
	} else {
	  millis = STATUS_REFRESH_MILLIS;
	}
	if( mhz != null ) {
	  text = JTCUtil.getEmulatedSpeedText( mhz.doubleValue() );
	}
      }
    }
    ensureStatusTimerMillis( millis );
    this.statusBar.setText( text != null ? text : DEFAULT_STATUS_TEXT );
  }
}
