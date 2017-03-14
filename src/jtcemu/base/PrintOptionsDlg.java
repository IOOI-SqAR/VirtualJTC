/*
 * (c) 2007-2010 Jens Mueller
 * (c) 2017 Lars Sonchocky-Helldorf
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Dialog zur Eingabe von Druckoptionen
 */

package jtcemu.base;

import java.awt.*;
import java.awt.event.*;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.swing.*;
import jtcemu.Main;


public class PrintOptionsDlg extends BaseDlg implements ActionListener
{
  private static final Locale locale = Locale.getDefault();
  private static final ResourceBundle printOptionsDlgResourceBundle = ResourceBundle.getBundle("resources.PrintOptionsDlg", locale);

  private JComboBox comboFontSize;
  private JButton   btnOK;
  private JButton   btnCancel;


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
    setTitle( printOptionsDlgResourceBundle.getString("window.title") );


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

    add( new JLabel( printOptionsDlgResourceBundle.getString("label.fontsize") ), gbc );

    this.comboFontSize = new JComboBox();
    this.comboFontSize.setEditable( false );
    this.comboFontSize.addItem( "6" );
    this.comboFontSize.addItem( "7" );
    this.comboFontSize.addItem( "8" );
    this.comboFontSize.addItem( "9" );
    this.comboFontSize.addItem( "10" );
    this.comboFontSize.addItem( "11" );
    this.comboFontSize.addItem( "12" );
    this.comboFontSize.addItem( "14"  );
    this.comboFontSize.addItem( "16"  );
    this.comboFontSize.addItem( "18" );
    gbc.gridx++;
    add( this.comboFontSize, gbc );

    this.comboFontSize.setSelectedItem(
        Integer.toString(
                Main.getIntProperty( "jtcemu.print.font.size", 10 ) ) );


    // Knoepfe
    JPanel panelBtn = new JPanel( new GridLayout( 1, 2, 5, 5 ) );
    gbc.insets.top = 10;
    gbc.gridwidth  = 2;
    gbc.gridx      = 0;
    gbc.gridy++;
    add( panelBtn,gbc );

    this.btnOK = new JButton( printOptionsDlgResourceBundle.getString("button.ok") );
    this.btnOK.addActionListener( this );
    panelBtn.add( this.btnOK );

    this.btnCancel = new JButton( printOptionsDlgResourceBundle.getString("button.cancel") );
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
        Main.setProperty( "jtcemu.print.font.size", text );
        doClose();
      }
    }
  }
}
