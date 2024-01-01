/*
 * (c) 2007-2021 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Komponente fuer die Bildschirmtastatur
 * fuer das 2K-Betriebssystem sowie fuer ES1988
 */

package org.jens_mueller.jtcemu.platform.se.keyboard;

import org.jens_mueller.jtcemu.base.JTCSys;

import java.awt.*;


public class OS2kKeyboardFld extends AbstractKeyboardFld
{
  private static Font baseFont = new Font( Font.SANS_SERIF, Font.BOLD, 14 );
  private static Font sht1Font = new Font( Font.SANS_SERIF, Font.BOLD, 12 );
  private static Font sht2Font = new Font( Font.SANS_SERIF, Font.PLAIN, 8 );

  private boolean es1988;


  public OS2kKeyboardFld( JTCSys jtcSys, boolean es1988 )
  {
    super( jtcSys, 1, 12, 55, 40 );
    this.es1988 = es1988;

    addKey( getImageDown(), getImageUp() );
    addKey( "1", "!", "TRAP" );
    addKey( "2", "\"" );
    addKey( "3", "#" );
    addKey( "4", "$" );
    addKey( "5", "%" );
    addKey( "6", "&" );
    addKey( "7", "\'" );
    addKey( "8", "(" );
    addKey( "9", ")" );
    addKey( "0" );
    addKey( getImageLeft(), null, "NEW" );

    if( es1988 ) {
      addKey( "=" );
    } else {
      addKey( getImageLeft(), getImageRight() );
    }
    addKey( "Q" );
    addKey( "W", null, "WAIT" );
    addKey( "E", null, "END" );
    addKey( "R", null, "RETURN" );
    addKey( "T", null, "STOP" );
    addKey( "Z" );
    addKey( "U" );
    addKey( "I", null, "INPUT" );
    addKey( "O", null, "PROC", "LOAD" );
    addKey( "P", null, "PRINT", "SAVE" );
    addKey( "+", ";", "RUN" );

    addKey( getImageShift() );
    addKey( "A" );
    addKey( "S", null, "GOSUB" );
    addKey( "D" );
    addKey( "F" );
    addKey( "G", null, "GOTO" );
    addKey( "H", null, "PTH" );
    addKey( "J" );
    addKey( "K", "[" );
    addKey( "L", null, "LET" );
    addKey( "*", ":" );
    addKey( "-", es1988 ? null : "=", "LIST" );

    addKey( getImageSpace() );
    addKey( "Y" );
    addKey( "X" );
    addKey( "C", null, "CALL" );
    addKey( "V" );
    addKey( "B" );
    addKey( "N" );
    addKey( "M", "]", "REM" );
    addKey( ",", "<", "THEN" );
    addKey( ".", ">" );
    addKey( "/", "?", "TOFF" );
    addKey( "Enter", null, null, "OFF" );
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public JTCSys.OSType getOSType()
  {
    return this.es1988 ? JTCSys.OSType.ES1988 : JTCSys.OSType.OS2K;
  }


  @Override
  public void paintKeyFld( Graphics g, KeyFld keyFld )
  {
    super.paintKeyFld( g, keyFld );
    g.setColor( Color.BLACK );
    g.setFont( sht2Font );

    String s = keyFld.getShift3Text();
    if( s != null ) {
      drawRight( g, s, keyFld.getWidth() - 5, 15 );
    }
    s = keyFld.getShift2Text();
    if( s != null ) {
      drawRight( g, s, keyFld.getWidth() - 5, keyFld.getHeight() - 6 );
    }
    s = keyFld.getShift1Text();
    if( s != null ) {
      g.setFont( sht1Font );
      g.drawString( s, 5, 18 );
    }
    s = keyFld.getBaseText();
    if( s != null ) {
      g.setFont( baseFont );
      g.drawString( s, 5, keyFld.getHeight() - 6 );
    }
    Image img = keyFld.getShift1Image();
    if( img != null ) {
      g.drawImage( img, 5, 10, this );
    }
    img = keyFld.getBaseImage();
    if( img != null ) {
      g.drawImage( img, 5, keyFld.getHeight() - 16, this );
    }
  }
}
