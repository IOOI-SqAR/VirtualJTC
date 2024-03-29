/*
 * (c) 2017 Lars Sonchocky-Helldorf
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Instruction Tracer
 */

package org.sqar.virtualjtc.jtcemu.tools;

import org.sqar.virtualjtc.z8.Z8;
import org.sqar.virtualjtc.z8.Z8PCListener;
import org.sqar.virtualjtc.z8.Z8Reassembler;

public class InstructionTracer implements Z8PCListener
{
  private LineAppendable appendable;
  private Z8Reassembler z8Reass;


  public InstructionTracer(Z8 z8, LineAppendable appendable)
  {
    this.z8Reass    = new Z8Reassembler( z8.getMemory() );
    this.appendable = appendable;
  }

  @Override
  public void z8PCUpdate(Z8 z8, int pc)
  {
    StringBuilder buf  = new StringBuilder( 128 );
    z8Reass.reassemble(buf, pc);
    
    appendable.appendLine(String.format("0x%04X", pc) + "\t" + buf.toString());
  }

}
