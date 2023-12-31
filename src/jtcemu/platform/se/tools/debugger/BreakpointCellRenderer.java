/*
 * (c) 2019-2020 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * CellRenderer fuer einen Eintrag in einer Breakpoint-Liste
 */

package jtcemu.platform.se.tools.debugger;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Image;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.SwingConstants;
import jtcemu.platform.se.base.GUIUtil;


public class BreakpointCellRenderer extends DefaultListCellRenderer
{
  private static Image stopImg   = null;
  private static Icon  stopIcon  = null;
  private static Icon  blankIcon = null;


  public BreakpointCellRenderer()
  {
    if( stopImg == null ) {
      stopImg = GUIUtil.readImage( this, "/images/debug/bp_stop.png" );
    }
  }


	/* --- ueberschriebene Methoden --- */

  @Override
  public Component getListCellRendererComponent(
					JList<?> list,
					Object   value,
					int      index,
					boolean  isSelected,
					boolean  cellHasFocus )
  {
    Component c = super.getListCellRendererComponent(
						list,
						value,
						index,
						isSelected,
						cellHasFocus );
    if( (value != null) && (c != null) ) {
      if( (value instanceof AbstractBreakpoint) && (c instanceof JLabel) ) {
	Icon icon = null;
	if( ((AbstractBreakpoint) value).isEnabled() ) {
	  if( (stopIcon == null) && (stopImg != null) ) {
	    stopIcon = new ImageIcon( stopImg );
	  }
	  icon = stopIcon;
	} else {
	  if( (blankIcon == null) && (stopImg != null) ) {
	    blankIcon = new ImageIcon( stopImg )
				{
				  @Override
				  public void paintIcon(
						Component c,
						Graphics  g,
						int       x,
						int       y )
				  {
				    // kein Icon malen
				  }
				};
	  }
	  icon = blankIcon;
	}
	((JLabel) c).setHorizontalTextPosition( SwingConstants.TRAILING );
	((JLabel) c).setVerticalTextPosition( SwingConstants.CENTER );
	((JLabel) c).setIcon( icon );

	// den vollstaendigen Text als ToolTip anzeigen
	String s = null;
	if( value != null ) {
	  s = value.toString();
	}
	((JLabel) c).setToolTipText( s );
      }
    }
    return c;
  }
}
