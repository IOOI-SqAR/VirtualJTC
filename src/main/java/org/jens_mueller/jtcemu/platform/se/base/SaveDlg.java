/*
 * (c) 2007-2021 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Dialog fuer Speichern einer Datei
 */

package org.jens_mueller.jtcemu.platform.se.base;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import javax.swing.ButtonGroup;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JRadioButton;
import org.jens_mueller.jtcemu.base.AppContext;
import org.jens_mueller.jtcemu.base.FileInfo;
import org.jens_mueller.jtcemu.base.FileSaver;
import org.jens_mueller.jtcemu.base.JTCSys;
import org.jens_mueller.jtcemu.base.JTCUtil;
import org.jens_mueller.jtcemu.platform.se.Main;
import org.jens_mueller.jtcemu.tools.ToolUtil;


public class SaveDlg extends BaseDlg implements ActionListener
{
  private static final String LABEL_BEG_ADDR   = "Anfangsadresse:";
  private static final String LABEL_END_ADDR   = "Endadresse:";
  private static final String LABEL_START_ADDR = "Startadresse:";

  private TopFrm       topFrm;
  private JTCSys       jtcSys;
  private JTextField   fldBegAddr;
  private JTextField   fldEndAddr;
  private JTextField   fldFileBegAddr;
  private JTextField   fldFileStartAddr;
  private JTextField   fldFileDesc;
  private JLabel       labelFileBegAddr;
  private JLabel       labelFileBegAddrOpt;
  private JLabel       labelFileStartAddr;
  private JLabel       labelFileStartAddrOpt;
  private JLabel       labelFileDesc;
  private JRadioButton btnFmtJTC;
  private JRadioButton btnFmtTAP;
  private JRadioButton btnFmtBIN;
  private JRadioButton btnFmtHEX;
  private JButton      btnSave;
  private JButton      btnHelp;
  private JButton      btnCancel;


  public SaveDlg( TopFrm topFrm, JTCSys jtcSys )
  {
    super( topFrm );
    setTitle( "Datei speichern" );
    this.topFrm = topFrm;
    this.jtcSys = jtcSys;


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

    panelAddr.add( new JLabel( LABEL_BEG_ADDR ), gbcAddr );

    this.fldBegAddr = new JTextField( 4 );
    gbcAddr.fill    = GridBagConstraints.HORIZONTAL;
    gbcAddr.weightx = 0.5;
    gbcAddr.gridx++;
    panelAddr.add( this.fldBegAddr, gbcAddr );

    gbcAddr.fill    = GridBagConstraints.NONE;
    gbcAddr.weightx = 0.0;
    gbcAddr.gridx++;
    panelAddr.add( new JLabel( LABEL_END_ADDR ), gbcAddr );

    this.fldEndAddr = new JTextField( 4 );
    gbcAddr.fill    = GridBagConstraints.HORIZONTAL;
    gbcAddr.weightx = 0.5;
    gbcAddr.gridx++;
    panelAddr.add( this.fldEndAddr, gbcAddr );


    // Bereich Dateiformat
    JPanel panelFmt = new JPanel( new FlowLayout( FlowLayout.LEFT, 5, 5 ) );
    panelFmt.setBorder( BorderFactory.createTitledBorder( "Dateiformat" ) );
    gbc.gridy++;
    add( panelFmt, gbc );

    ButtonGroup grpFmt = new ButtonGroup();

    this.btnFmtJTC = new JRadioButton( "JTC", true );
    grpFmt.add( this.btnFmtJTC );
    panelFmt.add( this.btnFmtJTC );

    this.btnFmtTAP = new JRadioButton( "KC-TAP", false );
    grpFmt.add( this.btnFmtTAP );
    panelFmt.add( this.btnFmtTAP );

    this.btnFmtBIN = new JRadioButton( "BIN", false );
    grpFmt.add( this.btnFmtBIN );
    panelFmt.add( this.btnFmtBIN );

    this.btnFmtHEX = new JRadioButton( "Intel-HEX", false );
    grpFmt.add( this.btnFmtHEX );
    panelFmt.add( this.btnFmtHEX );


    // Dateikopfdaten
    JPanel panelHead = new JPanel( new GridBagLayout() );
    panelHead.setBorder( BorderFactory.createTitledBorder(
					"Angaben in der Datei" ) );
    gbc.gridy++;
    add( panelHead, gbc );

    GridBagConstraints gbcHead = new GridBagConstraints(
					0, 0,
					1, 1,
					0.0, 0.0,
					GridBagConstraints.EAST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 0, 5 ),
					0, 0 );

    this.labelFileBegAddr = new JLabel( LABEL_BEG_ADDR );
    panelHead.add( this.labelFileBegAddr, gbcHead );

    this.labelFileStartAddr = new JLabel( LABEL_START_ADDR );
    gbcHead.gridy++;
    panelHead.add( this.labelFileStartAddr, gbcHead );

    this.labelFileDesc    = new JLabel( "Bezeichnung:" );
    gbcHead.insets.bottom = 5;
    gbcHead.gridy++;
    panelHead.add( this.labelFileDesc, gbcHead );

    this.fldFileBegAddr   = new JTextField( 4 );
    gbcHead.anchor        = GridBagConstraints.WEST;
    gbcHead.fill          = GridBagConstraints.HORIZONTAL;
    gbcHead.weightx       = 1.0;
    gbcHead.insets.bottom = 0;
    gbcHead.gridy         = 0;
    gbcHead.gridx++;
    panelHead.add( this.fldFileBegAddr, gbcHead );

    this.fldFileStartAddr = new JTextField( 4 );
    gbcHead.gridy++;
    panelHead.add( this.fldFileStartAddr, gbcHead );

    this.fldFileDesc = new JTextField( new LimitedLengthDoc( 11 ), "", 0 );
    gbcHead.insets.bottom = 5;
    gbcHead.gridwidth     = GridBagConstraints.REMAINDER;
    gbcHead.gridy++;
    panelHead.add( this.fldFileDesc, gbcHead );

    this.labelFileBegAddrOpt = new JLabel( "(nur wenn abweichend)" );
    gbcHead.insets.bottom    = 0;
    gbcHead.gridy            = 0;
    gbcHead.gridx++;
    panelHead.add( this.labelFileBegAddrOpt, gbcHead );

    this.labelFileStartAddrOpt = new JLabel( "(optional)" );
    gbcHead.gridy++;
    panelHead.add( this.labelFileStartAddrOpt, gbcHead );

    updHeadFields();


    // Knoepfe
    JPanel panelBtn   = new JPanel( new GridLayout( 1, 3, 5, 5 ) );
    gbc.fill          = GridBagConstraints.NONE;
    gbc.weightx       = 0.0;
    gbc.insets.top    = 10;
    gbc.insets.left   = 10;
    gbc.insets.right  = 10;
    gbc.insets.bottom = 10;
    gbc.gridy++;
    add( panelBtn,gbc );

    this.btnSave = new JButton( "Speichern..." );
    panelBtn.add( this.btnSave );

    this.btnHelp = new JButton( "Hilfe..." );
    panelBtn.add( this.btnHelp );

    this.btnCancel = new JButton( "Abbrechen" );
    panelBtn.add( this.btnCancel );


    // Fenstergroesse
    pack();
    setParentCentered();
    setResizable( true );


    // ggf. Adressen eines BASIC-Programms eintragen
    int endAddr = ToolUtil.getBasicEndAddress( jtcSys );
    if( endAddr > 0xE002 ) {
      this.fldBegAddr.setText( "E000" );
      this.fldEndAddr.setText( String.format( "%04X", endAddr ) );
    }


    // Listener
    this.fldBegAddr.addActionListener( this );
    this.fldEndAddr.addActionListener( this );
    this.fldFileBegAddr.addActionListener( this );
    this.fldFileStartAddr.addActionListener( this );
    this.fldFileDesc.addActionListener( this );
    this.btnFmtJTC.addActionListener( this );
    this.btnFmtTAP.addActionListener( this );
    this.btnFmtBIN.addActionListener( this );
    this.btnFmtHEX.addActionListener( this );
    this.btnSave.addActionListener( this );
    this.btnHelp.addActionListener( this );
    this.btnCancel.addActionListener( this );
  }


	/* --- ActionListener --- */

  @Override
  public void actionPerformed( ActionEvent e )
  {
    Object src = e.getSource();
    if( (src == this.fldFileDesc) || (src == this.btnSave) ) {
      doSave();
    }
    else if( src == this.btnHelp ) {
      HelpFrm.open( "/help/loadsave.htm" );
    }
    else if( src == this.btnCancel ) {
      doClose();
    }
    else if( (src == this.btnFmtJTC)
	     || (src == this.btnFmtTAP)
	     || (src == this.btnFmtBIN)
	     || (src == this.btnFmtHEX) )
    {
      updHeadFields();
    }
    else if( src instanceof JTextField ) {
      ((JTextField) src).transferFocus();
    }
  }


	/* --- private Methoden --- */

  private void doSave()
  {
    try {
      int begAddr = JTCUtil.parseHex4(
			this.fldBegAddr.getText(),
			"Anfangsadresse in zu speichernder Bereich:" );
      int endAddr = JTCUtil.parseHex4(
			this.fldEndAddr.getText(),
			LABEL_END_ADDR );
      if( endAddr < begAddr ) {
	throw new IOException( "Die Endadresse kann nicht vor der"
					+ " Anfangsadresse liegen!" );
      }

      int fileBegAddr = begAddr;
      if( this.fldFileBegAddr.isEnabled() ) {
	String text = this.fldFileBegAddr.getText();
	if( text != null ) {
	  text = text.trim();
	  if( !text.isEmpty() ) {
	    fileBegAddr = JTCUtil.parseHex4(
				text,
				"Anfangsadresse in Angaben in der Datei:" );
	  }
	}
      }

      int fileStartAddr = -1;
      if( this.fldFileStartAddr.isEnabled() ) {
	String text = this.fldFileStartAddr.getText();
	if( text != null ) {
	  text = text.trim();
	  if( !text.isEmpty() ) {
	    fileStartAddr = JTCUtil.parseHex4( text, LABEL_START_ADDR );
	  }
	}
      }

      boolean jtcSelected = this.btnFmtJTC.isSelected();
      boolean tapSelected = this.btnFmtTAP.isSelected();
      boolean hexSelected = this.btnFmtHEX.isSelected();

      File             file   = null;
      FileSaver.Format format = FileSaver.Format.BIN;
      if( jtcSelected ) {
	format = FileSaver.Format.JTC;
        file   = FileDlg.showFileSaveDlg(
			this,
			"JTC-Datei speichern",
			AppContext.getLastDirFile(
					FileInfo.FILE_GROUP_SOFTWARE ),
			GUIUtil.jtcFileFilter );
      } else if( tapSelected ) {
	format = FileSaver.Format.TAP;
        file   = FileDlg.showFileSaveDlg(
			this,
			"KC-TAP-Datei speichern",
			AppContext.getLastDirFile(
					FileInfo.FILE_GROUP_SOFTWARE ),
			GUIUtil.tapFileFilter );
      } else if( hexSelected ) {
	format = FileSaver.Format.HEX;
        file   = FileDlg.showFileSaveDlg(
			this,
			"HEX-Datei speichern",
			AppContext.getLastDirFile(
					FileInfo.FILE_GROUP_SOFTWARE ),
			GUIUtil.hexFileFilter );
      } else {
        file = FileDlg.showFileSaveDlg(
			this,
			"BIN-Datei speichern",
			AppContext.getLastDirFile(
					FileInfo.FILE_GROUP_SOFTWARE ),
			GUIUtil.binaryFileFilter );
      }
      if( file != null ) {
	String statusText = FileSaver.save(
					this.jtcSys,
					begAddr,
					endAddr,
					fileStartAddr,
					file,
					format,
					fileBegAddr,
					this.fldFileDesc.getText() );
	if( statusText != null ) {
	  doClose();
	  this.topFrm.showStatusText( statusText );
	}
      }
    }
    catch( Exception ex ) {
      Main.showError( this, ex );
    }
  }


  private void updHeadFields()
  {
    boolean reqHeader  = (this.btnFmtJTC.isSelected()
					|| this.btnFmtTAP.isSelected());
    boolean reqBegAddr = this.btnFmtHEX.isSelected();

    this.labelFileBegAddr.setEnabled( reqHeader || reqBegAddr );
    this.fldFileBegAddr.setEditable( reqHeader || reqBegAddr );
    this.labelFileBegAddrOpt.setEnabled( reqHeader || reqBegAddr );

    this.labelFileStartAddr.setEnabled( reqHeader );
    this.fldFileStartAddr.setEditable( reqHeader );
    this.labelFileStartAddrOpt.setEnabled( reqHeader );

    this.labelFileDesc.setEnabled( reqHeader );
    this.fldFileDesc.setEditable( reqHeader );
  }
}
