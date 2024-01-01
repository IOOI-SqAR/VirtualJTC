/*
 * (c) 2007-2009 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Basisklasse fuer die Emulation
 * des Anschlusses des Magnettonbandgeraetes (Ausgang)
 *
 * Die Ausgabe erfolgt als Rechteckkurve
 */

package org.sqar.virtualjtc.jtcemu.audio;

import org.sqar.virtualjtc.z8.Z8;
import org.sqar.virtualjtc.jtcemu.base.JTCSys;


public abstract class AudioOut extends AudioIO
{
  public static final byte PHASE0_VALUE   = (byte) -100;
  public static final byte PHASE1_VALUE   = (byte) 100;
  public static final byte NO_PHASE_VALUE = (byte) 0;

  protected JTCSys  jtcSys;
  protected boolean enabled;
  protected int     lastPhaseSamples;
  protected int     maxPauseCycles;
  protected int     sampleRate;

  private boolean firstPhaseChange;


  protected AudioOut(Z8 z8, JTCSys jtcSys )
  {
    super( z8 );
    this.jtcSys           = jtcSys;
    this.enabled          = false;
    this.firstPhaseChange = false;
    this.lastPhaseSamples = 0;
    this.maxPauseCycles   = 0;
    this.sampleRate       = 0;
  }


  /*
   * Die Methode wird im CPU-Emulations-Thread aufgerufen
   * und besagt, dass am entsprechenden Ausgabetor ein Wert anliegt.
   */
  public void writePhase( boolean phase )
  {
    if( this.enabled && (this.cyclesPerFrame > 0) ) {
      if( this.firstCall ) {
        this.firstCall        = false;
        this.firstPhaseChange = true;
        this.lastPhase        = phase;
      } else {

        if( phase != this.lastPhase ) {
          this.lastPhase = phase;
          if( this.firstPhaseChange ) {
            this.firstPhaseChange = false;
            this.lastCycles       = this.z8.getTotalCycles();
          } else {
            long totalCycles = this.z8.getTotalCycles();
            int  diffCycles  = this.z8.calcDiffCycles(
                                                this.lastCycles,
                                                totalCycles );

            if( diffCycles > 0 ) {
              currentCycles( totalCycles, diffCycles );
              if( totalCycles > this.lastCycles ) {

                // Anzahl der zu erzeugenden Samples
                int nSamples = diffCycles / this.cyclesPerFrame;
                if( diffCycles < this.maxPauseCycles ) {

                  /*
                   * Wenn der Abstand seit dem letzten Phasenwechsel zu gross
                   * ist, soll die zwischenzeitliche Amplitude Null sein.
                   * damit die Phasenlage nicht einseitig ist.
                   * Als zu grosser Abstand wird mehr als die 6-fache Dauer
                   * der letzten Phasenlage angenommen.
                   * Der Faktor muss kleiner 8 sein,
                   * damit beim 2K-Aufzeichnungsverfahren
                   * (Amplitudenmodulation, 8 Schwingungen pro Bit)
                   * bei einem 0-Bit die Amplitude auch Null ist.
                   * Auf der anderen Seite muss der Faktor so gross sein,
                   * dass beim 4K- und 6K-Aufzeichnungsverfahren
                   * (Frequenzmodulation) die Phasenlage bei jedem
                   * Frequenzsprung auch sicher geaendert und
                   * die Amplitude nicht Null wird.
                   */
                  if( (this.lastPhaseSamples > 0)
                      && (nSamples > 6 * this.lastPhaseSamples) )
                  {
                    writeSamples( nSamples, NO_PHASE_VALUE );
                  } else {
                    writeSamples(
                                nSamples,
                                phase ? PHASE1_VALUE : PHASE0_VALUE );
                  }
                }

                /*
                 * Anzahl der verstrichenen Taktzyklen auf den Wert
                 * des letzten ausgegebenen Samples korrigieren
                 */
                this.lastCycles += ((long) nSamples * this.cyclesPerFrame);
                this.lastPhaseSamples = nSamples;
              }
            }
          }
        }
      }
    }
  }


  protected abstract void writeSamples( int nSamples, byte value );
}
