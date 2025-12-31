/*
 * (c) 2019-2020 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Parser fuer einen Ausdruck
 */

package org.sqar.virtualjtc.jtcemu.tools.assembler;

import org.sqar.virtualjtc.jtcemu.base.JTCUtil;

import java.text.CharacterIterator;
import java.util.Arrays;
import java.util.Map;


public class ExprParser {
    private static String[] sortedReservedWords = {
            "AND", "LAND", "LNOT", "LOR", "LXOR", "MOD", "NOT", "OR",
            "SHL", "SHR", "XOR"};

    private CharacterIterator iter;
    private int instBegAddr;
    private Map<String, AsmLabel> labels;
    private boolean checkLabels;
    private boolean labelsIgnoreCase;


    public static boolean isReservedWord(String text) {
        return (Arrays.binarySearch(sortedReservedWords, text) >= 0);
    }


    public static Integer parseExpr(
            CharacterIterator iter,
            Z8Assembler asm,
            boolean checkLabels) throws AsmException {
        return (new ExprParser(
                iter,
                asm.getInstBegAddr(),
                asm.getLabels(),
                asm.getOptions().getLabelsIgnoreCase(),
                checkLabels)).parseExpr(asm);
    }


    public static int parseNumber(CharacterIterator iter)
            throws AsmException {
        Integer value = checkParseNumber(iter);
        if (value == null) {
            throw new AsmException("Zahl erwartet"); // TODO: i18n
        }
        return value.intValue();
    }


    public static int readIntNumber(CharacterIterator iter)
            throws AsmException {
        int value = 0;
        char ch = iter.current();
        while ((ch >= '0') && (ch <= '9')) {
            value = (value * 10) + (ch - '0');
            checkMaxNumber(value);
            ch = iter.next();
        }
        return value;
    }


    /* --- Konstruktor --- */

    private ExprParser(
            CharacterIterator iter,
            int instBegAddr,
            Map<String, AsmLabel> labels,
            boolean labelsIgnoreCase,
            boolean checkLabels) {
        this.iter = iter;
        this.instBegAddr = instBegAddr;
        this.labels = labels;
        this.checkLabels = checkLabels;
        this.labelsIgnoreCase = labelsIgnoreCase;
    }


    /* --- private Methoden --- */

    private static void checkMaxNumber(int value) throws AsmException {
        if (value > 0xFFFF)
            throw new AsmException("Zahl zu gro\u00DF"); // TODO: i18n
    }


    private static Integer checkParseNumber(CharacterIterator iter)
            throws AsmException {
        Integer value = null;
        if (AsmUtil.checkAndParseToken(iter, "%(2)")) {
            char ch = iter.current();
            if ((ch < '0') || (ch > '1')) {
                throw new AsmException("Ziffer 0 oder 1 erwartet"); // TODO: i18n
            }
            value = Integer.valueOf(readBinNumber(iter));
        } else if (AsmUtil.checkAndParseToken(iter, "%(8)")) {
            char ch = iter.current();
            if ((ch < '0') || (ch > '7')) {
                throw new AsmException("Ziffer 0 bis 7 erwartet"); // TODO: i18n
            }
            int v = ch - '0';
            ch = iter.next();
            while ((ch >= '0') && (ch <= '7')) {
                v = (v << 3) + (ch - '0');
                checkMaxNumber(v);
                ch = iter.next();
            }
            value = Integer.valueOf(v);
        } else {
            char ch = AsmUtil.skipBlanks(iter);
            if (ch == '%') {
                ch = iter.next();
                if (!JTCUtil.isHexChar(ch)) {
                    AsmUtil.throwHexCharExpected(ch);
                }
                value = Integer.valueOf(readHexNumber(iter));
            } else {
                if ((ch >= '0') && (ch <= '9')) {
                    int begIdx = iter.getIndex();
                    try {
                        int v = readHexNumber(iter);
                        ch = iter.current();
                        if ((ch == 'H') || (ch == 'h')) {
                            iter.next();
                            value = Integer.valueOf(v);
                        }
                    } catch (AsmException ex) {
                        value = null;
                    }
                    if (value == null) {
                        iter.setIndex(begIdx);
                        try {
                            int v = readBinNumber(iter);
                            ch = iter.current();
                            if ((ch == 'B') || (ch == 'b')) {
                                iter.next();
                                value = Integer.valueOf(v);
                            }
                        } catch (AsmException ex) {
                            value = null;
                        }
                    }
                    if (value == null) {
                        iter.setIndex(begIdx);
                        value = Integer.valueOf(readIntNumber(iter));
                    }
                }
            }
        }
        return value;
    }


    private Integer parseExpr(Z8Assembler asm) throws AsmException {
        Integer value = parseAddExpr(asm);
        for (; ; ) {
            if (AsmUtil.checkAndParseToken(this.iter, "=")) {
                Integer v2 = parseAddExpr(asm);
                if ((value != null) && (v2 != null)) {
                    value = (value.intValue() == v2.intValue() ? 0xFFFF : 0);
                } else {
                    value = null;
                }
            } else if (AsmUtil.checkAndParseToken(this.iter, "<>")) {
                Integer v2 = parseAddExpr(asm);
                if ((value != null) && (v2 != null)) {
                    value = (value.intValue() != v2.intValue() ? 0xFFFF : 0);
                } else {
                    value = null;
                }
            } else if (AsmUtil.checkAndParseToken(this.iter, "<=")) {
                Integer v2 = parseAddExpr(asm);
                if ((value != null) && (v2 != null)) {
                    value = (value.intValue() <= v2.intValue() ? 0xFFFF : 0);
                } else {
                    value = null;
                }
            } else if (AsmUtil.checkAndParseToken(this.iter, "<")) {
                Integer v2 = parseAddExpr(asm);
                if ((value != null) && (v2 != null)) {
                    value = (value.intValue() < v2.intValue() ? 0xFFFF : 0);
                } else {
                    value = null;
                }
            } else if (AsmUtil.checkAndParseToken(this.iter, ">=")) {
                Integer v2 = parseAddExpr(asm);
                if ((value != null) && (v2 != null)) {
                    value = (value.intValue() >= v2.intValue() ? 0xFFFF : 0);
                } else {
                    value = null;
                }
            } else if (AsmUtil.checkAndParseToken(this.iter, ">")) {
                Integer v2 = parseAddExpr(asm);
                if ((value != null) && (v2 != null)) {
                    value = (value.intValue() > v2.intValue() ? 0xFFFF : 0);
                } else {
                    value = null;
                }
            } else {
                break;
            }
        }
        return value;
    }


    private Integer parseAddExpr(Z8Assembler asm) throws AsmException {
        Integer value = parseMulExpr(asm);
        for (; ; ) {
            if (AsmUtil.checkAndParseToken(this.iter, "+")) {
                Integer v2 = parseMulExpr(asm);
                if ((value != null) && (v2 != null)) {
                    value = (value.intValue() + v2.intValue()) & 0xFFFF;
                } else {
                    value = null;
                }
            } else if (AsmUtil.checkAndParseToken(this.iter, "-")) {
                Integer v2 = parseMulExpr(asm);
                if ((value != null) && (v2 != null)) {
                    value = (value.intValue() - v2.intValue()) & 0xFFFF;
                } else {
                    value = null;
                }
            } else if (AsmUtil.checkAndParseToken(this.iter, "OR", "LOR")) {
                Integer v2 = parseMulExpr(asm);
                if ((value != null) && (v2 != null)) {
                    value = (value.intValue() | v2.intValue()) & 0xFFFF;
                } else {
                    value = null;
                }
            } else if (AsmUtil.checkAndParseToken(this.iter, "XOR", "LXOR")) {
                Integer v2 = parseMulExpr(asm);
                if ((value != null) && (v2 != null)) {
                    value = (value.intValue() ^ v2.intValue()) & 0xFFFF;
                } else {
                    value = null;
                }
            } else {
                break;
            }
        }
        return value;
    }


    private Integer parseMulExpr(Z8Assembler asm) throws AsmException {
        Integer value = parseUnaryExpr(asm);
        for (; ; ) {
            if (AsmUtil.checkAndParseToken(this.iter, "*")) {
                Integer v2 = parseUnaryExpr(asm);
                if ((value != null) && (v2 != null)) {
                    value = (value.intValue() * v2.intValue()) & 0xFFFF;
                } else {
                    value = null;
                }
            } else if (AsmUtil.checkAndParseToken(this.iter, "/")) {
                Integer v2 = parseUnaryExpr(asm);
                if ((value != null) && (v2 != null)) {
                    if (v2.intValue() == 0) {
                        throw new AsmException("Division durch 0"); // TODO: i18n
                    }
                    value = (value.intValue() / v2.intValue()) & 0xFFFF;
                } else {
                    value = null;
                }
            } else if (AsmUtil.checkAndParseToken(this.iter, "AND", "LAND")) {
                Integer v2 = parseUnaryExpr(asm);
                if ((value != null) && (v2 != null)) {
                    value = (value.intValue() & v2.intValue()) & 0xFFFF;
                } else {
                    value = null;
                }
            } else if (AsmUtil.checkAndParseToken(this.iter, "MOD")) {
                Integer v2 = parseUnaryExpr(asm);
                if ((value != null) && (v2 != null)) {
                    if (v2.intValue() == 0) {
                        throw new AsmException("Modulo 0");
                    }
                    value = (value.intValue() % v2.intValue()) & 0xFFFF;
                } else {
                    value = null;
                }
            } else if (AsmUtil.checkAndParseToken(this.iter, "SHL")) {
                Integer v2 = parseUnaryExpr(asm);
                if ((value != null) && (v2 != null)) {
                    value = (value.intValue() << v2.intValue()) & 0xFFFE;
                } else {
                    value = null;
                }
            } else if (AsmUtil.checkAndParseToken(this.iter, "SHR")) {
                Integer v2 = parseUnaryExpr(asm);
                if ((value != null) && (v2 != null)) {
                    value = (value.intValue() >> v2.intValue()) & 0x7FFF;
                } else {
                    value = null;
                }
            } else {
                break;
            }
        }
        return value;
    }


    private Integer parseUnaryExpr(Z8Assembler asm) throws AsmException {
        Integer value = null;
        if (AsmUtil.checkAndParseToken(this.iter, "+")) {
            value = parsePrimExpr(asm);
        } else if (AsmUtil.checkAndParseToken(this.iter, "-")) {
            Integer v2 = parsePrimExpr(asm);
            if (v2 != null) {
                value = -v2.intValue() & 0xFFFF;
            }
        } else if (AsmUtil.checkAndParseToken(this.iter, "NOT", "LNOT")) {
            Integer v2 = parsePrimExpr(asm);
            if (v2 != null) {
                value = ~v2.intValue() & 0xFFFF;
            }
        } else if (AsmUtil.checkAndParseToken(this.iter, "(")) {
            value = parseExpr(asm);
            AsmUtil.parseToken(this.iter, ')');
        } else {
            value = parsePrimExpr(asm);
        }
        return value;
    }


    private Integer parsePrimExpr(Z8Assembler asm) throws AsmException {
        Integer value = null;
        char ch = AsmUtil.skipBlanks(this.iter);
        if (ch == '$') {
            this.iter.next();
            value = this.instBegAddr;
        } else if (ch == '\'') {
            ch = this.iter.next();
            if (ch == CharacterIterator.DONE) {
                throw new AsmException("Unerwartetes Ende des Zeilenende"); // TODO: i18n
            }
            value = Integer.valueOf(ch);
            if (this.iter.next() != '\'') {
                AsmUtil.throwCharExpected('\'');
            }
            this.iter.next();
        } else {
            value = checkParseNumber(iter);
            if (value == null) {
                ch = AsmUtil.skipBlanks(this.iter);
                if (AsmUtil.isIdentifierStart(ch)) {
                    value = parseLabel();
                } else {
                    AsmUtil.throwUnexpectedChar(ch);
                }
            }
        }
        return value;
    }


    private Integer parseLabel() throws AsmException {
        Integer value = null;
        String labelName = AsmUtil.tryReadIdentifier(this.iter, false);
        if (labelName != null) {
            String upperLabelName = labelName.toUpperCase();
            if (RegArg.isRegName(upperLabelName)) {
                throw new AsmException(
                        String.format(
                                "Register %s nicht erwartet", // TODO: i18n
                                labelName));
            }
            AsmLabel label = this.labels.get(
                    this.labelsIgnoreCase ?
                            labelName.toUpperCase()
                            : labelName);
            if (label != null) {
                Object o = label.getLabelValue();
                if (o != null) {
                    if (o instanceof Integer) {
                        value = (Integer) o;
                    }
                }
            } else {
                if (this.checkLabels) {
                    throw new AsmException(
                            "Marke \'" + labelName + "\' nicht definiert"); // TODO: i18n
                }
            }
        }
        return value;
    }


    private static int readBinNumber(CharacterIterator iter)
            throws AsmException {
        int value = 0;
        char ch = iter.current();
        while ((ch >= '0') && (ch <= '1')) {
            value = (value << 1) | (ch - '0');
            checkMaxNumber(value);
            ch = iter.next();
        }
        return value;
    }


    private static int readHexNumber(CharacterIterator iter)
            throws AsmException {
        int value = 0;
        char ch = iter.current();
        while (JTCUtil.isHexChar(ch)) {
            value = (value << 4) | AsmUtil.getHexCharValue(ch);
            checkMaxNumber(value);
            ch = iter.next();
        }
        return value;
    }
}
