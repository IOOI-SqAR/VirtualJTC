/*
 * (c) 2007 Jens Mueller
 * (c) 2017 Lars Sonchocky-Helldorf
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Basisklasse fuer alle Fenster,
 * deren Hauptinhalt ein mehrzeiliges Textfeld ist
 */

package jtcemu.base;

import java.awt.Font;
import java.awt.event.*;
import java.awt.print.*;
import java.io.*;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.*;
import javax.swing.JTextArea;
import javax.swing.text.*;
import jtcemu.Main;


public abstract class AbstractTextFrm extends BaseFrm
{
  private static Locale locale = Locale.getDefault();
  private static ResourceBundle abstractTextFrmResourceBundle = ResourceBundle.getBundle("resources.AbstractTextFrm", locale);

  protected String    lineSep;
  protected JTextArea textArea;


  protected AbstractTextFrm()
  {
    this.lineSep  = null;
    this.textArea = new JTextArea();
    this.textArea.setFont( new Font( "Monospaced", Font.PLAIN, 12 ) );
  }


  protected void doPrint()
  {
    Printable printable = null;
    Font      font      = this.textArea.getFont();
    if( font != null ) {
      this.textArea.setFont(
        new Font(
                "Monospaced",
                Font.PLAIN,
                Main.getIntProperty( "jtcemu.print.font.size", 10 ) ) );
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
      atts.add( new JobName( "JTCEMU", Locale.getDefault() ) );

      PrinterJob pj = PrinterJob.getPrinterJob();
      pj.setCopies( 1 );
      pj.setJobName( "JTCEMU" );
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
      throw new IOException( abstractTextFrmResourceBundle.getString("error.readFile.fileToLarge.message") );
    }
    StringBuilder buf = new StringBuilder(
                                len >= 0 ? ((int) (len + 1)) : 0x4000 );

    String lineSep = null;
    Reader in      = null;
    try {
      in = new BufferedReader( new FileReader( file ) );

      boolean cr = false;
      int     ch = in.read();
      while( ch != -1 ) {
        if( ch == '\r' ) {
          if( cr ) {
            buf.append( (char) '\r' );
          } else {
            cr = true;
          }
        } else if( ch == '\n' ) {
          buf.append( (char) '\n' );
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
            buf.append( (char) '\n' );
            cr = false;
            if( lineSep == null ) {
              lineSep = "\r";
            }
          } else {
            buf.append( (char) ch );
          }
        }
        ch = in.read();
      }
      setText( buf.toString() );
      this.lineSep = lineSep;
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
      if( out != null ) {
        try {
          out.close();
        }
        catch( IOException ex ) {}
      }
    }
  }
}

