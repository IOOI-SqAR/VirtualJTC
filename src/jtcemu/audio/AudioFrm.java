/*
 * (c) 2007-2010 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Emulation des Anschlusses des Magnettonbandgeraetes
 */

package jtcemu.audio;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.*;
import javax.sound.sampled.*;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import jtcemu.Main;
import jtcemu.base.*;
import z8.Z8;


public class AudioFrm extends BaseFrm implements ActionListener
{
  private static final int[] sampleRates = {
                                96000, 48000, 44100, 32000,
                                22050, 16000, 11025, 8000 };

  private static AudioFrm instance = null;

  private Z8                                 z8;
  private JTCSys                             jtcSys;
  private Thread                             emuThread;
  private javax.swing.filechooser.FileFilter readFileFilter;
  private javax.swing.filechooser.FileFilter writeFileFilter;
  private File                               curFile;
  private File                               lastFile;
  private AudioFormat                        audioFmt;
  private AudioIO                            audioIO;
  private JRadioButton                       btnSoundOut;
  private JRadioButton                       btnDataOut;
  private JRadioButton                       btnDataIn;
  private JRadioButton                       btnFileOut;
  private JRadioButton                       btnFileIn;
  private JRadioButton                       btnFileLastIn;
  private JLabel                             labelSampleRate;
  private JComboBox                          comboSampleRate;
  private JLabel                             labelSpeed;
  private JSpinner                           spinnerSpeed;
  private JLabel                             labelThreshold;
  private JSlider                            sliderThreshold;
  private JLabel                             labelChannel;
  private JRadioButton                       btnChannel0;
  private JRadioButton                       btnChannel1;
  private JCheckBox                          btnMonitorPlay;
  private JLabel                             labelFileName;
  private JTextField                         fldFileName;
  private JLabel                             labelFormat;
  private JTextField                         fldFormat;
  private JLabel                             labelProgress;
  private JProgressBar                       progressBar;
  private JButton                            btnEnable;
  private JButton                            btnDisable;
  private JButton                            btnHelp;
  private JButton                            btnClose;


  public void doQuit()
  {
    doDisable();
    doClose();
  }


  public void fireDisable()
  {
    SwingUtilities.invokeLater(
                new Runnable()
                {
                  public void run()
                  {
                    doDisable();
                  }
                } );
  }


  public void fireProgressUpdate( final double value )
  {
    SwingUtilities.invokeLater(
                new Runnable()
                {
                  public void run()
                  {
                    updProgressBar( value );
                  }
                } );
  }


  public static void open( Z8 z8, JTCSys jtcSys, Thread emuThread )
  {
    if( instance != null ) {
      instance.setVisible( true );
      instance.setState( Frame.NORMAL );
      instance.toFront();
    } else {
      instance = new AudioFrm( z8, jtcSys, emuThread );
      instance.setVisible( true );
    }
  }


  public static void quit()
  {
    if( instance != null )
      instance.doQuit();
  }


        /* --- ActionListener --- */

  @Override
  public void actionPerformed( ActionEvent e )
  {
    GUIUtil.setWaitCursor( this, true );
    try {
      Object src = e.getSource();

      if( (src == this.btnSoundOut)
          || (src == this.btnDataOut)
          || (src == this.btnDataIn)
          || (src == this.btnFileOut)
          || (src == this.btnFileIn)
          || (src == this.btnFileLastIn) )
      {
        updOptFields();
      }
      else if( (src == this.btnChannel0) || (src == this.btnChannel1) ) {
        updChannel();
      }
      else if( src == this.btnEnable ) {
        doEnable();
      }
      else if( src == this.btnDisable ) {
        doDisable();
      }
      else if( src == this.btnHelp ) {
        HelpFrm.open( "/help/audio.htm" );
      }
      else if( src == this.btnClose ) {
        doClose();
      }
    }
    catch( Exception ex ) {
      Main.showError( this, ex );
    }
    GUIUtil.setWaitCursor( this, false );
  }


        /* --- ueberschriebene Methoden --- */

  @Override
  public void lafChanged()
  {
    pack();
  }


  @Override
  public void prepareSettingsToSave()
  {
    Object value = this.spinnerSpeed.getValue();
    if( value != null ) {
      Main.setProperty(
                "jtcemu.audio.speed_adjustment.percent",
                value.toString() );
    }
    Main.setProperty(
                "jtcemu.audio.threshold.percent",
                String.valueOf( this.sliderThreshold.getValue() ) );
    Main.setProperty(
                "jtcemu.audio.monitor.enabled",
                Boolean.toString( btnMonitorPlay.isSelected() ) );
  }


  @Override
  public void settingsChanged()
  {
    updOptFields();
  }


        /* --- private Konstruktoren und Methoden --- */

  private AudioFrm( Z8 z8, JTCSys jtcSys, Thread emuThread )
  {
    this.z8              = z8;
    this.jtcSys          = jtcSys;
    this.emuThread       = null;
    this.readFileFilter  = null;
    this.writeFileFilter = null;
    this.curFile         = null;
    this.lastFile        = null;
    this.audioFmt        = null;
    this.audioIO         = null;
    setTitle( "JTCEMU Audio/Kassette" );


    // Fensterinhalt
    setLayout( new GridBagLayout() );

    GridBagConstraints gbc = new GridBagConstraints(
                                                0, 0,
                                                1, 1,
                                                1.0, 0.0,
                                                GridBagConstraints.NORTHWEST,
                                                GridBagConstraints.HORIZONTAL,
                                                new Insets( 5, 5, 5, 5 ),
                                                0, 0 );


    // Bereich Funktion
    JPanel panelFct = new JPanel( new GridBagLayout() );
    panelFct.setBorder( BorderFactory.createTitledBorder( "Funktion" ) );
    add( panelFct, gbc );

    GridBagConstraints gbcFct = new GridBagConstraints(
                                                0, 0,
                                                1, 1,
                                                0.0, 0.0,
                                                GridBagConstraints.NORTHWEST,
                                                GridBagConstraints.NONE,
                                                new Insets( 5, 5, 0, 5 ),
                                                0, 0 );

    ButtonGroup grpFct = new ButtonGroup();

    this.btnSoundOut = new JRadioButton( "T\u00F6ne ausgeben", true );
    grpFct.add( this.btnSoundOut );
    this.btnSoundOut.addActionListener( this );
    panelFct.add( this.btnSoundOut, gbcFct );

    this.btnDataOut = new JRadioButton( "Daten am Audio-Ausgang ausgeben" );
    grpFct.add( this.btnDataOut );
    this.btnDataOut.addActionListener( this );
    gbcFct.insets.top = 0;
    gbcFct.gridy++;
    panelFct.add( this.btnDataOut, gbcFct );

    this.btnDataIn = new JRadioButton( "Daten vom Audio-Eingang lesen" );
    grpFct.add( this.btnDataIn );
    this.btnDataIn.addActionListener( this );
    gbcFct.gridy++;
    panelFct.add( this.btnDataIn, gbcFct );

    this.btnFileOut = new JRadioButton( "Sound-Datei speichern" );
    grpFct.add( this.btnFileOut );
    this.btnFileOut.addActionListener( this );
    gbcFct.gridy++;
    panelFct.add( this.btnFileOut, gbcFct );

    this.btnFileIn = new JRadioButton( "Sound-Datei lesen" );
    grpFct.add( this.btnFileIn );
    this.btnFileIn.addActionListener( this );
    gbcFct.gridy++;
    panelFct.add( this.btnFileIn, gbcFct );

    this.btnFileLastIn = new JRadioButton(
                                "Letzte Sound-Datei (noch einmal) lesen" );
    grpFct.add( this.btnFileLastIn );
    this.btnFileLastIn.addActionListener( this );
    gbcFct.insets.bottom = 5;
    gbcFct.gridy++;
    panelFct.add( this.btnFileLastIn, gbcFct );


    // Bereich Optionen
    JPanel panelOpt = new JPanel( new GridBagLayout() );
    panelOpt.setBorder( BorderFactory.createTitledBorder( "Optionen" ) );
    gbc.gridy++;
    add( panelOpt, gbc );

    GridBagConstraints gbcOpt = new GridBagConstraints(
                                                0, 0,
                                                1, 1,
                                                0.0, 0.0,
                                                GridBagConstraints.WEST,
                                                GridBagConstraints.NONE,
                                                new Insets( 5, 5, 5, 5 ),
                                                0, 0 );

    this.labelSampleRate = new JLabel( "Abtastrate (Hz):" );
    panelOpt.add( this.labelSampleRate, gbcOpt );

    this.comboSampleRate = new JComboBox();
    this.comboSampleRate.setEditable( false );
    this.comboSampleRate.addItem( "Standard" );
    for( int i = 0; i < this.sampleRates.length; i++ ) {
      this.comboSampleRate.addItem( String.valueOf( this.sampleRates[ i ] ) );
    }
    gbcOpt.gridwidth = GridBagConstraints.REMAINDER;
    gbcOpt.gridx++;
    panelOpt.add( this.comboSampleRate, gbcOpt );

    this.labelSpeed   = new JLabel( "Geschwindigkeit [%]:" );
    gbcOpt.insets.top = 5;
    gbcOpt.gridwidth  = 1;
    gbcOpt.gridx      = 0;
    gbcOpt.gridy++;
    panelOpt.add( this.labelSpeed, gbcOpt );

    Integer pValue = Main.getIntegerProperty(
                        "jtcemu.audio.speed_adjustment.percent" );
    this.spinnerSpeed = new JSpinner(
                                new SpinnerNumberModel(
                                        pValue != null ? pValue.intValue() : 0,
                                        -20,
                                        20,
                                        1 ) );
    gbcOpt.gridwidth  = 2;
    gbcOpt.gridx++;
    panelOpt.add( this.spinnerSpeed, gbcOpt );

    this.labelThreshold = new JLabel( "Schwellwert [%]:" );
    gbcOpt.gridx = 0;
    gbcOpt.gridy++;
    panelOpt.add( this.labelThreshold, gbcOpt );

    pValue = Main.getIntegerProperty(
                        "jtcemu.audio.threshold.percent" );
    this.sliderThreshold = new JSlider(
                                JSlider.HORIZONTAL,
                                0,
                                100,
                                pValue != null ? pValue.intValue() : 50 );
    this.sliderThreshold.setMajorTickSpacing( 50 );
    this.sliderThreshold.setMinorTickSpacing( 10 );
    this.sliderThreshold.setPaintLabels( true );
    this.sliderThreshold.setPaintTicks( true );
    this.sliderThreshold.setPaintTrack( true );
    this.sliderThreshold.setSnapToTicks( false );
    gbcOpt.fill      = GridBagConstraints.HORIZONTAL;
    gbcOpt.weightx   = 1.0;
    gbcOpt.gridwidth = GridBagConstraints.REMAINDER;
    gbcOpt.gridx++;
    panelOpt.add( this.sliderThreshold, gbcOpt );

    this.labelChannel = new JLabel( "Aktiver Kanal:" );
    gbcOpt.fill       = GridBagConstraints.NONE;
    gbcOpt.weightx    = 0.0;
    gbcOpt.gridwidth  = 1;
    gbcOpt.gridx      = 0;
    gbcOpt.gridy++;
    panelOpt.add( this.labelChannel, gbcOpt );

    ButtonGroup grpChannel = new ButtonGroup();

    this.btnChannel0 = new JRadioButton( "Links", true );
    grpChannel.add( this.btnChannel0 );
    this.btnChannel0.addActionListener( this );
    gbcOpt.gridx++;
    panelOpt.add( this.btnChannel0, gbcOpt );

    this.btnChannel1 = new JRadioButton( "Rechts", false );
    grpChannel.add( this.btnChannel1 );
    this.btnChannel1.addActionListener( this );
    gbcOpt.gridx++;
    panelOpt.add( this.btnChannel1, gbcOpt );

    this.btnMonitorPlay  = new JCheckBox( "Mith\u00F6ren", false );
    gbcOpt.insets.bottom = 5;
    gbcOpt.gridwidth     = GridBagConstraints.REMAINDER;
    gbcOpt.gridx         = 0;
    gbcOpt.gridy++;
    panelOpt.add( this.btnMonitorPlay, gbcOpt );

    String monitorText = Main.getProperty( "jtcemu.audio.monitor.enabled" );
    if( monitorText != null ) {
      this.btnMonitorPlay.setSelected( Boolean.parseBoolean( monitorText ) );
    }


    // Bereich Status
    JPanel panelStatus = new JPanel( new GridBagLayout() );
    panelStatus.setBorder( BorderFactory.createTitledBorder( "Status" ) );

    GridBagConstraints gbcStatus = new GridBagConstraints(
                                                0, 0,
                                                1, 1,
                                                0.0, 0.0,
                                                GridBagConstraints.EAST,
                                                GridBagConstraints.NONE,
                                                new Insets( 5, 5, 5, 5 ),
                                                0, 0 );

    this.labelFileName = new JLabel( "Datei:" );
    panelStatus.add( this.labelFileName, gbcStatus );

    this.labelFormat = new JLabel( "Format:" );
    gbcStatus.gridy++;
    panelStatus.add( this.labelFormat, gbcStatus );

    this.labelProgress = new JLabel( "Fortschritt:" );
    gbcStatus.gridy++;
    panelStatus.add( this.labelProgress, gbcStatus );

    this.fldFileName = new JTextField();
    this.fldFileName.setEditable( false );
    gbcStatus.anchor  = GridBagConstraints.WEST;
    gbcStatus.fill    = GridBagConstraints.HORIZONTAL;
    gbcStatus.weightx = 1.0;
    gbcStatus.gridy   = 0;
    gbcStatus.gridx++;
    panelStatus.add( this.fldFileName, gbcStatus );

    this.fldFormat = new JTextField();
    this.fldFormat.setEditable( false );
    gbcStatus.gridy++;
    panelStatus.add( this.fldFormat, gbcStatus );

    this.progressBar = new JProgressBar( JProgressBar.HORIZONTAL, 0, 100 );
    this.progressBar.setBorderPainted( true );
    this.progressBar.setStringPainted( false );
    this.progressBar.setValue( 0 );
    gbcStatus.gridy++;
    panelStatus.add( this.progressBar, gbcStatus );

    gbc.gridy++;
    add( panelStatus, gbc );


    // Knoepfe
    JPanel panelBtn = new JPanel( new GridLayout( 4, 1, 5, 5 ) );

    this.btnEnable = new JButton( "Aktivieren" );
    this.btnEnable.addActionListener( this );
    panelBtn.add( this.btnEnable );

    this.btnDisable = new JButton( "Deaktivieren" );
    this.btnDisable.addActionListener( this );
    panelBtn.add( this.btnDisable );

    this.btnHelp = new JButton( "Hilfe..." );
    this.btnHelp.addActionListener( this );
    panelBtn.add( this.btnHelp );

    this.btnClose = new JButton( "Schlie\u00DFen" );
    this.btnClose.addActionListener( this );
    panelBtn.add( this.btnClose );

    gbc.fill       = GridBagConstraints.NONE;
    gbc.gridheight = GridBagConstraints.REMAINDER;
    gbc.gridy      = 0;
    gbc.gridx++;
    add( panelBtn, gbc );


    // sonstiges
    pack();
    setResizable( false );
    if( !GUIUtil.applyWindowSettings( this ) ) {
      setLocationByPlatform( true );
    }
    setAudioState( false );
  }


  private static javax.swing.filechooser.FileFilter createFileFilter(
                                                String                  text,
                                                AudioFileFormat.Type... fmts )
  {
    javax.swing.filechooser.FileFilter rv = null;
    if( fmts != null ) {
      if( fmts.length > 0 ) {
        Collection<String> suffixes = new ArrayList<String>( fmts.length );
        for( int i = 0; i < fmts.length; i++ ) {
          String suffix = fmts[ i ].getExtension();
          if( suffix != null ) {
            if( suffix.length() > 0 )
              suffixes.add( suffix );
          }
        }
        if( !suffixes.isEmpty() ) {
          try {
            rv = new FileNameExtensionFilter(
                        text,
                        suffixes.toArray( new String[ suffixes.size() ] ) );
          }
          catch( ArrayStoreException ex ) {}
        }
      }
    }
    return rv;
  }


  private void doEnable()
  {
    float  speedF = 1F;
    Object speedV = this.spinnerSpeed.getValue();
    if( speedV != null ) {
      if( speedV instanceof Number )
        speedF += (((Number) speedV).floatValue() / 100F);
    }
    int cyclesPerSecond = (int) Math.round(
                        (float) this.z8.getCyclesPerSecond() * speedF );

    if( this.btnSoundOut.isSelected() ) {
      // keine Geschwindigkeitsanpassung!
      doEnableAudioOutLine( this.z8.getCyclesPerSecond(), false );
    }
    else if( this.btnDataOut.isSelected() ) {
      doEnableAudioOutLine( cyclesPerSecond, true );
    }
    else if( this.btnDataIn.isSelected() ) {
      doEnableAudioInLine( cyclesPerSecond );
    }
    else if( this.btnFileOut.isSelected() ) {
      doEnableAudioOutFile( cyclesPerSecond );
    }
    else if( this.btnFileIn.isSelected() ) {
      doEnableAudioInFile( cyclesPerSecond, null );
    }
    else if( this.btnFileLastIn.isSelected() ) {
      doEnableAudioInFile( cyclesPerSecond, this.lastFile );
    }
  }


  private void doEnableAudioInFile( int cyclesPerSecond, File file )
  {
    if( file == null ) {
      if( this.readFileFilter == null ) {
        this.readFileFilter = createFileFilter(
                                        "Audiodateien",
                                        AudioFileFormat.Type.AIFC,
                                        AudioFileFormat.Type.AIFF,
                                        AudioFileFormat.Type.AU,
                                        AudioFileFormat.Type.SND,
                                        AudioFileFormat.Type.WAVE );
      }
      file = FileDlg.showFileOpenDlg(
                                this,
                                "Sound-Datei \u00F6ffnen",
                                "\u00D6ffnen",
                                Main.getLastPathFile(),
                                this.readFileFilter );
    }
    if( file != null ) {
      stopAudio();
      boolean monitorPlay = this.btnMonitorPlay.isSelected();
      AudioIn audioIn     = new AudioInFile(
                                        this.z8,
                                        this,
                                        file,
                                        monitorPlay );
      this.audioFmt = audioIn.startAudio(
                                cyclesPerSecond,
                                getSampleRate(),
                                getThresholdValue() );
      this.audioIO  = audioIn;
      if( this.audioFmt != null ) {
        updChannel();
        this.jtcSys.setAudioIn( audioIn );
        this.curFile    = file;
        this.lastFile   = file;
        Main.setLastFile( file );
        setAudioState( true );
        if( audioIn.isMonitorPlayActive() != monitorPlay ) {
          showErrorNoMonitorPlay();
        }
      } else {
        showError( this.audioIO.getErrorText() );
      }
    }
  }


  private void doEnableAudioInLine( int cyclesPerSecond )
  {
    stopAudio();
    AudioIn audioIn = new AudioInLine( this.z8 );
    this.audioFmt   = audioIn.startAudio(
                                        cyclesPerSecond,
                                        getSampleRate(),
                                        getThresholdValue() );
    this.audioIO    = audioIn;
    if( this.audioFmt != null ) {
      updChannel();
      this.jtcSys.setAudioIn( audioIn );
      setAudioState( true );
      setEmuThreadPriority( Thread.MAX_PRIORITY );
    } else {
      showError( this.audioIO.getErrorText() );
    }
  }


  private void doEnableAudioOutFile( int cyclesPerSecond )
  {
    if( this.writeFileFilter == null ) {
      this.writeFileFilter = createFileFilter(
                                        "Unterst\u00FCtzte Audiodateien",
                                        AudioSystem.getAudioFileTypes() );
    }
    File file = FileDlg.showFileSaveDlg(
                                this,
                                "Sound-Datei speichern",
                                Main.getLastPathFile(),
                                this.writeFileFilter );
    if( file != null ) {
      AudioFileFormat.Type fileType = getAudioFileType( file );
      if( fileType != null ) {
        stopAudio();
        boolean  monitorPlay = this.btnMonitorPlay.isSelected();
        AudioOut audioOut    = new AudioOutFile(
                                        this.z8,
                                        this.jtcSys,
                                        this,
                                        file,
                                        fileType,
                                        monitorPlay );
        this.audioFmt = audioOut.startAudio(
                                        cyclesPerSecond,
                                        getSampleRate(),
                                        getThresholdValue() );
        this.audioIO  = audioOut;
        if( this.audioFmt != null ) {
          this.jtcSys.setAudioOut( audioOut, true );
          this.curFile  = file;
          this.lastFile = file;
          Main.setLastFile( file );
          setAudioState( true );
          if( audioOut.isMonitorPlayActive() != monitorPlay ) {
            showErrorNoMonitorPlay();
          }
        } else {
          showError( this.audioIO.getErrorText() );
        }
      }
    }
  }


  private void doEnableAudioOutLine(
                                int     cyclesPerSecond,
                                boolean forDataTransfer )
  {
    stopAudio();
    AudioOut audioOut = new AudioOutLine(
                                this.z8,
                                this.jtcSys,
                                this.btnDataOut.isSelected() );
    this.audioFmt = audioOut.startAudio(
                                cyclesPerSecond,
                                getSampleRate(),
                                getThresholdValue() );
    this.audioIO  = audioOut;
    if( this.audioFmt != null ) {
      this.jtcSys.setAudioOut( audioOut, forDataTransfer );
      setAudioState( true );
      setEmuThreadPriority( Thread.MAX_PRIORITY );
    } else {
      showError( this.audioIO.getErrorText() );
    }
  }


  private void doDisable()
  {
    stopAudio();
    this.curFile  = null;
    this.audioFmt = null;
    setAudioState( false );
    setEmuThreadPriority( Thread.NORM_PRIORITY );
  }


  private AudioFileFormat.Type getAudioFileType( File file )
  {
    Collection<AudioFileFormat.Type> types =
                                new ArrayList<AudioFileFormat.Type>();
    if( AudioSystem.isFileTypeSupported( AudioFileFormat.Type.AIFC ) ) {
      types.add( AudioFileFormat.Type.AIFC );
    }
    if( AudioSystem.isFileTypeSupported( AudioFileFormat.Type.AIFF ) ) {
      types.add( AudioFileFormat.Type.AIFF );
    }
    if( AudioSystem.isFileTypeSupported( AudioFileFormat.Type.AU ) ) {
      types.add( AudioFileFormat.Type.AU );
    }
    if( AudioSystem.isFileTypeSupported( AudioFileFormat.Type.SND ) ) {
      types.add( AudioFileFormat.Type.SND );
    }
    if( AudioSystem.isFileTypeSupported( AudioFileFormat.Type.WAVE ) ) {
      types.add( AudioFileFormat.Type.WAVE );
    }

    String fileName = file.getName();
    if( fileName != null ) {
      fileName = fileName.toUpperCase( Locale.ENGLISH );
      for( AudioFileFormat.Type fileType : types ) {
        String ext = fileType.getExtension();
        if( ext != null ) {
          ext = ext.toUpperCase();
          if( !ext.startsWith( "." ) ) {
            ext = "." + ext;
          }
          if( fileName.endsWith( ext ) )
            return fileType;
        }
      }
    }

    StringBuilder buf = new StringBuilder( 64 );
    buf.append( "Das Dateiformat wird nicht unterst\u00FCtzt." );
    if( !types.isEmpty() ) {
      buf.append( "\nM\u00F6gliche Dateiendungen sind:\n" );
      String delim = null;
      for( AudioFileFormat.Type fileType : types ) {
        String ext = fileType.getExtension();
        if( ext != null ) {
          if( delim != null ) {
            buf.append( delim );
          }
          buf.append( ext );
          delim = ", ";
        }
      }
    }
    Main.showError( this, buf.toString() );
    return null;
  }


  public static String getAudioFormatText( AudioFormat fmt )
  {
    StringBuilder buf = new StringBuilder( 64 );
    if( fmt != null ) {
      buf.append( (int) fmt.getSampleRate() );
      buf.append( " Hz, " );

      buf.append( fmt.getSampleSizeInBits() );
      buf.append( " Bit, " );

      int channels = fmt.getChannels();
      switch( channels ) {
        case 1:
          buf.append( "Mono" );
          break;
        case 2:
          buf.append( "Stereo" );
          break;
        default:
          buf.append( channels );
          buf.append( " Kan\u00E4le" );
          break;
      }
    }
    return buf.toString();
  }


  private int getSampleRate()
  {
    int i = this.comboSampleRate.getSelectedIndex() - 1;  // 0: automatisch
    return ((i >= 0) && (i < this.sampleRates.length)) ?
                                        this.sampleRates[ i ] : 0;
  }


  private float getThresholdValue()
  {
    return (float) this.sliderThreshold.getValue()
                        / (float) this.sliderThreshold.getMaximum();
  }


  private void setAudioState( boolean state )
  {
    if( state && (this.audioFmt != null) ) {
      this.labelFormat.setEnabled( true );
      this.fldFormat.setText( getAudioFormatText( this.audioFmt ) );
    } else {
      this.labelFormat.setEnabled( false );
      this.fldFormat.setText( "" );
    }

    if( state && (this.curFile != null) ) {
      this.labelFileName.setEnabled( true );
      this.fldFileName.setText( this.curFile.getPath() );
    } else {
      this.labelFileName.setEnabled( false );
      this.fldFileName.setText( "" );
    }

    boolean progressState = false;
    if( state ) {
      AudioIO audioIO = this.audioIO;
      if( audioIO != null )
        progressState = audioIO.isProgressUpdateEnabled();
    }
    if( progressState ) {
      this.labelProgress.setEnabled( true );
      this.progressBar.setEnabled( true );
    } else {
      this.progressBar.setValue( this.progressBar.getMinimum() );
      this.progressBar.setEnabled( false );
      this.labelProgress.setEnabled( false );
    }

    this.btnDisable.setEnabled( state );
    state = !state;
    this.btnEnable.setEnabled( state );
    this.btnSoundOut.setEnabled( state );
    this.btnDataOut.setEnabled( state );
    this.btnDataIn.setEnabled( state );
    this.btnFileOut.setEnabled( state );
    this.btnFileIn.setEnabled( state );
    this.btnFileLastIn.setEnabled( state && (this.lastFile != null) );
    updOptFields();
  }


  private void setEmuThreadPriority( int priority )
  {
    try {
      this.emuThread.setPriority( priority );
    }
    catch( Exception ex ) {}
  }


  private void showError( String errorText )
  {
    if( errorText == null ) {
      if( this.comboSampleRate.getSelectedIndex() > 0 ) {
        errorText = "Es kann kein Audiokanal mit den angegebenen"
                                + " Optionen ge\u00F6ffnet werden.";
      } else {
        errorText = "Es kann kein Audiokanal ge\u00F6ffnet werden.";
      }
    }
    Main.showError( this, errorText );
  }


  private void showErrorNoMonitorPlay()
  {
    Main.showError(
        this,
        "Das Mith\u00F6ren ist nicht m\u00F6glich,\n"
                + "da das \u00D6ffnen eines Audiokanals mit dem Format\n"
                + "der Sound-Datei fehlgeschlagen ist." );
  }


  private void stopAudio()
  {
    AudioIO audioIO = this.audioIO;
    this.audioIO    = null;
    this.jtcSys.setAudioIn( null );
    this.jtcSys.setAudioOut( null, false );
    if( audioIO != null ) {
      audioIO.stopAudio();
      String errorText = audioIO.getErrorText();
      if( errorText != null )
        Main.showError( this, errorText );
    }
  }


  private void updChannel()
  {
    AudioIO     audioIO  = this.audioIO;
    AudioFormat audioFmt = this.audioFmt;
    if( (audioIO != null) && (audioFmt != null) ) {
      if( audioIO instanceof AudioIn ) {
        int channel = 0;
        if( this.btnChannel1.isSelected() ) {
          channel = 1;
        }
        if( channel >= audioFmt.getChannels() ) {
          channel = 0;
          this.btnChannel0.setSelected( true );
        }
        ((AudioIn) audioIO).setSelectedChannel( channel );
      }
    }
  }


  private void updOptFields()
  {
    AudioIO     audioIO  = this.audioIO;
    AudioFormat audioFmt = this.audioFmt;

    // Sample-Rate
    boolean state = ((audioIO == null)
        && (this.btnSoundOut.isSelected()
            || this.btnDataOut.isSelected()
            || this.btnDataIn.isSelected()
            || this.btnFileOut.isSelected()));

    this.labelSampleRate.setEnabled( state );
    this.comboSampleRate.setEnabled( state );

    // Geschwindigkeit
    if( (audioIO == null)
        && (this.btnDataIn.isSelected()
            || this.btnDataOut.isSelected()
            || this.btnFileIn.isSelected()
            || this.btnFileLastIn.isSelected()
            || this.btnFileOut.isSelected()) )
    {
      this.labelSpeed.setEnabled( true );
      this.spinnerSpeed.setEnabled( true );
    } else {
      this.labelSpeed.setEnabled( false );
      this.spinnerSpeed.setEnabled( false );
    }

    // Schwellwert
    state = ((audioIO == null)
             && (this.jtcSys.getOSType() == JTCSys.OSType.OS2K)
             && (this.btnDataIn.isSelected()
                 || this.btnFileIn.isSelected()
                 || this.btnFileLastIn.isSelected()));
    this.labelThreshold.setEnabled( state );
    this.sliderThreshold.setEnabled( state );

    // Kanalauswahl
    state = false;
    if( (audioIO != null) && (audioFmt != null) ) {
      if( audioIO instanceof AudioIn ) {
        if( audioFmt.getChannels() > 1 )
          state = true;
      }
    } else {
      state = (this.btnDataIn.isSelected()
               || this.btnFileIn.isSelected()
               || this.btnFileLastIn.isSelected());
    }
    this.labelChannel.setEnabled( state );
    this.btnChannel0.setEnabled( state );
    this.btnChannel1.setEnabled( state );

    // Mithoeren
    state = ((audioIO == null)
        && (this.btnFileOut.isSelected()
            || this.btnFileIn.isSelected()
            || this.btnFileLastIn.isSelected()));
    this.btnMonitorPlay.setEnabled( state );
  }


  private void updProgressBar( double value )
  {
    int intVal = (int) Math.round(
        value * (double) this.progressBar.getMaximum() ) + 1;

    if( intVal < this.progressBar.getMinimum() ) {
      intVal = this.progressBar.getMinimum();
    }
    else if( intVal > this.progressBar.getMaximum() ) {
      intVal = this.progressBar.getMaximum();
    }
    this.progressBar.setValue( intVal );
  }
}

