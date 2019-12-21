/*
 * (c) 2007-2010 Jens Mueller
 * (c) 2017 Lars Sonchocky-Helldorf
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Klasse fuer die Emulation
 * des Anschlusses des Magnettonbandgeraetes (Ausgang),
 * indem die Audio-Daten in eine Datei geschrieben werden.
 */

package org.sqar.virtualjtc.jtcemu.audio;

import java.io.*;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.sound.sampled.*;
import org.sqar.virtualjtc.jtcemu.base.JTCSys;
import org.sqar.virtualjtc.z8.Z8;


public class AudioOutFile extends AudioOut
{
  private static final Locale locale = Locale.getDefault();
  private static final ResourceBundle audioOutFileResourceBundle = ResourceBundle.getBundle("AudioOutFile", locale);

  private AudioFrm             audioFrm;
  private File                 file;
  private AudioFileFormat.Type fileType;
  private boolean              monitorPlay;
  private AudioFormat          audioFmt;
  private AudioDataQueue       queue;


  public AudioOutFile(
                Z8                   z8,
                JTCSys               jtcSys,
                AudioFrm             audioFrm,
                File                 file,
                AudioFileFormat.Type fileType,
                boolean              monitorPlay )
  {
    super( z8, jtcSys );
    this.audioFrm    = audioFrm;
    this.file        = file;
    this.fileType    = fileType;
    this.monitorPlay = monitorPlay;
    this.audioFmt    = null;
    this.queue       = null;
  }


        /* --- ueberschriebene Methoden --- */

  /*
   * Wenn die Pause zu groess ist,
   * wird das Schreiben der Sound-Datei abgebrochen.
   */
  @Override
  protected void currentCycles( long totalCycles, int diffCycles )
  {
    if( diffCycles > this.maxPauseCycles ) {
      this.lastCycles = totalCycles;
      this.enabled    = false;
      this.audioFrm.fireDisable();
    }
  }


  @Override
  public AudioFormat startAudio(
                        int   cyclesPerSecond,
                        int   sampleRate,
                        float thresholdValue )
  {
    if( this.queue == null ) {
      if( cyclesPerSecond > 0 ) {
        if( sampleRate > 0 ) {
          this.sampleRate = sampleRate;
        } else {
          this.sampleRate = 44100;
        }
        this.queue            = new AudioDataQueue( this.sampleRate * 60 );
        this.lastPhaseSamples = 0;
        this.maxPauseCycles   = cyclesPerSecond;        // 1 Sekunde
        this.enabled          = true;
        this.audioFmt         = new AudioFormat(
                                        this.sampleRate,
                                        8,
                                        1,
                                        true,
                                        false );
        this.cyclesPerFrame = Math.round( (float) cyclesPerSecond
                                        / this.audioFmt.getFrameRate() );

        if( this.monitorPlay ) {
          openMonitorLine( this.audioFmt );
        }
      }
    }
    return this.audioFmt;
  }


  @Override
  public void stopAudio()
  {
    closeMonitorLine();

    AudioDataQueue queue = this.queue;
    this.enabled         = false;
    this.errorText       = queue.getErrorText();
    if( (this.errorText == null) && (queue.getTotalSampleCount() > 0) ) {
      try {
        AudioSystem.write(
                new AudioInputStream(
                        queue,
                        this.audioFmt,
                        queue.getTotalSampleCount() ),
                this.fileType,
                this.file );
      }
      catch( Exception ex ) {
        this.errorText = audioOutFileResourceBundle.getString("error.stopAudio.unknownError.errorText") + ex.getMessage();
      }
    } else {
      this.errorText = audioOutFileResourceBundle.getString("error.stopAudio.noAudioCreated.errorText");
    }
  }


  @Override
  protected void writeSamples( int nSamples, byte value )
  {
    if( nSamples > 0 ) {
      for( int i = 0; i < nSamples; i++ ) {
        this.queue.write( value );
      }
      if( isMonitorPlayActive() ) {
        for( int i = 0; i < nSamples; i++ )
          writeMonitorLine( value );
      }
    }
  }
}
