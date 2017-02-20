/*
 * (c) 2007-2010 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Fenster fuer die Einstellungen
 */

package jtcemu.base;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.lang.*;
import java.text.ParseException;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.Document;
import jtcemu.*;


public class SettingsFrm extends BaseFrm
			implements
				ActionListener,
				ChangeListener,
				ListSelectionListener
{
  private static final int MAX_MARGIN = 199;

  private static SettingsFrm instance = null;

  private TopFrm                      topFrm;
  private JTCSys                      jtcSys;
  private Vector<ExtROM>              extROMs;
  private File                        propsFile;
  private JPanel                      panelEtc;
  private JPanel                      panelLAF;
  private JPanel                      panelROM;
  private JRadioButton                btnOS2k;
  private JRadioButton                btnES1988;
  private JRadioButton                btnES23;
  private JRadioButton                btnES40;
  private JRadioButton                btnRAM1k;
  private JRadioButton                btnRAM2k;
  private JRadioButton                btnRAM8k;
  private JRadioButton                btnRAM32k;
  private JRadioButton                btnRAM64k;
  private JCheckBox                   btnAlwaysScreenActive;
  private JCheckBox                   btnEmuKeyboardSyscall;
  private JCheckBox                   btnEmuReg80ToEF;
  private JCheckBox                   btnReloadROMOnReset;
  private JCheckBox                   btnConfirmInit;
  private JCheckBox                   btnConfirmReset;
  private JCheckBox                   btnConfirmQuit;
  private JRadioButton                btnFileDlgEmu;
  private JRadioButton                btnFileDlgNative;
  private JComboBox                   comboScreenRefresh;
  private JList                       listROM;
  private JSpinner                    spinnerMargin;
  private JTextField                  fldPropsFile;
  private ButtonGroup                 grpLAF;
  private UIManager.LookAndFeelInfo[] lafs;
  private JButton                     btnApply;
  private JButton                     btnSave;
  private JButton                     btnHelp;
  private JButton                     btnClose;
  private JButton                     btnROMAdd;
  private JButton                     btnROMRemove;
  private JButton                     btnDeletePropsFile;
  private JTabbedPane                 tabbedPane;


  public static void close()
  {
    if( instance != null )
      instance.doClose();
  }


  public static void open( TopFrm topFrm, JTCSys jtcSys )
  {
    if( instance != null ) {
      instance.setState( Frame.NORMAL );
      instance.toFront();
    } else {
      instance = new SettingsFrm( topFrm, jtcSys );
      instance.setVisible( true );
    }
  }


	/* --- ActionListener --- */

  @Override
  public void actionPerformed( ActionEvent e )
  {
    Object src = e.getSource();
    if( src != null ) {
      if( src == this.btnROMAdd ) {
	doROMAdd();
      }
      else if( src == this.btnROMRemove ) {
	doROMRemove();
      }
      else if( src == this.btnDeletePropsFile ) {
	doDeletePropsFile();
      }
      else if( src == this.btnApply ) {
	doApply();
      }
      else if( src == this.btnSave ) {
	doSave();
      }
      else if( src == this.btnHelp ) {
	HelpFrm.open( "/help/settings.htm" );
      }
      else if( src == this.btnClose ) {
	doClose();
      }
      else if( src instanceof JRadioButton ) {
	updSysFields();
	setDataChanged();
      }
      else if( (src instanceof JCheckBox)
	       || (src instanceof JComboBox)
	       || (src instanceof JRadioButton) )
      {
	setDataChanged();
      }
    }
  }


	/* --- ChangeListener --- */

  @Override
  public void stateChanged( ChangeEvent e )
  {
    if( e.getSource() == this.spinnerMargin )
      setDataChanged();
  }


	/* --- ListSelectionListener --- */

  @Override
  public void valueChanged( ListSelectionEvent e )
  {
    this.btnROMRemove.setEnabled( this.listROM.getSelectedIndex() >= 0 );
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  protected boolean doClose()
  {
    boolean rv = super.doClose();
    if( rv ) {
      instance = null;
    }
    return rv;
  }


  @Override
  public void lafChanged()
  {
    pack();
  }


	/* --- private Methoden --- */

  private SettingsFrm( TopFrm topFrm, JTCSys jtcSys )
  {
    setTitle( "JTCEMU Einstellungen" );
    this.topFrm    = topFrm;
    this.jtcSys    = jtcSys;
    this.extROMs   = new Vector<ExtROM>();
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
    JPanel panelSys = new JPanel( new GridBagLayout() );
    this.tabbedPane.addTab( "System", panelSys );

    GridBagConstraints gbcSys = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.WEST,
						GridBagConstraints.NONE,
						new Insets( 5, 5, 0, 5 ),
						0, 0 );

    JTCSys.OSType osType = this.jtcSys.getOSType();
    ButtonGroup   grpOS  = new ButtonGroup();

    panelSys.add(
		new JLabel( "Betriebssystem / Grafikaufl\u00F6sung:" ),
		gbcSys );

    this.btnOS2k = new JRadioButton(
			"2 KByte BASIC-System / 64x64 Pixel",
			(osType != JTCSys.OSType.ES1988)
					&& (osType != JTCSys.OSType.ES23)
					&& (osType != JTCSys.OSType.ES40) );
    grpOS.add( this.btnOS2k );
    this.btnOS2k.addActionListener( this );
    gbcSys.insets.top  = 0;
    gbcSys.insets.left = 50;
    gbcSys.gridy++;
    panelSys.add( this.btnOS2k, gbcSys );

    this.btnES1988 = new JRadioButton(
			"4 KByte EMR-ES 1988 / 64x64 Pixel",
			osType == JTCSys.OSType.ES1988 );
    grpOS.add( this.btnES1988 );
    this.btnES1988.addActionListener( this );
    gbcSys.gridy++;
    panelSys.add( this.btnES1988, gbcSys );

    this.btnES23 = new JRadioButton(
			"4 KByte ES 2.3 / 128x128 Pixel",
			osType == JTCSys.OSType.ES23 );
    grpOS.add( this.btnES23 );
    this.btnES23.addActionListener( this );
    gbcSys.gridy++;
    panelSys.add( this.btnES23, gbcSys );

    this.btnES40 = new JRadioButton(
			"6 KByte ES 4.0 / 320x192 Pixel, 16 Farben",
			osType == JTCSys.OSType.ES40 );
    grpOS.add( this.btnES40 );
    this.btnES40.addActionListener( this );
    gbcSys.insets.bottom = 5;
    gbcSys.gridy++;
    panelSys.add( this.btnES40, gbcSys );

    this.btnAlwaysScreenActive = new JCheckBox(
		"Bildschirmausgabe auch bei gesperrtem Interrupt 4",
		this.jtcSys.getAlwaysScreenActive() );
    this.btnAlwaysScreenActive.addActionListener( this );
    gbcSys.insets.top    = 5;
    gbcSys.insets.bottom = 0;
    gbcSys.insets.left   = 5;
    gbcSys.gridy++;
    panelSys.add( this.btnAlwaysScreenActive, gbcSys );

    this.btnEmuKeyboardSyscall = new JCheckBox(
		"Systemroutine f\u00FCr Tastaturabfrage emulieren",
		this.jtcSys.getEmulateKeyboardSyscall() );
    this.btnEmuKeyboardSyscall.addActionListener( this );
    gbcSys.insets.top = 0;
    gbcSys.gridy++;
    panelSys.add( this.btnEmuKeyboardSyscall, gbcSys );

    this.btnEmuReg80ToEF = new JCheckBox(
		"Register %80 bis %EF emulieren",
		this.jtcSys.getEmulateRegisters80ToEF() );
    this.btnEmuReg80ToEF.addActionListener( this );
    gbcSys.insets.bottom = 5;
    gbcSys.gridy++;
    panelSys.add( this.btnEmuReg80ToEF, gbcSys );

    updSysFields();


    // Bereich RAM
    JPanel panelRAM = new JPanel( new GridBagLayout() );
    this.tabbedPane.addTab( "RAM", panelRAM );

    GridBagConstraints gbcRAM = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.WEST,
						GridBagConstraints.NONE,
						new Insets( 5, 5, 0, 5 ),
						0, 0 );

    ButtonGroup grpRAM  = new ButtonGroup();
    int         ramSize = this.jtcSys.getRAMSize();

    this.btnRAM1k = new JRadioButton(
			"1 KByte RAM (%E000-%E0FF und %FD00-%FFFF)",
			ramSize == 0x0400 );
    grpRAM.add( this.btnRAM1k );
    this.btnRAM1k.addActionListener( this );
    panelRAM.add( this.btnRAM1k, gbcRAM );

    this.btnRAM2k = new JRadioButton(
			"2 KByte RAM (%E000-%E4FF und %FD00-%FFFF)",
			ramSize == 0x0800 );
    grpRAM.add( this.btnRAM2k );
    this.btnRAM2k.addActionListener( this );
    gbcRAM.insets.top = 0;
    gbcRAM.gridy++;
    panelRAM.add( this.btnRAM2k, gbcRAM );

    this.btnRAM8k = new JRadioButton(
			"8 KByte RAM (%E000-%FFFF)",
			ramSize == 0x2000 );
    grpRAM.add( this.btnRAM8k );
    this.btnRAM8k.addActionListener( this );
    gbcRAM.gridy++;
    panelRAM.add( this.btnRAM8k, gbcRAM );

    this.btnRAM32k = new JRadioButton(
			"32 KByte RAM (%8000-%FFFF)",
			ramSize == 0x8000 );
    grpRAM.add( this.btnRAM32k );
    this.btnRAM32k.addActionListener( this );
    gbcRAM.gridy++;
    panelRAM.add( this.btnRAM32k, gbcRAM );

    this.btnRAM64k = new JRadioButton(
			"64 KByte RAM abz\u00FCglich ROM und IO-Bereich",
			ramSize == 0x10000 );
    grpRAM.add( this.btnRAM64k );
    this.btnRAM64k.addActionListener( this );
    gbcRAM.insets.bottom = 5;
    gbcRAM.gridy++;
    panelRAM.add( this.btnRAM64k, gbcRAM );


    // Bereich ROM
    this.panelROM = new JPanel( new GridBagLayout() );
    this.tabbedPane.addTab( "ROM", this.panelROM );

    GridBagConstraints gbcROM = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.WEST,
						GridBagConstraints.NONE,
						new Insets( 5, 5, 0, 5 ),
						0, 0 );

    this.panelROM.add( new JLabel( "Eingebundene ROM-Images:" ), gbcROM );

    this.listROM = new JList();
    this.listROM.setDragEnabled( false );
    this.listROM.setLayoutOrientation( JList.VERTICAL );
    this.listROM.addListSelectionListener( this );
    gbcROM.fill          = GridBagConstraints.BOTH;
    gbcROM.weightx       = 1.0;
    gbcROM.weighty       = 1.0;
    gbcROM.insets.top    = 0;
    gbcROM.insets.right  = 0;
    gbcROM.insets.bottom = 5;
    gbcROM.gridy++;
    this.panelROM.add( new JScrollPane( this.listROM ), gbcROM );

    JPanel panelROMBtn = new JPanel( new GridLayout( 2, 1, 5, 5 ) );
    gbcROM.fill         = GridBagConstraints.NONE;
    gbcROM.weightx      = 0.0;
    gbcROM.weighty      = 0.0;
    gbcROM.insets.right = 5;
    gbcROM.gridheight   = 1;
    gbcROM.gridx++;
    this.panelROM.add( panelROMBtn, gbcROM );

    this.btnROMAdd = new JButton( "Hinzuf\u00FCgen" );
    this.btnROMAdd.addActionListener( this );
    panelROMBtn.add( this.btnROMAdd );

    this.btnROMRemove = new JButton( "Entfernen" );
    this.btnROMRemove.setEnabled( false );
    this.btnROMRemove.addActionListener( this );
    panelROMBtn.add( this.btnROMRemove );

    ExtROM[] aExtROMs = this.jtcSys.getExtROMs();
    if( aExtROMs != null ) {
      for( int i = 0; i < aExtROMs.length; i++ ) {
	this.extROMs.add( aExtROMs[ i ] );
      }
      try {
	Collections.sort( this.extROMs );
      }
      catch( ClassCastException ex ) {}
      this.listROM.setListData( this.extROMs );
    }

    this.btnReloadROMOnReset = new JCheckBox(
			"ROM-Images bei jedem RESET neu laden",
			Main.getBooleanProperty(
					"jtcemu.rom.reload_on_reset",
					false ) );
    this.btnReloadROMOnReset.setEnabled( !this.extROMs.isEmpty() );
    this.btnReloadROMOnReset.addActionListener( this );
    gbcROM.gridx = 0;
    gbcROM.gridy++;
    this.panelROM.add( this.btnReloadROMOnReset, gbcROM );


    // Bereich Bestaetigungen
    JPanel panelConfirm = new JPanel( new GridBagLayout() );
    this.tabbedPane.addTab( "Best\u00E4tigungen", panelConfirm );

    GridBagConstraints gbcConfirm = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.WEST,
						GridBagConstraints.NONE,
						new Insets( 5, 5, 0, 5 ),
						0, 0 );

    panelConfirm.add(
		new JLabel( "Folgende Aktionen m\u00FCssen in einem"
				+ " Dialog best\u00E4tigt werden:" ),
		gbcConfirm );

    this.btnConfirmInit = new JCheckBox(
		"Einschalten/Initialisieren (Arbeitsspeicher l\u00F6schen)",
		Main.getBooleanProperty( "jtcemu.confirm.init", true ) );
    this.btnConfirmInit.addActionListener( this );
    gbcConfirm.insets.top  = 0;
    gbcConfirm.insets.left = 50;
    gbcConfirm.gridy++;
    panelConfirm.add( this.btnConfirmInit, gbcConfirm );

    this.btnConfirmReset = new JCheckBox(
		"Zur\u00FCcksetzen (RESET)",
		Main.getBooleanProperty( "jtcemu.confirm.reset", true ) );
    this.btnConfirmReset.addActionListener( this );
    gbcConfirm.gridy++;
    panelConfirm.add( this.btnConfirmReset, gbcConfirm );

    this.btnConfirmQuit = new JCheckBox(
		"JTCEMU beenden",
		Main.getBooleanProperty( "jtcemu.confirm.quit", true ) );
    this.btnConfirmQuit.addActionListener( this );
    gbcConfirm.insets.bottom = 5;
    gbcConfirm.gridy++;
    panelConfirm.add( this.btnConfirmQuit, gbcConfirm );


    // Bereich Erscheinungsbild
    this.panelLAF = null;
    this.grpLAF   = new ButtonGroup();
    this.lafs     = UIManager.getInstalledLookAndFeels();
    if( this.lafs != null ) {
      if( this.lafs.length > 1 ) {
	this.panelLAF = new JPanel( new GridBagLayout() );
	this.tabbedPane.addTab( "Erscheinungsbild", this.panelLAF );

	GridBagConstraints gbcLAF = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.WEST,
						GridBagConstraints.NONE,
						new Insets( 5, 5, 0, 5 ),
						0, 0 );

	String      curClName = null;
	LookAndFeel laf       = UIManager.getLookAndFeel();
	if( laf != null ) {
	  curClName = laf.getClass().getName();
	}

	for( int i = 0; i < this.lafs.length; i++ ) {
	  String       clName = this.lafs[ i ].getClassName();
	  JRadioButton btn    = new JRadioButton( this.lafs[ i ].getName() );
	  this.grpLAF.add( btn );
	  btn.setActionCommand( clName );
	  btn.addActionListener( this );
	  if( curClName != null ) {
	    if( clName.equals( curClName ) ) {
	      btn.setSelected( true );
	    }
	  }
	  if( i == this.lafs.length - 1 ) {
	    gbcLAF.insets.bottom = 5;
	  }
	  this.panelLAF.add( btn, gbcLAF );
	  gbcLAF.insets.top = 0;
	  gbcLAF.gridy++;
	}
      }
    }


    // Bereich Sonstiges
    this.panelEtc = new JPanel( new GridBagLayout() );
    this.tabbedPane.addTab( "Sonstiges", this.panelEtc );

    GridBagConstraints gbcEtc = new GridBagConstraints(
					0, 0,
					GridBagConstraints.REMAINDER, 1,
					0.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 0, 5 ),
					0, 0 );

    this.panelEtc.add( new JLabel( "Dateiauswahldialog:" ), gbcEtc );

    ButtonGroup grpFileSelect = new ButtonGroup();

    this.btnFileDlgEmu = new JRadioButton(
		"JTCEMU-Dateiauswahldialog verwenden",
		true );
    grpFileSelect.add( this.btnFileDlgEmu );
    this.btnFileDlgEmu.addActionListener( this );
    gbcEtc.insets.top  = 0;
    gbcEtc.insets.left = 50;
    gbcEtc.gridy++;
    this.panelEtc.add( this.btnFileDlgEmu, gbcEtc );

    this.btnFileDlgNative = new JRadioButton(
		"Dateiauswahldialog des Betriebssystems verwenden",
		false );
    grpFileSelect.add( this.btnFileDlgNative );
    this.btnFileDlgNative.addActionListener( this );
    gbcEtc.insets.bottom = 5;
    gbcEtc.gridy++;
    this.panelEtc.add( this.btnFileDlgNative, gbcEtc );

    String s = Main.getProperty( "jtcemu.filedialog" );
    if( s != null ) {
      if( s.equals( "native" ) ) {
	this.btnFileDlgNative.setSelected( true );
      }
    }

    gbcEtc.anchor      = GridBagConstraints.EAST;
    gbcEtc.insets.top  = 5;
    gbcEtc.insets.left = 5;
    gbcEtc.gridwidth   = 1;
    gbcEtc.gridy++;
    this.panelEtc.add( new JLabel( "Rand um Bildschirmausgabe:" ), gbcEtc );

    SpinnerModel sm = new SpinnerNumberModel( 0, 0, MAX_MARGIN, 1 );
    try {
      int margin = Main.getIntProperty( "jtcemu.screen.margin", 20 );
      if( (margin >= 0) && (margin <= MAX_MARGIN) ) {
	sm.setValue( new Integer( margin ) );
      }
    }
    catch( IllegalArgumentException ex ) {}

    this.spinnerMargin = new JSpinner( sm );
    this.spinnerMargin.addChangeListener( this );

    gbcEtc.anchor = GridBagConstraints.WEST;
    gbcEtc.gridx++;
    this.panelEtc.add( this.spinnerMargin, gbcEtc );

    gbcEtc.gridx++;
    this.panelEtc.add( new JLabel( "Pixel" ), gbcEtc );

    gbcEtc.anchor = GridBagConstraints.EAST;
    gbcEtc.gridx  = 0;
    gbcEtc.gridy++;
    this.panelEtc.add(
		new JLabel( "Zykluszeit f\u00FCr Aktualisierung"
				+ " der Bildschirmausgabe:" ),
		gbcEtc );

    this.comboScreenRefresh = new JComboBox();
    this.comboScreenRefresh.setEditable( false );
    this.comboScreenRefresh.addItem( "10" );
    this.comboScreenRefresh.addItem( "20" );
    this.comboScreenRefresh.addItem( "50" );
    this.comboScreenRefresh.addItem( "100" );
    this.comboScreenRefresh.addItem( "200" );
    this.comboScreenRefresh.setSelectedItem(
		Integer.toString( topFrm.getScreenRefreshMillis() ) );
    this.comboScreenRefresh.addActionListener( this );
    gbcEtc.anchor = GridBagConstraints.WEST;
    gbcEtc.gridx++;
    this.panelEtc.add( this.comboScreenRefresh, gbcEtc );

    gbcEtc.gridx++;
    this.panelEtc.add( new JLabel( "ms" ), gbcEtc );

    if( this.propsFile != null ) {
      gbcEtc.insets.bottom = 0;
      gbcEtc.gridx         = 0;
      gbcEtc.gridy++;
      this.panelEtc.add(
		new JLabel( "Einstellungen werden gespeichert in der Datei:" ),
		gbcEtc );

      this.fldPropsFile = new JTextField();
      this.fldPropsFile.setEditable( false );
      this.fldPropsFile.setText( this.propsFile.getAbsolutePath() );
      gbcEtc.fill          = GridBagConstraints.HORIZONTAL;
      gbcEtc.weightx       = 1.0;
      gbcEtc.insets.top    = 0;
      gbcEtc.insets.bottom = 5;
      gbcEtc.gridy++;
      this.panelEtc.add( this.fldPropsFile, gbcEtc );

      this.btnDeletePropsFile = new JButton( "L\u00F6schen" );
      if( !this.propsFile.exists() ) {
	this.btnDeletePropsFile.setEnabled( false );
      }
      this.btnDeletePropsFile.addActionListener( this );
      gbcEtc.fill      = GridBagConstraints.NONE;
      gbcEtc.weightx   = 0.0;
      gbcEtc.gridwidth = 2;
      gbcEtc.gridx++;
      this.panelEtc.add( this.btnDeletePropsFile, gbcEtc );
    } else {
      this.fldPropsFile       = null;
      this.btnDeletePropsFile = null;
    }


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


  private void doApply()
  {
    Properties    props  = new Properties();
    JTCSys.OSType osType = applySys( props );
    boolean state = applyROM( props );
    if( state ) {
      state = applyEtc( props );
    }
    if( state ) {
      state = applyLAF( props );
    }
    if( state ) {

      // Warnung, wenn die RAM-Groesse zu klein ist
      String msg = null;
      if( this.btnES1988.isSelected()
	  && this.btnRAM1k.isSelected() )
      {
	msg = "Das EMR-ES 1988 erfordert mindestens 2 KByte RAM.";
      }
      else if( (this.btnES23.isSelected() || this.btnES40.isSelected())
	       && (this.btnRAM1k.isSelected() || this.btnRAM2k.isSelected()) )
      {
	msg = "ES 2.3 und ES 4.0 erfordern mindestens 8 KByte RAM.";
      }
      if( msg != null ) {
	if( JOptionPane.showConfirmDialog(
		this,
		msg + "\nM\u00F6chten Sie die Einstellungen trotzdem"
			+ " \u00FCbernehmen?",
		"Warnung",
		JOptionPane.YES_NO_OPTION,
		JOptionPane.WARNING_MESSAGE ) != JOptionPane.YES_OPTION )
	{
	  state = false;
	}
      }
    }
    if( state ) {
      boolean forceReset = (osType != this.jtcSys.getOSType());

      // Array mit neuen ROMs erzeugen
      ExtROM[] newExtROMs = null;
      if( !this.extROMs.isEmpty() ) {
	try {
	  newExtROMs = this.extROMs.toArray(
				new ExtROM[ this.extROMs.size() ] );
	}
	catch( ArrayStoreException ex ) {}
      }

      /*
       * Wenn sich die eingebundenen ROM-Images geaendert haben,
       * ist ein RESET erforderlich.
       */
      if( !forceReset ) {
        int nNewExtROMs = 0;
        if( newExtROMs != null ) {
          nNewExtROMs = newExtROMs.length;
        }
	ExtROM[] oldExtROMs  = this.jtcSys.getExtROMs();
        int      nOldExtROMs = 0;
        if( oldExtROMs != null ) {
          nOldExtROMs = oldExtROMs.length;
        }
        if( nNewExtROMs == nOldExtROMs ) {
          if( (newExtROMs != null) && (oldExtROMs != null) ) {
            for( int i = 0; i < newExtROMs.length; i++ ) {
              if( !newExtROMs[ i ].equals( oldExtROMs[ i ] ) ) {
                forceReset = true;
                break;
              }
            }
          }
        } else {
          forceReset = true;
        }
      }

      // zuerst neue Eigenschaften setzen
      int ramSize = 0x10000;
      if( this.btnRAM1k.isSelected() ) {
	ramSize = 0x0400;
      } else if( this.btnRAM2k.isSelected() ) {
	ramSize = 0x0800;
      } else if( this.btnRAM8k.isSelected() ) {
	ramSize = 0x2000;
      } else if( this.btnRAM32k.isSelected() ) {
	ramSize = 0x8000;
      }
      if( ramSize != this.jtcSys.getRAMSize() ) {
          forceReset = true;
      }
      props.setProperty(
		"jtcemu.ram.size",
		String.format( "%dK", ramSize / 1024 ) );
      props.setProperty(
		"jtcemu.confirm.init",
		Boolean.toString( this.btnConfirmInit.isSelected() ) );
      props.setProperty(
		"jtcemu.confirm.reset",
		Boolean.toString( this.btnConfirmReset.isSelected() ) );
      props.setProperty(
		"jtcemu.confirm.quit",
		Boolean.toString( this.btnConfirmQuit.isSelected() ) );
      Main.putProperties( props );

      // JTCSys aktualisieren
      this.jtcSys.setExtROMs( newExtROMs );
      this.jtcSys.settingsChanged();
      if( forceReset ) {
	this.jtcSys.fireReset();
      }

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
      this.btnApply.setEnabled( false );
      if( this.btnSave != null ) {
	this.btnSave.setEnabled( true );
      }
    }
  }


  private void doDeletePropsFile()
  {
    if( this.propsFile != null ) {
      if( JOptionPane.showConfirmDialog(
		this,
		"M\u00F6chten Sie die gespeicherten Einstellungen"
			+ " l\u00F6schen?\n"
			+ "Beim n\u00E4chsten mal startet dann JTCEMU"
			+ " mit den\n"
			+ "Standardeinstellungen.",
		"Best\u00E4tigung",
		JOptionPane.YES_NO_OPTION ) == JOptionPane.YES_OPTION )
      {
	if( this.propsFile.delete() ) {
	  if( this.btnDeletePropsFile != null ) {
	    this.btnDeletePropsFile.setEnabled( false );
	  }
	} else {
	  JOptionPane.showMessageDialog(
		this,
		"Die Datei mit den gespeicherten Einstellungen\n"
			+ "konnte nicht gel\u00F6scht werden.",
		"Fehler",
		JOptionPane.ERROR_MESSAGE );
	}
      }
    }
  }


  private void doSave()
  {
    Properties props = Main.getProperties();
    if( (props != null) && (this.propsFile != null) ) {
      Frame[] frames = Frame.getFrames();
      if( frames != null ) {
	for( int i = 0; i < frames.length; i++ ) {
	  Frame  f = frames[ i ];
	  String p = f.getClass().getName();
	  props.setProperty( p + ".window.x", Integer.toString( f.getX() ) );
	  props.setProperty( p + ".window.y", Integer.toString( f.getY() ) );
	  if( f.isResizable() ) {
	    props.setProperty(
			p + ".window.width",
			Integer.toString( f.getWidth() ) );
	    props.setProperty(
			p + ".window.height",
			Integer.toString( f.getHeight() ) );
	  }
	  if( f instanceof BaseFrm ) {
	    ((BaseFrm) f).prepareSettingsToSave();
	  }
	}
      }
      try {
	Writer out = null;
	try {
	  out = new FileWriter( this.propsFile );
	  props.store( out, "JU+TE-Computer-Emulator" );
	  out.close();
	  out = null;
	  if( this.btnSave != null ) {
	    this.btnSave.setEnabled( false );
	  }
	  if( this.btnDeletePropsFile != null ) {
	    this.btnDeletePropsFile.setEnabled( true );
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
      catch( IOException ex ) {
	Main.showError( this, ex );
      }
    }
  }


  private void doROMAdd()
  {
    File file = FileDlg.showFileOpenDlg(
				this,
				"ROM-Datei laden",
				"Laden",
				Main.getLastPathFile(),
				GUIUtil.romFileFilter );
    if( file != null ) {
      try {
	ExtROM  rom      = new ExtROM( file );
	Integer addr     = null;
	String  fileName = file.getName();
	if( fileName != null ) {
	  fileName = fileName.toUpperCase();
	  int len  = fileName.length();
	  int pos  = fileName.indexOf( '_' );
	  while( (pos >= 0) && ((pos + 4) < len) ) {
	    if( isHexChar( fileName.charAt( pos + 1 ) )
		&& isHexChar( fileName.charAt( pos + 2 ) )
		&& isHexChar( fileName.charAt( pos + 3 ) )
		&& isHexChar( fileName.charAt( pos + 4 ) ) )
	    {
	      try {
		addr = Integer.valueOf(
				fileName.substring( pos + 1, pos + 5 ),
				16 );
	      }
	      catch( NumberFormatException ex ) {}
	      if( addr != null ) {
		break;
	      }
	    }
	    if( pos + 5 < len ) {
	      pos = fileName.indexOf( '_', pos + 1 );
	    } else {
	      pos = -1;
	    }
	  }
	}
	addr = GUIUtil.askHex4( this, "Anfangsadresse", addr );
	if( addr != null ) {
	  rom.setBegAddress( addr.intValue() );
	  this.extROMs.add( rom );
	  try {
	    Collections.sort( this.extROMs );
	  }
	  catch( ClassCastException ex ) {}
	  this.listROM.setListData( this.extROMs );
	  this.btnReloadROMOnReset.setEnabled( true );
	  setDataChanged();

	  int idx = this.extROMs.indexOf( rom );
	  if( idx >= 0 ) {
	    this.listROM.setSelectedIndex( idx );
	  }
	  Main.setLastFile( file );
	}
      }
      catch( Exception ex ) {
	Main.showError( this, ex );
      }
    }
  }


  private void doROMRemove()
  {
    int idx = this.listROM.getSelectedIndex();
    if( (idx >= 0) && (idx < this.extROMs.size()) ) {
      this.extROMs.remove( idx );
      this.listROM.setListData( this.extROMs );
      this.btnReloadROMOnReset.setEnabled( !this.extROMs.isEmpty() );
      setDataChanged();
    }
  }


  private boolean applyEtc( Properties props )
  {
    boolean rv = true;
    props.setProperty(
		"jtcemu.filedialog",
		this.btnFileDlgNative.isSelected() ? "native" : "jtcemu" );

    Object obj = this.spinnerMargin.getValue();
    props.setProperty(
		"jtcemu.screen.margin",
		obj != null ? obj.toString() : "0" );

    obj = this.comboScreenRefresh.getSelectedItem();
    if( obj != null ) {
      String text = obj.toString();
      if( text != null ) {
	props.setProperty( "jtcemu.screen.refresh.ms", text );
      }
    }
    return rv;
  }


  private boolean applyLAF( Properties props )
  {
    boolean     rv = true;
    ButtonModel bm = this.grpLAF.getSelection();
    if( bm != null ) {
      String lafClassName = bm.getActionCommand();
      if( lafClassName != null ) {
	if( lafClassName.length() > 0 ) {
	  boolean     lafChanged = true;
	  LookAndFeel oldLAF     = UIManager.getLookAndFeel();
	  if( oldLAF != null ) {
	    if( lafClassName.equals( oldLAF.getClass().getName() ) ) {
	      lafChanged = false;
	    }
	  }
	  if( lafChanged ) {
	    try {
	      UIManager.setLookAndFeel( lafClassName );
	      SwingUtilities.invokeLater(
				new Runnable()
				{
				  public void run()
				  {
				    informLAFChanged();
				  }
				} );
	      props.setProperty( "jtcemu.laf.classname", lafClassName );
	    }
	    catch( Exception ex ) {
	      rv = false;
	      if( this.panelLAF != null ) {
		this.tabbedPane.setSelectedComponent( this.panelLAF );
	      }
	      Main.showError(
			this,
			"Das Erscheinungsbild kann nicht"
				+ " eingestellt werden." );
	    }
	  }
	}
      }
    }
    return rv;
  }


  private boolean applyROM( Properties props )
  {
    boolean rv = true;
    int     n  = 0;
    int     a  = -1;
    for( ExtROM rom : this.extROMs ) {
      n++;
      int begAddr = rom.getBegAddress();
      if( begAddr <= a ) {
	this.tabbedPane.setSelectedComponent( this.panelROM );
	Main.showError(
		this,
		String.format(
			"ROM an Adresse %04X \u00FCberschneidet sich"
				+ " mit vorherigem ROM.",
			begAddr ) );
	rv = false;
	break;
      }
      props.setProperty(
		String.format( "jtcemu.rom.%d.address", n ),
		String.format( "%04X", begAddr ) );
      props.setProperty(
		String.format( "jtcemu.rom.%d.file", n ),
		rom.getFile().getPath() );
      a = rom.getEndAddress();
    }
    props.setProperty( "jtcemu.rom.count", Integer.toString( n ) );
    props.setProperty(
		"jtcemu.rom.reload_on_reset",
		Boolean.toString( this.btnReloadROMOnReset.isSelected() ) );
    return rv;
  }


  private JTCSys.OSType applySys( Properties props )
  {
    JTCSys.OSType rv = JTCSys.OSType.OS2K;
    if( this.btnES40.isSelected() ) {
      props.setProperty( "jtcemu.os", "ES4.0" );
      rv = JTCSys.OSType.ES40;
    } else if( this.btnES23.isSelected() ) {
      props.setProperty( "jtcemu.os", "ES2.3" );
      rv = JTCSys.OSType.ES23;
    } else if( this.btnES1988.isSelected() ) {
      props.setProperty( "jtcemu.os", "ES1988" );
      rv = JTCSys.OSType.ES1988;
    } else {
      props.setProperty( "jtcemu.os", "2K" );
    }
    props.setProperty(
		"jtcemu.screen.active.always",
		Boolean.toString( this.btnAlwaysScreenActive.isSelected() ) );
    props.setProperty(
		"jtcemu.keyboard.emulate_syscall",
		Boolean.toString( this.btnEmuKeyboardSyscall.isSelected() ) );
    props.setProperty(
		"jtcemu.z8.emulate_registers_80toEF",
		Boolean.toString( this.btnEmuReg80ToEF.isSelected() ) );
    return rv;
  }


  private void informLAFChanged()
  {
    Frame[] frames = Frame.getFrames();
    if( frames != null ) {
      for( int i = 0; i< frames.length; i++ ) {
	Frame frm = frames[ i ];
	if( frm != null ) {
	  SwingUtilities.updateComponentTreeUI( frm );
	  if( frm instanceof BaseFrm ) {
	    ((BaseFrm) frm).lafChanged();
	  }
	}
      }
    }
  }


  private static boolean isHexChar( char ch )
  {
    return ((ch >= '0') && (ch <= '9')) || ((ch >= 'A') && (ch <= 'F'));
  }


  private void setDataChanged()
  {
    this.btnApply.setEnabled( true );
    if( this.btnSave != null ) {
      this.btnSave.setEnabled( false );
    }
  }


  private void updSysFields()
  {
    boolean state = (this.btnOS2k.isSelected() || this.btnES1988.isSelected());
    this.btnEmuKeyboardSyscall.setEnabled( state );
    this.btnAlwaysScreenActive.setEnabled(
		state || this.btnES23.isSelected() );
  }
}

