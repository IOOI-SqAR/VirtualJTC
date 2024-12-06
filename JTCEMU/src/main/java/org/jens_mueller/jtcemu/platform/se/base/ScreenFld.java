/*
 * (c) 2007-2019 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Komponente fuer die Darstellung des Bildschirminhaltes
 */

package org.jens_mueller.jtcemu.platform.se.base;

import org.jens_mueller.jtcemu.base.CharRaster;
import org.jens_mueller.jtcemu.base.JTCScreen;
import org.jens_mueller.jtcemu.base.JTCSys;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;


public class ScreenFld
        extends JComponent
        implements JTCScreen, MouseMotionListener {
    private TopFrm topFrm;
    private JTCSys jtcSys;
    private int jtcScreenH;
    private int jtcScreenW;
    private int scale;
    private int margin;
    private volatile boolean dirty;
    private Color[] colors;
    private Color markXORColor;
    private CharRaster charRaster;
    private Point dragStart;
    private Point dragEnd;
    private boolean textSelected;
    private int selectionCharX1;
    private int selectionCharX2;
    private int selectionCharY1;
    private int selectionCharY2;


    public ScreenFld(TopFrm topFrm, JTCSys jtcSys, int scale) {
        this.topFrm = topFrm;
        this.jtcSys = jtcSys;
        this.scale = scale;
        this.margin = 0;
        this.dirty = false;
        this.markXORColor = new Color(192, 192, 0);
        this.colors = null;
        this.charRaster = null;
        this.dragStart = null;
        this.dragEnd = null;
        this.textSelected = false;
        this.selectionCharX1 = -1;
        this.selectionCharY1 = -1;
        this.selectionCharX2 = -1;
        this.selectionCharY2 = -1;

        int[] rgbs = jtcSys.getColorModeRGBs();
        this.colors = new Color[rgbs.length];
        for (int i = 0; i < this.colors.length; i++) {
            this.colors[i] = new Color(0xFF000000 | rgbs[i]);
        }
        updPreferredSize();
        this.jtcSys.setScreen(this);
        addMouseMotionListener(this);
        addMouseListener(
                new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        if (e.getButton() == MouseEvent.BUTTON1) {
                            clearSelection();
                        }
                    }
                });
    }


    public void clearSelection() {
        this.dragStart = null;
        this.dragEnd = null;
        this.selectionCharX1 = -1;
        this.selectionCharY1 = -1;
        this.selectionCharX2 = -1;
        this.selectionCharY2 = -1;
        setScreenDirty();
    }


    public BufferedImage createBufferedImage() {
        BufferedImage img = null;
        int w = getWidth();
        int h = getHeight();
        if ((w > 0) && (h > 0) && (this.colors != null)) {
            byte[] r = new byte[this.colors.length];
            byte[] g = new byte[this.colors.length];
            byte[] b = new byte[this.colors.length];
            for (int i = 0; i < this.colors.length; i++) {
                r[i] = (byte) this.colors[i].getRed();
                g[i] = (byte) this.colors[i].getGreen();
                b[i] = (byte) this.colors[i].getBlue();
            }
            img = new BufferedImage(
                    w,
                    h,
                    BufferedImage.TYPE_BYTE_INDEXED,
                    new IndexColorModel(
                            Integer.highestOneBit(
                                    this.colors.length - 1),
                            this.colors.length,
                            r, g, b));
            Graphics graphics = img.createGraphics();
            paint(graphics, w, h, false);
            graphics.dispose();
        }
        return img;
    }


    public int getMargin() {
        return this.margin;
    }


    public int getScale() {
        return this.scale;
    }


    public String getSelectedText() {
        String text = null;
        if ((this.selectionCharX1 >= 0)
                && (this.selectionCharY1 >= 0)
                && (this.selectionCharX2 >= 0)
                && (this.selectionCharY2 >= 0)) {
            text = this.jtcSys.getScreenText(
                    this.selectionCharX1,
                    this.selectionCharY1,
                    this.selectionCharX2,
                    this.selectionCharY2);
        }
        return text;
    }


    public boolean isDirty() {
        return this.dirty;
    }


    public void setMargin(int margin) {
        this.margin = margin;
        updPreferredSize();
    }


    public void setScale(int scale) {
        if ((scale > 0) && (scale != this.scale)) {
            this.scale = scale;
            updPreferredSize();
        }
    }


    public void updPreferredSize() {
        this.jtcScreenW = jtcSys.getScreenWidth();
        this.jtcScreenH = jtcSys.getScreenHeight();

        int margin = this.margin;
        if (margin < 0) {
            margin = 0;
        }
        setPreferredSize(
                new Dimension(
                        (2 * margin) + (this.jtcScreenW * this.scale),
                        (2 * margin) + (this.jtcScreenH * this.scale)));
        Container parent = getParent();
        if (parent != null) {
            parent.invalidate();
        } else {
            invalidate();
        }
    }


    /* --- JTCScreen --- */

    @Override
    public void screenConfigChanged() {
        final TopFrm topFrm = this.topFrm;
        EventQueue.invokeLater(
                new Runnable() {
                    @Override
                    public void run() {
                        topFrm.screenConfigChanged();
                    }
                });
    }


    @Override
    public void setScreenDirty() {
        this.dirty = true;
    }


    /* --- MouseMotionListener --- */

    @Override
    public void mouseDragged(MouseEvent e) {
        if (e.getComponent() == this) {
            if (this.dragStart == null) {
                this.charRaster = this.jtcSys.getScreenCharRaster();
                if (this.charRaster != null) {
                    this.dragStart = new Point(e.getX(), e.getY());
                    this.dragEnd = null;
                    setScreenDirty();
                }
            } else {
                if (this.charRaster != null) {
                    this.dragEnd = new Point(e.getX(), e.getY());
                } else {
                    this.dragEnd = null;
                    this.dragStart = null;
                }
                setScreenDirty();
            }
            e.consume();
        }
    }


    @Override
    public void mouseMoved(MouseEvent e) {
        // leer
    }


    /* --- ueberschriebene Methoden --- */

    @Override
    public boolean isFocusable() {
        return true;
    }


    @Override
    public void paint(Graphics g) {
        this.dirty = false;
        paint(g, getWidth(), getHeight(), true);
    }


    /*
     * update(...) wird ueberschrieben,
     * da paint(...) die Komponente vollstaendig fuellt
     * und somit das standardmaessige Fuellen mit der Hintegrundfarbe
     * entfallen kann.
     */
    @Override
    public void update(Graphics g) {
        paint(g);
    }


    /* --- private Methoden --- */

    private Color getColor(int colorNum) {
        Color color = Color.BLACK;
        if (this.jtcSys.isMonochrome()) {
            if (colorNum > 0) {
                color = Color.WHITE;
            }
        } else {
            if ((colorNum >= 0) && (colorNum < this.colors.length)) {
                color = this.colors[colorNum];
            }
        }
        return color;
    }


    private void paint(Graphics g, int w, int h, boolean withMarking) {
        if ((w > 0) && (h > 0)) {

            // Hintergrund
            Color bgColor = Color.BLACK;
            g.setColor(bgColor);
            g.fillRect(0, 0, w, h);

            // Vordergrund
            int baseW = this.jtcScreenW;
            int baseH = this.jtcScreenH;
            if ((baseW > 0) && (baseH > 0)
                    && this.jtcSys.isScreenOutputEnabled()) {
                // zentrieren
                int xOffs = (w - (baseW * this.scale)) / 2;
                if (xOffs < 0) {
                    xOffs = 0;
                }
                int yOffs = (h - (baseH * this.scale)) / 2;
                if (yOffs < 0) {
                    yOffs = 0;
                }
                if ((xOffs > 0) || (yOffs > 0)) {
                    g.translate(xOffs, yOffs);
                }

                /*
                 * Aus Gruenden der Performance werden nebeneinander liegende
                 * Punkte zusammengefasst und als Linie gezeichnet.
                 */
                for (int y = 0; y < baseH; y++) {
                    Color lastColor = null;
                    int xColorBeg = -1;
                    for (int x = 0; x < baseW; x++) {
                        Color color = getColor(this.jtcSys.getPixelColorNum(x, y));
                        if ((color != null) && (color != lastColor)) {
                            if ((lastColor != null)
                                    && (lastColor != bgColor)
                                    && (xColorBeg >= 0)) {
                                g.setColor(lastColor);
                                g.fillRect(
                                        xColorBeg * this.scale,
                                        y * this.scale,
                                        (x - xColorBeg) * this.scale,
                                        this.scale);
                            }
                            xColorBeg = x;
                            lastColor = color;
                        }
                    }
                    if ((lastColor != null)
                            && (lastColor != bgColor)
                            && (xColorBeg >= 0)) {
                        g.setColor(lastColor);
                        g.fillRect(
                                xColorBeg * this.scale,
                                y * this.scale,
                                (baseW - xColorBeg) * this.scale,
                                this.scale);
                    }
                }

                // Markierter Text
                if (withMarking) {
                    if ((xOffs > 0) && (yOffs > 0)) {
                        g.translate(-xOffs, -yOffs);
                    }
                    boolean textSelected = false;
                    CharRaster charRaster = this.charRaster;
                    Point dragStart = this.dragStart;
                    Point dragEnd = this.dragEnd;
                    int scale = this.scale;
                    if ((charRaster != null)
                            && (dragStart != null)
                            && (dragEnd != null)
                            && (scale > 0)) {
                        int nCols = charRaster.getColCount();
                        int nRows = charRaster.getRowCount();
                        int wChar = charRaster.getCharWidth();
                        int hChar = charRaster.getCharHeight();
                        if ((nCols > 0) && (nRows > 0)
                                && (wChar > 0) && (hChar > 0)) {
                            int x1 = dragStart.x;
                            int y1 = dragStart.y;
                            int x2 = dragEnd.x;
                            int y2 = dragEnd.y;
                            xOffs += (charRaster.getXOffset() * scale);
                            yOffs += (charRaster.getYOffset() * scale);

                            // Zeichenpositionen berechnen
                            this.selectionCharX1 = Math.max(
                                    (x1 - xOffs) / scale, 0) / wChar;
                            this.selectionCharY1 = Math.max(
                                    (y1 - yOffs) / scale, 0) / hChar;
                            this.selectionCharX2 = Math.max(
                                    (x2 - xOffs) / scale, 0) / wChar;
                            this.selectionCharY2 = Math.max(
                                    (y2 - yOffs) / scale, 0) / hChar;
                            if (this.selectionCharX1 >= nCols) {
                                this.selectionCharX1 = nCols - 1;
                            }
                            if (this.selectionCharY1 >= nRows) {
                                this.selectionCharY1 = nRows - 1;
                            }
                            if (this.selectionCharX2 >= nCols) {
                                this.selectionCharX2 = nCols - 1;
                            }
                            if (this.selectionCharY2 >= nRows) {
                                this.selectionCharY2 = nRows - 1;
                            }

                            // Koordinaten tauschen, wenn Endpunkt vor Startpunkt liegt
                            if ((this.selectionCharY1 > this.selectionCharY2)
                                    || ((this.selectionCharY1 == this.selectionCharY2)
                                    && (this.selectionCharX1 > this.selectionCharX2))) {
                                int m = this.selectionCharX1;
                                this.selectionCharX1 = this.selectionCharX2;
                                this.selectionCharX2 = m;

                                m = this.selectionCharY1;
                                this.selectionCharY1 = this.selectionCharY2;
                                this.selectionCharY2 = m;

                                m = x1;
                                x1 = x2;
                                x2 = m;

                                m = y1;
                                y1 = y2;
                                y2 = m;
                            }

                            /*
                             * Koordinaten anpassen,
                             * wenn Endpunkt ausserhalb der Bildschirmausgabe liegt
                             */
                            if (y1 < yOffs) {
                                this.selectionCharX1 = 0;
                                this.selectionCharY1 = 0;
                            } else {
                                if (x1 > (xOffs + (scale * nCols * wChar))) {
                                    this.selectionCharX1 = 0;
                                    this.selectionCharY1++;
                                }
                            }
                            if (y2 > (yOffs + (scale * (nRows * hChar)))) {
                                this.selectionCharX2 = nCols - 1;
                                this.selectionCharY2 = nRows - 1;
                            } else {
                                if (x2 < xOffs) {
                                    this.selectionCharX2 = nCols - 1;
                                    --this.selectionCharY2;
                                }
                            }

                            // Markierter Text visualisieren
                            g.setColor(Color.WHITE);
                            g.setXORMode(this.markXORColor);
                            if (this.selectionCharY1 == this.selectionCharY2) {
                                g.fillRect(
                                        xOffs + (scale * this.selectionCharX1 * wChar),
                                        yOffs + (scale * this.selectionCharY1 * hChar),
                                        scale * (this.selectionCharX2
                                                - this.selectionCharX1 + 1) * wChar,
                                        scale * hChar);
                            } else {
                                g.fillRect(
                                        xOffs + (scale * this.selectionCharX1 * wChar),
                                        yOffs + (scale * this.selectionCharY1 * hChar),
                                        scale * (nCols - this.selectionCharX1) * wChar,
                                        scale * hChar);
                                if (this.selectionCharY1 + 1 < this.selectionCharY2) {
                                    g.fillRect(
                                            xOffs,
                                            yOffs + (scale * (this.selectionCharY1 + 1) * hChar),
                                            scale * nCols * wChar,
                                            scale * (this.selectionCharY2
                                                    - this.selectionCharY1 - 1) * hChar);
                                }
                                g.fillRect(
                                        xOffs,
                                        yOffs + (scale * this.selectionCharY2 * hChar),
                                        scale * (this.selectionCharX2 + 1) * wChar,
                                        scale * hChar);
                            }
                            textSelected = true;
                        }
                    }
                    if (textSelected != this.textSelected) {
                        this.textSelected = textSelected;
                        this.topFrm.setScreenTextSelected(textSelected);
                    }
                }
            }
        }
    }
}
