/*
 * (c) 2007-2011 Jens Mueller
 * (c) 2017 Lars Sonchocky-Helldorf
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Fenster fuer Reassembler
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


public class ReassFrm extends AbstractTextFrm implements
                                                ActionListener,
                                                CaretListener
{
  private static final Locale locale = Locale.getDefault();
  private static final ResourceBundle reassFrmResourceBundle = ResourceBundle.getBundle("resources.ReassFrm", locale);

  private static ReassFrm instance = null;

  private Z8Reassembler  z8Reass;
  private File           lastFile;
  private JTextComponent selectionFld;
  private JMenuItem      mnuReassemble;
  private JMenuItem      mnuSaveAs;
  private JMenuItem      mnuPrintOptions;
  private JMenuItem      mnuPrint;
  private JMenuItem      mnuClose;
  private JMenuItem      mnuCopy;
  private JMenuItem      mnuSelectAll;
  private JMenuItem      mnuHelpContent;
  private JTextField     fldBegAddr;
  private JTextField     fldEndAddr;


  public static void close()
  {
    if( instance != null )
      instance.doClose();
  }


  public static void open( Z8Memory memory )
  {
    if( instance != null ) {
      instance.setState( Frame.NORMAL );
      instance.toFront();
    } else {
      instance = new ReassFrm( memory );
      instance.setVisible( true );
    }
  }


        /* --- ActionListener --- */

  @Override
  public void actionPerformed( ActionEvent e )
  {
    Object src = e.getSource();
    if( src != null ) {
      if( src == this.fldBegAddr ) {
        this.fldEndAddr.requestFocus();
      }
      else if( (src == this.fldEndAddr) || (src == this.mnuReassemble) ) {
        doReassemble();
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
        HelpFrm.open( reassFrmResourceBundle.getString("help.path") );
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
      instance = null;
    }
    return rv;
  }


        /* --- Konstruktor --- */

  private ReassFrm( Z8Memory memory )
  {
    setTitle( reassFrmResourceBundle.getString("window.title") );
    this.z8Reass      = new Z8Reassembler( memory );
    this.lastFile     = null;
    this.selectionFld = null;


    // Menu
    JMenuBar mnuBar = new JMenuBar();
    setJMenuBar( mnuBar );


    // Menu Datei
    JMenu mnuFile = new JMenu( reassFrmResourceBundle.getString("menu.file") );
    mnuFile.setMnemonic( 'D' );
    mnuBar.add( mnuFile );

    this.mnuReassemble = new JMenuItem( reassFrmResourceBundle.getString("menuItem.reassemble") );
    this.mnuReassemble.setAccelerator(
                        KeyStroke.getKeyStroke(
                                        KeyEvent.VK_R,
                                        InputEvent.CTRL_MASK ) );
    this.mnuReassemble.addActionListener( this );
    mnuFile.add( this.mnuReassemble );
    mnuFile.addSeparator();

    this.mnuSaveAs = new JMenuItem( reassFrmResourceBundle.getString("menuItem.saveAs") );
    this.mnuSaveAs.setAccelerator(
                KeyStroke.getKeyStroke(
                        KeyEvent.VK_S,
                        InputEvent.CTRL_MASK | InputEvent.SHIFT_MASK ) );
    this.mnuSaveAs.setEnabled( false );
    this.mnuSaveAs.addActionListener( this );
    mnuFile.add( this.mnuSaveAs );
    mnuFile.addSeparator();

    this.mnuPrintOptions = new JMenuItem( reassFrmResourceBundle.getString("menuItem.printOptions") );
    this.mnuPrintOptions.addActionListener( this );
    mnuFile.add( this.mnuPrintOptions );

    this.mnuPrint = new JMenuItem( reassFrmResourceBundle.getString("menuItem.print") );
    this.mnuPrint.setAccelerator( KeyStroke.getKeyStroke(
                                        KeyEvent.VK_P,
                                        InputEvent.CTRL_MASK ) );
    this.mnuPrint.setEnabled( false );
    this.mnuPrint.addActionListener( this );
    mnuFile.add( this.mnuPrint );
    mnuFile.addSeparator();

    this.mnuClose = new JMenuItem( reassFrmResourceBundle.getString("menuItem.close") );
    this.mnuClose.addActionListener( this );
    mnuFile.add( this.mnuClose );


    // Menu Bearbeiten
    JMenu mnuEdit = new JMenu( reassFrmResourceBundle.getString("menu.edit") );
    mnuEdit.setMnemonic( 'B' );
    mnuBar.add( mnuEdit );

    this.mnuCopy = new JMenuItem( reassFrmResourceBundle.getString("menuItem.copy") );
    this.mnuCopy.setAccelerator( KeyStroke.getKeyStroke(
                                        KeyEvent.VK_C,
                                        InputEvent.CTRL_MASK ) );
    this.mnuCopy.setEnabled( false );
    this.mnuCopy.addActionListener( this );
    mnuEdit.add( this.mnuCopy );
    mnuEdit.addSeparator();

    this.mnuSelectAll = new JMenuItem( reassFrmResourceBundle.getString("menuItem.selectAll") );
    this.mnuSelectAll.setEnabled( false );
    this.mnuSelectAll.addActionListener( this );
    mnuEdit.add( this.mnuSelectAll );


    // Menu Hilfe
    JMenu mnuHelp = new JMenu( reassFrmResourceBundle.getString("menu.help") );
    mnuBar.add( mnuHelp );

    this.mnuHelpContent = new JMenuItem( reassFrmResourceBundle.getString("menuItem.helpContent") );
    this.mnuHelpContent.addActionListener( this );
    mnuHelp.add( this.mnuHelpContent );


    // Fensterinhalt
    setLayout( new BorderLayout() );


    // Kopfbereich
    JPanel panelHead = new JPanel( new GridBagLayout() );
    add( panelHead, BorderLayout.NORTH );

    GridBagConstraints gbcHead = new GridBagConstraints(
                                        0, 0,
                                        1, 1,
                                        0.0, 0.0,
                                        GridBagConstraints.WEST,
                                        GridBagConstraints.NONE,
                                        new Insets( 5, 5, 5, 5 ),
                                        0, 0 );

    panelHead.add( new JLabel( reassFrmResourceBundle.getString("label.startAddress") ), gbcHead );

    this.fldBegAddr = new JTextField( 4 );
    this.fldBegAddr.addActionListener( this );
    this.fldBegAddr.addCaretListener( this );
    gbcHead.fill    = GridBagConstraints.HORIZONTAL;
    gbcHead.weightx = 0.5;
    gbcHead.gridx++;
    panelHead.add( this.fldBegAddr, gbcHead );

    gbcHead.fill    = GridBagConstraints.NONE;
    gbcHead.weightx = 0.0;
    gbcHead.gridx++;
    panelHead.add( new JLabel( reassFrmResourceBundle.getString("label.endAddress") ), gbcHead );

    this.fldEndAddr = new JTextField( 4 );
    this.fldEndAddr.addActionListener( this );
    this.fldEndAddr.addCaretListener( this );
    gbcHead.fill    = GridBagConstraints.HORIZONTAL;
    gbcHead.weightx = 0.5;
    gbcHead.gridx++;
    panelHead.add( this.fldEndAddr, gbcHead );


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
      this.fldBegAddr.setColumns( 0 );
      this.fldEndAddr.setColumns( 0 );
      this.textArea.setColumns( 0 );
      this.textArea.setRows( 0 );
    }
  }


        /* --- Aktionen --- */

  private void doReassemble()
  {
    try {
      int    begAddr = GUIUtil.parseHex4( this.fldBegAddr, reassFrmResourceBundle.getString("doReassemble.begAddr") );
      int    endAddr = begAddr;
      String text    = this.fldEndAddr.getText();
      if( text != null ) {
        if( !text.isEmpty() ) {
          endAddr = GUIUtil.parseHex4( this.fldEndAddr, reassFrmResourceBundle.getString("doReassemble.endAddr") );
        }
      }
      StringBuilder buf = new StringBuilder( 0x4000 );
      this.z8Reass.reassemble( buf, begAddr, endAddr );
      setText( buf.toString() );
      this.textArea.requestFocus();
      if( buf.length() > 0 ) {
        this.mnuSaveAs.setEnabled( true );
        this.mnuPrint.setEnabled( true );
        this.mnuSelectAll.setEnabled( true );
      }
    }
    catch( ParseException ex ) {
      JOptionPane.showMessageDialog(
                        this,
                        ex.getMessage(),
                        reassFrmResourceBundle.getString("dialog.doReassemble.inputError.title"),
                        JOptionPane.ERROR_MESSAGE );
    }
  }


  private void doSaveAs()
  {
    File file = FileDlg.showFileSaveDlg(
                                this,
                                reassFrmResourceBundle.getString("dialog.doReassemble.fileSaveDlg.title"),
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
