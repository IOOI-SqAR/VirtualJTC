/*
 * (c) 2007-2021 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Dialog fuer Laden einer Datei
 */

package org.jens_mueller.jtcemu.platform.se.base;

import org.jens_mueller.jtcemu.base.AppContext;
import org.jens_mueller.jtcemu.base.FileInfo;
import org.jens_mueller.jtcemu.base.FileLoader;
import org.jens_mueller.jtcemu.base.JTCUtil;
import org.jens_mueller.jtcemu.platform.se.Main;
import org.jens_mueller.z8.Z8Memory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;


public class LoadDlg extends BaseDlg implements ActionListener {
    private static final String LABEL_BEG_ADDR = "Laden ab Adresse:";
    private static final String LABEL_END_ADDR = "Bis max. Adresse:";

    private TopFrm topFrm;
    private Z8Memory memory;
    private File file;
    private FileInfo.Format fileFmt;
    private JTextField fldBegAddr;
    private JTextField fldEndAddr;
    private JCheckBox btnLoadAsBIN;
    private JButton btnLoad;
    private JButton btnHelp;
    private JButton btnCancel;


    public static boolean loadFile(
            Window owner,
            TopFrm topFrm,
            Z8Memory memory,
            File file,
            FileInfo.Format fmt,
            int begAddr,
            int endAddr,
            int startAddr) {
        boolean done = false;
        FileLoader fileLoader = new FileLoader(memory);
        String statusText = fileLoader.loadFile(
                file,
                fmt,
                begAddr,
                endAddr,
                startAddr);
        if (statusText != null) {
            AppContext.setLastFile(FileInfo.FILE_GROUP_SOFTWARE, file);
            done = true;
        }
        String msg = fileLoader.getMessage();
        if (!done && (msg == null)) {
            msg = "Datei konnte nicht geladen werden.";
        }
        if (msg != null) {
            if (done) {
                showSuppressableInfoDlg(owner, msg);
            } else {
                JOptionPane.showMessageDialog(
                        owner,
                        msg,
                        "Fehler",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
        if (statusText != null) {
            topFrm.showStatusText(statusText);
        }
        return done;
    }


    public static void open(
            TopFrm topFrm,
            Z8Memory memory,
            File file,
            FileInfo fileInfo) {
        (new LoadDlg(topFrm, memory, file, fileInfo)).setVisible(true);
    }


    /* --- ActionListener --- */

    @Override
    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();
        if ((src == this.fldEndAddr) || (src == this.btnLoad)) {
            doLoad();
        } else if (src == this.btnHelp) {
            HelpFrm.open("/help/loadsave.htm");
        } else if (src == this.btnCancel) {
            doClose();
        } else if (src == this.fldBegAddr) {
            this.fldBegAddr.transferFocus();
        }
    }


    /* --- ueberschriebene Methoden --- */

    @Override
    protected boolean doClose() {
        boolean rv = super.doClose();
        if (rv) {
            this.fldBegAddr.removeActionListener(this);
            this.fldEndAddr.removeActionListener(this);
            this.btnLoad.removeActionListener(this);
            this.btnHelp.removeActionListener(this);
            this.btnCancel.removeActionListener(this);
        }
        return rv;
    }


    /* --- Konstruktor --- */

    private LoadDlg(
            TopFrm topFrm,
            Z8Memory memory,
            File file,
            FileInfo fileInfo) {
        super(topFrm);
        setTitle("Datei laden");
        this.topFrm = topFrm;
        this.memory = memory;
        this.file = file;
        this.fileFmt = fileInfo.getFormat();


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


        // Bereich Dateiformat
        JPanel panelFile = new JPanel(new GridBagLayout());
        panelFile.setBorder(BorderFactory.createTitledBorder("Datei"));
        add(panelFile, gbc);

        GridBagConstraints gbcFile = new GridBagConstraints(
                0, 0,
                1, 1,
                0.0, 0.0,
                GridBagConstraints.EAST,
                GridBagConstraints.NONE,
                new Insets(5, 5, 5, 5),
                0, 0);

        panelFile.add(new JLabel("Dateiname:"), gbcFile);
        gbcFile.insets.top = 0;
        gbcFile.gridy++;
        panelFile.add(new JLabel("Info:"), gbcFile);

        JTextField fldFileName = new JTextField();
        fldFileName.setEditable(false);
        gbcFile.anchor = GridBagConstraints.WEST;
        gbcFile.fill = GridBagConstraints.HORIZONTAL;
        gbcFile.weightx = 1.0;
        gbcFile.insets.top = 5;
        gbcFile.gridx = 1;
        gbcFile.gridy = 0;
        panelFile.add(fldFileName, gbcFile);

        JTextField fldFileInfo = new JTextField();
        fldFileInfo.setEditable(false);
        gbcFile.insets.top = 0;
        gbcFile.gridy++;
        panelFile.add(fldFileInfo, gbcFile);

        this.btnLoadAsBIN = null;
        if (!this.fileFmt.equals(FileInfo.Format.BIN)) {
            this.btnLoadAsBIN = new JCheckBox("Als BIN-Datei laden");
            gbcFile.fill = GridBagConstraints.NONE;
            gbcFile.weightx = 0.0;
            gbcFile.gridy++;
            panelFile.add(this.btnLoadAsBIN, gbcFile);
        }


        // Bereich Ladeadressen
        JPanel panelAddr = new JPanel(new GridBagLayout());
        panelAddr.setBorder(BorderFactory.createTitledBorder("Ladeadressen"));
        gbc.gridy++;
        add(panelAddr, gbc);

        GridBagConstraints gbcAddr = new GridBagConstraints(
                0, 0,
                1, 1,
                0.0, 0.0,
                GridBagConstraints.WEST,
                GridBagConstraints.NONE,
                new Insets(5, 5, 5, 5),
                0, 0);

        panelAddr.add(new JLabel(LABEL_BEG_ADDR), gbcAddr);

        this.fldBegAddr = createJTextField(4);
        gbcAddr.fill = GridBagConstraints.HORIZONTAL;
        gbcAddr.weightx = 0.5;
        gbcAddr.gridx++;
        panelAddr.add(this.fldBegAddr, gbcAddr);

        gbcAddr.fill = GridBagConstraints.NONE;
        gbcAddr.weightx = 0.0;
        gbcAddr.gridx++;
        panelAddr.add(new JLabel(LABEL_END_ADDR), gbcAddr);

        this.fldEndAddr = createJTextField(4);
        gbcAddr.fill = GridBagConstraints.HORIZONTAL;
        gbcAddr.weightx = 0.5;
        gbcAddr.gridx++;
        panelAddr.add(this.fldEndAddr, gbcAddr);


        // Knoepfe
        JPanel panelBtn = new JPanel(new GridLayout(1, 3, 5, 5));
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0.0;
        gbc.insets.top = 10;
        gbc.insets.left = 10;
        gbc.insets.right = 10;
        gbc.insets.bottom = 10;
        gbc.gridy++;
        add(panelBtn, gbc);

        this.btnLoad = new JButton("Laden");
        panelBtn.add(this.btnLoad);

        this.btnHelp = new JButton("Hilfe...");
        panelBtn.add(this.btnHelp);

        this.btnCancel = new JButton("Abbrechen");
        panelBtn.add(this.btnCancel);


        // Vorbelegung
        String fileName = file.getName();
        if (fileName != null) {
            fldFileName.setText(fileName);
        }
        String infoText = fileInfo.getInfoText();
        if (infoText != null) {
            fldFileInfo.setText(infoText);
        }
        int begAddr = fileInfo.getBegAddr();
        if (begAddr < 0) {
            begAddr = FileLoader.DEFAULt_LOAD_ADDR;
        }
        this.fldBegAddr.setText(String.format("%04X", begAddr));


        // Fenstergroesse
        pack();
        this.fldBegAddr.setColumns(0);
        this.fldEndAddr.setColumns(0);
        setParentCentered();
        setResizable(true);


        // Listener
        this.fldBegAddr.addActionListener(this);
        this.fldEndAddr.addActionListener(this);
        this.btnLoad.addActionListener(this);
        this.btnHelp.addActionListener(this);
        this.btnCancel.addActionListener(this);
    }


    /* --- private Methoden --- */

    private JTextField createJTextField(int cols) {
        JTextField fld = new JTextField(cols);
        Dimension pSize = fld.getPreferredSize();
        if (pSize != null) {
            if (pSize.height > 0) {
                fld.setPreferredSize(new Dimension(1, pSize.height));
            }
        }
        return fld;
    }


    private void doLoad() {
        try {
            int endAddr = -1;
            int begAddr = JTCUtil.parseHex4(
                    this.fldBegAddr.getText(),
                    LABEL_BEG_ADDR);

            String text = this.fldEndAddr.getText();
            if (text != null) {
                text = text.trim();
                if (!text.isEmpty()) {
                    endAddr = JTCUtil.parseHex4(text, LABEL_END_ADDR);
                }
            }
            FileInfo.Format fmt = this.fileFmt;
            if (this.btnLoadAsBIN != null) {
                if (this.btnLoadAsBIN.isSelected()) {
                    fmt = FileInfo.Format.BIN;
                }
            }
            if (fmt != null) {
                if (loadFile(
                        this,
                        this.topFrm,
                        this.memory,
                        this.file,
                        fmt,
                        begAddr,
                        endAddr,
                        -1)) {
                    doClose();
                }
            }
        } catch (Exception ex) {
            Main.showError(this, ex);
        }
    }
}
