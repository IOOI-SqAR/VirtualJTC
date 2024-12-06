/*
 * (c) 2014-2021 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Laden einer Datei in den Arbeitsspeicher
 */

package org.jens_mueller.jtcemu.base;

import org.jens_mueller.z8.Z8Memory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.Reader;


public class FileLoader {
    public static final int DEFAULt_LOAD_ADDR = 0xE000;

    private Z8Memory memory;
    private String msg;
    private int lastLoadedAddr;
    private boolean outOfRam;


    public FileLoader(Z8Memory memory) {
        this.memory = memory;
        this.msg = null;
        this.lastLoadedAddr = -1;
        this.outOfRam = false;
    }


    public String getMessage() {
        return this.msg;
    }


    /*
     * Die Methode laedt eine Datei in den Arbeitsspeicher.
     * Im Erfolgsfall wird ein Infotext zurueckgeliefert.
     */
    public String loadFile(
            File file,
            FileInfo.Format fmt,
            int begAddr,
            int endAddr,
            int startAddr) {
        String rv = null;
        if (begAddr < 0) {
            this.msg = "Die Datei konnte nicht geladen werden,\n"
                    + "da die Anfangsadresse nicht bekannt ist.";
        } else if ((endAddr >= 0) && (endAddr < begAddr)) {
            this.msg = "Die Datei konnte nicht geladen werden,\n"
                    + "da die Endadresse kleiner als die"
                    + " Anfangsadresse ist.";
        } else {
            if (endAddr < 0) {
                endAddr = 0xFFFF;
            }
            try {
                if ((file != null) && (fmt != null)
                        && (begAddr >= 0) && (begAddr <= endAddr)) {
                    this.lastLoadedAddr = -1;
                    switch (fmt) {
                        case JTC:
                            loadFileIntoMem(file, begAddr, endAddr, 128);
                            break;

                        case TAP:
                            loadTapFileIntoMem(file, begAddr, endAddr);
                            break;

                        case BIN:
                            loadFileIntoMem(file, begAddr, endAddr, 0);
                            break;

                        case HEX:
                            loadHexFileIntoMem(file, begAddr, endAddr);
                            break;
                    }
                    if (this.lastLoadedAddr >= begAddr) {
                        if ((startAddr >= 0) && (startAddr <= 0xFFFF)) {
                            rv = String.format(
                                    "Datei nach %%%04X-%%%04X geladen, Start: %%%04X",
                                    begAddr,
                                    this.lastLoadedAddr,
                                    startAddr);
                        } else {
                            rv = String.format(
                                    "Datei nach %%%04X-%%%04X geladen",
                                    begAddr,
                                    this.lastLoadedAddr);
                        }
                    }
                }
                if (this.outOfRam) {
                    if (rv != null) {
                        this.msg = "Die Datei konnte nur teilweise geladen werden,\n"
                                + "da entweder der betreffenden Adressbereich nicht"
                                + " vollst\u00E4ndig RAM ist\n"
                                + "oder versucht wurde, \u00FCber das Ende des Adressbereichs"
                                + " hinaus zu laden.";
                    } else {
                        this.msg = "Die Datei konnte nicht geladen werden,\n"
                                + "da der betreffende Adressbereich kein RAM ist.";
                    }
                }
            } catch (IOException ex) {
                this.msg = ex.getMessage();
                rv = null;
            }
        }
        return rv;
    }


    /* --- private Methoden --- */

    private void loadFileIntoMem(
            File file,
            int addr,
            int endAddr,
            int bytesToSkip) throws IOException {
        InputStream in = null;
        try {
            in = new FileInputStream(file);
            while (bytesToSkip > 0) {
                if (in.read() < 0) {
                    break;
                }
                --bytesToSkip;
            }
            while (addr <= endAddr) {
                int b = in.read();
                if (b < 0) {
                    break;
                }
                setMemByte(addr++, b);
            }
        } finally {
            JTCUtil.closeSilently(in);
        }
    }


    private void loadHexFileIntoMem(
            File file,
            int memAddr,
            int memEndAddr) throws IOException {
        boolean success = false;
        Reader in = null;
        try {
            in = new BufferedReader(new FileReader(file));

            boolean firstAddr = true;
            boolean enabled = true;
            boolean cancelled = false;
            boolean outOfRange = false;
            int memOffs = 0;
            int ch = in.read();
            while (enabled && (ch != -1)) {

                // Startmarkierung suchen
                while ((ch >= 0) && (ch != ':')) {
                    ch = in.read();
                }
                if (ch >= 0) {

                    // Segment verarbeiten
                    int cnt = parseHex(in, 2);
                    int addr = parseHex(in, 4);
                    int type = parseHex(in, 2);
                    switch (type) {
                        case 0:            // Data Record
                            if (cnt > 0) {
                                if (firstAddr) {
                                    firstAddr = false;
                                    memOffs = memAddr - addr;
                                }
                                int dstAddr = addr + memOffs;
                                while (cnt > 0) {
                                    if ((dstAddr >= memAddr) && (dstAddr <= memEndAddr)) {
                                        success |= setMemByte(dstAddr++, parseHex(in, 2));
                                    } else {
                                        outOfRange = true;
                                    }
                                    --cnt;
                                }
                            }
                            break;

                        case 1:            // End of File Record
                            enabled = false;
                            break;

                        case 2:            // Extended Segment Address Record
                            while (cnt > 0) {
                                if (parseHex(in, 2) != 0) {
                                    this.msg = "Die Datei enth\u00E4lt einen Datensatz"
                                            + " f\u00FCr eine segmentierte Adresse,\n"
                                            + "der von "
                                            + AppContext.getAppName()
                                            + " nicht unterst\u00FCtzt wird.";
                                    cancelled = true;
                                    enabled = false;
                                    break;
                                }
                                --cnt;
                            }
                            break;

                        case 3:            // Start Segment Address Record
                        case 5:            // Start Linear Address Record
                            // Datensatz ignorieren
                            break;

                        case 4:            // Extended Linear Address Record
                            while (cnt > 0) {
                                if (parseHex(in, 2) != 0) {
                                    this.msg = "Die Datei enth\u00E4lt einen Datensatz"
                                            + " f\u00FCr eine lineare 32-Bit-Adresse,\n"
                                            + "die au\u00DFerhalb des von "
                                            + AppContext.getAppName()
                                            + " emulierten Adressraums liegt.";
                                    cancelled = true;
                                    enabled = false;
                                    break;
                                }
                                --cnt;
                            }
                            break;

                        default:
                            this.msg = String.format(
                                    "Die Datei enth\u00E4lt einen Datensatzart"
                                            + " des Typs %d,\n"
                                            + "der von "
                                            + AppContext.getAppName()
                                            + " nicht unterst\u00FCtzt wird.",
                                    type);
                            cancelled = true;
                            enabled = false;
                    }
                    if (this.msg != null) {
                        if (success && cancelled) {
                            this.msg = this.msg
                                    + "\nEs werden nur die Daten bis zu diesem Datensatz"
                                    + " geladen.";
                        }
                        enabled = false;
                    }
                    ch = in.read();
                }
            }
            if (outOfRange) {
                String msg2 = "Die Datei enth\u00E4lt Datens\u00E4tze"
                        + " mit Adressen, die entweder vor der\n"
                        + "Anfangsadresse oder hinter dem zu ladenden"
                        + " Adressbereich liegen.\n"
                        + "Diese Datens\u00E4tze werden ignoriert.";
                if (this.msg != null) {
                    this.msg = this.msg + "\n\n" + msg2;
                } else {
                    this.msg = msg2;
                }
            }
        } finally {
            JTCUtil.closeSilently(in);
        }
    }


    private void loadTapFileIntoMem(
            File file,
            int addr,
            int endAddr) throws IOException {
        InputStream in = null;
        try {
            in = new FileInputStream(file);
            for (int i = 0; i < 145; i++) {
                if (in.read() < 0) {
                    break;
                }
            }
            int posInBlk = 0;
            while (addr <= endAddr) {
                int b = in.read();
                if (b < 0) {
                    break;
                }
                if (posInBlk == 0) {
                    posInBlk = 128;
                } else {
                    setMemByte(addr++, b);
                    --posInBlk;
                }
            }
        } finally {
            JTCUtil.closeSilently(in);
        }
    }


    private static int parseHex(Reader in, int cnt) throws IOException {
        int value = 0;
        while (cnt > 0) {
            int ch = in.read();
            if ((ch >= '0') && (ch <= '9')) {
                value = (value << 4) | ((ch - '0') & 0x0F);
            } else if ((ch >= 'A') && (ch <= 'F')) {
                value = (value << 4) | ((ch - 'A' + 10) & 0x0F);
            } else if ((ch >= 'a') && (ch <= 'f')) {
                value = (value << 4) | ((ch - 'a' + 10) & 0x0F);
            } else {
                throw new IOException(
                        "Die Datei entspricht nicht dem erwarteten HEX-Format.");
            }
            --cnt;
        }
        return value;
    }


    private boolean setMemByte(int addr, int b) {
        boolean done = false;
        if (addr < 0x10000) {
            done = this.memory.setMemByte(addr, false, b);
        }
        if (done) {
            if (addr > this.lastLoadedAddr) {
                this.lastLoadedAddr = addr;
            }
        } else {
            this.outOfRam = true;
        }
        return done;
    }
}
