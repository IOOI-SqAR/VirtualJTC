/*
 * (c) 2007-2010 Jens Mueller
 * (c) 2017 Lars Sonchocky-Helldorf
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Dialog fuer Suchen & Ersetzen
 */

package jtcemu.tools;

import java.awt.*;
import java.awt.event.*;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.regex.*;
import javax.swing.*;
import jtcemu.base.*;
import jtcemu.Main;


public class FindAndReplaceDlg extends BaseDlg implements ActionListener
{
  private static final Locale locale = Locale.getDefault();
  private static final ResourceBundle findAndReplaceDlgResourceBundle = ResourceBundle.getBundle("resources.FindAndReplaceDlg", locale);

  private Pattern    pattern;
  private String     findText;
  private String     replaceText;
  private boolean    replaceAll;
  private boolean    caseSensitive;
  private boolean    regularExpr;
  private JTextField fldFind;
  private JTextField fldReplace;
  private JCheckBox  btnCaseSensitive;
  private JCheckBox  btnRegularExpr;
  private JButton    btnFind;
  private JButton    btnReplaceAll;
  private JButton    btnCancel;


  public FindAndReplaceDlg(
                        Window  owner,
                        String  findText,
                        String  replaceText,
                        boolean caseSensitive,
                        boolean regularExpr )
  {
    super( owner );
    
    setTitle( findAndReplaceDlgResourceBundle.getString("window.title") );
    
    this.pattern       = null;
    this.findText      = null;
    this.replaceText   = null;
    this.replaceAll    = false;
    this.caseSensitive = false;
    this.regularExpr   = false;


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


    // Eingabefelder
    JPanel panelInput = new JPanel( new GridBagLayout() );
    add( panelInput, gbc );

    GridBagConstraints gbcInput = new GridBagConstraints(
                                        0, 0,
                                        1, 1,
                                        0.0, 0.0,
                                        GridBagConstraints.NORTHEAST,
                                        GridBagConstraints.NONE,
                                        new Insets( 5, 0, 5, 5 ),
                                        0, 0 );

    panelInput.add( new JLabel( findAndReplaceDlgResourceBundle.getString("label.panelInput.searchFor") ), gbcInput );
    gbcInput.insets.top = 5;
    gbcInput.gridy++;
    panelInput.add( new JLabel( findAndReplaceDlgResourceBundle.getString("label.panelInput.replaceBy") ), gbcInput );

    this.fldFind = new JTextField();
    if( findText != null ) {
      this.fldFind.setText( findText );
    }
    this.fldFind.addActionListener( this );
    gbcInput.anchor     = GridBagConstraints.NORTHWEST;
    gbcInput.fill       = GridBagConstraints.HORIZONTAL;
    gbcInput.insets.top = 0;
    gbcInput.weightx    = 1.0;
    gbcInput.gridy      = 0;
    gbcInput.gridx++;
    panelInput.add( this.fldFind, gbcInput );

    this.fldReplace = new JTextField();
    if( replaceText != null ) {
      this.fldReplace.setText( replaceText );
    }
    this.fldReplace.addActionListener( this );
    gbcInput.gridy++;
    panelInput.add( this.fldReplace, gbcInput );

    this.btnCaseSensitive = new JCheckBox(
                                        findAndReplaceDlgResourceBundle.getString("button.caseSensitive"),
                                        caseSensitive );
    gbcInput.fill          = GridBagConstraints.NONE;
    gbcInput.weightx       = 0.0;
    gbcInput.insets.bottom = 0;
    gbcInput.gridy++;
    panelInput.add( this.btnCaseSensitive, gbcInput );

    this.btnRegularExpr = new JCheckBox(
                                findAndReplaceDlgResourceBundle.getString("button.regularExpr"),
                                regularExpr );
    gbcInput.insets.top = 0;
    gbcInput.gridy++;
    panelInput.add( this.btnRegularExpr, gbcInput );


    // Knoepfe
    JPanel panelBtn = new JPanel( new GridLayout( 3, 1, 5, 5 ) );
    gbc.fill    = GridBagConstraints.NONE;
    gbc.weightx = 0.0;
    gbc.gridx++;
    add( panelBtn,gbc );

    this.btnFind = new JButton( findAndReplaceDlgResourceBundle.getString("button.find") );
    this.btnFind.addActionListener( this );
    panelBtn.add( this.btnFind );

    this.btnReplaceAll = new JButton( findAndReplaceDlgResourceBundle.getString("button.replaceAll") );
    this.btnReplaceAll.addActionListener( this );
    panelBtn.add( this.btnReplaceAll );

    this.btnCancel = new JButton( findAndReplaceDlgResourceBundle.getString("button.cancel") );
    this.btnCancel.addActionListener( this );
    panelBtn.add( this.btnCancel );


    // Fenstergroesse
    pack();
    setParentCentered();
    setResizable( true );
  }


  public boolean getCaseSensitive()
  {
    return this.caseSensitive;
  }


  public String getFindText()
  {
    return this.findText;
  }


  public Pattern getPattern()
  {
    return this.pattern;
  }


  public boolean getRegularExpr()
  {
    return this.regularExpr;
  }


  public boolean getReplaceAll()
  {
    return this.replaceAll;
  }


  public String getReplaceText()
  {
    return this.replaceText;
  }


        /* --- ActionEvent --- */

  @Override
  public void actionPerformed( ActionEvent e )
  {
    Object src = e.getSource();
    if( src == this.fldFind ) {
      this.fldReplace.requestFocus();
    }
    else if( (src == this.fldReplace) || (src == this.btnFind) ) {
      if( doApply() )
        doClose();
    }
    else if( src == this.btnReplaceAll ) {
      if( doApply() ) {
        this.replaceAll = true;
        doClose();
      }
    }
    else if( src == this.btnCancel ) {
      doClose();
    }
  }


        /* --- private Methoden --- */

  private boolean doApply()
  {
    boolean rv   = false;
    String  text = this.fldFind.getText();
    if( text == null ) {
      text = "";
    }
    if( text.length() > 0 ) {
      try {
        this.caseSensitive = this.btnCaseSensitive.isSelected();
        this.regularExpr   = this.btnRegularExpr.isSelected();

        int flags = Pattern.MULTILINE | Pattern.UNICODE_CASE;
        if( !this.caseSensitive ) {
          flags |= Pattern.CASE_INSENSITIVE;
        }
        if( !this.regularExpr ) {
          flags |= Pattern.LITERAL;
        }
        this.pattern     = Pattern.compile( text, flags );
        this.findText    = text;
        this.replaceText = this.fldReplace.getText();
        rv = true;
      }
      catch( PatternSyntaxException ex ) {
        Main.showError( this, ex );
      }
    } else {
      Main.showError( this, findAndReplaceDlgResourceBundle.getString("error.doApply.inputExpected.errorText") );
    }
    return rv;
  }
}
