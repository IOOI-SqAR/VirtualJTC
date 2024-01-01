/*
 * (c) 2007-2021 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Basisklasse fuer alle Dialoge
 */

package jtcemu.platform.se.base;

import java.awt.Component;
import java.awt.Dialog;
import java.awt.Window;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.HashSet;
import java.util.Set;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import jtcemu.platform.se.Main;


public class BaseDlg extends JDialog implements WindowListener
{
  private static Set<String> suppressedMessages = new HashSet<>();


  protected BaseDlg( Window owner )
  {
    super( owner, Dialog.ModalityType.DOCUMENT_MODAL );
    setDefaultCloseOperation( DO_NOTHING_ON_CLOSE );
    Main.setIconImages( this );
    addWindowListener( this );
  }


  protected boolean doClose()
  {
    setVisible( false );
    dispose();
    return true;
  }


  protected void setParentCentered()
  {
    setParentCentered( this );
  }


  public static void setParentCentered( Window window )
  {
    Component p = window.getParent();
    if( p != null ) {
      int x = p.getX() + ((p.getWidth() - window.getWidth()) / 2);
      int y = p.getY() + ((p.getHeight() - window.getHeight()) / 2);
      window.setLocation( x > 0 ? x : 0, y > 0 ? y : 0 );
    }
  }


  public static void showError( Component owner, String msg )
  {
    if( msg == null ) {
      msg = "Unbekannter Fehler";
    }
    JOptionPane.showMessageDialog(
			owner,
			msg,
			"Fehler",
			JOptionPane.ERROR_MESSAGE );
  }


  public static void showSuppressableInfoDlg( Component owner, String msg )
  {
    if( msg != null ) {
      if( !suppressedMessages.contains( msg ) ) {
	JCheckBox cb = new JCheckBox( "Diesen Hinweis nicht mehr anzeigen" );
	JOptionPane.showMessageDialog(
			owner,
			new Object[] { msg, cb },
			"Hinweis",
			JOptionPane.INFORMATION_MESSAGE );
	if( cb.isSelected() ) {
	  suppressedMessages.add( msg );
	}
      }
    }
  }


	/* --- WindowListener --- */

  @Override
  public void windowActivated( WindowEvent e )
  {
    // leer;
  }


  @Override
  public void windowClosed( WindowEvent e )
  {
    // leer;
  }


  @Override
  public void windowClosing( WindowEvent e )
  {
    doClose();
  }


  @Override
  public void windowDeactivated( WindowEvent e )
  {
    // leer;
  }


  @Override
  public void windowDeiconified( WindowEvent e )
  {
    // leer;
  }


  @Override
  public void windowIconified( WindowEvent e )
  {
    // leer;
  }


  @Override
  public void windowOpened( WindowEvent e )
  {
    // leer
  }
}