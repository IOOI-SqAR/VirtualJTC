/*
 * (c) 2007-2019 Jens Mueller
 *
 * Z8 Emulator
 *
 * Interface fuer Arbeitsspeicher
 */

package org.jens_mueller.z8;


public interface Z8Memory
{
  public int     getMemByte( int addr, boolean dataMemory );
  public boolean setMemByte( int addr, boolean dataMemory, int value );
}
