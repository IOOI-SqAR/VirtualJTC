/*
 * (c) 2014-2021 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Schnittstelle zur Ausgabe von Meldungen des Parsers
 */

package jtcemu.tools;

import z8.Z8Memory;


public class BasicUtil
{
  /*
   * Die Methode liest aus dem Arbeitsspeicher ein Tiny-MP-BASIC-Programm
   * und liefert es in textueller Form zurueck.
   * Wenn sich an der angegebenen Adresse kein BASIC-Programm befindert,
   * wird null zurueckgeliefert.
   */
  public static String getBasicProgramTextFromMemory(
					Z8Memory memory,
					int      addr )
  {
    String rv = null;
    if( memory != null ) {
      int a = addr;
      int b = memory.getMemByte( a++, false );
      if( ((b & 0x80) != 0)
	  && ((b != 0xFF) || (memory.getMemByte( a, false ) != 0xFF)) )
      {
	StringBuilder buf = new StringBuilder( 0x4000 );
	while( (a < 0xFFFF) && (b != 0) ) {
	  int lineNum = ((b << 8) & 0x7F00)
				| memory.getMemByte( a++, false );
	  if( lineNum == 0 ) {
	    break;
	  }
	  buf.append( lineNum );
	  b = memory.getMemByte( a++, false );
	  while( (b != 0) && (b != 0x0D) ) {
	    buf.append( '\u0020' );
	    boolean instIF   = false;
	    boolean instTRAP = false;
	    switch( b ) {
	      case '/':
		buf.append( "TOFF" );
		break;

	      case '!':
		buf.append( "TRAP" );
		instTRAP = true;
		break;

	      case '>':
		buf.append( "ELSE" );
		break;

	      case 'C':
		buf.append( "CALL" );
		break;

	      case 'E':
		buf.append( "END" );
		break;

	      case 'F':
		buf.append( "IF" );
		instIF = true;
		break;

	      case 'G':
		buf.append( "GOTO" );
		break;

	      case 'H':
		buf.append( "PTH" );
		break;

	      case 'I':
		buf.append( "INPUT" );
		break;

	      case 'L':
		buf.append( "LET" );
		break;

	      case 'M':
		a = processREM( memory, a, buf );
		break;

	      case 'O':
		buf.append( "PROC" );
		break;

	      case 'P':
		buf.append( "PRINT" );
		break;

	      case 'R':
		buf.append( "RETURN" );
		break;

	      case 'S':
		buf.append( "GOSUB" );
		break;

	      case 'T':
		buf.append( "STOP" );
		break;

	      case 'W':
		buf.append( "WAIT" );
		break;

	      default:
		buf.append( (char) b );
	    }
	    b = memory.getMemByte( a++, false );
	    if( (b != 0) && (b != 0x0D) && (b != ';') ) {
	      buf.append( '\u0020' );
	      do {
		if( instTRAP && (b == ',') ) {
		  buf.append( " TO " );
		  instTRAP = false;
		} else {
		  buf.append( toChar( b ) );
		}
		b = memory.getMemByte( a++, false );
	      } while( (b != 0x0D) && (b != ';') );
	    }
	    if( b == ';' ) {
	      if( instIF ) {
		buf.append( " THEN" );
	      } else {
		buf.append( toChar( b ) );
	      }
	      b = memory.getMemByte( a++, false );
	    }
	  }
	  buf.append( '\n' );
	  b = memory.getMemByte( a++, false );
	}
	rv = buf.toString();
      }
    }
    return rv;
  }


	/* --- private Methoden --- */

  private static boolean isEndOfRemAt( Z8Memory memory, int addr )
  {
    int b = memory.getMemByte( addr, false );
    int c = memory.getMemByte( addr + 1, false );
    return (b == ';') || ((b == 0x0D) && ((c == 0) || ((c & 0x80) != 0)));
  }


  private static int processREM(
				Z8Memory      memory,
				int           addr,
				StringBuilder buf )
  {
    int oldLen = buf.length();
    buf.append( "REM" );
    int a = addr;
    if( !isEndOfRemAt( memory, a ) ) {
      if( memory.getMemByte( a, false ) != 0x20 ) {
	buf.append( '\u0020' );
      }
      boolean hasBin = false;
      while( !isEndOfRemAt( memory, a ) ) {
	int b = memory.getMemByte( a++, false );
	if( (b < 0x1A) || (b > 0x7F) ) {
	  hasBin = true;
	  break;
	}
	buf.append( toChar( b ) );
      }
      if( hasBin ) {
	buf.setLength( oldLen );
	buf.append( "BREM " );
	a = addr;
	int n = 0;
	while( !isEndOfRemAt( memory, a ) ) {
	  if( n >= 16 ) {
	    buf.append( "\nBREM " );
	    n = 0;
	  }
	  if( n > 0 ) {
	    buf.append( ',' );
	  }
	  buf.append(
		String.format(
			"%%%02X",
			memory.getMemByte( a++, false ) ) );
	  n++;
	}
      }
    }
    return a;
  }


  private static char toChar( int b )
  {
    char ch = (char) b;
    switch( b ) {
      case 0x1A:
	ch = '\u00E4';
	break;
      case 0x1B:
	ch = '\u00F6';
	break;
      case 0x1C:
	ch = '\u00FC';
	break;
      case 0x1D:
	ch = '\u00C4';
	break;
      case 0x1E:
	ch = '\u00D6';
	break;
      case 0x1F:
	ch = '\u00DC';
	break;
      case 0x7F:
	ch = '\u00DF';
	break;
    }
    return ch;
  }


	/* --- Konstruktor --- */

  private BasicUtil()
  {
    // nicht instanziierbar
  }
}
