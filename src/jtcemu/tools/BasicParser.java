/*
 * (c) 2007-2009 Jens Mueller
 * (c) 2017 Lars Sonchocky-Helldorf
 *
 * Jugend+Technik-Computer-Emulator
 *
 * BASIC-Parser
 */

package jtcemu.tools;

import java.io.ByteArrayOutputStream;
import java.text.*;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.swing.JTextArea;


public class BasicParser
{
  private static final Locale locale = Locale.getDefault();
  private static final ResourceBundle basicParserResourceBundle = ResourceBundle.getBundle("resources.BasicParser", locale);

  private CharacterIterator     iter;
  private JTextArea             logOut;
  private ByteArrayOutputStream codeBuf;
  private String                srcText;
  private int                   srcLen;
  private int                   errorCnt;
  private int                   warningCnt;
  private int                   lastCode;
  private int                   lineOffset;
  private int                   lineNum;
  private int                   curBasicLineNum;
  private int                   lastBasicLineNum;
  private boolean               allowAllBasicLineNums;
  private boolean               hasLines;
  private boolean               lineHasIF;
  private boolean               prevLineHasIF;
  private boolean               firstInstInLine;


  public BasicParser(
                String                text,
                JTextArea             logOut,
                ByteArrayOutputStream codeBuf )
  {
    this.srcText = (text != null ? text : "");
    this.srcLen  = this.srcText.length();
    this.iter    = new StringCharacterIterator( this.srcText );
    this.logOut  = logOut;
    this.codeBuf = codeBuf;
  }


  public boolean parse( boolean allowAllBasicLineNums )
  {
    this.allowAllBasicLineNums = allowAllBasicLineNums;
    this.hasLines              = false;
    this.lineHasIF             = false;
    this.prevLineHasIF         = false;
    this.firstInstInLine       = false;
    this.errorCnt              = 0;
    this.warningCnt            = 0;
    this.lastCode              = 0;
    this.lineOffset            = 0;
    this.lineNum               = 1;
    this.curBasicLineNum       = -1;
    this.lastBasicLineNum      = -1;

    try {
      char ch = this.iter.first();
      while( ch != CharacterIterator.DONE ) {
        parseLine();
        ch = skipSpaces();
      }
      if( this.hasLines ) {
        putCode( 0x0D );
      }
      if( this.codeBuf != null ) {
        if( this.codeBuf.size() > 0 )
          this.codeBuf.write( 0 );
      }
    }
    catch( TooManyErrorsException ex ) {
      this.logOut.append( basicParserResourceBundle.getString("error.parse.tooManyErrorsException.errorText") );
    }
    this.logOut.append( "\n" );
    this.logOut.append( Integer.toString( this.errorCnt ) );
    this.logOut.append( basicParserResourceBundle.getString("error.parse.errorTextModule.0") );
    if( this.warningCnt == 1 ) {
      this.logOut.append( basicParserResourceBundle.getString("error.parse.errorTextModule.1") );
    } else if( this.warningCnt > 1 ) {
      this.logOut.append( basicParserResourceBundle.getString("error.parse.errorTextModule.2") );
      this.logOut.append( Integer.toString( this.warningCnt ) );
      this.logOut.append( basicParserResourceBundle.getString("error.parse.errorTextModule.3") );
    }
    this.logOut.append( "\n" );
    return (this.errorCnt == 0);
  }


        /* --- private Methoden --- */

  private void parseLine() throws TooManyErrorsException
  {
    this.lineOffset   = this.iter.getIndex();
    this.curBasicLineNum = -1;

    char ch = skipSpaces();
    if( (ch != CharacterIterator.DONE) && (ch != '\n') ) {
      try {

        // BASIC-Zeilennummer parsen
        if( (ch >= '0') && (ch <= '9') ) {
          boolean errDone      = false;
          int     basicLineNum = (ch - '0');
          ch                   = this.iter.next();
          while( (ch >= '0') && (ch <= '9') ) {
            if( !errDone ) {
              basicLineNum = (basicLineNum * 10) + (ch - '0');
              if( basicLineNum > 32767 ) {
                putError( basicParserResourceBundle.getString("error.parseLine.lineNumberToLarge.errorText"), -1 );
                errDone = true;
              }
            }
            ch = this.iter.next();
          }
          if( !errDone && !this.allowAllBasicLineNums ) {
            int lByte = basicLineNum & 0xFF;
            if( (lByte == 0x00) || (lByte == 0x0D) ) {
              putError(MessageFormat.format( basicParserResourceBundle.getString("error.parseLine.illegalLineNumber.messageformat"), Integer.toString( basicLineNum )), -1 );
              errDone = true;
            }
          }
          if( !errDone
              && (this.lastBasicLineNum >= 0)
              && (basicLineNum <= this.lastBasicLineNum) )
          {
            putError(
                basicParserResourceBundle.getString("error.parseLine.nextLineNumberMustBeLargerThanPrevious.errorText"),
                -1 );
            errDone = true;
          }
          if( !errDone ) {
            if( this.hasLines ) {
              putCode( 0x0D );
            }
            putCode( (basicLineNum >> 8) | 0x80 );
            putCode( basicLineNum );
            this.curBasicLineNum  = basicLineNum;
            this.lastBasicLineNum = basicLineNum;
            this.hasLines         = true;
            this.firstInstInLine  = true;
            this.prevLineHasIF    = this.lineHasIF;
            this.lineHasIF        = false;
          }
        } else {
          if( this.hasLines ) {
            if( this.lastCode != ';' ) {
              putCode( ';' );
            }
          } else {
            throwParseException( basicParserResourceBundle.getString("error.parseLine.atLeastTheFirstLineMustHaveALineNumber.errorText") );
          }
        }

        // Anweisungen parsen
        parseInst();
        this.firstInstInLine = false;

        // ggf. weiter Anweisungen parsen
        ch = skipSpaces();
        while( ch == ';' ) {
          this.iter.next();
          ch = skipSpaces();
          if( (ch != CharacterIterator.DONE) && (ch != '\n') ) {
            putCode( ';' );
            parseInst();
            ch = skipSpaces();
          }
        }
        if( (ch != CharacterIterator.DONE) && (ch != '\n') ) {
          throwUnexpectedChar( ch );
        }
      }
      catch( ParseException ex ) {
        putError( ex.getMessage(), ex.getErrorOffset() );
      }
    }

    // Zeilenende ueberspringen
    ch = this.iter.current();
    while( (ch != CharacterIterator.DONE) && (ch != '\n') ) {
      ch = this.iter.next();
    }
    if( ch == '\n' ) {
      this.iter.next();
      this.lineNum++;
    }
  }


  private void parseInst() throws ParseException
  {
    int ch = skipSpaces();
    if( ch == '?' ) {
      this.iter.next();
      parsePRINT( 'P' );
    } else if( ch == '!' ) {
      this.iter.next();
      parseREM();
    } else {
      int    begInst  = this.iter.getIndex();
      String instName = readIdentifier();
      int    instLen  = instName.length();
      if( instLen == 1 ) {
        this.iter.setIndex( begInst );
        parseLET();
      } else if( instLen > 1 ) {
        if( instName.equals( "CALL" ) ) {
          putCode( 'C' );
          parseExpr();
        }
        else if( instName.equals( "ELSE" ) ) {
          parseELSE();
        }
        else if( instName.equals( "END" ) ) {
          putCode( 'E' );
        }
        else if( instName.equals( "GOSUB" ) ) {
          putCode( 'S' );
          parseExpr();
        }
        else if( instName.equals( "GOTO" ) ) {
          putCode( 'G' );
          parseExpr();
        }
        else if( instName.equals( "IF" ) ) {
          parseIF();
        }
        else if( instName.equals( "INPUT" ) ) {
          parseINPUT();
        }
        else if( instName.equals( "LET" ) ) {
          parseLET();
        }
        else if( instName.equals( "PRINTHEX" ) || instName.equals( "PTH" ) ) {
          parsePRINT( 'H' );
        }
        else if( instName.equals( "PRINT" ) ) {
          parsePRINT( 'P' );
        }
        else if( instName.equals( "PROC" ) ) {
          parsePROC();
        }
        else if( instName.equals( "PTC" ) ) {
          putCode( 'O' );
          putCode( instName );
          parseArgList( 1 );
        }
        else if( instName.equals( "RETURN" ) ) {
          putCode( 'R' );
        }
        else if( instName.equals( "REM" ) ) {
          parseREM();
        }
        else if( instName.equals( "SETR" )
                 || instName.equals( "SETRR" )
                 || instName.equals( "SETEB" )
                 || instName.equals( "SETEW" ) )
        {
          putCode( 'O' );
          putCode( instName );
          parseArgList( 2 );
        }
        else if( instName.equals( "STOP" ) ) {
          putCode( 'T' );
        }
        else if( instName.equals( "TOFF" ) ) {
          putCode( '/' );
        }
        else if( instName.equals( "TRAP" ) ) {
          parseTRAP();
        }
        else if( instName.equals( "WAIT" ) ) {
          putCode( 'W' );
          parseExpr();
        } else {
          throwParseException( instName + basicParserResourceBundle.getString("error.parseInst.unknownInstruction.errorText") );
        }
      } else {
        throwParseException( basicParserResourceBundle.getString("error.parseInst.missingInstruction.errorText") );
      }
    }
  }


  private void parseELSE() throws ParseException
  {
    if( !this.firstInstInLine ) {
      throwParseException( basicParserResourceBundle.getString("error.parseELSE.isNotFirstInstInLine.errorText") );
    }
    if( !this.prevLineHasIF ) {
      throwParseException( basicParserResourceBundle.getString("error.parseELSE.prevLineHasNoIF.errorText") );
    }
    putCode( '>' );
    if( skipSpaces() != ';' ) {
      throwParseException( basicParserResourceBundle.getString("error.parseELSE.missingSemicolon.errorText") );
    }
  }


  private void parseIF() throws ParseException
  {
    this.lineHasIF = true;
    putCode( 'F' );
    parseCondExpr();
    boolean done = false;
    char    ch   = skipSpaces();
    if( ch == ',' ) {
      putCode( ';' );
      this.iter.next();
    } else {
      int    pos  = this.iter.getIndex();
      String name = readIdentifier();
      if( name.length() > 0 ) {
        putCode( ';' );
        if( name.equals( "THEN" ) ) {
          ch = skipSpaces();
          if( (ch >= '0') && (ch <= '9') ) {
            putCode( 'G' );
            parseDecNumber();
            done = true;
          }
        } else {
          this.iter.setIndex( pos );
        }
      } else {
        throwUnexpectedChar( ch );
      }
    }
    if( !done ) {
      parseInst();
    }
    ch = skipSpaces();
    while( ch == ';' ) {
      putCode( ';' );
      this.iter.next();
      parseInst();
      ch = skipSpaces();
    }
  }


  private void parseINPUT() throws ParseException
  {
    putCode( 'I' );
    parseOptStringLiteral();
    char ch = Character.toUpperCase( skipSpaces() );
    if( (ch < 'A') || (ch > 'Z') ) {
      throwVarExpected();
    }
    putCode( ch );
    this.iter.next();
  }


  private void parseLET() throws ParseException
  {
    putCode( 'L' );
    boolean loop = true;
    while( loop ) {
      char ch = Character.toUpperCase( skipSpaces() );
      if( (ch < 'A') || (ch > 'Z') ) {
        throwVarExpected();
      }
      putCode( ch );
      this.iter.next();
      if( skipSpaces() != '=' ) {
        throwParseException( basicParserResourceBundle.getString("error.parseLET.equalsSignExpected.errorText") );
      }
      putCode( '=' );
      this.iter.next();
      parseExpr();
      ch = skipSpaces();
      if( ch == ',' ) {
        putCode( ch );
        this.iter.next();
      } else {
        loop = false;
      }
    }
  }


  private void parsePRINT( char cmdChar ) throws ParseException
  {
    putCode( cmdChar );
    boolean loop = true;
    while( loop ) {
      parseOptStringLiteral();
      char ch = skipSpaces();
      if( (ch != CharacterIterator.DONE)
          && (ch != '\n')
          && (ch != ';')
          && (ch != ',') )
      {
        parseExpr();
      }
      if( skipSpaces() == ',' ) {
        putCode( ',' );
        this.iter.next();
        ch = skipSpaces();
        if( (ch == CharacterIterator.DONE) || (ch == '\n') || (ch == ';') ) {
          loop = false;
        }
      } else {
        loop = false;
      }
    }
  }


  private void parsePROC() throws ParseException
  {
    putCode( 'O' );
    if( skipSpaces() == '[' ) {
      putCode( '[' );
      this.iter.next();
      boolean loop = true;
      while( loop ) {
        char ch = Character.toUpperCase( skipSpaces() );
        if( (ch < 'A') || (ch > 'Z') ) {
          throwVarExpected();
        }
        putCode( ch );
        this.iter.next();
        ch = skipSpaces();
        if( ch == ',' ) {
          putCode( ch );
          this.iter.next();
        } else {
          loop = false;
        }
      }
      if( skipSpaces() != ']' ) {
        throwParseException( basicParserResourceBundle.getString("error.parsePROC.closingSquaredBraceExpected.errorText") );
      }
      putCode( ']' );
      this.iter.next();
      if( skipSpaces() != '=' ) {
        throwParseException( basicParserResourceBundle.getString("error.parsePROC.equalsSignExpected.errorText") );
      }
      putCode( '=' );
      this.iter.next();
    }
    String procName = readIdentifier();
    if( procName.equals( "ABS" )
        || procName.equals( "GETR" )
        || procName.equals( "GETRR" )
        || procName.equals( "GETEB" )
        || procName.equals( "GETEW" )
        || procName.equals( "NEG" )
        || procName.equals( "NOT" )
        || procName.equals( "PTC" ) )
    {
      putCode( procName );
      parseArgList( 1 );
    } else if( procName.equals( "SETR" )
             || procName.equals( "SETRR" )
             || procName.equals( "SETEB" )
             || procName.equals( "SETEW" ) )
    {
      putCode( procName );
      parseArgList( 2 );
    } else {
      throwParseException( procName + basicParserResourceBundle.getString("error.parsePROC.unknownProcedure.errorText") );
    }
  }


  private void parseREM() throws ParseException
  {
    putCode( 'M' );
    char ch = this.iter.next();
    while( (ch != CharacterIterator.DONE) && (ch != '\n') ) {
      putCode( ch );
      ch = this.iter.next();
    }
  }


  private void parseTRAP() throws ParseException
  {
    putCode( '!' );
    parseCondExpr();
    char ch = skipSpaces();
    if( ch == ',' ) {
      putCode( ch );
      this.iter.next();
    } else {
      String name = readIdentifier();
      if( name.length() > 0 ) {
        if( name.equals( "TO" ) ) {
          putCode( ';' );
        } else {
          throw new ParseException(
                        MessageFormat.format(basicParserResourceBundle.getString("error.parseTRAP.wrongTokenFound.messageformat"), name),
                        this.iter.getIndex() );
        }
      } else {
        throwUnexpectedChar( ch );
      }
    }
    parseExpr();
  }


  private void parseArgList( int argCnt ) throws ParseException
  {
    if( argCnt > 0 ) {
      if( skipSpaces() != '[' ) {
        throwParseException( basicParserResourceBundle.getString("error.parseArgList.openingSquaredBracketExpected.errorText") );
      }
      putCode( '[' );
      this.iter.next();
      boolean loop = true;
      while( loop ) {
        parseExpr();
        --argCnt;
        if( argCnt > 0 ) {
          if( skipSpaces() != ',' ) {
            throwParseException( basicParserResourceBundle.getString("error.parseArgList.commaExpected.errorText") );
          }
          putCode( ',' );
          this.iter.next();
        } else {
          loop = false;
        }
      }
      if( skipSpaces() != ']' ) {
        throwParseException( basicParserResourceBundle.getString("error.parseArgList.closingSquaredBracketExpected.errorText") );
      }
      putCode( ']' );
      this.iter.next();
    }
  }


  private void parseCondExpr() throws ParseException
  {
    parseExpr();
    char ch = skipSpaces();
    if( ch == '<' ) {
      putCode( ch );
      ch = this.iter.next();
      if( (ch == '=') || (ch == '>') ) {
        this.iter.next();
        putCode( ch );
      }
      parseExpr();
    } else if( ch == '>' ) {
      putCode( ch );
      if( this.iter.next() == '=' ) {
        this.iter.next();
        putCode( '=' );
      }
      parseExpr();
    } else if( ch == '=' ) {
      putCode( ch );
      this.iter.next();
      parseExpr();
    } else {
      throwParseException( basicParserResourceBundle.getString("error.parseCondExpr.comparisonOperatorExpected.errorText") );
    }
  }


  private void parseExpr() throws ParseException
  {
    char ch = skipSpaces();
    if( ch == '-' ) {
      putCode( ch );
      this.iter.next();
      parseDecNumber();
    } else {
      parsePrimExpr();
    }
    ch = skipSpaces();
    while( (ch == '+')
           || (ch == '-')
           || (ch == '*')
           || (ch == '/')
           || (ch == '$') )
    {
      putCode( ch );
      if( ch == '$' ) {
        ch = Character.toUpperCase( this.iter.next() );
        if( (ch == 'A') || (ch == 'M') || (ch == 'O') || (ch == 'X') ) {
          putCode( ch );
        } else {
          throwParseException( basicParserResourceBundle.getString("error.parseExpr.a_m_o_x_Expected.errorText") );
        }
      }
      ch = this.iter.next();
      parsePrimExpr();
      ch = skipSpaces();
    }
  }


  private void parsePrimExpr() throws ParseException
  {
    char ch = skipSpaces();
    if( ch == '(' ) {
      putCode( ch );
      this.iter.next();
      parseExpr();
      if( skipSpaces() != ')' ) {
        throwParseException( basicParserResourceBundle.getString("error.parsePrimExpr.closingRoundBracketExpected.errorText") );
      }
      putCode( ')' );
      this.iter.next();
    } else if( (ch >= '0') && (ch <= '9') ) {
      parseDecNumber();
    } else if( ch == '%' ) {
      parseHexNumber();
    } else {
      String funcName = readIdentifier();
      int    funcLen  = funcName.length();
      if( funcLen == 1 ) {
        putCode( funcName.charAt( 0 ) );
      } else if( funcLen > 1 ) {
        if( funcName.equals( "ABS" )
            || funcName.equals( "GETR" )
            || funcName.equals( "GETRR" )
            || funcName.equals( "GETEB" )
            || funcName.equals( "GETEW" )
            || funcName.equals( "NOT" )
            || funcName.equals( "RL" )
            || funcName.equals( "RR" ) )
        {
          putCode( funcName );
          parseArgList( 1 );
        } else if( funcName.equals( "GTC" ) || funcName.equals( "INPUT" ) ) {
          putCode( funcName );
        } else {
          throwParseException( funcName + basicParserResourceBundle.getString("error.parsePrimExpr.unknownFunction.errorText") );
        }
      } else {
        throwParseException( basicParserResourceBundle.getString("error.parsePrimExpr.openingRoundBracketOrNumberOrLetterOrPercentSignExpected.errorText") );
      }
    }
  }


  private void parseDecNumber() throws ParseException
  {
    char ch = skipSpaces();
    if( (ch >= '0') && (ch <= '9') ) {
      putCode( ch );
      int v = ch - '0';
      ch    = this.iter.next();
      while( (ch >= '0') && (ch <= '9') ) {
        v = (v * 10) + (ch - '0');
        if( v > 32767 ) {
          throwParseException( basicParserResourceBundle.getString("error.parseDecNumber.numberToLarge.errorText") );
        }
        putCode( ch );
        ch = this.iter.next();
      }
    }
  }


  private void parseHexNumber() throws ParseException
  {
    char ch = skipSpaces();
    if( ch == '%' ) {
      putCode( ch );
      ch    = Character.toUpperCase( this.iter.next() );
      int v = 0;
      while( ((ch >= '0') && (ch <= '9')) || ((ch >= 'A') && (ch <= 'F')) ) {
        putCode( ch );
        if( (ch >= '0') && (ch <= '9') ) {
          v = (v << 4) | (ch - '0');
        } else if( (ch >= 'A') && (ch <= 'F') ) {
          v = (v << 4) | (ch + 10 - 'A');
        }
        if( v >= 0x10000 ) {
          throwParseException( basicParserResourceBundle.getString("error.parseHexNumber.numberToLarge.errorText") );
        }
        ch = Character.toUpperCase( this.iter.next() );
      }
    }
  }


  private void parseOptStringLiteral() throws ParseException
  {
    char ch = skipSpaces();
    if( ch == '\"' ) {
      putCode( ch );
      boolean warnNL           = false;
      boolean warnNotASCII     = false;
      boolean warnNotPrintable = false;

      ch = this.iter.next();
      while( (ch != CharacterIterator.DONE) && (ch != '\"') ) {
        if( (ch > 0) && (ch <= 0x7F) ) {
          if( ch == '\n' ) {
            this.lineNum++;
            if( !warnNL ) {
              putWarning( basicParserResourceBundle.getString("error.parseOptStringLiteral.lineBreakFound.errorText") );
              warnNL = true;
            }
          }
          else if( (ch < 0x20) || (ch > 0x5F) ) {
            if( !warnNotPrintable ) {
              putWarning( MessageFormat.format(basicParserResourceBundle.getString("error.parseOptStringLiteral.notPrintable.messageformat"), ch, null) );
              warnNotPrintable = true;
            }
          }
          putCode( ch );
        } else {
          if( !warnNotASCII ) {
            putWarning( basicParserResourceBundle.getString("error.parseOptStringLiteral.notASCII.errorText") );
            warnNotASCII = true;
          }
        }
        ch = this.iter.next();
      }
      if( ch == '\"' ) {
        putCode( ch );
        this.iter.next();
      } else {
        throwParseException( basicParserResourceBundle.getString("error.parseOptStringLiteral.stringNotTerminated.errorText") );
      }
    }
  }


  private void putCode( int b )
  {
    if( this.codeBuf != null ) {
      this.codeBuf.write( b );
      this.lastCode = b;
    }
  }


  private void putCode( String text )
  {
    if( (text != null) && (this.codeBuf != null) ) {
      int len = text.length();
      if( len > 0 ) {
        for( int i = 0; i < len; i++ ) {
          this.codeBuf.write( text.charAt( i ) );
        }
        this.lastCode = text.charAt( len - 1 );
      }
    }
  }


  private void putError(
                String msg,
                int    errorOffset ) throws TooManyErrorsException
  {
    putMessage( basicParserResourceBundle.getString("putError.message"), msg );
    if( (this.logOut != null) && (errorOffset >= this.lineOffset) ) {
      StringBuilder buf = new StringBuilder( 256 );
      int           pos = this.lineOffset;
      while( (pos < errorOffset) && (pos < this.srcLen) ) {
        buf.append( (char) this.srcText.charAt( pos++ ) );
      }
      buf.append( " ???" );
      if( pos < this.srcLen - 1 ) {
        buf.append( (char) '\u0020' );
        while( pos < this.srcLen ) {
          char ch = this.srcText.charAt( pos++ );
          if( ch == '\n' ) {
            break;
          }
          buf.append( ch );
        }
      }
      buf.append( "\n\n" );
      this.logOut.append( buf.toString() );
    }
    this.errorCnt++;
    if( this.errorCnt >= 100 ) {
      throw new TooManyErrorsException();
    }
  }


  private void putWarning( String msg )
  {
    putMessage( basicParserResourceBundle.getString("putWarning.message"), msg );
    this.warningCnt++;
  }


  private void putMessage( String msgType, String msgText )
  {
    if( (msgText != null) && (this.logOut != null) ) {
      this.logOut.append( msgType );
      this.logOut.append( basicParserResourceBundle.getString("putMessage.logTextModule.0") );
      this.logOut.append( Integer.toString( this.lineNum ) );
      if( this.curBasicLineNum >= 0 ) {
        this.logOut.append( basicParserResourceBundle.getString("putMessage.logTextModule.1") );
        this.logOut.append( Integer.toString( this.curBasicLineNum ) );
      }
      this.logOut.append( ": " );
      this.logOut.append( msgText );
      this.logOut.append( "\n" );
    }
  }


  private String readIdentifier()
  {
    StringBuilder buf = new StringBuilder();
    char ch = Character.toUpperCase( skipSpaces() );
    while( (ch >= 'A') && (ch <= 'Z') ) {
      buf.append( ch );
      ch = Character.toUpperCase( this.iter.next() );
    }
    return buf.toString();
  }


  private char skipSpaces()
  {
    char ch = this.iter.current();
    while( (ch == '\u0020') || (ch == '\t') ) {
      ch = this.iter.next();
    }
    return ch;
  }


  private void throwParseException( String msg ) throws ParseException
  {
    throw new ParseException( msg, this.iter.getIndex() );
  }


  private void throwUnexpectedChar( char ch ) throws ParseException
  {
    if( ch == CharacterIterator.DONE ) {
      throwParseException( basicParserResourceBundle.getString("error.throwUnexpectedChar.unexpectedProgramEnd.errorText") );
    } else if( ch == '\n' ) {
      throwParseException( basicParserResourceBundle.getString("error.throwUnexpectedChar.unexpectedLineBreak.errorText") );
    } else {
      throwParseException( MessageFormat.format(basicParserResourceBundle.getString("error.throwUnexpectedChar.unexpectedCharacter.messageformat"), ch) );
    }
  }


  private void throwVarExpected() throws ParseException
  {
    throwParseException( basicParserResourceBundle.getString("error.throwVarExpected.variableExpected.errorText") );
  }
}

