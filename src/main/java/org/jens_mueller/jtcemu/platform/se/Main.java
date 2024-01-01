/*
 * (c) 2007-2021 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Programmstart
 */

package org.jens_mueller.jtcemu.platform.se;

import java.awt.Component;
import java.awt.Desktop;
import java.awt.EventQueue;
import java.awt.Image;
import java.awt.Window;
import java.io.Console;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Properties;
import java.util.regex.PatternSyntaxException;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import org.jens_mueller.jtcemu.base.AppContext;
import org.jens_mueller.jtcemu.base.JTCSys;
import org.jens_mueller.jtcemu.base.JTCUtil;
import org.jens_mueller.jtcemu.platform.se.base.TopFrm;
import org.jens_mueller.jtcemu.platform.se.tools.assembler.CmdLineAssembler;


public class Main
{
  public static final String APPNAME = "JTCEMU";
  public static final String VERSION = "2.1";
  public static final String APPINFO = APPNAME + " Version " + VERSION;

  public static final String PROP_LAF_CLASSNAME = "laf.classname";
  public static final String PROP_PREFIX        = "jtcemu.";
  public static final String PROP_VERSION       = "version";
  public static final String PROP_UI_SCALE      = "ui.scale";
  public static final String VALUE_NONE         = "none";

  private static final String SYSPROP_UI_SCALE_VALUE = "sun.java2d.uiScale";
  private static final String SYSPROP_UI_SCALE_ENABLED
					= "sun.java2d.uiScale.enabled";

  private static final String pathsFileName = "jtcemu_paths.xml";
  private static final String propsFileName = "jtcemu_config.xml";

  private static TopFrm                   topFrm        = null;
  private static PrintWriter              consoleWriter = null;
  private static File                     pathsFile     = null;
  private static File                     propsFile     = null;
  private static PrintRequestAttributeSet printAtts     = null;
  private static java.util.List<Image>    windowIcons   = null;

  private static String[] usageLines = {
	"",
	"Aufruf:",
	"  java -jar jtcemu.jar <Argumente>",
	"",
	"Argumente:",
	"  <kein Argument>              Emulator starten",
	"  -h oder --help               diese Hilfe anzeigen",
	"  -V oder --version            Versionsnummer anzeigen",
	"  --as oder --assembler        Assembler starten",
	"  --as -h                      Hilfe zum Assembler anzeigen",
	"" };


  public static void main( String[] args )
  {
    // Unter MacOS Applikationsname setzen
    if( JTCUtil.isMacOS() ) {
      System.setProperty( "apple.awt.application.name", APPNAME );
    }

    // interner Applikationskontext setzen
    AppContext.setAppInfo( APPNAME, VERSION );
    AppContext.setPropPrefix( PROP_PREFIX );

    /*
     * In der Eingabeaufforderung von Windows
     * ist der DOS-Zeichensatz installiert.
     * Deshalb werden ueber System.out und System.err ausgegebene Umlaute
     * dort falsch angezeigt.
     * Ueber System.console() erfolgt die Ausgabe dagegen richtig.
     * Aus diesem Grund wird unter Windows die Console verwendet,
     * wenn sie vorhanden ist.
     */
    if( File.separatorChar == '\\' ) {
      Console console = System.console();
      if( console != null ) {
	consoleWriter = console.writer();
      }
    }

    // Konfigurationsdatei ermitteln
    try {
      String appData = System.getenv( "APPDATA" );
      if( appData != null ) {
	if( !appData.isEmpty() ) {
	  pathsFile = new File( appData, pathsFileName );
	  propsFile = new File( appData, propsFileName );
	}
      }
      String homeDir = System.getProperty( "user.home" );
      if( homeDir != null ) {
	if( !homeDir.isEmpty() ) {
	  if( pathsFile == null ) {
	    if( File.separatorChar == '/' ) {
	      pathsFile = new File( homeDir, "." + pathsFileName );
	    } else {
	      pathsFile = new File( homeDir, pathsFileName );
	    }
	  }
	  if( propsFile == null ) {
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

    // Kommenadozeile auswerten
    boolean done    = false;
    int     argIdx  = 0;
    if( argIdx < args.length ) {
      String arg = args[ argIdx++ ];
      if( arg.equals( "-?" )
	  || arg.equalsIgnoreCase( "-h" )
	  || arg.equalsIgnoreCase( "--help" ) )
      {
	printlnOut();
	printlnOut( APPINFO );
	printlnOut( AppContext.COPYRIGHT );
	for( String s : usageLines ) {
	  printlnOut( s );
	}
	exitSuccess();
      }
      else if( arg.equals( "-V" ) || arg.equalsIgnoreCase( "--version" ) ) {
	printlnOut( APPINFO );
	exitSuccess();
      }
      else if( arg.equals( "--as" )
	       || arg.equalsIgnoreCase( "--assembler" ) )
      {
	if( CmdLineAssembler.execute( args, argIdx ) ) {
	  exitSuccess();
	} else {
	  exitFailure();
	}
      }
      else if( arg.startsWith( "-" ) ) {
	printlnErr( String.format( "Unbekannte Option \'%s\'", arg ) );
      }
    }

    if( !done ) {

      // gespeicherte Eigenschaften lesen
      Exception propsEx = null;
      if( pathsFile == null ) {
	pathsFile = new File( pathsFileName );
      }
      AppContext.setLastPathsFile( pathsFile );
      if( propsFile == null ) {
	propsFile = new File( propsFileName );
      }
      if( propsFile.exists() ) {
	InputStream in = null;
	try {
	  in               = new FileInputStream( propsFile );
	  Properties props = new Properties();
	  props.loadFromXML( in );
	  String v = props.getProperty( PROP_PREFIX + PROP_VERSION );
	  if( v != null ) {
	    if( v.equals( VERSION ) ) {
	      AppContext.putProperties( props );
	    }
	  }
	}
	catch( IOException ex ) {
	  propsEx = ex;
	}
	finally {
	  JTCUtil.closeSilently( in );
	}
      }

      /*
       * UI-Skalierung
       *
       * Wenn in den Systemeigenschaften Einstellungen zur UI-Skalierung
       * zu finden sind, d.h. wenn also beim Aufruf von JKCEMU
       * welche explizit angegeben wurden
       * sollen die im Profil befindlichen Einstellungen
       * nicht angewendet werden.
       */
      if( !containsSysProperty( SYSPROP_UI_SCALE_VALUE )
	  && !containsSysProperty( SYSPROP_UI_SCALE_ENABLED )
	  && !containsSysProperty( "sun.java2d.win.uiScaleX" )
	  && !containsSysProperty( "sun.java2d.win.uiScaleY" ) )
      {
	String text = AppContext.getProperty( PROP_UI_SCALE );
	if( text != null ) {
	  text = text.trim();
	  if( text.equalsIgnoreCase( VALUE_NONE ) ) {
	    System.setProperty( SYSPROP_UI_SCALE_ENABLED, "false" );
	  } else {
	    Integer v = parseUIScalePercentText( text );
	    if( v != null ) {
	      System.setProperty( SYSPROP_UI_SCALE_ENABLED, "true" );
	      System.setProperty(
			SYSPROP_UI_SCALE_VALUE,
			String.valueOf( (float) v / 100F ) );
	    }
	  }
	}
      }

      // Bei Metal L&F fette Schrift ausschalten
      UIManager.put( "swing.boldMetal", Boolean.FALSE );

      // Emulator im AWT-Thread starten
      final Exception propsEx1 = propsEx;
      EventQueue.invokeLater(
		new Runnable()
		{
		  @Override
		  public void run()
		  {
		    startEmu( propsEx1 );
		  }
		} );
    }
  }


  public static void exitFailure()
  {
    System.exit( -1 );
  }


  public static void exitSuccess()
  {
    System.exit( 0 );
  }


  public static File getPathsFile()
  {
    return pathsFile;
  }


  public static PrintRequestAttributeSet getPrintRequestAttributeSet()
  {
    if( printAtts == null ) {
      printAtts = new HashPrintRequestAttributeSet();
    }
    return printAtts;
  }


  public static File getPropertiesFile()
  {
    return propsFile;
  }


  public static TopFrm getTopFrm()
  {
    return topFrm;
  }


  public static Integer parseUIScalePercentText( String text )
  {
    Integer rv = null;
    if( text != null ) {
      int pos = text.indexOf( '%' );
      if( pos >= 0 ) {
	text = text.substring( 0, pos );
      }
      try {
	int v = Integer.parseInt( text.trim() );
	if( (v >= 50) && (v <= 400) ) {
	  rv = Integer.valueOf( v );
	}
      }
      catch( NumberFormatException ex ) {}
    }
    return rv;
  }


  public static void printErr( String text )
  {
    if( consoleWriter != null ) {
      consoleWriter.print( text );
    } else {
      System.err.print( text );
    }
  }


  public static void printlnErr( String text )
  {
    if( consoleWriter != null ) {
      consoleWriter.println( text );
      consoleWriter.flush();
    } else {
      System.err.println( text );
    }
  }


  public static void printlnErr()
  {
    if( consoleWriter != null ) {
      consoleWriter.println();
      consoleWriter.flush();
    } else {
      System.err.println();
    }
  }


  public static void printOut( String text )
  {
    if( consoleWriter != null ) {
      consoleWriter.print( text );
    } else {
      System.out.print( text );
    }
  }


  public static void printlnOut( String text )
  {
    if( consoleWriter != null ) {
      consoleWriter.println( text );
      consoleWriter.flush();
    } else {
      System.out.println( text );
    }
  }


  public static void printlnOut()
  {
    if( consoleWriter != null ) {
      consoleWriter.println();
      consoleWriter.flush();
    } else {
      System.out.println();
    }
  }


  public static void setPrintRequestAttributeSet( PrintRequestAttributeSet a )
  {
    printAtts = a;
  }


  public static void showError( Component owner, String msg )
  {
    JOptionPane.showMessageDialog(
		owner,
		msg != null ? msg : "Unbekannter Fehler",
		"Fehler",
		JOptionPane.ERROR_MESSAGE );
  }


  public static void showError( Component owner, Exception ex )
  {
    if( ex != null ) {
      String msg = ex.getMessage();
      if( msg != null ) {
	if( msg.isEmpty() ) {
	  msg = null;
	}
      }
      if( msg == null ) {
	msg = ex.getClass().getName();
      }
      if( ex instanceof RuntimeException ) {
	try {
	  StringWriter sw = new StringWriter();
	  PrintWriter  pw = new PrintWriter( sw );
	  ex.printStackTrace( pw );
	  pw.flush();
	  pw.close();
	  sw.close();
	  for( String line : sw.toString().split( "\n" ) ) {
	    int idx1 = line.indexOf( "jtcemu." );
	    int idx2 = line.indexOf( "z8." );
	    if( (idx1 >= 0) || (idx2 >= 0) ) {
	      int idx3 = line.indexOf( '(' );
	      int idx4 = line.indexOf( ')' );
	      if( (idx3 >= 0) && (idx4 > (idx3 + 1)) ) {
		msg = String.format(
				"%s:\n%s",
				line.substring( idx3 + 1, idx4 ),
				msg );
		break;
	      }
	    }
	  }
	}
	catch( IOException ex1 ) {}
	catch( PatternSyntaxException ex2 ) {}
      }
      showError( owner, msg );
    }
  }


  public static void setIconImages( Window window )
  {
    if( windowIcons != null ) {
      if( !windowIcons.isEmpty() ) {
	window.setIconImages( windowIcons );
      }
    }
  }


	/* --- private Methoden --- */

  private static boolean containsSysProperty( String propName )
  {
    String s = System.getProperty( propName );
    return s != null ? !s.trim().isEmpty() : false;
  }


  private static void initDesktop( Image taskBarIcon )
  {
    /*
     * Default-Menu setzen (ab Java 9 moeglich),
     * relevant auf dem Mac
     */
    try {
      if( Desktop.isDesktopSupported() ) {
	Desktop d = Desktop.getDesktop();
	Method  m = d.getClass().getMethod(
					"setDefaultMenuBar",
					JMenuBar.class );

	JMenu menu = new JMenu();
	menu.add( topFrm.createAboutMenuItem() );
	menu.addSeparator();
	menu.add( topFrm.createQuitMenuItem() );

	JMenuBar mnuBar = new JMenuBar();
	mnuBar.add( menu );
	m.invoke( d, mnuBar );
      }
    }
    catch( Throwable t ) {}

    /*
     * Icon in Taskbar setzen (ab Java 9 moeglich),
     * Damit setzt man auf einem Mac das Dok-Icon.
     */
    if( taskBarIcon != null ) {
      try {
	Class<?> cl  = Main.class.forName( "java.awt.Taskbar" );
	Object   obj = cl.getDeclaredMethod( "getTaskbar" ).invoke( null );
	cl.getMethod( "setIconImage", Image.class ).invoke(
							obj,
							taskBarIcon );
      }
      catch( Throwable t ) {}
    }
  }


  private static void readWindowIcon( String resource )
  {
    URL url = Main.class.getResource( resource );
    if( url != null ) {
      Image image = topFrm.getToolkit().createImage( url );
      if( image != null ) {
	windowIcons.add( image );
      }
    }
  }


  private static void startEmu( Exception propsEx )
  {
    // ggf. Fehlermeldung vom Laden der Einstellungen anzeigen
    if( propsEx != null ) {
      showError( null, propsEx );
    }

    // Erscheinungsbild setzen
    String lafClassName = AppContext.getProperty( PROP_LAF_CLASSNAME );
    if( lafClassName != null ) {
      if( lafClassName.isEmpty() ) {
	lafClassName = null;
      }
    }
    if( lafClassName == null ) {
      String osName = System.getProperty( "os.name" );
      if( osName != null ) {
	osName = osName.toLowerCase();
	if( (osName.indexOf( "mac" ) >= 0)
	    || (osName.indexOf( "win" ) >= 0) )
	{
	  lafClassName = UIManager.getSystemLookAndFeelClassName();
	}
      }
    }
    if( lafClassName != null ) {
      if( !lafClassName.isEmpty() ) {
	try {
	  UIManager.setLookAndFeel( lafClassName );
	}
	catch( Exception ex ) {
	  ex.printStackTrace();
	}
      }
    }

    // Emulation aufsetzen
    try {

      // Emulation der JTC-Hardware aufsetzen
      JTCSys jtcSys = new JTCSys();

      // Hauptfenster anlegen und Icons laden
      topFrm      = new TopFrm( jtcSys );
      windowIcons = new ArrayList<>();
      readWindowIcon( "/images/window/jtcemu_20x20.png" );
      readWindowIcon( "/images/window/jtcemu_32x32.png" );
      readWindowIcon( "/images/window/jtcemu_48x48.png" );
      setIconImages( topFrm );

      // Desktop-Integration
      int nIcons = windowIcons.size();
      initDesktop( nIcons > 0 ? windowIcons.get( nIcons - 1 ) : null );

      // Hauptfenster anzeigen
      topFrm.setVisible( true );
      jtcSys.setErrorViewer( topFrm );

      // ggf. Fehler beim Laden der externen ROM-Dateien anzeigen
      String errText = jtcSys.getErrorText();
      if( errText != null ) {
	showError( topFrm, errText );
      }
    }
    catch( IOException ex ) {
      showError( null, ex );
      System.exit( -1 );
    }
  }
}
