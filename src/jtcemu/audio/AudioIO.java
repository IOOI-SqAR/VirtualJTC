/*
 * (c) 2007-2008 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Basisklasse fuer die Emulation
 * des Anschlusses des Magnettonbandgeraetes (Eingang und Ausgang)
 */

package jtcemu.audio;

import java.lang.*;
import javax.sound.sampled.*;
import z8.Z8;


public abstract class AudioIO
{
  protected Z8      z8;
  protected boolean firstCall;
  protected boolean progressEnabled;
  protected boolean lastPhase;
  protected long    lastCycles;
  protected int     cyclesPerFrame;
  protected String  errorText;

  private volatile SourceDataLine monitorLine;
  private volatile byte[]         monitorBuf;
  private volatile int            monitorPos;


  protected AudioIO( Z8 z8 )
  {
    this.z8              = z8;
    this.firstCall       = true;
    this.progressEnabled = false;
    this.lastPhase       = false;
    this.lastCycles      = 0;
    this.cyclesPerFrame  = 0;
    this.errorText       = null;
    this.monitorLine     = null;
    this.monitorBuf      = null;
    this.monitorPos      = 0;
  }


  /*
   * Die Methode wird aufgerufen um den aktuellen Zaehlerstand
   * der Taktzyklen sowie die Anzahl der Taktzyklen seit dem
   * letzten Aufruf mitzuteilen.
   * Abgeleitete Klassen koennen diese Methode ueberschreiben,
   * um z.B. auf eine zu lange Pause zu reagieren.
   */
  protected void currentCycles( long totalCycles, int diffCycles )
  {
    // empty
  }


  public String getErrorText()
  {
    return this.errorText;
  }


  public boolean isMonitorPlayActive()
  {
    return this.monitorLine != null;
  }


  public boolean isProgressUpdateEnabled()
  {
    return this.progressEnabled;
  }


  protected void openMonitorLine( AudioFormat fmt )
  {
    if( this.monitorLine == null ) {
      DataLine.Info  info = new DataLine.Info( SourceDataLine.class, fmt );
      SourceDataLine line = null;
      try {
	if( AudioSystem.isLineSupported( info ) ) {
	  line = (SourceDataLine) AudioSystem.getLine( info );
	  if( line != null ) {
	    line.open( fmt );
	    line.start();

	    // Buffer anlegen
	    int r = Math.round( fmt.getSampleRate() );
	    int n = line.getBufferSize() / 32;
	    if( n < r / 8 ) {
	      n = r / 8;		// min. 1/8 Sekunde
	    }
	    else if( n > r / 2 ) {
	      n = r / 2;		// max. 1/2 Sekunde
	    }
	    if( n < 1 ) {
	      n = 1;
	    }
	    this.monitorBuf = new byte[ n ];
	  }
	}
      }
      catch( Exception ex ) {
	DataLineCloser.closeDataLine( line );
	line = null;
      }
      this.monitorLine = line;
    }
  }


  protected void closeMonitorLine()
  {
    DataLine line    = this.monitorLine;
    this.monitorLine = null;
    if( line != null )
      DataLineCloser.closeDataLine( line );
  }


  protected void writeMonitorLine( byte[] buf )
  {
    SourceDataLine line = this.monitorLine;
    if( (line != null) && (buf != null) )
      line.write( buf, 0, buf.length );
  }


  protected void writeMonitorLine( int frameValue )
  {
    SourceDataLine line = this.monitorLine;
    byte[]         buf  = this.monitorBuf;
    if( (line != null) && (buf != null) ) {
      if( this.monitorPos >= buf.length ) {
	line.write( buf, 0, buf.length );
	this.monitorPos = 0;
      }
      buf[ this.monitorPos ] = (byte) frameValue;
      this.monitorPos++;
    }
  }


  public abstract AudioFormat startAudio(
					int   cyclesPerSecond,
					int   sampleRate,
					float thresholdValue );

  public abstract void stopAudio();
}

