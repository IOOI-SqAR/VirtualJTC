/*
 * (c) 2007-2011 Jens Mueller
 * (c) 2017 Lars Sonchocky-Helldorf
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Fenster fuer Instruction Tracer
 */

package jtcemu.tools;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.text.ParseException;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.JTextComponent;
import jtcemu.base.*;
import jtcemu.Main;
import z8.*;


public class InstructionTracerFrm extends AbstractTextFrm implements
                                                ActionListener,
                                                CaretListener, 
                                                Appendable
{
  private static final Locale locale = Locale.getDefault();
  private static final ResourceBundle instructionTracerFrmResourceBundle = ResourceBundle.getBundle("resources.InstructionTracerFrm", locale);

  private static InstructionTracerFrm instance = null;

  private InstructionTracer instructionTracer;
  private Z8             z8;
  private File           lastFile;
  private JTextComponent selectionFld;
  private JMenuItem      mnuRun;
  private JMenuItem      mnuStop;
  private JMenuItem      mnuSaveAs;
  private JMenuItem      mnuPrintOptions;
  private JMenuItem      mnuPrint;
  private JMenuItem      mnuClose;
  private JMenuItem      mnuCopy;
  private JMenuItem      mnuSelectAll;
  private JMenuItem      mnuHelpContent;
  private JButton btnRun;
  private JButton btnStop;


  public static void close()
  {
    if( instance != null )    
      instance.doClose();
  }


  public static void open( Z8 z8 )
  {
    if( instance != null ) {
      instance.setState( Frame.NORMAL );
      instance.toFront();
    } else {
      instance = new InstructionTracerFrm( z8 );
      instance.setVisible( true );
    }
  }


        /* --- ActionListener --- */

  @Override
  public void actionPerformed( ActionEvent e )
  {
    Object src = e.getSource();
    if( src != null ) {
      if ( (src == this.mnuRun) || (src == this.btnRun) ) {
        this.btnRun.setEnabled(false);
        this.btnStop.setEnabled(true);
        
        this.z8.addPCListener(Z8PCListener.ALL_ADDRESSES, this.instructionTracer);
      }
      else if ( (src == this.mnuStop) || (src == this.btnStop) ) {
        this.btnStop.setEnabled(false);
        this.btnRun.setEnabled(true);
        
        this.z8.removePCListener(Z8PCListener.ALL_ADDRESSES, this.instructionTracer);
      }
      else if( src == this.mnuSaveAs ) {
        doSaveAs();
      }
      else if( src == this.mnuPrintOptions ) {
        PrintOptionsDlg.open( this );
      }
      else if( src == this.mnuPrint ) {
        doPrint();
      }
      else if( src == this.mnuClose ) {
        doClose();
      }
      else if( src == this.mnuCopy ) {
        if( this.selectionFld != null ){
          this.selectionFld.copy();
        }
      }
      else if( src == this.mnuSelectAll ) {
        this.textArea.requestFocus();
        this.textArea.selectAll();
      }
      else if( src == this.mnuHelpContent ) {
        HelpFrm.open( instructionTracerFrmResourceBundle.getString("help.path") );
      }
    }
  }


        /* --- CaretListener --- */

  @Override
  public void caretUpdate( CaretEvent e )
  {
    Object src = e.getSource();
    if( src != null ) {
      if( src instanceof JTextComponent ) {
        this.selectionFld = (JTextComponent) src;
        int begPos        = this.selectionFld.getSelectionStart();
        this.mnuCopy.setEnabled( (begPos >= 0)
                && (begPos < this.selectionFld.getSelectionEnd()) );
      }
    }
  }


        /* --- ueberschriebene Methoden --- */

  @Override
  protected boolean doClose()
  {
    boolean rv = super.doClose();
    if( rv ) {
      this.z8.removePCListener(Z8PCListener.ALL_ADDRESSES, this.instructionTracer);

      instance = null;
    }
    return rv;
  }


        /* --- Konstruktor --- */

  private InstructionTracerFrm( Z8 z8 )
  {
    setTitle( instructionTracerFrmResourceBundle.getString("window.title") );
    this.z8 = z8;
    this.lastFile     = null;
    this.selectionFld = null;

    this.instructionTracer = new InstructionTracer(z8, this);

    // Menu
    JMenuBar mnuBar = new JMenuBar();
    setJMenuBar( mnuBar );


    // Menu Datei
    JMenu mnuFile = new JMenu( instructionTracerFrmResourceBundle.getString("menu.file") );
    mnuFile.setMnemonic( 'D' );
    mnuBar.add( mnuFile );

    this.mnuRun = new JMenuItem( instructionTracerFrmResourceBundle.getString("menuItem.run") );
    this.mnuRun.setAccelerator(
                        KeyStroke.getKeyStroke(
                                        KeyEvent.VK_R,
                                        InputEvent.CTRL_MASK ) );
    this.mnuRun.addActionListener( this );
    mnuFile.add( this.mnuRun );

    this.mnuStop = new JMenuItem( instructionTracerFrmResourceBundle.getString("menuItem.stop") );
    this.mnuStop.setAccelerator(
                        KeyStroke.getKeyStroke(
                                        KeyEvent.VK_R,
                                        InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK ) );
    this.mnuStop.addActionListener( this );
    mnuFile.add( this.mnuStop );
    mnuFile.addSeparator();

    this.mnuSaveAs = new JMenuItem( instructionTracerFrmResourceBundle.getString("menuItem.saveAs") );
    this.mnuSaveAs.setAccelerator(
                KeyStroke.getKeyStroke(
                        KeyEvent.VK_S,
                        InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK ) );
    this.mnuSaveAs.setEnabled( false );
    this.mnuSaveAs.addActionListener( this );
    mnuFile.add( this.mnuSaveAs );
    mnuFile.addSeparator();

    this.mnuPrintOptions = new JMenuItem( instructionTracerFrmResourceBundle.getString("menuItem.printOptions") );
    this.mnuPrintOptions.addActionListener( this );
    mnuFile.add( this.mnuPrintOptions );

    this.mnuPrint = new JMenuItem( instructionTracerFrmResourceBundle.getString("menuItem.print") );
    this.mnuPrint.setAccelerator( KeyStroke.getKeyStroke(
                                        KeyEvent.VK_P,
                                        InputEvent.CTRL_MASK ) );
    this.mnuPrint.setEnabled( false );
    this.mnuPrint.addActionListener( this );
    mnuFile.add( this.mnuPrint );
    mnuFile.addSeparator();

    this.mnuClose = new JMenuItem( instructionTracerFrmResourceBundle.getString("menuItem.close") );
    this.mnuClose.addActionListener( this );
    mnuFile.add( this.mnuClose );


    // Menu Bearbeiten
    JMenu mnuEdit = new JMenu( instructionTracerFrmResourceBundle.getString("menu.edit") );
    mnuEdit.setMnemonic( 'B' );
    mnuBar.add( mnuEdit );

    this.mnuCopy = new JMenuItem( instructionTracerFrmResourceBundle.getString("menuItem.copy") );
    this.mnuCopy.setAccelerator( KeyStroke.getKeyStroke(
                                        KeyEvent.VK_C,
                                        InputEvent.CTRL_MASK ) );
    this.mnuCopy.setEnabled( false );
    this.mnuCopy.addActionListener( this );
    mnuEdit.add( this.mnuCopy );
    mnuEdit.addSeparator();

    this.mnuSelectAll = new JMenuItem( instructionTracerFrmResourceBundle.getString("menuItem.selectAll") );
    this.mnuSelectAll.setEnabled( false );
    this.mnuSelectAll.addActionListener( this );
    mnuEdit.add( this.mnuSelectAll );


    // Menu Hilfe
    JMenu mnuHelp = new JMenu( instructionTracerFrmResourceBundle.getString("menu.help") );
    mnuBar.add( mnuHelp );

    this.mnuHelpContent = new JMenuItem( instructionTracerFrmResourceBundle.getString("menuItem.helpContent") );
    this.mnuHelpContent.addActionListener( this );
    mnuHelp.add( this.mnuHelpContent );


    // Fensterinhalt
    setLayout( new BorderLayout() );


    // Kopfbereich
    JPanel panelHead = new JPanel( new FlowLayout( FlowLayout.LEFT, 0, 0 ) );
    add( panelHead, BorderLayout.NORTH );

    // Werkzeugleiste
    JToolBar toolBar = new JToolBar();
    toolBar.setFloatable( false );
    toolBar.setBorderPainted( false );
    toolBar.setOrientation( JToolBar.HORIZONTAL );
    toolBar.setRollover( true );
    panelHead.add( toolBar );

    this.btnRun = GUIUtil.createImageButton(
                        this,
                        "/images/debug/run.png",
                        instructionTracerFrmResourceBundle.getString("button.run") );
    toolBar.add( this.btnRun );

    this.btnStop = GUIUtil.createImageButton(
                        this,
                        "/images/debug/stop.png",
                        instructionTracerFrmResourceBundle.getString("button.stop") );
    this.btnStop.setEnabled( false );
    toolBar.add( this.btnStop );

    // Ergebnisbereich
    this.textArea.setColumns( 40 );
    this.textArea.setRows( 20 );
    this.textArea.setEditable( false );
    this.textArea.setMargin( new Insets( 5, 5, 5, 5 ) );
    this.textArea.addCaretListener( this );
    add( new JScrollPane( this.textArea ), BorderLayout.CENTER );


    // sonstiges
    setResizable( true );
    if( !GUIUtil.applyWindowSettings( this ) ) {
      pack();
      setLocationByPlatform( true );
      this.textArea.setColumns( 0 );
      this.textArea.setRows( 0 );
    }
  }


        /* --- Aktionen --- */

  /* (non-Javadoc)
   * @see jtcemu.tools.Appendable#appendLine(java.lang.String)
   */
  @Override
  public void appendLine(String line)
  {
    String oldContent = this.textArea.getText();
    String newContent = oldContent + line + "\n";
    setText( newContent );
    
    this.textArea.requestFocus();
    
    if( newContent.length() > 0 ) {
      this.mnuSaveAs.setEnabled( true );
      this.mnuPrint.setEnabled( true );
      this.mnuSelectAll.setEnabled( true );
    }
  }


  protected void setText( String text )
  {
    try {
      this.textArea.setText( text );
      this.textArea.setCaretPosition( text.length() );
    }
    catch( IllegalArgumentException ex ) {}
  }

  private void doSaveAs()
  {
    File file = FileDlg.showFileSaveDlg(
                                this,
                                instructionTracerFrmResourceBundle.getString("dialog.doSaveAs.fileSaveDlg.title"),
                                this.lastFile != null ?
                                        this.lastFile
                                        : Main.getLastPathFile(),
                                GUIUtil.textFileFilter );
    if( file != null ) {
      try {
        Writer out = null;
        try {
          out = new BufferedWriter( new FileWriter( file ) );
          this.textArea.write( out );
          out.close();
          out           = null;
          this.lastFile = file;
          Main.setLastFile( file );
        }
        finally {
          if( out != null ) {
            try {
              out.close();
            }
            catch( IOException ex ) {}
          }
        }
      }
      catch( IOException ex ) {
        Main.showError( this, ex );
      }
    }
  }
}
