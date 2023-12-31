/*
 * (c) 2019-2020 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Eintrag im Assembler-Stack fuer bedingte Assemblierung
 */

package jtcemu.tools.assembler;


public class AsmStackEntry
{
  private int     lineNum;
  private boolean asmEnabled;


  public AsmStackEntry( int lineNum, boolean asmEnabled )
  {
    this.lineNum    = lineNum;
    this.asmEnabled = asmEnabled;
  }


  public int getLineNum()
  {
    return this.lineNum;
  }


  public void invertAssemblingEnabled()
  {
    this.asmEnabled = !this.asmEnabled;
  }


  public boolean isAssemblingEnabled()
  {
    return this.asmEnabled;
  }


  public void setAssemblingEnabled( boolean state )
  {
    this.asmEnabled = state;
  }
}
