/*
 * (c) 2019 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Thread zum Einfuegen von Text
 */

package jtcemu.base;


public class PasteWorker extends Thread
{
  private static final long WAIT_AFTER_ANY_KEY_MILLIS     = 150;
  private static final long WAIT_AFTER_ENTER_MILLIS       = 300;
  private static final long WAIT_AFTER_KEY_PRESSED_MILLIS = 20;

  private JTCSys           jtcSys;
  private String           text;
  private PasteObserver    observer;
  private volatile boolean running;


  public PasteWorker( JTCSys jtcSys, String text, PasteObserver observer )
  {
    super( AppContext.getAppName() + " paste worker" );
    this.jtcSys   = jtcSys;
    this.text     = text;
    this.observer = observer;
    this.running  = true;
  }


  public void fireStop()
  {
    this.running = false;
  }


	/* --- Runnable --- */

  @Override
  public void run()
  {
    try {
      String text = this.text;
      if( text != null ) {
	int len = text.length();
	int pos = 0;
	while( this.running && (pos < len) ) {
	  long ms = WAIT_AFTER_ANY_KEY_MILLIS;
	  char ch = text.charAt( pos++ );
	  if( (ch == '\r') || (ch == '\n') ) {
	    this.jtcSys.keyPressed( JTCSys.Key.ENTER, false );
	    ms = WAIT_AFTER_ENTER_MILLIS;
	  } else if( (ch >= '\u0000') && (ch <= '\u007E') ) {
	    this.jtcSys.keyTyped( ch, true );
	  }
	  sleep( WAIT_AFTER_KEY_PRESSED_MILLIS );
	  this.jtcSys.keyReleased();
	  sleep( ms );
	}
      }
    }
    catch( InterruptedException ex ) {}
    finally {
      if( this.observer != null ) {
	this.observer.pastingFinished();
      }
    }
  }
}
