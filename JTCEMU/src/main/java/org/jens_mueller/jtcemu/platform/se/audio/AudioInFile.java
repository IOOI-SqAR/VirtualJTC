/*
 * (c) 2007-2021 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Klasse fuer die Emulation
 * des Anschlusses des Magnettonbandgeraetes (Eingang),
 * indem die Audio-Daten von einer Datei gelesen werden.
 */

package org.jens_mueller.jtcemu.platform.se.audio;

import org.jens_mueller.jtcemu.base.JTCUtil;
import org.jens_mueller.z8.Z8;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.SourceDataLine;
import java.io.File;
import java.io.IOException;


public class AudioInFile extends AudioIn
{
  private static final String TEXT_NO_MONITOR_LINE =
	"Audiokanal zum Mith\u00F6ren konnte nicht ge\u00F6ffnet werden.";

  private static final String TEXT_MONITOR_NOT_WORKING =
	"Audiokanal zum Mith\u00F6ren funktioniert nicht.";

  private File             file;
  private AudioInputStream in;
  private byte[]           frameBuf;
  private long             framePos;
  private long             frameLen;
  private volatile boolean pause;
  private boolean          monitorPause;
  private volatile Boolean monitorRequest;
  private SourceDataLine   monitorLine;
  private byte[]           monitorBuf;
  private int              monitorPos;
  private int              monitorOffs;


  public AudioInFile(
		AudioInFld audioInFld,
		Z8         z8,
		float      thresholdValue,
		File       file,
		boolean    monitor )
 {
    super( audioInFld, z8, thresholdValue );
    this.file           = file;
    this.in             = null;
    this.frameBuf       = null;
    this.framePos       = 0;
    this.frameLen       = -1;
    this.pause          = true;
    this.monitorPause   = false;
    this.monitorRequest = (monitor ? Boolean.TRUE : null);
    this.monitorLine    = null;
    this.monitorBuf     = null;
    this.monitorPos     = 0;
    this.monitorOffs    = 0;
  }


  public long getFramePos()
  {
    return this.framePos;
  }


  public long getFrameLength()
  {
    return this.frameLen;
  }


  public boolean isPause()
  {
    return this.pause;
  }


  public void setMonitorRequest( boolean request )
  {
    this.monitorRequest = Boolean.valueOf( request );
  }


  public void setPause( boolean state )
  {
    this.pause = state;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  protected void checkOpenSource() throws IOException
  {
    if( (this.in == null) && !this.stopRequested ) {
      this.in         = AudioData.openFile( this.file );
      AudioFormat fmt = this.in.getFormat();
      this.frameLen   = this.in.getFrameLength();
      this.frameBuf   = new byte[ fmt.getFrameSize() ];
      setAudioFormat( fmt );
    }
  }


  @Override
  public synchronized void closeCPUSynchronLine()
  {
    if( (this.monitorLine != null)
	&& isCPUSynchronLine( this.monitorLine ) )
    {
      this.monitorRequest = false;
      closeDataLine( this.monitorLine );
      this.monitorLine = null;
      this.audioFld.fireMonitorStatusChanged( this, false, null );
    }
  }


  @Override
  protected void closeSource()
  {
    if( this.in != null ) {
      JTCUtil.closeSilently( this.in );
      this.in = null;
    }
    closeMonitor( null );
  }


  @Override
  protected byte[] readFrame() throws IOException, InterruptedException
  {
    AudioInputStream in  = this.in;
    byte[]           buf = this.frameBuf;
    if( (in != null) && (buf != null) && !this.pause ) {
      if( in.read( buf ) == buf.length ) {
	this.framePos++;
      } else {
	requestStop();
      }
    }
    return buf;
  }


  @Override
  protected int readFrameAndGetSample()
			throws IOException, InterruptedException
  {
    int sampleValue = super.readFrameAndGetSample();

    // Mithoeren
    Boolean monitorRequest = this.monitorRequest;
    if( monitorRequest != null ) {
      if( monitorRequest.booleanValue()
	  && (this.frameRate > 0)
	  && (this.sampleSizeInBits > 0)
	  && (this.sampleSizeInBytes > 0) )
      {
	if( this.monitorLine == null ) {
	  try {
	    this.monitorLine = openSourceDataLine(
					new AudioFormat(
						(float) this.frameRate,
						this.sampleSizeInBits,
						1,
						this.dataSigned,
						this.bigEndian ),
					null );
	    if( this.monitorLine != null ) {
	      this.monitorPause = false;
	      this.monitorPos   = 0;
	      this.monitorBuf   = new byte[ Math.min(
			this.monitorLine.getBufferSize() / 4, 512 ) ];
	      this.audioFld.fireMonitorStatusChanged( this, true, null );
	    } else {
	      this.audioFld.fireMonitorStatusChanged(
						this,
						false,
						TEXT_NO_MONITOR_LINE );
	    }
	  }
	  catch( IOException ex ) {
	    this.audioFld.fireMonitorStatusChanged(
						this,
						false,
						ex.getMessage() );
	  }
	}
      } else {
	closeMonitor( null );
      }
      this.monitorRequest = null;
    }
    SourceDataLine line = this.monitorLine;
    if( line != null ) {
      try {
	if( this.pause ) {
	  if( !this.monitorPause ) {
	    line.stop();
	    dataLineStatusChanged();
	    this.monitorPause = true;
	  }
	} else {
	  if( this.monitorPause ) {
	    line.start();
	    dataLineStatusChanged();
	    this.monitorPause = false;
	  }
	}
	if( (sampleValue >= 0)
	    && (this.sampleSizeInBytes > 0)
	    && (this.monitorBuf != null)
	    && !this.monitorPause )
	{
	  if( (this.monitorPos + this.sampleSizeInBytes)
					> this.monitorBuf.length )
	  {
	    if( this.monitorPos > 0 ) {
	      if( line.available() < this.monitorPos ) {
		int n = 0;
		do {
		  Thread.sleep( 10 );
		  n++;
		  if( n > 100 ) {
		    closeMonitor( TEXT_MONITOR_NOT_WORKING );
		    line = null;
		    break;
		  }
		} while( line.available() < this.monitorPos );
	      }
	      if( line != null ) {
		if( this.stopRequested ) {
		  line.flush();
		} else {
		  line.write( this.monitorBuf, 0, this.monitorPos );
		  this.monitorPos = 0;
		}
	      }
	    } else {
	      closeMonitor( TEXT_MONITOR_NOT_WORKING );
	    }
	  }
	  if( (this.monitorPos + this.sampleSizeInBytes)
					> this.monitorBuf.length )
	  {
	    closeMonitor( TEXT_MONITOR_NOT_WORKING );
	  } else {
	    int v = sampleValue;
	    if( this.bigEndian ) {
	      int pos = this.monitorPos + this.sampleSizeInBytes;
	      for( int i = 0; i < this.sampleSizeInBytes; i++ ) {
		--pos;
		this.monitorBuf[ pos ] = (byte) v;
		v >>= 8;
	      }
	      this.monitorPos += this.sampleSizeInBytes;
	    } else {
	      for( int i = 0; i < this.sampleSizeInBytes; i++ ) {
		this.monitorBuf[ this.monitorPos++ ] = (byte) v;
		v >>= 8;
	      }
	    }
	  }
	}
      }
      catch( InterruptedException ex ) {
	closeMonitor( null );
	throw ex;
      }
    }
    return sampleValue;
  }


  @Override
  public void requestStop()
  {
    this.monitorRequest = Boolean.FALSE;
    super.requestStop();
  }


	/* --- private Methoden --- */

  private void closeMonitor( String errorText )
  {
    if( this.monitorLine != null ) {
      closeDataLine( this.monitorLine );
      this.monitorLine = null;
      this.audioFld.fireMonitorStatusChanged( this, false, errorText );
    }
  }
}
