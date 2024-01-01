/*
 * (c) 2007-2021 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Anzeige der Hilfetexte
 */

package org.jens_mueller.jtcemu.platform.se.base;

import org.jens_mueller.jtcemu.base.AppContext;
import org.jens_mueller.jtcemu.base.JTCUtil;
import org.jens_mueller.jtcemu.platform.se.Main;

import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.Copies;
import javax.print.attribute.standard.JobName;
import javax.swing.*;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.IOException;
import java.net.URL;
import java.util.EmptyStackException;
import java.util.Locale;
import java.util.Stack;


public class HelpFrm extends BaseFrm
			implements
				ActionListener,
				CaretListener,
				HyperlinkListener,
				Printable
{
  public static class URLStackEntry
  {
    public URL    url;
    public Double viewPos;	// relative Angabe von 0.0 bis 1.0

    public URLStackEntry( URL url, Double viewPos )
    {
      this.url     = url;
      this.viewPos = viewPos;
    }
  };


  private static HelpFrm instance     = null;
  private static Point   lastLocation = null;

  private javax.swing.Timer    timer;
  private Double               posToScroll;
  private URL                  urlHome;
  private Stack<URLStackEntry> urlStack;
  private JMenuItem            mnuCopyPage;
  private JMenuItem            mnuCopyPageWithLinks;
  private JMenuItem            mnuPrint;
  private JMenuItem            mnuClose;
  private JMenuItem            mnuCopy;
  private JMenuItem            mnuSelectAll;
  private JMenuItem            mnuBack;
  private JMenuItem            mnuHome;
  private JButton              btnBack;
  private JButton              btnHome;
  private JButton              btnPrint;
  private JEditorPane          editorPane;
  private JScrollPane          scrollPane;


  public static void close()
  {
    if( instance != null )
      instance.doClose();
  }


  /*
   * Das Oeffnen bzw. Anzeigen des Fensters erfolgt erst im naechsten
   * Event-Verarbeitungszyklus, damit es keine Probleme gibt,
   * wenn die Methode aus einem modalen Dialog heraus aufgerufen werden.
   */
  public static void open( final String page )
  {
    EventQueue.invokeLater(
		new Runnable()
		{
		  @Override
		  public void run()
		  {
		    openInternal( page );
		  }
		} );
  }


  public void setPage( String page )
  {
    setURL(
	true,
	page != null ? getClass().getResource( page ) : null,
	null );
  }


	/* --- ActionListener --- */

  @Override
  public void actionPerformed( ActionEvent e )
  {
    Object src = e.getSource();
    if( src != null ) {
      if( src == this.mnuClose ) {
	doClose();
      } else if( src == this.mnuCopyPage ) {
	doCopyPage( true );
      } else if( src == this.mnuCopyPageWithLinks ) {
	doCopyPage( false );
      } else if( src == this.mnuCopy ) {
	this.editorPane.copy();
      } else if( src == this.mnuSelectAll ) {
	this.editorPane.selectAll();
      } else if( (src == this.mnuBack) || (src == this.btnBack) ) {
	doBack();
      } else if( (src == this.mnuHome) || (src == this.btnHome) ) {
	setURL( true, null, null );
      } else if( src == this.timer ) {
	this.timer.stop();
	doScrollTo( this.posToScroll );
	this.posToScroll = null;
      } else {
	GUIUtil.setWaitCursor( this, true );
	if( (src == this.mnuPrint) || (src == this.btnPrint) ) {
	  doPrint();
	}
	GUIUtil.setWaitCursor( this, false );
      }
    }
  }


	/* --- CaretListener --- */

  @Override
  public void caretUpdate( CaretEvent e )
  {
    int a = this.editorPane.getSelectionStart();
    int b = this.editorPane.getSelectionEnd();
    this.mnuCopy.setEnabled( (a >= 0) && (a < b) );
  }


	/* --- HyperlinkListener --- */

  @Override
  public void hyperlinkUpdate( HyperlinkEvent e )
  {
    if( e.getEventType() == HyperlinkEvent.EventType.ACTIVATED )
      setURL( true, e.getURL(), null );
  }


	/* --- Printable --- */

  @Override
  public int print(
		Graphics   g,
		PageFormat pf,
		int        pageNum ) throws PrinterException
  {
    double x = pf.getImageableX();
    double y = pf.getImageableY();
    double w = pf.getImageableWidth();
    double h = pf.getImageableHeight();

    if( (w < 1.0) || (h < 1.0) ) {
      throw new PrinterException(
		"Die Seite hat keinen bedruckbaren Bereich,\n"
			+ "da die R\u00E4nder zu gro\u00DF sind." );
    }


    // Skalierungsfaktort berechnen, damit die Breite stimmt
    double scale = w / editorPane.getWidth();

    // eigentliches Drucken
    int rv    = NO_SUCH_PAGE;
    int yOffs = pageNum * (int) Math.round( h / scale );
    if( (yOffs >= 0) && (yOffs < editorPane.getHeight()) ) {

      // Skalieren und zu druckender Bereich markieren
      if( (scale < 1.0) && (g instanceof Graphics2D) ) {
	((Graphics2D) g).scale( scale, scale );

	x /= scale;
	y /= scale;
	w /= scale;
	h /= scale;
      }

      // Seite drucken
      int xInt = (int) Math.round( x );
      int yInt = (int) Math.round( y );
      int wInt = (int) Math.round( w );
      int hInt = (int) Math.round( h );

      g.clipRect( xInt, yInt, wInt + 1, hInt + 1 );
      g.translate( xInt, (yInt - yOffs) );
      editorPane.print( g );

      rv = PAGE_EXISTS;
    }
    return rv;
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  protected boolean doClose()
  {
    boolean rv = super.doClose();
    if( rv ) {
      lastLocation = getLocation();
      instance     = null;
    }
    return rv;
  }


  @Override
  public String getPropPrefix()
  {
    return "help.";
  }


	/* --- private Konstruktoren und Methoden --- */

  private HelpFrm()
  {
    setTitle( AppContext.getAppName() + " Hilfe" );
    this.timer    = new javax.swing.Timer( 500, this );
    this.urlStack = new Stack<URLStackEntry>();
    this.urlHome  = getClass().getResource( "/help/se/home.htm" );


    // Menu
    JMenuBar mnuBar = new JMenuBar();
    setJMenuBar( mnuBar );


    // Menu Datei
    JMenu mnuFile = new JMenu( "Datei" );
    mnuFile.setMnemonic( KeyEvent.VK_D );
    mnuBar.add( mnuFile );

    this.mnuCopyPage = createJMenuItem(
			"Hilfeseite ohne Hypertext-Links kopieren" );
    mnuFile.add( this.mnuCopyPage );

    this.mnuCopyPageWithLinks = createJMenuItem(
			"Hilfeseite mit Hypertext-Links kopieren" );
    mnuFile.add( this.mnuCopyPageWithLinks );
    mnuFile.addSeparator();

    this.mnuPrint = createJMenuItem( "Drucken...", KeyEvent.VK_P );
    mnuFile.add( this.mnuPrint );
    mnuFile.addSeparator();

    this.mnuClose = createJMenuItem( "Schlie\u00DFen" );
    mnuFile.add( this.mnuClose );


    // Menu Bearbeiten
    JMenu mnuEdit = new JMenu( "Bearbeiten" );
    mnuEdit.setMnemonic( KeyEvent.VK_B );
    mnuBar.add( mnuEdit );

    this.mnuCopy = createJMenuItem( "Kopieren", KeyEvent.VK_C );
    this.mnuCopy.setEnabled( false );
    mnuEdit.add( this.mnuCopy );
    mnuEdit.addSeparator();

    this.mnuSelectAll = createJMenuItem( "Alles ausw\u00E4hlen" );
    mnuEdit.add( this.mnuSelectAll );


    // Menu Navigation
    JMenu mnuNav = new JMenu( "Navigation" );
    mnuNav.setMnemonic( KeyEvent.VK_N );
    mnuBar.add( mnuNav );

    this.mnuBack = createJMenuItem( "Zur\u00FCck", KeyEvent.VK_B );
    mnuNav.add( this.mnuBack );

    this.mnuHome = createJMenuItem( "Startseite", KeyEvent.VK_H );
    mnuNav.add( this.mnuHome );


    // Fensterinhalt
    setLayout( new GridBagLayout() );

    GridBagConstraints gbc = new GridBagConstraints(
					0, 0,
					1, 1,
					0.0, 0.0,
					GridBagConstraints.NORTHWEST,
					GridBagConstraints.NONE,
					new Insets( 5, 0, 0, 0 ),
					0, 0 );

    // Werkzeugleiste
    JToolBar toolBar = new JToolBar();
    toolBar.setFloatable( false );
    toolBar.setBorderPainted( false );
    toolBar.setOrientation( JToolBar.HORIZONTAL );
    toolBar.setRollover( true );

    this.btnBack = GUIUtil.createImageButton(
				this,
				"/images/nav/back.png",
				"Zur\u00FCck" );
    toolBar.add( this.btnBack );

    this.btnHome = GUIUtil.createImageButton(
				this,
				"/images/nav/home.png",
				"Startseite" );
    toolBar.add( this.btnHome );
    toolBar.addSeparator();

    this.btnPrint = GUIUtil.createImageButton(
				this,
				"/images/file/print.png",
				"Drucken" );
    toolBar.add( this.btnPrint );

    add( toolBar, gbc );


    // Anzeigebereich
    gbc.anchor    = GridBagConstraints.CENTER;
    gbc.fill      = GridBagConstraints.BOTH;
    gbc.weightx   = 1.0;
    gbc.weighty   = 1.0;
    gbc.gridwidth = 2;
    gbc.gridy++;

    this.editorPane = new JEditorPane();
    this.editorPane.setMargin( new Insets( 5, 5, 5, 5 ) );
    this.editorPane.addCaretListener( this );
    this.editorPane.addHyperlinkListener( this );
    this.editorPane.setEditable( false );

    this.scrollPane = new JScrollPane( this.editorPane );
    add( this.scrollPane, gbc );


    // Fenstergroesse
    setResizable( true );
    if( !GUIUtil.applyWindowSettings( this ) ) {
      setSize( 500, 400 );
      setLocationByPlatform( true );
    }
  }


  private JMenuItem createJMenuItem( String text )
  {
    JMenuItem item = new JMenuItem( text);
    item.addActionListener( this );
    return item;
  }


  private JMenuItem createJMenuItem( String text, int accKeyCode )
  {
    JMenuItem item = createJMenuItem( text);
    item.setAccelerator(
		KeyStroke.getKeyStroke(
				accKeyCode,
				GUIUtil.getMenuShortcutKeyMask( this ) ) );
    return item;
  }


  private void doBack()
  {
    if( this.urlStack.size() > 1 ) {
      try {
	this.urlStack.pop();	// aktuelle Seite vom Stack entfernen
	URLStackEntry entry = this.urlStack.pop();
	setURL( false, entry.url, entry.viewPos );
      }
      catch( EmptyStackException ex ) {}
    }
  }


  private void doCopyPage( boolean removeLinks )
  {
    try {
      URL url = this.urlHome;
      if( !this.urlStack.isEmpty() ) {
	url = urlStack.peek().url;
      }
      String htmlText = JTCUtil.loadHtml( url, removeLinks );
      if( htmlText != null ) {
	TransferableHTML t = new TransferableHTML( htmlText );
	getToolkit().getSystemClipboard().setContents( t, t );
      }
    }
    catch( Exception ex ) {
      Main.showError(
		this,
		"Die Seite konnte nicht nicht die Zwischenablage"
			+ " kopiert werden." );
    }
  }


  private void doPrint()
  {
    try {

      // Optionen setzen
      String                   jobName = "JU+TE-Computer Hilfe";
      PrintRequestAttributeSet attrs   = Main.getPrintRequestAttributeSet();
      attrs.add( new Copies( 1 ) );
      attrs.add( new JobName( jobName, Locale.getDefault() ) );

      PrinterJob pj = PrinterJob.getPrinterJob();
      pj.setCopies( 1 );
      pj.setJobName( jobName );
      if( pj.printDialog( attrs ) ) {
        pj.setPrintable( this );
        pj.print( attrs );
        Main.setPrintRequestAttributeSet( attrs );
      }
    }
    catch( PrinterException ex ) {
      Main.showError( this, ex );
    }
  }


  private void doScrollTo( Double viewPos )
  {
    if( viewPos != null ) {
      double d = viewPos.doubleValue();
      int    h = this.editorPane.getHeight();
      if( (d > 0.0) && (d <= 1.0) && (h > 0) ) {
	JViewport vp = this.scrollPane.getViewport();
	if( vp != null ) {
	  vp.setViewPosition(
		new Point( 0, (int) Math.round( d * (double) h ) ) );
	}
      }
    }
  }


  private static void openInternal( String page )
  {
    if( instance == null ) {
      instance = new HelpFrm();
      if( lastLocation != null ) {
	instance.setLocation( lastLocation );
      }
      instance.setVisible( true );
    }
    instance.setPage( page );
    GUIUtil.toFront( instance );
  }


  private void setURL( boolean saveCurViewPos, URL url, Double viewPos )
  {
    if( url == null ) {
      this.urlStack.clear();
      url = this.urlHome;
    }
    if( url != null ) {

      /*
       * Seite nur anzeigen, wenn sie sich von der vorhergehenden
       * unterscheidet
       */
      boolean       alreadyVisible = false;
      URLStackEntry topEntry       = null;
      if( this.urlStack.size() > 0 ) {
	topEntry = this.urlStack.peek();
	if( topEntry.url.equals( url ) ) {
	  alreadyVisible = true;
	}
      }
      if( !alreadyVisible ) {
	try {

	  // aktuelle Position ermitteln und im letzten Stack-Eintrag merken
	  if( saveCurViewPos && (topEntry != null) ) {
	    topEntry.viewPos = null;
	    int h = this.editorPane.getHeight();
	    if( h > 0 ) {
	      JViewport vp = this.scrollPane.getViewport();
	      if( vp != null ) {
		Point pt = vp.getViewPosition();
		if( pt != null ) {
		  double d = (double) pt.y / (double) h;
		  if( (d > 0.0) && (d <= 1.0) ) {
		    topEntry.viewPos = d;
		  }
		}
	      }
	    }
	  }

	  // neue Seite anzeigen
	  this.editorPane.setPage( url );

	  // wenn Seite angezeigt werden konnte, neuen Stack-Eintrag erzeugen
	  this.urlStack.push( new URLStackEntry( url, null ) );

	  // auf Position scrollen
	  if( viewPos != null ) {
	    this.posToScroll = viewPos;
	    this.timer.restart();
	  }

	  // Aktionsknoepfe aktualisieren
	  boolean stateBack = (this.urlStack.size() > 1);
	  this.mnuBack.setEnabled( stateBack );
	  this.btnBack.setEnabled( stateBack );

	  boolean stateHome = (stateBack || !url.equals( this.urlHome));
	  this.mnuHome.setEnabled( stateHome );
	  this.btnHome.setEnabled( stateHome );
	}
	catch( IOException ex ) {
	  JOptionPane.showMessageDialog(
		this,
		"Die Hilfeseite kann nicht angezeigt werden.",
		"Fehler",
		JOptionPane.ERROR_MESSAGE );
	}
      }
    }
  }
}
