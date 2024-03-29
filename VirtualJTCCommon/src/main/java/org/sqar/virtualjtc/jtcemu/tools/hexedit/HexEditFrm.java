/*
 * (c) 2010-2011 Jens Mueller
 * (c) 2017 Lars Sonchocky-Helldorf
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Hex-Editor
 */

package org.sqar.virtualjtc.jtcemu.tools.hexedit;

import java.awt.*;
import java.awt.dnd.*;
import java.awt.event.*;
import java.io.*;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.naming.SizeLimitExceededException;
import javax.swing.*;

import org.sqar.virtualjtc.jtcemu.Main;
import org.sqar.virtualjtc.jtcemu.base.*;


public class HexEditFrm
                extends AbstractHexCharFrm
                implements DropTargetListener
{
  private static final Locale locale = Locale.getDefault();
  private static final ResourceBundle hexEditFrmResourceBundle = ResourceBundle.getBundle("HexEditFrm", locale);

  private static final int BUF_EXTEND = 0x2000;

  private static HexEditFrm instance = null;

  private File      file;
  private byte[]    fileBytes;
  private int       fileLen;
  private int       savedPos;
  private boolean   dataChanged;
  private JMenuItem mnuNew;
  private JMenuItem mnuOpen;
  private JMenuItem mnuSave;
  private JMenuItem mnuSaveAs;
  private JMenuItem mnuPrintOptions;
  private JMenuItem mnuPrint;
  private JMenuItem mnuClose;
  private JMenuItem mnuBytesCopy;
  private JMenuItem mnuBytesSave;
  private JMenuItem mnuBytesAppend;
  private JMenuItem mnuBytesInsert;
  private JMenuItem mnuBytesOverwrite;
  private JMenuItem mnuBytesRemove;
  private JMenuItem mnuSavePos;
  private JMenuItem mnuGotoSavedPos;
  private JMenuItem mnuSelectToSavedPos;
  private JMenuItem mnuFind;
  private JMenuItem mnuFindNext;
  private JMenuItem mnuHelpContent;
  private JButton   btnNew;
  private JButton   btnOpen;
  private JButton   btnSave;
  private JButton   btnFind;


  public static boolean close()
  {
    return instance != null ? instance.doClose() : true;
  }


  public static void open()
  {
    if( instance != null ) {
      instance.setState( Frame.NORMAL );
      instance.toFront();
    } else {
      instance = new HexEditFrm();
      instance.setVisible( true );
    }
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
    // empty
  }


  @Override
  public void dragOver( DropTargetDragEvent e )
  {
    // empty
  }


  @Override
  public void drop( DropTargetDropEvent e )
  {
    File file = GUIUtil.fileDrop( this, e );
    if( file != null ) {
      openFile( file );
    }
  }


  @Override
  public void dropActionChanged( DropTargetDragEvent e )
  {
    if( !GUIUtil.isFileDrop( e ) )
      e.rejectDrag();
  }


        /* --- ueberschriebene Methoden --- */

  @Override
  public void actionPerformed( ActionEvent e )
  {
    Object src = e.getSource();
    if( src != null ) {
      if( (src == this.btnNew) || (src == this.mnuNew) ) {
        doNew();
      } else if( (src == this.btnOpen) || (src == this.mnuOpen) ) {
        doOpen();
      } else if( (src == this.btnSave) || (src == this.mnuSave) ) {
        doSave( false );
      } else if( src == this.mnuSaveAs ) {
        doSave( true );
      } else if( src == this.mnuPrintOptions ) {
        PrintOptionsDlg.open( this );
      } else if( src == this.mnuPrint ) {
        doPrint();
      } else if( src == this.mnuClose ) {
        doClose();
      } else if( src == this.mnuBytesAppend ) {
        doBytesAppend();
      } else if( src == this.mnuBytesCopy ) {
        doBytesCopy();
      } else if( src == this.mnuBytesSave ) {
        doBytesSave();
      } else if( src == this.mnuBytesInsert ) {
        doBytesInsert();
      } else if( src == this.mnuBytesOverwrite ) {
        doBytesOverwrite();
      } else if( src == this.mnuBytesRemove ) {
        doBytesRemove();
      } else if( src == this.mnuSavePos ) {
        doSavePos();
      } else if( src == this.mnuGotoSavedPos ) {
        doGotoSavedPos( false );
      } else if( src == this.mnuSelectToSavedPos ) {
        doGotoSavedPos( true );
      } else if( (src == this.btnFind) || (src == this.mnuFind) ) {
        doFind();
      } else if( src == this.mnuFindNext ) {
        doFindNext();
      } else if( src == this.mnuHelpContent ) {
        HelpFrm.open( hexEditFrmResourceBundle.getString("help.path") );
      } else {
        super.actionPerformed( e );
      }
    }
  }


  @Override
  public boolean doClose()
  {
    boolean rv = confirmDataSaved();
    if( rv ) {
      rv = super.doClose();
    }
    if( rv ) {
      instance = null;
    }
    return rv;
  }


  @Override
  public byte getDataByte( int idx )
  {
    byte rv = (byte) 0;
    if( this.fileBytes != null ) {
      if( (idx >= 0) && (idx < this.fileBytes.length) ) {
        rv = this.fileBytes[ idx ];
      }
    }
    return rv;
  }


  @Override
  public int getDataLength()
  {
    return this.fileLen;
  }


  @Override
  protected void setContentActionsEnabled( boolean state )
  {
    this.mnuSaveAs.setEnabled( state );
    this.mnuPrint.setEnabled( state );
    this.mnuFind.setEnabled( state );
    this.btnFind.setEnabled( state );
  }


  @Override
  protected void setFindNextActionsEnabled( boolean state )
  {
    this.mnuFindNext.setEnabled( state );
  }


  @Override
  protected void setSelectedByteActionsEnabled( boolean state )
  {
    this.mnuBytesCopy.setEnabled( state );
    this.mnuBytesSave.setEnabled( state );
    this.mnuBytesInsert.setEnabled( state );
    this.mnuBytesOverwrite.setEnabled( state );
    this.mnuBytesRemove.setEnabled( state );
    this.mnuSavePos.setEnabled( state );
    this.mnuSelectToSavedPos.setEnabled( state && (this.savedPos >= 0) );
  }


        /* --- Konstruktor --- */

  private HexEditFrm()
  {
    this.file        = null;
    this.fileBytes   = new byte[ 0x100 ];
    this.fileLen     = 0;
    this.savedPos    = -1;
    this.dataChanged = false;
    updTitle();


    // Menu
    JMenuBar mnuBar = new JMenuBar();
    setJMenuBar( mnuBar );


    // Menu Datei
    JMenu mnuFile = new JMenu( hexEditFrmResourceBundle.getString("menu.file") );
    mnuFile.setMnemonic( KeyEvent.VK_D );
    mnuBar.add( mnuFile );

    this.mnuNew = createJMenuItem( hexEditFrmResourceBundle.getString("menuItem.new") );
    mnuFile.add( this.mnuNew );

    this.mnuOpen = createJMenuItem( hexEditFrmResourceBundle.getString("menuItem.open") );
    mnuFile.add( this.mnuOpen );
    mnuFile.addSeparator();

    this.mnuSave = createJMenuItem(
                hexEditFrmResourceBundle.getString("menuItem.save"),
                KeyStroke.getKeyStroke( KeyEvent.VK_S, Event.CTRL_MASK ) );
    this.mnuSave.setEnabled( false );
    mnuFile.add( this.mnuSave );

    this.mnuSaveAs = createJMenuItem( hexEditFrmResourceBundle.getString("menuItem.saveAs") );
    this.mnuSaveAs.setEnabled( false );
    mnuFile.add( this.mnuSaveAs );
    mnuFile.addSeparator();

    this.mnuPrintOptions = createJMenuItem( hexEditFrmResourceBundle.getString("menuItem.printOptions") );
    mnuFile.add( this.mnuPrintOptions );

    this.mnuPrint = createJMenuItem(
                        hexEditFrmResourceBundle.getString("menuItem.print"),
                        KeyStroke.getKeyStroke(
                                KeyEvent.VK_P,
                                InputEvent.CTRL_MASK ) );
    this.mnuPrint.setEnabled( false );
    mnuFile.add( this.mnuPrint );
    mnuFile.addSeparator();

    this.mnuClose = createJMenuItem( hexEditFrmResourceBundle.getString("menuItem.close") );
    mnuFile.add( this.mnuClose );


    // Menu Bearbeiten
    JMenu mnuEdit = new JMenu( hexEditFrmResourceBundle.getString("menu.edit") );
    mnuEdit.setMnemonic( KeyEvent.VK_B );
    mnuBar.add( mnuEdit );

    this.mnuBytesCopy = createJMenuItem( hexEditFrmResourceBundle.getString("menuItem.bytesCopy") );
    this.mnuBytesCopy.setEnabled( false );
    mnuEdit.add( this.mnuBytesCopy );

    this.mnuBytesSave = createJMenuItem( hexEditFrmResourceBundle.getString("menuItem.bytesSave") );
    this.mnuBytesSave.setEnabled( false );
    mnuEdit.add( this.mnuBytesSave );
    mnuEdit.addSeparator();

    this.mnuBytesInsert = createJMenuItem(
                hexEditFrmResourceBundle.getString("menuItem.bytesInsert"),
                KeyStroke.getKeyStroke( KeyEvent.VK_I, Event.CTRL_MASK ) );
    this.mnuBytesInsert.setEnabled( false );
    mnuEdit.add( this.mnuBytesInsert );

    this.mnuBytesOverwrite = createJMenuItem(
                hexEditFrmResourceBundle.getString("menuItem.bytesOverwrite"),
                KeyStroke.getKeyStroke( KeyEvent.VK_O, Event.CTRL_MASK ) );
    this.mnuBytesOverwrite.setEnabled( false );
    mnuEdit.add( this.mnuBytesOverwrite );

    this.mnuBytesAppend = createJMenuItem(
                hexEditFrmResourceBundle.getString("menuItem.bytesAppend"),
                KeyStroke.getKeyStroke( KeyEvent.VK_E, Event.CTRL_MASK ) );
    mnuEdit.add( this.mnuBytesAppend );
    mnuEdit.addSeparator();

    this.mnuBytesRemove = createJMenuItem(
                hexEditFrmResourceBundle.getString("menuItem.bytesRemove"),
                KeyStroke.getKeyStroke( KeyEvent.VK_DELETE, 0 ) );
    this.mnuBytesRemove.setEnabled( false );
    mnuEdit.add( this.mnuBytesRemove );
    mnuEdit.addSeparator();

    this.mnuSavePos = createJMenuItem( hexEditFrmResourceBundle.getString("menuItem.savePos") );
    this.mnuSavePos.setEnabled( false );
    mnuEdit.add( this.mnuSavePos );

    this.mnuGotoSavedPos = createJMenuItem(
        hexEditFrmResourceBundle.getString("menuItem.gotoSavedPos") );
    this.mnuGotoSavedPos.setEnabled( false );
    mnuEdit.add( this.mnuGotoSavedPos );

    this.mnuSelectToSavedPos = createJMenuItem(
        hexEditFrmResourceBundle.getString("menuItem.selectToSavedPos") );
    this.mnuSelectToSavedPos.setEnabled( false );
    mnuEdit.add( this.mnuSelectToSavedPos );
    mnuEdit.addSeparator();

    this.mnuFind = createJMenuItem(
        hexEditFrmResourceBundle.getString("menuItem.find"),
                KeyStroke.getKeyStroke( KeyEvent.VK_F, Event.CTRL_MASK ) );
    this.mnuFind.setEnabled( false );
    mnuEdit.add( this.mnuFind );

    this.mnuFindNext = createJMenuItem(
        hexEditFrmResourceBundle.getString("menuItem.findNext"),
                KeyStroke.getKeyStroke( KeyEvent.VK_F3, 0 ) );
    this.mnuFindNext.setEnabled( false );
    mnuEdit.add( this.mnuFindNext );


    // Menu Hilfe
    JMenu mnuHelp = new JMenu( hexEditFrmResourceBundle.getString("menu.help") );
    mnuBar.add( mnuHelp );

    this.mnuHelpContent = createJMenuItem( hexEditFrmResourceBundle.getString("menuItem.helpContent") );
    mnuHelp.add( this.mnuHelpContent );


    // Fensterinhalt
    setLayout( new GridBagLayout() );

    GridBagConstraints gbc = new GridBagConstraints(
                                        0, 0,
                                        1, 1,
                                        1.0, 0.0,
                                        GridBagConstraints.WEST,
                                        GridBagConstraints.HORIZONTAL,
                                        new Insets( 5, 5, 5, 5 ),
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
                                hexEditFrmResourceBundle.getString("button.new") );
    toolBar.add( this.btnNew );

    this.btnOpen = GUIUtil.createImageButton(
                                this,
                                "/images/file/open.png",
                                hexEditFrmResourceBundle.getString("button.open") );
    toolBar.add( this.btnOpen );

    this.btnSave = GUIUtil.createImageButton(
                                this,
                                "/images/file/save.png",
                                hexEditFrmResourceBundle.getString("button.save") );
    this.btnSave.setEnabled( false );
    toolBar.add( this.btnSave );
    toolBar.addSeparator();

    this.btnFind = GUIUtil.createImageButton(
                                this,
                                "/images/edit/find.png",
                                hexEditFrmResourceBundle.getString("button.find") );
    this.btnFind.setEnabled( false );
    toolBar.add( this.btnFind );


    // Hex-ASCII-Anzeige
    gbc.anchor  = GridBagConstraints.CENTER;
    gbc.fill    = GridBagConstraints.BOTH;
    gbc.weighty = 1.0;
    gbc.gridy++;
    add( createHexCharFld(), gbc );

    // Anzeige der Cursor-Position
    gbc.fill    = GridBagConstraints.HORIZONTAL;
    gbc.weighty = 0.0;
    gbc.gridy++;
    add( createCaretPosFld( hexEditFrmResourceBundle.getString("label.caretPos") ), gbc );

    // Anzeige der Dezimalwerte der Bytes ab Cursor-Position
    gbc.gridy++;
    add( createValueFld(), gbc );


    // Drag&Drop aktivieren
    (new DropTarget( this.hexCharFld, this )).setActive( true );


    // sonstiges
    if( !GUIUtil.applyWindowSettings( this ) ) {
      pack();
      setLocationByPlatform( true );
    }
    setResizable( true );
  }


        /* --- Aktionen --- */

  private void doBytesAppend()
  {
    ReplyBytesDlg dlg = new ReplyBytesDlg(
                                        this,
                                        hexEditFrmResourceBundle.getString("dialog.doBytesAppend.replyBytesDlg.title"),
                                        this.lastInputFmt,
                                        this.lastBigEndian,
                                        null );
    dlg.setVisible( true );
    byte[] a = dlg.getApprovedBytes();
    if( a != null ) {
      if( a.length > 0 ) {
        int oldLen = this.fileLen;
        this.lastInputFmt  = dlg.getApprovedInputFormat();
        this.lastBigEndian = dlg.getApprovedBigEndian();
        try {
          insertBytes( this.fileLen, a, 0 );
        }
        catch( SizeLimitExceededException ex ) {
          Main.showError( this, ex.getMessage() );
        }
        setDataChanged( true );
        updView();
        setSelection( oldLen, this.fileLen - 1 );
      }
    }
  }


  private void doBytesInsert()
  {
    int caretPos = this.hexCharFld.getCaretPosition();
    if( (caretPos >= 0) && (caretPos < this.fileLen) ) {
      ReplyBytesDlg dlg = new ReplyBytesDlg(
                                        this,
                                        hexEditFrmResourceBundle.getString("dialog.doBytesInsert.replyBytesDlg.title"),
                                        this.lastInputFmt,
                                        this.lastBigEndian,
                                        null );
      dlg.setVisible( true );
      byte[] a = dlg.getApprovedBytes();
      if( a != null ) {
        if( a.length > 0 ) {
          this.lastInputFmt  = dlg.getApprovedInputFormat();
          this.lastBigEndian = dlg.getApprovedBigEndian();
          try {
            insertBytes( caretPos, a, 0 );
          }
          catch( SizeLimitExceededException ex ) {
            Main.showError( this, ex.getMessage() );
          }
          setDataChanged( true );
          updView();
          setSelection( caretPos, caretPos + a.length - 1 );
        }
      }
    }
  }


  private void doBytesOverwrite()
  {
    int caretPos = this.hexCharFld.getCaretPosition();
    if( (caretPos >= 0) && (caretPos < this.fileLen) ) {
      ReplyBytesDlg dlg = new ReplyBytesDlg(
                                        this,
                                        hexEditFrmResourceBundle.getString("dialog.doBytesOverwrite.replyBytesDlg.title"),
                                        this.lastInputFmt,
                                        this.lastBigEndian,
                                        null );
      dlg.setVisible( true );
      byte[] a = dlg.getApprovedBytes();
      if( a != null ) {
        if( a.length > 0 ) {
          this.lastInputFmt  = dlg.getApprovedInputFormat();
          this.lastBigEndian = dlg.getApprovedBigEndian();
          try {
            int src = 0;
            int dst = caretPos;
            while( (src < a.length) && (dst < this.fileLen) ) {
              this.fileBytes[ dst++ ] = a[ src++ ];
            }
            if( src < a.length ) {
              insertBytes( dst, a, src );
            }
          }
          catch( SizeLimitExceededException ex ) {
            Main.showError( this, ex.getMessage() );
          }
          setDataChanged( true );
          updView();
          setSelection( caretPos, caretPos + a.length - 1 );
        }
      }
    }
  }


  private void doBytesRemove()
  {
    int caretPos = this.hexCharFld.getCaretPosition();
    int markPos  = this.hexCharFld.getMarkPosition();
    int m1       = -1;
    int m2       = -1;
    if( (caretPos >= 0) && (markPos >= 0) ) {
      m1 = Math.min( caretPos, markPos );
      m2 = Math.max( caretPos, markPos );
    } else {
      m1 = caretPos;
      m2 = caretPos;
    }
    if( m2 >= this.fileLen ) {
      m2 = this.fileLen - 1;
    }
    if( m1 >= 0 ) {
      String msg = null;
      if( m2 > m1 ) {
        msg = String.format(
            hexEditFrmResourceBundle.getString("doBytesRemove.removeBytes.formatString"),
                m2 - m1 + 1);
      }
      else if( m2 == m1 ) {
        msg = String.format(
            hexEditFrmResourceBundle.getString("doBytesRemove.removeByte.formatString"),
                this.fileBytes[ m1 ] );
      }
      if( msg != null ) {
        JOptionPane pane = new JOptionPane(
                                        msg,
                                        JOptionPane.QUESTION_MESSAGE,
                                        JOptionPane.YES_NO_OPTION );
        pane.createDialog( this, hexEditFrmResourceBundle.getString("dialog.doBytesRemove.removeDialog.title") ).setVisible( true );
        Object option = pane.getValue();
        if( option != null ) {
          if( option.equals( JOptionPane.YES_OPTION ) ) {
            if( m2 + 1 < this.fileLen ) {
              m2++;
              while( m2 < this.fileLen ) {
                this.fileBytes[ m1++ ] = this.fileBytes[ m2++ ];
              }
            }
            this.fileLen = m1;
            setDataChanged( true );
            updView();
            setCaretPosition( m1, false );
          }
        }
      }
    }
  }


  private void doBytesSave()
  {
    int dataLen  = getDataLength();
    int caretPos = this.hexCharFld.getCaretPosition();
    int markPos  = this.hexCharFld.getMarkPosition();
    int m1       = -1;
    int m2       = -1;
    if( (caretPos >= 0) && (markPos >= 0) ) {
      m1 = Math.min( caretPos, markPos );
      m2 = Math.max( caretPos, markPos );
    } else {
      m1 = caretPos;
      m2 = caretPos;
    }
    if( m2 >= dataLen ) {
      m2 = dataLen - 1;
    }
    if( m1 >= 0 ) {
      int len = Math.min( m2, this.fileBytes.length ) - m1 + 1;
      if( len > 0 ) {
        File presetDir = null;
        if( this.file != null ) {
          presetDir = this.file.getParentFile();
        }
        if( presetDir == null ) {
          presetDir = Main.getLastPathFile();
        }
        File file = FileDlg.showFileSaveDlg(
                                        this,
                                        hexEditFrmResourceBundle.getString("dialog.doBytesSave.fileSaveDlg.title"),
                                        presetDir );
        if( file != null ) {
          try {
            OutputStream out = null;
            try {
              out = new FileOutputStream( file );
              out.write( this.fileBytes, m1, len  );
              out.close();
              out = null;
            }
            finally {
              close( out );
            }
            Main.setLastFile( file );
          }
          catch( Exception ex ) {
            Main.showError( this, ex );
          }
        }
      }
    }
  }


  private void doGotoSavedPos( boolean moveOp )
  {
    if( this.savedPos >= 0 ) {
      this.hexCharFld.setCaretPosition( this.savedPos, moveOp );
      updCaretPosFields();
    }
  }


  private void doNew()
  {
    if( confirmDataSaved() ) {
      this.fileBytes = new byte[ 0x100 ];
      this.fileLen   = 0;
      this.file = null;
      setDataChanged( false );
      updTitle();
      updView();
      setCaretPosition( -1, false );
      this.hexCharFld.setYOffset( 0 );
    }
  }


  private void doOpen()
  {
    if( confirmDataSaved() ) {
      File file = FileDlg.showFileOpenDlg(
                                this,
                                hexEditFrmResourceBundle.getString("dialog.doOpen.fileOpenDlg.title"),
                                hexEditFrmResourceBundle.getString("dialog.doOpen.fileOpenDlg.approveBtnText"),
                                Main.getLastPathFile() );
      if( file != null )
        openFile( file );
    }
  }


  private boolean doSave( boolean forceFileDlg )
  {
    boolean rv   = false;
    File    file = this.file;
    if( forceFileDlg || (file == null) ) {
      file = FileDlg.showFileSaveDlg(
                this,
                hexEditFrmResourceBundle.getString("dialog.doSave.fileSaveDlg.title"),
                file != null ? file : Main.getLastPathFile() );
    }
    if( file != null ) {
      try {
        OutputStream out = null;
        try {
          out = new FileOutputStream( file );
          if( (this.fileLen > 0) && (this.fileBytes.length > 0) ) {
            out.write(
                this.fileBytes,
                0,
                Math.min( this.fileLen, this.fileBytes.length) );
          }
          out.close();
          out       = null;
          this.file = file;
          rv        = true;
          if( !setDataChanged( false ) ) {
            updTitle();
          }
        }
        finally {
          close( out );
        }
        Main.setLastFile( file );
      }
      catch( Exception ex ) {
        Main.showError( this, ex );
      }
    }
    return rv;
  }


  private void doSavePos()
  {
    int caretPos = this.hexCharFld.getCaretPosition();
    if( caretPos >= 0 ) {
      this.savedPos = caretPos;
      this.mnuGotoSavedPos.setEnabled( true );
      this.mnuSelectToSavedPos.setEnabled( true );
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


  private boolean confirmDataSaved()
  {
    boolean rv = true;
    if( this.dataChanged ) {
      setState( Frame.NORMAL );
      toFront();
      String[] options = { 
          hexEditFrmResourceBundle.getString("confirmDataSaved.options.save"), 
          hexEditFrmResourceBundle.getString("confirmDataSaved.options.discard"), 
          hexEditFrmResourceBundle.getString("confirmDataSaved.options.cancel") 
      };
      int      selOpt  = JOptionPane.showOptionDialog(
                                this,
                                hexEditFrmResourceBundle.getString("dialog.confirmDataSaved.optionDialog.message"),
                                hexEditFrmResourceBundle.getString("dialog.confirmDataSaved.optionDialog.title"),
                                JOptionPane.YES_NO_CANCEL_OPTION,
                                JOptionPane.WARNING_MESSAGE,
                                null,
                                options,
                                hexEditFrmResourceBundle.getString("dialog.confirmDataSaved.optionDialog.initialValue") );
      if( selOpt == 0 ) {
        rv = doSave( false );
      }
      else if( selOpt != 1 ) {
        rv = false;
      }
    }
    return rv;
  }


  private void insertBytes(
                        int    dstPos,
                        byte[] srcBuf,
                        int    srcPos ) throws SizeLimitExceededException
  {
    if( (srcPos >= 0) && (srcPos < srcBuf.length) && (srcBuf.length > 0) ) {
      int diffLen = srcBuf.length - srcPos;
      int reqLen  = this.fileLen + diffLen;
      if( reqLen >= this.fileBytes.length ) {
        int n = Math.min( reqLen + BUF_EXTEND, Integer.MAX_VALUE );
        if( n < reqLen) {
          throw new SizeLimitExceededException( hexEditFrmResourceBundle.getString("error.insertBytes.sizeLimitExceeded.errorText") );
        }
        byte[] tmpBuf = new byte[ n ];
        if( dstPos > 0 ) {
          System.arraycopy( this.fileBytes, 0, tmpBuf, 0, dstPos );
        }
        System.arraycopy( srcBuf, srcPos, tmpBuf, dstPos, diffLen );
        if( dstPos < this.fileLen ) {
          System.arraycopy(
                        this.fileBytes,
                        dstPos,
                        tmpBuf,
                        dstPos + diffLen,
                        this.fileLen - dstPos );
        }
        this.fileBytes = tmpBuf;
      } else {
        for( int i = this.fileLen - 1; i >= dstPos; --i ) {
          this.fileBytes[ i + diffLen ] = this.fileBytes[ i ];
        }
        System.arraycopy( srcBuf, srcPos, this.fileBytes, dstPos, diffLen );
      }
      this.fileLen += diffLen;
    }
  }


  private void openFile( File file )
  {
    try {
      InputStream in = null;
      try {
        long len = file.length();
        if( len > Integer.MAX_VALUE ) {
          throwFileTooBig();
        }
        if( len > 0 ) {
          len = len * 10L / 9L;
        }
        if( len < BUF_EXTEND ) {
          len = BUF_EXTEND;
        } else if( len > Integer.MAX_VALUE ) {
          len = Integer.MAX_VALUE;
        }
        byte[] fileBytes = new byte[ (int) len ];
        int    fileLen   = 0;

        in = new FileInputStream( file );
        while( fileLen < fileBytes.length ) {
          int n = in.read( fileBytes, fileLen, fileBytes.length - fileLen );
          if( n <= 0 ) {
            break;
          }
          fileLen += n;
        }
        if( fileLen >= fileBytes.length ) {
          int b = in.read();
          while( b != -1 ) {
            if( fileLen >= fileBytes.length ) {
              int n = Math.min( fileLen + BUF_EXTEND, Integer.MAX_VALUE );
              if( fileLen >= n ) {
                throwFileTooBig();
              }
              byte[] a = new byte[ n ];
              System.arraycopy( fileBytes, 0, a, 0, fileLen );
              fileBytes = a;
            }
            fileBytes[ fileLen++ ] = (byte) b;
            b = in.read();
          }
        }
        in.close();

        this.file      = file;
        this.fileBytes = fileBytes;
        this.fileLen   = fileLen;
        this.savedPos  = -1;
        this.mnuGotoSavedPos.setEnabled( false );
        this.mnuSelectToSavedPos.setEnabled( false );
        if( !setDataChanged( false ) ) {
          updTitle();
        }
        updView();
        setCaretPosition( -1, false );
        this.hexCharFld.setYOffset( 0 );
        Main.setLastFile( file );
      }
      finally {
        close( in );
      }
    }
    catch( IOException ex ) {
      Main.showError( this, ex );
    }
  }


  private boolean setDataChanged( boolean state )
  {
    boolean rv = false;
    if( state != this.dataChanged ) {
      this.dataChanged = state;
      updTitle();
      this.mnuSave.setEnabled( this.dataChanged );
      this.btnSave.setEnabled( this.dataChanged );
      rv = true;
    }
    return rv;
  }


  private static void throwFileTooBig() throws IOException
  {
    throw new IOException( hexEditFrmResourceBundle.getString("error.throwFileTooBig.errorText") );
  }


  private void updTitle()
  {
    StringBuilder buf = new StringBuilder( 128 );
    buf.append( hexEditFrmResourceBundle.getString("window.title") );
    if( this.file != null ) {
      buf.append( file.getPath() );
    } else {
      buf.append( hexEditFrmResourceBundle.getString("window.title.newDocument") );
    }
    setTitle( buf.toString() );
  }
}

