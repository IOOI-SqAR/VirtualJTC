/*
 * (c) 2019-2020 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Kapselung eines Quelltextes
 */

package org.sqar.virtualjtc.jtcemu.tools.assembler;

import org.sqar.virtualjtc.jtcemu.base.JTCUtil;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.PatternSyntaxException;


public class AsmSource {
    private java.util.List<String> lines;
    private String text;
    private String orgText;
    private String name;
    private File file;
    private boolean eof;
    private int eol;
    private int bol;
    private int pos;
    private int lineNum;
    private Map<Integer, Integer> lineNum2Addr;


    public File getFile() {
        return this.file;
    }


    public String getCurLine() {
        String rv = null;
        if (this.lines != null) {
            int lineNum = this.lineNum - 1;
            if ((lineNum >= 0) && (lineNum < this.lines.size())) {
                rv = this.lines.get(lineNum);
            }
        } else if (this.text != null) {
            if (this.bol >= 0) {
                if (this.eol >= this.bol) {
                    rv = this.text.substring(this.bol, this.eol);
                } else {
                    rv = this.text.substring(this.bol);
                }
            }
        }
        return rv;
    }


    public static File getIncludeFile(
            AsmSource baseSource,
            String fileName) {
        File file = null;
        File dirFile = null;
        if (baseSource != null) {
            File baseFile = baseSource.getFile();
            if (baseFile != null) {
                dirFile = baseFile.getParentFile();
            }
        }
        if (!fileName.startsWith("/")
                && (fileName.indexOf('/') >= 0)
                && ((File.separatorChar == '/')
                || (fileName.indexOf(File.separatorChar) < 0))) {
            // plattformunabhaengige relative Pfadangabe
            try {
                String[] items = fileName.split("/");
                if (items != null) {
                    if (items.length > 0) {
                        file = dirFile;
                        for (String s : items) {
                            if (file != null) {
                                file = new File(file, s);
                            } else {
                                file = new File(s);
                            }
                        }
                    }
                }
            } catch (PatternSyntaxException ex) {
            }
        }
        if (file == null) {
            file = new File(fileName);
            if ((dirFile != null) && !file.isAbsolute()) {
                file = new File(dirFile, fileName);
            }
        }
        return file;
    }


    public Map<Integer, Integer> getLineAddrMap() {
        int n = 0;
        if (this.lineNum2Addr != null) {
            n = this.lineNum2Addr.size();
        }
        return this.lineNum2Addr;
    }


    public int getLineNum() {
        return this.lineNum;
    }


    public String getName() {
        return this.name;
    }


    public String getOrgText() {
        return this.orgText;
    }


    public static AsmSource readFile(File file) throws IOException {
        return new AsmSource(
                readLines(new FileReader(file)),
                null,
                file.getName(),
                file);
    }


    public static AsmSource readText(
            String text,
            String name,
            File file) {
        if ((name == null) && (file != null)) {
            name = file.getName();
        }
        return new AsmSource(null, text, name, file);
    }


    public String readLine() {
        String rv = null;
        if (!this.eof) {
            if (this.lines != null) {
                if ((this.lineNum >= 0) && (this.lineNum < this.lines.size())) {
                    rv = this.lines.get(this.lineNum++);
                }
            } else if (this.text != null) {
                int len = this.text.length();
                if (this.pos < len) {
                    int eol = this.text.indexOf('\n', this.pos);
                    if (eol >= this.pos) {
                        rv = this.text.substring(this.pos, eol);
                        this.bol = this.pos;
                        this.eol = eol;
                        this.pos = eol + 1;
                    } else {
                        rv = this.text.substring(this.pos);
                        this.bol = this.pos;
                        this.eol = -1;
                        this.pos = len;
                    }
                    this.lineNum++;
                }
            }
        }
        return rv;
    }


    public void reset() {
        this.lineNum = 0;
        this.pos = 0;
        this.eof = false;
        if (this.lineNum2Addr != null) {
            this.lineNum2Addr.clear();
        }
    }


    public void setEOF() {
        this.eof = true;
    }


    public void setLineAddr(int addr) {
        if (this.lineNum2Addr == null) {
            this.lineNum2Addr = new HashMap<>();
        }
        this.lineNum2Addr.put(this.lineNum, addr);
    }


    /* --- Konstruktor --- */

    private AsmSource(
            java.util.List<String> lines,
            String text,
            String name,
            File file) {
        this.lines = lines;
        this.file = file;
        this.text = text;
        this.orgText = text;
        this.name = name;
        this.bol = -1;
        this.eol = -1;
        this.pos = 0;
        this.lineNum = 0;
        this.lineNum2Addr = null;
    }


    /* --- private Methoden --- */

    private static java.util.List<String> readLines(Reader in)
            throws IOException {
        java.util.List<String> lines = new ArrayList<>(0x1000);
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(in);
            String line = reader.readLine();
            while (line != null) {
                lines.add(line);
                line = reader.readLine();
            }
        } finally {
            JTCUtil.closeSilently(reader);
        }
        return lines;
    }
}
