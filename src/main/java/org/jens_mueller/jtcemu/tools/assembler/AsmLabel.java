/*
 * (c) 2019-2021 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Daten einer Assemblermarke
 */

package jtcemu.tools.assembler;


public class AsmLabel implements Comparable<AsmLabel>
{
  private String labelName;
  private Object labelValue;

  public AsmLabel( String labelName, int labelValue )
  {
    this.labelName  = labelName;
    this.labelValue = Integer.valueOf( labelValue );
  }


  public String getLabelName()
  {
    return this.labelName;
  }


  public Object getLabelValue()
  {
    return this.labelValue;
  }


  public boolean hasIntValue()
  {
    boolean rv = false;
    if( this.labelValue != null ) {
      if( this.labelValue instanceof Number ) {
	rv = true;
      }
    }
    return rv;
  }


  public int intValue()
  {
    int rv = 0;
    if( this.labelValue != null ) {
      if( this.labelValue instanceof Number ) {
	rv = ((Number) this.labelValue).intValue();
      }
    }
    return rv;
  }


  public void setLabelValue( Object value )
  {
    this.labelValue = value;
  }


	/* --- Comparable --- */

  @Override
  public int compareTo( AsmLabel label )
  {
    return this.labelName.compareTo( label.getLabelName() );
  }
}
