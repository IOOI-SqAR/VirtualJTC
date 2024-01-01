/*
 * (c) 2021 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Komponente zur Anzeige einer LED
 */

package jtcemu.platform.se.audio;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import javax.swing.JComponent;


public class LED extends JComponent
{
  private boolean lighted;

  public LED()
  {
    this.lighted = false;
    setPreferredSize( new Dimension( 30, 12 ) );
  }


  public void setLighted( boolean state )
  {
    if( state != this.lighted ) {
      this.lighted = state;
      if( isEnabled() ) {
	repaint();
      }
    }
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void paintComponent( Graphics g )
  {
    int w = getWidth() - 1;
    int h = getHeight() - 1;
    if( (w > 1) && (h > 1) ) {
      g.setColor( this.lighted && isEnabled() ?
					Color.RED
					: Color.LIGHT_GRAY );
      g.fillRect( 0, 0, w, h );
      g.setColor( Color.BLACK );
    }
  }
}