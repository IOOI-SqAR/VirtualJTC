/*
 * (c) 2014-2019 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Inhalt des Tabs fuer Bildschirm und Tastatur
 */

package jtcemu.platform.fx.base;

import javafx.geometry.Insets;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import jtcemu.base.AppContext;
import jtcemu.platform.fx.Main;


public class AboutNode extends ScrollPane
{
  private static AboutNode instance = null;


  public static void showTab( Main main )
  {
    if( instance == null ) {
      instance = new AboutNode();
    }
    main.showTab( "\u00DCber", instance, true );
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
