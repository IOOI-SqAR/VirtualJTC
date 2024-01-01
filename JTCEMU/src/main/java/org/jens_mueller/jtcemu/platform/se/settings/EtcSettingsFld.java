/*
 * (c) 2020-2021 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Sonstige Einstellungen
 */

package org.jens_mueller.jtcemu.platform.se.settings;

import org.jens_mueller.jtcemu.base.AppContext;
import org.jens_mueller.jtcemu.platform.se.Main;
import org.jens_mueller.jtcemu.platform.se.base.FileDlg;
import org.jens_mueller.jtcemu.platform.se.base.TopFrm;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Properties;


public class EtcSettingsFld
			extends JPanel
			implements ActionListener, ChangeListener
{
  private static final int MAX_MARGIN = 199;

  private SettingsFrm       settingsFrm;
  private File              pathsFile;
  private File              propsFile;
  private JRadioButton      btnFileDlgEmu;
  private JRadioButton      btnFileDlgNative;
  private JComboBox<String> comboScreenRefresh;
  private JSpinner          spinnerMargin;
  private JTextField        fldPathsFile;
  private JTextField        fldPropsFile;
  private JButton           btnPathsFileDelete;
  private JButton           btnPropsFileDelete;


  public EtcSettingsFld( SettingsFrm settingsFrm, TopFrm topFrm )
  {
    this.settingsFrm = settingsFrm;
    this.pathsFile   = Main.getPathsFile();
    this.propsFile   = Main.getPropertiesFile();
    setLayout( new GridBagLayout() );

    GridBagConstraints gbc = new GridBagConstraints(
					0, 0,
					GridBagConstraints.REMAINDER, 1,
					0.0, 0.0,
					GridBagConstraints.WEST,
					GridBagConstraints.NONE,
					new Insets( 5, 5, 0, 5 ),
					0, 0 );

    add( new JLabel( "Dateiauswahldialog:" ), gbc );

    ButtonGroup grpFileSelect = new ButtonGroup();

    this.btnFileDlgEmu = new JRadioButton(
		AppContext.getAppName() + "-Dateiauswahldialog verwenden",
		true );
    grpFileSelect.add( this.btnFileDlgEmu );
    this.btnFileDlgEmu.addActionListener( this );
    gbc.insets.top  = 0;
    gbc.insets.left = 50;
    gbc.gridy++;
    add( this.btnFileDlgEmu, gbc );

    this.btnFileDlgNative = new JRadioButton(
		"Dateiauswahldialog des Betriebssystems verwenden" );
    grpFileSelect.add( this.btnFileDlgNative );
    this.btnFileDlgNative.addActionListener( this );
    gbc.insets.bottom = 5;
    gbc.gridy++;
    add( this.btnFileDlgNative, gbc );

    String s = AppContext.getProperty( FileDlg.PROP_FILEDIALOG );
    if( s != null ) {
      if( s.equals( FileDlg.VALUE_FILEDIALOG_NATIVE ) ) {
	this.btnFileDlgNative.setSelected( true );
      }
    }

    gbc.anchor      = GridBagConstraints.EAST;
    gbc.insets.top  = 5;
    gbc.insets.left = 5;
    gbc.gridwidth   = 1;
    gbc.gridy++;
    add( new JLabel( "Rand um Bildschirmausgabe:" ), gbc );

    SpinnerNumberModel sm = new SpinnerNumberModel( 0, 0, MAX_MARGIN, 1 );
    try {
      int margin = AppContext.getIntProperty(
				TopFrm.PROP_SCREEN_MARGIN,
				TopFrm.DEFAULT_SCREEN_MARGIN );
      if( (margin >= 0) && (margin <= MAX_MARGIN) ) {
	sm.setValue( margin );
      }
    }
    catch( IllegalArgumentException ex ) {}

    this.spinnerMargin = new JSpinner( sm );
    this.spinnerMargin.addChangeListener( this );

    gbc.anchor = GridBagConstraints.WEST;
    gbc.gridx++;
    add( this.spinnerMargin, gbc );

    gbc.gridx++;
    add( new JLabel( "Pixel" ), gbc );

    gbc.anchor = GridBagConstraints.EAST;
    gbc.gridx  = 0;
    gbc.gridy++;
    add(
	new JLabel( "Aktualisierungszyklus der Bildschirmausgabe:" ),
	gbc );

    this.comboScreenRefresh = new JComboBox<>();
    this.comboScreenRefresh.setEditable( false );
    this.comboScreenRefresh.addItem( "10" );
    this.comboScreenRefresh.addItem( "20" );
    this.comboScreenRefresh.addItem( "30" );
    this.comboScreenRefresh.addItem( "50" );
    this.comboScreenRefresh.addItem( "100" );
    this.comboScreenRefresh.addItem( "200" );
    this.comboScreenRefresh.setSelectedItem(
		Integer.toString( topFrm.getScreenRefreshMillis() ) );
    this.comboScreenRefresh.addActionListener( this );
    gbc.anchor = GridBagConstraints.WEST;
    gbc.gridx++;
    add( this.comboScreenRefresh, gbc );

    gbc.gridx++;
    add( new JLabel( "ms" ), gbc );

    if( this.pathsFile != null ) {
      gbc.insets.top    = 15;
      gbc.insets.bottom = 0;
      gbc.gridwidth     = 1;
      gbc.gridx         = 0;
      gbc.gridy++;
      add(
	new JLabel( "Zuletzt verwendete Dateipfade werden gespeichert"
						+ " in Datei:" ),
	gbc );

      this.fldPathsFile = new JTextField();
      this.fldPathsFile.setEditable( false );
      this.fldPathsFile.setText( this.pathsFile.getAbsolutePath() );
      gbc.fill          = GridBagConstraints.HORIZONTAL;
      gbc.weightx       = 1.0;
      gbc.insets.top    = 0;
      gbc.insets.bottom = 5;
      gbc.gridwidth     = 1;
      gbc.gridy++;
      add( this.fldPathsFile, gbc );

      this.btnPathsFileDelete = new JButton( "L\u00F6schen" );
      this.btnPathsFileDelete.addActionListener( this );
      gbc.fill      = GridBagConstraints.NONE;
      gbc.weightx   = 0.0;
      gbc.gridwidth = 2;
      gbc.gridx++;
      add( this.btnPathsFileDelete, gbc );
    } else {
      this.fldPathsFile       = null;
      this.btnPathsFileDelete = null;
    }
    updDeleteLastPathsBtnEnabled();

    if( this.propsFile != null ) {
      gbc.insets.top    = 15;
      gbc.insets.bottom = 0;
      gbc.gridwidth     = 1;
      gbc.gridx         = 0;
      gbc.gridy++;
      add(
	new JLabel( "Einstellungen werden gespeichert in Datei:" ),
	gbc );

      this.fldPropsFile = new JTextField();
      this.fldPropsFile.setEditable( false );
      this.fldPropsFile.setText( this.propsFile.getAbsolutePath() );
      gbc.fill          = GridBagConstraints.HORIZONTAL;
      gbc.weightx       = 1.0;
      gbc.insets.top    = 0;
      gbc.insets.bottom = 5;
      gbc.gridwidth     = 1;
      gbc.gridy++;
      add( this.fldPropsFile, gbc );

      this.btnPropsFileDelete = new JButton( "L\u00F6schen" );
      if( !this.propsFile.exists() ) {
	this.btnPropsFileDelete.setEnabled( false );
      }
      this.btnPropsFileDelete.addActionListener( this );
      gbc.fill      = GridBagConstraints.NONE;
      gbc.weightx   = 0.0;
      gbc.gridwidth = 2;
      gbc.gridx++;
      add( this.btnPropsFileDelete, gbc );
    } else {
      this.fldPropsFile       = null;
      this.btnPropsFileDelete = null;
    }
    AppContext.setOnLastPathsSaved(
			new Runnable()
			{
			  @Override
			  public void run()
			  {
			    updDeleteLastPathsBtnEnabled();
			  }
			} );
  }


  public void applyInput( Properties props )
  {
    boolean rv = true;
    props.setProperty(
		AppContext.getPropPrefix() + FileDlg.PROP_FILEDIALOG,
		this.btnFileDlgNative.isSelected() ?
				FileDlg.VALUE_FILEDIALOG_NATIVE
				: FileDlg.VALUE_FILEDIALOG_JTCEMU );

    Object obj = this.spinnerMargin.getValue();
    props.setProperty(
		AppContext.getPropPrefix() + TopFrm.PROP_SCREEN_MARGIN,
		obj != null ? obj.toString() : "0" );

    obj = this.comboScreenRefresh.getSelectedItem();
    if( obj != null ) {
      String text = obj.toString();
      if( text != null ) {
	props.setProperty(
		AppContext.getPropPrefix() + TopFrm.PROP_SCREEN_REFRESH_MS,
		text );
      }
    }
  }


  public void propsFileSaved()
  {
    if( this.btnPropsFileDelete != null )
      this.btnPropsFileDelete.setEnabled( true );
  }


	/* --- ActionListener --- */

  @Override
  public void actionPerformed( ActionEvent e )
  {
    Object src = e.getSource();
    if( src != null ) {
      if( src == this.btnPathsFileDelete ) {
	doDeletePathsFile();
      }
      else if( src == this.btnPropsFileDelete ) {
	doDeletePropsFile();
      }
      else if( (src instanceof JRadioButton)
	       || (src instanceof JComboBox) )
      {
	this.settingsFrm.setDataChanged();
      }
    }
  }


	/* --- ChangeListener --- */

  @Override
  public void stateChanged( ChangeEvent e )
  {
    if( e.getSource() == this.spinnerMargin )
      this.settingsFrm.setDataChanged();
  }


	/* --- Aktionen --- */

  private void doDeletePathsFile()
  {
    if( this.pathsFile != null ) {
      if( JOptionPane.showConfirmDialog(
		this,
		"M\u00F6chten Sie die zuletzt verwendeten Dateipfade"
			+ " l\u00F6schen?\n"
			+ "Die Datei wird beim n\u00E4chsten"
			+ " Laden oder Speichern einer Datei"
			+ " automatisch wieder angelegt.",
		"Best\u00E4tigung",
		JOptionPane.YES_NO_OPTION ) == JOptionPane.YES_OPTION )
      {
	boolean deleted = this.pathsFile.delete();
	updDeleteLastPathsBtnEnabled();
	this.settingsFrm.propsFileDeleted();
	if( !deleted && this.pathsFile.exists() ) {
	  JOptionPane.showMessageDialog(
		this,
		"Die Datei mit den zuletzt verwendeten Dateipfaden\n"
			+ "konnte nicht gel\u00F6scht werden.",
		"Fehler",
		JOptionPane.ERROR_MESSAGE );
	}
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
			+ AppContext.getAppName() + " startet dann beim"
			+ " n\u00E4chsten mal mit Standardeinstellungen.",
		"Best\u00E4tigung",
		JOptionPane.YES_NO_OPTION ) == JOptionPane.YES_OPTION )
      {
	boolean deleted = this.propsFile.delete();
	if( this.btnPropsFileDelete != null ) {
	  this.btnPropsFileDelete.setEnabled( false );
	}
	this.settingsFrm.propsFileDeleted();
	if( !deleted && this.propsFile.exists() ) {
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


	/* --- private Methoden --- */

  private void updDeleteLastPathsBtnEnabled()
  {
    if( this.btnPathsFileDelete != null ) {
      boolean state = false;
      if( this.pathsFile != null ) {
	state = this.pathsFile.exists();
      }
      this.btnPathsFileDelete.setEnabled( state );
    }
  }


  private void updDeletePropsBtnEnabled()
  {
    if( this.btnPropsFileDelete != null ) {
      boolean state = false;
      if( this.propsFile != null ) {
	state = this.propsFile.exists();
      }
      this.btnPropsFileDelete.setEnabled( state );
    }
  }
}
