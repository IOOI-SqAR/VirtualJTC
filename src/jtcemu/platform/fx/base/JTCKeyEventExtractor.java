/*
 * (c) 2021 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * EventDispatcher zum Herausfiltern der fuer den Emulator
 * relevanten Tastaturereignisse aus einer TabPane
 * und Weiterleiten an JTCNode
 *
 * Auf diese Art und Weise werden die Tastaturereignisse,
 * die normalerweise zur Steuerung der TabPane verwendet werden
 * (z.B. Cursor links und Cursor rechts), an den Emulator weitergeleitet.
 * Die TabPane kann aber trotzdem mit der Tastatur bedient werden,
 * indem man zusaetzlich die Alt- oder Meta-Taste drueckt.
 * Diese Klasse wandelt dazu solche KeyEvents in KeyEvents ohne
 * gedrueckte Alt- bzw. Meta-Taste um und gibt sie an die TabPane zurueck.
 */

package jtcemu.platform.fx.base;

import javafx.event.Event;
import javafx.event.EventDispatchChain;
import javafx.event.EventDispatcher;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import jtcemu.platform.fx.base.JTCNode;


public class JTCKeyEventExtractor implements EventDispatcher
{
  private TabPane         tabPane;
  private JTCNode         jtcNode;
  private EventDispatcher dispatcher;


  public JTCKeyEventExtractor( TabPane tabPane, JTCNode jtcNode )
  {
    this.tabPane    = tabPane;
    this.jtcNode    = jtcNode;
    this.dispatcher = tabPane.getEventDispatcher();
  }


  @Override
  public Event dispatchEvent( Event e, EventDispatchChain tail )
  {
    if( e != null ) {
      if( e instanceof KeyEvent ) {
	KeyEvent keyEvent = (KeyEvent) e;
	if( !keyEvent.isAltDown() && !keyEvent.isMetaDown() ) {
	  Tab tab = this.tabPane.getSelectionModel().getSelectedItem();
	  if( tab != null ) {
	    if( tab.getContent() == this.jtcNode ) {
	      this.jtcNode.handleKeyEvent( keyEvent );
	      e = null;
	    }
	  }
	}

	/*
	 * Tastatuebedienung der TabPane mit gedrueckter
	 * Alt- oder Meta-Taste ermoeglichen
	 */
	if( keyEvent.isAltDown() || keyEvent.isMetaDown() ) {
	  KeyCode keyCode = keyEvent.getCode();
	  if( (keyCode == KeyCode.LEFT)
	      || (keyCode == KeyCode.RIGHT) )
	  {
	    KeyEvent e2 = new KeyEvent(
				keyEvent.getSource(),
				keyEvent.getTarget(),
				keyEvent.getEventType(),
				keyEvent.getCharacter(),
				keyEvent.getText(),
				keyCode,
				keyEvent.isShiftDown(),
				keyEvent.isControlDown(),
				false,			// Alt
				false );		// Meta
	    e.consume();
	    e = e2;
	  }
	}
      }
    }
    if( e != null ) {
      if( this.dispatcher != null ) {
	e = this.dispatcher.dispatchEvent( e, tail );
      } else if( tail != null ) {
	e = tail.dispatchEvent( e );
      }
    }
    return e;
  }
}
