/*
 * (c) 2014-2019 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Dialog fuer Suchen & Ersetzen
 */

package jtcemu.platform.fx.tools;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import jtcemu.platform.fx.Main;
import jtcemu.platform.fx.base.GUIUtil;


public class FindAndReplaceDlg extends Stage
{
  public enum Action { FIND_NEXT, REPLACE_ALL, CANCEL };

  private static final String TEXT_FIND = "Suchen";

  private Action    action;
  private String    searchText;
  private String    replaceText;
  private boolean   replaceAll;
  private boolean   caseSensitive;
  private TextField fldSearch;
  private TextField fldReplace;
  private CheckBox  btnCaseSensitive;


  public static FindAndReplaceDlg createFindDlg(
					Window owner,
					String searchText )
  {
    return new FindAndReplaceDlg( owner, searchText, null, false, null );
  }


  public static FindAndReplaceDlg createFindAndReplaceDlg(
					Window  owner,
					String  searchText,
					boolean caseSensitive,
					String  replaceText )
  {
    return new FindAndReplaceDlg(
			owner,
			searchText,
			caseSensitive,
			true,
			replaceText );
  }


  public Action getAction()
  {
    return this.action;
  }


  public boolean getCaseSensitive()
  {
    return this.caseSensitive;
  }


  public String getSearchText()
  {
    return this.searchText;
  }


  public boolean getReplaceAll()
  {
    return this.replaceAll;
  }


  public String getReplaceText()
  {
    return this.replaceText;
  }


	/* --- Konstruktor --- */

  private FindAndReplaceDlg(
			Window  owner,
			String  searchText,
			Boolean caseSensitive,
			boolean withReplace,
			String  replaceText )
  {
    if( owner != null ) {
      initOwner( owner );
    }
    initModality( Modality.WINDOW_MODAL );
    setOnShown( e->GUIUtil.centerStageOnOwner( this ) );
    setResizable( true );
    setTitle( TEXT_FIND );
    Main.addIconsTo( this );
    this.action        = Action.CANCEL;
    this.searchText    = null;
    this.replaceText   = null;
    this.replaceAll    = false;
    this.caseSensitive = false;


    // Eingabereich
    GridPane inputNode = new GridPane();
    inputNode.setHgap( 5.0 );
    inputNode.setVgap( 5.0 );
    inputNode.setPadding( new Insets( 5 ) );

    int y = 0;
    inputNode.add( new Label( "Suchen nach:" ), 0, y );

    this.fldSearch = new TextField();
    if( searchText != null ) {
      this.fldSearch.setText( searchText );
    }
    inputNode.add( this.fldSearch, 1, y );
    GridPane.setFillWidth( this.fldSearch, true );
    GridPane.setHgrow( this.fldSearch, Priority.ALWAYS );

    if( withReplace ) {
      y++;
      inputNode.add( new Label( "Ersetzen durch:" ), 0, y );

      this.fldReplace = new TextField();
      if( replaceText != null ) {
	this.fldReplace.setText( replaceText );
      }
      inputNode.add( this.fldReplace, 1, y );
      GridPane.setFillWidth( this.fldReplace, true );
      GridPane.setHgrow( this.fldReplace, Priority.ALWAYS );
    }

    if( caseSensitive != null ) {
      y++;
      this.btnCaseSensitive = new CheckBox(
			"Gro\u00DF-/Kleinschreibung beachten" );
      this.btnCaseSensitive.setSelected( caseSensitive.booleanValue() );
      inputNode.add( this.btnCaseSensitive, 1, y );
    } else {
      this.btnCaseSensitive = null;
    }


    // Schaltflaechen
    TilePane buttonPane = new TilePane(
				Orientation.HORIZONTAL,
				10.0, 10.0 );
    buttonPane.setAlignment( Pos.CENTER );

    Button btnFind = new Button( TEXT_FIND );
    btnFind.setMaxWidth( Double.MAX_VALUE );
    buttonPane.getChildren().add( btnFind );

    Button btnReplaceAll = null;
    if( withReplace ) {
      btnReplaceAll = new Button( "Alle ersetzen" );
      btnReplaceAll.setMaxWidth( Double.MAX_VALUE );
      buttonPane.getChildren().add( btnReplaceAll );
    }

    Button btnCancel = new Button( "Abbrechen" );
    btnCancel.setMaxWidth( Double.MAX_VALUE );
    buttonPane.getChildren().add( btnCancel );

    buttonPane.setPrefColumns( buttonPane.getChildren().size() );



    // Gesamtlayout
    VBox rootNode = new VBox( 10.0, inputNode, buttonPane );
    rootNode.setFillWidth( true );
    rootNode.setPadding( new Insets( 10.0 ) );
    setScene( new Scene( rootNode ) );

    final Stage stage = this;
    Platform.runLater( ()->sizeToScene() );


    // Aktionen
    if( withReplace ) {
      this.fldSearch.setOnAction( e->this.fldReplace.requestFocus() );
      this.fldReplace.setOnAction( e->doApply( false ) );
      btnReplaceAll.setOnAction( e->doApply( true ) );
    } else {
      this.fldSearch.setOnAction( e->doApply( false ) );
    }
    btnFind.setOnAction( e->doApply( false ) );
    btnCancel.setOnAction( e->close() );
  }


	/* --- private Methoden --- */

  private void doApply( boolean replaceAll )
  {
    String searchText = this.fldSearch.getText();
    if( searchText == null ) {
      searchText = "";
    }
    if( !searchText.isEmpty() ) {
      if( this.btnCaseSensitive != null ) {
	this.caseSensitive = this.btnCaseSensitive.isSelected();
      }
      this.searchText = searchText;
      if( this.fldReplace != null ) {
	this.replaceText = this.fldReplace.getText();
      }
      this.replaceAll = replaceAll;
      this.action     = (replaceAll ? Action.REPLACE_ALL : Action.FIND_NEXT);
      close();
    } else {
      this.fldSearch.requestFocus();
    }
  }
}
