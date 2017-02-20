/*
 * (c) 2007-2010 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Basisklasse fuer alle Frames
 */

package jtcemu.base;

import java.awt.event.*;
import java.lang.*;
import javax.swing.JFrame;
import jtcemu.Main;


public class BaseFrm extends JFrame implements WindowListener
{
  protected BaseFrm()
  {
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


  public void lafChanged()
  {
    // leer
  }


  public void prepareSettingsToSave()
  {
    // leer
  }


  public void settingsChanged()
  {
    // leer
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
