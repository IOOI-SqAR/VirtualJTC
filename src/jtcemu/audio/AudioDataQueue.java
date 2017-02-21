/*
 * (c) 2007-2010 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Speicheroptimierte Queue fuer Audio-Daten
 * Dabei wird davon ausgegangen,
 * das aufeinanderfolgende Samples haeufig den geleichen Wert haben.
 *
 * Auf der einen Seite werden die Audio-Daten hineingeschrieben.
 * Auf der anderen Seite koennen die daraus gebildeten Audio-Daten
 * als InputStream gelesen werden.
 * Mit dem Lesen sollte erst begonnen werden,
 * wenn keine Daten mehr hineingeschrieben werden.
 *
 * In jedem Element des Attributs "sampleCounts" werden die Anzahl
 * der Samples mit gleichem Wert gespeichert.
 * Der zugehoerige Wert steht dabei in "sampleValues".
 * Wenn ein Element ueberlaueft, wird das nachste Element
 * mit dem gleichen Wert beschrieben.
 */

package jtcemu.audio;

import java.io.InputStream;
import java.lang.*;


public class AudioDataQueue extends InputStream
{
  private byte[] sampleValues;
  private byte[] sampleCounts;
  private int    totalSampleCnt;
  private int    size;
  private int    pos;
  private String errorText;


  public AudioDataQueue( int initSize )
  {
    this.sampleValues   = new byte[ initSize ];
    this.sampleCounts   = new byte[ initSize ];
    this.totalSampleCnt = 0;
    this.size           = 0;
    this.pos            = 0;
    this.errorText      = null;
  }


  public String getErrorText()
  {
    return this.errorText;
  }


  public int getTotalSampleCount()
  {
    return this.totalSampleCnt;
  }


  public void write( byte value )
  {
    boolean done = false;
    if( (this.sampleValues != null) && (this.sampleCounts != null) ) {
      if( this.size > 0 ) {
        byte lastValue = this.sampleValues[ this.size - 1 ];
        if( value == lastValue ) {
          writeLastValueAgain();
          this.totalSampleCnt++;
          done = true;
        }
      }
      if( !done ) {
        if( ensureSize() ) {
          this.sampleCounts[ this.size ] = 1;
          this.sampleValues[ this.size ] = value;
          this.size++;
          this.totalSampleCnt++;
        }
      }
    }
  }


        /* --- ueberschriebene Methoden --- */

  @Override
  public boolean markSupported()
  {
    return false;
  }


  @Override
  public int read()
  {
    int rv = -1;
    if( (this.sampleValues != null) && (this.sampleCounts != null) ) {
      if( this.pos < this.size ) {
        if( this.sampleCounts[ this.pos ] <= 0 ) {
          this.pos++;
        }
      }
      if( this.pos < this.size ) {
        rv = (int) this.sampleValues[ this.pos ] & 0x0FF;
        this.sampleCounts[ this.pos ]--;
      }
    }
    return rv;
  }


        /* --- private Methoden --- */

  private boolean ensureSize()
  {
    boolean status = true;
    if( this.size >= this.sampleValues.length ) {
      try {

        // neue Array-Groesse bestimmen
        int stepSize = this.sampleValues.length / 2;
        if( stepSize < 0x100 ) {
          stepSize = 0x100;
        }
        else if( stepSize > 0x100000 ) {
          stepSize = 0x100000;
        }

        // Werte-Array kopieren
        byte[] buf = new byte[ this.sampleValues.length + stepSize ];
        System.arraycopy(
                this.sampleValues, 0, buf, 0, this.sampleValues.length );
        this.sampleValues = buf;

        // Anzahl-Array kopieren
        buf = new byte[ this.sampleCounts.length + stepSize ];
        System.arraycopy(
                this.sampleCounts, 0, buf, 0, this.sampleCounts.length );
        this.sampleCounts = buf;
      }
      catch( OutOfMemoryError ex ) {
        status              = false;
        this.sampleValues   = null;
        this.sampleCounts   = null;
        this.totalSampleCnt = 0;
        this.errorText      = "Kein Speicher mehr f\u00FCr die Aufzeichnung\n"
                                        + "der Audio-Daten verf\u00FCgbar.";
        System.gc();
      }
    }
    return status;
  }


  private void writeLastValueAgain()
  {
    final int  i = this.size - 1;
    final byte n = this.sampleCounts[ i ];
    if( n == Byte.MAX_VALUE ) {
      if( ensureSize() ) {
        this.sampleValues[ this.size ] = this.sampleValues[ i ];
        this.sampleCounts[ this.size ] = 1;
        this.size++;
      }
    } else {
      this.sampleCounts[ i ]++;
    }
  }
}

