/*
 * (c) 2019-2020 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Basisklasse fuer einen Dialog zum Anlegen und Bearbeiten
 * eines Haltepunktes
 */

package org.jens_mueller.jtcemu.platform.se.tools.debugger;

import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JPanel;
import org.jens_mueller.jtcemu.platform.se.base.BaseDlg;


public abstract class AbstractBreakpointDlg
				extends BaseDlg
				implements ActionListener
{
  public static final String ACTION_APPROVE        = "approve";
  public static final String ACTION_CANCEL         = "cancel";
  public static final String ACTION_TRANSFER_FOCUS = "transfer";

  private AbstractBreakpoint approvedBreakpoint;
  private JButton            btnOK;
  private JButton            btnCancel;


  protected AbstractBreakpointDlg(
			Window             owner,
			String             watchedObj,
			AbstractBreakpoint breakpoint )
  {
    super( owner );
    if( breakpoint != null ) {
      setTitle( String.format( "Haltepunkt auf %s bearbeiten", watchedObj ) );
    } else {
      setTitle( "Neuer Haltepunkt auf " + watchedObj );
    }
    this.approvedBreakpoint = null;
    this.btnOK              = null;
    this.btnCancel          = null;
  }


  protected void approveBreakpoint( AbstractBreakpoint breakpoint )
  {
    this.approvedBreakpoint = breakpoint;
    doClose();
  }


  protected JPanel createGeneralButtons()
  {
    JPanel panel = new JPanel( new GridBagLayout() );

    GridBagConstraints gbc = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.CENTER,
						GridBagConstraints.NONE,
						new Insets( 5, 5, 5, 5 ),
						0, 0 );

    JPanel panelBtn = new JPanel( new GridLayout( 1, 2, 5, 5 ) );
    panel.add( panelBtn, gbc );

    this.btnOK = new JButton( "OK" );
    this.btnOK.setActionCommand( ACTION_APPROVE );
    this.btnOK.addActionListener( this );
    panelBtn.add( this.btnOK );

    this.btnCancel = new JButton( "Abbrechen" );
    this.btnCancel.setActionCommand( ACTION_CANCEL );
    this.btnCancel.addActionListener( this );
    panelBtn.add( this.btnCancel );

    return panel;
  }


  protected abstract void doApprove();


  public AbstractBreakpoint getApprovedBreakpoint()
  {
    return this.approvedBreakpoint;
  }


	/* --- ActionListener --- */

  @Override
  public void actionPerformed( ActionEvent e )
  {
    String cmd = e.getActionCommand();
    if( cmd != null ) {
      switch( cmd ) {
	case ACTION_APPROVE:
	  doApprove();
	  break;
	case ACTION_CANCEL:
	  doClose();
	  break;
	case ACTION_TRANSFER_FOCUS:
	  Object src = e.getSource();
	  if( src != null ) {
	    if( src instanceof Container ) {
	      ((Container) src).transferFocus();
	    }
	  }
	  break;
      }
    }
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public boolean doClose()
  {
    boolean rv = super.doClose();
    if( rv ) {
      if( this.btnOK != null ) {
	this.btnOK.removeActionListener( this );
      }
      if( this.btnCancel != null ) {
	this.btnCancel.removeActionListener( this );
      }
    }
    return rv;
  }
}
