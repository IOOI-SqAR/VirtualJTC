/*
 * (c) 2020 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Schnittstelle zur Ausgabe von Text
 */

package org.sqar.virtualjtc.jtcemu.tools;


public interface TextOutput {
    void print(String text);

    void println();
}
