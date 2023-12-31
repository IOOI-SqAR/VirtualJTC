/*
 * (c) 2019-2021 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Dialog zum Anlegen und Aendern eines Haltepunktes
 * auf eine Speicherzelle bzw. einen Speicherbereich
 */

package jtcemu.platform.se.tools.debugger;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.WindowEvent;
import java.awt.event.ActionListener;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JTextField;
import jtcemu.base.JTCUtil;
import jtcemu.base.UserInputException;


public class MemBreakpointDlg
			extends AbstractBreakpointDlg
			implements ActionListener
{
  private static final String LABEL_TEXT_BEG = "Adresse / Anfangsadresse:";
  private static final String LABEL_TEXT_END = "Endadresse (optional):";

  private MemBreakpoint breakpoint;
  private JCheckBox     tglRead;
  private JCheckBox     tglWrite;
  private JTextField    fldBegAddr;
  private JTextField    fldEndAddr;


  public static AbstractBreakpoint openAdd( Window owner )
  {
    MemBreakpointDlg dlg = new MemBreakpointDlg( owner, null );
    dlg.setVisible( true );
    return dlg.getApprovedBreakpoint();
  }


  public static AbstractBreakpoint openEdit(
					Window             owner,
					AbstractBreakpoint breakpoint )
  {
    AbstractBreakpoint approvedBreakpoint = null;
    if( breakpoint != null ) {
      if( breakpoint instanceof MemBreakpoint ) {
	MemBreakpointDlg dlg = new MemBreakpointDlg(
					owner,
					(MemBreakpoint) breakpoint );
	dlg.setVisible( true );
	approvedBreakpoint = dlg.getApprovedBreakpoint();
      }
    }
    return approvedBreakpoint;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  protected void doApprove()
  {
    String curLabel = LABEL_TEXT_BEG;
    try {
      int endAddr = -1;
      int begAddr = JTCUtil.parseHex4(
				this.fldBegAddr.getText(),
				LABEL_TEXT_BEG );
      String endAddrText = this.fldEndAddr.getText();
      if( endAddrText != null ) {
	endAddrText = endAddrText.trim();
	if( !endAddrText.isEmpty() ) {
	  curLabel = LABEL_TEXT_END;
	  endAddr  = JTCUtil.parseHex4( endAddrText, LABEL_TEXT_END );
	  if( endAddr < begAddr ) {
	    throw new UserInputException(
			"Endadresse liegt vor der Anfangsadresse." );
	  }
	}
      }
      if( endAddr < 0 ) {
	endAddr = begAddr;
      }
      if( this.breakpoint != null ) {
	this.breakpoint.setValues(
				begAddr,
				endAddr,
				this.tglRead.isSelected(),
				this.tglWrite.isSelected() );
	approveBreakpoint( this.breakpoint );
      } else {
	approveBreakpoint(
		new MemBreakpoint(
				begAddr,
				endAddr,
				this.tglRead.isSelected(),
				this.tglWrite.isSelected() ) );
      }
    }
    catch( UserInputException ex ) {
      showError( this, ex.getMessage() );
    }
  }


  @Override
  public boolean doClose()
  {
    boolean rv = super.doClose();
    if( rv && (this.fldBegAddr != null) ) {
      this.fldBegAddr.removeActionListener( this );
    }
    if( rv && (this.fldEndAddr != null) ) {
      this.fldEndAddr.removeActionListener( this );
    }
    return rv;
  }


  @Override
  public void windowOpened( WindowEvent e )
  {
    if( this.fldBegAddr != null ) {
      this.fldBegAddr.requestFocus();
    }
  }


	/* --- Konstruktor --- */

  private MemBreakpointDlg(
			Window        owner,
			MemBreakpoint breakpoint )
  {
    super( owner, "Speicherbereich", breakpoint );
    this.breakpoint = breakpoint;


    // Fensterinhalt
    setLayout( new GridBagLayout() );

    GridBagConstraints gbc = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.EAST,
						GridBagConstraints.NONE,
						new Insets( 5, 5, 0, 5 ),
						0, 0 );

    add( new JLabel( LABEL_TEXT_BEG ), gbc );
    gbc.gridy++;
    add( new JLabel( LABEL_TEXT_END ), gbc );
    gbc.gridy++;
    add( new JLabel( "Anhalten beim:" ), gbc );

    this.fldBegAddr = new JTextField();
    this.fldBegAddr.setActionCommand( ACTION_TRANSFER_FOCUS );
    gbc.anchor    = GridBagConstraints.WEST;
    gbc.fill      = GridBagConstraints.HORIZONTAL;
    gbc.weightx   = 1.0;
    gbc.gridwidth = 2;
    gbc.gridy     = 0;
    gbc.gridx++;
    add( this.fldBegAddr, gbc );

    this.fldEndAddr = new JTextField();
    this.fldEndAddr.setActionCommand( ACTION_APPROVE );
    gbc.gridy++;
    add( this.fldEndAddr, gbc );

    this.tglRead      = new JCheckBox( "Lesen", true );
    gbc.insets.bottom = 5;
    gbc.fill          = GridBagConstraints.NONE;
    gbc.weightx       = 0.0;
    gbc.gridwidth     = 1;
    gbc.gridy++;
    add( this.tglRead, gbc );

    this.tglWrite = new JCheckBox( "Schreiben", true );
    gbc.gridx++;
    add( this.tglWrite, gbc );

    gbc.anchor    = GridBagConstraints.CENTER;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.gridx     = 0;
    gbc.gridy++;
    add( createGeneralButtons(), gbc );


    // Vorbelegungen
    if( breakpoint != null ) {
      int begAddr = breakpoint.getBegAddr();
      int endAddr = breakpoint.getEndAddr();
      this.fldBegAddr.setText( String.format( "%04X", begAddr ) );
      if( endAddr == begAddr ) {
	this.fldEndAddr.setText( "" );
      } else {
	this.fldEndAddr.setText( String.format( "%04X", endAddr ) );
      }
      this.tglRead.setSelected( breakpoint.isRead() );
      this.tglWrite.setSelected( breakpoint.isWrite() );
    }

    // Fenstergroesse und -position
    pack();
    setParentCentered();
    setResizable( true );

    // Listener
    this.fldBegAddr.addActionListener( this );
    this.fldEndAddr.addActionListener( this );
  }
}
