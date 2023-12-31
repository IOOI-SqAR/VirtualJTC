/*
 * (c) 2007-2010 Jens Mueller
 * (c) 2017 Lars Sonchocky-Helldorf
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Dialog fuer Laden einer Datei
 */

package org.sqar.virtualjtc.jtcemu.base;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.swing.*;

import org.sqar.virtualjtc.jtcemu.Main;
import org.sqar.virtualjtc.z8.Z8Memory;


public class LoadDlg extends BaseDlg implements ActionListener
{
  private static final Locale locale = Locale.getDefault();
  private static final ResourceBundle loadDlgResourceBundle = ResourceBundle.getBundle("LoadDlg", locale);

  private static final String textBegAddr = loadDlgResourceBundle.getString("text.begAddr");
  private static final String textEndAddr = loadDlgResourceBundle.getString("text.EndAddr");

  private Z8Memory memory;
  private byte[]       fileBytes;
  private int          fileLen;
  private File         file;
  private FileInfo     fileInfo;
  private JLabel       labelFileBegAddr;
  private JLabel       labelFileEndAddr;
  private JLabel       labelFileDesc;
  private JTextField   fldFileDesc;
  private JTextField   fldFileBegAddr;
  private JTextField   fldFileEndAddr;
  private JTextField   fldBegAddr;
  private JTextField   fldEndAddr;
  private JRadioButton btnFmtJTC;
  private JRadioButton btnFmtTAP;
  private JRadioButton btnFmtBIN;
  private JRadioButton btnFmtHEX;
  private JButton      btnLoad;
  private JButton      btnHelp;
  private JButton      btnCancel;


  public LoadDlg(
                Window   owner,
                Z8Memory memory,
                byte[]   fileBytes,
                int      fileLen,
                File     file )
  {
    super( owner );
    setTitle( loadDlgResourceBundle.getString("window.title") + file.getName() );
    this.memory    = memory;
    this.fileBytes = fileBytes;
    this.fileLen   = Math.min( fileLen, fileBytes.length );
    this.file      = file;


    // Fensterinhalt
    setLayout( new GridBagLayout() );

    GridBagConstraints gbc = new GridBagConstraints(
                                        0, 0,
                                        1, 1,
                                        1.0, 0.0,
                                        GridBagConstraints.CENTER,
                                        GridBagConstraints.HORIZONTAL,
                                        new Insets( 5, 5, 5, 5 ),
                                        0, 0 );


    // Bereich Dateiformat
    JPanel panelFmt = new JPanel( new GridBagLayout() );
    panelFmt.setBorder( BorderFactory.createTitledBorder( loadDlgResourceBundle.getString("titledBorder.fileFormat") ) );
    add( panelFmt, gbc );

    GridBagConstraints gbcFmt = new GridBagConstraints(
                                        0, 0,
                                        GridBagConstraints.REMAINDER, 1,
                                        0.0, 0.0,
                                        GridBagConstraints.WEST,
                                        GridBagConstraints.NONE,
                                        new Insets( 5, 5, 5, 5 ),
                                        0, 0 );


    JPanel panelFmtBtn = new JPanel(
                                new FlowLayout( FlowLayout.LEFT, 5, 5 ) );
    panelFmt.add( panelFmtBtn, gbcFmt );

    ButtonGroup grpFmt = new ButtonGroup();

    this.btnFmtJTC = new JRadioButton( "JTC/KCC", false );
    this.btnFmtJTC.addActionListener( this );
    grpFmt.add( this.btnFmtJTC );
    panelFmtBtn.add( this.btnFmtJTC );

    this.btnFmtTAP = new JRadioButton( "KC-TAP", false );
    this.btnFmtTAP.addActionListener( this );
    grpFmt.add( this.btnFmtTAP );
    panelFmtBtn.add( this.btnFmtTAP );

    this.btnFmtBIN = new JRadioButton( "BIN", true );
    this.btnFmtBIN.addActionListener( this );
    grpFmt.add( this.btnFmtBIN );
    panelFmtBtn.add( this.btnFmtBIN );

    this.btnFmtHEX = new JRadioButton( "Intel-HEX", false );
    this.btnFmtHEX.addActionListener( this );
    grpFmt.add( this.btnFmtHEX );
    panelFmtBtn.add( this.btnFmtHEX );


    this.labelFileBegAddr = new JLabel( loadDlgResourceBundle.getString("label.labelFileBegAddr") );
    gbcFmt.anchor    = GridBagConstraints.EAST;
    gbcFmt.gridwidth = 1;
    gbcFmt.gridx     = 0;
    gbcFmt.gridy++;
    panelFmt.add( this.labelFileBegAddr, gbcFmt );

    this.fldFileBegAddr = createJTextField( 4 );
    this.fldFileBegAddr.setEditable( false );
    gbcFmt.anchor  = GridBagConstraints.WEST;
    gbcFmt.fill    = GridBagConstraints.HORIZONTAL;
    gbcFmt.weightx = 0.5;
    gbcFmt.gridx++;
    panelFmt.add( this.fldFileBegAddr, gbcFmt );

    this.labelFileEndAddr = new JLabel( loadDlgResourceBundle.getString("label.labelFileEndAddr") );
    gbcFmt.anchor  = GridBagConstraints.EAST;
    gbcFmt.fill    = GridBagConstraints.NONE;
    gbcFmt.weightx = 0.0;
    gbcFmt.gridx++;
    panelFmt.add( this.labelFileEndAddr, gbcFmt );

    this.fldFileEndAddr = createJTextField( 4 );
    this.fldFileEndAddr.setEditable( false );
    gbcFmt.anchor  = GridBagConstraints.WEST;
    gbcFmt.fill    = GridBagConstraints.HORIZONTAL;
    gbcFmt.weightx = 0.5;
    gbcFmt.gridx++;
    panelFmt.add( this.fldFileEndAddr, gbcFmt );

    this.labelFileDesc = new JLabel( loadDlgResourceBundle.getString("label.labelFileDesc") );
    gbcFmt.anchor     = GridBagConstraints.EAST;
    gbcFmt.fill       = GridBagConstraints.NONE;
    gbcFmt.weightx    = 0.0;
    gbcFmt.insets.top = 0;
    gbcFmt.gridx      = 0;
    gbcFmt.gridy++;
    panelFmt.add( this.labelFileDesc, gbcFmt );

    this.fldFileDesc = createJTextField( 0 );
    this.fldFileDesc.setEditable( false );
    gbcFmt.anchor    = GridBagConstraints.WEST;
    gbcFmt.fill      = GridBagConstraints.HORIZONTAL;
    gbcFmt.weightx   = 1.0;
    gbcFmt.gridwidth = 3;
    gbcFmt.gridx++;
    panelFmt.add( this.fldFileDesc, gbcFmt );


    // Bereich Ladeadressen
    JPanel panelAddr = new JPanel( new GridBagLayout() );
    panelAddr.setBorder( BorderFactory.createTitledBorder( loadDlgResourceBundle.getString("titledBorder.addresses") ) );
    gbc.gridy++;
    add( panelAddr, gbc );

    GridBagConstraints gbcAddr = new GridBagConstraints(
                                        0, 0,
                                        1, 1,
                                        0.0, 0.0,
                                        GridBagConstraints.WEST,
                                        GridBagConstraints.NONE,
                                        new Insets( 5, 5, 5, 5 ),
                                        0, 0 );

    panelAddr.add( new JLabel( textBegAddr + ":" ), gbcAddr );

    this.fldBegAddr = createJTextField( 4 );
    this.fldBegAddr.addActionListener( this );
    gbcAddr.fill    = GridBagConstraints.HORIZONTAL;
    gbcAddr.weightx = 0.5;
    gbcAddr.gridx++;
    panelAddr.add( this.fldBegAddr, gbcAddr );

    gbcAddr.fill    = GridBagConstraints.NONE;
    gbcAddr.weightx = 0.0;
    gbcAddr.gridx++;
    panelAddr.add( new JLabel( textEndAddr + ":" ), gbcAddr );

    this.fldEndAddr = createJTextField( 4 );
    this.fldEndAddr.addActionListener( this );
    gbcAddr.fill    = GridBagConstraints.HORIZONTAL;
    gbcAddr.weightx = 0.5;
    gbcAddr.gridx++;
    panelAddr.add( this.fldEndAddr, gbcAddr );


    // Knoepfe
    JPanel panelBtn = new JPanel( new GridLayout( 1, 3, 5, 5 ) );
    gbc.fill       = GridBagConstraints.NONE;
    gbc.weightx    = 0.0;
    gbc.insets.top = 10;
    gbc.gridy++;
    add( panelBtn,gbc );

    this.btnLoad = new JButton( loadDlgResourceBundle.getString("button.load") );
    this.btnLoad.addActionListener( this );
    panelBtn.add( this.btnLoad );

    this.btnHelp = new JButton( loadDlgResourceBundle.getString("button.help") );
    this.btnHelp.addActionListener( this );
    panelBtn.add( this.btnHelp );

    this.btnCancel = new JButton( loadDlgResourceBundle.getString("button.cancel") );
    this.btnCancel.addActionListener( this );
    panelBtn.add( this.btnCancel );


    // Fenstergroesse
    pack();
    this.fldFileBegAddr.setColumns( 0 );
    this.fldFileEndAddr.setColumns( 0 );
    this.fldBegAddr.setColumns( 0 );
    this.fldEndAddr.setColumns( 0 );
    setParentCentered();
    setResizable( true );


    // Datei analysieren
    boolean done  = false;
    this.fileInfo = FileInfo.analyzeFile( this.fileBytes, this.fileLen );
    if( this.fileInfo != null ) {
      switch( fileInfo.getFormat() ) {
        case JTC:
          this.btnFmtJTC.setSelected( true );
          this.fldBegAddr.setText(
                this.fileInfo.getBegAddrText( FileInfo.Format.JTC ) );
          done = true;
          break;

        case TAP:
          this.btnFmtTAP.setSelected( true );
          this.fldBegAddr.setText(
                this.fileInfo.getBegAddrText( FileInfo.Format.TAP ) );
          done = true;
          break;

        case HEX:
          this.btnFmtHEX.setSelected( true );
          this.fldBegAddr.setText(
                this.fileInfo.getBegAddrText( FileInfo.Format.HEX ) );
          done = true;
          break;
      }
    }
    if( !done ) {
      this.btnFmtBIN.setSelected( true );

      // Standard-Ladeadresse
      this.fldBegAddr.setText( "E000" );

      /*
       * Bei einer BIN-Datei versuchen,
       * die Ladeadressen aus dem Dateinamen zu ermitteln
       */
      String fileName = file.getName();
      if( fileName != null ) {
        fileName = fileName.toUpperCase();
        int len  = fileName.length();
        int pos  = fileName.indexOf( '_' );
        while( (pos >= 0) && ((pos + 4) < len) ) {
          if( isHexChar( fileName.charAt( pos + 1 ) )
              && isHexChar( fileName.charAt( pos + 2 ) )
              && isHexChar( fileName.charAt( pos + 3 ) )
              && isHexChar( fileName.charAt( pos + 4 ) ) )
          {
            this.fldBegAddr.setText( fileName.substring( pos + 1, pos + 5 ) );
            if( (pos + 9) < len ) {
              char ch = fileName.charAt( pos + 5 );
              if( ((ch == '_') || (ch == '-'))
                  && isHexChar( fileName.charAt( pos + 6 ) )
                  && isHexChar( fileName.charAt( pos + 7 ) )
                  && isHexChar( fileName.charAt( pos + 8 ) )
                  && isHexChar( fileName.charAt( pos + 9 ) ) )
              {
                this.fldEndAddr.setText(
                                fileName.substring( pos + 6, pos + 10 ) );
              }
            }
            break;
          }
          if( pos + 5 < len ) {
            pos = fileName.indexOf( '_', pos + 1 );
          } else {
            pos = -1;
          }
        }
      }
    }


    // Listener fuer Dateiformatumschaltung
    this.btnFmtJTC.addActionListener( this );
    this.btnFmtTAP.addActionListener( this );
    this.btnFmtBIN.addActionListener( this );
    this.btnFmtHEX.addActionListener( this );
    updFileMetaFields();
  }


        /* --- ActionListener --- */

  @Override
  public void actionPerformed( ActionEvent e )
  {
    Object src = e.getSource();
    if( (src == this.fldEndAddr) || (src == this.btnLoad) ) {
      doLoad();
    }
    else if( src == this.btnHelp ) {
      HelpFrm.open( loadDlgResourceBundle.getString("help.loadsave.path") );
    }
    else if( src == this.btnCancel ) {
      doClose();
    }
    else if( src == this.fldBegAddr ) {
      this.fldBegAddr.transferFocus();
    } else if( src instanceof JRadioButton ) {
      updFileMetaFields();
    }
  }


        /* --- ueberschriebene Methoden --- */

  @Override
  public void windowOpened( WindowEvent e )
  {
    if( e.getWindow() == this ) {
      if( this.btnFmtJTC.isSelected() ) {
        this.btnFmtJTC.requestFocus();
      }
      else if( this.btnFmtTAP.isSelected() ) {
        this.btnFmtTAP.requestFocus();
      }
      else if( this.btnFmtBIN.isSelected() ) {
        this.btnFmtBIN.requestFocus();
      }
      else if( this.btnFmtHEX.isSelected() ) {
        this.btnFmtHEX.requestFocus();
      }
    }
  }


        /* --- private Methoden --- */

  private JTextField createJTextField( int cols )
  {
    JTextField fld = new JTextField( cols );
    Dimension  pSize = fld.getPreferredSize();
    if( pSize != null ) {
      if( pSize.height > 0 )
        fld.setPreferredSize( new Dimension( 1, pSize.height ) );
    }
    return fld;
  }


  private void doLoad()
  {
    try {
      int endAddr = 0xFFFF;
      int begAddr = GUIUtil.parseHex4(
                                this.fldBegAddr.getText(),
                                textBegAddr );

      String text = this.fldEndAddr.getText();
      if( text != null ) {
        text = text.trim();
        if( !text.isEmpty() ) {
          endAddr = GUIUtil.parseHex4( text, textEndAddr );
        }
      }
      if( this.btnFmtJTC.isSelected() ) {
        handleBtnFmtJTC(endAddr, begAddr);
      }
      else if( this.btnFmtTAP.isSelected() ) {
        handleBtnFmtTAP(endAddr, begAddr);
      }
      else if( this.btnFmtBIN.isSelected() ) {
        handleBtnFmtBIN(endAddr, begAddr);
      }
      else if( this.btnFmtHEX.isSelected() ) {
        handleBtnFmtHEX(endAddr, begAddr);
      }
    }
    catch( Exception ex ) {
      Main.showError( this, ex );
    }
  }


  private void handleBtnFmtJTC(int endAddr, int begAddr) throws IOException
  {
    loadIntoMem(
            begAddr,
            endAddr,
            this.fileBytes,
            128,
            getContentLen( 17 ) );
    doClose();
  }


  private void handleBtnFmtTAP(int endAddr, int begAddr) throws IOException
  {
    boolean status = true;
    int     nTotal = getContentLen( 34 );
    int     nBlk   = 0;
    int     pos    = 145;
    int     addr   = begAddr;
    while( (nTotal > 0)
           && (pos < this.fileBytes.length)
           && (addr <= endAddr) )
    {
      if( nBlk == 0 ) {
        nBlk = 128;
      } else {
        if( !this.memory.setMemByte(
                            addr++,
                            false,
                            this.fileBytes[ pos ] ) )
        {
          status = false;
        }
        --nBlk;
        --nTotal;
      }
      pos++;
    }
    Main.setLastFile( this.file );
    if( !status ) {
      fireLoadedOutOfRAM();
    }
    doClose();
  }


  private void handleBtnFmtBIN(int endAddr, int begAddr) throws IOException
  {
    loadIntoMem( begAddr, endAddr, this.fileBytes, 0, -1 );
    doClose();
  }


  private void handleBtnFmtHEX(int endAddr, int begAddr) throws IOException
  {
    String infoMsg = null;

    ByteArrayOutputStream out = new ByteArrayOutputStream( 0x4000 );
    ByteArrayInputStream  in  = new ByteArrayInputStream(
                                                    this.fileBytes,
                                                    0,
                                                    this.fileLen );
    boolean loop     = true;
    int     firstAddr = -1;
    int     curAddr  = -1;
    int     ch       = in.read();
    while( loop && (ch != -1) ) {

      // Startmarkierung suchen
      while( (ch != -1) && (ch != ':') ) {
        ch = in.read();
      }
      if( ch != -1 ) {
        // Segment verarbeiten
        int cnt  = parseHex( in, 2 );
        int addr = parseHex( in, 4 );
        int type = parseHex( in, 2 );
        switch( type ) {
          case 0:                       // Data Record
            if( cnt > 0 ) {
              if( firstAddr < 0 ) {
                firstAddr = addr;
                curAddr   = addr;
              }
              if( addr == curAddr ) {
                while( cnt > 0 ) {
                  out.write( parseHex( in, 2 ) );
                  --cnt;
                  curAddr++;
                }
              } else {
                infoMsg = loadDlgResourceBundle.getString("dialog.handleBtnFmtHEX.infoMsg.dataRecord.message");
              }
            }
            break;

          case 1:                        // End of File Record
            loop = false;
            break;

          case 2:                        // Extended Segment Address Record
            while( cnt > 0 ) {
              if( parseHex( in, 2 ) != 0 ) {
                infoMsg = loadDlgResourceBundle.getString("dialog.handleBtnFmtHEX.infoMsg.extendedSegmentAddressRecord.message");
              }
              --cnt;
            }
            break;

          case 3:                        // Start Segment Address Record
          case 5:                        // Start Linear Address Record
            // Datensatz ignorieren
            break;

          case 4:                        // Extended Linear Address Record
            while( cnt > 0 ) {
              if( parseHex( in, 2 ) != 0 ) {
                infoMsg = loadDlgResourceBundle.getString("dialog.handleBtnFmtHEX.infoMsg.extendedLinearAddressRecord.message");
              }
              --cnt;
            }
            break;

          default:
            infoMsg = String.format( loadDlgResourceBundle.getString("dialog.handleBtnFmtHEX.infoMsg.default.message"), type );
        }
        if( infoMsg != null ) {
          if( out.size() > 0 ) {
            infoMsg = infoMsg
                    + loadDlgResourceBundle.getString("dialog.handleBtnFmtHEX.infoMsg.message.hint");
          } else {
            throw new IOException( infoMsg );
          }
          loop = false;
        }
        ch = in.read();
      }
    }
    in.close();
    out.close();
    if( infoMsg != null ) {
      JOptionPane.showMessageDialog(
            this,
            infoMsg,
            loadDlgResourceBundle.getString("dialog.handleBtnFmtHEX.infoMsg.title"),
            JOptionPane.WARNING_MESSAGE );
    }
    if( firstAddr >= 0 ) {
      byte[] dataBytes = out.toByteArray();
      if( dataBytes != null ) {
        loadIntoMem( begAddr, endAddr, dataBytes, 0, -1 );
        doClose();
      }
    }
  }


  private int getContentLen( int addrPos )
  {
    int len = 0;
    if( addrPos + 1 < this.fileLen ) {
      int begAddr = ((this.fileBytes[ addrPos + 1 ] << 8) & 0xFF00)
                        | (this.fileBytes[ addrPos ] & 0x00FF);
      int endAddr = ((this.fileBytes[ addrPos + 3 ] << 8) & 0xFF00)
                        | (this.fileBytes[ addrPos + 2 ] & 0x00FF);
      len = endAddr - begAddr + 1;
    }
    return len > 0 ? len : 0;
  }


  private int getKCBasicBegPos( int blkBegPos )
  {
    int pos = -1;
    if( this.fileLen > blkBegPos + 11 ) {
      if( ((this.fileBytes[ blkBegPos ] == 0xD3)
                        && (this.fileBytes[ blkBegPos + 1 ] == 0xD3)
                        && (this.fileBytes[ blkBegPos + 2 ] == 0xD3))
          || ((this.fileBytes[ blkBegPos ] == 0xD6)
                        && (this.fileBytes[ blkBegPos + 1 ] == 0xD6)
                        && (this.fileBytes[ blkBegPos + 2 ] == 0xD6)) )
      {
        // KC-BASIC-Programm im Binaerformat
        pos = blkBegPos + 12;
      }
      else if( ((this.fileBytes[ blkBegPos ] == 0xD4)
                        && (this.fileBytes[ blkBegPos + 1 ] == 0xD4)
                        && (this.fileBytes[ blkBegPos + 2 ] == 0xD4))
               || ((this.fileBytes[ blkBegPos ] == 0xD7)
                        && (this.fileBytes[ blkBegPos + 1 ] == 0xD7)
                        && (this.fileBytes[ blkBegPos + 2 ] == 0xD7)) )
      {
        // Datenfelder eines KC-BASIC-Programms
        pos = blkBegPos + 11;
      }
      else if( ((this.fileBytes[ blkBegPos ] == 0xD5)
                        && (this.fileBytes[ blkBegPos + 1 ] == 0xD5)
                        && (this.fileBytes[ blkBegPos + 2 ] == 0xD5))
               || ((this.fileBytes[ blkBegPos ] == 0xD8)
                        && (this.fileBytes[ blkBegPos + 1 ] == 0xD8)
                        && (this.fileBytes[ blkBegPos + 2 ] == 0xD8)) )
      {
        // KC-BASIC-Programm im ASCII-Format
        pos = blkBegPos + 13;
        while( pos < this.fileBytes.length ) {
          if( this.fileBytes[ pos ] != (byte) 0 ) {
            break;
          }
          pos++;
        }
      }
    }
    return pos;
  }


  private void loadIntoMem(
                        int    addr,
                        int    endAddr,
                        byte[] dataBytes,
                        int    pos,
                        int    len ) throws IOException
  {
    boolean status = true;
    while( (len != 0) && (pos < dataBytes.length) && (addr <= endAddr) ) {
      if( !this.memory.setMemByte( addr++, false, dataBytes[ pos++ ] ) ) {
        status = false;
      }
      if( len > 0 )
        --len;
    }
    Main.setLastFile( this.file );
    if( !status )
      fireLoadedOutOfRAM();
  }


  private static int parseHex( InputStream in, int cnt ) throws IOException
  {
    int value = 0;
    while( cnt > 0 ) {
      int ch = in.read();
      if( (ch >= '0') && (ch <= '9') ) {
        value = (value << 4) | ((ch - '0') & 0x0F);
      } else if( (ch >= 'A') && (ch <= 'F') ) {
        value = (value << 4) | ((ch - 'A' + 10) & 0x0F);
      } else if( (ch >= 'a') && (ch <= 'f') ) {
        value = (value << 4) | ((ch - 'a' + 10) & 0x0F);
      } else {
        throw new IOException( loadDlgResourceBundle.getString("error.parseHex.parseError.message") );
      }
      --cnt;
    }
    return value;
  }


  private void fireLoadedOutOfRAM() throws IOException
  {
    throw new IOException( loadDlgResourceBundle.getString("error.fireLoadedOutOfRAM.message") );
  }


  private static boolean isHexChar( char ch )
  {
    return ((ch >= '0') && (ch <= '9')) || ((ch >= 'A') && (ch <= 'F'));
  }


  private void updFileMetaFields()
  {
    FileInfo.Format fmt = FileInfo.Format.BIN;
    if( this.btnFmtJTC.isSelected() ) {
      fmt = FileInfo.Format.JTC;
    }
    else if( this.btnFmtTAP.isSelected() ) {
      fmt = FileInfo.Format.TAP;
    }
    else if( this.btnFmtHEX.isSelected() ) {
      fmt = FileInfo.Format.HEX;
    }
    if( (fmt == FileInfo.Format.JTC)
        || (fmt == FileInfo.Format.TAP)
        || (fmt == FileInfo.Format.HEX) )
    {
      this.labelFileBegAddr.setEnabled( true );
      this.fldFileBegAddr.setEnabled( true );
      if( this.fileInfo != null ) {
        this.fldFileBegAddr.setText( this.fileInfo.getBegAddrText( fmt ) );
      } else {
        this.fldFileBegAddr.setText( null );
      }
    } else {
      this.labelFileBegAddr.setEnabled( false );
      this.fldFileBegAddr.setEnabled( false );
      this.fldFileBegAddr.setText( null );
    }
    if( (fmt == FileInfo.Format.JTC)
        || (fmt == FileInfo.Format.TAP) )
    {
      this.labelFileEndAddr.setEnabled( true );
      this.fldFileEndAddr.setEnabled( true );
      if( this.fileInfo != null ) {
        this.fldFileEndAddr.setText( this.fileInfo.getEndAddrText( fmt ) );
      } else {
        this.fldFileEndAddr.setText( null );
      }
      this.labelFileDesc.setEnabled( true );
      this.fldFileDesc.setEnabled( true );
      if( this.fileInfo != null ) {
        this.fldFileDesc.setText( this.fileInfo.getFileDesc( fmt ) );
      } else {
        this.fldFileDesc.setText( null );
      }
    } else {
      this.labelFileEndAddr.setEnabled( false );
      this.fldFileEndAddr.setEnabled( false );
      this.fldFileEndAddr.setText( null );
      this.labelFileDesc.setEnabled( false );
      this.fldFileDesc.setEnabled( false );
      this.fldFileDesc.setText( null );
    }
  }
}
