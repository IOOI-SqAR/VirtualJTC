/*
 * (c) 2019 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Basisklasse fuer einen Haltepunkt
 */

package org.sqar.virtualjtc.jtcemu.tools.debugger;

import org.sqar.virtualjtc.z8.Z8Breakpoint;


public abstract class AbstractBreakpoint
			implements
				Comparable<AbstractBreakpoint>,
				Z8Breakpoint
{
  private boolean enabled;
  private String  text;


  protected AbstractBreakpoint()
  {
    this.enabled = true;
  }


  public boolean isEnabled()
  {
    return this.enabled;
  }


  public void setEnabled( boolean state )
  {
    this.enabled = state;
  }


  protected void setText( String text )
  {
    this.text = text;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public String toString()
  {
    return this.text != null ? this.text : super.toString();
  }


	/* --- Compareable --- */

  @Override
  public int compareTo( AbstractBreakpoint breakpoint )
  {
    return toString().compareTo( breakpoint.toString() );
  }
}
