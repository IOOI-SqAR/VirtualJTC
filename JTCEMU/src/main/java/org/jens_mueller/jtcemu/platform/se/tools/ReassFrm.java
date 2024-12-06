/*
 * (c) 2007-2021 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Fenster fuer Reassembler
 */

package org.jens_mueller.jtcemu.platform.se.tools;

import org.jens_mueller.jtcemu.base.AppContext;
import org.jens_mueller.jtcemu.base.JTCUtil;
import org.jens_mueller.jtcemu.base.UserInputException;
import org.jens_mueller.jtcemu.platform.se.Main;
import org.jens_mueller.jtcemu.platform.se.base.*;
import org.jens_mueller.jtcemu.tools.assembler.AsmUtil;
import org.jens_mueller.z8.Z8Memory;
import org.jens_mueller.z8.Z8Reassembler;

import javax.swing.*;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;


public class ReassFrm extends AbstractTextFrm implements
        ActionListener,
        CaretListener {
    private static final String FILE_GROUP_REASS = "reass";
    private static final String FILE_GROUP_SOURCE = "reass_to_source";
    private static final String LABEL_BEG_ADDR = "Anfangsadresse:";
    private static final String LABEL_END_ADDR = "Endadresse:";

    private static ReassFrm instance = null;
    private static Point lastLocation = null;

    private Z8Reassembler z8Reass;
    private TextFinder textFinder;
    private File file;
    private File sourceFile;
    private String labelPrefix;
    private JTextComponent selectionFld;
    private int begAddr;
    private int endAddr;
    private JButton btnReassemble;
    private JMenuItem mnuReassemble;
    private JMenuItem mnuSaveAs;
    private JMenuItem mnuSourceCopy;
    private JMenuItem mnuSourceSaveAs;
    private JMenuItem mnuPrintOptions;
    private JMenuItem mnuPrint;
    private JMenuItem mnuClose;
    private JMenuItem mnuCopy;
    private JMenuItem mnuFind;
    private JMenuItem mnuFindNext;
    private JMenuItem mnuFindPrev;
    private JMenuItem mnuSelectAll;
    private JMenuItem mnuHelpContent;
    private JTextField fldBegAddr;
    private JTextField fldEndAddr;


    public static void close() {
        if (instance != null)
            instance.doClose();
    }


    public static void open(Z8Memory memory) {
        if (instance == null) {
            instance = new ReassFrm(memory);
            if (lastLocation != null) {
                instance.setLocation(lastLocation);
            }
            instance.setVisible(true);
        }
        GUIUtil.toFront(instance);
    }


    public static void reset() {
        if (instance != null)
            instance.updView();
    }


    /* --- ActionListener --- */

    @Override
    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();
        if (src != null) {
            if (src == this.fldBegAddr) {
                this.fldEndAddr.requestFocus();
            } else if ((src == this.fldEndAddr)
                    || (src == this.btnReassemble)
                    || (src == this.mnuReassemble)) {
                doReassemble();
            } else if (src == this.mnuSaveAs) {
                doSaveAs();
            } else if (src == this.mnuSourceCopy) {
                doSourceCopy();
            } else if (src == this.mnuSourceSaveAs) {
                doSourceSaveAs();
            } else if (src == this.mnuPrintOptions) {
                PrintOptionsDlg.open(this);
            } else if (src == this.mnuPrint) {
                doPrint();
            } else if (src == this.mnuClose) {
                doClose();
            } else if (src == this.mnuCopy) {
                if (this.selectionFld != null) {
                    this.selectionFld.copy();
                }
            } else if (src == this.mnuFind) {
                doFind();
            } else if (src == this.mnuFindNext) {
                doFindNext();
            } else if (src == this.mnuFindPrev) {
                doFindPrev();
            } else if (src == this.mnuSelectAll) {
                this.textArea.requestFocus();
                this.textArea.selectAll();
            } else if (src == this.mnuHelpContent) {
                HelpFrm.open("/help/common/reassembler.htm");
            }
        }
    }


    /* --- CaretListener --- */

    @Override
    public void caretUpdate(CaretEvent e) {
        Object src = e.getSource();
        if (src != null) {
            if (src instanceof JTextComponent) {
                this.selectionFld = (JTextComponent) src;
                int begPos = this.selectionFld.getSelectionStart();
                this.mnuCopy.setEnabled((begPos >= 0)
                        && (begPos < this.selectionFld.getSelectionEnd()));
            }
        }
    }


    /* --- ueberschriebene Methoden --- */

    @Override
    protected boolean doClose() {
        boolean rv = super.doClose();
        if (rv) {
            lastLocation = getLocation();
            instance = null;
        }
        return rv;
    }


    @Override
    public String getPropPrefix() {
        return "reassembler.";
    }


    /* --- Konstruktor --- */

    private ReassFrm(Z8Memory memory) {
        setTitle(AppContext.getAppName() + " Reassembler");
        this.z8Reass = new Z8Reassembler(memory);
        this.textFinder = new TextFinder();
        this.labelPrefix = "M_";
        this.file = null;
        this.sourceFile = null;
        this.selectionFld = null;
        this.begAddr = -1;
        this.endAddr = -1;


        // Menu
        JMenuBar mnuBar = new JMenuBar();
        setJMenuBar(mnuBar);


        // Menu Datei
        JMenu mnuFile = new JMenu("Datei");
        mnuFile.setMnemonic(KeyEvent.VK_D);
        mnuBar.add(mnuFile);

        this.mnuReassemble = new JMenuItem("Reassemblieren");
        this.mnuReassemble.setAccelerator(getMenuShortcut(KeyEvent.VK_R));
        this.mnuReassemble.addActionListener(this);
        mnuFile.add(this.mnuReassemble);

        this.mnuSaveAs = new JMenuItem("Speichern unter...");
        this.mnuSaveAs.setAccelerator(
                getMenuShortcutWithShift(KeyEvent.VK_S));
        this.mnuSaveAs.setEnabled(false);
        this.mnuSaveAs.addActionListener(this);
        mnuFile.add(this.mnuSaveAs);
        mnuFile.addSeparator();

        JMenu mnuSource = new JMenu("Als Assembler-Quelltext");
        mnuFile.add(mnuSource);

        this.mnuSourceCopy = new JMenuItem("kopieren");
        this.mnuSourceCopy.setEnabled(false);
        this.mnuSourceCopy.addActionListener(this);
        mnuSource.add(this.mnuSourceCopy);

        this.mnuSourceSaveAs = new JMenuItem("speichern unter...");
        this.mnuSourceSaveAs.setEnabled(false);
        this.mnuSourceSaveAs.addActionListener(this);
        mnuSource.add(this.mnuSourceSaveAs);

        this.mnuPrintOptions = new JMenuItem("Druckoptionen...");
        this.mnuPrintOptions.addActionListener(this);
        mnuFile.add(this.mnuPrintOptions);

        this.mnuPrint = new JMenuItem("Drucken...");
        this.mnuPrint.setAccelerator(getMenuShortcut(KeyEvent.VK_P));
        this.mnuPrint.setEnabled(false);
        this.mnuPrint.addActionListener(this);
        mnuFile.add(this.mnuPrint);
        mnuFile.addSeparator();

        this.mnuClose = new JMenuItem("Schlie\u00DFen");
        this.mnuClose.addActionListener(this);
        mnuFile.add(this.mnuClose);


        // Menu Bearbeiten
        JMenu mnuEdit = new JMenu("Bearbeiten");
        mnuEdit.setMnemonic(KeyEvent.VK_B);
        mnuBar.add(mnuEdit);

        this.mnuCopy = new JMenuItem("Kopieren");
        this.mnuCopy.setAccelerator(getMenuShortcut(KeyEvent.VK_C));
        this.mnuCopy.setEnabled(false);
        this.mnuCopy.addActionListener(this);
        mnuEdit.add(this.mnuCopy);
        mnuEdit.addSeparator();

        this.mnuFind = new JMenuItem("Suchen...");
        this.mnuFind.setAccelerator(getMenuShortcut(KeyEvent.VK_F));
        this.mnuFind.setEnabled(false);
        this.mnuFind.addActionListener(this);
        mnuEdit.add(this.mnuFind);

        this.mnuFindNext = new JMenuItem("Weitersuchen");
        this.mnuFindNext.setAccelerator(
                KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0));
        this.mnuFindNext.setEnabled(false);
        this.mnuFindNext.addActionListener(this);
        mnuEdit.add(this.mnuFindNext);

        this.mnuFindPrev = new JMenuItem("R\u00FCckw\u00E4rts suchen");
        this.mnuFindPrev.setAccelerator(
                KeyStroke.getKeyStroke(
                        KeyEvent.VK_F3,
                        InputEvent.SHIFT_DOWN_MASK));
        this.mnuFindPrev.setEnabled(false);
        this.mnuFindPrev.addActionListener(this);
        mnuEdit.add(this.mnuFindPrev);
        mnuEdit.addSeparator();

        this.mnuSelectAll = new JMenuItem("Alles ausw\u00E4hlen");
        this.mnuSelectAll.setEnabled(false);
        this.mnuSelectAll.addActionListener(this);
        mnuEdit.add(this.mnuSelectAll);


        // Menu Hilfe
        JMenu mnuHelp = new JMenu("Hilfe");
        mnuHelp.setMnemonic(KeyEvent.VK_H);
        mnuBar.add(mnuHelp);

        this.mnuHelpContent = new JMenuItem("Hilfe zum Reassembler...");
        this.mnuHelpContent.addActionListener(this);
        mnuHelp.add(this.mnuHelpContent);


        // Fensterinhalt
        setLayout(new BorderLayout());


        // Kopfbereich
        JPanel panelHead = new JPanel(new GridBagLayout());
        add(panelHead, BorderLayout.NORTH);

        GridBagConstraints gbcHead = new GridBagConstraints(
                0, 0,
                1, 1,
                0.0, 0.0,
                GridBagConstraints.WEST,
                GridBagConstraints.NONE,
                new Insets(5, 5, 5, 5),
                0, 0);

        panelHead.add(new JLabel(LABEL_BEG_ADDR), gbcHead);

        this.fldBegAddr = new JTextField(4);
        this.fldBegAddr.addActionListener(this);
        this.fldBegAddr.addCaretListener(this);
        gbcHead.fill = GridBagConstraints.HORIZONTAL;
        gbcHead.weightx = 0.5;
        gbcHead.gridx++;
        panelHead.add(this.fldBegAddr, gbcHead);

        gbcHead.fill = GridBagConstraints.NONE;
        gbcHead.weightx = 0.0;
        gbcHead.gridx++;
        panelHead.add(new JLabel(LABEL_END_ADDR), gbcHead);

        this.fldEndAddr = new JTextField(4);
        this.fldEndAddr.addActionListener(this);
        this.fldEndAddr.addCaretListener(this);
        gbcHead.fill = GridBagConstraints.HORIZONTAL;
        gbcHead.weightx = 0.5;
        gbcHead.gridx++;
        panelHead.add(this.fldEndAddr, gbcHead);

        this.btnReassemble = GUIUtil.createImageButton(
                this,
                "/images/file/reload.png",
                "Reassemblieren");
        gbcHead.fill = GridBagConstraints.NONE;
        gbcHead.weightx = 0.0;
        gbcHead.gridx++;
        panelHead.add(this.btnReassemble, gbcHead);


        // Ergebnisbereich
        this.textArea.setColumns(40);
        this.textArea.setRows(20);
        this.textArea.setEditable(false);
        this.textArea.setMargin(new Insets(5, 5, 5, 5));
        this.textArea.addCaretListener(this);
        add(new JScrollPane(this.textArea), BorderLayout.CENTER);


        // sonstiges
        setResizable(true);
        if (!GUIUtil.applyWindowSettings(this)) {
            pack();
            setLocationByPlatform(true);
            this.fldBegAddr.setColumns(0);
            this.fldEndAddr.setColumns(0);
            this.textArea.setColumns(0);
            this.textArea.setRows(0);
        }
    }


    /* --- Aktionen --- */

    private void doFind() {
        this.textFinder.openFindDlg(this.textArea);
        if (textFinder.hasSearchText()) {
            this.mnuFindPrev.setEnabled(true);
            this.mnuFindNext.setEnabled(true);
        }
    }


    private void doFindNext() {
        this.textFinder.findNext(this.textArea);
    }


    private void doFindPrev() {
        this.textFinder.findPrev(this.textArea);
    }


    private void doReassemble() {
        try {
            this.begAddr = GUIUtil.parseHex4(this.fldBegAddr, LABEL_BEG_ADDR);
            this.endAddr = GUIUtil.parseHex4(this.fldEndAddr, LABEL_END_ADDR);
            if (this.endAddr < this.begAddr) {
                throw new UserInputException(
                        "Endadresse ist kleiner als Anfangsadresse.");
            }
            updView();
        } catch (UserInputException ex) {
            JOptionPane.showMessageDialog(
                    this,
                    ex.getMessage(),
                    "Eingabefehler",
                    JOptionPane.ERROR_MESSAGE);
        }
    }


    private void doSaveAs() {
        saveTextFileAs(
                this.textArea.getText(),
                "Textdatei speichern",
                this.file,
                FILE_GROUP_REASS,
                GUIUtil.textFileFilter);
    }


    private void doSourceCopy() {
        String sourceText = createSourceText();
        if (sourceText != null) {
            if (!GUIUtil.copyToClipboard(this, sourceText)) {
                Main.showError(
                        this,
                        "Der erzeugte Assembler-Quelltext konnte nicht\n"
                                + "in die Zwischenablage kopiert werden.");
            }
        }
    }


    private void doSourceSaveAs() {
        saveTextFileAs(
                createSourceText(),
                "Assembler-Quelltext speichern",
                this.sourceFile,
                FILE_GROUP_SOURCE,
                GUIUtil.asmFileFilter);
    }


    /* --- private Methoden --- */

    private String createSourceText() {
        String text = null;
        if ((this.z8Reass != null)
                && (this.begAddr >= 0) && (this.begAddr < this.endAddr)) {
            String labelPrefix = JOptionPane.showInputDialog(
                    this,
                    "Prefix f\u00FCr Marken:",
                    this.labelPrefix);
            if (labelPrefix != null) {
                boolean prefixOK = false;
                int len = labelPrefix.length();
                if (len > 0) {
                    prefixOK = AsmUtil.isIdentifierStart(labelPrefix.charAt(0));
                    for (int i = 1; i < len; i++) {
                        prefixOK &= AsmUtil.isIdentifierPart(labelPrefix.charAt(i));
                    }
                }
                if (prefixOK) {
                    this.labelPrefix = labelPrefix;
                    text = this.z8Reass.reassembleToSource(
                            this.begAddr,
                            this.endAddr,
                            labelPrefix);
                } else {
                    BaseDlg.showError(
                            this,
                            "Der Pr\u00E4fix entspricht nicht den Namenskonventionen"
                                    + " f\u00FCr Assembler-Marken.");
                }
            }
        }
        return text;
    }


    private void saveTextFileAs(
            String text,
            String title,
            File oldFile,
            String fileGroup,
            FileFilter... fileFilters) {
        if (text != null) {
            File file = FileDlg.showFileSaveDlg(
                    this,
                    title,
                    oldFile != null ?
                            oldFile
                            : AppContext.getLastDirFile(fileGroup),
                    GUIUtil.asmFileFilter);
            if (file != null) {
                try {
                    BufferedWriter out = null;
                    try {
                        out = new BufferedWriter(new FileWriter(file));
                        int len = text.length();
                        for (int i = 0; i < len; i++) {
                            char ch = text.charAt(i);
                            if (ch == '\n') {
                                out.newLine();
                            } else {
                                out.write(ch);
                            }
                        }
                        out.close();
                        out = null;
                        this.sourceFile = file;
                        AppContext.setLastFile(FILE_GROUP_SOURCE, file);
                    } finally {
                        JTCUtil.closeSilently(out);
                    }
                } catch (IOException ex) {
                    Main.showError(this, ex);
                }
            }
        }
    }


    private void updView() {
        if ((this.begAddr >= 0) && (this.begAddr <= this.endAddr)) {
            String text = this.z8Reass.reassemble(this.begAddr, this.endAddr);
            setText(text);
            this.textArea.requestFocus();
            if (!text.isEmpty()) {
                this.mnuSaveAs.setEnabled(true);
                this.mnuSourceCopy.setEnabled(true);
                this.mnuSourceSaveAs.setEnabled(true);
                this.mnuPrint.setEnabled(true);
                this.mnuFind.setEnabled(true);
                this.mnuSelectAll.setEnabled(true);
            }
        }
    }
}
