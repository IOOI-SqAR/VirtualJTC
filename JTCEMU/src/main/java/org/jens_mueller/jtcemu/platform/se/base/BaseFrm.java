/*
 * (c) 2007-2020 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Basisklasse fuer alle Frames
 */

package org.jens_mueller.jtcemu.platform.se.base;

import org.jens_mueller.jtcemu.platform.se.Main;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;


public abstract class BaseFrm extends JFrame implements WindowListener {
    protected BaseFrm() {
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        Main.setIconImages(this);
        addWindowListener(this);
    }


    protected boolean doClose() {
        setVisible(false);
        dispose();
        return true;
    }


    public KeyStroke getMenuShortcut(int keyCode) {
        return KeyStroke.getKeyStroke(
                keyCode,
                GUIUtil.getMenuShortcutKeyMask(this));
    }


    public KeyStroke getMenuShortcutWithShift(int keyCode) {
        return KeyStroke.getKeyStroke(
                keyCode,
                GUIUtil.getMenuShortcutKeyMask(this)
                        | InputEvent.SHIFT_DOWN_MASK);
    }


    public abstract String getPropPrefix();


    public void lafChanged() {
        // leer
    }


    public void memorizeSettings() {
        GUIUtil.memorizeWindowSettings(this);
    }


    public void settingsChanged() {
        // leer
    }


    /* --- WindowListener --- */

    @Override
    public void windowActivated(WindowEvent e) {
        // leer;
    }


    @Override
    public void windowClosed(WindowEvent e) {
        // leer;
    }


    @Override
    public void windowClosing(WindowEvent e) {
        doClose();
    }


    @Override
    public void windowDeactivated(WindowEvent e) {
        // leer;
    }


    @Override
    public void windowDeiconified(WindowEvent e) {
        // leer;
    }


    @Override
    public void windowIconified(WindowEvent e) {
        // leer;
    }


    @Override
    public void windowOpened(WindowEvent e) {
        // leer
    }
}
