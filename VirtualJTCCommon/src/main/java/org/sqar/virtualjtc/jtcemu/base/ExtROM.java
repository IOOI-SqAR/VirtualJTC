/*
 * (c) 2007-2019 Jens Mueller
 * (c) 2017-2024 Lars Sonchocky-Helldorf
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Daten einer externen ROM-Datei
 */

package org.sqar.virtualjtc.jtcemu.base;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.ResourceBundle;


public class ExtROM implements Comparable<ExtROM> {
    private static final Locale locale = Locale.getDefault();
    private static final ResourceBundle extROMResourceBundle = ResourceBundle.getBundle("ExtROM", locale);

    private int begAddr;
    private int endAddr;
    private int maxSize;
    private File file;
    private byte[] fileBytes;
    private String text;


    public ExtROM(File file, int maxSize) throws IOException {
        this.maxSize = maxSize;
        this.file = file;
        this.fileBytes = null;
        this.begAddr = 0;
        this.endAddr = 0;
        this.text = "%0000  " + this.file.getPath();
        reload();
    }


    public synchronized int getBegAddr() {
        return this.begAddr;
    }


    public synchronized int getEndAddr() {
        return this.endAddr;
    }


    public synchronized int getByte(int addr) {
        int rv = 0;
        if (this.fileBytes != null) {
            int idx = addr - this.begAddr;
            if ((idx >= 0) && (idx < this.fileBytes.length)) {
                rv = (int) this.fileBytes[idx] & 0xFF;
            }
        }
        return rv;
    }


    public File getFile() {
        return this.file;
    }


    public void reload() throws IOException {
        byte[] fileBytes = JTCUtil.readFile(this.file, this.maxSize);
        if (fileBytes != null) {
            this.fileBytes = fileBytes;
            this.endAddr = this.begAddr + this.fileBytes.length - 1;
        }
    }


    public synchronized void setBegAddr(int addr) {
        this.begAddr = addr;
        this.endAddr = 0;
        if (this.fileBytes != null) {
            this.endAddr = this.begAddr + this.fileBytes.length - 1;
        }
        this.text = String.format(
                "%%%04X  %s",
                this.begAddr,
                this.file.getPath());
    }


    public int size() {
        return this.fileBytes != null ? this.fileBytes.length : 0;
    }


    /* --- Comparable --- */

    @Override
    public int compareTo(ExtROM data) {
        return data != null ? (this.begAddr - data.begAddr) : -1;
    }


    /* --- ueberschriebene Methoden --- */

    @Override
    public boolean equals(Object o) {
        boolean rv = false;
        if (o != null) {
            if (o == this) {
                rv = true;
            } else {
                if (o instanceof ExtROM) {
                    rv = true;
                    ExtROM data = (ExtROM) o;
                    if (this.begAddr != data.getBegAddr()) {
                        rv = false;
                    } else {
                        if ((this.fileBytes != null) && (data.fileBytes != null)) {
                            if (this.fileBytes.length == data.fileBytes.length) {
                                for (int i = 0; i < this.fileBytes.length; i++) {
                                    if (this.fileBytes[i] != data.fileBytes[i]) {
                                        rv = false;
                                        break;
                                    }
                                }
                            } else {
                                rv = false;
                            }
                        } else {
                            int n1 = 0;
                            if (this.fileBytes != null) {
                                n1 = this.fileBytes.length;
                            }
                            int n2 = 0;
                            if (data.fileBytes != null) {
                                n2 = data.fileBytes.length;
                            }
                            if (n1 != n2) {
                                rv = false;
                            }
                        }
                    }
                }
            }
        }
        return rv;
    }


    @Override
    public String toString() {
        return this.text;
    }
}