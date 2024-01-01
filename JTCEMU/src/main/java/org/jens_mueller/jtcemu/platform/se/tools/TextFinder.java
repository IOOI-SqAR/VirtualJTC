/*
 * (c) 2019 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Suchen und Ersetzen von Text in einer JTextArea
 */

package org.jens_mueller.jtcemu.platform.se.tools;

import org.jens_mueller.jtcemu.base.JTCUtil;
import org.jens_mueller.jtcemu.platform.se.base.GUIUtil;

import javax.swing.*;
import java.awt.*;
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


  public boolean findNext( JTextArea textArea )
  {
    return find( textArea, -1, false, true );
  }


  public boolean findPrev( JTextArea textArea )
  {
    return find( textArea, -1, true, true );
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


  public void openFindDlg( JTextArea textArea )
  {
    FindAndReplaceDlg dlg = FindAndReplaceDlg.createFindDlg(
					GUIUtil.getWindow( textArea ),
					getPresetSearchText( textArea ) );
    dlg.setVisible( true );

    String searchText = dlg.getSearchText();
    if( searchText != null ) {
      if( !searchText.isEmpty() ) {
	setValues( searchText, false, null );
	find( textArea, -1, false, true );
      }
    }
  }


  public void openFindAndReplaceDlg( JTextArea textArea )
  {
    Window window = GUIUtil.getWindow( textArea );
    if( window != null ) {
      FindAndReplaceDlg dlg = FindAndReplaceDlg.createFindAndReplaceDlg(
				window,
				getPresetSearchText( textArea ),
				this.caseSensitive,
				this.replaceText );
      dlg.setVisible( true );

      String searchText = dlg.getSearchText();
      if( searchText != null ) {
	if( !searchText.isEmpty() ) {
	  switch( dlg.getAction() ) {
	    case FIND_NEXT:
	      setValues(
			searchText,
			dlg.getCaseSensitive(),
			dlg.getReplaceText() );
	      find( textArea, -1, false, true );
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
		try {
		  textArea.setCaretPosition( 0 );
		}
		catch( IllegalArgumentException ex ) {}
		showInfoDlg(
			textArea,
			String.format(
				"%d Textersetzung%s durchgef\u00FChrt.",
				n,
				n == 1 ? "" : "en" ),
			"Text ersetzen" );
	      } else {
		showTextNotFound( textArea );
	      }
	      break;
	  }
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
			JTextArea textArea,
			int       startPos,
			boolean   backward,
			boolean   interactive )
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
	  if( GUIUtil.hasSelection( textArea ) ) {
	    startPos = Math.min(
				textArea.getSelectionStart(),
				textArea.getSelectionEnd() );
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
	  if( GUIUtil.hasSelection( textArea ) ) {
	    startPos = Math.max(
				textArea.getSelectionStart(),
				textArea.getSelectionEnd() );
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
	  Window window = GUIUtil.getWindow( textArea );
	  if( window != null ) {
	    window.toFront();
	  }
	  textArea.requestFocus();
	  if( interactive ) {
	    textArea.setCaretPosition( foundPos );
	  }
	  textArea.select( foundPos, foundPos + this.searchText.length() );
	  rv = true;
	} else {
	  if( interactive ) {
	    showTextNotFound( textArea );
	  }
	}
      }
    }
    return rv;
  }


  private String getPresetSearchText( JTextArea textArea )
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


  private static void showInfoDlg(
			Component owner,
			String    msg,
			String    title )
  {
    JOptionPane.showMessageDialog(
			GUIUtil.getWindow( owner ),
			msg,
			title,
			JOptionPane.INFORMATION_MESSAGE );
  }


  private static void showTextNotFound( Component owner )
  {
    showInfoDlg( owner, "Text nicht gefunden!", "Text suchen" );
  }
}
