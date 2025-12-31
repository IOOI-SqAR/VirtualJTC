/*
 * (c) 2007-2021 Jens Mueller
 * (c) 2017-2024 Lars Sonchocky-Helldorf
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Fenster fuer Bildschirmtastatur
 */

package org.sqar.virtualjtc.jtcemu.keyboard;

import org.sqar.virtualjtc.jtcemu.Main;
import org.sqar.virtualjtc.jtcemu.base.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.Locale;
import java.util.ResourceBundle;


public class KeyboardFrm extends BaseFrm implements ActionListener {
    private static final Locale locale = Locale.getDefault();
    private static final ResourceBundle keyboardFrmResourceBundle = ResourceBundle.getBundle("KeyboardFrm", locale);

    private static final String PROP_HOLD_SHIFT_KEYS = "keyboard.hold_shift_keys";

    private static KeyboardFrm instance = null;
    private static Point lastWinPos = null;

    private JTCSys jtcSys;
    private AbstractKeyboardFld keyboardFld;
    private boolean notified;
    private boolean holdShiftKeys;
    private JMenuItem mnuClose;
    private JMenuItem mnuHelpContent;
    private JCheckBoxMenuItem mnuHoldShiftKeys;


    public static void close() {
        if (instance != null)
            instance.doClose();
    }


    public boolean getHoldShiftKeys() {
        return this.holdShiftKeys;
    }


    public static void open(JTCSys jtcSys) {
        if (instance != null) {
            instance.setVisible(true);
            instance.setState(NORMAL);
            instance.toFront();
        } else {
            AbstractKeyboardFld kbFld = createKeyboardFld(jtcSys);
            if (kbFld != null) {
                instance = new KeyboardFrm(jtcSys, kbFld);
                instance.setVisible(true);
            }
        }
    }


    public static void reset() {
        if (instance != null) {
            if (instance.jtcSys.getOSType() == instance.keyboardFld.getOSType()) {
                instance.keyboardFld.reset();
            } else {
                instance.doClose();
            }
        }
    }


    public static void updKeyFields() {
        final KeyboardFrm keyboardFrm = instance;
        if (keyboardFrm != null) {
            EventQueue.invokeLater(
                    new Runnable() {
                        @Override
                        public void run() {
                            keyboardFrm.updKeyFieldsInternal();
                        }
                    });
        }
    }


    /* --- ActionListener --- */

    @Override
    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();
        if (src == this.mnuClose) {
            doClose();
        } else if ((src == this.mnuHoldShiftKeys)
                && (this.mnuHoldShiftKeys != null)) {
            this.holdShiftKeys = this.mnuHoldShiftKeys.isSelected();
            if (!this.holdShiftKeys) {
                this.keyboardFld.reset();
            }
            AppContext.setProperty(
                    PROP_HOLD_SHIFT_KEYS,
                    Boolean.toString(this.holdShiftKeys));
        } else if (src == this.mnuHelpContent) {
            HelpFrm.open("/help/common/keyboard.htm"); // TODO: i18n
        }
    }


    /* --- ueberschriebene Methoden --- */

    @Override
    public void addNotify() {
        super.addNotify();
        if (!this.notified) {
            this.notified = true;
            addKeyListener(Main.getTopFrm());
            this.mnuClose.addActionListener(this);
            if (this.mnuHoldShiftKeys != null) {
                this.mnuHoldShiftKeys.addActionListener(this);
            }
        }
    }


    @Override
    protected boolean doClose() {
        boolean rv = super.doClose();
        if (rv) {
            lastWinPos = instance.getLocation();
            instance = null;
        }
        return rv;
    }


    @Override
    public String getPropPrefix() {
        return "keyboard.";
    }


    @Override
    public void lafChanged() {
        pack();
    }


    @Override
    public void removeNotify() {
        if (this.notified) {
            this.notified = false;
            removeKeyListener(Main.getTopFrm());
            this.mnuClose.removeActionListener(this);
            if (this.mnuHoldShiftKeys != null) {
                this.mnuHoldShiftKeys.removeActionListener(this);
            }
        }
        super.removeNotify();
    }


    /* --- private Methoden --- */

    private static AbstractKeyboardFld createKeyboardFld(JTCSys jtcSys) {
        AbstractKeyboardFld fld = null;
        switch (jtcSys.getOSType()) {
            case OS2K:
                fld = new OS2kKeyboardFld(jtcSys, false);
                break;
            case ES1988:
                fld = new OS2kKeyboardFld(jtcSys, true);
                break;
            case ES23:
                fld = new ES23KeyboardFld(jtcSys);
                break;
            case ES40:
                fld = new ES40KeyboardFld(jtcSys);
                break;
        }
        if (fld != null) {
            fld.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        }
        return fld;
    }


    private void updKeyFieldsInternal() {
        this.keyboardFld.updKeyFields();
    }


    /* --- Konstruktor --- */

    private KeyboardFrm(JTCSys jtcSys, AbstractKeyboardFld keyboardFld) {
        setTitle(keyboardFrmResourceBundle.getString("window.title") + " Tastatur"); // TODO: i18n

        this.jtcSys = jtcSys;
        this.keyboardFld = keyboardFld;
        this.notified = false;
        this.holdShiftKeys = AppContext.getBooleanProperty(
                PROP_HOLD_SHIFT_KEYS,
                true);


        // Menu
        JMenuBar mnuBar = new JMenuBar();
        setJMenuBar(mnuBar);


        // Menu Datei
        JMenu mnuFile = new JMenu(keyboardFrmResourceBundle.getString("menu.file"));
        mnuFile.setMnemonic(KeyEvent.VK_D);
        mnuBar.add(mnuFile);

        this.mnuClose = new JMenuItem(keyboardFrmResourceBundle.getString("menuItem.close"));
        mnuFile.add(this.mnuClose);


        // Menu Einstellungen
        String holdShiftKeysText = keyboardFld.getHoldShiftKeysText();
        if (holdShiftKeysText != null) {
            JMenu mnuSettings = new JMenu(keyboardFrmResourceBundle.getString("menu.settings"));
            mnuSettings.setMnemonic(KeyEvent.VK_E);
            mnuBar.add(mnuSettings);

            this.mnuHoldShiftKeys = new JCheckBoxMenuItem(
                    holdShiftKeysText,
                    this.holdShiftKeys);
            mnuSettings.add(this.mnuHoldShiftKeys);
        } else {
            this.mnuHoldShiftKeys = null;
        }


        // Menu Hilfe
        JMenu mnuHelp = new JMenu("Hilfe"); // TODO: i18n
        mnuHelp.setMnemonic(KeyEvent.VK_H);
        mnuBar.add(mnuHelp);

        this.mnuHelpContent = new JMenuItem("Hilfe zur Tastatur..."); // TODO: i18n
        this.mnuHelpContent.addActionListener(this);
        mnuHelp.add(this.mnuHelpContent);


        // Fensterinhalt
        setLayout(new BorderLayout());
        add(this.keyboardFld, BorderLayout.CENTER);
        this.keyboardFld.setKeyboardFrm(this);

        // Fenstergroesse und -position
        pack();
        setFocusable(true);
        setResizable(false);
        if (lastWinPos != null) {
            setLocation(lastWinPos);
        } else {
            if (!GUIUtil.applyWindowSettings(this)) {
                setLocationByPlatform(true);
            }
        }
    }
}
