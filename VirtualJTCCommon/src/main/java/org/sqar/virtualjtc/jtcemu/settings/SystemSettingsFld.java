/*
 * (c) 2020 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Einstellungen zum emulierten System
 */

package org.sqar.virtualjtc.jtcemu.settings;

import org.sqar.virtualjtc.jtcemu.base.AppContext;
import org.sqar.virtualjtc.jtcemu.base.JTCSys;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;


public class SystemSettingsFld extends JPanel implements ActionListener {
    private static final Locale locale = Locale.getDefault();
    private static final ResourceBundle systemSettingsFldResourceBundle = ResourceBundle.getBundle("SystemSettingsFld", locale);

    private SettingsFrm settingsFrm;
    private JRadioButton rbOS2k;
    private JRadioButton rbES1988;
    private JRadioButton rbES23;
    private JRadioButton rbES40;
    private JCheckBox cbEmuRegs80ToEF;
    private JCheckBox cbRegInitZero;


    public SystemSettingsFld(SettingsFrm settingsFrm, JTCSys jtcSys) {
        this.settingsFrm = settingsFrm;
        setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints(
                0, 0,
                1, 1,
                0.0, 0.0,
                GridBagConstraints.WEST,
                GridBagConstraints.NONE,
                new Insets(5, 5, 0, 5),
                0, 0);

        ButtonGroup grpOS = new ButtonGroup();

        add(new JLabel(systemSettingsFldResourceBundle.getString("tabs.sys.osVersionScreenResolution")), gbc);

        this.rbOS2k = new JRadioButton(
                systemSettingsFldResourceBundle.getString("button.sys.OS2k"));
        grpOS.add(this.rbOS2k);
        this.rbOS2k.addActionListener(this);
        gbc.insets.top = 0;
        gbc.insets.left = 50;
        gbc.gridy++;
        add(this.rbOS2k, gbc);

        this.rbES1988 = new JRadioButton(
                systemSettingsFldResourceBundle.getString("button.sys.ES1988"));
        grpOS.add(this.rbES1988);
        this.rbES1988.addActionListener(this);
        gbc.gridy++;
        add(this.rbES1988, gbc);

        this.rbES23 = new JRadioButton(
                systemSettingsFldResourceBundle.getString("button.sys.ES23"));
        grpOS.add(this.rbES23);
        this.rbES23.addActionListener(this);
        gbc.gridy++;
        add(this.rbES23, gbc);

        this.rbES40 = new JRadioButton(
                systemSettingsFldResourceBundle.getString("button.sys.ES40"));
        grpOS.add(this.rbES40);
        this.rbES40.addActionListener(this);
        gbc.gridy++;
        add(this.rbES40, gbc);

        switch (jtcSys.getOSType()) {
            case ES1988:
                this.rbES1988.setSelected(true);
                break;
            case ES23:
                this.rbES23.setSelected(true);
                break;
            case ES40:
                this.rbES40.setSelected(true);
                break;
            default:
                this.rbOS2k.setSelected(true);
        }

        this.cbEmuRegs80ToEF = new JCheckBox(
                systemSettingsFldResourceBundle.getString("button.sys.emuReg80ToEF"),
                jtcSys.getEmulateRegisters80ToEF());
        this.cbEmuRegs80ToEF.addActionListener(this);
        gbc.insets.top = 10;
        gbc.insets.left = 5;
        gbc.gridy++;
        add(this.cbEmuRegs80ToEF, gbc);

        this.cbRegInitZero = new JCheckBox(
                "Register mit %00 statt mit Zufallswerten initialisieren", // TODO: i18n
                jtcSys.getZ8().isRegInitZero());
        this.cbRegInitZero.addActionListener(this);
        gbc.insets.top = 0;
        gbc.insets.bottom = 5;
        gbc.gridy++;
        add(this.cbRegInitZero, gbc);
    }


    public JTCSys.OSType applyInput(Properties props) {
        JTCSys.OSType osType = getSelectedOSType();
        props.setProperty(
                AppContext.getPropPrefix() + JTCSys.PROP_OS,
                osType.toString());
        props.setProperty(
                AppContext.getPropPrefix() + JTCSys.PROP_Z8_REGS_80_TO_EF,
                Boolean.toString(this.cbEmuRegs80ToEF.isSelected()));
        props.setProperty(
                AppContext.getPropPrefix() + JTCSys.PROP_Z8_REG_INIT_ZERO,
                Boolean.toString(this.cbRegInitZero.isSelected()));
        return osType;
    }


    public JTCSys.OSType getSelectedOSType() {
        JTCSys.OSType osType = JTCSys.OSType.OS2K;
        if (this.rbES1988.isSelected()) {
            osType = JTCSys.OSType.ES1988;
        } else if (this.rbES23.isSelected()) {
            osType = JTCSys.OSType.ES23;
        } else if (this.rbES40.isSelected()) {
            osType = JTCSys.OSType.ES40;
        }
        return osType;
    }


    /* --- ActionListener --- */

    @Override
    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();
        if ((src == this.rbOS2k)
                || (src == this.rbES1988)
                || (src == this.rbES23)
                || (src == this.rbES40)) {
            this.settingsFrm.selectedOSTypeChanged(getSelectedOSType());
            this.settingsFrm.setDataChanged();
        } else if ((src == this.cbEmuRegs80ToEF)
                || (src == this.cbRegInitZero)) {
            this.settingsFrm.setDataChanged();
        }
    }
}
