/*
 * (c) 2020 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Einstellungen fuer Bestaetigungen
 */

package org.sqar.virtualjtc.jtcemu.settings;

import org.sqar.virtualjtc.jtcemu.base.AppContext;
import org.sqar.virtualjtc.jtcemu.base.JTCUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;


public class ConfirmSettingsFld extends JPanel implements ActionListener {
    private static final Locale locale = Locale.getDefault();
    private static final ResourceBundle confirmSettingsFldResourceBundle = ResourceBundle.getBundle("ConfirmSettingsFld", locale);

    private SettingsFrm settingsFrm;
    private JCheckBox cbConfirmPowerOn;
    private JCheckBox cbConfirmReset;
    private JCheckBox cbConfirmQuit;


    public ConfirmSettingsFld(SettingsFrm settingsFrm) {
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

        add(
                new JLabel(confirmSettingsFldResourceBundle.getString("tabs.confirm.listOfConfirmationsLabel")),
                gbc);

        this.cbConfirmPowerOn = new JCheckBox(
                confirmSettingsFldResourceBundle.getString("button.confirm.init"),
                AppContext.getBooleanProperty(
                        JTCUtil.PROP_CONFIRM_POWER_ON,
                        true));
        this.cbConfirmPowerOn.addActionListener(this);
        gbc.insets.top = 0;
        gbc.insets.left = 50;
        gbc.gridy++;
        add(this.cbConfirmPowerOn, gbc);

        this.cbConfirmReset = new JCheckBox(
                confirmSettingsFldResourceBundle.getString("button.confirm.reset"),
                AppContext.getBooleanProperty(
                        JTCUtil.PROP_CONFIRM_RESET,
                        true));
        this.cbConfirmReset.addActionListener(this);
        gbc.gridy++;
        add(this.cbConfirmReset, gbc);

        this.cbConfirmQuit = new JCheckBox(
                confirmSettingsFldResourceBundle.getString("button.confirm.quit"),
                AppContext.getBooleanProperty(
                        JTCUtil.PROP_CONFIRM_QUIT,
                        true));
        this.cbConfirmQuit.addActionListener(this);
        gbc.insets.bottom = 5;
        gbc.gridy++;
        add(this.cbConfirmQuit, gbc);
    }


    public void applyInput(Properties props) {
        JTCUtil.applyConfirmSettings(
                props,
                this.cbConfirmPowerOn.isSelected(),
                this.cbConfirmReset.isSelected(),
                this.cbConfirmQuit.isSelected());
    }


    /* --- ActionListener --- */

    @Override
    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();
        if ((src == this.cbConfirmPowerOn)
                || (src == this.cbConfirmReset)
                || (src == this.cbConfirmQuit)) {
            this.settingsFrm.setDataChanged();
        }
    }
}
