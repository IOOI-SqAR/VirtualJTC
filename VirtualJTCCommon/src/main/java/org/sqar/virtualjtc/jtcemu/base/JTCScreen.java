/*
 * (c) 2014-2019 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Schnittstelle zur Ausgabe von Meldungen des Parsers
 */

package org.sqar.virtualjtc.jtcemu.base;


public interface JTCScreen {
    void screenConfigChanged();

    void setScreenDirty();
}
