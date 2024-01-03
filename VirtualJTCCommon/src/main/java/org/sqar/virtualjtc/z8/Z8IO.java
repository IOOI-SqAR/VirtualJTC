/*
 * (c) 2007 Jens Mueller
 *
 * Z8 Emulator
 *
 * Interface fuer Arbeitsspeicher
 */

package org.sqar.virtualjtc.z8;


public interface Z8IO {
    int getPortValue(int port);

    void setPortValue(int port, int value);
}

