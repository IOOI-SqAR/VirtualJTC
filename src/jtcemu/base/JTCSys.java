/*
 * (c) 2007-2021 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Emulation der spezifischen Hardware des JU+TE-Computers,
 * d.h. der Hardware ausserhalb des eigentlichen Einchipmikrorechners
 *
 * Hier wird auch die Tastatur emuliert,
 * da sie ueber einen Adressbereich des Arbeitsspeichers gelesen wird.
 */

package jtcemu.base;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import z8.Z8;
import z8.Z8IO;
import z8.Z8Listener;
import z8.Z8Memory;


public class JTCSys implements Z8IO, Z8Listener, Z8Memory
{
  public static final int DEFAULT_BASIC_ADDR           = 0xE000;
  public static final int DEFAULT_Z8_CYCLES_PER_SECOND = 4000000;  // intern
  public static final int MAX_ROM_SIZE                 = 0x10000;
  public static final int MAX_ROMBANK_SIZE             = 256 * 0x2000;

  public static final String PROP_Z8_REGS_80_TO_EF = "z8.regs80toEF";
  public static final String PROP_Z8_REG_INIT_ZERO = "z8.reg.init.zero";
  public static final String PROP_RAM_INIT_ZERO    = "ram.init.zero";
  public static final String PROP_RAM_SIZE         = "ram.size";
  public static final String PROP_ROM_PREFIX       = "rom.";
  public static final String PROP_ROM_COUNT        = "rom.count";
  public static final String PROP_ROM_RELOAD       = "rom.reload";
  public static final String PROP_ROMBANK_ENABLED  = "rombank.enabled";
  public static final String PROP_ROMBANK_FILE     = "rombank.file";
  public static final String PROP_OS               = "os";
  public static final String VALUE_OS_2K           = "2K";
  public static final String VALUE_OS_ES1988       = "ES1988";
  public static final String VALUE_OS_ES23         = "ES2.3";
  public static final String VALUE_OS_ES40         = "ES4.0";

  public static final boolean DEFAULT_RAM_INIT_ZERO    = true;
  public static final boolean DEFAULT_Z8_REG_INIT_ZERO = true;

  public static enum OSType { OS2K, ES1988, ES23, ES40 };

  public static enum Key { LEFT, RIGHT, UP, DOWN, BACK_SPACE, HOME,
			SPACE, ENTER, ESCAPE, INSERT, DELETE, CLEAR,
			F1, F2, F3, F4, F5, F6, F7, F8 };

  private static final int[][] keyMatrix2kNormal = {
	{ 0, 0x20, 'Y', 'X', 'C', 'V', 'B', 'N', 'M', ',', '.', '/',
	  0x0D, 0x1B },
	{ 0, 0, 'A', 'S', 'D', 'F', 'G', 'H', 'J', 'K', 'L', '*', '-' },
	{ 0, 0x0B, 'Q', 'W', 'E', 'R', 'T', 'Z', 'U', 'I', 'O', 'P', '+' },
	{ 0, 0x0A, '1', '2', '3', '4', '5', '6', '7', '8', '9', '0',
	  0x08 } };

  private static final int[][] keyMatrix2kShift = {
	{ 0, 0, 0, 0, 0, 0, 0, '\\', ']', '<', '>', '?', 0x7F, },
	{ 0, 0, 0, 0, 0, 0, 0, 0, 0, '[', 'l', ':', '=' },
	{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, '_', '@', ';' },
	{ 0, 0, '!', '\"', '#', '$', '%',  '&', '\'', '(', ')' } };

  private static final int[][] keyMatrixES1988Normal = {
	{ 0, 0x20, 'Y', 'X', 'C', 'V', 'B', 'N', 'M', ',', '.', '/',
	  0x0D, 0x1B },
	{ 0, 0, 'A', 'S', 'D', 'F', 'G', 'H', 'J', 'K', 'L', '*', '-' },
	{ 0, '=', 'Q', 'W', 'E', 'R', 'T', 'Z', 'U', 'I', 'O', 'P', '+' },
	{ 0, 0x0A, '1', '2', '3', '4', '5', '6', '7', '8', '9', '0' } };

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

  private static final int ADDR_VIDEO_INTERRUPT = 0x080C;

  /*
   * Farbzuordnung in der Methode getPixelColorNum(x,y)
   * Diese Zuordnung hat nichts mit der Farbzuordnung
   * im emulierten System zu tun.
   */
  private static final int RED_MASK    = 0x08;
  private static final int GREEN_MASK  = 0x04;
  private static final int BLUE_MASK   = 0x02;
  private static final int BRIGHT_MASK = 0x01;

  private String               errorText;
  private volatile ErrorViewer errorViewer;
  private volatile JTCScreen   screen;
  private volatile Z8Listener  resetListener;
  private PasteWorker          pasteWorker;
  private Random               random;
  private volatile Z8          z8;
  private OSType               osType;
  private OSType               newOSType;
  private ExtROM[]             extROMs;
  private ExtROM[]             newExtROMs;
  private ExtROM               romBank;
  private ExtROM[]             newRomBank;
  private volatile AudioReader tapeReader;
  private volatile AudioWriter tapeWriter;
  private volatile AudioWriter loudspeaker;
  private volatile long        lastScreenCycles;
  private int                  screenWidth;
  private int                  screenHeight;
  private CharRaster           charRaster;
  private byte[]               u883rom;
  private byte[]               os2k_0800;
  private byte[]               es1988_0800;
  private byte[]               es1988_2000;
  private byte[]               es23_0800;
  private byte[]               es40_0800;
  private byte[]               es40RomBank_0800;
  private byte[]               rom0800;
  private byte[]               rom2000;
  private byte[]               ram;
  private byte[]               ramV;
  private byte[]               ramB;
  private byte[]               ramG;
  private byte[]               ramR;
  private int                  newMaxGPRNum;
  private int                  newRamSize;
  private int                  ramSize;
  private int                  romBankMask;
  private int                  romBankNum;
  private int                  port3Value;
  private int[]                colorModeRGBs;
  private int[]                keyMatrixCols;
  private boolean              ignoreKeyChar;
  private volatile boolean     shiftStatusSet;
  private volatile boolean     tapeInPhase;
  private volatile boolean     tapeOutPhase;
  private volatile boolean     loudspeakerPhase;
  private boolean              videoV;
  private boolean              videoB;
  private boolean              videoG;
  private boolean              videoR;
  private volatile boolean     monochrome;


  public JTCSys() throws IOException
  {
    this.errorText        = null;
    this.errorViewer      = null;
    this.resetListener    = null;
    this.random           = new Random( System.currentTimeMillis() );
    this.ram              = new byte[ 0x10000 ];
    this.ramV             = new byte[ 0x2000 ];
    this.ramB             = new byte[ 0x2000 ];
    this.ramG             = new byte[ 0x2000 ];
    this.ramR             = new byte[ 0x2000 ];
    this.screen           = null;
    this.charRaster       = null;
    this.pasteWorker      = null;
    this.tapeReader       = null;
    this.tapeWriter       = null;
    this.loudspeaker      = null;
    this.romBankMask      = 0;
    this.romBankNum       = 0;
    this.port3Value       = 0xFF;
    this.lastScreenCycles = -1;
    this.screenHeight     = 0;
    this.screenWidth      = 0;
    this.ignoreKeyChar    = false;
    this.shiftStatusSet   = false;
    this.tapeInPhase      = false;
    this.tapeOutPhase     = false;
    this.loudspeakerPhase = false;
    this.videoV           = false;
    this.videoB           = false;
    this.videoG           = false;
    this.videoR           = false;
    this.monochrome       = true;
    this.newOSType        = null;
    this.newExtROMs       = null;
    this.newRomBank       = null;
    this.newMaxGPRNum     = 0;
    this.newRamSize       = 0;
    this.keyMatrixCols    = new int[ 0x10 ];
    resetKeyMatrixStatus();

    // Farbtabelle fuer Color-Mode
    this.colorModeRGBs = new int[ 0x10 ];
    for( int i = 0; i < this.colorModeRGBs.length; i++ ) {
      int r = ((i & RED_MASK) == 0   ? 0xA0 : 0);
      int g = ((i & GREEN_MASK) == 0 ? 0xA0 : 0);
      int b = ((i & BLUE_MASK) == 0  ? 0xA0 : 0);
      if( (i & BRIGHT_MASK) == 0 ) {
	r += 0x5F;
	g += 0x5F;
	b += 0x5F;
      }
      this.colorModeRGBs[ i ] = ((r << 16) & 0x00FF0000)
					| ((g << 8) & 0x0000FF00)
					| (b & 0x000000FF);
    }

    // Einstellungen lesen
    this.osType  = readOSType();
    this.ramSize = readRamSize();

    // Resourcen lesen
    this.u883rom          = readResource( "/rom/u883rom.bin" );
    this.os2k_0800        = readResource( "/rom/os2k_0800.bin" );
    this.es1988_0800      = readResource( "/rom/es1988_0800.bin" );
    this.es1988_2000      = readResource( "/rom/es1988_2000.bin" );
    this.es23_0800        = readResource( "/rom/es23_0800.bin" );
    this.es40_0800        = readResource( "/rom/es40_0800.bin" );
    this.es40RomBank_0800 = readResource( "/rom/es40c_0800.bin" );

    // externe ROM-Dateien laden
    StringBuilder buf = new StringBuilder();
    this.extROMs      = readExtROMs( buf );
    if( supportsROMBank( this.osType ) ) {
      this.romBank = readRomBank( buf );
      buildRomBankMask();
    }
    if( buf.length() > 0 ) {
      this.errorText = buf.toString();
    }

    // Z8 anlegen
    this.z8 = new Z8( isRegInitZero(), this, this );
    this.z8.setCyclesPerSecond( DEFAULT_Z8_CYCLES_PER_SECOND );
    this.z8.setMaxGPRNum( readMaxGPRNum() );
    this.z8.setResetListener( this );
    resetInternal( false, false );		// kein Power On!
    checkSetZ8PreExecInstListener();
    initRAM();
  }


  public void cancelPastingText()
  {
    PasteWorker pasteWorker = this.pasteWorker;
    if( pasteWorker != null ) {
      pasteWorker.fireStop();
      this.pasteWorker = null;
    }
  }


  public void fireReset( boolean initRAM )
  {
    cancelPastingText();
    resetKeyMatrixStatus();
    this.z8.fireReset( initRAM );
  }


  public int[] getColorModeRGBs()
  {
    return this.colorModeRGBs;
  }


  public boolean getEmulateRegisters80ToEF()
  {
    return this.z8.getMaxGPRNum() >= 0xEF;
  }


  public String getErrorText()
  {
    return this.errorText;
  }


  public ExtROM[] getExtROMs()
  {
    return this.extROMs;
  }


  public int[] getKeyMatrixCols()
  {
    return this.keyMatrixCols;
  }


  public OSType getOSType()
  {
    return this.osType;
  }


  public int getPixelColorNum ( int x, int y )
  {
    int rv = 0;
    if( this.osType == OSType.ES40 ) {
      int grp  = y / 3;
      int yGrp = y % 3;
      int idx  = (grp * 128) + (yGrp * 40) + (x / 8);
      if( idx < this.ramV.length ) {
	int m = (1 << (7 - (x % 8)));
	if( (this.ramV[ idx ] & m) == 0 ) {
	  rv |= BRIGHT_MASK;
	}
	if( (this.ramB[ idx ] & m) == 0 ) {
	  rv |= BLUE_MASK;
	}
	if( (this.ramG[ idx ] & m) == 0 ) {
	  rv |= GREEN_MASK;
	}
	if( (this.ramR[ idx ] & m) == 0 ) {
	  rv |= RED_MASK;
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


  public ExtROM getROMBank()
  {
    return this.romBank;
  }


  public static String getROMPropFile( int idx )
  {
    return String.format( "%s%d.file", PROP_ROM_PREFIX, idx );
  }


  public static String getROMPropAddr( int idx )
  {
    return String.format( "%s%d.addr", PROP_ROM_PREFIX, idx );
  }


  public synchronized CharRaster getScreenCharRaster()
  {
    return this.charRaster;
  }


  public synchronized int getScreenHeight()
  {
    return this.screenHeight;
  }


  public synchronized int getScreenWidth()
  {
    return this.screenWidth;
  }


  public String getScreenText()
  {
    String rv = null;
    if( this.osType == OSType.ES23 ) {
      rv = getScreenText( 0xF400, 16, 16, 16, 0, 0, 15, 15 );
    } else if( this.osType == OSType.ES40 ) {
      rv = getScreenText( 0xFC00, 24, 40, 40, 0, 0, 39, 23 );
    } else {
      rv = getScreenText( 0xFD00, 8, 13, 16, 0, 0, 12, 7 );
    }
    return rv;
  }


  public String getScreenText( int x1, int y1, int x2, int y2 )
  {
    String rv = null;
    if( this.osType == OSType.ES23 ) {
      rv = getScreenText( 0xF400, 16, 16, 16, x1, y1, x2, y2 );
    } else if( this.osType == OSType.ES40 ) {
      rv = getScreenText( 0xFC00, 24, 40, 40, x1, y1, x2, y2 );
    } else {
      rv = getScreenText( 0xFD00, 8, 13, 16, x1, y1, x2, y2 );
    }
    return rv;
  }


  public Z8 getZ8()
  {
    return this.z8;
  }


  public boolean isMonochrome()
  {
    return this.monochrome;
  }


  public static boolean isRamInitZero()
  {
    return AppContext.getBooleanProperty(
				PROP_RAM_INIT_ZERO,
				DEFAULT_RAM_INIT_ZERO );
  }


  public static boolean isRegInitZero()
  {
    return AppContext.getBooleanProperty(
				PROP_Z8_REG_INIT_ZERO,
				DEFAULT_Z8_REG_INIT_ZERO );
  }


  public boolean isScreenOutputEnabled()
  {
    boolean rv = (this.osType == OSType.ES40);
    if( !rv ) {
      if( (this.z8.getTotalCycles() - this.lastScreenCycles) < 400000 ) {
	rv = true;
      }
    }
    return rv;
  }


  public boolean keyPressed( Key key, boolean shiftDown )
  {
    boolean rv = false;
    synchronized( this.keyMatrixCols ) {
      resetKeyMatrixStatus();
      if( this.osType == OSType.ES23 ) {
	int keyValue = 0;
	switch( key ) {
	  case LEFT:
	    keyValue = 1;
	    break;
	  case RIGHT:
	    keyValue = 2;
	    break;
	  case UP:
	    keyValue = 3;
	    break;
	  case DOWN:
	    keyValue = 4;
	    break;
	  case HOME:
	    keyValue = 5;
	    break;
	  case DELETE:
	    keyValue = 7;
	    break;
	  case BACK_SPACE:
	    keyValue = 8;
	    break;
	  case INSERT:
	    keyValue = 9;
	    break;
	  case CLEAR:
	    keyValue = 0x0C;
	    break;
	  case ENTER:
	    keyValue = 0x0D;
	    break;
	}
	rv = setKeyMatrixStatusES23( keyValue );
      }
      else if( this.osType == OSType.ES40 ) {
	switch( key ) {
	  case LEFT:
	    rv = setKeyMatrixStatusES40( 1 );
	    break;
	  case RIGHT:
	    rv = setKeyMatrixStatusES40( 2 );
	    break;
	  case UP:
	    rv = setKeyMatrixStatusES40( 3 );
	    break;
	  case DOWN:
	    rv = setKeyMatrixStatusES40( 4 );
	    break;
	  case HOME:
	    rv = setKeyMatrixStatusES40( 5 );
	    break;
	  case DELETE:
	    rv = setKeyMatrixStatusES40( 7 );
	    break;
	  case BACK_SPACE:
	    rv = setKeyMatrixStatusES40( 8 );
	    break;
	  case INSERT:
	    rv = setKeyMatrixStatusES40( 9 );
	    break;
	  case CLEAR:
	    rv = setKeyMatrixStatusES40( 0x0C );
	    break;
	  case ENTER:
	    rv = setKeyMatrixStatusES40( 0x0D );
	    break;
	  case ESCAPE:
	    rv = setKeyMatrixStatusES40( 0x0E );
	    break;
	  case F1:
	    this.keyMatrixCols[ 1 ] = 0x04;
	    this.keyMatrixCols[ 2 ] = 0x04;
	    rv                      = true;
	    break;
	  case F2:
	    this.keyMatrixCols[ 1 ] = 0x04;
	    this.keyMatrixCols[ 3 ] = 0x04;
	    rv                      = true;
	    break;
	  case F3:
	    this.keyMatrixCols[ 1 ] = 0x04;
	    this.keyMatrixCols[ 4 ] = 0x04;
	    rv                      = true;
	    break;
	  case F4:
	    this.keyMatrixCols[ 1 ] = 0x04;
	    this.keyMatrixCols[ 5 ] = 0x04;
	    rv                      = true;
	    break;
	  case F5:
	    this.keyMatrixCols[ 1 ] = 0x04;
	    this.keyMatrixCols[ 6 ] = 0x04;
	    rv                      = true;
	    break;
	  case F6:
	    this.keyMatrixCols[ 1 ] = 0x04;
	    this.keyMatrixCols[ 7 ] = 0x04;
	    rv                      = true;
	    break;
	  case F7:
	    this.keyMatrixCols[ 1 ] = 0x04;
	    this.keyMatrixCols[ 8 ] = 0x04;
	    rv                      = true;
	    break;
	  case F8:
	    this.keyMatrixCols[ 1 ] = 0x04;
	    this.keyMatrixCols[ 9 ] = 0x04;
	    rv                      = true;
	    break;
	}
      } else {
	switch( key ) {
	  case LEFT:
	    setShift( false );
	    this.keyMatrixCols[ 1 ] = 0x04;
	    rv                      = true;
	    break;
	  case RIGHT:
	    setShift( true );
	    this.keyMatrixCols[ 1 ] = 0x04;
	    rv                      = true;
	    break;
	  case UP:
	    if( this.osType == OSType.OS2K ) {
	      setShift( true );
	      this.keyMatrixCols[ 1 ] = 0x08;
	      rv                      = true;
	    }
	    break;
	  case DOWN:
	    if( this.osType == OSType.OS2K ) {
	      setShift( false );
	      this.keyMatrixCols[ 1 ] = 0x08;
	      rv                      = true;
	    }
	    break;
	  case BACK_SPACE:
	    setShift( false );
	    this.keyMatrixCols[ 12 ] = 0x08;
	    rv                       = true;
	    break;
	  case SPACE:
	    setShift( false );
	    this.keyMatrixCols[ 1 ] = 0x01;
	    rv                      = true;
	    break;
	  case ENTER:
	    setShift( shiftDown );
	    this.keyMatrixCols[ 12 ] = 0x01;
	    rv                       = true;
	    break;
	  case ESCAPE:
	    setShift( false );
	    this.keyMatrixCols[ 13 ] = 0x01;
	    rv                       = true;
	    break;
	}
      }
      if( rv ) {
	this.ignoreKeyChar = true;
      }
    }
    return rv;
  }


  public void keyReleased()
  {
    synchronized( this.keyMatrixCols ) {
      resetKeyMatrixStatus();
      this.ignoreKeyChar = false;
    }
  }


  public boolean keyTyped( char ch )
  {
    return keyTyped( ch, false );
  }


  public boolean keyTyped( char ch, boolean forceCase )
  {
    boolean rv = false;
    synchronized( this.keyMatrixCols ) {
      if( this.ignoreKeyChar ) {
	this.ignoreKeyChar = false;
      } else {
	if( this.osType == OSType.ES23 ) {
	  if( (ch >= 'A') && (ch <= 'Z') ) {
	    ch = (char) (ch - 'A' + 'a');
	  }
	  rv = setKeyMatrixStatusES23( ch );
	} else if( this.osType == OSType.ES40 ) {
	  if( !forceCase ) {
	    if( (ch >= 'A') && (ch <= 'Z') ) {
	      ch = (char) (ch - 'A' + 'a');
	    }
	    else if( (ch >= 'a') && (ch <= 'z') ) {
	      ch = (char) (ch - 'a' + 'A');
	    }
	  }
	  rv = setKeyMatrixStatusES40( ch );
	} else {
	  rv = setKeyMatrixStatus2kOrES1988( ch );
	}
      }
    }
    return rv;
  }


  public void setErrorViewer( ErrorViewer errorViewer )
  {
    this.errorViewer = errorViewer;
  }


  public void setLoudspeaker( AudioWriter audioWriter )
  {
    this.loudspeaker = audioWriter;
    checkSetZ8PreExecInstListener();
  }


  public void setResetListener( Z8Listener listener )
  {
    this.resetListener = listener;
  }


  public synchronized void settingsChanged(
				ExtROM[] extROMs,
				ExtROM   romBank )
  {
    boolean forcePowerOn = false;

    int maxGPRNum = readMaxGPRNum();
    if( maxGPRNum != this.z8.getMaxGPRNum() ) {
      this.newMaxGPRNum = maxGPRNum;
      forcePowerOn      = true;
    }

    int ramSize = readRamSize();
    if( ramSize != this.ramSize ) {
      this.newRamSize = ramSize;
      forcePowerOn    = true;
    }

    OSType osType = readOSType();
    if( osType != this.osType ) {
      this.newOSType = osType;
      forcePowerOn   = true;
    }

    if( !supportsROMBank( osType ) ) {
      romBank = null;
    }
    if( JTCUtil.differsExtROM( this.romBank, romBank ) ) {
      if( romBank != null ) {
	romBank.setBegAddr( 0 );
	this.newRomBank = new ExtROM[] { romBank };
      } else {
	this.newRomBank = new ExtROM[ 0 ];
      }
      forcePowerOn = true;
    }

    if( JTCUtil.differsExtROMs( this.extROMs, extROMs ) ) {
      this.newExtROMs = (extROMs != null ? extROMs : new ExtROM[ 0 ]);
      forcePowerOn    = true;
    }

    this.z8.setRegInitZero( isRegInitZero() );
    if( forcePowerOn ) {
      fireReset( true );
    }
  }


  public void setTapeReader( AudioReader audioReader )
  {
    this.tapeReader = audioReader;
    checkSetZ8PreExecInstListener();
  }


  public void setTapeWriter( AudioWriter audioWriter )
  {
    this.tapeWriter = audioWriter;
    checkSetZ8PreExecInstListener();
  }


  public void setScreen( JTCScreen screen )
  {
    this.screen = screen;
  }


  public void startPastingText( String text, PasteObserver observer )
  {
    boolean done = false;
    if( text != null ) {
      if( !text.isEmpty() ) {
	cancelPastingText();
	this.pasteWorker = new PasteWorker( this, text, observer );
	this.pasteWorker.start();
	done = true;
      }
    }
    if( !done && (observer != null) ) {
      observer.pastingFinished();
    }
  }


  public boolean supportsLoudspeaker()
  {
    return (this.osType == OSType.OS2K);
  }


  public static boolean supportsROMBank( OSType osType )
  {
    return (osType == OSType.ES40);
  }


	/* --- Z8IO --- */

  @Override
  public int getPortValue( int port )
  {
    int rv = 0xFF;
    if( port == 3 ) {
      rv &= ~0x04;			// P32=0
      if( !this.tapeInPhase ) {
	rv &= ~0x01;			// P30=0
      }
    }
    return rv;
  }


  @Override
  public void setPortValue( int port, int value )
  {
    if( port == 3 ) {
      if( ((value & 0x20) == 0) && ((this.port3Value & 0x20) != 0) ) {
	this.romBankNum = 0;
      }
      else if( ((value & 0x80) == 0) && ((this.port3Value & 0x80) != 0) )
      {
	this.romBankNum = (this.romBankNum + 1) & this.romBankMask;
      }
      this.port3Value = value;

      if( this.osType == OSType.OS2K ) {
	value &= 0xC0;
	this.tapeOutPhase     = (value != 0xC0);
	this.loudspeakerPhase = (value != 0x40);
      } else {
	this.tapeOutPhase = ((value & 0x40) != 0);
      }
    }
  }


	/* --- Z8Listener --- */

  @Override
  public void z8Update( Z8 z8, Z8Listener.Reason reason )
  {
    if( z8 == this.z8 ) {
      switch( reason ) {
	case PRE_INST_EXEC:
	  if( (z8.getPC() == ADDR_VIDEO_INTERRUPT)
	      && (this.osType != OSType.ES40) )
	  {
	    this.lastScreenCycles = z8.getTotalCycles();
	  }
	  AudioReader tapeReader = this.tapeReader;
	  if( tapeReader != null ) {
	    if( this.osType == OSType.OS2K ) {
	      this.tapeInPhase = tapeReader.readVolumeStatus();
	    } else {
	      this.tapeInPhase = tapeReader.readPhase();
	    }
	  }
	  AudioWriter tapeWriter = this.tapeWriter;
	  if( tapeWriter != null ) {
	    tapeWriter.writePhase( this.tapeOutPhase );
	  }
	  AudioWriter loudspeaker = this.loudspeaker;
	  if( loudspeaker != null ) {
	    loudspeaker.writePhase( this.loudspeakerPhase );
	  }
	  break;

	case POWER_ON:
	case RESET:
	  resetInternal(
		reason == Z8Listener.Reason.POWER_ON,
		AppContext.getBooleanProperty( PROP_ROM_RELOAD, false ) );
	  Z8Listener listener = this.resetListener;
	  if( listener != null ) {
	    listener.z8Update( z8, reason );
	  }
	  break;
      }
    }
  }


	/* --- Z8Memory --- */

  @Override
  public int getMemByte( int addr, boolean dataMem )
  {
    addr &= 0xFFFF;

    int     rv   = 0xFF;
    boolean done = false;
    if( (addr >= 0x2000) && (addr < 0x4000) ) {
      ExtROM romBank = this.romBank;
      if( romBank != null ) {
	rv = romBank.getByte( (addr - 0x2000) + (this.romBankNum * 0x2000) );
	done = true;
      }
    }
    if( !done ) {
      for( ExtROM rom : this.extROMs ) {
	if( (addr >= rom.getBegAddr()) && (addr <= rom.getEndAddr()) ) {
	  rv   = rom.getByte( addr );
	  done = true;
	  break;
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
	    rv &= this.ramV[ idx ];
	  }
	  if( this.videoB && (idx < this.ramB.length) ) {
	    rv &= this.ramB[ idx ];
	  }
	  if( this.videoG && (idx < this.ramG.length) ) {
	    rv &= this.ramG[ idx ];
	  }
	  if( this.videoR && (idx < this.ramR.length) ) {
	    rv &= this.ramR[ idx ];
	  }
	} else {
	  if( addr >= 0x10000 - this.ramSize ) {
	    rv = this.ram[ addr ];
	  }
	}
      } else if( (addr >= 0x6000) && (addr < 0x8000) ) {
	if( (this.osType != OSType.ES40) || (addr >= 0x7000) ) {
	  synchronized( this.keyMatrixCols ) {
	    rv = (this.keyMatrixCols[ addr & 0x000F ] << 4) | 0x0F;
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
  public boolean setMemByte( int addr, boolean dataMem, int v )
  {
    addr &= 0xFFFF;

    boolean  rv      = false;
    boolean  done    = false;
    if( (addr >= 0x2000) && (addr < 0x4000) && (this.romBank != null) ) {
      done = true;
    }
    if( !done ) {
      for( ExtROM rom : this.extROMs ) {
	if( (addr >= rom.getBegAddr()) && (addr <= rom.getEndAddr()) ) {
	  done = true;
	  break;
	}
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
	  if( dirty  ) {
	    setScreenDirty();
	  }
	} else {
	  if( addr >= ramBegAddr ) {
	    this.ram[ addr ] = (byte) v;
	    rv               = true;
	  }
	}
      } else if( (addr >= 0x6000) && (addr < 0x8000) ) {
	if( (this.osType == OSType.ES40) && (addr < 0x6400) ) {
	  this.videoV = ((addr & 0x0010) == 0);
	  this.videoB = ((addr & 0x0020) == 0);
	  this.videoG = ((addr & 0x0040) == 0);
	  this.videoR = ((addr & 0x0080) == 0);
	}
      }
      else if( addr >= 0x8000 ) {
	if( (ramSize == 0x0400) && (addr >= 0xE000) ) {
	  int a = addr & 0x03FF;
	  if( a >= 0x0100 ) {
	    this.ram[ 0xFD00 - 0x0100 + a ] = (byte) v;
	    if( (a >= 0x0200)
		&& ((this.osType == OSType.OS2K)
			|| (this.osType == OSType.ES1988)) )
	    {
	      setScreenDirty();
	    }
	  } else {
	    this.ram[ 0xE000 + a ] = (byte) v;
	  }
	  if( this.osType == OSType.ES23 ) {
	    setScreenDirty();
	  }
	  rv = true;
	}
	if( (ramSize == 0x0800) && (addr >= 0xE000) ) {
	  int a = addr & 0x7FF;
	  if( a >= 0x0500 ) {
	    this.ram[ 0xFD00 - 0x0500 + a ] = (byte) v;
	    if( (a >= 0x0600)
		&& ((this.osType == OSType.OS2K)
			|| (this.osType == OSType.ES1988)) )
	    {
	      setScreenDirty();
	    }
	  } else {
	    this.ram[ 0xE000 + a ] = (byte) v;
	  }
	  if( this.osType == OSType.ES23 ) {
	    setScreenDirty();
	  }
	  rv = true;
	}
	else if( addr >= ramBegAddr ) {
	  this.ram[ addr ] = (byte) v;
	  rv               = true;
	  if( ((addr >= 0xFE00)
			&& ((this.osType == OSType.OS2K)
				|| (this.osType == OSType.ES1988)))
	      || ((addr >= 0xF800) && (this.osType == OSType.ES23)) )
	  {
	    setScreenDirty();
	  }
	}
      }
    }
    return rv;
  }


	/* --- private Methoden --- */

  private void buildRomBankMask()
  {
    int mask = 0;
    if( this.romBank != null ) {
      int nBanks = (this.romBank.size() + 0x1FFF) / 0x2000;
      if( nBanks > 0 ) {
	--nBanks;
	while( (nBanks | mask) != mask ) {
	  mask = (mask << 1) | 1;
	}
      }
    }
    this.romBankMask = mask;
  }


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


  private void checkSetZ8PreExecInstListener()
  {
    if( (this.osType != OSType.ES40)
	|| (this.tapeReader != null)
	|| (this.tapeWriter != null)
	|| (this.loudspeaker != null) )
    {
      this.z8.setPreInstExecListener( this );
    } else {
      this.z8.setPreInstExecListener( null );
    }
  }


  private String getScreenText(
			int begAddr,
			int nRows,
			int nCols,
			int rowDist,
			int x1,
			int y1,
			int x2,
			int y2 )
  {
    StringBuilder buf = new StringBuilder( nRows * (nCols + 1) );
    int           nNL = 0;
    for( int i = y1; i < nRows; i++ ) {
      if( i > y2 ) {
	break;
      }
      if( i > y1 ) {
	nNL++;
      }
      int addr    = begAddr + (i * rowDist);
      int nSpaces = 0;
      for( int k = 0; k < nCols; k++ ) {
	if( (i == y2) && (k > x2) ) {
	  break;
	}
	int b = getMemByte( addr++, false );
	if( (i > y1) || (k >= x1) )  {
	  if( b == 0x20 ) {
	    nSpaces++;
	  } else {
	    if( this.osType == OSType.ES40 ) {
	      switch( b ) {
		case 0x1A:			// ae
		  b = '\u00E4';
		  break;
		case 0x1B:			// oe
		  b = '\u00F6';
		  break;
		case 0x1C:			// ue
		  b = '\u00FC';
		  break;
		case 0x1D:			// Ae
		  b = '\u00C4';
		  break;
		case 0x1E:			// Oe
		  b = '\u00D6';
		  break;
		case 0x1F:			// Ue
		  b = '\u00DC';
		  break;
		case 0x7F:			// Ae
		  b = '\u00DF';
		  break;
		default:
		  if( (b < 0x1A) || (b > 0x7F) ) {
		    b = '_';
		  }
	      }
	    } else {
	      if( (b < 0x20) || (b > 0x7E) ) {
		b = '_';
	      } else {
		if( (this.osType == OSType.OS2K)
		    || (this.osType == OSType.ES1988) )
		{
		  if( b == 0x5C ) {
		    b = 0x5E;
		  } else if( b == 0x5E ) {
		    b = 0x5C;
		  }
		}
	      }
	    }
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
    }
    return buf.toString();
  }


  private void initRAM()
  {
    if( isRamInitZero() ) {
      Arrays.fill( this.ram, (byte) 0 );
      Arrays.fill( this.ramV, (byte) 0 );
      Arrays.fill( this.ramB, (byte) 0 );
      Arrays.fill( this.ramG, (byte) 0 );
      Arrays.fill( this.ramR, (byte) 0 );
    } else {
      this.random.nextBytes( this.ram );
      this.random.nextBytes( this.ramV );
      this.random.nextBytes( this.ramB );
      this.random.nextBytes( this.ramG );
      this.random.nextBytes( this.ramR );
    }
  }


  private static ExtROM[] readExtROMs( StringBuilder outErrText )
  {
    ExtROM[] roms = null;
    int      n    = AppContext.getIntProperty( PROP_ROM_COUNT, 0 );
    if( n > 0 ) {
      java.util.List<ExtROM> list = new ArrayList<>( n );
      for( int i = 0; i < n; i++ ) {
	String addrText = AppContext.getProperty( getROMPropAddr( i ) );
	String fileName = AppContext.getProperty( getROMPropFile( i ) );
	if( (addrText != null) && (fileName != null) ) {
	  int addr = -1;
	  try {
	    if( addrText.startsWith( "%" ) ) {
	      addr = Integer.parseInt( addrText.substring( 1 ), 16 );
	    } else {
	      addr = Integer.parseInt( addrText );
	    }
	    if( (addr >= 0) && !fileName.isEmpty() ) {
	      ExtROM rom = new ExtROM( new File( fileName ), MAX_ROM_SIZE );
	      rom.setBegAddr( addr );
	      list.add( rom );
	    }
	  }
	  catch( NumberFormatException ex ) {}
	  catch( IOException ex ) {
	    if( outErrText != null ) {
	      if( outErrText.length() > 0 ) {
		outErrText.append( "\n\n" );
	      }
	      outErrText.append(
		String.format(
			"Laden der externen ROM-Datei an Adresse %%%04X:\n",
			addr ) );
	      outErrText.append( ex.getMessage() );
	    }
	  }
	}
      }
      roms = list.toArray( new ExtROM[ list.size() ] );
    }
    return roms != null ? roms : new ExtROM[ 0 ];
  }


  private static int readMaxGPRNum()
  {
    return AppContext.getBooleanProperty( PROP_Z8_REGS_80_TO_EF, false ) ?
								0xEF : 0x7F;
  }


  private static OSType readOSType()
  {
    OSType osType = OSType.OS2K;
    String text   = AppContext.getProperty( PROP_OS );
    if( text != null ) {
      for( OSType tmpOSType : OSType.values() ) {
	if( text.equals( tmpOSType.toString() ) ) {
	  osType = tmpOSType;
	  break;
	}
      }
    }
    return osType;
  }


  private static int readRamSize()
  {
    int    ramSize = 0x8000;		// Standard: 32 KByte
    String text    = AppContext.getProperty( PROP_RAM_SIZE );
    if( text != null ) {
      try {
	text    = text.toUpperCase().trim();
	int idx = text.indexOf( 'H' );
	if( idx > 0 ) {
	  ramSize = Integer.parseInt(
				text.substring( 0, idx ).trim(),
				16 );
	} else {
	  idx = text.indexOf( 'K' );
	  if( idx > 0 ) {
	    ramSize = Integer.parseInt(
				text.substring( 0, idx ).trim() ) * 0x0400;
	  } else {
	    ramSize = Integer.parseInt( text );
	  }
	}
      }
      catch( NumberFormatException ex ) {}
    }
    return ramSize;
  }


  private byte[] readResource( String resource ) throws IOException
  {
    ByteArrayOutputStream buf  = new ByteArrayOutputStream( 0x0800 );
    boolean               done = false;
    InputStream           in   = null;
    try {
      in = getClass().getResourceAsStream( resource );
      if( in != null ) {
	int b = in.read();
	while( b >= 0 ) {
	  buf.write( b );
	  b = in.read();
	}
	done = true;
      }
    }
    catch( IOException ex ) {}
    finally {
      JTCUtil.closeSilently( in );
    }
    if( !done ) {
      throw new IOException( "Resource " + resource
				+ " kann nicht geladen werden." );
    }
    return buf.toByteArray();
  }


  private static ExtROM readRomBank( StringBuilder outErrText )
  {
    ExtROM romBank = null;
    if( AppContext.getBooleanProperty( PROP_ROMBANK_ENABLED, false ) ) {
      String file = AppContext.getProperty( PROP_ROMBANK_FILE );
      if( file != null ) {
	try {
	  if( !file.isEmpty() ) {
	    romBank = new ExtROM( new File( file ), MAX_ROMBANK_SIZE );
	  }
	}
	catch( IOException ex ) {
	  if( outErrText != null ) {
	    if( outErrText.length() > 0 ) {
	      outErrText.append( "\n\n" );
	    }
	    outErrText.append(
		"Laden der Datei f\u00FCr ROM-Bank:\n" );
	    outErrText.append( ex.getMessage() );
	  }
	}
      }
    }
    return romBank;
  }


  private void reload( ExtROM rom, String info, StringBuilder outErrText )
  {
    if( (rom != null)
	&& AppContext.getBooleanProperty( PROP_ROM_RELOAD, false ) )
    {
      try {
	rom.reload();
      }
      catch( IOException ex ) {
	if( outErrText != null ) {
	  if( outErrText.length() > 0 ) {
	    outErrText.append( "\n\n" );
	  }
	  outErrText.append( info );
	  outErrText.append( " konnte nicht erneut geladen werden.\n"
		+ "Es bleibt der alte Inhalt erhalten." );
	}
      }
    }
  }


  private synchronized void resetInternal(
				boolean powerOn,
				boolean reloadEnabled )
  {
    this.errorText      = null;
    this.port3Value     = 0xFF;
    this.romBankNum     = 0;
    this.shiftStatusSet = false;
    Arrays.fill( this.keyMatrixCols, 0 );

    StringBuilder outErrText = null;
    if( this.newExtROMs != null ) {
      this.extROMs    = this.newExtROMs;
      this.newExtROMs = null;
    }
    if( reloadEnabled && powerOn ) {
      if( this.extROMs.length > 0 ) {
	outErrText = new StringBuilder();
	for( ExtROM rom : this.extROMs ) {
	  reload(
	      rom,
	      String.format( "ROM an Adresse %%%04X", rom.getBegAddr() ),
	      outErrText );
	}
      }
    }
    if( this.newRomBank != null ) {
      if( this.newRomBank.length > 0 ) {
	this.romBank = this.newRomBank [ 0 ];
	buildRomBankMask();
      } else {
	this.romBank     = null;
	this.romBankMask = 0;
      }
      this.newRomBank = null;
    }
    if( reloadEnabled && powerOn && (this.romBank != null) ) {
      if( outErrText == null ) {
	outErrText = new StringBuilder();
      }
      reload( this.romBank, "ROM-Bank", outErrText );
      buildRomBankMask();
    }
    if( outErrText != null ) {
      if( outErrText.length() > 0 ) {
	this.errorText = outErrText.toString();
	ErrorViewer errorViewer = this.errorViewer;
	if( errorViewer != null ) {
	  errorViewer.showError( this.errorText );
	}
      }
    }
    if( this.newMaxGPRNum > 0 ) {
      this.z8.setMaxGPRNum( this.newMaxGPRNum );
      this.newMaxGPRNum = 0;
    }
    if( this.newRamSize > 0 ) {
      this.ramSize    = this.newRamSize;
      this.newRamSize = 0;
    }
    if( this.newOSType != null ) {
      if( this.newOSType != this.osType ) {
	powerOn = true;
      }
      this.osType    = this.newOSType;
      this.newOSType = null;
    }
    if( powerOn ) {
      initRAM();
    }

    int oldScreenWidth    = this.screenWidth;
    int oldScreenHeight   = this.screenHeight;
    boolean oldMonochrome = this.monochrome;
    switch( this.osType ) {
      case ES40:
	this.monochrome   = false;
	this.screenWidth  = 320;
	this.screenHeight = 192;
	this.charRaster   = new CharRaster( 40, 24, 8, 8 );
	if( this.romBank != null ) {
	  this.rom0800 = this.es40RomBank_0800;
	} else {
	  this.rom0800 = this.es40_0800;
	}
	this.rom2000 = new byte[ 0 ];
	break;
      case ES23:
	this.monochrome   = true;
	this.screenWidth  = 128;
	this.screenHeight = 128;
	this.charRaster   = new CharRaster( 16, 16, 8, 8 );
	this.rom0800      = this.es23_0800;
	this.rom2000      = new byte[ 0 ];
	break;
      case ES1988:
	this.monochrome   = true;
	this.screenWidth  = 64;
	this.screenHeight = 64;
	this.charRaster   = new CharRaster( 13, 8, 5, 7, 0, 7 );
	this.rom0800      = this.es1988_0800;
	this.rom2000      = this.es1988_2000;
	break;
      default:
	this.monochrome   = true;
	this.screenWidth  = 64;
	this.screenHeight = 64;
	this.charRaster   = new CharRaster( 13, 8, 5, 7, 0, 7 );
	this.rom0800      = this.os2k_0800;
	this.rom2000      = new byte[ 0 ];
    }
    JTCScreen screen = this.screen;
    if( screen != null ) {
      if( (this.monochrome != oldMonochrome)
	  || (this.screenWidth != oldScreenWidth)
	  || (this.screenHeight != oldScreenHeight) )
      {
	screen.screenConfigChanged();
      }
      screen.setScreenDirty();
    }
    checkSetZ8PreExecInstListener();
  }


  private void resetKeyMatrixStatus()
  {
    synchronized( this.keyMatrixCols ) {
      Arrays.fill( this.keyMatrixCols, 0 );
      if( this.shiftStatusSet ) {
	this.ram[ 0xFFFF ]  = (byte) 0xFF;
	this.shiftStatusSet = false;
      }
    }
  }


  private boolean setKeyMatrixStatus2kOrES1988( int ch )
  {
    boolean rv = false;
    resetKeyMatrixStatus();
    switch( ch ) {
      case 0x0A:
      case 0x1A:
	this.keyMatrixCols[ 1 ] = 0x08;
	rv                      = true;
	break;

      case 0x0B:
      case 0x1B:
	this.keyMatrixCols[ 1 ] = 0x04;
	rv                      = true;
	break;

      case 0x20:
	this.keyMatrixCols[ 1 ] = 0x01;
	rv                      = true;
	break;

      case 0x08:
      case 0x18:
	this.keyMatrixCols[ 12 ] = 0x08;
	rv                       = true;
	break;

      case 0x0D:
      case 0x7F:
	this.keyMatrixCols[ 12 ] = 0x01;
	rv                       = true;
	break;

      default:
	if( (ch >= 'A') && (ch <= 'Z') ) {
	  this.ram[ 0xFFFF ] = (byte) 0;
	} else {
	  ch = Character.toUpperCase( ch );
	}
	if( this.osType == OSType.OS2K ) {
	  rv = checkKeyMatrix( keyMatrix2kNormal, ch );
	  if( !rv ) {
	    if( checkKeyMatrix( keyMatrix2kShift, ch ) ) {
	      this.ram[ 0xFFFF ] = (byte) 0;
	      rv                 = true;
	    }
	  }
	} else {
	  rv = checkKeyMatrix( keyMatrixES1988Normal, ch );
	  if( !rv ) {
	    // Shift-Ebene identisch zum 2K-System
	    if( checkKeyMatrix( keyMatrix2kShift, ch ) ) {
	      this.ram[ 0xFFFF ] = (byte) 0;
	      rv                 = true;
	    }
	  }
	}
    }
    return rv;
  }


  private boolean setKeyMatrixStatusES23( int ch )
  {
    boolean rv = false;
    resetKeyMatrixStatus();
    switch( ch ) {
      case 1:
	this.keyMatrixCols[ 13 ] = 0x02;
	rv                       = true;
	break;

      case 2:
	this.keyMatrixCols[ 15 ] = 0x02;
	rv                       = true;
	break;

      case 3:
	this.keyMatrixCols[ 14 ] = 0x04;
	rv                       = true;
	break;

      case 4:
	this.keyMatrixCols[ 14 ] = 0x01;
	rv                       = true;
	break;

      case 5:
	this.keyMatrixCols[ 14 ] = 0x02;
	rv                       = true;
	break;

      case 7:
	this.keyMatrixCols[ 13 ] = 0x04;
	rv                       = true;
	break;

      case 8:
	this.keyMatrixCols[ 13 ] = 0x08;
	rv                       = true;
	break;

      case 9:
	this.keyMatrixCols[ 14 ] = 0x08;
	rv                       = true;
	break;

      case 0x0C:
	this.keyMatrixCols[ 15 ] = 0x08;
	rv                       = true;
	break;

      case 0x0D:
	this.keyMatrixCols[ 11 ] = 0x01;
	rv                       = true;
	break;

      default:
	rv = checkKeyMatrix( keyMatrixES23Normal, ch );
	if( !rv ) {
	  if( checkKeyMatrix( keyMatrixES23Shift, ch ) ) {
	    this.keyMatrixCols[ 0 ] = 0x01;
	    rv                      = true;
	  } else {
	    if( checkKeyMatrix( keyMatrixES23Control, ch ) ) {
	      this.keyMatrixCols[ 0 ] = 0x02;
	      rv                      = true;
	    }
	  }
	}
    }
    return rv;
  }


  private boolean setKeyMatrixStatusES40( int ch )
  {
    boolean rv = false;
    resetKeyMatrixStatus();
    if( ch == '\u00DF' ) {
      this.keyMatrixCols[ 1 ]  = 0x04;
      this.keyMatrixCols[ 12 ] = 0x08;
      rv                       = true;
    } else {
      rv = checkKeyMatrix( keyMatrixES40Normal, ch );
      if( !rv ) {
	if( checkKeyMatrix( keyMatrixES40Shift1, ch ) ) {
	  this.keyMatrixCols[ 1 ] = 0x01;
	  rv                      = true;
	} else {
	  if( checkKeyMatrix( keyMatrixES40Shift2, ch ) ) {
	    this.keyMatrixCols[ 1 ] = 0x02;
	    rv                      = true;
	  }
	}
      }
    }
    return rv;
  }


  private void setScreenDirty()
  {
    JTCScreen screen = this.screen;
    if( screen != null ) {
      screen.setScreenDirty();
    }
  }


  private void setShift( boolean state )
  {
    this.ram[ 0xFFFF ]  = (byte) (state ? 0 : 0xFF);
    this.shiftStatusSet = true;
  }
}
