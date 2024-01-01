/*
 * (c) 2007-2010 Jens Mueller
 *
 * Z8 Emulator
 *
 * Interface fuer Debugger
 */

package org.jens_mueller.z8;


import org.sqar.virtualjtc.z8.Z8;

public interface Z8Debugger
{
  public void z8DebugStatusChanged( Z8 z8 );
}

