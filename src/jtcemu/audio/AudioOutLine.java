/*
 * (c) 2007-2010 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Klasse zur Bedienung des Audio-Ausgangs
 * fuer die Emulation des Anschlusses des Magnettonbandgeraetes
 */

package jtcemu.audio;

import javax.sound.sampled.*;
import jtcemu.base.JTCSys;
import z8.Z8;


public class AudioOutLine extends AudioOut
{
  private static int[] sampleRatesSound = { 22050, 16000, 8000 };
  private static int[] sampleRatesData  = { 44100, 32000, 22050 };

  private boolean        forDataTransfer;
  private SourceDataLine dataLine;
  private byte[]         audioDataBuf;
  private int            audioDataPos;


  public AudioOutLine( Z8 z8, JTCSys jtcSys, boolean forDataTransfer )
  {
    super( z8, jtcSys );
    this.forDataTransfer = forDataTransfer;
    this.dataLine        = null;
    this.audioDataBuf    = null;
    this.audioDataPos    = 0;
  }


        /* --- ueberschriebene Methoden --- */

  /*
   * Mit dieser Methode erfaehrt die Klasse den aktuellen
   * Taktzyklenzahlerstand und die Anzahl der seit dem letzten
   * Aufruf vergangenen Taktzyklen.
   *
   * Sollte die Zeit zu gross sein, werden die im Puffer stehenden
   * Audio-Daten ignoriert.
   */
  @Override
  protected void currentCycles( long totalCycles, int diffCycles )
  {
    if( diffCycles > this.maxPauseCycles ) {
      this.lastCycles   = totalCycles;
      this.audioDataPos = 0;
      DataLine line = this.dataLine;
      if( line != null ) {
        line.flush();
      }

    } else {

      /*
       * Wenn Daten geschrieben werden, darf das Soundsystem
       * auf keinen Fall auf die CPU-Emulation warten.
       * In diesem Fall wird die Geschwindigkeitsbremse
       * der CPU-Emulation temporaer, d.h.,
       * bis mindestens zum naechsten Soundsystemaufruf, abgeschaltet.
       */
      if( this.forDataTransfer )
        this.z8.setSpeedUnlimitedFor( diffCycles * 8 );
    }
  }


  @Override
  public AudioFormat startAudio(
                        int   cyclesPerSecond,
                        int   sampleRate,
                        float thresholdValue )
  {
    AudioFormat fmt = null;
    if( this.dataLine != null ) {
      fmt = this.dataLine.getFormat();
    } else {
      if( cyclesPerSecond > 0 ) {

        // Audio-Ausgabekanal oeffnen, Mono
        SourceDataLine line = null;
        if( sampleRate > 0 ) {
          line = openSourceDataLine( sampleRate );
        } else {
          int[] sampleRates = this.forDataTransfer ? sampleRatesData : sampleRatesSound;
          for( int i = 0; (line == null) && (i < sampleRates.length); i++ )
            line = openSourceDataLine( sampleRates[ i ] );
        }
        if( line != null ) {
          this.lastPhaseSamples = 0;
          this.maxPauseCycles   = cyclesPerSecond / 10;        // 0.1 Sekunde
          this.enabled          = true;
          this.dataLine         = line;
          fmt                   = this.dataLine.getFormat();
          this.cyclesPerFrame   = Math.round( (float) cyclesPerSecond
                                                        / fmt.getFrameRate() );

          // Audio-Buffer anlegen
          int r = Math.round( fmt.getFrameRate() );
          int n = line.getBufferSize() / 32;
          if( n > r / 2 ) {                // max. 1/2 Sekunde puffern
            n = r / 2;
          }
          if( n < 1 ) {
            n = 1;
          }
          this.audioDataBuf = new byte[ n * fmt.getFrameSize() ];
          this.audioDataPos = 0;
        }
      }
    }
    return fmt;
  }


  @Override
  public void stopAudio()
  {
    DataLine line = this.dataLine;
    this.dataLine = null;
    this.enabled  = false;
    DataLineCloser.closeDataLine( line );
  }


  @Override
  protected void writeSamples( int nSamples, byte value )
  {
    SourceDataLine line         = this.dataLine;
    byte[]         audioDataBuf = this.audioDataBuf;
    if( (line != null) && (audioDataBuf != null) ) {
      for( int i = 0; i < nSamples; i++ ) {
        if( this.audioDataPos >= audioDataBuf.length ) {
          line.write( audioDataBuf, 0, audioDataBuf.length );
          this.audioDataPos = 0;
        }
        audioDataBuf[ this.audioDataPos++ ] = value;
      }
    }
  }


        /* --- private Methoden --- */

  private SourceDataLine openSourceDataLine( int sampleRate )
  {
    SourceDataLine line = openSourceDataLine2( sampleRate, false );
    if( line == null ) {
      line = openSourceDataLine2( sampleRate, true );
    }
    return line;
  }


  private SourceDataLine openSourceDataLine2(
                                int     sampleRate,
                                boolean bigEndian )
  {
    AudioFormat fmt = new AudioFormat(
                                (float) sampleRate,
                                8,
                                1,
                                true,
                                bigEndian );

    SourceDataLine line = null;
    try {
      line = AudioSystem.getSourceDataLine( fmt );
      if( line != null ) {
        line.open( fmt );
        line.start();
        this.sampleRate = sampleRate;
      }
    }
    catch( Exception ex ) {
      DataLineCloser.closeDataLine( line );
      line = null;
    }
    return line;
  }
}
