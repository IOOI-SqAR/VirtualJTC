/*
 * (c) 2007-2010 Jens Mueller
 * (c) 2017 Lars Sonchocky-Helldorf
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Debugger
 */

package org.sqar.virtualjtc.jtcemu.tools;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.Timer;

import javax.swing.*;
import javax.swing.event.*;
import org.sqar.virtualjtc.jtcemu.base.*;
import org.sqar.virtualjtc.jtcemu.Main;
import org.sqar.virtualjtc.z8.*;
import org.sqar.virtualjtc.z8.Z8.RunMode;


public class DebugFrm extends BaseFrm
                        implements
                                ActionListener,
                                ChangeListener,
                                ListSelectionListener,
                                MouseListener,
                                Z8Debugger
{
  private static final Locale locale = Locale.getDefault();
  private static final ResourceBundle debugFrmResourceBundle = ResourceBundle.getBundle("DebugFrm", locale);

  private static final int DEFAULT_AUTOSTEP_FREQUENCY = 2000;

  private static DebugFrm instance = null;
  
  private static int SPL   = 0xFF;
  private static int SPH   = 0xFE;
  private static int FLAGS = 0xFC;
  private static int P01N  = 0xF8;

  private Z8             z8;
  private Z8Memory       memory;
  private Z8Reassembler  reassembler;
  private Z8Breakpoint[] breakpoints;
  private boolean        editMode;
  private JMenuItem      mnuClose;
  private JMenuItem      mnuStop;
  private JMenuItem      mnuStepOver;
  private JMenuItem      mnuStepInto;
  private JMenuItem      mnuRunToRET;
  private JMenuItem      mnuRun;
  private JMenuItem      mnuAddBreakpoint;
  private JMenuItem      mnuRemoveBreakpoint;
  private JMenuItem      mnuRemoveAllBreakpoints;
  private JMenuItem      mnuEnableBreakpoint;
  private JMenuItem      mnuDisableBreakpoint;
  private JMenuItem      mnuEnableAllBreakpoints;
  private JMenuItem      mnuDisableAllBreakpoints;
  private JMenuItem      mnuHelpContent;
  private JPopupMenu     mnuPopup;
  private JMenuItem      popupAddBreakpoint;
  private JMenuItem      popupRemoveBreakpoint;
  private JMenuItem      popupRemoveAllBreakpoints;
  private JMenuItem      popupEnableBreakpoint;
  private JMenuItem      popupDisableBreakpoint;
  private JMenuItem      popupEnableAllBreakpoints;
  private JMenuItem      popupDisableAllBreakpoints;
  private JButton        btnStop;
  private JButton        btnStepInto;
  private JButton        btnStepOver;
  private JButton        btnRun;
  private JButton        btnRunToRET;
  private JCheckBox      autoStepCheckBox;
  private JComboBox      autoStepModeComboBox;
  private JLabel         autoStepLabel;
  private JTextField     autoStepFrequency;
  private JLabel         autoStepFrequencyUnitLabel;
  private HexFld[]       grp15Flds;
  private HexFld[]       regFlds;
  private HexFld         fldPC;
  private HexFld         fldRegFLAGS;
  private JCheckBox      btnFlagC;
  private JCheckBox      btnFlagZ;
  private JCheckBox      btnFlagS;
  private JCheckBox      btnFlagV;
  private JTextField     fldStackMode;
  private JTextField     fldStackValues;
  private JTextArea      fldReass;
  private JList          listBreakpoint;
  private JLabel         labelStatus;

  private Timer          autoStepTimer;
  
  
  private enum AutoStepMode
  {
    STEP_OVER( debugFrmResourceBundle.getString("comboBox.autoStep.stepOver") ),
    STEP_INTO( debugFrmResourceBundle.getString("comboBox.autoStep.stepInto") ),
    RUN_TO_RET( debugFrmResourceBundle.getString("comboBox.autoStep.runToRET") ),
    RUN( debugFrmResourceBundle.getString("comboBox.autoStep.run") );

    private final String displayString;
    
    private AutoStepMode(String displayString) {
      this.displayString = displayString;
    }
    
    @Override
    public String toString() {
      return displayString;
    }
  }
 

  public static boolean close()
  {
    return instance != null ? instance.doClose() : true;
  }


  public static void open( Z8 z8 )
  {
    if( instance != null ) {
      instance.setState( Frame.NORMAL );
      instance.toFront();
    } else {
      instance = new DebugFrm( z8 );
      instance.setVisible( true );
    }
  }


        /* --- ActionListener --- */

  @Override
  public void actionPerformed( ActionEvent e )
  {
    Object src = e.getSource();
    if( src != null ) {
      if( src == this.btnFlagC ) {
        this.fldRegFLAGS.setEditBit( 0x80, this.btnFlagC.isSelected() );
      } else if( src == this.btnFlagZ ) {
        this.fldRegFLAGS.setEditBit( 0x40, this.btnFlagZ.isSelected() );
      } else if( src == this.btnFlagS ) {
        this.fldRegFLAGS.setEditBit( 0x20, this.btnFlagS.isSelected() );
      } else if( src == this.btnFlagV ) {
        this.fldRegFLAGS.setEditBit( 0x10, this.btnFlagV.isSelected() );
      } else if( (src == this.mnuStop) || (src == this.btnStop) ) {
        setDebugAction( Z8.DebugAction.STOP );
      } else if( (src == this.mnuStepOver) || (src == this.btnStepOver) ) {
        setDebugAction( Z8.DebugAction.STEP_OVER );
      } else if( (src == this.mnuStepInto) || (src == this.btnStepInto) ) {
        setDebugAction( Z8.DebugAction.STEP_INTO );
      } else if( (src == this.mnuRunToRET) || (src == this.btnRunToRET) ) {
        setDebugAction( Z8.DebugAction.RUN_TO_RET );
      } else if( (src == this.mnuRun) || (src == this.btnRun) ) {
        setDebugAction( Z8.DebugAction.RUN );
      } else if( (src == this.autoStepCheckBox) ) {
        toggleAutoStep(this.autoStepCheckBox.isSelected());
      } else if( (src == this.mnuRemoveBreakpoint)
                 || (src == this.popupRemoveBreakpoint) )
      {
        doRemoveBreakpoint();
      } else if( (src == this.mnuEnableBreakpoint)
                 || (src == this.popupEnableBreakpoint) )
      {
        doBreakpointEnabled( true );
      } else if( (src == this.mnuDisableBreakpoint)
                 || (src == this.popupDisableBreakpoint) )
      {
        doBreakpointEnabled( false );
      } else if( (src == this.mnuEnableAllBreakpoints)
                 || (src == this.popupEnableAllBreakpoints) )
      {
        doAllBreakpointsEnabled( true );
      } else if( (src == this.mnuDisableAllBreakpoints)
                 || (src == this.popupDisableAllBreakpoints) )
      {
        doAllBreakpointsEnabled( false );
      } else {
        GUIUtil.setWaitCursor( this, true );
        if( src == this.mnuClose ) {
          doClose();
        }
        else if( (src == this.mnuAddBreakpoint)
                 || (src == this.popupAddBreakpoint) )
        {
          doAddBreakpoint();
        }
        else if( (src == this.mnuRemoveAllBreakpoints)
                 || (src == this.popupRemoveAllBreakpoints) )
        {
          doRemoveAllBreakpoints();
        }
        else if( src == this.mnuHelpContent ) {
          HelpFrm.open( debugFrmResourceBundle.getString("help.path") );
        }
        GUIUtil.setWaitCursor( this, false );
      }
    }
  }


  /* --- ChangeListener --- */

  @Override
  public void stateChanged( ChangeEvent e )
  {
    Object src = e.getSource();
    if( src == this.fldRegFLAGS ) {
      updFlagCheckBoxes( this.fldRegFLAGS.isEditMode() );
    } else if( src == this.fldPC ) {
      updReassFld();
    }
  }


  /* --- ListSelectionListener --- */

  @Override
  public void valueChanged( ListSelectionEvent e )
  {
    if( e.getSource() == this.listBreakpoint )
      updActionFields();
  }


  /* --- MouseListener --- */

  @Override
  public void mouseClicked( MouseEvent e )
  {
    if( (e.getComponent() == this.listBreakpoint) && e.isPopupTrigger() ) {
      showBreakpointPopup( e );
    }
  }


  @Override
  public void mouseEntered( MouseEvent e )
  {
    // leer
  }


  @Override
  public void mouseExited( MouseEvent e )
  {
    // leer
  }


  @Override
  public void mousePressed( MouseEvent e )
  {
    if( (e.getComponent() == this.listBreakpoint) && e.isPopupTrigger() ) {
      showBreakpointPopup( e );
    }
  }


  @Override
  public void mouseReleased( MouseEvent e )
  {
    if( (e.getComponent() == this.listBreakpoint) && e.isPopupTrigger() ) {
      showBreakpointPopup( e );
    }
  }


        /* --- Z8Debugger --- */

  @Override
  public void z8DebugStatusChanged( Z8 z8 )
  {
    if( z8 == this.z8 ) {
      final Z8.RunMode runMode = z8.getRunMode();
      EventQueue.invokeLater(
                new Runnable()
                {
                  public void run()
                  {
                    debugStatusChanged( runMode );
                  }
                } );
    }
  }


        /* --- ueberschriebene Methoden --- */

  @Override
  protected boolean doClose()
  {
    boolean state = true;
    if( !this.z8.wasQuitFired()
        && (this.z8.getRunMode() == Z8.RunMode.DEBUG_STOP) )
    {
      if( JOptionPane.showConfirmDialog(
                this,
                debugFrmResourceBundle.getString("dialog.confirmContinue.message"),
                debugFrmResourceBundle.getString("dialog.confirmContinue.title"),
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE ) != JOptionPane.YES_OPTION )
      {
        state = false;
      }
    }
    if( state ) {
      this.z8.setDebugger( null );
      this.z8.setDebugAction( null );
      this.z8.setBreakpoints( null );
      
      if (this.autoStepTimer != null) {
        this.autoStepTimer.cancel();
      }
      
      state = super.doClose();
    }
    if( state ) {
      instance = null;
    }
    return state;
  }


  @Override
  public void lafChanged()
  {
    SwingUtilities.updateComponentTreeUI( this.mnuPopup );
    pack();
  }


        /* --- private Konstruktoren und Methoden --- */

  private DebugFrm( Z8 z8 )
  {
    setTitle( debugFrmResourceBundle.getString("window.title") );
    this.z8          = z8;
    this.memory      = z8.getMemory();
    this.reassembler = new Z8Reassembler( z8.getMemory() );
    this.breakpoints = new Z8Breakpoint[ 0 ];
    this.editMode    = false;
    this.grp15Flds   = new HexFld[ 16 ];
    this.regFlds     = new HexFld[
                                Main.getBooleanProperty(
                                        "org.sqar.virtualjtc.z8.emulate_registers_80toEF",
                                        false ) ? 0xF0 : 0x80 ];


    // Menu
    JMenuBar mnuBar = new JMenuBar();
    setJMenuBar( mnuBar );


    // Menu Datei
    JMenu mnuFile = new JMenu( debugFrmResourceBundle.getString("menu.file") );
    mnuFile.setMnemonic( 'D' );
    mnuBar.add( mnuFile );

    this.mnuClose = createJMenuItem( debugFrmResourceBundle.getString("menuItem.close") );
    mnuFile.add( this.mnuClose );


    // Menu Debuggen
    JMenu mnuDebug = new JMenu( debugFrmResourceBundle.getString("menu.debug") );
    mnuDebug.setMnemonic( 'b' );
    mnuBar.add( mnuDebug );

    this.mnuStop = createJMenuItem( debugFrmResourceBundle.getString("menuItem.stop"), KeyEvent.VK_F4, 0 );
    mnuDebug.add( this.mnuStop );

    this.mnuRun = createJMenuItem( debugFrmResourceBundle.getString("menuItem.run"), KeyEvent.VK_F5, 0 );
    this.mnuRun.setEnabled( false );
    mnuDebug.add( this.mnuRun );

    this.mnuStepOver = createJMenuItem( debugFrmResourceBundle.getString("menuItem.stepOver"), KeyEvent.VK_F6, 0 );
    this.mnuStepOver.setEnabled( false );
    mnuDebug.add( this.mnuStepOver );

    this.mnuStepInto = createJMenuItem( debugFrmResourceBundle.getString("menuItem.stepInto"), KeyEvent.VK_F7, 0 );
    this.mnuStepInto.setEnabled( false );
    mnuDebug.add( this.mnuStepInto );

    this.mnuRunToRET = createJMenuItem( debugFrmResourceBundle.getString("menuItem.runToRET"), KeyEvent.VK_F8, 0 );
    this.mnuRunToRET.setEnabled( false );
    mnuDebug.add( this.mnuRunToRET );
    mnuDebug.addSeparator();

    this.mnuAddBreakpoint = createJMenuItem( debugFrmResourceBundle.getString("menuItem.addBreakpoint"), KeyEvent.VK_F9, 0 );
    mnuDebug.add( this.mnuAddBreakpoint );

    this.mnuRemoveBreakpoint = createJMenuItem( debugFrmResourceBundle.getString("menuItem.removeBreakpoint"), KeyEvent.VK_DELETE, 0 );
    mnuDebug.add( this.mnuRemoveBreakpoint );

    this.mnuRemoveAllBreakpoints = createJMenuItem( debugFrmResourceBundle.getString("menuItem.removeAllBreakpoints") );
    mnuDebug.add( this.mnuRemoveAllBreakpoints );
    mnuDebug.addSeparator();

    this.mnuEnableBreakpoint = createJMenuItem( debugFrmResourceBundle.getString("menuItem.enableBreakpoint") );
    mnuDebug.add( this.mnuEnableBreakpoint );

    this.mnuDisableBreakpoint = createJMenuItem( debugFrmResourceBundle.getString("menuItem.disableBreakpoint") );
    mnuDebug.add( this.mnuDisableBreakpoint );

    this.mnuEnableAllBreakpoints = createJMenuItem( debugFrmResourceBundle.getString("menuItem.enableAllBreakpoints") );
    mnuDebug.add( this.mnuEnableAllBreakpoints );

    this.mnuDisableAllBreakpoints = createJMenuItem( debugFrmResourceBundle.getString("menuItem.disableAllBreakpoints") );
    mnuDebug.add( this.mnuDisableAllBreakpoints );


    // Menu Hilfe
    JMenu mnuHelp = new JMenu( debugFrmResourceBundle.getString("menu.help") );
    mnuBar.add( mnuHelp );

    this.mnuHelpContent = new JMenuItem( debugFrmResourceBundle.getString("menuItem.helpContent") );
    this.mnuHelpContent.addActionListener( this );
    mnuHelp.add( this.mnuHelpContent );


    // PopupMenu
    this.mnuPopup = new JPopupMenu();

    this.popupAddBreakpoint = createJMenuItem( debugFrmResourceBundle.getString("menuItem.addBreakpoint") );
    this.mnuPopup.add( this.popupAddBreakpoint );

    this.popupRemoveBreakpoint = createJMenuItem( debugFrmResourceBundle.getString("menuItem.removeBreakpoint") );
    this.popupRemoveBreakpoint.setEnabled( false );
    this.mnuPopup.add( this.popupRemoveBreakpoint );

    this.popupRemoveAllBreakpoints = createJMenuItem( debugFrmResourceBundle.getString("menuItem.removeAllBreakpoints") );
    this.popupRemoveAllBreakpoints.setEnabled( false );
    this.mnuPopup.add( this.popupRemoveAllBreakpoints );
    this.mnuPopup.addSeparator();

    this.popupEnableBreakpoint = createJMenuItem( debugFrmResourceBundle.getString("menuItem.enableBreakpoint") );
    this.popupEnableBreakpoint.setEnabled( false );
    this.mnuPopup.add( this.popupEnableBreakpoint );

    this.popupDisableBreakpoint = createJMenuItem( debugFrmResourceBundle.getString("menuItem.disableBreakpoint") );
    this.popupDisableBreakpoint.setEnabled( false );
    this.mnuPopup.add( this.popupDisableBreakpoint );

    this.popupEnableAllBreakpoints = createJMenuItem( debugFrmResourceBundle.getString("menuItem.enableAllBreakpoints") );
    this.popupEnableAllBreakpoints.setEnabled( false );
    this.mnuPopup.add( this.popupEnableAllBreakpoints );

    this.popupDisableAllBreakpoints = createJMenuItem( debugFrmResourceBundle.getString("menuItem.disableAllBreakpoints") );
    this.popupDisableAllBreakpoints.setEnabled( false );
    this.mnuPopup.add( this.popupDisableAllBreakpoints );


    // Fensterinhalt
    setLayout( new GridBagLayout() );

    GridBagConstraints gbc = new GridBagConstraints(
                                        0, 0,
                                        1, 1,
                                        0.0, 0.0,
                                        GridBagConstraints.WEST,
                                        GridBagConstraints.NONE,
                                        new Insets( 5, 5, 5, 5 ),
                                        0, 0 );


    // Werkzeugleiste
    JToolBar toolBar = new JToolBar();
    toolBar.setFloatable( false );
    toolBar.setBorderPainted( false );
    toolBar.setOrientation( JToolBar.HORIZONTAL );
    toolBar.setRollover( true );
    add( toolBar, gbc );

    this.btnRun = GUIUtil.createImageButton(
                        this,
                        "/images/debug/run.png",
                        debugFrmResourceBundle.getString("button.run") );
    this.btnRun.setEnabled( false );
    toolBar.add( this.btnRun );

    this.btnStop = GUIUtil.createImageButton(
                        this,
                        "/images/debug/stop.png",
                        debugFrmResourceBundle.getString("button.stop") );
    toolBar.add( this.btnStop );

    this.btnStepOver = GUIUtil.createImageButton(
                        this,
                        "/images/debug/step_over.png",
                        debugFrmResourceBundle.getString("button.stepOver") );
    this.btnStepOver.setEnabled( false );
    toolBar.add( this.btnStepOver );

    this.btnStepInto = GUIUtil.createImageButton(
                        this,
                        "/images/debug/step_into.png",
                        debugFrmResourceBundle.getString("button.stepInto") );
    this.btnStepInto.setEnabled( false );
    toolBar.add( this.btnStepInto );

    this.btnRunToRET = GUIUtil.createImageButton(
                        this,
                        "/images/debug/step_up.png",
                        debugFrmResourceBundle.getString("button.runToRET") );
    this.btnRunToRET.setEnabled( false );
    toolBar.add( this.btnRunToRET );
    
    this.autoStepCheckBox = new JCheckBox( debugFrmResourceBundle.getString("checkBox.autoStep") );
    this.autoStepCheckBox.addActionListener( this );
    this.autoStepCheckBox.setEnabled( false );
    toolBar.add( this.autoStepCheckBox );
    
    this.autoStepModeComboBox = new JComboBox( new DefaultComboBoxModel(AutoStepMode.values()) );
    toolBar.add( this.autoStepModeComboBox );
    
    this.autoStepLabel = new JLabel( debugFrmResourceBundle.getString("label.autoStep") );
    toolBar.add( this.autoStepLabel );
    
    this.autoStepFrequency = new JTextField(5);
    this.autoStepFrequency.setText(Integer.toString(DEFAULT_AUTOSTEP_FREQUENCY));
    toolBar.add( this.autoStepFrequency );
    
    this.autoStepFrequencyUnitLabel = new JLabel( debugFrmResourceBundle.getString("label.autoStep.frequencyUnit") );
    toolBar.add( this.autoStepFrequencyUnitLabel );

    // Bereich Register
    JPanel panelReg = new JPanel( new GridBagLayout() );
    panelReg.setBorder( BorderFactory.createTitledBorder( debugFrmResourceBundle.getString("titledBorder.registers") ) );
    gbc.gridheight = 2;
    gbc.gridy++;
    add( panelReg, gbc );

    GridBagConstraints gbcReg = new GridBagConstraints(
                                        1, 0,
                                        1, 1,
                                        0.0, 0.0,
                                        GridBagConstraints.CENTER,
                                        GridBagConstraints.NONE,
                                        new Insets( 5, 2, 2, 2 ),
                                        0, 0 );

    panelReg.add( new JLabel( "SIO" ), gbcReg );
    gbcReg.gridx++;
    panelReg.add( new JLabel( "TMR" ), gbcReg );
    gbcReg.gridx++;
    panelReg.add( new JLabel( "T1" ), gbcReg );
    gbcReg.gridx++;
    panelReg.add( new JLabel( "PRE1" ), gbcReg );
    gbcReg.gridx++;
    panelReg.add( new JLabel( "T0" ), gbcReg );
    gbcReg.gridx++;
    panelReg.add( new JLabel( "PRE0" ), gbcReg );
    gbcReg.gridx++;
    panelReg.add( new JLabel( "P2M" ), gbcReg );
    gbcReg.gridx++;
    panelReg.add( new JLabel( "P3M" ), gbcReg );
    gbcReg.gridx++;
    panelReg.add( new JLabel( "P01M" ), gbcReg );
    gbcReg.gridx++;
    panelReg.add( new JLabel( "IPR" ), gbcReg );
    gbcReg.gridx++;
    panelReg.add( new JLabel( "IRQ" ), gbcReg );
    gbcReg.gridx++;
    panelReg.add( new JLabel( "IMR" ), gbcReg );
    gbcReg.gridx++;
    panelReg.add( new JLabel( "FLG" ), gbcReg );
    gbcReg.gridx++;
    panelReg.add( new JLabel( "RP" ), gbcReg );
    gbcReg.gridx++;
    panelReg.add( new JLabel( "SPH" ), gbcReg );
    gbcReg.insets.right = 5;
    gbcReg.gridx++;
    panelReg.add( new JLabel( "SPL" ), gbcReg );

    gbcReg.insets.top   = 2;
    gbcReg.insets.right = 2;
    gbcReg.gridx        = 0;
    gbcReg.gridy++;
    panelReg.add( new JLabel( "F" ), gbcReg );

    for( int i = 0; i < this.grp15Flds.length; i++ ) {
      gbcReg.gridx++;
      HexFld fld = new HexFld( 2 );
      this.grp15Flds[ i ] = fld;
      if( i == 15 ) {
        gbcReg.insets.right = 5;
      }
      panelReg.add( fld.getComponent(), gbcReg );
    }
    this.grp15Flds[ 0 ].setReadOnly( true );        // SIO
    this.grp15Flds[ 2 ].setReadOnly( true );        // T1
    this.grp15Flds[ 3 ].setReadOnly( true );        // PRE1
    this.grp15Flds[ 4 ].setReadOnly( true );        // T0
    this.grp15Flds[ 5 ].setReadOnly( true );        // PRE0

    gbcReg.insets.top   = 5;
    gbcReg.insets.right = 2;
    gbcReg.gridx        = 0;
    gbcReg.gridy++;
    for( int i = 0; i < 16; i++ ) {
      gbcReg.gridx++;
      panelReg.add( new JLabel( "R" + String.valueOf( i ) ), gbcReg );
    }

    gbcReg.insets.top = 2;
    int reg        = this.regFlds.length - 16;
    while( (reg >= 0) && (reg < this.regFlds.length) ) {
      gbcReg.insets.right = 2;
      if( reg == 0 ) {
        gbcReg.insets.bottom = 5;
      }
      gbcReg.gridx = 0;
      gbcReg.gridy++;
      panelReg.add(
                new JLabel( Integer.toHexString( reg >> 4 ).toUpperCase() ),
                gbcReg );
      for( int i = 0; i < 16; i++ ) {
        gbcReg.gridx++;
        if( (reg == 0) && (i < 2) ) {
          this.regFlds[ reg + i ] = null;
        } else {
          HexFld fld = new HexFld( 2 );
          this.regFlds[ reg + i ] = fld;
          panelReg.add( fld.getComponent(), gbcReg );
        }
      }
      reg -= 0x10;
    }
    this.regFlds[ 2 ].setReadOnly( true );
    this.regFlds[ 3 ].setReadOnly( true );

    gbcReg.insets.bottom = 5;
    gbcReg.gridx         = 0;
    gbcReg.gridy++;
    for( int i = 0; i < 16; i++ ) {
      gbcReg.gridx++;
      panelReg.add( new JLabel( String.format( "%1X", i ) ), gbcReg );
    }

    this.fldRegFLAGS = this.grp15Flds[ FLAGS - 0xF0 ];


    // Bereich Flags
    JPanel panelFlags = new JPanel( new GridBagLayout() );
    panelFlags.setBorder( BorderFactory.createTitledBorder( debugFrmResourceBundle.getString("titledBorder.flags") ) );
    gbc.fill       = GridBagConstraints.HORIZONTAL;
    gbc.weightx    = 1.0;
    gbc.gridheight = 1;
    gbc.gridx++;
    add( panelFlags, gbc );

    GridBagConstraints gbcFlags = new GridBagConstraints(
                                        1, 0,
                                        1, 1,
                                        0.0, 0.0,
                                        GridBagConstraints.WEST,
                                        GridBagConstraints.NONE,
                                        new Insets( 5, 5, 0, 5 ),
                                        0, 0 );

    this.btnFlagC = new JCheckBox( "Carry" );
    this.btnFlagC.addActionListener( this );
    panelFlags.add( this.btnFlagC, gbcFlags );

    this.btnFlagZ = new JCheckBox( "Zero" );
    this.btnFlagZ.addActionListener( this );
    gbcFlags.insets.top = 0;
    gbcFlags.gridy++;
    panelFlags.add( this.btnFlagZ, gbcFlags );

    this.btnFlagS = new JCheckBox( "Sign" );
    this.btnFlagS.addActionListener( this );
    gbcFlags.gridy++;
    panelFlags.add( this.btnFlagS, gbcFlags );

    this.btnFlagV = new JCheckBox( "Overflow" );
    this.btnFlagV.addActionListener( this );
    gbcFlags.insets.bottom = 5;
    gbcFlags.gridy++;
    panelFlags.add( this.btnFlagV, gbcFlags );

    updFlagCheckBoxes( false );
    this.fldRegFLAGS.addChangeListener( this );


    // Bereich Haltepunkte
    JPanel panelBreakpoint = new JPanel( new BorderLayout() );
    panelBreakpoint.setBorder( BorderFactory.createTitledBorder( debugFrmResourceBundle.getString("titledBorder.breakpoints") ) );
    gbc.fill       = GridBagConstraints.BOTH;
    gbc.weightx    = 1.0;
    gbc.weighty    = 1.0;
    gbc.gridheight = 2;
    gbc.gridy++;
    add( panelBreakpoint, gbc );

    this.listBreakpoint = new JList();
    this.listBreakpoint.setPrototypeCellValue( "XXXXXXXXXX" );
    this.listBreakpoint.setVisibleRowCount( 4 );
    this.listBreakpoint.addListSelectionListener( this );
    this.listBreakpoint.addMouseListener( this );
    panelBreakpoint.add(
                new JScrollPane( this.listBreakpoint ),
                BorderLayout.CENTER );


    // Bereich Programmausfuehrung
    JPanel panelPC = new JPanel( new GridBagLayout() );
    panelPC.setBorder( BorderFactory.createTitledBorder( debugFrmResourceBundle.getString("titledBorder.programExecution") ) );
    gbc.gridheight = 1;
    gbc.gridx      = 0;
    gbc.gridy++;
    add( panelPC, gbc );

    GridBagConstraints gbcPC = new GridBagConstraints(
                                        0, 0,
                                        1, 1,
                                        0.0, 0.0,
                                        GridBagConstraints.EAST,
                                        GridBagConstraints.NONE,
                                        new Insets( 5, 5, 2, 2 ),
                                        0, 0 );

    panelPC.add( new JLabel( "Stack:" ), gbcPC );
    gbcPC.insets.top = 2;
    gbcPC.gridy++;
    panelPC.add( new JLabel( "PC:" ), gbcPC );
    gbcPC.insets.bottom = 5;

    this.fldStackMode = new JTextField( 4 );
    this.fldStackMode.setEditable( false );
    gbcPC.anchor        = GridBagConstraints.WEST;
    gbcPC.insets.left   = 2;
    gbcPC.insets.top    = 5;
    gbcPC.insets.bottom = 2;
    gbcPC.gridy         = 0;
    gbcPC.gridx++;
    panelPC.add( this.fldStackMode, gbcPC );

    this.fldPC = new HexFld( 4 );
    gbcPC.insets.top = 2;
    gbcPC.gridy++;
    panelPC.add( this.fldPC.getComponent(), gbcPC );

    Font monospacedFont = new Font( "Monospaced", Font.PLAIN, 12 );

    this.fldStackValues = new JTextField();
    this.fldStackValues.setFont( monospacedFont );
    this.fldStackValues.setEditable( false );
    gbcPC.fill          = GridBagConstraints.HORIZONTAL;
    gbcPC.weightx       = 1.0;
    gbcPC.insets.top    = 5;
    gbcPC.insets.bottom = 2;
    gbcPC.insets.right  = 5;
    gbcPC.gridy         = 0;
    gbcPC.gridx++;
    panelPC.add( this.fldStackValues, gbcPC );

    this.fldReass = new JTextArea( 4, 0 );
    this.fldReass.setBorder( BorderFactory.createEtchedBorder() );
    this.fldReass.setFont( monospacedFont );
    this.fldReass.setEditable( false );
    gbcPC.fill          = GridBagConstraints.BOTH;
    gbcPC.weighty       = 1.0;
    gbcPC.insets.top    = 2;
    gbcPC.insets.bottom = 5;
    gbcPC.gridheight    = 3;
    gbcPC.gridy++;
    panelPC.add( this.fldReass, gbcPC );

    this.fldPC.addChangeListener( this );


    // Statuszeile
    gbc.fill       = GridBagConstraints.HORIZONTAL;
    gbc.weighty    = 0.0;
    gbc.gridwidth  = GridBagConstraints.REMAINDER;
    gbc.insets.top = 0;
    gbc.gridy++;
    add( new JSeparator(), gbc );

    this.labelStatus = new JLabel();
    gbc.weightx = 0.0;
    gbc.fill    = GridBagConstraints.NONE;
    gbc.gridy++;
    add( this.labelStatus, gbc );
    updStatusText( this.z8.getRunMode() );


    // sonstiges
    updBreakpointList( -1 );
    updActionFields();
    setResizable( true );
    if( !GUIUtil.applyWindowSettings( this ) ) {
      pack();
      setLocationByPlatform( true );
    }
    z8.setDebugger( this );
  }


  private JMenuItem createJMenuItem( String text )
  {
    JMenuItem item = new JMenuItem( text);
    item.addActionListener( this );
    return item;
  }


  private JMenuItem createJMenuItem( String text, int keyCode, int modifiers )
  {
    JMenuItem item = createJMenuItem( text);
    item.setAccelerator( KeyStroke.getKeyStroke( keyCode, modifiers ) );
    return item;
  }


  private void debugStatusChanged( Z8.RunMode runMode )
  {
    if( runMode != null ) {
      switch( runMode ) {
        case RUNNING:
          if( this.editMode ) {
            this.editMode = false;

            this.mnuStop.setEnabled( true );
            this.mnuStepOver.setEnabled( false );
            this.mnuStepInto.setEnabled( false );
            this.mnuRunToRET.setEnabled( false );
            this.mnuRun.setEnabled( false );

            this.btnStop.setEnabled( true );
            this.btnStepOver.setEnabled( false );
            this.btnStepInto.setEnabled( false );
            this.btnRunToRET.setEnabled( false );
            this.btnRun.setEnabled( false );
            
            this.autoStepCheckBox.setEnabled( false );

            disableRegEdit( this.grp15Flds, 0xF0, false );
            disableRegEdit( this.regFlds, 0, false );
            updFlagCheckBoxes( false );

            this.fldPC.clearValue();
            this.fldReass.setText( "" );
            this.fldStackMode.setText( "" );
            this.fldStackValues.setText( "" );
          }
          break;

        case DEBUG_STOP:
          this.editMode = true;

          this.mnuStop.setEnabled( false );
          this.mnuStepOver.setEnabled( true );
          this.mnuStepInto.setEnabled( true );
          this.mnuRunToRET.setEnabled( true );
          this.mnuRun.setEnabled( true );

          this.btnStop.setEnabled( false );
          this.btnStepOver.setEnabled( true );
          this.btnStepInto.setEnabled( true );
          this.btnRunToRET.setEnabled( true );
          this.btnRun.setEnabled( true );
          
          this.autoStepCheckBox.setEnabled( true );

          enableRegEdit( this.grp15Flds, 0xF0 );
          enableRegEdit( this.regFlds, 0 );
          updFlagCheckBoxes( true );

          StringBuilder buf = new StringBuilder( 128 );
          if( this.z8.isInternalStackEnabled() ) {
            this.fldStackMode.setText( debugFrmResourceBundle.getString("textfield.stackMode.internal") );
            int sp = this.grp15Flds[ SPL - 0xF0 ].getOrgValue();
            for( int i = 0; i < 16; i++ ) {
              if( i > 0 ) {
                buf.append( (char) '\u0020' );
              }
              buf.append( String.format(
                                "%02X",
                                this.z8.viewRegValue( sp++ ) ) );
            }
          } else {
            this.fldStackMode.setText( debugFrmResourceBundle.getString("textfield.stackMode.external") );
            int sp = (this.grp15Flds[ SPH - 0xF0 ].getOrgValue() << 8)
                                | this.grp15Flds[ SPL - 0xF0 ].getOrgValue();
            for( int i = 0; i < 16; i++ ) {
              if( i > 0 ) {
                buf.append( (char) '\u0020' );
              }
              buf.append( String.format(
                        "%02X",
                        this.memory.getMemByte( sp++, false ) ) );
            }
          }
          this.fldStackValues.setText( buf.toString() );

          int addr = this.z8.getPC();
          this.fldPC.setValue( addr );
          updReassFld();
          break;
      }
      updStatusText( runMode );
    }
  }


  private void doAddBreakpoint()
  {
    Integer addr = GUIUtil.askHex4( this, debugFrmResourceBundle.getString("dialog.askForBreakpoint.message"), null );
    if( addr != null ) {
      try {
        Z8Breakpoint bp = new Z8Breakpoint( addr.intValue() );
        if( this.breakpoints != null ) {
          int idx = Arrays.binarySearch( this.breakpoints, bp );
          if( idx < 0 ) {
            Z8Breakpoint[] a = new Z8Breakpoint[
                                        this.breakpoints.length + 1 ];
            idx     = -(idx + 1);
            int dst = 0;
            for( int i = 0; i < this.breakpoints.length; i++ ) {
              if( i == idx ) {
                if( dst < a.length ) {
                  a[ dst++ ] = bp;
                }
              }
              if( dst < a.length ) {
                a[ dst++ ] = this.breakpoints[ i ];
              }
            }
            if( (idx == this.breakpoints.length) && (dst < a.length) ) {
              a[ dst++ ] = bp;
            }
            this.breakpoints = a;
          }
          updBreakpointList( idx );
        } else {
          this.breakpoints      = new Z8Breakpoint[ 1 ];
          this.breakpoints[ 0 ] = bp;
          updBreakpointList( 0 );
        }
      }
      catch( ClassCastException ex ) {}
    }
  }


  private void doAllBreakpointsEnabled( boolean state )
  {
    if( this.breakpoints.length > 0 ) {
      for( int i = 0; i < this.breakpoints.length; i++ ) {
        this.breakpoints[ i ].setEnabled( state );
      }
      updBreakpointList( -1 );
      updActionFields();
    }
  }


  private void doBreakpointEnabled( boolean state )
  {
    Z8Breakpoint breakpoint = getSelectedBreakpoint();
    if( breakpoint != null ) {
      breakpoint.setEnabled( state );
      updBreakpointList( -1 );
      updActionFields();
    }
  }


  private void doRemoveAllBreakpoints()
  {
    if( JOptionPane.showConfirmDialog(
                this,
                debugFrmResourceBundle.getString("dialog.doRemoveAllBreakpoints.message"),
                debugFrmResourceBundle.getString("dialog.doRemoveAllBreakpoints.title"),
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE ) == JOptionPane.YES_OPTION )
    {
      this.breakpoints = new Z8Breakpoint[ 0 ];
      updBreakpointList( -1 );
      updActionFields();
    }
  }


  private void doRemoveBreakpoint()
  {
    int idx = this.listBreakpoint.getSelectedIndex();
    if( (idx >= 0) && (this.breakpoints.length > 0) ) {
      Z8Breakpoint[] a = new Z8Breakpoint[ this.breakpoints.length - 1 ];
      int   p = 0;
      for( int i = 0; (i < this.breakpoints.length) && (p < a.length); i++ ) {
        if( i != idx )
          a[ p++ ] = this.breakpoints[ i ];
      }
      this.breakpoints = a;
      updBreakpointList( -1 );
      updActionFields();
    }
  }


  private void disableRegEdit( HexFld[] fields, int addr, boolean useEdits )
  {
    for( int i = 0; i < fields.length; i++ ) {
      HexFld fld = fields[ i ];
      if( fld != null ) {
        if( useEdits && !fld.isReadOnly() && fld.wasDataChanged() ) {
          this.z8.setRegValue( addr + i, fld.getValue() );
        }
        fld.clearValue();
      }
    }
  }


  private Z8Breakpoint getSelectedBreakpoint()
  {
    Z8Breakpoint breakpoint = null;
    Object       obj        = this.listBreakpoint.getSelectedValue();
    if( obj != null ) {
      if( obj instanceof Z8Breakpoint )
        breakpoint = (Z8Breakpoint) obj;
    }
    return breakpoint;
  }


  private void enableRegEdit( HexFld[] fields, int addr )
  {
    for( int i = 0; i < fields.length; i++ ) {
      HexFld fld = fields[ i ];
      if( fld != null )
        fld.setValue( this.z8.viewRegValue( addr + i ) );
    }
  }


  private void setDebugAction( Z8.DebugAction debugAction )
  {
    this.mnuStop.setEnabled( true );
    this.mnuStepOver.setEnabled( false );
    this.mnuStepInto.setEnabled( false );
    this.mnuRunToRET.setEnabled( false );
    this.mnuRun.setEnabled( false );

    this.btnStop.setEnabled( true );
    this.btnStepOver.setEnabled( false );
    this.btnStepInto.setEnabled( false );
    this.btnRunToRET.setEnabled( false );
    this.btnRun.setEnabled( false );
    
    this.autoStepCheckBox.setEnabled( false );

    disableRegEdit( this.grp15Flds, 0xF0, true );
    disableRegEdit( this.regFlds, 0, true );
    if( this.fldPC.wasDataChanged() ) {
      this.z8.setPC( this.fldPC.getValue() );
    }
    this.fldPC.clearValue();
    this.fldReass.setText( "" );
    this.fldStackMode.setText( "" );
    this.fldStackValues.setText( "" );
    updFlagCheckBoxes( false );
    this.z8.setDebugAction( debugAction );
    updStatusText( Z8.RunMode.RUNNING );
  }


  private void toggleAutoStep(boolean selected)
  {
    if( selected ) {
      this.autoStepFrequency.setEnabled( false );
      
      int autoStepFrequency = DEFAULT_AUTOSTEP_FREQUENCY;
      
      try {
        autoStepFrequency = Integer.parseInt(this.autoStepFrequency.getText());
        
        if (autoStepFrequency <= 0) {
          autoStepFrequency = DEFAULT_AUTOSTEP_FREQUENCY;
        }
      } catch (NumberFormatException e) {
        // ingnore
      }
      
      TimerTask timerTask = new TimerTask()
      {
        
        @Override
        public void run()
        {
          final RunMode runMode = DebugFrm.this.z8.getRunMode();
          
          // only fire if we aren't currently running
          if (!Z8.RunMode.RUNNING.equals(runMode)) {
            final AutoStepMode autoStepMode = (AutoStepMode) DebugFrm.this.autoStepModeComboBox.getSelectedItem();
            
            switch (autoStepMode) {
              case STEP_OVER:
                EventQueue.invokeLater(new Runnable()
                {
                  
                  @Override
                  public void run()
                  {
                    setDebugAction( Z8.DebugAction.STEP_OVER );
                  }
                });
                break;
              case STEP_INTO:
                EventQueue.invokeLater(new Runnable()
                {
                  
                  @Override
                  public void run()
                  {
                    setDebugAction( Z8.DebugAction.STEP_INTO );
                  }
                });
                break;
              case RUN_TO_RET:
                EventQueue.invokeLater(new Runnable()
                {
                  
                  @Override
                  public void run()
                  {
                    setDebugAction( Z8.DebugAction.RUN_TO_RET );
                  }
                });
                break;
              case RUN:
                EventQueue.invokeLater(new Runnable()
                {
                  
                  @Override
                  public void run()
                  {
                    setDebugAction( Z8.DebugAction.RUN );
                  }
                });
                break;
            }
          }
        }
      };
      
      this.autoStepTimer = new Timer(true);
      this.autoStepTimer.scheduleAtFixedRate(timerTask, 0, autoStepFrequency);
    } else {
      if (this.autoStepTimer != null) {
        this.autoStepTimer.cancel();
      }
      
      this.autoStepFrequency.setEnabled( true );
    }
  }


  private void showBreakpointPopup( MouseEvent e )
  {
    this.mnuPopup.show( e.getComponent(), e.getX(), e.getY() );
    e.consume();
  }


  private void updActionFields()
  {
    boolean      enabled    = false;
    boolean      selected   = false;
    Z8Breakpoint breakpoint = getSelectedBreakpoint();
    if( breakpoint != null ) {
      enabled  = breakpoint.isEnabled();
      selected = true;
    }
    this.mnuRemoveBreakpoint.setEnabled( selected );
    this.mnuEnableBreakpoint.setEnabled( selected && !enabled );
    this.mnuDisableBreakpoint.setEnabled( selected && enabled );
    this.popupRemoveBreakpoint.setEnabled( selected );
    this.popupEnableBreakpoint.setEnabled( selected && !enabled );
    this.popupDisableBreakpoint.setEnabled( selected && enabled );
  }


  private void updBreakpointList( int idxToSelect )
  {
    int n = 0;
    if( this.breakpoints != null ) {
      n = this.breakpoints.length;
      this.listBreakpoint.setListData( this.breakpoints );
    } else {
      this.listBreakpoint.setListData( new Z8Breakpoint[ 0 ] );
    }
    if( (idxToSelect >= 0) && (idxToSelect < n) ) {
      this.listBreakpoint.setSelectedIndex( idxToSelect );
    }
    this.z8.setBreakpoints( this.breakpoints );

    boolean state = (n > 0);
    this.mnuRemoveAllBreakpoints.setEnabled( state );
    this.mnuEnableAllBreakpoints.setEnabled( state );
    this.mnuDisableAllBreakpoints.setEnabled( state );
    this.popupRemoveAllBreakpoints.setEnabled( state );
    this.popupEnableAllBreakpoints.setEnabled( state );
    this.popupDisableAllBreakpoints.setEnabled( state );
  }


  private void updFlagCheckBoxes( boolean state )
  {
    this.btnFlagC.setEnabled( state );
    this.btnFlagZ.setEnabled( state );
    this.btnFlagS.setEnabled( state );
    this.btnFlagV.setEnabled( state );

    int flags = 0;
    if( state ) {
      flags = this.fldRegFLAGS.getValue();
    }
    this.btnFlagC.setSelected( (flags & 0x80) != 0 );
    this.btnFlagZ.setSelected( (flags & 0x40) != 0 );
    this.btnFlagS.setSelected( (flags & 0x20) != 0 );
    this.btnFlagV.setSelected( (flags & 0x10) != 0 );
  }


  private void updReassFld()
  {
    int           addr = this.fldPC.getValue();
    StringBuilder buf  = new StringBuilder( 128 );
    for( int i = 0; i < 4; i++ ) {
      if( i > 0 ) {
        buf.append( (char) '\n' );
      }
      this.reassembler.reassemble( buf, addr );
      addr = this.reassembler.getNextAddr() & 0xFFFF;
    }
    this.fldReass.setText( buf.toString() );
  }


  private void updStatusText( Z8.RunMode runMode )
  {
    String statusText = debugFrmResourceBundle.getString("runMode.ready");
    if( runMode != null ) {
      switch( runMode ) {
        case RUNNING:
          statusText = debugFrmResourceBundle.getString("runMode.running");;
          break;

        case INST_HALT:
          statusText = debugFrmResourceBundle.getString("runMode.instHalt");;
          break;

        case INST_STOP:
          statusText = debugFrmResourceBundle.getString("runMode.instStop");;
          break;

        case DEBUG_STOP:
          statusText = debugFrmResourceBundle.getString("runMode.debugStop");;
          break;
      }
    }
    this.labelStatus.setText( statusText );
  }
}

