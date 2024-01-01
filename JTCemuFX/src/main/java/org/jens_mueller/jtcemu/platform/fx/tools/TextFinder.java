/*
 * (c) 2019 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Suchen und Ersetzen von Text in einer JTextArea
 */

package org.jens_mueller.jtcemu.platform.fx.tools;

import javafx.scene.control.IndexRange;
import javafx.scene.control.TextArea;
import javafx.stage.Window;
import org.jens_mueller.jtcemu.base.JTCUtil;
import org.jens_mueller.jtcemu.platform.fx.base.MsgDlg;

import java.util.concurrent.atomic.AtomicInteger;


public class TextFinder
{
  private String  searchText;
  private String  internalSearchText;
  private String  replaceText;
  private boolean caseSensitive;


  public TextFinder()
  {
    this.searchText         = null;
    this.caseSensitive      = false;
    this.replaceText        = null;
    this.internalSearchText = null;
  }


  private TextFinder(
		String  searchText,
		boolean caseSensitive,
		String  replaceText )
  {
    setValues( searchText, caseSensitive, replaceText );
  }


  public boolean findNext( Window owner, TextArea textArea )
  {
    return find( owner, textArea, -1, false, true );
  }


  public boolean findPrev( Window owner, TextArea textArea )
  {
    return find( owner, textArea, -1, true, true );
  }


  public String getReplaceText()
  {
    return this.replaceText;
  }


  public boolean hasReplaceText()
  {
    return (this.replaceText != null) && !this.replaceText.isEmpty();
  }


  public boolean hasSearchText()
  {
    return (this.searchText != null) && !this.searchText.isEmpty();
  }


  public void openFindDlg( Window owner, TextArea textArea )
  {
    FindAndReplaceDlg dlg = FindAndReplaceDlg.createFindDlg(
				owner,
				getPresetSearchText( textArea ) );
    dlg.showAndWait();

    String searchText = dlg.getSearchText();
    if( searchText != null ) {
      if( !searchText.isEmpty() ) {
	setValues( searchText, false, null );
	find( owner, textArea, -1, false, true );
      }
    }
  }


  public void openFindAndReplaceDlg( Window owner, TextArea textArea )
  {
    FindAndReplaceDlg dlg = FindAndReplaceDlg.createFindAndReplaceDlg(
				owner,
				getPresetSearchText( textArea ),
				this.caseSensitive,
				this.replaceText );
    dlg.showAndWait();

    String searchText = dlg.getSearchText();
    if( searchText != null ) {
      if( !searchText.isEmpty() ) {
	switch( dlg.getAction() ) {
	  case FIND_NEXT:
	    setValues(
		searchText,
		dlg.getCaseSensitive(),
		dlg.getReplaceText() );
	    find( owner, textArea, -1, false, true );
	    break;

	  case REPLACE_ALL:
	    setValues(
		searchText,
		dlg.getCaseSensitive(),
		dlg.getReplaceText() );
	    AtomicInteger count = new AtomicInteger();
	    String        text  = JTCUtil.replaceAll(
					textArea.getText(),
					searchText,
					dlg.getCaseSensitive(),
					dlg.getReplaceText(),
					count );
	    int n = count.get();
	    if( n > 0 ) {
	      textArea.setText( text );
	      MsgDlg.showInfoMsg(
			owner,
			String.format(
				"%d Textersetzung%s durchgef\u00FChrt.",
				n,
				n == 1 ? "" : "en" ),
			"Text ersetzen" );
	    } else {
	      showTextNotFound( owner );
	    }
	    break;
	}
      }
    }
  }


	/* --- private Methoden --- */

  /*
   * Eigentliche Suche,
   * Bei startPos < 0 wird die Suche am ausgewaehlten Text bzw.
   * an der aktuellen Cursor-Position fortgesetzt.
   */
  private boolean find(
			Window   owner,
			TextArea textArea,
			int      startPos,
			boolean  backward,
			boolean  interactive )
  {
    boolean rv = false;
    if( this.internalSearchText != null ) {
      if( !this.internalSearchText.isEmpty() ) {
	String  baseText = textArea.getText();
	if( baseText == null ) {
	  baseText = "";
	}
	if( !this.caseSensitive ) {
	  baseText = baseText.toUpperCase();
	}
	if( startPos < 0 ) {
	  startPos = textArea.getCaretPosition();
	}
	int len      = baseText.length();
	int foundPos = -1;
	if( backward ) {
	  IndexRange range = textArea.getSelection();
	  if( range != null ) {
	    if( range.getLength() > 0 ) {
	      startPos = Math.min( range.getStart(), range.getEnd() );
	    }
	  }
	  --startPos;
	  for( int i = 0; i < 2; i++ ) {
	    if( startPos < 1 ) {
	      startPos = len - 1;
	    } else if( startPos > (len - 1) ) {
	      startPos = len - 1;
	    }
	    if( startPos < 0 ) {
	      startPos = 0;
	    }
	    foundPos = baseText.lastIndexOf(
					this.internalSearchText,
					startPos );
	    if( foundPos >= 0 ) {
	      break;
	    }
	    startPos = len - 1;
	  }
	} else {
	  IndexRange range = textArea.getSelection();
	  if( range != null ) {
	    if( range.getLength() > 0 ) {
	      startPos = Math.max( range.getStart(), range.getEnd() );
	    }
	  }
	  for( int i = 0; i < 2; i++ ) {
	    if( (startPos < 0) || (startPos >= len) ) {
	      startPos = 0;
	    }
	    foundPos = baseText.indexOf(
					this.internalSearchText,
					startPos );
	    if( foundPos >= 0 ) {
	      break;
	    }
	    startPos = 0;
	  }
	}
	if( (foundPos >= 0) && (foundPos < len) ) {
	  textArea.requestFocus();
	  if( interactive ) {
	    textArea.positionCaret( foundPos );
	  }
	  textArea.selectRange(
			foundPos,
			foundPos + this.searchText.length() );
	  rv = true;
	} else {
	  if( interactive ) {
	    showTextNotFound( owner );
	  }
	}
      }
    }
    return rv;
  }


  private String getPresetSearchText( TextArea textArea )
  {
    String text = textArea.getSelectedText();
    if( text != null ) {
      if( text.isEmpty() ) {
	text = null;
      }
    }
    return text != null ? text : this.searchText;
  }


  private void setValues(
		String  searchText,
		boolean caseSensitive,
		String  replaceText )
  {
    this.searchText         = searchText;
    this.caseSensitive      = caseSensitive;
    this.replaceText        = replaceText;
    this.internalSearchText = searchText;
    if( !caseSensitive && (searchText != null) ) {
      this.internalSearchText = searchText.toUpperCase();
    }
  }


  private static void showTextNotFound( Window owner )
  {
    MsgDlg.showInfoMsg(
                owner,
                "Text nicht gefunden!",
                "Text suchen" );
  }
}
