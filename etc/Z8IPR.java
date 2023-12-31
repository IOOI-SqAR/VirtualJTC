/*
 * (c) 2007-2019 Jens Mueller
 *
 * Ermittlung der Z8-IPR-Kodierung
 */


public class Z8IPR
{
  public static void main( String[] args )
  {
    for( int i = 0; i < 64; i++ ) {
      String text = "";
      String info = "";
      int grp = (i >> 2) & 0x06;
      if( (i & 0x01) != 0 ) {
	grp |= 0x01;
      }
      switch( grp ) {
	case 0:
	  text = "\t{ 0, 1, 2, 3, 4, 5 },\t// IPR=0x%02X reserviert\n";
	  break;

	case 1:
	  text = "\t{ C, A, B },\t// IPR=0x%02X GRP=0x%02X %s\n";
	  info = "C > A > B";
	  break;

	case 2:
	  text = "\t{ A, B, C },\t// IPR=0x%02X GRP=0x%02X %s\n";
	  info = "A > B > C";
	  break;

	case 3:
	  text = "\t{ A, C, B },\t// IPR=0x%02X GRP=0x%02X %s\n";
	  info = "A > C > B";
	  break;

	case 4:
	  text = "\t{ B, C, A },\t// IPR=0x%02X GRP=0x%02X %s\n";
	  info = "B > C > A";
	  break;

	case 5:
	  text = "\t{ C, B, A },\t// IPR=0x%02X GRP=0x%02X %s\n";
	  info = "C > B > A";
	  break;

	case 6:
	  text = "\t{ B, A, C },\t// IPR=0x%02X GRP=0x%02X %s\n";
	  info = "B > A > C";
	  break;

	case 7:
	  text = "\t{ 0, 1, 2, 3, 4, 5 },\t// IPR=0x%02X reserviert\n";
	  break;
      }
      if( text != null ) {
	if( (i & 0x02) == 0 ) {
	  text = text.replace( "C", "1, 4" );
	} else {
	  text = text.replace( "C", "4, 1" );
	}
	if( (i & 0x04) == 0 ) {
	  text = text.replace( "B", "2, 0" );
	} else {
	  text = text.replace( "B", "0, 2" );
	}
	if( (i & 0x20) == 0 ) {
	  text = text.replace( "A", "5, 3" );
	} else {
	  text = text.replace( "A", "3, 5" );
	}
      }
      System.out.printf( text, i, grp, info );
    }
  }
}
