/*
 * (c) 2007-2010 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Document mit einer begrenzten Textlaenge
 */

package jtcemu.base;

import java.lang.*;
import javax.swing.text.*;


public class LimitedLengthDoc extends PlainDocument
{
  private int maxLen;


  public LimitedLengthDoc( int maxLen )
  {
    this.maxLen = maxLen;
  }


        /* --- ueberschriebene Methoden --- */

  @Override
  public void insertString(
                        int                           offs,
                        String                        text,
                        javax.swing.text.AttributeSet attrs )
                                                throws BadLocationException
  {
    if( text != null ) {
      if( this.maxLen > 0 ) {
        int n = this.maxLen - getLength();
        int l = text.length();
        if( l > n ) {
          super.insertString( offs, text.substring( 0, n ), attrs );
        } else {
          super.insertString( offs, text, attrs );
        }
      } else {
        super.insertString( offs, text, attrs );
      }
    }
  }
}
