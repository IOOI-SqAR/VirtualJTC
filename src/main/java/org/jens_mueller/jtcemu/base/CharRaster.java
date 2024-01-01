/*
 * (c) 2019 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Anordnung der Zeichen auf dem Bildschirm
 */

package org.jens_mueller.jtcemu.base;


public class CharRaster
{
  private int colCount;
  private int rowCount;
  private int charHeight;
  private int charWidth;
  private int xOffs;
  private int yOffs;


  public CharRaster(
		int colCount,
		int rowCount,
		int charWidth,
		int charHeight,
		int xOffs,
		int yOffs )
  {
    this.colCount   = colCount;
    this.rowCount   = rowCount;
    this.charWidth  = charWidth;
    this.charHeight = charHeight;
    this.xOffs      = xOffs;
    this.yOffs      = yOffs;
  }


  public CharRaster(
		int colCount,
		int rowCount,
		int charWidth,
		int charHeight )
  {
    this( colCount, rowCount, charWidth, charHeight, 0, 0 );
  }


  public int getCharHeight()
  {
    return this.charHeight;
  }


  public int getCharWidth()
  {
    return this.charWidth;
  }


  public int getColCount()
  {
    return this.colCount;
  }


  public int getRowCount()
  {
    return this.rowCount;
  }


  public int getXOffset()
  {
    return this.xOffs;
  }


  public int getYOffset()
  {
    return this.yOffs;
  }
}
