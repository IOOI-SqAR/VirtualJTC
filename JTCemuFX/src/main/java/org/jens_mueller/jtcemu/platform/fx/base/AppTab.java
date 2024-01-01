/*
 * (c) 2014-2019 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Schnittstelle fuer den Inhalt eines Tabs auf Applikationsebene
 */

package org.jens_mueller.jtcemu.platform.fx.base;

import javafx.event.Event;
import javafx.scene.control.MenuBar;
import javafx.scene.control.Tab;


public interface AppTab
{
  public MenuBar getMenuBar();
  public String  getTitle();
  public void    setTab( Tab tab );
  public void    tabClosed();
  public boolean tabCloseRequest( Event event );
  public void    tabSelected();
}
