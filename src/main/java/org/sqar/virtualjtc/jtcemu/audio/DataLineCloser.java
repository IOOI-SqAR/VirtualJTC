/*
 * (c) 2007-2010 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Thread zum Schliessen einer DataLine
 *
 * Das Schliessen einer DataLine kann den aktuellen Thread blockieren.
 * Damit das nicht passiert, wird das Schliessen in einen separaten
 * Thread ausgelagert.
 */

package org.sqar.virtualjtc.jtcemu.audio;

import javax.sound.sampled.*;


public class DataLineCloser extends Thread
{
  private DataLine dataLine;


  public static void closeDataLine( DataLine dataLine )
  {
    if( dataLine != null ) {
      if( dataLine.isOpen() ) {
        Thread thread = new DataLineCloser( dataLine );
        thread.start();

        // max. eine Sekunde auf Thread-Beendigung warten
        try {
          thread.join( 1000 );
        }
        catch( InterruptedException ignored) {}
      }
    }
  }


        /* --- ueberschriebene Methoden --- */

  @Override
  public void run()
  {
    if( this.dataLine != null ) {
      try {
        this.dataLine.flush();
      }
      catch( Exception ignored) {}

      try {
        this.dataLine.stop();
      }
      catch( Exception ignored) {}

      try {
        this.dataLine.close();
      }
      catch( Exception ignored) {}

      this.dataLine = null;
    }
  }


        /* --- private Methoden --- */

  private DataLineCloser( DataLine dataLine )
  {
    this.dataLine = dataLine;
  }
}

