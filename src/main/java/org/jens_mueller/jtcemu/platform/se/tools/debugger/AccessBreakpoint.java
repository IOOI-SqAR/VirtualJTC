/*
 * (c) 2019-2020 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Basisklasse fuer einen Haltepunkt mit Zugriffsmodus
 *
 * Von den beiden Attributes read und write muss
 * minestens eins immer true sein.
 */

package org.jens_mueller.jtcemu.platform.se.tools.debugger;


public abstract class AccessBreakpoint extends AbstractBreakpoint
{
  private boolean read;
  private boolean write;


  protected AccessBreakpoint( boolean read, boolean write )
  {
    this.read  = true;
    this.write = true;
    setAccess( read, write );
  }


  protected void appendAccessTextTo( StringBuilder buf )
  {
    if( isEnabled() && (this.read || this.write) ) {
      if( buf.length() > 0 ) {
	buf.append( ':' );
      }
      if( this.read ) {
	buf.append( 'R' );
      }
      if( this.write ) {
	buf.append( 'W' );
      }
    }
  }


  protected boolean isRead()
  {
    return isEnabled() && this.read;
  }


  protected boolean isWrite()
  {
    return isEnabled() && this.write;
  }


  protected void setAccess( boolean read, boolean write )
  {
    if( read || write ) {
      this.read  = read;
      this.write = write;
      setEnabled( true );
    } else {
      setEnabled( false );
    }
  }
}
