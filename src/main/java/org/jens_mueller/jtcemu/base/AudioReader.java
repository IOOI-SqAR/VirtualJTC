/*
 * (c) 2020 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Schnittstelle fuer den eingangsseitigen Anschluss
 * des Kassettenrecorders
 */

package org.jens_mueller.jtcemu.base;


public interface AudioReader
{
  /*
   * Die Methode wird im CPU-Emulations-Thread aufgerufen
   * und liest die Phase des Toneingangs.
   */
  public boolean readPhase();

  /*
   * Die Methode wird im CPU-Emulations-Thread aufgerufen
   * und gibt den Status zurueck, ob seit dem letzten Aufruf
   * eine Schwingung mit ueberwiegend einer hohen Amplitude anlag.
   */
  public boolean readVolumeStatus();
}
