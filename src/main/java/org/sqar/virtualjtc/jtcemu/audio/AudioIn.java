/*
 * (c) 2007-2010 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Basisklasse fuer die Emulation
 * des Anschlusses des Magnettonbandgeraetes (Eingang)
 */

package org.sqar.virtualjtc.jtcemu.audio;

import javax.sound.sampled.*;
import org.sqar.virtualjtc.z8.Z8;


public abstract class AudioIn extends AudioIO
{
  protected int   minValue;
  protected int   maxValue;
  protected float thresholdValue;

  private volatile int selectedChannel;
  private int          sampleBitMask;
  private int          sampleSignMask;
  private int          sampleSizeInBytes;
  private int          channelCount;
  private int          nThresholdSamples;
  private int          thresholdCounter;
  private int          adjustPeriodLen;
  private int          adjustPeriodCnt;
  private boolean      volumeStatus;
  private boolean      bigEndian;
  private boolean      dataSigned;


  protected AudioIn( Z8 z8 )
  {
    super( z8 );
    this.thresholdValue    = 0F;
    this.minValue          = 0;
    this.maxValue          = 0;
    this.selectedChannel   = 0;
    this.sampleBitMask     = 0;
    this.sampleSignMask    = 0;
    this.sampleSizeInBytes = 0;
    this.channelCount      = 0;
    this.nThresholdSamples = 0;
    this.thresholdCounter  = 0;
    this.adjustPeriodLen   = 0;
    this.adjustPeriodCnt   = 0;
    this.volumeStatus      = false;
    this.bigEndian         = false;
    this.dataSigned        = false;
  }


  protected abstract byte[] readFrame();


  /*
   * Die Methode wird im CPU-Emulations-Thread aufgerufen
   * und liest die Phase des Toneingangs.
   */
  public boolean readPhase()
  {
    int v = readSamples();
    if( v != -1 ) {
      int d = this.maxValue - this.minValue;
      if( this.lastPhase ) {
        if( v < this.minValue + (d / 3) ) {
          this.lastPhase = false;
        }
      } else {
        if( v > this.maxValue - (d / 3) ) {
          this.lastPhase = true;
        }
      }
    }
    return this.lastPhase;
  }


  /*
   * Die Methode wird im CPU-Emulations-Thread aufgerufen
   * und gibt den Status zurueck, ob seit dem letzten Aufruf
   * eine Schwingung mit ueberwiegend einer hohen Amplitude anlag.
   */
  public boolean readVolumeStatus()
  {
    readSamples();
    return this.volumeStatus;
  }


  protected void setAudioFormat( AudioFormat fmt )
  {
    if( fmt != null ) {
      int sampleSizeInBits = fmt.getSampleSizeInBits();

      this.sampleBitMask = 0;
      for( int i = 0; i < sampleSizeInBits; i++ ) {
        this.sampleBitMask = (this.sampleBitMask << 1) | 1;
      }
      this.sampleSizeInBytes = (sampleSizeInBits + 7) / 8;
      this.channelCount      = fmt.getChannels();
      this.bigEndian         = fmt.isBigEndian();
      this.dataSigned        = fmt.getEncoding().equals(
                                        AudioFormat.Encoding.PCM_SIGNED );

      this.sampleSignMask = 0;
      if( this.dataSigned ) {
        this.sampleSignMask = 1;
        for( int i = 1; i < sampleSizeInBits; i++ ) {
          this.sampleSignMask <<= 1;
        }
      }

      /*
       * Min-/Max-Regelung initialisieren
       *
       * Nach einer Periodenlaenge werden die Minimum- und Maximum-Werte
       * zueinander um einen Schritt angenaehert,
       * um so einen dynamischen Mittelwert errechnen zu koennen.
       */
      float frameRate = fmt.getFrameRate();
      this.adjustPeriodLen = (int) frameRate / 256;
      if( this.adjustPeriodLen < 1 ) {
        this.adjustPeriodLen = 1;
      }
      this.adjustPeriodCnt  = this.adjustPeriodLen;
      this.firstCall        = true;

      /*
       * Anzahl Samples unter dem Schwellwert,
       * die zu einem H/L-Wechsel fuehren
       *
       * Der Wert sollte in etwa der Laenge einer Schwingung
       * der Traegerfrequenz entsprechen.
       */
      this.nThresholdSamples = (int) (frameRate / 4410F);

      /*
       * Wenn die SIO bereits vor den Aktivieren der Audio-Funktion
       * auf das Start-Bit lauscht, muss es vor der Verarbeitung
       * der ersten Audio-Daten 1-Pegel lesen.
       */
      this.volumeStatus = true;
    }
  }


  public void setSelectedChannel( int channel )
  {
    this.selectedChannel = channel;
  }


        /* --- private Methoden --- */

  private int readSample()
  {
    int    value     = -1;
    byte[] frameData = readFrame();
    if( frameData != null ) {
      int offset = this.selectedChannel * this.sampleSizeInBytes;
      if( offset + this.sampleSizeInBytes <= frameData.length ) {
        value = 0;
        if( this.bigEndian ) {
          for( int i = 0; i < this.sampleSizeInBytes; i++ ) {
            value = (value << 8) | ((int) frameData[ offset + i ] & 0xFF);
          }
        } else {
          for( int i = this.sampleSizeInBytes - 1; i >= 0; --i ) {
            value = (value << 8) | ((int) frameData[ offset + i ] & 0xFF);
          }
        }
        value &= this.sampleBitMask;
      }
    }
    return value;
  }


  /*
   * Die Methode liest die Samples seit dem letzten Aufruf
   * und gibt den Wert des letzten Samples zurueck.
   *
   * Rueckgabewert:
   *  -1: kein Wert gelesen
   */
  private int readSamples()
  {
    int rv = -1;
    if( this.cyclesPerFrame > 0 ) {
      if( this.firstCall ) {
        this.firstCall  = false;
        this.lastCycles = this.z8.getTotalCycles();

      } else {

        long totalCycles = this.z8.getTotalCycles();
        int  diffCycles  = this.z8.calcDiffCycles(
                                                this.lastCycles,
                                                totalCycles );

        if( diffCycles > 0 ) {
          currentCycles( totalCycles, diffCycles );
          if( totalCycles > this.lastCycles ) {

            // bis zum naechsten auszuwertenden Samples lesen
            int nSamples = diffCycles / this.cyclesPerFrame;
            if( nSamples > 0 ) {
              int v = 0;
              int i = nSamples;
              do {
                v = readSample();
                if( v != -1 ) {

                  // dynamische Mittelwertbestimmung
                  if( this.adjustPeriodCnt > 0 ) {
                    --this.adjustPeriodCnt;
                  } else {
                    this.adjustPeriodCnt = this.adjustPeriodLen;
                    if( this.minValue < this.maxValue ) {
                      this.minValue++;
                    }
                    if( this.maxValue > this.minValue ) {
                      --this.maxValue;
                    }
                  }

                  // Wenn gelesender Wert negativ ist, Zahl korrigieren
                  if( this.dataSigned && ((v & this.sampleSignMask) != 0) ) {
                    v |= ~this.sampleBitMask;
                  }

                  // Minimum-/Maximum-Werte und Mittelwert aktualisieren
                  if( v < this.minValue ) {
                    this.minValue = v;
                  }
                  else if( v > this.maxValue ) {
                    this.maxValue = v;
                  }

                  // aktueller Low/High-Status ermitteln
                  int m = (this.maxValue - this.minValue) / 2;
                  int t = Math.round( (float) m * this.thresholdValue );
                  int d = Math.abs( v - this.minValue - m );
                  if( d > t ) {
                    this.volumeStatus = true;
                    this.thresholdCounter = 0;
                  } else {
                    if( this.thresholdCounter < this.nThresholdSamples ) {
                      this.thresholdCounter++;
                    } else {
                      this.volumeStatus     = false;
                      this.thresholdCounter = 0;
                    }
                  }
                  rv = v;
                }
              } while( --i > 0 );

              /*
               * Anzahl der verstrichenen Taktzyklen auf den Wert
               * des letzten gelesenen Samples korrigieren
               */
              this.lastCycles += (nSamples * this.cyclesPerFrame);
            }
          }
        }
      }
    }
    return rv;
  }
}
