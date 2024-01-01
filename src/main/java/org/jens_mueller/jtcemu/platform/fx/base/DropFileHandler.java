/*
 * (c) 2014-2019 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Schnittstelle fuer die Behandlung einer per Drag&Drop-Aktion
 * uebertragenen Datei
 */

package org.jens_mueller.jtcemu.platform.fx.base;

import java.io.File;


public interface DropFileHandler
{
  public boolean handleDroppedFile( Object target, File file );
}
