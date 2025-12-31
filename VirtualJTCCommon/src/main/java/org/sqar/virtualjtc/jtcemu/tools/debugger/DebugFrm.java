/*
 * (c) 2007-2021 Jens Mueller
 * (c) 2017-2024 Lars Sonchocky-Helldorf
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Debugger
 */

package org.sqar.virtualjtc.jtcemu.tools.debugger;

import org.sqar.virtualjtc.jtcemu.base.BaseFrm;
import org.sqar.virtualjtc.jtcemu.base.GUIUtil;
import org.sqar.virtualjtc.jtcemu.base.HelpFrm;
import org.sqar.virtualjtc.jtcemu.settings.SortedListModel;
import org.sqar.virtualjtc.z8.Z8;
import org.sqar.virtualjtc.z8.Z8Debugger;
import org.sqar.virtualjtc.z8.Z8Memory;
import org.sqar.virtualjtc.z8.Z8Reassembler;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.*;
import java.util.Timer;
import java.util.*;


public class DebugFrm extends BaseFrm
        implements
        ActionListener,
        ChangeListener,
        FocusListener,
        KeyListener,
        ListSelectionListener,
        MouseListener,
        Z8Debugger {
    private static final Locale locale = Locale.getDefault();
    private static final ResourceBundle debugFrmResourceBundle = ResourceBundle.getBundle("DebugFrm", locale);

    public static class BreakpointListModel
            extends SortedListModel<AbstractBreakpoint> {
        public BreakpointListModel() {
            // leer
        }
    }


    public static class MyTextArea extends JTextArea {
        public MyTextArea(int rows, int cols) {
            super(rows, cols);
        }

        @Override
        public int getRowHeight() {
            return super.getRowHeight();
        }
    }


    private static final int DEFAULT_AUTOSTEP_FREQUENCY = 2000;

    private static final int DEFAULT_REASS_ROWS = 4;

    private static final int P01M = 0xF8;
    private static final int FLAGS = 0xFC;
    private static final int RP = 0xFD;
    private static final int SPH = 0xFE;
    private static final int SPL = 0xFF;

    private static DebugFrm instance = null;
    private static Point lastLocation = null;

    private Z8 z8;
    private boolean z8Pause;
    private boolean upperGPRsVisible;
    private Z8Memory memory;
    private Z8Reassembler reassembler;
    private boolean editMode;
    private BreakpointListModel pcBreakpoints;
    private BreakpointListModel memBreakpoints;
    private BreakpointListModel regBreakpoints;
    private BreakpointCellRenderer breakpointCellRenderer;
    private Component lastFocussedFld;
    private Component triggerFld;
    private JMenuItem mnuClose;
    private JMenuItem mnuStop;
    private JMenuItem mnuStepOver;
    private JMenuItem mnuStepInto;
    private JMenuItem mnuRunToRET;
    private JMenuItem mnuRun;
    private JMenuItem mnuAddPCBreakpoint;
    private JMenuItem mnuAddMemBreakpoint;
    private JMenuItem mnuAddRegBreakpoint;
    private JMenuItem mnuBreakpointEdit;
    private JMenuItem mnuBreakpointRemove;
    private JMenuItem mnuBreakpointRemoveAll;
    private JMenuItem mnuBreakpointEnable;
    private JMenuItem mnuBreakpointDisable;
    private JMenuItem mnuBreakpointEnableAll;
    private JMenuItem mnuBreakpointDisableAll;
    private JMenuItem mnuHelpContent;
    private JPopupMenu mnuPopup;
    private JMenuItem popupBreakpointAdd;
    private JMenuItem popupBreakpointEdit;
    private JMenuItem popupBreakpointRemove;
    private JMenuItem popupBreakpointRemoveAll;
    private JMenuItem popupBreakpointEnable;
    private JMenuItem popupBreakpointDisable;
    private JMenuItem popupBreakpointEnableAll;
    private JMenuItem popupBreakpointDisableAll;
    private JButton btnStop;
    private JButton btnStepInto;
    private JButton btnStepOver;
    private JButton btnRun;
    private JButton btnRunToRET;
    private JButton btnCyclesReset;
    private JCheckBox autoStepCheckBox;
    private JComboBox autoStepModeComboBox;
    private JLabel autoStepLabel;
    private JTextField autoStepFrequency;
    private JLabel autoStepFrequencyUnitLabel;
    private JLabel labelStatus;
    private JLabel labelRegSelection;
    private JLabel[] regLabels;
    private int markedRegIdx;
    private HexFld[] markedRegFlds;
    private HexFld[] regFlds;
    private HexFld[] grp15Flds;
    private HexFld fldPC;
    private HexFld fldRegP01M;
    private HexFld fldRegFLAGS;
    private HexFld fldRegRP;
    private HexFld fldRegSPH;
    private HexFld fldRegSPL;
    private JCheckBox btnFlagC;
    private JCheckBox btnFlagZ;
    private JCheckBox btnFlagS;
    private JCheckBox btnFlagV;
    private JRadioButton btnLowerGPRs;
    private JRadioButton btnUpperGPRs;
    private JTextField fldCycles;
    private JTextField fldStackMode;
    private JTextField fldStackValues;
    private JTextField[] fldRRs;
    private MyTextArea fldReass;
    private JList<AbstractBreakpoint> listPCBreakpoint;
    private JList<AbstractBreakpoint> listMemBreakpoint;
    private JList<AbstractBreakpoint> listRegBreakpoint;

    private Timer autoStepTimer;


    private enum AutoStepMode {
        STEP_OVER(debugFrmResourceBundle.getString("comboBox.autoStep.stepOver")),
        STEP_INTO(debugFrmResourceBundle.getString("comboBox.autoStep.stepInto")),
        RUN_TO_RET(debugFrmResourceBundle.getString("comboBox.autoStep.runToRET")),
        RUN(debugFrmResourceBundle.getString("comboBox.autoStep.run"));

        private final String displayString;

        AutoStepMode(String displayString) {
            this.displayString = displayString;
        }

        @Override
        public String toString() {
            return displayString;
        }
    }


    public static boolean close() {
        return instance == null || instance.doClose();
    }


    public static void open(Z8 z8) {
        if (instance == null) {
            instance = new DebugFrm(z8);
            if (lastLocation != null) {
                instance.setLocation(lastLocation);
            }
            instance.setVisible(true);
        }
        instance.z8StatusChangedInternal();
        GUIUtil.toFront(instance);
    }


    public static void z8StatusChanged() {
        if (instance != null)
            instance.z8StatusChangedInternal();
    }


    /* --- ActionListener --- */

    @Override
    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();
        if (src != null) {
            if (src == this.btnFlagC) {
                this.fldRegFLAGS.setEditBit(0x80, this.btnFlagC.isSelected());
            } else if (src == this.btnFlagZ) {
                this.fldRegFLAGS.setEditBit(0x40, this.btnFlagZ.isSelected());
            } else if (src == this.btnFlagS) {
                this.fldRegFLAGS.setEditBit(0x20, this.btnFlagS.isSelected());
            } else if (src == this.btnFlagV) {
                this.fldRegFLAGS.setEditBit(0x10, this.btnFlagV.isSelected());
            } else if ((src == this.mnuStop) || (src == this.btnStop)) {
                setDebugAction(Z8.DebugAction.STOP);
            } else if ((src == this.mnuStepOver) || (src == this.btnStepOver)) {
                setDebugAction(Z8.DebugAction.STEP_OVER);
            } else if ((src == this.mnuStepInto) || (src == this.btnStepInto)) {
                setDebugAction(Z8.DebugAction.STEP_INTO);
            } else if ((src == this.mnuRunToRET) || (src == this.btnRunToRET)) {
                setDebugAction(Z8.DebugAction.RUN_TO_RET);
            } else if ((src == this.mnuRun) || (src == this.btnRun)) {
                setDebugAction(Z8.DebugAction.RUN);
            } else if ((src == this.autoStepCheckBox)) {
                toggleAutoStep(this.autoStepCheckBox.isSelected());
            } else if ((src == this.btnLowerGPRs) || (src == this.btnUpperGPRs)) {
                updGPRVisibility();
                updGPRs(this.z8Pause);
                updWorkingRegGroupMarking(this.z8Pause);
            } else if (src == this.mnuClose) {
                doClose();
            } else if (src == this.mnuBreakpointRemove) {
                doBreakpointRemove(this.lastFocussedFld);
            } else if (src == this.mnuBreakpointEnable) {
                doBreakpointSetStatus(this.lastFocussedFld, true);
            } else if (src == this.mnuBreakpointDisable) {
                doBreakpointSetStatus(this.lastFocussedFld, false);
            } else if (src == this.mnuBreakpointEnableAll) {
                doBreakpointSetStatusAll(null, true);
            } else if (src == this.mnuBreakpointDisableAll) {
                doBreakpointSetStatusAll(null, false);
            } else if (src == this.mnuHelpContent) {
                HelpFrm.open("/help/se/debugger.htm"); // TODO: i18n
            } else if (src == this.btnCyclesReset) {
                doResetCycles();
            } else if (src == this.popupBreakpointRemove) {
                doBreakpointRemove(this.triggerFld);
            } else if (src == this.popupBreakpointEnable) {
                doBreakpointSetStatus(this.triggerFld, true);
            } else if (src == this.popupBreakpointDisable) {
                doBreakpointSetStatus(this.triggerFld, false);
            } else if (src == this.popupBreakpointEnableAll) {
                doBreakpointSetStatusAll(this.triggerFld, true);
            } else if (src == this.popupBreakpointDisableAll) {
                doBreakpointSetStatusAll(this.triggerFld, false);
            } else {
                /*
                 * Aktionen, die einen Dialog oeffnen
                 */
                GUIUtil.setWaitCursor(this, true);
                if (src == this.mnuAddPCBreakpoint) {
                    doAddPCBreakpoint();
                } else if (src == this.mnuAddMemBreakpoint) {
                    doAddMemBreakpoint();
                } else if (src == this.mnuAddRegBreakpoint) {
                    doAddRegBreakpoint();
                } else if (src == this.mnuBreakpointEdit) {
                    doBreakpointEdit(this.lastFocussedFld);
                } else if (src == this.mnuBreakpointRemoveAll) {
                    doBreakpointRemoveAll(null);
                } else if (src == this.popupBreakpointAdd) {
                    doAddBreakpoint(this.triggerFld);
                } else if (src == this.popupBreakpointEdit) {
                    doBreakpointEdit(this.triggerFld);
                } else if (src == this.popupBreakpointRemoveAll) {
                    doBreakpointRemoveAll(this.triggerFld);
                } else if (src == this.mnuHelpContent) {
                    HelpFrm.open(debugFrmResourceBundle.getString("help.path"));
                }
                GUIUtil.setWaitCursor(this, false);
            }
        }
    }


    /* --- ChangeListener --- */

    @Override
    public void stateChanged(final ChangeEvent e) {
        EventQueue.invokeLater(
                new Runnable() {
                    @Override
                    public void run() {
                        stateChangedInternal(e);
                    }
                });
    }


    /* --- FocusListener --- */

    @Override
    public void focusGained(FocusEvent e) {
        if (!e.isTemporary()) {
            this.lastFocussedFld = e.getComponent();
            updSelectedBPActionsEnabled();
        }
    }


    @Override
    public void focusLost(FocusEvent e) {
        if (!e.isTemporary()) {
            Component c = e.getComponent();
            if (c instanceof JList) {
                ((JList) c).clearSelection();
            }
        }
    }


    /* --- KeyListener --- */

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
            JList<AbstractBreakpoint> list = getBreakpointList(e.getComponent());
            if (list != null) {
                doBreakpointEdit(list);
                e.consume();
            }
        }
    }


    @Override
    public void keyReleased(KeyEvent e) {
        // leer
    }


    @Override
    public void keyTyped(KeyEvent e) {
        // leer
    }


    /* --- ListSelectionListener --- */

    @Override
    public void valueChanged(ListSelectionEvent e) {
        Object src = e.getSource();
        if ((src == this.listPCBreakpoint)
                || (src == this.listMemBreakpoint)
                || (src == this.listRegBreakpoint)) {
            updSelectedBPActionsEnabled();
        }
    }


    /* --- MouseListener --- */

    @Override
    public void mouseClicked(MouseEvent e) {
        checkShowBreakpointPopup(e);
        if (!e.isConsumed()
                && (e.getButton() == MouseEvent.BUTTON1)
                && (e.getClickCount() > 1)) {
            doBreakpointEdit(e.getComponent());
        }
    }


    @Override
    public void mouseEntered(MouseEvent e) {
        // leer
    }


    @Override
    public void mouseExited(MouseEvent e) {
        // leer
    }


    @Override
    public void mousePressed(MouseEvent e) {
        checkShowBreakpointPopup(e);
    }


    @Override
    public void mouseReleased(MouseEvent e) {
        checkShowBreakpointPopup(e);
    }


    /* --- Z8Debugger --- */

    @Override
    public void z8DebugStatusChanged(Z8 z8) {
        if (z8 == this.z8) {
            final Z8.RunMode runMode = z8.getRunMode();
            EventQueue.invokeLater(
                    new Runnable() {
                        public void run() {
                            debugStatusChanged(runMode);
                        }
                    });
        }
    }


    /* --- ueberschriebene Methoden --- */

    @Override
    protected boolean doClose() {
        boolean state = true;
        if (!this.z8.wasQuitFired()
                && (this.z8.getRunMode() == Z8.RunMode.DEBUG_STOP)) {
            if (JOptionPane.showConfirmDialog(
                    this,
                    debugFrmResourceBundle.getString("dialog.confirmContinue.message"),
                    debugFrmResourceBundle.getString("dialog.confirmContinue.title"),
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) {
                state = false;
            }
        }
        if (state) {
            this.z8.setDebugger(null);
            this.z8.setDebugAction(null);
            this.z8.setBreakpoints(null);

            if (this.autoStepTimer != null) {
                this.autoStepTimer.cancel();
            }

            state = super.doClose();
        }
        if (state) {
            instance = null;
        }
        return state;
    }


    @Override
    public String getPropPrefix() {
        return "debugger.";
    }


    @Override
    public void lafChanged() {
        SwingUtilities.updateComponentTreeUI(this.mnuPopup);
        pack();
    }


    /* --- Konstruktor --- */

    private DebugFrm(Z8 z8) {
        setTitle(debugFrmResourceBundle.getString("window.title") + " Debugger"); // TODO: i18n
        this.z8 = z8;
        this.z8Pause = z8.isPause();
        this.memory = z8.getMemory();
        this.reassembler = new Z8Reassembler(z8.getMemory());
        this.editMode = false;
        this.pcBreakpoints = new BreakpointListModel();
        this.memBreakpoints = new BreakpointListModel();
        this.regBreakpoints = new BreakpointListModel();
        this.lastFocussedFld = null;
        this.triggerFld = null;
        this.grp15Flds = new HexFld[16];
        this.regFlds = new HexFld[128];
        this.regLabels = new JLabel[8];
        this.upperGPRsVisible = false;
        this.markedRegIdx = -1;
        this.markedRegFlds = null;


        // Menu
        JMenuBar mnuBar = new JMenuBar();
        setJMenuBar(mnuBar);


        // Menu Datei
        JMenu mnuFile = new JMenu(debugFrmResourceBundle.getString("menu.file"));
        mnuFile.setMnemonic(KeyEvent.VK_D);
        mnuBar.add(mnuFile);

        this.mnuClose = createJMenuItem(debugFrmResourceBundle.getString("menuItem.close"));
        mnuFile.add(this.mnuClose);


        // Menu Debuggen
        JMenu mnuDebug = new JMenu(debugFrmResourceBundle.getString("menu.debug"));
        mnuDebug.setMnemonic('b');
        mnuBar.add(mnuDebug);

        this.mnuStop = createJMenuItem(debugFrmResourceBundle.getString("menuItem.stop"), KeyEvent.VK_F4, 0);
        mnuDebug.add(this.mnuStop);

        this.mnuRun = createJMenuItem(debugFrmResourceBundle.getString("menuItem.run"), KeyEvent.VK_F5, 0);
        this.mnuRun.setEnabled(false);
        mnuDebug.add(this.mnuRun);

        this.mnuStepOver = createJMenuItem(debugFrmResourceBundle.getString("menuItem.stepOver"), KeyEvent.VK_F6, 0);
        this.mnuStepOver.setEnabled(false);
        mnuDebug.add(this.mnuStepOver);

        this.mnuStepInto = createJMenuItem(debugFrmResourceBundle.getString("menuItem.stepInto"), KeyEvent.VK_F7, 0);
        this.mnuStepInto.setEnabled(false);
        mnuDebug.add(this.mnuStepInto);

        this.mnuRunToRET = createJMenuItem(debugFrmResourceBundle.getString("menuItem.runToRET"), KeyEvent.VK_F8, 0);
        this.mnuRunToRET.setEnabled(false);
        mnuDebug.add(this.mnuRunToRET);
        mnuDebug.addSeparator();

        this.mnuAddPCBreakpoint = createJMenuItem(debugFrmResourceBundle.getString("menuItem.addBreakpoint"), KeyEvent.VK_F9, 0);
        mnuDebug.add(this.mnuAddPCBreakpoint);

        this.mnuAddMemBreakpoint = createJMenuItem(
                "Haltepunkt auf Speicher hinzuf\u00FCgen..."); // TODO: i18n
        mnuDebug.add(this.mnuAddMemBreakpoint);

        this.mnuAddRegBreakpoint = createJMenuItem(
                "Haltepunkt auf Register hinzuf\u00FCgen..."); // TODO: i18n
        mnuDebug.add(this.mnuAddRegBreakpoint);

        this.mnuBreakpointEdit = createJMenuItem("Haltepunkt bearbeiten..."); // TODO: i18n
        mnuDebug.add(this.mnuBreakpointEdit);

        this.mnuBreakpointRemove = createJMenuItem(debugFrmResourceBundle.getString("menuItem.removeBreakpoint"), KeyEvent.VK_DELETE, 0);
        mnuDebug.add(this.mnuBreakpointRemove);

        this.mnuBreakpointRemoveAll = createJMenuItem(debugFrmResourceBundle.getString("menuItem.removeAllBreakpoints"));
        mnuDebug.add(this.mnuBreakpointRemoveAll);
        mnuDebug.addSeparator();

        this.mnuBreakpointEnable = createJMenuItem(debugFrmResourceBundle.getString("menuItem.enableBreakpoint"));
        mnuDebug.add(this.mnuBreakpointEnable);

        this.mnuBreakpointDisable = createJMenuItem(debugFrmResourceBundle.getString("menuItem.disableBreakpoint"));
        mnuDebug.add(this.mnuBreakpointDisable);

        this.mnuBreakpointEnableAll = createJMenuItem(debugFrmResourceBundle.getString("menuItem.enableAllBreakpoints"));
        mnuDebug.add(this.mnuBreakpointEnableAll);

        this.mnuBreakpointDisableAll = createJMenuItem(debugFrmResourceBundle.getString("menuItem.disableAllBreakpoints"));
        mnuDebug.add(this.mnuBreakpointDisableAll);


        // Menu Hilfe
        JMenu mnuHelp = new JMenu(debugFrmResourceBundle.getString("menu.help"));
        mnuHelp.setMnemonic(KeyEvent.VK_H);
        mnuBar.add(mnuHelp);

        this.mnuHelpContent = new JMenuItem(debugFrmResourceBundle.getString("menuItem.helpContent"));
        this.mnuHelpContent.addActionListener(this);
        mnuHelp.add(this.mnuHelpContent);


        // PopupMenu
        this.mnuPopup = new JPopupMenu();

        this.popupBreakpointAdd = createJMenuItem(debugFrmResourceBundle.getString("menuItem.addBreakpoint"));
        this.mnuPopup.add(this.popupBreakpointAdd);

        this.popupBreakpointEdit = createJMenuItem(
                "Haltepunkt bearbeiten..."); // TODO: i18n
        this.mnuPopup.add(this.popupBreakpointEdit);
        this.mnuPopup.addSeparator();

        this.popupBreakpointEnable = createJMenuItem(debugFrmResourceBundle.getString("menuItem.enableBreakpoint"));
        this.mnuPopup.add(this.popupBreakpointEnable);

        this.popupBreakpointDisable = createJMenuItem(debugFrmResourceBundle.getString("menuItem.disableBreakpoint"));
        this.mnuPopup.add(this.popupBreakpointDisable);
        this.mnuPopup.addSeparator();

        this.popupBreakpointEnableAll = createJMenuItem(debugFrmResourceBundle.getString("menuItem.enableAllBreakpoints"));
        this.mnuPopup.add(this.popupBreakpointEnableAll);

        this.popupBreakpointDisableAll = createJMenuItem(debugFrmResourceBundle.getString("menuItem.disableAllBreakpoints"));
        this.mnuPopup.add(this.popupBreakpointDisableAll);
        this.mnuPopup.addSeparator();

        this.popupBreakpointRemove = createJMenuItem(debugFrmResourceBundle.getString("menuItem.removeBreakpoint"));
        this.mnuPopup.add(this.popupBreakpointRemove);

        this.popupBreakpointRemoveAll = createJMenuItem(debugFrmResourceBundle.getString("menuItem.removeAllBreakpoints"));
        this.mnuPopup.add(this.popupBreakpointRemoveAll);


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


        // Kopfbereich
        JPanel panelHead = new JPanel();
        panelHead.setLayout(new BoxLayout(panelHead, BoxLayout.X_AXIS));
        add(panelHead, gbc);

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.setBorderPainted(false);
        toolBar.setOrientation(JToolBar.HORIZONTAL);
        toolBar.setRollover(true);
        panelHead.add(toolBar);

        this.btnRun = GUIUtil.createImageButton(
                this,
                "/images/debug/run.png",
                debugFrmResourceBundle.getString("button.run"));
        this.btnRun.setEnabled(false);
        toolBar.add(this.btnRun);

        this.btnStop = GUIUtil.createImageButton(
                this,
                "/images/debug/stop.png",
                debugFrmResourceBundle.getString("button.stop"));
        toolBar.add(this.btnStop);

        this.btnStepOver = GUIUtil.createImageButton(
                this,
                "/images/debug/step_over.png",
                debugFrmResourceBundle.getString("button.stepOver"));
        this.btnStepOver.setEnabled(false);
        toolBar.add(this.btnStepOver);

        this.btnStepInto = GUIUtil.createImageButton(
                this,
                "/images/debug/step_into.png",
                debugFrmResourceBundle.getString("button.stepInto"));
        this.btnStepInto.setEnabled(false);
        toolBar.add(this.btnStepInto);

        this.btnRunToRET = GUIUtil.createImageButton(
                this,
                "/images/debug/step_up.png",
                debugFrmResourceBundle.getString("button.runToRET"));
        this.btnRunToRET.setEnabled(false);
        toolBar.add(this.btnRunToRET);

        this.autoStepCheckBox = new JCheckBox(debugFrmResourceBundle.getString("checkBox.autoStep"));
        this.autoStepCheckBox.addActionListener(this);
        this.autoStepCheckBox.setEnabled(false);
        toolBar.add(this.autoStepCheckBox);

        this.autoStepModeComboBox = new JComboBox(new DefaultComboBoxModel(AutoStepMode.values()));
        toolBar.add(this.autoStepModeComboBox);

        this.autoStepLabel = new JLabel(debugFrmResourceBundle.getString("label.autoStep"));
        toolBar.add(this.autoStepLabel);

        this.autoStepFrequency = new JTextField(5);
        this.autoStepFrequency.setText(Integer.toString(DEFAULT_AUTOSTEP_FREQUENCY));
        toolBar.add(this.autoStepFrequency);

        this.autoStepFrequencyUnitLabel = new JLabel(debugFrmResourceBundle.getString("label.autoStep.frequencyUnit"));
        toolBar.add(this.autoStepFrequencyUnitLabel);

        panelHead.add(Box.createRigidArea(new Dimension(20, 0)));

        this.labelRegSelection = new JLabel("Register anzeigen:"); // TODO: i18n
        panelHead.add(this.labelRegSelection);

        panelHead.add(Box.createRigidArea(new Dimension(5, 0)));

        ButtonGroup grpGRPs = new ButtonGroup();

        this.btnLowerGPRs = new JRadioButton("bis %80", true); // TODO: i18n
        this.btnLowerGPRs.addActionListener(this);
        grpGRPs.add(this.btnLowerGPRs);
        panelHead.add(this.btnLowerGPRs);

        this.btnUpperGPRs = new JRadioButton("ab %80"); // TODO: i18n
        this.btnUpperGPRs.addActionListener(this);
        grpGRPs.add(this.btnUpperGPRs);
        panelHead.add(this.btnUpperGPRs);


        // Bereich Register
        JPanel panelReg = new JPanel(new GridBagLayout());
        panelReg.setBorder(BorderFactory.createTitledBorder(debugFrmResourceBundle.getString("titledBorder.registers")));
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0.0;
        gbc.gridwidth = 1;
        gbc.gridheight = 3;
        gbc.gridy++;
        add(panelReg, gbc);

        GridBagConstraints gbcReg = new GridBagConstraints(
                1, 0,
                1, 1,
                0.0, 0.0,
                GridBagConstraints.CENTER,
                GridBagConstraints.NONE,
                new Insets(5, 2, 2, 2),
                0, 0);

        panelReg.add(new JLabel("SIO"), gbcReg);
        gbcReg.gridx++;
        panelReg.add(new JLabel("TMR"), gbcReg);
        gbcReg.gridx++;
        panelReg.add(new JLabel("T1"), gbcReg);
        gbcReg.gridx++;
        panelReg.add(new JLabel("PRE1"), gbcReg);
        gbcReg.gridx++;
        panelReg.add(new JLabel("T0"), gbcReg);
        gbcReg.gridx++;
        panelReg.add(new JLabel("PRE0"), gbcReg);
        gbcReg.gridx++;
        panelReg.add(new JLabel("P2M"), gbcReg);
        gbcReg.gridx++;
        panelReg.add(new JLabel("P3M"), gbcReg);
        gbcReg.gridx++;
        panelReg.add(new JLabel("P01M"), gbcReg);
        gbcReg.gridx++;
        panelReg.add(new JLabel("IPR"), gbcReg);
        gbcReg.gridx++;
        panelReg.add(new JLabel("IRQ"), gbcReg);
        gbcReg.gridx++;
        panelReg.add(new JLabel("IMR"), gbcReg);
        gbcReg.gridx++;
        panelReg.add(new JLabel("FLG"), gbcReg);
        gbcReg.gridx++;
        panelReg.add(new JLabel("RP"), gbcReg);
        gbcReg.gridx++;
        panelReg.add(new JLabel("SPH"), gbcReg);
        gbcReg.insets.right = 5;
        gbcReg.gridx++;
        panelReg.add(new JLabel("SPL"), gbcReg);

        /*
         * Dieses Label muss die breiteste Zelle in der Spalte sein!
         * Deshalb das Leerzeichen vor und hinter dem F
         */
        gbcReg.insets.top = 2;
        gbcReg.insets.right = 2;
        gbcReg.gridx = 0;
        gbcReg.gridy++;
        panelReg.add(new JLabel(" F "), gbcReg);

        for (int i = 0; i < this.grp15Flds.length; i++) {
            gbcReg.gridx++;
            HexFld fld = new HexFld(2);
            this.grp15Flds[i] = fld;
            if (i == 15) {
                gbcReg.insets.right = 5;
            }
            panelReg.add(fld.getComponent(), gbcReg);
            fld.addChangeListener(this);
        }
        this.grp15Flds[0].setReadOnly(true);        // SIO
        this.grp15Flds[2].setReadOnly(true);        // T1
        this.grp15Flds[3].setReadOnly(true);        // PRE1
        this.grp15Flds[4].setReadOnly(true);        // T0
        this.grp15Flds[5].setReadOnly(true);        // PRE0

        gbcReg.insets.top = 5;
        gbcReg.insets.right = 2;
        gbcReg.gridx = 0;
        gbcReg.gridy++;
        for (int i = 0; i < 16; i++) {
            gbcReg.gridx++;
            panelReg.add(new JLabel(String.format("%1X", i)), gbcReg);
        }

        gbcReg.insets.top = 2;
        for (int rRow = (this.regFlds.length / 16) - 1; rRow >= 0; --rRow) {
            gbcReg.insets.right = 2;
            if (rRow == 0) {
                gbcReg.insets.bottom = 5;
            }
            gbcReg.gridx = 0;
            gbcReg.gridy++;
            this.regLabels[rRow] = new JLabel(String.format("%1X", rRow));
            panelReg.add(this.regLabels[rRow], gbcReg);
            for (int rCol = 0; rCol < 16; rCol++) {
                gbcReg.gridx++;
                HexFld fld = null;
                if ((rRow > 0) || (rCol >= 2)) {
                    fld = new HexFld(2);
                    panelReg.add(fld.getComponent(), gbcReg);
                    fld.addChangeListener(this);
                }
                this.regFlds[(rRow * 16) + rCol] = fld;
            }
        }
        this.regFlds[2].setReadOnly(true);
        this.regFlds[3].setReadOnly(true);

        gbcReg.insets.bottom = 5;
        gbcReg.gridx = 0;
        gbcReg.gridy++;
        for (int i = 0; i < 16; i++) {
            gbcReg.gridx++;
            panelReg.add(new JLabel(String.format("R%d", i)), gbcReg);
        }

        this.fldRegSPL = this.grp15Flds[SPL - 0xF0];
        this.fldRegSPH = this.grp15Flds[SPH - 0xF0];
        this.fldRegFLAGS = this.grp15Flds[FLAGS - 0xF0];
        this.fldRegRP = this.grp15Flds[RP - 0xF0];
        this.fldRegP01M = this.grp15Flds[P01M - 0xF0];


        // Bereich Programmausfuehrung
        JPanel panelPC = new JPanel(new GridBagLayout());
        panelPC.setBorder(BorderFactory.createTitledBorder(debugFrmResourceBundle.getString("titledBorder.programExecution")));
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;
        gbc.gridheight = 1;
        gbc.gridy += 3;
        add(panelPC, gbc);

        GridBagConstraints gbcPC = new GridBagConstraints(
                0, 0,
                1, 1,
                0.0, 0.0,
                GridBagConstraints.EAST,
                GridBagConstraints.NONE,
                new Insets(5, 5, 0, 5),
                0, 0);

        panelPC.add(new JLabel("Stack:"), gbcPC); // TODO: i18n

        this.fldStackMode = new JTextField();
        this.fldStackMode.setEditable(false);
        gbcPC.anchor = GridBagConstraints.WEST;
        gbcPC.fill = GridBagConstraints.HORIZONTAL;
        gbcPC.insets.left = 0;
        gbcPC.gridx++;
        panelPC.add(this.fldStackMode, gbcPC);

        this.fldStackValues = new JTextField();
        this.fldStackValues.setEditable(false);
        gbcPC.weightx = 1.0;
        gbcPC.gridx++;
        panelPC.add(this.fldStackValues, gbcPC);

        Font monospacedFont = GUIUtil.getMonospacedFont(this.fldStackValues);
        this.fldStackValues.setFont(monospacedFont);

        gbcPC.anchor = GridBagConstraints.EAST;
        gbcPC.fill = GridBagConstraints.NONE;
        gbcPC.weightx = 0.0;
        gbcPC.insets.left = 5;
        gbcPC.gridx = 0;
        gbcPC.gridy++;
        panelPC.add(new JLabel("PC:"), gbcPC);

        this.fldPC = new HexFld(4);
        gbcPC.anchor = GridBagConstraints.WEST;
        gbcPC.fill = GridBagConstraints.HORIZONTAL;
        gbcPC.insets.left = 0;
        gbcPC.gridx++;
        panelPC.add(this.fldPC.getComponent(), gbcPC);

        this.fldReass = new MyTextArea(DEFAULT_REASS_ROWS, 0);
        this.fldReass.setBorder(BorderFactory.createEtchedBorder());
        this.fldReass.setFont(monospacedFont);
        this.fldReass.setEditable(false);
        gbcPC.fill = GridBagConstraints.BOTH;
        gbcPC.insets.bottom = 5;
        gbcPC.weightx = 1.0;
        gbcPC.weighty = 1.0;
        gbcPC.gridheight = 3;
        gbcPC.gridx++;
        panelPC.add(this.fldReass, gbcPC);

        /*
         * Aus optischen Gruenden soll hier eine freie Flaeche entstehen,
         * die jedoch zusammengeschoben werden kann.
         * Aus diesem Grund wird hier ein Platzhalter eingefuegt,
         * der nach der Groessenberechnung des Fensters wieder entfernt wird.
         */
        Component placeholder = Box.createVerticalStrut(20);
        gbcPC.anchor = GridBagConstraints.SOUTHWEST;
        gbcPC.fill = GridBagConstraints.NONE;
        gbcPC.weightx = 0.0;
        gbcPC.weighty = 0.0;
        gbcPC.insets.left = 5;
        gbcPC.gridheight = 1;
        gbcPC.gridwidth = 2;
        gbcPC.gridx = 0;
        gbcPC.gridy++;
        panelPC.add(placeholder, gbcPC);

        JPanel panelCycles = new JPanel(new GridBagLayout());
        gbcPC.gridy++;
        panelPC.add(panelCycles, gbcPC);

        GridBagConstraints gbcCycles = new GridBagConstraints(
                0, 0,
                1, 1,
                0.0, 0.0,
                GridBagConstraints.WEST,
                GridBagConstraints.NONE,
                new Insets(0, 0, 0, 0),
                0, 0);

        panelCycles.add(new JLabel("Interne Taktzyklen:"), gbcCycles); // TODO: i18n

        this.fldCycles = new JTextField();
        this.fldCycles.setEditable(false);
        gbcCycles.fill = GridBagConstraints.HORIZONTAL;
        gbcCycles.insets.top = 5;
        gbcCycles.gridy++;
        panelCycles.add(this.fldCycles, gbcCycles);

        this.btnCyclesReset = new JButton("Zur\u00FCcksetzen"); // TODO: i18n
        this.btnCyclesReset.setEnabled(false);
        this.btnCyclesReset.addActionListener(this);
        gbcCycles.gridy++;
        panelCycles.add(this.btnCyclesReset, gbcCycles);

        this.fldPC.addChangeListener(this);


        // Bereich Flags
        JPanel panelFlags = new JPanel();
        panelFlags.setBorder(BorderFactory.createTitledBorder("Flags")); // TODO: i18n
        gbc.fill = GridBagConstraints.NONE;
        gbc.weighty = 0.0;
        gbc.insets.left = 5;
        gbc.gridheight = 1;
        gbc.gridy = 1;
        gbc.gridx++;
        add(panelFlags, gbc);

        panelFlags.setLayout(new BoxLayout(panelFlags, BoxLayout.X_AXIS));

        this.btnFlagC = new JCheckBox("Carry"); // TODO: i18n
        this.btnFlagC.addActionListener(this);
        panelFlags.add(this.btnFlagC);

        this.btnFlagZ = new JCheckBox("Zero"); // TODO: i18n
        this.btnFlagZ.addActionListener(this);
        panelFlags.add(this.btnFlagZ);

        this.btnFlagS = new JCheckBox("Sign"); // TODO: i18n
        this.btnFlagS.addActionListener(this);
        panelFlags.add(this.btnFlagS);

        this.btnFlagV = new JCheckBox("Overflow"); // TODO: i18n
        this.btnFlagV.addActionListener(this);
        panelFlags.add(this.btnFlagV);


        // Bereich Doppelregister zeigen auf
        JPanel panelRR = new JPanel(new GridBagLayout());
        panelRR.setBorder(
                BorderFactory.createTitledBorder(
                        "Doppelregister zeigen auf ...")); // TODO: i18n
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridy++;
        add(panelRR, gbc);

        GridBagConstraints gbcRR = new GridBagConstraints(
                0, 0,
                1, 1,
                0.0, 0.0,
                GridBagConstraints.WEST,
                GridBagConstraints.NONE,
                new Insets(5, 5, 0, 5),
                0, 0);

        this.fldRRs = new JTextField[8];
        for (int i = 0; i < this.fldRRs.length; i++) {
            if (i == 0) {
                gbcRR.insets.top = 2;
            } else if (i == 7) {
                gbcRR.insets.bottom = 5;
            }

            gbcRR.fill = GridBagConstraints.NONE;
            gbcRR.weightx = 0.0;
            gbcRR.gridx = 0;
            panelRR.add(new JLabel(String.format("@RR%d:", i * 2)), gbcRR);

            this.fldRRs[i] = new JTextField();
            this.fldRRs[i].setEditable(false);
            gbcRR.fill = GridBagConstraints.HORIZONTAL;
            gbcRR.weightx = 1.0;
            gbcRR.gridx++;
            panelRR.add(this.fldRRs[i], gbcRR);

            gbcRR.gridy++;
        }


        // Bereich Haltepunkte
        JPanel panelBreakpoint = new JPanel(new GridBagLayout());
        panelBreakpoint.setBorder(
                BorderFactory.createTitledBorder("Haltepunkte auf ...")); // TODO: i18n
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;
        gbc.gridheight = 2;
        gbc.gridy++;
        add(panelBreakpoint, gbc);

        GridBagConstraints gbcBreakpoint = new GridBagConstraints(
                0, 0,
                1, 1,
                0.0, 0.0,
                GridBagConstraints.WEST,
                GridBagConstraints.NONE,
                new Insets(5, 5, 0, 5),
                0, 0);

        panelBreakpoint.add(new JLabel("PC-Adresse"), gbcBreakpoint); // TODO: i18n
        gbcBreakpoint.gridx++;
        panelBreakpoint.add(new JLabel("Speicher"), gbcBreakpoint); // TODO: i18n
        gbcBreakpoint.gridx++;
        panelBreakpoint.add(new JLabel("Register"), gbcBreakpoint); // TODO: i18n

        gbcBreakpoint.weightx = 0.25;
        gbcBreakpoint.weighty = 1.0;
        gbcBreakpoint.fill = GridBagConstraints.BOTH;
        gbcBreakpoint.insets.top = 0;
        gbcBreakpoint.insets.bottom = 5;
        gbcBreakpoint.gridx = 0;
        gbcBreakpoint.gridy++;

        AbstractBreakpoint prototypeBP = new MemBreakpoint(
                0xFFFF,
                0xFFFF,
                true,
                true);

        this.breakpointCellRenderer = new BreakpointCellRenderer();

        this.listPCBreakpoint = createBreakpointList(
                this.pcBreakpoints,
                prototypeBP);
        panelBreakpoint.add(
                new JScrollPane(this.listPCBreakpoint),
                gbcBreakpoint);

        this.listMemBreakpoint = createBreakpointList(
                this.memBreakpoints,
                prototypeBP);
        gbcBreakpoint.weightx = 0.45;
        gbcBreakpoint.gridx++;
        panelBreakpoint.add(
                new JScrollPane(this.listMemBreakpoint),
                gbcBreakpoint);

        this.listRegBreakpoint = createBreakpointList(
                this.regBreakpoints,
                prototypeBP);
        gbcBreakpoint.weightx = 0.3;
        gbcBreakpoint.gridx++;
        panelBreakpoint.add(
                new JScrollPane(this.listRegBreakpoint),
                gbcBreakpoint);


        // Statuszeile
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weighty = 0.0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.insets.top = 0;
        gbc.insets.right = 0;
        gbc.gridheight = 1;
        gbc.gridx = 0;
        gbc.gridy += 2;
        add(new JSeparator(), gbc);

        this.labelStatus = new JLabel();
        gbc.weightx = 0.0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets.left = 5;
        gbc.insets.right = 5;
        gbc.gridy++;
        add(this.labelStatus, gbc);
        updStatusText(this.z8.getRunMode());


        // sonstiges
        updGPRVisibility();
        updAllBPActionsEnabled();
        updSelectedBPActionsEnabled();
        updFields(this.z8Pause);
        setResizable(true);
        if (!GUIUtil.applyWindowSettings(this)) {
            pack();
            setLocationByPlatform(true);
        }
        z8.setDebugger(this);
        this.fldReass.setRows(0);
        panelPC.remove(placeholder);
        addComponentListener(
                new ComponentAdapter() {
                    @Override
                    public void componentResized(ComponentEvent e) {
                        updReassFld();
                    }
                });
    }


    /* --- Aktionen --- */

    private void doAddBreakpoint(Component c) {
        if (c != null) {
            c = getJList(c);
            if (c != null) {
                if (c == this.listPCBreakpoint) {
                    doAddPCBreakpoint();
                } else if (c == this.listMemBreakpoint) {
                    doAddMemBreakpoint();
                } else if (c == this.listRegBreakpoint) {
                    doAddRegBreakpoint();
                }
            }
        }
    }


    private void doAddPCBreakpoint() {
        addBreakpoint(
                this.listPCBreakpoint,
                PCBreakpointDlg.openAdd(this));
    }


    private void doAddMemBreakpoint() {
        addBreakpoint(
                this.listMemBreakpoint,
                MemBreakpointDlg.openAdd(this));
    }


    private void doAddRegBreakpoint() {
        addBreakpoint(
                this.listRegBreakpoint,
                RegBreakpointDlg.openAdd(this));
    }


    private void doBreakpointEdit(Component c) {
        if (c != null) {
            c = getJList(c);
            if (c != null) {
                if (c == this.listPCBreakpoint) {
                    AbstractBreakpoint bp = PCBreakpointDlg.openEdit(
                            this,
                            getSingleSelectedBreakpoint(this.listPCBreakpoint));
                    if (bp != null) {
                        updBreakpointList(this.listPCBreakpoint, bp);
                    }
                } else if (c == this.listMemBreakpoint) {
                    AbstractBreakpoint bp = MemBreakpointDlg.openEdit(
                            this,
                            getSingleSelectedBreakpoint(this.listMemBreakpoint));
                    if (bp != null) {
                        updBreakpointList(this.listMemBreakpoint, bp);
                    }
                } else if (c == this.listRegBreakpoint) {
                    AbstractBreakpoint bp = RegBreakpointDlg.openEdit(
                            this,
                            getSingleSelectedBreakpoint(this.listRegBreakpoint));
                    if (bp != null) {
                        updBreakpointList(this.listRegBreakpoint, bp);
                    }
                }
            }
        }
    }


    private void doBreakpointRemove(Component c) {
        if (c != null) {
            JList<AbstractBreakpoint> list = getBreakpointList(c);
            if (list != null) {
                java.util.List<AbstractBreakpoint> selBPs
                        = list.getSelectedValuesList();
                if (!selBPs.isEmpty()) {
                    BreakpointListModel model = getBreakpointListModel(list);
                    if (model != null) {
                        model.removeAll(selBPs);
                        updAllBPActionsEnabled();
                        updSelectedBPActionsEnabled();
                        updBreakpointsInZ8();
                    }
                }
            }
        }
    }


    private void doBreakpointRemoveAll(Component c) {
        if (JOptionPane.showConfirmDialog(
                this,
                "M\u00F6chten Sie alle Haltepunkte entfernen?", // TODO: i18n
                "Best\u00E4tigung", // TODO: i18n
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
            if (c != null) {
                BreakpointListModel bps = getCorrespondingBreakpoints(c);
                if (bps != null) {
                    bps.clear();
                }
            } else {
                this.pcBreakpoints.clear();
                this.memBreakpoints.clear();
                this.regBreakpoints.clear();
            }
            updAllBPActionsEnabled();
            updSelectedBPActionsEnabled();
            updBreakpointsInZ8();
        }
    }


    private void doBreakpointSetStatus(Component c, boolean state) {
        JList<AbstractBreakpoint> list = getBreakpointList(c);
        if (list != null) {
            BreakpointListModel model = getBreakpointListModel(list);
            if (model != null) {
                int[] indices = list.getSelectedIndices();
                if (indices != null) {
                    for (int i : indices) {
                        model.getElementAt(i).setEnabled(state);
                        model.fireContentsChanged(i, i);
                    }
                }
            }
            updAllBPActionsEnabled();
            updSelectedBPActionsEnabled();
            updBreakpointsInZ8();
        }
    }


    private void doBreakpointSetStatusAll(Component c, boolean state) {
        if (c != null) {
            BreakpointListModel bps = getCorrespondingBreakpoints(c);
            if (bps != null) {
                int n = bps.getSize();
                for (int i = 0; i < n; i++) {
                    bps.getElementAt(i).setEnabled(state);
                }
                bps.fireContentsChanged();
                updAllBPActionsEnabled();
                updSelectedBPActionsEnabled();
                updBreakpointsInZ8();
            }
        }
    }


    private void doResetCycles() {
        this.z8.resetTotalCycles();
        this.fldCycles.setText(this.z8Pause ? "0" : "");
    }


    /* --- private Methoden --- */

    private void addBreakpoint(
            JList<AbstractBreakpoint> list,
            AbstractBreakpoint bp) {
        if (bp != null) {
            BreakpointListModel model = getBreakpointListModel(list);
            if (model != null) {
                int idx = model.indexOf(bp);
                if (idx < 0) {
                    model.addElement(bp);
                    idx = model.indexOf(bp);
                }
                if (idx >= 0) {
                    list.setSelectedIndex(idx);
                    list.requestFocus();
                }
                updAllBPActionsEnabled();
                updSelectedBPActionsEnabled();
                updBreakpointsInZ8();
            }
        }
    }


    private void addEnabledBreakpointsTo(
            java.util.List<AbstractBreakpoint> dst,
            BreakpointListModel src) {
        int n = src.getSize();
        for (int i = 0; i < n; i++) {
            AbstractBreakpoint bp = src.getElementAt(i);
            if (bp.isEnabled()) {
                dst.add(bp);
            }
        }
    }


    private void checkShowBreakpointPopup(MouseEvent e) {
        Component c = e.getComponent();
        if ((c != null) && e.isPopupTrigger()) {
            JList<AbstractBreakpoint> list = getBreakpointList(c);
            if (list != null) {
                this.triggerFld = list;

                boolean stateAll = false;
                boolean stateSelected = false;
                boolean allEnabled = false;
                boolean allDisabled = false;
                boolean selectedEnabled = false;
                boolean selectedDisabled = false;

                BreakpointListModel model = getBreakpointListModel(list);
                if (model != null) {
                    int n = model.getSize();
                    if (n > 0) {
                        stateAll = true;
                        for (int i = 0; i < n; i++) {
                            AbstractBreakpoint bp = model.getElementAt(i);
                            if (bp != null) {
                                if (bp.isEnabled()) {
                                    allEnabled = true;
                                } else {
                                    allDisabled = true;
                                }
                            }
                        }
                    }
                }
                for (AbstractBreakpoint bp : list.getSelectedValuesList()) {
                    if (bp != null) {
                        stateSelected = true;
                        if (bp.isEnabled()) {
                            selectedEnabled = true;
                        } else {
                            selectedDisabled = true;
                        }
                    }
                }
                this.popupBreakpointEdit.setEnabled(stateSelected);
                this.popupBreakpointRemove.setEnabled(stateSelected);
                this.popupBreakpointEnable.setEnabled(selectedDisabled);
                this.popupBreakpointDisable.setEnabled(selectedEnabled);
                this.popupBreakpointEnableAll.setEnabled(allDisabled);
                this.popupBreakpointDisableAll.setEnabled(allEnabled);
                this.popupBreakpointRemoveAll.setEnabled(stateAll);
                this.mnuPopup.show(e.getComponent(), e.getX(), e.getY());
                e.consume();
            }
        }
    }


    private JList<AbstractBreakpoint> createBreakpointList(
            BreakpointListModel model,
            AbstractBreakpoint prototypeBP) {
        JList<AbstractBreakpoint> list = new JList<>(model);
        list.setCellRenderer(this.breakpointCellRenderer);
        list.setDragEnabled(false);
        list.setLayoutOrientation(JList.VERTICAL);
        list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        list.setVisibleRowCount(2);
        if (prototypeBP != null) {
            list.setPrototypeCellValue(prototypeBP);
        }
        list.addFocusListener(this);
        list.addKeyListener(this);
        list.addListSelectionListener(this);
        list.addMouseListener(this);
        return list;
    }


    private JMenuItem createJMenuItem(String text) {
        JMenuItem item = new JMenuItem(text);
        item.addActionListener(this);
        return item;
    }


    private JMenuItem createJMenuItem(String text, int keyCode, int modifiers) {
        JMenuItem item = createJMenuItem(text);
        item.setAccelerator(KeyStroke.getKeyStroke(keyCode, modifiers));
        return item;
    }


    private void debugStatusChanged(Z8.RunMode runMode) {
        if (runMode != null) {
            switch (runMode) {
                case RUNNING:
                    if (this.editMode) {
                        this.editMode = false;

                        this.mnuStop.setEnabled(true);
                        this.mnuStepOver.setEnabled(false);
                        this.mnuStepInto.setEnabled(false);
                        this.mnuRunToRET.setEnabled(false);
                        this.mnuRun.setEnabled(false);

                        this.btnStop.setEnabled(true);
                        this.btnStepOver.setEnabled(false);
                        this.btnStepInto.setEnabled(false);
                        this.btnRunToRET.setEnabled(false);
                        this.btnRun.setEnabled(false);

                        this.autoStepCheckBox.setEnabled(false);

                        disableRegEdit(this.grp15Flds, 0, 16, 0xF0);
                        disableRegEdit(this.regFlds, 0, 16, 0x00);
                        updFlagCheckBoxes(false);

                        this.fldPC.clearValue();
                        this.fldReass.setText("");
                        this.fldStackMode.setText("");
                        this.fldStackValues.setText("");
                    }
                    break;

                case DEBUG_STOP:
                    this.editMode = true;

                    this.mnuStop.setEnabled(false);
                    this.mnuStepOver.setEnabled(true);
                    this.mnuStepInto.setEnabled(true);
                    this.mnuRunToRET.setEnabled(true);
                    this.mnuRun.setEnabled(true);

                    this.btnStop.setEnabled(false);
                    this.btnStepOver.setEnabled(true);
                    this.btnStepInto.setEnabled(true);
                    this.btnRunToRET.setEnabled(true);
                    this.btnRun.setEnabled(true);

                    this.autoStepCheckBox.setEnabled(true);

                    enableRegEdit(this.grp15Flds, 0, 16, 0xF0);
                    enableRegEdit(this.regFlds, 0, 16, 0x00);
                    updFlagCheckBoxes(true);

                    StringBuilder buf = new StringBuilder(128);
                    if (this.z8.isInternalStackEnabled()) {
                        this.fldStackMode.setText(debugFrmResourceBundle.getString("textfield.stackMode.internal"));
                        int sp = this.grp15Flds[SPL - 0xF0].getOrgValue();
                        for (int i = 0; i < 16; i++) {
                            if (i > 0) {
                                buf.append((char) '\u0020');
                            }
                            buf.append(String.format(
                                    "%02X",
                                    this.z8.viewRegValue(sp++)));
                        }
                    } else {
                        this.fldStackMode.setText(debugFrmResourceBundle.getString("textfield.stackMode.external"));
                        int sp = (this.grp15Flds[SPH - 0xF0].getOrgValue() << 8)
                                | this.grp15Flds[SPL - 0xF0].getOrgValue();
                        for (int i = 0; i < 16; i++) {
                            if (i > 0) {
                                buf.append((char) '\u0020');
                            }
                            buf.append(String.format(
                                    "%02X",
                                    this.memory.getMemByte(sp++, false)));
                        }
                    }
                    this.fldStackValues.setText(buf.toString());

                    int addr = this.z8.getPC();
                    this.fldPC.setValue(addr);
                    updReassFld();
                    break;
            }
            updStatusText(runMode);
        }
    }


    private void disableRegEdit(HexFld[] fields, int pos, int len, int addr) {
        while ((pos < fields.length) && (len > 0)) {
            HexFld fld = fields[pos++];
            if (fld != null) {
                if (!fld.isReadOnly() && fld.wasDataChanged()) {
                    this.z8.setRegValue(addr, fld.getValue());
                }
                fld.clearValue();
            }
            addr++;
        }
    }

    private void enableRegEdit(HexFld[] fields, int pos, int len, int addr) {
        while ((pos < fields.length) && (len > 0)) {
            HexFld fld = fields[pos++];
            if (fld != null) {
                int v = this.z8.viewRegValue(addr);
                if (v >= 0) {
                    fld.setValue(v);
                } else {
                    fld.clearValue();
                }
            }
            addr++;
        }
    }


    private JList<AbstractBreakpoint> getBreakpointList(Component c) {
        JList<AbstractBreakpoint> rv = null;
        c = getJList(c);
        while (c != null) {
            if (c == this.listPCBreakpoint) {
                rv = this.listPCBreakpoint;
                break;
            }
            if (c == this.listMemBreakpoint) {
                rv = this.listMemBreakpoint;
                break;
            }
            if (c == this.listRegBreakpoint) {
                rv = this.listRegBreakpoint;
                break;
            }
            c = c.getParent();
        }
        return rv;
    }


    private BreakpointListModel getBreakpointListModel(JList<?> list) {
        BreakpointListModel blm = null;
        if (list != null) {
            ListModel<?> model = list.getModel();
            if (model != null) {
                if (model instanceof BreakpointListModel) {
                    blm = (BreakpointListModel) model;
                }
            }
        }
        return blm;
    }


    private BreakpointListModel getCorrespondingBreakpoints(Component c) {
        BreakpointListModel rv = null;
        c = getJList(c);
        if (c != null) {
            if (c == this.listPCBreakpoint) {
                rv = this.pcBreakpoints;
            } else if (c == this.listMemBreakpoint) {
                rv = this.memBreakpoints;
            } else if (c == this.listRegBreakpoint) {
                rv = this.regBreakpoints;
            }
        }
        return rv;
    }


    private JList<?> getJList(Component c) {
        JList<?> rv = null;
        while (c != null) {
            if (c instanceof JList) {
                rv = (JList) c;
                break;
            }
            c = c.getParent();
        }
        return rv;
    }


    private static AbstractBreakpoint getSingleSelectedBreakpoint(
            JList<AbstractBreakpoint> list) {
        int[] indices = list.getSelectedIndices();
        return ((indices != null) && (indices.length == 1)) ?
                list.getSelectedValue()
                : null;
    }


    private void setDebugAction(Z8.DebugAction debugAction) {
        this.mnuStop.setEnabled(true);
        this.mnuStepOver.setEnabled(false);
        this.mnuStepInto.setEnabled(false);
        this.mnuRunToRET.setEnabled(false);
        this.mnuRun.setEnabled(false);

        this.btnStop.setEnabled(true);
        this.btnStepOver.setEnabled(false);
        this.btnStepInto.setEnabled(false);
        this.btnRunToRET.setEnabled(false);
        this.btnRun.setEnabled(false);

        this.autoStepCheckBox.setEnabled(false);

        disableRegEdit(this.grp15Flds, 0, 16, 0xF0);
        disableRegEdit(this.regFlds, 0, 16, 0x00);
        if (this.fldPC.wasDataChanged()) {
            this.z8.setPC(this.fldPC.getValue());
        }
        this.fldPC.clearValue();
        this.fldReass.setText("");
        this.fldStackMode.setText("");
        this.fldStackValues.setText("");
        updFlagCheckBoxes(false);
        this.z8.setDebugAction(debugAction);
        updStatusText(Z8.RunMode.RUNNING);
    }


    private void updGPRVisibility() {
        boolean upperGPRsVisible = this.btnUpperGPRs.isSelected();
        if (upperGPRsVisible != this.upperGPRsVisible) {
            this.upperGPRsVisible = upperGPRsVisible;
            int v = (upperGPRsVisible ? 8 : 1);
            for (int i = 1; i < this.regLabels.length; i++) {
                this.regLabels[i].setText(String.format("%1X", v++));
            }
        }
    }


    private void stateChangedInternal(ChangeEvent e) {
        Object src = e.getSource();
        if (src == this.fldRegFLAGS) {
            updFlagCheckBoxes(this.z8Pause);
        } else if (src == this.fldRegRP) {
            updWorkingRegGroupMarking(this.z8Pause);
        } else if (src == this.fldPC) {
            updReassFld();
        }
        if ((src instanceof HexFld) && (src != this.fldReass)) {
            updDoubleRegFlds(this.z8Pause);
        }
        if ((src == this.fldRegP01M)
                || (src == this.fldRegSPH)
                || (src == this.fldRegSPL)) {
            updStackFlds(this.z8Pause);
        }
    }


    private void updAllBPActionsEnabled() {
        final BreakpointListModel[] models = {
                this.pcBreakpoints,
                this.memBreakpoints,
                this.regBreakpoints};
        boolean enabled = false;
        boolean disabled = false;
        for (BreakpointListModel model : models) {
            int n = model.getSize();
            for (int i = 0; i < n; i++) {
                if (model.getElementAt(i).isEnabled()) {
                    enabled = true;
                } else {
                    disabled = true;
                }
            }
        }
        this.mnuBreakpointEnableAll.setEnabled(disabled);
        this.mnuBreakpointDisableAll.setEnabled(enabled);
        this.mnuBreakpointRemoveAll.setEnabled(enabled || disabled);
    }


    private void updSelectedBPActionsEnabled() {
        boolean enabled = false;
        boolean disabled = false;
        JList<AbstractBreakpoint> list = getBreakpointList(
                this.lastFocussedFld);
        if (list != null) {
            for (AbstractBreakpoint bp : list.getSelectedValuesList()) {
                if (bp.isEnabled()) {
                    enabled = true;
                } else {
                    disabled = true;
                }
            }
        }
        this.mnuBreakpointDisable.setEnabled(enabled);
        this.mnuBreakpointEnable.setEnabled(disabled);
        this.mnuBreakpointEdit.setEnabled(enabled || disabled);
        this.mnuBreakpointRemove.setEnabled(enabled || disabled);
    }


    private void toggleAutoStep(boolean selected) {
        if (selected) {
            this.autoStepFrequency.setEnabled(false);

            int autoStepFrequency = DEFAULT_AUTOSTEP_FREQUENCY;

            try {
                autoStepFrequency = Integer.parseInt(this.autoStepFrequency.getText());

                if (autoStepFrequency <= 0) {
                    autoStepFrequency = DEFAULT_AUTOSTEP_FREQUENCY;
                }
            } catch (NumberFormatException e) {
                // ingnore
            }

            TimerTask timerTask = new TimerTask() {

                @Override
                public void run() {
                    final Z8.RunMode runMode = DebugFrm.this.z8.getRunMode();

                    // only fire if we aren't currently running
                    if (!Z8.RunMode.RUNNING.equals(runMode)) {
                        final AutoStepMode autoStepMode = (AutoStepMode) DebugFrm.this.autoStepModeComboBox.getSelectedItem();

                        switch (autoStepMode) {
                            case STEP_OVER:
                                EventQueue.invokeLater(new Runnable() {

                                    @Override
                                    public void run() {
                                        setDebugAction(Z8.DebugAction.STEP_OVER);
                                    }
                                });
                                break;
                            case STEP_INTO:
                                EventQueue.invokeLater(new Runnable() {

                                    @Override
                                    public void run() {
                                        setDebugAction(Z8.DebugAction.STEP_INTO);
                                    }
                                });
                                break;
                            case RUN_TO_RET:
                                EventQueue.invokeLater(new Runnable() {

                                    @Override
                                    public void run() {
                                        setDebugAction(Z8.DebugAction.RUN_TO_RET);
                                    }
                                });
                                break;
                            case RUN:
                                EventQueue.invokeLater(new Runnable() {

                                    @Override
                                    public void run() {
                                        setDebugAction(Z8.DebugAction.RUN);
                                    }
                                });
                                break;
                        }
                    }
                }
            };

            this.autoStepTimer = new Timer(true);
            this.autoStepTimer.scheduleAtFixedRate(timerTask, 0, autoStepFrequency);
        } else {
            if (this.autoStepTimer != null) {
                this.autoStepTimer.cancel();
            }

            this.autoStepFrequency.setEnabled(true);
        }
    }


    private void updBreakpointList(
            JList<AbstractBreakpoint> list,
            AbstractBreakpoint bpToSelect) {
        if (bpToSelect != null) {
            BreakpointListModel model = getBreakpointListModel(list);
            if (model != null) {
                int n = model.getSize();
                for (int i = 0; i < n; i++) {
                    AbstractBreakpoint bp = model.getElementAt(i);
                    if (bp.equals(bpToSelect)) {
                        list.setSelectedIndex(i);
                        list.requestFocus();
                        break;
                    }
                }
            }
        }
        updAllBPActionsEnabled();
        updSelectedBPActionsEnabled();
        updBreakpointsInZ8();
    }


    private void updBreakpointsInZ8() {
        AbstractBreakpoint[] bps = null;
        int n = this.pcBreakpoints.getSize()
                + this.memBreakpoints.getSize()
                + this.regBreakpoints.getSize();
        if (n > 0) {
            java.util.List<AbstractBreakpoint> list = new ArrayList<>(n);
            addEnabledBreakpointsTo(list, this.pcBreakpoints);
            addEnabledBreakpointsTo(list, this.memBreakpoints);
            addEnabledBreakpointsTo(list, this.regBreakpoints);
            n = list.size();
            if (n > 0) {
                try {
                    bps = list.toArray(new AbstractBreakpoint[n]);
                } catch (ArrayStoreException ex) {
                }
            }
        }
        this.z8.setBreakpoints(bps);
    }


    private void updDoubleRegFlds(boolean state) {
        state &= this.z8Pause;
        if (state) {
            int rp = this.z8.getRegValue(0xFD) & 0xF0;        // RP
            for (int i = 0; i < this.fldRRs.length; i++) {
                int rr = rp | (i * 2);
                int addr = ((this.z8.getRegValue(rr) << 8) & 0xFF00)
                        | (this.z8.getRegValue(rr + 1) & 0x00FF);
                StringBuilder buf = new StringBuilder();
                for (int k = 0; k < 8; k++) {
                    if (k > 0) {
                        buf.append('\u0020');
                    }
                    buf.append(String.format(
                            "%02X",
                            this.z8.getMemByte(addr++, false)));
                }
                this.fldRRs[i].setText(buf.toString());
                this.fldRRs[i].setEnabled(true);
            }
        } else {
            for (int i = 0; i < this.fldRRs.length; i++) {
                this.fldRRs[i].setText("");
                this.fldRRs[i].setEnabled(false);
            }
        }
    }


    private void updFields(boolean state) {
        boolean hasUpperGPRs = (this.z8.getMaxGPRNum() >= 0x80);
        if (hasUpperGPRs) {
            this.labelRegSelection.setEnabled(state);
            this.btnLowerGPRs.setEnabled(state);
            this.btnUpperGPRs.setEnabled(state);
        } else {
            if (this.btnUpperGPRs.isSelected()) {
                this.btnLowerGPRs.setSelected(true);
            }
            updGPRVisibility();
            this.btnLowerGPRs.setEnabled(false);
            this.btnUpperGPRs.setEnabled(false);
        }
        this.labelRegSelection.setVisible(hasUpperGPRs);
        this.btnLowerGPRs.setVisible(hasUpperGPRs);
        this.btnUpperGPRs.setVisible(hasUpperGPRs);

        this.mnuStop.setEnabled(!state);
        this.mnuStepOver.setEnabled(state);
        this.mnuStepInto.setEnabled(state);
        this.mnuRunToRET.setEnabled(state);
        this.mnuRun.setEnabled(state);

        this.btnStop.setEnabled(!state);
        this.btnStepOver.setEnabled(state);
        this.btnStepInto.setEnabled(state);
        this.btnRunToRET.setEnabled(state);
        this.btnRun.setEnabled(state);
        this.btnCyclesReset.setEnabled(state);

        if (state) {
            this.fldRegRP.setInputMask(this.z8.getRegRPValueMask());

            enableRegEdit(this.grp15Flds, 0, 16, 0xF0);
            enableRegEdit(this.regFlds, 0, 16, 0x00);

            this.fldCycles.setText(String.valueOf(this.z8.getTotalCycles()));

            int addr = this.z8.getPC();
            this.fldPC.setValue(addr);
        } else {
            disableRegEdit(this.grp15Flds, 0, 16, 0xF0);
            disableRegEdit(this.regFlds, 0, 16, 0x00);
            this.fldCycles.setText("");
            this.fldPC.clearValue();
        }
        updGPRs(state);

        updFlagCheckBoxes(state);
        updWorkingRegGroupMarking(state);
        updDoubleRegFlds(state);
        updStackFlds(state);
        updReassFld(state);
        this.labelStatus.setText(state ?
                "Programmausf\u00FChrung angehalten" // TODO: i18n
                : "Programmausf\u00FChrung l\u00E4uft..."); // TODO: i18n
    }


    private void updFlagCheckBoxes(boolean state) {
        state &= this.z8Pause;

        this.btnFlagC.setEnabled(state);
        this.btnFlagZ.setEnabled(state);
        this.btnFlagS.setEnabled(state);
        this.btnFlagV.setEnabled(state);

        int flags = 0;
        if (state) {
            flags = this.fldRegFLAGS.getValue();
        }
        this.btnFlagC.setSelected((flags & 0x80) != 0);
        this.btnFlagZ.setSelected((flags & 0x40) != 0);
        this.btnFlagS.setSelected((flags & 0x20) != 0);
        this.btnFlagV.setSelected((flags & 0x10) != 0);
    }


    private void updGPRs(boolean state) {
        if (state) {
            enableRegEdit(
                    this.regFlds,
                    16,
                    this.regFlds.length - 16,
                    this.upperGPRsVisible ? 0x80 : 0x10);
        } else {
            disableRegEdit(
                    this.regFlds,
                    16,
                    this.regFlds.length - 16,
                    this.upperGPRsVisible ? 0x80 : 0x10);
        }
    }


    private void updReassFld() {
        updReassFld(this.z8Pause);
    }


    private void updReassFld(boolean state) {
        String text = "";
        if (state) {

            // Anzahl der sichtbaren Zeilen ermitteln
            int nRows = 0;
            int hAvail = this.fldReass.getHeight();
            Border border = this.fldReass.getBorder();
            if (border != null) {
                if (border != null) {
                    Insets insets = border.getBorderInsets(this.fldReass);
                    if (insets != null) {
                        hAvail -= insets.top;
                        hAvail -= insets.bottom;
                    }
                }
            }
            Insets margin = this.fldReass.getMargin();
            if (margin != null) {
                hAvail -= margin.top;
                hAvail -= margin.bottom;
            }
            if (hAvail > 0) {
                int hRow = this.fldReass.getRowHeight();
                if (hRow > 0) {
                    nRows = hAvail / hRow;
                }
            }

            // Reassembler-Text erzeugen
            int addr = this.fldPC.getValue();
            StringBuilder buf = new StringBuilder(256);
            for (int i = 0; i < nRows; i++) {
                if (i > 0) {
                    buf.append('\n');
                }
                this.reassembler.reassemble(buf, addr);
                addr = this.reassembler.getNextAddr() & 0xFFFF;
            }
            text = buf.toString();
        }
        this.fldReass.setText(text);
    }


    private void updStackFlds(boolean state) {
        state &= this.z8Pause;
        if (state) {
            StringBuilder buf = new StringBuilder(128);
            if (this.z8.isInternalStackEnabled()) {
                this.fldStackMode.setText("intern"); // TODO: i18n
                int sp = this.grp15Flds[SPL - 0xF0].getOrgValue();
                for (int i = 0; i < 16; i++) {
                    if (i > 0) {
                        buf.append((char) '\u0020');
                    }
                    int v = this.z8.viewRegValue(sp++);
                    if (v >= 0) {
                        buf.append(String.format("%02X", v));
                    } else {
                        buf.append("??");
                    }
                }
            } else {
                this.fldStackMode.setText("extern"); // TODO: i18n
                int sp = (this.grp15Flds[SPH - 0xF0].getOrgValue() << 8)
                        | this.grp15Flds[SPL - 0xF0].getOrgValue();
                for (int i = 0; i < 12; i++) {
                    if (i > 0) {
                        buf.append('\u0020');
                    }
                    buf.append(String.format(
                            "%02X",
                            this.memory.getMemByte(sp++, true)));
                }
            }
            this.fldStackValues.setText(buf.toString());
        } else {
            this.fldStackMode.setText("");
            this.fldStackValues.setText("");
        }
    }


    private void updWorkingRegGroupMarking(boolean state) {
        state &= this.z8Pause;
        HexFld[] flds = this.markedRegFlds;
        int idx = this.markedRegIdx;
        if ((flds != null) && (idx >= 0)) {
            if (idx < flds.length) {
                updWorkingRegGroupMarking(flds, idx, false);
            }
        }
        if (state) {
            idx = this.fldRegRP.getValue() & 0xF0;
            if (idx == 0xF0) {
                updWorkingRegGroupMarking(this.grp15Flds, 0, true);
            } else if (idx == 0) {
                updWorkingRegGroupMarking(this.regFlds, 0, true);
            } else {
                if (this.btnUpperGPRs.isSelected()) {
                    idx -= 0x70;
                }
                updWorkingRegGroupMarking(this.regFlds, idx, true);
            }
        }
    }


    private void updWorkingRegGroupMarking(
            HexFld[] flds,
            int idx,
            boolean state) {
        if (idx >= 0) {
            if (state) {
                this.markedRegFlds = flds;
                this.markedRegIdx = idx;
            } else {
                this.markedRegFlds = null;
                this.markedRegIdx = -1;
            }
            for (int i = 0; i < 16; i++) {
                if (idx < flds.length) {
                    HexFld fld = flds[idx++];
                    if (fld != null) {
                        fld.setMarked(state);
                    }
                }
            }
        }
    }


    private void updStatusText(Z8.RunMode runMode) {
        String statusText = debugFrmResourceBundle.getString("runMode.ready");
        if (runMode != null) {
            switch (runMode) {
                case RUNNING:
                    statusText = debugFrmResourceBundle.getString("runMode.running");
                    break;

                case INST_HALT:
                    statusText = debugFrmResourceBundle.getString("runMode.instHalt");
                    break;

                case INST_STOP:
                    statusText = debugFrmResourceBundle.getString("runMode.instStop");
                    break;

                case DEBUG_STOP:
                    statusText = debugFrmResourceBundle.getString("runMode.debugStop");
                    break;
            }
        }
        this.labelStatus.setText(statusText);
    }


    private void z8StatusChangedInternal() {
        this.z8Pause = this.z8.isPause();
        updFields(this.z8Pause);
    }
}
