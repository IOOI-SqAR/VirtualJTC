/*
 * (c) 2014-2020 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Allgemeine Hilfsfunktionen
 */

package org.jens_mueller.jtcemu.base;

import java.io.Closeable;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class JTCUtil
{
  public static final String PROP_CONFIRM_POWER_ON = "confirm.power_on";
  public static final String PROP_CONFIRM_RESET    = "confirm.reset";
  public static final String PROP_CONFIRM_QUIT     = "confirm.quit";


  private static Boolean osMac = null;
  private static Boolean osWin = null;

  public static void applyConfirmSettings(
				Properties props,
				boolean    confirmPowerOn,
				boolean    confirmReset,
				boolean    confirmQuit )
  {
    props.setProperty(
		AppContext.getPropPrefix() + PROP_CONFIRM_POWER_ON,
		Boolean.toString( confirmPowerOn ) );
    props.setProperty(
		AppContext.getPropPrefix() + PROP_CONFIRM_RESET,
		Boolean.toString( confirmReset ) );
    props.setProperty(
		AppContext.getPropPrefix() + PROP_CONFIRM_QUIT,
		Boolean.toString( confirmQuit ) );
  }


  public static void applyROMSettings(
				Properties props,
				ExtROM[]   extROMs,
				boolean    hasROMBank )
			throws UserInputException
  {
    int lastAddr = -1;
    int idx      = 0;
    if( extROMs != null ) {
      for( ExtROM rom : extROMs ) {
	int begAddr = rom.getBegAddr();
	if( hasROMBank
	    && (((begAddr < 0x2000) && (rom.getEndAddr() >= 0x2000))
		|| ((begAddr >= 0x2000) && (begAddr < 0x4000))) )
	{
	  throw new UserInputException(
		String.format(
			"ROM an Adresse %%%04X \u00FCberschneidet sich"
				+ " mit der ROM-Bank (%%2000 bis %%3FFF).",
			begAddr ) );
	}
	if( begAddr <= lastAddr ) {
	  throw new UserInputException(
		String.format(
			"ROM an Adresse %%%04X \u00FCberschneidet sich"
				+ " mit dem vorherigen ROM.",
			begAddr ) );
	}
	props.setProperty(
		AppContext.getPropPrefix() + JTCSys.getROMPropAddr( idx ),
		String.format( "%%%04X", rom.getBegAddr() ) );
	props.setProperty(
		AppContext.getPropPrefix() + JTCSys.getROMPropFile( idx ),
		rom.getFile().getPath() );
	lastAddr = rom.getEndAddr();
	idx++;
      }
    }
    props.setProperty(
		AppContext.getPropPrefix() + JTCSys.PROP_ROM_COUNT,
		Integer.toString( idx ) );
  }


  public static void applyROMBankSettings(
				Properties props,
				boolean    state,
				String     fileName )
			throws UserInputException
  {
    if( fileName == null ) {
      fileName = "";
    }
    if( state && fileName.isEmpty() ) {
      throw new UserInputException(
		"F\u00FCr die ROM-Bank wurde keine Datei angegeben." );
    }
    props.setProperty(
		AppContext.getPropPrefix() + JTCSys.PROP_ROMBANK_ENABLED,
		Boolean.toString( state ) );
    props.setProperty(
		AppContext.getPropPrefix() + JTCSys.PROP_ROMBANK_FILE,
		fileName );
  }


  public static void checkRAMSize(
			JTCSys.OSType osType,
			int           ramSize ) throws UserInputException
  {
    if( osType != null ) {
      switch( osType ) {
	case ES1988:
	  if( ramSize < 0x0800 ) {
	    throw new UserInputException(
		"EMR-ES 1988 erfordert mindestens 2 KByte RAM." );
	  }
	  break;
	case ES23:
	case ES40:
	  if( ramSize < 0x2000 ) {
	    throw new UserInputException(
		"ES 2.3 und ES 4.0 erfordern mindestens 8 KByte RAM." );
	  }
	  break;
      }
    }
  }


  public static void closeSilently( Closeable stream )
  {
    if( stream != null ) {
      try {
	stream.close();
      }
      catch( IOException ex ) {}
    }
  }


  public static boolean differsExtROM( ExtROM rom1, ExtROM rom2 )
  {
    boolean rv = true;
    if( (rom1 != null) && (rom2 != null) ) {
      rv = !rom1.equals( rom2 );
    } else {
      if( (rom1 == null) && (rom2 == null) ) {
	rv = false;
      }
    }
    return rv;
  }


  public static boolean differsExtROMs( ExtROM[] roms1, ExtROM[] roms2 )
  {
    boolean rv = true;
    int     n1 = 0;
    int     n2 = 0;
    if( roms1 != null ) {
      n1 = roms1.length;
    }
    if( roms2 != null ) {
      n2 = roms2.length;
    }
    if( n1 == n2 ) {
      rv = false;
      if( (roms1 != null) && (roms2 != null) ) {
	for( int i = 0; i < n1; i++ ) {
	  if( differsExtROM( roms1[ i ], roms2[ i ] ) ) {
	    rv = true;
	    break;
	  }
	}
      }
    }
    return rv;
  }


  public static Integer getBegAddrFromFilename( File file )
  {
    Integer addr = null;
    if( file != null ) {
      String  filename = file.getName();
      if( filename != null ) {
	filename = filename.toUpperCase();
	int len  = filename.length();
	int pos  = filename.indexOf( '_' );
	while( (pos >= 0) && ((pos + 4) < len) ) {
	  if( isHexChar( filename.charAt( pos + 1 ) )
	      && isHexChar( filename.charAt( pos + 2 ) )
	      && isHexChar( filename.charAt( pos + 3 ) )
	      && isHexChar( filename.charAt( pos + 4 ) ) )
	  {
	    try {
	      addr = Integer.valueOf(
				filename.substring( pos + 1, pos + 5 ),
				16 );
	    }
	    catch( NumberFormatException ex ) {}
	    if( addr != null ) {
	      break;
	    }
	  }
	  if( pos + 5 < len ) {
	    pos = filename.indexOf( '_', pos + 1 );
	  } else {
	    pos = -1;
	  }
	}
      }
    }
    return addr;
  }


  public static String getEmulatedSpeedText( Double mhz )
  {
    String text = null;
    if( mhz != null ) {
      long mhz10 = Math.round( mhz.doubleValue() * 10.0 );
      long mhz20 = mhz10 * 2;
      if( mhz10 >= 1 ) {
	text = String.format(
		"Emulierte Taktfrequenz: %1d.%01d MHz extern"
				+ " / %1d.%01d MHz intern",
		mhz20 / 10,
		mhz20 % 10,
		mhz10 / 10,
		mhz10 % 10 );
      }
    }
    return text;
  }


  public static boolean isHexChar( char ch )
  {
    return ((ch >= '0') && (ch <= '9'))
		|| ((ch >= 'A') && (ch <= 'F'))
		|| ((ch >= 'a') && (ch <= 'f'));
  }


  public static boolean isMacOS()
  {
    if( osMac == null ) {
      Boolean state  = Boolean.FALSE;
      String  osName = System.getProperty( "os.name" );
      if( osName != null ) {
	if( osName.toUpperCase().startsWith( "MAC" ) ) {
	  state = Boolean.TRUE;
	}
      }
      osMac = state;
    }
    return osMac.booleanValue();
  }


  public static boolean isWinOS()
  {
    if( osWin == null ) {
      Boolean state  = Boolean.FALSE;
      String  osName = System.getProperty( "os.name" );
      if( osName != null ) {
	osName = osName.toUpperCase();
	if( osName.startsWith( "WIN" ) || osName.startsWith( "MS WIN" ) ) {
	  state = Boolean.TRUE;
	}
      } else {
	osWin = Boolean.valueOf( File.separatorChar == '\\' );
      }
      osWin = state;
    }
    return osWin.booleanValue();
  }


  public static String loadHtml(
			URL     url,
			boolean removeLinks ) throws IOException
  {
    String         rv = null;
    BufferedReader in = null;
    try {
      in = new BufferedReader(
		new InputStreamReader( url.openStream(), "UTF-8" ) );
      String eol = System.getProperty( "line.separator" );
      if( eol != null ) {
	if( eol.isEmpty() ) {
	  eol = null;
	}
      }
      if( eol == null ) {
	eol = "\r\n";
      }
      StringBuilder buf  = new StringBuilder( 0x1000 );
      String        line = in.readLine();
      while( line != null ) {
	buf.append( line );
	buf.append( eol );
	line = in.readLine();
      }
      if( buf.length() > 0 ) {
	if( removeLinks ) {
	  int idx1 = 0;
	  for(;;) {
	    idx1 = buf.indexOf( "<a href=", idx1 );
	    if( idx1 < 0 ) {
	      break;
	    }
	    int idx2 = buf.indexOf( ">", idx1 );
	    if( idx2 > idx1 ) {
	      int idx3 = buf.indexOf( "</a>", idx2 + 1 );
	      if( idx3 > idx2 ) {
		buf = buf.delete( idx3, idx3 + 4 );
		buf = buf.delete( idx1, idx2 + 1 );
	      } else {
		idx1 = idx2 + 1;
	      }
	    } else {
	      idx1++;
	    }
	  }
	} else {
	  buf = replaceAll( buf, "\"../common/", "\"" );
	  buf = replaceAll( buf, "\"../fx/", "\"" );
	}
	rv = buf.toString();
      }
    }
    finally {
      JTCUtil.closeSilently( in );
    }
    return rv;
  }


  public static int parseHex2(
			String text,
			String fldLabel ) throws UserInputException
  {
    return parseHex( text, fldLabel, 0xFF );
  }


  public static int parseHex4(
			String text,
			String fldLabel ) throws UserInputException
  {
    return parseHex( text, fldLabel, 0xFFFF );
  }


  public static byte[] readFile(
				File file,
				int  maxSize ) throws IOException
  {
    int  bufSize = 0x1000;
    long fileLen = file.length();
    if( fileLen > 0 ) {
      if( fileLen > 0x10000 ) {
	bufSize = 0x10000;
      } else {
	bufSize = (int) fileLen;
      }
    }
    ByteArrayOutputStream buf = new ByteArrayOutputStream( bufSize );
    InputStream           in  = null;
    try {
      in = new FileInputStream( file );

      int n = 0;
      int b = in.read();
      while( b >= 0 ) {
	if( (maxSize > 0) && (n >= maxSize) ) {
	  throw new IOException( "Datei zu gro\u00DF" );
	}
	buf.write( b );
	b = in.read();
	n++;
      }
    }
    finally {
      JTCUtil.closeSilently( in );
    }
    return buf.toByteArray();
  }


  public static String replaceAll(
				String        baseText,
				String        searchText,
				boolean       caseSensitive,
				String        replaceText,
				AtomicInteger rvCount )
  {
    String rv = baseText;
    if( (baseText != null) && (searchText != null) ) {
      if( replaceText == null ) {
	replaceText = "";
      }
      try {
	int flags = Pattern.UNICODE_CASE
			| Pattern.UNICODE_CHARACTER_CLASS;
	if( !caseSensitive ) {
	  flags |= Pattern.CASE_INSENSITIVE;
	}
	Pattern p = Pattern.compile( Pattern.quote( searchText ), flags );
	Matcher m = p.matcher( baseText );
	StringBuffer buf = new StringBuffer( baseText.length() + 32 );
	while( m.find() ) {
	  if( rvCount != null ) {
	    rvCount.incrementAndGet();
	  }
	  m.appendReplacement( buf, replaceText );
	}
	m.appendTail( buf );
	rv = buf.toString();
      }
      catch( PatternSyntaxException ex ) {}
    }
    return rv;
  }


	/* --- private Methoden --- */

  private static int parseHex(
			String text,
			String fldLabel,
			int    maxValue ) throws UserInputException
  {
    int     value = 0;
    boolean done  = false;
    if( text != null ) {
      text    = text.trim();
      int len = text.length();
      for( int i = 0; i < len; i++ ) {
	char ch = Character.toUpperCase( text.charAt( i ) );
	if( (ch == '%') && (i == 0) ) {
	  // %-Zeichen am Anfang erlauben, d.h. ueberlesen
	} else if( (ch >= '0') && (ch <= '9') ) {
	  value = (value << 4) | (ch - '0');
	  done  = true;
	} else if( (ch >= 'A') && (ch <= 'F') ) {
	  value = (value << 4) | (ch - 'A' + 10);
	  done  = true;
	} else {
	  throw new UserInputException(
			fldLabel +  " Ung\u00FCltige Hexadezimalzahl" );
	}
	if( (value & ~maxValue) != 0 ) {
	  throw new UserInputException( fldLabel + " Wert zu gro\u00DF!" );
	}
      }
    }
    if( !done ) {
      throw new UserInputException( fldLabel + " Eingabe erwartet" );
    }
    return value;
  }


  private static StringBuilder replaceAll(
					StringBuilder buf,
					String        searchText,
					String        replaceText )
  {
    int searchLen  = searchText.length();
    int replaceLen = replaceText.length();
    int idx = 0;
    for(;;) {
      idx = buf.indexOf( searchText, idx );
      if( idx < 0 ) {
	break;
      }
      buf = buf.replace( idx, idx + searchLen, replaceText );
      idx += replaceLen;
    }
    return buf;
  }


	/* --- Konstruktor --- */

  private JTCUtil()
  {
    // Klasse nicht instanziierbar
  }
}
