/*
 * (c) 2007-2021 Jens Mueller
 *
 * Z8-Reassembler
 */

package org.jens_mueller.z8;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;


public class Z8Reassembler
{
  private static final String[] condNames = {
		"F", "LT", "LE", "ULE", "OV", "MI", "Z", "C",
		null, "GE", "GT", "UGT", "NOV", "PL", "NZ", "NC" };

  private static final String[] regNames = {
		"SIO", "TMR", "T1", "PRE1", "T0", "PRE0", "P2M", "P3M",
		"P01M", "IPR", "IRQ", "IMR", "FLAGS", "RP", "SPH", "SPL" };

  private Z8Memory memory;
  private int      addr;
  private int      destAddr;
  private String   mnemonic;
  private String[] args;


  public Z8Reassembler( Z8Memory memory )
  {
    this.memory   = memory;
    this.addr     = 0;
    this.destAddr = -1;
    this.mnemonic = null;
    this.args     = null;
  }


  public int getNextAddr()
  {
    return this.addr;
  }


  public static String getRegName( int r )
  {
    String rv  = null;
    int    idx = r - 0xF0;
    if( (idx >= 0) && (idx < regNames.length) ) {
      rv = regNames[ idx ];
    } else {
      rv = getValue( r );
    }
    return rv;
  }


  public static int getRegNum( String text )
  {
    int rv = -1;
    if( text != null ) {
      text = text.trim().toUpperCase();
      int r = 0xF0;
      for( String regName : regNames ) {
	if( text.equals( regName ) ) {
	  rv = r;
	  break;
	}
	r++;
      }
    }
    return rv;
  }


  public void reassemble(
			StringBuilder dstBuf,
			int           addr )
  {
    this.addr = addr;
    reassNextInst().appendTo( dstBuf );
  }


  public String reassemble( int begAddr, int endAddr )
  {
    StringBuilder buf = new StringBuilder( 0x4000 );
    this.addr         = begAddr;
    while( this.addr <= endAddr ) {
      reassNextInst().appendTo( buf );
      buf.append( '\n' );
    }
    return buf.toString();
  }


  public String reassembleToSource(
				int    begAddr,
				int    endAddr,
				String labelPrefix )
  {
    List<Z8ReassInst> instructions = new ArrayList<>( 0x1000 );
    Set<Integer>      instAddrs    = new TreeSet<>();
    Set<Integer>      destAddrs    = new TreeSet<>();

    // Liste mit reassemblierten Instruktionen und Zieladdressen erzeugen
    this.addr = begAddr;
    while( this.addr <= endAddr ) {
      Z8ReassInst inst     = reassNextInst();
      int         destAddr = inst.getDestAddr();
      if( destAddr >= 0 ) {
	destAddrs.add( destAddr );
      }
      instAddrs.add( inst.getAddr() );
      instructions.add( inst );
    }

    // Ausgabetext erzeugen
    StringBuilder buf = new StringBuilder( 0x4000 );
    buf.append( String.format( "\t.ORG\t%%%04X\n\n", begAddr ) );
    for( Z8ReassInst inst : instructions ) {
      int addr = inst.getAddr();
      if( destAddrs.contains( addr ) ) {
	String label = String.format( "%s%04X:", labelPrefix, addr );
	buf.append( label );
	if( label.length() > 7 ) {
	  buf.append( '\n' );
	}
      }
      buf.append( '\t' );
      buf.append( inst.getMnemonic() );

      boolean warning = false;
      String  args[]  = inst.getArgs();
      if( args != null ) {
	int nArgs = args.length;
	if( nArgs > 0 ) {
	  buf.append( '\t' );
	  int destAddr = inst.getDestAddr();
	  if( destAddr >= 0 ) {
	    if( instAddrs.contains( destAddr ) ) {
	      --nArgs;
	    } else {
	      if( (destAddr >= begAddr) && (destAddr <= endAddr) ) {
		/*
		 * Zieladresse liegt im reassemblierten Bereich,
		 * zeigt aber nicht auf den Anfang einer Instruktion
		 */
		warning = true;
	      }
	      destAddr = -1;
	    }
	  }
	  for( int i = 0; i < nArgs; i++ ) {
	    if( i > 0 ) {
	      buf.append( ", " );
	    }
	    buf.append( args[ i ] );
	  }
	  if( destAddr >= 0 ) {
	    if( nArgs > 0 ) {
	      buf.append( ", " );
	    }
	    buf.append( String.format( "%s%04X", labelPrefix, destAddr ) );
	  }
	}
      }
      if( inst.isUnknown() ) {
	buf.append( "\t\t;???" );
      } else if( warning ) {
	buf.append( "\t\t;!!!" );
      }
      buf.append( '\n' );
    }
    return buf.toString();
  }


	/* --- private Methoden --- */

  private Z8ReassInst reassNextInst()
  {
    this.mnemonic = null;
    this.args     = null;

    Z8ReassInst z8ReassInst = null;
    int         instBegAddr = this.addr;
    int         opc         = nextByte();
    if( (instBegAddr < 0x000C) && ((instBegAddr & 0x0001) == 0) ) {
      putInst( ".DW", String.format( "%%%02X%02X", opc, nextByte() ) );
    } else {
      int nibbleH = opc & 0xF0;
      int nibbleL = opc & 0x0F;
      int h, l;
      switch( nibbleL ) {
	case 0x08:					// LD r1,R2
	  putInst(
		"LD",
		getWorkingRegName( opc >> 4 ),
		getRegText( nextByte() ) );
	  break;

	case 0x09:					// LD r2,R1
	  putInst(
		"LD",
		getRegName( nextByte() ),	// E0: kein Working Reg!
		getWorkingRegName( opc >> 4 ) );
	  break;

	case 0x0A:					// DJNZ r1,RA
	  putInstWithDestAddr(
		"DJNZ",
		getWorkingRegName( opc >> 4 ),
		getRelAddr( nextByte() ) );
	  break;

	case 0x0B:					// JR cc,RA
	  putInstWithDestAddr(
		"JR",
		getCondName( opc ),
		getRelAddr( nextByte() ) );
	  break;

	case 0x0C:					// LD r1,IM
	  putInst(
		"LD",
		getWorkingRegName( opc >> 4 ),
		getDirectValue( nextByte() ) );
	  break;

	case 0x0D:					// JP cc,DA
	  h = nextByte();
	  l = nextByte();
	  putInstWithDestAddr(
		"JP",
		getCondName( opc ),
		(h << 8) | l );
	  break;

	case 0x0E:					// INC r1
	  putInst( "INC", getWorkingRegName( opc >> 4 ) );
	  break;

	default:
	  String mnemonic = null;
	  switch( nibbleH >> 4 ) {
	    case 0:
	      mnemonic = "ADD";
	      break;

	    case 1:
	      mnemonic = "ADC";
	      break;

	    case 2:
	      mnemonic = "SUB";
	      break;

	    case 3:
	      mnemonic = "SBC";
	      break;

	    case 4:
	      mnemonic = "OR";
	      break;

	    case 5:
	      mnemonic = "AND";
	      break;

	    case 6:
	      mnemonic = "TCM";
	      break;

	    case 7:
	      mnemonic = "TM";
	      break;

	    case 0x0A:
	      mnemonic = "CP";
	      break;

	    case 0x0B:
	      mnemonic = "XOR";
	      break;
	  }
	  if( (mnemonic != null) && (nibbleL >= 2) && (nibbleL < 8) ) {
	    reassInst( nibbleL, mnemonic );
	  } else {
	    reassRemainingInst( opc );
	  }
      }
    }
    if( instBegAddr < this.addr ) {
      z8ReassInst = new Z8ReassInst( instBegAddr );
      int addr    = instBegAddr;
      while( addr < this.addr ) {
	z8ReassInst.addCodeByte(
			(byte) this.memory.getMemByte( addr++, false ) );
      }
      if( this.mnemonic != null ) {
	z8ReassInst.setMnemonic( this.mnemonic, this.args );
	z8ReassInst.setDestAddr( this.destAddr );
      } else {
	z8ReassInst.setMnemonic(
		".DB",
		new String[] {
			String.format(
				"%%%02X",
				this.memory.getMemByte(
						instBegAddr,
						false ) ),
		} );
	z8ReassInst.setUnknown();
      }
    }
    return z8ReassInst;
  }


  private void reassInst( int nibbleL, String mnemonic )
  {
    int b, r1, r2;
    switch( nibbleL ) {
      case 0x02:					// XYZ r1,r2
	b = nextByte();
	putInst(
		mnemonic,
		getWorkingRegName( b >> 4 ),
		getWorkingRegName( b ) );
	break;

      case 0x03:					// XYZ r1,Ir2
	b = nextByte();
	putInst(
		mnemonic,
		getWorkingRegName( b >> 4 ),
		getIndirectWorkingRegName( b ) );
	break;

      case 0x04:					// XYZ R2,R1
	r1 = nextByte();
	r2 = nextByte();
	putInst( mnemonic, getRegText( r2 ), getRegText( r1 ) );
	break;

      case 0x05:					// XYZ IR2,R1
	r1 = nextByte();
	r2 = nextByte();
	putInst( mnemonic, getRegText( r2 ), getIndirectRegName( r1 ) );
	break;

      case 0x06:					// XYZ R1,IM
	r1 = nextByte();
	b  = nextByte();
	putInst( mnemonic, getRegText( r1 ), getDirectValue( b ) );
	break;

      case 0x07:					// XYZ IR1,IM
	r1 = nextByte();
	b  = nextByte();
	putInst( mnemonic, getIndirectRegName( r1 ), getDirectValue( b ) );
	break;
    }
  }


  private void reassRemainingInst( int opc )
  {
    int a, b;
    switch( opc ) {
      case 0x00:					// DEC R1
	putInst( "DEC", getRegText( nextByte() ) );
	break;

      case 0x01:					// DEC IR1
	putInst( "DEC", getIndirectRegName( nextByte() ) );
	break;

      case 0x10:					// RLC R1
	putInst( "RLC", getRegText( nextByte() ) );
	break;

      case 0x11:					// RLC IR1
	putInst( "RLC", getIndirectRegName( nextByte() ) );
	break;

      case 0x20:					// INC R1
	putInst( "INC", getRegText( nextByte() ) );
	break;

      case 0x21:					// INC IR1
	putInst( "INC", getIndirectRegName( nextByte() ) );
	break;

      case 0x30:					// JP IRR1
	putInst( "JP", getIndirectRegNameW( nextByte() ) );
	break;

      case 0x31:					// SRP IM
	putInst( "SRP", getDirectValue( nextByte() ) );
	break;

      case 0x40:					// DA R1
	putInst( "DA", getRegText( nextByte() ) );
	break;

      case 0x41:					// DA IR1
	putInst( "DA", getIndirectRegName( nextByte() ) );
	break;

      case 0x4F:					// WDh
	putInst( "WDh" );
	break;

      case 0x50:					// POP R1
	putInst( "POP", getRegText( nextByte() ) );
	break;

      case 0x51:					// POP IR1
	putInst( "POP", getIndirectRegName( nextByte() ) );
	break;

      case 0x5F:					// WDT
	putInst( "WDT" );
	break;

      case 0x60:					// COM R1
	putInst( "COM", getRegText( nextByte() ) );
	break;

      case 0x61:					// COM IR1
	putInst( "COM", getIndirectRegName( nextByte() ) );
	break;

      case 0x6F:					// STOP
	putInst( "STOP" );
	break;

      case 0x70:					// PUSH R1
	putInst( "PUSH", getRegText( nextByte() ) );
	break;

      case 0x71:					// PUSH IR1
	putInst( "PUSH", getIndirectRegName( nextByte() ) );
	break;

      case 0x7F:					// HALT
	putInst( "HALT" );
	break;

      case 0x80:					// DECW R1
	putInst( "DECW", getRegText( nextByte() ) );
	break;

      case 0x81:					// DECW IR1
	putInst( "DECW", getIndirectRegName( nextByte() ) );
	break;

      case 0x82:					// LDE r1,Irr2
	b = nextByte();
	putInst(
		"LDE",
		getWorkingRegName( b >> 4 ),
		getIndirectWorkingRegNameW( b ) );
	break;

      case 0x83:					// LDEI Ir1,Irr2
	b = nextByte();
	putInst(
		"LDEI",
		getIndirectWorkingRegName( b >> 4 ),
		getIndirectWorkingRegNameW( b ) );
	break;

      case 0x8F:					// DI
	putInst( "DI" );
	break;

      case 0x90:					// RL R1
	putInst( "RL", getRegText( nextByte() ) );
	break;

      case 0x91:					// RL IR1
	putInst( "RL", getIndirectRegName( nextByte() ) );
	break;

      case 0x92:					// LDE Irr2,r1
	b = nextByte();
	putInst(
		"LDE",
		getIndirectWorkingRegNameW( b ),
		getWorkingRegName( b >> 4 ) );
	break;

      case 0x93:					// LDEI Irr1,Ir2
	b = nextByte();
	putInst(
		"LDEI",
		getIndirectWorkingRegNameW( b ),
		getIndirectWorkingRegName( b >> 4 ) );
	break;

      case 0x9F:					// EI
	putInst( "EI" );
	break;

      case 0xA0:					// INCW R1
	putInst( "INCW", getRegText( nextByte() ) );
	break;

      case 0xA1:					// INCW IR1
	putInst( "INCW", getIndirectRegName( nextByte() ) );
	break;

      case 0xAF:					// RET
	putInst( "RET" );
	break;

      case 0xB0:					// CLR R1
	putInst( "CLR", getRegText( nextByte() ) );
	break;

      case 0xB1:					// CLR IR1
	putInst( "CLR", getIndirectRegName( nextByte() ) );
	break;

      case 0xBF:					// IRET
	putInst( "IRET" );
	break;

      case 0xC0:					// RRC R1
	putInst( "RRC", getRegText( nextByte() ) );
	break;

      case 0xC1:					// RRC IR1
	putInst( "RRC", getIndirectRegName( nextByte() ) );
	break;

      case 0xC2:					// LDC r1,Irr2
	b = nextByte();
	putInst(
		"LDC",
		getWorkingRegName( b >> 4 ),
		getIndirectWorkingRegNameW( b ) );
	break;

      case 0xC3:					// LDCI Ir1,Irr2
	b = nextByte();
	putInst(
		"LDCI",
		getIndirectWorkingRegName( b >> 4 ),
		getIndirectWorkingRegNameW( b ) );
	break;

      case 0xC7:					// LD r1,x(r2)
	a = nextByte();
	b = nextByte();
	putInst(
		"LD",
		getWorkingRegName( a >> 4 ),
		getSigned8BitValue( b )
			+ "(" + getWorkingRegName( a ) + ")" );
	break;

      case 0xCF:					// RCF
	putInst( "RCF" );
	break;

      case 0xD0:					// SRA R1
	putInst( "SRA", getRegText( nextByte() ) );
	break;

      case 0xD1:					// SRA IR1
	putInst( "SRA", getIndirectRegName( nextByte() ) );
	break;

      case 0xD2:					// LDC Irr2,r1
	b = nextByte();
	putInst(
		"LDC",
		getIndirectWorkingRegNameW( b ),
		getWorkingRegName( b >> 4 ) );
	break;

      case 0xD3:					// LDCI Irr2,Ir1
	b = nextByte();
	putInst(
		"LDCI",
		getIndirectWorkingRegNameW( b ),
		getIndirectWorkingRegName( b >> 4 ) );
	break;

      case 0xD4:					// CALL IRR1
	putInst( "CALL", getIndirectRegNameW( nextByte() ) );
	break;

      case 0xD6:					// CALL DA
	a = nextByte();
	b = nextByte();
	putInstWithDestAddr( "CALL", (a << 8) | b );
	break;

      case 0xD7:					// LD r2,x(r1)
	a = nextByte();
	b = nextByte();
	putInst(
		"LD",
		getSigned8BitValue( b )
			+ "(" + getWorkingRegName( a ) + ")",
		getWorkingRegName( a >> 4 ) );
	break;

      case 0xDF:					// SCF
	putInst( "SCF" );
	break;

      case 0xE0:					// RR R1
	putInst( "RR", getRegText( nextByte() ) );
	break;

      case 0xE1:					// RR IR1
	putInst( "RR", getIndirectRegName( nextByte() ) );
	break;

      case 0xE3:					// LD r1,IR2
	b = nextByte();
	putInst(
		"LD",
		getWorkingRegName( b >> 4 ),
		getIndirectWorkingRegName( b ) );
	break;

      case 0xE4:					// LD R2,R1
	a = nextByte();
	b = nextByte();
	putInst( "LD", getRegText( b ), getRegText( a ) );
	break;

      case 0xE5:					// LD IR2,R1
	a = nextByte();
	b = nextByte();
	putInst( "LD", getRegText( b ), getIndirectRegName( a ) );
	break;

      case 0xE6:					// LD R1,IM
	a = nextByte();
	b = nextByte();
	putInst( "LD", getRegText( a ), getDirectValue( b ) );
	break;

      case 0xE7:					// LD IR1,IM
	a = nextByte();
	b = nextByte();
	putInst( "LD", getIndirectRegName( a ), getDirectValue( b ) );
	break;

      case 0xEF:					// CCF
	putInst( "CCF" );
	break;

      case 0xF0:					// SWAP R1
	putInst( "SWAP", getRegText( nextByte() ) );
	break;

      case 0xF1:					// SWAP IR1
	putInst( "SWAP", getIndirectRegName( nextByte() ) );
	break;

      case 0xF3:					// LD Ir1,r2
	b = nextByte();
	putInst(
		"LD",
		getIndirectWorkingRegName( b >> 4 ),
		getWorkingRegName( b ) );
	break;

      case 0xF5:					// LD R2,IR1
	a = nextByte();
	b = nextByte();
	putInst( "LD", getIndirectRegName( b ), getRegText( a ) );
	break;

      case 0xFF:					// NOP
	putInst( "NOP" );
	break;
    }
  }


  private String getCondName( int value )
  {
    String rv  = null;
    int    idx = (value >> 4) & 0x0F;
    return (idx >= 0) && (idx < condNames.length) ? condNames[ idx ] : null;
  }


  private static String getDirectValue( int v )
  {
    return v <= 9 ?
		String.format( "#%1d", v )
		: String.format( "#%%%02X", v );
  }


  private static String getValueW( int h, int l )
  {
    return String.format( "%%%04X", (h << 8) | l );
  }


  private static String getIndirectRegName( int r )
  {
    return "@" + getRegText( r );
  }


  private static String getIndirectRegNameW( int r )
  {
    String rv = null;
    if( (r & 0xF0) == 0xE0 ) {
      rv = getIndirectWorkingRegNameW( r );
    } else {
      int idx = r - 0xF0;
      if( (idx >= 0) && (idx < regNames.length) ) {
	rv = "@" + regNames[ idx ];
      } else {
	rv = "@" + getValue( r );
      }
    }
    return rv;
  }


  private static String getIndirectWorkingRegName( int r )
  {
    return String.format( "@R%1d", r & 0x0F );
  }


  private static String getIndirectWorkingRegNameW( int r )
  {
    return String.format( "@RR%1d", r & 0x0F );
  }


  private int getRelAddr( int relAddr )
  {
    return (this.addr + (int) ((byte) relAddr)) & 0xFFFF;
  }


  private static String getRegText( int r )
  {
    String rv = null;
    if( (r & 0xF0) == 0xE0 ) {
      rv = getWorkingRegName( r );
    } else {
      rv = getRegName( r );
    }
    return rv;
  }


  private String getSigned8BitValue( int v )
  {
    return String.valueOf( (byte) (v & 0xFF) );
  }


  private static String getValue( int v )
  {
    return v <= 9 ?
		String.format( "%%%1d", v )
		: String.format( "%%%02X", v );
  }


  private static String getWorkingRegName( int r )
  {
    return String.format( "R%1d", r & 0x0F );
  }


  private int nextByte()
  {
    return this.memory.getMemByte( this.addr++ & 0xFFFF, false );
  }


  private void putInst( String mnemonic, String... args )
  {
    this.mnemonic = mnemonic;
    this.args     = args;
    this.destAddr = -1;
  }


  private void putInstWithDestAddr(
			String mnemonic,
			int    destAddr )
  {
    this.mnemonic = mnemonic;
    this.args     = new String[] { String.format( "%%%04X", destAddr ) };
    this.destAddr = destAddr;
  }


  private void putInstWithDestAddr(
			String mnemonic,
			String arg1,
			int    destAddr )
  {
    if( arg1 != null ) {
      this.mnemonic = mnemonic;
      this.args     = new String[] {
				arg1,
				String.format( "%%%04X", destAddr ) };
      this.destAddr = destAddr;
    } else {
      putInstWithDestAddr( mnemonic, destAddr );
    }
  }
}
