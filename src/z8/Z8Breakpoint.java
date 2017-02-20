/*
 * (c) 2007-2010 Jens Mueller
 *
 * Z8 Emulator
 *
 * Haltepunkt
 */

package z8;

import java.lang.*;


public class Z8Breakpoint implements Comparable<Z8Breakpoint>
{
  private int     addr;
  private boolean enabled;
  private String  text;


  public Z8Breakpoint( int addr )
  {
    this.addr    = addr;
    this.enabled = true;
    this.text    = null;
  }


  public int getAddress()
  {
    return this.addr;
  }


  public boolean isEnabled()
  {
    return this.enabled;
  }


  public void setEnabled( boolean state )
  {
    this.enabled = state;
    this.text    = null;
  }


	/* --- Comparable --- */

  @Override
  public int compareTo( Z8Breakpoint bp )
  {
    return bp != null ? (this.addr - bp.addr) : -1;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public String toString()
  {
    if( this.text == null ) {
      if( this.enabled ) {
	this.text = String.format( "%04X", this.addr );
      } else {
	this.text = String.format( "( %04X )", this.addr );
      }
    }
    return text;
  }
}

