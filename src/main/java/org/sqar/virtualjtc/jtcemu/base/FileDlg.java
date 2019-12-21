/*
 * (c) 2007-2010 Jens Mueller
 * (c) 2017 Lars Sonchocky-Helldorf
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Dialog fuer Auswahl einer Datei
 */

package org.sqar.virtualjtc.jtcemu.base;

import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.io.File;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import org.sqar.virtualjtc.jtcemu.Main;


public class FileDlg extends BaseDlg implements
                                        ActionListener,
                                        PropertyChangeListener
{
  private static final Locale locale = Locale.getDefault();
  private static final ResourceBundle fileDlgResourceBundle = ResourceBundle.getBundle("FileDlg", locale);

  private static final String defaultStatusText = fileDlgResourceBundle.getString("statusText.default");
  private static       Frame  defaultOwner      = null;

  private boolean      forSave;
  private File         selectedFile;
  private JFileChooser fileChooser;
  private JLabel       labelStatus;


  public static File showFileOpenDlg(
                        Window        owner,
                        String        title,
                        String        approveBtnText,
                        File          preSelection,
                        FileFilter... fileFilters )
  {
    File file = null;
    if( isNativeFileDialogEnabled() ) {
      file = openFileDialog( owner, title, FileDialog.LOAD, preSelection );
    } else {
      FileDlg dlg = new FileDlg(
                        owner,
                        false,
                        title,
                        approveBtnText,
                        preSelection,
                        fileFilters );
      dlg.setVisible( true );
      file = dlg.selectedFile;
    }
    return file;
  }


  public static File showFileSaveDlg(
                        Window        owner,
                        String        title,
                        File          preSelection,
                        FileFilter... fileFilters )
  {
    File file = null;
    if( isNativeFileDialogEnabled() ) {
      file = openFileDialog( owner, title, FileDialog.SAVE, preSelection );
    } else {
      FileDlg dlg = new FileDlg(
                        owner,
                        true,
                        title,
                        fileDlgResourceBundle.getString("dialog.showFileSaveDlg.approveBtnText"),
                        preSelection,
                        fileFilters );
      dlg.setVisible( true );
      file = dlg.selectedFile;
    }
    return file;
  }


        /* --- ActionListener --- */

  @Override
  public void actionPerformed( ActionEvent e )
  {
    if( e.getSource() == this.fileChooser ) {
      String actionCmd = e.getActionCommand();
      if( actionCmd != null ) {
        if( actionCmd.equals( JFileChooser.APPROVE_SELECTION ) ) {
          File file = this.fileChooser.getSelectedFile();
          if( this.forSave && (file != null) ) {
            if( file.exists() ) {
              if( JOptionPane.showConfirmDialog(
                        this,
                        fileDlgResourceBundle.getString("dialog.forSave.confirmOverwrite.message"),
                        fileDlgResourceBundle.getString("dialog.forSave.confirmOverwrite.title"),
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.WARNING_MESSAGE )
                                        != JOptionPane.OK_OPTION )
              {
                file = null;
              }
            }
          }
          if( file != null ) {
            this.selectedFile = file;
            doClose();
          }
        }
        else if( actionCmd.equals( JFileChooser.CANCEL_SELECTION ) ) {
          doClose();
        }
      }
    }
  }


        /* --- PropertyChangeListener --- */

  @Override
  public void propertyChange( PropertyChangeEvent e )
  {
    if( e.getSource() == this.fileChooser ) {
      String prop = e.getPropertyName();
      if( prop != null ) {
        if( prop.equals( JFileChooser.DIRECTORY_CHANGED_PROPERTY ) ) {
          this.labelStatus.setText( defaultStatusText );
        }
        else if( prop.equals( JFileChooser.SELECTED_FILE_CHANGED_PROPERTY ) ) {
          String statusText = defaultStatusText;
          Object value      = e.getNewValue();
          if( value != null ) {
            if( value instanceof File ) {
              File     file       = (File) value;
              FileInfo info       = FileInfo.analyzeFile( file );
              if( info != null ) {
                String infoText = info.getInfoText();
                if( infoText != null ) {
                  statusText = infoText;
                } else {
                  if( file != null ) {
                    String s = file.getName();
                    if( s != null ) {
                      int pos = s.lastIndexOf( '.' );
                      if( (pos >= 0) && ((pos + 1) < s.length()) ) {
                        statusText = String.format(
                                fileDlgResourceBundle.getString("statusText.formatstring"),
                                s.substring( pos + 1 ).toUpperCase() );
                      }
                    }
                  }
                }
              }
            }
          }
          this.labelStatus.setText( statusText );
        }
      }
    }
  }


        /* --- private Konstruktoren und Methoden --- */

  private FileDlg(
                Window        owner,
                boolean       forSave,
                String        title,
                String        approveBtnText,
                File          preSelection,
                FileFilter... fileFilters )
  {
    super( owner );
    setTitle( title );
    this.forSave      = forSave;
    this.selectedFile = null;


    // Fensterinhalt
    setLayout( new GridBagLayout() );

    GridBagConstraints gbc = new GridBagConstraints(
                                        0, 0,
                                        1, 1,
                                        1.0, 1.0,
                                        GridBagConstraints.CENTER,
                                        GridBagConstraints.BOTH,
                                        new Insets( 0, 0, 0, 0 ),
                                        0, 0 );

    this.fileChooser = new JFileChooser();
    if( preSelection != null ) {
      if( preSelection.exists() && preSelection.isDirectory() ) {
        this.fileChooser.setCurrentDirectory( preSelection );
      } else {
        this.fileChooser.setSelectedFile( preSelection );
      }
    }
    this.fileChooser.setAcceptAllFileFilterUsed( true );
    if( fileFilters != null ) {
      if( fileFilters.length == 1 ) {
        this.fileChooser.addChoosableFileFilter( fileFilters[ 0 ] );
        this.fileChooser.setFileFilter( fileFilters[ 0 ] );
      }
      else if( fileFilters.length > 1 ) {
        FileFilter aaff = this.fileChooser.getAcceptAllFileFilter();
        for( int i = 0; i < fileFilters.length; i++ ) {
          this.fileChooser.addChoosableFileFilter( fileFilters[ i ] );
        }
        if( aaff != null ) {
          this.fileChooser.setFileFilter( aaff );
        }
      }
    }
    this.fileChooser.setApproveButtonText( approveBtnText );
    this.fileChooser.setControlButtonsAreShown( true );
    this.fileChooser.setDialogType( JFileChooser.CUSTOM_DIALOG );
    this.fileChooser.setFileSelectionMode( JFileChooser.FILES_ONLY );
    this.fileChooser.setMultiSelectionEnabled( false );
    this.fileChooser.addActionListener( this );
    this.fileChooser.addPropertyChangeListener( this );
    add( this.fileChooser, gbc );


    // Statuszeile
    gbc.fill    = GridBagConstraints.HORIZONTAL;
    gbc.weighty = 0.0;
    gbc.gridy++;
    add( new JSeparator(), gbc );

    this.labelStatus = new JLabel( defaultStatusText );
    gbc.anchor = GridBagConstraints.WEST;
    gbc.insets.top    = 3;
    gbc.insets.left   = 5;
    gbc.insets.bottom = 1;
    gbc.gridy++;
    add( this.labelStatus, gbc );


    // sonstiges
    pack();
    setResizable( true );
    setParentCentered();
  }


  private static boolean isNativeFileDialogEnabled()
  {
    boolean rv = false;
    String  s  = Main.getProperty( "org.sqar.virtualjtc.jtcemu.filedialog" );
    if( s != null ) {
      rv = s.equals( "native" );
    }
    return rv;
  }


  private static File openFileDialog(
                                Window owner,
                                String title,
                                int    mode,
                                File   preSelection )
  {
    FileDialog dlg = null;
    if( owner instanceof Dialog ) {
      dlg = new FileDialog( (Dialog) owner, title, mode );
    } else if( owner instanceof Frame ) {
      dlg = new FileDialog( (Frame) owner, title, mode );
    } else {
      if( defaultOwner == null ) {
        defaultOwner = new Frame();
      }
      dlg = new FileDialog( defaultOwner, title, mode );
    }
    dlg.setModalityType( Dialog.ModalityType.DOCUMENT_MODAL );
    if( preSelection != null ) {
      if( preSelection.isDirectory() ) {
        dlg.setDirectory( preSelection.getPath() );
      } else {
        String dirName = preSelection.getParent();
        if( dirName != null ) {
          if( !dirName.isEmpty() ) {
            dlg.setDirectory( dirName );
          }
        }
        String fileName = preSelection.getName();
        if( fileName != null ) {
          if( !fileName.isEmpty() ) {
            dlg.setFile( fileName );
          }
        }
      }
    }
    dlg.setVisible( true );
    File   rv = null;
    String s  = dlg.getFile();
    if( s != null ) {
      if( !s.isEmpty() ) {
        rv = new File( s );
      }
    }
    return rv;
  }
}

