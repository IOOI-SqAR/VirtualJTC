/*
 * (c) 2020 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Wiedergabe gespeicherter Audiodaten
 */

package org.jens_mueller.jtcemu.platform.se.audio;

import org.jens_mueller.jtcemu.base.AppContext;
import org.jens_mueller.jtcemu.base.JTCUtil;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import javax.swing.*;
import java.awt.*;


public class AudioPlayer implements Runnable
{
  private Component owner;
  private AudioData samples;
  private boolean   cancelled;


  public static AudioPlayer play( Component owner, AudioData samples )
  {
    AudioPlayer player = new AudioPlayer( owner, samples );
    (new Thread(
		player,
		AppContext.getAppName() + " audio player" )).start();
    return player;
  }


  public void cancel()
  {
    this.cancelled = true;
  }


	/* --- Runnable --- */

  @Override
  public void run()
  {
    if( this.samples != null ) {
      int  sampleRate  = this.samples.getSampleRate();
      long sampleCount = this.samples.getSampleCount();
      if( (sampleRate > 0) && (sampleCount > 0) ) {
	SourceDataLine             line = null;
	ProgressMonitorInputStream in   = null;
	try {
	  AudioFormat format = new AudioFormat(
					(float) sampleRate,
					8,
					1,
					false,
					false );

	  line = AudioSystem.getSourceDataLine( format );
	  line.open( format );
	  line.start();

	  in = new ProgressMonitorInputStream(
				this.owner,
				"Wiedergabe...",
				this.samples.createAudioInputStream() );
	  ProgressMonitor pm = in.getProgressMonitor();
	  if( pm != null ) {
	    pm.setMinimum( 0 );
	    pm.setMaximum( sampleCount > Integer.MAX_VALUE ?
						Integer.MAX_VALUE
						: (int) sampleCount );
	  }

	  byte[] buf = new byte[ (sampleRate / 5) + 0x100 ];
	  int    n   = 0;
	  while( !this.cancelled ) {
	    n = in.read( buf );
	    if( n <= 0 ) {
	      break;
	    }
	    line.write( buf, 0, n );
	  }
	}
	catch( Exception ex ) {}
	finally {
	  JTCUtil.closeSilently( in );
	  if( line != null ) {
	    if( this.cancelled ) {
	      line.flush();
	    } else {
	      line.drain();
	    }
	    line.stop();
	    line.close();
	  }
	}
      }
    }
  }


	/* --- Konstruktor --- */

  private AudioPlayer( Component owner, AudioData samples )
  {
    this.owner     = owner;
    this.samples   = samples;
    this.cancelled = false;
  }
}
