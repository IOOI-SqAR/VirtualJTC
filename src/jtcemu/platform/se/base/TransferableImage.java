/*
 * (c) 2007-2019 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Uebertragbares Bild
 */

package jtcemu.platform.se.base;

import java.awt.Image;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;


public class TransferableImage implements ClipboardOwner, Transferable
{
  private static final DataFlavor[] imgFlavors = { DataFlavor.imageFlavor };

  private Image image;


  public TransferableImage( Image image )
  {
    this.image = image;
  }


	/* --- ClipboardOwner --- */

  @Override
  public void lostOwnership( Clipboard clp, Transferable data )
  {
    // leer
  }


	/* --- Transferable --- */

  @Override
  public Object getTransferData( DataFlavor flavor )
					throws UnsupportedFlavorException
  {
    if( !isDataFlavorSupported( flavor ) ) {
      throw new UnsupportedFlavorException( flavor );
    }
    return this.image;
  }


  @Override
  public DataFlavor[] getTransferDataFlavors()
  {
    return imgFlavors;
  }


  @Override
  public boolean isDataFlavorSupported( DataFlavor flavor )
  {
    return flavor.equals( DataFlavor.imageFlavor );
  }
}
