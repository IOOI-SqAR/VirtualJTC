/*
 * (c) 2014-2019 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Allgemeiner Mitteilungsdialog
 */

package org.jens_mueller.jtcemu.platform.fx.base;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.jens_mueller.jtcemu.platform.fx.Main;


public class MsgDlg extends Stage
{
  private static enum MsgType { ERROR, INFO, QUESTION, WARNING };

  private int pressedBtnNum = -1;


  public static void showErrorMsg( Window owner, Exception ex )
  {
    String msg = "Unbekannter Fehler";
    if( ex != null ) {
      msg = ex.getMessage();
      if( msg != null ) {
	if( msg.isEmpty() ) {
	  msg = null;
	}
      }
      if( msg == null ) {
	msg = ex.getClass().getName();
      }
    }
    showErrorMsg( owner, msg );
  }


  public static void showErrorMsg( Window owner, String msg )
  {
    showMsg( owner, msg, "Fehler", MsgType.ERROR );
  }


  public static void showInfoMsg( Window owner, String msg )
  {
    showInfoMsg( owner, msg, "Information" );
  }


  public static void showInfoMsg( Window owner, String msg, String title )
  {
    showMsg( owner, msg, title, MsgType.INFO );
  }


  public static boolean showYesNoDlg(
				Window owner,
				String msg,
				String title )
  {
    MsgDlg dlg = new MsgDlg(
			owner,
			msg,
			title,
			MsgType.QUESTION,
			"Ja",
			"Nein" );
    dlg.showAndWait();
    return dlg.pressedBtnNum == 0;
  }


  public static boolean showYesNoWarningDlg( Window owner, String msg )
  {
    MsgDlg dlg = new MsgDlg(
			owner,
			msg,
			"Warnung",
			MsgType.WARNING,
			"Ja",
			"Nein" );
    dlg.showAndWait();
    return dlg.pressedBtnNum == 0;
  }


	/* --- Konstruktor --- */

  private MsgDlg(
		Window    owner,
		String    msg,
		String    title,
		MsgType   msgType,
		String... buttons )
  {
    initOwner( owner );
    initModality( Modality.WINDOW_MODAL );
    setOnShown( e->GUIUtil.centerStageOnOwner( this ) );
    setResizable( false );
    setTitle( title );
    Main.addIconsTo( this );


    // Info
    HBox infoNode = new HBox( 20.0 );
    infoNode.setAlignment( Pos.CENTER );

    if( msgType != null ) {
      String  fontName = "SansSerif";
      float   fontSize = 40;
      boolean fgStroke = true;
      Color   color    = null;
      String  text     = null;
      switch( msgType ) {
	case INFO:
	  fontName = "Serif";
	  color    = Color.DODGERBLUE;
	  text     = "i";
	  break;
	case ERROR:
	  fontSize = 16;
	  fgStroke = false;
	  color    = Color.RED;
	  text     = "STOP";
	  break;
	case QUESTION:
	  color = Color.GREEN;
	  text  = "?";
	  break;
	case WARNING:
	  color = new Color( 0.9, 0.9, 0.0, 1.0 );
	  text  = "!";
	  break;
      }
      if( (color != null) && (text != null) ) {
	Font font = Font.font( fontName, FontWeight.BOLD, fontSize );
 	if( font != null ) {
	  Circle circle = new Circle( 25 );
	  circle.setFill( color );
	  circle.setStroke( Color.GRAY );

	  Text symbolText = new Text( text );
	  symbolText.setFill( Color.WHITE );
	  symbolText.setFont( font );
	  if( fgStroke ) {
	    symbolText.setStroke( Color.GRAY );
	  }
	  infoNode.getChildren().add( new StackPane( circle, symbolText ) );
	}
      }
    }

    infoNode.getChildren().add( new Text( msg != null ? msg : "Fehler" ) );


    // Schaltflaechen
    TilePane buttonPane = new TilePane( Orientation.HORIZONTAL, 10.0, 0.0 );
    buttonPane.setAlignment( Pos.CENTER );
    if( buttons != null ) {
      for( int i = 0; i < buttons.length; i++ ) {
	Button    btn    = new Button( buttons[ i ] );
	final int btnNum = i;
	btn.setOnAction( e->btnPressed( btnNum ) );
	btn.setOnKeyPressed( e->keyPressed( btnNum, e ) );
	btn.setMaxWidth( Double.MAX_VALUE );
	buttonPane.getChildren().add( btn );
      }
    }
    buttonPane.setPrefColumns( buttons.length );


    // Gesamt-Layout
    VBox rootNode = new VBox( 10.0, infoNode, buttonPane );
    rootNode.setFillWidth( true );
    rootNode.setPadding( new Insets( 10.0 ) );
    setScene( new Scene( rootNode ) );

    final Stage stage = this;
    Platform.runLater( ()->sizeToScene() );
  }


	/* --- private Methoden --- */

  private void btnPressed( int btnNum )
  {
    this.pressedBtnNum = btnNum;
    close();
  }


  private void keyPressed( int btnNum, KeyEvent e )
  {
    switch( e.getCode() ) {
      case ENTER:
      case SPACE:
	e.consume();
	this.pressedBtnNum = btnNum;
	close();
	break;
    }
  }


  private static void showMsg(
			Window  owner,
			String  msg,
			String  title,
			MsgType msgType )
  {
    (new MsgDlg( owner, msg, title, msgType, "OK" )).showAndWait();
  }
}
