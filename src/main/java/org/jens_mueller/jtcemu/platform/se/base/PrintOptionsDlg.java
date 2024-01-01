/*
 * (c) 2007-2019 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Dialog zur Eingabe von Druckoptionen
 */

package jtcemu.platform.se.base;

import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import jtcemu.base.AppContext;


public class PrintOptionsDlg extends BaseDlg implements ActionListener
{
  private JComboBox<Integer> comboFontSize;
  private JButton            btnOK;
  private JButton            btnCancel;


  public static void open( Window owner )
  {
    (new PrintOptionsDlg( owner )).setVisible( true );
  }


	/* --- ActionListener --- */

  @Override
  public void actionPerformed( ActionEvent e )
  {
    Object src = e.getSource();
    if( src == this.btnOK ) {
      doApply();
    }
    else if( src == this.btnCancel ) {
      doClose();
    }
  }


	/* --- private Konstruktoren und Methoden --- */

  private PrintOptionsDlg( Window owner )
  {
    super( owner );
    setTitle( "Druckoptionen" );


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

    add( new JLabel( "Schriftgr\u00F6\u00DFe:" ), gbc );

    this.comboFontSize = new JComboBox<>();
    this.comboFontSize.setEditable( false );
    this.comboFontSize.addItem( 6 );
    this.comboFontSize.addItem( 7 );
    this.comboFontSize.addItem( 8 );
    this.comboFontSize.addItem( 9 );
    this.comboFontSize.addItem( 10 );
    this.comboFontSize.addItem( 11 );
    this.comboFontSize.addItem( 12 );
    this.comboFontSize.addItem( 14  );
    this.comboFontSize.addItem( 16  );
    this.comboFontSize.addItem( 18 );
    gbc.gridx++;
    add( this.comboFontSize, gbc );

    this.comboFontSize.setSelectedItem(
		AppContext.getIntProperty(
			AbstractTextFrm.PROP_PRINT_FONT_SIZE,
			AbstractTextFrm.DEFAULT_PRINT_FONT_SIZE ) );


    // Knoepfe
    JPanel panelBtn = new JPanel( new GridLayout( 1, 2, 5, 5 ) );
    gbc.insets.top = 10;
    gbc.gridwidth  = 2;
    gbc.gridx      = 0;
    gbc.gridy++;
    add( panelBtn,gbc );

    this.btnOK = new JButton( "OK" );
    this.btnOK.addActionListener( this );
    panelBtn.add( this.btnOK );

    this.btnCancel = new JButton( "Abbrechen" );
    this.btnCancel.addActionListener( this );
    panelBtn.add( this.btnCancel );


    // Fenstergroesse
    pack();
    setParentCentered();
    setResizable( true );
  }


  private void doApply()
  {
    Object obj = this.comboFontSize.getSelectedItem();
    if( obj != null ) {
      String text = obj.toString();
      if( text != null ) {
	AppContext.setProperty(
			AbstractTextFrm.PROP_PRINT_FONT_SIZE,
			text );
	doClose();
      }
    }
  }
}
