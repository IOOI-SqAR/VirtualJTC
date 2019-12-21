/*
 * (c) 2007-2010 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Klasse zur Bedienung des Audio-Eingangs
 * fuer die Emulation des Anschlusses des Magnettonbandgeraetes
 */

package org.sqar.virtualjtc.jtcemu.audio;

import javax.sound.sampled.*;
import org.sqar.virtualjtc.z8.Z8;


public class AudioInLine extends AudioIn
{
  private static int[] sampleRates = { 44100, 32000, 22050 };

  private TargetDataLine dataLine;
  private byte[]         frameBuf;
  private byte[]         audioDataBuf;
  private int            audioDataLen;
  private int            audioDataPos;
  private int            maxPauseCycles;


  public AudioInLine( Z8 z8 )
  {
    super( z8 );
    this.dataLine       = null;
    this.frameBuf       = null;
    this.audioDataBuf   = null;
    this.audioDataLen   = 0;
    this.audioDataPos   = 0;
    this.maxPauseCycles = 0;
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
      this.lastCycles = totalCycles;
      this.minValue   = 0;
      this.maxValue   = 0;
      DataLine line   = this.dataLine;
      if( line != null )
        line.flush();

    } else {

      /*
       * Wenn Daten gelesen werden, darf das Soundsystem
       * auf keinen Fall auf die CPU-Emulation warten.
       * In diesem Fall wird die Geschwindigkeitsbremse
       * der CPU-Emulation temporaer, d.h.,
       * bis mindestens zum naechsten Soundsystemaufruf, abgeschaltet.
       */
      this.z8.setSpeedUnlimitedFor( diffCycles * 8 );
    }
  }


  @Override
  protected byte[] readFrame()
  {
    int            value        = -1;
    TargetDataLine line         = this.dataLine;
    byte[]         audioDataBuf = this.audioDataBuf;
    byte[]         frameBuf     = this.frameBuf;
    if( (line != null) && (audioDataBuf != null) && (frameBuf != null) ) {
      
      if( this.audioDataPos >= this.audioDataLen ) {
        this.audioDataLen = line.read(
                                this.audioDataBuf,
                                0,
                                this.audioDataBuf.length );
        this.audioDataPos = 0;
      }
      if( this.audioDataPos + frameBuf.length <= this.audioDataLen ) {
        System.arraycopy(
                audioDataBuf,
                this.audioDataPos,
                frameBuf, 0,
                frameBuf.length );
        this.audioDataPos += frameBuf.length;
      }
    }
    return frameBuf;
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

        // Audio-Eingabekanal oeffnen
        TargetDataLine line = null;
        if( sampleRate > 0 ) {
          line = openTargetDataLine( sampleRate );
        } else {
          for( int i = 0;
               (line == null) && (i < sampleRates.length);
               i++ )
          {
            line = openTargetDataLine( sampleRates[ i ] );
          }
        }

        if( line != null ) {
          fmt                 = line.getFormat();
          this.dataLine       = line;
          this.maxPauseCycles = cyclesPerSecond;        // 1 Sekunde
          this.cyclesPerFrame = Math.round( (float) cyclesPerSecond
                                                / fmt.getFrameRate() );
          this.thresholdValue = thresholdValue;

          // Buffer fuer ein Frame anlegen
          this.frameBuf = new byte[ fmt.getFrameSize() ];

          // Buffer fuer Leseoperationen anlegen
          int r = Math.round( fmt.getFrameRate() );
          int n = this.dataLine.getBufferSize() / 32;
          if( n > r / 2 ) {                // max. 1/2 Sekunde puffern
            n = r / 2 ;
          }
          if( n < 1 ) {
            n = 1;
          }
          this.audioDataBuf = new byte[ n * this.frameBuf.length ];
          this.audioDataLen = 0;
          this.audioDataPos = this.audioDataLen;
          setAudioFormat( fmt );
        }
      }
    }
    return fmt;
  }


  @Override
  public void stopAudio()
  {
    this.frameBuf     = null;
    this.audioDataBuf = null;
    this.audioDataPos = 0;

    DataLine line = dataLine;
    this.dataLine = null;
    DataLineCloser.closeDataLine( line );
  }


        /* --- private Methoden --- */

  private TargetDataLine openTargetDataLine( int sampleRate )
  {
    TargetDataLine line = openTargetDataLine( sampleRate, 2 );
    if( line == null ) {
      line = openTargetDataLine( sampleRate, 1 );
    }
    return line;
  }


  private TargetDataLine openTargetDataLine( int sampleRate, int channels )
  {
    TargetDataLine line = openTargetDataLine( sampleRate, channels, false );
    if( line == null ) {
      line = openTargetDataLine( sampleRate, channels, true );
    }
    return line;
  }


  private TargetDataLine openTargetDataLine(
                                int     sampleRate,
                                int     channels,
                                boolean bigEndian )
  {
    AudioFormat fmt = new AudioFormat(
                                sampleRate,
                                8,
                                channels,
                                true,
                                bigEndian );

    TargetDataLine line = null;
    try {
      line = (TargetDataLine) AudioSystem.getTargetDataLine( fmt );
      if( line != null ) {
        line.open( fmt );
        line.start();
      }
    }
    catch( Exception ex ) {
      DataLineCloser.closeDataLine( line );
      line = null;
    }
    return line;
  }
}
