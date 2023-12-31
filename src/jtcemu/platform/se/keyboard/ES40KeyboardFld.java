/*
 * (c) 2021 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Komponente fuer die Bildschirmtastatur fuer ES4.0
 */

package jtcemu.platform.se.keyboard;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import jtcemu.base.JTCSys;


public class ES40KeyboardFld extends AbstractKeyboardFld
{
  private static Font baseFont1 = new Font( Font.SANS_SERIF, Font.BOLD, 14 );
  private static Font baseFont2 = new Font( Font.SANS_SERIF, Font.PLAIN, 14 );
  private static Font shiftFont = new Font( Font.SANS_SERIF, Font.PLAIN, 11 );


  public ES40KeyboardFld( JTCSys jtcSys )
  {
    super( jtcSys, 1, 12, 50, 50 );

    addKey( null );
    addKey( "1", "!",  "DBS" );
    addKey( "2", "\"", "INS" );
    addKey( "3", "#",  "CLS" );
    addKey( "4", "$" );
    addKey( "5", "%",  "\u00E4" );
    addKey( "6", "&",  "\u00F6" );
    addKey( "7", "\'", "\u00FC" );
    addKey( "8", "@",  "\u00C4" );
    addKey( "9", "(",  "\u00D6" );
    addKey( "0", ")",  "\u00FC" );
    addKey( "<", ">",  "^", "\u00DF" );

    addKey( "SHT3", null, null, null ).setShiftKey( true );
    addKey( "Q", null, "DEL",        "F1" );
    addKey( "W", null, getImageUp(), "F2" );
    addKey( "E", null, "SOL",        "F3" );
    addKey( "R", null, "LDE",        "F4" );
    addKey( "T", null, null,         "F5" );
    addKey( "Y", null, null,         "F6" );
    addKey( "U", null, null,         "F7" );
    addKey( "I", null, null,         "F8" );
    addKey( "O" );
    addKey( "P" );
    addKey( "+", "-",  "\\" );

    addKey( "SHT2", null, null, null ).setShiftKey( true );
    addKey( "A", null, getImageLeft() );
    addKey( "S", null, "HOM" );
    addKey( "D", null, getImageRight() );
    addKey( "F", null, "LIN" );
    addKey( "G" );
    addKey( "H" );
    addKey( "J" );
    addKey( "K" );
    addKey( "L" );
    addKey( ";", ":" );
    addKey( "*", "/", "|" );

    addKey( "SHT1", null, null, null ).setShiftKey( true );
    addKey( "Z" );
    addKey( "X", null, getImageDown() );
    addKey( "C" );
    addKey( "V", null, "ESC" );
    addKey( "B" );
    addKey( "N" );
    addKey( "M" );
    addKey( ",", "[", "{" );
    addKey( ".", "]", "}" );
    addKey( getImageSpace(), "=", "_" );
    addKey( "RET", "?", "~" );
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public String getHoldShiftKeysText()
  {
    return "SHT-Tasten gedr\u00FCckt halten";
  }


  @Override
  public JTCSys.OSType getOSType()
  {
    return JTCSys.OSType.ES40;
  }


  @Override
  public void paintKeyFld( Graphics g, KeyFld keyFld )
  {
    super.paintKeyFld( g, keyFld );
    g.setColor( Color.BLACK );
    g.setFont( shiftFont );

    String s = keyFld.getShift3Text();
    if( s != null ) {
      drawRight( g, s, keyFld.getWidth() - 5, 15 );
    }
    s = keyFld.getShift2Text();
    if( s != null ) {
      g.drawString( s, 5, 26 );
    }
    s = keyFld.getShift1Text();
    if( s != null ) {
      drawRight( g, s, keyFld.getWidth() - 5, keyFld.getHeight() - 15 );
    }
    s = keyFld.getBaseText();
    if( s != null ) {
      g.setFont( s.length() > 1 ? baseFont2 : baseFont1 );
      g.drawString( s, 5, keyFld.getHeight() - 5 );
    }
    Image img = keyFld.getShift2Image();
    if( img != null ) {
      g.drawImage( img, 5, 15, this );
    }
    img = keyFld.getBaseImage();
    if( img != null ) {
      g.drawImage( img, 5, keyFld.getHeight() - 15, this );
    }
  }
}
