/*
 * (c) 2007-2010 Jens Mueller
 *
 * Z8 Emulator
 */

package z8;

import java.lang.*;
import java.util.*;


public class Z8 implements Runnable
{
  public static class PCListenerItem
  {
    public volatile int          addr;
    public volatile Z8PCListener listener;

    PCListenerItem( int addr, Z8PCListener listener )
    {
      this.addr     = addr;
      this.listener = listener;
    }
  };


  public enum RunMode     { RUNNING, INST_HALT, INST_STOP, DEBUG_STOP };
  public enum DebugAction { RUN, RUN_TO_RET, STEP_INTO, STEP_OVER, STOP };

  private enum InstType { ADD, ADC, SUB, SBC, OR, AND, TCM, TM, CP, XOR };


  /*
   * Die Tabelle dient zum Entschluesseln der Interrupt-Prioritaet.
   * Dazu werden die unteren 6 Bits des IPR als Index verwendet.
   */
  private static final int[][] iprCodings = {
	null,			// IPR=0x00 GRP=0x00 reserviert
	{ 1, 4, 5, 3, 2, 0 },	// IPR=0x01 GRP=0x01 C > A > B
	null,			// IPR=0x02 GRP=0x00 reserviert
	{ 4, 1, 5, 3, 2, 0 },	// IPR=0x03 GRP=0x01 C > A > B
	null,			// IPR=0x04 GRP=0x00 reserviert
	{ 1, 4, 5, 3, 0, 2 },	// IPR=0x05 GRP=0x01 C > A > B
	null,			// IPR=0x06 GRP=0x00 reserviert
	{ 4, 1, 5, 3, 0, 2 },	// IPR=0x07 GRP=0x01 C > A > B
	{ 5, 3, 2, 0, 1, 4 },	// IPR=0x08 GRP=0x02 A > B > C
	{ 5, 3, 1, 4, 2, 0 },	// IPR=0x09 GRP=0x03 A > C > B
	{ 5, 3, 2, 0, 4, 1 },	// IPR=0x0A GRP=0x02 A > B > C
	{ 5, 3, 4, 1, 2, 0 },	// IPR=0x0B GRP=0x03 A > C > B
	{ 5, 3, 0, 2, 1, 4 },	// IPR=0x0C GRP=0x02 A > B > C
	{ 5, 3, 1, 4, 0, 2 },	// IPR=0x0D GRP=0x03 A > C > B
	{ 5, 3, 0, 2, 4, 1 },	// IPR=0x0E GRP=0x02 A > B > C
	{ 5, 3, 4, 1, 0, 2 },	// IPR=0x0F GRP=0x03 A > C > B
	{ 2, 0, 1, 4, 5, 3 },	// IPR=0x10 GRP=0x04 B > C > A
	{ 1, 4, 2, 0, 5, 3 },	// IPR=0x11 GRP=0x05 C > B > A
	{ 2, 0, 4, 1, 5, 3 },	// IPR=0x12 GRP=0x04 B > C > A
	{ 4, 1, 2, 0, 5, 3 },	// IPR=0x13 GRP=0x05 C > B > A
	{ 0, 2, 1, 4, 5, 3 },	// IPR=0x14 GRP=0x04 B > C > A
	{ 1, 4, 0, 2, 5, 3 },	// IPR=0x15 GRP=0x05 C > B > A
	{ 0, 2, 4, 1, 5, 3 },	// IPR=0x16 GRP=0x04 B > C > A
	{ 4, 1, 0, 2, 5, 3 },	// IPR=0x17 GRP=0x05 C > B > A
	{ 2, 0, 5, 3, 1, 4 },	// IPR=0x18 GRP=0x06 B > A > C
	null,			// IPR=0x19 GRP=0x07 reserviert
	{ 2, 0, 5, 3, 4, 1 },	// IPR=0x1A GRP=0x06 B > A > C
	null,			// IPR=0x1B GRP=0x07 reserviert
	{ 0, 2, 5, 3, 1, 4 },	// IPR=0x1C GRP=0x06 B > A > C
	null,			// IPR=0x1D GRP=0x07 reserviert
	{ 0, 2, 5, 3, 4, 1 },	// IPR=0x1E GRP=0x06 B > A > C
	null,			// IPR=0x1F GRP=0x07 reserviert
	null,			// IPR=0x20 GRP=0x00 reserviert
	{ 1, 4, 3, 5, 2, 0 },	// IPR=0x21 GRP=0x01 C > A > B
	null,			// IPR=0x22 GRP=0x00 reserviert
	{ 4, 1, 3, 5, 2, 0 },	// IPR=0x23 GRP=0x01 C > A > B
	null,			// IPR=0x24 GRP=0x00 reserviert
	{ 1, 4, 3, 5, 0, 2 },	// IPR=0x25 GRP=0x01 C > A > B
	null,			// IPR=0x26 GRP=0x00 reserviert
	{ 4, 1, 3, 5, 0, 2 },	// IPR=0x27 GRP=0x01 C > A > B
	{ 3, 5, 2, 0, 1, 4 },	// IPR=0x28 GRP=0x02 A > B > C
	{ 3, 5, 1, 4, 2, 0 },	// IPR=0x29 GRP=0x03 A > C > B
	{ 3, 5, 2, 0, 4, 1 },	// IPR=0x2A GRP=0x02 A > B > C
	{ 3, 5, 4, 1, 2, 0 },	// IPR=0x2B GRP=0x03 A > C > B
	{ 3, 5, 0, 2, 1, 4 },	// IPR=0x2C GRP=0x02 A > B > C
	{ 3, 5, 1, 4, 0, 2 },	// IPR=0x2D GRP=0x03 A > C > B
	{ 3, 5, 0, 2, 4, 1 },	// IPR=0x2E GRP=0x02 A > B > C
	{ 3, 5, 4, 1, 0, 2 },	// IPR=0x2F GRP=0x03 A > C > B
	{ 2, 0, 1, 4, 3, 5 },	// IPR=0x30 GRP=0x04 B > C > A
	{ 1, 4, 2, 0, 3, 5 },	// IPR=0x31 GRP=0x05 C > B > A
	{ 2, 0, 4, 1, 3, 5 },	// IPR=0x32 GRP=0x04 B > C > A
	{ 4, 1, 2, 0, 3, 5 },	// IPR=0x33 GRP=0x05 C > B > A
	{ 0, 2, 1, 4, 3, 5 },	// IPR=0x34 GRP=0x04 B > C > A
	{ 1, 4, 0, 2, 3, 5 },	// IPR=0x35 GRP=0x05 C > B > A
	{ 0, 2, 4, 1, 3, 5 },	// IPR=0x36 GRP=0x04 B > C > A
	{ 4, 1, 0, 2, 3, 5 },	// IPR=0x37 GRP=0x05 C > B > A
	{ 2, 0, 3, 5, 1, 4 },	// IPR=0x38 GRP=0x06 B > A > C
	null,			// IPR=0x39 GRP=0x07 reserviert
	{ 2, 0, 3, 5, 4, 1 },	// IPR=0x3A GRP=0x06 B > A > C
	null,			// IPR=0x3B GRP=0x07 reserviert
	{ 0, 2, 3, 5, 1, 4 },	// IPR=0x3C GRP=0x06 B > A > C
	null,			// IPR=0x3D GRP=0x07 reserviert
	{ 0, 2, 3, 5, 4, 1 },	// IPR=0x3E GRP=0x06 B > A > C
	null,			// IPR=0x3F GRP=0x07 reserviert
    };


  /*
   * Da das tatsaechliche Verhalten des Z8 im Fall eines ungueltigen
   * IPR-Wertes nicht dokumentiert ist,
   * wird eine Standard-Prioritaet verwendet,
   * um in dem Fall die Interrupt-Annahme trotzdem zu ermoeglichen.
   */
  private static final int[] defaultInterruptPriority = { 0, 1, 2, 3, 4, 5 };


  // Maskierungen der Interrupts in IMR und IRQ
  private static final int[] interruptMasks
				= { 0x01, 0x02, 0x04, 0x08, 0x10, 0x20 };


  private static final long cyclesWrap = Long.MAX_VALUE / 1100L;
  private static final int  SPL        = 0xFF;
  private static final int  SPH        = 0xFE;
  private static final int  RP         = 0xFD;
  private static final int  FLAGS      = 0xFC;
  private static final int  IMR        = 0xFB;
  private static final int  IRQ        = 0xFA;
  private static final int  IPR        = 0xF9;
  private static final int  P01M       = 0xF8;
  private static final int  P3M        = 0xF7;
  private static final int  P2M        = 0xF6;
  private static final int  PRE0       = 0xF5;
  private static final int  T0         = 0xF4;
  private static final int  PRE1       = 0xF3;
  private static final int  T1         = 0xF2;
  private static final int  TMR        = 0xF1;
  private static final int  SIO        = 0xF0;

  private volatile Z8IO             z8io                 = null;
  private volatile PCListenerItem[] pcListenerItems      = null;
  private volatile int              pc                   = 0;
  private int                       debugSP              = 0;
  private int                       cyclesPerSecond      = 0;
  private int                       overlapCycles        = 0;
  private int                       fetchCycles          = 0;
  private int                       pipelineCycles       = 0;
  private volatile long             unlimitedSpeedCycles = -1;
  private volatile long             totalCycles          = 0;
  private int                       sioPreDiv            = 0;
  private int                       sioIn                = 0;
  private int                       sioIn1Bits           = 0;
  private int                       sioInShift           = 0;
  private int                       sioInShiftNum        = 0;
  private int                       sioOutShift          = 0;
  private int                       regSPL               = 0;
  private int                       regSPH               = 0;
  private int                       regRP                = 0;
  private int                       regFLAGS             = 0;
  private int                       regIMR               = 0;
  private int                       regIRQ               = 0;
  private int                       regIPR               = 0;
  private int                       regP01M              = 0;
  private int                       regP3M               = 0;
  private int                       regP2M               = 0;
  private int                       regTMR               = 0;
  private int                       maxGPRNum            = 0xEF;
  private int[]                     registers            = new int[ 0xF0 ];
  private int[]                     regOut               = new int[ 4 ];
  private int[]                     portIn               = new int[ 4 ];
  private int[]                     portOut              = new int[ 4 ];
  private int[]                     portLastOut          = new int[ 4 ];
  private int                       port3LastIn          = 0xFF;
  private volatile Z8Breakpoint[]   breakpoints          = null;
  private boolean                   flagC                = false;
  private boolean                   flagD                = false;
  private boolean                   flagH                = false;
  private boolean                   flagS                = false;
  private boolean                   flagV                = false;
  private boolean                   flagZ                = false;
  private boolean                   timer1ExtClock       = false;
  private volatile boolean          initFired            = false;
  private volatile boolean          resetFired           = false;
  private volatile boolean          quitFired            = false;
  private Z8Timer                   timer0               = new Z8Timer();
  private Z8Timer                   timer1               = new Z8Timer();
  private Z8Memory                  memory               = null;
  private volatile Z8Debugger       debugger             = null;
  private volatile DebugAction      debugAction          = null;
  private volatile RunMode          runMode              = RunMode.RUNNING;
  private Object                    waitMonitor          = new Object();


  public Z8( int cyclesPerSecond, Z8Memory memory, Z8IO z8io )
  {
    this.cyclesPerSecond = cyclesPerSecond / 2;		// interner Teiler
    this.memory          = memory;
    this.z8io            = z8io;
    Arrays.fill( this.regOut, 0xFF );
    Arrays.fill( this.portIn, -1 );
    Arrays.fill( this.portOut, 0xFF );
    Arrays.fill( this.portLastOut, 0xFF );
    reset( true );
  }


  public synchronized void addPCListener( int addr, Z8PCListener listener )
  {
    PCListenerItem[] items = null;
    if( this.pcListenerItems != null ) {
      items = new PCListenerItem[ this.pcListenerItems.length + 1 ];
      for( int i = 0; i < this.pcListenerItems.length; i++ ) {
	items[ i ] = this.pcListenerItems[ i ];
      }
    } else {
      items = new PCListenerItem[ 1 ];
    }
    items[ items.length - 1 ] = new PCListenerItem( addr, listener );
    this.pcListenerItems      = items;
  }


  public synchronized void removePCListener( int addr, Z8PCListener listener )
  {
    if( this.pcListenerItems != null ) {
      java.util.List<PCListenerItem> list = new ArrayList<PCListenerItem>(
						this.pcListenerItems.length );
      boolean changed = false;
      for( int i = 0; i < this.pcListenerItems.length; i++ ) {
	PCListenerItem item = this.pcListenerItems[ i ];
	if( (item.addr == addr) && (item.listener == listener) ) {
	  changed = true;
	} else {
	  list.add( item );
	}
      }
      if( changed ) {
	int n = list.size();
	if( n > 0 ) {
	  this.pcListenerItems = list.toArray( new PCListenerItem[ n ] );
	} else {
	  this.pcListenerItems = null;
	}
      }
    }
  }


  public int calcDiffCycles( long cycles1, long cycles2 )
  {
    long diffCycles = cycles2 - cycles1;
    return (int) (diffCycles < 0 ? (cyclesWrap + diffCycles) : diffCycles);
  }


  public void fireQuit()
  {
    this.quitFired = true;
    synchronized( this.waitMonitor ) {
      if( this.runMode != RunMode.RUNNING ) {
	try {
	  this.runMode = RunMode.RUNNING;
	  this.waitMonitor.notifyAll();
	}
	catch( IllegalMonitorStateException ex ) {}
      }
    }
  }


  public synchronized void fireReset( boolean forceInit )
  {
    this.initFired   = forceInit;
    this.resetFired  = true;
    this.debugAction = DebugAction.RUN;
    synchronized( this.waitMonitor ) {
      if( this.runMode != RunMode.RUNNING ) {
	try {
	  this.runMode = RunMode.RUNNING;
	  this.waitMonitor.notifyAll();
	}
	catch( IllegalMonitorStateException ex ) {}
      }
    }
  }


  public int getCyclesPerSecond()
  {
    return this.cyclesPerSecond;
  }


  public int getMaxGPRNum()
  {
    return this.maxGPRNum;
  }


  public Z8Memory getMemory()
  {
    return this.memory;
  }


  public int getPC()
  {
    return this.pc;
  }


  public int getRegValue( int r )
  {
    int rv = 0xFF;
    if( (r >= 0) && (r < this.registers.length) && (r <= this.maxGPRNum) ) {
      switch( r ) {
	case 0:
	  if( ((this.regP3M & 0x04) == 0x04)		// Handshake
	      && ((this.regP01M & 0x03) == 0x01) )	// Eingang
	  {
	    this.portOut[ 3 ] |= 0x20;			// P35=1
	  } else {
	    updInputReg0();
	  }
	  break;

	case 1:
	  if( ((this.regP3M & 0x18) == 0x18)		// Handshake
	      && ((this.regP01M & 0x18) == 0x08) )	// Eingang
	  {
	    this.portOut[ 3 ] |= 0x10;			// P34=1
	  } else {
	    updInputReg1();
	  }
	  break;

	case 2:
	  if( ((this.regP3M & 0x20) == 0x20)		// Handshake
	      && ((this.regP2M & 0x80) == 0x80) )	// Eingang
	  {
	    this.portOut[ 3 ] |= 0x40;			// P36=1
	  } else {
	    updInputReg2();
	  }
	  break;

	case 3:
	  this.registers[ 3 ] = (this.portOut[ 3 ] & 0xF0)
					| (getPortValue( 3 ) & 0x0F);
	  break;
      }
      rv = this.registers[ r ];
    } else {
      switch( r ) {
	case SPL:
	  rv = this.regSPL;
	  break;

	case SPH:
	  rv = this.regSPH;
	  break;

	case RP:
	  rv = this.regRP;
	  break;

	case FLAGS:
	  rv = getRegFLAGS();
	  break;

	case IMR:
	  rv = this.regIMR;
	  break;

	case IRQ:
	  rv = this.regIRQ;
	  break;

	case T0:
	  rv = this.timer0.getCounter();
	  break;

	case T1:
	  rv = this.timer1.getCounter();
	  break;

	case TMR:
	  rv = this.regTMR;
	  break;

	case SIO:
	  rv = this.sioIn;
	  break;
      }
    }
    return rv;
  }


  public RunMode getRunMode()
  {
    RunMode runMode = null;
    synchronized( this.waitMonitor ) {
      runMode = this.runMode;
    }
    return runMode;
  }


  public long getTotalCycles()
  {
    return this.totalCycles;
  }


  public boolean isInternalStackEnabled()
  {
    return ((this.regP01M & 0x04) != 0);
  }


  public int pop()
  {
    int rv = 0;
    if( isInternalStackEnabled() ) {
      int a       = this.regSPL;
      rv          = getRegValue( a );
      this.regSPL = (a + 1) & 0xFF;
    } else {
      int a       = (this.regSPH << 8) | this.regSPL;
      rv          = this.memory.getMemByte( a++, true );
      this.regSPH = (a >> 8) & 0xFF;
      this.regSPL = a & 0xFF;
    }
    return rv;
  }


  public int popw()
  {
    int h = pop();
    return (h << 8) | pop();
  }


  public void push( int v )
  {
    if( isInternalStackEnabled() ) {
      int a = this.regSPL;
      setRegValue( --a, v );
      this.regSPL = a & 0xFF;
    } else {
      int a = (((this.regSPH << 8) | this.regSPL) - 1) & 0xFFFF;
      this.memory.setMemByte( a, true, v );
      this.regSPH = a >> 8;
      this.regSPL = a & 0xFF;
    }
  }


  public void pushw( int v )
  {
    push( v );
    push( v >> 8 );
  }


  public synchronized void setBreakpoints( Z8Breakpoint[] breakpoints )
  {
    this.breakpoints = breakpoints;
  }


  public synchronized void setDebugAction( DebugAction debugAction )
  {
    this.debugAction = debugAction;
    if( getRunMode() == RunMode.DEBUG_STOP ) {
      this.debugSP = getSP();
      if( (debugAction == null) || (debugAction != DebugAction.STOP) ) {
	synchronized( this.waitMonitor ) {
	  try {
	    this.waitMonitor.notifyAll();
	  }
	  catch( IllegalMonitorStateException ex ) {}
	}
      }
    } else {
      this.debugSP = -1;
    }
  }


  public void setDebugger( Z8Debugger debugger )
  {
    this.debugger = debugger;
  }


  public void setMaxGPRNum( int value )
  {
    this.maxGPRNum = value;
  }


  public void setPC( int addr )
  {
    this.pc = addr;
  }


  public void setRegValue( int r, int v )
  {
    v &= 0xFF;
    if( (r >= 0) && (r < this.regOut.length) ) {
      this.regOut[ r ] = v;
      int portOut      = this.portOut[ r ];
      switch( r ) {
	case 0:
	  if( (this.regP01M & 0xC0) == 0 ) {		// P04-07: Ausgang
	    portOut = (v & 0xF0) | (portOut & 0x0F);
	  }
	  if( (this.regP01M & 0x03) == 0 ) {		// P00-03: Ausgang
	    portOut = (portOut & 0xF0) | (v & 0x0F);
	  }
	  if( ((this.regP01M & 0xC0) == 0)		// P04-07: Ausgang
	      && ((this.regP3M & 0x04) != 0)		// Port 0: Handshake
	      && ((getPortValue( 3 ) & 0x04) != 0) )	// P32=1
	  {
	    this.portOut[ 3 ] &= 0xDF;			// P35=0
	  }
	  break;

	case 1:
	  if( (this.regP01M & 0x18) == 0 ) {		// Port 1: Ausgang
	    portOut = v;
	    if( ((this.regP3M & 0x18) == 0x18)		// Port 1: Handshake
		&& ((getPortValue( 3 ) & 0x08) != 0) )	// P33=1
	    {
	      this.portOut[ 3 ] &= 0xEF;		// P34=0
	    }
	  }
	  break;

	case 2:
	  portOut = ((portOut & this.regP2M) | (v & ~this.regP2M)) & 0xFF;
	  if( ((this.regP2M & 0x80) == 0)		// Port 2: Ausgang
	      && ((this.regP3M & 0x20) != 0)		// Port 2: Handshake
	      && ((getPortValue( 3 ) & 0x02) != 0) )	// P31=1
	  {
	    this.portOut[ 3 ] &= 0xBF;			// P36=0
	  }
	  break;

	case 3:
	  portOut = (portOut & 0xF0) | (this.port3LastIn & 0x0F);
	  if( (this.regP3M & 0x18) == 0 ) {
	    portOut = (portOut & 0xEF) | (v & 0x10);	// P34: Ausgang
	  }
	  if( (this.regP3M & 0x04) == 0 ) {
	    portOut = (portOut & 0xDF) | (v & 0x20);	// P35: Ausgang
	  }
	  if( (this.regP3M & 0x20) == 0 ) {
	    portOut = (portOut & 0xBF) | (v & 0x40);	// P36: Ausgang
	  }
	  if( (this.regP3M & 0x80) == 0 ) {
	    portOut = (portOut & 0x7F) | (v & 0x80);	// P37: Ausgang
	  }
	  break;
      }
      this.portOut[ r ] = portOut;
    }
    else if( (r >= this.regOut.length)
	     && (r < this.registers.length)
	     && (r <= this.maxGPRNum) )
    {
      this.registers[ r ] = v;
    } else {
      switch( r ) {
	case SPL:
	  this.regSPL = v;
	  break;

	case SPH:
	  this.regSPH = v;
	  break;

	case RP:
	  this.regRP = v;
	  break;

	case FLAGS:
	  this.regFLAGS = v;
	  this.flagH    = ((v & 0x04) != 0);
	  this.flagD    = ((v & 0x08) != 0);
	  this.flagV    = ((v & 0x10) != 0);
	  this.flagS    = ((v & 0x20) != 0);
	  this.flagZ    = ((v & 0x40) != 0);
	  this.flagC    = ((v & 0x80) != 0);
	  break;

	case IMR:
	  this.regIMR = v;
	  break;

	case IRQ:
	  this.regIRQ = v;
	  break;

	case IPR:
	  this.regIPR = v;
	  break;

	case P01M:
	  this.regP01M = v;
	  break;

	case P3M:
	  if( ((this.regP3M & 0x40) == 0)
	      && ((v & 0x40) != 0) )
	  {
	    this.sioInShiftNum = 0;	// SIO aktivieren -> zuruecksetzen
	    this.sioOutShift   = 0;
	  }
	  this.regP3M = v;
	  break;

	case P2M:
	  this.regP2M = v;
	  break;

	case PRE0:
	  this.timer0.setPreCounter( v );
	  this.regTMR |= 0x01;
	  break;

	case T0:
	  this.timer0.setCounter( v );
	  this.regTMR |= 0x01;
	  break;

	case PRE1:
	  this.timer1.setPreCounter( v );
	  this.timer1ExtClock = ((v & 0x02) == 0);
	  this.regTMR |= 0x04;
	  break;

	case T1:
	  this.timer1.setCounter( v );
	  this.regTMR |= 0x04;
	  break;

	case TMR:
	  this.regTMR = v;
	  break;

	case SIO:
	  if( (this.regP3M & 0x40) != 0 ) {
	    if( (this.regP3M & 0x80) != 0 ) {
	      int n = 0;
	      int m = v;
	      for( int i = 0; i < 7; i++ ) {
		if( (m & 0x01) != 0 ) {
		  n++;
		}
		m >>= 1;
	      }
	      v = ((m << 7) & 0x80) | (v & 0x7F);
	    }

	    // 1 Start- und 2 Stop-Bits hinzufuegen
	    this.sioOutShift = 0x600 | (v << 1);

	    // vor Start-Bit muss 1-Pegel ausgegeben werden
	    if( (this.portLastOut[ 3 ] & 0x80) == 0 ) {
	      this.sioOutShift = (this.sioOutShift << 1) | 0x01;
	    }
	  }
	  break;
      }
    }
  }


  /*
   * Diese Methode schaltet die Geschwindigkeitsbremse
   * fuer die Dauer der uebergebenen Anzahl an internen Z8-Taktzyklen aus.
   */
  public void setSpeedUnlimitedFor( int unlimitedSpeedCycles )
  {
    this.unlimitedSpeedCycles = this.totalCycles + (long) unlimitedSpeedCycles;
  }


  public int viewRegValue( int r )
  {
    int rv = 0xFF;
    if( (r >= 0) && (r < this.registers.length) && (r <= this.maxGPRNum) ) {
      rv = this.registers[ r ];
    } else {
      switch( r ) {
	case SPL:
	  rv = this.regSPL;
	  break;

	case SPH:
	  rv = this.regSPH;
	  break;

	case RP:
	  rv = this.regRP;
	  break;

	case FLAGS:
	  rv = getRegFLAGS();
	  break;

	case IMR:
	  rv = this.regIMR;
	  break;

	case IRQ:
	  rv = this.regIRQ;
	  break;

	case IPR:
	  rv = this.regIPR;
	  break;

	case P01M:
	  rv = this.regP01M;
	  break;

	case P3M:
	  rv = this.regP3M;
	  break;

	case P2M:
	  rv = this.regP2M;
	  break;

	case PRE0:
	  rv = this.timer0.getPreCounter();
	  break;

	case T0:
	  rv = this.timer0.getCounter();
	  break;

	case PRE1:
	  rv = this.timer1.getPreCounter();
	  break;

	case T1:
	  rv = this.timer1.getCounter();
	  break;

	case TMR:
	  rv = this.regTMR;
	  break;

	case SIO:
	  rv = this.sioIn;
	  break;
      }
    }
    return rv;
  }


  public boolean wasQuitFired()
  {
    return this.quitFired;
  }


	/* --- Runnable --- */

  @Override
  public void run()
  {
    long millis1           = System.currentTimeMillis();
    int  lastInstCycles    = 0;
    long cyclesSinceAdjust = 0;
    this.totalCycles       = 0;
    while( !this.quitFired ) {

      // Geschwindigkeit
      cyclesSinceAdjust += lastInstCycles;
      this.totalCycles  += lastInstCycles;
      if( (this.unlimitedSpeedCycles < this.totalCycles)
	  && ((cyclesSinceAdjust > 10000) || (this.totalCycles > cyclesWrap)) )
      {
	long plannedMillis = 1000L * this.totalCycles / this.cyclesPerSecond;
	long usedMillis    = System.currentTimeMillis() - millis1;
	long millisToWait  = plannedMillis - usedMillis;
	if( millisToWait > 10 ) {
	  try {
	    Thread.sleep( Math.min( millisToWait, 50 ) );
	  }
	  catch( InterruptedException ex ) {}
	}
	cyclesSinceAdjust = 0;
	if( this.totalCycles > cyclesWrap ) {
	  this.totalCycles          = 0;
	  this.unlimitedSpeedCycles = -1;
	  millis1                   = System.currentTimeMillis();
	}
      }

      // bei HALT warten
      synchronized( this.waitMonitor ) {
	while( this.runMode == RunMode.INST_HALT ) {
	  Z8Debugger debugger = this.debugger;
	  if( debugger != null ) {
	    debugger.z8DebugStatusChanged( this );
	  }
	  try {
	    this.waitMonitor.wait();
	  }
	  catch( IllegalMonitorStateException ex ) {}
	  catch( InterruptedException ex ) {}
	  this.runMode      = RunMode.RUNNING;
	  this.totalCycles  = 0;
	  millis1           = System.currentTimeMillis();
	  if( debugger != null ) {
	    debugger.z8DebugStatusChanged( this );
	  }
	}
      }

      // Reset?
      if( this.resetFired ) {
	reset( this.initFired );
      }

      // Status der Eingangsports zuruecksetzen
      Arrays.fill( this.portIn, -1 );

      // Zwischenspeicher fuer Ausgangsports aktualisieren
      for( int i = 0; i < this.portOut.length; i++ ) {
	this.portOut[ i ] = this.portLastOut[ i ];
      }

      // P30: 1->0 pruefen
      if( ((this.regP3M & 0x40) == 0) && wentP3BitFrom1To0( 0x01 ) ) {
	this.regIRQ |= 0x08;			// IRQ3, wenn SIO inaktiv
      }

      // P31: 1->0 pruefen
      boolean p31From1To0 = wentP3BitFrom1To0( 0x02 );
      if( p31From1To0 ) {
	if( (this.regP3M & 0x20) == 0x20 ) {		// Handshake Port 2
	  if( (this.regP2M & 0x80) == 0x80 ) {		// Eingang
	    if( (this.portLastOut[ 3 ] & 0x40) == 0x40 ) {
	      updInputReg2();				// P36=1: uebernehmen
	      this.portOut[ 3 ] &= ~0x40;		// P36=0
	      this.regIRQ |= 0x04;			// IRQ2
	    }
	  } else {					// Ausgang
	    this.portOut[ 3 ] |= 0x40;			// P36=1
	    this.regIRQ |= 0x04;			// IRQ2
	  }
	} else {
	  this.regIRQ |= 0x04;				// IRQ2
	}
      }

      // P32: 1->0 pruefen
      if( wentP3BitFrom1To0( 0x04 ) ) {
	if( (this.regP3M & 0x04) == 0x04 ) {		// Handshake Port 0
	  if( (this.regP01M & 0x03) == 0x01 ) {		// Eingang
	    if( (this.portLastOut[ 3 ] & 0x20) == 0x20 ) {
	      updInputReg0();				// P35=1: uebernehmen
	      this.portOut[ 3 ] &= ~0x20;		// P35=0
	      this.regIRQ |= 0x01;			// IRQ0
	    }
	  } else {					// Ausgang
	    this.portOut[ 3 ] |= 0x20;			// P35=1
	    this.regIRQ |= 0x01;			// IRQ0
	  }
	} else {
	  this.regIRQ |= 0x01;				// IRQ0
	}
      }

      // P33: 1->0 pruefen
      if( wentP3BitFrom1To0( 0x08 ) ) {
	if( (this.regP3M & 0x18) == 0x18 ) {		// Handshake Port 1
	  if( (this.regP01M & 0x18) == 0x08 ) {		// Eingang
	    if( (this.portLastOut[ 3 ] & 0x10) == 0x10 ) {
	      updInputReg1();				// P34=1: uebernehmen
	      this.portOut[ 3 ] &= ~0x10;		// P34=0
	      this.regIRQ |= 0x02;			// IRQ1
	    }
	  } else {					// Ausgang
	    this.portOut[ 3 ] |= 0x10;			// P34=1
	    this.regIRQ |= 0x02;			// IRQ1
	  }
	} else {
	  this.regIRQ |= 0x02;				// IRQ1
	}
      }

      // Bei STOP Timer laufen lassen und auf Interrupt pruefen
      if( this.runMode == RunMode.INST_STOP ) {
	updCycles( 4, 0 );
      }
      else if( this.runMode == RunMode.RUNNING ) {

	// Debug?
	Z8Debugger debugger = this.debugger;
	if( debugger != null ) {
	  Z8.DebugAction debugAction = null;
	  Z8Breakpoint[] breakpoints = null;
	  int            pc          = 0;
	  synchronized( this ) {
	    debugAction = this.debugAction;
	    breakpoints = this.breakpoints;
	    pc          = this.pc;
	  }
	  boolean reqStop = false;
	  if( breakpoints != null ) {
	    for( int i = 0; i < breakpoints.length; i++ ) {
	      Z8Breakpoint breakpoint = breakpoints[ i ];
	      if( breakpoint.isEnabled()
		  && (pc == breakpoint.getAddress()) )
	      {
		reqStop = true;
		break;
	      }
	    }
	  }
	  if( !reqStop ) {
	    if( debugAction != null ) {
	      switch( debugAction ) {
		case RUN_TO_RET:
		  int opc = this.memory.getMemByte( pc, false );
		  if( ((opc == 0xAF) || (opc == 0xBF))
		      && (getSP() >= this.debugSP) )
		  {
		    reqStop = true;
		  }
		  break;

		case STEP_OVER:
		  if( getSP() >= this.debugSP ) {
		    reqStop = true;
		  }
		  break;

		case STEP_INTO:
		case STOP:
		  reqStop = true;
		  break;
	      }
	    }
	  }
	  if( reqStop ) {
	    synchronized( this.waitMonitor ) {
	      this.runMode = RunMode.DEBUG_STOP;
	      debugger.z8DebugStatusChanged( this );
	      try {
		this.waitMonitor.wait();
	      }
	      catch( IllegalMonitorStateException ex ) {}
	      catch( InterruptedException ex ) {}
	      this.runMode     = RunMode.RUNNING;
	      this.totalCycles = 0;
	      millis1          = System.currentTimeMillis();
	      debugger.z8DebugStatusChanged( this );
	    }
	    synchronized( this ) {
	      debugAction = this.debugAction;
	    }
	    if( debugAction == DebugAction.STEP_OVER ) {
	      int opc = this.memory.getMemByte( this.pc, false );
	      if( (opc != 0xD4) && (opc != 0xD6) )
		this.debugAction = DebugAction.STEP_INTO;
	    }
	  }
	}

	// PC-Listener?
	PCListenerItem[] pcListenerItems = this.pcListenerItems;
	if( pcListenerItems != null ) {
	  for( int i = 0; i < pcListenerItems.length; i++ ) {
	    if( pcListenerItems[ i ].addr == this.pc )
	      pcListenerItems[ i ].listener.z8PCUpdate( this, this.pc );
	  }
	}

	// Befehl ausfuehren
	this.fetchCycles    = 0;
	this.pipelineCycles = 0;
	execNextInst();
      }

      /*
       * Waehrend des Lesens des Befehls wird auf Interrupt geprueft,
       * d.h., der aktuelle Befehl wird noch ausgefuehrt.
       * Deshalb erfolgt im Emulator die Interrupt-Pruefung erst
       * nach Befehlsausfuehrung
       */
      if( ((this.regIMR & 0x80) != 0)
	  && ((this.regIRQ & this.regIMR & 0x3F) != 0) )
      {
	int[] priority = null;
	int   ipr      = this.regIPR & 0x3F;
	if( (ipr >= 0) && (ipr < iprCodings.length) ) {
	  priority = iprCodings[ ipr ];
	}
	if( priority == null ) {
	  priority = defaultInterruptPriority;
	}
	for( int i = 0; i < priority.length; i++ ) {
	  int irq = priority[ i ];
	  if( (irq >= 0) && (irq < interruptMasks.length) ) {
	    int m = interruptMasks[ irq ];
	    if( (this.regIRQ & this.regIMR & m) != 0 ) {
	      pushw( this.pc );
	      push( getRegValue( FLAGS ) );
	      int v   = irq * 2;
	      this.pc = (this.memory.getMemByte( v, false ) << 8)
				  | this.memory.getMemByte( v + 1, false );
	      this.regIRQ &= ~m;
	      this.regIMR &= 0x7F;
	      updCycles( 6, 0 );
	      break;
	    }
	  }
	}
      }

      // Laufzeit des Befehls ermitteln
      int fetchCycles = this.fetchCycles - this.overlapCycles;
      if( fetchCycles < 0 ) {
	fetchCycles = 0;
      }
      lastInstCycles     = fetchCycles + this.pipelineCycles;
      this.overlapCycles = this.pipelineCycles;

      // Timer aktualisieren
      boolean sioPulse = false;
      if( (this.regTMR & 0x02) != 0 ) {
	if( this.timer0.update( lastInstCycles ) ) {
	  if( (this.regP3M & 0x40) != 0 ) {
	    sioPulse = true;
	  } else {
	    this.regIRQ |= 0x10;
	  }
	  if( (this.regTMR & 0xC0) == 0x40 ) {
	    changeP36();
	  }
	}
      }
      if( (this.regTMR & 0x08) != 0 ) {
	int t1Cycles = 0;
	if( this.timer1ExtClock ) {
	  if( (this.regP3M & 0x20) == 0 ) {	// P31: kein Handshake (Port 2)
	    switch( this.regTMR & 0x30 ) {
	      case 0x00:			// P31: externe Taktquelle
		if( p31From1To0 ) {
		  t1Cycles = 1;
		}
		break;

	      case 0x10:			// P31: Tor
		if( (getPortValue( 3 ) & 0x02) != 0 ) {
		  t1Cycles = lastInstCycles;
		}
		break;

	      case 0x20:			// Trigger, nicht retriggerbar
		if( (this.timer1.getCounter() == 0) && p31From1To0 ) {
		  this.regTMR |= 0x04;
		}
		break;

	      case 0x30:			// Trigger, retriggerbar
		if( p31From1To0 ) {
		  this.regTMR |= 0x04;
		}
		break;
	    }
	  }
	} else {
	  t1Cycles = lastInstCycles;
	}
	if( t1Cycles > 0 ) {
	  if( this.timer1.update( t1Cycles ) ) {
	    this.regIRQ |= 0x20;
	    if( (this.regTMR & 0xC0) == 0x80 ) {
	      changeP36();
	    }
	  }
	}
      }
      if( (this.regTMR & 0x01) != 0 ) {
	this.timer0.init();
	this.regTMR &= ~0x01;
      }
      if( (this.regTMR & 0x04) != 0 ) {
	this.timer1.init();
	this.regTMR &= ~0x04;
      }

      // SIO
      if( sioPulse && (this.regP3M & 0x40) != 0 ) {
	// 1:16-Teilung
	if( this.sioPreDiv < 15 ) {
	  this.sioPreDiv++;
	} else {
	  this.sioPreDiv = 0;
	  if( this.sioOutShift != 0 ) {
	    int v = this.portOut[ 3 ] & 0x7F;
	    if( (this.sioOutShift & 0x01) != 0 ) {
	      v |= 0x80;
	    }
	    this.portOut[ 3 ] = v;
	    this.sioOutShift >>= 1;
	    if( this.sioOutShift == 0 ) {
	      this.regIRQ |= 0x10;
	    }
	  }
	  if( this.sioInShiftNum == 0 ) {
	    if( (getPortValue( 3 ) & 0x01) != 0 ) {
	      this.sioInShiftNum = 1;
	    }
	  }
	  else if( this.sioInShiftNum == 1 ) {
	    if( (getPortValue( 3 ) & 0x01) == 0 ) {
	      this.sioInShiftNum = 2;
	      this.sioInShift    = 0;
	      this.sioIn1Bits    = 0;
	    }
	  } else if( (this.sioInShiftNum >= 2) && (this.sioInShiftNum <= 9) ) {
	    this.sioInShiftNum++;
	    this.sioInShift >>= 1;
	    if( (getPortValue( 3 ) & 0x01) != 0 ) {
	      this.sioInShift |= 0x80;
	      if( ((this.regP3M & 0x80) == 0) || (this.sioInShiftNum < 9) ) {
		this.sioIn1Bits++;
	      }
	    }
	  } else if( this.sioInShiftNum == 10 ) {
	    if( (getPortValue( 3 ) & 0x01) == 0 ) {
	      this.sioInShiftNum = 0;
	    } else {
	      this.sioInShiftNum = 1;
	    }
	    if( (this.regP3M & 0x80) != 0 ) {
	      if( (this.sioIn1Bits & 0x01) != 0 ) {
		this.sioInShift = this.sioInShift | 0x80;
	      } else {
		this.sioInShift = this.sioInShift & 0x7F;
	      }
	    }
	    this.sioIn = this.sioInShift;
	    this.regIRQ |= 0x08;
	  }
	}
      }

      // Ports aktualisieren
      if( this.portIn[ 3 ] >= 0 ) {
	this.port3LastIn = this.portIn[ 3 ];
      }
      updPorts();
    }
  }


	/* --- private Methoden --- */

  private void execNextInst()
  {
    int b, r, m;
    int opc     = nextByte();
    int nibbleH = opc & 0xF0;
    int nibbleL = opc & 0x0F;
    switch( nibbleL ) {
      case 0x08:					// LD r1,R2
	b = nextByte();
	r = getWorkingRegIndex( opc >> 4 );
	setRegValue( r, getReg( b ) );
	updCycles( 6, 5 );
	break;

      case 0x09:					// LD r2,R1
	r = getWorkingRegIndex( opc >> 4 );
	setReg( nextByte(), getRegValue( r ) );
	updCycles( 6, 5 );
	break;

      case 0x0A:					// DJNZ r1,RA
	b = nextByte();
	r = getWorkingRegIndex( opc >> 4 );
	if( r >= 0 ) {
	  m = (getRegValue( r ) - 1) & 0xFF;
	  setRegValue( r, m );
	  if( m != 0 ) {
	    this.pc = (this.pc + (int) (byte) b) & 0xFFFF;
	    updCycles( 2, 0 );		// insgesamt 12
	  }
	}
	updCycles( 10, 5 );
	break;

      case 0x0B:					// JR cc,RA
	b = nextByte();
	if( checkCond( opc ) ) {
	  this.pc = (this.pc + (int) (byte) b) & 0xFFFF;
	  updCycles( 12, 0 );
	} else {
	  updCycles( 10, 0 );
	}
	break;

      case 0x0C:					// LD r1,IM
	setReg( getWorkingRegIndex( opc >> 4 ), nextByte() );
	updCycles( 6, 5 );
	break;

      case 0x0D:					// JP cc,DA
	b = nextByte();
	m = nextByte();
	if( checkCond( opc ) ) {
	  this.pc = (b << 8) | m;
	  updCycles( 12, 0 );
	} else {
	  updCycles( 10, 0 );
	}
	break;

      case 0x0E:					// INC r1
	doInstINC( getWorkingRegIndex( opc >> 4 ) );
	updCycles( 6, 5 );
	break;

      default:
	InstType instType = null;
	switch( nibbleH >> 4 ) {
	  case 0:
	    instType = InstType.ADD;
	    break;

	  case 1:
	    instType = InstType.ADC;
	    break;

	  case 2:
	    instType = InstType.SUB;
	    break;

	  case 3:
	    instType = InstType.SBC;
	    break;

	  case 4:
	    instType = InstType.OR;
	    break;

	  case 5:
	    instType = InstType.AND;
	    break;

	  case 6:
	    instType = InstType.TCM;
	    break;

	  case 7:
	    instType = InstType.TM;
	    break;

	  case 0x0A:
	    instType = InstType.CP;
	    break;

	  case 0x0B:
	    instType = InstType.XOR;
	    break;
	}
	if( (instType != null) && (nibbleL >= 2) && (nibbleL < 8) ) {
	  execInst( nibbleL, instType );
	} else {
	  execRemainingInst( opc );
	}
    }
  }


  private void execInst( int nibbleL, InstType instType )
  {
    int b, r1, r2;
    switch( nibbleL ) {
      case 0x02:					// XYZ r1,r2
	b  = nextByte();
	r1 = getWorkingRegIndex( b >> 4 );
	r2 = getWorkingRegIndex( b );
	doInstXYZ(
		r1,
		instType,
		getRegValue( r1 ),
		getRegValue( r2 ) );
	updCycles( 6, 5 );
	break;

      case 0x03:					// XYZ r1,Ir2
	b  = nextByte();
	r1 = getWorkingRegIndex( b >> 4 );
	r2 = getRegValue( getWorkingRegIndex( b ) );
	doInstXYZ(
		r1, 
		instType,
		getRegValue( r1 ),
		getRegValue( r2 ) );
	updCycles( 6, 5 );
	break;

      case 0x04:					// XYZ R2,R1
	r2 = nextByte();
	r1 = nextByte();
	if( (r1 & 0xF0) == 0xE0 ) {
	  r1 = getWorkingRegIndex( r1 );
	}
	doInstXYZ( r1, instType, getReg( r1 ), getReg( r2 ) );
	updCycles( 10, 5 );
	break;

      case 0x05:					// XYZ IR2,R1
	r2 = getIndirectRegIndex( nextByte() );
	r1 = nextByte();
	if( (r1 & 0xF0) == 0xE0 ) {
	  r1 = getWorkingRegIndex( r1 );
	}
	doInstXYZ( r1, instType, getReg( r1 ), getRegValue( r2 ) );
	updCycles( 10, 5 );
	break;

      case 0x06:					// XYZ R1,IM
	r1 = nextByte();
	b  = nextByte();
	if( (r1 & 0xF0) == 0xE0 ) {
	  r1 = getWorkingRegIndex( r1 );
	}
	doInstXYZ( r1, instType, getReg( r1 ), b );
	updCycles( 10, 5 );
	break;

      case 0x07:					// XYZ IR1,IM
	r1 = getIndirectRegIndex( nextByte() );
	b  = nextByte();
	doInstXYZ( r1, instType, getRegValue( r1 ), b );
	updCycles( 10, 5 );
	break;
    }
  }


  private void execRemainingInst( int opc )
  {
    int a, b, r1, r2;
    switch( opc ) {
      case 0x00:					// DEC R1
	doInstDEC( getRegIndex( nextByte() ) );
	updCycles( 6, 5 );
	break;

      case 0x01:					// DEC IR1
	doInstDEC( getIndirectRegIndex( nextByte() ) );
	updCycles( 6, 5 );
	break;

      case 0x10:					// RLC R1
	doInstRLC( getRegIndex( nextByte() ) );
	updCycles( 6, 5 );
	break;

      case 0x11:					// RLC IR1
	doInstRLC( getIndirectRegIndex( nextByte() ) );
	updCycles( 6, 5 );
	break;

      case 0x20:					// INC R1
	doInstINC( getRegIndex( nextByte() ) );
	updCycles( 6, 5 );
	break;

      case 0x21:					// INC IR1
	doInstINC( getIndirectRegIndex( nextByte() ) );
	updCycles( 6, 5 );
	break;

      case 0x30:					// JP IRR1
	r1 = getRegIndex( nextByte() );
	if( r1 >= 0 ) {
	  this.pc = (getRegValue( r1 ) << 8) | getRegValue( r1 + 1 );
	  updCycles( 8, 0 );
	}
	break;

      case 0x31:					// SRP IM
	setRegValue( RP, nextByte() );
	updCycles( 6, 1 );
	break;

      case 0x40:					// DA R1
	doInstDA( getRegIndex( nextByte() ) );
	updCycles( 8, 5 );
	break;

      case 0x41:					// DA IR1
	doInstDA( getIndirectRegIndex( nextByte() ) );
	updCycles( 8, 5 );
	break;

      case 0x50:					// POP R1
	setReg( nextByte(), pop() );
	updCycles( 10, 5 );
	break;

      case 0x51:					// POP IR1
	setRegValue( getIndirectRegIndex( nextByte() ), pop() );
	updCycles( 10, 5 );
	break;

      case 0x60:					// COM R1
	r1 = nextByte();
	setReg( r1, updFlagsSVZ( ~getReg( r1 ) ) );
	updCycles( 6, 5 );
	break;

      case 0x61:					// COM IR1
	r1 = getIndirectRegIndex( nextByte() );
	if( r1 >= 0 ) {
	  setRegValue( r1, updFlagsSVZ( getRegValue( r1 ) ) );
	}
	updCycles( 6, 5 );
	break;

      case 0x6F:					// STOP
	synchronized( this.waitMonitor ) {
	  this.runMode = RunMode.INST_STOP;
	  Z8Debugger debugger = this.debugger;
	  if( debugger != null ) {
	    debugger.z8DebugStatusChanged( this );
	  }
	}
	updCycles( 6, 0 );
	break;

      case 0x70:					// PUSH R1
	push( getReg( nextByte() ) );
	if( isInternalStackEnabled() ) {
	  updCycles( 10, 1 );
	} else {
	  updCycles( 12, 1 );
	}
	break;

      case 0x71:					// PUSH IR1
	push( getReg( getIndirectRegIndex( nextByte() ) ) );
	if( isInternalStackEnabled() ) {
	  updCycles( 12, 1 );
	} else {
	  updCycles( 14, 1 );
	}
	break;

      case 0x7F:					// HALT
	synchronized( this.waitMonitor ) {
	  this.runMode = RunMode.INST_HALT;
	}
	updCycles( 7, 0 );
	break;

      case 0x80:					// DECW RR1
	doInstDECW( getRegIndex( nextByte() ) );
	updCycles( 10, 5 );
	break;

      case 0x81:					// DECW IR1
	doInstDECW( getIndirectRegIndex( nextByte() ) );
	updCycles( 10, 5 );
	break;

      case 0x82:					// LDE r1,lrr2
      case 0xC2:					// LDC r1,lrr2
	b  = nextByte();
	r1 = getWorkingRegIndex( b >> 4 );
	r2 = getWorkingRegIndex( b );
	setRegValue(
		r1,
		this.memory.getMemByte( getRegWValue( r2 ), opc == 0x82 ) );
	updCycles( 12, 0 );
	break;

      case 0x83:					// LDEI lr1,lrr2
      case 0xC3:					// LDCI lr1,lrr2
	b  = nextByte();
	r1 = getWorkingRegIndex( b >> 4 );
	r2 = getWorkingRegIndex( b );
	a  = getRegWValue( r2 );
	b  = getRegValue( r1 );
	setRegValue( b, this.memory.getMemByte( a, opc == 0x83 ) );
	setRegValue( r1, b + 1 );
	setRegWValue( r2, a + 1 );
	updCycles( 18, 0 );
	break;

      case 0x8F:					// DI
	this.regIMR &= 0x7F;
	updCycles( 6, 1 );
	break;

      case 0x90:					// RL R1
	doInstRL( getRegIndex( nextByte() ) );
	updCycles( 6, 5 );
	break;

      case 0x91:					// RL IR1
	doInstRL( getIndirectRegIndex( nextByte() ) );
	updCycles( 6, 5 );
	break;

      case 0x92:					// LDE r2,lrr1
      case 0xD2:					// LDC r2,lrr1
	b  = nextByte();
	r2 = getWorkingRegIndex( b >> 4 );
	r1 = getWorkingRegIndex( b );
	this.memory.setMemByte(
			getRegWValue( r1 ),
			opc == 0x92,
			getRegValue( r2 ) );
	updCycles( 12, 0 );
	break;

      case 0x93:					// LDEI r2,lrr1
      case 0xD3:					// LDCI lrr1,lr2
	b  = nextByte();
	r2 = getWorkingRegIndex( b >> 4 );
	r1 = getWorkingRegIndex( b );
	a  = getRegWValue( r1 );
	b  = getRegValue( r2 );
	this.memory.setMemByte( a, opc == 0x93, getRegValue( b ) );
	setRegWValue( r1, a + 1 );
	setRegValue( r2, b + 1 );
	updCycles( 18, 0 );
	break;

      case 0x9F:					// EI
	this.regIMR |= 0x80;
	updCycles( 6, 1 );
	break;

      case 0xA0:					// INCW RR1
	doInstINCW( getRegIndex( nextByte() ) );
	updCycles( 10, 5 );
	break;

      case 0xA1:					// INCW IR1
	doInstINCW( getIndirectRegIndex( nextByte() ) );
	updCycles( 10, 5 );
	break;

      case 0xAF:					// RET
	this.pc = popw();
	updCycles( 14, 0 );
	break;

      case 0xB0:					// CLR R1
	setReg( nextByte(), 0 );
	updCycles( 6, 5 );
	break;

      case 0xB1:					// CLR IR1
	r1 = getIndirectRegIndex( nextByte() );
	if( r1 >= 0 ) {
	  setRegValue( r1, 0 );
	}
	updCycles( 6, 5 );
	break;

      case 0xBF:					// IRET
	setRegValue( FLAGS, pop() );
	this.regIMR |= 0x80;
	this.pc = popw();
	updCycles( 16, 0 );
	break;

      case 0xC0:					// RRC R1
	doInstRRC( getRegIndex( nextByte() ) );
	updCycles( 6, 5 );
	break;

      case 0xC1:					// RRC IR1
	doInstRRC( getIndirectRegIndex( nextByte() ) );
	updCycles( 6, 5 );
	break;

      case 0xC7:					// LD r1,x,R2
	b  = nextByte();
	r1 = getWorkingRegIndex( b >> 4 );
	r2 = getWorkingRegIndex( b );
	setRegValue(
		r1,
		getRegValue( (getRegValue( r2 ) + nextByte()) & 0xFF ) );
	updCycles( 10, 5 );
	break;

      case 0xCF:					// RCF
	this.flagC = false;
	updCycles( 6, 5 );
	break;

      case 0xD0:					// SRA R1
	doInstSRA( getRegIndex( nextByte() ) );
	updCycles( 6, 5 );
	break;

      case 0xD1:					// SRA IR1
	doInstSRA( getIndirectRegIndex( nextByte() ) );
	updCycles( 6, 5 );
	break;

      case 0xD4:					// CALL IRR1
	r1 = getRegIndex( nextByte() );
	if( (r1 >= 0) && ((r1 & 0x01) == 0) ) {
	  pushw( this.pc );
	  this.pc = getRegWValue( r1 );
	  updCycles( 20, 0 );
	}
	break;

      case 0xD6:					// CALL DA
	a = nextByte();
	b = nextByte();
	pushw( this.pc );
	this.pc = (a << 8) | b;
	updCycles( 20, 0 );
	break;

      case 0xD7:					// LD r2,x,R1
	b  = nextByte();
	r2 = getWorkingRegIndex( b >> 4 );
	r1 = getWorkingRegIndex( b );
	setRegValue(
		(getRegValue( r1 ) + nextByte()) & 0xFF,
		getRegValue( r2 ) );
	updCycles( 10, 5 );
	break;

      case 0xDF:					// SCF
	this.flagC = true;
	updCycles( 6, 5 );
	break;

      case 0xE0:					// RR R1
	doInstRR( getRegIndex( nextByte() ) );
	updCycles( 6, 5 );
	break;

      case 0xE1:					// RR IR1
	doInstRR( getIndirectRegIndex( nextByte() ) );
	updCycles( 6, 5 );
	break;

      case 0xE3:					// LD r1,IR2
	b  = nextByte();
	r1 = getWorkingRegIndex( b >> 4 );
	r2 = getWorkingRegIndex( b );
	setRegValue( r1, getRegValue( getRegValue( r2 ) ) );
	updCycles( 6, 5 );
	break;

      case 0xE4:					// LD R2,R1
	b = getReg( nextByte() );
	setReg( nextByte(), b );
	updCycles( 10, 5 );
	break;

      case 0xE5:					// LD IR2,R1
	r2 = getIndirectRegIndex( nextByte() );
	setReg( nextByte(), getRegValue( r2 ) );
	updCycles( 10, 5 );
	break;

      case 0xE6:					// LD R1,IM
	r1 = getRegIndex( nextByte() );
	setRegValue( r1, nextByte() );
	updCycles( 10, 5 );
	break;

      case 0xE7:					// LD IR1,IM
	r1 = getIndirectRegIndex( nextByte() );
	setRegValue( r1, nextByte() );
	updCycles( 10, 5 );
	break;

      case 0xEF:					// CCF
	this.flagC = !this.flagC;
	updCycles( 6, 5 );
	break;

      case 0xF0:					// SWAP R1
	doInstSWAP( getRegIndex( nextByte() ) );
	updCycles( 8, 5 );
	break;

      case 0xF1:					// SWAP IR1
	doInstSWAP( getIndirectRegIndex( nextByte() ) );
	updCycles( 8, 5 );
	break;

      case 0xF3:					// LD Ir1,r2
	b  = nextByte();
	r1 = getWorkingRegIndex( b >> 4 );
	r2 = getWorkingRegIndex( b );
	setRegValue( getRegValue( r1 ), getRegValue( r2 ) );
	updCycles( 6, 5 );
	break;

      case 0xF5:					// LD R2,IR1
	b  = getReg( nextByte() );
	r1 = getIndirectRegIndex( nextByte() );
	setReg( r1, b );
	updCycles( 10, 5 );
	break;

      /*
       * Alle anderen Befehle werden als NOP behandelt,
       * auch WDh und WDT.
       * Da es den Watchdog Timer nicht bei allen Z8-Systemen gibt,
       * wird hier auch keiner emuliert.
       */
      default:						// NOP, WDh, WDT
	updCycles( 6, 0 );
	break;
    }
  }


  private boolean checkCond( int value )
  {
    boolean rv = false;
    switch( (value >> 4) & 0x0F ) {
      case 1:						// LT
	rv = (this.flagS != this.flagV);
	break;

      case 2:						// LE
	rv = this.flagZ || (this.flagS != this.flagV);
	break;

      case 3:						// ULE
	rv = this.flagC || this.flagZ;
	break;

      case 4:						// OV
	rv = this.flagV;
	break;

      case 5:						// MI
	rv = this.flagS;
	break;

      case 6:						// Z, EQ
	rv = this.flagZ;
	break;

      case 7:						// C, ULT
	rv = this.flagC;
	break;

      case 8:						// ohne Bedingung
	rv = true;
	break;

      case 9:						// GE
	rv = (this.flagS == this.flagV);
	break;

      case 0x0A:					// GT
	rv = !this.flagZ && (this.flagS == this.flagV);
	break;

      case 0x0B:					// UGT
	rv = (!this.flagC && !this.flagZ);
	break;

      case 0x0C:					// NOV
	rv = !this.flagV;
	break;

      case 0x0D:					// PL
	rv = !this.flagS;
	break;

      case 0x0E:					// NZ, NE
	rv = !this.flagZ;
	break;

      case 0x0F:					// NC, UGE
	rv = !this.flagC;
	break;
    }
    return rv;
  }


  private int doInstAdd( int v1, int v2, int v3 )
  {
    int m  = v1 + v2;
    int rv = m + v3;
    this.flagV = ((v1 & 0x80) == (v2 & 0x80)) && ((v1 & 0x80) != (m & 0x80));
    if( !this.flagV ) {
      this.flagV = ((m & 0x80) == (v3 & 0x80)) && ((m & 0x80) != (rv & 0x80));
    }
    this.flagC = ((rv & 0xFF00) != 0);
    this.flagZ = ((rv & 0xFF) == 0);
    this.flagS = ((rv & 0x80) != 0);
    this.flagD = false;
    this.flagH = ((((v1 & 0x0F) + (v2 & 0x0F) + (v3 & 0x0F)) & 0xF0) != 0);
    return rv & 0xFF;
  }


  private void doInstDA( int r )
  {
    if( r >= 0 ) {
      int v = getRegValue( r );
      int h = (v & 0xF0) >> 4;
      int l = v & 0x0F;
      if( this.flagD ) {
	if( !this.flagC && (h <= 8) && this.flagH && (l >= 6) ) {
	  v += 0xFA;
	  this.flagC = false;
	} else if( this.flagC && (h >= 7) && !this.flagH && (l <= 9) ) {
	  v += 0xA0;
	  this.flagC = true;
	} else if( this.flagC && (h >= 6) && this.flagH && (l >= 6) ) {
	  v += 0x9A;
	  this.flagC = true;
	} else {
	  this.flagC = false;
	}
      } else {
	if( (!this.flagC && (h <= 8) && !this.flagH && (l >= 0x0A))
	    || (!this.flagC && (h <= 9) && this.flagH && (l <= 3)) )
	{
	  v += 0x06;
	  this.flagC = false;
	} else if( (!this.flagC && (h >= 0x0A) && !this.flagH && (l <= 9))
		   || (this.flagC && (h <= 2) && !this.flagH && (l <= 9)) )
	{
	  v += 0x60;
	  this.flagC = true;
	} else if( (!this.flagC && (h >= 9) && !this.flagH && (l >= 0x0A))
		   || (!this.flagC && (h >= 0x0A) && this.flagH && (l <= 3))
		   || (this.flagC && (h <= 2) && !this.flagH && (l >= 0x0A))
		   || (this.flagC && (h <= 3) && this.flagH && (l <= 3)) )
	{
	  v += 0x66;
	  this.flagC = true;
	} else {
	  this.flagC = false;
	}
      }
      v &= 0xFF;
      this.flagZ = (v == 0);
      this.flagS = ((v & 0x80) != 0);
      setRegValue( r, v );
    }
  }


  private void doInstDEC( int r )
  {
    if( r >= 0 ) {
      int v = getRegValue( r );
      int m = (v - 1) & 0xFF;
      this.flagZ = (m == 0);
      this.flagS = ((m & 0x80) != 0);
      this.flagV = (v == 0x80);
      setRegValue( r, m );
    }
  }


  private void doInstDECW( int r )
  {
    if( (r >= 0) && ((r & 0x01) == 0) ) {
      int v = getRegWValue( r );
      int m = (v - 1) & 0xFFFF;
      this.flagZ = (m == 0);
      this.flagS = ((m & 0x8000) != 0);
      this.flagV = (v == 0x8000);
      setRegWValue( r, m );
    }
  }


  private void doInstINC( int r )
  {
    if( r >= 0 ) {
      int v = getRegValue( r );
      int m = (v + 1) & 0xFF;
      this.flagZ = (m == 0);
      this.flagS = ((m & 0x80) != 0);
      this.flagV = (v == 0x7F);
      setRegValue( r, m );
    }
  }


  private void doInstINCW( int r )
  {
    if( (r >= 0) && ((r & 0x01) == 0) ) {
      int v = getRegWValue( r );
      int m = (v + 1) & 0xFFFF;
      this.flagZ = (m == 0);
      this.flagS = ((m & 0x8000) != 0);
      this.flagV = (v == 0x7FFF);
      setRegWValue( r, m );
    }
  }


  private void doInstRL( int r )
  {
    if( r >= 0 ) {
      int v  = getRegValue( r );
      int m = v << 1;
      if( (m & 0x100) != 0 ) {
	m |= 0x01;
	this.flagC = true;
      } else {
	this.flagC = false;
      }
      m &= 0xFF;
      this.flagZ = (m == 0);
      this.flagS = ((m & 0x80) != 0);
      this.flagV = ((m & 0x80) != (v & 0x80));
      setRegValue( r, m );
    }
  }


  private void doInstRLC( int r )
  {
    if( r >= 0 ) {
      int v = getRegValue( r );
      int m = v << 1;
      if( this.flagC ) {
	m |= 0x01;
      }
      this.flagC = ((m & 0x100) != 0);
      m &= 0xFF;
      this.flagZ = (m == 0);
      this.flagS = ((m & 0x80) != 0);
      this.flagV = ((m & 0x80) != (v & 0x80));
      setRegValue( r, m );
    }
  }


  private void doInstRR( int r )
  {
    if( r >= 0 ) {
      int     v  = getRegValue( r );
      this.flagC = ((v & 0x01) != 0);
      int m = v >> 1;
      if( this.flagC ) {
	m |= 0x80;
      }
      this.flagZ = (m == 0);
      this.flagS = this.flagC;
      this.flagV = (m & 0x80) != (v & 0x80);
      setRegValue( r, m );
    }
  }


  private void doInstRRC( int r )
  {
    if( r >= 0 ) {
      int     v  = getRegValue( r );
      boolean b7 = this.flagC;
      this.flagC = ((v & 0x01) != 0);
      int m = v >> 1;
      if( b7 ) {
	m |= 0x80;
      }
      this.flagZ = (m == 0);
      this.flagS = b7;
      this.flagV = (m & 0x80) != (v & 0x80);
      setRegValue( r, m );
    }
  }


  private int doInstSub( int v1, int v2, int v3 )
  {
    int m  = v1 - v2;
    int rv = m - v3;
    this.flagV = ((v1 & 0x80) != (v2 & 0x80)) && ((m & 0x80) == (v2 & 0x80));
    if( !this.flagV ) {
      this.flagV = ((m & 0x80) != (v3 & 0x80)) && ((rv & 0x80) == (v3 & 0x80));
    }
    this.flagC = ((rv & 0xFF00) != 0);
    this.flagZ = ((rv & 0xFF) == 0);
    this.flagS = ((rv & 0x80) != 0);
    this.flagD = true;
    this.flagH = ((((v1 & 0x0F) - (v2 & 0x0F) - (v3 & 0x0F)) & 0xF0) == 0);
    return rv & 0xFF;
  }


  private void doInstSRA( int r )
  {
    if( r >= 0 ) {
      int v      = getRegValue( r );
      this.flagC = ((v & 0x01) != 0);
      this.flagS = ((v & 0x80) != 0);
      v = v >> 1;
      if( this.flagS ) {
	v |= 0x80;
      }
      this.flagZ = (v == 0);
      this.flagV = false;
      setRegValue( r, v );
    }
  }


  private void doInstSWAP( int r )
  {
    if( r >= 0 ) {
      int v = getRegValue( r );
      int m = (v >> 4) | (v << 4);
      this.flagZ = (m == 0);
      this.flagS = ((m & 0x80) != 0);
      setRegValue( r, m );
    }
  }


  private void doInstXYZ( int dst, InstType instType, int op1, int op2 )
  {
    switch( instType ) {
      case ADD:
	setRegValue( dst, doInstAdd( op1, op2, 0 ) );
	break;

      case ADC:
	setRegValue( dst, doInstAdd( op1, op2, this.flagC ? 1 : 0 ) );
	break;

      case SUB:
	setRegValue( dst, doInstSub( op1, op2, 0 ) );
	break;

      case SBC:
	setRegValue( dst, doInstSub( op1, op2, this.flagC ? 1 : 0 ) );
	break;

      case OR:
	setRegValue( dst, updFlagsSVZ( op1 | op2 ) );
	break;

      case AND:
	setRegValue( dst, updFlagsSVZ( op1 & op2 ) );
	break;

      case TCM:
	updFlagsSVZ( ~op1 & op2 );
	break;

      case TM:
	updFlagsSVZ( op1 & op2 );
	break;

      case CP:
	doInstSub( op1, op2, 0 );
	break;

      case XOR:
	setRegValue( dst, updFlagsSVZ( op1 ^ op2 ) );
	break;

    }
  }


  private int getIndirectRegIndex( int r )
  {
    if( (r & 0xF0) == 0xE0 ) {
      r = getWorkingRegIndex( r );
    }
    return r >= 0 ? getRegValue( r ) : -1;
  }


  private int getReg( int r )
  {
    if( (r & 0xF0) == 0xE0 ) {
      r = getWorkingRegIndex( r );
    }
    return getRegValue( r );
  }


  private int getRegW( int r )
  {
    if( (r & 0xF0) == 0xE0 ) {
      r = getWorkingRegIndex( r );
    }
    return getRegWValue( r );
  }


  private int getRegIndex( int r )
  {
    return (r & 0xF0) == 0xE0 ? getWorkingRegIndex( r ) : r;
  }


  private int getRegWValue( int r )
  {
    return (r & 0x01) == 0 ?
		((getRegValue( r ) << 8) | getRegValue( r + 1 ))
		: 0xFFFF;
  }


  private int getSP()
  {
    return isInternalStackEnabled() ?
		this.regSPL
		: ((this.regSPH << 8) | this.regSPL);
  }


  private int getWorkingRegIndex( int r )
  {
    return (this.regRP & 0xF0) | (r & 0x0F);
  }


  private int nextByte()
  {
    int rv  = this.memory.getMemByte( this.pc, false );
    this.pc = (this.pc + 1) & 0xFFFF;
    return rv;
  }


  private int getPortValue( int port )
  {
    int rv = 0xFF;
    if( (port >= 0) && (port < this.portIn.length) ) {
      if( this.portIn[ port ] < 0 ) {
	Z8IO z8io = this.z8io;
	if( z8io != null ) {
	  this.portIn[ port ] = z8io.getPortValue( port ) & 0xFF;
	}
      }
      rv = this.portIn[ port ];
    }
    return rv;
  }


  private void reset( boolean forceInit )
  {
    setRegValue( 3, 0xFF );
    setRegValue( TMR, 0 );
    setRegValue( P2M, 0xFF );
    setRegValue( P3M, 0 );
    setRegValue( P01M, 0x4D );
    setRegValue( IRQ, 0 );
    this.regIMR &= 0x7F;
    setRegValue( RP, 0 );
    this.pc            = 0x000C;
    this.overlapCycles = 0;
    this.resetFired    = false;
    if( forceInit ) {
      this.memory.initRAM();
    }
  }


  private void setReg( int r, int v )
  {
    if( (r & 0xF0) == 0xE0 ) {
      r = getWorkingRegIndex( r );
    }
    setRegValue( r, v );
  }


  private void setRegW( int r, int v )
  {
    if( (r & 0xF0) == 0xE0 ) {
      r = getWorkingRegIndex( r );
    }
    setRegWValue( r, v );
  }


  private void setRegWValue( int r, int v )
  {
    if( (r & 0x01) == 0 ) {
      setRegValue( r, v >> 8 );
      setRegValue( r + 1, v );
    }
  }


  private int getRegFLAGS()
  {
    int rv = this.regFLAGS & 0x03;
    if( this.flagH ) {
      rv |= 0x04;
    }
    if( this.flagD ) {
      rv |= 0x08;
    }
    if( this.flagV ) {
      rv |= 0x10;
    }
    if( this.flagS ) {
      rv |= 0x20;
    }
    if( this.flagZ ) {
      rv |= 0x40;
    }
    if( this.flagC ) {
      rv |= 0x80;
    }
    return rv;
  }


  private void updCycles( int fetchCycles, int pipelineCycles )
  {
    this.fetchCycles    = fetchCycles;
    this.pipelineCycles = pipelineCycles;
  }


  private int updFlagsSVZ( int v )
  {
    this.flagZ = (v == 0);
    this.flagS = ((v & 0x80) != 0);
    this.flagV = false;
    return v;
  }


  private void changeP36()
  {
    this.portOut[ 3 ] = (this.portOut[ 3 ] & 0xBF)
				| (~this.portLastOut[ 3 ] & 0x40);
  }


  private void updInputReg0()
  {
    int v = 0;
    if( ((this.regP01M & 0x03) == 0x01)			// P00-03: Eingang
	|| ((this.regP01M & 0xC0) == 0x40) )		// P04-07: Eingang
    {
      v = getPortValue( 0 );
    }
    switch( this.regP01M & 0x03 ) {			// P00-03
      case 0:						// Ausgang
	this.registers[ 0 ] = this.portLastOut[ 0 ] & 0x0F;
	break;

      case 0x01:					// Eingang
	this.registers[ 0 ] = v & 0x0F;
	break;

      case 0x10:					// A8-A11
      case 0x11:					// A8-A11
	this.registers[ 0 ] = (this.pc - 1) & 0x0F;
	break;
    }
    switch( this.regP01M & 0xC0 ) {			// P04-07
      case 0:						// Ausgang
	this.registers[ 0 ] |= this.portLastOut[ 0 ] & 0xF0;
	break;

      case 0x40:					// Eingang
	this.registers[ 0 ] |= v & 0xF0;
	break;

      case 0x80:					// A12-A16
      case 0xC0:					// A12-A16
	this.registers[ 0 ] |= (this.pc - 1) & 0xF0;
	break;
    }
  }


  private void updInputReg1()
  {
    switch( this.regP01M & 0x18 ) {
      case 0:						// Ausgang
	this.registers[ 1 ] = this.portLastOut[ 1 ];
	break;

      case 0x08:					// Eingabe
      case 0x18:					// hochohmig
	this.registers[ 1 ] = getPortValue( 1 );
	break;

      case 0x10:					// A0-A7
	this.registers[ 1 ] = (this.pc - 1) & 0xFF;
	break;
    }
  }


  private void updInputReg2()
  {
    /*
     * Wenn alle Bits auf Ausgang programmiert und die Ausgaenge
     * nicht hochohmig sind, hat das externe System keinen Einfluss.
     * In dem Fall wird einfach das Augangsregister gelesen.
     */
    if( (this.regP2M == 0) && ((this.regP3M & 0x01) == 0x01) ) {
      this.registers[ 2 ] = this.portLastOut[ 2 ];
    } else {
      int v = getPortValue( 2 );
      if( (this.regP3M & 0x01) == 0x01 ) {
	v = (v & this.regP2M) | (this.portLastOut[ 2 ] & ~this.regP2M);
      }
      this.registers[ 2 ] = v & 0xFF;
    }
  }


  private void updPorts()
  {
    Z8IO z8io = this.z8io;
    if( z8io != null ) {
      for( int i = 0; i < this.portOut.length; i++ ) {
	int v = this.portOut[ i ];
        if( v != this.portLastOut[ i ] ) {
	  this.portLastOut[ i ] = v;
	  z8io.setPortValue( i, v );
	}
      }
    } else {
      for( int i = 0; i < this.portOut.length; i++ ) {
	this.portLastOut[ i ] = this.portOut[ i ];
      }
    }
  }


  private boolean wentP3BitFrom1To0( int mask )
  {
    int vOld = this.port3LastIn & mask;
    int vNew = getPortValue( 3 ) & mask;
    return (vNew == 0) && (vNew != vOld);
  }
}

