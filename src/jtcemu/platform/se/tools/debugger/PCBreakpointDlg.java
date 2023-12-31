/*
 * (c) 2019-2020 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Dialog zum Anlegen und Bearbeiten eines Haltepunktes
 * auf eine Programmadresse
 */

package jtcemu.platform.se.tools.debugger;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.WindowEvent;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JTextField;
import jtcemu.base.JTCUtil;
import jtcemu.base.UserInputException;


public class PCBreakpointDlg extends AbstractBreakpointDlg
{
  private static final String LABEL_TEXT = "Adresse:";

  private PCBreakpoint breakpoint;
  private JTextField   fldAddr;
  private JCheckBox    tglEnabled;


  public static AbstractBreakpoint openAdd( Window owner )
  {
    PCBreakpointDlg dlg = new PCBreakpointDlg( owner, null );
    dlg.setVisible( true );
    return dlg.getApprovedBreakpoint();
  }


  public static AbstractBreakpoint openEdit(
					Window             owner,
					AbstractBreakpoint breakpoint )
  {
    AbstractBreakpoint approvedBreakpoint = null;
    if( breakpoint != null ) {
      if( breakpoint instanceof PCBreakpoint ) {
	PCBreakpointDlg dlg = new PCBreakpointDlg(
					owner,
					(PCBreakpoint) breakpoint );
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
      int addr = JTCUtil.parseHex4( this.fldAddr.getText(), LABEL_TEXT );
      if( this.breakpoint != null ) {
	this.breakpoint.setAddr( addr );
	this.breakpoint.setEnabled( this.tglEnabled.isSelected() );
	approveBreakpoint( this.breakpoint );
      } else {
	approveBreakpoint( new PCBreakpoint( addr ) );
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
    if( rv && (this.fldAddr != null) ) {
      this.fldAddr.removeActionListener( this );
    }
    return rv;
  }


  @Override
  public void windowOpened( WindowEvent e )
  {
    if( this.fldAddr != null ) {
      this.fldAddr.requestFocus();
    }
  }


	/* --- Konstruktor --- */

  private PCBreakpointDlg( Window owner, PCBreakpoint breakpoint )
  {
    super( owner, "Programmadresse", breakpoint );
    this.breakpoint = breakpoint;


    // Fensterinhalt
    setLayout( new GridBagLayout() );

    GridBagConstraints gbc = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.WEST,
						GridBagConstraints.NONE,
						new Insets( 5, 5, 5, 5 ),
						0, 0 );

    add( new JLabel( LABEL_TEXT ), gbc );

    this.fldAddr = new JTextField();
    this.fldAddr.setActionCommand( ACTION_APPROVE );
    gbc.fill    = GridBagConstraints.HORIZONTAL;
    gbc.weightx = 1.0;
    gbc.gridx++;
    add( this.fldAddr, gbc );

    this.tglEnabled = new JCheckBox( "Haltepunkt aktiviert" );
    gbc.anchor      = GridBagConstraints.CENTER;
    gbc.fill        = GridBagConstraints.NONE;
    gbc.weightx     = 0.0;
    gbc.gridwidth   = GridBagConstraints.REMAINDER;
    gbc.gridx       = 0;
    gbc.gridy++;
    add( this.tglEnabled, gbc );

    gbc.gridy++;
    add( createGeneralButtons(), gbc );


    // Vorbelegungen
    if( breakpoint != null ) {
      this.fldAddr.setText( String.format( "%04X", breakpoint.getAddr() ) );
      this.tglEnabled.setSelected( breakpoint.isEnabled() );
    } else {
      this.tglEnabled.setSelected( true );
    }

    // Fenstergroesse und -position
    pack();
    setParentCentered();
    setResizable( true );

    // Listener
    this.fldAddr.addActionListener( this );
  }
}
