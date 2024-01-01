/*
 * (c) 2007-2020 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Fenster fuer die Einstellungen
 */

package org.jens_mueller.jtcemu.platform.se.settings;

import java.awt.Component;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import org.jens_mueller.jtcemu.base.AppContext;
import org.jens_mueller.jtcemu.base.ExtROM;
import org.jens_mueller.jtcemu.base.JTCSys;
import org.jens_mueller.jtcemu.base.JTCUtil;
import org.jens_mueller.jtcemu.platform.se.Main;
import org.jens_mueller.jtcemu.platform.se.base.BaseFrm;
import org.jens_mueller.jtcemu.platform.se.base.GUIUtil;
import org.jens_mueller.jtcemu.platform.se.base.HelpFrm;
import org.jens_mueller.jtcemu.platform.se.base.TopFrm;


public class SettingsFrm extends BaseFrm implements ActionListener
{
  private static SettingsFrm instance     = null;
  private static Point       lastLocation = null;

  private TopFrm             topFrm;
  private JTCSys             jtcSys;
  private File               propsFile;
  private ConfirmSettingsFld tabConfirm;
  private EtcSettingsFld     tabEtc;
  private LAFSettingsFld     tabLAF;
  private RAMSettingsFld     tabRAM;
  private ROMSettingsFld     tabROM;
  private SystemSettingsFld  tabSys;
  private UIScaleSettingsFld tabUIScale;
  private JButton            btnApply;
  private JButton            btnSave;
  private JButton            btnHelp;
  private JButton            btnClose;
  private JTabbedPane        tabbedPane;


  public static void close()
  {
    if( instance != null )
      instance.doClose();
  }


  public static void open( TopFrm topFrm, JTCSys jtcSys )
  {
    if( instance == null ) {
      instance = new SettingsFrm( topFrm, jtcSys );
      if( lastLocation != null ) {
	instance.setLocation( lastLocation );
      }
      instance.setVisible( true );
    }
    GUIUtil.toFront( instance );
  }


  public JTCSys.OSType getSelectedOSType()
  {
    return this.tabSys.getSelectedOSType();
  }


  public void selectedOSTypeChanged( JTCSys.OSType osType )
  {
    this.tabROM.updRomBankFieldsEnabled( osType );
  }


  public void setDataChanged()
  {
    this.btnApply.setEnabled( true );
    if( this.btnSave != null ) {
      this.btnSave.setEnabled( false );
    }
  }


  public void propsFileDeleted()
  {
    if( this.btnSave != null )
      this.btnSave.setEnabled( true );
  }


	/* --- ActionListener --- */

  @Override
  public void actionPerformed( ActionEvent e )
  {
    Object src = e.getSource();
    if( src == this.btnApply ) {
      doApply();
    }
    else if( src == this.btnSave ) {
      doSave();
    }
    else if( src == this.btnHelp ) {
      HelpFrm.open( "/help/se/settings.htm" );
    }
    else if( src == this.btnClose ) {
      doClose();
    }
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
    return "settings.";
  }


  @Override
  public void lafChanged()
  {
    pack();
  }


	/* --- Konstruktor --- */

  private SettingsFrm( TopFrm topFrm, JTCSys jtcSys )
  {
    setTitle( AppContext.getAppName() + " Einstellungen" );
    this.topFrm    = topFrm;
    this.jtcSys    = jtcSys;
    this.propsFile = Main.getPropertiesFile();


    // Fensterinhalt
    setLayout( new GridBagLayout() );

    GridBagConstraints gbc = new GridBagConstraints(
					0, 0,
					1, 1,
					1.0, 1.0,
					GridBagConstraints.WEST,
					GridBagConstraints.BOTH,
					new Insets( 5, 5, 5, 5 ),
					0, 0 );

    this.tabbedPane = new JTabbedPane( JTabbedPane.TOP );
    add( this.tabbedPane, gbc );


    // Bereich System
    this.tabSys = new SystemSettingsFld( this, jtcSys );
    this.tabbedPane.addTab( "System", this.tabSys );


    // Bereich RAM
    this.tabRAM = new RAMSettingsFld( this, jtcSys );
    this.tabbedPane.addTab( "RAM", this.tabRAM );


    // Bereich ROM
    this.tabROM = new ROMSettingsFld( this, jtcSys );
    this.tabbedPane.addTab( "ROM", this.tabROM );


    // Bereich Bestaetigungen
    this.tabConfirm = new ConfirmSettingsFld( this );
    this.tabbedPane.addTab( "Best\u00E4tigungen", this.tabConfirm );


    // Bereich Erscheinungsbild
    this.tabLAF = new LAFSettingsFld( this );
    if( this.tabLAF.getNumSelectableLAFs() > 1 ) {
      this.tabbedPane.addTab( "Erscheinungsbild", this.tabLAF );
    } else {
      this.tabLAF = null;
    }


    // Bereich Fensterskalierung
    this.tabUIScale = new UIScaleSettingsFld( this );
    this.tabbedPane.addTab( "Fensterskalierung", this.tabUIScale );


    // Bereich Sonstiges
    this.tabEtc = new EtcSettingsFld( this, this.topFrm );
    this.tabbedPane.addTab( "Sonstiges", this.tabEtc );


    // Knoepfe
    JPanel panelBtn = new JPanel(
		new GridLayout( this.propsFile != null ? 4 : 3, 1, 5, 5 ) );

    gbc.anchor  = GridBagConstraints.NORTHEAST;
    gbc.fill    = GridBagConstraints.NONE;
    gbc.weightx = 0.0;
    gbc.weighty = 0.0;
    gbc.gridx++;
    add( panelBtn, gbc );

    this.btnApply = new JButton( "\u00DCbernehmen" );
    this.btnApply.setEnabled( false );
    this.btnApply.addActionListener( this );
    panelBtn.add( this.btnApply );

    if( this.propsFile != null ) {
      this.btnSave = new JButton( "Speichern" );
      this.btnSave.addActionListener( this );
      panelBtn.add( this.btnSave );
    } else {
      this.btnSave = null;
    }

    this.btnHelp = new JButton( "Hilfe..." );
    this.btnHelp.addActionListener( this );
    panelBtn.add( this.btnHelp );

    this.btnClose = new JButton( "Schlie\u00DFen" );
    this.btnClose.addActionListener( this );
    panelBtn.add( this.btnClose );


    // sonstiges
    setResizable( true );
    if( !GUIUtil.applyWindowSettings( this ) ) {
      pack();
      setLocationByPlatform( true );
    }
  }


	/* --- private Methoden --- */

  private void doApply()
  {
    Component tab = null;
    try {
      Properties props = new Properties();

      // Einstellungen im Tab System uebernehmen
      tab = this.tabSys;
      JTCSys.OSType osType = this.tabSys.applyInput( props );
 
      // Einstellungen im Tab RAM uebernehmen
      tab = this.tabRAM;
      this.tabRAM.applyInput( props );

      // Einstellungen im Tab ROM uebernehmen
      tab              = this.tabROM;
      ExtROM[] extROMs = this.tabROM.applyInput( props, osType );

      // Einstellungen im Tab Bestaetigungen uebernehmen
      tab = this.tabRAM;
      this.tabConfirm.applyInput( props );

      // Einstellungen im Tab Sonstiges uebernehmen
      tab = this.tabEtc;
      this.tabEtc.applyInput( props );

      // Einstellungen im Tab Erscheinungsbild uebernehmen
      if( this.tabLAF != null ) {
	tab = this.tabLAF;
	this.tabLAF.applyInput( props );
      }

      // Einstellungen fuer Fensterskalierung uebernehmen
      tab = this.tabEtc;
      this.tabUIScale.applyInput( props );

      // uebernommene Einstellungen aktivieren
      AppContext.putProperties( props );

      // Schaltflaechen aktualisieren
      this.btnApply.setEnabled( false );
      if( this.btnSave != null ) {
	this.btnSave.setEnabled( Main.getPropertiesFile() != null );
      }

      // JTCSys aktualisieren
      this.jtcSys.settingsChanged(
		extROMs,
		this.tabROM.isRomBankSelected() ?
					this.tabROM.getRomBank()
					: null );

      // zum Schluss (nach JTCSys!) andere Fenster informieren
      Frame[] frames = Frame.getFrames();
      if( frames != null ) {
	for( int i = 0; i < frames.length; i++ ) {
	  Frame frm = frames[ i ];
	  if( frm != null ) {
	    if( frm instanceof BaseFrm ) {
	      ((BaseFrm) frm).settingsChanged();
	    }
	  }
	}
      }
    }
    catch( Exception ex ) {
      if( tab != null ) {
	this.tabbedPane.setSelectedComponent( tab );
      }
      Main.showError( this, ex.getMessage() );
    }
  }


  private void doSave()
  {
    Properties props = AppContext.getProperties();
    if( (props != null) && (this.propsFile != null) ) {
      Frame[] frames = Frame.getFrames();
      if( frames != null ) {
	for( int i = 0; i < frames.length; i++ ) {
	  Frame f = frames[ i ];
	  if( f instanceof BaseFrm ) {
	    ((BaseFrm) f).memorizeSettings();
	  }
	}
      }
      try {
	OutputStream out = null;
	try {
	  out = new FileOutputStream( this.propsFile );
	  props.setProperty(
			Main.PROP_PREFIX + Main.PROP_VERSION,
			Main.VERSION );
	  props.storeToXML( out, AppContext.getAppName() );
	  out.close();
	  out = null;
	  if( this.btnSave != null ) {
	    this.btnSave.setEnabled( false );
	  }
	  this.tabEtc.propsFileSaved();
	}
	finally {
	  JTCUtil.closeSilently( out );
	}
      }
      catch( IOException ex ) {
	Main.showError( this, ex );
      }
    }
  }
}
