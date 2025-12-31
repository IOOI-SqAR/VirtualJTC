/*
 * (c) 2007-2010 Jens Mueller
 * (c) 2017 Lars Sonchocky-Helldorf
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Dialog fuer Suchen & Ersetzen
 */

package org.sqar.virtualjtc.jtcemu.tools;

import java.awt.*;
import java.awt.event.*;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.regex.*;
import javax.swing.*;

import org.sqar.virtualjtc.jtcemu.Main;
import org.sqar.virtualjtc.jtcemu.base.*;


public class FindAndReplaceDlg extends BaseDlg implements ActionListener {
    private static final Locale locale = Locale.getDefault();
    private static final ResourceBundle findAndReplaceDlgResourceBundle = ResourceBundle.getBundle("FindAndReplaceDlg", locale);

    public enum Action {FIND_NEXT, REPLACE_ALL, CANCEL}

    ;

    private Action action;
    private String searchText;
    private String replaceText;
    private JTextField fldSearchText;
    private JTextField fldReplaceText;
    private JCheckBox tglCaseSensitive;
    private JButton btnFind;
    private JButton btnReplaceAll;
    private JButton btnCancel;


    public static FindAndReplaceDlg createFindDlg(
            Window owner,
            String searchText) {
        return new FindAndReplaceDlg(owner, searchText, null, false, null);
    }


    public static FindAndReplaceDlg createFindAndReplaceDlg(
            Window owner,
            String searchText,
            boolean caseSensitive,
            String replaceText) {
        return new FindAndReplaceDlg(
                owner,
                searchText,
                caseSensitive,
                true,
                replaceText);
    }


    public Action getAction() {
        return this.action;
    }


    public boolean getCaseSensitive() {
        return this.tglCaseSensitive != null ?
                this.tglCaseSensitive.isSelected()
                : false;
    }


    public String getReplaceText() {
        return this.replaceText != null ? this.replaceText : "";
    }


    public String getSearchText() {
        return this.searchText != null ? this.searchText : "";
    }


    /* --- Konstruktor --- */

    private FindAndReplaceDlg(
            Window owner,
            String searchText,
            Boolean caseSensitive,
            boolean withReplace,
            String replaceText) {
        super(owner);

        setTitle(findAndReplaceDlgResourceBundle.getString("window.title"));

        this.action = Action.CANCEL;
        this.searchText = null;
        this.replaceText = null;


        // Layout
        setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints(
                0, 0,
                1, 1,
                0.0, 0.0,
                GridBagConstraints.NORTHEAST,
                GridBagConstraints.NONE,
                new Insets(5, 0, 5, 5),
                0, 0);


        // Eingabebereich
        add(new JLabel(findAndReplaceDlgResourceBundle.getString("label.panelInput.searchFor")), gbc);

        this.fldSearchText = new JTextField();
        if (searchText != null) {
            this.fldSearchText.setText(searchText);
        }
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridx++;
        add(this.fldSearchText, gbc);

        if (withReplace) {
            gbc.anchor = GridBagConstraints.EAST;
            gbc.fill = GridBagConstraints.NONE;
            gbc.weightx = 0.0;
            gbc.gridx = 0;
            gbc.gridy++;
            add(new JLabel(findAndReplaceDlgResourceBundle.getString("label.panelInput.replaceBy")), gbc);

            this.fldReplaceText = new JTextField();
            if (replaceText != null) {
                this.fldReplaceText.setText(replaceText);
            }
            gbc.anchor = GridBagConstraints.WEST;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1.0;
            gbc.gridx++;
            add(this.fldReplaceText, gbc);
        } else {
            this.fldReplaceText = null;
        }


        // Toggle fuer Gross-/Kleinschreibung
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0.0;
        if (caseSensitive != null) {
            gbc.gridy++;
            this.tglCaseSensitive = new JCheckBox(
                    findAndReplaceDlgResourceBundle.getString("button.caseSensitive"),
                    caseSensitive);
            add(this.tglCaseSensitive, gbc);
        } else {
            this.tglCaseSensitive = null;
        }


        // Schaltflaechen
        JPanel panelBtn = new JPanel();
        panelBtn.setLayout(new GridLayout(1, withReplace ? 3 : 2, 5, 5));

        this.btnFind = new JButton(findAndReplaceDlgResourceBundle.getString("button.find"));
        panelBtn.add(this.btnFind);

        if (withReplace) {
            this.btnReplaceAll = new JButton(findAndReplaceDlgResourceBundle.getString("button.replaceAll"));
            panelBtn.add(this.btnReplaceAll);
        } else {
            this.btnReplaceAll = null;
        }

        this.btnCancel = new JButton(findAndReplaceDlgResourceBundle.getString("button.cancel"));
        panelBtn.add(this.btnCancel);

        gbc.anchor = GridBagConstraints.CENTER;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.gridx = 0;
        gbc.gridy++;
        add(panelBtn, gbc);


        // Listener
        this.fldSearchText.addActionListener(this);
        if (this.fldReplaceText != null) {
            this.fldReplaceText.addActionListener(this);
        }
        this.btnFind.addActionListener(this);
        if (this.btnReplaceAll != null) {
            this.btnReplaceAll.addActionListener(this);
        }
        this.btnCancel.addActionListener(this);


        // Fenstergroesse und -position
        pack();
        setParentCentered();
        setResizable(false);
    }


    /* --- ActionListener --- */

    @Override
    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();
        if ((src == this.fldReplaceText) || (src == this.btnFind)) {
            doFind(Action.FIND_NEXT);
        } else if (src == this.fldSearchText) {
            if (this.fldReplaceText != null) {
                this.fldReplaceText.requestFocus();
            } else {
                doFind(Action.FIND_NEXT);
            }
        } else if (src == this.btnReplaceAll) {
            doFind(Action.REPLACE_ALL);
        } else if (src == this.btnCancel) {
            doClose();
        }
    }


    @Override
    public void windowOpened(WindowEvent e) {
        if (e.getWindow() == this)
            this.fldSearchText.requestFocus();
    }


    /* --- private Methoden --- */

    private void doFind(Action action) {
        this.searchText = this.fldSearchText.getText();
        if (this.fldReplaceText != null) {
            this.replaceText = this.fldReplaceText.getText();
        }
        if (this.searchText != null) {
            if (!this.searchText.isEmpty()) {
                this.action = action;
                doClose();
            }
        }
    }
}
