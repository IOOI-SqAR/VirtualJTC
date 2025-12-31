/*
 * (c) 2007-2010 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Basisklasse fuer alle Frames
 */

package org.sqar.virtualjtc.jtcemu.base;

import org.sqar.virtualjtc.jtcemu.Main;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public abstract class BaseFrm extends JFrame implements ActionListener, WindowListener {

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


    /* --- ActionListener --- */

    @Override
    public void actionPerformed(ActionEvent e) {

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
