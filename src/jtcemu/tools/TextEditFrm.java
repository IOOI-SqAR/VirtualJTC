/*
 * (c) 2007-2010 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Texteditor
 */

package jtcemu.tools;

import java.awt.*;
import java.awt.print.*;
import java.awt.datatransfer.*;
import java.awt.dnd.*;
import java.awt.event.*;
import java.io.*;
import java.lang.*;
import java.text.CharacterIterator;
import java.util.*;
import java.util.regex.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.undo.UndoManager;
import jtcemu.base.*;
import jtcemu.Main;
import z8.Z8Memory;


public class TextEditFrm extends AbstractTextFrm
			implements
				ActionListener,
				CaretListener,
				DocumentListener,
				DropTargetListener,
				FlavorListener,
				FocusListener,
				MouseListener,
				UndoableEditListener
{
  private static TextEditFrm instance = null;

  private JTCSys         jtcSys;
  private UndoManager    undoMngr;
  private Integer        basicAddrTransfer;
  private int            basicAddrFetch;
  private boolean        dataChanged;
  private boolean        findCaseSensitive;
  private boolean        findRegularExpr;
  private Pattern        findPattern;
  private String         findText;
  private String         replaceText;
  private File           file;
  private JTextComponent focusFld;
  private JTextComponent selectionFld;
  private JMenuItem      mnuNew;
  private JMenuItem      mnuOpen;
  private JMenuItem      mnuLoadBasicMemE000;
  private JMenuItem      mnuLoadBasicMemWith;
  private JMenuItem      mnuSave;
  private JMenuItem      mnuSaveAs;
  private JMenuItem      mnuPrintOptions;
  private JMenuItem      mnuPrint;
  private JMenuItem      mnuClose;
  private JMenuItem      mnuUndo;
  private JMenuItem      mnuCut;
  private JMenuItem      mnuCopy;
  private JMenuItem      mnuPaste;
  private JMenuItem      mnuSelectAll;
  private JMenuItem      mnuFindAndReplace;
  private JMenuItem      mnuFindNext;
  private JMenuItem      mnuReplace;
  private JMenuItem      mnuGoto;
  private JMenuItem      mnuBasicParse;
  private JMenuItem      mnuBasicIntoEmu;
  private JMenuItem      mnuBasicIntoEmuOpt;
  private JMenuItem      mnuHelpContent;
  private JButton        btnNew;
  private JButton        btnOpen;
  private JButton        btnSave;
  private JButton        btnPrint;
  private JButton        btnUndo;
  private JButton        btnCut;
  private JButton        btnCopy;
  private JButton        btnPaste;
  private JButton        btnFindAndReplace;
  private Document       textDoc;
  private JTextArea      logArea;
  private JSplitPane     splitPane;
  private JLabel         labelStatus;


  public static boolean close()
  {
    return instance != null ? instance.doClose() : true;
  }


  public boolean confirmDataSaved()
  {
    boolean rv = false;
    if( this.dataChanged ) {
      String[]    options = { "Speichern", "Verwerfen", "Abbrechen" };
      JOptionPane pane    = new JOptionPane(
		"Der Text wurde ge\u00E4ndert, aber nicht gespeichert.\n"
			+ "Sie k\u00F6nnen den Text jetzt speichern,"
			+ " verwerfern\n"
			+ "oder die Aktion abbrechen.",
		JOptionPane.WARNING_MESSAGE );
      pane.setOptions( options );
      pane.setInitialValue( options[ 0 ] );
      pane.setWantsInput( false );
      pane.createDialog( this, "Daten ge\u00E4ndert" ).setVisible( true );
      Object value = pane.getValue();
      if( value != null ) {
	if( value.equals( options[ 0 ] ) ) {
	  rv = doSave( false );
	}
	else if( value.equals( options[ 1 ] ) ) {
	  rv = true;
	}
      }
    } else {
      rv = true;
    }
    return rv;
  }


  public static void open( JTCSys jtcSys )
  {
    if( instance != null ) {
      instance.setState( Frame.NORMAL );
      instance.toFront();
    } else {
      instance = new TextEditFrm( jtcSys );
      instance.setVisible( true );
    }
  }


	/* --- ActionListener --- */

  @Override
  public void actionPerformed( ActionEvent e )
  {
    GUIUtil.setWaitCursor( this, true );
    Object src = e.getSource();
    if( src != null ) {
      if( (src == this.mnuNew) || (src == this.btnNew) ) {
	doNew();
      }
      else if( (src == this.mnuOpen) || (src == this.btnOpen) ) {
	doOpen();
      }
      else if( src == this.mnuLoadBasicMemE000 ) {
	doLoadBasicMem( new Integer( 0xE000 ) );
      }
      else if( src == this.mnuLoadBasicMemWith ) {
	doLoadBasicMem( null );
      }
      else if( (src == this.mnuSave) || (src == this.btnSave) ) {
	doSave( false );
      }
      else if( src == this.mnuSaveAs ) {
	doSave( true );
      }
      else if( src == this.mnuPrintOptions ) {
	PrintOptionsDlg.open( this );
      }
      else if( (src == this.mnuPrint) || (src == this.btnPrint) ) {
	doPrint();
      }
      else if( src == this.mnuClose ) {
	doClose();
      }
      else if( (src == this.mnuUndo) || (src == this.btnUndo) ) {
	doUndo();
      }
      else if( (src == this.mnuCut) || (src == this.btnCut) ) {
	if( this.selectionFld != null ) {
	  if( this.selectionFld.isEditable() )
	    this.selectionFld.cut();
	}
      }
      else if( (src == this.mnuCopy) || (src == this.btnCopy) ) {
	if( this.selectionFld != null )
	  this.selectionFld.copy();
      }
      else if( (src == this.mnuPaste) || (src == this.btnPaste) ) {
	if( this.focusFld != null ) {
	  if( this.focusFld.isEditable() )
	    this.focusFld.paste();
	}
      }
      else if( src == this.mnuSelectAll ) {
	if( this.focusFld != null ) {
	  this.focusFld.requestFocus();
	  this.focusFld.selectAll();
	}
      }
      else if( (src == this.mnuFindAndReplace)
	       || (src == this.btnFindAndReplace) )
      {
	doFindAndReplace();
      }
      else if( src == this.mnuReplace ) {
	doReplace();
      }
      else if( src == this.mnuFindNext ) {
	findNext();
      }
      else if( src == this.mnuGoto ) {
	doGoto();
      }
      else if( src == this.mnuBasicParse ) {
	doPrgBasic( false, false );
      }
      else if( src == this.mnuBasicIntoEmu ) {
	doPrgBasic( true, false );
      }
      else if( src == this.mnuHelpContent ) {
	HelpFrm.open( "/help/texteditor.htm" );
      }
      else if( src == this.mnuBasicIntoEmuOpt ) {
	doPrgBasic( true, true );
      }
    }
    GUIUtil.setWaitCursor( this, false );
  }


	/* --- CaretListener --- */

  @Override
  public void caretUpdate( CaretEvent e )
  {
    Object src = e.getSource();
    if( src != null ) {
      if( src instanceof JTextComponent ) {
	this.selectionFld = (JTextComponent) src;
	int     begPos    = this.selectionFld.getSelectionStart();
	boolean selected  = ((begPos >= 0)
		&& (begPos < this.selectionFld.getSelectionEnd()));
	this.mnuCut.setEnabled( selected && this.selectionFld.isEditable() );
	this.btnCut.setEnabled( selected && this.selectionFld.isEditable() );
	this.mnuCopy.setEnabled( selected );
	this.btnCopy.setEnabled( selected );
	this.mnuReplace.setEnabled( selected && (this.replaceText != null) );
      }
      if( src == this.textArea ) {
	updStatusText();
      }
    }
  }


	/* --- DocumentListener --- */

  @Override
  public void changedUpdate( DocumentEvent e )
  {
    docChanged( e );
  }


  @Override
  public void insertUpdate( DocumentEvent e )
  {
    docChanged( e );
  }



  @Override
  public void removeUpdate( DocumentEvent e )
  {
    docChanged( e );
  }


	/* --- DropTargetListener --- */

  @Override
  public void dragEnter( DropTargetDragEvent e )
  {
    if( !GUIUtil.isFileDrop( e ) )
      e.rejectDrag();
  }


  @Override
  public void dragExit( DropTargetEvent e )
  {
    // leer
  }


  @Override
  public void dragOver( DropTargetDragEvent e )
  {
    // leer
  }


  @Override
  public void drop( DropTargetDropEvent e )
  {
    File file = GUIUtil.fileDrop( this, e );
    if( file != null ) {
      loadFile( file );
    }
  }


  @Override
  public void dropActionChanged( DropTargetDragEvent e )
  {
    if( !GUIUtil.isFileDrop( e ) )
      e.rejectDrag();
  }


	/* --- FlavorListener --- */

  @Override
  public void flavorsChanged( FlavorEvent e )
  {
    Object src = e.getSource();
    if( src != null ) {
      if( src instanceof Clipboard ) {
	updPasteBtn( (Clipboard) src );
      }
    }
  }


	/* --- FocusListener --- */

  @Override
  public void focusGained( FocusEvent e )
  {
    Component c = e.getComponent();
    if( (c != null) && !e.isTemporary() ) {
      if( c instanceof JTextComponent )
	this.focusFld = (JTextComponent) c;
    }
  }


  @Override
  public void focusLost( FocusEvent e )
  {
    if( !e.isTemporary() )
      this.focusFld = null;
  }


	/* --- MouseListener --- */

  @Override
  public void mouseClicked( MouseEvent e )
  {
    if( (e.getComponent() == this.logArea) && (e.getClickCount() > 1) ) {
      Point pt = e.getPoint();
      if( pt != null ) {
	int pos = this.logArea.viewToModel( pt );
	if( pos >= 0 ) {
	  String text = this.logArea.getText();
	  if( text != null ) {
	    int len = text.length();
	    if( pos < len ) {
	      while( pos > 0 ) {
		if( text.charAt( pos - 1 ) == '\n' ) {
		  break;
		}
		--pos;
	      }
	      int eol = text.indexOf( '\n', pos );
	      if( eol >= pos ) {
		processLineAction( text.substring( pos, eol ) );
	      } else {
		processLineAction( text.substring( pos ) );
	      }
	    }
	  }
	}
      }
      e.consume();
    }
  }


  @Override
  public void mouseEntered( MouseEvent e )
  {
    // leer
  }


  @Override
  public void mouseExited( MouseEvent e )
  {
    // leer
  }


  @Override
  public void mousePressed( MouseEvent e )
  {
    // leer
  }


  @Override
  public void mouseReleased( MouseEvent e )
  {
    // leer
  }


	/* --- UndoableEditListener --- */

  @Override
  public void undoableEditHappened( UndoableEditEvent e )
  {
    this.undoMngr.undoableEditHappened( e );
    updUndoBtn();
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  protected boolean doClose()
  {
    boolean rv = confirmDataSaved();
    if( rv ) {
      rv = super.doClose();
    }
    if( rv ) {
      this.undoMngr.discardAllEdits();
      instance = null;
      updUndoBtn();
    }
    return rv;
  }


  @Override
  public void prepareSettingsToSave()
  {
    Main.setProperty(
	getClass().getName() + ".split.location",
	Integer.toString( this.splitPane.getDividerLocation() ) );
  }


  @Override
  public void windowOpened( WindowEvent e )
  {
    boolean done = false;
    Integer pValue = Main.getIntegerProperty(
			getClass().getName() + ".split.location" );
    if( pValue != null ) {
      this.splitPane.setDividerLocation( pValue.intValue() );
    } else {
      this.splitPane.setDividerLocation( 0.8 );
    }
    this.textArea.requestFocus();
  }


	/* --- privater Konstruktor --- */

  private TextEditFrm( JTCSys jtcSys )
  {
    this.jtcSys            = jtcSys;
    this.undoMngr          = new UndoManager();
    this.basicAddrTransfer = null;
    this.basicAddrFetch    = 0xE000;
    this.dataChanged       = false;
    this.findCaseSensitive = false;
    this.findRegularExpr   = false;
    this.findPattern       = null;
    this.findText          = null;
    this.replaceText       = null;
    this.file              = null;
    this.focusFld          = null;
    this.selectionFld      = null;
    this.replaceText       = null;
    updTitle();


    // Menu
    JMenuBar mnuBar = new JMenuBar();
    setJMenuBar( mnuBar );


    // Menu Datei
    JMenu mnuFile = new JMenu( "Datei" );
    mnuFile.setMnemonic( 'D' );
    mnuBar.add( mnuFile );

    this.mnuNew = new JMenuItem( "Neuer Text" );
    this.mnuNew.setAccelerator( KeyStroke.getKeyStroke(
						KeyEvent.VK_N,
						InputEvent.CTRL_MASK ) );
    this.mnuNew.addActionListener( this );
    mnuFile.add( this.mnuNew );

    this.mnuOpen = new JMenuItem( "\u00D6ffnen..." );
    this.mnuOpen.setAccelerator( KeyStroke.getKeyStroke(
						KeyEvent.VK_O,
						InputEvent.CTRL_MASK ) );
    this.mnuOpen.addActionListener( this );
    mnuFile.add( this.mnuOpen );
    mnuFile.addSeparator();

    this.mnuLoadBasicMemE000 = new JMenuItem(
			"BASIC-Programm aus Arbeitsspeicher ab E000 laden" );
    this.mnuLoadBasicMemE000.setAccelerator( KeyStroke.getKeyStroke(
						KeyEvent.VK_B,
						InputEvent.CTRL_MASK ) );
    this.mnuLoadBasicMemE000.addActionListener( this );
    mnuFile.add( this.mnuLoadBasicMemE000 );

    this.mnuLoadBasicMemWith = new JMenuItem(
			"BASIC-Programm aus Arbeitsspeicher laden..." );
    this.mnuLoadBasicMemWith.addActionListener( this );
    mnuFile.add( this.mnuLoadBasicMemWith );
    mnuFile.addSeparator();

    this.mnuSave = new JMenuItem( "Speichern" );
    this.mnuSave.setAccelerator( KeyStroke.getKeyStroke(
						KeyEvent.VK_S,
						InputEvent.CTRL_MASK ) );
    this.mnuSave.addActionListener( this );
    mnuFile.add( this.mnuSave );

    this.mnuSaveAs = new JMenuItem( "Speichern unter..." );
    this.mnuSaveAs.setAccelerator(
		KeyStroke.getKeyStroke(
			KeyEvent.VK_S,
			InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK ) );
    this.mnuSaveAs.addActionListener( this );
    mnuFile.add( this.mnuSaveAs );
    mnuFile.addSeparator();

    this.mnuPrintOptions = new JMenuItem( "Druckoptionen..." );
    this.mnuPrintOptions.addActionListener( this );
    mnuFile.add( this.mnuPrintOptions );

    this.mnuPrint = new JMenuItem( "Drucken..." );
    this.mnuPrint.setAccelerator( KeyStroke.getKeyStroke(
					KeyEvent.VK_P,
					InputEvent.CTRL_MASK ) );
    this.mnuPrint.addActionListener( this );
    mnuFile.add( this.mnuPrint );
    mnuFile.addSeparator();

    this.mnuClose = new JMenuItem( "Schlie\u00DFen" );
    this.mnuClose.addActionListener( this );
    mnuFile.add( this.mnuClose );


    // Menu Bearbeiten
    JMenu mnuEdit = new JMenu( "Bearbeiten" );
    mnuEdit.setMnemonic( 'B' );
    mnuBar.add( mnuEdit );

    this.mnuUndo = new JMenuItem( "R\u00FCckg\u00E4ngig" );
    this.mnuUndo.setAccelerator( KeyStroke.getKeyStroke(
						KeyEvent.VK_Z,
						InputEvent.CTRL_MASK ) );
    this.mnuUndo.setEnabled( false );
    this.mnuUndo.addActionListener( this );
    mnuEdit.add( this.mnuUndo );
    mnuEdit.addSeparator();

    this.mnuCut = new JMenuItem( "Ausschneiden" );
    this.mnuCut.setAccelerator( KeyStroke.getKeyStroke(
						KeyEvent.VK_X,
						InputEvent.CTRL_MASK ) );
    this.mnuCut.setEnabled( false );
    this.mnuCut.addActionListener( this );
    mnuEdit.add( this.mnuCut );

    this.mnuCopy = new JMenuItem( "Kopieren" );
    this.mnuCopy.setAccelerator( KeyStroke.getKeyStroke(
						KeyEvent.VK_C,
						InputEvent.CTRL_MASK ) );
    this.mnuCopy.setEnabled( false );
    this.mnuCopy.addActionListener( this );
    mnuEdit.add( this.mnuCopy );

    this.mnuPaste = new JMenuItem( "Einf\u00FCgen" );
    this.mnuPaste.setAccelerator( KeyStroke.getKeyStroke(
						KeyEvent.VK_V,
						InputEvent.CTRL_MASK ) );
    this.mnuPaste.setEnabled( false );
    this.mnuPaste.addActionListener( this );
    mnuEdit.add( this.mnuPaste );
    mnuEdit.addSeparator();

    this.mnuSelectAll = new JMenuItem( "Alles ausw\u00E4hlen" );
    this.mnuSelectAll.addActionListener( this );
    mnuEdit.add( this.mnuSelectAll );
    mnuEdit.addSeparator();

    this.mnuFindAndReplace = new JMenuItem( "Suchen und ersetzen..." );
    this.mnuFindAndReplace.setAccelerator( KeyStroke.getKeyStroke(
						KeyEvent.VK_F,
						InputEvent.CTRL_MASK ) );
    this.mnuFindAndReplace.addActionListener( this );
    mnuEdit.add( this.mnuFindAndReplace );

    this.mnuReplace = new JMenuItem( "Ersetzen" );
    this.mnuReplace.setAccelerator( KeyStroke.getKeyStroke(
						KeyEvent.VK_R,
						InputEvent.CTRL_MASK ) );
    this.mnuReplace.setEnabled( false );
    this.mnuReplace.addActionListener( this );
    mnuEdit.add( this.mnuReplace );

    this.mnuFindNext = new JMenuItem( "Weitersuchen" );
    this.mnuFindNext.setEnabled( false );
    this.mnuFindNext.setAccelerator(
			KeyStroke.getKeyStroke( KeyEvent.VK_F3, 0 ) );
    this.mnuFindNext.addActionListener( this );
    mnuEdit.add( this.mnuFindNext );
    mnuEdit.addSeparator();

    this.mnuGoto = new JMenuItem( "Gehe zu Zeile..." );
    this.mnuGoto.setAccelerator( KeyStroke.getKeyStroke(
						KeyEvent.VK_G,
						InputEvent.CTRL_MASK ) );
    this.mnuGoto.addActionListener( this );
    mnuEdit.add( this.mnuGoto );


    // Menu Programmierung
    JMenu mnuPrg = new JMenu( "Programmierung" );
    mnuPrg.setMnemonic( 'P' );
    mnuBar.add( mnuPrg );

    this.mnuBasicParse = new JMenuItem(
			"BASIC-Programm syntaktisch pr\u00FCfen" );
    this.mnuBasicParse.addActionListener( this );
    mnuPrg.add( this.mnuBasicParse );

    this.mnuBasicIntoEmu = new JMenuItem(
			"BASIC-Programm in Arbeitsspeicher laden" );
    this.mnuBasicIntoEmu.setAccelerator(
			KeyStroke.getKeyStroke( KeyEvent.VK_F9, 0 ) );
    this.mnuBasicIntoEmu.addActionListener( this );
    mnuPrg.add( this.mnuBasicIntoEmu );

    this.mnuBasicIntoEmuOpt = new JMenuItem(
			"BASIC-Programm in Arbeitsspeicher laden mit..." );
    this.mnuBasicIntoEmuOpt.addActionListener( this );
    mnuPrg.add( this.mnuBasicIntoEmuOpt );


    // Menu Hilfe
    JMenu mnuHelp = new JMenu( "?" );
    mnuBar.add( mnuHelp );

    this.mnuHelpContent = new JMenuItem( "Hilfe..." );
    this.mnuHelpContent.addActionListener( this );
    mnuHelp.add( this.mnuHelpContent );


    // Fensterinhalt
    setLayout( new GridBagLayout() );

    GridBagConstraints gbc = new GridBagConstraints(
					0, 0,
					1, 1,
					1.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.HORIZONTAL,
					new Insets( 0, 0, 0, 0 ),
					0, 0 );


    // Werkzeugleiste
    JToolBar toolBar = new JToolBar();
    toolBar.setFloatable( false );
    toolBar.setBorderPainted( false );
    toolBar.setOrientation( JToolBar.HORIZONTAL );
    toolBar.setRollover( true );
    add( toolBar, gbc );

    this.btnNew = GUIUtil.createImageButton(
				this,
				"/images/file/new.png",
				"Neu" );
    toolBar.add( this.btnNew );

    this.btnOpen = GUIUtil.createImageButton(
				this,
				"/images/file/open.png",
				"\u00D6ffnen" );
    toolBar.add( this.btnOpen );

    this.btnSave = GUIUtil.createImageButton(
				this,
				"/images/file/save.png",
				"Speichern" );
    toolBar.add( this.btnSave );

    this.btnPrint = GUIUtil.createImageButton(
				this,
				"/images/file/print.png",
				"Drucken" );
    toolBar.add( this.btnPrint );
    toolBar.addSeparator();

    this.btnUndo = GUIUtil.createImageButton(
				this,
				"/images/edit/undo.png",
				"R\u00FCckg\u00E4ngig" );
    this.btnUndo.setEnabled( false );
    toolBar.add( this.btnUndo );
    toolBar.addSeparator();

    this.btnCut = GUIUtil.createImageButton(
				this,
				"/images/edit/cut.png",
				"Ausschneiden" );
    this.btnCut.setEnabled( false );
    toolBar.add( this.btnCut );

    this.btnCopy = GUIUtil.createImageButton(
				this,
				"/images/edit/copy.png",
				"Kopieren" );
    this.btnCopy.setEnabled( false );
    toolBar.add( this.btnCopy );

    this.btnPaste = GUIUtil.createImageButton(
				this,
				"/images/edit/paste.png",
				"Einf\u00FCgen" );
    this.btnPaste.setEnabled( false );
    toolBar.add( this.btnPaste );
    toolBar.addSeparator();

    this.btnFindAndReplace = GUIUtil.createImageButton(
				this,
				"/images/edit/find.png",
				"Suchen und Ersetzen..." );
    toolBar.add( this.btnFindAndReplace );


    // Textbereich
    this.splitPane = new JSplitPane( JSplitPane.VERTICAL_SPLIT );
    this.splitPane.setContinuousLayout( false );
    this.splitPane.setOneTouchExpandable( true );
    this.splitPane.setResizeWeight( 1.0 );
    gbc.anchor  = GridBagConstraints.CENTER;
    gbc.fill    = GridBagConstraints.BOTH;
    gbc.weighty = 1.0;
    gbc.gridy++;
    add( this.splitPane, gbc );

    this.textArea.setRows( 20 );
    this.textArea.setColumns( 60 );
    this.textArea.setEditable( true );
    this.textArea.setMargin( new Insets( 5, 5, 5, 5 ) );
    this.textArea.addCaretListener( this );
    this.textArea.addFocusListener( this );

    JScrollPane textScrollPane = new JScrollPane( this.textArea );
    this.splitPane.setTopComponent( textScrollPane );

    this.textDoc = this.textArea.getDocument();
    if( this.textDoc != null ) {
      this.textDoc.addDocumentListener( this );
      this.textDoc.addUndoableEditListener( this );
      this.mnuSave.setEnabled( false );
      this.btnSave.setEnabled( false );
    }

    this.logArea = new JTextArea( 2, 60 );
    this.logArea.setEditable( false );
    this.logArea.setMargin( new Insets( 5, 5, 5, 5 ) );
    this.logArea.addCaretListener( this );
    this.logArea.addFocusListener( this );
    this.logArea.addMouseListener( this );

    this.splitPane.setBottomComponent( new JScrollPane( this.logArea ) );


    // Statuszeile
    this.labelStatus  = new JLabel();
    gbc.fill          = GridBagConstraints.HORIZONTAL;
    gbc.weighty       = 0.0;
    gbc.insets.left   = 5;
    gbc.insets.top    = 5;
    gbc.insets.bottom = 2;
    gbc.insets.right  = 2;
    gbc.gridy++;
    add( this.labelStatus, gbc );
    updStatusText();


    // Fenstergroesse und -position
    setResizable( true );
    if( !GUIUtil.applyWindowSettings( this ) ) {
      pack();
      setLocationByPlatform( true );
    }
    this.textArea.setColumns( 0 );
    this.textArea.setRows( 0 );
    this.logArea.setColumns( 0 );
    this.logArea.setRows( 0 );


    // Drop-Ziel
    (new DropTarget( this.textArea, this )).setActive( true );
    (new DropTarget( textScrollPane, this )).setActive( true );


    // Listener fuer Zwischenablage
    Toolkit tk = getToolkit();
    if( tk != null ) {
      Clipboard clp = tk.getSystemClipboard();
      if( clp != null ) {
	clp.addFlavorListener( this );
	updPasteBtn( clp );
      }
    }
  }


	/* --- Aktionen --- */

  private void doLoadBasicMem( Integer addr )
  {
    if( confirmDataSaved() ) {
      if( addr == null ) {
	addr = GUIUtil.askHex4(
			this,
			"Anfangsadresse des BASIC-Programms",
			this.basicAddrFetch );
      }
      if( addr != null ) {
	int a = addr.intValue();
	int b = this.jtcSys.getMemByte( a++, false );
	if( (b == 0)
	    || ((b == 0xFF) && (this.jtcSys.getMemByte( a, false ) == 0xFF)) )
	{
	  Main.showError(
		this,
		"An der entsprechenden Adresse befindet sich\n"
			+ "im Arbeitsspeicher kein BASIC-Programm." );
	} else {
	  StringBuilder buf = new StringBuilder( 0x4000 );
	  while( (a < 0xFFFF) && (b != 0) ) {
	    int lineNum = (b << 8) | this.jtcSys.getMemByte( a++, false );
	    if( lineNum == 0xFFFF ) {
	      break;
	    }
	    buf.append( lineNum & 0x7FFF );
	    b = this.jtcSys.getMemByte( a++, false );
	    while( (b != 0) && (b != 0x0D) ) {
	      buf.append( (char) '\u0020' );
	      boolean instIF   = false;
	      boolean instTRAP = false;
	      switch( b ) {
		case '/':
		  buf.append( "TOFF" );
		  break;

		case '!':
		  buf.append( "TRAP" );
		  instTRAP = true;
		  break;

		case '>':
		  buf.append( "ELSE" );
		  break;

		case 'C':
		  buf.append( "CALL" );
		  break;

		case 'E':
		  buf.append( "END" );
		  break;

		case 'F':
		  buf.append( "IF" );
		  instIF = true;
		  break;

		case 'G':
		  buf.append( "GOTO" );
		  break;

		case 'H':
		  buf.append( "PTH" );
		  break;

		case 'I':
		  buf.append( "INPUT" );
		  break;

		case 'L':
		  buf.append( "LET" );
		  break;

		case 'M':
		  buf.append( "REM" );
		  break;

		case 'O':
		  buf.append( "PROC" );
		  break;

		case 'P':
		  buf.append( "PRINT" );
		  break;

		case 'R':
		  buf.append( "RETURN" );
		  break;

		case 'S':
		  buf.append( "GOSUB" );
		  break;

		case 'T':
		  buf.append( "STOP" );
		  break;

		case 'W':
		  buf.append( "WAIT" );
		  break;

		default:
		  buf.append( (char) b );
	      }
	      b = this.jtcSys.getMemByte( a++, false );
	      if( (b != 0) && (b != 0x0D) && (b != ';') ) {
		buf.append( (char) '\u0020' );
		do {
		  if( instTRAP && (b == ',') ) {
		    buf.append( " TO " );
		    instTRAP = false;
		  } else {
		    buf.append( (char) b );
		  }
		  b = this.jtcSys.getMemByte( a++, false );
		} while( (b != 0) && (b != 0x0D) && (b != ';') );
	      }
	      if( b == ';' ) {
		if( instIF ) {
		  buf.append( " THEN" );
		} else {
		  buf.append( (char) b );
		}
		b = this.jtcSys.getMemByte( a++, false );
	      }
	    }
	    buf.append( (char) '\n' );
	    b = this.jtcSys.getMemByte( a++, false );
	  }
	  setText( buf.toString() );
	  this.logArea.setText( "" );
	  this.labelStatus.setText( "BASIC-Programm geladen" );
	  this.basicAddrFetch = addr;
	  this.file           = null;
	  this.undoMngr.discardAllEdits();
	  updUndoBtn();
	  fireDataUnchanged( true );
	  updTitle();
	}
      }
    }
  }


  private void doNew()
  {
    if( confirmDataSaved() ) {
      setText( "" );
      this.logArea.setText( "" );
      this.file = null;
      this.undoMngr.discardAllEdits();
      updUndoBtn();
      fireDataUnchanged( false );
      updTitle();
      updStatusText();
    }
  }


  private void doOpen()
  {
    if( confirmDataSaved() ) {
      File file = FileDlg.showFileOpenDlg(
				this,
				"Textdatei \u00F6ffnen",
				"\u00D6ffnen",
				Main.getLastPathFile(),
				GUIUtil.textFileFilter,
				GUIUtil.basicFileFilter );
      if( file != null )
	loadFile( file );
    }
  }


  private void doFindAndReplace()
  {
    String findText = this.textArea.getSelectedText();
    if( findText == null ) {
      findText = this.findText;
    }
    FindAndReplaceDlg dlg = new FindAndReplaceDlg(
					this,
					findText,
					this.replaceText,
					this.findCaseSensitive,
					this.findRegularExpr );
    dlg.setVisible( true );
    Pattern pattern = dlg.getPattern();
    if( pattern != null ) {
      this.findPattern       = pattern;
      this.findText          = dlg.getFindText();
      this.replaceText       = dlg.getReplaceText();
      this.findCaseSensitive = dlg.getCaseSensitive();
      this.findRegularExpr   = dlg.getRegularExpr();
      if( dlg.getReplaceAll() ) {
	String text = this.textArea.getText();
	if( text != null ) {
	  setText( pattern.matcher( text ).replaceAll(
			this.replaceText != null ? this.replaceText : "" ) );
	}
      } else {
	findNext();
      }
      this.mnuFindNext.setEnabled( true );
    }
  }


  private void doGoto()
  {
    Integer lineNum = null;
    try {
      lineNum = new Integer( this.textArea.getLineOfOffset(
				this.textArea.getCaretPosition() ) + 1 );
    }
    catch( BadLocationException ex ) {
      lineNum = null;
    }
    lineNum = GUIUtil.askDecimal( this, "Zeile", lineNum, new Integer( 1 ) );
    if( lineNum != null )
      gotoLine( lineNum.intValue() );
  }


  private void doPrgBasic( boolean intoEmu, boolean askOpt )
  {
    this.logArea.setText( "" );
    ByteArrayOutputStream codeBuf = null;
    if( intoEmu ) {
      codeBuf = new ByteArrayOutputStream( 0x2000 );
    }
    StringBuilder logBuf = new StringBuilder( 0x0800 );
    if( (new BasicParser(
		this.textArea.getText(),
		this.logArea,
		codeBuf )).parse(
			this.jtcSys.getOSType() == JTCSys.OSType.ES40
			|| this.jtcSys.getOSType() == JTCSys.OSType.ES23 ) )
    {
      if( codeBuf != null ) {
	if( codeBuf.size() > 0 ) {
	  Integer addr = this.basicAddrTransfer;
	  if( askOpt || (addr == null) ) {
	    addr = GUIUtil.askHex4(
			this,
			"Anfangsadresse des BASIC-Programms",
			addr != null ?
				addr
				: new Integer( this.basicAddrFetch ) );
	  }
	  if( addr != null ) {
	    byte[] dataBytes = codeBuf.toByteArray();
	    if( dataBytes != null ) {
	      if( dataBytes.length > 0 ) {
		int a = addr.intValue();
		for( int i = 0; i < dataBytes.length; i++ ) {
		  this.jtcSys.setMemByte( a++, false, dataBytes[ i ] );
		}
		this.logArea.append(
			String.format(
				"BASIC-Programm in Speicherbereich"
					+ " %04X-%04X geladen\n",
				addr,
				a - 1 ) );
	      }
	    }
	    this.basicAddrTransfer = addr;
	  }
	}
      }
    }
  }


  private void doReplace()
  {
    if( this.replaceText != null ) {
      int begPos = this.textArea.getSelectionStart();
      int endPos = this.textArea.getSelectionEnd();
      if( (begPos >= 0) && (begPos < endPos) )
	this.textArea.replaceRange( this.replaceText, begPos, endPos );
    }
  }


  private boolean doSave( boolean forceFileDlg )
  {
    boolean rv   = false;
    File    file = this.file;
    if( forceFileDlg || (file == null) ) {
      file = FileDlg.showFileSaveDlg(
		this,
		"Textdatei speichern",
		this.file != null ?  this.file : Main.getLastPathFile(),
		GUIUtil.textFileFilter,
		GUIUtil.basicFileFilter );
    }
    if( file != null ) {
      try {
	writeFile( file );
	this.file = file;
	Main.setLastFile( file );
	fireDataUnchanged( false );
	updTitle();
	this.labelStatus.setText( "Datei gespeichert" );
	rv = true;
      }
      catch( IOException ex ) {
	Main.showError( this, ex );
      }
    }
    return rv;
  }


  private void doUndo()
  {
    if( this.undoMngr.canUndo() ) {
      this.undoMngr.undo();
      updUndoBtn();
    }
  }


	/* --- private Methoden --- */

  private static void close( Closeable stream )
  {
    if( stream != null ) {
      try {
	stream.close();
      }
      catch( IOException ex ) {}
    }
  }


  private void docChanged( DocumentEvent e )
  {
    if( e.getDocument() == this.textDoc )
      setDataChanged( true );
  }


  private void findNext()
  {
    if( this.findPattern != null ) {
      boolean found = false;
      String  text  = this.textArea.getText();
      if( text != null ) {
	int pos = this.textArea.getCaretPosition();
	if( (pos == this.textArea.getSelectionStart())
	    && (pos < this.textArea.getSelectionEnd()) )
	{
	  pos = this.textArea.getSelectionEnd();
	}
	if( pos >= text.length() ) {
	  pos = 0;
	}
	Matcher matcher = this.findPattern.matcher( text );
	found           = findNext( matcher, pos );
	if( !found ) {
	  found = findNext( matcher, 0 );
	}
      }
      if( !found ) {
	JOptionPane.showMessageDialog(
		this,
		"Text nicht gefunden",
		"Hinweis",
		JOptionPane.INFORMATION_MESSAGE );
      }
    }
  }


  private boolean findNext( Matcher matcher, int pos )
  {
    boolean found = false;
    if( matcher.find( pos ) ) {
      try {
	this.textArea.setCaretPosition( matcher.start() );
	this.textArea.moveCaretPosition( matcher.end() );
      }
      catch( IllegalArgumentException ex ) {}
      found = true;
    }
    return found;
  }


  private void fireDataUnchanged( final boolean state )
  {
    final JMenuItem mnuSave = this.mnuSave;
    final JButton   btnSave = this.btnSave;
    setDataChanged( state );
    SwingUtilities.invokeLater(
		new Runnable()
		{
		  public void run()
		  {
		    setDataChanged( false );
		    if( state ) {
		      mnuSave.setEnabled( true );
		      btnSave.setEnabled( true );
		    }
		  }
		} );
  }


  private void loadFile( File file )
  {
    try {
      readFile( file );
      this.logArea.setText( "" );
      this.file = file;
      Main.setLastFile( file );
      this.undoMngr.discardAllEdits();
      updUndoBtn();
      fireDataUnchanged( false );
      updTitle();
      this.labelStatus.setText( "Datei geladen" );
    }
    catch( IOException ex ) {
      Main.showError( this, ex );
    }
  }


  /*
   * Diese Methode wird ausgefuehrt,
   * wenn auf die uebergebene Zeile doppelt geklickt wurde.
   */
  private void processLineAction( String text )
  {
    final String[] lineBegTexts = { "Fehler in Zeile ", "Warnung in Zeile " };
    if( text != null ) {
      int pos = -1;
      for( int i = 0; i < lineBegTexts.length; i++ ) {
	if( text.startsWith( lineBegTexts[ i ] ) ) {
	  pos = lineBegTexts[ i ].length();
	  break;
	}
      }
      if( pos > 0 ) {
	int len = text.length();
	if( pos < len ) {
	  char ch = text.charAt( pos++ );
	  if( (ch >= '0') && (ch <= '9') ) {
	    int lineNum = ch - '0';
	    while( pos < len ) {
	      ch = text.charAt( pos++ );
	      if( (ch < '0') || (ch > '9') ) {
		break;
	      }
	      lineNum = (lineNum * 10) + (ch - '0');
	    }
	    gotoLine( lineNum );
	  }
	}
      }
    }
  }


  private void setDataChanged( boolean state )
  {
    this.dataChanged = state;
    if( this.textDoc != null ) {
      this.mnuSave.setEnabled( state );
      this.btnSave.setEnabled( state );
    }
  }


  private void updPasteBtn( Clipboard clp )
  {
    boolean state = clp.isDataFlavorAvailable( DataFlavor.stringFlavor );
    this.mnuPaste.setEnabled( state );
    this.btnPaste.setEnabled( state );
  }


  private void updTitle()
  {
    if( this.file != null ) {
      setTitle( "JTCEMU Texteditor: " + file.getPath() );
    } else {
      setTitle( "JTCEMU Texteditor: Neuer Text" );
    }
  }


  private void updStatusText()
  {
    String text = "Bereit";
    try {
      int pos = this.textArea.getCaretPosition();
      int row = this.textArea.getLineOfOffset( pos );
      int bol = textArea.getLineStartOffset( row );
      int col = pos - bol;
      if( col > 0 ) {
	Document doc     = textArea.getDocument();
	int      tabSize = textArea.getTabSize();
	if( (doc != null) && (tabSize > 0) ) {
	  Segment seg = new Segment();
	  seg.setPartialReturn( false );
	  doc.getText( bol, col, seg );
	  col = 0;
	  int ch = seg.first();
	  while( ch != CharacterIterator.DONE ) {
	    if( ch == '\t' ) {
	      col = ((col / tabSize) + 1) * tabSize;
	    } else {
	      col++;
	    }
	    ch = seg.next();
	  }
	}
      }
      text = String.format( "Z:%d S:%d", row + 1, col + 1 );
    }
    catch( BadLocationException ex ) {}
    this.labelStatus.setText( text );
  }


  private void updUndoBtn()
  {
    boolean state = this.undoMngr.canUndo();
    this.mnuUndo.setEnabled( state );
    this.btnUndo.setEnabled( state );
  }
}

