/*
 * (c) 2014-2020 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Node fuer sonstige Einstellungen
 */

package org.jens_mueller.jtcemu.platform.fx.settings;

import java.io.File;
import java.util.Properties;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import org.jens_mueller.jtcemu.base.AppContext;
import org.jens_mueller.jtcemu.platform.fx.JTCEMUApplication;
import org.jens_mueller.jtcemu.platform.fx.base.MsgDlg;
import org.jens_mueller.jtcemu.platform.fx.base.ScreenNode;


public class EtcSettingsNode extends ScrollPane
{
  private SettingsNode      settingsNode;
  private JTCEMUApplication JTCEMUApplication;
  private ComboBox<Integer> comboMargin;
  private ComboBox<Integer> comboRefreshMillis;
  private Button            btnPathsDelete;
  private Button            btnPropsDelete;


  public EtcSettingsNode( final SettingsNode settingsNode, JTCEMUApplication JTCEMUApplication)
  {
    this.settingsNode = settingsNode;
    this.JTCEMUApplication = JTCEMUApplication;

    int      gridRow  = 0;
    GridPane gridPane = new GridPane();
    gridPane.setAlignment( Pos.CENTER_LEFT );
    gridPane.setPadding( new Insets( 10 ) );
    gridPane.setHgap( 5 );
    gridPane.setVgap( 5 );
    setContent( gridPane );


    // Rand um Bildschirmausgabe
    gridPane.add(
		new Label( "Rand um Bildschirmausgabe:" ),
		0,
		gridRow );

    this.comboMargin = new ComboBox<>();
    this.comboMargin.getItems().addAll( 0, 5, 10, 15, 20, 30, 50, 100 );
    gridPane.add( this.comboMargin, 1, gridRow );

    gridPane.add( new Label( "Pixel" ), 2, gridRow );


    // Aktualisierungszyklus
    gridRow++;
    gridPane.add(
		new Label( "Aktualisierungszyklus der Bildschirmausgabe:" ),
		0,
		gridRow );

    this.comboRefreshMillis = new ComboBox<>();
    this.comboRefreshMillis.getItems().addAll( 10, 20, 30, 50, 100, 200 );
    gridPane.add( this.comboRefreshMillis, 1, gridRow );

    gridPane.add( new Label( "ms" ), 2, gridRow++ );


    // Datei zur Speicherung der zuletzt verwendeten Pfade
    File pathsFile = JTCEMUApplication.getPathsFile();
    if( pathsFile != null ) {
      Label labelPathsFile = new Label(
		"Zuletzt verwendete Dateipfade werden gespeichert"
			+ " in der Datei:" );
      GridPane.setColumnSpan( labelPathsFile, 3 );
      GridPane.setFillWidth( labelPathsFile, false );
      GridPane.setMargin( labelPathsFile, new Insets( 20, 0, 0, 0 ) );
      gridPane.add( labelPathsFile, 0, gridRow++ );

      TextField fldPathsFile = new TextField();
      fldPathsFile.setEditable( false );
      fldPathsFile.setText( pathsFile.getPath() );
      GridPane.setColumnSpan( fldPathsFile, 4 );
      GridPane.setFillWidth( fldPathsFile, true );
      gridPane.add( fldPathsFile, 0, gridRow++ );

      this.btnPathsDelete = new Button(
		"Datei mit gespeicherten Dateipfaden l\u00F6schen" );
      this.btnPathsDelete.setOnAction( e->doPathsDelete() );
      this.btnPathsDelete.setDisable( !pathsFile.exists() );
      GridPane.setColumnSpan( this.btnPathsDelete, 3 );
      GridPane.setFillWidth( this.btnPathsDelete, false );
      gridPane.add( this.btnPathsDelete, 0, gridRow++ );
    } else {
      this.btnPathsDelete = null;
    }
    updDeleteLastPathsBtnEnabled();


    // Datei zur Speicherung der Einstellungen
    File propsFile = JTCEMUApplication.getPropertiesFile();
    if( propsFile != null ) {
      Label labelPropsFile = new Label(
		"Einstellungen werden gespeichert in der Datei:" );
      GridPane.setColumnSpan( labelPropsFile, 3 );
      GridPane.setFillWidth( labelPropsFile, false );
      GridPane.setMargin( labelPropsFile, new Insets( 20, 0, 0, 0 ) );
      gridPane.add( labelPropsFile, 0, gridRow++ );

      TextField fldPropsFile = new TextField();
      fldPropsFile.setEditable( false );
      fldPropsFile.setText( propsFile.getPath() );
      GridPane.setColumnSpan( fldPropsFile, 4 );
      GridPane.setFillWidth( fldPropsFile, true );
      gridPane.add( fldPropsFile, 0, gridRow++ );

      this.btnPropsDelete = new Button(
		"Datei mit gespeicherten Einstellungen l\u00F6schen" );
      this.btnPropsDelete.setOnAction( e->doPropsDelete() );
      this.btnPropsDelete.setDisable( !propsFile.exists() );
      GridPane.setColumnSpan( this.btnPropsDelete, 3 );
      GridPane.setFillWidth( this.btnPropsDelete, false );
      gridPane.add( this.btnPropsDelete, 0, gridRow );
    } else {
      this.btnPropsDelete = null;
    }


    // Felder aktualisieren und Listener setzen
    updSettings();
    this.comboMargin.setOnAction( e->settingsNode.setDataChanged() );
    this.comboRefreshMillis.setOnAction( e->settingsNode.setDataChanged() );
    AppContext.setOnLastPathsSaved( ()->updDeleteLastPathsBtnEnabled() );
  }


  public void applySettingsTo( Properties props )
  {
    Integer margin = this.comboMargin.getValue();
    if( margin != null ) {
      props.setProperty(
		AppContext.getPropPrefix() + ScreenNode.PROP_SCREEN_MARGIN,
		margin.toString() );
    }
    Integer refreshMillis = this.comboRefreshMillis.getValue();
    if( refreshMillis != null ) {
      props.setProperty(
		AppContext.getPropPrefix()
			+ ScreenNode.PROP_SCREEN_REFRESH_MS,
		refreshMillis.toString() );
    }
  }


  public void updPropsFileDeleteButtonEnabled()
  {
    if( this.btnPropsDelete != null ) {
      boolean disable   = true;
      File    propsFile = JTCEMUApplication.getPropertiesFile();
      if( propsFile != null ) {
	if( propsFile.exists() ) {
	  disable = false;
	}
      }
      this.btnPropsDelete.setDisable( disable );
    }
  }


  public void updSettings()
  {
    ScreenNode screen = this.JTCEMUApplication.getScreen();
    if( screen != null ) {
      this.comboRefreshMillis.setValue( (int) screen.getRefreshMillis() );
    }
    this.comboMargin.setValue(
		AppContext.getIntProperty(
				ScreenNode.PROP_SCREEN_MARGIN,
				ScreenNode.DEFAULT_SCREEN_MARGIN ) );
    this.comboRefreshMillis.setValue(
		AppContext.getIntProperty(
				ScreenNode.PROP_SCREEN_REFRESH_MS,
				ScreenNode.DEFAULT_SCREEN_REFRESH_MS ) );
  }


	/* --- private Methoden --- */

  private void doPathsDelete()
  {
    File pathsFile = JTCEMUApplication.getPathsFile();
    if( pathsFile != null ) {
      if( MsgDlg.showYesNoDlg(
		this.JTCEMUApplication.getStage(),
		"M\u00F6chten Sie die zuletzt verwendeten Dateipfade"
			+ " l\u00F6schen?\n"
			+ "Die Datei wird beim n\u00E4chsten"
			+ " Laden oder Speichern einer Datei"
			+ " automatisch wieder angelegt.",
		"Best\u00E4tigung" ) )
      {
	boolean deleted = pathsFile.delete();
	updDeleteLastPathsBtnEnabled();
	if( !deleted && pathsFile.exists() ) {
	  MsgDlg.showErrorMsg(
		this.JTCEMUApplication.getStage(),
		"Die Datei mit den zuletzt verwendeten Dateipfaden\n"
			+ "konnte nicht gel\u00F6scht werden." );
	}
      }
    }
  }


  private void doPropsDelete()
  {
    File propFile = JTCEMUApplication.getPropertiesFile();
    if( propFile != null ) {
      if( MsgDlg.showYesNoDlg(
		this.JTCEMUApplication.getStage(),
		"M\u00F6chten Sie die gespeicherten Einstellungen"
			+ " l\u00F6schen?\n"
			+ AppContext.getAppName() + " startet dann beim"
			+ " n\u00E4chsten mal mit Standardeinstellungen.",
		"Best\u00E4tigung" ) )
      {
	boolean deleted = propFile.delete();
	updPropsFileDeleteButtonEnabled();
	this.settingsNode.updSaveButtonEnabled();
	if( !deleted && propFile.exists() ) {
	  MsgDlg.showErrorMsg(
		this.JTCEMUApplication.getStage(),
		"Die Datei mit den gespeicherten Einstellungen\n"
			+ "konnte nicht gel\u00F6scht werden." );
	}
      }
    }
  }


  private void updDeleteLastPathsBtnEnabled()
  {
    if( this.btnPathsDelete != null ) {
      boolean state     = false;
      File    pathsFile = JTCEMUApplication.getPathsFile();
      if( pathsFile != null ) {
	state = pathsFile.exists();
      }
      this.btnPathsDelete.setDisable( !state );
    }
  }
}
