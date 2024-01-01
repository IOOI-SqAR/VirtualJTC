/*
 * (c) 2020 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Iterator fuer Kommandozeilenargumente
 */

package org.jens_mueller.jtcemu.platform.se.tools.assembler;


public class CmdLineIterator
{
  private String[] args;
  private int      argIdx;


  public CmdLineIterator( String[] args, int argIdx )
  {
    this.args   = args;
    this.argIdx = argIdx;
  }


  public String next()
  {
    String rv = null;
    while( this.argIdx < this.args.length ) {
      String arg = this.args[ this.argIdx++ ];
      if( arg != null ) {
	if( !arg.isEmpty() ) {
	  rv = arg;
	  break;
	}
      }
    }
    return rv;
  }
}
