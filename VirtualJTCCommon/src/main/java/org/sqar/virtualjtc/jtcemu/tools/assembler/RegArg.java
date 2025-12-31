/*
 * (c) 2019-2021 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Register-Argument eines Assemblerbefehls
 */

package org.sqar.virtualjtc.jtcemu.tools.assembler;

import java.text.CharacterIterator;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


public class RegArg
{
  // Register entsprechend des numerischen Wertes sortiert
  private static final String[] regNames = {
	"SIO",  "TMR", "T1",  "PRE1", "T0", "PRE0", "P2M", "P3M",
	"P01M", "IPR", "IRQ", "IMR",  "FLAGS", "RP",  "SPH", "SPL" };

  private static final String[] sortedWorkingRegNames = {
	"R0", "R1", "R10", "R11", "R12", "R13", "R14", "R15",
	"R2", "R3", "R4", "R5", "R6", "R7", "R8", "R9", 
	"RR0", "RR10", "RR12", "RR14", "RR2", "RR4", "RR6", "RR8" };

  private static Map<String,Integer> regName2Num = null;

  private boolean indirect;
  private boolean doubleReg;
  private boolean workingReg;
  private int     regNum;
  private Integer offset;


  public int getDoubleRegularRegNum( Z8Assembler asm ) throws AsmException
  {
    int r = getRegularRegNum( asm );
    checkEvenDoubleRegNum( r, asm );
    return r;
  }


  public int getDoubleWorkingRegNum( Z8Assembler asm ) throws AsmException
  {
    int r = getWorkingRegNum();
    checkEvenDoubleRegNum( r, asm );
    return r;
  }


  public int getOffset( Z8Assembler asm ) throws AsmException
  {
    if( this.offset == null ) {
      // sollte nie vorkommen
      throw new AsmException( "Offset erwartet" );
    }
    int value = this.offset.intValue();
    if( ((value < 0) || (value > 0xFF))
	&& (((short) ((byte) (value & 0xFF))) != ((short) value)) )
    {
      asm.putWarning( "Offset au\u00DFerhalb 8-Bit-Bereich:"
					+ " Bits werden ignoriert" );
    }
    return value;
  }


  public int getRegNum()
  {
    return this.regNum;
  }


  public static String getRegText( int regNum )
  {
    int idx = regNum - 0xF0;
    return (idx >= 0) && (idx < regNames.length) ?
			regNames[ idx ]
			: String.format( "%%02X", regNum );
  }


  public int getRegularRegNum( Z8Assembler asm ) throws AsmException
  {
    int r = 0;
    if( this.workingReg ) {
      r = 0xE0 | (this.regNum & 0x0F);
    } else {
      r = asm.checkRegNum8Bit( this.regNum );
      if( (r >= 0xE0) && (r <= 0xEF) ) {
	asm.putWarning(
		String.format(
			"Register %d / %%%02X ist Working Register R%1d",
			r,
			r,
			r & 0x0F ) );
      } else {
	asm.checkRegNumExists( r );
      }
    }
    return r;
  }


  public int getWorkingRegNum() throws AsmException
  {
    if( !this.workingReg ) {
      throw new AsmException( "Working Register erwartet" );
    }
    return this.regNum;
  }


  public boolean isDirectReg()
  {
    return !this.indirect && (this.offset == null);
  }


  public boolean isDirectSingleReg()
  {
    return !this.indirect
		&& !this.doubleReg
		&& (this.offset == null);
  }


  public boolean isDirectSingleWorkingReg()
  {
    return !this.indirect
		&& !this.doubleReg
		&& this.workingReg
		&& (this.offset == null);
  }


  public boolean isIndirectReg()
  {
    return this.indirect && (this.offset == null);
  }


  public boolean isIndirectSingleReg()
  {
    return this.indirect && !this.doubleReg && (this.offset == null);
  }


  public boolean isIndirectSingleWorkingReg()
  {
    return this.indirect
		&& !this.doubleReg
		&& this.workingReg
		&& (this.offset == null);
  }


  public boolean isIndirectWorkingReg()
  {
    return this.indirect
		&& this.workingReg
		&& (this.offset == null);
  }


  public boolean isOffsetSingleWorkingReg()
  {
    return !this.indirect
		&& !this.doubleReg
		&& this.workingReg
		&& (this.offset != null);
  }


  public static boolean isRegName( String upperName )
  {
    boolean rv = upperName.equals( "SP" );
    if( !rv ) {
      if( (getRegNum( upperName ) != null)
	  || (Arrays.binarySearch( sortedWorkingRegNames, upperName ) >= 0) )
      {
	rv = true;
      }
    }
    return rv;
  }


  public static RegArg parseRegArg(
				CharacterIterator iter,
				Z8Assembler       asm ) throws AsmException
  {
    RegArg  regArg  = null;
    char    ch      = AsmUtil.skipBlanks( iter );
    int     begIdx1 = iter.getIndex();
    try {

      // indirekte Registerangabe pruefen
      boolean indirect = false;
      if( ch == '@' ) {
	indirect = true;
	iter.next();
	ch = AsmUtil.skipBlanks( iter );
      }

      // Registername pruefen
      int begIdx2 = iter.getIndex();
      try {
	String regName = AsmUtil.tryReadIdentifier( iter, true );
	if( regName != null ) {
	  if( regName.equals( "SP" ) ) {
	    regArg = new RegArg(
				indirect,
				true,		// Doppelregister
				false,		// kein Working Register
				0xFE,		// Registernummer
				null );		// kein Offset
	  } else {
	    Integer regNum = getRegNum( regName );
	    if( regNum != null ) {
	      regArg = new RegArg(
				indirect,
				false,		// kein Doppelregister
				false,		// kein Working Register
				regNum.intValue(),	// Registernummer
				null );		// kein Offset
	    }
	  }
	}
      }
      finally {
	if( regArg == null ) {
	  iter.setIndex( begIdx2 );
	}
      }
      if( regArg == null ) {

	// Working Register pruefen
	int begIdx3 = iter.getIndex();
	try {
	  if( (ch == 'R') || (ch == 'r') ) {
	    boolean doubleReg = false;
	    ch                = iter.next();
	    if( (ch == 'R') || (ch == 'r') ) {
	      doubleReg = true;
	      ch = iter.next();
	    }
	    regArg = new RegArg(
				indirect,
				doubleReg,
				true,		// Working Register
				parseWorkingRegNum( iter ),
				null );		// kein Offset
	  }
	}
	finally {
	  if( regArg == null ) {
	    iter.setIndex( begIdx3 );
	  }
	}
      }
      if( regArg == null ) {

	// regulaere Registerangabe pruefen
	int v = asm.parseIntExprPass2( iter );
	ch    = AsmUtil.skipBlanks( iter );
	if( ch == '(' ) {
	  if( indirect ) {
	    throw new AsmException( "Indirekte Registeraddressierung"
				+ " mit Offset nicht m\u00F6glich" );
	  }
	  iter.next();
	  ch    = AsmUtil.skipBlanks( iter );
	  if( (ch != 'R') && (ch != 'r') ) {
	    AsmUtil.throwCharExpected( 'R' );
	  }
	  ch    = iter.next();
	  int r = parseWorkingRegNum( iter );
	  if( AsmUtil.skipBlanks( iter ) != ')' ) {
	    AsmUtil.throwCharExpected( ch );
	  }
	  iter.next();
	  regArg = new RegArg(
			false,		// indirekt
			false,		// kein Doppelregister
			true,		// Working Register
			r,		// Registernummer
			Integer.valueOf( v ) );		// Offset
	} else {
	  regArg = new RegArg(
			indirect,
			false,		// kein Doppelregister
			false,		// kein Working Register
			v,		// Registernummer
			null );		// kein Offset
	}
      }
    }
    finally {
      if( regArg == null ) {
	iter.setIndex( begIdx1 );
      }
    }
    if( regArg == null ) {
      throw new AsmException( "Argument mit Register erwartet" );
    }
    return regArg;
  }


	/* --- private Methoden --- */

  private void checkEvenDoubleRegNum( int regNum, Z8Assembler asm )
  {
    if( (regNum & 0x01) != 0 ) {
      asm.putWarning( "Doppelregister mit ungerader Nummer" );
    }
  }


  private static Integer getRegNum( String regName )
  {
    if( regName2Num == null ) {
      regName2Num = new HashMap<>();
      regName2Num.put( "P0", 0 );
      regName2Num.put( "P1", 1 );
      regName2Num.put( "P2", 2 );
      regName2Num.put( "P3", 3 );
      int v = 0xF0;
      for( String s : regNames ) {
	regName2Num.put( s, v++ );
      }
    }
    return regName2Num.get( regName );
  }


  private static int parseWorkingRegNum( CharacterIterator iter )
						throws AsmException
  {
    char ch = iter.current();
    if( (ch < '0') || (ch > '9') ) {
      if( AsmUtil.isPrintable( ch ) ) {
	throw new AsmException(
		String.format(
			"Working Register Nummer erwartet anstelle \'%c\'",
			ch ) );
      } else {
	throw new AsmException( "Working Register Nummer erwartet" );
      }
    }
    int r = ExprParser.readIntNumber( iter );
    if( (r < 0) || (r > 15) ) {
      throw new AsmException(
		"Working Register au\u00DFerhalb von 0...15" );
    }
    return r;
  }


	/* --- Konstruktor --- */

  private RegArg(
		boolean indirect,
		boolean doubleReg,
		boolean workingReg,
		int     regNum,
		Integer offset )
  {
    this.indirect   = indirect;
    this.doubleReg  = doubleReg;
    this.workingReg = workingReg;
    this.regNum     = regNum;
    this.offset     = offset;
  }
}
