/*
 * (c) 2007-2010 Jens Mueller
 * (c) 2017 Lars Sonchocky-Helldorf
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Informationen ueber eine Datei
 */

package org.sqar.virtualjtc.jtcemu.base;

import java.io.*;
import java.util.Locale;
import java.util.ResourceBundle;


public class FileInfo
{
  private static final Locale locale = Locale.getDefault();
  private static final ResourceBundle fileInfoResourceBundle = ResourceBundle.getBundle("FileInfo", locale);

  public enum Format { JTC, TAP, HEX, BIN };

  private byte[] header;
  private long   fileLen;
  private Format fmt;
  private String infoText;


  public static FileInfo analyzeFile( File file )
  {
    FileInfo rv = null;
    if( file != null ) {
      try {
        long fileLen = file.length();
        if( fileLen > 0 ) {
          InputStream in = null;
          try {
            in = new FileInputStream( file );

            byte[] header = new byte[ 40 ];
            int    nBytes = in.read( header );
            if( (nBytes == header.length) || (nBytes == fileLen) ) {
              rv = analyzeFile( header, fileLen );
            }
          }
          finally {
            if( in != null ) {
              try {
                in.close();
              }
              catch( IOException ex ) {}
            }
          }
        }
      }
      catch( Exception ex ) {}
    }
    return rv;
  }


  public static FileInfo analyzeFile( byte[] header, long fileLen )
  {
    FileInfo rv = null;
    if( header != null ) {
      Format fmt      = null;
      String infoText = null;
      
      String file = fileInfoResourceBundle.getString("analyzeFile.file");

      if( (fileLen > 127) && (header.length > 20) ) {
        if( (header[ 16 ] == 2) || (header[ 16 ] == 3) ) {
          fmt = Format.JTC;

          boolean kcc = false;
          for( int i = 11; i < 16; i++ ) {
            if( header[ i ] != 0 ) {
              kcc = true;
              break;
            }
          }
          StringBuilder buf = new StringBuilder( 26 );
          if( header[ 16 ] == 2 ) {
            buf.append( String.format(
                                "%s-%s: %02X%02X-%02X%02X ",
                                kcc ? "KCC" : "JTC",
                                file,
                                header[ 18 ] & 0xFF,
                                header[ 17 ] & 0xFF,
                                header[ 20 ] & 0xFF,
                                header[ 19 ] & 0xFF ) );
          } else {
            buf.append( String.format(
                                "KCC-%s: %02X%02X-%02X%02X Start=%02X%02X ",
                                file,
                                header[ 18 ] & 0xFF,
                                header[ 17 ] & 0xFF,
                                header[ 20 ] & 0xFF,
                                header[ 19 ] & 0xFF,
                                header[ 22 ] & 0xFF,
                                header[ 21 ] & 0xFF ) );
          }
          if( appendFileDesc( buf, header, 0 ) ) {
            infoText = buf.toString();
          } else {
            fmt = null;
          }
        }
      }
      if( (fmt == null) && (fileLen > 144) && (header.length > 37) ) {
        fmt      = Format.TAP;
        String s = "\u00C3KC-TAPE by AF.\u0020";
        int    n = s.length();
        for( int i = 0; i < n; i++ ) {
           if( ((int) header[ i ] & 0xFF) != (int) s.charAt( i ) ) {
              fmt = null;
              break;
           }
        }

        // KC-BASIC-Dateien werden nicht unterstuetzt
        if( (fmt == Format.TAP)
            && ((header[ 17 ] < 0xD3) || (header[ 17 ] > 0xD8))
            && ((header[ 18 ] < 0xD3) || (header[ 18 ] > 0xD8))
            && ((header[ 19 ] < 0xD3) || (header[ 19 ] > 0xD8))
            && ((header[ 33 ] == 2) || (header[ 33 ] == 3)) )
        {
          StringBuilder buf = new StringBuilder( 11 );
          buf.append( "KC-TAP-" + file + ": " );
          buf.append( String.format(
                                "%02X%02X-%02X%02X ",
                                header[ 35 ] & 0xFF,
                                header[ 34 ] & 0xFF,
                                header[ 37 ] & 0xFF,
                                header[ 36 ] & 0xFF ) );
          if( header[ 33 ] == 3 ) {
           buf.append( String.format(
                                "Start=%02X%02X ",
                                header[ 39 ] & 0xFF,
                                header[ 38 ] & 0xFF ) );
          }
          appendFileDesc( buf, header, 17 );
          infoText = buf.toString();
        } else {
          fmt = null;
        }
      }
      if( (fmt == null) && (fileLen > 10) && (header.length > 10) ) {
        char c3 = (char) (header[ 3 ] & 0xFF);
        char c4 = (char) (header[ 4 ] & 0xFF);
        char c5 = (char) (header[ 5 ] & 0xFF);
        char c6 = (char) (header[ 6 ] & 0xFF);
        if( (header[ 0 ] == ':')
            && isHexChar( header[ 1 ] )
            && isHexChar( header[ 2 ] )
            && isHexChar( c3 )
            && isHexChar( c4 )
            && isHexChar( c5 )
            && isHexChar( c6 )
            && isHexChar( header[ 7 ] )
            && isHexChar( header[ 8 ] )
            && isHexChar( header[ 9 ] )
            && isHexChar( header[ 10 ] ) )
        {
          fmt            = Format.HEX;
          char[] begAddr = new char[ 4 ];
          begAddr[ 0 ]   = c3;
          begAddr[ 1 ]   = c4;
          begAddr[ 2 ]   = c5;
          begAddr[ 3 ]   = c6;
          infoText = "Intel-HEX-" + file + ": " + (new String( begAddr ));
        }
      }
      if( fmt == null ) {
        fmt = Format.BIN;
      }
      rv = new FileInfo( header, fileLen, fmt, infoText );
    }
    return rv;
  }


  public String getBegAddrText( Format fmt )
  {
    String rv = null;
    if( fmt != null ) {
      switch( fmt ) {
        case JTC:
          if( (this.fileLen > 20) && (this.header.length > 20) ) {
            rv = String.format(
                        "%02X%02X",
                        this.header[ 18 ] & 0xFF,
                        this.header[ 17 ] & 0xFF );
          }
          break;

        case TAP:
          if( (this.fileLen > 37) && (this.header.length > 37) ) {
            rv = String.format(
                        "%02X%02X",
                        this.header[ 35 ] & 0xFF,
                        this.header[ 34 ] & 0xFF );
          }
          break;

        case HEX:
          if( (this.fileLen > 6) && (this.header.length > 6) ) {
            char c3 = (char) (this.header[ 3 ] & 0xFF);
            char c4 = (char) (this.header[ 4 ] & 0xFF);
            char c5 = (char) (this.header[ 5 ] & 0xFF);
            char c6 = (char) (this.header[ 6 ] & 0xFF);
            if( isHexChar( c3 )
                && isHexChar( c4 )
                && isHexChar( c5 )
                && isHexChar( c6 ) )
            {
              char[] s = new char[ 4 ];
              s[ 0 ]   = c3;
              s[ 1 ]   = c4;
              s[ 2 ]   = c5;
              s[ 3 ]   = c6;
              rv       = new String( s );
            }
          }
          break;
      }
    }
    return rv;
  }


  public String getEndAddrText( Format fmt )
  {
    String rv = null;
    if( fmt != null ) {
      switch( fmt ) {
        case JTC:
          if( (this.fileLen > 20) && (this.header.length > 20) ) {
            rv = String.format(
                        "%02X%02X",
                        this.header[ 20 ] & 0xFF,
                        this.header[ 19 ] & 0xFF );
          }
          break;

        case TAP:
          if( (this.fileLen > 37) && (this.header.length > 37) ) {
            rv = String.format(
                        "%02X%02X",
                        this.header[ 37 ] & 0xFF,
                        this.header[ 36 ] & 0xFF );
          }
          break;
      }
    }
    return rv;
  }


  public Format getFormat()
  {
    return this.fmt;
  }


  public String getFileDesc( Format fmt )
  {
    String rv = null;
    if( fmt != null ) {
      int pos = -1;
      switch( fmt ) {
        case JTC:
          pos = 0;
          break;

        case TAP:
          pos = 17;
          break;
      }
      if( (pos >= 0)
          && (this.fileLen > pos + 11)
          && (this.header.length > pos + 117) )
      {
        StringBuilder buf = new StringBuilder( 11 );
        appendFileDesc( buf, this.header, pos );
        rv = buf.toString();
      }
    }
    return rv;
  }


  public String getInfoText()
  {
    return this.infoText;
  }


        /* --- private Konstruktoren und Methoden --- */

  public FileInfo(
                byte[] header,
                long   fileLen,
                Format fmt,
                String infoText )
  {
    this.header   = header;
    this.fileLen  = fileLen;
    this.fmt      = fmt;
    this.infoText = infoText;
  }


  /*
   * Die Methode haengt an den uebergebenen StringBuilder
   * die ab der Position pos stehende Dateibezeichnung an.
   * Nullbytes werden in Leerzeichen gewandelt
   *
   * Rueckgabewert:
   *  true:  alle angehaengten Zeichen waren druckbare Zeichen
   *  false: einige Zeichen waren nicht druckbar
   */
  private static boolean appendFileDesc(
                                StringBuilder dst,
                                byte[]        header,
                                int           pos )
  {
    boolean rv  = true;
    int     n   = 11;
    int     nSp = 0;
    while( (n > 0) && (pos < header.length) ) {
      int b = (int) header[ pos++ ] & 0xFF;
      if( (b == 0) || (b == 0x20) ) {
        nSp++;
      } else {
        while( nSp > 0 ) {
          dst.append( (char) '\u0020' );
          --nSp;
        }
        if( (b > 0x20) && Character.isDefined( b ) ) {
          dst.append( (char) b );
        } else {
          dst.append( (char) '?' );
          rv = false;
        }
      }
      --n;
    }
    return rv;
  }


  private static boolean isHexChar( int ch )
  {
    return ((ch >= '0') && (ch <= '9'))
           || ((ch >= 'A') && (ch <= 'F'))
           || ((ch >= 'a') && (ch <= 'f'));
  }
}

