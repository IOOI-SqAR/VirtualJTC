/*
 * (c) 2019 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Drucken von Text
 *
 * Da die beiden Methoden "PrinterJob.showPageSetupDialog( Window owner )"
 * und "PrinterJob.showPrintDialog( Window owner )" das uebergeordnete
 * Fenster (also dieses hier) nicht blockieren,
 * wird das mit dem Attribut "actionsEnabled" simuliert.
 */

package jtcemu.platform.fx.tools;

import java.io.IOException;
import java.util.Set;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.print.JobSettings;
import javafx.print.PageLayout;
import javafx.print.PageOrientation;
import javafx.print.PageRange;
import javafx.print.Paper;
import javafx.print.PrintColor;
import javafx.print.Printer;
import javafx.print.PrinterJob;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import jtcemu.base.AppContext;
import jtcemu.platform.fx.Main;
import jtcemu.platform.fx.base.GUIUtil;


public class PlainTextPrintDlg extends Stage implements Runnable
{
  public static class NamedItem<T>
  {
    private String name;
    private T      item;

    public NamedItem( String name, T item )
    {
      this.name = name;
      this.item = item;
    }

    public T getItem()
    {
      return this.item;
    }

    @Override
    public String toString()
    {
      return this.name;
    }
  };

  private static final int    DEFAULT_FONT_SIZE = 11;
  private static final double LINE_SPACING      = 1.0;

  private static boolean     lastTitleState  = false;
  private static boolean     lastPageNums    = false;
  private static boolean     lastFontBold    = false;
  private static Integer     lastFontSize    = null;
  private static Printer     lastPrinter     = null;
  private static JobSettings lastJobSettings = null;
  private static int         jobNum          = 1;

  private Main                         main;
  private String                       content;
  private String                       approvedTitle;
  private boolean                      actionsEnabled;
  private Font                         printFont;
  private PrinterJob                   printerJob;
  private ComboBox<NamedItem<Printer>> comboPrinter;
  private ComboBox<Integer>            comboFontSize;
  private Label                        labelPaperInfo;
  private CheckBox                     tglFontBold;
  private CheckBox                     tglPageNums;
  private CheckBox                     tglTitle;
  private TextField                    fldTitle;


  public static void showAndWait( Main main, String content, String title )
  {
    if( content != null ) {
      if( content.isEmpty() ) {
	content = null;
      }
    }
    if( content != null ) {
      /*
       * Unter Linux wurde der Effekt beobachtet,
       * dass sich nur mit Angabe eines Druckers
       * ein PrinterJob-Objekt erzeugen liess.
       * Aus diesem Grund wird versucht,
       * einen Drucker zu finden.
       */
      Printer printer = lastPrinter;
      if( printer == null ) {
	printer = Printer.getDefaultPrinter();
      }
      if( printer == null ) {
	Set<Printer> printers = Printer.getAllPrinters();
	if( printers != null ) {
	  for( Printer p : printers ) {
	    if( p != null ) {
	      printer = p;
	      break;
	    }
	  }
	}
      }
      PrinterJob printerJob = null;
      if( printer != null ) {
	printerJob = PrinterJob.createPrinterJob( printer );
      } else {
	printerJob = PrinterJob.createPrinterJob();
      }
      if( printerJob != null ) {
	PlainTextPrintDlg dlg = new PlainTextPrintDlg(
						main,
						content,
						title,
						printerJob );
	dlg.showAndWait();
	if( dlg.printFont != null ) {
	  (new Thread( dlg )).start();
	} else {
	  printerJob.cancelJob();
	}
      } else {
	main.showError( "Drucken nicht m\u00F6glich" );
      }
    } else {
      main.showError( "Es gibt nichts zu drucken." );
    }
  }


	/* --- Runnable --- */

  @Override
  public void run()
  {
    boolean prinerJobDone = false;
    try {
      PageLayout  pageLayout  = null;
      JobSettings jobSettings = this.printerJob.getJobSettings();
      if( jobSettings != null ) {
	pageLayout = jobSettings.getPageLayout();
      }
      if( pageLayout == null ) {
	Printer printer = this.printerJob.getPrinter();
	if( printer != null ) {
	  pageLayout = printer.getDefaultPageLayout();
	}
      }
      if( pageLayout == null ) {
	throwPrintingFailed();
      }

      final double hPage = pageLayout.getPrintableHeight();
      final double wPage = pageLayout.getPrintableWidth();
      Text  textNode     = new Text( 0.0, 0.0, this.content )
				{
				  @Override
				  public double prefWidth( double h )
				  {
				    return wPage;
				  }
				};
      textNode.setFont( this.printFont );
      textNode.setLineSpacing( LINE_SPACING );
      textNode.setTextAlignment( TextAlignment.LEFT );
      textNode.setTextOrigin( VPos.TOP );
      textNode.autosize();

      double hTextTotal = textNode.prefHeight( wPage );
      double vSpacing   = this.printFont.getSize() * 1.5;

      // Hoehe Kopfzeile
      double hHeader    = 0.0;
      HBox   headerNode = null;
      if( this.approvedTitle != null ) {
	Text titleNode = new Text( this.approvedTitle );
	titleNode.setFont(
		Font.font(
			this.printFont.getFamily(),
			FontWeight.BOLD,
			this.printFont.getSize() + 2.0 ) );
	titleNode.setTextAlignment( TextAlignment.CENTER );
	titleNode.setTextOrigin( VPos.TOP );

	headerNode = new HBox( 0.0, titleNode );
	headerNode.setAlignment( Pos.TOP_CENTER );
	headerNode.autosize();
	hHeader = headerNode.prefHeight( wPage ) + vSpacing;
      }
      if( (hHeader + hTextTotal) <= hPage ) {

	// Alles passt auf eine Seite
	Node pageNode = textNode;
	if( headerNode != null ) {
	  pageNode = new VBox( vSpacing, headerNode, textNode );
	}
	if( !this.printerJob.printPage( pageLayout, pageNode ) ) {
	  throwPrintingFailed();
	}

      } else {

	// Zeilen zaehlen
	int len   = this.content.length();
	int lines = 1;
	for( int i = 0; i < len; i++ ) {
	  if( this.content.charAt( i ) == '\n' ) {
	    lines++;
	  }
	}

	// Hoehe der Fusszeile ermitteln
	double hFooter     = 0.0;
	HBox   footerNode  = null;
	Text   pageNumNode = null;
	if( lastPageNums ) {
	  pageNumNode = new Text( "- 0123456789 -" );
	  pageNumNode.setFont( this.printFont );
	  pageNumNode.setTextAlignment( TextAlignment.CENTER );
	  pageNumNode.setTextOrigin( VPos.BOTTOM );

	  footerNode = new HBox( 0.0, pageNumNode );
	  footerNode.setAlignment( Pos.BOTTOM_CENTER );
	  footerNode.autosize();
	  hFooter = footerNode.prefHeight( wPage ) + vSpacing;
	}

	final double hText = hPage - hHeader - hFooter;
	textNode = new Text( 0.0, 0.0, "" )
			{
			  @Override
			  public double prefHeight( double w )
			  {
			    return hText;
			  }

			  @Override
			  public double prefWidth( double h )
			  {
			    return wPage;
			  }
			};
	textNode.setFont( this.printFont );
	textNode.setLineSpacing( LINE_SPACING );
	textNode.setTextAlignment( TextAlignment.LEFT );
	textNode.setTextOrigin( VPos.TOP );

	// Zeilen pro Seite berechnen
	double hRow        = hTextTotal / (double) lines;
	int    rowsPerPage = (int) (hText / hRow);
	if( rowsPerPage < 1 ) {
	  throw new IOException( "Druckbereich zu klein" );
	}

	// Node zur Darstellung einer Seite
	VBox pageNode = new VBox( vSpacing );
	pageNode.setFillWidth( true );
	if( headerNode != null ) {
	  pageNode.getChildren().add( headerNode );
	}
	pageNode.getChildren().add( textNode );
	if( footerNode != null ) {
	  pageNode.getChildren().add( footerNode );
	}

	// Seiten drucken
	int pageNum = 1;
	int idx     = 0;
	while( idx < len ) {
	  int begIdx = idx;
	  int rows   = 1;
	  while( idx < len ) {
	    char ch = this.content.charAt( idx++ );
	    if( ch == '\n' ) {
	      rows++;
	      if( rows >= rowsPerPage ) {
		textNode.setText(
			this.content.substring( begIdx, idx - 1 ) );
		if( pageNumNode != null ) {
		  pageNumNode.setText( String.format( "- %d -", pageNum++ ) );
		}
		if( !printerJob.printPage( pageLayout, pageNode ) ) {
		  throwPrintingFailed();
		}
		begIdx = idx;
		rows   = 1;
	      }
	    }
	  }
	  if( begIdx < len ) {
	    textNode.setText( this.content.substring( begIdx ) );
	    if( pageNumNode != null ) {
	      pageNumNode.setText( String.format( "- %d -", pageNum ) );
	    }
	    if( !this.printerJob.printPage( pageLayout, pageNode ) ) {
	      throwPrintingFailed();
	    }
	  }
	}
      }
      this.printerJob.endJob();
      prinerJobDone = true;
    }
    catch( Exception ex ) {
      Platform.runLater( ()->this.main.showError( ex ) );
    }
    finally {
      if( !prinerJobDone ) {
	this.printerJob.cancelJob();
      }
    }
  }


	/* --- Konstruktor --- */

  private PlainTextPrintDlg(
			Main       main,
			String     content,
			String     title,
			PrinterJob printerJob )
  {
    initOwner( main.getStage() );
    initModality( Modality.WINDOW_MODAL );
    setOnShown( e->GUIUtil.centerStageOnOwner( this ) );
    setResizable( true );
    setTitle( "Drucken" );
    Main.addIconsTo( this );
    this.main           = main;
    this.content        = content;
    this.printerJob     = printerJob;
    this.printFont      = null;
    this.approvedTitle  = null;
    this.actionsEnabled = true;

    JobSettings jobSettings = printerJob.getJobSettings();
    if( jobSettings != null ) {
      jobSettings.setCopies( 1 );
      jobSettings.setPageRanges( (PageRange[]) null );
      jobSettings.setPrintColor( PrintColor.MONOCHROME );
      jobSettings.setJobName( String.format(
					"%s Print Job %d",
					AppContext.getAppName(),
					jobNum++ ) );
    }


    // Drucker, Druckbereich und Anzahl Exemplare
    GridPane printerJobNode = new GridPane();
    printerJobNode.setPadding( new Insets( 5 ) );
    printerJobNode.setHgap( 5.0 );
    printerJobNode.setVgap( 5.0 );

    final Label labelPrinter = new Label( "Drucker:" );
    printerJobNode.add( labelPrinter, 0, 0 );

    this.comboPrinter = new ComboBox<>();
    this.comboPrinter.setEditable( false );
    for( Printer printer : Printer.getAllPrinters() ) {
      if( printer != null ) {
	this.comboPrinter.getItems().add(
		new NamedItem<>( printer.getName(), printer ) );
      }
    }
    printerJobNode.add( this.comboPrinter, 1, 0 );

    Button btnPrinterSettings = new Button( "Einstellungen..." );
    printerJobNode.add( btnPrinterSettings, 1, 1 );

    TitledPane printerJobPane = new TitledPane(
			"Drucker, Druckbereich und Anzahl Exemplare",
			printerJobNode );
    printerJobPane.setCollapsible( false );


    // Seitenlayout
    GridPane pageSettingsNode = new GridPane();
    pageSettingsNode.setPadding( new Insets( 5 ) );
    pageSettingsNode.setHgap( 5.0 );
    pageSettingsNode.setVgap( 5.0 );

    final Label labelPaper = new Label( "Papierformat:" );
    pageSettingsNode.add( labelPaper, 0, 0 );

    this.labelPaperInfo = new Label();
    pageSettingsNode.add( this.labelPaperInfo, 1, 0 );

    Button btnPageSettings = new Button( "Seite einrichten..." );
    pageSettingsNode.add( btnPageSettings, 1, 2 );

    TitledPane pageSettingsPane = new TitledPane(
					"Seitenlayout",
					pageSettingsNode );
    pageSettingsPane.setCollapsible( false );


    // Schrift
    GridPane fontNode = new GridPane();
    fontNode.setPadding( new Insets( 5 ) );
    fontNode.setHgap( 5.0 );
    fontNode.setVgap( 5.0 );

    final Label labelFontSize = new Label( "Schriftgr\u00F6\u00DFe:" );
    fontNode.add( labelFontSize, 0, 0 );

    this.comboFontSize = new ComboBox<>();
    this.comboFontSize.setEditable( false );
    this.comboFontSize.getItems().addAll(
		6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 18, 22, 24 );
    this.comboFontSize.setValue(
		lastFontSize != null ? lastFontSize : DEFAULT_FONT_SIZE );
    fontNode.add( this.comboFontSize, 1, 0 );

    fontNode.add( new Label( "Schriftstil:" ), 0, 1 );

    this.tglFontBold = new CheckBox( "Fett" );
    this.tglFontBold.setSelected( lastFontBold );
    fontNode.add( this.tglFontBold, 1, 1 );

    TitledPane fontPane = new TitledPane( "Schrift", fontNode );
    fontPane.setCollapsible( false );


    // Sonstiges
    GridPane etcNode = new GridPane();
    etcNode.setPadding( new Insets( 5 ) );
    etcNode.setHgap( 5.0 );
    etcNode.setVgap( 5.0 );

    this.tglTitle = new CheckBox( "Titel:" );
    this.tglTitle.setSelected( lastTitleState );
    etcNode.add( this.tglTitle, 0, 0 );

    this.fldTitle = new TextField();
    if( title != null ) {
      this.fldTitle.setText( title );
    }
    this.fldTitle.setDisable( !lastTitleState );
    this.fldTitle.setMaxWidth( Double.MAX_VALUE );
    etcNode.add( this.fldTitle, 1, 0 );
    GridPane.setHgrow( this.fldTitle, Priority.ALWAYS );

    this.tglPageNums = new CheckBox(
		"Seitenzahlen drucken (nur bei mehr als einer Seite)" );
    this.tglPageNums.setSelected( lastPageNums );
    GridPane.setColumnSpan( this.tglPageNums, 2 );
    etcNode.add( this.tglPageNums, 0, 1 );

    TitledPane etcPane = new TitledPane( "Sonstiges", etcNode );
    etcPane.setCollapsible( false );


    // Schaltflaechen
    Button btnPrint = new Button( "Drucken" );
    btnPrint.setMaxWidth( Double.MAX_VALUE );

    Button btnCancel = new Button( "Abbrechen" );
    btnCancel.setMaxWidth( Double.MAX_VALUE );

    TilePane buttonPane = new TilePane(
				Orientation.HORIZONTAL,
				10.0, 0.0,
				btnPrint, btnCancel );
    buttonPane.setAlignment( Pos.CENTER );
    buttonPane.setPrefColumns( 2 );


    // Gesamtinhalt
    VBox rootNode = new VBox(
			10.0,
			printerJobPane,
			pageSettingsPane,
			fontPane,
			etcPane,
			buttonPane );
    rootNode.setFillWidth( true );
    rootNode.setPadding( new Insets( 10.0 ) );
    setScene( new Scene( rootNode ) );

    final Stage stage = this;
    Platform.runLater( ()->sizeToScene() );
    Platform.runLater( ()->setToSamePrefWidth(
					labelPrinter,
					labelPaper,
					labelFontSize ) );


    // Vorbelegung
    if( lastJobSettings != null ) {
      jobSettings.setCollation( lastJobSettings.getCollation() );
      jobSettings.setPageLayout( lastJobSettings.getPageLayout() );
      jobSettings.setPaperSource( lastJobSettings.getPaperSource() );
      jobSettings.setPrintQuality( lastJobSettings.getPrintQuality() );
      jobSettings.setPrintResolution( lastJobSettings.getPrintResolution() );
      jobSettings.setPrintSides( lastJobSettings.getPrintSides() );
    }
    if( !selectPrinter( lastPrinter ) ) {
      if( !selectPrinter( Printer.getDefaultPrinter() ) ) {
	selectPrinter( printerJob.getPrinter() );
      }
    }
    updPaperInfo();
    titleStateChanged();


    // Aktionen
    this.comboPrinter.setOnAction( e->printerSelectionChanged() );
    btnPrinterSettings.setOnAction( e->doPrinterSettings() );
    btnPageSettings.setOnAction( e->doPageSettings() );
    this.tglTitle.setOnAction( e->titleStateChanged() );
    btnPrint.setOnAction( e->doPrint() );
    btnCancel.setOnAction( e->doCancel() );
    setOnCloseRequest( e->closeRequested( e ) );
  }


	/* --- private Methoden --- */

  private void closeRequested( WindowEvent e )
  {
    if( this.actionsEnabled ) {
      close();
    } else {
      e.consume();
    }
  }


  private void doCancel()
  {
    if( this.actionsEnabled )
      close();
  }


  private void doPageSettings()
  {
    if( this.actionsEnabled ) {
      try {
	this.actionsEnabled = false;
	if( this.printerJob.showPageSetupDialog( this.main.getStage() ) ) {
	  updPaperInfo();
	}
      }
      finally {
	this.actionsEnabled = true;
      }
    }
  }


  private void doPrinterSettings()
  {
    if( this.actionsEnabled ) {
      try {
	this.actionsEnabled = false;
	if( this.printerJob.showPrintDialog( this.main.getStage() ) ) {
	  selectPrinter( this.printerJob.getPrinter() );
	  updPaperInfo();
	}
      }
      finally {
	this.actionsEnabled = true;
      }
    }
  }


  private void doPrint()
  {
    if( this.actionsEnabled ) {
      Printer printer = this.printerJob.getPrinter();
      if( printer != null ) {
	this.printerJob.setPrinter( printer );	// nur zur Sicherheit
	lastPrinter     = printer;
	lastJobSettings = this.printerJob.getJobSettings();
	lastFontSize    = this.comboFontSize.getValue();
	lastFontBold    = this.tglFontBold.isSelected();
	lastTitleState  = this.tglTitle.isSelected();
	lastPageNums    = this.tglPageNums.isSelected();
	this.printFont  = Font.font(
				"Monospaced",
				lastFontBold ?
					FontWeight.BOLD
					: FontWeight.NORMAL,
				lastFontSize != null ?
					lastFontSize.doubleValue()
					: (double) DEFAULT_FONT_SIZE );
	/*
	 * Obwohl in den JobSettings die Farbe auf Monochrom gestellt wurde,
	 * kann in den druckerspezifischen Einstellungen trotzdem
	 * Farbdruck voreingestellt sein,
	 * Den man dann mit Klicken auf OK aktiviert.
	 * Aus diesem Grund wird hier zur Sicherheit nochmal
	 * Monochrom eingestellt.
	 */
	if( lastJobSettings != null ) {
	  lastJobSettings.setPrintColor( PrintColor.MONOCHROME );
	}

	if( lastTitleState ) {
	  this.approvedTitle = this.fldTitle.getText();
	  if( this.approvedTitle != null ) {
	    if( this.approvedTitle.isEmpty() ) {
	      this.approvedTitle = null;
	    }
	  }
	}
	close();
      }
    }
  }


  private void printerSelectionChanged()
  {
    NamedItem<Printer> item = this.comboPrinter.getValue();
    if( item != null ) {
      this.printerJob.setPrinter( item.getItem() );
    }
    updPaperInfo();
  }


  private boolean selectPrinter( Printer printerToSelect )
  {
    boolean rv = false;
    if( printerToSelect != null ) {
      NamedItem<Printer> itemToSelect = null;
      for( NamedItem<Printer> item : this.comboPrinter.getItems() ) {
	if( printerToSelect.equals( item.getItem() ) ) {
	  itemToSelect = item;
	  break;
	}
      }
      if( itemToSelect == null ) {
	itemToSelect = new NamedItem<>(
				printerToSelect.getName(),
				printerToSelect );
	this.comboPrinter.getItems().add( itemToSelect );
      }
      this.comboPrinter.setOnAction( null );
      this.comboPrinter.setValue( itemToSelect );
      this.comboPrinter.setOnAction( e->printerSelectionChanged() );
      this.printerJob.setPrinter( printerToSelect );
      rv = true;
    }
    return rv;
  }


  private static void setToSamePrefWidth( Region... regions )
  {
    double wMax = 0.0;
    for( Region r : regions ) {
      double w = r.prefWidth( -1.0 );
      if( w > wMax ) {
	wMax = w;
      }
    }
    if( wMax > 0.0 ) {
      for( Region r : regions ) {
	r.setPrefWidth( wMax );
      }
    }
  }


  private static void throwPrintingFailed() throws IOException
  {
    throw new IOException( "Fehler beim Drucken" );
  }


  private void titleStateChanged()
  {
    this.fldTitle.setDisable( !this.tglTitle.isSelected() );
  }


  private void updPaperInfo()
  {
    String      text        = "Standard";
    JobSettings jobSettings = this.printerJob.getJobSettings();
    if( jobSettings != null ) {
      PageLayout pageLayout = jobSettings.getPageLayout();
      if( pageLayout != null ) {
	StringBuilder buf   = new StringBuilder();
	Paper         paper = pageLayout.getPaper();
	if( paper != null ) {
	  buf.append( paper.getName() );
	}
	PageOrientation pageOrientation = pageLayout.getPageOrientation();
	if( pageOrientation != null ) {
	  switch( pageOrientation ) {
	    case LANDSCAPE:
	    case REVERSE_LANDSCAPE:
	      if( buf.length() > 0 ) {
		buf.append( '\u0020' );
	      }
	      buf.append( "Querformat" );
	      break;
	    case PORTRAIT:
	    case REVERSE_PORTRAIT:
	      if( buf.length() > 0 ) {
		buf.append( '\u0020' );
	      }
	      buf.append( "Hochformat" );
	      break;
	  }
	}
	if( buf.length() > 0 ) {
	  text = buf.toString();
	}
      }
    }
    this.labelPaperInfo.setText( text );
  }
}
