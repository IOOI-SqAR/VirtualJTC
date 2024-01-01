/*
 * (c) 2019-2021 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Dialog zum Anlegen und Aendern eines Haltepunktes
 * auf ein Register
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
import z8.Z8Reassembler;


public class RegBreakpointDlg
			extends AbstractBreakpointDlg
			implements ActionListener
{
  private static final String LABEL_TEXT = "Register:";

  private RegBreakpoint breakpoint;
  private JCheckBox     tglRead;
  private JCheckBox     tglWrite;
  private JTextField    fldReg;


  public static AbstractBreakpoint openAdd( Window owner )
  {
    RegBreakpointDlg dlg = new RegBreakpointDlg( owner, null );
    dlg.setVisible( true );
    return dlg.getApprovedBreakpoint();
  }


  public static AbstractBreakpoint openEdit(
					Window             owner,
					AbstractBreakpoint breakpoint )
  {
    AbstractBreakpoint approvedBreakpoint = null;
    if( breakpoint != null ) {
      if( breakpoint instanceof RegBreakpoint ) {
	RegBreakpointDlg dlg = new RegBreakpointDlg(
					owner,
					(RegBreakpoint) breakpoint );
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
    try {
      String text   = this.fldReg.getText();
      int    regNum = Z8Reassembler.getRegNum( text );
      if( regNum < 0 ) {
	regNum = JTCUtil.parseHex2( text, LABEL_TEXT );
      }
      if( this.breakpoint != null ) {
	this.breakpoint.setValues(
				regNum,
				this.tglRead.isSelected(),
				this.tglWrite.isSelected() );
	approveBreakpoint( this.breakpoint );
      } else {
	approveBreakpoint(
		new RegBreakpoint(
				regNum,
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
    if( rv && (this.fldReg != null) ) {
      this.fldReg.removeActionListener( this );
    }
    return rv;
  }


  @Override
  public void windowOpened( WindowEvent e )
  {
    if( this.fldReg != null ) {
      this.fldReg.requestFocus();
    }
  }


	/* --- Konstruktor --- */

  private RegBreakpointDlg(
			Window        owner,
			RegBreakpoint breakpoint )
  {
    super( owner, "Register", breakpoint );
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

    add( new JLabel( LABEL_TEXT ), gbc );
    gbc.gridy++;
    add( new JLabel( "Anhalten beim:" ), gbc );

    this.fldReg = new JTextField();
    this.fldReg.setActionCommand( ACTION_APPROVE );
    gbc.anchor    = GridBagConstraints.WEST;
    gbc.fill      = GridBagConstraints.HORIZONTAL;
    gbc.weightx   = 1.0;
    gbc.gridwidth = 2;
    gbc.gridy     = 0;
    gbc.gridx++;
    add( this.fldReg, gbc );

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
      this.fldReg.setText(
		Z8Reassembler.getRegName( breakpoint.getRegNum() ) );
      this.tglRead.setSelected( breakpoint.isRead() );
      this.tglWrite.setSelected( breakpoint.isWrite() );
    }

    // Fenstergroesse und -position
    pack();
    setParentCentered();
    setResizable( true );

    // Listener
    this.fldReg.addActionListener( this );
  }
}
