/*
 * (c) 2019-2021 Jens Mueller
 *
 * Reassemblierte Z8-Instruktion
 */

package z8;


public class Z8ReassInst
{
  private int      addr;
  private int      destAddr;
  private int      codeLen;
  private byte[]   codeBytes;
  private String   mnemonic;
  private String[] args;
  private boolean  unknown;


  public Z8ReassInst( int addr )
  {
    this.addr      = addr;
    this.destAddr  = -1;
    this.codeBytes = new byte[ 3 ];
    this.codeLen   = 0;
    this.mnemonic  = null;
    this.args      = null;
    this.unknown   = false;
  }


  public void addCodeByte( byte b )
  {
    if( this.codeLen >= this.codeBytes.length ) {
      throw new IllegalStateException(
			"Zu viele Code-Bytes f\u00FCr Z8-Instruktion" );
    }
    this.codeBytes[ this.codeLen++ ] = b;
  }


  public void appendTo( StringBuilder buf )
  {
    int begPos = buf.length();
    buf.append( String.format( "%04X   ", this.addr ) );
    int instCol = 19;
    for( int i = 0;
	 (i < this.codeLen) && (i < this.codeBytes.length);
	 i++ )
    {
      if( i > 0 ) {
	buf.append( '\u0020' );
      }
      buf.append( String.format(
			"%02X",
			(int) this.codeBytes[ i ] & 0xFF ) );
    }
    int n = begPos + instCol - buf.length();
    if( n < 1 ) {
      n = 1;
    }
    for( int i = 0; i < n; i++ ) {
      buf.append( '\u0020' );
    }
    begPos = buf.length();
    buf.append( this.mnemonic );
    if( this.args != null ) {
      for( int i = 0; i < this.args.length; i++ ) {
	String arg = this.args[ i ];
	if( arg != null ) {
	  if( begPos >= 0 ) {
	    n = begPos + 8 - buf.length();
	    if( n < 1 ) {
	      n = 1;
	    }
	    for( int k = 0; k < n; k++ ) {
	      buf.append( '\u0020' );
	    }
	    begPos = -1;
	  } else {
	    buf.append( ", " );
	  }
	  buf.append( arg );
	}
      }
    }
    if( this.unknown ) {
      buf.append( "\t\t;???" );
    }
  }


  public int getAddr()
  {
    return this.addr;
  }


  public String[] getArgs()
  {
    return this.args;
  }


  public int getDestAddr()
  {
    return this.destAddr;
  }


  public String getMnemonic()
  {
    return this.mnemonic;
  }


  public boolean isUnknown()
  {
    return this.unknown;
  }


  public void setDestAddr( int addr )
  {
    this.destAddr = addr;
  }


  public void setMnemonic( String mnemonic, String... args )
  {
    this.mnemonic = mnemonic;
    this.args     = args;
  }


  public void setUnknown()
  {
    this.unknown = true;
  }
}
