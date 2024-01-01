/*
 * (c) 2020 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Schnittstelle fuer den ausgangsseitigen Anschluss
 * des Kassettenrecorders und des Lautsprechers
 */

package org.jens_mueller.jtcemu.base;


public interface AudioWriter
{
  /*
   * Die Methode wird im CPU-Emulations-Thread aufgerufen
   * und besagt, dass am entsprechenden Ausgabetor ein Wert anliegt.
   */
  public void writePhase( boolean phase );
}
