/*
 * (c) 2014-2019 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Speichern eines Bereichs des Arbeitsspeichers in eine Datei
 */

package org.jens_mueller.jtcemu.base;

import org.jens_mueller.z8.Z8Memory;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.Writer;
import java.io.IOException;
import java.io.OutputStream;


public class FileSaver
{
  public static enum Format { BIN, HEX, JTC, TAP };


  public static void save(
			int          begAddr,
			int          startAddr,
			final byte[] dataBytes,
			File         file,
			String       fileDesc ) throws IOException
  {
    FileSaver.Format fileFmt  = FileSaver.Format.BIN;
    String           fileName = file.getName();
    if( fileName != null ) {
      if( fileDesc == null ) {
	fileDesc = "";
      }
      String upperName = fileName.toUpperCase();
      if( fileDesc.isEmpty() ) {
	int pos = fileName.lastIndexOf( '.' );
	if( pos > 0 ) {
	  fileDesc = upperName.substring( 0, pos );
	}
      }
      if( upperName.endsWith( ".HEX" ) ) {
	fileFmt = FileSaver.Format.HEX;
      } else if( upperName.endsWith( ".JTC" )
		 || upperName.endsWith( ".KCC" ) )
      {
	fileFmt = FileSaver.Format.JTC;
      } else if( upperName.endsWith( ".TAP" ) ) {
	fileFmt = FileSaver.Format.TAP;
      }
    }
    save(
	new Z8Memory()
	{
	  @Override
	  public int getMemByte( int addr, boolean dataMemory )
	  {
	    return (addr >= 0) && (addr < dataBytes.length) ?
			dataBytes[ addr ] & 0xFF
			: 0;
	  }

	  @Override
	  public boolean setMemByte( int addr, boolean dataMem, int value )
	  {
	    return false;
	  }
	},
	0,
	dataBytes.length - 1,
	startAddr,
	file,
	fileFmt,
	begAddr,
	fileDesc );
  }


  /*
   * Die Methode speichert einen Adressbereich in eine Datei.
   * Im Erfolgsfall wird ein Infotext zurueckgeliefert.
   */
  public static String save(
			Z8Memory memory,
			int      begAddr,
			int      endAddr,
			int      startAddr,
			File     file,
			Format   format,
			int      fileBegAddr,
			String   fileDesc ) throws IOException
  {
    int addr = begAddr;
    if( format.equals( Format.HEX ) ) {
      Writer out = null;
      try {
	out = new BufferedWriter( new FileWriter( file ) );

	int cnt = 1;
	while( (addr <= endAddr) && (cnt > 0) ) {
	  cnt = writeHexSegment( memory, out, addr, endAddr, fileBegAddr );
	  addr += cnt;
	  fileBegAddr += cnt;
	}
	out.write( ':' );
	writeHexByte( out, 0 );
	writeHexByte( out, 0 );
	writeHexByte( out, 0 );
	writeHexByte( out, 1 );
	writeHexByte( out, 0xFF );
	out.write( 0x0D );
	out.write( 0x0A );
	out.close();
	out = null;
	AppContext.setLastFile( FileInfo.FILE_GROUP_SOFTWARE, file );
      }
      finally {
	JTCUtil.closeSilently( out );
      }

    } else {

      // alle anderen Formate sind binear -> OutputStream oeffnen
      OutputStream out = null;
      try {
	out = new BufferedOutputStream( new FileOutputStream( file ) );
	if( format.equals( Format.JTC ) ) {
	  writeJTCHeader(
			out,
			addr,
			endAddr,
			startAddr,
			fileBegAddr,
			fileDesc );
	  while( addr <= endAddr ) {
	    out.write( memory.getMemByte( addr++, false ) );
	  }

	} else if( format.equals( Format.TAP ) ) {
	  String s = "\u00C3KC-TAPE by AF.\u0020";
	  int    n = s.length();
	  for( int i = 0; i < n; i++ ) {
	    out.write( s.charAt( i ) );
	  }

	  int blkNum = 1;
	  out.write( blkNum++ );
	  writeJTCHeader(
			out,
			addr,
			endAddr,
			startAddr,
			fileBegAddr,
			fileDesc );

	  n = 0;
	  while( addr <= endAddr ) {
	    if( n == 0 ) {
	      out.write( (addr + 128) > endAddr ? 0xFF : blkNum++ );
	      n = 128;
	    }
	    out.write( memory.getMemByte( addr++, false ) );
	    --n;
	  }
	  while( n > 0 ) {
	    out.write( 0 );
	    --n;
	  }
	} else {
	  while( addr <= endAddr ) {
	    out.write( memory.getMemByte( addr++, false ) );
	  }
	}
	out.close();
	out = null;
	AppContext.setLastFile( FileInfo.FILE_GROUP_SOFTWARE, file );
      }
      finally {
	JTCUtil.closeSilently( out );
      }
    }
    return addr > begAddr ?
		String.format(
			"Adressbereich %%%04X-%%%04X gespeichert",
			begAddr,
			addr - 1 )
		: null;
  }


  private static void writeHexChar( Writer out, int value ) throws IOException
  {
    value &= 0x0F;
    out.write( value < 10 ? (value + '0') : (value - 10 + 'A') );
  }


  private static void writeHexByte( Writer out, int value ) throws IOException
  {
    writeHexChar( out, value >> 4 );
    writeHexChar( out, value );
  }


  /*
   * Die Methode schreibt ein Datensegment im Intel-Hex-Format.
   *
   * Rueckabewert: Anzahl der geschriebenen Bytes
   */
  private static int writeHexSegment(
			Z8Memory memory,
			Writer   out,
			int      addr,
			int      endAddr,
			int      fileBegAddr ) throws IOException
  {
    int cnt = 0;
    if( (addr >= 0) && (addr <= endAddr) ) {
      cnt = endAddr - addr + 1;
      if( cnt > 32 ) {
	cnt = 32;
      }
      out.write( ':' );
      writeHexByte( out, cnt );

      int hFileBegAddr = fileBegAddr >> 8;
      writeHexByte( out, hFileBegAddr );
      writeHexByte( out, fileBegAddr );
      writeHexByte( out, 0 );

      int cks = (cnt & 0xFF) + (hFileBegAddr & 0xFF) + (fileBegAddr & 0xFF);
      for( int i = 0; i < cnt; i++ ) {
	int b = memory.getMemByte( addr++, false );
	writeHexByte( out, b );
	cks += b;
      }
      writeHexByte( out, 0 - cks );
      out.write( 0x0D );
      out.write( 0x0A );
    }
    return cnt;
  }


  private static void writeJTCHeader(
			OutputStream out,
			int          begAddr,
			int          endAddr,
			int          startAddr,
			int          fileBegAddr,
			String       fileDesc ) throws IOException
  {
    int n    = 11;
    int src  = 0;
    if( fileDesc != null ) {
      int len = fileDesc.length();
      while( (src < len) && (n > 0) ) {
	char ch = fileDesc.charAt( src++ );
	if( (ch >= '\u0020') && (ch <= 0xFF) ) {
	  out.write( ch );
	  --n;
	}
      }
    }
    while( n > 0 ) {
      out.write( '\u0020' );
      --n;
    }
    for( int i = 0; i < 5; i++ ) {
      out.write( 0 );
    }
    out.write( startAddr >= 0 ? 3 : 2 );
    out.write( fileBegAddr & 0xFF );
    out.write( fileBegAddr >> 8 );
    int fileEndAddr = fileBegAddr + endAddr - begAddr;
    out.write( fileEndAddr & 0xFF );
    out.write( fileEndAddr >> 8 );
    if( startAddr >= 0 ) {
      if( (startAddr >= begAddr) && (startAddr <= endAddr) ) {
	startAddr = startAddr + fileBegAddr - begAddr;
      }
      out.write( startAddr & 0xFF );
      out.write( startAddr >> 8 );
    } else {
      out.write( 0 );
      out.write( 0 );
    }
    for( int i = 0; i < 105; i++ ) {
      out.write( 0 );
    }
  }
}
