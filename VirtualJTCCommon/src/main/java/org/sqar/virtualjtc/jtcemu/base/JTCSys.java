/*
 * (c) 2007-2010 Jens Mueller
 * (c) 2017 Lars Sonchocky-Helldorf
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Emulation der spezifischen Hardware des JU+TE-Computers,
 * d.h. der Hardware ausserhalb des eigentlichen Einchipmikrorechners
 *
 * Hier wird auch die Tastatur emuliert,
 * da sie ueber einen Adressbereich des Arbeitsspeichers gelesen wird.
 */

package org.sqar.virtualjtc.jtcemu.base;

import java.awt.event.*;
import java.io.*;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Locale;
import java.util.ResourceBundle;

import org.sqar.virtualjtc.jtcemu.Main;
import org.sqar.virtualjtc.jtcemu.audio.*;
import org.sqar.virtualjtc.z8.*;


public class JTCSys implements
                        KeyListener,
                        Z8IO,
                        Z8Memory,
                        Z8PCListener
{
  private static final Locale locale = Locale.getDefault();
  private static final ResourceBundle jtcSysResourceBundle = ResourceBundle.getBundle("JTCSys", locale);

  public static enum OSType { OS2K, ES1988, ES23, ES40 };

  private static final int[][] keyMatrix2kNormal = {
        { 0, 0x20, 'Y', 'X', 'C', 'V', 'B', 'N', 'M', ',', '.', '/', 0 },
        { 0, 0, 'A', 'S', 'D', 'F', 'G', 'H', 'J', 'K', 'L', '*', '-' },
        { 0, 0, 'Q', 'W', 'E', 'R', 'T', 'Z', 'U', 'I', 'O', 'P', '+' },
        { 0, 0, '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', 0 } };

  private static final int[][] keyMatrix2kShift = {
        { 0, 0x20, 'y', 'x', 'c', 'v', 'b', 'n', 'm', '<', '>', '?', 0 },
        { 0, 0, 'a', 's', 'd', 'f', 'g', 'h', 'j', 'k', 'l', ':', '=' },
        { 0, 0, 'q', 'w', 'e', 'r', 't', 'z', 'u', 'i', 'o', 'p', ';' },
        { 0, 0, '!', '\"', '#', '$', '%', '&', '\'', '(', ')', 0, 0 } };

  private static final int[][] keyMatrixES1988Normal = {
        { 0, 0x20, 'Y', 'X', 'C', 'V', 'B', 'N', 'M', ',', '.', '/', 0 },
        { 0, 0, 'A', 'S', 'D', 'F', 'G', 'H', 'J', 'K', 'L', '*', '-' },
        { 0, '=',  'Q', 'W', 'E', 'R', 'T', 'Z', 'U', 'I', 'O', 'P', '+' },
        { 0, 0, '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', 0 } };

  private static final int[][] keyMatrixES1988Shift = {
        { 0, 0x20, 'y', 'x', 'c', 'v', 'b', 'n', 'm', '<', '>', '?', 0 },
        { 0, 0, 'a', 's', 'd', 'f', 'g', 'h', 'j', 'k', 'l', ':', 0 },
        { 0, 0, 'q', 'w', 'e', 'r', 't', 'z', 'u', 'i', 'o', 'p', ';' },
        { 0, 0, '!', '\"', '#', '$', '%', '&', '\'', '(', ')', 0, 0 } };

  private static final int[][] keyMatrixES23Normal = {
        { 0, 'y', 'x', 'c', 'v', 'b', 'n', 'm', ',', '.', '/', 0x0D, 0x20 },
        { 0, 'a', 's', 'd', 'f', 'g', 'h', 'j', 'k', 'l', ';', '+', '*' },
        { 0, 'q', 'w', 'e', 'r', 't', 'z', 'u', 'i', 'o', 'p', '-', '=' },
        { 0, '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '<', '>' } };

  private static final int[][] keyMatrixES23Shift = {
        { 0, 0, 0, 0, 0, 0, 0, 0, '[', ']', '?' },
        { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, ':', '\\', '^' },
        { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, '_' },
        { 0, '!', '\"', '#', '$', '%', '&', '\'', '@', '(', ')' } };

  private static final int[][] keyMatrixES23Control = {
        { 0, 25, 24, 3, 22,  2, 14, 13 },
        { 0,  1, 19, 4,  6,  7,  8, 10, 11, 12 },
        { 0, 17, 23, 5, 18, 20, 26, 21,  9, 15, 16 },
        { 0 } };

  private static final int[][] keyMatrixES40Normal = {
        { 0, 0, 'z', 'x', 'c', 'v', 'b', 'n', 'm', ',', '.', 0x20, 0x0D },
        { 0, 0, 'a', 's', 'd', 'f', 'g', 'h', 'j', 'k', 'l', ';', '*' },
        { 0, 0, 'q', 'w', 'e', 'r', 't', 'y', 'u', 'i', 'o', 'p', '+' },
        { 0, 0, '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '<' } };

  private static final int[][] keyMatrixES40Shift1 = {
        { 0, 0, 'Z', 'X', 'C', 'V', 'B', 'N', 'M', '[', ']', '=', '?' },
        { 0, 0, 'A', 'S', 'D', 'F', 'G', 'H', 'J', 'K', 'L', ':', '/' },
        { 0, 0, 'Q', 'W', 'E', 'R', 'T', 'Y', 'U', 'I', 'O', 'P', '-' },
        { 0, 0, '!', '\"', '#', '$', '%', '&', '\'', '@', '(', ')', '>' } };

  private static final int[][] keyMatrixES40Shift2 = {
        { 0, 0, 0, 4, 0,    0x0E, 0, 0, 0, '{', '}', '_', '~' },
        { 0, 0, 1, 5, 2,    0x0B, 0, 0, 0, 0,   0,   0,   '|' },
        { 0, 0, 7, 3, 6,    0x0A, 0, 0, 0, 0,   0,   0,   '\\' },
        { 0, 0, 8, 9, 0x0C, 0, '\u00C4', '\u00D6', '\u00DC',
                                        '\u00E4', '\u00F6', '\u00FC', '^' } };


  private ScreenFld         screenFld;
  private volatile Z8       z8;
  private volatile OSType   osType;
  private volatile AudioIn  audioIn;
  private volatile AudioOut audioOut;
  private volatile ExtROM[] extROMs;
  private volatile long     lastScreenCycles;
  private byte[]            u883rom;
  private byte[]            os2k_0800;
  private byte[]            es1988_0800;
  private byte[]            es1988_2000;
  private byte[]            es23_0800;
  private byte[]            es40_0800;
  private byte[]            rom0800;
  private byte[]            rom2000;
  private byte[]            ram;
  private byte[]            ramV;
  private byte[]            ramB;
  private byte[]            ramG;
  private byte[]            ramR;
  private volatile int      curKeyValue;
  private volatile int      ramSize;
  private int[]             keyMatrixCols;
  private boolean           ignoreKeyChar;
  private boolean           alwaysScreenActive;
  private boolean           emuKeyboardSyscall;
  private boolean           emuReg80ToEF;
  private boolean           pcListener080CAdded;
  private boolean           pcListener0C56Added;
  private boolean           audioDataOut;
  private boolean           videoV;
  private boolean           videoB;
  private boolean           videoG;
  private boolean           videoR;


  public JTCSys() throws IOException
  {
    this.screenFld           = null;
    this.z8                  = null;
    this.osType              = OSType.OS2K;
    this.audioIn             = null;
    this.audioOut            = null;
    this.extROMs             = null;
    this.lastScreenCycles    = -1;
    this.u883rom             = readFile("/org.sqar.virtualjtc/rom/u883rom.bin");
    this.os2k_0800           = readFile("/org.sqar.virtualjtc/rom/os2k_0800.bin");
    this.es1988_0800         = readFile("/org.sqar.virtualjtc/rom/es1988_0800.bin");
    this.es1988_2000         = readFile("/org.sqar.virtualjtc/rom/es1988_2000.bin");
    this.es23_0800           = readFile("/org.sqar.virtualjtc/rom/es23_0800.bin");
    this.es40_0800           = readFile("/org.sqar.virtualjtc/rom/es40_0800.bin");
    this.ram                 = new byte[ 0x10000 ];
    this.ramV                = new byte[ 0x2000 ];
    this.ramB                = new byte[ 0x2000 ];
    this.ramG                = new byte[ 0x2000 ];
    this.ramR                = new byte[ 0x2000 ];
    this.curKeyValue         = 0;
    this.ramSize             = 0x10000;
    this.ignoreKeyChar       = false;
    this.alwaysScreenActive  = false;
    this.emuKeyboardSyscall  = false;
    this.emuReg80ToEF        = false;
    this.pcListener080CAdded = false;
    this.pcListener0C56Added = false;
    this.audioDataOut        = false;
    this.videoV              = false;
    this.videoB              = false;
    this.videoG              = false;
    this.videoR              = false;
    this.keyMatrixCols       = new int[ 16 ];
    resetKeyMatrixStatus();
  }


  public void fireReset()
  {
    if( this.z8 != null )
      this.z8.fireReset( false );
  }


  public boolean getAlwaysScreenActive()
  {
    return this.alwaysScreenActive;
  }


  public boolean getEmulateKeyboardSyscall()
  {
    return this.emuKeyboardSyscall;
  }


  public boolean getEmulateRegisters80ToEF()
  {
    return this.emuReg80ToEF;
  }


  public ExtROM[] getExtROMs()
  {
    return this.extROMs;
  }


  public OSType getOSType()
  {
    return this.osType;
  }


  public int getPixel( int x, int y )
  {
    int rv = 0;
    if( this.osType == OSType.ES40 ) {
      int grp  = y / 3;
      int yGrp = y % 3;
      int idx  = (grp * 128) + (yGrp * 40) + (x / 8);
      if( idx < this.ramV.length ) {
        int m = (1 << (7 - (x % 8)));
        if( (this.ramV[ idx ] & m) == 0 ) {
          rv |= 1;
        }
        if( (this.ramB[ idx ] & m) == 0 ) {
          rv |= 2;
        }
        if( (this.ramG[ idx ] & m) == 0 ) {
          rv |= 4;
        }
        if( (this.ramR[ idx ] & m) == 0 ) {
          rv |= 8;
        }
      }
    } else if( this.osType == OSType.ES23 ) {
      int idx = 0xF800 + (y * 16) + (x / 8);
      if( (idx >= 0) && (idx < this.ram.length) ) {
        if( (this.ram[ idx ] & (1 << (7 - (x % 8)))) == 0 ) {
          rv = 1;
        } else {
          rv = 0;
        }
      }
    } else {
      int idx = 0xFE00 + (y * 8) + (x / 8);
      if( (idx >= 0) && (idx < this.ram.length) ) {
        if( (this.ram[ idx ] & (1 << (7 - (x % 8)))) == 0 ) {
          rv = 1;
        } else {
          rv = 0;
        }
      }
    }
    return rv;
  }


  public int getRAMSize()
  {
    return this.ramSize;
  }


  public String getScreenText()
  {
    String rv = null;
    if( this.osType == OSType.ES23 ) {
      rv = getScreenText( 0xF400, 16, 16, 16 );
    } else if( this.osType == OSType.ES40 ) {
      rv = getScreenText( 0xFC00, 24, 40, 40 );
    } else {
      rv = getScreenText( 0xFD00, 8, 13, 16 );
    }
    return rv;
  }


  public boolean isScreenOutputEnabled()
  {
    boolean rv = (this.alwaysScreenActive || (this.osType == OSType.ES40));
    if( !rv && (this.z8 != null) ) {
      rv = (this.z8.calcDiffCycles(
                        this.lastScreenCycles,
                        this.z8.getTotalCycles() ) < 400000);
    }
    return rv;
  }


  public void resetKeyMatrixStatus()
  {
    Arrays.fill( this.keyMatrixCols, 0 );
  }


  public void resetKeyMatrixColBits( int col, int mask )
  {
    if( (col >= 0) && (col < this.keyMatrixCols.length) )
      this.keyMatrixCols[ col ] &= ~mask;
  }


  public void setKeyMatrixColBits( int col, int mask )
  {
    if( (col >= 0) && (col < this.keyMatrixCols.length) )
      this.keyMatrixCols[ col ] |= mask;
  }


  public void settingsChanged()
  {
    OSType osType = OSType.OS2K;
    String text   = Main.getProperty( "org.sqar.virtualjtc.jtcemu.os" );
    if( text != null ) {
      if( text.equals( "ES1988" ) ) {
        osType = OSType.ES1988;
      }
      else if( text.equals( "ES2.3" ) ) {
        osType = OSType.ES23;
      }
      else if( text.equals( "ES4.0" ) ) {
        osType = OSType.ES40;
      }
    }
    if( osType == OSType.ES40 ) {
      this.rom0800 = this.es40_0800;
      this.rom2000 = new byte[ 0 ];
    } else if( osType == OSType.ES23 ) {
      this.rom0800 = this.es23_0800;
      this.rom2000 = new byte[ 0 ];
    } else if( osType == OSType.ES1988 ) {
      this.rom0800 = this.es1988_0800;
      this.rom2000 = this.es1988_2000;
    } else {
      this.rom0800 = this.os2k_0800;
      this.rom2000 = new byte[ 0 ];
    }
    this.osType = osType;

    this.alwaysScreenActive = Main.getBooleanProperty(
                                        "org.sqar.virtualjtc.jtcemu.screen.active.always",
                                        true );
    this.emuKeyboardSyscall = Main.getBooleanProperty(
                                        "org.sqar.virtualjtc.jtcemu.keyboard.emulate_syscall",
                                        true );
    this.emuReg80ToEF = Main.getBooleanProperty(
                                        "org.sqar.virtualjtc.z8.emulate_registers_80toEF",
                                        false );
    if( this.z8 != null ) {
      this.z8.setMaxGPRNum( this.emuReg80ToEF ? 0xEF : 0x7F );
      if( !this.alwaysScreenActive
          && ((this.osType == OSType.OS2K)
                || (this.osType == OSType.ES1988)
                || (this.osType == OSType.ES23)) )
      {
        if( !this.pcListener080CAdded && (this.z8 != null) ) {
          this.z8.addPCListener( 0x080C, this );
          this.pcListener080CAdded = true;
        }
      } else {
        if( this.pcListener080CAdded && (this.z8 != null) ) {
          this.z8.removePCListener( 0x080C, this );
          this.pcListener080CAdded = false;
        }
      }
      if( this.emuKeyboardSyscall
          && ((this.osType == OSType.OS2K)
                        || (this.osType == OSType.ES1988)) )
      {
        if( !this.pcListener0C56Added && (this.z8 != null) ) {
          this.z8.addPCListener( 0x0C56, this );
          this.pcListener0C56Added = true;
        }
      } else {
        if( this.pcListener0C56Added && (this.z8 != null) ) {
          this.z8.removePCListener( 0x0C56, this );
          this.pcListener0C56Added = false;
        }
      }
    }
    int ramSize = 0x10000;
    text        = Main.getProperty( "org.sqar.virtualjtc.jtcemu.ram.size" );
    if( text != null ) {
      if( text.equals( "1K" ) ) {
        ramSize = 0x0400;
      } else if( text.equals( "2K" ) ) {
        ramSize = 0x0800;
      } else if( text.equals( "8K" ) ) {
        ramSize = 0x2000;
      } else if( text.equals( "32K" ) ) {
        ramSize = 0x8000;
      }
    }
    this.ramSize = ramSize;
  }


  public void setAudioIn( AudioIn audioIn )
  {
    this.audioIn = audioIn;
  }


  public void setAudioOut( AudioOut audioOut, boolean forDataTransfer )
  {
    this.audioOut     = audioOut;
    this.audioDataOut = forDataTransfer;
  }


  public void setExtROMs( ExtROM[] extROMs )
  {
    this.extROMs = extROMs;
  }


  public void setScreenFld( ScreenFld screenFld )
  {
    this.screenFld = screenFld;
  }


  public void setZ8( Z8 z8 )
  {
    this.z8 = z8;
  }


        /* --- KeyListener --- */

  /*
   * Mit keyPressed werden nur die Tasten verarbeitet,
   * die direkt kein keyTyped-Event ausloesen (z.B. Cursor-Tasten).
   * Alle anderen Zeichen werden mit keyTyped eingegeben,
   * da nur dort die Methode getKeyChar() garantiert
   * einen gueltigen Wert liefert.
   */
  @Override
  public void keyPressed( KeyEvent e )
  {
    resetKeyMatrixStatus();
    this.curKeyValue = 0;
    int keyCode      = e.getKeyCode();
    if( this.osType == OSType.ES23 ) {
      if( (keyCode == KeyEvent.VK_LEFT)
          || (keyCode == KeyEvent.VK_RIGHT)
          || (keyCode == KeyEvent.VK_UP)
          || (keyCode == KeyEvent.VK_DOWN)
          || (keyCode == KeyEvent.VK_HOME)
          || (keyCode == KeyEvent.VK_DELETE)
          || (keyCode == KeyEvent.VK_BACK_SPACE)
          || (keyCode == KeyEvent.VK_INSERT)
          || (keyCode == KeyEvent.VK_CLEAR)
          || (keyCode == KeyEvent.VK_ENTER) )
      {
        switch( keyCode ) {
          case KeyEvent.VK_LEFT:
            this.curKeyValue = 1;
            break;

          case KeyEvent.VK_RIGHT:
            this.curKeyValue = 2;
            break;

          case KeyEvent.VK_UP:
            this.curKeyValue = 3;
            break;

          case KeyEvent.VK_DOWN:
            this.curKeyValue = 4;
            break;

          case KeyEvent.VK_HOME:
            this.curKeyValue = 5;
            break;

          case KeyEvent.VK_DELETE:
            this.curKeyValue = 7;
            break;

          case KeyEvent.VK_BACK_SPACE:
            this.curKeyValue = 8;
            break;

          case KeyEvent.VK_INSERT:
            this.curKeyValue = 9;
            break;

          case KeyEvent.VK_CLEAR:
            this.curKeyValue = 0x0C;
            break;

          case KeyEvent.VK_ENTER:
            this.curKeyValue = 0x0D;
            break;
        }
        setKeyMatrixStatusES23( this.curKeyValue );
        this.ignoreKeyChar = true;
        e.consume();
      }
    }
    else if( this.osType == OSType.ES40 ) {
      if( (keyCode == KeyEvent.VK_LEFT)
          || (keyCode == KeyEvent.VK_RIGHT)
          || (keyCode == KeyEvent.VK_UP)
          || (keyCode == KeyEvent.VK_DOWN)
          || (keyCode == KeyEvent.VK_HOME)
          || (keyCode == KeyEvent.VK_DELETE)
          || (keyCode == KeyEvent.VK_BACK_SPACE)
          || (keyCode == KeyEvent.VK_INSERT)
          || (keyCode == KeyEvent.VK_CLEAR)
          || (keyCode == KeyEvent.VK_ENTER)
          || (keyCode == KeyEvent.VK_ESCAPE)
          || (keyCode == KeyEvent.VK_F1)
          || (keyCode == KeyEvent.VK_F2)
          || (keyCode == KeyEvent.VK_F3)
          || (keyCode == KeyEvent.VK_F4)
          || (keyCode == KeyEvent.VK_F5)
          || (keyCode == KeyEvent.VK_F6) )
      {
        switch( keyCode ) {
          case KeyEvent.VK_LEFT:
            this.curKeyValue = 1;
            break;

          case KeyEvent.VK_RIGHT:
            this.curKeyValue = 2;
            break;

          case KeyEvent.VK_UP:
            this.curKeyValue = 3;
            break;

          case KeyEvent.VK_DOWN:
            this.curKeyValue = 4;
            break;

          case KeyEvent.VK_HOME:
            this.curKeyValue = 5;
            break;

          case KeyEvent.VK_DELETE:
            this.curKeyValue = 7;
            break;

          case KeyEvent.VK_BACK_SPACE:
            this.curKeyValue = 8;
            break;

          case KeyEvent.VK_INSERT:
            this.curKeyValue = 9;
            break;

          case KeyEvent.VK_CLEAR:
            this.curKeyValue = 0x0C;
            break;

          case KeyEvent.VK_ENTER:
            this.curKeyValue = 0x0D;
            break;

          case KeyEvent.VK_ESCAPE:
            this.curKeyValue = 0x0E;
            break;

          case KeyEvent.VK_F1:
            this.keyMatrixCols[ 1 ] = 0x04;
            this.keyMatrixCols[ 2 ] = 0x04;
            break;

          case KeyEvent.VK_F2:
            this.keyMatrixCols[ 1 ] = 0x04;
            this.keyMatrixCols[ 3 ] = 0x04;
            break;

          case KeyEvent.VK_F3:
            this.keyMatrixCols[ 1 ] = 0x04;
            this.keyMatrixCols[ 4 ] = 0x04;
            break;

          case KeyEvent.VK_F4:
            this.keyMatrixCols[ 1 ] = 0x04;
            this.keyMatrixCols[ 5 ] = 0x04;
            break;

          case KeyEvent.VK_F5:
            this.keyMatrixCols[ 1 ] = 0x04;
            this.keyMatrixCols[ 6 ] = 0x04;
            break;

          case KeyEvent.VK_F6:
            this.keyMatrixCols[ 1 ] = 0x04;
            this.keyMatrixCols[ 7 ] = 0x04;
            break;
        }
        setKeyMatrixStatusES40( this.curKeyValue );
        this.ignoreKeyChar = true;
        e.consume();
      }
    } else {
      if( (keyCode == KeyEvent.VK_LEFT)
          || (keyCode == KeyEvent.VK_RIGHT)
          || (keyCode == KeyEvent.VK_UP)
          || (keyCode == KeyEvent.VK_DOWN)
          || (keyCode == KeyEvent.VK_BACK_SPACE)
          || (keyCode == KeyEvent.VK_CONTROL)
          || (keyCode == KeyEvent.VK_SPACE)
          || (keyCode == KeyEvent.VK_ENTER) )
      {
        switch( keyCode ) {
          case KeyEvent.VK_LEFT:
            this.curKeyValue = 0x0B;
            break;

          case KeyEvent.VK_RIGHT:
            this.curKeyValue = 0x1B;
            break;

          case KeyEvent.VK_UP:
            this.curKeyValue = 0x1A;
            break;

          case KeyEvent.VK_DOWN:
            this.curKeyValue = 0x0A;
            break;

          case KeyEvent.VK_BACK_SPACE:
            this.curKeyValue = 0x08;
            break;

          case KeyEvent.VK_SPACE:
            this.curKeyValue = 0x20;
            break;

          case KeyEvent.VK_ENTER:
            if( (e.getModifiers() & KeyEvent.SHIFT_MASK) != 0 ) {
              this.curKeyValue = 0x7F;            // OFF
            } else {
              this.curKeyValue = 0x0D;            // ENTER
            }
            break;
        }
        if( ((keyCode != KeyEvent.VK_LEFT) && (keyCode != KeyEvent.VK_RIGHT))
            || (this.osType == OSType.OS2K) )
        {
          setKeyMatrixStatus2kOrES1988( this.curKeyValue );
        }
        if( keyCode == KeyEvent.VK_CONTROL ) {
          this.keyMatrixCols[ 1 ] |= 0x02;
        }
        this.ignoreKeyChar = true;
        e.consume();
      }
    }
  }


  @Override
  public void keyReleased( KeyEvent e )
  {
    resetKeyMatrixStatus();
    this.curKeyValue   = 0;
    this.ignoreKeyChar = false;
  }


  @Override
  public void keyTyped( KeyEvent e )
  {
    if( !this.ignoreKeyChar ) {
      int ch = e.getKeyChar();
      if( this.osType == OSType.ES23 ) {
        if( (ch >= 'A') && (ch <= 'Z') ) {
          ch = ch - 'A' + 'a';
        }
        this.curKeyValue = ch;
        setKeyMatrixStatusES23( this.curKeyValue );
      } else if( this.osType == OSType.ES40 ) {
        this.curKeyValue = ch;
        setKeyMatrixStatusES40( this.curKeyValue );
      } else {
        if( (ch >= 'A') && (ch <= 'Z') ) {
          if( (ch >= 0x40) && (ch < 0x50) ) {
            this.curKeyValue = (char) (ch + 0x10);
          } else if( (ch >= 0x50) && (ch < 0x60) ) {
            this.curKeyValue = (char) (ch - 0x10);
          }
          setKeyMatrixStatus2kOrES1988( ch );
        } else if( (ch >= 'a') && (ch <= 'z') ) {
          this.curKeyValue = Character.toUpperCase( ch );
          setKeyMatrixStatus2kOrES1988( this.curKeyValue );
        } else {
          this.curKeyValue = ch;
          setKeyMatrixStatus2kOrES1988( this.curKeyValue );
        }
      }
    } else {
      this.ignoreKeyChar = false;
    }
    e.consume();
  }


        /* --- Z8IO --- */

  @Override
  public int getPortValue( int port )
  {
    int rv = 0xFF;
    if( port == 3 ) {
      rv &= ~0x04;                        // P32=0
      AudioIn audioIn = this.audioIn;
      if( audioIn != null ) {
        boolean b = true;
        if( this.osType == OSType.OS2K ) {
          b = audioIn.readVolumeStatus();
        } else {
          b = audioIn.readPhase();
        }
        if( !b )
          rv &= ~0x01;                        // P30=0
      }
    }
    return rv;
  }


  @Override
  public void setPortValue( int port, int value )
  {
    if( port == 3 ) {
      AudioOut audioOut = this.audioOut;
      if( audioOut != null ) {
        if( this.osType == OSType.OS2K ) {
          if( this.audioDataOut ) {
            audioOut.writePhase( (value & 0xC0) != 0xC0 );
          } else {
            audioOut.writePhase( (value & 0xC0) != 0x40 );
          }
        } else {
          audioOut.writePhase( (value & 0x40) != 0 );
        }
      }
    }
  }


        /* --- Z8Memory --- */

  @Override
  public int getMemByte( int addr, boolean dataMem )
  {
    addr &= 0xFFFF;

    int      rv      = 0xFF;
    boolean  done    = false;
    ExtROM[] extROMs = this.extROMs;
    if( extROMs != null ) {
      for( int i = 0; !done && (i < extROMs.length); i++ ) {
        ExtROM rom = this.extROMs[ i ];
        if( (addr >= rom.getBegAddress()) && (addr <= rom.getEndAddress()) ) {
          rv = rom.getByte( addr );
          done = true;
        }
      }
    }
    if( !done ) {
      if( (addr >= 0) && (addr < this.u883rom.length) ) {
        rv = this.u883rom[ addr ];
      } else if( (addr >= 0x0800) && (addr - 0x0800 < this.rom0800.length) ) {
        rv = this.rom0800[ addr - 0x0800 ];
      } else if( (addr >= 0x2000) && (addr - 0x2000 < this.rom2000.length) ) {
        rv = this.rom2000[ addr - 0x2000 ];
      } else if( (addr >= 0x4000) && (addr <= 0x5FFF) ) {
        if( this.osType == OSType.ES40 ) {
          int idx = addr - 0x4000;
          if( this.videoV && (idx < this.ramV.length) ) {
            rv = this.ramV[ idx ];
          }
          else if( this.videoB && (idx < this.ramB.length) ) {
            rv = this.ramB[ idx ];
          }
          else if( this.videoG && (idx < this.ramG.length) ) {
            rv = this.ramG[ idx ];
          }
          else if( this.videoR && (idx < this.ramR.length) ) {
            rv = this.ramR[ idx ];
          }
        } else {
          if( addr >= 0x10000 - this.ramSize ) {
            rv = this.ram[ addr ];
          }
        }
      } else if( (addr >= 0x6000) && (addr < 0x8000) ) {
        if( (this.osType != OSType.ES40) || (addr >= 0x7000) ) {
          int col = addr & 0x000F;
          if( col < this.keyMatrixCols.length ) {
            rv = (this.keyMatrixCols[ col ] << 4) | 0x0F;
          } else {
            rv = 0x0F;
          }
        }
      } else {
        if( (ramSize == 0x0400) && (addr >= 0xE000) ) {
          int a = addr & 0x03FF;
          if( a >= 0x0100 ) {
            rv = this.ram[ 0xFD00 - 0x0100 + a ];
          } else {
            rv = this.ram[ 0xE000 + a ];
          }
        }
        else if( (ramSize == 0x0800) && (addr >= 0xE000) ) {
          int a = addr & 0x07FF;
          if( a >= 0x0500 ) {
            rv = this.ram[ 0xFD00 - 0x0500 + a ];
          } else {
            rv = this.ram[ 0xE000 + a ];
          }
        }
        else if( addr >= 0x10000 - this.ramSize ) {
          rv = this.ram[ addr ];
        }
      }
    }
    return rv & 0xFF;
  }


  @Override
  public void initRAM()
  {
    Arrays.fill( this.ram, (byte) 0x00 );
  }


  @Override
  public boolean setMemByte( int addr, boolean dataMem, int v )
  {
    addr &= 0xFFFF;

    boolean  rv      = false;
    boolean  done    = false;
    ExtROM[] extROMs = this.extROMs;
    if( extROMs != null ) {
      for( int i = 0; !done && (i < extROMs.length); i++ ) {
        ExtROM rom = this.extROMs[ i ];
        if( (addr >= rom.getBegAddress()) && (addr <= rom.getEndAddress()) )
          done = true;
      }
    }
    if( !done ) {
      int ramBegAddr = 0x10000 - this.ramSize;
      if( (addr >= 0x1000) && (addr <= 0x17FF)
          && (addr >= ramBegAddr)
          && ((this.osType == OSType.OS2K) || (this.osType == OSType.ES1988)) )
      {
        this.ram[ addr ] = (byte) v;
        rv               = true;
      }
      else if( (addr >= 0x1800) && (addr <= 0x1FFF)
          && (addr >= ramBegAddr)
          && (this.osType != OSType.ES40) )
      {
        this.ram[ addr ] = (byte) v;
        rv               = true;
      }
      else if( (addr >= 0x2000) && (addr <= 0x27FF)
               && (addr >= ramBegAddr)
               && (this.osType != OSType.ES1988) )
      {
        this.ram[ addr ] = (byte) v;
        rv               = true;
      }
      else if( (addr >= 0x2800) && (addr <= 0x3FFF)
               && (addr >= ramBegAddr) )
      {
        this.ram[ addr ] = (byte) v;
        rv               = true;
      }
      else if( (addr >= 0x4000) && (addr <= 0x5FFF) ) {
        if( this.osType == OSType.ES40 ) {
          int     idx   = addr - 0x4000;
          boolean dirty = false;
          if( this.videoV && (idx < this.ramV.length) ) {
            this.ramV[ idx ] = (byte) v;
            dirty            = true;
          }
          if( this.videoB && (idx < this.ramB.length) ) {
            this.ramB[ idx ] = (byte) v;
            dirty            = true;
          }
          if( this.videoG && (idx < this.ramG.length) ) {
            this.ramG[ idx ] = (byte) v;
            dirty            = true;
          }
          if( this.videoR && (idx < this.ramR.length) ) {
            this.ramR[ idx ] = (byte) v;
            dirty            = true;
          }
          if( dirty && (this.screenFld != null) ) {
            this.screenFld.setDirty();
          }
        } else {
          if( addr >= ramBegAddr ) {
            this.ram[ addr ] = (byte) v;
            rv               = true;
          }
        }
      } else if( (addr >= 0x6000) && (addr < 0x8000) ) {
        if( (this.osType == OSType.ES40) && (addr < 0x6400) ) {
          this.videoV = ((addr & 0x10) == 0);
          this.videoB = ((addr & 0x20) == 0);
          this.videoG = ((addr & 0x40) == 0);
          this.videoR = ((addr & 0x80) == 0);
        }
      }
      else if( addr >= 0x8000 ) {
        if( (ramSize == 0x0400) && (addr >= 0xE000) ) {
          int a = addr & 0x3FF;
          if( a >= 0x0100 ) {
            this.ram[ 0xFD00 - 0x0100 + a ] = (byte) v;
            if( (this.screenFld != null)
                && ((this.osType == OSType.OS2K)
                        || (this.osType == OSType.ES1988))
                && (a >= 0x0200) )
            {
              this.screenFld.setDirty();
            }
          } else {
            this.ram[ 0xE000 + a ] = (byte) v;
          }
          if( (this.screenFld != null) && (this.osType == OSType.ES23) ) {
            this.screenFld.setDirty();
          }
          rv = true;
        }
        if( (ramSize == 0x0800) && (addr >= 0xE000) ) {
          int a = addr & 0x7FF;
          if( a >= 0x0500 ) {
            this.ram[ 0xFD00 - 0x0500 + a ] = (byte) v;
            if( (this.screenFld != null)
                && ((this.osType == OSType.OS2K)
                        || (this.osType == OSType.ES1988))
                && (a >= 0x0600) )
            {
              this.screenFld.setDirty();
            }
          } else {
            this.ram[ 0xE000 + a ] = (byte) v;
          }
          if( (this.screenFld != null) && (this.osType == OSType.ES23) ) {
            this.screenFld.setDirty();
          }
          rv = true;
        }
        else if( addr >= ramBegAddr ) {
          this.ram[ addr ] = (byte) v;
          rv               = true;
          if( (this.screenFld != null)
              || (((this.osType == OSType.OS2K)
                                || (this.osType == OSType.ES1988))
                        && (addr >= 0xFE00))
              || ((this.osType == OSType.ES23) && (addr >= 0xF800)) )
          {
            this.screenFld.setDirty();
          }
        }
      }
    }
    return rv;
  }


        /* --- Z8PCListener --- */

  @Override
  public void z8PCUpdate( Z8 z8, int pc )
  {
    if( z8 == this.z8 ) {
      if( (pc == 0x080C)
          && ((this.osType == OSType.OS2K)
                || (this.osType == OSType.ES1988)
                || (this.osType == OSType.ES23)) )
      {
        this.lastScreenCycles = z8.getTotalCycles();
      }
      if( (pc == 0x0C56)
          && ((this.osType == OSType.OS2K)
                || (this.osType == OSType.ES1988)) )
      {
        int m = z8.getRegValue( 0x6D );
        if( m == 0 ) {
          int keyValue = this.curKeyValue;
          if( keyValue > 0 ) {
            z8.setRegValue( 0x6D, keyValue | 0x80 );
          }
        } else {
          if( (m & 0x80) != 0 ) {
            if( this.curKeyValue == 0 ) {
              z8.setRegValue( 0x6D, m & 0x7F );
            }
          }
        }
        z8.setPC( z8.popw() );
      }
    }
  }


        /* --- private Methoden --- */

  private boolean checkKeyMatrix( int[][] matrix, int ch )
  {
    boolean rv = false;
    if( ch > 0 ) {
      for( int row = 0; !rv && (row < matrix.length); row++ ) {
        int[] line = matrix[ row ];
        for( int col = 1; !rv && (col < line.length); col++ ) {
          if( line[ col ] == ch ) {
            if( col < this.keyMatrixCols.length ) {
              this.keyMatrixCols[ col ] = (1 << row);
            }
            rv = true;
          }
        }
      }
    }
    return rv;
  }


  private String getScreenText(
                        int begAddr,
                        int nRows,
                        int nCols,
                        int rowDist )
  {
    StringBuilder buf = new StringBuilder( nRows * (nCols + 1) );
    int           nNL = 0;
    for( int i = 0; i < nRows; i++ ) {
      if( i > 0 ) {
        nNL++;
      }
      int addr    = begAddr + (i * rowDist);
      int nSpaces = 0;
      for( int k = 0; k < nCols; k++ ) {
        int b = getMemByte( addr++, false );
        if( b == 0x20 ) {
          nSpaces++;
        }
        else if( (b > 0x20) && (b < 0x7F) ) {
          while( nNL > 0 ) {
            buf.append( (char) '\n' );
            --nNL;
          }
          while( nSpaces > 0 ) {
            buf.append( (char) '\u0020' );
            --nSpaces;
          }
          buf.append( (char) b );
        }
      }
    }
    return buf.toString();
  }


  private byte[] readFile( String resource ) throws IOException
  {
    ByteArrayOutputStream buf  = new ByteArrayOutputStream( 0x0800 );
    boolean               done = false;
    InputStream           in   = null;
    try {
      in = getClass().getResourceAsStream( resource );
      if( in != null ) {
        int b = in.read();
        while( b != -1 ) {
          buf.write( b );
          b = in.read();
        }
        done = true;
      }
    }
    catch( IOException ex ) {}
    finally {
      if( in != null ) {
        try {
          in.close();
        }
        catch( IOException ex ) {}
      }
    }
    if( !done ) {
      throw new IOException( MessageFormat.format( jtcSysResourceBundle.getString("error.readFile.cantLoadResource.messageformat"), resource) );
    }
    return buf.toByteArray();
  }


  private void setKeyMatrixStatus2kOrES1988( int ch )
  {
    resetKeyMatrixStatus();
    switch( ch ) {
      case 0x0A:
      case 0x1A:
        this.keyMatrixCols[ 1 ] = 0x08;
        break;

      case 0x0B:
      case 0x1B:
        this.keyMatrixCols[ 1 ] = 0x04;
        break;

      case 0x20:
        this.keyMatrixCols[ 1 ] = 0x01;
        break;

      case 0x08:
      case 0x18:
        this.keyMatrixCols[ 12 ] = 0x08;
        break;

      case 0x0D:
      case 0x7F:
        this.keyMatrixCols[ 12 ] = 0x01;
        break;

      default:
        if( this.osType == OSType.OS2K ) {
          if( !checkKeyMatrix( this.keyMatrix2kNormal, ch ) ) {
            checkKeyMatrix( this.keyMatrix2kShift, ch );
          }
        } else {
          if( !checkKeyMatrix( this.keyMatrixES1988Normal, ch ) ) {
            checkKeyMatrix( this.keyMatrixES1988Shift, ch );
          }
        }
    }
  }


  private void setKeyMatrixStatusES23( int ch )
  {
    resetKeyMatrixStatus();
    switch( ch ) {
      case 1:
        this.keyMatrixCols[ 13 ]  = 0x02;
        break;

      case 2:
        this.keyMatrixCols[ 15 ]  = 0x02;
        break;

      case 3:
        this.keyMatrixCols[ 14 ]  = 0x04;
        break;

      case 4:
        this.keyMatrixCols[ 14 ]  = 0x01;
        break;

      case 5:
        this.keyMatrixCols[ 14 ]  = 0x02;
        break;

      case 7:
        this.keyMatrixCols[ 13 ]  = 0x04;
        break;

      case 8:
        this.keyMatrixCols[ 13 ]  = 0x08;
        break;

      case 9:
        this.keyMatrixCols[ 14 ]  = 0x08;
        break;

      case 0x0C:
        this.keyMatrixCols[ 15 ]  = 0x08;
        break;

      case 0x0D:
        this.keyMatrixCols[ 11 ]  = 0x01;
        break;

      default:
        if( !checkKeyMatrix( this.keyMatrixES23Normal, ch ) ) {
          if( checkKeyMatrix( this.keyMatrixES23Shift, ch ) ) {
            this.keyMatrixCols[ 0 ] = 0x01;
          } else {
            if( checkKeyMatrix( this.keyMatrixES23Control, ch ) )
              this.keyMatrixCols[ 0 ] = 0x02;
          }
        }
    }
  }


  private void setKeyMatrixStatusES40( int ch )
  {
    resetKeyMatrixStatus();
    if( ch == '\u00DF' ) {
      this.keyMatrixCols[ 1 ]  = 0x04;
      this.keyMatrixCols[ 12 ] = 0x08;
    } else {
      if( !checkKeyMatrix( this.keyMatrixES40Normal, ch ) ) {
        if( checkKeyMatrix( this.keyMatrixES40Shift1, ch ) ) {
          this.keyMatrixCols[ 1 ] = 0x01;
        } else {
          if( checkKeyMatrix( this.keyMatrixES40Shift2, ch ) )
            this.keyMatrixCols[ 1 ] = 0x02;
        }
      }
    }
  }
}

