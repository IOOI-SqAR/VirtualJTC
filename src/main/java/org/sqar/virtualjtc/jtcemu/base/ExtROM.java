/*
 * (c) 2007-2010 Jens Mueller
 * (c) 2017 Lars Sonchocky-Helldorf
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Daten eines externen ROM-Images
 */

package org.sqar.virtualjtc.jtcemu.base;

import java.io.*;
import java.util.Locale;
import java.util.ResourceBundle;


public class ExtROM implements Comparable<ExtROM>
{
  private static final Locale locale = Locale.getDefault();
  private static final ResourceBundle extROMResourceBundle = ResourceBundle.getBundle("ExtROM", locale);

  private int    begAddr;
  private int    endAddr;
  private File   file;
  private byte[] fileBytes;
  private String text;


  public ExtROM( File file ) throws IOException
  {
    this.begAddr   = 0;
    this.endAddr   = 0;
    this.file      = file;
    this.fileBytes = null;
    this.text      = "%0000  " + this.file.getPath();
    reload();
  }


  public synchronized int getBegAddress()
  {
    return this.begAddr;
  }


  public synchronized int getEndAddress()
  {
    return this.endAddr;
  }


  public synchronized int getByte( int addr )
  {
    int rv = 0;
    if( this.fileBytes != null ) {
      int idx = addr - this.begAddr;
      if( (idx >= 0) && (idx < this.fileBytes.length) )
        rv = (int) this.fileBytes[ idx ] & 0xFF;
    }
    return rv;
  }


  public File getFile()
  {
    return this.file;
  }


  public synchronized void setBegAddress( int addr )
  {
    this.begAddr = addr;
    this.endAddr = 0;
    if( this.fileBytes != null ) {
      this.endAddr = this.begAddr + this.fileBytes.length - 1;
    }
    this.text = String.format(
                        "%%%04X  %s",
                        this.begAddr,
                        this.file.getPath() );
  }


  public void reload() throws IOException
  {
    InputStream in = null;
    try {
      int  bufSize = 0x1000;
      long fileLen = file.length();
      if( fileLen > 0 ) {
        if( fileLen > 0x10000 ) {
          bufSize = 0x10000;
        } else {
          bufSize = (int) fileLen;
        }
      }
      in = new FileInputStream( this.file );

      ByteArrayOutputStream buf = new ByteArrayOutputStream( bufSize );

      int n = 0;
      int b = in.read();
      while( b != -1 ) {
        if( n >= 0x10000 ) {
          throw new IOException( extROMResourceBundle.getString("error.reload.fileToLarge.message") );
        }
        buf.write( b );
        b = in.read();
        n++;
      }
      if( buf.size() > 0 ) {
        byte[] fileBytes = buf.toByteArray();
        if( fileBytes != null ) {
          this.fileBytes = fileBytes;
          this.endAddr   = this.begAddr + this.fileBytes.length - 1;
        }
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


        /* --- Comparable --- */

  @Override
  public int compareTo( ExtROM data )
  {
    return data != null ? (this.begAddr - data.begAddr) : -1;
  }


        /* --- ueberschriebene Methoden --- */

  @Override
  public boolean equals( Object o )
  {
    boolean rv = false;
    if( o != null ) {
      if( o == this ) {
        rv = true;
      } else {
        if( o instanceof ExtROM ) {
          rv          = true;
          ExtROM data = (ExtROM) o;
          if( this.begAddr != data.getBegAddress() ) {
            rv = false;
          } else {
            if( (this.fileBytes != null) && (data.fileBytes != null) ) {
              if( this.fileBytes.length == data.fileBytes.length ) {
                for( int i = 0; i < this.fileBytes.length; i++ ) {
                  if( this.fileBytes[ i ] != data.fileBytes[ i ] ) {
                    rv = false;
                    break;
                  }
                }
              } else {
                rv = false;
              }
            } else {
              int n1 = 0;
              if( this.fileBytes != null ) {
                n1 = this.fileBytes.length;
              }
              int n2 = 0;
              if( data.fileBytes != null ) {
                n2 = data.fileBytes.length;
              }
              if( n1 != n2 ) {
                rv = false;
              }
            }
          }
        }
      }
    }
    return rv;
  }


  @Override
  public String toString()
  {
    return this.text;
  }
}

