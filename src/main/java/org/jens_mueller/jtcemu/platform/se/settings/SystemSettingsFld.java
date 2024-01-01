/*
 * (c) 2020 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Einstellungen zum emulierten System
 */

package jtcemu.platform.se.settings;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Properties;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import jtcemu.base.AppContext;
import jtcemu.base.JTCSys;


public class SystemSettingsFld extends JPanel implements ActionListener
{
  private SettingsFrm  settingsFrm;
  private JRadioButton rbOS2k;
  private JRadioButton rbES1988;
  private JRadioButton rbES23;
  private JRadioButton rbES40;
  private JCheckBox    cbEmuRegs80ToEF;
  private JCheckBox    cbRegInitZero;


  public SystemSettingsFld( SettingsFrm settingsFrm, JTCSys jtcSys )
  {
    this.settingsFrm = settingsFrm;
    setLayout( new GridBagLayout() );

    GridBagConstraints gbc = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.WEST,
						GridBagConstraints.NONE,
						new Insets( 5, 5, 0, 5 ),
						0, 0 );

    ButtonGroup grpOS = new ButtonGroup();

    add( new JLabel( "Betriebssystem / Grafikaufl\u00F6sung:" ), gbc );

    this.rbOS2k = new JRadioButton(
			"2 KByte BASIC-System / 64x64 Pixel" );
    grpOS.add( this.rbOS2k );
    this.rbOS2k.addActionListener( this );
    gbc.insets.top  = 0;
    gbc.insets.left = 50;
    gbc.gridy++;
    add( this.rbOS2k, gbc );

    this.rbES1988 = new JRadioButton(
			"4 KByte EMR-ES 1988 / 64x64 Pixel" );
    grpOS.add( this.rbES1988 );
    this.rbES1988.addActionListener( this );
    gbc.gridy++;
    add( this.rbES1988, gbc );

    this.rbES23 = new JRadioButton(
			"4 KByte ES 2.3 / 128x128 Pixel" );
    grpOS.add( this.rbES23 );
    this.rbES23.addActionListener( this );
    gbc.gridy++;
    add( this.rbES23, gbc );

    this.rbES40 = new JRadioButton(
			"6 KByte ES 4.0 / 320x192 Pixel, 16 Farben" );
    grpOS.add( this.rbES40 );
    this.rbES40.addActionListener( this );
    gbc.gridy++;
    add( this.rbES40, gbc );

    switch( jtcSys.getOSType() ) {
      case ES1988:
	this.rbES1988.setSelected( true );
	break;
      case ES23:
	this.rbES23.setSelected( true );
	break;
      case ES40:
	this.rbES40.setSelected( true );
	break;
      default:
	this.rbOS2k.setSelected( true );
    }

    this.cbEmuRegs80ToEF = new JCheckBox(
		"Register %80 bis %EF emulieren",
		jtcSys.getEmulateRegisters80ToEF() );
    this.cbEmuRegs80ToEF.addActionListener( this );
    gbc.insets.top  = 10;
    gbc.insets.left = 5;
    gbc.gridy++;
    add( this.cbEmuRegs80ToEF, gbc );

    this.cbRegInitZero = new JCheckBox(
		"Register mit %00 statt mit Zufallswerten initialisieren",
		jtcSys.getZ8().isRegInitZero() );
    this.cbRegInitZero.addActionListener( this );
    gbc.insets.top    = 0;
    gbc.insets.bottom = 5;
    gbc.gridy++;
    add( this.cbRegInitZero, gbc );
  }


  public JTCSys.OSType applyInput( Properties props )
  {
    JTCSys.OSType osType = getSelectedOSType();
    props.setProperty(
		AppContext.getPropPrefix() + JTCSys.PROP_OS,
		osType.toString() );
    props.setProperty(
		AppContext.getPropPrefix() + JTCSys.PROP_Z8_REGS_80_TO_EF,
		Boolean.toString( this.cbEmuRegs80ToEF.isSelected() ) );
    props.setProperty(
		AppContext.getPropPrefix() + JTCSys.PROP_Z8_REG_INIT_ZERO,
		Boolean.toString( this.cbRegInitZero.isSelected() ) );
    return osType;
  }


  public JTCSys.OSType getSelectedOSType()
  {
    JTCSys.OSType osType = JTCSys.OSType.OS2K;
    if( this.rbES1988.isSelected() ) {
      osType = JTCSys.OSType.ES1988;
    } else if( this.rbES23.isSelected() ) {
      osType = JTCSys.OSType.ES23;
    } else if( this.rbES40.isSelected() ) {
      osType = JTCSys.OSType.ES40;
    }
    return osType;
  }


	/* --- ActionListener --- */

  @Override
  public void actionPerformed( ActionEvent e )
  {
    Object src = e.getSource();
    if( (src == this.rbOS2k)
	|| (src == this.rbES1988)
	|| (src == this.rbES23)
	|| (src == this.rbES40) )
    {
      this.settingsFrm.selectedOSTypeChanged( getSelectedOSType() );
      this.settingsFrm.setDataChanged();
    }
    else if( (src == this.cbEmuRegs80ToEF)
	     || (src == this.cbRegInitZero) )
    {
      this.settingsFrm.setDataChanged();
    }
  }
}
