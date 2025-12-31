/*
 * (c) 2007-2010 Jens Mueller
 * (c) 2017 Lars Sonchocky-Helldorf
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Informationen ueber eine Datei
 */

package org.sqar.virtualjtc.jtcemu.base;

import java.io.*;
import java.util.Arrays;
import java.util.Locale;
import java.util.ResourceBundle;


public class FileInfo {
    private static final Locale locale = Locale.getDefault();
    private static final ResourceBundle fileInfoResourceBundle = ResourceBundle.getBundle("FileInfo", locale);

    public static final String FILE_GROUP_SOFTWARE = "software";

    public enum Format {JTC, TAP, HEX, BIN}

    ;

    private byte[] header;
    private long fileLen;
    private int begAddr;
    private int endAddr;
    private int startAddr;
    private Format fmt;
    private String infoText;


    public static FileInfo analyzeFile(File file) {
        FileInfo rv = null;
        if (file != null) {
            try {
                long fileLen = file.length();
                if (fileLen > 0) {
                    String fileExt = "";
                    String fileName = file.getName();
                    if (fileName != null) {
                        int pos = fileName.lastIndexOf('.');
                        if ((pos >= 0) && ((pos + 1) < fileName.length())) {
                            fileExt = fileName.substring(pos + 1).toUpperCase();
                        }
                    }
                    InputStream in = null;
                    try {
                        in = new FileInputStream(file);

                        byte[] header = new byte[40];
                        int nBytes = in.read(header);
                        if ((nBytes == header.length) || (nBytes == fileLen)) {
                            rv = analyzeFile(fileExt, header, fileLen);
                        }
                    } finally {
                        JTCUtil.closeSilently(in);
                    }
                }
            } catch (Exception ex) {
            }

            /*
             * Wenn keine Anfangsadresse bekannt ist,
             * dann versuchen, die Ladeadressen aus dem Dateinamen zu ermitteln
             */
            if (rv != null) {
                if (rv.getBegAddr() < 0) {
                    try {
                        String fName = file.getName();
                        if (fName != null) {
                            int[] addrs = new int[3];
                            Arrays.fill(addrs, -1);

                            int dstIdx = 0;
                            int srcIdx = fName.lastIndexOf('.');
                            if (srcIdx >= 0) {
                                srcIdx -= 5;
                                while ((srcIdx >= 0) && (dstIdx < addrs.length)) {
                                    if ((fName.charAt(srcIdx) != '_')
                                            || !JTCUtil.isHexChar(fName.charAt(srcIdx + 1))
                                            || !JTCUtil.isHexChar(fName.charAt(srcIdx + 2))
                                            || !JTCUtil.isHexChar(fName.charAt(srcIdx + 3))
                                            || !JTCUtil.isHexChar(fName.charAt(srcIdx + 4))) {
                                        break;
                                    }
                                    addrs[dstIdx++] = Integer.parseInt(
                                            fName.substring(srcIdx + 1, srcIdx + 5),
                                            16);
                                    srcIdx -= 5;
                                }
                                switch (dstIdx) {
                                    case 1:
                                        rv.begAddr = addrs[0];
                                        break;
                                    case 2:
                                        rv.begAddr = addrs[1];
                                        rv.endAddr = addrs[0];
                                        break;
                                    case 3:
                                        rv.begAddr = addrs[2];
                                        rv.endAddr = addrs[1];
                                        rv.startAddr = addrs[0];
                                        break;
                                }
                            }
                        }
                    } catch (NumberFormatException ex) {
                    }
                }
            }
        }
        return rv;
    }


    public int getBegAddr() {
        return this.begAddr;
    }


    public int getEndAddr() {
        return this.endAddr;
    }


    public Format getFormat() {
        return this.fmt;
    }


    public String getInfoText() {
        return this.infoText;
    }


    public int getStartAddr() {
        return this.startAddr;
    }


    /* --- Konstruktor --- */

    private FileInfo(
            byte[] header,
            long fileLen,
            int begAddr,
            int endAddr,
            int startAddr,
            Format fmt,
            String infoText) {
        this.header = header;
        this.fileLen = fileLen;
        this.begAddr = begAddr;
        this.endAddr = endAddr;
        this.startAddr = startAddr;
        this.fmt = fmt;
        this.infoText = infoText;
    }


    /* --- private Methoden --- */

    private static FileInfo analyzeFile(
            String fileExt,
            byte[] header,
            long fileLen) {
        FileInfo rv = null;
        if (header != null) {
            int begAddr = -1;
            int endAddr = -1;
            int startAddr = -1;
            Format fmt = null;
            String infoText = null;

            if ((fileLen > 144) && (header.length > 37)) {
                fmt = Format.TAP;
                String s = "\u00C3KC-TAPE by AF.\u0020";
                int n = s.length();
                for (int i = 0; i < n; i++) {
                    if (((int) header[i] & 0xFF) != (int) s.charAt(i)) {
                        fmt = null;
                        break;
                    }
                }

                // KC-BASIC-Dateien werden nicht unterstuetzt
                if ((fmt == Format.TAP)
                        && ((header[17] < 0xD3) || (header[17] > 0xD8))
                        && ((header[18] < 0xD3) || (header[18] > 0xD8))
                        && ((header[19] < 0xD3) || (header[19] > 0xD8))
                        && ((header[33] == 2) || (header[33] == 3))) {
                    begAddr = getWord(header, 34);
                    endAddr = getWord(header, 36);
                    if (endAddr == 0) {
                        endAddr = 0xFFFF;
                    }
                    StringBuilder buf = new StringBuilder(11);
                    buf.append("KC-TAP-Datei: ");
                    buf.append(String.format("%04X-%04X ", begAddr, endAddr));
                    if (header[33] == 3) {
                        startAddr = getWord(header, 38);
                        buf.append(String.format(
                                "Start=%02X%02X ",
                                header[39] & 0xFF,
                                header[38] & 0xFF));
                    }
                    appendFileDesc(buf, header, 17);
                    infoText = buf.toString();
                } else {
                    fmt = null;
                }
            }
            if ((fmt == null) && (fileLen > 10) && (header.length > 10)) {
                char c3 = (char) (header[3] & 0xFF);
                char c4 = (char) (header[4] & 0xFF);
                char c5 = (char) (header[5] & 0xFF);
                char c6 = (char) (header[6] & 0xFF);
                if ((header[0] == ':')
                        && isHexByte(header[1])
                        && isHexByte(header[2])
                        && JTCUtil.isHexChar(c3)
                        && JTCUtil.isHexChar(c4)
                        && JTCUtil.isHexChar(c5)
                        && JTCUtil.isHexChar(c6)
                        && isHexByte(header[7])
                        && isHexByte(header[8])
                        && isHexByte(header[9])
                        && isHexByte(header[10])) {
                    fmt = Format.HEX;
                    String s = new String(new char[]{c3, c4, c5, c6});
                    infoText = "Intel-HEX-Datei: " + s;
                    try {
                        begAddr = Integer.parseInt(s, 16);
                    } catch (NumberFormatException ex) {
                    }
                }
            }
            if ((fmt == null) && (fileLen > 127) && (header.length > 20)) {
                if ((header[16] == 2) || (header[16] == 3)) {
                    begAddr = getWord(header, 17);
                    endAddr = getWord(header, 19);
                    if (endAddr == 0) {
                        endAddr = 0xFFFF;
                    }
                    if (begAddr <= endAddr) {
                        String fileType = "JTC";
                        if (fileExt != null) {
                            if (fileExt.startsWith("KC") && (fileExt.length() == 3)) {
                                fileType = fileExt;
                            }
                        }

                        StringBuilder buf = new StringBuilder(64);
                        if (header[16] == 3) {
                            startAddr = getWord(header, 21);
                            buf.append(String.format(
                                    "%s-Datei: %04X-%04X Start=%02X%02X ",
                                    fileType,
                                    begAddr,
                                    endAddr,
                                    header[22] & 0xFF,
                                    header[21] & 0xFF));
                        } else {
                            buf.append(String.format(
                                    "%s-Datei: %04X-%04X ",
                                    fileType,
                                    begAddr,
                                    endAddr));
                        }
                        if (appendFileDesc(buf, header, 0)) {
                            infoText = buf.toString();
                        }
                        fmt = Format.JTC;
                    }
                }
            }
            if (fmt == null) {
                fmt = Format.BIN;
            }
            rv = new FileInfo(
                    header,
                    fileLen,
                    begAddr,
                    endAddr,
                    startAddr,
                    fmt,
                    infoText);
        }
        return rv;
    }


    /*
     * Die Methode haengt an den uebergebenen StringBuilder
     * die ab der Position pos stehende Dateibezeichnung an.
     * Nullbytes werden in Leerzeichen gewandelt
     *
     * Rueckgabewert:
     *  true:  alle angehaengten Zeichen waren druckbare Zeichen
     *  false: einige Zeichen waren nicht druckbar
     */
    private static boolean appendFileDesc(
            StringBuilder dst,
            byte[] header,
            int pos) {
        boolean rv = true;
        int n = 11;
        int nSp = 0;
        while ((n > 0) && (pos < header.length)) {
            int b = (int) header[pos++] & 0xFF;
            if ((b == 0) || (b == 0x20)) {
                nSp++;
            } else {
                while (nSp > 0) {
                    dst.append((char) '\u0020');
                    --nSp;
                }
                if ((b > 0x20) && Character.isDefined(b)) {
                    dst.append((char) b);
                } else {
                    dst.append((char) '?');
                    rv = false;
                }
            }
            --n;
        }
        return rv;
    }


    private static int getWord(byte[] a, int idx) {
        int rv = -1;
        if (a != null) {
            if (a.length > (idx + 1)) {
                rv = (((int) a[idx + 1] << 8) & 0xFF00) | ((int) a[idx] & 0xFF);
            }
        }
        return rv;
    }


    /* --- private Methoden --- */

    private static boolean isHexByte(byte b) {
        return JTCUtil.isHexChar((char) ((int) b & 0xFF));
    }
}
