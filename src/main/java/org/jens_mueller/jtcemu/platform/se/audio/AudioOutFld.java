/*
 * (c) 2016-2021 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Komponente fuer die ausgangsseitige Emulation
 * des Kassettenrecorderanschlusses und des Lautsprechers
 */

package jtcemu.platform.se.audio;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import javax.sound.sampled.Line;
import javax.sound.sampled.Mixer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import jtcemu.base.AppContext;
import jtcemu.base.JTCSys;
import jtcemu.platform.se.base.BaseDlg;
import jtcemu.platform.se.base.FileDlg;


public class AudioOutFld
		extends AbstractAudioIOFld
		implements ActionListener
{
  public static final String PROP_LINE     = "line";
  public static final String PROP_RECORDER = "recorder";

  private boolean     loudspeaker;
  private boolean     notified;
  private boolean     recordedSaved;
  private AudioData   recordedData;
  private AudioOut    audioOut;
  private AudioPlayer audioPlayer;
  private JLabel      labelFormat;
  private JLabel      labelRecDuration;
  private JTextField  fldFormat;
  private JTextField  fldRecDuration;
  private JCheckBox   btnToLine;
  private JCheckBox   btnToRecorder;
  private JButton     btnEnable;
  private JButton     btnDisable;
  private JButton     btnPlay;
  private JButton     btnSave;


  public AudioOutFld(
		AudioFrm audioFrm,
		JTCSys   jtcSys,
		String   propPrefix,
		boolean  loudspeaker )
  {
    super( audioFrm, jtcSys, propPrefix );
    this.loudspeaker   = loudspeaker;
    this.notified      = false;
    this.recordedSaved = false;
    this.recordedData  = null;
    this.audioOut      = null;
    this.audioPlayer   = null;

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

    this.btnToLine = new JCheckBox(
	"Audiodaten \u00FCber Sound-System (z.B. Lautsprecher) ausgegeben" );
    panelFct.add( this.btnToLine, gbcFct );

    this.btnToRecorder = new JCheckBox(
	"Audiodaten aufnehmen und in Datei speichern" );
    gbcFct.insets.top    = 0;
    gbcFct.insets.bottom = 5;
    gbcFct.gridy++;
    panelFct.add( this.btnToRecorder, gbcFct );


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

    panelOpt.add( this.labelMixer, gbcOpt );

    gbcOpt.insets.bottom = 5;
    gbcOpt.gridy++;
    panelOpt.add( this.labelFrameRate, gbcOpt );

    gbcOpt.anchor        = GridBagConstraints.WEST;
    gbcOpt.insets.bottom = 0;
    gbcOpt.gridwidth     = GridBagConstraints.REMAINDER;
    gbcOpt.gridy         = 0;
    gbcOpt.gridx++;
    panelOpt.add( this.comboMixer, gbcOpt );

    gbcOpt.insets.bottom = 5;
    gbcOpt.gridy++;
    panelOpt.add( this.comboFrameRate, gbcOpt );


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

    this.labelFormat = new JLabel( "Format:" );
    panelStatus.add( this.labelFormat, gbcStatus );

    this.labelRecDuration = new JLabel( "Aufnahmedauer:" );
    gbcStatus.insets.bottom = 5;
    gbcStatus.gridy++;
    panelStatus.add( this.labelRecDuration, gbcStatus );

    this.fldFormat = new JTextField();
    this.fldFormat.setEditable( false );
    gbcStatus.fill          = GridBagConstraints.HORIZONTAL;
    gbcStatus.weightx       = 1.0;
    gbcStatus.insets.bottom = 0;
    gbcStatus.gridy         = 0;
    gbcStatus.gridx++;
    panelStatus.add( this.fldFormat, gbcStatus );

    this.fldRecDuration = new JTextField();
    this.fldRecDuration.setEditable( false );
    gbcStatus.anchor        = GridBagConstraints.WEST;
    gbcStatus.insets.bottom = 5;
    gbcStatus.gridy++;
    panelStatus.add( this.fldRecDuration, gbcStatus );


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

    JPanel panelBtn = new JPanel( new GridLayout( 4, 1, 5, 5 ) );
    panelEast.add( panelBtn, gbcEast );

    this.btnEnable = new JButton( "Aktivieren" );
    panelBtn.add( this.btnEnable );

    this.btnDisable = new JButton( "Deaktivieren" );
    panelBtn.add( this.btnDisable );

    this.btnPlay = new JButton( "Wiedergabe" );
    panelBtn.add( this.btnPlay );

    this.btnSave = new JButton( "Speichern..." );
    panelBtn.add( this.btnSave );


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


    // Vorbelegungen
    applySettings();
    this.btnToLine.setSelected(
		AppContext.getBooleanProperty(
				this.propPrefix + PROP_LINE,
				true ) );
    this.btnToRecorder.setSelected(
		AppContext.getBooleanProperty(
				this.propPrefix + PROP_RECORDER,
				false ) );
    updFieldsEnabled();
  }


  public void closeCPUSynchronLine()
  {
    AudioOut audioOut = this.audioOut;
    if( audioOut != null ) {
      audioOut.closeCPUSynchronLine();
    }
  }


  public boolean confirmDataSaved()
  {
    boolean rv = true;
    if( (this.recordedData != null) && !this.recordedSaved ) {
      try {
	this.audioFrm.setSelectedComponent( this );
	rv = false;

	String[]    options = { "Speichern", "Verwerfen", "Abbrechen" };
	JOptionPane pane    = new JOptionPane(
		"Die aufgenommenen Audiodaten wurden nicht gespeichert.\n"
			+ "Sie k\u00F6nnen diese jetzt speichern,"
			+ " verwerfern\n"
			+ "oder die Aktion abbrechen.",
		JOptionPane.WARNING_MESSAGE );
	pane.setOptions( options );
	pane.setInitialValue( options[ 0 ] );
	pane.setWantsInput( false );
	pane.createDialog( this, "Daten ge\u00E4ndert" ).setVisible( true );
	Object value = pane.getValue();
	if( value != null ) {
	  if( value.equals( options[ 0 ] ) ) {
	    doSave();
	    rv = this.recordedSaved;
	  }
	  else if( value.equals( options[ 1 ] ) ) {
	    this.recordedSaved = true;
	    rv                 = true;
	  }
	}
      }
      catch( IllegalArgumentException ex ) {}
    }
    return rv;
  }


  public void doDisable()
  {
    AudioOut audioOut = this.audioOut;
    if( audioOut != null ) {
      audioOut.requestStop();
    }
  }


  public boolean isAudioActive()
  {
    return (this.audioOut != null);
  }


  public void memorizeSettings()
  {
    super.memorizeSettings();
    AppContext.setProperty(
		this.propPrefix + PROP_ENABLED,
		this.audioOut != null );
    AppContext.setProperty(
		this.propPrefix + PROP_LINE,
		this.btnToLine.isSelected() );
    AppContext.setProperty(
		this.propPrefix + PROP_RECORDER,
		this.btnToRecorder.isSelected() );
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
    } else if( src == this.btnSave ) {
      doSave();
    } else if( src == this.btnToLine ) {
      if( !this.btnToLine.isSelected()
	  && !this.btnToRecorder.isSelected() )
      {
	this.btnToRecorder.setSelected( true );
      }
      updFieldsEnabled();
    } else if( src == this.btnToRecorder ) {
      if( !this.btnToLine.isSelected()
	  && !this.btnToRecorder.isSelected() )
      {
	this.btnToLine.setSelected( true );
      }
      updFieldsEnabled();
    }
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
      this.btnSave.addActionListener( this );
      this.btnToLine.addActionListener( this );
      this.btnToRecorder.addActionListener( this );
    }
  }


  @Override
  protected void audioFinished( String errMsg )
  {
    super.audioFinished( errMsg );
    if( this.loudspeaker ) {
      this.jtcSys.setLoudspeaker( null );
    } else {
      this.jtcSys.setTapeWriter( null );
    }
    if( this.audioOut != null ) {
      this.recordedData = this.audioOut.getRecordedData();
      if( this.recordedData != null ) {
	if( this.recordedData.getSampleCount() <= 0 ) {
	  this.recordedData = null;
	}
      }
      this.audioOut = null;
    }
    updFieldsEnabled();
    if( errMsg != null ) {
      showErrorDlg( errMsg );
    }
  }


  @Override
  protected void doEnable()
  {
    try {
      if( this.audioOut == null ) {
	if( confirmDataSaved() ) {
	  this.audioFrm.checkOpenCPUSynchronLine();
	  boolean  lineRequested = this.btnToLine.isSelected();
	  boolean  recRequested  = this.btnToRecorder.isSelected();
	  AudioOut audioOut      = new AudioOut(
					this,
					this.jtcSys.getZ8(),
					getSelectedFrameRate(),
					lineRequested,
					lineRequested ?
						getSelectedMixer()
						: null,
					recRequested );
	  this.recordedData  = null;
	  this.recordedSaved = false;
	  this.audioOut      = audioOut;
	  if( this.loudspeaker ) {
	    this.jtcSys.setLoudspeaker( audioOut );
	  } else {
	    this.jtcSys.setTapeWriter( audioOut );
	  }
	  updProgressFld();
	  updFieldsEnabled();
	  if( recRequested ) {
	    startProgressTimer();
	  }
	}
      }
    }
    catch( IOException ex ) {
      showErrorDlg( ex.getMessage() );
    }
  }


  @Override
  protected void formatChanged( String formatText )
  {
    this.fldFormat.setText( formatText );
  }


  @Override
  protected AudioIO getAudioIO()
  {
    return this.audioOut;
  }


  @Override
  protected Line.Info[] getLineInfo( Mixer mixer )
  {
    return mixer != null ? mixer.getSourceLineInfo() : null;
  }


  @Override
  public void removeNotify()
  {
    if( this.notified ) {
      this.notified = false;
      cancelAudioPlayer();
      this.btnEnable.removeActionListener( this );
      this.btnDisable.removeActionListener( this );
      this.btnPlay.removeActionListener( this );
      this.btnSave.removeActionListener( this );
      this.btnToLine.removeActionListener( this );
      this.btnToRecorder.removeActionListener( this );
    }
    super.removeNotify();
  }


  @Override
  public void updProgressFld()
  {
    AudioOut audioOut = this.audioOut;
    this.fldRecDuration.setText(
		audioOut != null ? audioOut.getRecordedDurationText() : "" );
  }


	/* --- private Methoden --- */

  private void cancelAudioPlayer()
  {
    AudioPlayer player = this.audioPlayer;
    if( player != null ) {
      player.cancel();
    }
  }


  private void doPlay()
  {
    AudioData samples = this.recordedData;
    if( samples != null ) {
      cancelAudioPlayer();
      this.audioPlayer = AudioPlayer.play( this.audioFrm, samples );
    }
  }


  private void doSave()
  {
    AudioData samples = this.recordedData;
    if( samples != null ) {
      long sampleCount = samples.getSampleCount();
      int  sampleRate  = samples.getSampleRate();
      if( (sampleCount > 0) && (sampleRate > 0) ) {
	File file = FileDlg.showFileSaveDlg(
			this.audioFrm,
			"Sound-/Tape-Datei speichern...",
			AppContext.getLastDirFile( FILE_GROUP_AUDIO ),
			getFileFilter() );
	if( file != null ) {
	  try {
	    samples.writeToFile( file );
	    AppContext.setLastFile( FILE_GROUP_AUDIO, file );
	    this.recordedSaved = true;
	  }
	  catch( IOException ex ) {
	    BaseDlg.showError( this, ex.getMessage() );
	  }
	}
      }
    }
  }


  private void updFieldsEnabled()
  {
    boolean  line     = this.btnToLine.isSelected();
    boolean  recorder = this.btnToRecorder.isSelected();
    boolean  recorded = (this.recordedData != null);
    boolean  running  = isAudioActive();
    this.btnEnable.setEnabled( !running );
    this.btnDisable.setEnabled( running );
    this.btnPlay.setEnabled( recorded && !running );
    this.btnSave.setEnabled( recorded && !running );
    this.btnToLine.setEnabled( !running );
    this.btnToRecorder.setEnabled( !running );
    this.labelMixer.setEnabled( !running && line );
    this.comboMixer.setEnabled( !running && line );
    this.labelFrameRate.setEnabled( !running );
    this.comboFrameRate.setEnabled( !running );
    if( !running && !recorded ) {
      this.fldFormat.setText( "" );
      this.fldRecDuration.setText( "" );
    }
    this.labelFormat.setEnabled( running );
    this.fldFormat.setEnabled( running );
    this.labelRecDuration.setEnabled( recorder && running );
    this.fldRecDuration.setEnabled( recorder && running );
    this.volumeBar.setVolumeBarState( running );
  }
}
