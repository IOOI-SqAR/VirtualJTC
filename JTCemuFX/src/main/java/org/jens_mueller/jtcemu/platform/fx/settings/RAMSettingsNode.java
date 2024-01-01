/*
 * (c) 2014-2020 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Node fuer die RAM-Einstellungen
 */

package org.jens_mueller.jtcemu.platform.fx.settings;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;
import org.jens_mueller.jtcemu.base.AppContext;
import org.jens_mueller.jtcemu.base.JTCSys;
import org.jens_mueller.jtcemu.base.JTCUtil;
import org.jens_mueller.jtcemu.base.UserInputException;

import java.util.Properties;


public class RAMSettingsNode extends ScrollPane
{
  private JTCSys      jtcSys;
  private RadioButton btnRAM1k;
  private RadioButton btnRAM2k;
  private RadioButton btnRAM8k;
  private RadioButton btnRAM32k;
  private RadioButton btnRAM64k;
  private CheckBox    btnRAMInitZero;


  public RAMSettingsNode(final SettingsNode settingsNode, JTCSys jtcSys )
  {
    this.jtcSys = jtcSys;

    ToggleGroup grpRAM = new ToggleGroup();

    this.btnRAM1k = new RadioButton(
			"1 KByte RAM (%E000-%E0FF und %FD00-%FFFF)" );
    this.btnRAM1k.setToggleGroup( grpRAM );

    this.btnRAM2k = new RadioButton(
			"2 KByte RAM (%E000-%E4FF und %FD00-%FFFF)" );
    this.btnRAM2k.setToggleGroup( grpRAM );

    this.btnRAM8k = new RadioButton( "8 KByte RAM (%E000-%FFFF)" );
    this.btnRAM8k.setToggleGroup( grpRAM );

    this.btnRAM32k = new RadioButton( "32 KByte RAM (%8000-%FFFF)" );
    this.btnRAM32k.setToggleGroup( grpRAM );

    this.btnRAM64k = new RadioButton(
			"64 KByte RAM abz\u00FCglich ROM und IO-Bereich" );
    this.btnRAM64k.setToggleGroup( grpRAM );

    this.btnRAMInitZero = new CheckBox(
		"RAM mit %00 statt mit Zufallswerten initialisieren" );

    VBox vbox = new VBox(
			5,
			this.btnRAM1k,
			this.btnRAM2k,
			this.btnRAM8k,
			this.btnRAM32k,
			this.btnRAM64k,
			this.btnRAMInitZero );
    vbox.setAlignment( Pos.CENTER_LEFT );
    vbox.setPadding( new Insets( 10 ) );
    VBox.setMargin( this.btnRAMInitZero, new Insets( 20, 0, 0, 0 ) );
    setContent( vbox );

    updSettings();
    this.btnRAM1k.setOnAction( e->settingsNode.setDataChanged() );
    this.btnRAM2k.setOnAction( e->settingsNode.setDataChanged() );
    this.btnRAM8k.setOnAction( e->settingsNode.setDataChanged() );
    this.btnRAM32k.setOnAction( e->settingsNode.setDataChanged() );
    this.btnRAM64k.setOnAction( e->settingsNode.setDataChanged() );
    this.btnRAMInitZero.setOnAction( e->settingsNode.setDataChanged() );
  }


  public int applySettingsTo(
			Properties    props,
			JTCSys.OSType osType ) throws UserInputException
  {
    int ramSize = 0x10000;
    if( this.btnRAM1k.isSelected() ) {
      ramSize = 0x0400;
    } else if( this.btnRAM2k.isSelected() ) {
      ramSize = 0x0800;
    } else if( this.btnRAM8k.isSelected() ) {
      ramSize = 0x2000;
    } else if( this.btnRAM32k.isSelected() ) {
      ramSize = 0x8000;
    }
    JTCUtil.checkRAMSize( osType, ramSize );
    props.setProperty(
		AppContext.getPropPrefix() + JTCSys.PROP_RAM_SIZE,
		String.format( "%dK", ramSize / 1024 ) );
    props.setProperty(
		AppContext.getPropPrefix() + JTCSys.PROP_RAM_INIT_ZERO,
		Boolean.toString( this.btnRAMInitZero.isSelected() ) );
    return ramSize;
  }


  public void updSettings()
  {
    if( this.jtcSys != null ) {
      switch( this.jtcSys.getRAMSize() ) {
	case 0x0400:
	  this.btnRAM1k.setSelected( true );
	  break;
	case 0x0800:
	  this.btnRAM2k.setSelected( true );
	  break;
	case 0x2000:
	  this.btnRAM8k.setSelected( true );
	  break;
	case 0x10000:
	  this.btnRAM64k.setSelected( true );
	default:
	  this.btnRAM32k.setSelected( true );
      }
    }
    this.btnRAMInitZero.setSelected( JTCSys.isRamInitZero() );
  }
}
