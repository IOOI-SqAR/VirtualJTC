/*
 * (c) 2020-2021 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Kommandozeilenschnittstelle des Assemblers
 */

package jtcemu.platform.se.tools.assembler;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.AbstractMap;
import java.util.ArrayList;
import jtcemu.base.FileSaver;
import jtcemu.platform.se.Main;
import jtcemu.tools.TextOutput;
import jtcemu.tools.assembler.AsmException;
import jtcemu.tools.assembler.AsmOptions;
import jtcemu.tools.assembler.AsmUtil;
import jtcemu.tools.assembler.ExprParser;
import jtcemu.tools.assembler.Z8Assembler;


public class CmdLineAssembler
{
  private static final String[] usageLines = {
	"",
	"Aufruf:",
	"  java -jar jtcemu.jar --as [Optionen] <Datei>",
	"  java -jar jtcemu.jar --assembler [Optionen] <Datei>",
	"",
	"Optionen:",
	"  -h              diese Hilfe anzeigen",
	"  -l              Markentabelle ausgeben",
	"  -o <Datei>      Ausgabedatei festlegen",
	"  -i              Gro\u00DF-/Kleinschreibung bei Marken ignorieren",
	"  -D <Marke>      Marke mit dem Wert %FFFF definieren",
	"  -D <Marke=Wert> Marke mit dem angegebenen Wert definieren",
	"" };


  public static boolean execute( String[] args, int argIdx )
  {
    boolean status      = false;
    boolean helpFlag    = false;
    String  outFileName = null;
    String  srcFileName = null;

    try {
      AsmOptions      options = new AsmOptions();
      CmdLineIterator iter    = new CmdLineIterator( args, argIdx );
      String          arg     = iter.next();
      while( arg != null ) {
	if( arg.charAt( 0 ) == '-' ) {
	  int len = arg.length();
	  if( len < 2 ) {
	    throwWrongCmdLine();
	  }
	  int pos = 1;
	  while( pos < len ) {
	    char ch = arg.charAt( pos++ );
	    switch( ch ) {
	      case 'h':
	      case 'H':
		helpFlag = true;
		break;
	      case 'D':
		{
		  String labelText = null;
		  if( pos < len ) {
		    labelText = arg.substring( pos );
		    pos       = len;		// Schleife verlassen
		  } else {
		    labelText = iter.next();
		  }
		  addLabel( options, labelText );
		}
		break;
	      case 'i':
		options.setLabelsIgnoreCase( true );
		break;
	      case 'l':
		options.setListLabels( true );
		break;
	      case 'o':
		if( outFileName != null ) {
		  throwWrongCmdLine();
		}
		if( pos < len ) {
		  outFileName = arg.substring( pos );
		  pos         = len;		// Schleife verlassen
		} else {
		  outFileName = iter.next();
		}
		if( outFileName == null ) {
		  throwWrongCmdLine();
		}
		break;
	      default:
		throw new IOException(
			String.format( "Unbekannte Option \'%c\'", ch ) );
	    }
	  }
	} else {
	  if( srcFileName != null ) {
	    throw new IOException( "Nur eine Quelltextdatei erlaubt" );
	  }
	  srcFileName = arg;
	}
	arg = iter.next();
      }
      if( helpFlag ) {
	Main.printlnOut();
	Main.printlnOut( Main.APPINFO + " Assembler" );
	for( String s : usageLines ) {
	  Main.printlnOut( s );
	}
      } else {
	if( srcFileName == null ) {
	  throw new IOException( "Quelltextdatei nicht angegeben" );
	}
	status = assemble( srcFileName, outFileName, options );
      }
    }
    catch( AsmException | IOException ex ) {
      Main.printlnErr();
      Main.printlnErr( Main.APPINFO + " Assembler:" );
      String msg = ex.getMessage();
      if( msg != null ) {
	if( !msg.isEmpty() ) {
	  Main.printlnErr( msg );
	}
      }
      for( String s : usageLines ) {
	Main.printlnErr( s );
      }
      status = false;
    }
    return status;
  }


	/* --- private Methoden --- */

  private static boolean assemble(
				String     srcFileName,
				String     outFileName,
				AsmOptions options )
  {
    boolean status = false;
    try {
      File srcFile = new File( srcFileName );
      File outFile = null;
      if( outFileName != null ) {
	outFile = new File( outFileName );
      } else {
	String fName = srcFile.getName();
	if( fName != null ) {
	  int pos = fName.lastIndexOf( '.' );
	  if( (pos >= 0) && (pos < fName.length()) ) {
	    fName = fName.substring( 0, pos );
	  }
	  fName += ".bin";
	} else {
	  fName = "out.bin";
	}
	File dirFile = srcFile.getParentFile();
        if( dirFile != null ) {
          outFile = new File( dirFile, fName );
        } else {
          outFile = new File( fName );
        }
      }
      if( outFile.equals( srcFile ) ) {
	throw new IOException( "Quelltext- und Ausgabedatei sind identisch" );
      }
      options.setCodeToFile( true, outFile );
      Z8Assembler asm = new Z8Assembler(
				null,
				null,
				srcFile,
				options,
				new TextOutput()
				{
				  @Override
				  public void print( String text )
				  {
				    Main.printErr( text );
				  }
				  @Override
				  public void println()
				  {
				    Main.printlnErr();
				  }
				},
				false,
				new TextOutput()
				{
				  @Override
				  public void print( String text )
				  {
				    Main.printOut( text );
				  }
				  @Override
				  public void println()
				  {
				    Main.printlnOut();
				  }
				} );
      byte[] codeBytes = asm.assemble();
      if( codeBytes != null ) {
	if( codeBytes.length > 0 ) {
	  Integer entryAddr = asm.getEntryAddr();
	  String  title     = asm.getTitle();
	  FileSaver.save(
		asm.getBegAddr(),
		entryAddr != null ? entryAddr.intValue() : -1,
		codeBytes,
		outFile,
		title != null ? title : AsmUtil.DEFAULT_APP_NAME );
	  status = true;
	}
      }
    }
    catch( IOException ex ) {
      String msg = ex.getMessage();
      if( msg != null ) {
	if( !msg.isEmpty() ) {
	  Main.printlnErr( msg );
	}
      }
    }
    return status;
  }


  private static void addLabel(
			AsmOptions options,
			String     text ) throws AsmException, IOException
  {
    if( text == null ) {
      throwWrongCmdLine();
    }
    String labelName = text;
    String valueText = null;
    int    pos       = text.indexOf( '=' );
    if( pos >= 0 ) {
      labelName = text.substring( 0, pos );
      valueText = text.substring( pos + 1 );
    }
    if( labelName.isEmpty() ) {
      throwWrongCmdLine();
    }
    AsmUtil.checkLabelName( labelName, options.getLabelsIgnoreCase() );
    int labelValue = -1;
    if( valueText != null ) {
      CharacterIterator iter = new StringCharacterIterator( valueText );
      if( iter.first() == CharacterIterator.DONE ) {
	throw new IOException(
		"Marke " + labelName + ": Wert fehlt" );
      }
      try {
	labelValue = ExprParser.parseNumber( iter );
      }
      catch( AsmException ex ) {
	throw new IOException(
		"Marke " + labelName + ": " + ex.getMessage() );
      }
      if( AsmUtil.skipBlanks( iter ) != CharacterIterator.DONE ) {
	throw new IOException( "Ung\u00FCltige Zahl bei Marke " + labelName );
      }
    }
    options.addLabel( labelName, labelValue );
  }


  private static void throwWrongCmdLine() throws IOException
  {
    throw new IOException( "Kommandozeile fehlerhaft" );
  }
}
