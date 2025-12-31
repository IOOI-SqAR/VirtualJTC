/*
 * (c) 2007-2021 Jens Mueller
 * (c) 2017-2024 Lars Sonchocky-Helldorf
 *
 * Zilog Z8 Emulator
 */

package org.sqar.virtualjtc.z8;

import java.util.*;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Random;


public class Z8 implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(Z8.class.getName());

    public static class PCListenerItem {
        public volatile int addr;
        public volatile Z8PCListener listener;

        PCListenerItem(int addr, Z8PCListener listener) {
            this.addr = addr;
            this.listener = listener;
        }
    }


    public enum RunMode {RUNNING, INST_HALT, INST_STOP, DEBUG_STOP}

    public enum DebugAction {RUN, RUN_TO_RET, STEP_INTO, STEP_OVER, STOP}

    private enum InstType {ADD, ADC, SUB, SBC, OR, AND, TCM, TM, CP, XOR}


    /*
     * Die Tabelle dient zum Entschluesseln der Interrupt-Prioritaet.
     * Dazu werden die unteren 6 Bits des IPR als Index verwendet.
     */
    private static final int[][] iprCodings = {
            null,                        // IPR=0x00 GRP=0x00 reserviert
            {1, 4, 5, 3, 2, 0},        // IPR=0x01 GRP=0x01 C > A > B
            null,                        // IPR=0x02 GRP=0x00 reserviert
            {4, 1, 5, 3, 2, 0},        // IPR=0x03 GRP=0x01 C > A > B
            null,                        // IPR=0x04 GRP=0x00 reserviert
            {1, 4, 5, 3, 0, 2},        // IPR=0x05 GRP=0x01 C > A > B
            null,                        // IPR=0x06 GRP=0x00 reserviert
            {4, 1, 5, 3, 0, 2},        // IPR=0x07 GRP=0x01 C > A > B
            {5, 3, 2, 0, 1, 4},        // IPR=0x08 GRP=0x02 A > B > C
            {5, 3, 1, 4, 2, 0},        // IPR=0x09 GRP=0x03 A > C > B
            {5, 3, 2, 0, 4, 1},        // IPR=0x0A GRP=0x02 A > B > C
            {5, 3, 4, 1, 2, 0},        // IPR=0x0B GRP=0x03 A > C > B
            {5, 3, 0, 2, 1, 4},        // IPR=0x0C GRP=0x02 A > B > C
            {5, 3, 1, 4, 0, 2},        // IPR=0x0D GRP=0x03 A > C > B
            {5, 3, 0, 2, 4, 1},        // IPR=0x0E GRP=0x02 A > B > C
            {5, 3, 4, 1, 0, 2},        // IPR=0x0F GRP=0x03 A > C > B
            {2, 0, 1, 4, 5, 3},        // IPR=0x10 GRP=0x04 B > C > A
            {1, 4, 2, 0, 5, 3},        // IPR=0x11 GRP=0x05 C > B > A
            {2, 0, 4, 1, 5, 3},        // IPR=0x12 GRP=0x04 B > C > A
            {4, 1, 2, 0, 5, 3},        // IPR=0x13 GRP=0x05 C > B > A
            {0, 2, 1, 4, 5, 3},        // IPR=0x14 GRP=0x04 B > C > A
            {1, 4, 0, 2, 5, 3},        // IPR=0x15 GRP=0x05 C > B > A
            {0, 2, 4, 1, 5, 3},        // IPR=0x16 GRP=0x04 B > C > A
            {4, 1, 0, 2, 5, 3},        // IPR=0x17 GRP=0x05 C > B > A
            {2, 0, 5, 3, 1, 4},        // IPR=0x18 GRP=0x06 B > A > C
            null,                        // IPR=0x19 GRP=0x07 reserviert
            {2, 0, 5, 3, 4, 1},        // IPR=0x1A GRP=0x06 B > A > C
            null,                        // IPR=0x1B GRP=0x07 reserviert
            {0, 2, 5, 3, 1, 4},        // IPR=0x1C GRP=0x06 B > A > C
            null,                        // IPR=0x1D GRP=0x07 reserviert
            {0, 2, 5, 3, 4, 1},        // IPR=0x1E GRP=0x06 B > A > C
            null,                        // IPR=0x1F GRP=0x07 reserviert
            null,                        // IPR=0x20 GRP=0x00 reserviert
            {1, 4, 3, 5, 2, 0},        // IPR=0x21 GRP=0x01 C > A > B
            null,                        // IPR=0x22 GRP=0x00 reserviert
            {4, 1, 3, 5, 2, 0},        // IPR=0x23 GRP=0x01 C > A > B
            null,                        // IPR=0x24 GRP=0x00 reserviert
            {1, 4, 3, 5, 0, 2},        // IPR=0x25 GRP=0x01 C > A > B
            null,                        // IPR=0x26 GRP=0x00 reserviert
            {4, 1, 3, 5, 0, 2},        // IPR=0x27 GRP=0x01 C > A > B
            {3, 5, 2, 0, 1, 4},        // IPR=0x28 GRP=0x02 A > B > C
            {3, 5, 1, 4, 2, 0},        // IPR=0x29 GRP=0x03 A > C > B
            {3, 5, 2, 0, 4, 1},        // IPR=0x2A GRP=0x02 A > B > C
            {3, 5, 4, 1, 2, 0},        // IPR=0x2B GRP=0x03 A > C > B
            {3, 5, 0, 2, 1, 4},        // IPR=0x2C GRP=0x02 A > B > C
            {3, 5, 1, 4, 0, 2},        // IPR=0x2D GRP=0x03 A > C > B
            {3, 5, 0, 2, 4, 1},        // IPR=0x2E GRP=0x02 A > B > C
            {3, 5, 4, 1, 0, 2},        // IPR=0x2F GRP=0x03 A > C > B
            {2, 0, 1, 4, 3, 5},        // IPR=0x30 GRP=0x04 B > C > A
            {1, 4, 2, 0, 3, 5},        // IPR=0x31 GRP=0x05 C > B > A
            {2, 0, 4, 1, 3, 5},        // IPR=0x32 GRP=0x04 B > C > A
            {4, 1, 2, 0, 3, 5},        // IPR=0x33 GRP=0x05 C > B > A
            {0, 2, 1, 4, 3, 5},        // IPR=0x34 GRP=0x04 B > C > A
            {1, 4, 0, 2, 3, 5},        // IPR=0x35 GRP=0x05 C > B > A
            {0, 2, 4, 1, 3, 5},        // IPR=0x36 GRP=0x04 B > C > A
            {4, 1, 0, 2, 3, 5},        // IPR=0x37 GRP=0x05 C > B > A
            {2, 0, 3, 5, 1, 4},        // IPR=0x38 GRP=0x06 B > A > C
            null,                        // IPR=0x39 GRP=0x07 reserviert
            {2, 0, 3, 5, 4, 1},        // IPR=0x3A GRP=0x06 B > A > C
            null,                        // IPR=0x3B GRP=0x07 reserviert
            {0, 2, 3, 5, 1, 4},        // IPR=0x3C GRP=0x06 B > A > C
            null,                        // IPR=0x3D GRP=0x07 reserviert
            {0, 2, 3, 5, 4, 1},        // IPR=0x3E GRP=0x06 B > A > C
            null,                        // IPR=0x3F GRP=0x07 reserviert
    };


    // Maskierungen der Interrupts in IMR und IRQ
    private static final int[] interruptMasks
            = {0x01, 0x02, 0x04, 0x08, 0x10, 0x20};


    private static final long cyclesWrap = Long.MAX_VALUE / 1100L;
    private static final int SPL = 0xFF;
    private static final int SPH = 0xFE;
    private static final int RP = 0xFD;
    private static final int FLAGS = 0xFC;
    private static final int IMR = 0xFB;
    private static final int IRQ = 0xFA;
    private static final int IPR = 0xF9;
    private static final int P01M = 0xF8;
    private static final int P3M = 0xF7;
    private static final int P2M = 0xF6;
    private static final int PRE0 = 0xF5;
    private static final int T0 = 0xF4;
    private static final int PRE1 = 0xF3;
    private static final int T1 = 0xF2;
    private static final int TMR = 0xF1;
    private static final int SIO = 0xF0;

    private volatile Z8IO z8io = null;
    private volatile Z8Listener preInstExecListener = null;
    private volatile Z8Listener resetListener = null;
    private volatile Z8Listener statusListener = null;
    private volatile PCListenerItem[] pcListenerItems = null;
    private volatile int pc = 0;
    private volatile int debugSP = 0;
    private volatile int cyclesPerSecond = 0;
    private volatile long unlimitedSpeedCycles = -1;
    private volatile long cyclesStartMillis = -1;
    private volatile long speedCycles = 0;
    private volatile long totalCycles = 0;
    private int instCycles = 0;
    private int sioPreDiv = 0;
    private int sioIn = 0;
    private int sioIn1Bits = 0;
    private int sioInShift = 0;
    private int sioInShiftNum = 0;
    private int sioOutShift = 0;
    private int regSPL = 0;
    private int regSPH = 0;
    private int regRP = 0;
    private int regFLAGS = 0;
    private int regIMR = 0;
    private int regIRQ = 0;
    private int regIPR = 0;
    private int regP01M = 0;
    private int regP3M = 0;
    private int regP2M = 0;
    private int regTMR = 0;
    private final int maxUB883XGPRNum = 0x7F;
    private int maxGPRNum = 0xEF;
    private final int[] registers = new int[0xF0];
    private final int[] regOut = new int[4];
    private final int[] portIn = new int[4];
    private final int[] portOut = new int[4];
    private final int[] portLastOut = new int[4];
    private int port3LastIn = 0xFF;
    private volatile Z8Breakpoint[] breakpoints = null;
    private boolean flagC = false;
    private boolean flagD = false;
    private boolean flagH = false;
    private boolean flagS = false;
    private boolean flagV = false;
    private boolean flagZ = false;
    private int[] interruptPriority = null;
    private volatile boolean eiExecuted = false;
    private volatile boolean regInitZero = false;
    private boolean timer1ExtClock = false;
    private volatile boolean powerOn = false;
    private volatile boolean resetFired = false;
    private volatile boolean quitFired = false;
    private final Z8Timer timer0 = new Z8Timer();
    private final Z8Timer timer1 = new Z8Timer();
    private Z8Memory memory = null;
    private Random random = null;
    private volatile Z8Debugger debugger = null;
    private volatile DebugAction debugAction = null;
    private volatile RunMode runMode = RunMode.RUNNING;
    private final Object waitMonitor = new Object();


    public Z8(boolean regInitZero, Z8Memory memory, Z8IO z8io) {
        this.regInitZero = regInitZero;
        this.memory = memory;
        this.z8io = z8io;
        this.random = new Random(System.currentTimeMillis());
        this.cyclesPerSecond = 0;
        Arrays.fill(this.regOut, 0xFF);
        Arrays.fill(this.portIn, -1);
        Arrays.fill(this.portOut, 0xFF);
        Arrays.fill(this.portLastOut, 0xFF);
        reset(true);
    }


    public synchronized void addPCListener(int addr, Z8PCListener listener) {
        PCListenerItem[] items;
        if (this.pcListenerItems != null) {
            items = new PCListenerItem[this.pcListenerItems.length + 1];
            System.arraycopy(this.pcListenerItems, 0, items, 0, this.pcListenerItems.length);
        } else {
            items = new PCListenerItem[1];
        }
        items[items.length - 1] = new PCListenerItem(addr, listener);
        this.pcListenerItems = items;
    }


    public synchronized void removePCListener(int addr, Z8PCListener listener) {
        if (this.pcListenerItems != null) {
            java.util.List<PCListenerItem> list = new ArrayList<>(
                    this.pcListenerItems.length);
            boolean changed = false;
            for (PCListenerItem item : this.pcListenerItems) {
                if ((item.addr == addr) && (item.listener == listener)) {
                    changed = true;
                } else {
                    list.add(item);
                }
            }
            if (changed) {
                int n = list.size();
                if (n > 0) {
                    this.pcListenerItems = list.toArray(new PCListenerItem[n]);
                } else {
                    this.pcListenerItems = null;
                }
            }
        }
    }


    public void fireQuit() {
        this.quitFired = true;
        synchronized (this.waitMonitor) {
            if (this.runMode != RunMode.RUNNING) {
                try {
                    this.runMode = RunMode.RUNNING;
                    this.waitMonitor.notifyAll();
                } catch (IllegalMonitorStateException ignored) {
                }
            }
        }
    }


    public synchronized void fireReset(boolean powerOn) {
        this.powerOn = powerOn;
        this.resetFired = true;
        this.debugAction = DebugAction.RUN;
        synchronized (this.waitMonitor) {
            if (this.runMode != RunMode.RUNNING) {
                try {
                    this.runMode = RunMode.RUNNING;
                    this.waitMonitor.notifyAll();
                } catch (IllegalMonitorStateException ignored) {
                }
            }
        }
    }


    public int getCyclesPerSecond() {
        return this.cyclesPerSecond;
    }


    public Double getEmulatedMHz() {
        Double mhz = null;
        long millis = 0;
        long cycles = 0;
        synchronized (this) {
            millis = this.cyclesStartMillis;
            cycles = this.speedCycles;
        }
        millis = System.currentTimeMillis() - millis;
        if (millis > 0) {
            mhz = (double) cycles / ((double) (millis * 1000));
        }
        return mhz;
    }


    public int getMaxGPRNum() {
        return this.maxGPRNum;
    }


    public int getMemByte(int addr, boolean dataMemory) {
        return this.memory.getMemByte(addr, dataMemory);
    }


    public Z8Memory getMemory() {
        return this.memory;
    }


    public int getPC() {
        return this.pc;
    }


    public int getRegNum(int r) {
        return (r & 0xF0) == 0xE0 ? getWorkingRegNum(r) : r;
    }


    public int getRegRPValueMask() {
        return 0xF0;
    }


    public int getRegValue(int r) {
        int rv = 0xFF;
        if ((r >= 0) && (r < this.registers.length) && (r <= this.maxGPRNum)) {
            switch (r) {
                case 0:
                    if (((this.regP3M & 0x04) == 0x04)        // Handshake
                            && ((this.regP01M & 0x03) == 0x01))    // Eingang
                    {
                        this.portOut[3] |= 0x20;            // P35=1
                    } else {
                        updInputReg0();
                    }
                    break;

                case 1:
                    if (((this.regP3M & 0x18) == 0x18)        // Handshake
                            && ((this.regP01M & 0x18) == 0x08))    // Eingang
                    {
                        this.portOut[3] |= 0x10;            // P34=1
                    } else {
                        updInputReg1();
                    }
                    break;

                case 2:
                    if (((this.regP3M & 0x20) == 0x20)        // Handshake
                            && ((this.regP2M & 0x80) == 0x80))    // Eingang
                    {
                        this.portOut[3] |= 0x40;            // P36=1
                    } else {
                        updInputReg2();
                    }
                    break;

                case 3:
                    this.registers[3] = (this.portOut[3] & 0xF0)
                            | (getPortValue(3) & 0x0F);
                    break;
            }
            rv = this.registers[r];
            if (r > maxUB883XGPRNum) {
                // accessing upper register, probably something is going wrong here (the UB883X doesn't has them)
                Object[] params = {Integer.toHexString(r), Integer.toHexString(rv), Integer.toHexString(this.pc)};
                LOGGER.log(Level.INFO, "Reading value 0x{1} from register 0x{0}, at PC 0x{2}", params);
            }
        } else {
            rv = switch (r) {
                case SIO -> this.sioIn;
                case TMR -> this.regTMR;
                case T1 -> this.timer1.getCounter();
                case PRE1 ->        // Write Only Register
                        0xFF;
                case T0 -> this.timer0.getCounter();        // Write Only Register
                // Write Only Register
                // Write Only Register
                // Write Only Register
                case PRE0, P2M, P3M, P01M, IPR ->        // Write Only Register
                        0xFF;
                case IRQ -> this.regIRQ;
                case IMR -> this.regIMR;
                case FLAGS -> getRegFLAGS();
                case RP -> this.regRP;
                case SPH -> this.regSPH;
                case SPL -> this.regSPL;
                default ->
                    /*
                     * Lesen eines nicht vorhandenen Registers:
                     *   Nicht immer, aber haeufig wird die Registeradresse
                     *   (Registernummer) zurueckgeliefert.
                     *   Es ist aber nicht klar, in welchen Faellen
                     *   die Registernummer oder ein anderer Wert gelesen wird.
                     *   Aus diesem Grund wird hier der Einfachheit halber
                     *   immer die Registernummer zurueckgeliefert.
                     */
                        r;
            };
        }
        return rv;
    }


    public int getRegWValue(int r) {
        r &= 0xFE;            // Bit 0 der Registeradresse ignorieren
        return ((getRegValue(r) << 8) | getRegValue(r + 1));
    }


    public int getSP() {
        return isInternalStackEnabled() ?
                this.regSPL
                : ((this.regSPH << 8) | this.regSPL);
    }


    public RunMode getRunMode() {
        RunMode runMode;
        synchronized (this.waitMonitor) {
            runMode = this.runMode;
        }
        return runMode;
    }


    public long getTotalCycles() {
        return this.totalCycles;
    }


    public int getWorkingRegNum(int r) {
        return (this.regRP & 0xF0) | (r & 0x0F);
    }


    public boolean isInternalStackEnabled() {
        return ((this.regP01M & 0x04) != 0);
    }


    public boolean isPause() {
        return getRunMode() != RunMode.RUNNING;
    }


    public boolean isRegInitZero() {
        return this.regInitZero;
    }


    public int pop() {
        int rv = 0;
        if (isInternalStackEnabled()) {
            int a = this.regSPL;
            rv = getRegValue(a);
            this.regSPL = (a + 1) & 0xFF;
        } else {
            int a = (this.regSPH << 8) | this.regSPL;
            rv = this.memory.getMemByte(a++, true);
            this.regSPH = (a >> 8) & 0xFF;
            this.regSPL = a & 0xFF;
        }
        return rv;
    }


    public int popw() {
        int h = pop();
        return (h << 8) | pop();
    }


    public void push(int v) {
        if (isInternalStackEnabled()) {
            int a = this.regSPL;
            setRegValue(--a, v);
            this.regSPL = a & 0xFF;
        } else {
            int a = (((this.regSPH << 8) | this.regSPL) - 1) & 0xFFFF;
            this.memory.setMemByte(a, true, v);
            this.regSPH = a >> 8;
            this.regSPL = a & 0xFF;
        }
    }


    public void pushw(int v) {
        push(v);
        push(v >> 8);
    }


    public synchronized void resetSpeed() {
        this.speedCycles = 0;
        this.cyclesStartMillis = System.currentTimeMillis();
        this.unlimitedSpeedCycles = -1;
    }


    public void resetTotalCycles() {
        this.totalCycles = 0;
    }


    public void setBreakpoints(Z8Breakpoint[] breakpoints) {
        this.breakpoints = breakpoints;
    }


    public void setCyclesPerSecond(int cycles) {
        boolean done = false;
        synchronized (this) {
            if (cycles != this.cyclesPerSecond) {
                this.cyclesPerSecond = cycles;
                resetSpeed();
                done = true;
            }
        }
        Z8Listener listener = this.statusListener;
        if (listener != null) {
            listener.z8Update(this, Z8Listener.Reason.CYCLES_PER_SECOND_CHANGED);
        }
    }


    public synchronized void setDebugAction(DebugAction debugAction) {
        this.debugAction = debugAction;
        if (getRunMode() == RunMode.DEBUG_STOP) {
            this.debugSP = getSP();
            if ((debugAction != DebugAction.STOP)) {
                synchronized (this.waitMonitor) {
                    try {
                        this.waitMonitor.notifyAll();
                    } catch (IllegalMonitorStateException ignored) {
                    }
                }
            }
        } else {
            this.debugSP = -1;
        }
    }


    public void setDebugger(Z8Debugger debugger) {
        this.debugger = debugger;
    }


    public synchronized void setMaxGPRNum(int maxGPRNum) {
        if (maxGPRNum != this.maxGPRNum) {
            this.maxGPRNum = maxGPRNum;
            statusChanged();
        }
    }


    public void setPC(int addr) {
        this.pc = addr;
    }


    public synchronized void setPause(boolean state) {
        if (state) {
            if (!isPause()) {
                this.runMode = RunMode.DEBUG_STOP;
                setDebugAction(DebugAction.STOP);
            }
        } else {
            if (isPause()) {
                this.runMode = RunMode.RUNNING;
                setDebugAction(DebugAction.RUN);
            }
        }
    }


    public void setPreInstExecListener(Z8Listener listener) {
        this.preInstExecListener = listener;
    }


    public void setRegInitZero(boolean state) {
        this.regInitZero = state;
    }


    public void setRegValue(int r, int v) {
        v &= 0xFF;
        if ((r >= 0) && (r < this.regOut.length)) {
            this.regOut[r] = v;
            int portOut = this.portOut[r];
            switch (r) {
                case 0:
                    if ((this.regP01M & 0xC0) == 0) {        // P04-07: Ausgang
                        portOut = (v & 0xF0) | (portOut & 0x0F);
                    }
                    if ((this.regP01M & 0x03) == 0) {        // P00-03: Ausgang
                        portOut = (portOut & 0xF0) | (v & 0x0F);
                    }
                    if (((this.regP01M & 0xC0) == 0)        // P04-07: Ausgang
                            && ((this.regP3M & 0x04) != 0)        // Port 0: Handshake
                            && ((getPortValue(3) & 0x04) != 0))    // P32=1
                    {
                        this.portOut[3] &= 0xDF;            // P35=0
                    }
                    break;

                case 1:
                    if ((this.regP01M & 0x18) == 0) {        // Port 1: Ausgang
                        portOut = v;
                        if (((this.regP3M & 0x18) == 0x18)        // Port 1: Handshake
                                && ((getPortValue(3) & 0x08) != 0))    // P33=1
                        {
                            this.portOut[3] &= 0xEF;        // P34=0
                        }
                    }
                    break;

                case 2:
                    portOut = ((portOut & this.regP2M) | (v & ~this.regP2M)) & 0xFF;
                    if (((this.regP2M & 0x80) == 0)        // Port 2: Ausgang
                            && ((this.regP3M & 0x20) != 0)        // Port 2: Handshake
                            && ((getPortValue(3) & 0x02) != 0))    // P31=1
                    {
                        this.portOut[3] &= 0xBF;            // P36=0
                    }
                    break;

                case 3:
                    portOut = (portOut & 0xF0) | (this.port3LastIn & 0x0F);
                    if ((this.regP3M & 0x18) == 0) {
                        portOut = (portOut & 0xEF) | (v & 0x10);    // P34: Ausgang
                    }
                    if ((this.regP3M & 0x04) == 0) {
                        portOut = (portOut & 0xDF) | (v & 0x20);    // P35: Ausgang
                    }
                    if ((this.regP3M & 0x20) == 0) {
                        portOut = (portOut & 0xBF) | (v & 0x40);    // P36: Ausgang
                    }
                    if ((this.regP3M & 0x80) == 0) {
                        portOut = (portOut & 0x7F) | (v & 0x80);    // P37: Ausgang
                    }
                    break;
            }
            this.portOut[r] = portOut;
        } else if ((r >= this.regOut.length)
                && (r < this.registers.length)
                && (r <= this.maxGPRNum)) {
            if (r > maxUB883XGPRNum) {
                // accessing upper register, probably something is going wrong here (the UB883X doesn't has them)
                Object[] params = {Integer.toHexString(r), Integer.toHexString(v), Integer.toHexString(this.pc)};
                LOGGER.log(Level.INFO, "Writing value 0x{1} to register 0x{0}, at PC 0x{2}", params);
            }
            this.registers[r] = v;
        } else {
            switch (r) {
                case SPL:
                    this.regSPL = v;
                    break;

                case SPH:
                    this.regSPH = v;
                    break;

                case RP:
                    this.regRP = (v & getRegRPValueMask());
                    break;

                case FLAGS:
                    this.regFLAGS = v;
                    this.flagH = ((v & 0x04) != 0);
                    this.flagD = ((v & 0x08) != 0);
                    this.flagV = ((v & 0x10) != 0);
                    this.flagS = ((v & 0x20) != 0);
                    this.flagZ = ((v & 0x40) != 0);
                    this.flagC = ((v & 0x80) != 0);
                    break;

                case IMR:
                    this.regIMR = v;
                    break;

                case IRQ:
                    if (this.eiExecuted) {
                        this.regIRQ = v;
                    }
                    break;

                case IPR: {
                    int[] iPriority = iprCodings[v & 0x3F];
                    if (iPriority != null) {
                        /*
                         * Bei einem ungueltigen Wert ist das Verhalten des Z8
                         * nicht immer gleich. Haeufig bleibt aber
                         * die alte Interrupt-Prioritaet erhalten.
                         * Aus diesem Grund wird das auch hier so emuliert.
                         */
                        this.interruptPriority = iPriority;
                    }
                    this.regIPR = v;
                }
                break;

                case P01M:
                    this.regP01M = v;
                    break;

                case P3M:
                    if (((this.regP3M & 0x40) == 0)
                            && ((v & 0x40) != 0)) {
                        this.sioInShiftNum = 0;    // SIO aktivieren -> zuruecksetzen
                        this.sioOutShift = 0;
                    }
                    this.regP3M = v;
                    break;

                case P2M:
                    this.regP2M = v;
                    break;

                case PRE0:
                    this.timer0.setPreCounter(v);
                    this.regTMR |= 0x01;
                    break;

                case T0:
                    this.timer0.setCounter(v);
                    this.regTMR |= 0x01;
                    break;

                case PRE1:
                    this.timer1.setPreCounter(v);
                    this.timer1ExtClock = ((v & 0x02) == 0);
                    this.regTMR |= 0x04;
                    break;

                case T1:
                    this.timer1.setCounter(v);
                    this.regTMR |= 0x04;
                    break;

                case TMR:
                    this.regTMR = v;
                    break;

                case SIO:
                    if ((this.regP3M & 0x40) != 0) {
                        if ((this.regP3M & 0x80) != 0) {
                            int n = 0;
                            int m = v;
                            for (int i = 0; i < 7; i++) {
                                if ((m & 0x01) != 0) {
                                    n++;
                                }
                                m >>= 1;
                            }
                            v = ((m << 7) & 0x80) | (v & 0x7F);
                        }

                        // 1 Start- und 2 Stop-Bits hinzufuegen
                        this.sioOutShift = 0x600 | (v << 1);

                        // vor Start-Bit muss 1-Pegel ausgegeben werden
                        if ((this.portLastOut[3] & 0x80) == 0) {
                            this.sioOutShift = (this.sioOutShift << 1) | 0x01;
                        }
                    }
                    break;
            }
        }
    }


    public void setStatusListener(Z8Listener listener) {
        this.statusListener = listener;
    }


    public void setResetListener(Z8Listener listener) {
        this.resetListener = listener;
    }


    /*
     * Diese Methode schaltet die Geschwindigkeitsbremse
     * fuer die Dauer der uebergebenen Anzahl an internen Z8-Taktzyklen aus.
     */
    public void setSpeedUnlimitedFor(int unlimitedSpeedCycles) {
        this.unlimitedSpeedCycles = this.speedCycles
                + (long) unlimitedSpeedCycles;
    }

    /*
     * Die Methode liest den Inhalt eines Registers
     * fuer die optische Anzeige.
     * Bei nicht vorhandenen Registern wird -1 zurueckgeliefert.
     */
    public int viewRegValue(int r) {
        int rv = 0xFF;
        if ((r >= 0) && (r < this.registers.length) && (r <= this.maxGPRNum)) {
            rv = this.registers[r];
        } else {
            rv = switch (r) {
                case SPL -> this.regSPL;
                case SPH -> this.regSPH;
                case RP -> this.regRP;
                case FLAGS -> getRegFLAGS();
                case IMR -> this.regIMR;
                case IRQ -> this.regIRQ;
                case IPR -> this.regIPR;
                case P01M -> this.regP01M;
                case P3M -> this.regP3M;
                case P2M -> this.regP2M;
                case PRE0 -> this.timer0.getPreCounter();
                case T0 -> this.timer0.getCounter();
                case PRE1 -> this.timer1.getPreCounter();
                case T1 -> this.timer1.getCounter();
                case TMR -> this.regTMR;
                case SIO -> this.sioIn;
                default -> rv;
            };
        }
        return rv;
    }


    public boolean wasQuitFired() {
        return this.quitFired;
    }


    /* --- Runnable --- */

    @Override
    public void run() {
        long cyclesSinceAdjust = 0;
        this.cyclesStartMillis = System.currentTimeMillis();
        this.instCycles = 0;
        this.speedCycles = 0;
        this.totalCycles = 0;
        while (!this.quitFired) {
            this.totalCycles += this.instCycles;

            // Geschwindigkeit
            cyclesSinceAdjust += this.instCycles;
            this.speedCycles += this.instCycles;
            if ((this.unlimitedSpeedCycles < this.speedCycles)
                    && (cyclesSinceAdjust > 10000)) {
                long usedMillis = 0;
                synchronized (this) {
                    usedMillis = System.currentTimeMillis() - this.cyclesStartMillis;
                }
                int cyclesPerSecond = this.cyclesPerSecond;
                if (cyclesPerSecond > 0) {
                    long plannedMillis = 1000L * this.speedCycles / cyclesPerSecond;
                    long millisToWait = plannedMillis - usedMillis;
                    if (millisToWait > 10) {
                        try {
                            Thread.sleep(Math.min(millisToWait, 50));
                        } catch (InterruptedException ignored) {
                        }
                    }
                    cyclesSinceAdjust = 0;
                }
            }

            // bei HALT warten
            synchronized (this.waitMonitor) {
                while (this.runMode == RunMode.INST_HALT) {
                    Z8Debugger debugger = this.debugger;
                    if (debugger != null) {
                        debugger.z8DebugStatusChanged(this);
                    }
                    try {
                        this.waitMonitor.wait();
                    } catch (IllegalMonitorStateException | InterruptedException ignored) {
                    }
                    this.runMode = RunMode.RUNNING;
                    this.totalCycles = 0;
                    if (debugger != null) {
                        debugger.z8DebugStatusChanged(this);
                    }
                }
            }

            // Reset?
            if (this.resetFired) {
                reset(this.powerOn);
            }

            // Status der Eingangsports zuruecksetzen
            Arrays.fill(this.portIn, -1);

            // Zwischenspeicher fuer Ausgangsports aktualisieren
            System.arraycopy(this.portLastOut, 0, this.portOut, 0, this.portOut.length);

            // P30: 1->0 pruefen
            if (((this.regP3M & 0x40) == 0) && wentP3BitFrom1To0(0x01)) {
                this.regIRQ |= 0x08;            // IRQ3, wenn SIO inaktiv
            }

            // P31: 1->0 pruefen
            boolean p31From1To0 = wentP3BitFrom1To0(0x02);
            if (p31From1To0) {
                if ((this.regP3M & 0x20) == 0x20) {        // Handshake Port 2
                    if ((this.regP2M & 0x80) == 0x80) {        // Eingang
                        if ((this.portLastOut[3] & 0x40) == 0x40) {
                            updInputReg2();                // P36=1: uebernehmen
                            this.portOut[3] &= ~0x40;        // P36=0
                            this.regIRQ |= 0x04;            // IRQ2
                        }
                    } else {                    // Ausgang
                        this.portOut[3] |= 0x40;            // P36=1
                        this.regIRQ |= 0x04;            // IRQ2
                    }
                } else {
                    this.regIRQ |= 0x04;                // IRQ2
                }
            }

            // P32: 1->0 pruefen
            if (wentP3BitFrom1To0(0x04)) {
                if ((this.regP3M & 0x04) == 0x04) {        // Handshake Port 0
                    if ((this.regP01M & 0x03) == 0x01) {        // Eingang
                        if ((this.portLastOut[3] & 0x20) == 0x20) {
                            updInputReg0();                // P35=1: uebernehmen
                            this.portOut[3] &= ~0x20;        // P35=0
                            this.regIRQ |= 0x01;            // IRQ0
                        }
                    } else {                    // Ausgang
                        this.portOut[3] |= 0x20;            // P35=1
                        this.regIRQ |= 0x01;            // IRQ0
                    }
                } else {
                    this.regIRQ |= 0x01;                // IRQ0
                }
            }

            // P33: 1->0 pruefen
            if (wentP3BitFrom1To0(0x08)) {
                if ((this.regP3M & 0x18) == 0x18) {        // Handshake Port 1
                    if ((this.regP01M & 0x18) == 0x08) {        // Eingang
                        if ((this.portLastOut[3] & 0x10) == 0x10) {
                            updInputReg1();                // P34=1: uebernehmen
                            this.portOut[3] &= ~0x10;        // P34=0
                            this.regIRQ |= 0x02;            // IRQ1
                        }
                    } else {                    // Ausgang
                        this.portOut[3] |= 0x10;            // P34=1
                        this.regIRQ |= 0x02;            // IRQ1
                    }
                } else {
                    this.regIRQ |= 0x02;                // IRQ1
                }
            }

            // Bei STOP Timer laufen lassen und auf Interrupt pruefen
            if (this.runMode == RunMode.INST_STOP) {
                this.instCycles = 4;
            } else if (this.runMode == RunMode.RUNNING) {

                // Debug?
                Z8Debugger debugger = this.debugger;
                if (debugger != null) {
                    Z8.DebugAction debugAction = null;
                    Z8Breakpoint[] breakpoints = null;
                    int pc = 0;
                    synchronized (this) {
                        debugAction = this.debugAction;
                        breakpoints = this.breakpoints;
                        pc = this.pc;
                    }
                    boolean reqStop = false;
                    if (breakpoints != null) {
                        for (Z8Breakpoint breakpoint : breakpoints) {
                        if (breakpoint.matches(this)) {
                                reqStop = true;
                                break;
                            }
                        }
                    }
                    if (!reqStop) {
                        if (debugAction != null) {
                            switch (debugAction) {
                                case RUN_TO_RET:
                                    int opc = this.memory.getMemByte(pc, false);
                                    if (((opc == 0xAF) || (opc == 0xBF))
                                            && (getSP() >= this.debugSP)) {
                                        reqStop = true;
                                    }
                                    break;

                                case STEP_OVER:
                                    if (getSP() >= this.debugSP) {
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
                    if (reqStop) {
                        synchronized (this.waitMonitor) {
                            this.runMode = RunMode.DEBUG_STOP;
                            debugger.z8DebugStatusChanged(this);
                            try {
                                this.waitMonitor.wait();
                            } catch (IllegalMonitorStateException | InterruptedException ignored) {
                            }
                            this.runMode = RunMode.RUNNING;
                            this.totalCycles = 0;
                            debugger.z8DebugStatusChanged(this);
                        }
                        synchronized (this) {
                            debugAction = this.debugAction;
                        }
                        if (debugAction == DebugAction.STEP_OVER) {
                            int opc = this.memory.getMemByte(this.pc, false);
                            if ((opc != 0xD4) && (opc != 0xD6))
                                this.debugAction = DebugAction.STEP_INTO;
                        }
                    }
                }

                // Listener vor Ausfuehrung eines Befehls
                Z8Listener listener = this.preInstExecListener;
                if (listener != null) {
                    listener.z8Update(this, Z8Listener.Reason.PRE_INST_EXEC);
                }

                // PC-Listener?
                PCListenerItem[] pcListenerItems = this.pcListenerItems;
                if (pcListenerItems != null) {
                    for (PCListenerItem pcListenerItem : pcListenerItems) {
                        if (pcListenerItem.addr == Z8PCListener.ALL_ADDRESSES) {
                            pcListenerItem.listener.z8PCUpdate(this, this.pc);
                        } else if (pcListenerItem.addr == this.pc) {
                            pcListenerItem.listener.z8PCUpdate(this, this.pc);
                        }
                    }
                }

                // Befehl ausfuehren
                this.instCycles = 0;
                execNextInst();
            }

            /*
             * Waehrend des Lesens des Befehls wird auf Interrupt geprueft,
             * d.h., der aktuelle Befehl wird noch ausgefuehrt.
             * Deshalb erfolgt im Emulator die Interrupt-Pruefung erst
             * nach Befehlsausfuehrung
             */
            if (((this.regIMR & 0x80) != 0)
                    && ((this.regIRQ & this.regIMR & 0x3F) != 0)) {
                int[] iPriority = this.interruptPriority;
                if (iPriority != null) {
                    for (int irq : iPriority) {
                        if ((irq >= 0) && (irq < interruptMasks.length)) {
                            int m = interruptMasks[irq];
                            if ((this.regIRQ & this.regIMR & m) != 0) {
                                pushw(this.pc);
                                push(getRegValue(FLAGS));
                                int v = irq * 2;
                                this.pc = (this.memory.getMemByte(v, false) << 8)
                                        | this.memory.getMemByte(v + 1, false);
                                this.regIRQ &= ~m;
                                this.regIMR &= 0x7F;
                                this.instCycles = 6;
                                break;
                            }
                        }
                    }
                }
            }

            // Timer aktualisieren
            boolean sioPulse = false;
            if ((this.regTMR & 0x02) != 0) {
                if (this.timer0.update(this.instCycles)) {
                    if ((this.regP3M & 0x40) != 0) {
                        sioPulse = true;
                    } else {
                        this.regIRQ |= 0x10;
                    }
                    if ((this.regTMR & 0xC0) == 0x40) {
                        changeP36();
                    }
                }
            }
            if ((this.regTMR & 0x08) != 0) {
                int t1Cycles = 0;
                if (this.timer1ExtClock) {
                    if ((this.regP3M & 0x20) == 0) {    // P31: kein Handshake (Port 2)
                        switch (this.regTMR & 0x30) {
                            case 0x00:            // P31: externe Taktquelle
                                if (p31From1To0) {
                                    t1Cycles = 1;
                                }
                                break;

                            case 0x10:            // P31: Tor
                                if ((getPortValue(3) & 0x02) != 0) {
                                    t1Cycles = this.instCycles;
                                }
                                break;

                            case 0x20:            // Trigger, nicht retriggerbar
                                if ((this.timer1.getCounter() == 0) && p31From1To0) {
                                    this.regTMR |= 0x04;
                                }
                                break;

                            case 0x30:            // Trigger, retriggerbar
                                if (p31From1To0) {
                                    this.regTMR |= 0x04;
                                }
                                break;
                        }
                    }
                } else {
                    t1Cycles = this.instCycles;
                }
                if (t1Cycles > 0) {
                    if (this.timer1.update(t1Cycles)) {
                        this.regIRQ |= 0x20;
                        if ((this.regTMR & 0xC0) == 0x80) {
                            changeP36();
                        }
                    }
                }
            }
            if ((this.regTMR & 0x01) != 0) {
                this.timer0.init();
                this.regTMR &= ~0x01;
            }
            if ((this.regTMR & 0x04) != 0) {
                this.timer1.init();
                this.regTMR &= ~0x04;
            }

            // SIO
            if (sioPulse && (this.regP3M & 0x40) != 0) {
                // 1:16-Teilung
                if (this.sioPreDiv < 15) {
                    this.sioPreDiv++;
                } else {
                    this.sioPreDiv = 0;
                    if (this.sioOutShift != 0) {
                        int v = this.portOut[3] & 0x7F;
                        if ((this.sioOutShift & 0x01) != 0) {
                            v |= 0x80;
                        }
                        this.portOut[3] = v;
                        this.sioOutShift >>= 1;
                        if (this.sioOutShift == 0) {
                            this.regIRQ |= 0x10;
                        }
                    }
                    if (this.sioInShiftNum == 0) {
                        if ((getPortValue(3) & 0x01) != 0) {
                            this.sioInShiftNum = 1;
                        }
                    } else if (this.sioInShiftNum == 1) {
                        if ((getPortValue(3) & 0x01) == 0) {
                            this.sioInShiftNum = 2;
                            this.sioInShift = 0;
                            this.sioIn1Bits = 0;
                        }
                    } else if ((this.sioInShiftNum >= 2) && (this.sioInShiftNum <= 9)) {
                        this.sioInShiftNum++;
                        this.sioInShift >>= 1;
                        if ((getPortValue(3) & 0x01) != 0) {
                            this.sioInShift |= 0x80;
                            if (((this.regP3M & 0x80) == 0) || (this.sioInShiftNum < 9)) {
                                this.sioIn1Bits++;
                            }
                        }
                    } else if (this.sioInShiftNum == 10) {
                        if ((getPortValue(3) & 0x01) == 0) {
                            this.sioInShiftNum = 0;
                        } else {
                            this.sioInShiftNum = 1;
                        }
                        if ((this.regP3M & 0x80) != 0) {
                            if ((this.sioIn1Bits & 0x01) != 0) {
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
            if (this.portIn[3] >= 0) {
                this.port3LastIn = this.portIn[3];
            }
            updPorts();
        }
    }


    /* --- private Methoden --- */

    private void execNextInst() {
        int b, r1, r2;
        int opc = nextByte();
        int nibbleH = opc & 0xF0;
        int nibbleL = opc & 0x0F;
        switch (nibbleL) {
            case 0x08:                    // LD r1,R2
                r1 = getWorkingRegNum(opc >> 4);
                r2 = nextByte();
                setRegValue(r1, getReg(r2));
                this.instCycles = 6;
                break;

            case 0x09:                    // LD R2,r1
                r1 = getWorkingRegNum(opc >> 4);
                r2 = nextByte();    // Working Register hier nicht moeglich
                setReg(r2, getRegValue(r1));
                this.instCycles = 6;
                break;

            case 0x0A:                    // DJNZ r1,RA
                r1 = getWorkingRegNum(opc >> 4);
                r2 = nextByte();
                b = (getRegValue(r1) - 1) & 0xFF;
                setRegValue(r1, b);
                if (b != 0) {
                    this.pc = (this.pc + (int) (byte) r2) & 0xFFFF;
                    this.instCycles = 12;
                } else {
                    this.instCycles = 10;
                }
                break;

            case 0x0B:                    // JR cc,RA
                b = nextByte();
                if (checkCond(opc)) {
                    this.pc = (this.pc + (int) ((byte) b)) & 0xFFFF;
                    this.instCycles = 12;
                } else {
                    this.instCycles = 10;
                }
                break;

            case 0x0C:                    // LD r1,IM
                setReg(getWorkingRegNum(opc >> 4), nextByte());
                this.instCycles = 6;
                break;

            case 0x0D:                    // JP cc,DA
                r1 = nextByte();
                r2 = nextByte();
                if (checkCond(opc)) {
                    this.pc = (r1 << 8) | r2;
                    this.instCycles = 12;
                } else {
                    this.instCycles = 10;
                }
                break;

            case 0x0E:                    // INC r1
                doInstINC(getWorkingRegNum(opc >> 4));
                this.instCycles = 6;
                break;

            default:
                InstType instType = switch (nibbleH >> 4) {
                    case 0 -> InstType.ADD;
                    case 1 -> InstType.ADC;
                    case 2 -> InstType.SUB;
                    case 3 -> InstType.SBC;
                    case 4 -> InstType.OR;
                    case 5 -> InstType.AND;
                    case 6 -> InstType.TCM;
                    case 7 -> InstType.TM;
                    case 0x0A -> InstType.CP;
                    case 0x0B -> InstType.XOR;
                    default -> null;
                };
                if ((instType != null) && (nibbleL >= 2) && (nibbleL < 8)) {
                    execInst(nibbleL, instType);
                } else {
                    execRemainingInst(opc);
                }
        }
    }


    private void execInst(int nibbleL, InstType instType) {
        int b, r1, r2;
        switch (nibbleL) {
            case 0x02:                    // XYZ r1,r2
                b = nextByte();
                r1 = getWorkingRegNum(b >> 4);
                r2 = getWorkingRegNum(b);
                doInstXYZ(
                        r1,
                        instType,
                        getRegValue(r1),
                        getRegValue(r2));
                this.instCycles = 6;
                break;

            case 0x03:                    // XYZ r1,Ir2
                b = nextByte();
                r1 = getWorkingRegNum(b >> 4);
                r2 = getRegValue(getWorkingRegNum(b));
                doInstXYZ(
                        r1,
                        instType,
                        getRegValue(r1),
                        getRegValue(r2));
                this.instCycles = 6;
                break;

            case 0x04:                    // XYZ R2,R1
                r1 = nextByte();
                r2 = nextByte();
                if ((r2 & 0xF0) == 0xE0) {
                    r2 = getWorkingRegNum(r2);
                }
                doInstXYZ(r2, instType, getRegValue(r2), getReg(r1));
                this.instCycles = 10;
                break;

            case 0x05:                    // XYZ IR2,R1
                r1 = getIndirectRegNum(nextByte());
                r2 = nextByte();
                if ((r2 & 0xF0) == 0xE0) {
                    r2 = getWorkingRegNum(r2);
                }
                doInstXYZ(r2, instType, getRegValue(r2), getRegValue(r1));
                this.instCycles = 10;
                break;

            case 0x06:                    // XYZ R1,IM
                r1 = nextByte();
                b = nextByte();
                if ((r1 & 0xF0) == 0xE0) {
                    r1 = getWorkingRegNum(r1);
                }
                doInstXYZ(r1, instType, getRegValue(r1), b);
                this.instCycles = 10;
                break;

            case 0x07:                    // XYZ IR1,IM
                r1 = getIndirectRegNum(nextByte());
                b = nextByte();
                doInstXYZ(r1, instType, getRegValue(r1), b);
                this.instCycles = 10;
                break;
        }
    }


    private void execRemainingInst(int opc) {
        int a, b, r1, r2;
        switch (opc) {
            case 0x00:                    // DEC R1
                doInstDEC(getRegNum(nextByte()));
                this.instCycles = 6;
                break;

            case 0x01:                    // DEC IR1
                doInstDEC(getIndirectRegNum(nextByte()));
                this.instCycles = 6;
                break;

            case 0x10:                    // RLC R1
                doInstRLC(getRegNum(nextByte()));
                this.instCycles = 6;
                break;

            case 0x11:                    // RLC IR1
                doInstRLC(getIndirectRegNum(nextByte()));
                this.instCycles = 6;
                break;

            case 0x20:                    // INC R1
                doInstINC(getRegNum(nextByte()));
                this.instCycles = 6;
                break;

            case 0x21:                    // INC IR1
                doInstINC(getIndirectRegNum(nextByte()));
                this.instCycles = 6;
                break;

            case 0x30:                    // JP IRR1
                r1 = (getRegNum(nextByte()) & 0xFE);
                this.pc = (getRegValue(r1) << 8) | getRegValue(r1 + 1);
                this.instCycles = 8;
                break;

            case 0x31:                    // SRP IM
                setRegValue(RP, nextByte() & 0xF0);
                this.instCycles = 6;
                break;

            case 0x40:                    // DA R1
                doInstDA(getRegNum(nextByte()));
                this.instCycles = 8;
                break;

            case 0x41:                    // DA IR1
                doInstDA(getIndirectRegNum(nextByte()));
                this.instCycles = 8;
                break;

            case 0x50:                    // POP R1
                setReg(nextByte(), pop());
                this.instCycles = 10;
                break;

            case 0x51:                    // POP IR1
                setRegValue(getIndirectRegNum(nextByte()), pop());
                this.instCycles = 10;
                break;

            case 0x60:                    // COM R1
                r1 = nextByte();
                setReg(r1, updFlagsSVZ(~getReg(r1)));
                this.instCycles = 6;
                break;

            case 0x61:                    // COM IR1
                r1 = getIndirectRegNum(nextByte());
                setRegValue(r1, updFlagsSVZ(~getRegValue(r1)));
                this.instCycles = 6;
                break;

            case 0x6F:                                        // STOP
                synchronized (this.waitMonitor) {
                    this.runMode = RunMode.INST_STOP;
                    Z8Debugger debugger = this.debugger;
                    if (debugger != null) {
                        debugger.z8DebugStatusChanged(this);
                    }
                }
                this.instCycles = 6;
                break;

            case 0x70:                    // PUSH R1
                push(getReg(nextByte()));
                if (isInternalStackEnabled()) {
                    this.instCycles = 10;
                } else {
                    this.instCycles = 12;
                }
                break;

            case 0x71:                    // PUSH IR1
                push(getReg(getIndirectRegNum(nextByte())));
                if (isInternalStackEnabled()) {
                    this.instCycles = 12;
                } else {
                    this.instCycles = 14;
                }
                break;

            case 0x7F:                                        // HALT
                synchronized (this.waitMonitor) {
                    this.runMode = RunMode.INST_HALT;
                }
                this.instCycles = 7;
                break;

            case 0x80:                    // DECW RR1
                doInstDECW(getRegNum(nextByte()));
                this.instCycles = 10;
                break;

            case 0x81:                    // DECW IR1
                doInstDECW(getIndirectRegNum(nextByte()));
                this.instCycles = 10;
                break;

            case 0x82:                    // LDE r1,Irr2
            case 0xC2:                    // LDC r1,Irr2
                b = nextByte();
                r1 = getWorkingRegNum(b >> 4);
                r2 = getWorkingRegNum(b);
                setRegValue(
                        r1,
                        this.memory.getMemByte(getRegWValue(r2), opc == 0x82));
                this.instCycles = 12;
                break;

            case 0x83:                    // LDEI Ir1,Irr2
            case 0xC3:                    // LDCI Ir1,Irr2
                b = nextByte();
                r1 = getWorkingRegNum(b >> 4);
                r2 = getWorkingRegNum(b);
                a = getRegValue(r1);
                b = getRegWValue(r2);
                setRegValue(a, this.memory.getMemByte(b, opc == 0x83));
                setRegValue(r1, a + 1);
                setRegWValue(r2, b + 1);
                this.instCycles = 18;
                break;

            case 0x8F:                    // DI
                this.regIMR &= 0x7F;
                this.instCycles = 6;
                break;

            case 0x90:                    // RL R1
                doInstRL(getRegNum(nextByte()));
                this.instCycles = 6;
                break;

            case 0x91:                    // RL IR1
                doInstRL(getIndirectRegNum(nextByte()));
                this.instCycles = 6;
                break;

            case 0x92:                    // LDE Irr2,r1
            case 0xD2:                    // LDC Irr2,r1
                b = nextByte();
                r1 = getWorkingRegNum(b >> 4);
                r2 = getWorkingRegNum(b);
                this.memory.setMemByte(
                        getRegWValue(r2),
                        opc == 0x92,
                        getRegValue(r1));
                this.instCycles = 12;
                break;

            case 0x93:                    // LDEI Irr2,Ir1
            case 0xD3:                    // LDCI Irr2,Ir1
                b = nextByte();
                r1 = getWorkingRegNum(b >> 4);
                r2 = getWorkingRegNum(b);
                a = getRegValue(r1);
                b = getRegWValue(r2);
                this.memory.setMemByte(b, opc == 0x93, getRegValue(a));
                setRegValue(r1, a + 1);
                setRegWValue(r2, b + 1);
                this.instCycles = 18;
                break;

            case 0x9F:                    // EI
                this.regIMR |= 0x80;
                this.eiExecuted = true;
                this.instCycles = 6;
                break;

            case 0xA0:                    // INCW RR1
                doInstINCW(getRegNum(nextByte()));
                this.instCycles = 10;
                break;

            case 0xA1:                    // INCW IR1
                doInstINCW(getIndirectRegNum(nextByte()));
                this.instCycles = 10;
                break;

            case 0xAF:                    // RET
                this.pc = popw();
                this.instCycles = 14;
                break;

            case 0xB0:                    // CLR R1
                setReg(nextByte(), 0);
                this.instCycles = 6;
                break;

            case 0xB1:                    // CLR IR1
                setRegValue(getIndirectRegNum(nextByte()), 0);
                this.instCycles = 6;
                break;

            case 0xBF:                    // IRET
                setRegValue(FLAGS, pop());
                this.regIMR |= 0x80;
                this.pc = popw();
                this.instCycles = 16;
                break;

            case 0xC0:                    // RRC R1
                doInstRRC(getRegNum(nextByte()));
                this.instCycles = 6;
                break;

            case 0xC1:                    // RRC IR1
                doInstRRC(getIndirectRegNum(nextByte()));
                this.instCycles = 6;
                break;

            case 0xC7:                    // LD r1,x(r2)
                b = nextByte();
                r1 = getWorkingRegNum(b >> 4);
                r2 = getWorkingRegNum(b);
                setRegValue(
                        r1,
                        getRegValue((getRegValue(r2) + nextByte()) & 0xFF));
                this.instCycles = 10;
                break;

            case 0xCF:                    // RCF
                this.flagC = false;
                this.instCycles = 6;
                break;

            case 0xD0:                    // SRA R1
                doInstSRA(getRegNum(nextByte()));
                this.instCycles = 6;
                break;

            case 0xD1:                    // SRA IR1
                doInstSRA(getIndirectRegNum(nextByte()));
                this.instCycles = 6;
                break;

            case 0xD4:                    // CALL IRR1
                r1 = (getRegNum(nextByte()) & 0xFE);
                pushw(this.pc);
                this.pc = getRegWValue(r1);
                this.instCycles = 20;
                break;

            case 0xD6:                    // CALL DA
                a = nextByte();
                b = nextByte();
                pushw(this.pc);
                this.pc = (a << 8) | b;
                this.instCycles = 20;
                break;

            case 0xD7:                    // LD r2,x(r1)
                b = nextByte();
                r1 = getWorkingRegNum(b >> 4);
                r2 = getWorkingRegNum(b);
                setRegValue(
                        (getRegValue(r2) + nextByte()) & 0xFF,
                        getRegValue(r1));
                this.instCycles = 10;
                break;

            case 0xDF:                    // SCF
                this.flagC = true;
                this.instCycles = 6;
                break;

            case 0xE0:                    // RR R1
                doInstRR(getRegNum(nextByte()));
                this.instCycles = 6;
                break;

            case 0xE1:                    // RR IR1
                doInstRR(getIndirectRegNum(nextByte()));
                this.instCycles = 6;
                break;

            case 0xE3:                    // LD r1,IR2
                b = nextByte();
                r1 = getWorkingRegNum(b >> 4);
                r2 = getWorkingRegNum(b);
                setRegValue(r1, getRegValue(getRegValue(r2)));
                this.instCycles = 6;
                break;

            case 0xE4:                    // LD R2,R1
                r1 = nextByte();
                r2 = nextByte();
                setReg(r2, getReg(r1));
                this.instCycles = 10;
                break;

            case 0xE5:                    // LD R2,IR1
                r1 = nextByte();
                r2 = nextByte();
                setReg(r2, getRegValue(getIndirectRegNum(r1)));
                this.instCycles = 10;
                break;

            case 0xE6:                    // LD R1,IM
                r1 = getRegNum(nextByte());
                setRegValue(r1, nextByte());
                this.instCycles = 10;
                break;

            case 0xE7:                    // LD IR1,IM
                r1 = getIndirectRegNum(nextByte());
                setRegValue(r1, nextByte());
                this.instCycles = 10;
                break;

            case 0xEF:                    // CCF
                this.flagC = !this.flagC;
                this.instCycles = 6;
                break;

            case 0xF0:                    // SWAP R1
                doInstSWAP(getRegNum(nextByte()));
                this.instCycles = 8;
                break;

            case 0xF1:                    // SWAP IR1
                doInstSWAP(getIndirectRegNum(nextByte()));
                this.instCycles = 8;
                break;

            case 0xF3:                    // LD Ir1,r2
                b = nextByte();
                r1 = getWorkingRegNum(b >> 4);
                r2 = getWorkingRegNum(b);
                setRegValue(getRegValue(r1), getRegValue(r2));
                this.instCycles = 6;
                break;

            case 0xF5:                    // LD IR2,R1
                r1 = nextByte();
                r2 = getIndirectRegNum(nextByte());
                setReg(r2, getReg(r1));
                this.instCycles = 10;
                break;

            /*
             * Alle anderen Befehle werden als NOP behandelt,
             * auch WDh und WDT.
             * Da es den Watchdog Timer nicht bei allen Z8-Systemen gibt,
             * wird hier keiner emuliert.
             */
            default:                        // NOP, WDh, WDT
                this.instCycles = 6;
                break;
        }
    }


    private boolean checkCond(int value) {
        boolean rv = switch (value & 0xF0) {
            case 0x10 ->                    // LT
                    (this.flagS ^ this.flagV);
            case 0x20 ->                    // LE
                    (this.flagZ || (this.flagS ^ this.flagV));
            case 0x30 ->                    // ULE
                    (this.flagC || this.flagZ);
            case 0x40 ->                    // OV
                    this.flagV;
            case 0x50 ->                    // MI
                    this.flagS;
            case 0x60 ->                    // Z, EQ
                    this.flagZ;
            case 0x70 ->                    // C, ULT
                    this.flagC;
            case 0x80 ->                    // ohne Bedingung
                    true;
            case 0x90 ->                    // GE
                    this.flagS == this.flagV;
            case 0xA0 ->                    // GT
                    !(this.flagZ || (this.flagS ^ this.flagV));
            case 0xB0 ->                    // UGT
                    (!this.flagC && !this.flagZ);
            case 0xC0 ->                    // NOV
                    !this.flagV;
            case 0xD0 ->                    // PL
                    !this.flagS;
            case 0xE0 ->                    // NZ, NE
                    !this.flagZ;
            case 0xF0 ->                    // NC, UGE
                    !this.flagC;
            default -> false;
        };
        return rv;
    }


    private int doInstAdd(int v1, int v2, int v3) {
        int m = v1 + v2;
        int rv = m + v3;
        this.flagV = ((v1 & 0x80) == (v2 & 0x80)) && ((v1 & 0x80) != (m & 0x80));
        if (!this.flagV) {
            this.flagV = ((m & 0x80) == (v3 & 0x80)) && ((m & 0x80) != (rv & 0x80));
        }
        this.flagC = ((rv & 0xFF00) != 0);
        this.flagZ = ((rv & 0xFF) == 0);
        this.flagS = ((rv & 0x80) != 0);
        this.flagD = false;
        this.flagH = ((((v1 & 0x0F) + (v2 & 0x0F) + (v3 & 0x0F)) & 0xF0) != 0);
        return rv & 0xFF;
    }


    private void doInstCP(int v1, int v2) {
        int m = v1 - v2;
        this.flagV = ((v1 & 0x80) != (v2 & 0x80)) && ((m & 0x80) == (v2 & 0x80));
        this.flagC = ((m & 0xFF00) != 0);
        this.flagZ = ((m & 0xFF) == 0);
        this.flagS = ((m & 0x80) != 0);
    }


    private void doInstDA(int r) {
        int v = getRegValue(r);
        int h = (v & 0xF0) >> 4;
        int l = v & 0x0F;
        if (this.flagD) {
            if (!this.flagC && (h <= 8) && this.flagH && (l >= 6)) {
                v += 0xFA;
                this.flagC = false;
            } else if (this.flagC && (h >= 7) && !this.flagH && (l <= 9)) {
                v += 0xA0;
                this.flagC = true;
            } else if (this.flagC && (h >= 6) && this.flagH && (l >= 6)) {
                v += 0x9A;
                this.flagC = true;
            } else {
                this.flagC = false;
            }
        } else {
            if ((!this.flagC && (h <= 8) && !this.flagH && (l >= 0x0A))
                    || (!this.flagC && (h <= 9) && this.flagH && (l <= 3))) {
                v += 0x06;
                this.flagC = false;
            } else if ((!this.flagC && (h >= 0x0A) && !this.flagH && (l <= 9))
                    || (this.flagC && (h <= 2) && !this.flagH && (l <= 9))) {
                v += 0x60;
                this.flagC = true;
            } else if ((!this.flagC && (h >= 9) && !this.flagH && (l >= 0x0A))
                    || (!this.flagC && (h >= 0x0A) && this.flagH && (l <= 3))
                    || (this.flagC && (h <= 2) && !this.flagH && (l >= 0x0A))
                    || (this.flagC && (h <= 3) && this.flagH && (l <= 3))) {
                v += 0x66;
                this.flagC = true;
            } else {
                this.flagC = false;
            }
        }
        v &= 0xFF;
        this.flagZ = (v == 0);
        this.flagS = ((v & 0x80) != 0);
        setRegValue(r, v);
    }


    private void doInstDEC(int r) {
        int v = getRegValue(r);
        int m = (v - 1) & 0xFF;
        this.flagZ = (m == 0);
        this.flagS = ((m & 0x80) != 0);
        this.flagV = ((m & 0x80) != (v & 0x80));
        setRegValue(r, m);
    }


    private void doInstDECW(int r) {
        r &= 0xFE;            // Bit 0 der Registeradresse ignorieren
        int v = getRegWValue(r);
        int m = (v - 1) & 0xFFFF;
        this.flagZ = (m == 0);
        this.flagS = ((m & 0x8000) != 0);
        this.flagV = ((m & 0x8000) != (v & 0x8000));
        setRegWValue(r, m);
    }


    private void doInstINC(int r) {
        int v = getRegValue(r);
        int m = (v + 1) & 0xFF;
        this.flagZ = (m == 0);
        this.flagS = ((m & 0x80) != 0);
        this.flagV = ((m & 0x80) != (v & 0x80));
        setRegValue(r, m);
    }


    private void doInstINCW(int r) {
        r &= 0xFE;            // Bit 0 der Registeradresse ignorieren
        int v = getRegWValue(r);
        int m = (v + 1) & 0xFFFF;
        this.flagZ = (m == 0);
        this.flagS = ((m & 0x8000) != 0);
        this.flagV = ((m & 0x8000) != (v & 0x8000));
        setRegWValue(r, m);
    }


    private void doInstRL(int r) {
        int v = getRegValue(r);
        int m = (v << 1);
        if ((m & 0x100) != 0) {
            m |= 0x01;
            this.flagC = true;
        } else {
            this.flagC = false;
        }
        m &= 0xFF;
        this.flagZ = (m == 0);
        this.flagS = ((m & 0x80) != 0);
        this.flagV = ((m & 0x80) != (v & 0x80));
        setRegValue(r, m);
    }


    private void doInstRLC(int r) {
        int v = getRegValue(r);
        int m = (v << 1);
        if (this.flagC) {
            m |= 0x01;
        }
        this.flagC = ((m & 0x100) != 0);
        m &= 0xFF;
        this.flagZ = (m == 0);
        this.flagS = ((m & 0x80) != 0);
        this.flagV = ((m & 0x80) != (v & 0x80));
        setRegValue(r, m);
    }


    private void doInstRR(int r) {
        int v = getRegValue(r);
        this.flagC = ((v & 0x01) != 0);
        int m = (v >> 1) & 0x7F;
        if (this.flagC) {
            m |= 0x80;
        }
        this.flagZ = (m == 0);
        this.flagS = this.flagC;
        this.flagV = (m & 0x80) != (v & 0x80);
        setRegValue(r, m);
    }


    private void doInstRRC(int r) {
        int v = getRegValue(r);
        boolean b7 = this.flagC;
        this.flagC = ((v & 0x01) != 0);
        int m = (v >> 1) & 0x7F;
        if (b7) {
            m |= 0x80;
        }
        this.flagZ = (m == 0);
        this.flagS = b7;
        this.flagV = (m & 0x80) != (v & 0x80);
        setRegValue(r, m);
    }


    private int doInstSub(int v1, int v2, int v3) {
        int m = v1 - v2;
        int rv = m - v3;
        this.flagV = ((v1 & 0x80) != (v2 & 0x80)) && ((m & 0x80) == (v2 & 0x80));
        if (!this.flagV) {
            this.flagV = ((m & 0x80) != (v3 & 0x80))
                    && ((rv & 0x80) == (v3 & 0x80));
        }
        this.flagC = ((rv & 0xFF00) != 0);
        this.flagZ = ((rv & 0xFF) == 0);
        this.flagS = ((rv & 0x80) != 0);
        this.flagD = true;
        this.flagH = ((((v1 & 0x0F) - (v2 & 0x0F) - (v3 & 0x0F)) & 0xF0) != 0);
        return rv & 0xFF;
    }


    private void doInstSRA(int r) {
        int v = getRegValue(r);
        this.flagC = ((v & 0x01) != 0);
        this.flagS = ((v & 0x80) != 0);
        v = (v >> 1) & 0xFF;
        if (this.flagS) {
            v |= 0x80;
        }
        this.flagZ = (v == 0);
        this.flagV = false;
        setRegValue(r, v);
    }


    private void doInstSWAP(int r) {
        int v = getRegValue(r);
        int m = ((v << 4) & 0xF0) | ((v >> 4) & 0x0F);
        this.flagZ = (m == 0);
        this.flagS = ((m & 0x80) != 0);
        setRegValue(r, m);
    }


    private void doInstXYZ(int dst, InstType instType, int op1, int op2) {
        switch (instType) {
            case ADD:
                setRegValue(dst, doInstAdd(op1, op2, 0));
                break;

            case ADC:
                setRegValue(dst, doInstAdd(op1, op2, this.flagC ? 1 : 0));
                break;

            case SUB:
                setRegValue(dst, doInstSub(op1, op2, 0));
                break;

            case SBC:
                setRegValue(dst, doInstSub(op1, op2, this.flagC ? 1 : 0));
                break;

            case OR:
                setRegValue(dst, updFlagsSVZ(op1 | op2));
                break;

            case AND:
                setRegValue(dst, updFlagsSVZ(op1 & op2));
                break;

            case TCM:
                updFlagsSVZ(~op1 & op2);
                break;

            case TM:
                updFlagsSVZ(op1 & op2);
                break;

            case CP:
                doInstCP(op1, op2);
                break;

            case XOR:
                setRegValue(dst, updFlagsSVZ(op1 ^ op2));
                break;

        }
    }


    private int getIndirectRegNum(int r) {
        if ((r & 0xF0) == 0xE0) {
            r = getWorkingRegNum(r);
        }
        return getRegValue(r);
    }


    private int getReg(int r) {
        return getRegValue(getRegNum(r));
    }


    private int getRegFLAGS() {
        int rv = this.regFLAGS & 0x03;
        if (this.flagH) {
            rv |= 0x04;
        }
        if (this.flagD) {
            rv |= 0x08;
        }
        if (this.flagV) {
            rv |= 0x10;
        }
        if (this.flagS) {
            rv |= 0x20;
        }
        if (this.flagZ) {
            rv |= 0x40;
        }
        if (this.flagC) {
            rv |= 0x80;
        }
        return rv;
    }


    private int nextByte() {
        int rv = this.memory.getMemByte(this.pc, false);
        this.pc = (this.pc + 1) & 0xFFFF;
        return rv;
    }


    private int getPortValue(int port) {
        int rv = 0xFF;
        if ((port >= 0) && (port < this.portIn.length)) {
            if (this.portIn[port] < 0) {
                Z8IO z8io = this.z8io;
                if (z8io != null) {
                    this.portIn[port] = z8io.getPortValue(port) & 0xFF;
                }
            }
            rv = this.portIn[port];
        }
        return rv;
    }


    /*
     * Der Initialwert des Registers P01M (%F8)
     * unterscheidet sich zwischen den einzelnen Z8-Chips:
     *   Z8601/Z8611: P01M=%6D
     *   Z8681:       P01M=%75
     *   Z8682:       P01M=%96
     *
     * Emuliert wird hier der Z8601/Z8611.
     */
    private void reset(boolean powerOn) {
        this.pc = 0x000C;
        this.eiExecuted = false;
        this.resetFired = false;
        if (powerOn) {
            if (this.regInitZero) {
                Arrays.fill(this.registers, 0);
            } else {
                for (int i = 0; i < this.registers.length; i++) {
                    this.registers[i] = this.random.nextInt() & 0xFF;
                }
            }
            this.interruptPriority = null;
        }
        setRegValue(TMR, 0);
        setRegValue(PRE1, this.timer1.getPreCounter() & 0xFC);    // B0=B1=0
        setRegValue(PRE0, this.timer0.getPreCounter() & 0xFE);    // B0=0
        setRegValue(P2M, 0xFF);
        setRegValue(P3M, this.regP3M & 0x02);    // ausser Bit 1 alle 0
        setRegValue(P01M, 0x6D);            // Z8601/Z8611
        setRegValue(IRQ, 0);
        setRegValue(IMR, this.regIMR & 0x7F);    // Bit 7: 0
        setRegValue(3, 0xFF);            // nach den Steuerregistern!

        Z8Listener listener = this.resetListener;
        if (listener != null) {
            listener.z8Update(
                    this,
                    powerOn ?
                            Z8Listener.Reason.POWER_ON
                            : Z8Listener.Reason.RESET);
        }
    }


    private void setReg(int r, int v) {
        if ((r & 0xF0) == 0xE0) {
            r = getWorkingRegNum(r);
        }
        setRegValue(r, v);
    }


    private void setRegWValue(int r, int v) {
        r &= 0xFE;            // Bit 0 der Registeradresse ignorieren
        setRegValue(r, v >> 8);
        setRegValue(r + 1, v);
    }


    private void statusChanged() {
        Z8Listener listener = this.statusListener;
        if (listener != null) {
            listener.z8Update(this, Z8Listener.Reason.STATUS_CHANGED);
        }
    }


    private int updFlagsSVZ(int v) {
        this.flagZ = ((v & 0xFF) == 0);
        this.flagS = ((v & 0x80) != 0);
        this.flagV = false;
        return v;
    }


    private void changeP36() {
        this.portOut[3] = (this.portOut[3] & 0xBF)
                | (~this.portLastOut[3] & 0x40);
    }


    private void updInputReg0() {
        int v = 0;
        if (((this.regP01M & 0x03) == 0x01)            // P00-03: Eingang
                || ((this.regP01M & 0xC0) == 0x40))        // P04-07: Eingang
        {
            v = getPortValue(0);
        }
        switch (this.regP01M & 0x03) {            // P00-03
            case 0:                        // Ausgang
                this.registers[0] = this.portLastOut[0] & 0x0F;
                break;

            case 0x01:                    // Eingang
                this.registers[0] = v & 0x0F;
                break;

            case 0x10:                    // A8-A11
            case 0x11:                    // A8-A11
                this.registers[0] = (this.pc - 1) & 0x0F;
                break;
        }
        switch (this.regP01M & 0xC0) {            // P04-07
            case 0:                        // Ausgang
                this.registers[0] |= this.portLastOut[0] & 0xF0;
                break;

            case 0x40:                    // Eingang
                this.registers[0] |= v & 0xF0;
                break;

            case 0x80:                    // A12-A16
            case 0xC0:                    // A12-A16
                this.registers[0] |= (this.pc - 1) & 0xF0;
                break;
        }
    }


    private void updInputReg1() {
        switch (this.regP01M & 0x18) {
            case 0:                        // Ausgang
                this.registers[1] = this.portLastOut[1];
                break;

            case 0x08:                    // Eingabe
            case 0x18:                    // hochohmig
                this.registers[1] = getPortValue(1);
                break;

            case 0x10:                    // A0-A7
                this.registers[1] = (this.pc - 1) & 0xFF;
                break;
        }
    }


    private void updInputReg2() {
        /*
         * Wenn alle Bits auf Ausgang programmiert und die Ausgaenge
         * nicht hochohmig sind, hat das externe System keinen Einfluss.
         * In dem Fall wird einfach das Augangsregister gelesen.
         */
        if ((this.regP2M == 0) && ((this.regP3M & 0x01) == 0x01)) {
            this.registers[2] = this.portLastOut[2];
        } else {
            int v = getPortValue(2);
            if ((this.regP3M & 0x01) == 0x01) {
                v = (v & this.regP2M) | (this.portLastOut[2] & ~this.regP2M);
            }
            this.registers[2] = v & 0xFF;
        }
    }


    private void updPorts() {
        Z8IO z8io = this.z8io;
        if (z8io != null) {
            for (int i = 0; i < this.portOut.length; i++) {
                int v = this.portOut[i];
                if (v != this.portLastOut[i]) {
                    this.portLastOut[i] = v;
                    z8io.setPortValue(i, v);
                }
            }
        } else {
            System.arraycopy(this.portOut, 0, this.portLastOut, 0, this.portOut.length);
        }
    }


    private boolean wentP3BitFrom1To0(int mask) {
        int vOld = this.port3LastIn & mask;
        int vNew = getPortValue(3) & mask;
        return (vNew == 0) && (vNew != vOld);
    }
}
