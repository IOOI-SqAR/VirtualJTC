/*
 * (c) 2019-2020 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Haltepunkt auf eine Programmcodeadresse
 */

package org.jens_mueller.jtcemu.platform.se.tools.debugger;

import org.jens_mueller.z8.Z8;


public class PCBreakpoint extends AbstractBreakpoint
{
  private int addr;


  public PCBreakpoint( int addr )
  {
    setAddr( addr );
  }


  public int getAddr()
  {
    return this.addr;
  }


  public void setAddr( int addr )
  {
    this.addr = addr;
    setText( String.format( "%04X", addr ) );
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public int compareTo( AbstractBreakpoint bp )
  {
    int rv = -1;
    if( bp != null ) {
      if( bp instanceof PCBreakpoint ) {
	rv = this.addr - ((PCBreakpoint) bp).getAddr();
      } else {
	rv = super.compareTo( bp );
      }
    }
    return rv;
  }


  @Override
  public boolean equals( Object o )
  {
    boolean rv = false;
    if( o != null ) {
      if( o instanceof PCBreakpoint ) {
	if( ((PCBreakpoint) o).getAddr() == this.addr ) {
	  rv = true;
	}
      }
    }
    return rv;
  }


  @Override
  public boolean matches( Z8 z8 )
  {
    return (this.addr == z8.getPC());
  }
}
