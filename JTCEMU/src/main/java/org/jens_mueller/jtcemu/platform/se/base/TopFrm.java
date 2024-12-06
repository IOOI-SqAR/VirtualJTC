/*
 * (c) 2007-2021 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Hauptfenster
 */

package org.jens_mueller.jtcemu.platform.se.base;

import org.jens_mueller.jtcemu.base.*;
import org.jens_mueller.jtcemu.platform.se.Main;
import org.jens_mueller.jtcemu.platform.se.audio.AudioFrm;
import org.jens_mueller.jtcemu.platform.se.audio.AudioIO;
import org.jens_mueller.jtcemu.platform.se.keyboard.KeyboardFrm;
import org.jens_mueller.jtcemu.platform.se.settings.SettingsFrm;
import org.jens_mueller.jtcemu.platform.se.tools.ReassFrm;
import org.jens_mueller.jtcemu.platform.se.tools.TextEditFrm;
import org.jens_mueller.jtcemu.platform.se.tools.debugger.DebugFrm;
import org.jens_mueller.jtcemu.platform.se.tools.hexedit.HexEditFrm;
import org.jens_mueller.jtcemu.platform.se.tools.hexedit.MemEditFrm;
import org.jens_mueller.z8.Z8;
import org.jens_mueller.z8.Z8Listener;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.dnd.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;


public class TopFrm extends BaseFrm
        implements
        ActionListener,
        DropTargetListener,
        ErrorViewer,
        FlavorListener,
        KeyListener,
        PasteObserver,
        Z8Listener {
    public static final int DEFAULT_SCREEN_MARGIN = 20;
    public static final int DEFAULT_SCREEN_REFRESH_MS = 50;
    public static final int DEFAULT_SCREEN_SCALE = 3;

    public static final String PROP_SCREEN_MARGIN = "screen.margin";
    public static final String PROP_SCREEN_REFRESH_MS = "screen.refresh.ms";
    public static final String PROP_SCREEN_SCALE = "screen.scale";

    private static final String ACTION_ABOUT = "about";
    private static final String ACTION_AUDIO = "audio";
    private static final String ACTION_COPY = "copy";
    private static final String ACTION_DEBUG = "debug";
    private static final String ACTION_HELP = "help";
    private static final String ACTION_HEXEDIT = "hexedit";
    private static final String ACTION_KEYBOARD = "keyboard";
    private static final String ACTION_LOAD = "load";
    private static final String ACTION_LOAD_OPT = "load_opt";
    private static final String ACTION_LICENSE = "license";
    private static final String ACTION_MEMEDIT = "memedit";
    private static final String ACTION_PASTE = "paste";
    private static final String ACTION_PASTE_CANCEL = "paste.cancel";
    private static final String ACTION_PAUSE = "pause";
    private static final String ACTION_POWER_ON = "power_on";
    private static final String ACTION_QUIT = "quit";
    private static final String ACTION_REASS = "reass";
    private static final String ACTION_RESET = "reset";
    private static final String ACTION_SAVE = "save";
    private static final String ACTION_SCALE_PREFIX = "scale.";
    private static final String ACTION_SCR_IMG_COPY = "screen.img.copy";
    private static final String ACTION_SCR_IMG_SAVE_AS = "screen.img.save";
    private static final String ACTION_SCR_TEXT_COPY = "screen.text.copy";
    private static final String ACTION_SETTINGS = "settings";
    private static final String ACTION_SPEED = "speed";
    private static final String ACTION_TEXTEDIT = "textedit";
    private static final String ACTION_THANKS = "thanks";
    private static final String FILE_GROUP_IMAGE = "image";

    private static final String DEFAULT_STATUS_TEXT = "Emulator l\u00E4uft...";

    private static final String TEXT_MAX_SPEED = "Maximale Geschwindigkeit";
    private static final String TEXT_NORM_SPEED = "Normale Geschwindigkeit";
    private static final String TEXT_PAUSE = "Pause";
    private static final String TEXT_GO_ON = "Fortsetzen";

    private static final int STATUS_REFRESH_MILLIS = 750;
    private static final int STATUS_SHOW_MSG_MILLIS = 5000;

    private JTCSys jtcSys;
    private Z8 z8;
    private Map<Integer, JRadioButtonMenuItem> scale2MenuItems;
    private Map<Dimension, Integer> jtcScreenSize2Scale;
    private Dimension jtcScreenSize;
    private Thread emuThread;
    private Clipboard clipboard;
    private boolean screenOutputEnabled;
    private ScreenFld screenFld;
    private javax.swing.Timer screenRefreshTimer;
    private javax.swing.Timer statusRefreshTimer;
    private int mnuShortcutKeyMask;
    private JMenuItem mnuCopy;
    private JMenuItem mnuPaste;
    private JMenuItem mnuPasteCancel;
    private JMenuItem mnuPause;
    private JMenuItem mnuSpeed;
    private JLabel fldStatusText;


    public TopFrm(JTCSys jtcSys) {
        setTitle("JU+TE-Computer");
        this.jtcSys = jtcSys;
        this.z8 = jtcSys.getZ8();
        this.clipboard = getToolkit().getSystemClipboard();
        this.screenOutputEnabled = false;
        this.scale2MenuItems = new HashMap<Integer, JRadioButtonMenuItem>();
        this.jtcScreenSize2Scale = new HashMap<Dimension, Integer>();
        this.jtcScreenSize2Scale.put(
                new Dimension(64, 64),
                DEFAULT_SCREEN_SCALE);
        this.jtcScreenSize2Scale.put(new Dimension(128, 128), 2);
        this.jtcScreenSize2Scale.put(new Dimension(320, 192), 1);


        /*
         * Die Shortcut-Taste soll nicht die Control-Taste sein.
         * Aus diesem Grund wird geprueft,
         * ob die uebliche Shortcut-Taste die Control-Taste ist.
         * Wenn nein, wird diese verwendet (ist beim Mac so),
         * anderenfalls die ALT-Taste.
         */
        this.mnuShortcutKeyMask = 0;
        Toolkit tk = getToolkit();
        try {
            // Seit Java 10 gibt es die Methode Toolkit.getMenuShortcutKeyMaskEx()
            Object v = tk.getClass().getMethod("getMenuShortcutKeyMaskEx")
                    .invoke(tk);
            if (v != null) {
                if (v instanceof Number) {
                    this.mnuShortcutKeyMask = ((Number) v).intValue();
                    if (this.mnuShortcutKeyMask == InputEvent.CTRL_DOWN_MASK) {
                        this.mnuShortcutKeyMask = InputEvent.ALT_DOWN_MASK;
                    }
                }
            }
        } catch (Exception ex) {
        }
        if (this.mnuShortcutKeyMask == 0) {
            /*
             * Vor Java 10 gibt es die Methode Toolkit.getMenuShortcutKeyMask(),
             * die noch eine alte Control-Maske liefert
             * (Event.CTRL_MASK bzw. InputEvent.CTRL_MASK).
             * Da aber die Felder der alten Codes mit Java 9 deprecated sind,
             * werden diese hier per Reflection ausgelesen und es wird auch
             * auf InputEvent.CTRL_DOWN_MASK geprueft.
             */
            int ctrlMask = InputEvent.CTRL_DOWN_MASK;
            try {
                ctrlMask |= getClass().forName("java.awt.Event")
                        .getDeclaredField("CTRL_MASK")
                        .getInt(null);
            } catch (Exception ex) {
            }
            try {
                ctrlMask |= InputEvent.class.getDeclaredField("CTRL_MASK")
                        .getInt(null);
            } catch (Exception ex) {
            }
            try {
                Object v = tk.getClass().getMethod("getMenuShortcutKeyMask")
                        .invoke(tk);
                if (v != null) {
                    if (v instanceof Number) {
                        this.mnuShortcutKeyMask = ((Number) v).intValue();
                        if ((this.mnuShortcutKeyMask & ctrlMask) != 0) {
                            this.mnuShortcutKeyMask = InputEvent.ALT_DOWN_MASK;
                        }
                    }
                }
            } catch (Exception ex) {
            }
        }
        if (this.mnuShortcutKeyMask == 0) {
            this.mnuShortcutKeyMask = InputEvent.ALT_DOWN_MASK;
        }

        // Menu
        JMenuBar mnuBar = new JMenuBar();
        setJMenuBar(mnuBar);


        // Menu Datei
        JMenu mnuFile = new JMenu("Datei");
        mnuFile.setMnemonic(KeyEvent.VK_D);
        mnuBar.add(mnuFile);
        mnuFile.add(
                createJMenuItem("Laden...", KeyEvent.VK_L, ACTION_LOAD));
        mnuFile.add(
                createJMenuItem("Laden mit Optionen...", ACTION_LOAD_OPT));
        mnuFile.add(
                createJMenuItem("Speichern...", KeyEvent.VK_S, ACTION_SAVE));
        mnuFile.addSeparator();
        mnuFile.add(
                createJMenuItem("Texteditor...", KeyEvent.VK_T, ACTION_TEXTEDIT));
        mnuFile.addSeparator();
        mnuFile.add(createQuitMenuItem());


        // Menu Bearbeiten
        JMenu mnuEdit = new JMenu("Bearbeiten");
        mnuEdit.setMnemonic(KeyEvent.VK_B);
        mnuBar.add(mnuEdit);

        this.mnuCopy = createJMenuItem(
                "Kopieren",
                KeyEvent.VK_C,
                ACTION_COPY);
        this.mnuCopy.setEnabled(false);
        mnuEdit.add(this.mnuCopy);

        this.mnuPaste = createJMenuItem(
                "Einf\u00FCgen",
                KeyEvent.VK_V,
                ACTION_PASTE);
        mnuEdit.add(this.mnuPaste);
        mnuEdit.addSeparator();

        this.mnuPasteCancel = createJMenuItem(
                "Einf\u00FCgen abbrechen",
                ACTION_PASTE_CANCEL);
        this.mnuPasteCancel.setEnabled(false);
        mnuEdit.add(this.mnuPasteCancel);


        // Menu Extra
        JMenu mnuExtra = new JMenu("Extra");
        mnuExtra.setMnemonic(KeyEvent.VK_E);
        mnuBar.add(mnuExtra);

        JMenu mnuScreenScale = new JMenu("Ansicht");
        mnuExtra.add(mnuScreenScale);

        int screenScale = AppContext.getIntProperty(
                PROP_SCREEN_SCALE,
                DEFAULT_SCREEN_SCALE);
        ButtonGroup grpScale = new ButtonGroup();
        for (int v = 1; v <= 9; v++) {
            String text = Integer.toString(v);
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(
                    text + "00 %",
                    v == screenScale);
            item.setActionCommand(ACTION_SCALE_PREFIX + text);
            item.setAccelerator(KeyStroke.getKeyStroke(
                    (char) ('0' + v),
                    this.mnuShortcutKeyMask));
            item.addActionListener(this);
            grpScale.add(item);
            mnuScreenScale.add(item);
            this.scale2MenuItems.put(v, item);
        }

        JMenu mnuScreenImg = new JMenu("Bildschirmausgabe");
        mnuExtra.add(mnuScreenImg);
        mnuExtra.addSeparator();

        mnuScreenImg.add(createJMenuItem(
                "als Text kopieren",
                ACTION_SCR_TEXT_COPY));
        mnuScreenImg.add(createJMenuItem(
                "als Bild kopieren",
                ACTION_SCR_IMG_COPY));
        mnuScreenImg.add(createJMenuItem(
                "als Bild speichern unter...",
                ACTION_SCR_IMG_SAVE_AS));

        mnuExtra.add(createJMenuItem(
                "Debugger...",
                KeyEvent.VK_D,
                true,
                ACTION_DEBUG));
        mnuExtra.add(createJMenuItem(
                "Reassembler...",
                KeyEvent.VK_R,
                true,
                ACTION_REASS));
        mnuExtra.add(createJMenuItem(
                "Speichereditor...",
                KeyEvent.VK_M,
                true,
                ACTION_MEMEDIT));
        mnuExtra.add(createJMenuItem(
                "Hex-Editor...",
                KeyEvent.VK_H,
                true,
                ACTION_HEXEDIT));
        mnuExtra.addSeparator();
        mnuExtra.add(createJMenuItem(
                "Audio/Kassette...",
                KeyEvent.VK_A,
                ACTION_AUDIO));
        mnuExtra.add(createJMenuItem(
                "Tastatur...",
                KeyEvent.VK_K,
                ACTION_KEYBOARD));
        mnuExtra.add(createJMenuItem(
                "Einstellungen...",
                ACTION_SETTINGS));
        mnuExtra.addSeparator();

        this.mnuSpeed = createJMenuItem(
                TEXT_MAX_SPEED,
                KeyEvent.VK_G,
                ACTION_SPEED);
        mnuExtra.add(this.mnuSpeed);

        this.mnuPause = createJMenuItem(
                TEXT_PAUSE,
                KeyEvent.VK_P,
                ACTION_PAUSE);
        mnuExtra.add(this.mnuPause);
        mnuExtra.addSeparator();
        mnuExtra.add(createJMenuItem(
                "Zur\u00FCcksetzen (RESET)",
                KeyEvent.VK_R,
                ACTION_RESET));
        mnuExtra.add(createJMenuItem(
                "Einschalten (Speicher l\u00F6schen)",
                KeyEvent.VK_I,
                ACTION_POWER_ON));


        // Menu Hilfe
        JMenu mnuHelp = new JMenu("Hilfe");
        mnuHelp.setMnemonic(KeyEvent.VK_H);
        mnuHelp.add(createJMenuItem(
                AppContext.getAppName() + "-Hilfe...",
                ACTION_HELP));
        mnuHelp.addSeparator();
        mnuHelp.add(createAboutMenuItem());
        mnuHelp.add(
                createJMenuItem("Lizenzbestimmungen...", ACTION_LICENSE));
        mnuHelp.add(createJMenuItem("Danksagung...", ACTION_THANKS));
        mnuBar.add(mnuHelp);


        // Fensterinhalt
        setLayout(new BorderLayout());


        // Werkzeugleiste
        JPanel panelToolBar = new JPanel(
                new FlowLayout(FlowLayout.LEFT, 0, 0));
        add(panelToolBar, BorderLayout.NORTH);

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.setBorderPainted(false);
        toolBar.setOrientation(JToolBar.HORIZONTAL);
        toolBar.setRollover(true);
        panelToolBar.add(toolBar);

        toolBar.add(GUIUtil.createImageButton(
                this,
                "/images/file/open.png",
                "Laden",
                ACTION_LOAD));

        toolBar.add(GUIUtil.createImageButton(
                this,
                "/images/file/save.png",
                "Speichern",
                ACTION_SAVE));
        toolBar.addSeparator();

        toolBar.add(GUIUtil.createImageButton(
                this,
                "/images/file/edit.png",
                "Texteditor",
                ACTION_TEXTEDIT));
        toolBar.add(GUIUtil.createImageButton(
                this,
                "/images/file/audio.png",
                "Audio/Kassette",
                ACTION_AUDIO));
        toolBar.add(GUIUtil.createImageButton(
                this,
                "/images/edit/settings.png",
                "Einstellungen",
                ACTION_SETTINGS));

        toolBar.addSeparator();

        toolBar.add(GUIUtil.createImageButton(
                this,
                "/images/file/reset.png",
                "Zur\u00FCcksetzen (RESET)",
                ACTION_RESET));


        // Bildschirmausgabe
        this.screenFld = new ScreenFld(this, this.jtcSys, screenScale);
        this.screenFld.addKeyListener(this);
        add(this.screenFld, BorderLayout.CENTER);

        this.jtcScreenSize = getJTCScreenSize();
        this.jtcScreenSize2Scale.put(this.jtcScreenSize, screenScale);


        // Statuszeile
        JPanel panelStatusBar = new JPanel(
                new FlowLayout(FlowLayout.LEFT, 5, 5));
        add(panelStatusBar, BorderLayout.SOUTH);

        this.fldStatusText = new JLabel(DEFAULT_STATUS_TEXT);
        panelStatusBar.add(this.fldStatusText);


        // Timer zum zyklischen Aktualisieren der Bildschirmausgabe
        this.screenRefreshTimer = new javax.swing.Timer(
                getSettingsScreenRefreshMillis(),
                this);
        this.screenRefreshTimer.start();


        // Timer zum Aktualisieren der Statuszeile
        this.statusRefreshTimer = new javax.swing.Timer(
                STATUS_REFRESH_MILLIS,
                this);


        // Fenstergroesse und -position
        updScreenSize(false);
        setResizable(true);
        if (!GUIUtil.applyWindowSettings(this)) {
            pack();
            setLocationByPlatform(true);
        }


        // Clipboard ueberwachen
        if (this.clipboard != null) {
            this.clipboard.addFlavorListener(this);
            clipboardContentChanged();
        }


        // Drop-Ziel
        (new DropTarget(this.screenFld, this)).setActive(true);


        // Emulation starten
        this.z8.setStatusListener(this);
        this.emuThread = new Thread(this.z8, "Z8 emulation");
        this.emuThread.start();
        this.jtcSys.setResetListener(this);
    }


    public JMenuItem createAboutMenuItem() {
        return createJMenuItem(
                "\u00DCber " + AppContext.getAppName() + "...",
                ACTION_ABOUT);
    }


    public JMenuItem createQuitMenuItem() {
        return createJMenuItem("Beenden", ACTION_QUIT);
    }


    public void doPowerOn() {
        boolean state = true;
        if (AppContext.getBooleanProperty(
                JTCUtil.PROP_CONFIRM_POWER_ON,
                true)) {
            if (JOptionPane.showConfirmDialog(
                    this,
                    "M\u00F6chten Sie das Aus- und wieder Einschalten"
                            + " emulieren\n"
                            + "und den Emulator initialisieren?\n"
                            + "Dabei wird der Arbeitsspeicher gel\u00F6scht.",
                    "Best\u00E4tigung",
                    JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
                state = false;
            }
        }
        if (state) {
            resetEmu(true);
        }
    }


    public void doReset() {
        boolean state = true;
        if (AppContext.getBooleanProperty(
                JTCUtil.PROP_CONFIRM_RESET,
                true)) {
            String msg = "M\u00F6chten Sie jetzt ein RESET ausl\u00F6sen\n"
                    + "und den Emulator zur\u00FCcksetzen?";
            if ((this.jtcSys.getOSType() == JTCSys.OSType.OS2K)
                    || (this.jtcSys.getOSType() == JTCSys.OSType.ES1988)) {
                msg += "\nWenn der A-Cursor zu sehen ist,\n"
                        + "sollten Sie kein RESET ausl\u00F6sen!";
            }
            if (JOptionPane.showConfirmDialog(
                    this,
                    msg,
                    "Best\u00E4tigung",
                    JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
                state = false;
            }
        }
        if (state) {
            resetEmu(false);
        }
    }


    public int getScreenRefreshMillis() {
        return this.screenRefreshTimer.getDelay();
    }


    public void screenConfigChanged() {
        if (updScreenSize(true)) {
            pack();
        }
        JRadioButtonMenuItem item = this.scale2MenuItems.get(
                this.screenFld.getScale());
        if (item != null) {
            item.setSelected(true);
        }
    }


    public boolean setMaxSpeed(Component owner, boolean state) {
        boolean rv = false;
        if (state) {
            if (this.z8.getCyclesPerSecond() > 0) {
                if (AudioIO.isCPUSynchronLineOpen()) {
                    JOptionPane.showMessageDialog(
                            owner,
                            "Solange ein Audiokanal ge\u00F6ffnet ist,\n"
                                    + "kann nicht auf maximale Geschwindigkeit"
                                    + " geschaltet werden,\n"
                                    + "da der Audiokanal bremst.",
                            "Hinweis",
                            JOptionPane.INFORMATION_MESSAGE);
                } else {
                    this.z8.setCyclesPerSecond(0);
                    rv = true;
                }
            }
        } else {
            this.z8.setCyclesPerSecond(JTCSys.DEFAULT_Z8_CYCLES_PER_SECOND);
            rv = true;
        }
        return rv;
    }


    public void setScreenTextSelected(boolean state) {
        this.mnuCopy.setEnabled(state);
    }


    public void showStatusText(String text) {
        if (text != null) {
            if (!text.isEmpty()) {
                if (this.statusRefreshTimer.isRunning()) {
                    this.statusRefreshTimer.stop();
                }
                this.fldStatusText.setText(text);
                this.statusRefreshTimer.setDelay(STATUS_SHOW_MSG_MILLIS);
                this.statusRefreshTimer.setInitialDelay(STATUS_SHOW_MSG_MILLIS);
                this.statusRefreshTimer.start();
            }
        }
    }


    /* --- ActionListener --- */

    @Override
    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();
        if (src == this.screenRefreshTimer) {
            boolean screenOutputEnabled = this.jtcSys.isScreenOutputEnabled();
            if ((screenOutputEnabled != this.screenOutputEnabled)
                    || this.screenFld.isDirty()) {
                this.screenFld.repaint();
                this.screenOutputEnabled = screenOutputEnabled;
            }
        } else if (src == this.statusRefreshTimer) {
            updStatusText();
        } else {
            String actionCmd = e.getActionCommand();
            if (actionCmd != null) {
                if (actionCmd.equals(ACTION_QUIT)) {
                    doQuit();
                } else {
                    GUIUtil.setWaitCursor(this, true);
                    try {
                        if (actionCmd.equals(ACTION_LOAD)) {
                            doLoad(false);
                        } else if (actionCmd.equals(ACTION_LOAD_OPT)) {
                            doLoad(true);
                        } else if (actionCmd.equals(ACTION_SAVE)) {
                            (new SaveDlg(this, this.jtcSys)).setVisible(true);
                        } else if (actionCmd.equals(ACTION_TEXTEDIT)) {
                            TextEditFrm.open(this.jtcSys);
                        } else if (actionCmd.equals(ACTION_COPY)) {
                            copyText(this.screenFld.getSelectedText());
                        } else if (actionCmd.equals(ACTION_PASTE)) {
                            doPaste();
                        } else if (actionCmd.equals(ACTION_PASTE_CANCEL)) {
                            doPasteCancel();
                        } else if (actionCmd.equals(ACTION_SCR_TEXT_COPY)) {
                            copyText(this.jtcSys.getScreenText());
                        } else if (actionCmd.equals(ACTION_SCR_IMG_COPY)) {
                            doScreenImageCopy();
                        } else if (actionCmd.equals(ACTION_SCR_IMG_SAVE_AS)) {
                            doScreenImageSaveAs();
                        } else if (actionCmd.equals(ACTION_DEBUG)) {
                            DebugFrm.open(this.z8);
                        } else if (actionCmd.equals(ACTION_REASS)) {
                            ReassFrm.open(this.jtcSys);
                        } else if (actionCmd.equals(ACTION_MEMEDIT)) {
                            MemEditFrm.open(this.jtcSys);
                        } else if (actionCmd.equals(ACTION_HEXEDIT)) {
                            HexEditFrm.open();
                        } else if (actionCmd.equals(ACTION_AUDIO)) {
                            AudioFrm.open(this.jtcSys);
                        } else if (actionCmd.equals(ACTION_KEYBOARD)) {
                            KeyboardFrm.open(this.jtcSys);
                        } else if (actionCmd.equals(ACTION_SETTINGS)) {
                            SettingsFrm.open(this, this.jtcSys);
                        } else if (actionCmd.equals(ACTION_SPEED)) {
                            doSpeed();
                        } else if (actionCmd.equals(ACTION_PAUSE)) {
                            doPause();
                        } else if (actionCmd.equals(ACTION_POWER_ON)) {
                            doPowerOn();
                        } else if (actionCmd.equals(ACTION_RESET)) {
                            doReset();
                        } else if (actionCmd.equals(ACTION_HELP)) {
                            HelpFrm.open(null);
                        } else if (actionCmd.equals(ACTION_ABOUT)) {
                            doAbout();
                        } else if (actionCmd.equals(ACTION_LICENSE)) {
                            HelpFrm.open("/help/common/license.htm");
                        } else if (actionCmd.equals(ACTION_THANKS)) {
                            HelpFrm.open("/help/common/thanks.htm");
                        } else if (actionCmd.startsWith(ACTION_SCALE_PREFIX)) {
                            int prefixLen = ACTION_SCALE_PREFIX.length();
                            if (actionCmd.length() > prefixLen) {
                                try {
                                    int scale = Integer.parseInt(
                                            actionCmd.substring(prefixLen));
                                    this.screenFld.setScale(scale);
                                    this.jtcScreenSize2Scale.put(this.jtcScreenSize, scale);
                                    pack();
                                } catch (NumberFormatException ex) {
                                }
                            }
                        }
                    } catch (Exception ex) {
                        Main.showError(this, ex);
                    }
                    GUIUtil.setWaitCursor(this, false);
                }
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
        final File file = GUIUtil.fileDrop(this, e);
        if (file != null) {
            // nicht auf Benutzereingaben warten
            EventQueue.invokeLater(
                    new Runnable() {
                        @Override
                        public void run() {
                            loadFile(file, false);
                        }
                    });
        }
    }


    @Override
    public void dropActionChanged(DropTargetDragEvent e) {
        if (!GUIUtil.isFileDrop(e))
            e.rejectDrag();
    }


    /* --- ErrorViewer --- */

    public void showError(final String msg) {
        final Component owner = this;
        EventQueue.invokeLater(
                new Runnable() {
                    @Override
                    public void run() {
                        Main.showError(owner, msg);
                    }
                });
    }


    /* --- FlavorListener --- */

    @Override
    public void flavorsChanged(FlavorEvent e) {
        if (e.getSource() == this.clipboard)
            clipboardContentChanged();
    }


    /* --- KeyListener --- */

    @Override
    public void keyPressed(KeyEvent e) {
        if (!e.isAltDown()) {
            JTCSys.Key key = null;
            switch (e.getKeyCode()) {
                case KeyEvent.VK_LEFT:
                    key = JTCSys.Key.LEFT;
                    break;
                case KeyEvent.VK_RIGHT:
                    key = JTCSys.Key.RIGHT;
                    break;
                case KeyEvent.VK_UP:
                    key = JTCSys.Key.UP;
                    break;
                case KeyEvent.VK_DOWN:
                    key = JTCSys.Key.DOWN;
                    break;
                case KeyEvent.VK_BACK_SPACE:
                    key = JTCSys.Key.BACK_SPACE;
                    break;
                case KeyEvent.VK_HOME:
                    key = JTCSys.Key.HOME;
                    break;
                case KeyEvent.VK_SPACE:
                    key = JTCSys.Key.SPACE;
                    break;
                case KeyEvent.VK_ENTER:
                    key = JTCSys.Key.ENTER;
                    break;
                case KeyEvent.VK_ESCAPE:
                    key = JTCSys.Key.ESCAPE;
                    break;
                case KeyEvent.VK_INSERT:
                    key = JTCSys.Key.INSERT;
                    break;
                case KeyEvent.VK_DELETE:
                    key = JTCSys.Key.DELETE;
                    break;
                case KeyEvent.VK_CLEAR:
                    key = JTCSys.Key.CLEAR;
                    break;
                case KeyEvent.VK_F1:
                    key = JTCSys.Key.F1;
                    break;
                case KeyEvent.VK_F2:
                    key = JTCSys.Key.F2;
                    break;
                case KeyEvent.VK_F3:
                    key = JTCSys.Key.F3;
                    break;
                case KeyEvent.VK_F4:
                    key = JTCSys.Key.F4;
                    break;
                case KeyEvent.VK_F5:
                    key = JTCSys.Key.F5;
                    break;
                case KeyEvent.VK_F6:
                    key = JTCSys.Key.F6;
                    break;
                case KeyEvent.VK_F7:
                    key = JTCSys.Key.F7;
                    break;
                case KeyEvent.VK_F8:
                    key = JTCSys.Key.F8;
                    break;
            }
            if (key != null) {
                if (this.jtcSys.keyPressed(key, e.isShiftDown())) {
                    KeyboardFrm.updKeyFields();
                    e.consume();
                }
            }
        }
    }


    @Override
    public void keyReleased(KeyEvent e) {
        if (!e.isAltDown()) {
            this.jtcSys.keyReleased();
            KeyboardFrm.updKeyFields();
            e.consume();
        }
    }


    @Override
    public void keyTyped(KeyEvent e) {
        if (!e.isAltDown()) {
            this.jtcSys.keyTyped(e.getKeyChar());
            KeyboardFrm.updKeyFields();
            e.consume();
        }
    }


    /* --- PasteObserver --- */

    @Override
    public void pastingFinished() {
        EventQueue.invokeLater(
                new Runnable() {
                    @Override
                    public void run() {
                        setPasting(false);
                    }
                });
    }


    /* --- Z8Listener --- */

    @Override
    public void z8Update(Z8 z8, final Z8Listener.Reason reason) {
        if (z8 == this.z8) {
            if (z8.isPause()) {
                AudioFrm.closeCPUSynchronLine();
            }
            EventQueue.invokeLater(
                    new Runnable() {
                        @Override
                        public void run() {
                            z8UpdateInternal(reason);
                        }
                    });
        }
    }


    /* --- ueberschriebene Methoden  --- */

    @Override
    public String getPropPrefix() {
        return "emulator.";
    }


    @Override
    public void lafChanged() {
        pack();
    }


    @Override
    public void memorizeSettings() {
        super.memorizeSettings();
        AppContext.setProperty(
                PROP_SCREEN_SCALE,
                Integer.toString(this.screenFld.getScale()));
    }


    @Override
    public void settingsChanged() {
        this.screenRefreshTimer.setDelay(getSettingsScreenRefreshMillis());
        if (((this.jtcSys.getOSType() != JTCSys.OSType.OS2K)
                && (this.jtcSys.getOSType() != JTCSys.OSType.ES1988))) {
            KeyboardFrm.close();
        }
    }


    @Override
    public void windowClosing(WindowEvent e) {
        doQuit();
    }


    @Override
    public void windowOpened(WindowEvent e) {
        AudioFrm.checkOpen(this.jtcSys);
        this.screenFld.requestFocus();
    }


    /* --- Aktionen --- */

    private void doAbout() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JLabel labelName = new JLabel(AppContext.getAppName());
        labelName.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));
        panel.add(labelName);

        JLabel labelVersion = new JLabel(
                "Version " + AppContext.getAppVersion());
        labelVersion.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        panel.add(labelVersion);

        panel.add(Box.createRigidArea(new Dimension(0, 10)));

        JOptionPane.showMessageDialog(
                this,
                new Object[]{panel, AppContext.getAboutContent()},
                "\u00DCber...",
                JOptionPane.INFORMATION_MESSAGE);
    }


    private void doLoad(boolean withOptions) {
        String title = "Datei laden";
        if (withOptions) {
            title += " mit Optionen";
        }
        File file = FileDlg.showFileOpenDlg(
                this,
                title,
                "Laden...",
                AppContext.getLastDirFile(
                        FileInfo.FILE_GROUP_SOFTWARE),
                GUIUtil.binaryFileFilter,
                GUIUtil.hexFileFilter,
                GUIUtil.jtcFileFilter,
                GUIUtil.tapFileFilter);
        if (file != null) {
            loadFile(file, withOptions);
        }
    }


    private void doPaste() {
        if (this.clipboard != null) {
            try {
                if (this.clipboard.isDataFlavorAvailable(
                        DataFlavor.stringFlavor)) {
                    Object o = this.clipboard.getData(DataFlavor.stringFlavor);
                    if (o != null) {
                        this.jtcSys.startPastingText(o.toString(), this);
                        setPasting(true);
                    }
                }
            } catch (Exception ex) {
            }
        }
    }


    private void doPasteCancel() {
        this.jtcSys.cancelPastingText();
        setPasting(false);
    }


    private void doPause() {
        this.z8.setPause(!this.z8.isPause());
    }


    private void doQuit() {
        boolean state = true;
        if (AppContext.getBooleanProperty(
                JTCUtil.PROP_CONFIRM_QUIT,
                true)) {
            if (JOptionPane.showConfirmDialog(
                    this,
                    "M\u00F6chten Sie " + AppContext.getAppName() + " beenden?",
                    "Best\u00E4tigung",
                    JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
                state = false;
            }
        }
        if (state) {
            state = TextEditFrm.close();
        }
        if (state) {
            state = HexEditFrm.close();
        }
        if (state) {
            this.screenRefreshTimer.stop();
            this.z8.fireQuit();
            AudioFrm.quit();
            DebugFrm.close();
            HelpFrm.close();
            KeyboardFrm.close();
            MemEditFrm.close();
            ReassFrm.close();
            SettingsFrm.close();
            try {
                this.emuThread.join(500);
            } catch (InterruptedException ex) {
            }
            doClose();
            System.exit(0);
        }
    }


    private void doScreenImageCopy() {
        try {
            if (this.clipboard != null) {
                Image image = this.screenFld.createBufferedImage();
                if (image != null) {
                    TransferableImage tImg = new TransferableImage(image);
                    this.clipboard.setContents(tImg, tImg);
                }
            }
        } catch (IllegalStateException ex) {
        }
    }


    private void doScreenImageSaveAs() {
        try {
            String[] fmtNames = ImageIO.getWriterFormatNames();
            if (fmtNames != null) {
                if (fmtNames.length < 1) {
                    fmtNames = null;
                }
            }
            if (fmtNames == null) {
                throw new IOException(
                        "Das Speichern von Bilddateien wird auf\n"
                                + "dieser Plattform nicht unterst\u00FCtzt.");
            }
            BufferedImage image = this.screenFld.createBufferedImage();
            if (image != null) {
                File file = FileDlg.showFileSaveDlg(
                        this,
                        "Bilddatei speichern",
                        AppContext.getLastDirFile(FILE_GROUP_IMAGE),
                        new FileNameExtensionFilter(
                                "Unterst\u00FCtzte Bilddateien",
                                fmtNames));
                if (file != null) {
                    String s = file.getName();
                    if (s != null) {
                        s = s.toUpperCase();
                        String fmt = null;
                        for (int i = 0; i < fmtNames.length; i++) {
                            if (s.endsWith("." + fmtNames[i].toUpperCase())) {
                                fmt = fmtNames[i];
                                break;
                            }
                        }
                        if (fmt != null) {
                            OutputStream out = null;
                            try {
                                out = new FileOutputStream(file);
                                if (!ImageIO.write(image, fmt, file)) {
                                    fmt = null;
                                }
                                out.close();
                                AppContext.setLastFile(FILE_GROUP_IMAGE, file);
                            } catch (IOException ex) {
                                file.delete();
                                throw ex;
                            } finally {
                                JTCUtil.closeSilently(out);
                            }
                        }
                        if (fmt == null) {
                            throw new IOException(
                                    "Das durch die Dateiendung angegebene Format\n"
                                            + "wird nicht unterst\u00FCtzt.");
                        }
                    }
                }
            }
        } catch (Exception ex) {
            Main.showError(this, ex);
        }
    }


    private void doSpeed() {
        setMaxSpeed(this, this.z8.getCyclesPerSecond() > 0);
    }


    /* --- private Methoden --- */

    private void clipboardContentChanged() {
        boolean state = false;
        if (this.clipboard != null) {
            try {
                state = this.clipboard.isDataFlavorAvailable(
                        DataFlavor.stringFlavor);
            } catch (IllegalStateException ex) {
            }
        }
        this.mnuPaste.setEnabled(state);
    }


    private void copyText(String text) {
        if ((this.clipboard != null) && (text != null)) {
            try {
                StringSelection data = new StringSelection(text);
                this.clipboard.setContents(data, data);
            } catch (IllegalStateException ex) {
            }
        }
    }


    private JMenuItem createJMenuItem(String text, String actionCmd) {
        JMenuItem item = new JMenuItem(text);
        if (actionCmd != null) {
            item.setActionCommand(actionCmd);
        }
        item.addActionListener(this);
        return item;
    }


    private JMenuItem createJMenuItem(
            String text,
            int accKeyCode,
            boolean shift,
            String actionCmd) {
        JMenuItem item = createJMenuItem(text, actionCmd);
        item.setAccelerator(
                KeyStroke.getKeyStroke(
                        accKeyCode,
                        this.mnuShortcutKeyMask
                                | (shift ? InputEvent.SHIFT_DOWN_MASK : 0)));
        return item;
    }


    private JMenuItem createJMenuItem(
            String text,
            int accKeyCode,
            String actionCmd) {
        return createJMenuItem(text, accKeyCode, false, actionCmd);
    }


    private Dimension getJTCScreenSize() {
        return new Dimension(
                this.jtcSys.getScreenWidth(),
                this.jtcSys.getScreenHeight());
    }


    private static int getSettingsScreenRefreshMillis() {
        return AppContext.getIntProperty(
                PROP_SCREEN_REFRESH_MS,
                DEFAULT_SCREEN_REFRESH_MS);
    }


    private void loadFile(File file, boolean withOptions) {
        if (this.jtcSys != null) {
            int begAddr = -1;
            FileInfo fileInfo = FileInfo.analyzeFile(file);
            if (fileInfo != null) {
                begAddr = fileInfo.getBegAddr();
            }
            if (!withOptions && (fileInfo != null) && (begAddr >= 0)) {
                LoadDlg.loadFile(
                        this,
                        this,
                        this.jtcSys,
                        file,
                        fileInfo.getFormat(),
                        begAddr,
                        fileInfo.getEndAddr(),
                        fileInfo.getStartAddr());
            } else {
                LoadDlg.open(this, this.jtcSys, file, fileInfo);
            }
        }
    }


    private void resetEmu(boolean initRAM) {
        this.screenFld.clearSelection();
        this.jtcSys.fireReset(initRAM);

        /*
         * Der Timer wird neu gestartet fuer den Fall,
         * dass sich der Timer aufgehaengt haben sollte,
         * und man moechte ihn mit RESET reaktivieren.
         */
        this.screenRefreshTimer.restart();
    }


    private void setPasting(boolean state) {
        this.mnuPasteCancel.setEnabled(state);
    }


    private boolean updScreenSize(boolean updScale) {
        Dimension newSize = getJTCScreenSize();
        boolean sizeChanged = !newSize.equals(this.jtcScreenSize);
        if (sizeChanged) {
            Integer screenScale = null;
            if (updScale) {
                screenScale = this.jtcScreenSize2Scale.get(newSize);
            }
            if (screenScale != null) {
                this.screenFld.setScale(screenScale);
            } else {
                this.screenFld.updPreferredSize();
            }
            this.jtcScreenSize = newSize;
        }
        int mOld = this.screenFld.getMargin();
        int mNew = AppContext.getIntProperty(
                PROP_SCREEN_MARGIN,
                DEFAULT_SCREEN_MARGIN);
        this.screenFld.setMargin(mNew);
        if (mNew != mOld) {
            sizeChanged = true;
        }
        return sizeChanged;
    }


    private void updStatusText() {
        if (this.statusRefreshTimer.isRunning()) {
            this.statusRefreshTimer.stop();
        }
        String text = null;
        if (this.z8.isPause()) {
            this.mnuPause.setText(TEXT_GO_ON);
            this.mnuPause.setEnabled(true);
            text = "Emulator angehalten";
        } else {
            this.mnuPause.setText(TEXT_PAUSE);
            this.mnuPause.setEnabled(true);
            Double mhz = this.z8.getEmulatedMHz();
            int millis = STATUS_REFRESH_MILLIS;
            if (this.z8.getCyclesPerSecond() > 0) {
                if (mhz != null) {
                    if ((mhz.doubleValue() >= 3.8)
                            && (mhz.doubleValue() <= 4.2)) {
                        mhz = null;
                    } else {
                        millis = STATUS_REFRESH_MILLIS;
                    }
                }
            } else {
                millis = STATUS_REFRESH_MILLIS;
            }
            if (mhz != null) {
                text = JTCUtil.getEmulatedSpeedText(mhz.doubleValue());
            }
            this.statusRefreshTimer.setDelay(millis);
            this.statusRefreshTimer.setInitialDelay(millis);
            this.statusRefreshTimer.start();
        }
        this.fldStatusText.setText(text != null ? text : DEFAULT_STATUS_TEXT);
    }


    private void z8UpdateInternal(Z8Listener.Reason reason) {
        switch (reason) {
            case POWER_ON:
            case RESET:
                AudioFrm.reset();
                KeyboardFrm.reset();
                MemEditFrm.reset();
                ReassFrm.reset();
                break;

            case CYCLES_PER_SECOND_CHANGED:
                if (this.z8.getCyclesPerSecond() > 0) {
                    this.mnuSpeed.setText(TEXT_MAX_SPEED);
                } else {
                    this.mnuSpeed.setText(TEXT_NORM_SPEED);
                }
                AudioFrm.z8CyclesPerSecondChanged();
                updStatusText();
                break;

            case STATUS_CHANGED:
                updStatusText();
                DebugFrm.z8StatusChanged();
                break;
        }
    }
}
