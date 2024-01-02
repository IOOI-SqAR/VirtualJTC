/*
 * (c) 2014-2021 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Zugriff Informationen im Applikationskontext
 */

package org.sqar.virtualjtc.jtcemu.base;

import java.io.*;
import java.util.Properties;


public class AppContext
{
  public static final String COPYRIGHT = "(c) 2008-2021 Jens M\u00FCller";

  private static String     appName          = null;
  private static String     appVersion       = null;
  private static String     propPrefix       = "";
  private static File       lastPathsFile    = null;
  private static Runnable   onLastPathsSaved = null;
  private static Properties lastPaths        = new Properties();
  private static Properties properties       = new Properties();


  public static String getAboutContent()
  {
    return COPYRIGHT
		+ "\n"
		+ "Lizenz: GNU General Public License Version 3\n"
		+ "\n"
		+ "Benutzung des Programms erfolgt auf eigene Gefahr!\n"
		+ "Jegliche Haftung und Gew\u00E4hrleistung"
		+ " ist ausgeschlossen!\n"
		+ "\n"
		+ "Der Emulator enth\u00E4lt ROM-Images des"
		+ " Einchipmikrorechners U883\n"
		+ "sowie der Betriebssysteme, die mit der Bauanleitung des\n"
		+ "JU+TE-Computers ver\u00F6ffentlicht bzw."
		+ " von der Redaktion\n"
		+ "Jugend+Technik zur Verf\u00FCgung gestellt wurden.\n"
		+ "\n"
		+ "Die Urheberschaften an den ROM-Images liegen bei:\n"
		+ "- VEB Mikroelektronik Erfurt (Tiny-MP-BASIC-Interpreter)\n"
		+ "- Dr. Helmut Hoyer (2K-System und EMR-ES 1988)\n"
		+ "- Harun Scheutzow (ES 2.3 und ES 4.0)\n"
		+ "- Rolf Weidlich (Erweiterungen in ES 4.0c)";
  }


  public static String getAppName()
  {
    return appName;
  }


  public static String getAppVersion()
  {
    return appVersion;
  }


  public static boolean getBooleanProperty(
				String     keyword,
				boolean    defaultValue )
  {
    boolean rv = defaultValue;
    String  s  = getProperty( keyword );
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
    String  s  = getProperty( keyword );
    if( s != null ) {
      s = s.trim();
      if( !s.isEmpty() ) {
	try {
	  rv = Integer.valueOf( s );
	}
	catch( NumberFormatException ex ) {}
      }
    }
    return rv;
  }


  public static int getIntProperty( String keyword )
  {
    return getIntProperty( keyword, 0 );
  }


  public static int getIntProperty( String keyword, int defaultValue )
  {
    int    rv = defaultValue;
    String s  = getProperty( keyword );
    if( s != null ) {
      s = s.trim();
      if( !s.isEmpty() ) {
	try {
	  rv = Integer.parseInt( s );
	}
	catch( NumberFormatException ex ) {}
      }
    }
    return rv;
  }


  public static File getLastDirFile( String category )
  {
    String fileName = lastPaths.getProperty( propPrefix + category );
    return fileName != null ? new File( fileName ) : null;
  }


  public static Properties getProperties()
  {
    return properties;
  }


  public static String getProperty( String keyword )
  {
    if( !keyword.startsWith( propPrefix ) ) {
      keyword = propPrefix + keyword;
    }
    return properties.getProperty( keyword );
  }


  public static String getPropPrefix()
  {
    return propPrefix;
  }


  public static void putProperties( Properties props )
  {
    if( props != null )
      properties.putAll( props );
  }


  public static void setAppInfo( String name, String version )
  {
    appName    = name;
    appVersion = version;
  }


  public static void setLastFile( String category, File file )
  {
    if( file != null ) {
      if( !file.isDirectory() ) {
	file = file.getParentFile();
      }
    }
    if( file != null ) {
      lastPaths.setProperty( propPrefix + category, file.getPath() );

      file = lastPathsFile;
      if( file != null ) {
	OutputStream out = null;
	try {
	  out = new FileOutputStream( file );
	  lastPaths.storeToXML( out, "Last used file paths" );
	  out.close();
	  out = null;

	  // ggf. Listener informieren
	  Runnable runnable = onLastPathsSaved;
	  if( runnable != null ) {
	    runnable.run();
	  }
	}
	catch( IOException ex ) {
	  // zukuenftig nicht mehr speichern
	  lastPathsFile = null;
	}
	finally {
	  JTCUtil.closeSilently( out );
	}
      }
    }
  }


  public static void setLastPathsFile( File file )
  {
    lastPathsFile = file;

    InputStream in = null;
    try {
      in = new FileInputStream( file );
      lastPaths.loadFromXML( in );
    }
    catch( IOException ex ) {}
    finally {
      JTCUtil.closeSilently( in );
    }
  }


  public static void setOnLastPathsSaved( Runnable runnable )
  {
    onLastPathsSaved = runnable;
  }


  public static void setProperty( String keyword, String value )
  {
    if( !keyword.startsWith( propPrefix ) ) {
      keyword = propPrefix + keyword;
    }
    properties.setProperty( keyword, value != null ? value : "" );
  }


  public static void setProperty( String keyword, boolean value )
  {
    setProperty( keyword, String.valueOf( value ) );
  }


  public static void setProperty( String keyword, int value )
  {
    setProperty( keyword, String.valueOf( value ) );
  }


  public static void setPropPrefix( String text )
  {
    propPrefix = text;
  }


	/* --- Konstruktor --- */

  private AppContext()
  {
    // Klasse nicht instanziierbar
  }
}
