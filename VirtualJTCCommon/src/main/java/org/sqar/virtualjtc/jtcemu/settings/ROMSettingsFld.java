/*
 * (c) 2020 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Einstellungen zum emulierten ROM
 */

package org.sqar.virtualjtc.jtcemu.settings;

import org.sqar.virtualjtc.jtcemu.Main;
import org.sqar.virtualjtc.jtcemu.base.*;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.*;


public class ROMSettingsFld
        extends JPanel
        implements
        ActionListener,
        DropTargetListener,
        ListSelectionListener {
    private static final Locale locale = Locale.getDefault();
    private static final ResourceBundle romSettingsFldResourceBundle = ResourceBundle.getBundle("ROMSettingsFld", locale);

    private static final String FILE_GROUP_ROM = "rom";
    private static final String FILE_GROUP_ROMBANK = "rombank";

    private SettingsFrm settingsFrm;
    private SortedListModel<ExtROM> extROMs;
    private ExtROM romBank;
    private JCheckBox cbRomBank;
    private JCheckBox cbReloadROMs;
    private JList<ExtROM> listROM;
    private JLabel labelRomBankFile;
    private JTextField fldRomBankFile;
    private JButton btnRomAdd;
    private JButton btnRomRemove;
    private JButton btnRomBankFileSelect;
    private JButton btnRomBankFileRemove;


    public ROMSettingsFld(SettingsFrm settingsFrm, JTCSys jtcSys) {
        this.settingsFrm = settingsFrm;
        this.extROMs = new SortedListModel<ExtROM>();
        this.romBank = jtcSys.getROMBank();

        setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints(
                0, 0,
                GridBagConstraints.REMAINDER, 1,
                0.0, 0.0,
                GridBagConstraints.WEST,
                GridBagConstraints.NONE,
                new Insets(5, 5, 0, 5),
                0, 0);

        add(
                new JLabel(romSettingsFldResourceBundle.getString("tabs.rom.embeddedROMIMages")),
                gbc);

        this.listROM = new JList<>(this.extROMs);
        this.listROM.setDragEnabled(false);
        this.listROM.setLayoutOrientation(JList.VERTICAL);
        this.listROM.setVisibleRowCount(4);
        this.listROM.addListSelectionListener(this);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.insets.top = 0;
        gbc.gridy++;
        add(new JScrollPane(this.listROM), gbc);

        JPanel panelRomBtn = new JPanel(new GridLayout(1, 2, 5, 5));
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0.0;
        gbc.weighty = 0.0;
        gbc.insets.top = 10;
        gbc.gridy++;
        add(panelRomBtn, gbc);

        this.btnRomAdd = new JButton(romSettingsFldResourceBundle.getString("button.rom.add"));
        this.btnRomAdd.addActionListener(this);
        panelRomBtn.add(this.btnRomAdd);

        this.btnRomRemove = new JButton(romSettingsFldResourceBundle.getString("button.rom.remove"));
        this.btnRomRemove.setEnabled(false);
        this.btnRomRemove.addActionListener(this);
        panelRomBtn.add(this.btnRomRemove);

        ExtROM[] aExtROMs = jtcSys.getExtROMs();
        if (aExtROMs != null) {
            for (int i = 0; i < aExtROMs.length; i++) {
                this.extROMs.addElement(aExtROMs[i]);
            }
        }

        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.insets.top = 15;
        gbc.gridy++;
        add(new JSeparator(), gbc);

        this.cbRomBank = new JCheckBox(
                "ROM-Bank im Bereich %2000-%3FFF emulieren (nur ES 4.0)"); // TODO: i18n
        this.cbRomBank.addActionListener(this);
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0.0;
        gbc.gridy++;
        add(this.cbRomBank, gbc);

        this.labelRomBankFile = new JLabel("Inhalt der ROM-Bank:"); // TODO: i18n
        gbc.insets.left = 50;
        gbc.insets.top = 0;
        gbc.gridy++;
        add(this.labelRomBankFile, gbc);

        this.fldRomBankFile = new JTextField();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridwidth = 1;
        gbc.gridy++;
        add(this.fldRomBankFile, gbc);

        this.btnRomBankFileSelect = GUIUtil.createImageButton(
                this,
                "/images/file/open.png",
                "\u00D4ffnen..."); // TODO: i18n
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0.0;
        gbc.insets.left = 5;
        gbc.gridx++;
        add(this.btnRomBankFileSelect, gbc);

        this.btnRomBankFileRemove = GUIUtil.createImageButton(
                this,
                "/images/file/remove.png",
                "Entfernen"); // TODO: i18n
        gbc.gridx++;
        add(this.btnRomBankFileRemove, gbc);

        if (this.romBank != null) {
            this.cbRomBank.setSelected(true);
            this.fldRomBankFile.setText(this.romBank.getFile().getPath());
        } else {
            this.cbRomBank.setSelected(false);

            /*
             * Wenn in den Einstellungen trotzdem eine ROM-Bank-Datei
             * angegeben ist, dann diese lesen
             */
            try {
                String fileName = AppContext.getProperty(JTCSys.PROP_ROMBANK_FILE);

                if (fileName != null) {
                    if (!fileName.isEmpty()) {
                        this.romBank = new ExtROM(
                                new File(fileName),
                                JTCSys.MAX_ROMBANK_SIZE);
                        this.fldRomBankFile.setText(fileName);
                    }
                }
            } catch (IOException ex) {
            }
        }
        updRomBankFieldsEnabled(jtcSys.getOSType());

        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.insets.top = 15;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.gridx = 0;
        gbc.gridy++;
        add(new JSeparator(), gbc);

        this.cbReloadROMs = new JCheckBox(
                romSettingsFldResourceBundle.getString("button.rom.reloadOnReset"),
                AppContext.getBooleanProperty(
                        JTCSys.PROP_ROM_RELOAD,
                        false));
        this.cbReloadROMs.addActionListener(this);
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0.0;
        gbc.insets.bottom = 5;
        gbc.gridy++;
        add(this.cbReloadROMs, gbc);
        updReloadToggleEnabled();


        // Drop-Ziele
        (new DropTarget(this.listROM, this)).setActive(true);
        (new DropTarget(this.fldRomBankFile, this)).setActive(true);
    }


    public ExtROM[] applyInput(
            Properties props,
            JTCSys.OSType osType) throws UserInputException {
        props.setProperty(
                AppContext.getPropPrefix() + JTCSys.PROP_ROM_RELOAD,
                Boolean.toString(this.cbReloadROMs.isSelected()));

        boolean romBankState = JTCSys.supportsROMBank(osType)
                && this.cbRomBank.isSelected();
        JTCUtil.applyROMBankSettings(
                props,
                romBankState,
                this.fldRomBankFile.getText());

        ExtROM[] extROMs = null;
        try {
            extROMs = this.extROMs.toArray(new ExtROM[this.extROMs.getSize()]);
            JTCUtil.applyROMSettings(props, extROMs, romBankState);
        } catch (ArrayStoreException ex) {
        }
        return extROMs != null ? extROMs : new ExtROM[0];
    }


    public ExtROM getRomBank() {
        return this.romBank;
    }


    public boolean isRomBankSelected() {
        return this.cbRomBank.isSelected();
    }


    public void updRomBankFieldsEnabled(JTCSys.OSType osType) {
        boolean state = JTCSys.supportsROMBank(osType);
        this.cbRomBank.setEnabled(state);

        state &= this.cbRomBank.isSelected();
        this.labelRomBankFile.setEnabled(state);
        this.fldRomBankFile.setEnabled(state);
        this.btnRomBankFileSelect.setEnabled(state);

        String fileName = this.fldRomBankFile.getText();
        if (fileName != null) {
            if (fileName.trim().isEmpty()) {
                fileName = null;
            }
        }
        this.btnRomBankFileRemove.setEnabled(state && (fileName != null));
    }


    /* --- ActionListener --- */

    @Override
    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();
        if (src != null) {
            if (src == this.cbRomBank) {
                updRomBankFieldsEnabled(this.settingsFrm.getSelectedOSType());
                updReloadToggleEnabled();
                this.settingsFrm.setDataChanged();
            } else if (src == this.btnRomBankFileSelect) {
                doRomBankFileSelect();
            } else if (src == this.btnRomBankFileRemove) {
                doRomBankFileRemove();
            } else if (src == this.btnRomAdd) {
                doRomAdd();
            } else if (src == this.btnRomRemove) {
                doRomRemove();
            } else if (src instanceof JCheckBox) {
                this.settingsFrm.setDataChanged();
            }
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
        if (GUIUtil.isFileDrop(e)) {
            e.acceptDrop(DnDConstants.ACTION_COPY);    // Quelle nicht loeschen
            final Component c = e.getDropTargetContext().getComponent();
            Transferable t = e.getTransferable();
            if ((c != null) && (t != null)) {
                try {
                    Object o = t.getTransferData(DataFlavor.javaFileListFlavor);
                    if (o != null) {
                        if (o instanceof Collection) {
                            Iterator iter = ((Collection) o).iterator();
                            if (iter != null) {
                                if (iter.hasNext()) {
                                    o = iter.next();
                                    if (o != null) {
                                        File file = null;
                                        if (o instanceof File) {
                                            file = (File) o;
                                        } else if (o instanceof String) {
                                            file = new File(o.toString());
                                        }
                                        if (file != null) {
                                            // nicht auf Benutzereingaben warten
                                            final File file1 = file;
                                            final JList listROM = this.listROM;
                                            final JTextField fldRomBankFile = this.fldRomBankFile;
                                            EventQueue.invokeLater(
                                                    new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            if (c == listROM) {
                                                                addRomFile(file1);
                                                            } else if (c == fldRomBankFile) {
                                                                loadRomBankFile(file1);
                                                            }
                                                        }
                                                    });
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception ex) {
                }
            }
            e.dropComplete(true);
        } else {
            e.rejectDrop();
        }
    }


    @Override
    public void dropActionChanged(DropTargetDragEvent e) {
        if (!GUIUtil.isFileDrop(e))
            e.rejectDrag();
    }


    /* --- ListSelectionListener --- */

    @Override
    public void valueChanged(ListSelectionEvent e) {
        this.btnRomRemove.setEnabled(this.listROM.getSelectedIndex() >= 0);
    }


    /* --- Aktionen --- */

    private void doRomAdd() {
        File file = FileDlg.showFileOpenDlg(
                this.settingsFrm,
                "ROM-Datei laden", // TODO: i18n
                "Laden", // TODO: i18n
                AppContext.getLastDirFile(FILE_GROUP_ROM),
                GUIUtil.romFileFilter);
        if (file != null) {
            addRomFile(file);
        }
    }


    private void doRomRemove() {
        int idx = this.listROM.getSelectedIndex();
        if ((idx >= 0) && (idx < this.extROMs.getSize())) {
            this.extROMs.remove(idx);
            updReloadToggleEnabled();
            this.settingsFrm.setDataChanged();
        }
    }


    private void doRomBankFileSelect() {
        if (JTCSys.supportsROMBank(this.settingsFrm.getSelectedOSType())
                && this.cbRomBank.isSelected()) {
            File file = FileDlg.showFileOpenDlg(
                    this.settingsFrm,
                    "ROM-Datei laden", // TODO: i18n
                    "Laden", // TODO: i18n
                    AppContext.getLastDirFile(FILE_GROUP_ROMBANK),
                    GUIUtil.romFileFilter);
            if (file != null) {
                loadRomBankFile(file);
            }
        }
    }


    private void doRomBankFileRemove() {
        this.romBank = null;
        this.fldRomBankFile.setText("");
        this.btnRomBankFileRemove.setEnabled(false);
        updReloadToggleEnabled();
        this.settingsFrm.setDataChanged();
    }


    /* --- private Methoden --- */

    private void addRomFile(File file) {
        try {
            ExtROM rom = new ExtROM(file, JTCSys.MAX_ROM_SIZE);
            Integer addr = GUIUtil.askHex4(
                    this,
                    "Anfangsadresse (hex)", // TODO: i18n
                    JTCUtil.getBegAddrFromFilename(file));
            if (addr != null) {
                rom.setBegAddr(addr.intValue());
                this.extROMs.addElement(rom);
                updReloadToggleEnabled();
                this.settingsFrm.setDataChanged();

                int idx = this.extROMs.indexOf(rom);
                if (idx >= 0) {
                    this.listROM.setSelectedIndex(idx);
                }
                AppContext.setLastFile(FILE_GROUP_ROM, file);
            }
        } catch (Exception ex) {
            Main.showError(this, ex);
        }
    }


    private void loadRomBankFile(File file) {
        try {
            this.romBank = new ExtROM(file, JTCSys.MAX_ROMBANK_SIZE);
            this.fldRomBankFile.setText(file.getPath());
            this.btnRomBankFileRemove.setEnabled(
                    this.fldRomBankFile.isEnabled());
            updReloadToggleEnabled();
            this.settingsFrm.setDataChanged();
            AppContext.setLastFile(FILE_GROUP_ROMBANK, file);
        } catch (Exception ex) {
            Main.showError(this, ex);
        }
    }


    private void updReloadToggleEnabled() {
        this.cbReloadROMs.setEnabled(
                !this.extROMs.isEmpty() || this.cbRomBank.isSelected());
    }
}
