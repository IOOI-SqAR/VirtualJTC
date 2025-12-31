/*
 * (c) 2019-2020 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Optionen fuer den Assembler
 */

package org.sqar.virtualjtc.jtcemu.tools.assembler;

import org.sqar.virtualjtc.jtcemu.base.AppContext;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;


public class AsmOptions {
    private static final String OPTION_CODE_TO_EMU = "code.to_emulator";
    private static final String OPTION_CODE_TO_FILE = "code.to_file";
    private static final String OPTION_CODE_FILENAME = "code.file.name";
    private static final String OPTION_LIST_LABELS = "labels.list";
    private static final String OPTION_LABELS_IGNORE_CASE
            = "labels.ignore_case";
    private static final String OPTION_WARN_NON_ASCII_CHARS
            = "warn_non_ascii_chars";

    private static String prefix = null;

    private Map<String, AsmLabel> predefinedLabels;
    private boolean codeToEmu;
    private boolean codeToFile;
    private File codeFile;
    private boolean labelsIgnoreCase;
    private boolean listLabels;
    private boolean warnNonAsciiChars;


    public AsmOptions() {
        this(null);
    }


    public AsmOptions(AsmOptions src) {
        if (prefix == null) {
            prefix = AppContext.getPropPrefix() + "asm.";
        }
        this.predefinedLabels = new HashMap<>();
        if (src != null) {
            this.codeToEmu = src.codeToEmu;
            this.codeToFile = src.codeToFile;
            this.codeFile = src.codeFile;
            this.labelsIgnoreCase = src.labelsIgnoreCase;
            this.listLabels = src.listLabels;
            this.warnNonAsciiChars = src.warnNonAsciiChars;
            this.predefinedLabels.putAll(src.predefinedLabels);
        } else {
            this.codeToEmu = true;
            this.codeToFile = false;
            this.codeFile = null;
            this.labelsIgnoreCase = false;
            this.listLabels = false;
            this.warnNonAsciiChars = false;
            this.predefinedLabels.put(
                    Z8Assembler.BUILT_IN_LABEL,
                    new AsmLabel(Z8Assembler.BUILT_IN_LABEL, -1));
        }
    }


    public void addLabel(String name, int value) throws AsmException {
        name = AsmUtil.checkLabelName(name, this.labelsIgnoreCase);
        if (this.predefinedLabels.containsKey(name)) {
            AsmUtil.throwLabelAlreadyExists(name);
        }
        this.predefinedLabels.put(name, new AsmLabel(name, value));
    }


    public static AsmOptions createOf(Properties props) {
        AsmOptions options = new AsmOptions();
        if (props != null) {
            options.codeToEmu = getBoolean(
                    props,
                    OPTION_CODE_TO_EMU,
                    false);

            options.codeToFile = getBoolean(
                    props,
                    OPTION_CODE_TO_FILE,
                    false);

            String codeFileName = props.getProperty(OPTION_CODE_FILENAME);
            if (codeFileName != null) {
                if (!codeFileName.isEmpty()) {
                    options.codeFile = new File(codeFileName);
                }
            }

            options.labelsIgnoreCase = getBoolean(
                    props,
                    OPTION_LABELS_IGNORE_CASE,
                    false);

            options.listLabels = getBoolean(
                    props,
                    OPTION_LIST_LABELS,
                    false);

            options.warnNonAsciiChars = getBoolean(
                    props,
                    OPTION_WARN_NON_ASCII_CHARS,
                    false);
        }
        return options;
    }


    public File getCodeFile() {
        return this.codeFile;
    }


    public boolean getCodeToEmu() {
        return this.codeToEmu;
    }


    public boolean getCodeToFile() {
        return this.codeToFile;
    }


    public boolean getLabelsIgnoreCase() {
        return this.labelsIgnoreCase;
    }


    public boolean getListLabels() {
        return this.listLabels;
    }


    public Map<String, AsmLabel> getPredefinedLabels() {
        return this.predefinedLabels;
    }


    public boolean getWarnNonAsciiChars() {
        return this.warnNonAsciiChars;
    }


    public void putOptionsTo(Properties props) {
        if (props != null) {
            props.setProperty(
                    prefix + OPTION_CODE_TO_EMU,
                    Boolean.toString(this.codeToEmu));

            props.setProperty(
                    prefix + OPTION_CODE_TO_FILE,
                    Boolean.toString(this.codeToFile));

            String codeFileName = null;
            if (this.codeFile != null) {
                codeFileName = this.codeFile.getPath();
            }
            props.setProperty(
                    prefix + OPTION_CODE_FILENAME,
                    codeFileName != null ? codeFileName : "");

            props.setProperty(
                    prefix + OPTION_LABELS_IGNORE_CASE,
                    Boolean.toString(this.labelsIgnoreCase));

            props.setProperty(
                    prefix + OPTION_LIST_LABELS,
                    Boolean.toString(this.listLabels));

            props.setProperty(
                    prefix + OPTION_WARN_NON_ASCII_CHARS,
                    Boolean.toString(this.warnNonAsciiChars));
        }
    }


    /*
     * Die Methode vergleicht nur die eigentlichen Optionen,
     * die auch in die Profildatei geschrieben werden.
     */
    public boolean sameOptions(AsmOptions o) {
        return o != null ?
                (equals(this.codeFile, o.codeFile)
                        && (this.codeToEmu == o.codeToEmu)
                        && (this.codeToFile == o.codeToFile)
                        && (this.labelsIgnoreCase == o.labelsIgnoreCase)
                        && (this.listLabels == o.listLabels)
                        && (this.warnNonAsciiChars == o.warnNonAsciiChars))
                : false;
    }


    public void setCodeToEmu(boolean state) {
        this.codeToEmu = state;
    }


    public void setCodeToFile(boolean state, File file) {
        this.codeToFile = state;
        this.codeFile = file;
    }


    public void setLabelsIgnoreCase(boolean state) {
        this.labelsIgnoreCase = state;
    }


    public void setListLabels(boolean state) {
        this.listLabels = state;
    }


    public void setWarnNonAsciiChars(boolean state) {
        this.warnNonAsciiChars = state;
    }


    /* --- geschuetzte Methoden --- */

    protected static boolean equals(Object o1, Object o2) {
        boolean rv = false;
        if ((o1 != null) && (o2 != null)) {
            rv = o1.equals(o2);
        } else if ((o1 == null) && (o2 == null)) {
            rv = true;
        }
        return rv;
    }


    protected static boolean getBoolean(
            Properties props,
            String keyword,
            boolean defaultValue) {
        boolean rv = defaultValue;
        if (props != null) {
            String value = props.getProperty(prefix + keyword);
            if (value != null) {
                if (!value.isEmpty()) {
                    rv = Boolean.parseBoolean(value);
                }
            }
        }
        return rv;
    }
}
