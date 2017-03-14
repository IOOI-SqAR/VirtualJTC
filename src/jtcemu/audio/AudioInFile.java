/*
 * (c) 2007-2010 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Klasse fuer die Emulation
 * des Anschlusses des Magnettonbandgeraetes (Eingang),
 * indem die Audio-Daten von einer Datei gelesen werden.
 */

package jtcemu.audio;

import java.io.*;
import javax.sound.sampled.*;
import z8.Z8;


public class AudioInFile extends AudioIn
{
  private AudioFrm         audioFrm;
  private File             file;
  private boolean          monitorPlay;
  private AudioInputStream in;
  private byte[]           frameBuf;
  private long             frameCount;
  private long             framePos;
  private int              progressStepSize;
  private int              progressStepCnt;


  public AudioInFile(
                Z8       z8,
                AudioFrm audioFrm,
                File     file,
                boolean  monitorPlay )
  {
    super( z8 );
    this.audioFrm         = audioFrm;
    this.file             = file;
    this.monitorPlay      = monitorPlay;
    this.in               = null;
    this.frameBuf         = null;
    this.frameCount       = 0L;
    this.framePos         = 0L;
    this.progressStepSize = 0;
    this.progressStepCnt  = 0;
  }


        /* --- ueberschriebene Methoden --- */

  @Override
  protected byte[] readFrame()
  {
    AudioInputStream in  = this.in;
    byte[]           buf = this.frameBuf;
    if( (in != null) && (buf != null) ) {
      try {
        if( in.read( buf ) == buf.length ) {
          if( isMonitorPlayActive() ) {
            writeMonitorLine( buf );
          }
          this.framePos++;
          if( this.progressStepCnt > 0 ) {
            --this.progressStepCnt;
          } else {
            this.progressStepCnt = this.progressStepSize;
            this.audioFrm.fireProgressUpdate(
                        (double) this.framePos / (double) this.frameCount );
          }
        } else {
          buf = null;
          this.audioFrm.fireDisable();
        }
      }
      catch( IOException ex ) {
        this.errorText = ex.getMessage();
      }
    }
    return buf;
  }


  @Override
  public AudioFormat startAudio(
                        int   cyclesPerSecond,
                        int   sampleRate,
                        float thresholdValue )
  {
    AudioFormat fmt = null;
    if( (this.in == null) && (cyclesPerSecond > 0) ) {
      try {
        this.in         = AudioSystem.getAudioInputStream( this.file );
        fmt             = this.in.getFormat();
        this.frameCount = this.in.getFrameLength();
        if( this.frameCount > 0 ) {
          this.progressStepSize = (int) this.frameCount / 100;
          this.progressStepCnt  = this.progressStepSize;
          this.progressEnabled  = true;
        }
      }
      catch( UnsupportedAudioFileException ex ) {
        this.errorText = "Das Dateiformat wird nicht unterst\u00FCtzt.";
      }
      catch( Exception ex ) {
        this.errorText = "Die Datei kann nicht ge\u00F6ffnet werden.\n\n"
                                      + ex.getMessage();
      }
      if( (this.in != null) || (fmt != null) ) {
        this.frameBuf       = new byte[ fmt.getFrameSize() ];
        this.cyclesPerFrame = Math.round( (float) cyclesPerSecond
                                                / fmt.getFrameRate() );
        this.thresholdValue = thresholdValue;
      } else {
        stopAudio();
      }
    }
    setAudioFormat( fmt );
    if( this.monitorPlay ) {
      openMonitorLine( fmt );
    }
    return fmt;
  }


  @Override
  public void stopAudio()
  {
    closeMonitorLine();

    InputStream in       = this.in;
    this.in              = null;
    this.frameBuf        = null;
    this.progressEnabled = false;
    if( in != null ) {
      try {
        in.close();
      }
      catch( Exception ex ) {}
    }
  }
}
