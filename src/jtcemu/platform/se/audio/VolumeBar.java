/*
 * (c) 2016-2020 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Komponente zur Anzeige der Lautstaerke
 */

package jtcemu.platform.se.audio;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;


public class VolumeBar extends JPanel
{
  private static final int VOLUME_BAR_MAX = 1000;

  private Object            lockObj;
  private javax.swing.Timer timer;
  private JProgressBar      progressBar;
  private int               minLimit;
  private int               maxLimit;
  private int               minValue;
  private int               maxValue;


  public VolumeBar()
  {
    this.minLimit  = 0;
    this.maxLimit  = 255;
    this.minValue  = this.maxLimit;
    this.maxValue  = this.minLimit;
    this.lockObj   = new Object();
    this.timer     = new javax.swing.Timer(
			100,
			new ActionListener()
			{
			  public void actionPerformed( ActionEvent e )
			  {
			    updVolumeBar();
			  }
			} );

    // Layout
    setLayout( new GridBagLayout() );

    GridBagConstraints gbc = new GridBagConstraints(
					0, 0,
					1, 1,
					0.0, 1.0,
					GridBagConstraints.CENTER,
					GridBagConstraints.VERTICAL,
					new Insets( 5, 5, 5, 5 ),
					0, 0 );
    this.progressBar = new JProgressBar(
				JProgressBar.VERTICAL,
				0,
				VOLUME_BAR_MAX );
    add( this.progressBar, gbc );
  }


  public void setVolumeLimits( int minLimit, int maxLimit )
  {
    if( minLimit < maxLimit ) {
      synchronized( this.lockObj ) {
	this.minLimit = minLimit;
	this.maxLimit = maxLimit;
	this.maxValue = maxLimit;
	this.minValue = minLimit;
	this.progressBar.setValue( 0 );
      }
    }
  }


  public void setVolumeBarState( boolean state )
  {
    if( state ) {
      this.timer.start();
    } else {
      this.progressBar.setValue( 0 );
      this.timer.stop();
    }
    this.progressBar.setEnabled( state );
  }


  public void updVolume( int value )
  {
    synchronized( this.lockObj ) {
      if( value < this.minValue ) {
	this.minValue = value;
      }
      if( value > this.maxValue ) {
	this.maxValue = value;
      }
    }
  }


	/* --- private Methoden --- */

  private void updVolumeBar()
  {
    int barValue = 0;
    int volume   = 0;
    synchronized( this.lockObj ) {
      volume        = this.maxValue - this.minValue;
      this.minValue = this.maxLimit;
      this.maxValue = this.minLimit;
    }
    /*
     * Logarithmische Pegelanzeige:
     *   Der Pegel wird auf den Bereich 0 bis 100 normiert,
     *   aus dem Wert plus eins der Logarithmus gebildet
     *   und anschliessend auf den Bereich der Anzeige skaliert.
     */
    double v = (double) volume
		/ (double) (this.maxLimit - this.minLimit)
		* 100.0;
    if( v > 0.0 ) {
      barValue = (int) Math.round( Math.log( 1.0 + v )
					* (double) VOLUME_BAR_MAX
					/ 4.6 );	// log(100)
      if( barValue < 0 ) {
	barValue = 0;
      } else if( barValue > VOLUME_BAR_MAX ) {
	barValue = VOLUME_BAR_MAX;
      }
    }
    this.progressBar.setValue( barValue );
  }
}
