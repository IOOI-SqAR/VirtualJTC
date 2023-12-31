/*
 * (c) 2007-2019 Jens Mueller
 *
 * Z8 Emulator
 *
 * Emulation eines Timers
 */

package z8;


public class Z8Timer
{
  private int     div4Counter;
  private int     preCounter;
  private int     preCounterInit;
  private int     counter;
  private int     counterInit;
  private boolean loopMode;
  private boolean loopModeInit;


  public Z8Timer()
  {
    this.div4Counter    = 4;
    this.preCounter     = 0;
    this.preCounterInit = 0;
    this.counter        = 0;
    this.counterInit    = 0;
    this.loopMode       = false;
    this.loopModeInit   = false;
  }


  public int getCounter()
  {
    return this.counter;
  }


  public int getPreCounter()
  {
    return this.preCounter;
  }


  public void init()
  {
    this.preCounter = this.preCounterInit;
    this.counter    = this.counterInit;
    this.loopMode   = this.loopModeInit;
  }


  public void setCounter( int value )
  {
    this.counterInit = value & 0xFF;
  }


  public void setPreCounter( int value )
  {
    this.preCounterInit = (value >> 2) & 0x3F;
    this.loopModeInit   = ((value & 0x01) != 0);
  }


  public boolean update( int cycles )
  {
    boolean rv = false;
    if( this.loopMode || (this.counter > 0) ) {
      while( cycles > 0 ) {
	if( cycles >= this.div4Counter ) {
	  cycles -= this.div4Counter;
	  this.div4Counter = 4;
	  if( decPreCounter() ) {
	    rv = true;
	  }
	} else {
	  this.div4Counter -= cycles;
	  cycles = 0;
	}
      }
    }
    return rv;
  }


	/* --- private Methoden --- */

  private boolean decPreCounter()
  {
    boolean rv      = false;
    this.preCounter = (this.preCounter - 1) & 0x3F;
    if( this.preCounter == 0 ) {
      rv = decCounter();
      this.preCounter = this.preCounterInit;
    }
    return rv;
  }


  private boolean decCounter()
  {
    boolean rv   = false;
    this.counter = (this.counter - 1) & 0xFF;
    if( this.counter == 0 ) {
      rv = true;
      if( this.loopMode ) {
	this.counter = this.counterInit;
      }
    }
    return rv;
  }
}
