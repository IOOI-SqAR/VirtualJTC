/*
 * (c) 2019-2021 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Hilfsfunktionen fuer einen Compiler/Assembler
 */

package org.jens_mueller.jtcemu.tools.assembler;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.CharacterIterator;
import java.util.Arrays;
import java.util.InvalidPropertiesFormatException;
import java.util.Properties;
import org.jens_mueller.jtcemu.base.AppContext;
import org.jens_mueller.jtcemu.base.FileSaver;
import org.jens_mueller.jtcemu.base.JTCSys;
import org.jens_mueller.jtcemu.base.JTCUtil;
import org.jens_mueller.jtcemu.tools.TextOutput;


public class AsmUtil
{
  public static final String DEFAULT_APP_NAME = "MyAsmApp";
  public static final String PROP_SOURCE_FILE = "jtcemu.source.file";

  private static final String PROP_PROPERTIES_TYPE
					= "jtcemu.properties.type";
  private static final String VALUE_PROPERTIES_TYPE_PROJECT
					= "project.assembler";


  private static String[] sortedReservedWords = {
	"C", "EQ", "F", "GE", "GT", "HI", "HIGH", "LE", "LT", "LO", "LOW",
	"MI", "NC", "NE", "NOV", "NZ", "OV", "PL",
	"UGE", "UGT", "ULE", "ULT", "Z" };


  public static void assemble(
			String     asmText,
			File       asmFile,
			AsmOptions options,
			JTCSys     jtcSys,
			TextOutput textOutput )
  {
    try {
      Z8Assembler asm = new Z8Assembler(
				asmText,
				null,
				asmFile,
				options,
				textOutput,
				false,
				textOutput );
      final byte[] codeBytes = asm.assemble();
      if( codeBytes != null ) {
	boolean logged = false;
	if( codeBytes.length > 0 ) {
	  if( options.getCodeToEmu() ) {
	    boolean failed = false;
	    int     addr   = asm.getBegAddr();
	    for( int i = 0; i < codeBytes.length; i++ ) {
	      if( !jtcSys.setMemByte( addr++, false, codeBytes[ i ] ) ) {
		failed = true;
	      }
	    }
	    if( failed ) {
	      textOutput.print(
			String.format(
				"Assembler-Programm konnte nicht"
					+ " (vollst\u00E4ndig) in den"
					+ " Speicherbereich %%%04X-%%%04X"
					+ " geladen werden.",
				asm.getBegAddr(),
				addr - 1 ) );
	      textOutput.println();
	    } else {
	      textOutput.print(
			String.format(
				"Assembler-Programm in Speicherbereich"
					+ " %04X-%04X geladen",
				asm.getBegAddr(),
				addr - 1 ) );
	      textOutput.println();
	      Integer entryAddr = asm.getEntryAddr();
	      if( entryAddr != null ) {
		textOutput.print(
			String.format(
				"Startadresse: %04X",
				entryAddr ) );
		textOutput.println();
	      }
	    }
	    logged = true;
	  }
	  if( options.getCodeToFile() ) {
	    File   file  = options.getCodeFile();
	    String title = asm.getTitle();
	    if( file != null ) {
	      Integer entryAddr = asm.getEntryAddr();
	      FileSaver.save(
			asm.getBegAddr(),
			entryAddr != null ? entryAddr.intValue() : -1,
			codeBytes,
			file,
			title != null ? title : DEFAULT_APP_NAME );
	    }
	  }
	}
	if( !logged ) {
	  textOutput.print( "Fertig" );
	  textOutput.println();
	}
      }
    }
    catch( IOException ex ) {
      textOutput.print( "Ein-/Ausgabefehler" );
      String msg = ex.getMessage();
      if( msg != null ) {
	if( !msg.isEmpty() ) {
	  textOutput.print( ": " );
	  textOutput.print( msg );
	}
      }
      textOutput.println();
    }
  }


  public static boolean checkAndParseToken(
				CharacterIterator iter,
				String...         tokens )
  {
    boolean rv     = true;
    int     begIdx = iter.getIndex();
    for( String token : tokens ) {
      try {
	int     len   = token.length();
	char    ch    = Character.toUpperCase( skipBlanks( iter ) );
	boolean ident = false;
	for( int i = 0; i < len; i++ ) {
	  char chPattern = token.charAt( i );
	  if( i == 0 ) {
	    ident = isIdentifierStart( chPattern );
	  } else {
	    ident = isIdentifierPart( chPattern );
	  }
	  if( ch != chPattern ) {
	    rv = false;
	    break;
	  }
	  ch = Character.toUpperCase( iter.next() );
	}

	/*
	 * Wenn auf einen Identifier geprueft wird,
	 * darf dahinter kein weiteres Identifier-Zeichen folgen.
	 */
	if( ident && isIdentifierPart( ch ) ) {
	  rv = false;
	}
      }
      finally {
	if( !rv ) {
	  iter.setIndex( begIdx );
	}
      }
      if( rv ) {
	break;
      }
    }
    return rv;
  }


  public static boolean checkComment( CharacterIterator iter )
  {
    boolean rv = false;
    char    ch = skipBlanks( iter );
    if( (ch == '!') || (ch == ';') ) {
      rv = true;
    } else if( ch == '/' ) {
      ch = iter.next();
      if( ch == '/' ) {
	rv = true;
      }
      iter.previous();
    }
    return rv;
  }


  public static String checkLabelName(
				String  labelName,
				boolean ignoreCase ) throws AsmException
  {
    int len = 0;
    if( labelName != null ) {
      if( ignoreCase ) {
	labelName = labelName.toUpperCase();
      }
      len = labelName.length();
    }
    if( len < 1 ) {
      throw new AsmException( "Name der Marke fehlt" );
    }
    char ch = labelName.charAt( 0 );
    if( !AsmUtil.isIdentifierStart( ch ) ) {
      if( AsmUtil.isPrintable( ch ) ) {
	throw new AsmException(
		String.format(
			"Zeichen \'%c\' am Anfang einer Marke nicht erlaubt",
			ch ) );
      }
      throw new AsmException(
			"Zeichen am Anfang der Marke nicht erlaubt" );
    }
    for( int i = 1; i < len; i++ ) {
      ch = labelName.charAt( i );
      if( !AsmUtil.isIdentifierPart( ch ) ) {
	if( AsmUtil.isPrintable( ch ) ) {
	  throw new AsmException(
		String.format(
			"Zeichen \'%c\' in einer Marke nicht erlaubt",
			ch ) );
	}
	throw new AsmException(
			"Zeichen in der Marke nicht erlaubt" );
      }
    }
    if( isReservedWord( labelName.toUpperCase() ) ) {
      throw new AsmException( "Reserviertes Schl\u00FCsselwort \'"
			+ labelName
			+ "\' als Marke nicht erlaubt" );
    }
    return labelName;
  }


  public static void ensureSourceFileExists( File file ) throws IOException
  {
    if( file == null ) {
      throw new IOException( "Sie m\u00FCssen zuerst die"
		+ " Assembler-Quelltextdatei speichern,\n"
		+ "bevor Sie das Projekt speichern k\u00F6nnen." );
    }
  }


  public static int getHexCharValue( char ch )
  {
    int value = 0;
    if( (ch >= '0') && (ch <= '9') ) {
      value = ch - '0';
    } else if( (ch >= 'A') && (ch <= 'F') ) {
      value = ch - 'A' + 10;
    } else if( (ch >= 'a') && (ch <= 'f') ) {
      value = ch - 'a' + 10;
    }
    return value;
  }


  public static File getProjectFilePreSelection(
					File prjFile,
					File srcFile )
  {
    if( prjFile == null ) {
      File   dirFile  = srcFile.getParentFile();
      String fileName = srcFile.getName();
      if( fileName != null ) {
	int idx = fileName.lastIndexOf( '.' );
	if( idx >= 0 ) {
	  fileName = fileName.substring( 0, idx );
	}
	fileName += ".prj";
	if( dirFile != null ) {
	  prjFile = new File( dirFile, fileName );
	} else {
	  prjFile = new File( fileName );
	}
      } else {
	prjFile = dirFile;
      }
    }
    return prjFile;
  }


  public static boolean isIdentifierPart( char ch )
  {
    return isIdentifierStart( ch ) || ((ch >= '0') && (ch <= '9'));
  }


  public static boolean isIdentifierStart( char ch )
  {
    return (ch == '_')
		|| ((ch >= 'A') && (ch <= 'Z'))
		|| ((ch >= 'a') && (ch <= 'z'));
  }


  public static boolean isPrintable( char ch )
  {
    return Character.isDefined( ch )
		&& (ch != CharacterIterator.DONE)
		&& !isBlank( ch );
  }


  public static boolean isReservedWord( String text )
  {
    return (Arrays.binarySearch( sortedReservedWords, text ) >= 0);
  }


  /*
   * Die Methode laedt eine Projektdatei.
   * Wenn die Datei als solche nicht geladen werden kann,
   * wird eine IOException geworfen.
   */
  public static Properties loadProject( File file ) throws IOException
  {
    Properties  props = null;
    InputStream in    = null;
    try {
      in    = new FileInputStream( file );
      props = new Properties();
      props.loadFromXML( in );
    }
    catch( InvalidPropertiesFormatException ex1 ) {
      props = null;
    }
    finally {
      JTCUtil.closeSilently( in );
    }
    if( props == null ) {
      throw new IOException( file.getPath() 
			+ ": Laden der Projektdatei nicht m\u00FCglich" );
    }
    String s = props.getProperty( PROP_PROPERTIES_TYPE );
    if( s != null ) {
      if( !s.equals( VALUE_PROPERTIES_TYPE_PROJECT ) ) {
	props = null;
      }
    } else {
      props = null;
    }
    if( props == null ) {
      throwNoPrjFile( file );
    }
    return props;
  }


  public static void parseToken(
			CharacterIterator iter,
			char              token ) throws AsmException
  {
    if( AsmUtil.skipBlanks( iter ) != token ) {
      throwCharExpected( token );
    }
    iter.next();
  }


  public static void saveProject(
			File       prjFile, 
			File       srcFile,
			AsmOptions options ) throws IOException
  {
    ensureSourceFileExists( srcFile );
    OutputStream out = null;
    try {
      Properties   props = new Properties();
      props.put( PROP_PROPERTIES_TYPE, VALUE_PROPERTIES_TYPE_PROJECT );
      props.put( PROP_SOURCE_FILE, srcFile.getPath() );
      if( options != null ) {
	options.putOptionsTo( props );
      }
      out = new BufferedOutputStream( new FileOutputStream( prjFile ) );
      props.storeToXML( out, "JTCEMU Assembler Project" );
      out.close();
      out = null;
    }
    finally {
      JTCUtil.closeSilently( out );
    }
  }


  public static char skipBlanks( CharacterIterator iter )
  {
    char ch = iter.current();
    while( isBlank( ch ) ) {
      ch = iter.next();
    }
    return ch;
  }


  public static void throwCharExpected( char ch ) throws AsmException
  {
    throw new AsmException(
		String.format( "Zeichen \'%c\' erwartet", ch ) );
  }


  public static void throwHexCharExpected( char ch ) throws AsmException
  {
    if( isPrintable( ch ) ) {
      throw new AsmException(
		String.format(
			"Hexadezimalziffer erwartet anstelle \'%c\'",
			ch ) );
    } else {
      throw new AsmException( "Hexadezimalziffer erwartet" );
    }
  }


  public static void throwInstructionWithoutIF( String instruction )
						throws AsmException
  {
    throw new AsmException( instruction + " ohne zugeh\u00F6riges $IF" );
  }


  public static void throwLabelAlreadyExists( String labelName )
						throws AsmException
  {
    throw new AsmException( "Marke \'" + labelName + "\' bereits definiert" );
  }


  public static void throwNoPrjFile( File file ) throws IOException
  {
    throw new IOException(
		String.format(
			"%s: Keine %s-Projektdatei",
			file.getPath(),
			AppContext.getAppName() ) );
  }


  public static void throwUnexpectedChar( char ch ) throws AsmException
  {
    if( ch == CharacterIterator.DONE ) {
      throw new AsmException( "Unerwartetes Zeilenende" );
    }
    if( isPrintable( ch ) ) {
      throw new AsmException(
		String.format( "Unerwartetes Zeichen \'%c\'", ch ) );
    }
    throw new AsmException( "Unerwartetes nichtdruckbares Zeichen" );
  }


  public static Integer tryReadConditionCode( CharacterIterator iter )
						throws AsmException
  {
    Integer code   = null;
    int     begIdx = iter.getIndex();
    try {
      String text = tryReadIdentifier( iter, true );
      if( text != null ) {
	switch( text ) {
	  case "C":
	    code = 0x70;
	    break;
	  case "EQ":
	    code = 0x60;
	    break;
	  case "F":
	    code = 0x00;
	    break;
	  case "GE":
	    code = 0x90;
	    break;
	  case "GT":
	    code = 0xA0;
	    break;
	  case "LE":
	    code = 0x20;
	    break;
	  case "LT":
	    code = 0x10;
	    break;
	  case "MI":
	    code = 0x50;
	    break;
	  case "NC":
	    code = 0xF0;
	    break;
	  case "NE":
	    code = 0xE0;
	    break;
	  case "NOV":
	    code = 0xC0;
	    break;
	  case "NZ":
	    code = 0xE0;
	    break;
	  case "OV":
	    code = 0x40;
	    break;
	  case "PL":
	    code = 0xD0;
	    break;
	  case "UGE":
	    code = 0xF0;
	    break;
	  case "UGT":
	    code = 0xB0;
	    break;
	  case "ULE":
	    code = 0x30;
	    break;
	  case "ULT":
	    code = 0x70;
	    break;
	  case "Z":
	    code = 0x60;
	    break;
	}
      }
    }
    finally {
      if( code == null ) {
	iter.setIndex( begIdx );
      }
    }
    return code;
  }


  public static String tryReadIdentifier(
				CharacterIterator iter,
				boolean           toUpper )
						throws AsmException
  {
    String text   = null;
    char   ch     = skipBlanks( iter );
    int    begIdx = iter.getIndex();
    try {
      if( isIdentifierStart( ch ) ) {
	StringBuilder buf = new StringBuilder();
	if( toUpper ) {
	  buf.append( Character.toUpperCase( ch ) );
	} else {
	  buf.append( ch );
	}
	ch = iter.next();
	while( isIdentifierPart( ch ) ) {
	  if( toUpper ) {
	    buf.append( Character.toUpperCase( ch ) );
	  } else {
	    buf.append( ch );
	  }
	  ch = iter.next();
	}
	text = buf.toString();
      }
    }
    finally {
      if( text == null ) {
	iter.setIndex( begIdx );
      }
    }
    return text;
  }


  public static Integer tryReadImmediateValue(
					CharacterIterator iter,
					Z8Assembler       asm )
						throws AsmException
  {
    Integer rv     = null;
    char    ch     = skipBlanks( iter );
    int     begIdx = iter.getIndex();
    try {
      if( ch == '#' ) {
	iter.next();
	if( checkAndParseToken( iter, "HIGH" )
	    || checkAndParseToken( iter, "HI" ) )
	{
	  rv = Integer.valueOf(
			(asm.parseIntExprPass2( iter ) >> 8) & 0xFF );
	}
	else if( checkAndParseToken( iter, "LOW" )
		 || checkAndParseToken( iter, "LO" ) )
	{
	  rv = Integer.valueOf(
			asm.parseIntExprPass2( iter ) & 0xFF );
	} else {
	  rv = Integer.valueOf(
			asm.check8Bit(
				asm.parseIntExprPass2( iter ) ) );
	}
      }
    }
    finally {
      if( rv == null ) {
	iter.setIndex( begIdx );
      }
    }
    return rv;
  }


  public static String tryReadStringLiteral( CharacterIterator iter )
						throws AsmException
  {
    String text   = null;
    char   ch     = skipBlanks( iter );
    int    begIdx = iter.getIndex();
    try {
      if( (ch == '\'') || (ch == '\"') ) {
	char encloseChar = ch;
	StringBuilder buf = new StringBuilder();
	ch = iter.next();
	while( (ch != CharacterIterator.DONE) && (ch != encloseChar) ) {
	  buf.append( ch );
	  ch = iter.next();
	}
	if( ch != encloseChar ) {
	  throw new AsmException( "Zeichenkette nicht geschlossen" );
	}
	iter.next();
	text = buf.toString();
      }
    }
    finally {
      if( text == null ) {
	iter.setIndex( begIdx );
      }
    }
    return text;
  }


  public static String tryReadWordSymbol( CharacterIterator iter )
  {
    String inst   = null;
    char   ch     = skipBlanks( iter );
    int    begIdx = iter.getIndex();
    try {
      if( isWordSymbolStart( ch ) ) {
	StringBuilder buf = new StringBuilder();
	buf.append( ch );
	ch = iter.next();
	while( isWordSymbolPart( ch ) ) {
	  buf.append( ch );
	  ch = iter.next();
	}
	inst = buf.toString();
      }
    }
    finally {
      if( inst == null ) {
	iter.setIndex( begIdx );
      }
    }
    return inst;
  }


	/* --- private Methoden --- */

  private static boolean isBlank( char ch )
  {
    return (ch == '\u0020')
		|| (ch == '\u00A0')
		|| (ch == '\r')
		|| (ch == '\n')
		|| (ch == '\t')
		|| Character.isWhitespace( ch );
  }


  private static boolean isWordSymbolPart( char ch )
  {
    return isIdentifierPart( ch ) || (ch == '.');
  }


  private static boolean isWordSymbolStart( char ch )
  {
    return isIdentifierStart( ch ) || (ch == '.') || (ch == '$');
  }


	/* --- Konstruktor --- */

  private AsmUtil()
  {
    // nicht instanziierbar
  }
}
