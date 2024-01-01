/*
 * (c) 2020 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Einstellungen fuer das Erscheinungsbild
 */

package org.jens_mueller.jtcemu.platform.se.settings;

import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Properties;
import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import org.jens_mueller.jtcemu.base.AppContext;
import org.jens_mueller.jtcemu.platform.se.Main;
import org.jens_mueller.jtcemu.platform.se.base.BaseFrm;


public class LAFSettingsFld extends JPanel implements ActionListener
{
  private SettingsFrm                 settingsFrm;
  private int                         lafCnt;
  private ButtonGroup                 grpLAF;
  private UIManager.LookAndFeelInfo[] lafs;


  public LAFSettingsFld( SettingsFrm settingsFrm )
  {
    this.settingsFrm = settingsFrm;
    this.lafCnt      = 0;
    setLayout( new GridBagLayout() );

    GridBagConstraints gbc = new GridBagConstraints(
						0, 0,
						1, 1,
						0.0, 0.0,
						GridBagConstraints.WEST,
						GridBagConstraints.NONE,
						new Insets( 5, 5, 0, 5 ),
						0, 0 );

    this.grpLAF = new ButtonGroup();
    this.lafs   = UIManager.getInstalledLookAndFeels();
    if( this.lafs != null ) {
      if( this.lafs.length > 1 ) {
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
	    gbc.insets.bottom = 5;
	  }
	  add( btn, gbc );
	  this.lafCnt++;
	  gbc.insets.top = 0;
	  gbc.gridy++;
	}
      }
    }
  }


  public int getNumSelectableLAFs()
  {
    return this.lafCnt;
  }


  public void applyInput( Properties props )
				throws UnsupportedLookAndFeelException
  {
    ButtonModel bm = this.grpLAF.getSelection();
    if( bm != null ) {
      String lafClassName = bm.getActionCommand();
      if( lafClassName != null ) {
	if( !lafClassName.isEmpty() ) {
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
	      EventQueue.invokeLater(
			new Runnable()
			{
			  @Override
			  public void run()
			  {
			    informLAFChanged();
			  }
			} );
	      props.setProperty(
			AppContext.getPropPrefix() + Main.PROP_LAF_CLASSNAME,
			lafClassName );
	    }
	    catch( Exception ex ) {
	      throw new UnsupportedLookAndFeelException(
				"Das Erscheinungsbild kann nicht"
					+ " eingestellt werden." );
	    }
	  }
	}
      }
    }
  }


	/* --- ActionListener --- */

  @Override
  public void actionPerformed( ActionEvent e )
  {
    Object src = e.getSource();
    if( src != null ) {
      if( src instanceof JRadioButton ) {
	this.settingsFrm.setDataChanged();
      }
    }
  }


	/* --- private Methoden --- */

  private void informLAFChanged()
  {
    for( Frame frame : Frame.getFrames() ) {
      SwingUtilities.updateComponentTreeUI( frame );
      if( frame instanceof BaseFrm ) {
	((BaseFrm) frame).lafChanged();
      }
    }
  }
}
