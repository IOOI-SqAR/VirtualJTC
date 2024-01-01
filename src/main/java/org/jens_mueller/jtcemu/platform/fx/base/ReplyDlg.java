/*
 * (c) 2014-2020 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Allgemeiner Dialog zur Eingabe
 */

package org.jens_mueller.jtcemu.platform.fx.base;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.jens_mueller.jtcemu.base.JTCUtil;
import org.jens_mueller.jtcemu.base.UserInputException;
import org.jens_mueller.jtcemu.platform.fx.JTCEMUApplication;


public class ReplyDlg extends Stage
{
  private boolean   approved;
  private TextField textField;


  public static Integer showReplyDecDlg(
				Window  owner,
				String  msg,
				Integer minValue,
				Integer defaultValue )
  {
    Integer rv   = null;
    String  text = null;
    if( defaultValue != null ) {
      text = defaultValue.toString();
    }
    while( rv == null ) {
      ReplyDlg dlg = new ReplyDlg( owner, msg, text );
      dlg.showAndWait();
      if( !dlg.approved ) {
	break;
      }
      try {
	String s = dlg.textField.getText();
	if( s != null ) {
	  rv = Integer.valueOf( s );
	}
	if( rv == null ) {
	  throw new NumberFormatException();
	}
	if( minValue != null ) {
	  if( rv.intValue() < minValue.intValue() ) {
	    throw new UserInputException( "Wert zu klein" );
	  }
	}
      }
      catch( NumberFormatException ex ) {
	MsgDlg.showErrorMsg( owner, msg + " Ung\u00FCltige Zahl" );
      }
      catch( UserInputException ex ) {
	MsgDlg.showErrorMsg( owner, msg + " " + ex.getMessage() );
      }
    }
    return rv;
  }


  public static Integer showReplyHex4Dlg(
				Window  owner,
				String  msg,
				Integer defaultValue )
  {
    Integer rv   = null;
    String  text = null;
    if( defaultValue != null ) {
      text = String.format( "%04X", defaultValue );
    }
    while( rv == null ) {
      ReplyDlg dlg = new ReplyDlg( owner, msg, text );
      dlg.showAndWait();
      if( !dlg.approved ) {
	break;
      }
      try {
	rv = JTCUtil.parseHex4( dlg.textField.getText(), "Eingabe:" );
      }
      catch( UserInputException ex ) {
	MsgDlg.showErrorMsg( owner, ex.getMessage() );
      }
    }
    return rv;
  }


  public static String showReplyTextDlg(
				Window  owner,
				String  msg,
				String  defaultText )
  {
    ReplyDlg dlg = new ReplyDlg( owner, msg, defaultText );
    dlg.showAndWait();
    return dlg.approved ? dlg.textField.getText() : null;
  }


	/* --- Konstruktor --- */

  private ReplyDlg(
		Window owner,
		String msg,
		String defaultText )
  {
    initOwner( owner );
    initModality( Modality.WINDOW_MODAL );
    setOnShown( e->GUIUtil.centerStageOnOwner( this ) );
    setResizable( false );
    setTitle( "Eingabe" );
    JTCEMUApplication.addIconsTo( this );
    this.approved = false;

    Label label = new Label( msg != null ? msg : "Eingabe:" );
    label.setAlignment( Pos.CENTER_LEFT );

    this.textField = new TextField();
    this.textField.setPrefColumnCount( 5 );
    this.textField.setAlignment( Pos.CENTER_LEFT );
    if( defaultText != null ) {
      this.textField.setText( defaultText );
    }
    this.textField.setOnAction( e->okPressed() );
    HBox.setHgrow( this.textField, Priority.ALWAYS );

    HBox inputGrp = new HBox( 5.0, label, this.textField );
    inputGrp.setAlignment( Pos.CENTER_LEFT );

    Button btnOK = new Button( "OK" );
    btnOK.setMaxWidth( Double.MAX_VALUE );
    btnOK.setOnAction( e->okPressed() );

    Button btnCancel = new Button( "Abbrechen" );
    btnCancel.setMaxWidth( Double.MAX_VALUE );
    btnCancel.setOnAction( e->close() );

    TilePane buttonPane = new TilePane( 
				Orientation.HORIZONTAL,
				10.0, 0.0,
				btnOK, btnCancel );
    buttonPane.setAlignment( Pos.CENTER );
    buttonPane.setPrefColumns( 2 );

    VBox rootNode = new VBox( 10.0, inputGrp, buttonPane );
    rootNode.setAlignment( Pos.CENTER );
    rootNode.setPadding( new Insets( 10 ) );
    setScene( new Scene( rootNode ) );

    final Stage stage = this;
    Platform.runLater( ()->sizeToScene() );
  }


	/* --- private Methoden --- */

  private void okPressed()
  {
    this.approved = true;
    close();
  }
}
