/*
 * (c) 2007 Jens Mueller
 *
 * Z8 Emulator
 *
 * Interface fuer Arbeitsspeicher
 */

package org.sqar.virtualjtc.z8;


public interface Z8Memory {
    void initRAM();

    int getMemByte(int addr, boolean dataMemory);

    boolean setMemByte(int addr, boolean dataMemory, int value);
}

