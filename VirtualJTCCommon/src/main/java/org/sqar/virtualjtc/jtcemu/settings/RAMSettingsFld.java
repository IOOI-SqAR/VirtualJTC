/*
 * (c) 2020 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Einstellungen zum emulierten RAM
 */

package org.sqar.virtualjtc.jtcemu.settings;

import org.sqar.virtualjtc.jtcemu.base.AppContext;
import org.sqar.virtualjtc.jtcemu.base.JTCSys;
import org.sqar.virtualjtc.jtcemu.base.JTCUtil;
import org.sqar.virtualjtc.jtcemu.base.UserInputException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;


public class RAMSettingsFld extends JPanel implements ActionListener {
    private static final Locale locale = Locale.getDefault();
    private static final ResourceBundle ramSettingsFldResourceBundle = ResourceBundle.getBundle("RAMSettingsFld", locale);

    private SettingsFrm settingsFrm;
    private JRadioButton rbRam1k;
    private JRadioButton rbRam2k;
    private JRadioButton rbRam8k;
    private JRadioButton rbRam32k;
    private JRadioButton rbRam64k;
    private JCheckBox cbRamInitZero;


    public RAMSettingsFld(SettingsFrm settingsFrm, JTCSys jtcSys) {
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

        ButtonGroup grpRAM = new ButtonGroup();
        int ramSize = jtcSys.getRAMSize();

        this.rbRam1k = new JRadioButton(
                ramSettingsFldResourceBundle.getString("button.ram.1k"),
                ramSize == 0x0400);
        grpRAM.add(this.rbRam1k);
        this.rbRam1k.addActionListener(this);
        add(this.rbRam1k, gbc);

        this.rbRam2k = new JRadioButton(
                ramSettingsFldResourceBundle.getString("button.ram.2k"),
                ramSize == 0x0800);
        grpRAM.add(this.rbRam2k);
        this.rbRam2k.addActionListener(this);
        gbc.insets.top = 0;
        gbc.gridy++;
        add(this.rbRam2k, gbc);

        this.rbRam8k = new JRadioButton(
                ramSettingsFldResourceBundle.getString("button.ram.8k"),
                ramSize == 0x2000);
        grpRAM.add(this.rbRam8k);
        this.rbRam8k.addActionListener(this);
        gbc.gridy++;
        add(this.rbRam8k, gbc);

        this.rbRam32k = new JRadioButton(
                ramSettingsFldResourceBundle.getString("button.ram.32k"),
                ramSize == 0x8000);
        grpRAM.add(this.rbRam32k);
        this.rbRam32k.addActionListener(this);
        gbc.gridy++;
        add(this.rbRam32k, gbc);

        this.rbRam64k = new JRadioButton(
                ramSettingsFldResourceBundle.getString("button.ram.64k"),
                ramSize == 0x10000);
        grpRAM.add(this.rbRam64k);
        this.rbRam64k.addActionListener(this);
        gbc.gridy++;
        add(this.rbRam64k, gbc);

        this.cbRamInitZero = new JCheckBox(
                "RAM mit %00 statt mit Zufallswerten initialisieren", // TODO: i18n
                JTCSys.isRamInitZero());
        this.cbRamInitZero.addActionListener(this);
        gbc.insets.top = 10;
        gbc.insets.bottom = 5;
        gbc.gridy++;
        add(this.cbRamInitZero, gbc);
    }


    public void applyInput(Properties props) throws UserInputException {
        int ramSize = 0x10000;
        if (this.rbRam1k.isSelected()) {
            ramSize = 0x0400;
        } else if (this.rbRam2k.isSelected()) {
            ramSize = 0x0800;
        } else if (this.rbRam8k.isSelected()) {
            ramSize = 0x2000;
        } else if (this.rbRam32k.isSelected()) {
            ramSize = 0x8000;
        }
        JTCUtil.checkRAMSize(this.settingsFrm.getSelectedOSType(), ramSize);
        props.setProperty(
                AppContext.getPropPrefix() + JTCSys.PROP_RAM_SIZE,
                String.format("%dK", ramSize / 1024));
        props.setProperty(
                AppContext.getPropPrefix() + JTCSys.PROP_RAM_INIT_ZERO,
                Boolean.toString(this.cbRamInitZero.isSelected()));
    }


    /* --- ActionListener --- */

    @Override
    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();
        if ((src == this.rbRam1k)
                || (src == this.rbRam2k)
                || (src == this.rbRam8k)
                || (src == this.rbRam32k)
                || (src == this.rbRam64k)
                || (src == this.cbRamInitZero)) {
            this.settingsFrm.setDataChanged();
        }
    }
}
