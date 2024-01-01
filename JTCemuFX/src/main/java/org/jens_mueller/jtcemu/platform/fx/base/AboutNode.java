/*
 * (c) 2014-2019 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Inhalt des Tabs fuer Bildschirm und Tastatur
 */

package org.jens_mueller.jtcemu.platform.fx.base;

import javafx.geometry.Insets;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import org.jens_mueller.jtcemu.base.AppContext;
import org.jens_mueller.jtcemu.platform.fx.JTCEMUApplication;


public class AboutNode extends ScrollPane
{
  private static AboutNode instance = null;


  public static void showTab( JTCEMUApplication JTCEMUApplication)
  {
    if( instance == null ) {
      instance = new AboutNode();
    }
    JTCEMUApplication.showTab( "\u00DCber", instance, true );
  }


	/* --- Konstruktor --- */

  private AboutNode()
  {
    Text appName = new Text( AppContext.getAppName() );
    appName.setFont( Font.font( "SansSerif", FontWeight.BOLD, 24.0 ) );

    Text appVersion = new Text( "Version " + AppContext.getAppVersion() );
    appVersion.setFont( Font.font( "SansSerif", FontWeight.BOLD, 18.0 ) );

    Text aboutContent = new Text( AppContext.getAboutContent() );

    VBox.setMargin( appName, new Insets( 10.0, 10.0, 0.0, 10.0 ) );
    VBox.setMargin( appVersion, new Insets( 0.0, 10.0, 10.0, 10.0 ) );
    VBox.setMargin( aboutContent, new Insets( 0.0, 10.0, 0.0, 10.0 ) );

    VBox vBox = new VBox( appName, appVersion, aboutContent );
    vBox.setSpacing( 5.0 );
    setContent( vBox );
    setFitToHeight( true );
    setFitToWidth( true );
  }
}
