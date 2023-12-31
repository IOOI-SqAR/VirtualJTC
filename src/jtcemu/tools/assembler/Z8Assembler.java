/*
 * (c) 2019-2021 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Z8-Assembler
 */

package jtcemu.tools.assembler;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import jtcemu.base.JTCUtil;
import jtcemu.tools.TextOutput;
import jtcemu.tools.TooManyErrorsException;


public class Z8Assembler
{
  public static final String BUILT_IN_LABEL       = "__JTCEMU__";

  private static String[] sortedReservedWords = {
	"$ELSE", "$ELSEIF", "$ENDIF", "$EVEN", "$FI",
	"$IF", "$IFDEF", "$IFFALSE", "$IFNDEF", "$IFTRUE",
	"$LISTING", "$LISTOFF", "$LISTON", "$NEWPAGE", "$PAGE",
	"$THEN", ".ASSUME", ".BINCLUDE", ".BYTE", ".CPU",
	".ALIGN", ".DB", ".DB.B", ".DB.W",
	".DEFB", ".DEFS", ".DEFW",
	".DS", ".DS.B", ".DS.W", ".DW",
	".ELSE", ".ELSEIF", ".END", ".ENDIF", ".ENT", ".ENTRY", ".ERROR",
	".EQU", ".EQUAL", ".EVEN", ".FI", ".GLOBALS",
	".IF", ".IFDEF", ".IFFALSE", ".IFNDEF", ".IFTRUE", ".INCLUDE",
	".LISTING", ".LISTOFF", ".LISTON", ".NEWPAGE",
	".ORG", ".ORIGIN", ".PAGE", ".THEN", ".TITLE",
	".WARNING", ".WARNOFF", ".WARNON", ".WORD",
	"ADC", "ADD", "ALIGN", "AND", "ASSUME", "BINCLUDE", 
	"CALL", "CCF", "CLR", "COM", "CP", "CPU",
	"DA", "DB", "DB.B", "DB.W",
	"DEC", "DECW", "DEFB", "DEFS", "DEFW",
	"DI", "DJNZ", "DS", "DS.B", "DS.W", "DW",
	"EI", "ELSE", "ELSEIF", "END", "ENDIF", "ENT", "EQU", "ERROR",
	"EVEN", "GLOBALS", "HALT",
	"IF", "IFDEF", "IFFALSE", "IFNDEF", "IFTRUE",
	"INC", "INCLUDE", "INCW", "IRET",
	"JP", "JR",
	"LD", "LDC", "LDCI", "LDE", "LDEI",
	"LISTING", "LISTOFF", "LISTON", "NEWPAGE", "NOP",
	"OR", "ORG", "PAGE", "POP", "PUSH",
	"RCF", "RET", "RL", "RLC", "RR", "RRC",
	"SBC", "SCF", "SRA", "SRP", "STOP", "SUB", "SWAP",
	"TCM", "THEN", "TITLE", "TM",
	"WARNING", "WARNOFF", "WARNON", "WDH", "WDT", "XOR" };

  private static final String TEXT_ERROR = "Fehler";

  private AsmSource             curSource;
  private AsmSource             mainSource;
  private AsmOptions            options;
  private TextOutput            errOut;
  private TextOutput            lstOut;
  private Stack<AsmStackEntry>  stack;
  private Map<File,byte[]>      file2Bytes;
  private Map<File,AsmSource>   file2Source;
  private Map<String,AsmLabel>  labels;
  private AsmLabel[]            sortedLabels;
  private ByteArrayOutputStream codeBuf;
  private boolean               addrOverflow;
  private boolean               suppressLineAddr;
  private boolean               suppressErrSum;
  private boolean               status;
  private boolean               warnEnabled;
  private CPUType               cpu;
  private String                title;
  private Integer               entryAddr;
  private int                   begAddr;
  private int                   endAddr;
  private int                   curAddr;
  private int                   instBegAddr;
  private int                   passNum;
  private int                   errorCnt;


  public Z8Assembler(
		String     srcText,
		String     srcName,
		File       srcFile,
		AsmOptions options,
		TextOutput errOut,
		boolean    suppressErrSum,
		TextOutput lstOut )
  {
    this.curSource      = null;
    this.mainSource     = null;
    this.options        = options;
    this.errOut         = errOut;
    this.suppressErrSum = suppressErrSum;
    this.lstOut         = lstOut;
    this.codeBuf        = new ByteArrayOutputStream( 0x8000 );
    this.stack          = new Stack<>();
    this.file2Bytes     = new HashMap<>();
    this.file2Source    = new HashMap<>();
    this.labels         = new HashMap<>();
    reset();

    // Quelltext oeffnen
    if( srcText != null ) {
      this.mainSource = AsmSource.readText( srcText, srcName, srcFile );
    } else {
      if( srcFile != null ) {
	try {
	  this.mainSource = AsmSource.readFile( srcFile );
	}
	catch( IOException ex ) {
	  String msg = ex.getMessage();
	  if( msg != null ) {
	    if( msg.trim().isEmpty() ) {
	      msg = null;
	    }
	  }
	  if( msg == null ) {
	    msg = "Datei kann nicht ge\u00F6ffnet werden.";
	  }
	  printErr( srcFile.getPath() + ": " + msg );
	  printlnErr();
	}
      }
    }
  }


  public byte[] assemble() throws IOException
  {
    byte[] codeOut = null;
    try {
      reset();
      this.passNum = 1;
      parseAsm();
      if( this.status ) {
	computeMissingLabelValues();
	if( this.mainSource != null ) {
	  this.mainSource.reset();
	}
	this.passNum   = 2;
	this.curSource = this.mainSource;
	parseAsm();
	if( this.codeBuf != null ) {
	  this.codeBuf.close();
	  if( this.status ) {
	    codeOut = this.codeBuf.toByteArray();
	  }
	}
	if( this.status ) {
	  if( this.options.getListLabels() ) {
	    listLabels();
	  }
	}
      }
    }
    catch( TooManyErrorsException ex ) {
      if( !suppressErrSum ) {
	printErr( "Abgebrochen aufgrund zu vieler Fehler" );
	printlnErr();
      }
      codeOut = null;
    }
    if( this.errorCnt > 0 ) {
      if( !suppressErrSum ) {
	printErr( String.format( "%d Fehler", this.errorCnt ) );
	printlnErr();
      }
      codeOut = null;
    }
    return codeOut;
  }


  public int check8Bit( int value )
  {
    if( (this.passNum == 2)
	&& ((value < 0) || (value > 0xFF))
	&& (((short) ((byte) (value & 0xFF))) != ((short) value)) )
    {
      putWarning( "Numerischer Wert au\u00DFerhalb 8-Bit-Bereich:"
					+ " Bits werden ignoriert" );
    }
    return value & 0xFF;
  }


  public int checkRegNum8Bit( int regNum )
  {
    if( this.passNum == 2 ) {
      if( (regNum < 0) || (regNum > 0xFF) ) {
	putWarning(
		String.format(
			"Registernummer %d / %%%03X au\u00DFerhalb"
				+ " 8-Bit-Bereich: Bits werden ignoriert",
			regNum,
			regNum ) );
      }
    }
    return regNum & 0xFF;
  }


  public int checkRegNumExists( int regNum ) throws AsmException
  {
    regNum &= 0xFF;
    if( this.passNum == 2 ) {
      if( this.cpu != null ) {
	this.cpu.checkRegNum( regNum, this );
      } else {
	CPUType.defaultCheckRegNum( regNum, this );
      }
    }
    return regNum;
  }


  public int getBegAddr()
  {
    return this.begAddr;
  }


  public Integer getEntryAddr()
  {
    return this.entryAddr;
  }


  public int getErrorCount()
  {
    return this.errorCnt;
  }


  public int getInstBegAddr()
  {
    return this.instBegAddr;
  }


  public Map<String,AsmLabel> getLabels()
  {
    return this.labels;
  }


  public AsmOptions getOptions()
  {
    return this.options;
  }


  /*
   * Sortierte Ausgabe der Markentabelle,
   * Eingebaute Marken sind nicht enthalten.
   */
  public AsmLabel[] getSortedLabels()
  {
    AsmLabel[] rv = this.sortedLabels;
    if( rv == null ) {
      int nSrc = this.labels.size();
      if( nSrc > 0 ) {
	Map<String,AsmLabel> labelMap = this.labels;
	try {
	  labelMap = new HashMap<>( nSrc );
	  labelMap.putAll( this.labels );
	  labelMap.remove( BUILT_IN_LABEL );
	}
	catch( UnsupportedOperationException
		| ClassCastException
		| IllegalArgumentException ex )
	{
	  labelMap = this.labels;
	}
	try {
	  Collection<AsmLabel> c = labelMap.values();
	  if( c != null ) {
	    int nAry = c.size();
	    if( nAry > 0 ) {
	      rv = c.toArray( new AsmLabel[ nAry ] );
	      if( rv != null ) {
		Arrays.sort( rv );
	      }
	    } else {
	      rv = new AsmLabel[ 0 ];
	    }
	  }
	}
	catch( ArrayStoreException ex ) {}
	catch( ClassCastException ex ) {}
	finally {
	  this.sortedLabels = rv;
	}
      }
    }
    return rv;
  }


  public String getTitle()
  {
    return this.title;
  }


  public int parseIntExprPass2( CharacterIterator iter ) throws AsmException
  {
    return parseIntExpr( iter, this.passNum == 2 );
  }


  public void putWarning( String msg )
  {
    if( this.warnEnabled && (this.passNum == 2) )
      appendLineNumMsgToErrLog( msg, "Warnung" );
  }


	/* --- private Methoden --- */

  private AsmLabel addLabel( String labelName ) throws AsmException
  {
    labelName = AsmUtil.checkLabelName(
				labelName,
				this.options.getLabelsIgnoreCase() );
    AsmLabel label = this.labels.get( labelName );
    if( this.passNum < 2 ) {
      if( label != null ) {
	AsmUtil.throwLabelAlreadyExists( label.getLabelName() );
	label.setLabelValue( Integer.valueOf( this.curAddr ) );
      } else {
	label = new AsmLabel( labelName, this.curAddr );
	this.labels.put( labelName, label );
	this.sortedLabels = null;
      }
    }
    return label;
  }


  public void appendLineNumMsgToErrLog( String msg, String msgType )
  {
    StringBuilder buf = new StringBuilder( 128 );
    if( this.curSource != null ) {
      int lineNum = this.curSource.getLineNum();
      if( lineNum > 0 ) {
	String srcName = this.curSource.getName();
	if( srcName != null ) {
	  if( !srcName.isEmpty() ) {
	    buf.append( srcName );
	    buf.append( ": " );
	  }
	}
	if( msgType != null ) {
	  buf.append( msgType );
	  buf.append( " in " );
	}
	buf.append( "Zeile " );
	buf.append( lineNum );
	buf.append( ": " );
      }
    }
    if( msg != null ) {
      buf.append( msg );
    }
    int len = buf.length();
    if( len > 0 ) {
      if( buf.charAt( len - 1 ) == '\n' ) {
	buf.setLength( len - 1 );
      }
    }
    printErr( buf.toString() );
    printlnErr();
  }


  private void printErr( String text )
  {
    if( this.errOut != null )
      this.errOut.print( text );
  }


  private void printlnErr()
  {
    if( this.errOut != null )
      this.errOut.println();
  }


  private void checkAddr() throws AsmException
  {
    /*
     * Wenn Code bis zur Adresse 0xFFFF erzeugt wurde,
     * steht this.curAddr auf 0x10000.
     * Wurde dieser Wert allerdings ueberschritten,
     * liegt ein Adressueberlauf vor.
     */
    if( !this.addrOverflow && (this.curAddr > 0x10000) ) {
      this.addrOverflow = true;
      throw new AsmException( "\u00DCberlauf: Adressz\u00E4hler > 0FFFFh" );
    }
  }


  private boolean checkAndParseThen( CharacterIterator iter )
  {
    boolean rv = AsmUtil.checkAndParseToken( iter, "$THEN" );
    if( !rv ) {
      rv = AsmUtil.checkAndParseToken( iter, ".THEN" );
      if( !rv ) {
	rv = AsmUtil.checkAndParseToken( iter, "THEN" );
      }
    }
    return rv;
  }


  private char checkChar( char ch ) throws AsmException
  {
    if( ch > 0xFF ) {
      throw new AsmException(
		String.format(
			"16-Bit-Unicodezeichen \'%c\' nicht erlaubt",
			ch ) );
    }
    if( this.options.getWarnNonAsciiChars()
	&& ((ch < '\u0020') || (ch > '\u007E')) )
    {
      if( (ch > '\u0020') && Character.isDefined( ch ) ) {
	putWarning(
		String.format(
			"\'%c\' ist kein ASCII-Zeichen",
			ch ) );
      } else {
	putWarning(
		String.format(
			"Zeichen mit dem Code %02X ist kein ASCII-Zeichen",
			(int) ch ) );
      }
    }
    return ch;
  }


  private void checkRegReadable( int regNum )
  {
   if( (regNum == 0xF3) || ((regNum >= 0xF5) && (regNum <= 0xF9)) ) {
      putWarning( String.format(
			"Register %s nicht lesbar (Write Only Register)",
			RegArg.getRegText( regNum ) ) );
    }
  }


  private void checkRegWReadable( int regNum )
  {
    checkRegReadable( regNum & 0xFE );
    checkRegReadable( regNum | 0x01 );
  }


  private void checkWatchdog() throws AsmException
  {
    if( this.passNum == 2 ) {
      if( this.cpu != null ) {
	this.cpu.checkWatchdog();
      } else {
	putWarning( "Watchdog-Befehle nicht in allen Z8-CPUs"
					+ " unterst\u00FCtzt" );
      }
    }
  }


  private void computeMissingLabelValues()
  {
    boolean computed = false;
    boolean failed   = false;
    do {
      computed = false;
      failed   = false;
      for( AsmLabel label : this.labels.values() ) {
	Object o = label.getLabelValue();
	if( o != null ) {
	  if( !(o instanceof Integer) ) {
	    String text = o.toString();
	    if( text != null ) {
	      try {
		Integer v = ExprParser.parseExpr(
				new StringCharacterIterator( text ),
				this,
				false );
		if( v != null ) {
		  label.setLabelValue( v );
		  computed = true;
		} else {
		  failed = true;
		}
	      }
	      catch( AsmException ex ) {}
	    }
	  }
	}
      }
    } while( computed && failed );
  }


  private static boolean isReservedWord( String upperText )
  {
    return (Arrays.binarySearch( sortedReservedWords, upperText ) >= 0)
		|| AsmUtil.isReservedWord( upperText )
		|| ExprParser.isReservedWord( upperText )
		|| RegArg.isRegName( upperText );
  }


  private void parseAsm() throws IOException, TooManyErrorsException
  {
    this.begAddr     = -1;
    this.endAddr     = -1;
    this.curAddr     = 0;
    this.instBegAddr = 0;
    this.stack.clear();
    while( this.curSource != null ) {
      String line = this.curSource.readLine();
      if( line != null ) {
	parseLine( line );
      } else {
	if( this.curSource != this.mainSource ) {
	  this.curSource = this.mainSource;
	} else {
	  this.curSource = null;
	}
      }
    }
    if( !this.stack.isEmpty() ) {
      try {
	int lineNum = this.stack.peek().getLineNum();
	StringBuilder buf = new StringBuilder( 32 );
	buf.append( "Bedingung" );
	if( lineNum > 0 ) {
	  buf.append( " in Zeile " );
	  buf.append( lineNum );
	}
	buf.append( " nicht geschlossen (ENDIF fehlt)" );
	appendLineNumMsgToErrLog( buf.toString(), TEXT_ERROR );
	this.status = false;
	this.errorCnt++;
      }
      catch( EmptyStackException ex ) {}
    }
  }


  private void parseLine( String line )
				throws IOException, TooManyErrorsException
  {
    this.instBegAddr      = this.curAddr;
    this.suppressLineAddr = false;

    AsmLabel label = null;
    try {
      CharacterIterator iter        = new StringCharacterIterator( line );
      String            instruction = AsmUtil.tryReadWordSymbol( iter );
      boolean           asmEnabled  = true;
      boolean           ignoreInst  = false;
      for( AsmStackEntry e : this.stack ) {
	if( !e.isAssemblingEnabled() ) {
	  asmEnabled = false;
	  break;
	}
      }
      if( instruction != null ) {
	if( iter.current() == ':' ) {
	  iter.next();
	  if( asmEnabled ) {
	    label = addLabel( instruction );
	  }
	  instruction = AsmUtil.tryReadWordSymbol( iter );
	}
      }
      if( instruction != null ) {
	String upperInst = instruction.toUpperCase();
	switch( upperInst ) {
	  case ".ASSUME":
	  case "ASSUME":
	  case ".GLOBALS":
	  case "GLOBALS":
	  case "$LISTING":
	  case ".LISTING":
	  case "LISTING":
	  case "$LISTOFF":
	  case ".LISTOFF":
	  case "LISTOFF":
	  case "$LISTON":
	  case ".LISTON":
	  case "LISTON":
	  case "$NEWPAGE":
	  case ".NEWPAGE":
	  case "NEWPAGE":
	  case "$PAGE":
	  case ".PAGE":
	  case "PAGE":
	    ignoreInst = true;
	    break;
	  case "$IF":
	  case "$IFTRUE":
	  case ".IF":
	  case ".IFTRUE":
	  case "IF":
	  case "IFTRUE":
	    parseDirectiveIF( iter, true );
	    break;
	  case "$IFFALSE":
	  case ".IFFALSE":
	  case "IFFALSE":
	    parseDirectiveIF( iter, false );
	    break;
	  case "$IFDEF":
	  case ".IFDEF":
	  case "IFDEF":
	    parseDirectiveIFDEF( iter, true );
	    break;
	  case "$IFNDEF":
	  case ".IFNDEF":
	  case "IFNDEF":
	    parseDirectiveIFDEF( iter, false );
	    break;
	  case "$ELSEIF":
	  case ".ELSEIF":
	  case "ELSEIF":
	    parseDirectiveELSEIF( iter, instruction );
	    break;
	  case "$ELSE":
	  case ".ELSE":
	  case "ELSE":
	    parseDirectiveELSE( iter, instruction );
	    break;
	  case "$ENDIF":
	  case ".ENDIF":
	  case "ENDIF":
	  case "$FI":
	  case ".FI":
	    parseDirectiveENDIF( iter, instruction );
	    break;
	  default:
	    if( asmEnabled ) {
	      switch( upperInst ) {
		case "$EVEN":
		case ".EVEN":
		case "EVEN":
		  parseDirectiveEVEN( iter );
		  break;
		case ".ALIGN":
		case "ALIGN":
		  parsePseudoALIGN( iter );
		  break;
		case ".BINCLUDE":
		case "BINCLUDE":
		  parsePseudoBINCLUDE( iter );
		  break;
		case ".CPU":
		case "CPU":
		  parsePseudoCPU( iter );
		  break;
		case ".BYTE":
		case ".DB":
		case ".DB.B":
		case ".DEFB":
		case "DB":
		case "DB.B":
		case "DEFB":
		  parsePseudoDB( iter );
		  break;
		case ".DS":
		case ".DS.B":
		case ".DEFS":
		case "DS":
		case "DS.B":
		case "DEFS":
		  parsePseudoDS( iter, 1 );
		  break;
		case ".DS.W":
		case "DS.W":
		  parsePseudoDS( iter, 2 );
		  break;
		case ".DW":
		case ".DB.W":
		case ".WORD":
		case "DW":
		case "DB.W":
		  parsePseudoDW( iter );
		  break;
		case ".END":
		case "END":
		  parsePseudoEND( iter );
		  break;
		case ".ENT":
		case ".ENTRY":
		case "ENT":
		  parsePseudoENT( iter );
		  break;
		case ".EQU":
		case ".EQUAL":
		case "EQU":
		  parsePseudoEQU( iter, instruction, label );
		  break;
		case ".ERROR":
		case "ERROR":
		  parsePseudoERROR( iter );
		  break;
		case ".INCLUDE":
		case "INCLUDE":
		  parsePseudoINCLUDE( iter );
		  break;
		case ".ORG":
		case ".ORIGIN":
		case "ORG":
		  parsePseudoORG( iter );
		  break;
		case ".TITLE":
		case "TITLE":
		  parsePseudoTITLE( iter );
		  break;
		case ".WARNING":
		case "WARNING":
		  parsePseudoWARNING( iter );
		  break;
		case ".WARNOFF":
		case "WARNOFF":
		  this.warnEnabled = false;
		  break;
		case ".WARNON":
		case "WARNON":
		  this.warnEnabled = true;
		  break;
		case "ADC":
		  parseInstXYZwith2Args( iter, 0x10 );
		  break;
		case "ADD":
		  parseInstXYZwith2Args( iter, 0x00 );
		  break;
		case "AND":
		  parseInstXYZwith2Args( iter, 0x50 );
		  break;
		case "CALL":
		  parseInstCALL( iter );
		  break;
		case "CCF":
		  putCode( 0xEF );
		  break;
		case "CLR":
		  parseInstXYZwith1Arg( iter, 0xB0, false );
		  break;
		case "COM":
		  parseInstXYZwith1Arg( iter, 0x60, true );
		  break;
		case "CP":
		  parseInstXYZwith2Args( iter, 0xA0 );
		  break;
		case "DA":
		  parseInstXYZwith1Arg( iter, 0x40, true );
		  break;
		case "DEC":
		  parseInstXYZwith1Arg( iter, 0x00, true );
		  break;
		case "DECW":
		  parseInstXYZwith1ArgW( iter, 0x80 );
		  break;
		case "DI":
		  putCode( 0x8F );
		  break;
		case "DJNZ":
		  parseInstDJNZ( iter );
		  break;
		case "EI":
		  putCode( 0x9F );
		  break;
		case "HALT":
		  putCode( 0x7F );
		  break;
		case "INC":
		  parseInstINC( iter );
		  break;
		case "INCW":
		  parseInstXYZwith1ArgW( iter, 0xA0 );
		  break;
		case "IRET":
		  putCode( 0xBF );
		  break;
		case "JP":
		  parseInstJP( iter );
		  break;
		case "JR":
		  parseInstJR( iter );
		  break;
		case "LD":
		  parseInstLD( iter );
		  break;
		case "LDC":
		  parseInstLDX( iter, 0xC0 );
		  break;
		case "LDCI":
		  parseInstLDXI( iter, 0xC0 );
		  break;
		case "LDE":
		  parseInstLDX( iter, 0x80 );
		  break;
		case "LDEI":
		  parseInstLDXI( iter, 0x80 );
		  break;
		case "NOP":
		  putCode( 0xFF );
		  break;
		case "OR":
		  parseInstXYZwith2Args( iter, 0x40 );
		  break;
		case "POP":
		  parseInstXYZwith1Arg( iter, 0x50, false );
		  break;
		case "PUSH":
		  parseInstXYZwith1Arg( iter, 0x70, true );
		  break;
		case "RCF":
		  putCode( 0xCF );
		  break;
		case "RET":
		  putCode( 0xAF );
		  break;
		case "RL":
		  parseInstXYZwith1Arg( iter, 0x90, true );
		  break;
		case "RLC":
		  parseInstXYZwith1Arg( iter, 0x10, true );
		  break;
		case "RR":
		  parseInstXYZwith1Arg( iter, 0xE0, true );
		  break;
		case "RRC":
		  parseInstXYZwith1Arg( iter, 0xC0, true );
		  break;
		case "SBC":
		  parseInstXYZwith2Args( iter, 0x30 );
		  break;
		case "SCF":
		  putCode( 0xDF );
		  break;
		case "SRA":
		  parseInstXYZwith1Arg( iter, 0xD0, true );
		  break;
		case "SRP":
		  parseInstSRP( iter );
		  break;
		case "STOP":
		  putCode( 0x6F );
		  break;
		case "SUB":
		  parseInstXYZwith2Args( iter, 0x20 );
		  break;
		case "SWAP":
		  parseInstXYZwith1Arg( iter, 0xF0, true );
		  break;
		case "TCM":
		  parseInstXYZwith2Args( iter, 0x60 );
		  break;
		case "TM":
		  parseInstXYZwith2Args( iter, 0x70 );
		  break;
		case "WDH":
		  checkWatchdog();
		  putCode( 0x4F );
		  break;
		case "WDT":
		  checkWatchdog();
		  putCode( 0x5F );
		  break;
		case "XOR":
		  parseInstXYZwith2Args( iter, 0xB0 );
		  break;
		default:
		  /*
		   * Pruefen, ob es sich um einen .EQU- handelt,
		   * bei dem die Marke keinen angehaengten Doppelpunkt hat
		   */
		  boolean done = false;
		  if( label == null ) {
		    String instr2 = AsmUtil.tryReadWordSymbol( iter );
		    if( instr2 != null ) {
		      switch( instr2.toUpperCase() ) {
			case ".EQU":
			case ".EQUAL":
			case "EQU":
			  label = addLabel( instruction );
			  parsePseudoEQU( iter, instr2, label );
			  done = true;
			  break;
		      }
		    }
		  }
		  if( !done ) {
		    throw new AsmException(
			"Unbekannte Mnemonik \'" + instruction + "\'" );
		  }
	      }
	    }
	}
      }
      if( !ignoreInst ) {

	// Kommentar
	char ch = AsmUtil.skipBlanks( iter );
	if( asmEnabled
	    && (ch != CharacterIterator.DONE)
	    && !AsmUtil.checkComment( iter ) )
	{
	  AsmUtil.throwUnexpectedChar( ch );
	}
      }
    }
    catch( AsmException ex ) {
      putError( ex.getMessage() );
    }
    finally {
      if( !this.suppressLineAddr
	  && (this.curSource != null)
	  && (this.passNum == 2) )
      {
	if( this.instBegAddr < this.curAddr ) {
	  this.curSource.setLineAddr( this.instBegAddr );
	} else {
	  if( label != null ) {
	    if( label.hasIntValue()
		&& (label.intValue() == this.instBegAddr) )
	    {
	      this.curSource.setLineAddr( this.instBegAddr );
	    }
	  }
	}
      }
    }
  }


  private static void parseComma( CharacterIterator iter )
						throws AsmException
  {
    AsmUtil.parseToken( iter, ',' );
  }


  private void parseDirectiveELSE(
			CharacterIterator iter,
			String            instruction )
						throws AsmException
  {
    if( this.stack.isEmpty() ) {
      AsmUtil.throwInstructionWithoutIF( instruction );
    }
    try {
      this.stack.peek().invertAssemblingEnabled();
    }
    catch( EmptyStackException ex ) {}
  }


  private void parseDirectiveELSEIF(
			CharacterIterator iter,
			String            instruction ) throws
						AsmException,
						TooManyErrorsException
  {
    if( this.stack.isEmpty() ) {
      AsmUtil.throwInstructionWithoutIF( instruction );
    }
    try {
      this.stack.peek().setAssemblingEnabled(
				parseDirectiveIFCondition( iter ) );
    }
    catch( EmptyStackException ex ) {}
  }


  private void parseDirectiveENDIF(
			CharacterIterator iter,
			String            instruction )
						throws AsmException
  {
    if( this.stack.isEmpty() ) {
      AsmUtil.throwInstructionWithoutIF( instruction );
    }
    try {
      this.stack.pop();
    }
    catch( EmptyStackException ex ) {}
    AsmUtil.tryReadWordSymbol( iter );		// optionale Info
  }


  private void parseDirectiveIF(
			CharacterIterator iter,
			boolean           initState ) throws
						AsmException,
						TooManyErrorsException
  {
    boolean state = false;
    try {
      state = parseDirectiveIFCondition( iter );
      if( !initState ) {
	state = !state;
      }
    }
    finally {
      this.stack.push(
		new AsmStackEntry( this.curSource.getLineNum(), state ) );
    }
  }


  private boolean parseDirectiveIFCondition(
			CharacterIterator iter ) throws
						AsmException,
						TooManyErrorsException
  {
    boolean state  = false;
    boolean done   = false;
    char    ch     = AsmUtil.skipBlanks( iter );
    int     begIdx = iter.getIndex();

    // auf einzelne Marke pruefen
    String labelName = AsmUtil.tryReadIdentifier(
				iter,
				this.options.getLabelsIgnoreCase() );
    if( labelName != null ) {
      if( (AsmUtil.skipBlanks( iter ) == CharacterIterator.DONE)
	  || checkAndParseThen( iter )
	  || AsmUtil.checkComment( iter ) )
      {
	AsmLabel label = this.labels.get( labelName );
	if( label != null ) {
	  if( label.hasIntValue() ) {
	    state = (label.intValue() != 0);
	  } else {
	    /*
	     * Fehler hier ausgeben und keine Exception werfen,
	     * damit bei $ELSEIF, $ELSE oder $ENDIF
	     * kein Strukturfehler auftritt
	     */
	    putError( "Marke \'" + labelName
			+ "\' hat unbestimmten Wert"
			+ " (Vorw\u00E4rtsreferenzen an der Stelle"
			+ " nicht erlaubt)" );
	  }
	}
	done = true;
      }
    }
    if( !done ) {
      iter.setIndex( begIdx );
      state = (parseIntExpr( iter, true ) != 0);
      checkAndParseThen( iter );
    }
    return state;
  }


  private void parseDirectiveIFDEF(
			CharacterIterator iter,
			boolean           initState )
						throws AsmException
  {
    boolean state = false;
    try {
      String labelName = AsmUtil.tryReadIdentifier(
				iter,
				this.options.getLabelsIgnoreCase() );
      if( labelName == null ) {
	throw new AsmException( "Marke erwartet" );
      }
      state = this.labels.containsKey( labelName );
      if( !initState ) {
	state = !state;
      }
      checkAndParseThen( iter );
    }
    finally {
      this.stack.push(
		new AsmStackEntry( this.curSource.getLineNum(), state ) );
    }
  }


  private void parseInstCALL( CharacterIterator iter ) throws AsmException
  {
    char ch = AsmUtil.skipBlanks( iter );
    if( ch == '@' ) {
      RegArg regArg = RegArg.parseRegArg( iter, this );
      if( regArg == null ) {
	throwNoSuchInstArgs();
      }
      if( !regArg.isIndirectReg() ) {
	throwNoSuchInstArgs();
      }
      putCode( 0xD4 );
      putCode( regArg.getDoubleRegularRegNum( this ) );
    } else {
      putCode( 0xD6 );
      putCodeWord( parseIntExprPass2( iter ) );
    }
  }


  private void parseInstDJNZ( CharacterIterator iter ) throws AsmException
  {
    RegArg regArg = RegArg.parseRegArg( iter, this );
    if( !regArg.isDirectSingleWorkingReg() ) {
      throw new AsmException( "Working Register erwartet" );
    }
    parseComma( iter );
    putCodeNibbles( regArg.getWorkingRegNum(), 0x0A );
    putCode( parseRelAddr( iter ) );
  }


  private void parseInstINC( CharacterIterator iter ) throws AsmException
  {
    RegArg regArg = RegArg.parseRegArg( iter, this );
    if( regArg.isDirectSingleWorkingReg() ) {
      putCodeNibbles( regArg.getWorkingRegNum(), 0x0E );
    } else if( regArg.isDirectSingleReg() ) {
      putCode( 0x20 );
      putCode( regArg.getRegularRegNum( this ) );
    } else if( regArg.isIndirectSingleReg() ) {
      putCode( 0x21 );
      putCode( regArg.getRegularRegNum( this ) );
    } else {
      throwNoSuchInstArgs();
    }
  }


  private void parseInstJP( CharacterIterator iter ) throws AsmException
  {
    Integer c = AsmUtil.tryReadConditionCode( iter );
    if( c != null ) {
      parseComma( iter );
      putCode( c.intValue() | 0x0D );
      putCodeWord( parseIntExprPass2( iter ) );
    } else {
      char ch = AsmUtil.skipBlanks( iter );
      if( ch == '@' ) {
	RegArg regArg = RegArg.parseRegArg( iter, this );
	if( !regArg.isIndirectReg() ) {
	  throwNoSuchInstArgs();
	}
	putCode( 0x30 );
	putCode( regArg.getDoubleRegularRegNum( this ) );
      } else {
	putCode( 0x8D );
	putCodeWord( parseIntExprPass2( iter ) );
      }
    }
  }


  private void parseInstJR( CharacterIterator iter ) throws AsmException
  {
    Integer c = AsmUtil.tryReadConditionCode( iter );
    if( c != null ) {
      parseComma( iter );
      putCode( c.intValue() | 0x0B );
    } else {
      putCode( 0x8B );
    }
    putCode( parseRelAddr( iter ) );
  }


  private void parseInstLD( CharacterIterator iter ) throws AsmException
  {
    boolean done   = false;
    RegArg  dstArg = RegArg.parseRegArg( iter, this );
    parseComma( iter );
    Integer immediateValue = AsmUtil.tryReadImmediateValue( iter, this );
    if( immediateValue != null ) {
      if( dstArg.isDirectSingleWorkingReg() ) {
	putCodeNibbles( dstArg.getWorkingRegNum(), 0x0C );
	putCode( immediateValue.intValue() );
	done = true;
      } else if( dstArg.isDirectSingleReg() ) {
	putCode( 0xE6 );
	putCode( dstArg.getRegularRegNum( this ) );
	putCode( immediateValue.intValue() );
	done = true;
      } else if( dstArg.isIndirectSingleReg() ) {
	putCode( 0xE7 );
	putCode( dstArg.getRegularRegNum( this ) );
	putCode( immediateValue.intValue() );
	done = true;
      }
    } else {
      RegArg srcArg = RegArg.parseRegArg( iter, this );
      if( srcArg.isDirectSingleReg() || srcArg.isIndirectSingleReg() ) {
	checkRegReadable( srcArg.getRegNum() );
      }
      if( dstArg.isDirectSingleWorkingReg() ) {
	if( srcArg.isDirectSingleReg() ) {
	  putCodeNibbles( dstArg.getWorkingRegNum(), 0x08 );
	  putCode( srcArg.getRegularRegNum( this ) );
	  done = true;
	} else if( srcArg.isIndirectSingleWorkingReg() ) {
	  putCode( 0xE3 );
	  putCodeNibbles(
		dstArg.getWorkingRegNum(),
		srcArg.getWorkingRegNum() );
	  done = true;
	} else if( srcArg.isOffsetSingleWorkingReg() ) {
	  putCode( 0xC7 );
	  putCodeNibbles(
		dstArg.getWorkingRegNum(),
		srcArg.getWorkingRegNum() );
	  putCode( srcArg.getOffset( this ) );
	  done = true;
	}
      }
      if( !done && dstArg.isIndirectSingleWorkingReg() ) {
	if( srcArg.isDirectSingleWorkingReg() ) {
	  putCode( 0xF3 );
	  putCodeNibbles(
		dstArg.getWorkingRegNum(),
		srcArg.getWorkingRegNum() );
	  done = true;
	}
      }
      if( !done && dstArg.isOffsetSingleWorkingReg() ) {
	if( srcArg.isDirectSingleWorkingReg() ) {
	  putCode( 0xD7 );
	  putCodeNibbles(
		srcArg.getWorkingRegNum(),
		dstArg.getWorkingRegNum() );
	  putCode( dstArg.getOffset( this ) );
	  done = true;
	}
      }
      if( !done && dstArg.isDirectSingleReg() ) {
	if( srcArg.isDirectSingleWorkingReg() ) {
	  putCodeNibbles( srcArg.getWorkingRegNum(), 0x09 );
	  // Zielregister E0..EF ist hier regulaeres Register!
	  putCode(
		checkRegNumExists(
			checkRegNum8Bit( dstArg.getRegNum() ) ) );
	  done = true;
	} else if( srcArg.isDirectSingleReg() ) {
	  putCode( 0xE4 );
	  putCode( srcArg.getRegularRegNum( this ) );
	  putCode( dstArg.getRegularRegNum( this ) );
	  done = true;
	} else if( srcArg.isIndirectSingleReg() ) {
	  putCode( 0xE5 );
	  putCode( srcArg.getRegularRegNum( this ) );
	  putCode( dstArg.getRegularRegNum( this ) );
	  done = true;
	}
      }
      if( !done && dstArg.isIndirectSingleReg() ) {
	if( srcArg.isDirectSingleReg() ) {
	  putCode( 0xF5 );
	  putCode( srcArg.getRegularRegNum( this ) );
	  putCode( dstArg.getRegularRegNum( this ) );
	  done = true;
	}
      }
    }
    if( !done ) {
      throwNoSuchInstArgs();
    }
  }


  private void parseInstLDX(
			CharacterIterator iter,
			int               baseCode ) throws AsmException
  {
    boolean done   = false;
    RegArg  dstArg = RegArg.parseRegArg( iter, this );
    parseComma( iter );
    RegArg srcArg = RegArg.parseRegArg( iter, this );
    if( dstArg.isDirectSingleWorkingReg() ) {
      if( srcArg.isIndirectWorkingReg() ) {
	putCode( baseCode + 0x02 );
	putCodeNibbles(
		dstArg.getWorkingRegNum(),
		srcArg.getDoubleWorkingRegNum( this ) );
	done = true;
      }
    } else if( dstArg.isIndirectWorkingReg() ) {
      if( srcArg.isDirectSingleWorkingReg() ) {
	putCode( baseCode + 0x12 );
	putCodeNibbles(
		srcArg.getWorkingRegNum(),
		dstArg.getDoubleWorkingRegNum( this ) );
	done = true;
      }
    }
    if( !done ) {
      throwNoSuchInstArgs();
    }
  }


  private void parseInstLDXI(
			CharacterIterator iter,
			int               baseCode ) throws AsmException
  {
    boolean done   = false;
    RegArg  dstArg = RegArg.parseRegArg( iter, this );
    parseComma( iter );
    RegArg srcArg = RegArg.parseRegArg( iter, this );
    if( dstArg.isIndirectSingleWorkingReg() ) {
      if( srcArg.isIndirectSingleWorkingReg() ) {
	throw new AsmException(
		"Ein Argument muss indirektes Doppelregister sein" );
      }
      if( srcArg.isIndirectWorkingReg() ) {
	putCode( baseCode + 0x03 );
	putCodeNibbles(
		dstArg.getWorkingRegNum(),
		srcArg.getDoubleWorkingRegNum( this ) );
	done = true;
      }
    } else if( dstArg.isIndirectWorkingReg() ) {
      if( srcArg.isIndirectSingleWorkingReg() ) {
	putCode( baseCode + 0x13 );
	putCodeNibbles(
		srcArg.getWorkingRegNum(),
		dstArg.getDoubleWorkingRegNum( this ) );
	done = true;
      }
    }
    if( !done ) {
      throwNoSuchInstArgs();
    }
  }


  private void parseInstSRP( CharacterIterator iter ) throws AsmException
  {
    Integer value = AsmUtil.tryReadImmediateValue( iter, this );
    if( value == null ) {
      throwNoSuchInstArgs();
    }
    putCode( 0x31 );
    putCode( value.intValue() );
  }


  private void parseInstXYZwith1Arg(
			CharacterIterator iter,
			int               baseCode,
			boolean           readAccess ) throws AsmException
  {
    RegArg regArg = RegArg.parseRegArg( iter, this );
    if( regArg.isDirectSingleReg() ) {
      if( readAccess ) {
	checkRegReadable( regArg.getRegNum() );
      }
      putCode( baseCode );
      putCode( regArg.getRegularRegNum( this ) );
    } else if( regArg.isIndirectSingleReg() ) {
      if( readAccess ) {
	checkRegReadable( regArg.getRegNum() );
      }
      putCode( baseCode + 0x01 );
      putCode( regArg.getRegularRegNum( this ) );
    } else {
      throwNoSuchInstArgs();
    }
  }


  private void parseInstXYZwith1ArgW(
			CharacterIterator iter,
			int               baseCode ) throws AsmException
  {
    RegArg regArg = RegArg.parseRegArg( iter, this );
    if( regArg.isDirectReg() ) {
      checkRegWReadable( regArg.getRegNum() );
      putCode( baseCode );
      putCode( regArg.getDoubleRegularRegNum( this ) );
    } else if( regArg.isIndirectSingleReg() ) {
      checkRegWReadable( regArg.getRegNum() );
      putCode( baseCode + 0x01 );
      putCode( regArg.getRegularRegNum( this ) );
    } else {
      throwNoSuchInstArgs();
    }
  }


  private void parseInstXYZwith2Args(
			CharacterIterator iter,
			int               baseCode ) throws AsmException
  {
    boolean done   = false;
    RegArg  dstArg = RegArg.parseRegArg( iter, this );
    if( dstArg.isDirectSingleReg() || dstArg.isIndirectSingleReg() ) {
      checkRegReadable( dstArg.getRegNum() );
    }
    parseComma( iter );
    Integer v = AsmUtil.tryReadImmediateValue( iter, this );
    if( v != null ) {
      if( dstArg.isDirectSingleReg() ) {
	putCode( baseCode + 0x06 );
	putCode( dstArg.getRegularRegNum( this ) );
	putCode( v.intValue() );
	done = true;
      } else if( dstArg.isIndirectSingleReg() ) {
	putCode( baseCode + 0x07 );
	putCode( dstArg.getRegularRegNum( this ) );
	putCode( v.intValue() );
	done = true;
      }
    } else {
      RegArg srcArg = RegArg.parseRegArg( iter, this );
      if( dstArg.isDirectSingleWorkingReg() ) {
	if( srcArg.isDirectSingleWorkingReg() ) {
	  putCode( baseCode + 0x02 );
	  putCodeNibbles(
			dstArg.getWorkingRegNum(),
			srcArg.getWorkingRegNum() );
	  done = true;
	} else if( srcArg.isIndirectSingleWorkingReg() ) {
	  putCode( baseCode + 0x03 );
	  putCodeNibbles(
			dstArg.getWorkingRegNum(),
			srcArg.getWorkingRegNum() );
	  done = true;
	}
      }
      if( !done && dstArg.isDirectSingleReg() ) {
	if( srcArg.isDirectSingleReg() ) {
	  checkRegReadable( srcArg.getRegNum() );
	  putCode( baseCode + 0x04 );
	  putCode( srcArg.getRegularRegNum( this ) );
	  putCode( dstArg.getRegularRegNum( this ) );
	  done = true;
	} else if( srcArg.isIndirectSingleReg() ) {
	  checkRegReadable( srcArg.getRegNum() );
	  putCode( baseCode + 0x05 );
	  putCode( srcArg.getRegularRegNum( this ) );
	  putCode( dstArg.getRegularRegNum( this ) );
	  done = true;
	}
      }
    }
    if( !done ) {
      throwNoSuchInstArgs();
    }
  }


  private void parsePseudoBINCLUDE( CharacterIterator iter )
						throws AsmException
  {
    File   file      = getIncludeFile( iter );
    byte[] fileBytes = this.file2Bytes.get( file );
    try {
      if( fileBytes == null ) {
	fileBytes = JTCUtil.readFile( file, 0x10000 );
	this.file2Bytes.put( file, fileBytes );
      }
      if( fileBytes == null ) {
	throw new IOException(
		file.getPath() + ": Datei konnte nicht gelesen werden." );
      }
      for( byte b : fileBytes ) {
	putCode( b );
      }
    }
    catch( IOException ex ) {
      String msg = ex.getMessage();
      if( msg != null ) {
	if( msg.trim().isEmpty() ) {
	  msg = null;
	}
      }
      if( msg == null ) {
	msg = "Datei kann nicht ge\u00F6ffnet werden.";
      }
      throw new AsmException( msg );
    }
  }


  private void parsePseudoCPU( CharacterIterator iter )
						throws AsmException
  {
    if( AsmUtil.skipBlanks( iter ) == '=' ) {
      iter.next();
    }
    String text = AsmUtil.tryReadWordSymbol( iter );
    if( text == null ) {
      text = AsmUtil.tryReadStringLiteral( iter );
    }
    if( text == null ) {
      AsmUtil.throwUnexpectedChar( iter.current() );
    }
    if( this.passNum == 2 ) {
      if( this.cpu != null ) {
	throw new AsmException( "CPU bereits angegeben" );
      }
      this.cpu = CPUType.getByName( text );
    }
  }


  private void parsePseudoDB( CharacterIterator iter ) throws AsmException
  {
    for(;;) {
      boolean chLiteral = false;
      if( AsmUtil.skipBlanks( iter ) == '\'' ) {
	int begPos = iter.getIndex();
	try {
	  if( iter.next() != CharacterIterator.DONE ) {
	    if( iter.next() == '\'' ) {
	      chLiteral = true;
	    }
	  }
	}
	finally {
	  iter.setIndex( begPos );
	}
      }
      String text = null;
      if( !chLiteral ) {
	text = AsmUtil.tryReadStringLiteral( iter );
      }
      if( text != null ) {
	CharacterIterator iter2 = new StringCharacterIterator( text );
	char              ch    = iter2.first();
	while( ch != CharacterIterator.DONE ) {
	  if( ch == '%' ) {
	    ch = iter2.next();
	    switch( ch ) {
	      case '%':
		// Prozentzeichen
		break;
	      case 'L':
	      case 'l':
		ch = '\n';
		break;
	      case 'P':
	      case 'p':
		ch = '\f';
		break;
	      case 'Q':
	      case 'q':
		ch = '\'';
		break;
	      case 'R':
	      case 'r':
		ch = '\r';
		break;
	      case 'T':
	      case 't':
		ch = '\t';
		break;
	      default:
		if( JTCUtil.isHexChar( ch ) ) {
		  int h = AsmUtil.getHexCharValue( ch );
		  ch    = iter2.next();
		  if( !JTCUtil.isHexChar( ch ) ) {
		    AsmUtil.throwHexCharExpected( ch );
		  }
		  ch = (char) (((h << 4) & 0xF0)
				| (AsmUtil.getHexCharValue( ch ) & 0x0F));
		} else {
		  if( ch == CharacterIterator.DONE ) {
		    throw new AsmException(
				"Unerwartetes Ende der Zeichensequenz" );
		  }
		  StringBuilder buf = new StringBuilder();
		  buf.append( "Ung\u00FCltige Zeichensequenz" );
		  if( (ch > '\u0020') && Character.isDefined( ch ) ) {
		    buf.append( " \'%" );
		    buf.append( ch );
		    buf.append( "\'" );
		  }
		  throw new AsmException( buf.toString() );
		}
	    }
	    putCode( ch );
	  } else {
	    putCode( checkChar( ch ) );
	  }
	  ch = iter2.next();
	}
      } else {
	putCode( check8Bit( parseIntExprPass2( iter ) ) );
      }
      if( AsmUtil.skipBlanks( iter ) != ',' ) {
	break;
      }
      iter.next();
    }
  }


  private void parsePseudoDS(
			CharacterIterator iter,
			int               factor ) throws AsmException
  {
    for(;;) {
      skipCode( parseIntExpr( iter, true ) * factor );
      if( AsmUtil.skipBlanks( iter ) != ',' ) {
	break;
      }
      iter.next();
    }
  }


  private void parsePseudoDW( CharacterIterator iter ) throws AsmException
  {
    for(;;) {
      putCodeWord( parseIntExprPass2( iter ) );
      if( AsmUtil.skipBlanks( iter ) != ',' ) {
	break;
      }
      iter.next();
    }
  }


  private void parsePseudoEND( CharacterIterator iter ) throws AsmException
  {
    if( this.curSource != null ) {
      this.curSource.setEOF();
    }
    AsmUtil.tryReadWordSymbol( iter );		// optionale Info
  }


  private void parsePseudoENT( CharacterIterator iter ) throws AsmException
  {
    if( this.passNum == 1 ) {
      if( this.entryAddr != null ) {
	throw new AsmException( "Mehrfache ENT-Anweisungen nicht erlaubt" );
      }
      this.entryAddr = this.curAddr;
    }
  }


  private void parsePseudoEQU(
			CharacterIterator iter,
			String            instruction,
			AsmLabel          label ) throws AsmException
  {
    this.suppressLineAddr = true;
    if( label == null ) {
      throw new AsmException( instruction + " ohne Marke" );
    }

    /*
     * Die Marke wurde bereits beim Parsen der Zeile erkannt
     * und der Markentabelle hinzugefuegt.
     * Hier muss nur der Wert korrigiert werden.
     * Waehrend der Berechnung des Werts wird die Marke
     * von der Markentabelle entfernt,
     * damit es zu keiner Falschberechnung kommt,
     * falls die Marke selbst im Ausdruck steht.
     */
    this.labels.remove( label.getLabelName() );
    Object value = null;
    try {
      AsmUtil.skipBlanks( iter );
      int begIdx = iter.getIndex();
      value      = ExprParser.parseExpr( iter, this, false );
      if( (value == null) && (this.passNum == 1) ) {
	/*
	 * Wenn im Lauf 1 der Wert nicht errechnet werden kann,
	 * dann den Text des Ausdrucks speichern,
	 * um die Berechnung spaeter erneut zu versuchen.
	 */
	StringBuilder buf    = new StringBuilder();
	int           endIdx = iter.getIndex();
	iter.setIndex( begIdx );
	char ch = iter.current();
	while( (ch != CharacterIterator.DONE)
	       && (iter.getIndex() < endIdx) )
	{
	  buf.append( ch );
	  ch = iter.next();
	}
	value = buf.toString();
      }
    }
    finally {
      switch( this.passNum ) {
	case 1:
	  label.setLabelValue( value );
	  break;
	case 2:
	  if( (value != null) && !label.hasIntValue() ) {
	    label.setLabelValue( value );
	  }
	  break;
      }
      this.labels.put( label.getLabelName(), label );
    }
  }


  private void parsePseudoERROR( CharacterIterator iter )
						throws AsmException
  {
    String msg = AsmUtil.tryReadStringLiteral( iter );
    if( msg == null ) {
      throw new AsmException( "Zeichenkette erwartet" );
    }
    appendLineNumMsgToErrLog( msg, TEXT_ERROR );
    this.status = false;
    this.errorCnt++;
  }


  private void parseDirectiveEVEN( CharacterIterator iter )
						throws AsmException
  {
    if( (this.curAddr & 0x0001) != 0 ) {
      putCode( 0xFF );
    }
  }


  private void parsePseudoALIGN( CharacterIterator iter )
						throws AsmException
  {
    int b = 0xFF;
    int v = ExprParser.parseNumber( iter );
    if( (v < 1) || (Integer.bitCount( v ) != 1) ) {
      throw new AsmException( "Zweierpotenz (1, 2, 4, 8, %10, %20 usw.)"
				      + " als Argument erwartet" );
    }
    if( AsmUtil.checkAndParseToken( iter, "," ) ) {
      b = check8Bit( parseIntExpr( iter, this.passNum == 2 ) );
    }
    if( v > 1 ) {
      --v;
      while( (this.curAddr & v) != 0 ) {
	putCode( b );
      }
    }
  }


  private void parsePseudoINCLUDE( CharacterIterator iter )
						throws AsmException
  {
    File file = getIncludeFile( iter );
    if( this.curSource != this.mainSource ) {
      throw new AsmException(
		"In sich geschachtelte INCLUDE-Befehle nicht erlaubt" );
    }
    AsmSource source = this.file2Source.get( file );
    if( source != null ) {
      source.reset();
      this.curSource = source;
    } else {
      try {
	source = AsmSource.readFile( file );
	this.file2Source.put( file, source );
	this.curSource = source;
      }
      catch( IOException ex ) {
	String msg = ex.getMessage();
	if( msg != null ) {
	  if( msg.trim().isEmpty() ) {
	    msg = null;
	  }
	}
	if( msg == null ) {
	  msg = "Datei kann nicht ge\u00F6ffnet werden.";
	}
	throw new AsmException( msg );
      }
    }
  }


  private void parsePseudoORG( CharacterIterator iter )
						throws AsmException
  {
    this.suppressLineAddr = true;

    int filler = 0xFF;
    int addr   = ExprParser.parseNumber( iter );
    if( addr < this.curAddr ) {
      throw new AsmException(
		String.format(
			"Zur\u00FCcksetzen des Addressz\u00E4hlers"
				+ " von %%%04X auf %%%04X nicht erlaubt",
			this.curAddr,
			addr ) );
    }
    if( (this.curAddr > 0) && (addr > this.curAddr) ) {
      skipCode( addr - this.curAddr );
    }
    this.curAddr = addr;
  }


  private void parsePseudoTITLE( CharacterIterator iter )
						throws AsmException
  {
    String text = AsmUtil.tryReadWordSymbol( iter );
    if( text == null ) {
      text = AsmUtil.tryReadStringLiteral( iter );
    }
    if( text == null ) {
      AsmUtil.throwUnexpectedChar( iter.current() );
    }
    if( this.passNum == 1 ) {
      if( this.title != null ) {
	throw new AsmException( "Titel bereits angegeben" );
      }
      this.title = text;
    }
  }


  private void parsePseudoWARNING( CharacterIterator iter )
						throws AsmException
  {
    String msg = AsmUtil.tryReadStringLiteral( iter );
    if( msg != null ) {
      putWarning( msg );
    } else {
      throwNoSuchInstArgs();
    }
  }


  /*
   * Zum Zeitpunkt des Aufrufs der Methode wurde bereits
   * das erste Code-Byte des Sprungbefehls schon erzeugt.
   * Aus diesem Grund muss die Sprungdistanz nur um eins erhoeht werden.
   */
  private int parseRelAddr( CharacterIterator iter ) throws AsmException
  {
    int v = parseIntExprPass2( iter );
    if( this.passNum == 2 ) {
      v = v - (this.curAddr + 1);
      if( (v < -0x80) || (v > 0x7F) ) {
	throw new AsmException( "Relative Sprungdistanz zu gro\u00DF" );
      }

    }
    return v & 0xFF;
  }


  private File getIncludeFile( CharacterIterator iter ) throws AsmException
  {
    String fileName = AsmUtil.tryReadStringLiteral( iter );
    if( fileName == null ) {
      fileName = AsmUtil.tryReadWordSymbol( iter );
    }
    if( fileName == null ) {
      throw new AsmException(
		"Dateiname oder String-Literal mit Dateinamen erwartet" );
    }
    return AsmSource.getIncludeFile( this.curSource, fileName );
  }


  private int parseIntExpr(
		CharacterIterator iter,
		boolean           needsResult ) throws AsmException
  {
    int     rv    = 0;
    Integer value = ExprParser.parseExpr( iter, this, needsResult );
    if( value != null ) {
      rv = value.intValue();
    } else {
      if( needsResult ) {
	throw new AsmException( "Wert des Ausdrucks nicht ermittelbar"
			+ " (nicht aufl\u00F6sbare Vorw\u00E4rtsreferenz"
			+ " in einer Marke)" );
      }
    }
    return rv;
  }


  private void putCode( int b ) throws AsmException
  {
    if( (this.codeBuf != null) && (this.passNum == 2) ) {
      if( this.begAddr < 0 ) {
	this.begAddr = this.curAddr;
      } else {
	if( this.endAddr + 1 < this.curAddr ) {
	  int n = this.curAddr - this.endAddr - 1;
	  for( int i = 0; i < n; i++ ) {
	    this.codeBuf.write( 0xFF );		// mit NOPs auffuellen
	  }
	}
      }
      this.endAddr = this.curAddr;
      this.codeBuf.write( b );
    }
    this.curAddr++;
    checkAddr();
  }


  private void putCodeNibbles( int h, int l ) throws AsmException
  {
    putCode( ((h << 4) & 0xF0) | (l & 0x0F) );
  }


  private void putCodeWord( int w ) throws AsmException
  {
    putCode( w >> 8 );
    putCode( w );
  }


  private void putError( String msg ) throws TooManyErrorsException
  {
    if( msg == null ) {
      msg = "Unbekannter Fehler";
    }
    appendLineNumMsgToErrLog( msg, TEXT_ERROR );
    this.status = false;
    this.errorCnt++;
    if( this.errorCnt >= 100 ) {
      throw new TooManyErrorsException();
    }
  }


  private void skipCode( int n ) throws AsmException
  {
    this.curAddr += n;
    checkAddr();
  }


  private void reset()
  {
    this.curSource        = this.mainSource;
    this.sortedLabels     = null;
    this.addrOverflow     = false;
    this.suppressLineAddr = false;
    this.status           = true;
    this.warnEnabled      = true;
    this.cpu              = null;
    this.title            = null;
    this.entryAddr        = null;
    this.begAddr          = -1;
    this.endAddr          = -1;
    this.curAddr          = 0;
    this.instBegAddr      = 0;
    this.passNum          = 0;
    this.errorCnt         = 0;
    this.stack.clear();
    this.labels.clear();
    this.labels.putAll( this.options.getPredefinedLabels() );
    if( this.curSource != null ) {
      this.curSource.reset();
    }
    if( this.codeBuf != null ) {
      this.codeBuf.reset();
    }
  }


  private void throwNoSuchInstArgs() throws AsmException
  {
    throw new AsmException( "Die Anweisung existiert nicht"
		+ " f\u00FCr die angegebenen Argumente." );
  }


  private void listLabels()
  {
    if( this.lstOut != null ) {
      boolean    firstLabel = true;
      AsmLabel[] labels     = getSortedLabels();
      if( labels != null ) {
	for( AsmLabel label : labels ) {
	  if( firstLabel ) {
	    firstLabel = false;
	    this.lstOut.println();
	    this.lstOut.print( "Markentabelle:" );
	    this.lstOut.println();
	  }
	  this.lstOut.print(
		String.format(
			"    %s  %s",
			label.hasIntValue() ?
				String.format( "%04X", label.intValue() )
				: "????",
			label.getLabelName() ) );
	  this.lstOut.println();
	}
      }
      if( firstLabel ) {
	this.lstOut.print( "Markentabelle ist leer." );
	this.lstOut.println();
      }
    }
  }
}
