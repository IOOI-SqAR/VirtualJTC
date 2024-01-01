/*
 * (c) 2007-2021 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * BASIC-Parser
 */

package jtcemu.tools;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.CharacterIterator;
import java.text.ParseException;
import java.text.StringCharacterIterator;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import jtcemu.base.JTCSys;
import jtcemu.tools.assembler.AsmOptions;
import jtcemu.tools.assembler.Z8Assembler;


public class BasicParser
{
  public static class CodeBuf extends ByteArrayOutputStream
  {
    public CodeBuf( int size )
    {
      super( size );
    }

    public void cut( int len )
    {
      if( this.count > len )
	this.count = len;
    }


    public boolean endsWithSemicolon()
    {
      boolean rv = false;
      if( (this.buf != null) && (this.count > 0) ) {
	if( this.buf[ this.count - 1 ] == ';' ) {
	  rv = true;
	}
      }
      return rv;
   }
  };


  private JTCSys                   jtcSys;
  private CharacterIterator        iter;
  private TextOutput               errOut;
  private CodeBuf                  codeBuf;
  private String                   srcText;
  private int                      srcLen;
  private int                      addr;
  private int                      errorCnt;
  private int                      warningCnt;
  private int                      lineOffset;
  private int                      lineNum;
  private int                      curBasicLineNum;
  private int                      lastBasicLineNum;
  private int                      lastBRemEnd;
  private boolean                  allow00or0D;
  private boolean                  upperCharsOnly;
  private boolean                  hasLines;
  private boolean                  lineHasIF;
  private boolean                  prevLineHasIF;
  private boolean                  firstInstInLine;
  private Map<String,List<String>> upperProc2Names;


  public static byte[] parse(
			JTCSys     jtcSys,
			int        addr,
			String     text,
			TextOutput errOut )
  {
    return (new BasicParser( jtcSys, addr, text, errOut )).parse();
  }


	/* --- Konstruktor --- */

  private BasicParser(
		JTCSys     jtcSys,
		int        addr,
		String     text,
		TextOutput errOut )
  {
    this.jtcSys          = jtcSys;
    this.addr            = addr;
    this.srcText         = (text != null ? text : "");
    this.srcLen          = this.srcText.length();
    this.allow00or0D     = (jtcSys.getOSType() == JTCSys.OSType.ES40
			   || jtcSys.getOSType() == JTCSys.OSType.ES23);
    this.upperCharsOnly  = (jtcSys.getOSType() != JTCSys.OSType.ES40);
    this.errOut          = errOut;
    this.iter            = new StringCharacterIterator( this.srcText );
    this.codeBuf         = new CodeBuf( this.srcLen + 16 );
    this.upperProc2Names = new HashMap<>();
  }


	/* --- private Methoden --- */

  private boolean checkToken( String token )
  {
    boolean rv     = true;
    char    ch     = Character.toUpperCase( this.iter.current() );
    int     begPos = this.iter.getIndex();
    int     len    = token.length();
    for( int i = 0; i < len; i++ ) {
      if( token.charAt( i ) != ch ) {
	this.iter.setIndex( begPos );
	rv = false;
	break;
      }
      ch = Character.toUpperCase( this.iter.next() );
    }
    return rv;
  }


  private static boolean isEndOfInst( char ch )
  {
    return (ch == CharacterIterator.DONE) || (ch == ';') || (ch == '\n');
  }


  private static boolean isDigit( char ch )
  {
    return (ch >= '0') && (ch <= '9');
  }


  private static boolean isLowerLetter( char ch )
  {
    return (ch >= 'a') && (ch <= 'z');
  }


  private static boolean isSpace( char ch )
  {
    return (ch == '\u0020') || (ch == '\u00A0');
  }


  private static boolean isUpperLetter( char ch )
  {
    return (ch >= 'A') && (ch <= 'Z');
  }


  private byte[] parse()
  {
    this.hasLines         = false;
    this.lineHasIF        = false;
    this.prevLineHasIF    = false;
    this.firstInstInLine  = false;
    this.errorCnt         = 0;
    this.warningCnt       = 0;
    this.lineOffset       = 0;
    this.lineNum          = 1;
    this.curBasicLineNum  = -1;
    this.lastBasicLineNum = -1;
    this.lastBRemEnd      = -1;

    try {
      char ch = this.iter.first();
      while( ch != CharacterIterator.DONE ) {
	parseLine();
	ch = skipSpaces();
      }
      if( this.hasLines ) {
	putCode( 0x0D );
      }
      if( this.codeBuf != null ) {
	if( this.codeBuf.size() > 0 ) {
	  this.codeBuf.write( 0 );
	}
      }
    }
    catch( TooManyErrorsException ex ) {
      this.errOut.println();
      this.errOut.print( "Abgebrochen augrund zu vieler Fehler" );
      this.errOut.println();
    }
    this.errOut.println();
    this.errOut.print( Integer.toString( this.errorCnt ) );
    this.errOut.print( " Fehler" );
    if( this.warningCnt == 1 ) {
      this.errOut.print( " und 1 Warnung" );
    } else if( this.warningCnt > 1 ) {
      this.errOut.print( " und " );
      this.errOut.print( Integer.toString( this.warningCnt ) );
      this.errOut.print( " Warnungen" );
    }
    this.errOut.println();
    return (this.errorCnt == 0 ? this.codeBuf.toByteArray() : null);
  }


  private void parseLine() throws TooManyErrorsException
  {
    this.lineOffset   = this.iter.getIndex();
    this.curBasicLineNum = -1;

    char ch = skipSpaces();
    if( (ch != CharacterIterator.DONE) && (ch != '\n') ) {
      try {

	// BASIC-Zeilennummer parsen
	int basicLineNum = readIntNumber();
	if( basicLineNum >= 0 ) {
	  boolean errDone      = false;
	  if( basicLineNum > 32767 ) {
	    putError( "Zeilennummer zu gro\u00DF", -1 );
	    errDone = true;
	  }
	  if( !errDone && !this.allow00or0D ) {
	    int lByte = basicLineNum & 0xFF;
	    if( (lByte == 0x00) || (lByte == 0x0D) ) {
	      putError(
		"Zeilennummer " + Integer.toString( basicLineNum )
			+ " beim 2K-Betriebssystem und beim EMR-ES1988"
			+ " nicht erlaubt, da ihre bin\u00E4re Form"
			+ " %00 oder %0D enth\u00E4lt",
		-1 );
	      errDone = true;
	    }
	  }
	  if( !errDone
	      && (this.lastBasicLineNum >= 0)
	      && (basicLineNum <= this.lastBasicLineNum) )
	  {
	    putError(
		"Zeilennummer muss gr\u00F6\u00DFer als die vorherige sein",
		-1 );
	    errDone = true;
	  }
	  if( !errDone ) {
	    if( this.hasLines ) {
	      putCode( 0x0D );
	    }
	    putCode( (basicLineNum >> 8) | 0x80 );
	    putCode( basicLineNum );
	    this.curBasicLineNum  = basicLineNum;
	    this.lastBasicLineNum = basicLineNum;
	    this.hasLines         = true;
	    this.firstInstInLine  = true;
	    this.prevLineHasIF    = this.lineHasIF;
	    this.lineHasIF        = false;
	  }
	} else {
	  if( this.hasLines ) {
	    if( !this.codeBuf.endsWithSemicolon() ) {
	      putCode( ';' );
	    }
	  } else {
	    throwParseException( "Mindestens die erste Zeile muss eine"
					+ " BASIC-Zeilennummer haben!" );
	  }
	}

	// Anweisungen parsen
	parseInst();
	this.firstInstInLine = false;

	// ggf. weiter Anweisungen parsen
	ch = skipSpaces();
	while( ch == ';' ) {
	  this.iter.next();
	  ch = skipSpaces();
	  if( (ch != CharacterIterator.DONE) && (ch != '\n') ) {
	    putCode( ';' );
	    parseInst();
	    ch = skipSpaces();
	  }
	}
	if( (ch != CharacterIterator.DONE) && (ch != '\n') ) {
	  throwUnexpectedChar( ch );
	}
      }
      catch( ParseException ex ) {
	putError( ex.getMessage(), ex.getErrorOffset() );
      }
    }

    // Zeilenende ueberspringen
    ch = this.iter.current();
    while( (ch != CharacterIterator.DONE) && (ch != '\n') ) {
      ch = this.iter.next();
    }
    if( ch == '\n' ) {
      this.iter.next();
      this.lineNum++;
    }
  }


  private void parseInst() throws ParseException, TooManyErrorsException
  {
    int ch = skipSpaces();
    if( ch == '?' ) {
      this.iter.next();
      parsePRINT( 'P' );
    } else if( ch == '!' ) {
      this.iter.next();
      parseREM();
    } else {
      int    begInst  = this.iter.getIndex();
      String instName = readKeyword();
      int    instLen  = instName.length();
      if( instLen == 1 ) {
	this.iter.setIndex( begInst );
	parseLET();
      } else if( instLen > 1 ) {
	if( instName.equals( "ASM" ) ) {
	  parseASM();
	}
	else if( instName.equals( "BREM" ) ) {
	  parseBREM();
	}
	else if( instName.equals( "CALL" ) ) {
	  putCode( 'C' );
	  parseExpr();
	}
	else if( instName.equals( "ELSE" ) ) {
	  parseELSE();
	}
	else if( instName.equals( "END" ) ) {
	  putCode( 'E' );
	}
	else if( instName.equals( "GOSUB" ) ) {
	  putCode( 'S' );
	  parseExpr();
	}
	else if( instName.equals( "GOTO" ) ) {
	  putCode( 'G' );
	  parseExpr();
	}
	else if( instName.equals( "IF" ) ) {
	  parseIF();
	}
	else if( instName.equals( "INPUT" ) ) {
	  parseINPUT();
	}
	else if( instName.equals( "LET" ) ) {
	  parseLET();
	}
	else if( instName.equals( "PRINTHEX" )
		 || instName.equals( "PTH" ) )
	{
	  parsePRINT( 'H' );
	}
	else if( instName.equals( "PRINT" ) ) {
	  parsePRINT( 'P' );
	}
	else if( instName.equals( "PROC" ) ) {
	  parsePROC();
	}
	else if( instName.equals( "PTC" ) ) {
	  putCode( 'O' );
	  putCode( instName );
	  parseArgList( 1 );
	}
	else if( instName.equals( "RETURN" ) ) {
	  putCode( 'R' );
	}
	else if( instName.equals( "REM" ) ) {
	  parseREM();
	}
	else if( instName.equals( "SETRR" ) ) {
	  putCode( 'O' );
	  parseSETRR();
	}
	else if( instName.equals( "SETR" )
		 || instName.equals( "SETEB" )
		 || instName.equals( "SETEW" ) )
	{
	  putCode( 'O' );
	  putCode( instName );
	  parseArgList( 2 );
	}
	else if( instName.equals( "STOP" ) ) {
	  putCode( 'T' );
	}
	else if( instName.equals( "TOFF" ) ) {
	  putCode( '/' );
	}
	else if( instName.equals( "TRAP" ) ) {
	  parseTRAP();
	}
	else if( instName.equals( "WAIT" ) ) {
	  putCode( 'W' );
	  parseExpr();
	} else {
	  throwParseException( instName + ": Unbekannte Anweisung" );
	}
      } else {
	throwParseException( "Anweisung erwartet" );
      }
    }
  }


  private void parseASM() throws ParseException, TooManyErrorsException
  {
    char ch = skipSpaces();
    if( ch != '\n' ) {
      throwUnexpectedChar( ch );
    }
    putCode( 'M' );					// REM
    StringBuilder buf = new StringBuilder( 0x1000 );
    buf.append( "\t.org\t%" );
    buf.append( String.format( "%04X", this.addr + this.codeBuf.size() ) );
    buf.append( '\n' );
    for(;;) {
      int eol = this.iter.getIndex();
      this.iter.next();
      ch = skipSpaces();
      this.iter.setIndex( eol );
      if( (ch == CharacterIterator.DONE) || isDigit( ch ) ) {
	break;
      }
      ch = this.iter.next();
      while( (ch != CharacterIterator.DONE) && (ch != '\n') ) {
	buf.append( ch );
	ch = this.iter.next();
      }
      buf.append( '\n' );
    }
    try {
      Z8Assembler asm = new Z8Assembler(
				buf.toString(),
				null,
				null,
				new AsmOptions(),
				this.errOut,
				true,
				null );
      byte[] mc = asm.assemble();
      if( mc != null ) {
	boolean nl = false;
	for( byte b : mc ) {
	  if( b == ';' ) {
	    throwParseException( "Im Assembler-Block wurde das Byte %3B"
			+ " (entspricht Semikolon) erzeugt,"
			+ " dass das Ende der Anweisung bedeutet"
			+ " und somit nicht erlaubt ist" );
	  }
	  if( !this.allow00or0D && ((b == 0) || (b == (byte) 0x0D)) ) {
	    throwParseException( "Im Assembler-Block wurde das Byte %00"
			+ " oder %0D erzeugt, dass beim 2K-Betriebssystem"
			+ " und beim EMR-ES1988 nicht erlaubt ist" );
	  }
	  if( nl && ((b & 0x80) != 0) ) {
	    throwParseException( "Im Assembler-Block wurde das Byte %0D"
			+ " gefolgt von einem Byte mit gesetztem Bit 7"
			+ " erzeugt,  was das Ende der Anweisung bedeutet"
			+ " und somit nicht erlaubt ist" );
	  }
	  nl = (b == (byte) 0x0D);
	}
	this.codeBuf.write( mc );
      }
      this.errorCnt += asm.getErrorCount();
      if( this.errorCnt >= 100 ) {
	throw new TooManyErrorsException();
      }
    }
    catch( IOException ex ) {
      throwParseException( "IO-Fehler: " + ex.getMessage() );
    }
  }


  private void parseBREM() throws ParseException
  {
    if( (this.codeBuf.size() == (this.lastBRemEnd + 1))
	&& this.codeBuf.endsWithSemicolon() )
    {
      this.codeBuf.cut( this.lastBRemEnd );
    } else {
      putCode( 'M' );				// REM
    }
    boolean nl = false;
    for( ;;) {
      int v = readNumber();
      if( v < 0 ) {
	throwParseException( "Zahl erwartet" );
      }
      if( v > 0x00FF ) {
	throwParseException( "Wert zu gro\u00DF" );
      }
      if( v == ';' ) {
	throwParseException( "Wert 59 bzw. %3B nicht erlaubt,"
			+ " da dies das Semikolon und somit"
			+ " Ende der Anweisung bedeutet" );
      }
      if( !this.allow00or0D && ((v == 0) || (v == 0x0D)) ) {
	throwParseException( "Werte 0 und 13 bzw. %0D beim"
			+ " 2K-Betriebssystem und beim EMR-ES1988"
			+ " nicht erlaubt" );
      }
      if( nl & ((v & 0x80) != 0) ) {
	throwParseException( "Wert 13 bzw. %0D gefolgt von einem Byte"
			+ " mit gesetztem Bit 7 nicht erlaubt,"
			+ " da dies das Ende der Anweisung bedeutet" );
      }
      putCode( v );
      char ch = skipSpaces();
      if( isEndOfInst( ch ) ) {
	break;
      }
      if( ch == ',' ) {
	ch = this.iter.next();
      }
      nl = (v == 0x0D);
    }
    this.lastBRemEnd = codeBuf.size();
  }


  private void parseELSE() throws ParseException
  {
    if( !this.firstInstInLine ) {
      throwParseException( "ELSE muss die erste Anweisung"
		+ " in der hinter IF folgenden Zeile sein"
		+ " (mit eigener BASIC-Zeilennummer)" );
    }
    if( !this.prevLineHasIF ) {
      throwParseException( "ELSE: vorherige Zeile enth\u00E4lt kein IF" );
    }
    putCode( '>' );
    if( skipSpaces() != ';' ) {
      throwParseException( "Hinter ELSE m\u00FCssen ein Semikolon und"
				+ " dahinter die Anweisungen folgen" );
    }
  }


  private void parseIF() throws ParseException, TooManyErrorsException
  {
    this.lineHasIF = true;
    putCode( 'F' );
    parseCondExpr();
    boolean done = false;
    char    ch   = skipSpaces();
    if( ch == ',' ) {
      putCode( ';' );
      this.iter.next();
    } else {
      int    pos  = this.iter.getIndex();
      String name = readKeyword();
      if( name.isEmpty() ) {
	throwUnexpectedChar( ch );
      }
      putCode( ';' );
      if( name.equals( "THEN" ) ) {
	ch = skipSpaces();
	if( isDigit( ch ) ) {
	  putCode( 'G' );
	  parseDecNumber();
	  done = true;
	}
      } else {
	this.iter.setIndex( pos );
      }
    }
    if( !done ) {
      parseInst();
    }
    ch = skipSpaces();
    while( ch == ';' ) {
      putCode( ';' );
      this.iter.next();
      parseInst();
      ch = skipSpaces();
    }
  }


  private void parseINPUT() throws ParseException
  {
    putCode( 'I' );
    for(;;) {
      parseOptStringLiteral();
      char ch = Character.toUpperCase( skipSpaces() );
      if( !isUpperLetter( ch ) ) {
	throwVarExpected();
      }
      putCode( ch );
      this.iter.next();
      if( skipSpaces() != ',' ) {
	break;
      }
      putCode( ',' );
      this.iter.next();
    }
  }


  private void parseLET() throws ParseException
  {
    putCode( 'L' );
    boolean loop = true;
    while( loop ) {
      char ch = Character.toUpperCase( skipSpaces() );
      if( !isUpperLetter( ch ) ) {
	throwVarExpected();
      }
      putCode( ch );
      this.iter.next();
      if( skipSpaces() != '=' ) {
	throwParseException( "\'=\' erwartet" );
      }
      putCode( '=' );
      this.iter.next();
      parseExpr();
      ch = skipSpaces();
      if( ch == ',' ) {
	putCode( ch );
        this.iter.next();
      } else {
	loop = false;
      }
    }
  }


  private void parsePRINT( char cmdChar ) throws ParseException
  {
    putCode( cmdChar );
    boolean loop = true;
    while( loop ) {
      parseOptStringLiteral();
      char ch = skipSpaces();
      if( !isEndOfInst( ch ) && (ch != ',') ) {
	parseExpr();
      }
      if( skipSpaces() == ',' ) {
	putCode( ',' );
	this.iter.next();
	ch = skipSpaces();
	if( isEndOfInst( ch ) ) {
	  loop = false;
	}
      } else {
	loop = false;
      }
    }
  }


  private void parsePROC() throws ParseException
  {
    putCode( 'O' );
    if( skipSpaces() == '[' ) {
      putCode( '[' );
      this.iter.next();
      boolean loop = true;
      while( loop ) {
	char ch = Character.toUpperCase( skipSpaces() );
	if( !isUpperLetter( ch ) ) {
	  throwVarExpected();
	}
	putCode( ch );
	this.iter.next();
	ch = skipSpaces();
	if( ch == ',' ) {
	  putCode( ch );
	  this.iter.next();
	} else {
	  loop = false;
	}
      }
      if( skipSpaces() != ']' ) {
	throwParseException( "\']\' erwartet" );
      }
      putCode( ']' );
      this.iter.next();
      if( skipSpaces() != '=' ) {
	throwParseException( "\'=\' erwartet" );
      }
      putCode( '=' );
      this.iter.next();
    }
    String procName  = readIdentifier();
    String upperName = procName.toUpperCase();
    if( upperName.equals( "ABS" )
	|| upperName.equals( "GETR" )
	|| upperName.equals( "GETRR" )
	|| upperName.equals( "GETEB" )
	|| upperName.equals( "GETEW" )
	|| upperName.equals( "NEG" )
	|| upperName.equals( "NOT" )
	|| upperName.equals( "PTC" ) )
    {
      putCode( upperName );
      parseArgList( 1 );
    } else if( upperName.equals( "SETRR" ) ) {
      parseSETRR();
    } else if( upperName.equals( "SETR" )
	     || upperName.equals( "SETEB" )
	     || upperName.equals( "SETEW" ) )
    {
      putCode( upperName );
      parseArgList( 2 );
    } else {
      putCode( toExtProcName( procName, "Unbekannte Prozedur" ) );
      parseOptArgList();
    }
  }


  private void parseREM() throws ParseException
  {
    putCode( 'M' );
    char ch = this.iter.current();
    while( isSpace( ch ) ) {
      ch = this.iter.next();
    }
    while( !isEndOfInst( ch ) ) {
      int b = -1;
      switch( ch ) {
	case '\u00E4':
	  b = 0x1A;
	  break;
	case '\u00F6':
	  b = 0x1B;
	  break;
	case '\u00FC':
	  b = 0x1C;
	  break;
	case '\u00C4':
	  b = 0x1D;
	  break;
	case '\u00D6':
	  b = 0x1E;
	  break;
	case '\u00DC':
	  b = 0x1F;
	  break;
	case '\u00DF':
	  b = 0x7F;
	  break;
	default:
	  if( (ch >= '\u0020') && (ch < '\u007F') ) {
	    b = ch;
	  }
      }
      if( b >= 0 ) {
	putCode( b );
      } else {
	putWarning(
		String.format(
			"Zeichen mit Code %%%04H ignoriert,"
				+ " ggf. BREM verwenden",
			(int) b ) );
      }
      ch = this.iter.next();
    }
  }


  private void parseSETRR() throws ParseException
  {
    putCode( "SETRR" );
    Integer[] args = parseArgList( 2 );
    if( args != null ) {
      if( args.length == 2 ) {
	if( args[ 0 ] != null ) {
	  if( args[ 0 ].intValue() == 8 ) {
	    // Register 8: Zeiger auf Prozedurtabelle
	    Set<String> procNames = new TreeSet<>();
	    this.upperProc2Names.clear();
	    if( args[ 1 ] != null ) {
	      int a = args[ 1 ].intValue();
	      int b = this.jtcSys.getMemByte( a++, false );
	      while( (b > 0) && (b <= 32)
		     && ((a + b + 2) < 0x10000) )
	      {
		boolean abort = false;
		int     len   = b;
		char[]  buf   = new char[ len ];
		for( int i = 0; i < len; i++ ) {
		  char c = (char) this.jtcSys.getMemByte( a++, false );
		  if( isUpperLetter( c )
		      || isLowerLetter( c )
		      || (c == '_') )
		  {
		    buf[ i ] = c;
		  } else if( (i > 0) && isDigit( c ) ) {
		    buf[ i ] = c;
		  } else {
		    abort = true;
		  }
		}
		if( abort ) {
		  break;
		}
		String procName    = String.valueOf( buf );
		String upperName   = procName.toUpperCase();
		List<String> names = this.upperProc2Names.get( upperName );
		if( names != null ) {
		  List<String> list = new ArrayList<>();
		  list.addAll( names );
		  list.add( procName );
		  this.upperProc2Names.put( upperName, list );
		} else {
		  this.upperProc2Names.put(
				upperName,
				Collections.singletonList( procName ) );
		}
		procNames.add( procName );
		a += 2;
		b = this.jtcSys.getMemByte( a++, false );
	      }
	    }
	    StringBuilder msg = new StringBuilder( 256 );
	    int           n   = procNames.size();
	    if( n > 0 ) {
	      if( n == 1 ) {
		msg.append( "1 externe Funktion/Prozedur" );
	      } else {
		msg.append( n );
		msg.append( " externe Funktionen/Prozeduren" );
	      }
	      msg.append( " gefunden: " );
	      int idx = 0;
	      for( String s : procNames ) {
		if( idx > 0 ) {
		  msg.append( ", " );
		}
		if( idx > 10 ) {
		  msg.append( "..." );
		  break;
		}
		msg.append( s );
		idx++;
	      }
	    } else {
	      msg.append( "Keine externe Funktion/Prozedur gefunden" );
	    }
	    putMessage( "Info", msg.toString() );
	  }
	}
      }
    }
  }


  private void parseTRAP() throws ParseException
  {
    putCode( '!' );
    parseCondExpr();
    char ch = skipSpaces();
    if( ch == ',' ) {
      putCode( ch );
      this.iter.next();
    } else {
      String name = readKeyword();
      if( name.isEmpty() ) {
	throwUnexpectedChar( ch );
      }
      if( name.equals( "TO" ) ) {
	putCode( ',' );
      } else {
	throw new ParseException(
			"TO anstelle " + name + " erwartet",
			this.iter.getIndex() );
      }
    }
    parseExpr();
  }


  private Integer[] parseArgList( int argCnt ) throws ParseException
  {
    Integer[] rv = null;
    if( argCnt > 0 ) {
      if( skipSpaces() != '[' ) {
	throwParseException( "\'[\' erwartet" );
      }
      rv = new Integer[ argCnt ];
      Arrays.fill( rv, null );
      putCode( '[' );
      this.iter.next();
      for( int i = 0; i < argCnt; i++ ) {
	if( i > 0 ) {
	  if( skipSpaces() != ',' ) {
	    throwParseException( "\',\' erwartet" );
	  }
	  putCode( ',' );
	  this.iter.next();
	}
	rv[ i ] = parseExpr();
      }
      if( skipSpaces() != ']' ) {
	throwParseException( "\']\' erwartet" );
      }
      putCode( ']' );
      this.iter.next();
    }
    return rv;
  }


  private void parseCondExpr() throws ParseException
  {
    parseExpr();
    char ch = skipSpaces();
    if( ch == '<' ) {
      putCode( ch );
      ch = this.iter.next();
      if( (ch == '=') || (ch == '>') ) {
	this.iter.next();
	putCode( ch );
      }
      parseExpr();
    } else if( ch == '>' ) {
      putCode( ch );
      if( this.iter.next() == '=' ) {
	this.iter.next();
	putCode( '=' );
      }
      parseExpr();
    } else if( ch == '=' ) {
      putCode( ch );
      this.iter.next();
      parseExpr();
    } else {
      throwParseException( "Vergleichsoperator \'<\', \'<=\', \'=\', \'>\'"
					+ " oder \'>=\' erwartet" );
    }
  }


  private Integer parseExpr() throws ParseException
  {
    Integer rv = null;
    char    ch = skipSpaces();
    if( ch == '-' ) {
      putCode( ch );
      this.iter.next();
      Integer op2 = parseDecNumber();
      if( op2 != null ) {
	rv = Integer.valueOf( (short) -op2.intValue() );
      }
    } else {
      rv = parsePrimExpr();
    }
    ch = skipSpaces();
    while( (ch == '+')
	   || (ch == '-')
	   || (ch == '*')
	   || (ch == '/')
	   || (ch == '$') )
    {
      putCode( ch );
      this.iter.next();
      if( ch == '$' ) {
	if( checkToken( "AND" ) || checkToken( "A" ) ) {
	  putCode( 'A' );
	} else if( checkToken( "MOD" ) || checkToken( "M" ) ) {
	  putCode( 'M' );
	} else if( checkToken( "OR" ) || checkToken( "O" ) ) {
	  putCode( 'O' );
	} else if( checkToken( "XOR" ) || checkToken( "X" ) ) {
	  putCode( 'X' );
	} else {
	  throwParseException( "\'AND\', \'MOD\', \'OR\' oder \'XOR\'"
		+ " bzw. \'A\', \'M\', \'O\' oder \'X\' erwartet" );
	}
      }
      Integer op2 = parsePrimExpr();
      if( (rv != null) && (op2 != null) ) {
	short op1 = rv.shortValue();
	rv        = null;
	switch( ch ) {
	  case '+':
	    rv = Integer.valueOf( op1 + op2.shortValue() );
	    break;
	  case '-':
	    rv = Integer.valueOf( op1 - op2.shortValue() );
	    break;
	  case '*':
	    rv = Integer.valueOf( op1 * op2.shortValue() );
	    break;
	  case '/':
	    if( op2 != null ) {
	      rv = Integer.valueOf( op1 / op2.shortValue() );
	    }
	    break;
	}
      } else {
	rv = null;
      }
      ch = skipSpaces();
    }
    return rv;
  }


  private Integer parsePrimExpr() throws ParseException
  {
    Integer rv = null;
    char    ch = skipSpaces();
    if( ch == '(' ) {
      putCode( ch );
      this.iter.next();
      rv = parseExpr();
      if( skipSpaces() != ')' ) {
	throwParseException( "\')\' erwartet" );
      }
      putCode( ')' );
      this.iter.next();
    } else if( isDigit( ch ) ) {
      rv = parseDecNumber();
    } else if( ch == '%' ) {
      rv = parseHexNumber();
    } else {
      String funcName  = readIdentifier();
      String upperName = funcName.toUpperCase();
      int    funcLen   = upperName.length();
      if( funcLen == 1 ) {
	putCode( upperName.charAt( 0 ) );
      } else if( funcLen > 1 ) {
	if( upperName.equals( "ABS" )
	    || upperName.equals( "GETR" )
	    || upperName.equals( "GETRR" )
	    || upperName.equals( "GETEB" )
	    || upperName.equals( "GETEW" )
	    || upperName.equals( "NOT" )
	    || upperName.equals( "RL" )
	    || upperName.equals( "RR" ) )
	{
	  putCode( upperName );
	  parseArgList( 1 );
	} else if( upperName.equals( "GTC" )
		   || upperName.equals( "INPUT" ) )
	{
	  putCode( upperName );
	} else {
	  putCode( toExtProcName( funcName, "Unbekannte Funktion" ) );
	  parseOptArgList();
	}
      } else {
	throwParseException(
		"Ziffer, Buchstabe, \'(\' oder \'%\' erwartet" );
      }
      rv = null;
    }
    return rv;
  }


  private Integer parseDecNumber() throws ParseException
  {
    Integer rv = null;
    char    ch = skipSpaces();
    if( isDigit( ch ) ) {
      putCode( ch );
      int v = ch - '0';
      ch    = this.iter.next();
      while( isDigit( ch ) ) {
	v = (v * 10) + (ch - '0');
	if( v > 0x7FFF ) {
	  throwParseException( "Zahl zu gro\u00DF" );
	}
	putCode( ch );
	ch = this.iter.next();
      }
      rv = Integer.valueOf( v );
    }
    return rv;
  }


  private Integer parseHexNumber() throws ParseException
  {
    Integer rv = null;
    char    ch = skipSpaces();
    if( ch == '%' ) {
      putCode( ch );
      ch    = Character.toUpperCase( this.iter.next() );
      int v = 0;
      int d = toHexValue( ch );
      while( d >= 0 ) {
	putCode( ch );
	v = (v << 4) | d;
	if( v > 0xFFFF ) {
	  throwParseException( "Hexadezimalzahl zu gro\u00DF" );
	}
	ch = Character.toUpperCase( this.iter.next() );
	d  = toHexValue( ch );
      }
      rv = Integer.valueOf( (short) v );
    }
    return rv;
  }


  private void parseOptArgList() throws ParseException
  {
    if( skipSpaces() == '[' ) {
      putCode( '[' );
      this.iter.next();
      for(;;) {
	parseExpr();
	if( skipSpaces() != ',' ) {
	  break;
	}
	putCode( ',' );
	this.iter.next();
      }
      if( skipSpaces() != ']' ) {
	throwParseException( "\']\' erwartet" );
      }
      putCode( ']' );
      this.iter.next();
    }
  }


  private void parseOptStringLiteral() throws ParseException
  {
    char ch = skipSpaces();
    if( ch == '\"' ) {
      putCode( ch );
      boolean warnNL           = false;
      boolean warnNotASCII     = false;
      boolean warnNotPrintable = false;

      ch = this.iter.next();
      while( (ch != CharacterIterator.DONE) && (ch != '\"') ) {
	if( (ch > 0) && (ch <= 0x7F) ) {
	  if( ch == '\n' ) {
	    this.lineNum++;
	    if( !warnNL ) {
	      putWarning( "Zeilenumbruch in Zeichenkette" );
	      warnNL = true;
	    }
	  }
	  else if( (ch < 0x20) || (ch == 0x60) || (ch > 0x7E)
		   || (this.upperCharsOnly && (ch > 0x5F)) )
	  {
	    if( !warnNotPrintable ) {
	      putWarning( "Zeichenkette enth\u00E4lt Zeichen \'" + ch + "\',"
			+ " welches nicht im Zeichensatz des JU+TE-Computers"
			+ " vorkommt" );
	      warnNotPrintable = true;
	    }
	  }
	  putCode( ch );
	} else {
	  if( !warnNotASCII ) {
	    putWarning( "Nicht-ASCII-Zeichen in Zeichenkette ignoriert" );
	    warnNotASCII = true;
	  }
	}
	ch = this.iter.next();
      }
      if( ch == '\"' ) {
	putCode( ch );
	this.iter.next();
      } else {
	throwParseException( "Zeichenkette nicht abgeschlossen" );
      }
    }
  }


  private void putCode( int b )
  {
    if( this.codeBuf != null ) {
      this.codeBuf.write( b );
    }
  }


  private void putCode( String text )
  {
    if( (text != null) && (this.codeBuf != null) ) {
      int len = text.length();
      for( int i = 0; i < len; i++ ) {
	this.codeBuf.write( text.charAt( i ) );
      }
    }
  }


  private void putError(
		String msg,
		int    errorOffset ) throws TooManyErrorsException
  {
    putMessage( "Fehler", msg );
    if( (this.errOut != null) && (errorOffset >= this.lineOffset) ) {
      StringBuilder buf = new StringBuilder( 256 );
      int           pos = this.lineOffset;
      while( (pos < errorOffset) && (pos < this.srcLen) ) {
	buf.append( (char) this.srcText.charAt( pos++ ) );
      }
      buf.append( " ???" );
      if( pos < this.srcLen - 1 ) {
	buf.append( '\u0020' );
	while( pos < this.srcLen ) {
	  char ch = this.srcText.charAt( pos++ );
	  if( ch == '\n' ) {
	    break;
	  }
	  buf.append( ch );
	}
      }
      this.errOut.print( buf.toString() );
      this.errOut.println();
      this.errOut.println();
    }
    this.errorCnt++;
    if( this.errorCnt >= 100 ) {
      throw new TooManyErrorsException();
    }
  }


  private void putWarning( String msg )
  {
    putMessage( "Warnung", msg );
    this.warningCnt++;
  }


  private void putMessage( String msgType, String msgText )
  {
    if( (msgText != null) && (this.errOut != null) ) {
      this.errOut.print( msgType );
      this.errOut.print( " in Zeile " );
      this.errOut.print( Integer.toString( this.lineNum ) );
      if( this.curBasicLineNum >= 0 ) {
	this.errOut.print( ", BASIC-Zeile " );
	this.errOut.print( Integer.toString( this.curBasicLineNum ) );
      }
      this.errOut.print( ": " );
      this.errOut.print( msgText );
      this.errOut.println();
    }
  }


  private String readIdentifier()
  {
    StringBuilder buf = new StringBuilder();
    char ch = skipSpaces();
    if( isUpperLetter( ch ) || isLowerLetter( ch ) || (ch == '_') ) {
      buf.append( ch );
      ch = this.iter.next();
      while( isUpperLetter( ch )
	     || isLowerLetter( ch )
	     || isDigit( ch )
	     || (ch == '_') )
      {
	buf.append( ch );
	ch = this.iter.next();
      }
    }
    return buf.toString();
  }


  private String readKeyword()
  {
    StringBuilder buf = new StringBuilder();
    char          ch  = Character.toUpperCase( skipSpaces() );
    while( isUpperLetter( ch ) ) {
      buf.append( ch );
      ch = Character.toUpperCase( this.iter.next() );
    }
    return buf.toString();
  }


  private int readNumber() throws ParseException
  {
    int  rv = -1;
    char ch = skipSpaces();
    if( isDigit( ch ) ) {
      rv = readIntNumber();
    } else if( ch == '%' ) {
      ch = this.iter.next();
      rv = toHexValue( ch );
      if( rv < 0 ) {
	throwUnexpectedChar( ch );
      }
      int v = toHexValue( this.iter.next() );
      while( v >= 0 ) {
	rv = (rv << 4) | v;
	v  = toHexValue( this.iter.next() );
      }
    }
    return rv;
  }


  private int readIntNumber()
  {
    int  rv = -1;
    char ch = skipSpaces();
    if( isDigit( ch ) ) {
      rv = (ch - '0');
      ch = this.iter.next();
      while( isDigit( ch ) ) {
	int vTmp = (rv * 10) + (ch - '0');
	if( vTmp < 0x00FFFFFF ) {
	  rv = vTmp;
	}
	ch = this.iter.next();
      }
    }
    return rv;
  }


  private char skipSpaces()
  {
    char ch = this.iter.current();
    while( isSpace( ch ) || (ch == '\t') ) {
      ch = this.iter.next();
    }
    return ch;
  }


  private void throwParseException( String msg ) throws ParseException
  {
    throw new ParseException( msg, this.iter.getIndex() );
  }


  private void throwUnexpectedChar( char ch ) throws ParseException
  {
    if( ch == CharacterIterator.DONE ) {
      throwParseException( "Unerwartetes Programmende" );
    } else if( ch == '\n' ) {
      throwParseException( "Unerwartetes Zeilenende" );
    } else {
      throwParseException( "Unerwartetes Zeichen: \'" + ch + "\'" );
    }
  }


  private void throwVarExpected() throws ParseException
  {
    throwParseException( "Variable erwartet" );
  }


  private String toExtProcName( String procName, String msg )
  {
    boolean      found = false;
    List<String> names = this.upperProc2Names.get(
					procName.toUpperCase() );
    if( names != null ) {
      if( names.size() == 1 ) {
	procName = names.get( 0 );
	found    = true;
      } else {
	found = names.contains( procName );
      }
    }
    if( !found ) {
      putWarning( procName + ": " + msg );
    }
    return procName;
  }


  private static int toHexValue( char ch )
  {
    int rv = -1;
    if( isDigit( ch ) ) {
      rv = ch - '0';
    } else if( isUpperLetter( ch ) ) {
      rv = ch - 'A' + 10;
    } else if( isLowerLetter( ch ) ) {
      rv = ch - 'a' + 10;
    }
    return rv;
  }
}
