/*
 * (c) 2021 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Komponente fuer die Bildschirmtastatur fuer ES2.3
 */

package org.sqar.virtualjtc.jtcemu.keyboard;


import org.sqar.virtualjtc.jtcemu.base.JTCSys;

import java.awt.*;


public class ES23KeyboardFld extends AbstractKeyboardFld {
    private static Font baseFont = new Font(Font.SANS_SERIF, Font.BOLD, 14);
    private static Font shiftFont = new Font(Font.SANS_SERIF, Font.PLAIN, 12);


    public ES23KeyboardFld(JTCSys jtcSys) {
        super(jtcSys, 0, 16, 45, 45);

        addKey(null);
        addKey("1", "!");
        addKey("2", "\"");
        addKey("3", "#");
        addKey("4", "$");
        addKey("5", "%");
        addKey("6", "&");
        addKey("7", "\'");
        addKey("8", "@");
        addKey("9", "(");
        addKey("0", ")");
        addKey("<");
        addKey(">");
        addKey("DBS");
        addKey("INS");
        addKey("CLS");

        addKey(null);
        addKey("Q");
        addKey("W");
        addKey("E");
        addKey("R");
        addKey("T");
        addKey("Z");
        addKey("U");
        addKey("I");
        addKey("O");
        addKey("P");
        addKey("-", "_");
        addKey("=");
        addKey("DEL");
        addKey(getImageUp());
        addKey("SOL");

        addKey("CTL").setShiftKey(true);
        addKey("A");
        addKey("S");
        addKey("D");
        addKey("F");
        addKey("G");
        addKey("H");
        addKey("J");
        addKey("K");
        addKey("L");
        addKey(";", ":");
        addKey("+", "\\");
        addKey("*", "^");
        addKey(getImageLeft());
        addKey("HOM");
        addKey(getImageRight());

        addKey("SHT").setShiftKey(true);
        addKey("Y");
        addKey("X");
        addKey("C");
        addKey("V");
        addKey("B");
        addKey("N");
        addKey("M");
        addKey(",");
        addKey(".");
        addKey("/");
        addKey("RET");
        addKey(getImageSpace());
        addKey(getImageSpace());
        addKey(getImageDown());
        addKey("RET");
    }


    /* --- ueberschriebene Methoden --- */

    @Override
    public String getHoldShiftKeysText() {
        return "CTL- und SHT-Taste gedr\u00FCckt halten"; // TODO: i18n
    }


    @Override
    public JTCSys.OSType getOSType() {
        return JTCSys.OSType.ES23;
    }


    @Override
    public void paintKeyFld(Graphics g, KeyFld keyFld) {
        super.paintKeyFld(g, keyFld);
        g.setColor(Color.BLACK);
        g.setFont(shiftFont);

        String s = keyFld.getShift1Text();
        if (s != null) {
            drawRight(g, s, keyFld.getWidth() - 8, 18);
        }
        s = keyFld.getBaseText();
        if (s != null) {
            g.setFont(baseFont);
            g.drawString(s, 8, keyFld.getHeight() - 8);
        }
        Image img = keyFld.getBaseImage();
        if (img != null) {
            g.drawImage(img, 8, keyFld.getHeight() - 18, this);
        }
    }
}
