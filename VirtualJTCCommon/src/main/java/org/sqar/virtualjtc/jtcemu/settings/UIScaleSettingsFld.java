/*
 * (c) 2020 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Einstellungen zur Skalierung der Fenster
 */

package org.sqar.virtualjtc.jtcemu.settings;

import org.sqar.virtualjtc.jtcemu.base.AppContext;
import org.sqar.virtualjtc.jtcemu.base.JTCUtil;
import org.sqar.virtualjtc.jtcemu.base.UserInputException;
import org.sqar.virtualjtc.jtcemu.Main;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;


public class UIScaleSettingsFld extends JPanel implements ActionListener {
    private static final Locale locale = Locale.getDefault();
    private static final ResourceBundle uiScaleSettingsFldResourceBundle = ResourceBundle.getBundle("UIScaleSettingsFld", locale);

    private static final String DEFAULT_SCALE_ITEM = "100 %";

    private static final String[] SCALE_ITEMS_WIN = {
            "75 %", "100 %", "125 %", "150 %", "175 %", "200 %"};

    private static final String[] SCALE_ITEMS_OTHER = {"100 %", "200 %"};

    private SettingsFrm settingsFrm;
    private JRadioButton rbScaleNone;
    private JRadioButton rbScaleDefault;
    private JRadioButton rbScaleFix;
    private JComboBox<String> comboScale;


    public UIScaleSettingsFld(SettingsFrm settingsFrm) {
        this.settingsFrm = settingsFrm;
        setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints(
                0, 0,
                GridBagConstraints.REMAINDER, 1,
                0.0, 0.0,
                GridBagConstraints.WEST,
                GridBagConstraints.NONE,
                new Insets(5, 5, 0, 5),
                0, 0);

        add(
                new JLabel("Ab Java 9 k\u00F6nnen die " // TODO: i18n
                        + AppContext.getAppName()
                        + "-Fenster und Fensterinhalte skaliert werden:"),
                gbc);

        ButtonGroup grpScale = new ButtonGroup();

        this.rbScaleNone = new JRadioButton("Keine Skalierung"); // TODO: i18n
        grpScale.add(this.rbScaleNone);
        gbc.insets.left = 50;
        gbc.gridy++;
        add(this.rbScaleNone, gbc);

        this.rbScaleDefault = new JRadioButton(
                "Skalierung entsprechend Java-Standard" // TODO: i18n
                        + " (automatische Skalierung bei"
                        + " hochaufl\u00F6senden Bildschirmen)");
        grpScale.add(this.rbScaleDefault);
        gbc.insets.top = 0;
        gbc.gridy++;
        add(this.rbScaleDefault, gbc);

        this.rbScaleFix = new JRadioButton(
                "Fest eingestellte Skalierung:"); // TODO: i18n
        grpScale.add(this.rbScaleFix);
        gbc.gridwidth = 1;
        gbc.gridy++;
        add(this.rbScaleFix, gbc);

        this.comboScale = new JComboBox<>(
                JTCUtil.isWinOS() ? SCALE_ITEMS_WIN : SCALE_ITEMS_OTHER);
        this.comboScale.setEditable(true);
        gbc.insets.top = 0;
        gbc.insets.left = 5;
        gbc.gridx++;
        add(this.comboScale, gbc);

        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.insets.top = 20;
        gbc.gridx = 0;
        if (!JTCUtil.isWinOS()) {
            gbc.gridy++;
            add(
                    new JLabel("Achtung! Auf diesem Betriebssystem hier" // TODO: i18n
                            + " funktionieren m\u00F6glicherweise nur Skalierungen"
                            + " in 100%-Schritten."),
                    gbc);
        }
        gbc.gridy++;
        add(
                new JLabel("Achtung! Die Einstellungen auf dieser Unterseite" // TODO: i18n
                        + " werden nur wirksam, wenn Sie die Einstellungen"
                        + " speichern"),
                gbc);
        gbc.insets.top = 0;
        gbc.gridy++;
        add(
                new JLabel("und anschlie\u00DFend " // TODO: i18n
                        + AppContext.getAppName()
                        + " neu starten und dabei Java 9 oder eine h\u00F6here"
                        + " Java-Version verwenden."),
                gbc);


        // Vorbelegung
        boolean done = false;
        String item = null;
        String text = AppContext.getProperty(Main.PROP_UI_SCALE);
        if (text != null) {
            text = text.trim();
            if (text.equalsIgnoreCase(Main.VALUE_NONE)) {
                this.rbScaleNone.setSelected(true);
                done = true;
            } else {
                item = parseAndPreparePercentText(text);
                if (item != null) {
                    setSelectedScaleItem(item);
                    this.rbScaleFix.setSelected(true);
                    done = true;
                }
            }
        }
        if (!done) {
            this.rbScaleDefault.setSelected(true);
        }
        if (item == null) {
            setSelectedScaleItem(DEFAULT_SCALE_ITEM);
        }
        updComboScaleEnabled();


        // Listener
        this.rbScaleNone.addActionListener(this);
        this.rbScaleDefault.addActionListener(this);
        this.rbScaleFix.addActionListener(this);
        this.comboScale.addActionListener(this);
    }


    public void applyInput(Properties props) throws UserInputException {
        String value = "default";
        if (this.rbScaleNone.isSelected()) {
            value = Main.VALUE_NONE;
        } else if (this.rbScaleFix.isSelected()) {
            value = parseComboScale();
            if (value == null) {
                throw new UserInputException(
                        "Skalierungsfaktor in Prozent: Ung\u00FCltiger Wert"); // TODO: i18n
            }
        }
        props.setProperty(
                AppContext.getPropPrefix() + Main.PROP_UI_SCALE,
                value);
    }


    /* --- ActionListener --- */

    @Override
    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();
        if ((src == this.rbScaleNone)
                || (src == this.rbScaleDefault)
                || (src == this.rbScaleFix)) {
            updComboScaleEnabled();
            this.settingsFrm.setDataChanged();
        } else if (src == this.comboScale) {
            parseComboScale();
            this.settingsFrm.setDataChanged();
        }
    }


    /* --- private Methoden --- */

    private String parseAndPreparePercentText(String text) {
        String rv = null;
        Integer v = Main.parseUIScalePercentText(text);
        if (v != null) {
            rv = String.format("%d %%", v);
        }
        return rv;
    }


    private String parseComboScale() {
        String rv = null;
        Object o = this.comboScale.getSelectedItem();
        if (o != null) {
            rv = parseAndPreparePercentText(o.toString());
            if (rv != null) {
                setSelectedScaleItem(rv);
            }
        }
        return rv;
    }


    private void setSelectedScaleItem(String item) {
        this.comboScale.removeActionListener(this);
        this.comboScale.setSelectedItem(item);
        this.comboScale.addActionListener(this);
    }


    private void updComboScaleEnabled() {
        this.comboScale.setEnabled(this.rbScaleFix.isSelected());
    }
}
