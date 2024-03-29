/*
 * (c) 2007-2008 Jens Mueller
 * (c) 2017 Lars Sonchocky-Helldorf
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Programmstart
 */

package org.sqar.virtualjtc.jtcemu;

import java.awt.*;
import java.io.*;
import java.net.URL;
import java.util.*;
import javax.print.attribute.*;
import javax.swing.*;
import org.sqar.virtualjtc.jtcemu.base.*;
import org.sqar.virtualjtc.z8.Z8;

import static org.sqar.virtualjtc.jtcemu.base.JTCSys.isRegInitZero;

public class Main
{
  private static final Locale locale = Locale.getDefault();
  private static final ResourceBundle mainResourceBundle = ResourceBundle.getBundle("Main", locale);

  private static final String propsFileName = "org.sqar.virtualjtc.jtcemu.props";
  
  private static TopFrm                   topFrm      = null;
  private static File                     lastFile    = null;
  private static File                     propsFile   = null;
  private static Properties               properties  = new Properties();
  private static PrintRequestAttributeSet printAtts   = null;
  private static java.util.List<Image>    windowIcons = null;


  public static void main( String[] args )
  {
    // Konfigurationsdatei ermitteln und laden
    try {
      String appData = System.getenv( "APPDATA" );
      if( appData != null ) {
        if( appData.length() > 0 ) {
          propsFile = new File( appData, propsFileName );
        }
      }
      if( propsFile == null ) {
        String homeDir = System.getProperty( "user.home" );
        if( homeDir != null ) {
          if( homeDir.length() > 0 ) {
            if( File.separatorChar == '/' ) {
              propsFile = new File( homeDir, "." + propsFileName );
            } else {
              propsFile = new File( homeDir, propsFileName );
            }
          }
        }
      }
    }
    catch( SecurityException ex ) {}
    if( propsFile == null ) {
      propsFile = new File( propsFileName );
    }
    if( propsFile.exists() ) {
      try {
        Reader in = null;
        try {
          in = new FileReader( propsFile );
          properties.load( in );

          // Erscheinungsbild setzen
          String lafClassName = properties.getProperty( "org.sqar.virtualjtc.jtcemu.laf.classname" );
          if( lafClassName != null ) {
            if( lafClassName.length() > 0 ) {
              try {
                UIManager.setLookAndFeel( lafClassName );
              }
              catch( Exception ex ) {}
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
      catch( IOException ex ) {
        showError( null, ex );
      }
    }

    // Emulation aufsetzen
    try {

      // Emulation der JTC-Hardware aufsetzen
      JTCSys jtcSys = new JTCSys();

      // Z8-Emulation mit externem 8MHz Takt aufsetzen
      Z8 z8 = new Z8( 8000000, isRegInitZero() , jtcSys, jtcSys);
      jtcSys.setZ8( z8 );
      jtcSys.settingsChanged();

      // Hauptfenster anlegen und Icons laden
      topFrm = new TopFrm( jtcSys, z8 );
      readWindowIcon( "/images/window/jtcemu_20x20.png" );
      readWindowIcon( "/images/window/jtcemu_32x32.png" );
      readWindowIcon( "/images/window/jtcemu_50x50.png" );
      setIconImages( topFrm );

      // externe ROM-Images laden
      int n = getIntProperty( "org.sqar.virtualjtc.rom.count", 0 );
      if( n > 0 ) {
        ArrayList<ExtROM> extROMs = new ArrayList<ExtROM>( n );
        for( int i = 0; i < n; i++ ) {
          String addrText = getProperty(
                        String.format( "org.sqar.virtualjtc.rom.%d.address", i + 1 ) );
          String fileName = getProperty(
                        String.format( "org.sqar.virtualjtc.rom.%d.file", i + 1 ) );
          if( (addrText != null) && (fileName != null) ) {
            try {
              int addr = Integer.parseInt( addrText, 16 );
              if( (addr >= 0) && (addr <= 0xFFFF) ) {
                try {
                  ExtROM extROM = new ExtROM( new File( fileName ) );
                  extROM.setBegAddr( addr );
                  extROMs.add( extROM );
                }
                catch( IOException ex ) {
                  String msg = ex.getMessage();
                  showError(
                        topFrm,
                        String.format( mainResourceBundle.getString("error.loadROMs.formatstring"), addr, msg != null ? msg : "" ) );
                }
              }
            }
            catch( Exception ex ) {};
          }
        }
        n = extROMs.size();
        if( n > 0 ) {
          try {
            jtcSys.setExtROMs( extROMs.toArray( new ExtROM[ n ] ) );
          }
          catch( ArrayStoreException ex ) {}
        }
        setProperty(
                "org.sqar.virtualjtc.rom.count",
                Integer.toString( extROMs.size() ) );
      }

      // Hauptfenster anzeigen
      topFrm.setVisible( true );
    }
    catch( IOException ex ) {
      showError( null, ex );
    }
  }


  public static boolean getBooleanProperty(
                                String  keyword,
                                boolean defaultValue )
  {
    boolean rv = defaultValue;
    String  s  = properties.getProperty( keyword );
    if( s != null ) {
      s = s.trim().toUpperCase();
      if( s.equals( "1" )
          || s.equals( "Y" )
          || s.equals( "TRUE" )
          || Boolean.parseBoolean( s ) )
      {
        rv = true;
      }
      if( s.equals( "0" ) || s.equals( "N" ) || s.equals( "FALSE" ) ) {
        rv = false;
      }
    }
    return rv;
  }


  public static Integer getIntegerProperty( String keyword )
  {
    Integer rv = null;
    String  s  = properties.getProperty( keyword );
    if( s != null ) {
      s = s.trim();
      if( s.length() > 0 ) {
        try {
          rv = Integer.valueOf( s );
        }
        catch( NumberFormatException ex ) {}
      }
    }
    return rv;
  }


  public static int getIntProperty( String keyword, int defaultValue )
  {
    Integer v = getIntegerProperty( keyword );
    return v != null ? v.intValue() : defaultValue;
  }


  public static File getLastPathFile()
  {
    File file = null;
    if( lastFile != null ) {
      if( lastFile.isDirectory() ) {
        file = lastFile;
      } else {
        file = lastFile.getParentFile();
      }
    }
    return file;
  }


  public static PrintRequestAttributeSet getPrintRequestAttributeSet()
  {
    if( printAtts == null ) {
      printAtts = new HashPrintRequestAttributeSet();
    }
    return printAtts;
  }


  public static Properties getProperties()
  {
    return properties;
  }


  public static File getPropertiesFile()
  {
    return propsFile;
  }


  public static String getProperty( String keyword )
  {
    return properties.getProperty( keyword );
  }


  public static void putProperties( Properties props )
  {
    if( props != null ) {
      properties.putAll( props );
    }
  }


  public static void setLastFile( File file )
  {
    lastFile = file;
  }


  public static void setPrintRequestAttributeSet( PrintRequestAttributeSet a )
  {
    printAtts = a;
  }


  public static void setProperty( String keyword, String value )
  {
    properties.setProperty( keyword, value );
  }


  public static void showError( Component owner, String msg )
  {
    JOptionPane.showMessageDialog(
                owner,
                msg != null ? msg : mainResourceBundle.getString("dialog.showError.default.message"),
                mainResourceBundle.getString("dialog.showError.title"),
                JOptionPane.ERROR_MESSAGE );
  }


  public static void showError( Component owner, Exception ex )
  {
    String msg = mainResourceBundle.getString("dialog.showError.default.message");
    if( ex != null ) {
      msg = ex.getMessage();
      if( msg != null ) {
        if( msg.length() < 1 )
          msg = null;
      }
      if( msg == null ) {
        msg = ex.getClass().getName();
      }
      showError( owner, msg );
    }
  }


  public static void setIconImages( Window window )
  {
    if( windowIcons != null )
      window.setIconImages( windowIcons );
  }


        /* --- private Methoden --- */

  private static void readWindowIcon( String resource )
  {
    URL url = Main.class.getResource( resource );
    if( url != null ) {
      Image image = topFrm.getToolkit().createImage( url );
      if( image != null ) {
        if( windowIcons == null ) {
          windowIcons = new ArrayList<Image>();
        }
        windowIcons.add( image );
      }
    }
  }
}

