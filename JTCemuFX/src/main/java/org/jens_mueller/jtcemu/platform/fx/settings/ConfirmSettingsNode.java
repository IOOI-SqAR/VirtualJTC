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
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import org.jens_mueller.jtcemu.base.AppContext;
import org.jens_mueller.jtcemu.base.JTCUtil;

import java.util.Properties;


public class ConfirmSettingsNode extends ScrollPane
{
  private CheckBox btnConfirmPowerOn;
  private CheckBox btnConfirmReset;
  private CheckBox btnConfirmQuit;


  public ConfirmSettingsNode( final SettingsNode settingsNode )
  {
    VBox vbox = new VBox( 5 );
    vbox.setAlignment( Pos.CENTER_LEFT );
    vbox.setPadding( new Insets( 10 ) );
    setContent( vbox );

    vbox.getChildren().add(
	new Label( "Folgende Aktionen m\u00FCssen in einem"
			+ " Dialog best\u00E4tigt werden:" ) );

    this.btnConfirmPowerOn = new CheckBox(
			"Einschalten (Arbeitsspeicher l\u00F6schen)" );
    VBox.setMargin( this.btnConfirmPowerOn, new Insets( 0, 0, 0, 50 ) );
    vbox.getChildren().add( this.btnConfirmPowerOn );

    this.btnConfirmReset = new CheckBox( "Zur\u00FCcksetzen (RESET)" );
    VBox.setMargin( this.btnConfirmReset, new Insets( 0, 0, 0, 50 ) );
    vbox.getChildren().add( this.btnConfirmReset );

    this.btnConfirmQuit = new CheckBox(
				AppContext.getAppName() + " beenden" );
    VBox.setMargin( this.btnConfirmQuit, new Insets( 0, 0, 0, 50 ) );
    vbox.getChildren().add( this.btnConfirmQuit );

    updSettings();
    this.btnConfirmPowerOn.setOnAction( e->settingsNode.setDataChanged() );
    this.btnConfirmReset.setOnAction( e->settingsNode.setDataChanged() );
    this.btnConfirmQuit.setOnAction( e->settingsNode.setDataChanged() );
  }


  public void applySettingsTo( Properties props )
  {
    JTCUtil.applyConfirmSettings(
			props,
			this.btnConfirmPowerOn.isSelected(),
			this.btnConfirmReset.isSelected(),
			this.btnConfirmQuit.isSelected() );
  }


  public void updSettings()
  {
    this.btnConfirmPowerOn.setSelected(
		AppContext.getBooleanProperty(
				JTCUtil.PROP_CONFIRM_POWER_ON,
				true ) );
    this.btnConfirmReset.setSelected(
		AppContext.getBooleanProperty(
				JTCUtil.PROP_CONFIRM_RESET,
				true ) );
    this.btnConfirmQuit.setSelected(
		AppContext.getBooleanProperty(
				JTCUtil.PROP_CONFIRM_QUIT,
				true ) );
  }
}
