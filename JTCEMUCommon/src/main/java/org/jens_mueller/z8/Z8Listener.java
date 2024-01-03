/*
 * (c) 2020 Jens Mueller
 *
 * Z8 Emulator
 *
 * Interface fuer einen Listener
 */

package org.jens_mueller.z8;


public interface Z8Listener {
    enum Reason {
        POWER_ON,
        RESET,
        PRE_INST_EXEC,
        CYCLES_PER_SECOND_CHANGED,
        STATUS_CHANGED
    }

    ;

    void z8Update(Z8 z8, Reason reason);
}
