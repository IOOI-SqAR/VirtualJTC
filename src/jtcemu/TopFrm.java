/*
 * (c) 2007-2011 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Hauptfenster
 */

package jtcemu;

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.dnd.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.*;
import java.util.*;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import jtcemu.audio.AudioFrm;
import jtcemu.base.*;
import jtcemu.tools.*;
import jtcemu.tools.hexedit.*;
import z8.*;


public class TopFrm extends BaseFrm
                        implements
                                ActionListener,
                                DropTargetListener
{
  private static final int[] screenScales = { 1, 2, 3, 4, 6, 8 };

  private JTCSys                            jtcSys;
  private Z8                                z8;
  private Map<Integer,JRadioButtonMenuItem> scale2MenuItems;
  private Map<ScreenFld.Mode,Integer>       mode2Scale;
  private Thread                            emuThread;
  private ScreenFld                         screenFld;
  private javax.swing.Timer                 screenRefreshTimer;
  private boolean                           screenOutputEnabled;


  public TopFrm( JTCSys jtcSys, Z8 z8 )
  {
    setTitle( "JU+TE-Computer" );
    this.jtcSys              = jtcSys;
    this.z8                  = z8;
    this.screenOutputEnabled = false;
    this.scale2MenuItems     = new Hashtable<Integer,JRadioButtonMenuItem>();
    this.mode2Scale          = new Hashtable<ScreenFld.Mode,Integer>();
    this.mode2Scale.put( ScreenFld.Mode.M64X64, new Integer( 3 ) );
    this.mode2Scale.put( ScreenFld.Mode.M128X128, new Integer( 2 ) );
    this.mode2Scale.put( ScreenFld.Mode.M320X192, new Integer( 1 ) );


    // Menu
    JMenuBar mnuBar = new JMenuBar();
    setJMenuBar( mnuBar );


    // Menu Datei
    JMenu mnuFile = new JMenu( "Datei" );
    mnuFile.setMnemonic( 'D' );
    mnuFile.add( createJMenuItem( "Laden...", KeyEvent.VK_F9, "load" ) );
    mnuFile.add( createJMenuItem( "Speichern...", KeyEvent.VK_F8, "save" ) );
    mnuFile.addSeparator();
    mnuFile.add( createJMenuItem( "Texteditor...", "edit" ) );
    mnuFile.addSeparator();
    mnuFile.add( createJMenuItem( "Beenden", "quit" ) );
    mnuBar.add( mnuFile );


    // Menu Extra
    JMenu mnuExtra = new JMenu( "Extra" );
    mnuExtra.setMnemonic( 'E' );

    JMenu mnuScreenScale = new JMenu( "Ansicht" );
    mnuExtra.add( mnuScreenScale );

    int         screenScale = Main.getIntProperty( "jtcemu.screen.scale", 3 );
    ButtonGroup grpScale    = new ButtonGroup();
    for( int i = 0; i < screenScales.length; i++ ) {
      int                  v    = screenScales[ i ];
      String               text = Integer.toString( v );
      JRadioButtonMenuItem item = new JRadioButtonMenuItem(
                                                        text + "00 %",
                                                        v == screenScale );
      item.setActionCommand( "screen.scale." + text );
      item.addActionListener( this );
      grpScale.add( item );
      mnuScreenScale.add( item );
      this.scale2MenuItems.put( new Integer( v ), item );
    }

    JMenu mnuScreenImg = new JMenu( "Bildschirmausgabe" );
    mnuExtra.add( mnuScreenImg );
    mnuExtra.addSeparator();

    mnuScreenImg.add( createJMenuItem(
                                "Als Text kopieren",
                                "screen.text.copy" ) );
    mnuScreenImg.add( createJMenuItem(
                                "Als Bild kopieren", "screen.image.copy" ) );
    mnuScreenImg.add( createJMenuItem(
                                "Als Bild speichern unter...",
                                "screen.image.save_as" ) );

    mnuExtra.add( createJMenuItem( "Debugger...", "debug" ) );
    mnuExtra.add( createJMenuItem( "Reassembler...", "reass" ) );
    mnuExtra.add( createJMenuItem( "Speichereditor...", "memedit" ) );
    mnuExtra.add( createJMenuItem( "Hex-Editor...", "hexedit" ) );
    mnuExtra.addSeparator();
    mnuExtra.add( createJMenuItem( "Audio/Kassette...", "audio" ) );
    mnuExtra.add( createJMenuItem( "Tastatur...", "keyboard" ) );
    mnuExtra.add( createJMenuItem( "Einstellungen...", "settings" ) );
    mnuExtra.addSeparator();
    mnuExtra.add( createJMenuItem(
                                "Zur\u00FCcksetzen (RESET)",
                                KeyEvent.VK_F7,
                                "reset" ) );
    mnuExtra.add( createJMenuItem(
                                "Einschalten/Initialisieren",
                                "power_on" ) );
    mnuBar.add( mnuExtra );


    // Menu Hilfe
    JMenu mnuHelp = new JMenu( "?" );
    mnuHelp.add( createJMenuItem( "Hilfe...", "help" ) );
    mnuHelp.addSeparator();
    mnuHelp.add( createJMenuItem( "\u00DCber JTCEMU...", "about" ) );
    mnuHelp.add( createJMenuItem( "Lizenzbestimmungen...", "license" ) );
    mnuHelp.add( createJMenuItem( "Danksagung...", "thanks" ) );
    mnuBar.add( mnuHelp );


    // Fensterinhalt
    setLayout( new BorderLayout( 5, 5 ) );


    // Werkzeugleiste
    JPanel panelToolBar = new JPanel(
                new FlowLayout( FlowLayout.LEFT, 0, 0 ) );
    add( panelToolBar, BorderLayout.NORTH );

    JToolBar toolBar = new JToolBar();
    toolBar.setFloatable( false );
    toolBar.setBorderPainted( false );
    toolBar.setOrientation( JToolBar.HORIZONTAL );
    toolBar.setRollover( true );
    panelToolBar.add( toolBar );

    toolBar.add( GUIUtil.createImageButton(
                                this,
                                "/images/file/open.png",
                                "Laden",
                                "load" ) );

    toolBar.add( GUIUtil.createImageButton(
                                this,
                                "/images/file/save.png",
                                "Speichern",
                                "save" ) );
    toolBar.addSeparator();

    toolBar.add( GUIUtil.createImageButton(
                                this,
                                "/images/file/edit.png",
                                "Texteditor",
                                "edit" ) );
    toolBar.add( GUIUtil.createImageButton(
                                this,
                                "/images/file/audio.png",
                                "Audio/Kassette",
                                "audio" ) );

    toolBar.addSeparator();

    toolBar.add( GUIUtil.createImageButton(
                                this,
                                "/images/file/reset.png",
                                "Zur\u00FCcksetzen (RESET)",
                                "reset" ) );


    // Bildschirmausgabe
    this.screenFld = new ScreenFld( this.jtcSys, screenScale );
    this.screenFld.addKeyListener( this.jtcSys );
    add( this.screenFld, BorderLayout.CENTER );

    this.mode2Scale.put(
                this.screenFld.getMode(),
                new Integer( screenScale ) );


    // Bildschirmausgabe zyklisch aktualisieren
    this.screenRefreshTimer = new javax.swing.Timer(
                                        getSettingsScreenRefreshMillis(),
                                        this );
    this.screenRefreshTimer.start();


    // Fenstergroesse und -position
    updScreenSize( false );
    setResizable( true );
    if( !GUIUtil.applyWindowSettings( this ) ) {
      pack();
      setLocationByPlatform( true );
    }


    // Drop-Ziel
    (new DropTarget( this.screenFld, this )).setActive( true );


    // Emulation starten
    this.emuThread = new Thread( this.z8 );
    this.emuThread.start();
  }


  public void doPowerOn()
  {
    boolean state = true;
    if( Main.getBooleanProperty( "jtcemu.confirm.init", true ) ) {
      if( JOptionPane.showConfirmDialog(
                this,
                "M\u00F6chten Sie das Aus- und wieder Einschalten emulieren\n"
                        + "und den Emulator initialisieren?\n"
                        + "Dabei wird der Arbeitsspeicher gel\u00F6scht.",
                "Best\u00E4tigung",
                JOptionPane.YES_NO_OPTION ) != JOptionPane.YES_OPTION )
      {
        state = false;
      }
    }
    if( state ) {
      resetEmu( true );
    }
  }


  public void doReset()
  {
    boolean state = true;
    if( Main.getBooleanProperty( "jtcemu.confirm.reset", true ) ) {
      String msg = "M\u00F6chten Sie jetzt ein RESET ausl\u00F6sen\n"
                        + "und den Emulator zur\u00FCcksetzen?";
      if( (this.jtcSys.getOSType() == JTCSys.OSType.OS2K)
          || (this.jtcSys.getOSType() == JTCSys.OSType.ES1988) )
      {
        msg += "\nWenn der A-Cursor zu sehen ist,\n"
                        + "sollten Sie kein RESET ausl\u00F6sen!";
      }
      if( JOptionPane.showConfirmDialog(
                this,
                msg,
                "Best\u00E4tigung",
                JOptionPane.YES_NO_OPTION ) != JOptionPane.YES_OPTION )
      {
        state = false;
      }
    }
    if( state )
      resetEmu( false );
  }


  public int getScreenRefreshMillis()
  {
    return this.screenRefreshTimer.getDelay();
  }


        /* --- ActionListener --- */

  @Override
  public void actionPerformed( ActionEvent e )
  {
    if( e.getSource() == this.screenRefreshTimer ) {
      boolean screenOutputEnabled = this.jtcSys.isScreenOutputEnabled();
      if( (screenOutputEnabled != this.screenOutputEnabled)
          || this.screenFld.isDirty() )
      {
        this.screenFld.repaint();
        this.screenOutputEnabled = screenOutputEnabled;
      }
    } else {
      String actionCmd = e.getActionCommand();
      if( actionCmd != null ) {
        if( actionCmd.equals( "quit" ) ) {
          doQuit();
        } else {
          GUIUtil.setWaitCursor( this, true );
          try {
            if( actionCmd.equals( "load" ) ) {
              File file = FileDlg.showFileOpenDlg(
                                        this,
                                        "Datei laden",
                                        "Laden...",
                                        Main.getLastPathFile(),
                                        GUIUtil.binaryFileFilter,
                                        GUIUtil.hexFileFilter,
                                        GUIUtil.jtcFileFilter,
                                        GUIUtil.tapFileFilter );
              if( file != null )
                loadFile( file );
            }
            else if( actionCmd.equals( "save" ) ) {
              (new SaveDlg( this, this.jtcSys )).setVisible( true );
            }
            else if( actionCmd.equals( "edit" ) ) {
              TextEditFrm.open( this.jtcSys );
            }
            else if( actionCmd.equals( "screen.text.copy" ) ) {
              doScreenTextCopy();
            }
            else if( actionCmd.equals( "screen.image.copy" ) ) {
              doScreenImageCopy();
            }
            else if( actionCmd.equals( "screen.image.save_as" ) ) {
              doScreenImageSaveAs();
            }
            else if( actionCmd.equals( "debug" ) ) {
              DebugFrm.open( this.z8 );
            }
            else if( actionCmd.equals( "reass" ) ) {
              ReassFrm.open( this.jtcSys );
            }
            else if( actionCmd.equals( "memedit" ) ) {
              MemEditFrm.open( this.jtcSys );
            }
            else if( actionCmd.equals( "hexedit" ) ) {
              HexEditFrm.open();
            }
            else if( actionCmd.equals( "audio" ) ) {
              AudioFrm.open( this.z8, this.jtcSys, this.emuThread );
            }
            else if( actionCmd.equals( "keyboard" ) ) {
              doKeyboardFrm();
            }
            else if( actionCmd.equals( "settings" ) ) {
              SettingsFrm.open( this, this.jtcSys );
            }
            else if( actionCmd.equals( "power_on" ) ) {
              doPowerOn();
            }
            else if( actionCmd.equals( "reset" ) ) {
              doReset();
            }
            else if( actionCmd.equals( "help" ) ) {
              HelpFrm.open( null );
            }
            else if( actionCmd.equals( "about" ) ) {
              doAbout();
            }
            else if( actionCmd.equals( "license" ) ) {
              HelpFrm.open( "/help/license.htm" );
            }
            else if( actionCmd.equals( "thanks" ) ) {
              HelpFrm.open( "/help/thanks.htm" );
            }
            else if( actionCmd.startsWith( "screen.scale." ) ) {
              if( actionCmd.length() > 13 ) {
                try {
                  int scale = Integer.parseInt( actionCmd.substring( 13 ) );
                  this.screenFld.setScreenScale( scale );
                  this.mode2Scale.put(
                                this.screenFld.getMode(),
                                new Integer( scale ) );
                  pack();
                }
                catch( NumberFormatException ex ) {}
              }
            }
          }
          catch( Exception ex ) {
            Main.showError( this, ex );
          }
          GUIUtil.setWaitCursor( this, false );
        }
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
    if( GUIUtil.isFileDrop( e ) ) {
      e.acceptDrop( DnDConstants.ACTION_COPY );    // Quelle nicht loeschen
      Transferable t = e.getTransferable();
      if( t != null ) {
        try {
          Object o = t.getTransferData( DataFlavor.javaFileListFlavor );
          if( o != null ) {
            if( o instanceof Collection ) {
              Iterator iter = ((Collection) o).iterator();
              if( iter != null ) {
                if( iter.hasNext() ) {
                  o = iter.next();
                  if( o != null ) {
                    File file = null;
                    if( o instanceof File ) {
                      file = (File) o;
                    }
                    else if( o instanceof String ) {
                      file = new File( o.toString() );
                    }
                    if( file != null )
                      loadFile( file );
                  }
                }
              }
            }
          }
        }
        catch( Exception ex ) {}
      }
      e.dropComplete( true );
    } else {
      e.rejectDrop();
    }
  }


  @Override
  public void dropActionChanged( DropTargetDragEvent e )
  {
    if( !GUIUtil.isFileDrop( e ) )
      e.rejectDrag();
  }


        /* --- ueberschriebene Methoden  --- */

  @Override
  public void lafChanged()
  {
    pack();
  }


  @Override
  public void prepareSettingsToSave()
  {
    Main.setProperty(
        "jtcemu.screen.scale",
        Integer.toString( this.screenFld.getScreenScale() ) );
  }


  @Override
  public void settingsChanged()
  {
    this.screenRefreshTimer.setDelay( getSettingsScreenRefreshMillis() );
    if( ((this.jtcSys.getOSType() != JTCSys.OSType.OS2K)
                && (this.jtcSys.getOSType() != JTCSys.OSType.ES1988))
        || this.jtcSys.getEmulateKeyboardSyscall() )
    {
      KeyboardFrm.close();
    }
    if( updScreenSize( true ) ) {
      pack();
    }
    JRadioButtonMenuItem item = this.scale2MenuItems.get(
                        new Integer( this.screenFld.getScreenScale() ) );
    if( item != null ) {
      item.setSelected( true );
    }
  }


  @Override
  public void windowClosing( WindowEvent e )
  {
    doQuit();
  }


  @Override
  public void windowOpened( WindowEvent e )
  {
    this.screenFld.requestFocus();
  }


        /* --- Aktionen --- */

  private void doAbout()
  {
    JOptionPane.showMessageDialog(
        this,
        "JTCEMU Version 1.1.1\n"
                + "Emulation des JU+TE-Computers\n"
                + "\n"
                + "(c) 2007-2011 Jens M\u00FCller\n"
                + "Lizenz: GNU General Public License Version 3\n"
                + "\n"
                + "Benutzung des Programms erfolgt auf eigene Gefahr!\n"
                + "Jegliche Haftung und Gew\u00E4hrleistung"
                + " ist ausgeschlossen!\n"
                + "\n"
                + "Der Emulator enth\u00E4lt ROM-Images der Betriebssysteme,\n"
                + "die mit der Bauanleitung des JU+TE-Computers"
                + " ver\u00F6ffentlicht\n"
                + "bzw. von der Redaktion Jugend+Technik\n"
                + "zur Verf\u00FCgung gestellt wurden.\n"
                + "Die Urheberschaften an diesen ROM-Images liegen bei:\n"
                + "\n"
                + "2K-Betriebssystem und 4K-System EMR-ES 1988:\n"
                + "(c) 1987-1988 Dr. Helmut Hoyer\n"
                + "\n"
                + "4K-Betriebssystem ES 2.3 und 6K-System ES 4.0:\n"
                + "(c) 1989-1990 Harun Scheutzow",
        "\u00DCber...",
        JOptionPane.INFORMATION_MESSAGE );
  }


  private void doKeyboardFrm()
  {
    if( ((this.jtcSys.getOSType() == JTCSys.OSType.OS2K)
                || (this.jtcSys.getOSType() == JTCSys.OSType.ES1988))
        && !this.jtcSys.getEmulateKeyboardSyscall() )
    {
      KeyboardFrm.open( this, this.jtcSys );
    } else {
      JOptionPane.showMessageDialog(
                this,
                "Das Fenster mit der originalen Tastaturmatrix"
                        + " steht nur zur Verf\u00FCgung,\n"
                        + "wenn das 2K-Betriebssystem oder das EMR-ES 1988"
                        + " aktiv ist und\n"
                        + "die Systemroutine f\u00FCr die Tastaturabfrage"
                        + " nicht emuliert wird\n"
                        + "(siehe Einstellungen, Bereich System).",
                "Hinweis",
                JOptionPane.INFORMATION_MESSAGE );
    }
  }


  private void doQuit()
  {
    boolean state = true;
    if( Main.getBooleanProperty( "jtcemu.confirm.quit", true ) ) {
      if( JOptionPane.showConfirmDialog(
                this,
                "M\u00F6chten Sie den Emulator beenden?",
                "Best\u00E4tigung",
                JOptionPane.YES_NO_OPTION ) != JOptionPane.YES_OPTION )
      {
        state = false;
      }
    }
    if( state ) {
      state = TextEditFrm.close();
    }
    if( state ) {
      state = HexEditFrm.close();
    }
    if( state ) {
      this.screenRefreshTimer.stop();
      this.z8.fireQuit();
      AudioFrm.quit();
      DebugFrm.close();
      HelpFrm.close();
      KeyboardFrm.close();
      MemEditFrm.close();
      ReassFrm.close();
      SettingsFrm.close();
      try {
        this.emuThread.join( 500 );
      }
      catch( InterruptedException ex ) {}
      doClose();
      System.exit( 0 );
    }
  }


  private void doScreenImageCopy()
  {
    try {
      Toolkit tk = getToolkit();
      if( tk != null ) {
        Clipboard clp = tk.getSystemClipboard();
        if( clp != null ) {
          Image image = this.screenFld.createBufferedImage();
          if( image != null ) {
            TransferableImage tImg = new TransferableImage( image );
            clp.setContents( tImg, tImg );
          }
        }
      }
    }
    catch( IllegalStateException ex ) {}
  }


  private void doScreenImageSaveAs()
  {
    try {
      String[] fmtNames = ImageIO.getWriterFormatNames();
      if( fmtNames != null ) {
        if( fmtNames.length < 1 )
          fmtNames = null;
      }
      if( fmtNames == null ) {
        throw new IOException(
                "Das Speichern von Bilddateien wird auf\n"
                        + "dieser Plattform nicht unterst\u00FCtzt." );
      }
      BufferedImage image = this.screenFld.createBufferedImage();
      if( image != null ) {
        File file = FileDlg.showFileSaveDlg(
                                this,
                                "Bilddatei speichern",
                                Main.getLastPathFile(),
                                new FileNameExtensionFilter(
                                        "Unterst\u00FCtzte Bilddateien",
                                        fmtNames ) );
        if( file != null ) {
          String s = file.getName();
          if( s != null ) {
            s = s.toUpperCase();
            String fmt = null;
            for( int i = 0; i < fmtNames.length; i++ ) {
              if( s.endsWith( "." + fmtNames[ i ].toUpperCase() ) ) {
                fmt = fmtNames[ i ];
                break;
              }
            }
            if( fmt != null ) {
              OutputStream out = null;
              try {
                out = new FileOutputStream( file );
                if( !ImageIO.write( image, fmt, file ) ) {
                  fmt = null;
                }
                out.close();
                Main.setLastFile( file );
              }
              catch( IOException ex ) {
                file.delete();
                throw ex;
              }
              finally {
                if( out != null ) {
                  try {
                    out.close();
                  }
                  catch( IOException ex ) {}
                }
              }
            }
            if( fmt == null ) {
              throw new IOException(
                        "Das durch die Dateiendung angegebene Format\n"
                                + "wird nicht unterst\u00FCtzt." );
            }
          }
        }
      }
    }
    catch( Exception ex ) {
      Main.showError( this, ex );
    }
  }


  private void doScreenTextCopy()
  {
    try {
      Toolkit tk = getToolkit();
      if( tk != null ) {
        Clipboard clp = tk.getSystemClipboard();
        if( clp != null ) {
          StringSelection data = new StringSelection(
                                        this.jtcSys.getScreenText() );
          clp.setContents( data, data );
        }
      }
    }
    catch( IllegalStateException ex ) {}
  }


        /* --- private Methoden --- */

  private JMenuItem createJMenuItem( String text, String actionCmd )
  {
    JMenuItem item = new JMenuItem( text );
    if( actionCmd != null ) {
      item.setActionCommand( actionCmd );
    }
    item.addActionListener( this );
    return item;
  }


  private JMenuItem createJMenuItem(
                                String text,
                                int    altKeyCode,
                                String actionCmd )
  {
    JMenuItem item = createJMenuItem( text, actionCmd );
    item.setAccelerator( KeyStroke.getKeyStroke( altKeyCode, 0 ) );
    return item;
  }


  private static int getSettingsScreenRefreshMillis()
  {
    return Main.getIntProperty( "jtcemu.screen.refresh.ms", 50 );
  }


  private void loadFile( File file ) throws IOException
  {
    int  bufSize = 0x2000;                                // default 8 KByte
    long fileLen = file.length();
    if( fileLen > 0 ) {
      // Puffer so gross, dass auch beim Hex-Format 64K gelesen werden koennen
      bufSize = Math.min( (int) fileLen, 0x40000 );
    }
    byte[] fileBytes = new byte[ bufSize ];
    fileLen          = 0;
    InputStream in   = null;
    try {
      in      = new FileInputStream( file );
      fileLen = in.read( fileBytes );
    }
    finally {
      if( in != null ) {
        try {
          in.close();
        }
        catch( IOException ex ) {}
      }
    }
    if( fileLen > 0 ) {
      (new LoadDlg(
                this,
                this.jtcSys,
                fileBytes,
                (int) fileLen,
                file )).setVisible( true );
    } else {
      JOptionPane.showMessageDialog(
                this,
                "Die Datei ist leer.",
                "Info",
                JOptionPane.INFORMATION_MESSAGE );
    }
  }


  private void resetEmu( boolean clearMem )
  {
    StringBuilder errBuf  = null;
    String        errAddr = null;
    int           addrCol = 0;
    int           errCnt  = 0;
    if(  Main.getBooleanProperty( "jtcemu.rom.reload_on_reset", false ) ) {
      ExtROM[] roms = this.jtcSys.getExtROMs();
      if( roms != null ) {
        for( int i = 0; i < roms.length; i++ ) {
          try {
            roms[ i ].reload();
          }
          catch( IOException ex ) {
            errAddr = String.format( "%%%04X", roms[ i ].getBegAddress() );
            errCnt++;
            if( errBuf == null ) {
              errBuf = new StringBuilder( 256 );;
              errBuf.append( "Die ROM-Images an folgenden Adressen"
                        + " konnten nicht geladen werden:\n" );
            }
            if( addrCol > 0 ) {
              errBuf.append( "  " );
            } else {
              errBuf.append( (char) '\n' );
            }
            errBuf.append( errAddr );
            addrCol++;
            if( addrCol >= 8 ) {
              addrCol = 0;
            }
          }
        }
      }
    }
    KeyboardFrm.reset();
    this.jtcSys.resetKeyMatrixStatus();
    this.z8.fireReset( clearMem );
    if( errCnt == 1 ) {
      Main.showError(
                this,
                "Das ROM-Image an Adresse " + errAddr + " konnte nicht"
                        + " geladen werden.\n"
                        + "Es bleibt das alte Image g\u00FCltig." );
    }
    else if( (errCnt > 1) && (errBuf != null) ) {
      errBuf.append( "\n\nAn diesen Adressen bleiben die alten Images"
                        + " g\u00FCltig." );
      Main.showError( this, errBuf.toString() );
    }
    /*
     * Der Timer wird neu gestartet fuer den Fall,
     * dass sich der Timer aufgehaengt haben sollte,
     * und man moechte ihn mit RESET reaktivieren.
     */
    this.screenRefreshTimer.restart();
  }


  private boolean updScreenSize( boolean updScale )
  {
    boolean        sizeChanged = false;
    ScreenFld.Mode mode        = ScreenFld.Mode.M64X64;
    if( this.jtcSys.getOSType() == JTCSys.OSType.ES40 ) {
      mode = ScreenFld.Mode.M320X192;
    }
    else if( this.jtcSys.getOSType() == JTCSys.OSType.ES23 ) {
      mode = ScreenFld.Mode.M128X128;
    }
    if( mode != this.screenFld.getMode() ) {
      Integer screenScale = null;
      if( updScale ) {
        screenScale = this.mode2Scale.get( mode );
      }
      if( screenScale != null ) {
        this.screenFld.setMode( mode, false );
        this.screenFld.setScreenScale( screenScale );
      } else {
        this.screenFld.setMode( mode, updScale );
      }
      sizeChanged = true;
    }
    int mOld = this.screenFld.getMargin();
    int mNew = Main.getIntProperty( "jtcemu.screen.margin", 20 );
    this.screenFld.setMargin( mNew );
    if( mNew != mOld ) {
      sizeChanged = true;
    }
    return sizeChanged;
  }
}

