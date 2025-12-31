/*
 * (c) 2019-2020 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Haltepunkt auf den Zugriff auf einen Speicherbereich
 */

package org.sqar.virtualjtc.jtcemu.tools.debugger;

import org.sqar.virtualjtc.z8.Z8;


public class MemBreakpoint extends AccessBreakpoint {
    private int begAddr;
    private int endAddr;


    public MemBreakpoint(
            int begAddr,
            int endAddr,
            boolean read,
            boolean write) {
        super(read, write);
        this.begAddr = begAddr;
        this.endAddr = endAddr;
        updText();
    }


    public int getBegAddr() {
        return this.begAddr;
    }


    public int getEndAddr() {
        return this.endAddr;
    }


    public void setValues(
            int begAddr,
            int endAddr,
            boolean read,
            boolean write) {
        super.setAccess(read, write);
        this.begAddr = begAddr;
        this.endAddr = endAddr;
        updText();
    }


    /* --- ueberschriebene Methoden --- */

    @Override
    public int compareTo(AbstractBreakpoint bp) {
        int rv = -1;
        if (bp != null) {
            if (bp instanceof MemBreakpoint) {
                rv = this.begAddr - ((MemBreakpoint) bp).getBegAddr();
                if (rv == 0) {
                    rv = this.endAddr - ((MemBreakpoint) bp).getEndAddr();
                }
            } else {
                rv = super.compareTo(bp);
            }
        }
        return rv;
    }


    @Override
    public boolean equals(Object o) {
        boolean rv = false;
        if (o != null) {
            if (o instanceof MemBreakpoint) {
                if ((((MemBreakpoint) o).getBegAddr() == this.begAddr)
                        && (((MemBreakpoint) o).getEndAddr() == this.endAddr)) {
                    rv = true;
                }
            }
        }
        return rv;
    }


    @Override
    public boolean matches(Z8 z8) {
        boolean rv = false;
        int pc = z8.getPC();
        switch (z8.getMemByte(pc, false)) {
            case 0x50:                    // POP R1
            case 0x51:                    // POP IR1
                if (isRead() && !z8.isInternalStackEnabled()) {
                    rv = matchesAddr(z8.getSP());
                }
                break;

            case 0x70:                    // PUSH R1
            case 0x71:                    // PUSH IR1
                if (isWrite() && !z8.isInternalStackEnabled()) {
                    rv = matchesAddr(z8.getSP() - 1);
                }
                break;

            case 0x82:                    // LDE r1,Irr2
            case 0x83:                    // LDEI Ir1,Irr2
            case 0xC2:                    // LDC r1,Irr2
            case 0xC3:                    // LDCI Ir1,Irr2
                if (isRead()) {
                    int b1 = z8.getMemByte((pc + 1) & 0xFFFF, false);
                    int r2 = z8.getWorkingRegNum(b1);
                    rv = matchesAddr(z8.getRegWValue(r2));
                }
                break;

            case 0x92:                    // LDE Irr2,r1
            case 0x93:                    // LDEI Irr2,Ir1
            case 0xD2:                    // LDC Irr2,r1
            case 0xD3:                    // LDCI Irr2,Ir1
                if (isWrite()) {
                    int b1 = z8.getMemByte((pc + 1) & 0xFFFF, false);
                    int r2 = z8.getWorkingRegNum(b1);
                    rv = matchesAddr(z8.getRegWValue(r2));
                }
                break;

            case 0xAF:                    // RET
                if (isRead() && !z8.isInternalStackEnabled()) {
                    int sp = z8.getSP();
                    rv = matchesAddr(sp) || matchesAddr(sp + 1);
                }
                break;

            case 0xBF:                    // IRET
                if (isRead() && !z8.isInternalStackEnabled()) {
                    int sp = z8.getSP();
                    rv = matchesAddr(sp)
                            || matchesAddr(sp + 2)
                            || matchesAddr(sp + 1);
                }
                break;

            case 0xD4:                    // CALL IRR1
            case 0xD6:                    // CALL DA
                if (isWrite() && !z8.isInternalStackEnabled()) {
                    int sp = z8.getSP();
                    rv = matchesAddr(sp - 1) || matchesAddr(sp - 2);
                }
                break;
        }
        return rv;
    }


    /* --- private Methoden --- */

    private boolean matchesAddr(int addr) {
        addr &= 0xFFFF;
        return (addr >= this.begAddr) && (addr <= this.endAddr);
    }


    private void updText() {
        StringBuilder buf = new StringBuilder();
        buf.append(String.format("%04X", this.begAddr));
        if (this.endAddr > this.begAddr) {
            buf.append(String.format("-%04X", this.endAddr));
        }
        appendAccessTextTo(buf);
        setText(buf.toString());
    }
}
