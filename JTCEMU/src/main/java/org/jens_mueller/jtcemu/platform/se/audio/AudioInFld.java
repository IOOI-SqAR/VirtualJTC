/*
 * (c) 2016-2021 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Komponente fuer die eingangsseitige Emulation
 * des Kassettenrecorderanschlusses
 */

package org.jens_mueller.jtcemu.platform.se.audio;

import org.jens_mueller.jtcemu.base.AppContext;
import org.jens_mueller.jtcemu.base.JTCSys;
import org.jens_mueller.jtcemu.platform.se.Main;
import org.jens_mueller.jtcemu.platform.se.base.FileDlg;
import org.jens_mueller.jtcemu.platform.se.base.GUIUtil;
import org.jens_mueller.jtcemu.platform.se.base.TopFrm;

import javax.sound.sampled.Line;
import javax.sound.sampled.Mixer;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.dnd.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;


public class AudioInFld
		extends AbstractAudioIOFld
		implements
			ActionListener,
			ChangeListener,
			DropTargetListener
{
  private static final String PROP_CHANNEL   = "channel";
  private static final String PROP_LINE      = "line";
  private static final String PROP_FILE      = "file";
  private static final String PROP_MONITOR   = "monitor";
  private static final String PROP_THRESHOLD = "threshold";
  private static final int    PROGRESS_MAX   = 1000;
  private static final int    THRESHOLD_MAX  = 100;

  private boolean      notified;
  private boolean      maxSpeedTriggered;
  private boolean      presetDone;
  private boolean      requestChannel1;
  private AudioIn audioIn;
  private File         file;
  private JRadioButton btnFromLine;
  private JRadioButton btnFromFile;
  private JRadioButton btnFromLastFile;
  private JRadioButton btnChannel0;
  private JRadioButton btnChannel1;
  private JCheckBox    tglMonitor;
  private JLabel       labelChannel;
  private JLabel       labelFile;
  private JLabel       labelFormat;
  private JLabel       labelProgress;
  private JLabel       labelThreshold;
  private JProgressBar progressBar;
  private JTextField   fldFile;
  private JTextField   fldFormat;
  private JButton      btnEnable;
  private JButton      btnDisable;
  private JButton      btnPlay;
  private JButton      btnPause;
  private JButton      btnMaxSpeed;
  private JSlider      sliderThreshold;
  private LED          ledThreshold;
  private DropTarget   dropTarget;


  public AudioInFld( AudioFrm audioFrm, JTCSys jtcSys, String propPrefix )
  {
    super( audioFrm, jtcSys, propPrefix );
    this.notified          = false;
    this.maxSpeedTriggered = false;
    this.presetDone        = false;
    this.requestChannel1   = false;
    this.audioIn           = null;
    this.file              = null;

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
					GridBagConstraints.WEST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 0, 5 ),
					0, 0 );

    ButtonGroup grpFct = new ButtonGroup();

    this.btnFromLine = new JRadioButton(
	"Audiodaten vom Sound-System (z.B. Mikrofonanschluss) lesen",
	true );
    grpFct.add( this.btnFromLine );
    panelFct.add( this.btnFromLine, gbcFct );

    this.btnFromFile = new JRadioButton(
	"Audiodaten aus Sound- oder Tape-Datei lesen" );
    grpFct.add( this.btnFromFile );
    gbcFct.insets.top = 0;
    gbcFct.gridy++;
    panelFct.add( this.btnFromFile, gbcFct );

    this.btnFromLastFile = new JRadioButton(
	"Letzte Sound- oder Tape-Datei noch einmal lesen" );
    grpFct.add( this.btnFromLastFile );
    gbcFct.insets.bottom = 5;
    gbcFct.gridy++;
    panelFct.add( this.btnFromLastFile, gbcFct );


    // Bereich Optionen
    JPanel panelOpt = new JPanel( new GridBagLayout() );
    panelOpt.setBorder( BorderFactory.createTitledBorder( "Optionen" ) );
    gbc.gridy++;
    add( panelOpt, gbc );

    GridBagConstraints gbcOpt = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.EAST,
						GridBagConstraints.NONE,
						new Insets( 5, 5, 0, 5 ),
						0, 0 );

    this.labelMixer = new JLabel( "Ger\u00E4t:" );
    panelOpt.add( this.labelMixer, gbcOpt );

    this.labelFrameRate = new JLabel( "Abtastrate (Hz):" );
    gbcOpt.gridy++;
    panelOpt.add( this.labelFrameRate, gbcOpt );

    this.labelChannel = new JLabel( "Aktiver Kanal:" );
    gbcOpt.gridy++;
    panelOpt.add( this.labelChannel, gbcOpt );

    this.labelThreshold  = new JLabel( "Schwellwert [%]:" );
    gbcOpt.insets.bottom = 5;
    gbcOpt.gridy++;
    panelOpt.add( this.labelThreshold, gbcOpt );

    gbcOpt.anchor        = GridBagConstraints.WEST;
    gbcOpt.insets.top    = 5;
    gbcOpt.insets.bottom = 0;
    gbcOpt.gridy         = 0;
    gbcOpt.gridx++;
    panelOpt.add( this.comboMixer, gbcOpt );

    gbcOpt.gridy++;
    panelOpt.add( this.comboFrameRate, gbcOpt );

    JPanel panelChannel = new JPanel( new GridBagLayout() );
    gbcOpt.fill         = GridBagConstraints.HORIZONTAL;
    gbcOpt.weightx      = 1.0;
    gbcOpt.gridy++;
    panelOpt.add( panelChannel, gbcOpt );

    GridBagConstraints gbcChannel = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.WEST,
						GridBagConstraints.NONE,
						new Insets( 0, 0, 0, 0 ),
						0, 0 );

    ButtonGroup grpChannel = new ButtonGroup();

    this.btnChannel0 = new JRadioButton( "Links", true );
    grpChannel.add( this.btnChannel0 );
    panelChannel.add( this.btnChannel0, gbcChannel );

    this.btnChannel1 = new JRadioButton( "Rechts" );
    grpChannel.add( this.btnChannel1 );
    gbcChannel.insets.left = 5;
    gbcChannel.gridx++;
    panelChannel.add( this.btnChannel1, gbcChannel );

    JPanel panelPlaceholder = new JPanel();
    panelPlaceholder.setPreferredSize( new Dimension( 1, 1 ) );
    gbcChannel.fill    = GridBagConstraints.HORIZONTAL;
    gbcChannel.weightx = 1.0;
    panelChannel.add( panelPlaceholder, gbcChannel );

    this.tglMonitor        = new JCheckBox( "Mith\u00F6ren" );
    gbcChannel.anchor      = GridBagConstraints.EAST;
    gbcChannel.fill        = GridBagConstraints.NONE;
    gbcChannel.weightx     = 0.0;
    gbcChannel.insets.left = 10;
    gbcChannel.gridx++;
    panelChannel.add( this.tglMonitor, gbcChannel );

    JPanel panelThreshold = new JPanel();
    gbcOpt.gridy++;
    panelOpt.add( panelThreshold, gbcOpt );

    panelThreshold.setLayout(
		new BoxLayout( panelThreshold, BoxLayout.X_AXIS ) );

    this.sliderThreshold = new JSlider(
				SwingConstants.HORIZONTAL,
				0, 100, 50 );
    this.sliderThreshold.setPaintLabels( false );
    this.sliderThreshold.setPaintTicks( false );
    this.sliderThreshold.setPaintTrack( true );
    this.sliderThreshold.setMaximumSize(
				new Dimension( Short.MAX_VALUE, 16 ) );

    panelThreshold.add( this.sliderThreshold );
    panelThreshold.add( Box.createHorizontalStrut( 10 ) );

    this.ledThreshold = new LED();
    this.ledThreshold.setMaximumSize( new Dimension( 30, 12 ) );
    panelThreshold.add( this.ledThreshold );


    // Bereich Status
    JPanel panelStatus = new JPanel( new GridBagLayout() );
    panelStatus.setBorder( BorderFactory.createTitledBorder( "Status" ) );
    gbc.gridy++;
    add( panelStatus, gbc );

    GridBagConstraints gbcStatus = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.EAST,
						GridBagConstraints.NONE,
						new Insets( 5, 5, 0, 5 ),
						0, 0 );

    this.labelFile = new JLabel( "Datei:" );
    panelStatus.add( this.labelFile, gbcStatus );

    this.labelFormat = new JLabel( "Format/L\u00E4nge:" );
    gbcStatus.gridy++;
    panelStatus.add( this.labelFormat, gbcStatus );

    this.labelProgress      = new JLabel( "Fortschritt:" );
    gbcStatus.insets.bottom = 5;
    gbcStatus.gridy++;
    panelStatus.add( this.labelProgress, gbcStatus );

    this.fldFile = new JTextField();
    this.fldFile.setEditable( false );
    gbcStatus.anchor        = GridBagConstraints.WEST;
    gbcStatus.fill          = GridBagConstraints.HORIZONTAL;
    gbcStatus.weightx       = 1.0;
    gbcStatus.insets.bottom = 0;
    gbcStatus.gridy         = 0;
    gbcStatus.gridx++;
    panelStatus.add( this.fldFile, gbcStatus );

    this.fldFormat = new JTextField();
    this.fldFormat.setEditable( false );
    gbcStatus.gridy++;
    panelStatus.add( this.fldFormat, gbcStatus );

    this.progressBar = new JProgressBar(
				SwingConstants.HORIZONTAL,
				0,
				PROGRESS_MAX );
    this.progressBar.setBorderPainted( true );
    this.progressBar.setStringPainted( false );
    gbcStatus.insets.bottom = 5;
    gbcStatus.gridy++;
    panelStatus.add( this.progressBar, gbcStatus );


    // rechte Seite
    JPanel panelEast = new JPanel( new GridBagLayout() );
    gbc.fill         = GridBagConstraints.VERTICAL;
    gbc.gridy        = 0;
    gbc.gridheight   = 3;
    gbc.gridx++;
    add( panelEast, gbc );

    GridBagConstraints gbcEast = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.NORTH,
						GridBagConstraints.NONE,
						new Insets( 5, 0, 0, 0 ),
						0, 0 );

    JPanel panelBtn = new JPanel( new GridLayout( 5, 1, 5, 5 ) );
    panelEast.add( panelBtn, gbcEast );

    this.btnEnable = new JButton( "Aktivieren" );
    panelBtn.add( this.btnEnable );

    this.btnDisable = new JButton( "Deaktivieren" );
    panelBtn.add( this.btnDisable );

    this.btnPlay = new JButton( "Abspielen" );
    panelBtn.add( this.btnPlay );

    this.btnPause = new JButton( "Pause" );
    panelBtn.add( this.btnPause );

    this.btnMaxSpeed = new JButton( "Turbo" );
    panelBtn.add( this.btnMaxSpeed );


    // Pegelanzeige
    this.volumeBar = new VolumeBar();
    this.volumeBar.setBorder( BorderFactory.createTitledBorder( "Pegel" ) );
    this.volumeBar.setPreferredSize( new Dimension( 1, 1 ) );
    gbcEast.insets.top = 20;
    gbcEast.fill       = GridBagConstraints.BOTH;
    gbcEast.weightx    = 1.0;
    gbcEast.weighty    = 1.0;
    gbcEast.gridy++;
    panelEast.add( this.volumeBar, gbcEast );


    // Dateinamensfeld als DropTarget aktivieren
    this.dropTarget = new DropTarget( this.fldFile, this );


    // Vorbelegungen
    applySettings();
    updFieldsEnabled();
    updThresholdFieldsEnabled();
    updFieldsEnabled();
    EventQueue.invokeLater(
			new Runnable()
			{
			  @Override
			  public void run()
			  {
			    presetFields();
			  }
			} );
  }


  public void closeCPUSynchronLine()
  {
    AudioIn audioIn = this.audioIn;
    if( audioIn != null ) {
      audioIn.closeCPUSynchronLine();
    }
  }


  public void doDisable()
  {
    AudioIn audioIn = this.audioIn;
    if( audioIn != null ) {
      audioIn.requestStop();
    }
    setMaxSpeed( false );
  }


  public boolean isAudioActive()
  {
    return (this.audioIn != null);
  }


  public void memorizeSettings()
  {
    super.memorizeSettings();
    AppContext.setProperty(
			this.propPrefix + PROP_ENABLED,
			this.audioIn != null );
    if( this.file != null ) {
      AppContext.setProperty(
			this.propPrefix + PROP_LINE,
			this.btnFromLine.isSelected() );
      AppContext.setProperty(
			this.propPrefix + PROP_FILE,
			this.file.getPath() );
    } else {
      AppContext.setProperty(
			this.propPrefix + PROP_LINE,
			true );
      AppContext.setProperty(
			this.propPrefix + PROP_FILE,
			null );
    }
    AppContext.setProperty(
			this.propPrefix + PROP_CHANNEL,
			this.btnChannel1.isSelected() ? 1 : 0 );
    AppContext.setProperty(
			this.propPrefix + PROP_MONITOR,
			this.tglMonitor.isSelected() );
    AppContext.setProperty(
			this.propPrefix + PROP_THRESHOLD,
			this.sliderThreshold.getValue() );
  }


  public void updThresholdFieldsEnabled()
  {
    boolean state = (this.jtcSys.getOSType() == JTCSys.OSType.OS2K);
    this.labelThreshold.setEnabled( state );
    this.sliderThreshold.setEnabled( state );
    this.ledThreshold.setEnabled( state );
  }


  public void z8CyclesPerSecondChanged()
  {
    updMaxSpeedBtnEnabled();
  }


	/* --- ActionListener --- */

  @Override
  public void actionPerformed( ActionEvent e )
  {
    Object src = e.getSource();
    if( src == this.btnEnable ) {
      doEnable();
    } else if( src == this.btnDisable ) {
      doDisable();
    } else if( src == this.btnPlay ) {
      doPlay();
    } else if( src == this.btnPause ) {
      doPause();
    } else if( src == this.btnMaxSpeed ) {
      doMaxSpeed();
    } else if( (src == this.btnFromLine)
	       || (src == this.btnFromFile)
	       || (src == this.btnFromLastFile) )
    {
      updFieldsEnabled();
    } else if( (src == this.btnChannel0) || (src == this.btnChannel1) ) {
      updSelectedChannel();
    } else if( src == this.tglMonitor ) {
      doMonitor();
    }
  }


	/* --- ChangeListener --- */

  @Override
  public void stateChanged( ChangeEvent e )
  {
    if( e.getSource() == this.sliderThreshold ) {
      AudioIn audioIn = this.audioIn;
      if( audioIn != null ) {
	audioIn.setThresholdValue( getThresholdValue() );
      }
    }
  }


	/* --- DropTargetListener --- */

  @Override
  public void dragEnter( DropTargetDragEvent e )
  {
    if( !GUIUtil.isFileDrop( e ) )
      e.rejectDrag();
  }


  @Override
  public void dragExit( DropTargetEvent e )
  {
    // leer
  }


  @Override
  public void dragOver( DropTargetDragEvent e )
  {
    // leer
  }


  @Override
  public void drop( DropTargetDropEvent e )
  {
    final File file = GUIUtil.fileDrop( this, e );
    if( file != null ) {
      // nicht auf Benutzereingaben warten
      EventQueue.invokeLater(
			new Runnable()
			{
			  @Override
			  public void run()
			  {
			    fileDropped( file );
			  }
			} );
    }
  }


  @Override
  public void dropActionChanged( DropTargetDragEvent e )
  {
    // leer
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public void addNotify()
  {
    super.addNotify();
    if( !this.notified ) {
      this.notified = true;
      this.btnEnable.addActionListener( this );
      this.btnDisable.addActionListener( this );
      this.btnPlay.addActionListener( this );
      this.btnPause.addActionListener( this );
      this.btnMaxSpeed.addActionListener( this );
      this.btnFromLine.addActionListener( this );
      this.btnFromFile.addActionListener( this );
      this.btnFromLastFile.addActionListener( this );
      this.btnChannel0.addActionListener( this );
      this.btnChannel1.addActionListener( this );
      this.tglMonitor.addActionListener( this );
      this.sliderThreshold.addChangeListener( this );
      this.dropTarget.setActive( true );

      Dimension tfSize = this.fldFormat.getPreferredSize();
      Dimension pbSize = this.progressBar.getPreferredSize();
      if( (tfSize != null) && (pbSize != null) ) {
	this.progressBar.setPreferredSize(
			new Dimension( pbSize.width, tfSize.height ) );
      }
    }
  }


  @Override
  protected void audioFinished( String errMsg )
  {
    super.audioFinished( errMsg );
    setMaxSpeed( false );
    this.jtcSys.setTapeReader( null );
    this.audioIn = null;
    if( this.file != null ) {
      this.fldFile.setText( this.file.getPath() );
    }
    this.fldFormat.setText( "" );
    updFieldsEnabled();
    if( errMsg != null ) {
      showErrorDlg( errMsg );
    }
  }


  @Override
  public void checkEnable()
  {
    presetFields();
    super.checkEnable();
  }


  @Override
  protected void doEnable()
  {
    try {
      if( this.audioIn == null ) {
	if( this.btnFromLine.isSelected() ) {
	  this.audioFrm.checkOpenCPUSynchronLine();
	  AudioIn audioIn = new AudioInLine(
				this,
				this.jtcSys.getZ8(),
				getThresholdValue(),
				getSelectedFrameRate(),
				getSelectedMixer() );
	  this.audioIn = audioIn;
	  this.jtcSys.setTapeReader( audioIn );
	  this.fldFile.setText( "" );
	  updProgressFld();
	  updFieldsEnabled();
	} else if( this.btnFromFile.isSelected() ) {
	  enableFile( null );
	} else if( this.btnFromLastFile.isSelected() ) {
	  enableFile( this.file );
	}
      }
    }
    catch( IOException ex ) {
      showErrorDlg( ex.getMessage() );
    }
  }


  @Override
  public void fireUpdVolume( final int value, final boolean volumeStatus )
  {
    final VolumeBar volumeBar    = this.volumeBar;
    final LED       thresholdLED = this.ledThreshold;
    EventQueue.invokeLater(
		new Runnable()
		{
		  @Override
		  public void run()
		  {
		    volumeBar.updVolume( value );
		    thresholdLED.setLighted( volumeStatus );
		  }
		} );
  }


  @Override
  protected void formatChanged( String formatText )
  {
    if( formatText == null ) {
      formatText = "";
    }
    AudioIn audioIn = this.audioIn;
    if( audioIn != null ) {
      if( audioIn instanceof AudioInFile ) {
	String lenText = AudioIO.getTimeText(
				((AudioInFile) audioIn).getFrameRate(),
				((AudioInFile) audioIn).getFrameLength() );
	if( lenText != null ) {
	  if( !lenText.isEmpty() ) {
	    if( formatText.isEmpty() ) {
	      formatText = lenText;
	    } else {
	      formatText = formatText + ", " + lenText;
	    }
	  }
	}
      }
      if( this.requestChannel1 && (audioIn.getChannels() > 1) ) {
	this.btnChannel1.setSelected( true );
      } else {
	this.btnChannel0.setSelected( true );
      }
    }
    this.fldFormat.setText( formatText );
    updChannelFieldsEnabled();
  }


  @Override
  protected AudioIO getAudioIO()
  {
    return this.audioIn;
  }


  @Override
  protected Line.Info[] getLineInfo( Mixer mixer )
  {
    return mixer != null ? mixer.getTargetLineInfo() : null;
  }


  @Override
  protected void monitorStatusChanged( boolean status, String errorText )
  {
    /*
     * Die Mithoeroption soll bei regulaerem Ende
     * (errorText == null) nicht ausgeschaltet werden.
     */
    if( (this.tglMonitor.isSelected() != status)
	&& (status || (errorText != null)) )
    {
      ActionListener[] listeners = this.tglMonitor.getActionListeners();
      if( listeners != null ) {
	for( ActionListener listener : listeners ) {
	   this.tglMonitor.removeActionListener( listener );
	}
      }
      this.tglMonitor.setSelected( status );
      if( listeners != null ) {
	for( ActionListener listener : listeners ) {
	   this.tglMonitor.addActionListener( listener );
	}
      }
      if( errorText != null ) {
	this.tglMonitor.setEnabled( false );
	showErrorDlg( errorText );
      }
    }
  }


  @Override
  public void removeNotify()
  {
    if( this.notified ) {
      this.notified = false;
      this.dropTarget.setActive( false );
      this.btnEnable.removeActionListener( this );
      this.btnDisable.removeActionListener( this );
      this.btnPlay.removeActionListener( this );
      this.btnPause.removeActionListener( this );
      this.btnMaxSpeed.removeActionListener( this );
      this.btnFromLine.removeActionListener( this );
      this.btnFromFile.removeActionListener( this );
      this.btnFromLastFile.removeActionListener( this );
      this.btnChannel0.removeActionListener( this );
      this.btnChannel1.removeActionListener( this );
      this.tglMonitor.removeActionListener( this );
      this.sliderThreshold.removeChangeListener( this );
    }
    super.removeNotify();
  }


  @Override
  public void updProgressFld()
  {
    int     value   = 0;
    AudioIn audioIn = this.audioIn;
    if( audioIn != null ) {
      if( audioIn instanceof AudioInFile ) {
	long pos = ((AudioInFile) audioIn).getFramePos();
	long len = ((AudioInFile) audioIn).getFrameLength();
	if( (pos >= 0) && (len > 0) ) {
	  value = (int) Math.round( pos * PROGRESS_MAX / len );
	  if( value > PROGRESS_MAX ) {
	    value = PROGRESS_MAX;
	  }
	}
      }
    }
    this.progressBar.setValue( value );
  }


	/* --- private Methoden --- */

  private void doMaxSpeed()
  {
    if( (this.audioIn != null)
	&& (this.jtcSys.getZ8().getCyclesPerSecond() > 0) )
    {
      setMaxSpeed( true );
    }
  }


  private void doMonitor()
  {
    AudioIn audioIn = this.audioIn;
    if( audioIn != null ) {
      if( audioIn instanceof AudioInFile ) {
	boolean state = this.tglMonitor.isSelected();
	if( state ) {
	  try {
	    this.audioFrm.checkOpenCPUSynchronLine();
	    ((AudioInFile) audioIn).setMonitorRequest( true );
	  }
	  catch( IOException ex ) {
	    this.tglMonitor.setSelected( false );
	    showErrorDlg( ex.getMessage() );
	  }
	} else {
	  ((AudioInFile) audioIn).setMonitorRequest( false );
	}
      }
    }
  }


  private void doPlay()
  {
    AudioIn audioIn = this.audioIn;
    if( audioIn != null ) {
      if( audioIn instanceof AudioInFile ) {
	((AudioInFile) audioIn).setPause( false );
	startProgressTimer();
	this.btnPlay.setEnabled( false );
	this.btnPause.setEnabled( true );
	updMaxSpeedBtnEnabled();
      }
    }
  }


  private void doPause()
  {
    AudioIn audioIn = this.audioIn;
    if( audioIn != null ) {
      if( audioIn instanceof AudioInFile ) {
	setMaxSpeed( false );
	((AudioInFile) audioIn).setPause( true );
	stopProgressTimer();
	this.btnPlay.setEnabled( true );
	this.btnPause.setEnabled( false );
	this.btnMaxSpeed.setEnabled( false );
      }
    }
  }


  private void enableFile( File file )
  {
    if( this.audioIn == null ) {
      if( file == null ) {
	file = FileDlg.showFileOpenDlg(
			this.audioFrm,
			"Sound-/Tape-Datei \u00F6ffnen",
			"\u00D6ffnen",
			AppContext.getLastDirFile( FILE_GROUP_AUDIO ),
			getFileFilter() );
      }
      if( file != null ) {
	AudioIn audioIn = new AudioInFile(
				this,
				this.jtcSys.getZ8(),
				getThresholdValue(),
				file,
				this.tglMonitor.isSelected() );
	this.audioIn = audioIn;
	this.file    = file;
	this.jtcSys.setTapeReader( audioIn );
	this.fldFile.setText( file.getPath() );
	updProgressFld();
	updFieldsEnabled();
      }
    }
  }


  private void fileDropped( File file )
  {
    if( this.audioIn != null ) {
      showErrorDlg( "Sie m\u00FCssen zuerst die Audiofunktion"
		+ " deaktivieren,\n"
		+ "bevor Sie eine neue Datei \u00F6ffnen k\u00F6nnen." );
    } else {
      this.btnFromFile.setSelected( true );
      enableFile( file );
    }
  }


  private float getThresholdValue()
  {
    return (float) this.sliderThreshold.getValue()
			/ (float) this.sliderThreshold.getMaximum();
  }


  private void presetFields()
  {
    if( !this.presetDone ) {
      String fileName = AppContext.getProperty( this.propPrefix + PROP_FILE );
      if( fileName != null ) {
	if( fileName.isEmpty() ) {
	  fileName = null;
	}
      }
      if( fileName != null ) {
	this.fldFile.setText( fileName );
	this.file = new File( fileName );
	if( AppContext.getBooleanProperty(
				this.propPrefix + PROP_LINE,
				false ) )
	{
	  this.btnFromLine.setSelected( true );
	} else {
	  this.btnFromLastFile.setSelected( true );
	}
      } else {
	this.btnFromLine.setSelected( true );
      }
      if( AppContext.getIntProperty(
			this.propPrefix + PROP_CHANNEL ) > 0 )
      {
	this.requestChannel1 = true;
      }
      this.tglMonitor.setSelected(
		AppContext.getBooleanProperty(
					this.propPrefix + PROP_MONITOR,
					false ) );
      Integer thresValue = AppContext.getIntegerProperty(
					this.propPrefix + PROP_THRESHOLD );
      if( thresValue != null ) {
	if( (thresValue.intValue() >= this.sliderThreshold.getMinimum())
	    && (thresValue.intValue() <= this.sliderThreshold.getMaximum()) )
	{
	  this.sliderThreshold.setValue( thresValue.intValue() );
	}
      }
      this.presetDone = true;
    }
  }


  private void setMaxSpeed( boolean state )
  {
    TopFrm topFrm = Main.getTopFrm();
    if( topFrm != null ) {
      if( state ) {
	AudioIn audioIn = this.audioIn;
	if( audioIn != null ) {
	  if( audioIn instanceof AudioInFile ) {
	    if( topFrm.setMaxSpeed( this, true ) ) {
	      this.maxSpeedTriggered = true;
	    }
	  }
	}
      } else {
	if( this.maxSpeedTriggered ) {
	  topFrm.setMaxSpeed( this, false );
	  this.maxSpeedTriggered = false;
	}
      }
    }
  }


  private void updChannelFieldsEnabled()
  {
    boolean state   = false;
    AudioIn audioIn = this.audioIn;
    if( audioIn != null ) {
      if( audioIn.getChannels() > 1 ) {
	state = true;
      } else {
	this.btnChannel0.setSelected( true );
      }
    }
    this.labelChannel.setEnabled( state );
    this.btnChannel0.setEnabled( state );
    this.btnChannel1.setEnabled( state );
  }


  private void updFieldsEnabled()
  {
    boolean running  = false;
    boolean fromLine = this.btnFromLine.isSelected();
    boolean fromFile = this.btnFromFile.isSelected()
				|| this.btnFromLastFile.isSelected();
    boolean pause    = false;
    AudioIn audioIn  = this.audioIn;
    if( audioIn != null ) {
      running  = true;
      if( audioIn instanceof AudioInFile ) {
	pause = ((AudioInFile) audioIn).isPause();
      }
    }
    this.btnEnable.setEnabled( !running );
    this.btnDisable.setEnabled( running );
    this.btnPlay.setEnabled( running && fromFile && pause );
    this.btnPause.setEnabled( running && fromFile && !pause );
    this.btnFromLine.setEnabled( !running );
    this.btnFromFile.setEnabled( !running );
    this.btnFromLastFile.setEnabled( !running && (this.file != null) );
    this.labelMixer.setEnabled( !running && fromLine );
    this.comboMixer.setEnabled( !running && fromLine );
    this.labelFrameRate.setEnabled( !running && fromLine );
    this.comboFrameRate.setEnabled( !running && fromLine );
    this.tglMonitor.setEnabled( fromFile );
    this.labelFile.setEnabled( running && fromFile );
    this.fldFile.setEnabled( running && fromFile );
    this.labelFormat.setEnabled( running );
    this.fldFormat.setEnabled( running );
    this.labelProgress.setEnabled( running && fromFile );
    if( !running || !fromFile ) {
      this.progressBar.setValue( 0 );
    }
    this.progressBar.setStringPainted( running && fromFile );
    this.progressBar.setEnabled( running && fromFile );
    this.volumeBar.setVolumeBarState( running );
    updChannelFieldsEnabled();
    updMaxSpeedBtnEnabled();
  }


  private void updMaxSpeedBtnEnabled()
  {
    boolean state   = false;
    AudioIn audioIn = this.audioIn;
    if( audioIn != null ) {
      if( audioIn instanceof AudioInFile ) {
	state = true;
	if( ((AudioInFile) audioIn).isPause() ) {
	  state = false;
	}
      }
      if( this.jtcSys.getZ8().getCyclesPerSecond() < 1 ) {
	state = false;
      }
    }
    this.btnMaxSpeed.setEnabled( state );
  }


  private void updSelectedChannel()
  {
    AudioIn audioIn = this.audioIn;
    if( audioIn != null ) {
      int channels = audioIn.getChannels();
      if( channels > 0 ) {
	int channel = 0;
	if( this.btnChannel1.isSelected() ) {
	  channel = 1;
	}
	if( channel >= audioIn.getChannels() ) {
	  channel = 0;
	  this.btnChannel0.setSelected( true );
	}
	audioIn.setSelectedChannel( channel );
      }
    }
  }
}
