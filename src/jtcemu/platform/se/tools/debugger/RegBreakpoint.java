/*
 * (c) 2019-2020 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Haltepunkt auf den Zugriff auf ein Register
 */

package jtcemu.platform.se.tools.debugger;

import java.util.concurrent.atomic.AtomicBoolean;
import z8.Z8;
import z8.Z8Reassembler;


public class RegBreakpoint extends AccessBreakpoint
{
  private static final int  IMR   = 0xFB;
  private static final int  FLAGS = 0xFC;
  private static final int  RP    = 0xFD;
  private static final int  SPH   = 0xFE;
  private static final int  SPL   = 0xFF;

  private int regNum;


  public RegBreakpoint( int regNum, boolean read, boolean write )
  {
    super( read, write );
    this.regNum = regNum;
    updText();
  }


  public int getRegNum()
  {
    return this.regNum;
  }


  public void setValues( int regNum, boolean read, boolean write )
  {
    super.setAccess( read, write );
    this.regNum = regNum;
    updText();
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public int compareTo( AbstractBreakpoint bp )
  {
    int rv = -1;
    if( bp != null ) {
      if( bp instanceof RegBreakpoint ) {
	rv = this.regNum - ((RegBreakpoint) bp).getRegNum();
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
      if( o instanceof RegBreakpoint ) {
	if( ((RegBreakpoint) o).getRegNum() == this.regNum ) {
	  rv = true;
	}
      }
    }
    return rv;
  }


  @Override
  public boolean matches( Z8 z8 )
  {
    boolean       rv  = false;
    AtomicBoolean rv2 = new AtomicBoolean();
    int           pc  = z8.getPC();
    int           b0  = z8.getMemByte( pc, false );
    int           hi0 = b0 & 0xF0;
    int           lo0 = b0 & 0x0F;
    int           b1  = z8.getMemByte( (pc + 1) & 0xFFFF, false );

    // Zugriff auf Stackpointer
    if( (matchesReg( SPH ) && !z8.isInternalStackEnabled())
	|| matchesReg( SPL ) )
    {
      if( (b0 == 0x50) || (b0 == 0x51)			// POP
	  || (b0 == 0x70) || (b0 == 0x71)		// PUSH
	  || (b0 == 0xAF)				// RET
	  || (b0 == 0xBF)				// IRET
	  || (b0 == 0xD4) || (b0 == 0xD6) )		// CALL
      {
	rv = true;
      }
    }

    // Lesezugriff pruefen
    if( isRead() ) {

      // Lesen spezieller Register
      if( matchesReg( FLAGS ) ) {
	if( (lo0 == 0x0B)				// JR cc,DA
	    || (lo0 == 0x0D)				// JP cc,DA
	    || ((b0 >= 0x10) && (b0 <= 0x17))		// RLC, ADC
	    || ((b0 >= 0x32) && (b0 <= 0x37))		// SBC
	    || ((b0 >= 0x40) && (b0 <= 0x41))		// DA
	    || ((b0 >= 0xC0) && (b0 <= 0xC1))		// RRC
	    || (b0 == 0xEF) )				// CCF
	{
	  rv = true;
	}
      } else if( matchesReg( RP ) ) {
	if( ((lo0 == 0x02) && ((hi0 <= 0xD0)))
	    || (lo0 == 0x03)
	    || (b0 == 0xC7)				// LD r1,x(r2)
	    || (b0 == 0xD7)				// LD r2,x(r1)
	    || (lo0 == 0x08)				// LD r1,R2
	    || (lo0 == 0x09)				// LD r2,R1
	    || (lo0 == 0x0A)				// DJNZ r1,DA
	    || (lo0 == 0x0C)				// LD r1,IM
	    || (lo0 == 0x0E) )				// INC r1
	{
	  rv = true;
	}
      }

      // Lesen eines sonstigen Registers
      switch( b0 ) {
	case 0x00:					// DEC R1
	case 0x10:					// RLC R1
	case 0x20:					// INC R1
	case 0x40:					// DA R1
	case 0x60:					// COM R1
	case 0x70:					// PUSH R1
	case 0x90:					// RL R1
	case 0xC0:					// RRC R1
	case 0xD0:					// SRA R1
	case 0xE0:					// RR R1
	case 0xF0:					// SWAP R1
	case 0xE4:					// LD R2,R1
	case 0xF5:					// LD IR2,R1
	case 0xE7:					// LD IR1,IM
	  // Nachfolgendes Byte gibt zu lesendes Register an.
	  if( matchesReg( getRegNum( b1, z8, rv2 ) ) ) {
	    rv = true;
	  }
	  break;

	case 0x30:					// JP IRR1
	case 0x80:					// DECW RR1
	case 0xA0:					// INCW RR1
	case 0xD4:					// CALL IRR1
	  // Nachfolgendes Byte gibt zu lesendes Doppelregister an.
	  {
	    int r = getRegNum( b1, z8, rv2 ) & 0xFE;
	    if( matchesReg( r ) || matchesReg( r + 1 ) ) {
	      rv = true;
	    }
	  }
	  break;

	case 0x01:					// DEC IR1
	case 0x11:					// RLC IR1
	case 0x21:					// INC IR1
	case 0x41:					// DA IR1
	case 0x61:					// COM IR1
	case 0x71:					// PUSH IR1
	case 0x91:					// RL IR1
	case 0xC1:					// RRC IR1
	case 0xD1:					// SRA IR1
	case 0xE1:					// RR IR1
	case 0xF1:					// SWAP IR1
	case 0xE5:					// LD R2,IR1
	  // Nachfolgendes Byte gibt zu lesendes Register indirekt an.
	  {
	    int r = getRegNum( b1, z8, rv2 );
	    if( matchesReg( r ) || matchesReg( z8.getRegValue( r ) ) ) {
	      rv = true;
	    }
	  }
	  break;

	case 0x31:					// JP IRR1
	case 0x81:					// DECW RR1
	case 0xA1:					// INCW RR1
	  // Nachfolgendes Byte gibt zu lesendes Doppelegister indirekt an.
	  {
	    int r = getRegNum( b1, z8, rv2 ) & 0xFE;
	    if( (r == this.regNum) || ((r + 1) == this.regNum) ) {
	      rv = true;
	    }
	  }
	  break;

	case 0x02:					// ADD r1,r2
	case 0x12:					// ADC r1,r2
	case 0x22:					// SUB r1,r2
	case 0x32:					// SBC r1,r2
	case 0x42:					// OR r1,r2
	case 0x52:					// AND r1,r2
	case 0x62:					// TCM r1,r2
	case 0x72:					// TM r1,r2
	case 0xA2:					// CP r1,r2
	case 0xB2:					// XOR r1,r2
	  // Nachfolgendes Byte gibt zwei zu lesende Arbeitsregister an.
	  if( matchesReg( z8.getWorkingRegNum( b1 >> 4 ) )
	      || matchesReg( z8.getWorkingRegNum( b1 ) ) )
	  {
	    rv = true;
	  }
	  break;

	case 0x82:					// LDE r1,Irr2
	case 0xC2:					// LDC r1,Irr2
	  {
	    int r2 = z8.getWorkingRegNum( b1 ) & 0xFE;
	    if( matchesReg( r2 ) || matchesReg( r2 + 1 ) ) {
	      rv = true;
	    }
	  }
	  break;

	case 0x92:					// LDE Irr2,r1
	case 0xD2:					// LDC Irr2,r1
	  {
	    int r1 = z8.getWorkingRegNum( b1 >> 4 );
	    int r2 = z8.getWorkingRegNum( b1 ) & 0xFE;
	    if( matchesReg( r1 )
		|| matchesReg( r2 ) || matchesReg( r2 + 1 ) )
	    {
	      rv = true;
	    }
	  }
	  break;

	case 0x03:					// ADD r1,Ir2
	case 0x13:					// ADC r1,Ir2
	case 0x23:					// SUB r1,Ir2
	case 0x33:					// SBC r1,Ir2
	case 0x43:					// OR r1,Ir2
	case 0x53:					// AND r1,Ir2
	case 0x63:					// TCM r1,Ir2
	case 0x73:					// TM r1,Ir2
	case 0xA3:					// CP r1,Ir2
	case 0xB3:					// XOR r1,Ir2
	  /*
	   * Nachfolgendes Byte gibt im oberen Nibble
	   * ein zu lesendes Arbeitsregister und im unteren Nibble
	   * ein zu lesendes indirtektes Arbeitsregister an.
	   */
	  {
	    int r1 = z8.getWorkingRegNum( b1 >> 4 );
	    int r2 = z8.getWorkingRegNum( b1 );
	    if( matchesReg( r1 )
		|| matchesReg( r2 )
		|| matchesReg( z8.getRegValue( r2 ) ) )
	    {
	      rv = true;
	    }
	  }
	  break;

	case 0x83:					// LDEI Ir1,Irr2
	case 0xC3:					// LDCI Ir1,Irr2
	  {
	    int r1 = z8.getWorkingRegNum( b1 >> 4 );
	    int r2 = z8.getWorkingRegNum( b1 ) & 0xFE;
	    if( matchesReg( r1 )
		|| matchesReg( r2 )
		|| matchesReg( r2 + 1 ) )
	    {
	      rv = true;
	    }
	  }
	  break;

	case 0x93:					// LDEI Irr2,Ir1
	case 0xD3:					// LDCI Irr2,Ir1
	  {
	    int r1 = z8.getWorkingRegNum( b1 >> 4 );
	    int r2 = z8.getWorkingRegNum( b1 ) & 0xFE;
	    if( matchesReg( r1 )
		|| matchesReg( z8.getRegValue( r1 ) )
		|| matchesReg( r2 )
		|| matchesReg( r2 + 1 ) )
	    {
	      rv = true;
	    }
	  }
	  break;

	case 0xE3:					// LD r1,IR2
	  {
	    int r2 = z8.getWorkingRegNum( b1 );
	    if( matchesReg( r2 )
		|| matchesReg( z8.getRegValue( r2 ) ) )
	    {
	      rv = true;
	    }
	  }
	  break;

	case 0xF3:					// LD Ir1,r2
	  {
	    int r1 = z8.getWorkingRegNum( b1 >> 4 );
	    int r2 = z8.getWorkingRegNum( b1 );
	    if( matchesReg( r1 ) || matchesReg( r2 ) ) {
	      rv = true;
	    }
	  }
	  break;

	case 0x04:					// ADD R2,R1
	case 0x14:					// ADC R2,R1
	case 0x24:					// SUB R2,R1
	case 0x34:					// SBC R2,R1
	case 0x44:					// OR R2,R1
	case 0x54:					// AND R2,R1
	case 0x64:					// TCM R2,R1
	case 0x74:					// TM R2,R1
	case 0xA4:					// CP R2,R1
	case 0xB4:					// XOR R2,R1
	  /*
	   * Beide nachfolgende Bytes geben jeweils
	   * ein zu lesendes Register an.
	   */
	  {
	    int b2 = z8.getMemByte( (pc + 2) & 0xFFFF, false );
	    if( matchesReg( getRegNum( b1, z8, rv2 ) )
		|| matchesReg( getRegNum( b2, z8, rv2 ) ) )
	    {
	      rv = true;
	    }
	  }
	  break;

	case 0x05:					// ADD R2,IR1
	case 0x15:					// ADC R2,IR1
	case 0x25:					// SUB R2,IR1
	case 0x35:					// SBC R2,IR1
	case 0x45:					// OR R2,IR1
	case 0x55:					// AND R2,IR1
	case 0x65:					// TCM R2,IR1
	case 0x75:					// TM R2,IR1
	case 0xA5:					// CP R2,IR1
	case 0xB5:					// XOR R2,IR1
	  /*
	   * Nachfolgendes Byte gibt indirekt zu lesendes Regsiter an
	   * und das danach folgende gibt direkt zu lesendes Registe an.	   * ein zu lesendes Register an.
	   */
	  {
	    int b2 = z8.getMemByte( (pc + 2) & 0xFFFF, false );
	    int r1 = getRegNum( b1, z8, rv2 );
	    if( matchesReg( r1 )
		|| matchesReg( z8.getRegValue( r1 ) )
		|| matchesReg( getRegNum( b2, z8, rv2 ) ) )
	    {
	      rv = true;
	    }
	  }
	  break;

	case 0x06:					// ADD R1,IM
	case 0x16:					// ADC R1,IM
	case 0x26:					// SUB R1,IM
	case 0x36:					// SBC R1,IM
	case 0x46:					// OR R1,IM
	case 0x56:					// AND R1,IM
	case 0x66:					// TCM R1,IM
	case 0x76:					// TM R1,IM
	case 0xA6:					// CP R1,IM
	case 0xB6:					// XOR R1,IM
	  // Das dritte Byte gibt das zu lesende Register an.
	  {
	    int b2 = z8.getMemByte( (pc + 2) & 0xFFFF, false );
	    if( matchesReg( getRegNum( b2, z8, rv2 ) ) ) {
	      rv = true;
	    }
	  }
	  break;

	case 0x07:					// ADD IR1,IM
	case 0x17:					// ADC IR1,IM
	case 0x27:					// SUB IR1,IM
	case 0x37:					// SBC IR1,IM
	case 0x47:					// OR IR1,IM
	case 0x57:					// AND IR1,IM
	case 0x67:					// TCM IR1,IM
	case 0x77:					// TM IR1,IM
	case 0xA7:					// CP IR1,IM
	case 0xB7:					// XOR IR1,IM
	  // Das dritte Byte gibt das indirekt zu lesende Register an.
	  {
	    int b2 = z8.getMemByte( (pc + 2) & 0xFFFF, false );
	    int r  = getRegNum( b2, z8, rv2 );
	    if( matchesReg( r ) || matchesReg( z8.getRegValue( r ) ) ) {
	      rv = true;
	    }
	  }
	  break;

	case 0xC7:					// LD r1,x(r2)
	  {
	    int r2 = z8.getWorkingRegNum( b1 );
	    int b2 = z8.getMemByte( (pc + 2) & 0xFFFF, false );
	    if( matchesReg( r2 )
		|| matchesReg( z8.getRegValue( r2 ) + b2 ) )
	    {
	      rv = true;
	    }
	  }
	  break;

	case 0xD7:					// LD r2,x(r1)
	  {
	    int r1 = z8.getWorkingRegNum( b1 >> 4 );
	    int r2 = z8.getWorkingRegNum( b1 );
	    if( matchesReg( r1 ) || matchesReg( r2 ) ) {
	      rv = true;
	    }
	  }
	  break;

	default:
	  switch( lo0 ) {
	    case 0x08:					// LD r1,R2
	      if( matchesReg( getRegNum( b1, z8, rv2 ) ) ) {
		rv = true;
	      }
	      break;

	    case 0x09:					// LD r2,R1
	    case 0x0A:					// DJNZ r1,RA
	    case 0x0E:					// INC r1
	      if( matchesReg( z8.getWorkingRegNum( b0 >> 4 ) ) ) {
		rv = true;
	      }
	      break;
	  }
      }
    }

    // Schreibzugriff pruefen
    if( isWrite() ) {

      // Schreiben des Interrupt Mask Registers
      if( matchesReg( IMR ) && ((b0 == 0x8F) || (b0 == 0x9F)) ) {
	rv = true;
      } else {

	// Schreiben des Flag-Registers
	if( matchesReg( FLAGS ) ) {
	  if( (((lo0 == 0x00) || (lo0 == 0x01))
		  && (hi0 != 0x30) && (hi0 != 0x50)
		  && (hi0 != 0x70) && (hi0 != 0xB0))
	      || (((lo0 >= 0x02) && (lo0 <= 0x07))
		  && ((hi0 <= 0x70)
		      || (hi0 == 0xA0)
		      || (hi0 == 0xB0)))
	      || (b0 == 0xBF) || (b0 == 0xCF) || (b0 == 0xEF) )
	  {
	    rv = true;
	  }
	}

	// Schreinem eines sonstigen Registers
	switch( b0 ) {
	  case 0x00:					// DEC R1
	  case 0x10:					// RLC R1
	  case 0x20:					// INC R1
	  case 0x40:					// DA R1
	  case 0x50:					// POP R1
	  case 0x60:					// COM R1
	  case 0x90:					// RL R1
	  case 0xB0:					// CLR R1
	  case 0xC0:					// RRC R1
	  case 0xD0:					// SRA R1
	  case 0xE0:					// RR R1
	  case 0xF0:					// SWAP R1
	  case 0x06:					// ADD R1,IM
	  case 0x16:					// ADC R1,IM
	  case 0x26:					// SUB R1,IM
	  case 0x36:					// SBC R1,IM
	  case 0x46:					// OR R1,IM
	  case 0x56:					// AND R1,IM
	  case 0xB6:					// XOR R1,IM
	  case 0xE6:					// LD R1,IM
	    // Nachfolgendes Byte gibt Schreibregister an.
	    if( matchesReg( z8.getRegNum( b1 ) ) ) {
	      rv = true;
	    }
	    break;

	  case 0x80:					// DECW RR1
	  case 0xA0:					// INCW RR1
	    // Nachfolgendes Byte gibt zu lesendes Doppelregister an.
	    {
	      int r = z8.getRegNum( b1 ) & 0xFE;
	      if( matchesReg( r ) || matchesReg( r + 1 ) ) {
		rv = true;
	      }
	    }
	    break;

	  case 0x01:					// DEC IR1
	  case 0x11:					// RLC IR1
	  case 0x21:					// INC IR1
	  case 0x41:					// DA IR1
	  case 0x51:					// POP IR1
	  case 0x61:					// COM IR1
	  case 0x91:					// RL IR1
	  case 0xB1:					// CLR IR1
	  case 0xC1:					// RRC IR1
	  case 0xD1:					// SRA IR1
	  case 0xE1:					// RR IR1
	  case 0xF1:					// SWAP IR1
	  case 0x07:					// ADD IR1,IM
	  case 0x17:					// ADC IR1,IM
	  case 0x27:					// SUB IR1,IM
	  case 0x37:					// SBC IR1,IM
	  case 0x47:					// OR IR1,IM
	  case 0x57:					// AND IR1,IM
	  case 0xB7:					// XOR IR1,IM
	  case 0xE7:					// LD R1,IM
	    // Nachfolgendes Byte gibt indirekt das Schreibregister an.
	    if( matchesReg( z8.getRegValue( z8.getRegNum( b1 ) ) ) ) {
	      rv = true;
	    }
	    break;

	  case 0x81:					// DECW IR1
	  case 0xA1:					// INCW IR1
	    // Nachfolgendes Byte gibt indirekt Schreibdoppelregister an.
	    {
	      int r = z8.getRegValue( z8.getRegNum( b1 ) ) & 0xFE;
	      if( matchesReg( r ) || matchesReg( r + 1 ) ) {
		rv = true;
	      }
	    }
	    break;

	  case 0x02:					// ADD r1,r2
	  case 0x12:					// ADC r1,r2
	  case 0x22:					// SUB r1,r2
	  case 0x32:					// SBC r1,r2
	  case 0x42:					// OR r1,r2
	  case 0x52:					// AND r1,r2
	  case 0x82:					// LDE r1,Irr2
	  case 0xB2:					// XOR r1,r2
	  case 0xC2:					// LDC r1,Irr2
	  case 0x03:					// ADD r1,r2
	  case 0x13:					// ADC r1,r2
	  case 0x23:					// SUB r1,r2
	  case 0x33:					// SBC r1,r2
	  case 0x43:					// OR r1,r2
	  case 0x53:					// AND r1,r2
	  case 0xB3:					// XOR r1,r2
	  case 0xE3:					// LD r1,Ir2
	  case 0xC7:					// LD r1,x(r2)
	    // Nachfolgendes Byte gibt im oberen Nibble Schreibregister an.
	    if( matchesReg( z8.getWorkingRegNum( b1 >> 4 ) ) ) {
	      rv = true;
	    }
	    break;

	  case 0x92:					// LDE Irr2,r1
	  case 0xD2:					// LDC Irr2,r1
	    {
	      int r2 = z8.getWorkingRegNum( b1 ) & 0xFE;
	      if( matchesReg( r2 ) || matchesReg( r2 + 1 ) ) {
		rv = true;
	      }
	    }
	    break;

	  case 0x83:					// LDEI Ir1,Irr2
	  case 0x93:					// LDEI Irr2,Ir1
	  case 0xC3:					// LDCI Ir1,Irr2
	  case 0xD3:					// LDCI Irr2,Ir1
	    {
	      int r1 = z8.getWorkingRegNum( b1 >> 4 );
	      int r2 = z8.getWorkingRegNum( b1 ) & 0xFE;
	      if( matchesReg( r1 )
		  || matchesReg( r2 ) || matchesReg( r2 + 1 ) )
	      {
		rv = true;
	      }
	    }
	    break;

	  case 0xF3:					// LD Ir1,r2
	    /*
	     * Nachfolgendes Byte gibt im oberen Nibble
	     * indirekt das Schreibregister an.
	     */
	    if( matchesReg(
			z8.getRegValue(
				z8.getWorkingRegNum( b1 >> 4 ) ) ) )
	    {
	      rv = true;
	    }
	    break;

	  case 0x04:					// ADD R2,R1
	  case 0x14:					// ADC R2,R1
	  case 0x24:					// SUB R2,R1
	  case 0x34:					// SBC R2,R1
	  case 0x44:					// OR R2,R1
	  case 0x54:					// AND R2,R1
	  case 0x84:					// LDE R2,R1
	  case 0xB4:					// XOR R2,R1
	  case 0xE4:					// LD R2,R1
	  case 0x05:					// ADD R2,IR1
	  case 0x15:					// ADC R2,IR1
	  case 0x25:					// SUB R2,IR1
	  case 0x35:					// SBC R2,IR1
	  case 0x45:					// OR R2,IR1
	  case 0x55:					// AND R2,IR1
	  case 0x85:					// LDE R2,IR1
	  case 0xB5:					// XOR R2,IR1
	  case 0xE5:					// LD R2,IR1
	    // Drittes Byte gibt das Schreibregister an.
	    {
	      int b2 = z8.getMemByte( (pc + 2) & 0xFFFF, false );
	      if( matchesReg( z8.getRegNum( b2 ) ) ) {
		rv = true;
	      }
	    }
	    break;

	  case 0xF5:					// LD IR2,R1
	    // Drittes Byte gibt indirekt das Schreibregister an.
	    {
	      int b2 = z8.getMemByte( (pc + 2) & 0xFFFF, false );
	      if( matchesReg( z8.getRegValue( z8.getRegNum( b2 ) ) ) ) {
		rv = true;
	      }
	    }
	    break;

	  case 0xD7:					// LD r2,x(r1)
	    {
	      int r2 = z8.getWorkingRegNum( b1 );
	      int b2 = z8.getMemByte( (pc + 2) & 0xFFFF, false );
	      if( matchesReg( z8.getRegValue( r2 ) + b2 ) ) {
		rv = true;
	      }
	    }
	    break;

	  default:
	    switch( lo0 ) {
	      case 0x08:				// LD r1,R2
	      case 0x0A:				// DJNZ r1,RA
	      case 0x0C:				// LD r1,IM
	      case 0x0E:				// INC r1
		if( matchesReg( z8.getWorkingRegNum( b0 >> 4 ) ) ) {
		  rv = true;
		}
		break;

	      case 0x09:				// LD r2,R1
		// R1 kann in dem Fall kein Arbeitsregister sein
		if( matchesReg( b1 ) ) {
		  rv = true;
		}
		break;
	    }
	}
      }
    }
    return rv || rv2.get();
  }


	/* --- private Methoden --- */

  private int getRegNum( int r, Z8 z8, AtomicBoolean rv )
  {
    if( ((r & 0xF0) == 0xC0) && matchesReg( RP ) ) {
      rv.set( true );
    }
    return z8.getRegNum( r );
  }


  private boolean matchesReg( int r )
  {
    return ((r & 0xFF) == this.regNum);
  }


  private void updText()
  {
    StringBuilder buf = new StringBuilder();
    buf.append( Z8Reassembler.getRegName( this.regNum ) );
    appendAccessTextTo( buf );
    setText( buf.toString() );
  }
}
