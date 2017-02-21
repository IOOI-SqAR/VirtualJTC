/*
 * (c) 2007-2010 Jens Mueller
 *
 * Z8-Reassembler
 */

package z8;

import java.io.*;
import java.lang.*;
import java.util.Arrays;


public class Z8Reassembler
{
  private static final String[] condNames = {
                "F", "LT", "LE", "ULE", "OV", "MI", "Z", "C",
                null, "GE", "GT", "UGT", "NOV", "PL", "NZ", "NC" };

  private static final String[] regNames = {
                "SIO", "TMR", "T1", "PRE1", "T0", "PRE0", "P2M", "P3M",
                "P01M", "IPR", "IRQ", "IMR", "FLAGS", "RP", "SPH", "SPL" };


  private Z8Memory      memory;
  private StringBuilder buf;
  private int           addr;
  private String        lastInstName;
  private String[]      lastInstArgs;


  public Z8Reassembler( Z8Memory memory )
  {
    this.memory       = memory;
    this.buf          = null;
    this.addr         = 0;
    this.lastInstName = null;
    this.lastInstArgs = null;
  }


  public int getNextAddr()
  {
    return this.addr;
  }


  public void reassemble( StringBuilder dst, int addr )
  {
    this.addr = addr;
    this.buf  = dst;
    reassNextInst( false );
  }


  public void reassemble(
                        StringBuilder dst,
                        int           begAddr,
                        int           endAddr )
  {
    this.addr = begAddr;
    this.buf  = dst;
    while( this.addr <= endAddr ) {
      reassNextInst( true );
      buf.append( (char) '\n' );
    }
  }


        /* --- private Methoden --- */

  private void reassNextInst( boolean includeAddr )
  {
    this.lastInstName = null;
    this.lastInstArgs = null;
    int instBegAddr   = this.addr;
    int opc           = nextByte();
    int nibbleH       = opc & 0xF0;
    int nibbleL       = opc & 0x0F;
    int h, l;
    switch( nibbleL ) {
      case 0x08:                                        // LD r1,R2
        putInst(
                "LD",
                getWorkingRegName( opc >> 4 ),
                getRegName( nextByte() ) );
        break;

      case 0x09:                                        // LD r2,R1
        putInst(
                "LD",
                getRegName( nextByte() ),
                getWorkingRegName( opc >> 4 ) );
        break;

      case 0x0A:                                        // DJNZ r1,RA
        putInst(
                "DJNZ",
                getWorkingRegName( opc >> 4 ),
                getRelAddrText( nextByte() ) );
        break;

      case 0x0B:                                        // JR cc,RA
        putInst(
                "JR",
                getCondName( opc ),
                getRelAddrText( nextByte() ) );
        break;

      case 0x0C:                                        // LD r1,IM
        putInst(
                "LD",
                getWorkingRegName( opc >> 4 ),
                getDirectValue( nextByte() ) );
        break;

      case 0x0D:                                        // JP cc,RA
        h = nextByte();
        l = nextByte();
        putInst(
                "JP",
                getCondName( opc ),
                getDirectValueW( h, l ) );
        break;

      case 0x0E:                                        // INC r1
        putInst( "INC", getWorkingRegName( opc >> 4 ) );
        break;

      default:
        String instName = null;
        switch( nibbleH >> 4 ) {
          case 0:
            instName = "ADD";
            break;

          case 1:
            instName = "ADC";
            break;

          case 2:
            instName = "SUB";
            break;

          case 3:
            instName = "SBC";
            break;

          case 4:
            instName = "OR";
            break;

          case 5:
            instName = "AND";
            break;

          case 6:
            instName = "TCM";
            break;

          case 7:
            instName = "TM";
            break;

          case 0x0A:
            instName = "CP";
            break;

          case 0x0B:
            instName = "XOR";
            break;
        }
        if( (instName != null) && (nibbleL >= 2) && (nibbleL < 8) ) {
          reassInst( nibbleL, instName );
        } else {
          reassRemainingInst( opc );
        }
    }
    int begPos  = this.buf.length();
    int instCol = 12;
    if( includeAddr ) {
      this.buf.append( String.format( "%04X   ", instBegAddr ) );
      instCol += 7;
    }
    if( instBegAddr < this.addr ) {
      this.buf.append(
                String.format(
                        "%02X",
                        this.memory.getMemByte( instBegAddr++, false ) ) );
    }
    while( instBegAddr < this.addr ) {
      this.buf.append(
                String.format(
                        " %02X",
                        this.memory.getMemByte( instBegAddr++, false ) ) );
    }
    if( this.lastInstName == null ) {
      this.lastInstName = "*NOP";
    }
    int n = begPos + instCol - this.buf.length();
    if( n < 1 ) {
      n = 1;
    }
    for( int i = 0; i < n; i++ ) {
      this.buf.append( (char) '\u0020' );
    }
    begPos = this.buf.length();
    this.buf.append( this.lastInstName );
    if( this.lastInstArgs != null ) {
      for( int i = 0; i < this.lastInstArgs.length; i++ ) {
        String arg = this.lastInstArgs[ i ];
        if( arg != null ) {
          if( begPos >= 0 ) {
            n = begPos + 8 - this.buf.length();
            if( n < 1 ) {
              n = 1;
            }
            for( int k = 0; k < n; k++ ) {
              this.buf.append( (char) '\u0020' );
            }
            begPos = -1;
          } else {
            this.buf.append( ", " );
          }
          this.buf.append( arg );
        }
      }
    }
  }


  private void reassInst( int nibbleL, String instName )
  { int b, r1, r2;
    switch( nibbleL ) {
      case 0x02:                                        // XYZ r1,r2
        b = nextByte();
        putInst(
                instName,
                getWorkingRegName( b >> 4 ),
                getWorkingRegName( b ) );
        break;

      case 0x03:                                        // XYZ r1,Ir2
        b = nextByte();
        putInst(
                instName,
                getWorkingRegName( b >> 4 ),
                getIndirectWorkingRegName( b ) );
        break;

      case 0x04:                                        // XYZ R2,R1
        r2 = nextByte();
        r1 = nextByte();
        putInst( instName, getRegName( r1 ), getRegName( r2 ) );
        break;

      case 0x05:                                        // XYZ IR2,R1
        r2 = nextByte();
        r1 = nextByte();
        putInst( instName, getIndirectRegName( r1 ), getRegName( r2 ) );
        break;

      case 0x06:                                        // XYZ R1,IM
        r1 = nextByte();
        b  = nextByte();
        putInst( instName, getRegName( r1 ), getDirectValue( b ) );
        break;

      case 0x07:                                        // XYZ IR1,IM
        r1 = nextByte();
        b  = nextByte();
        putInst( instName, getIndirectRegName( r1 ), getDirectValue( b ) );
        break;
    }
  }


  private void reassRemainingInst( int opc )
  {
    int a, b;
    switch( opc ) {
      case 0x00:                                        // DEC R1
        putInst( "DEC", getRegName( nextByte() ) );
        break;

      case 0x01:                                        // DEC IR1
        putInst( "DEC", getIndirectRegName( nextByte() ) );
        break;

      case 0x10:                                        // RLC R1
        putInst( "RLC", getRegName( nextByte() ) );
        break;

      case 0x11:                                        // RLC IR1
        putInst( "RLC", getIndirectRegName( nextByte() ) );
        break;

      case 0x20:                                        // INC R1
        putInst( "INC", getRegName( nextByte() ) );
        break;

      case 0x21:                                        // INC IR1
        putInst( "INC", getIndirectRegName( nextByte() ) );
        break;

      case 0x30:                                        // JP IRR1
        putInst( "JP", getIndirectRegNameW( nextByte() ) );
        break;

      case 0x31:                                        // SRP IM
        putInst( "SRP", getDirectValue( nextByte() ) );
        break;

      case 0x40:                                        // DA R1
        putInst( "DA", getRegName( nextByte() ) );
        break;

      case 0x41:                                        // DA IR1
        putInst( "DA", getIndirectRegName( nextByte() ) );
        break;

      case 0x4F:                                        // WDh
        putInst( "WDh" );
        break;

      case 0x50:                                        // POP R1
        putInst( "POP", getRegName( nextByte() ) );
        break;

      case 0x51:                                        // POP IR1
        putInst( "POP", getIndirectRegName( nextByte() ) );
        break;

      case 0x5F:                                        // WDT
        putInst( "WDT" );
        break;

      case 0x60:                                        // COM R1
        putInst( "COM", getRegName( nextByte() ) );
        break;

      case 0x61:                                        // COM IR1
        putInst( "COM", getIndirectRegName( nextByte() ) );
        break;

      case 0x6F:                                        // STOP
        putInst( "STOP" );
        break;

      case 0x70:                                        // PUSH R1
        putInst( "PUSH", getRegName( nextByte() ) );
        break;

      case 0x71:                                        // PUSH IR1
        putInst( "PUSH", getIndirectRegName( nextByte() ) );
        break;

      case 0x7F:                                        // HALT
        putInst( "HALT" );
        break;

      case 0x80:                                        // DECW R1
        putInst( "DECW", getRegName( nextByte() ) );
        break;

      case 0x81:                                        // DECW IR1
        putInst( "DECW", getIndirectRegName( nextByte() ) );
        break;

      case 0x82:                                        // LDE r1,lrr2
        b = nextByte();
        putInst(
                "LDE",
                getWorkingRegName( b >> 4 ),
                getIndirectWorkingRegNameW( b ) );
        break;

      case 0x83:                                        // LDEI lr1,lrr2
        b = nextByte();
        putInst(
                "LDEI",
                getIndirectWorkingRegName( b >> 4 ),
                getIndirectWorkingRegNameW( b ) );
        break;

      case 0x8F:                                        // DI
        putInst( "DI" );
        break;

      case 0x90:                                        // RL R1
        putInst( "RL", getRegName( nextByte() ) );
        break;

      case 0x91:                                        // RL IR1
        putInst( "RL", getIndirectRegName( nextByte() ) );
        break;

      case 0x92:                                        // LDE r2,lrr1
        b = nextByte();
        putInst(
                "LDE",
                getIndirectWorkingRegNameW( b ),
                getWorkingRegName( b >> 4 ) );
        break;

      case 0x93:                                        // LDEI r2,lrr1
        b = nextByte();
        putInst(
                "LDEI",
                getIndirectWorkingRegNameW( b ),
                getIndirectWorkingRegName( b >> 4 ) );
        break;

      case 0x9F:                                        // EI
        putInst( "EI" );
        break;

      case 0xA0:                                        // INCW R1
        putInst( "INCW", getRegName( nextByte() ) );
        break;

      case 0xA1:                                        // INCW IR1
        putInst( "INCW", getIndirectRegName( nextByte() ) );
        break;

      case 0xAF:                                        // RET
        putInst( "RET" );
        break;

      case 0xB0:                                        // CLR R1
        putInst( "CLR", getRegName( nextByte() ) );
        break;

      case 0xB1:                                        // CLR IR1
        putInst( "CLR", getIndirectRegName( nextByte() ) );
        break;

      case 0xBF:                                        // IRET
        putInst( "IRET" );
        break;

      case 0xC0:                                        // RRC R1
        putInst( "RRC", getRegName( nextByte() ) );
        break;

      case 0xC1:                                        // RRC IR1
        putInst( "RRC", getIndirectRegName( nextByte() ) );
        break;

      case 0xC2:                                        // LDC r1,lrr2
        b = nextByte();
        putInst(
                "LDC",
                getWorkingRegName( b >> 4 ),
                getIndirectWorkingRegNameW( b ) );
        break;

      case 0xC3:                                        // LDCI lr1,lrr2
        b = nextByte();
        putInst(
                "LDCI",
                getIndirectWorkingRegName( b >> 4 ),
                getIndirectWorkingRegNameW( b ) );
        break;

      case 0xC7:                                        // LD r1,x,R2
        a = nextByte();
        b = nextByte();
        putInst(
                "LD",
                getWorkingRegName( a >> 4 ),
                getValue( b ) + "(" + getWorkingRegName( a ) + ")" );
        break;

      case 0xCF:                                        // RCF
        putInst( "RCF" );
        break;

      case 0xD0:                                        // SRA R1
        putInst( "SRA", getRegName( nextByte() ) );
        break;

      case 0xD1:                                        // SRA IR1
        putInst( "SRA", getIndirectRegName( nextByte() ) );
        break;

      case 0xD2:                                        // LDC lrr1,r2
        b = nextByte();
        putInst(
                "LDC",
                getIndirectWorkingRegNameW( b ),
                getWorkingRegName( b >> 4 ) );
        break;

      case 0xD3:                                        // LDCI lrr1,lr2
        b = nextByte();
        putInst(
                "LDCI",
                getIndirectWorkingRegNameW( b ),
                getIndirectWorkingRegName( b >> 4 ) );
        break;

      case 0xD4:                                        // CALL IRR1
        putInst( "CALL", getIndirectRegNameW( nextByte() ) );
        break;

      case 0xD6:                                        // CALL DA
        a = nextByte();
        b = nextByte();
        putInst( "CALL", getDirectValueW( a, b ) );
        break;

      case 0xD7:                                        // LD r2,x,R1
        a = nextByte();
        b = nextByte();
        putInst(
                "LD",
                getValue( b ) + "(" + getWorkingRegName( a ) + ")",
                getWorkingRegName( a >> 4 ) );
        break;

      case 0xDF:                                        // SCF
        putInst( "SCF" );
        break;

      case 0xE0:                                        // RR R1
        putInst( "RR", getRegName( nextByte() ) );
        break;

      case 0xE1:                                        // RR IR1
        putInst( "RR", getIndirectRegName( nextByte() ) );
        break;

      case 0xE3:                                        // LD r1,IR2
        b = nextByte();
        putInst(
                "LD",
                getWorkingRegName( b >> 4 ),
                getIndirectWorkingRegName( b ) );
        break;

      case 0xE4:                                        // LD R2,R1
        a = nextByte();
        b = nextByte();
        putInst( "LD", getRegName( b ), getRegName( a ) );
        break;

      case 0xE5:                                        // LD IR2,R1
        a = nextByte();
        b = nextByte();
        putInst( "LD", getRegName( b ), getIndirectRegName( a ) );
        break;

      case 0xE6:                                        // LD R1,IM
        a = nextByte();
        b = nextByte();
        putInst( "LD", getRegName( a ), getDirectValue( b ) );
        break;

      case 0xE7:                                        // LD IR1,IM
        a = nextByte();
        b = nextByte();
        putInst( "LD", getIndirectRegName( a ), getDirectValue( b ) );
        break;

      case 0xEF:                                        // CCF
        putInst( "CCF" );
        break;

      case 0xF0:                                        // SWAP R1
        putInst( "SWAP", getRegName( nextByte() ) );
        break;

      case 0xF1:                                        // SWAP IR1
        putInst( "SWAP", getIndirectRegName( nextByte() ) );
        break;

      case 0xF3:                                        // LD Ir1,r2
        b = nextByte();
        putInst(
                "LD",
                getIndirectWorkingRegName( b >> 4 ),
                getWorkingRegName( b ) );
        break;

      case 0xF5:                                        // LD R2,IR1
        a = nextByte();
        b = nextByte();
        putInst( "LD", getIndirectRegName( b ), getRegName( a ) );
        break;

      case 0xFF:                                        // NOP
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
                ("#" + String.valueOf( v ))
                : String.format( "#%%%02X", v );
  }


  private static String getDirectValueW( int h, int l )
  {
    int v = (h << 8) | l;
    return v <= 9 ? String.valueOf( v ) : String.format( "#%%%04X", v );
  }


  private static String getIndirectRegName( int r )
  {
    return "@" + getRegName( r );
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
    return "@R" + String.valueOf( r & 0x0F );
  }


  private static String getIndirectWorkingRegNameW( int r )
  {
    return "@RR" + String.valueOf( r & 0x0F );
  }


  private static String getRegName( int r )
  {
    String rv = null;
    if( (r & 0xF0) == 0xE0 ) {
      rv = getWorkingRegName( r );
    } else {
      int idx = r - 0xF0;
      if( (idx >= 0) && (idx < regNames.length) ) {
        rv = regNames[ idx ];
      } else {
        rv = getValue( r );
      }
    }
    return rv;
  }


  private static String getValue( int v )
  {
    return v <= 9 ?
                ("%" + String.valueOf( v ))
                : String.format( "%%%02X", v );
  }


  private String getRelAddrText( int relAddr )
  {
    return String.format(
                "#%%%04X",
                (this.addr + (int) (byte) relAddr) & 0xFFFF );
  }


  private static String getWorkingRegName( int r )
  {
    return "R" + String.valueOf( r & 0x0F );
  }


  private int nextByte()
  {
    return this.memory.getMemByte( this.addr++ & 0xFFFF, false );
  }


  private void putInst( String instName, String... instArgs )
  {
    this.lastInstName = instName;
    this.lastInstArgs = instArgs;
  }
}

