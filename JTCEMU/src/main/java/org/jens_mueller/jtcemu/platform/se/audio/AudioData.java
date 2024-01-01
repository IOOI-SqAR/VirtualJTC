/*
 * (c) 2020 Jens Mueller
 *
 * Kleincomputer-Emulator
 *
 * Unterstuetzung fuer gespeicherte Audiodaten,
 * Intern werden die Audiodaten im CSW-Format gespeichert.
 */

package org.jens_mueller.jtcemu.platform.se.audio;

import org.jens_mueller.jtcemu.base.AppContext;
import org.jens_mueller.jtcemu.base.JTCUtil;

import javax.sound.sampled.*;
import java.io.*;


public class AudioData
{
  public static final String CSW_MAGIC = "Compressed Square Wave\u001A";


  private static class DataBuf extends ByteArrayOutputStream
  {
    private DataBuf()
    {
      super( 0x1000 );
    }

    private InputStream createInputStream()
    {
      return new ByteArrayInputStream( this.buf, 0, this.count );
    }
  };


  private static class CSWInputStream extends InputStream
  {
    private InputStream in;
    private boolean     curPhase;
    private long        curSampleCount;

    private CSWInputStream( InputStream in, boolean initialPhase )
    {
      this.in             = in;
      this.curPhase       = !initialPhase;
      this.curSampleCount = 0;
    }

    @Override
    public void close() throws IOException
    {
      this.in.close();
    }

    @Override
    public int read() throws IOException
    {
      int rv = -1;
      if( this.curSampleCount == 0 ) {
	this.curPhase       = !this.curPhase;
	this.curSampleCount = this.in.read();
	if( this.curSampleCount == 0 ) {
	  this.curSampleCount = readInt4( in );
	}
      }
      if( this.curSampleCount > 0 ) {
	--this.curSampleCount;
	rv = (this.curPhase ?
			AudioOut.PHASE_1_VALUE
			: AudioOut.PHASE_0_VALUE );
      }
      return rv;
    }

    @Override
    public int read( byte[] buf ) throws IOException
    {
      return read( buf, 0, buf.length );
    }

    @Override
    public int read( byte[] buf, int offs, int len ) throws IOException
    {
      int rv = 0;
      if( len > 0 ) {
	int b = read();
	if( b < 0 ) {
	  rv = -1;
	} else {
	  buf[ offs++ ] = (byte) b;
	  rv            = 1;
	  while( --len > 0 ) {
	    b = read();
	    if( b < 0 ) {
	      break;
	    }
	    buf[ offs++ ] = (byte) b;
	    rv++;
	  }
	}
      }
      return rv;
    }
  };


  private int     sampleRate;
  private long    sampleCount;
  private long    pendingSampleCount;
  private boolean initialPhase;
  private boolean lastPhase;
  private DataBuf cswBuf;


  public AudioData( int sampleRate )
  {
    this.sampleRate         = sampleRate;
    this.sampleCount        = 0;
    this.pendingSampleCount = 0;
    this.initialPhase       = false;
    this.lastPhase          = false;
    this.cswBuf             = new DataBuf();
  }


  public void addSamples( int sampleCount, boolean phase ) throws IOException
  {
    if( sampleCount > 0 ) {
      if( (this.sampleCount > 0) || (this.pendingSampleCount > 0) ) {
	if( phase == this.lastPhase ) {
	  this.pendingSampleCount += sampleCount;
	} else {
	  writePendingSamplesToCSWBuf();
	  this.pendingSampleCount = sampleCount;
	  this.lastPhase          = phase;
	}
      } else {
	this.initialPhase       = phase;
	this.lastPhase          = phase;
	this.pendingSampleCount = sampleCount;
      }
    }
  }


  public AudioInputStream createAudioInputStream()
  {
    return new AudioInputStream(
			new CSWInputStream(
				this.cswBuf.createInputStream(),
				this.initialPhase ),
			createAudioFormat( this.sampleRate ),
			this.sampleCount );
  }


  public void finish( int trailingSampleCount ) throws IOException
  {
    if( this.pendingSampleCount < trailingSampleCount ) {
      writePendingSamplesToCSWBuf();
    }
    this.pendingSampleCount = trailingSampleCount;
    writePendingSamplesToCSWBuf();
  }


  public long getSampleCount()
  {
    return this.sampleCount;
  }


  public int getSampleRate()
  {
    return this.sampleRate;
  }


  public static AudioInputStream openFile( File file ) throws IOException
  {
    AudioInputStream rv = null;
    InputStream      in = null;
    try {
      in = new BufferedInputStream(
			new FileInputStream( file ),
			0x1000 );
      in.mark( 0x1000 );

      // CSW-Signatur testen
      boolean csw = true;
      int     n   = CSW_MAGIC.length();
      for( int i = 0; i < n; i++ ) {
	if( CSW_MAGIC.charAt( i ) != in.read() ) {
	  csw = false;
	  break;
	}
      }
      if( csw ) {
	rv = getAudioInputStreamFromCSW( in );
      } else {
	in.reset();
	try {
	  rv = AudioSystem.getAudioInputStream( in );
	}
	catch( UnsupportedAudioFileException ex ) {
	  throwUnsupportedFileFormat();
	}
      }
    }
    catch( IOException | RuntimeException ex ) {
      JTCUtil.closeSilently( rv );
      JTCUtil.closeSilently( in );
      throw ex;
    }
    return rv;
  }


  public void writeToFile( File file ) throws IOException
  {
    boolean done  = false;
    String  fName = file.getName();
    if( fName != null ) {
      fName = fName.toLowerCase();
      if( fName.endsWith( ".csw" ) ) {
	writeToCSWFile( file );
	done = true;
      } else {
	AudioFileFormat.Type fileType = null;
	if( fName.endsWith( ".aifc" ) ) {
	  fileType = AudioFileFormat.Type.AIFC;
	}
	else if( fName.endsWith( ".aif" ) || fName.endsWith( ".aiff" ) ) {
	  fileType = AudioFileFormat.Type.AIFF;
	}
	 else if( fName.endsWith( ".au" ) ) {
	  fileType = AudioFileFormat.Type.AU;
	}
	else if( fName.endsWith( ".snd" ) ) {
	  fileType = AudioFileFormat.Type.SND;
	}
	else if( fName.endsWith( ".wav" ) ) {
	  fileType = AudioFileFormat.Type.WAVE;
	}
	if( fileType != null ) {
	  AudioInputStream in  = null;
	  OutputStream     out = null;
	  try {
	    out = new BufferedOutputStream( new FileOutputStream( file ) );
	    in  = createAudioInputStream();
	    AudioSystem.write( in, fileType, out );
	    out.close();
	    out  = null;
	    done = true;
	  }
	  finally {
	    JTCUtil.closeSilently( in );
	    JTCUtil.closeSilently( out );
	  }
	}
      }
    }
    if( !done ) {
      throwUnsupportedFileFormat();
    }
  }


	/* --- private Methoden --- */

  private static AudioFormat createAudioFormat( int sampleRate )
  {
    return new AudioFormat( (float) sampleRate, 8, 1, false, false );
  }


  // CSW-Signatur wurde bereits gelesen
  private static AudioInputStream getAudioInputStreamFromCSW(
					InputStream in ) throws IOException
  {
    AudioInputStream rv = null;

    // Hauptversionsnummer
    int version = in.read();
    if( (version < 1) || (version > 2) ) {
      throwUnsupportedFileFormat();
    }

    // Unterversionsnummer
    readByte( in );

    // restlichen Header
    int     sampleRate   = 0;
    long    sampleCount  = 0;
    boolean initialPhase = false;
    if( version == 1 ) {

      // Abtastrate
      int b      = readByte( in );
      sampleRate = ((readByte( in ) << 8) & 0xFF00) | b;

      // Kompression
      b = readByte( in );
      if( b != 1 ) {
	throwUnsupportedCompression( b );
      }

      /*
       * Flags, Bit 0: Initialphase
       *
       * Die Initialphase wird erst ab CSW-Versopn 1.01 gespeichert.
       * Sie wird aber auch bei Version 1.00 aus dem Flag-Byte gelesen.
       * Der in dem Fall moegicherweise falsch ermittelte Wert
       * spielt aber keine Rolle, da der richtige Wert nicht enthalten
       * und somit eh nicht ermittelbar ist.
       */
      initialPhase = ((readByte( in ) & 0x01) != 0);

      // reservierte Bytes
      for( int i = 0; i < 3; i++ ) {
	readByte( in );
      }

    } else {

      // Abtastrate
      sampleRate = (int) readInt4( in );

      // Laenge
      sampleCount = readInt4( in );

      // Kompression
      int b = readByte( in );
      if( b != 1 ) {
	throwUnsupportedCompression( b );
      }

      // Flags, Bit 0: Initialphase
      initialPhase = ((readByte( in ) & 0x01) != 0);

      // Laenge der Header-Erweiterung
      b = readByte( in );

      /*
       * Rest des Headers ueberlesen:
       * 16 Byte fuer erzeugende Applikation + Header-Erweiterung
       */
      b += 16;
      for( int i = 0; i < b; i++ ) {
	readByte( in );
      }
    }
    if( (sampleRate < 1) || (sampleCount < 0) ) {
      throwUnsupportedFileFormat();
    }

    /*
     * Wenn die Audio-Laenge im Header stand,
     * koennen die Audiodaten direkt aus der Datei gelesen werden.
     * Anderenfalls wird die Datei zuerst komplett eingelesen
     * und dabei die Laenge ermittelt.
     */
    if( sampleCount > 0 ) {
      rv = new AudioInputStream(
			new CSWInputStream( in, initialPhase ),
			createAudioFormat( sampleRate ),
			sampleCount );
    } else {
      DataBuf buf = new DataBuf();
      int     b   = in.read();
      while( b >= 0 ) {
	if( b == 0 ) {
	  long v = readInt4( in );
	  if( v < 1 ) {
	    break;
	  }
	  buf.write( 0 );
	  writeInt4( buf, v );
	  sampleCount += v;
	} else {
	  buf.write( b );
	  sampleCount += b;
	}
	b = in.read();
      }
      JTCUtil.closeSilently( in );
      rv = new AudioInputStream(
		new CSWInputStream( buf.createInputStream(), initialPhase ),
		createAudioFormat( sampleRate ),
		sampleCount );
    }
    return rv;
  }


  private static int readByte( InputStream in ) throws IOException
  {
    int b = in.read();
    if( b < 0 ) {
      throwUnsupportedFileFormat();
    }
    return b;
  }


  private static long readInt4( InputStream in ) throws IOException
  {
    long rv = -1;
    int  b0 = in.read();
    int  b1 = in.read();
    int  b2 = in.read();
    int  b3 = in.read();
    if( (b0 >= 0) && (b1 >= 0) && (b2 >= 0) && (b3 >= 0) ) {
      rv = (((long) b3 << 24) & 0xFF000000L)
			| (((long) b2 << 16) & 0x00FF0000L)
			| (((long) b1 << 8) & 0x0000FF00L)
			| ((long) b0 & 0x000000FFL);
    }
    return rv;
  }


  private static void throwUnsupportedCompression( int v ) throws IOException
  {
    throw new IOException(
		String.format(
			"Kompressionsmethode %d nicht unterst\u00FCtzt",
			v ) );
  }


  private static void throwUnsupportedFileFormat() throws IOException
  {
    throw new IOException( "Dateiformat nicht unterst\u00FCtzt" );
  }


  private static void writeASCII(
			OutputStream out,
			CharSequence text ) throws IOException
  {
    if( text != null ) {
      int len = text.length();
      for( int i = 0; i < len; i++ ) {
	out.write( text.charAt( i ) );
      }
    }
  }


  private void writeToCSWFile( File file ) throws IOException
  {
    OutputStream out = null;
    try {
      out = new FileOutputStream( file );

      // CSW-Signatur
      writeASCII( out, CSW_MAGIC );

      // CSW-Versionsnummer 2.00
      out.write( 2 );
      out.write( 0 );

      // 4 Bytes Abtastrate
      writeInt4( out, this.sampleRate );

      // 4 Bytes Gesamtanzahl Pulse
      writeInt4( out, this.sampleCount );

      // Kompression
      out.write( 1 );			// RLE

      // Flags
      out.write( this.initialPhase ? 0x01 : 0 );

      // Header Extension
      out.write( 0 );			// keine Erweiterung

      // Encoding Application
      writeFixLengthASCII( out, AppContext.getAppName(), 16, 0 );

      // CSW-Daten
      this.cswBuf.writeTo( out );

      // fertig
      out.close();
      out = null;
    }
    finally {
      JTCUtil.closeSilently( out );
    }
  }


  private static void writeFixLengthASCII(
			OutputStream out,
			CharSequence text,
			int          len,
			int          filler ) throws IOException
  {
    if( text != null ) {
      int srcLen = text.length();
      int srcIdx = 0;
      while( (len > 0) && (srcIdx < srcLen) ) {
	out.write( text.charAt( srcIdx++ ) );
	--len;
      }
    }
    while( len > 0 ) {
      out.write( 0 );
      --len;
    }
  }


  private static void writeInt4(
				OutputStream out,
				long         v ) throws IOException
  {
    for( int i = 0; i < 4; i++ ) {
      out.write( (int) v & 0xFF );
      v >>= 8;
    }
  }


  private void writePendingSamplesToCSWBuf() throws IOException
  {
    if( this.pendingSampleCount > 0xFF ) {
      this.cswBuf.write( 0 );
      writeInt4( this.cswBuf, this.pendingSampleCount );
    } else {
      this.cswBuf.write( (int) this.pendingSampleCount );
    }
    this.sampleCount += this.pendingSampleCount;
    if( this.sampleCount > Integer.MAX_VALUE ) {
      throw new IOException(
			"Menge der gespeicherten Audiodaten zu gro\u00DF" );
    }
  }
}
