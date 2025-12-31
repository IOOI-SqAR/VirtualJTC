/*
 * (c) 2007-2021 Jens Mueller
 * (c) 2017-2024 Lars Sonchocky-Helldorf
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Texteditor
 */

package org.sqar.virtualjtc.jtcemu.tools;

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.dnd.*;
import java.awt.event.*;
import java.awt.geom.Point2D;
import java.io.*;
import java.lang.reflect.Method;
import java.text.CharacterIterator;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.regex.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.undo.UndoManager;

import org.sqar.virtualjtc.jtcemu.Main;
import org.sqar.virtualjtc.jtcemu.base.*;
import org.sqar.virtualjtc.jtcemu.tools.assembler.*;


public class TextEditFrm extends AbstractTextFrm
        implements
        ActionListener,
        CaretListener,
        DocumentListener,
        DropTargetListener,
        FlavorListener,
        FocusListener,
        MouseListener,
        UndoableEditListener {
    private static final Locale locale = Locale.getDefault();
    private static final ResourceBundle textEditFrmResourceBundle = ResourceBundle.getBundle("TextEditFrm", locale);

    private static final String FILE_GROUP_TEXT = "text";
    private static final String FILE_GROUP_PROJECT = "project";
    private static final String LABEL_BEG_ADDR_OF_BASIC_PRG =
            "Anfangsadresse des BASIC-Programms:"; // TODO: i18n

    private static TextEditFrm instance = null;
    private static Point lastLocation = null;

    private JTCSys jtcSys;
    private UndoManager undoMngr;
    private AsmOptions asmOptions;
    private Integer basicAddrTransfer;
    private int basicAddrFetch;
    private boolean dataChanged;
    private File prjFile;
    private File textFile;
    private TextFinder textFinder;
    private Method viewToModelMethod;
    private JTextComponent focusFld;
    private JTextComponent selectionFld;
    private JMenuItem mnuNew;
    private JMenuItem mnuOpen;
    private JMenuItem mnuLoadBasicFromMem;
    private JMenuItem mnuLoadBasicFromMemWith;
    private JMenuItem mnuSave;
    private JMenuItem mnuSaveAs;
    private JMenuItem mnuPrintOptions;
    private JMenuItem mnuPrint;
    private JMenuItem mnuClose;
    private JMenuItem mnuUndo;
    private JMenuItem mnuCut;
    private JMenuItem mnuCopy;
    private JMenuItem mnuPaste;
    private JMenuItem mnuSelectAll;
    private JMenuItem mnuUpper;
    private JMenuItem mnuLower;
    private JMenuItem mnuFindAndReplace;
    private JMenuItem mnuFindNext;
    private JMenuItem mnuFindPrev;
    private JMenuItem mnuReplace;
    private JMenuItem mnuGoto;
    private JMenuItem mnuAssemble;
    private JMenuItem mnuAssembleWith;
    private JMenuItem mnuAsmPrjOpen;
    private JMenuItem mnuAsmPrjSave;
    private JMenuItem mnuAsmPrjSaveAs;
    private JMenuItem mnuBasicParse;
    private JMenuItem mnuBasicIntoEmu;
    private JMenuItem mnuBasicIntoEmuWith;
    private JMenuItem mnuHelpContent;
    private JButton btnNew;
    private JButton btnOpen;
    private JButton btnSave;
    private JButton btnPrint;
    private JButton btnUndo;
    private JButton btnCut;
    private JButton btnCopy;
    private JButton btnPaste;
    private JButton btnFindAndReplace;
    private Document textDoc;
    private JTextArea logArea;
    private JSplitPane splitPane;
    private JLabel labelStatus;


    public static boolean close() {
        return instance != null ? instance.doClose() : true;
    }


    public boolean confirmDataSaved() {
        boolean rv = false;
        if (this.dataChanged) {
            String[] options = {
                    textEditFrmResourceBundle.getString("confirmDataSaved.options.save"),
                    textEditFrmResourceBundle.getString("confirmDataSaved.options.discard"),
                    textEditFrmResourceBundle.getString("confirmDataSaved.options.cancel")
            };
            JOptionPane pane = new JOptionPane(
                    textEditFrmResourceBundle.getString("confirmDataSaved.message"),
                    JOptionPane.WARNING_MESSAGE);
            pane.setOptions(options);
            pane.setInitialValue(options[0]);
            pane.setWantsInput(false);
            pane.createDialog(this, textEditFrmResourceBundle.getString("confirmDataSaved.title")).setVisible(true);
            Object value = pane.getValue();
            if (value != null) {
                if (value.equals(options[0])) {
                    if (this.prjFile != null) {
                        rv = doPrgAsmPrjSave(false);
                    } else {
                        rv = doSave(false);
                    }
                } else if (value.equals(options[1])) {
                    rv = true;
                }
            }
        } else {
            rv = true;
        }
        return rv;
    }


    public static void open(JTCSys jtcSys) {
        if (instance == null) {
            instance = new TextEditFrm(jtcSys);
            if (lastLocation != null) {
                instance.setLocation(lastLocation);
            }
            instance.setVisible(true);
        }
        GUIUtil.toFront(instance);
    }


    /* --- ActionListener --- */

    @Override
    public void actionPerformed(ActionEvent e) {
        GUIUtil.setWaitCursor(this, true);
        Object src = e.getSource();
        if (src != null) {
            if ((src == this.mnuNew) || (src == this.btnNew)) {
                doNew();
            } else if ((src == this.mnuOpen) || (src == this.btnOpen)) {
                doOpen();
            } else if (src == this.mnuLoadBasicFromMem) {
                doLoadBasicMem(JTCSys.DEFAULT_BASIC_ADDR);
            } else if (src == this.mnuLoadBasicFromMemWith) {
                doLoadBasicMem(null);
            } else if ((src == this.mnuSave) || (src == this.btnSave)) {
                doSave(false);
            } else if (src == this.mnuSaveAs) {
                doSave(true);
            } else if (src == this.mnuPrintOptions) {
                PrintOptionsDlg.open(this);
            } else if ((src == this.mnuPrint) || (src == this.btnPrint)) {
                doPrint();
            } else if (src == this.mnuClose) {
                doClose();
            } else if ((src == this.mnuUndo) || (src == this.btnUndo)) {
                doUndo();
            } else if ((src == this.mnuCut) || (src == this.btnCut)) {
                if (this.selectionFld != null) {
                    if (this.selectionFld.isEditable()) {
                        this.selectionFld.cut();
                    }
                }
            } else if ((src == this.mnuCopy) || (src == this.btnCopy)) {
                if (this.selectionFld != null) {
                    this.selectionFld.copy();
                }
            } else if ((src == this.mnuPaste) || (src == this.btnPaste)) {
                if (this.focusFld != null) {
                    if (this.focusFld.isEditable()) {
                        this.focusFld.paste();
                    }
                }
            } else if (src == this.mnuSelectAll) {
                if (this.focusFld != null) {
                    this.focusFld.requestFocus();
                    this.focusFld.selectAll();
                }
            } else if (src == this.mnuUpper) {
                doUpper();
            } else if (src == this.mnuLower) {
                doLower();
            } else if ((src == this.mnuFindAndReplace)
                    || (src == this.btnFindAndReplace)) {
                doFindAndReplace();
            } else if (src == this.mnuFindNext) {
                doFindNext();
            } else if (src == this.mnuFindPrev) {
                doFindPrev();
            } else if (src == this.mnuReplace) {
                doReplace();
            } else if (src == this.mnuGoto) {
                doGoto();
            } else if (src == this.mnuAssemble) {
                doPrgAssemble(false);
            } else if (src == this.mnuAssembleWith) {
                doPrgAssemble(true);
            } else if (src == this.mnuAsmPrjOpen) {
                doPrgAsmPrjOpen();
            } else if (src == this.mnuAsmPrjSave) {
                doPrgAsmPrjSave(false);
            } else if (src == this.mnuAsmPrjSaveAs) {
                doPrgAsmPrjSave(true);
            } else if (src == this.mnuBasicParse) {
                doPrgBasic(false, false);
            } else if (src == this.mnuBasicIntoEmu) {
                doPrgBasic(true, false);
            } else if (src == this.mnuHelpContent) {
                HelpFrm.open(textEditFrmResourceBundle.getString("help.path"));
            } else if (src == this.mnuBasicIntoEmuWith) {
                doPrgBasic(true, true);
            }
        }
        GUIUtil.setWaitCursor(this, false);
    }


    /* --- CaretListener --- */

    @Override
    public void caretUpdate(CaretEvent e) {
        Object src = e.getSource();
        if (src != null) {
            if (src instanceof JTextComponent) {
                this.selectionFld = (JTextComponent) src;
                boolean selected = GUIUtil.hasSelection(this.selectionFld);
                this.mnuCut.setEnabled(selected && this.selectionFld.isEditable());
                this.btnCut.setEnabled(selected && this.selectionFld.isEditable());
                this.mnuCopy.setEnabled(selected);
                this.btnCopy.setEnabled(selected);
                this.mnuUpper.setEnabled(selected);
                this.mnuLower.setEnabled(selected);
                this.mnuReplace.setEnabled(
                        (this.selectionFld == this.textArea)
                                && selected
                                && this.textFinder.hasReplaceText());
            }
            if (src == this.textArea) {
                updStatusText();
            }
        }
    }


    /* --- DocumentListener --- */

    @Override
    public void changedUpdate(DocumentEvent e) {
        docChanged(e);
    }


    @Override
    public void insertUpdate(DocumentEvent e) {
        docChanged(e);
    }


    @Override
    public void removeUpdate(DocumentEvent e) {
        docChanged(e);
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
                            loadFile(file);
                        }
                    });
        }
    }


    @Override
    public void dropActionChanged(DropTargetDragEvent e) {
        if (!GUIUtil.isFileDrop(e))
            e.rejectDrag();
    }


    /* --- FlavorListener --- */

    @Override
    public void flavorsChanged(FlavorEvent e) {
        Object src = e.getSource();
        if (src != null) {
            if (src instanceof Clipboard) {
                updPasteBtn((Clipboard) src);
            }
        }
    }


    /* --- FocusListener --- */

    @Override
    public void focusGained(FocusEvent e) {
        Component c = e.getComponent();
        if ((c != null) && !e.isTemporary()) {
            if (c instanceof JTextComponent) {
                this.focusFld = (JTextComponent) c;
            }
        }
    }


    @Override
    public void focusLost(FocusEvent e) {
        if (!e.isTemporary())
            this.focusFld = null;
    }


    /* --- MouseListener --- */

    @Override
    public void mouseClicked(MouseEvent e) {
        if ((e.getComponent() == this.logArea) && (e.getClickCount() > 1)) {
            Point point = e.getPoint();
            if (point != null) {
                /*
                 * Ab Java 9 ersetzt die Methode JTextComponent.viewToModel2D(...)
                 * die Methode JTextComponent.viewToModel(...).
                 */
                if (this.viewToModelMethod == null) {
                    try {
                        this.viewToModelMethod = this.logArea.getClass().getMethod(
                                "viewToModel2D",
                                Point2D.class);
                    } catch (NoSuchMethodException ex) {
                    }
                }
                if (this.viewToModelMethod == null) {
                    try {
                        this.viewToModelMethod = this.logArea.getClass().getMethod(
                                "viewToModel",
                                Point.class);
                    } catch (NoSuchMethodException ex) {
                    }
                }
                if (this.viewToModelMethod != null) {
                    try {
                        Object pos = this.viewToModelMethod.invoke(
                                this.logArea,
                                point);
                        if (pos != null) {
                            if (pos instanceof Number) {
                                int lineNum = ToolUtil.getLineNumFromLineNumMsg(
                                        this.logArea.getText(),
                                        ((Number) pos).intValue());
                                if (lineNum > 0) {
                                    gotoLine(lineNum);
                                }
                            }
                        }
                    } catch (Exception ex) {
                    }
                }
            }
            e.consume();
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
        // leer
    }


    @Override
    public void mouseReleased(MouseEvent e) {
        // leer
    }


    /* --- UndoableEditListener --- */

    @Override
    public void undoableEditHappened(UndoableEditEvent e) {
        this.undoMngr.undoableEditHappened(e);
        updUndoBtn();
    }


    /* --- ueberschriebene Methoden --- */

    @Override
    protected boolean doClose() {
        boolean rv = confirmDataSaved();
        if (rv) {
            rv = super.doClose();
        }
        if (rv) {
            this.undoMngr.discardAllEdits();
            lastLocation = getLocation();
            instance = null;
            updUndoBtn();
        }
        return rv;
    }


    @Override
    public String getPropPrefix() {
        return "texteditor.";
    }


    @Override
    public void memorizeSettings() {
        super.memorizeSettings();
        AppContext.setProperty(
                getClass().getName() + ".split.location",
                Integer.toString(this.splitPane.getDividerLocation()));
    }


    @Override
    public void windowOpened(WindowEvent e) {
        boolean done = false;
        Integer pValue = AppContext.getIntegerProperty(
                getClass().getName() + ".split.location");
        if (pValue != null) {
            this.splitPane.setDividerLocation(pValue.intValue());
        } else {
            this.splitPane.setDividerLocation(0.8);
        }
        this.textArea.requestFocus();
    }


    /* --- privater Konstruktor --- */

    private TextEditFrm(JTCSys jtcSys) {
        this.jtcSys = jtcSys;
        this.textFinder = new TextFinder();
        this.undoMngr = new UndoManager();
        this.asmOptions = null;
        this.basicAddrTransfer = null;
        this.basicAddrFetch = JTCSys.DEFAULT_BASIC_ADDR;
        this.dataChanged = false;
        this.prjFile = null;
        this.textFile = null;
        this.viewToModelMethod = null;
        this.focusFld = null;
        this.selectionFld = null;
        updTitle();


        // Menu
        JMenuBar mnuBar = new JMenuBar();
        setJMenuBar(mnuBar);


        // Menu Datei
        JMenu mnuFile = new JMenu(textEditFrmResourceBundle.getString("menu.file"));
        mnuFile.setMnemonic(KeyEvent.VK_D);
        mnuBar.add(mnuFile);

        this.mnuNew = new JMenuItem(textEditFrmResourceBundle.getString("menuItem.new"));
        this.mnuNew.setAccelerator(getMenuShortcut(KeyEvent.VK_N));
        this.mnuNew.addActionListener(this);
        mnuFile.add(this.mnuNew);

        this.mnuOpen = new JMenuItem(textEditFrmResourceBundle.getString("menuItem.open"));
        this.mnuOpen.setAccelerator(getMenuShortcut(KeyEvent.VK_O));
        this.mnuOpen.addActionListener(this);
        mnuFile.add(this.mnuOpen);
        mnuFile.addSeparator();

        this.mnuLoadBasicFromMem = new JMenuItem(
                String.format(
                        textEditFrmResourceBundle.getString("menuItem.loadBasicMemE000"),
                        JTCSys.DEFAULT_BASIC_ADDR));
        this.mnuLoadBasicFromMem.setAccelerator(
                getMenuShortcut(KeyEvent.VK_B));
        this.mnuLoadBasicFromMem.addActionListener(this);
        mnuFile.add(this.mnuLoadBasicFromMem);

        this.mnuLoadBasicFromMemWith = new JMenuItem(
                textEditFrmResourceBundle.getString("menuItem.loadBasicMemWith"));
        this.mnuLoadBasicFromMemWith.setAccelerator(
                getMenuShortcutWithShift(KeyEvent.VK_B));
        this.mnuLoadBasicFromMemWith.addActionListener(this);
        mnuFile.add(this.mnuLoadBasicFromMemWith);
        mnuFile.addSeparator();

        this.mnuSave = new JMenuItem(textEditFrmResourceBundle.getString("menuItem.save"));
        this.mnuSave.setAccelerator(getMenuShortcut(KeyEvent.VK_S));
        this.mnuSave.addActionListener(this);
        mnuFile.add(this.mnuSave);

        this.mnuSaveAs = new JMenuItem(textEditFrmResourceBundle.getString("menuItem.saveAs"));
        this.mnuSaveAs.setAccelerator(
                getMenuShortcutWithShift(KeyEvent.VK_S));
        this.mnuSaveAs.addActionListener(this);
        mnuFile.add(this.mnuSaveAs);
        mnuFile.addSeparator();

        this.mnuPrintOptions = new JMenuItem(textEditFrmResourceBundle.getString("menuItem.printOptions"));
        this.mnuPrintOptions.addActionListener(this);
        mnuFile.add(this.mnuPrintOptions);

        this.mnuPrint = new JMenuItem(textEditFrmResourceBundle.getString("menuItem.print"));
        this.mnuPrint.setAccelerator(getMenuShortcut(KeyEvent.VK_P));
        this.mnuPrint.addActionListener(this);
        mnuFile.add(this.mnuPrint);
        mnuFile.addSeparator();

        this.mnuClose = new JMenuItem(textEditFrmResourceBundle.getString("menuItem.close"));
        this.mnuClose.addActionListener(this);
        mnuFile.add(this.mnuClose);


        // Menu Bearbeiten
        JMenu mnuEdit = new JMenu(textEditFrmResourceBundle.getString("menu.edit"));
        mnuEdit.setMnemonic(KeyEvent.VK_B);
        mnuBar.add(mnuEdit);

        this.mnuUndo = new JMenuItem(textEditFrmResourceBundle.getString("menuItem.undo"));
        this.mnuUndo.setAccelerator(getMenuShortcut(KeyEvent.VK_Z));
        this.mnuUndo.setEnabled(false);
        this.mnuUndo.addActionListener(this);
        mnuEdit.add(this.mnuUndo);
        mnuEdit.addSeparator();

        this.mnuCut = new JMenuItem(textEditFrmResourceBundle.getString("menuItem.cut"));
        this.mnuCut.setAccelerator(getMenuShortcut(KeyEvent.VK_X));
        this.mnuCut.setEnabled(false);
        this.mnuCut.addActionListener(this);
        mnuEdit.add(this.mnuCut);

        this.mnuCopy = new JMenuItem(textEditFrmResourceBundle.getString("menuItem.copy"));
        this.mnuCopy.setAccelerator(getMenuShortcut(KeyEvent.VK_C));
        this.mnuCopy.setEnabled(false);
        this.mnuCopy.addActionListener(this);
        mnuEdit.add(this.mnuCopy);

        this.mnuPaste = new JMenuItem(textEditFrmResourceBundle.getString("menuItem.paste"));
        this.mnuPaste.setAccelerator(getMenuShortcut(KeyEvent.VK_V));
        this.mnuPaste.setEnabled(false);
        this.mnuPaste.addActionListener(this);
        mnuEdit.add(this.mnuPaste);
        mnuEdit.addSeparator();

        this.mnuSelectAll = new JMenuItem(textEditFrmResourceBundle.getString("menuItem.selectAll"));
        this.mnuSelectAll.addActionListener(this);
        mnuEdit.add(this.mnuSelectAll);
        mnuEdit.addSeparator();

        this.mnuUpper = new JMenuItem("In Gro\u00DFbuchstaben wandeln"); // TODO: i18n
        this.mnuUpper.setAccelerator(getMenuShortcut(KeyEvent.VK_U));
        this.mnuUpper.setEnabled(false);
        this.mnuUpper.addActionListener(this);
        mnuEdit.add(this.mnuUpper);

        this.mnuLower = new JMenuItem("In Kleinbuchstaben wandeln"); // TODO: i18n
        this.mnuLower.setAccelerator(getMenuShortcut(KeyEvent.VK_L));
        this.mnuLower.setEnabled(false);
        this.mnuLower.addActionListener(this);
        mnuEdit.add(this.mnuLower);
        mnuEdit.addSeparator();

        this.mnuFindAndReplace = new JMenuItem(textEditFrmResourceBundle.getString("menuItem.findAndReplace"));
        this.mnuFindAndReplace.setAccelerator(getMenuShortcut(KeyEvent.VK_F));
        this.mnuFindAndReplace.addActionListener(this);
        mnuEdit.add(this.mnuFindAndReplace);

        this.mnuReplace = new JMenuItem(textEditFrmResourceBundle.getString("menuItem.replace"));
        this.mnuReplace.setAccelerator(getMenuShortcut(KeyEvent.VK_R));
        this.mnuReplace.setEnabled(false);
        this.mnuReplace.addActionListener(this);
        mnuEdit.add(this.mnuReplace);

        this.mnuFindNext = new JMenuItem(textEditFrmResourceBundle.getString("menuItem.findNext"));
        this.mnuFindNext.setEnabled(false);
        this.mnuFindNext.setAccelerator(
                KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0));
        this.mnuFindNext.addActionListener(this);
        mnuEdit.add(this.mnuFindNext);

        this.mnuFindPrev = new JMenuItem("R\u00FCckw\u00E4rts suchen"); // TODO: i18n
        this.mnuFindPrev.setEnabled(false);
        this.mnuFindPrev.setAccelerator(
                KeyStroke.getKeyStroke(
                        KeyEvent.VK_F3,
                        InputEvent.SHIFT_DOWN_MASK));
        this.mnuFindPrev.addActionListener(this);
        mnuEdit.add(this.mnuFindPrev);
        mnuEdit.addSeparator();

        this.mnuGoto = new JMenuItem(textEditFrmResourceBundle.getString("menuItem.goto"));
        this.mnuGoto.setAccelerator(getMenuShortcut(KeyEvent.VK_G));
        this.mnuGoto.addActionListener(this);
        mnuEdit.add(this.mnuGoto);


        // Menu Programmierung
        JMenu mnuPrg = new JMenu(textEditFrmResourceBundle.getString("menu.prg"));
        mnuPrg.setMnemonic(KeyEvent.VK_P);
        mnuBar.add(mnuPrg);

        this.mnuAssemble = new JMenuItem("Assembliere"); // TODO: i18n
        this.mnuAssemble.setAccelerator(
                KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0));
        this.mnuAssemble.addActionListener(this);
        mnuPrg.add(this.mnuAssemble);

        this.mnuAssembleWith = new JMenuItem("Assembliere mit..."); // TODO: i18n
        this.mnuAssembleWith.setAccelerator(
                KeyStroke.getKeyStroke(
                        KeyEvent.VK_F7,
                        InputEvent.SHIFT_DOWN_MASK));
        this.mnuAssembleWith.addActionListener(this);
        mnuPrg.add(this.mnuAssembleWith);
        mnuPrg.addSeparator();

        this.mnuAsmPrjOpen = new JMenuItem(
                "Assembler-Projekt \u00F6ffnen..."); // TODO: i18n
        this.mnuAsmPrjOpen.addActionListener(this);
        mnuPrg.add(this.mnuAsmPrjOpen);

        this.mnuAsmPrjSave = new JMenuItem("Assembler-Projekt speichern"); // TODO: i18n
        this.mnuAsmPrjSave.addActionListener(this);
        mnuPrg.add(this.mnuAsmPrjSave);

        this.mnuAsmPrjSaveAs = new JMenuItem(
                "Assembler-Projekt speichern unter..."); // TODO: i18n
        this.mnuAsmPrjSaveAs.addActionListener(this);
        mnuPrg.add(this.mnuAsmPrjSaveAs);
        mnuPrg.addSeparator();

        this.mnuBasicParse = new JMenuItem(textEditFrmResourceBundle.getString("menuItem.basicParse"));
        this.mnuBasicParse.addActionListener(this);
        mnuPrg.add(this.mnuBasicParse);

        this.mnuBasicIntoEmu = new JMenuItem(textEditFrmResourceBundle.getString("menuItem.basicIntoEmu"));
        this.mnuBasicIntoEmu.setAccelerator(
                KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0));
        this.mnuBasicIntoEmu.addActionListener(this);
        mnuPrg.add(this.mnuBasicIntoEmu);

        this.mnuBasicIntoEmuWith = new JMenuItem(
                textEditFrmResourceBundle.getString("menuItem.basicIntoEmuOpt"));
        this.mnuBasicIntoEmuWith.setAccelerator(
                KeyStroke.getKeyStroke(
                        KeyEvent.VK_F9,
                        InputEvent.SHIFT_DOWN_MASK));
        this.mnuBasicIntoEmuWith.addActionListener(this);
        mnuPrg.add(this.mnuBasicIntoEmuWith);


        // Menu Hilfe
        JMenu mnuHelp = new JMenu(textEditFrmResourceBundle.getString("menu.help"));
        mnuHelp.setMnemonic(KeyEvent.VK_H);
        mnuBar.add(mnuHelp);

        this.mnuHelpContent = new JMenuItem(textEditFrmResourceBundle.getString("menuItem.helpContent"));
        this.mnuHelpContent.addActionListener(this);
        mnuHelp.add(this.mnuHelpContent);


        // Fensterinhalt
        setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints(
                0, 0,
                1, 1,
                1.0, 0.0,
                GridBagConstraints.WEST,
                GridBagConstraints.HORIZONTAL,
                new Insets(0, 0, 0, 0),
                0, 0);


        // Werkzeugleiste
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.setBorderPainted(false);
        toolBar.setOrientation(JToolBar.HORIZONTAL);
        toolBar.setRollover(true);
        add(toolBar, gbc);

        this.btnNew = GUIUtil.createImageButton(
                this,
                "/images/file/new.png",
                textEditFrmResourceBundle.getString("button.new"));
        toolBar.add(this.btnNew);

        this.btnOpen = GUIUtil.createImageButton(
                this,
                "/images/file/open.png",
                textEditFrmResourceBundle.getString("button.open"));
        toolBar.add(this.btnOpen);

        this.btnSave = GUIUtil.createImageButton(
                this,
                "/images/file/save.png",
                textEditFrmResourceBundle.getString("button.save"));
        toolBar.add(this.btnSave);

        this.btnPrint = GUIUtil.createImageButton(
                this,
                "/images/file/print.png",
                textEditFrmResourceBundle.getString("button.print"));
        toolBar.add(this.btnPrint);
        toolBar.addSeparator();

        this.btnUndo = GUIUtil.createImageButton(
                this,
                "/images/edit/undo.png",
                textEditFrmResourceBundle.getString("button.undo"));
        this.btnUndo.setEnabled(false);
        toolBar.add(this.btnUndo);
        toolBar.addSeparator();

        this.btnCut = GUIUtil.createImageButton(
                this,
                "/images/edit/cut.png",
                textEditFrmResourceBundle.getString("button.cut"));
        this.btnCut.setEnabled(false);
        toolBar.add(this.btnCut);

        this.btnCopy = GUIUtil.createImageButton(
                this,
                "/images/edit/copy.png",
                textEditFrmResourceBundle.getString("button.copy"));
        this.btnCopy.setEnabled(false);
        toolBar.add(this.btnCopy);

        this.btnPaste = GUIUtil.createImageButton(
                this,
                "/images/edit/paste.png",
                textEditFrmResourceBundle.getString("button.paste"));
        this.btnPaste.setEnabled(false);
        toolBar.add(this.btnPaste);
        toolBar.addSeparator();

        this.btnFindAndReplace = GUIUtil.createImageButton(
                this,
                "/images/edit/find.png",
                textEditFrmResourceBundle.getString("button.findAndReplace"));
        toolBar.add(this.btnFindAndReplace);


        // Textbereich
        this.splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        this.splitPane.setContinuousLayout(false);
        this.splitPane.setOneTouchExpandable(true);
        this.splitPane.setResizeWeight(1.0);
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;
        gbc.gridy++;
        add(this.splitPane, gbc);

        this.textArea.setRows(20);
        this.textArea.setColumns(60);
        this.textArea.setEditable(true);
        this.textArea.setMargin(new Insets(5, 5, 5, 5));
        this.textArea.addCaretListener(this);
        this.textArea.addFocusListener(this);

        JScrollPane textScrollPane = new JScrollPane(this.textArea);
        this.splitPane.setTopComponent(textScrollPane);

        this.textDoc = this.textArea.getDocument();
        if (this.textDoc != null) {
            this.textDoc.addDocumentListener(this);
            this.textDoc.addUndoableEditListener(this);
            this.mnuSave.setEnabled(false);
            this.btnSave.setEnabled(false);
        }

        this.logArea = new JTextArea(2, 60);
        this.logArea.setEditable(false);
        this.logArea.setMargin(new Insets(5, 5, 5, 5));
        this.logArea.addCaretListener(this);
        this.logArea.addFocusListener(this);
        this.logArea.addMouseListener(this);

        this.splitPane.setBottomComponent(new JScrollPane(this.logArea));


        // Statuszeile
        this.labelStatus = new JLabel();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weighty = 0.0;
        gbc.insets.left = 5;
        gbc.insets.top = 5;
        gbc.insets.bottom = 2;
        gbc.insets.right = 2;
        gbc.gridy++;
        add(this.labelStatus, gbc);
        updStatusText();


        // Fenstergroesse und -position
        setResizable(true);
        if (!GUIUtil.applyWindowSettings(this)) {
            pack();
            setLocationByPlatform(true);
        }
        this.textArea.setColumns(0);
        this.textArea.setRows(0);
        this.logArea.setColumns(0);
        this.logArea.setRows(0);


        // Drop-Ziel
        (new DropTarget(this.textArea, this)).setActive(true);
        (new DropTarget(textScrollPane, this)).setActive(true);


        // Listener fuer Zwischenablage
        Toolkit tk = getToolkit();
        if (tk != null) {
            Clipboard clipboard = tk.getSystemClipboard();
            if (clipboard != null) {
                clipboard.addFlavorListener(this);
                updPasteBtn(clipboard);
            }
        }
    }


    /* --- Aktionen --- */

    private void doFindAndReplace() {
        if (this.logArea.hasFocus()) {
            this.textFinder.openFindDlg(this.logArea);
        } else {
            this.textFinder.openFindAndReplaceDlg(this.textArea);
        }
        if (this.textFinder.hasSearchText()) {
            this.mnuFindPrev.setEnabled(true);
            this.mnuFindNext.setEnabled(true);
        }
    }


    private void doFindNext() {
        this.textFinder.findNext(
                this.logArea.hasFocus() ? this.logArea : this.textArea);
    }


    private void doFindPrev() {
        this.textFinder.findPrev(
                this.logArea.hasFocus() ? this.logArea : this.textArea);
    }


    private void doGoto() {
        Integer lineNum = null;
        try {
            lineNum = this.textArea.getLineOfOffset(
                    this.textArea.getCaretPosition()) + 1;
        } catch (BadLocationException ex) {
            lineNum = null;
        }
        lineNum = GUIUtil.askDecimal(this, "Zeile", lineNum, 1); // TODO: i18n
        if (lineNum != null) {
            gotoLine(lineNum.intValue());
        }
    }


    private void doLoadBasicMem(Integer addr) {
        if (this.jtcSys != null) {
            if (confirmDataSaved()) {
                if (addr == null) {
                    addr = GUIUtil.askHex4(
                            this,
                            LABEL_BEG_ADDR_OF_BASIC_PRG,
                            this.basicAddrFetch);
                }
                if (addr != null) {
                    String text = BasicUtil.getBasicProgramTextFromMemory(
                            this.jtcSys,
                            addr.intValue());
                    if (text != null) {
                        setText(text);
                        this.logArea.setText("");
                        this.labelStatus.setText("BASIC-Programm geladen"); // TODO: i18n
                        this.basicAddrFetch = addr;
                        this.textFile = null;
                        this.prjFile = null;
                        this.undoMngr.discardAllEdits();
                        updUndoBtn();
                        fireDataUnchanged(true);
                        updTitle();
                    } else {
                        Main.showError(
                                this,
                                String.format(
                                        textEditFrmResourceBundle.getString("error.doLoadBasicMem.noBasicProgramFound.errorText"),
                                        addr));
                    }
                }
            }
        }
    }


    private void doLower() {
        String text = this.textArea.getSelectedText();
        if (text != null) {
            if (!text.isEmpty()) {
                this.textArea.replaceSelection(text.toLowerCase());
            }
        }
    }


    private void doNew() {
        if (confirmDataSaved()) {
            setText("");
            this.logArea.setText("");
            this.textFile = null;
            this.prjFile = null;
            this.undoMngr.discardAllEdits();
            updUndoBtn();
            fireDataUnchanged(false);
            updTitle();
            updStatusText();
        }
    }


    private void doOpen() {
        if (confirmDataSaved()) {
            File file = FileDlg.showFileOpenDlg(
                    this,
                    textEditFrmResourceBundle.getString("dialog.doOpen.title"),
                    textEditFrmResourceBundle.getString("dialog.doOpen.approveBtnText"),
                    AppContext.getLastDirFile(FILE_GROUP_TEXT),
                    GUIUtil.textFileFilter,
                    GUIUtil.basicFileFilter,
                    GUIUtil.asmFileFilter);
            if (file != null) {
                if (loadFile(file)) {
                    AppContext.setLastFile(FILE_GROUP_TEXT, file);
                }
            }
        }
    }


    private void doPrgAssemble(boolean askOpt) {
        AsmOptions options = this.asmOptions;
        if (askOpt || (options == null)) {
            options = AsmOptionsDlg.open(this, options);
        }
        if (options != null) {
            this.asmOptions = options;

            final JTextArea logArea = this.logArea;
            logArea.setText("");
            AsmUtil.assemble(
                    this.textArea.getText(),
                    this.textFile,
                    options,
                    this.jtcSys,
                    new TextOutput() {
                        @Override
                        public void print(String text) {
                            logArea.append(text);
                        }

                        @Override
                        public void println() {
                            logArea.append("\n");
                        }
                    });
        }
    }


    private void doPrgAsmPrjOpen() {
        if (confirmDataSaved()) {
            File file = FileDlg.showFileOpenDlg(
                    this,
                    "Assembler-Pojekt \u00F6ffnen", // TODO: i18n
                    "\u00D6ffnen", // TODO: i18n
                    AppContext.getLastDirFile(FILE_GROUP_PROJECT),
                    GUIUtil.prjFileFilter);
            if (file != null) {
                try {
                    loadProjectFile(file);
                } catch (IOException ex) {
                    Main.showError(this, ex);
                }
            }
        }
    }


    private boolean doPrgAsmPrjSave(boolean forceFileDlg) {
        boolean rv = false;
        try {
            AsmUtil.ensureSourceFileExists(this.textFile);

            File prjFile = this.prjFile;
            if (forceFileDlg || (prjFile == null)) {
                prjFile = FileDlg.showFileSaveDlg(
                        this,
                        "Assembler-Projekt speichern", // TODO: i18n
                        AsmUtil.getProjectFilePreSelection(
                                prjFile,
                                this.textFile),
                        GUIUtil.prjFileFilter);
            }
            if (prjFile != null) {
                writeFile(this.textFile);
                AsmUtil.saveProject(prjFile, this.textFile, this.asmOptions);
                this.prjFile = prjFile;
                AppContext.setLastFile(FILE_GROUP_PROJECT, prjFile);
                fireDataUnchanged(false);
                this.labelStatus.setText("Assembler-Projekt gespeichert"); // TODO: i18n
                rv = true;
            }
        } catch (IOException ex) {
            Main.showError(this, ex);
        }
        return rv;
    }


    private void doPrgBasic(boolean intoEmu, boolean askOpt) {
        Integer addr = null;
        if (intoEmu) {
            addr = this.basicAddrTransfer;
            if (askOpt || (addr == null)) {
                addr = GUIUtil.askHex4(
                        this,
                        textEditFrmResourceBundle.getString("doPrgBasic.askHex4.basicAddr"),
                        addr != null ? addr : JTCSys.DEFAULT_BASIC_ADDR);
            }
        } else {
            addr = Integer.valueOf(JTCSys.DEFAULT_BASIC_ADDR);
        }
        if (addr != null) {
            this.logArea.setText("");

            final JTextArea logArea = this.logArea;
            byte[] codeBytes = BasicParser.parse(
                    this.jtcSys,
                    addr.intValue(),
                    this.textArea.getText(),
                    new TextOutput() {
                        @Override
                        public void print(String text) {
                            logArea.append(text);
                        }

                        @Override
                        public void println() {
                            logArea.append("\n");
                        }
                    });
            if (intoEmu && (codeBytes != null)) {
                if (codeBytes.length > 0) {
                    int a = addr.intValue();
                    for (int i = 0; i < codeBytes.length; i++) {
                        this.jtcSys.setMemByte(a++, false, codeBytes[i]);
                    }
                    this.logArea.append(
                            String.format(
                                    textEditFrmResourceBundle.getString("doPrgBasic.logArea.formatString"),
                                    addr,
                                    a - 1));
                    this.basicAddrTransfer = addr;
                }
            }
        }
    }


    private void doReplace() {
        if (GUIUtil.hasSelection(this.textArea)) {
            String replaceText = this.textFinder.getReplaceText();
            if (replaceText != null) {
                this.textArea.replaceSelection(replaceText);
            }
        }
    }


    private boolean doSave(boolean forceFileDlg) {
        boolean rv = false;
        File file = this.textFile;
        if (forceFileDlg || (file == null)) {
            file = FileDlg.showFileSaveDlg(
                    this,
                    textEditFrmResourceBundle.getString("doSave.fileSaveDlg.title"),
                    this.textFile != null ?
                            this.textFile
                            : AppContext.getLastDirFile(FILE_GROUP_TEXT),
                    GUIUtil.textFileFilter,
                    GUIUtil.basicFileFilter,
                    GUIUtil.asmFileFilter);
        }
        if (file != null) {
            try {
                writeFile(file);
                this.textFile = file;
                AppContext.setLastFile(FILE_GROUP_TEXT, file);
                fireDataUnchanged(false);
                updTitle();
                this.labelStatus.setText(textEditFrmResourceBundle.getString("label.status.fileSaved"));
                rv = true;
            } catch (IOException ex) {
                Main.showError(this, ex);
            }
        }
        return rv;
    }


    private void doUndo() {
        if (this.undoMngr.canUndo()) {
            this.undoMngr.undo();
            updUndoBtn();
        }
    }


    private void doUpper() {
        String text = this.textArea.getSelectedText();
        if (text != null) {
            if (!text.isEmpty()) {
                this.textArea.replaceSelection(text.toUpperCase());
            }
        }
    }


    /* --- private Methoden --- */

    private void docChanged(DocumentEvent e) {
        if (e.getDocument() == this.textDoc)
            setDataChanged(true);
    }


    private void fireDataUnchanged(final boolean state) {
        final JMenuItem mnuSave = this.mnuSave;
        final JButton btnSave = this.btnSave;
        setDataChanged(state);
        EventQueue.invokeLater(
                new Runnable() {
                    @Override
                    public void run() {
                        setDataChanged(false);
                        if (state) {
                            mnuSave.setEnabled(true);
                            btnSave.setEnabled(true);
                        }
                    }
                });
    }


    private boolean loadFile(File file) {
        boolean done = false;
        try {
            String fName = file.getName();
            if (fName != null) {
                fName = fName.toLowerCase();
                if (fName.endsWith(".prj")) {
                    try {
                        loadProjectFile(file);
                        done = false;
                    } catch (IOException ex) {
                    }
                }
            }
            if (!done) {
                loadTextFile(file);
                this.labelStatus.setText(textEditFrmResourceBundle.getString("label.status.fileLoaded"));
                done = true;
            }
        } catch (IOException ex) {
            Main.showError(this, ex);
        }
        return done;
    }


    private void loadProjectFile(File file) throws IOException {
        Properties props = AsmUtil.loadProject(file);
        String asmFile = props.getProperty(AsmUtil.PROP_SOURCE_FILE);
        if (asmFile == null) {
            AsmUtil.throwNoPrjFile(file);
        }
        if (asmFile.isEmpty()) {
            AsmUtil.throwNoPrjFile(file);
        }
        loadTextFile(new File(asmFile));
        this.asmOptions = AsmOptions.createOf(props);
        this.labelStatus.setText("Assembler-Projekt geladen"); // TODO: i18n
        AppContext.setLastFile(FILE_GROUP_PROJECT, file);
    }


    private void loadTextFile(File file) throws IOException {
        readFile(file);
        this.logArea.setText("");
        this.textFile = file;
        this.prjFile = null;
        this.undoMngr.discardAllEdits();
        updUndoBtn();
        fireDataUnchanged(false);
        updTitle();
    }


    private void setDataChanged(boolean state) {
        this.dataChanged = state;
        if (this.textDoc != null) {
            this.mnuSave.setEnabled(state);
            this.btnSave.setEnabled(state);
        }
    }


    private void updPasteBtn(Clipboard c) {
        boolean state = false;
        try {
            state = c.isDataFlavorAvailable(DataFlavor.stringFlavor);
        } catch (IllegalStateException ex) {
        }
        this.mnuPaste.setEnabled(state);
        this.btnPaste.setEnabled(state);
    }


    private void updTitle() {
        if (this.textFile != null) {
            setTitle(AppContext.getAppName()
                    + textEditFrmResourceBundle.getString("window.title") + ": " + this.textFile.getPath());
        } else {
            setTitle(AppContext.getAppName()
                    + textEditFrmResourceBundle.getString("window.title") + ": " + textEditFrmResourceBundle.getString("window.title.newDocument"));
        }
    }


    private void updStatusText() {
        String text = "Bereit"; // TODO: i18n
        try {
            int pos = this.textArea.getCaretPosition();
            int row = this.textArea.getLineOfOffset(pos);
            int bol = textArea.getLineStartOffset(row);
            int col = pos - bol;
            if (col > 0) {
                Document doc = textArea.getDocument();
                int tabSize = textArea.getTabSize();
                if ((doc != null) && (tabSize > 0)) {
                    Segment seg = new Segment();
                    seg.setPartialReturn(false);
                    doc.getText(bol, col, seg);
                    col = 0;
                    int ch = seg.first();
                    while (ch != CharacterIterator.DONE) {
                        if (ch == '\t') {
                            col = ((col / tabSize) + 1) * tabSize;
                        } else {
                            col++;
                        }
                        ch = seg.next();
                    }
                }
            }
            text = String.format("Z:%d S:%d", row + 1, col + 1);
        } catch (BadLocationException ignored) {}
        this.labelStatus.setText(text);
    }


    private void updUndoBtn() {
        boolean state = this.undoMngr.canUndo();
        this.mnuUndo.setEnabled(state);
        this.btnUndo.setEnabled(state);
    }
}
