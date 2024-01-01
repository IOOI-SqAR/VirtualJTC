/*
 * (c) 2007-2021 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Dialog fuer Auswahl einer Datei
 */

package org.jens_mueller.jtcemu.platform.se.base;

import java.awt.Dialog;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JSeparator;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.jens_mueller.jtcemu.base.AppContext;
import org.jens_mueller.jtcemu.base.FileInfo;


public class FileDlg extends BaseDlg implements
					ActionListener,
					PropertyChangeListener
{
  public static final String PROP_FILEDIALOG         = "filedialog";
  public static final String VALUE_FILEDIALOG_NATIVE = "native";
  public static final String VALUE_FILEDIALOG_JTCEMU = "jtcemu";

  private static final String defaultStatusText = "Bereit";
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
      file = openNativeFileDialog(
				owner,
				title,
				FileDialog.LOAD,
				preSelection );
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
      file = openNativeFileDialog(
				owner,
				title,
				FileDialog.SAVE,
				preSelection );
    } else {
      FileDlg dlg = new FileDlg(
				owner,
				true,
				title,
				"Speichern",
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
	    file = checkAppendFileExtension(
				file,
				this.fileChooser.getFileFilter() );
	    if( file.exists() ) {
	      if( JOptionPane.showConfirmDialog(
			this,
			"Die Datei existiert bereits und wird"
				+ " \u00FCberschrieben!",
			"Warnung",
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
        else if( prop.equals( JFileChooser.SELECTED_FILE_CHANGED_PROPERTY ) )
	{
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
				"%s-Datei",
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


	/* --- Konstruktor --- */

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
      if( forSave ) {
	if( preSelection.exists() && preSelection.isDirectory() ) {
	  this.fileChooser.setCurrentDirectory( preSelection );
	} else {
	  this.fileChooser.setSelectedFile( preSelection );
	}
      } else {
	while( preSelection != null ) {
	  if( preSelection.exists() && preSelection.isDirectory() ) {
	    this.fileChooser.setCurrentDirectory( preSelection );
	    break;
	  }
	  preSelection = preSelection.getParentFile();
	}
      }
    }
    this.fileChooser.setAcceptAllFileFilterUsed( true );
    if( fileFilters != null ) {
      FileFilter filterToSelect = null;
      int        nFilters       = 0;
      for( FileFilter filter : fileFilters ) {
	if( filter != null ) {
	  this.fileChooser.addChoosableFileFilter( filter );
	  filterToSelect = filter;
	  nFilters++;
	}
      }
      if( (filterToSelect != null) && (nFilters == 1) ) {
        this.fileChooser.setFileFilter( filterToSelect );
      } else {
        FileFilter aaff = this.fileChooser.getAcceptAllFileFilter();
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


	/* --- private Methoden --- */

  private File checkAppendFileExtension(
				File       file,
				FileFilter filter )
  {
    String fileName = file.getName();
    if( (fileName != null) && (filter != null) ) {
      int idx = fileName.lastIndexOf( '.' );
      if( ((idx < 0) || (idx == (fileName.length() - 1)))
	  && (filter instanceof FileNameExtensionFilter) )
      {
	String[] e = ((FileNameExtensionFilter) filter).getExtensions();
	if( e != null ) {
	  if( e.length == 1 ) {
	    if( idx < 0 ) {
	      fileName += ".";
	    }
	    fileName += e[ 0 ];
	    File dirFile = file.getParentFile();
	    if( dirFile != null ) {
	      file = new File( dirFile, fileName );
	    } else {
	      file = new File( fileName );
	    }
	  }
	}
      }
    }
    return file;
  }


  private static boolean isNativeFileDialogEnabled()
  {
    boolean rv = false;
    String  s  = AppContext.getProperty( PROP_FILEDIALOG );
    if( s != null ) {
      rv = s.equals( VALUE_FILEDIALOG_NATIVE );
    }
    return rv;
  }


  private static File openNativeFileDialog(
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
    File[] files = dlg.getFiles();
    if( files != null ) {
      if( files.length > 0 ) {
	rv = files[ 0 ];
      }
    }
    return rv;
  }
}
