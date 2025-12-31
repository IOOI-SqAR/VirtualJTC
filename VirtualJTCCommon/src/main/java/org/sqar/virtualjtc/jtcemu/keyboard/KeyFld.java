/*
 * (c) 2007-2021 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Komponente fuer eine Taste der Bildschirmtastatur
 */

package org.sqar.virtualjtc.jtcemu.keyboard;


import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;


public class KeyFld extends JComponent {
    private static final Border borderPressed
            = BorderFactory.createLoweredBevelBorder();

    private static final Border borderReleased
            = BorderFactory.createRaisedBevelBorder();

    private int colIdx;
    private int colValue;
    private Object baseObj;
    private Object shift1Obj;
    private Object shift2Obj;
    private Object shift3Obj;
    private boolean shiftKey;
    private boolean pressed;
    private boolean snapped;


    public KeyFld(
            int colIdx,
            int colValue,
            Object baseObj,
            Object shift1Obj,
            Object shift2Obj,
            Object shift3Obj) {
        this.colIdx = colIdx;
        this.colValue = colValue;
        this.baseObj = baseObj;
        this.shift1Obj = shift1Obj;
        this.shift2Obj = shift2Obj;
        this.shift3Obj = shift3Obj;
        this.shiftKey = false;
        this.pressed = false;
        this.snapped = false;
        setBorder(borderReleased);
    }


    public Image getBaseImage() {
        return getImage(this.baseObj);
    }


    public String getBaseText() {
        return getText(this.baseObj);
    }


    public int getColIdx() {
        return this.colIdx;
    }


    public int getColValue() {
        return this.colValue;
    }


    public Image getShift1Image() {
        return getImage(this.shift1Obj);
    }


    public String getShift1Text() {
        return getText(this.shift1Obj);
    }


    public Image getShift2Image() {
        return getImage(this.shift2Obj);
    }


    public String getShift2Text() {
        return getText(this.shift2Obj);
    }


    public String getShift3Text() {
        return getText(this.shift3Obj);
    }


    public boolean isPressed() {
        return this.pressed;
    }


    public boolean isShiftKey() {
        return this.shiftKey;
    }


    public boolean isSnapped() {
        return this.snapped;
    }


    public void setPressed(boolean pressed, boolean snapped) {
        boolean oldPressed = this.pressed;
        this.pressed = pressed;
        this.snapped = snapped;
        if (pressed != oldPressed) {
            setBorder(pressed ? borderPressed : borderReleased);
            repaint();
        }
    }


    public void setShiftKey(boolean state) {
        this.shiftKey = state;
    }


    /* --- ueberschriebene Methoden --- */

    @Override
    protected void paintComponent(Graphics g) {
        Container parent = getParent();
        while (parent != null) {
            if (parent instanceof AbstractKeyboardFld) {
                ((AbstractKeyboardFld) parent).paintKeyFld(g, this);
                break;
            }
            parent = getParent();
        }
    }


    /* --- private Methoden --- */

    private static Image getImage(Object o) {
        Image img = null;
        if (o != null) {
            if (o instanceof Image) {
                img = (Image) o;
            }
        }
        return img;
    }


    private static String getText(Object o) {
        String s = null;
        if (o != null) {
            if (!(o instanceof Image)) {
                s = o.toString();
            }
        }
        return s;
    }
}
