/*
 * (c) 2007-2010 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Dialog fuer Speichern einer Datei
 */

package jtcemu.base;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;
import jtcemu.Main;
import z8.Z8Memory;


public class SaveDlg extends BaseDlg implements ActionListener
{
  private static final String textBegAddr     = "Anfangsadresse";
  private static final String textEndAddr     = "Endadresse";
  private static final String textFileBegAddr = "Abweichende Anfangsadresse";

  private Z8Memory     memory;
  private JTextField   fldBegAddr;
  private JTextField   fldEndAddr;
  private JTextField   fldFileBegAddr;
  private JTextField   fldFileDesc;
  private JLabel       labelFileBegAddr;
  private JLabel       labelFileDesc;
  private JRadioButton btnFmtJTC;
  private JRadioButton btnFmtTAP;
  private JRadioButton btnFmtBIN;
  private JRadioButton btnFmtHEX;
  private JButton      btnSave;
  private JButton      btnHelp;
  private JButton      btnCancel;


  public SaveDlg( Window owner, Z8Memory memory )
  {
    super( owner );
    setTitle( "Datei speichern" );
    this.memory = memory;


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


    // Bereich: Adressbereich
    JPanel panelAddr = new JPanel( new GridBagLayout() );
    panelAddr.setBorder( BorderFactory.createTitledBorder(
                                                "Zu speichernder Bereich" ) );
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

    this.fldBegAddr = new JTextField();
    this.fldBegAddr.addActionListener( this );
    gbcAddr.fill    = GridBagConstraints.HORIZONTAL;
    gbcAddr.weightx = 0.5;
    gbcAddr.gridx++;
    panelAddr.add( this.fldBegAddr, gbcAddr );

    gbcAddr.fill    = GridBagConstraints.NONE;
    gbcAddr.weightx = 0.0;
    gbcAddr.gridx++;
    panelAddr.add( new JLabel( textEndAddr + ":" ), gbcAddr );

    this.fldEndAddr = new JTextField();
    this.fldEndAddr.addActionListener( this );
    gbcAddr.fill    = GridBagConstraints.HORIZONTAL;
    gbcAddr.weightx = 0.5;
    gbcAddr.gridx++;
    panelAddr.add( this.fldEndAddr, gbcAddr );


    // Bereich Dateiformat
    JPanel panelFmt = new JPanel( new GridBagLayout() );
    panelFmt.setBorder( BorderFactory.createTitledBorder( "Dateiformat" ) );
    gbc.gridy++;
    add( panelFmt, gbc );

    GridBagConstraints gbcFmt = new GridBagConstraints(
                                        0, 0,
                                        1, 1,
                                        0.0, 0.0,
                                        GridBagConstraints.WEST,
                                        GridBagConstraints.NONE,
                                        new Insets( 5, 5, 5, 5 ),
                                        0, 0 );

    ButtonGroup grpFmt = new ButtonGroup();

    this.btnFmtJTC = new JRadioButton( "JTC", true );
    grpFmt.add( this.btnFmtJTC );
    panelFmt.add( this.btnFmtJTC, gbcFmt );

    this.btnFmtTAP = new JRadioButton( "KC-TAP", false );
    grpFmt.add( this.btnFmtTAP );
    gbcFmt.gridx++;
    panelFmt.add( this.btnFmtTAP, gbcFmt );

    this.btnFmtBIN = new JRadioButton( "BIN", false );
    grpFmt.add( this.btnFmtBIN );
    gbcFmt.gridx++;
    panelFmt.add( this.btnFmtBIN, gbcFmt );

    this.btnFmtHEX = new JRadioButton( "Intel-HEX", false );
    grpFmt.add( this.btnFmtHEX );
    gbcFmt.gridx++;
    panelFmt.add( this.btnFmtHEX, gbcFmt );

    this.labelFileBegAddr = new JLabel( textFileBegAddr + ":" );
    gbcFmt.anchor     = GridBagConstraints.EAST;
    gbcFmt.insets.top = 10;
    gbcFmt.gridwidth  = 3;
    gbcFmt.gridx      = 0;
    gbcFmt.gridy++;
    panelFmt.add( this.labelFileBegAddr, gbcFmt );

    this.fldFileBegAddr = new JTextField();
    this.fldFileBegAddr.addActionListener( this );
    gbcFmt.fill      = GridBagConstraints.HORIZONTAL;
    gbcFmt.weightx   = 1.0;
    gbcFmt.gridwidth = 1;
    gbcFmt.gridx += 3;
    panelFmt.add( this.fldFileBegAddr, gbcFmt );

    this.labelFileDesc = new JLabel( "Name in der Datei:" );
    gbcFmt.fill       = GridBagConstraints.NONE;
    gbcFmt.weightx    = 0.0;
    gbcFmt.insets.top = 0;
    gbcFmt.gridwidth  = 3;
    gbcFmt.gridx      = 0;
    gbcFmt.gridy++;
    panelFmt.add( this.labelFileDesc, gbcFmt );

    this.fldFileDesc = new JTextField( new LimitedLengthDoc( 11 ), "", 0 );
    this.fldFileDesc.addActionListener( this );
    gbcFmt.fill    = GridBagConstraints.HORIZONTAL;
    gbcFmt.weightx = 1.0;
    gbcFmt.gridx += 3;
    panelFmt.add( this.fldFileDesc, gbcFmt );

    this.btnFmtJTC.addActionListener( this );
    this.btnFmtTAP.addActionListener( this );
    this.btnFmtBIN.addActionListener( this );
    this.btnFmtHEX.addActionListener( this );
    updFmtFields();


    // Knoepfe
    JPanel panelBtn = new JPanel( new GridLayout( 1, 3, 5, 5 ) );
    gbc.fill       = GridBagConstraints.NONE;
    gbc.weightx    = 0.0;
    gbc.insets.top = 10;
    gbc.gridy++;
    add( panelBtn,gbc );

    this.btnSave = new JButton( "Speichern..." );
    this.btnSave.addActionListener( this );
    panelBtn.add( this.btnSave );

    this.btnHelp = new JButton( "Hilfe..." );
    this.btnHelp.addActionListener( this );
    panelBtn.add( this.btnHelp );

    this.btnCancel = new JButton( "Abbrechen" );
    this.btnCancel.addActionListener( this );
    panelBtn.add( this.btnCancel );


    // Fenstergroesse
    pack();
    setParentCentered();
    setResizable( true );


    // ggf. Adressen eines BASIC-Programms eintragen
    boolean has0D   = false;
    int     addr    = 0xE000;
    int     endAddr = -1;
    while( addr < 0xFC00 ) {
      int b = this.memory.getMemByte( addr, false );
      if( b == 0x0D ) {
        has0D = true;
      }
      else if( b == 0 ) {
        endAddr = addr;
        break;
      }
      addr++;
    }
    if( has0D && (endAddr > 0xE002) ) {
      this.fldBegAddr.setText( "E000" );
      this.fldEndAddr.setText( String.format( "%04X", endAddr ) );
    }
  }


        /* --- ActionListener --- */

  @Override
  public void actionPerformed( ActionEvent e )
  {
    Object src = e.getSource();
    if( (src == this.fldEndAddr)
        || (src == this.fldFileBegAddr)
        || (src == this.btnSave) )
    {
      doSave();
    }
    else if( src == this.btnHelp ) {
      HelpFrm.open( "/help/loadsave.htm" );
    }
    else if( src == this.btnCancel ) {
      doClose();
    }
    else if( src == this.fldBegAddr ) {
      this.fldBegAddr.transferFocus();
    }
    else if( (src == this.btnFmtJTC)
             || (src == this.btnFmtTAP)
             || (src == this.btnFmtBIN)
             || (src == this.btnFmtHEX) )
    {
      updFmtFields();
    }
  }


        /* --- private Methoden --- */

  private void doSave()
  {
    try {
      int addr = GUIUtil.parseHex4(
                                this.fldBegAddr.getText(),
                                textBegAddr );

      int endAddr = GUIUtil.parseHex4(
                                this.fldEndAddr.getText(),
                                textEndAddr );
      if( endAddr < addr ) {
        throw new IOException( "Die Endadresse kann nicht vor der"
                                        + " Anfangsadresse liegen!" );
      }

      int    fileBegAddr = addr;
      String text        = this.fldFileBegAddr.getText();
      if( text != null ) {
        text = text.trim();
        if( text.length() > 0 )
          fileBegAddr = GUIUtil.parseHex4( text, textFileBegAddr );
      }

      boolean jtcSelected = this.btnFmtJTC.isSelected();
      boolean tapSelected = this.btnFmtTAP.isSelected();
      boolean hexSelected = this.btnFmtHEX.isSelected();
      File    file        = null;
      if( jtcSelected ) {
        file = FileDlg.showFileSaveDlg(
                                this,
                                "JTC-Datei speichern",
                                Main.getLastPathFile(),
                                GUIUtil.jtcFileFilter );
      } else if( tapSelected ) {
        file = FileDlg.showFileSaveDlg(
                                this,
                                "KC-TAP-Datei speichern",
                                Main.getLastPathFile(),
                                GUIUtil.tapFileFilter );
      } else if( hexSelected ) {
        file = FileDlg.showFileSaveDlg(
                                this,
                                "HEX-Datei speichern",
                                Main.getLastPathFile(),
                                GUIUtil.hexFileFilter );
      } else {
        file = FileDlg.showFileSaveDlg(
                                this,
                                "BIN-Datei speichern",
                                Main.getLastPathFile(),
                                GUIUtil.binaryFileFilter );
      }
      if( file != null ) {
        if( hexSelected ) {
          Writer out = null;
          try {
            out = new BufferedWriter( new FileWriter( file ) );

            int cnt = 1;
            while( (addr <= endAddr) && (cnt > 0) ) {
              cnt = writeHexSegment( out, addr, endAddr, fileBegAddr );
              addr += cnt;
              fileBegAddr += cnt;
            }
            out.write( ':' );
            writeHexByte( out, 0 );
            writeHexByte( out, 0 );
            writeHexByte( out, 0 );
            writeHexByte( out, 1 );
            writeHexByte( out, 0xFF );
            out.write( 0x0D );
            out.write( 0x0A );
            out.close();
            out = null;
            Main.setLastFile( file );
            doClose();
          }
          finally {
            if( out != null ) {
              try {
                out.close();
              }
              catch( IOException ex ) {}
            }
          }

        } else {

          // alle anderen Formate sind binear -> OutputStream oeffnen
          OutputStream out = null;
          try {
            out = new BufferedOutputStream( new FileOutputStream( file ) );
            if( jtcSelected ) {
              writeJTCHeader( out, addr, endAddr, fileBegAddr );
              while( addr <= endAddr ) {
                out.write( this.memory.getMemByte( addr++, false ) );
              }
            } else if( tapSelected ) {
              String s = "\u00C3KC-TAPE by AF.\u0020";
              int    n = s.length();
              for( int i = 0; i < n; i++ ) {
                out.write( s.charAt( i ) );
              }

              int blkNum = 1;
              out.write( blkNum++ );
              writeJTCHeader( out, addr, endAddr, fileBegAddr );

              n = 0;
              while( addr <= endAddr ) {
                if( n == 0 ) {
                  out.write( (addr + 128) > endAddr ? 0xFF : blkNum++ );
                  n = 128;
                }
                out.write( this.memory.getMemByte( addr++, false ) );
                --n;
              }
              while( n > 0 ) {
                out.write( 0 );
                --n;
              }
            } else {
              while( addr <= endAddr ) {
                out.write( this.memory.getMemByte( addr++, false ) );
              }
            }
            out.close();
            out = null;
            Main.setLastFile( file );
            doClose();
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
      }
    }
    catch( Exception ex ) {
      Main.showError( this, ex );
    }
  }


  private void writeHexChar( Writer out, int value ) throws IOException
  {
    value &= 0x0F;
    out.write( value < 10 ? (value + '0') : (value - 10 + 'A') );
  }


  private void writeHexByte( Writer out, int value ) throws IOException
  {
    writeHexChar( out, value >> 4 );
    writeHexChar( out, value );
  }


  /*
   * Die Methode schreibt ein Datensegment im Intel-Hex-Format.
   *
   * Rueckabewert: Anzahl der geschriebenen Bytes
   */
  private int writeHexSegment(
                        Writer out,
                        int    addr,
                        int    endAddr,
                        int    fileBegAddr ) throws IOException
  {
    int cnt = 0;
    if( (addr >= 0) && (addr <= endAddr) ) {
      cnt = endAddr - addr + 1;
      if( cnt > 32 ) {
        cnt = 32;
      }
      out.write( ':' );
      writeHexByte( out, cnt );

      int hFileBegAddr = fileBegAddr >> 8;
      writeHexByte( out, hFileBegAddr );
      writeHexByte( out, fileBegAddr );
      writeHexByte( out, 0 );

      int cks = (cnt & 0xFF) + (hFileBegAddr & 0xFF) + (fileBegAddr & 0xFF);
      for( int i = 0; i < cnt; i++ ) {
        int b = this.memory.getMemByte( addr++, false );
        writeHexByte( out, b );
        cks += b;
      }
      writeHexByte( out, 0 - cks );
      out.write( 0x0D );
      out.write( 0x0A );
    }
    return cnt;
  }


  private void updFmtFields()
  {
    boolean reqHeader  = (this.btnFmtJTC.isSelected()
                                        || this.btnFmtTAP.isSelected());
    boolean reqBegAddr = this.btnFmtHEX.isSelected();
    this.labelFileBegAddr.setEnabled( reqHeader || reqBegAddr );
    this.fldFileBegAddr.setEditable( reqHeader || reqBegAddr );
    this.labelFileDesc.setEnabled( reqHeader );
    this.fldFileDesc.setEditable( reqHeader );
  }


  private void writeJTCHeader(
                        OutputStream out,
                        int          begAddr,
                        int          endAddr,
                        int          fileBegAddr ) throws IOException
  {
    int    n    = 11;
    int    src  = 0;
    String text = this.fldFileDesc.getText();
    if( text != null ) {
      int len = text.length();
      while( (src < len) && (n > 0) ) {
        char ch = text.charAt( src++ );
        if( (ch >= '\u0020') && (ch <= 0xFF) ) {
          out.write( ch );
          --n;
        }
      }
    }
    while( n > 0 ) {
      out.write( '\u0020' );
      --n;
    }
    for( int i = 0; i < 5; i++ ) {
      out.write( 0 );
    }
    out.write( 2 );
    out.write( fileBegAddr & 0xFF );
    out.write( fileBegAddr >> 8 );
    int fileEndAddr = fileBegAddr + endAddr - begAddr;
    out.write( fileEndAddr & 0xFF );
    out.write( fileEndAddr >> 8 );
    for( int i = 0; i < 107; i++ ) {
      out.write( 0 );
    }
  }
}
