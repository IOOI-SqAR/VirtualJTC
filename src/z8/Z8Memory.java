/*
 * (c) 2007 Jens Mueller
 *
 * Z8 Emulator
 *
 * Interface fuer Arbeitsspeicher
 */

package z8;

import java.lang.*;


public interface Z8Memory
{
  public void    initRAM();
  public int     getMemByte( int addr, boolean dataMemory );
  public boolean setMemByte( int addr, boolean dataMemory, int value );
}

