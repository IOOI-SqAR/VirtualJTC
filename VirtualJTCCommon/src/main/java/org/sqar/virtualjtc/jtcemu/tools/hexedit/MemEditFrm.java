/*
 * (c) 2010-2021 Jens Mueller
 * (c) 2017-2024 Lars Sonchocky-Helldorf
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Speichereditor
 */

package org.sqar.virtualjtc.jtcemu.tools.hexedit;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.text.ParseException;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.swing.*;

import org.sqar.virtualjtc.jtcemu.base.*;
import org.sqar.virtualjtc.z8.Z8Memory;


public class MemEditFrm extends AbstractHexCharFrm {
    private static final Locale locale = Locale.getDefault();
    private static final ResourceBundle memEditFrmResourceBundle = ResourceBundle.getBundle("MemEditFrm", locale);

    private static MemEditFrm instance = null;
    private static Point lastLocation = null;

    private Z8Memory memory;
    private int begAddr;
    private int endAddr;
    private int savedAddr;
    private File lastFile;
    private String textFind;
    private JButton btnRefresh;
    private JMenuItem mnuRefresh;
    private JMenuItem mnuClose;
    private JMenuItem mnuCopy;
    private JMenuItem mnuPrintOptions;
    private JMenuItem mnuPrint;
    private JMenuItem mnuOverwrite;
    private JMenuItem mnuSaveAddr;
    private JMenuItem mnuGotoSavedAddr;
    private JMenuItem mnuSelectToSavedAddr;
    private JMenuItem mnuFind;
    private JMenuItem mnuFindNext;
    private JMenuItem mnuHelpContent;
    private JTextField fldBegAddr;
    private JTextField fldEndAddr;


    public static void close() {
        if (instance != null)
            instance.doClose();
    }


    public static void open(Z8Memory memory) {
        if (instance == null) {
            instance = new MemEditFrm(memory);
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


    /* --- ueberschriebene Methoden --- */

    @Override
    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();
        if (src != null) {
            if (src == this.fldBegAddr) {
                this.fldEndAddr.requestFocus();
            } else if ((src == this.fldEndAddr)
                    || (src == this.btnRefresh)
                    || (src == this.mnuRefresh)) {
                doRefresh();
            } else if (src == this.mnuPrintOptions) {
                PrintOptionsDlg.open(this);
            } else if (src == this.mnuPrint) {
                doPrint();
            } else if (src == this.mnuClose) {
                doClose();
            } else if (src == this.mnuCopy) {
                doBytesCopy();
            } else if (src == this.mnuOverwrite) {
                doBytesOverwrite();
            } else if (src == this.mnuSaveAddr) {
                doSaveAddr();
            } else if (src == this.mnuGotoSavedAddr) {
                doGotoSavedAddr(false);
            } else if (src == this.mnuSelectToSavedAddr) {
                doGotoSavedAddr(true);
            } else if (src == this.mnuFind) {
                doFind();
            } else if (src == this.mnuFindNext) {
                doFindNext();
            } else if (src == this.mnuHelpContent) {
                HelpFrm.open(memEditFrmResourceBundle.getString("help.path"));
            } else {
                super.actionPerformed(e);
            }
        }
    }


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
    public int getAddrOffset() {
        return Math.max(this.begAddr, 0);
    }


    @Override
    public byte getDataByte(int idx) {
        byte rv = (byte) 0;
        if ((this.begAddr >= 0) && ((this.begAddr + idx) <= this.endAddr)) {
            rv = (byte) this.memory.getMemByte(this.begAddr + idx, false);
        }
        return rv;
    }


    @Override
    public int getDataLength() {
        return (this.begAddr >= 0) && (this.begAddr <= this.endAddr) ?
                this.endAddr - this.begAddr + 1
                : 0;
    }


    @Override
    public String getPropPrefix() {
        return "memeditor.";
    }


    @Override
    protected void setContentActionsEnabled(boolean state) {
        this.mnuPrint.setEnabled(state);
        this.mnuFind.setEnabled(state);
    }


    @Override
    protected void setFindNextActionsEnabled(boolean state) {
        this.mnuFindNext.setEnabled(state);
    }


    @Override
    protected void setSelectedByteActionsEnabled(boolean state) {
        this.mnuCopy.setEnabled(state);
        this.mnuOverwrite.setEnabled(state);
        this.mnuSaveAddr.setEnabled(state);
        this.mnuSelectToSavedAddr.setEnabled(state && (this.savedAddr >= 0));
    }


    /* --- Aktionen --- */

    private void doBytesOverwrite() {
        if ((this.begAddr >= 0) && (this.begAddr <= this.endAddr)) {
            int caretPos = this.hexCharFld.getCaretPosition();
            if ((caretPos >= 0) && (this.begAddr + caretPos <= this.endAddr)) {
                ReplyBytesDlg dlg = new ReplyBytesDlg(
                        this,
                        memEditFrmResourceBundle.getString("dialog.doBytesOverwrite.replyBytesDlg.title"),
                        this.lastInputFmt,
                        this.lastBigEndian,
                        null);
                dlg.setVisible(true);
                byte[] a = dlg.getApprovedBytes();
                if (a != null) {
                    if (a.length > 0) {
                        this.lastInputFmt = dlg.getApprovedInputFormat();
                        this.lastBigEndian = dlg.getApprovedBigEndian();

                        boolean failed = false;
                        int src = 0;
                        int addr = this.begAddr + caretPos;
                        while (src < a.length) {
                            if (addr > 0xFFFF) {
                                JOptionPane.showMessageDialog(
                                        this,
                                        memEditFrmResourceBundle.getString("dialog.doBytesOverwrite.messageDialog.message"),
                                        memEditFrmResourceBundle.getString("dialog.doBytesOverwrite.messageDialog.title"),
                                        JOptionPane.WARNING_MESSAGE);
                                break;
                            } else {
                                if (!this.memory.setMemByte(addr, false, a[src])) {
                                    String msg = String.format(memEditFrmResourceBundle.getString("dialog.doBytesOverwrite.couldNotEditRAM.formatString"), addr);
                                    if (src == (a.length - 1)) {
                                        JOptionPane.showMessageDialog(
                                                this,
                                                msg,
                                                memEditFrmResourceBundle.getString("dialog.doBytesOverwrite.couldNotEditRAM.title"),
                                                JOptionPane.ERROR_MESSAGE);
                                    } else {
                                        boolean cancel = true;
                                        String[] options = {
                                                memEditFrmResourceBundle.getString("doBytesOverwrite.options.continue"),
                                                memEditFrmResourceBundle.getString("doBytesOverwrite.options.cancel")
                                        };
                                        JOptionPane pane = new JOptionPane(
                                                msg,
                                                JOptionPane.ERROR_MESSAGE);
                                        pane.setOptions(options);
                                        pane.createDialog(this, memEditFrmResourceBundle.getString("dialog.doBytesOverwrite.couldNotEditRAM.title")).setVisible(true);
                                        Object value = pane.getValue();
                                        if (value != null) {
                                            if (value.equals(options[0])) {
                                                cancel = false;
                                            }
                                        }
                                        if (cancel) {
                                            break;
                                        }
                                    }
                                }
                            }
                            addr++;
                            src++;
                        }
                        if (addr > this.endAddr) {
                            this.endAddr = addr - 1;
                            this.fldEndAddr.setText(
                                    String.format("%04X", this.endAddr));
                        }
                        updView();
                        setSelection(caretPos, caretPos + a.length - 1);
                    }
                }
            }
        }
    }


    private void doGotoSavedAddr(boolean moveOp) {
        if ((this.savedAddr >= 0)
                && (this.savedAddr >= this.begAddr)
                && (this.savedAddr <= this.endAddr)) {
            this.hexCharFld.setCaretPosition(
                    this.savedAddr - this.begAddr,
                    moveOp);
            updCaretPosFields();
        }
    }


    private void doRefresh() {
        try {
            int begAddr = GUIUtil.parseHex4(this.fldBegAddr, memEditFrmResourceBundle.getString("doRefresh.parseHex4.begAddr"));
            this.endAddr = GUIUtil.parseHex4(this.fldEndAddr, memEditFrmResourceBundle.getString("doRefresh.parseHex4.endAddr"));
            this.begAddr = begAddr;
            updView();
        } catch (UserInputException ex) {
            JOptionPane.showMessageDialog(
                    this,
                    ex.getMessage(),
                    memEditFrmResourceBundle.getString("dialog.doRefresh.messageDialog.title"),
                    JOptionPane.ERROR_MESSAGE);
        }
    }


    private void doSaveAddr() {
        if (this.begAddr >= 0) {
            int caretPos = this.hexCharFld.getCaretPosition();
            if (caretPos >= 0) {
                this.savedAddr = this.begAddr + caretPos;
                this.mnuGotoSavedAddr.setEnabled(true);
                this.mnuSelectToSavedAddr.setEnabled(true);
            }
        }
    }


    /* --- Konstruktor --- */

    private MemEditFrm(Z8Memory memory) {
        this.memory = memory;
        this.begAddr = -1;
        this.endAddr = -1;
        this.savedAddr = -1;
        this.lastFile = null;
        this.textFind = null;
        setTitle(memEditFrmResourceBundle.getString("window.title"));


        // Menu
        JMenuBar mnuBar = new JMenuBar();
        setJMenuBar(mnuBar);


        // Menu Datei
        JMenu mnuFile = new JMenu(memEditFrmResourceBundle.getString("menu.file"));
        mnuFile.setMnemonic(KeyEvent.VK_D);
        mnuBar.add(mnuFile);

        this.mnuRefresh = new JMenuItem(memEditFrmResourceBundle.getString("menuItem.refresh"));
        this.mnuRefresh.addActionListener(this);
        mnuFile.add(this.mnuRefresh);
        mnuFile.addSeparator();

        this.mnuPrintOptions = new JMenuItem(memEditFrmResourceBundle.getString("menuItem.printOptions"));
        this.mnuPrintOptions.addActionListener(this);
        mnuFile.add(this.mnuPrintOptions);

        this.mnuPrint = new JMenuItem(memEditFrmResourceBundle.getString("menuItem.print"));
        this.mnuPrint.setAccelerator(getMenuShortcut(KeyEvent.VK_P));
        this.mnuPrint.setEnabled(false);
        this.mnuPrint.addActionListener(this);
        mnuFile.add(this.mnuPrint);
        mnuFile.addSeparator();

        this.mnuClose = new JMenuItem(memEditFrmResourceBundle.getString("menuItem.close"));
        this.mnuClose.addActionListener(this);
        mnuFile.add(this.mnuClose);


        // Menu Bearbeiten
        JMenu mnuEdit = new JMenu(memEditFrmResourceBundle.getString("menu.edit"));
        mnuEdit.setMnemonic(KeyEvent.VK_B);
        mnuBar.add(mnuEdit);

        this.mnuCopy = new JMenuItem(memEditFrmResourceBundle.getString("menuItem.copy"));
        this.mnuCopy.setEnabled(false);
        this.mnuCopy.addActionListener(this);
        mnuEdit.add(this.mnuCopy);
        mnuEdit.addSeparator();

        this.mnuOverwrite = new JMenuItem(memEditFrmResourceBundle.getString("menuItem.overwrite"));
        this.mnuOverwrite.setAccelerator(getMenuShortcut(KeyEvent.VK_O));
        this.mnuOverwrite.setEnabled(false);
        this.mnuOverwrite.addActionListener(this);
        mnuEdit.add(this.mnuOverwrite);
        mnuEdit.addSeparator();

        this.mnuSaveAddr = new JMenuItem(memEditFrmResourceBundle.getString("menuItem.saveAddr"));
        this.mnuSaveAddr.setEnabled(false);
        this.mnuSaveAddr.addActionListener(this);
        mnuEdit.add(this.mnuSaveAddr);

        this.mnuGotoSavedAddr = new JMenuItem(memEditFrmResourceBundle.getString("menuItem.gotoSavedAddr"));
        this.mnuGotoSavedAddr.setEnabled(false);
        this.mnuGotoSavedAddr.addActionListener(this);
        mnuEdit.add(this.mnuGotoSavedAddr);

        this.mnuSelectToSavedAddr = new JMenuItem(memEditFrmResourceBundle.getString("menuItem.selectToSavedAddr"));
        this.mnuSelectToSavedAddr.setEnabled(false);
        this.mnuSelectToSavedAddr.addActionListener(this);
        mnuEdit.add(this.mnuSelectToSavedAddr);
        mnuEdit.addSeparator();

        this.mnuFind = new JMenuItem(memEditFrmResourceBundle.getString("menuItem.find"));
        this.mnuFind.setAccelerator(getMenuShortcut(KeyEvent.VK_F));
        this.mnuFind.setEnabled(false);
        this.mnuFind.addActionListener(this);
        mnuEdit.add(this.mnuFind);

        this.mnuFindNext = new JMenuItem(memEditFrmResourceBundle.getString("menuItem.findNext"));
        this.mnuFindNext.setAccelerator(
                KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0));
        this.mnuFindNext.setEnabled(false);
        this.mnuFindNext.addActionListener(this);
        mnuEdit.add(this.mnuFindNext);


        // Menu Hilfe
        JMenu mnuHelp = new JMenu(memEditFrmResourceBundle.getString("menu.help"));
        mnuHelp.setMnemonic(KeyEvent.VK_H);
        mnuBar.add(mnuHelp);

        this.mnuHelpContent = new JMenuItem(memEditFrmResourceBundle.getString("menuItem.helpContent"));
        this.mnuHelpContent.addActionListener(this);
        mnuHelp.add(this.mnuHelpContent);


        // Fensterinhalt
        setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints(
                0, 0,
                1, 1,
                0.0, 0.0,
                GridBagConstraints.WEST,
                GridBagConstraints.NONE,
                new Insets(5, 5, 5, 5),
                0, 0);

        // Adresseingabe
        add(new JLabel(memEditFrmResourceBundle.getString("label.startAddress")), gbc);

        this.fldBegAddr = new JTextField(4);
        this.fldBegAddr.addActionListener(this);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0.5;
        gbc.gridx++;
        add(this.fldBegAddr, gbc);

        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0.0;
        gbc.gridx++;
        add(new JLabel(memEditFrmResourceBundle.getString("label.endAddress")), gbc);

        this.fldEndAddr = new JTextField(4);
        this.fldEndAddr.addActionListener(this);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0.5;
        gbc.gridx++;
        add(this.fldEndAddr, gbc);

        this.btnRefresh = GUIUtil.createImageButton(
                this,
                "/images/file/reload.png",
                "Auffrischen"); // TODO: i18n
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0.0;
        gbc.gridx++;
        add(this.btnRefresh, gbc);


        // Hex-ASCII-Anzeige
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.gridx = 0;
        gbc.gridy++;
        add(createHexCharFld(), gbc);

        // Anzeige der Cursor-Position
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weighty = 0.0;
        gbc.gridy++;
        add(createCaretPosFld(memEditFrmResourceBundle.getString("label.caretPos")), gbc);

        // Anzeige der Dezimalwerte der Bytes ab Cursor-Position
        gbc.gridy++;
        add(createValueFld(), gbc);


        // sonstiges
        if (!GUIUtil.applyWindowSettings(this)) {
            pack();
            setLocationByPlatform(true);
        }
        setResizable(true);
    }
}
