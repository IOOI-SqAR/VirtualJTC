/*
 * (c) 2007-2021 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Basisklasse fuer alle Fenster,
 * deren Hauptinhalt ein mehrzeiliges Textfeld ist
 */

package org.jens_mueller.jtcemu.platform.se.base;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.util.Locale;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.Copies;
import javax.print.attribute.standard.JobName;
import javax.swing.JTextArea;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Segment;
import org.jens_mueller.jtcemu.base.AppContext;
import org.jens_mueller.jtcemu.base.JTCUtil;
import org.jens_mueller.jtcemu.platform.se.Main;


public abstract class AbstractTextFrm extends BaseFrm
{
  public static final String PROP_PRINT_FONT_SIZE    = "print.font.size";
  public static final int    DEFAULT_PRINT_FONT_SIZE = 10;

  protected String    lineSep;
  protected JTextArea textArea;


  protected AbstractTextFrm()
  {
    this.lineSep  = null;
    this.textArea = new JTextArea();
    this.textArea.setFont( new Font( Font.MONOSPACED, Font.PLAIN, 12 ) );
  }


  protected void doPrint()
  {
    Printable printable = null;
    Font      font      = this.textArea.getFont();
    if( font != null ) {
      this.textArea.setFont(
	new Font(
		Font.MONOSPACED,
		Font.PLAIN,
		AppContext.getIntProperty(
				PROP_PRINT_FONT_SIZE,
				DEFAULT_PRINT_FONT_SIZE ) ) );
    }
    try {
      printable = this.textArea.getPrintable( null, null );
    }
    finally {
      if( font != null ) {
	this.textArea.setFont( font );
      }
    }
    if( printable != null ) {
      PrintRequestAttributeSet atts = Main.getPrintRequestAttributeSet();
      atts.add( new Copies( 1 ) );
      atts.add( new JobName( AppContext.getAppName(), Locale.getDefault() ) );

      PrinterJob pj = PrinterJob.getPrinterJob();
      pj.setCopies( 1 );
      pj.setJobName( AppContext.getAppName() );
      pj.setPrintable( printable );
      if( pj.printDialog( atts ) ) {
	try {
	  pj.print( atts );
	}
	catch( PrinterException ex ) {
	  Main.showError( this, ex );
	}
      }
    }
  }


  protected void gotoLine( int lineNum )
  {
    String text = this.textArea.getText();
    int    pos  = 0;
    try {
      pos = this.textArea.getLineStartOffset( lineNum - 1 );
    }
    catch( BadLocationException ex ) {
      if( text != null ) {
	pos = text.length();
      } else {
	pos = 0;
      }
    }
    this.textArea.requestFocus();

    final int lineBegPos = pos;
    try {
      this.textArea.setCaretPosition( lineBegPos );
    }
    catch( IllegalArgumentException ex ) {}

    // Zeile kurzzeitig markieren
    if( text != null ) {
      try {
	int eol = text.indexOf( '\n', lineBegPos );
	if( eol > lineBegPos ) {
	  final JTextArea textArea = this.textArea;
	  textArea.moveCaretPosition( eol );
	  javax.swing.Timer timer = new javax.swing.Timer(
			500,
			new ActionListener()
			{
			  public void actionPerformed( ActionEvent e )
			  {
			    try {
			      textArea.setCaretPosition( lineBegPos );
			      textArea.moveCaretPosition( lineBegPos );
			    }
			    catch( IllegalArgumentException ex ) {}
			  }
			} );
	  timer.setRepeats( false );
	  timer.start();
	}
      }
      catch( IllegalArgumentException ex ) {}
    }
  }


  protected void readFile( File file ) throws IOException
  {
    long len = file.length();
    if( len + 1 > Integer.MAX_VALUE ) {
      throw new IOException( "Datei zu gro\u00DF" );
    }
    StringBuilder buf = new StringBuilder(
				len >= 0 ? ((int) (len + 1)) : 0x4000 );

    String lineSep = null;
    Reader in      = null;
    try {
      in = new BufferedReader( new FileReader( file ) );

      boolean cr = false;
      int     ch = in.read();
      while( ch >= 0 ) {
	if( ch == '\r' ) {
	  if( cr ) {
	    buf.append( '\r' );
	  } else {
	    cr = true;
	  }
	} else if( ch == '\n' ) {
	  buf.append( '\n' );
	  if( cr ) {
	    cr = false;
	    if( lineSep == null ) {
	      lineSep = "\r\n";
	    }
	  } else {
	    if( lineSep == null ) {
	      lineSep = "\n";
	    }
	  }
	} else {
	  if( cr ) {
	    buf.append( '\n' );
	    cr = false;
	    if( lineSep == null ) {
	      lineSep = "\r";
	    }
	  }
	  buf.append( (char) ch );
	}
	ch = in.read();
      }
      setText( buf.toString() );
      this.lineSep = lineSep;
    }
    finally {
      JTCUtil.closeSilently( in );
    }
  }


  protected void setText( String text )
  {
    try {
      this.textArea.setText( text );
      this.textArea.setCaretPosition( 0 );
    }
    catch( IllegalArgumentException ex ) {}
  }


  protected void writeFile( File file ) throws IOException
  {
    BufferedWriter out = null;
    try {
      out = new BufferedWriter( new FileWriter( file ) );

      Document doc = this.textArea.getDocument();
      if( doc != null ) {
	int len = doc.getLength();
	if( len > 0 ) {
	  try {
	    Segment seg = new Segment();
	    seg.setPartialReturn( false );
	    doc.getText( 0, len, seg );
	    if( seg.array != null ) {
	      len = Math.min( seg.count, seg.array.length );
	      for( int i = 0; i < len; i++ ) {
		char ch = seg.array[ i ];
		if( ch == '\n' ) {
		  if( this.lineSep != null ) {
		    out.write( this.lineSep );
		  } else {
		    out.newLine();
		  }
		} else {
		  out.write( ch );
		}
	      }
	    }
	  }
	  catch( BadLocationException ex ) {}
	}
      }
    }
    finally {
      JTCUtil.closeSilently( out );
    }
  }
}
