/*
 * (c) 2019 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Dialog fuer Assembler-Optionen
 */

package org.sqar.virtualjtc.jtcemu.tools.assembler;

import org.sqar.virtualjtc.jtcemu.base.AppContext;
import org.sqar.virtualjtc.jtcemu.Main;
import org.sqar.virtualjtc.jtcemu.base.BaseDlg;
import org.sqar.virtualjtc.jtcemu.base.FileDlg;
import org.sqar.virtualjtc.jtcemu.base.GUIUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.dnd.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;


public class AsmOptionsDlg
        extends BaseDlg
        implements ActionListener, DropTargetListener {
    private static final String FILE_GROUP = "asm";

    private AsmOptions oldOptions;
    private AsmOptions approvedOptions;
    private File codeFile;
    private JButton btnAssemble;
    private JButton btnCancel;
    private JButton btnSelectCodeFile;
    private JCheckBox btnCodeToEmu;
    private JCheckBox btnCodeToFile;
    private JCheckBox btnLabelsIgnoreCase;
    private JCheckBox btnListLabels;
    private JCheckBox btnWarnNonAsciiChars;
    private JLabel labelCodeFile;
    private JTextField fldCodeFile;


    public static AsmOptions open(Window owner, AsmOptions options) {
        AsmOptionsDlg dlg = new AsmOptionsDlg(owner, options);
        dlg.setVisible(true);
        return dlg.approvedOptions;
    }


    /* --- ActionListener --- */

    @Override
    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();
        if (src == this.btnCodeToFile) {
            updCodeFileFieldsEnabled();
        } else if (src == this.btnSelectCodeFile) {
            doSelectCodeFile();
        } else if (src == this.btnAssemble) {
            doApprove();
        } else if (src == this.btnCancel) {
            doClose();
        }
    }


    /* --- DropTargetListener --- */

    @Override
    public void dragEnter(DropTargetDragEvent e) {
        if (!GUIUtil.isFileDrop(e))
            e.rejectDrag();
    }


    @Override
    public void dragExit(DropTargetEvent e) {
        // leer
    }


    @Override
    public void dragOver(DropTargetDragEvent e) {
        // leer
    }


    @Override
    public void drop(DropTargetDropEvent e) {
        final File file = GUIUtil.fileDrop(this, e);
        if (file != null) {
            // nicht auf Benutzereingaben warten
            EventQueue.invokeLater(
                    new Runnable() {
                        @Override
                        public void run() {
                            codeFileSelected(file);
                        }
                    });
        }
    }


    @Override
    public void dropActionChanged(DropTargetDragEvent e) {
        if (!GUIUtil.isFileDrop(e))
            e.rejectDrag();
    }


    /* --- Konstruktor --- */

    private AsmOptionsDlg(Window owner, AsmOptions options) {
        super(owner);
        setTitle("Assembler-Optionen"); // TODO: i18n
        this.oldOptions = options;
        this.approvedOptions = null;


        // Fensterinhalt
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints(
                0, 0,
                1, 1,
                1.0, 0.0,
                GridBagConstraints.CENTER,
                GridBagConstraints.HORIZONTAL,
                new Insets(5, 5, 5, 5),
                0, 0);

        // Bereich Marken
        JPanel panelLabel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        panelLabel.setBorder(BorderFactory.createTitledBorder("Marken")); // TODO: i18n
        gbc.gridy++;
        add(panelLabel, gbc);

        this.btnLabelsIgnoreCase = new JCheckBox(
                "Gro\u00DF-/Kleinschreibung bei Marken ignorieren"); // TODO: i18n
        panelLabel.add(this.btnLabelsIgnoreCase);

        this.btnListLabels = new JCheckBox("Markentabelle ausgeben"); // TODO: i18n
        panelLabel.add(this.btnListLabels);


        // Bereich Erzeugter Programmcode
        JPanel panelCode = new JPanel(new GridBagLayout());
        panelCode.setBorder(BorderFactory.createTitledBorder(
                "Erzeugter Programmcode")); // TODO: i18n
        gbc.gridy++;
        add(panelCode, gbc);

        GridBagConstraints gbcCode = new GridBagConstraints(
                0, 0,
                GridBagConstraints.REMAINDER, 1,
                0.0, 0.0,
                GridBagConstraints.WEST,
                GridBagConstraints.NONE,
                new Insets(5, 5, 0, 5),
                0, 0);

        this.btnCodeToEmu = new JCheckBox(
                "Programmcode in Emulator laden"); // TODO: i18n
        panelCode.add(this.btnCodeToEmu, gbcCode);

        this.btnCodeToFile = new JCheckBox(
                "Programmcode in Datei speichern"); // TODO: i18n
        gbcCode.insets.top = 0;
        gbcCode.gridy++;
        panelCode.add(this.btnCodeToFile, gbcCode);

        this.labelCodeFile = new JLabel("Dateiname:"); // TODO: i18n
        gbcCode.insets.left = 50;
        gbcCode.gridwidth = 1;
        gbcCode.gridy++;
        panelCode.add(this.labelCodeFile, gbcCode);

        this.fldCodeFile = new JTextField();
        this.fldCodeFile.setEditable(false);
        gbcCode.fill = GridBagConstraints.HORIZONTAL;
        gbcCode.weightx = 1.0;
        gbcCode.insets.left = 5;
        gbcCode.gridx++;
        panelCode.add(this.fldCodeFile, gbcCode);

        this.btnSelectCodeFile = new JButton("...");
        gbcCode.fill = GridBagConstraints.NONE;
        gbcCode.weightx = 0.0;
        gbcCode.gridx++;
        panelCode.add(this.btnSelectCodeFile, gbcCode);


        // Bereich Sonstiges
        JPanel panelEtc = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        panelEtc.setBorder(BorderFactory.createTitledBorder("Sonstiges")); // TODO: i18n
        gbc.gridy++;
        add(panelEtc, gbc);

        this.btnWarnNonAsciiChars = new JCheckBox(
                "Bei Nicht-ASCII-Zeichen warnen"); // TODO: i18n
        panelEtc.add(this.btnWarnNonAsciiChars);


        // Bereich Knoepfe
        JPanel panelBtn = new JPanel(new GridLayout(1, 2, 5, 5));
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0.0;
        gbc.insets.bottom = 10;
        gbc.gridy++;
        add(panelBtn, gbc);

        this.btnAssemble = new JButton("Assemblieren"); // TODO: i18n
        panelBtn.add(this.btnAssemble);

        this.btnCancel = new JButton("Abbrechen"); // TODO: i18n
        panelBtn.add(this.btnCancel);


        // Vorbelegungen
        if (this.oldOptions == null) {
            this.oldOptions = new AsmOptions();
        }
        this.btnLabelsIgnoreCase.setSelected(
                this.oldOptions.getLabelsIgnoreCase());

        this.btnListLabels.setSelected(this.oldOptions.getListLabels());
        this.btnCodeToEmu.setSelected(this.oldOptions.getCodeToEmu());
        this.btnCodeToFile.setSelected(this.oldOptions.getCodeToFile());

        this.codeFile = this.oldOptions.getCodeFile();
        if (this.codeFile != null) {
            this.fldCodeFile.setText(this.codeFile.getPath());
        }
        updCodeFileFieldsEnabled();

        this.btnWarnNonAsciiChars.setSelected(
                this.oldOptions.getWarnNonAsciiChars());


        // Fenstergroesse und -position
        pack();
        setParentCentered();
        setResizable(true);


        // Listener
        this.btnCodeToFile.addActionListener(this);
        this.btnSelectCodeFile.addActionListener(this);
        this.btnAssemble.addActionListener(this);
        this.btnCancel.addActionListener(this);


        // Drop-Ziel
        (new DropTarget(this.fldCodeFile, this)).setActive(true);
    }


    /* --- private Methoden --- */

    private void codeFileSelected(File file) {
        this.codeFile = file;
        this.fldCodeFile.setText(file.getPath());
    }


    protected void doApprove() {
        boolean codeToFile = this.btnCodeToFile.isSelected();
        if (codeToFile && (this.codeFile == null)) {
            Main.showError(
                    this,
                    "Erzeugter Programmcode in Datei speichern:\n" // TODO: i18n
                            + "Es wurde keine Datei angegeben.");
        } else {
            this.approvedOptions = new AsmOptions(this.oldOptions);
            this.approvedOptions.setCodeToEmu(this.btnCodeToEmu.isSelected());
            this.approvedOptions.setCodeToFile(codeToFile, this.codeFile);
            this.approvedOptions.setLabelsIgnoreCase(
                    this.btnLabelsIgnoreCase.isSelected());
            this.approvedOptions.setListLabels(
                    this.btnListLabels.isSelected());
            this.approvedOptions.setWarnNonAsciiChars(
                    this.btnWarnNonAsciiChars.isSelected());
            doClose();
        }
    }


    private void doSelectCodeFile() {
        File file = FileDlg.showFileSaveDlg(
                this,
                "Ausgabedatei festlegen", // TODO: i18n
                this.codeFile != null ?
                        this.codeFile
                        : AppContext.getLastDirFile(FILE_GROUP),
                GUIUtil.binaryFileFilter,
                GUIUtil.hexFileFilter,
                GUIUtil.jtcFileFilter,
                GUIUtil.tapFileFilter);
        if (file != null) {
            codeFileSelected(file);
            AppContext.setLastFile(FILE_GROUP, file);
        }
    }


    private void updCodeFileFieldsEnabled() {
        boolean state = this.btnCodeToFile.isSelected();
        this.labelCodeFile.setEnabled(state);
        this.fldCodeFile.setEnabled(state);
        this.btnSelectCodeFile.setEnabled(state);
    }
}
