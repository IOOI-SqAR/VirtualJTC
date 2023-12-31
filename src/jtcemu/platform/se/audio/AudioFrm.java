/*
 * (c) 2007-2021 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Emulation des Kassettenrecorderanschlusses und des Lautsprechers
 */

package jtcemu.platform.se.audio;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.io.IOException;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JTabbedPane;
import jtcemu.base.AppContext;
import jtcemu.base.JTCSys;
import jtcemu.platform.se.base.BaseFrm;
import jtcemu.platform.se.base.GUIUtil;
import jtcemu.platform.se.base.HelpFrm;


public class AudioFrm
		extends BaseFrm
		implements ActionListener, ComponentListener
{
  private static final String FILE_GROUP              = "audio";
  private static final String PROP_PREFIX_TAPE_IN     = "audio.tape.in.";
  private static final String PROP_PREFIX_TAPE_OUT    = "audio.tape.out.";
  private static final String PROP_PREFIX_LOUDSPEAKER = "audio.loudspeaker.";

  private static volatile AudioFrm instance = null;

  private JTCSys      jtcSys;
  private JMenuItem   mnuClose;
  private JMenuItem   mnuHelpContent;
  private JTabbedPane tabbedPane;
  private AudioInFld  tapeInFld;
  private AudioOutFld tapeOutFld;
  private AudioOutFld loudspeakerFld;


  public void checkOpenCPUSynchronLine() throws IOException
  {
    if( this.jtcSys.getZ8().isPause() ) {
      throw new IOException( "Der emulierte Einchipmikrorechner ist gerade"
		+ " auf Pause gesetzt.\n"
		+ "Aus diesem Grund kann kein Audiokanal ge\u00F6ffnet"
		+ " werden,\n"
		+ "der synchron zu diesem bedient werden soll." );
    }
    AudioIO.checkOpenExclCPUSynchronLine();
  }


  public static void checkOpen( JTCSys jtcSys )
  {
    boolean enabled = isAudioEnabled( PROP_PREFIX_TAPE_IN )
			|| isAudioEnabled( PROP_PREFIX_TAPE_OUT );
    if( !enabled && (jtcSys.getOSType() == JTCSys.OSType.OS2K) ) {
      enabled = isAudioEnabled( PROP_PREFIX_LOUDSPEAKER );
    }
    if( enabled ) {
      open( jtcSys );
      EventQueue.invokeLater(
			new Runnable()
			{
			  @Override
			  public void run()
			  {
			    checkEnable();
			  }
			} );
    }
  }


  public static void closeCPUSynchronLine()
  {
    if( instance != null ) {
      instance.tapeInFld.closeCPUSynchronLine();
      instance.tapeOutFld.closeCPUSynchronLine();
      instance.loudspeakerFld.closeCPUSynchronLine();
    }
  }


  public static void open( JTCSys jtcSys )
  {
    if( instance != null ) {
      instance.setVisible( true );
      instance.setState( Frame.NORMAL );
      instance.toFront();
    } else {
      instance = new AudioFrm( jtcSys );
      instance.setVisible( true );
    }
  }


  public static void quit()
  {
    if( instance != null )
      instance.doQuit();
  }


  public static void reset()
  {
    if( instance != null ) {
      instance.rebuildTabs();
      instance.tapeInFld.updThresholdFieldsEnabled();
    }
  }


  public void setSelectedComponent( Component c )
  {
    this.tabbedPane.setSelectedComponent( c );
  }


  public static void z8CyclesPerSecondChanged()
  {
    if( instance != null )
      instance.tapeInFld.z8CyclesPerSecondChanged();
  }


	/* --- ActionListener --- */

  @Override
  public void actionPerformed( ActionEvent e )
  {
    Object src = e.getSource();
    if( src == this.mnuHelpContent ) {
      HelpFrm.open( "/help/se/audio.htm" );
    }
    else if( src == this.mnuClose ) {
      doClose();
    }
  }


	/* --- ComponentListener --- */

  @Override
  public void componentHidden( ComponentEvent e )
  {
    // leer
  }


  @Override
  public void componentMoved( ComponentEvent e )
  {
    // leer
  }


  @Override
  public void componentResized( ComponentEvent e )
  {
    // leer
  }


  @Override
  public void componentShown( ComponentEvent e )
  {
    if( e.getSource() == this )
      pack();
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  protected boolean doClose()
  {
    boolean rv = this.tapeOutFld.confirmDataSaved();
    if( rv ) {
      rv = this.loudspeakerFld.confirmDataSaved();
    }
    if( rv ) {
      memorizeSettings();
      rv = super.doClose();
    }

    /*
     * Wenn beim Schliessen des Fensters keine Audiofunktion
     * mehr aktiv ist, soll dieses Fenster nicht wiederverwendet,
     * sondern beim naechsten Oeffnen neu erstellt werden,
     * um die Mixer-Liste neu einzulesen.
     */
    if( rv
	&& !this.tapeInFld.isAudioActive()
	&& !this.tapeOutFld.isAudioActive()
	&& !this.loudspeakerFld.isAudioActive() )
    {
      instance = null;
    }
    return rv;
  }


  @Override
  public String getPropPrefix()
  {
    return "audio.";
  }


  @Override
  public void lafChanged()
  {
    pack();
  }


  @Override
  public void memorizeSettings()
  {
    super.memorizeSettings();
    this.tapeInFld.memorizeSettings();
    this.tapeOutFld.memorizeSettings();
    this.loudspeakerFld.memorizeSettings();
  }


	/* --- Konstruktor --- */

  private AudioFrm( JTCSys jtcSys )
  {
    this.jtcSys    = jtcSys;
    this.tapeInFld = new AudioInFld(
				this,
				this.jtcSys,
				PROP_PREFIX_TAPE_IN );
    this.tapeOutFld = new AudioOutFld(
				this,
				this.jtcSys,
				PROP_PREFIX_TAPE_OUT,
				false );
    this.loudspeakerFld = new AudioOutFld(
				this,
				this.jtcSys,
				PROP_PREFIX_LOUDSPEAKER,
				true );
    setTitle( AppContext.getAppName() + " Audio/Kassette" );


    // Menu
    JMenuBar mnuBar = new JMenuBar();
    setJMenuBar( mnuBar );


    // Menu Datei
    JMenu mnuFile = new JMenu( "Datei" );
    mnuFile.setMnemonic( KeyEvent.VK_D );
    mnuBar.add( mnuFile );

    this.mnuClose = new JMenuItem( "Schlie\u00DFen" );
    this.mnuClose.addActionListener( this );
    mnuFile.add( this.mnuClose );


    // Menu Hilfe
    JMenu mnuHelp = new JMenu( "Hilfe" );
    mnuHelp.setMnemonic( KeyEvent.VK_H );
    mnuBar.add( mnuHelp );

    this.mnuHelpContent = new JMenuItem( "Hilfe zu Audio/Kassette..." );
    this.mnuHelpContent.addActionListener( this );
    mnuHelp.add( this.mnuHelpContent );


    // Fensterinhalt
    setLayout( new BorderLayout() );
    this.tabbedPane = new JTabbedPane( JTabbedPane.TOP );
    add( this.tabbedPane, BorderLayout.CENTER );


    // Reiter anlegen
    rebuildTabs();


    // sonstiges
    pack();
    if( !GUIUtil.applyWindowSettings( this ) ) {
      setLocationByPlatform( true );
    }
    setResizable( true );
    addComponentListener( this );
  }


	/* --- private Methoden --- */

  private static void checkEnable()
  {
    if( instance != null ) {
      instance.tapeInFld.checkEnable();
      instance.tapeOutFld.checkEnable();
      instance.loudspeakerFld.checkEnable();
    }
  }


  private void doQuit()
  {
    this.tapeInFld.doDisable();
    this.tapeOutFld.doDisable();
    this.loudspeakerFld.doDisable();
  }


  private static boolean isAudioEnabled( String propPrefix )
  {
    return AppContext.getBooleanProperty(
			propPrefix + AbstractAudioIOFld.PROP_ENABLED,
			false );
  }


  private void rebuildTabs()
  {
    int idx = this.tabbedPane.getSelectedIndex();
    this.tabbedPane.removeAll();
    this.tabbedPane.addTab( "Eingang Kassette", this.tapeInFld );
    if( this.jtcSys.supportsLoudspeaker() ) {
      this.tabbedPane.addTab( "Ausgang Kassette", this.tapeOutFld );
      this.tabbedPane.addTab( "Lautsprecher", this.loudspeakerFld );
    } else {
      this.tabbedPane.addTab(
			"Ausgang Kassette / Lautsprecher",
			this.tapeOutFld );
    }
    int n = this.tabbedPane.getTabCount();
    if( (idx < 0) || (idx >= n) ) {
      idx = n - 1;
    }
    if( idx >= 0 ) {
      this.tabbedPane.setSelectedIndex( idx );
    }
  }
}
