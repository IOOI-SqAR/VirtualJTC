/*
 * (c) 2014-2020 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Node fuer die Systemeinstellungen
 */

package org.jens_mueller.jtcemu.platform.fx.settings;

import java.util.Properties;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import org.jens_mueller.jtcemu.base.AppContext;
import org.jens_mueller.jtcemu.base.JTCSys;


public class SystemSettingsNode extends ScrollPane
{
  private SettingsNode settingsNode;
  private JTCSys       jtcSys;
  private RadioButton  btnOS2k;
  private RadioButton  btnES1988;
  private RadioButton  btnES23;
  private RadioButton  btnES40;
  private CheckBox     btnEmuRegs80ToEF;
  private CheckBox     btnRegsInitZero;


  public SystemSettingsNode( final SettingsNode settingsNode, JTCSys jtcSys )
  {
    this.settingsNode = settingsNode;
    this.jtcSys       = jtcSys;

    VBox vbox = new VBox( 5 );
    vbox.setAlignment( Pos.CENTER_LEFT );
    vbox.setPadding( new Insets( 10 ) );
    setContent( vbox );

    // Betriebssystem
    vbox.getChildren().add(
		new Label( "Betriebssystem / Grafikaufl\u00F6sung:" ) );

    ToggleGroup grpOS = new ToggleGroup();

    this.btnOS2k = new RadioButton( "2 KByte BASIC-System / 64x64 Pixel" );
    this.btnOS2k.setToggleGroup( grpOS );
    VBox.setMargin( this.btnOS2k, new Insets( 0, 0, 0, 50 ) );
    vbox.getChildren().add( this.btnOS2k );

    this.btnES1988 = new RadioButton( "4 KByte EMR-ES 1988 / 64x64 Pixel" );
    this.btnES1988.setToggleGroup( grpOS );
    VBox.setMargin( this.btnES1988, new Insets( 0, 0, 0, 50 ) );
    vbox.getChildren().add( this.btnES1988 );

    this.btnES23 = new RadioButton( "4 KByte ES 2.3 / 128x128 Pixel" );
    this.btnES23.setToggleGroup( grpOS );
    VBox.setMargin( this.btnES23, new Insets( 0, 0, 0, 50 ) );
    vbox.getChildren().add( this.btnES23 );

    this.btnES40 = new RadioButton(
			"6 KByte ES 4.0 / 320x192 Pixel, 16 Farben" );
    this.btnES40.setToggleGroup( grpOS );
    VBox.setMargin( this.btnES40, new Insets( 0, 0, 0, 50 ) );
    vbox.getChildren().add( this.btnES40 );

    // Register 80h bis EFh
    this.btnEmuRegs80ToEF = new CheckBox( "Register %80 bis %EF emulieren" );
    VBox.setMargin( this.btnEmuRegs80ToEF, new Insets( 20, 0, 0, 0 ) );
    vbox.getChildren().add( this.btnEmuRegs80ToEF );

    // Initialisierungswert der Register
    this.btnRegsInitZero = new CheckBox(
		"Register mit %00 statt mit Zufallswerten initialisieren" );
    vbox.getChildren().add( this.btnRegsInitZero );

    // sonstiges
    updSettings();
    this.btnOS2k.setOnAction( e->osSelectionChanged() );
    this.btnES1988.setOnAction( e->osSelectionChanged() );
    this.btnES23.setOnAction( e->osSelectionChanged() );
    this.btnES40.setOnAction( e->osSelectionChanged() );
    this.btnEmuRegs80ToEF.setOnAction(
			e->this.settingsNode.setDataChanged() );
    this.btnRegsInitZero.setOnAction(
			e->this.settingsNode.setDataChanged() );
  }


  public JTCSys.OSType applySettingsTo( Properties props )
  {
    JTCSys.OSType osType = getSelectedOS();
    props.setProperty(
		AppContext.getPropPrefix() + JTCSys.PROP_OS,
		osType.toString() );
    props.setProperty(
		AppContext.getPropPrefix() + JTCSys.PROP_Z8_REGS_80_TO_EF,
		Boolean.toString( this.btnEmuRegs80ToEF.isSelected() ) );
    props.setProperty(
		AppContext.getPropPrefix() + JTCSys.PROP_Z8_REG_INIT_ZERO,
		Boolean.toString( this.btnRegsInitZero.isSelected() ) );
    return osType;
  }


  public JTCSys.OSType getSelectedOS()
  {
    JTCSys.OSType osType = JTCSys.OSType.OS2K;
    if( this.btnES40.isSelected() ) {
      osType = JTCSys.OSType.ES40;
    } else if( this.btnES23.isSelected() ) {
      osType = JTCSys.OSType.ES23;
    } else if( this.btnES1988.isSelected() ) {
      osType = JTCSys.OSType.ES1988;
    }
    return osType;
  }


  public void updSettings()
  {
    if( this.jtcSys != null ) {
      switch( this.jtcSys.getOSType() ) {
	case ES1988:
	  this.btnES1988.setSelected( true );
	  break;
	case ES23:
	  this.btnES23.setSelected( true );
	  break;
	case ES40:
	  this.btnES40.setSelected( true );
	  break;
	default:
	  this.btnOS2k.setSelected( true );
      }
      this.btnEmuRegs80ToEF.setSelected(
			this.jtcSys.getEmulateRegisters80ToEF() );
      this.btnRegsInitZero.setSelected(
			this.jtcSys.getZ8().isRegInitZero() );
    }
  }


	/* --- private Methoden --- */

  private void osSelectionChanged()
  {
    this.settingsNode.osSelectionChanged( getSelectedOS() );
  }
}
