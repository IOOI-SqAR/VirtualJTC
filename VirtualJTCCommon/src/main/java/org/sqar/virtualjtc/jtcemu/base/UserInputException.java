/*
 * (c) 2014-2019 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Exception fuer eine fehlerhafte Benutzereingabe
 */

package org.sqar.virtualjtc.jtcemu.base;


public class UserInputException extends Exception
{
  public UserInputException(String msg )
  {
    super( msg );
  }
}
