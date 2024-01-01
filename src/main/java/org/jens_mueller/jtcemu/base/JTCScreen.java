/*
 * (c) 2014-2019 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Schnittstelle zur Ausgabe von Meldungen des Parsers
 */

package org.jens_mueller.jtcemu.base;


public interface JTCScreen
{
  public void screenConfigChanged();
  public void setScreenDirty();
}
