/*
 * (c) 2007-2020 Jens Mueller
 *
 * Z8 Emulator
 *
 * Schnittstelle fuer einen Haltepunkt
 */

package org.sqar.virtualjtc.z8;


public interface Z8Breakpoint {
    boolean matches(Z8 z8);
}
