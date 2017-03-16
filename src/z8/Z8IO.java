/*
 * (c) 2007 Jens Mueller
 *
 * Z8 Emulator
 *
 * Interface fuer Arbeitsspeicher
 */

package z8;


public interface Z8IO
{
  public int  getPortValue( int port );
  public void setPortValue( int port, int value );
}

