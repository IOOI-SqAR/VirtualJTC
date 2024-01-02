/*
 * (c) 2007-2010 Jens Mueller
 * (c) 2017 Lars Sonchocky-Helldorf
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Fenster fuer die Einstellungen
 */

package org.sqar.virtualjtc.jtcemu.base;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import org.sqar.virtualjtc.jtcemu.*;


public class SettingsFrm extends BaseFrm
                        implements
                                ActionListener,
                                ChangeListener,
                                ListSelectionListener
{
  private static final Locale locale = Locale.getDefault();
  private static final ResourceBundle settingsFrmResourceBundle = ResourceBundle.getBundle("SettingsFrm", locale);

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
      instance.setState(NORMAL);
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
        HelpFrm.open( settingsFrmResourceBundle.getString("help.path") );
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
    setTitle( settingsFrmResourceBundle.getString("window.title") );
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
    this.tabbedPane.addTab( settingsFrmResourceBundle.getString("tabs.sys"), panelSys );

    buildSysPanel(panelSys);

    updSysFields();


    // Bereich RAM
    JPanel panelRAM = new JPanel( new GridBagLayout() );
    this.tabbedPane.addTab( settingsFrmResourceBundle.getString("tabs.ram"), panelRAM );

    buildRamPanel(panelRAM);


    // Bereich ROM
    this.panelROM = new JPanel( new GridBagLayout() );
    this.tabbedPane.addTab( settingsFrmResourceBundle.getString("tabs.rom"), this.panelROM );

    buildRomPanel(this.panelROM);


    // Bereich Bestaetigungen
    JPanel panelConfirm = new JPanel( new GridBagLayout() );
    this.tabbedPane.addTab( settingsFrmResourceBundle.getString("tabs.confirm"), panelConfirm );

    buildConfirmPanel(panelConfirm);


    // Bereich Erscheinungsbild
    this.panelLAF = null;
    this.grpLAF   = new ButtonGroup();
    this.lafs     = UIManager.getInstalledLookAndFeels();
    if( this.lafs != null ) {
      if( this.lafs.length > 1 ) {
        this.panelLAF = new JPanel( new GridBagLayout() );
        this.tabbedPane.addTab( settingsFrmResourceBundle.getString("tabs.laf"), this.panelLAF );

        buildLAFPanel(this.panelLAF);
      }
    }


    // Bereich Sonstiges
    this.panelEtc = new JPanel( new GridBagLayout() );
    this.tabbedPane.addTab( settingsFrmResourceBundle.getString("tabs.etc"), this.panelEtc );

    buildEtcPanel(this.panelEtc, topFrm);


    // Knoepfe
    JPanel panelBtn = new JPanel(
                new GridLayout( this.propsFile != null ? 4 : 3, 1, 5, 5 ) );

    gbc.anchor  = GridBagConstraints.NORTHEAST;
    gbc.fill    = GridBagConstraints.NONE;
    gbc.weightx = 0.0;
    gbc.weighty = 0.0;
    gbc.gridx++;
    add( panelBtn, gbc );

    this.btnApply = new JButton( settingsFrmResourceBundle.getString("button.apply") );
    this.btnApply.setEnabled( false );
    this.btnApply.addActionListener( this );
    panelBtn.add( this.btnApply );

    if( this.propsFile != null ) {
      this.btnSave = new JButton( settingsFrmResourceBundle.getString("button.save") );
      this.btnSave.addActionListener( this );
      panelBtn.add( this.btnSave );
    } else {
      this.btnSave = null;
    }

    this.btnHelp = new JButton( settingsFrmResourceBundle.getString("button.help") );
    this.btnHelp.addActionListener( this );
    panelBtn.add( this.btnHelp );

    this.btnClose = new JButton( settingsFrmResourceBundle.getString("button.close") );
    this.btnClose.addActionListener( this );
    panelBtn.add( this.btnClose );


    // sonstiges
    setResizable( true );
    if( !GUIUtil.applyWindowSettings( this ) ) {
      pack();
      setLocationByPlatform( true );
    }
  }


  private void buildSysPanel(JPanel panelSys)
  {
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

    panelSys.add(new JLabel( settingsFrmResourceBundle.getString("tabs.sys.osVersionScreenResolution") ), gbcSys );

    this.btnOS2k = new JRadioButton(
                        settingsFrmResourceBundle.getString("button.sys.OS2k"),
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
                        settingsFrmResourceBundle.getString("button.sys.ES1988"),
                        osType == JTCSys.OSType.ES1988 );
    grpOS.add( this.btnES1988 );
    this.btnES1988.addActionListener( this );
    gbcSys.gridy++;
    panelSys.add( this.btnES1988, gbcSys );

    this.btnES23 = new JRadioButton(
                        settingsFrmResourceBundle.getString("button.sys.ES23"),
                        osType == JTCSys.OSType.ES23 );
    grpOS.add( this.btnES23 );
    this.btnES23.addActionListener( this );
    gbcSys.gridy++;
    panelSys.add( this.btnES23, gbcSys );

    this.btnES40 = new JRadioButton(
                        settingsFrmResourceBundle.getString("button.sys.ES40"),
                        osType == JTCSys.OSType.ES40 );
    grpOS.add( this.btnES40 );
    this.btnES40.addActionListener( this );
    gbcSys.insets.bottom = 5;
    gbcSys.gridy++;
    panelSys.add( this.btnES40, gbcSys );

    this.btnAlwaysScreenActive = new JCheckBox(
                settingsFrmResourceBundle.getString("button.sys.alwaysScreenActive"),
                this.jtcSys.getAlwaysScreenActive() );
    this.btnAlwaysScreenActive.addActionListener( this );
    gbcSys.insets.top    = 5;
    gbcSys.insets.bottom = 0;
    gbcSys.insets.left   = 5;
    gbcSys.gridy++;
    panelSys.add( this.btnAlwaysScreenActive, gbcSys );

    this.btnEmuKeyboardSyscall = new JCheckBox(
                settingsFrmResourceBundle.getString("button.sys.emuKeyboardSyscall"),
                this.jtcSys.getEmulateKeyboardSyscall() );
    this.btnEmuKeyboardSyscall.addActionListener( this );
    gbcSys.insets.top = 0;
    gbcSys.gridy++;
    panelSys.add( this.btnEmuKeyboardSyscall, gbcSys );

    this.btnEmuReg80ToEF = new JCheckBox(
                settingsFrmResourceBundle.getString("button.sys.emuReg80ToEF"),
                this.jtcSys.getEmulateRegisters80ToEF() );
    this.btnEmuReg80ToEF.addActionListener( this );
    gbcSys.insets.bottom = 5;
    gbcSys.gridy++;
    panelSys.add( this.btnEmuReg80ToEF, gbcSys );
  }


  private void buildRamPanel(JPanel panelRAM)
  {
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
                        settingsFrmResourceBundle.getString("button.ram.1k"),
                        ramSize == 0x0400 );
    grpRAM.add( this.btnRAM1k );
    this.btnRAM1k.addActionListener( this );
    
    panelRAM.add( this.btnRAM1k, gbcRAM );

    this.btnRAM2k = new JRadioButton(
                        settingsFrmResourceBundle.getString("button.ram.2k"),
                        ramSize == 0x0800 );
    grpRAM.add( this.btnRAM2k );
    this.btnRAM2k.addActionListener( this );
    gbcRAM.insets.top = 0;
    gbcRAM.gridy++;
    
    panelRAM.add( this.btnRAM2k, gbcRAM );

    this.btnRAM8k = new JRadioButton(
                        settingsFrmResourceBundle.getString("button.ram.8k"),
                        ramSize == 0x2000 );
    grpRAM.add( this.btnRAM8k );
    this.btnRAM8k.addActionListener( this );
    gbcRAM.gridy++;
    
    panelRAM.add( this.btnRAM8k, gbcRAM );

    this.btnRAM32k = new JRadioButton(
                        settingsFrmResourceBundle.getString("button.ram.32k"),
                        ramSize == 0x8000 );
    grpRAM.add( this.btnRAM32k );
    this.btnRAM32k.addActionListener( this );
    gbcRAM.gridy++;
    
    panelRAM.add( this.btnRAM32k, gbcRAM );

    this.btnRAM64k = new JRadioButton(
                        settingsFrmResourceBundle.getString("button.ram.64k"),
                        ramSize == 0x10000 );
    grpRAM.add( this.btnRAM64k );
    this.btnRAM64k.addActionListener( this );
    gbcRAM.insets.bottom = 5;
    gbcRAM.gridy++;
    
    panelRAM.add( this.btnRAM64k, gbcRAM );
  }


  private void buildRomPanel(JPanel panelROM)
  {
    GridBagConstraints gbcROM = new GridBagConstraints(
                                                0, 0,
                                                1, 1,
                                                0.0, 0.0,
                                                GridBagConstraints.WEST,
                                                GridBagConstraints.NONE,
                                                new Insets( 5, 5, 0, 5 ),
                                                0, 0 );

    panelROM.add( new JLabel( settingsFrmResourceBundle.getString("tabs.rom.embeddedROMIMages") ), gbcROM );

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
    
    panelROM.add( new JScrollPane( this.listROM ), gbcROM );

    JPanel panelROMBtn = new JPanel( new GridLayout( 2, 1, 5, 5 ) );
    gbcROM.fill         = GridBagConstraints.NONE;
    gbcROM.weightx      = 0.0;
    gbcROM.weighty      = 0.0;
    gbcROM.insets.right = 5;
    gbcROM.gridheight   = 1;
    gbcROM.gridx++;
    
    panelROM.add( panelROMBtn, gbcROM );

    this.btnROMAdd = new JButton( settingsFrmResourceBundle.getString("button.rom.add") );
    this.btnROMAdd.addActionListener( this );
    
    panelROMBtn.add( this.btnROMAdd );

    this.btnROMRemove = new JButton( settingsFrmResourceBundle.getString("button.rom.remove") );
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
                        settingsFrmResourceBundle.getString("button.rom.reloadOnReset"),
                        Main.getBooleanProperty(
                                        "org.sqar.virtualjtc.rom.reload_on_reset",
                                        false ) );
    this.btnReloadROMOnReset.setEnabled( !this.extROMs.isEmpty() );
    this.btnReloadROMOnReset.addActionListener( this );
    gbcROM.gridx = 0;
    gbcROM.gridy++;
    
    panelROM.add( this.btnReloadROMOnReset, gbcROM );
  }


  private void buildConfirmPanel(JPanel panelConfirm)
  {
    GridBagConstraints gbcConfirm = new GridBagConstraints(
                                                0, 0,
                                                1, 1,
                                                0.0, 0.0,
                                                GridBagConstraints.WEST,
                                                GridBagConstraints.NONE,
                                                new Insets( 5, 5, 0, 5 ),
                                                0, 0 );

    panelConfirm.add(
                new JLabel( settingsFrmResourceBundle.getString("tabs.confirm.listOfConfirmationsLabel") ),
                gbcConfirm );

    this.btnConfirmInit = new JCheckBox(
                settingsFrmResourceBundle.getString("button.confirm.init"),
                Main.getBooleanProperty( "org.sqar.virtualjtc.jtcemu.confirm.init", true ) );
    this.btnConfirmInit.addActionListener( this );
    gbcConfirm.insets.top  = 0;
    gbcConfirm.insets.left = 50;
    gbcConfirm.gridy++;
    
    panelConfirm.add( this.btnConfirmInit, gbcConfirm );

    this.btnConfirmReset = new JCheckBox(
                settingsFrmResourceBundle.getString("button.confirm.reset"),
                Main.getBooleanProperty( "org.sqar.virtualjtc.jtcemu.confirm.reset", true ) );
    this.btnConfirmReset.addActionListener( this );
    gbcConfirm.gridy++;
    
    panelConfirm.add( this.btnConfirmReset, gbcConfirm );

    this.btnConfirmQuit = new JCheckBox(
                settingsFrmResourceBundle.getString("button.confirm.quit"),
                Main.getBooleanProperty( "org.sqar.virtualjtc.jtcemu.confirm.quit", true ) );
    this.btnConfirmQuit.addActionListener( this );
    gbcConfirm.insets.bottom = 5;
    gbcConfirm.gridy++;
    
    panelConfirm.add( this.btnConfirmQuit, gbcConfirm );
  }


  private void buildLAFPanel(JPanel panelLAF)
  {
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
      
      panelLAF.add( btn, gbcLAF );
      
      gbcLAF.insets.top = 0;
      gbcLAF.gridy++;
    }
  }


  private void buildEtcPanel(JPanel panelEtc, TopFrm topFrm)
  {
    GridBagConstraints gbcEtc = new GridBagConstraints(
                                        0, 0,
                                        GridBagConstraints.REMAINDER, 1,
                                        0.0, 0.0,
                                        GridBagConstraints.WEST,
                                        GridBagConstraints.NONE,
                                        new Insets( 5, 5, 0, 5 ),
                                        0, 0 );

    
    panelEtc.add( new JLabel( settingsFrmResourceBundle.getString("tabs.etc.selectFileChooser") ), gbcEtc );

    ButtonGroup grpFileSelect = new ButtonGroup();

    this.btnFileDlgEmu = new JRadioButton(
                settingsFrmResourceBundle.getString("button.etc.fileDlgEmu"),
                true );
    grpFileSelect.add( this.btnFileDlgEmu );
    this.btnFileDlgEmu.addActionListener( this );
    gbcEtc.insets.top  = 0;
    gbcEtc.insets.left = 50;
    gbcEtc.gridy++;
    
    panelEtc.add( this.btnFileDlgEmu, gbcEtc );

    this.btnFileDlgNative = new JRadioButton(
                settingsFrmResourceBundle.getString("button.etc.fileDlgNative"),
                false );
    grpFileSelect.add( this.btnFileDlgNative );
    this.btnFileDlgNative.addActionListener( this );
    gbcEtc.insets.bottom = 5;
    gbcEtc.gridy++;
    
    panelEtc.add( this.btnFileDlgNative, gbcEtc );

    String s = Main.getProperty( "org.sqar.virtualjtc.jtcemu.filedialog" );
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
    
    
    
    panelEtc.add( new JLabel( settingsFrmResourceBundle.getString("tabs.etc.screen.border") ), gbcEtc );

    SpinnerModel sm = new SpinnerNumberModel( 0, 0, MAX_MARGIN, 1 );
    try {
      int margin = Main.getIntProperty( "org.sqar.virtualjtc.jtcemu.screen.margin", 20 );
      if( (margin >= 0) && (margin <= MAX_MARGIN) ) {
        sm.setValue(margin);
      }
    }
    catch( IllegalArgumentException ignored) {}

    this.spinnerMargin = new JSpinner( sm );
    this.spinnerMargin.addChangeListener( this );

    gbcEtc.anchor = GridBagConstraints.WEST;
    gbcEtc.gridx++;
    panelEtc.add( this.spinnerMargin, gbcEtc );

    gbcEtc.gridx++;
    panelEtc.add( new JLabel( settingsFrmResourceBundle.getString("tabs.etc.screen.pixel") ), gbcEtc );

    gbcEtc.anchor = GridBagConstraints.EAST;
    gbcEtc.gridx  = 0;
    gbcEtc.gridy++;
    
    
    
    panelEtc.add( new JLabel( settingsFrmResourceBundle.getString("tabs.etc.screen.refresh") ), gbcEtc );

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
    panelEtc.add( this.comboScreenRefresh, gbcEtc );

    gbcEtc.gridx++;
    panelEtc.add( new JLabel( "ms" ), gbcEtc );

    if( this.propsFile != null ) {
      gbcEtc.insets.bottom = 0;
      gbcEtc.gridx         = 0;
      gbcEtc.gridy++;
      
      panelEtc.add( new JLabel( settingsFrmResourceBundle.getString("tabs.etc.propsFile.label") ), gbcEtc );

      this.fldPropsFile = new JTextField();
      this.fldPropsFile.setEditable( false );
      this.fldPropsFile.setText( this.propsFile.getAbsolutePath() );
      gbcEtc.fill          = GridBagConstraints.HORIZONTAL;
      gbcEtc.weightx       = 1.0;
      gbcEtc.insets.top    = 0;
      gbcEtc.insets.bottom = 5;
      gbcEtc.gridy++;
      
      panelEtc.add( this.fldPropsFile, gbcEtc );

      this.btnDeletePropsFile = new JButton( settingsFrmResourceBundle.getString("button.etc.deletePropsFile") );
      if( !this.propsFile.exists() ) {
        this.btnDeletePropsFile.setEnabled( false );
      }
      this.btnDeletePropsFile.addActionListener( this );
      gbcEtc.fill      = GridBagConstraints.NONE;
      gbcEtc.weightx   = 0.0;
      gbcEtc.gridwidth = 2;
      gbcEtc.gridx++;
      
      panelEtc.add( this.btnDeletePropsFile, gbcEtc );
      
    } else {
      this.fldPropsFile       = null;
      this.btnDeletePropsFile = null;
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
        msg = settingsFrmResourceBundle.getString("doApply.es1988.ram1k.warning");
      }
      else if( (this.btnES23.isSelected() || this.btnES40.isSelected())
               && (this.btnRAM1k.isSelected() || this.btnRAM2k.isSelected()) )
      {
        msg = settingsFrmResourceBundle.getString("doApply.es40.ram2k.warning");
      }
      if( msg != null ) {
        if( JOptionPane.showConfirmDialog(
                this,
                msg + settingsFrmResourceBundle.getString("dialog.doApply.confirmFaultySettings.message"),
                settingsFrmResourceBundle.getString("dialog.doApply.confirmFaultySettings.title"),
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
                "org.sqar.virtualjtc.jtcemu.ram.size",
                String.format( "%dK", ramSize / 1024 ) );
      props.setProperty(
                "org.sqar.virtualjtc.jtcemu.confirm.init",
                Boolean.toString( this.btnConfirmInit.isSelected() ) );
      props.setProperty(
                "org.sqar.virtualjtc.jtcemu.confirm.reset",
                Boolean.toString( this.btnConfirmReset.isSelected() ) );
      props.setProperty(
                "org.sqar.virtualjtc.jtcemu.confirm.quit",
                Boolean.toString( this.btnConfirmQuit.isSelected() ) );
      Main.putProperties( props );

      // JTCSys aktualisieren
      this.jtcSys.setExtROMs( newExtROMs );
      this.jtcSys.settingsChanged();
      if( forceReset ) {
        this.jtcSys.fireReset();
      }

      // zum Schluss (nach JTCSys!) andere Fenster informieren
      Frame[] frames = getFrames();
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
                settingsFrmResourceBundle.getString("dialog.doDeletePropsFile.confirmResetToDefaults.message"),
                settingsFrmResourceBundle.getString("dialog.doDeletePropsFile.confirmResetToDefaults.title"),
                JOptionPane.YES_NO_OPTION ) == JOptionPane.YES_OPTION )
      {
        if( this.propsFile.delete() ) {
          if( this.btnDeletePropsFile != null ) {
            this.btnDeletePropsFile.setEnabled( false );
          }
        } else {
          JOptionPane.showMessageDialog(
                this,
                settingsFrmResourceBundle.getString("dialog.doDeletePropsFile.cantResetToDefaults.message"),
                settingsFrmResourceBundle.getString("dialog.doDeletePropsFile.cantResetToDefaults.title"),
                JOptionPane.ERROR_MESSAGE );
        }
      }
    }
  }


  private void doSave()
  {
    Properties props = Main.getProperties();
    if( (props != null) && (this.propsFile != null) ) {
      Frame[] frames = getFrames();
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
                                settingsFrmResourceBundle.getString("dialog.doROMAdd.fileOpenDialog.title"),
                                settingsFrmResourceBundle.getString("dialog.doROMAdd.fileOpenDialog.approveBtnText"),
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
        addr = GUIUtil.askHex4( this, settingsFrmResourceBundle.getString("dialog.doROMAdd.startAddress"), addr );
        if( addr != null ) {
          rom.setBegAddr( addr.intValue() );
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
                "org.sqar.virtualjtc.jtcemu.filedialog",
                this.btnFileDlgNative.isSelected() ? "native" : "org/sqar/virtualjtc/jtcemu");

    Object obj = this.spinnerMargin.getValue();
    props.setProperty(
                "org.sqar.virtualjtc.jtcemu.screen.margin",
                obj != null ? obj.toString() : "0" );

    obj = this.comboScreenRefresh.getSelectedItem();
    if( obj != null ) {
      String text = obj.toString();
      if( text != null ) {
        props.setProperty( "org.sqar.virtualjtc.jtcemu.screen.refresh.ms", text );
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
              props.setProperty( "org.sqar.virtualjtc.jtcemu.laf.classname", lafClassName );
            }
            catch( Exception ex ) {
              rv = false;
              if( this.panelLAF != null ) {
                this.tabbedPane.setSelectedComponent( this.panelLAF );
              }
              Main.showError(
                        this,
                        settingsFrmResourceBundle.getString("error.applyLAF.couldNotApplyLAF.message") );
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
      int begAddr = rom.getBegAddr();
      if( begAddr <= a ) {
        this.tabbedPane.setSelectedComponent( this.panelROM );
        Main.showError(
                this,
                String.format(
                        settingsFrmResourceBundle.getString("error.applyROM.romsOverlap.formatString"),
                        begAddr ) );
        rv = false;
        break;
      }
      props.setProperty(
                String.format( "org.sqar.virtualjtc.rom.%d.address", n ),
                String.format( "%04X", begAddr ) );
      props.setProperty(
                String.format( "org.sqar.virtualjtc.rom.%d.file", n ),
                rom.getFile().getPath() );
      a = rom.getEndAddr();
    }
    props.setProperty( "org.sqar.virtualjtc.rom.count", Integer.toString( n ) );
    props.setProperty(
                "org.sqar.virtualjtc.rom.reload_on_reset",
                Boolean.toString( this.btnReloadROMOnReset.isSelected() ) );
    return rv;
  }


  private JTCSys.OSType applySys( Properties props )
  {
    JTCSys.OSType rv = JTCSys.OSType.OS2K;
    if( this.btnES40.isSelected() ) {
      props.setProperty( "org.sqar.virtualjtc.jtcemu.os", "ES4.0" );
      rv = JTCSys.OSType.ES40;
    } else if( this.btnES23.isSelected() ) {
      props.setProperty( "org.sqar.virtualjtc.jtcemu.os", "ES2.3" );
      rv = JTCSys.OSType.ES23;
    } else if( this.btnES1988.isSelected() ) {
      props.setProperty( "org.sqar.virtualjtc.jtcemu.os", "ES1988" );
      rv = JTCSys.OSType.ES1988;
    } else {
      props.setProperty( "org.sqar.virtualjtc.jtcemu.os", "2K" );
    }
    props.setProperty(
                "org.sqar.virtualjtc.jtcemu.screen.active.always",
                Boolean.toString( this.btnAlwaysScreenActive.isSelected() ) );
    props.setProperty(
                "org.sqar.virtualjtc.jtcemu.keyboard.emulate_syscall",
                Boolean.toString( this.btnEmuKeyboardSyscall.isSelected() ) );
    props.setProperty(
                "org.sqar.virtualjtc.z8.emulate_registers_80toEF",
                Boolean.toString( this.btnEmuReg80ToEF.isSelected() ) );
    return rv;
  }


  private void informLAFChanged()
  {
    Frame[] frames = getFrames();
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

