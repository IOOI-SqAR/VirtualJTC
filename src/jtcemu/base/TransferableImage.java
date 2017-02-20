/*
 * (c) 2007-2010 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Uebertragbares Bild
 */

package jtcemu.base;

import java.awt.Image;
import java.awt.datatransfer.*;
import java.lang.*;


public class TransferableImage implements ClipboardOwner, Transferable
{
  private static final DataFlavor[] imgFlavorAry = { DataFlavor.imageFlavor };

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
    return imgFlavorAry;
  }


  @Override
  public boolean isDataFlavorSupported( DataFlavor flavor )
  {
    return flavor.equals( DataFlavor.imageFlavor );
  }
}
