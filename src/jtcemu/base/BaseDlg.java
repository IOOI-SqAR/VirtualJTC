/*
 * (c) 2007-2010 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Basisklasse fuer alle Dialoge
 */

package jtcemu.base;

import java.awt.*;
import java.awt.event.*;
import javax.swing.JDialog;
import jtcemu.Main;


public class BaseDlg extends JDialog implements WindowListener
{
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
    Component p = getParent();
    if( p != null ) {
      int x = p.getX() + ((p.getWidth() - getWidth()) / 2);
      int y = p.getY() + ((p.getHeight() - getHeight()) / 2);
      setLocation( x > 0 ? x : 0, y > 0 ? y : 0 );
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
