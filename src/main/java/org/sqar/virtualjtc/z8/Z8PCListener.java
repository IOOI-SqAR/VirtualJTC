/*
 * (c) 2007 Jens Mueller
 *
 * Z8 Emulator
 *
 * Interface fuer Listener des Program Counters (PC)
 */

package org.sqar.virtualjtc.z8;


public interface Z8PCListener
{
  public static final int ALL_ADDRESSES = -1;
  
  public void z8PCUpdate( Z8 z8, int pc );
}

