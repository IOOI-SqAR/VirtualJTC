/*
 * (c) 2019-2020 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Eigenschaften der CPU
 */

package org.jens_mueller.jtcemu.tools.assembler;


public class CPUType
{
  private String  cpuName;
  private boolean reg1reserved;
  private boolean regs80toEFexists;
  private boolean regSIOexists;
  private boolean regSPHreserved;
  private boolean watchdog;


  public void checkRegNum(
		int         regNum,
		Z8Assembler asm ) throws AsmException
  {
    regNum &= 0xFF;
    if( regNum == 1 ) {
      if( this.reg1reserved ) {
	asm.putWarning(
		String.format(
			"Register 1 in der CPU %s reserviert",
			this.cpuName ) );
      }
    }
    else if( (regNum >= 0x80) && (regNum < 0xF0) ) {
      if( !this.regs80toEFexists ) {
	putWarningRegNotExists( asm, regNum );
      }
    }
    else if( regNum == 0xF0 ) {
      if( !this.regSIOexists ) {
	putWarningRegNotExists( asm, regNum );
      }
    }
    else if( regNum == 0xFE ) {
      if( this.regSPHreserved ) {
	asm.putWarning(
		String.format(
			"Register SPH (%%%FE) in der CPU %s reserviert",
			this.cpuName ) );
      }
    }
  }


  public static void defaultCheckRegNum(
				int         regNum,
				Z8Assembler asm )
  {
    regNum &= 0xFF;
    if( ((regNum >= 0x80) && (regNum < 0xF0)) ) {
      asm.putWarning(
	String.format(
		"Register %d / %%%02X existiert nicht in allen Z8-CPUs"
			+ " (CPU nicht angegeben)",
		regNum,
		regNum ) );
    }
  }


  public void checkWatchdog() throws AsmException
  {
    if( !this.watchdog ) {
      throw new AsmException(
		String.format(
			"CPU %s unterst\u00FCtzt keine Watchdog-Befehle",
			this.cpuName ) );
    }
  }


  public static CPUType getByName( String cpuName ) throws AsmException
  {
    boolean reg1reserved     = false;
    boolean regs80toEFexists = false;
    boolean regSIOexists     = false;
    boolean regSPHreserved   = false;
    boolean watchdog         = false;

    cpuName = cpuName.toUpperCase();
    switch( cpuName ) {
      case "Z8":
	regs80toEFexists = true;
	regSIOexists     = true;
	watchdog         = true;
	break;
      case "U881":
      case "U882":
      case "U883":
      case "U884":
      case "U886":
      case "Z8601":
      case "Z8603":
      case "Z8611":
      case "Z8612":
      case "Z8613":
      case "Z8671":
      case "Z8681":
      case "Z8682":
	regSIOexists = true;
	break;
      case "Z86C04":
      case "Z86C08":
	reg1reserved   = true;
	regSPHreserved = true;
	break;
      case "Z86C93":
	reg1reserved     = true;
	regs80toEFexists = true;
	regSIOexists     = true;
	break;
      case "Z86E04":
      case "Z86E08":
	reg1reserved = true;
	watchdog     = true;
	break;
      default:
	throw new AsmException(
			String.format(
				"CPU %s nicht unterst\u00FCtzt",
				cpuName ) );
    }
    return new CPUType(
		cpuName,
		reg1reserved,
		regs80toEFexists,
		regSIOexists,
		regSPHreserved,
		watchdog );
  }


	/* --- private Methoden --- */

  private void putWarningRegNotExists( Z8Assembler asm, int regNum )
  {
    asm.putWarning(
		String.format(
			"Register %d / %%%02X existiert nicht"
				+ " in der CPU %s",
			regNum,
			regNum,
			this.cpuName ) );
  }


	/* --- Konstruktor --- */

  private CPUType(
		String  cpuName,
		boolean reg1reserved,
		boolean regs80toEFexists,
		boolean regSIOexists,
		boolean regSPHreserved,
		boolean watchdog )
  {
    this.cpuName          = cpuName;
    this.reg1reserved     = reg1reserved;
    this.regs80toEFexists = regs80toEFexists;
    this.regSIOexists     = regSIOexists;
    this.regSPHreserved   = regSPHreserved;
    this.watchdog         = watchdog;
  }
}
