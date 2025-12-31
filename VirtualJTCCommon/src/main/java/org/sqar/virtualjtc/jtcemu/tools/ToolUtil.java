/*
 * (c) 2019-2021 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * plattformunabhaengige Hilfsfunktionen fuer die Werkzeuge
 */

package org.sqar.virtualjtc.jtcemu.tools;

import org.sqar.virtualjtc.jtcemu.base.JTCSys;


public class ToolUtil {
    public static int getBasicEndAddress(JTCSys jtcSys) {
        int endAddr = -1;
        int maxAddr = 0xFBFF;
        if (jtcSys.getOSType() == JTCSys.OSType.ES23) {
            maxAddr = 0xF7FF;
        }
        boolean hasCR = false;
        boolean lastCR = false;
        int addr = 0xE000;
        while (addr <= maxAddr) {
            int b = jtcSys.getMemByte(addr, false);
            if (b == 0x0D) {
                hasCR = true;
                lastCR = true;
            } else {
                if (lastCR && (b == 0)) {
                    endAddr = addr;
                    break;
                }
                lastCR = false;
            }
            addr++;
        }
        return hasCR && (endAddr > 0xE002) ? endAddr : -1;
    }


    public static int getLineNumFromLineNumMsg(String text, int startIdx) {
        int rv = -1;
        if ((text != null) && (startIdx >= 0)) {
            int len = text.length();
            if (startIdx < len) {

                // betreffende Textzeile extrahieren
                int begIdx = startIdx;
                while (begIdx > 0) {
                    char ch = text.charAt(begIdx - 1);
                    if (ch == '\n') {
                        break;
                    }
                    --begIdx;
                }
                int endIdx = startIdx;
                while (endIdx < len) {
                    char ch = text.charAt(endIdx);
                    if (ch == '\n') {
                        break;
                    }
                    endIdx++;
                }
                String line = text.substring(begIdx, endIdx);

                // Zeilennummer hinter dem Wort "Zeile" ermitteln
                int idx = line.indexOf("Zeile"); // TODO: i18n
                if (idx >= 0) {
                    idx += 5;
                    int lineLen = text.length();
                    while (idx < lineLen) {
                        if (line.charAt(idx) != '\u0020') {
                            break;
                        }
                        idx++;
                    }
                    int lineNum = 0;
                    while (idx < lineLen) {
                        char ch = line.charAt(idx++);
                        if ((ch < '0') || (ch > '9')) {
                            break;
                        }
                        lineNum = (lineNum * 10) + (ch - '0');
                    }
                    if (lineNum > 0) {
                        rv = lineNum;
                    }
                }
            }
        }
        return rv;
    }


    /* --- Konstruktor --- */

    private ToolUtil() {
        // nicht instanziierbar
    }
}
