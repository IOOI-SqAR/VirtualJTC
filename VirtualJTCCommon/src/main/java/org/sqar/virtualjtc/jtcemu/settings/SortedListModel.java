/*
 * (c) 2020 Jens Mueller
 *
 * Jugend+Technik-Computer-Emulator
 *
 * Sortiertes ListModel
 */

package org.sqar.virtualjtc.jtcemu.settings;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;


public class SortedListModel<E extends Comparable<? super E>>
					extends AbstractListModel<E>
{
  private java.util.List<E> list;


  public SortedListModel()
  {
    this.list = new ArrayList<>();
  }


  public void addElement( E e )
  {
    int idx = this.list.size();
    this.list.add( e );
    fireIntervalAdded( this, idx, idx );
    Collections.sort( this.list );
    fireContentsChanged( this, 0, idx );
  }


  public void clear()
  {
    int endIdx = this.list.size() - 1;
    this.list.clear();
    if( endIdx >= 0 ) {
      fireIntervalRemoved( this, 0, endIdx );
    }
  }


  public void fireContentsChanged()
  {
    int n = this.list.size();
    if( n > 0 ) {
      fireContentsChanged( this, 0, n - 1 );
    }
  }


  public void fireContentsChanged( int begIdx, int endIdx )
  {
    fireContentsChanged( this, begIdx, endIdx );
  }


  public int indexOf( E e )
  {
    return this.list.indexOf( e );
  }


  public boolean isEmpty()
  {
    return this.list.isEmpty();
  }


  public void remove( int idx )
  {
    this.list.remove( idx );
    if( idx >= 0 ) {
      fireIntervalRemoved( this, idx, idx );
    }
  }


  public void removeAll( Collection<?> c )
  {
    if( c != null ) {
      for( Object o : c ) {
	if( o != null ) {
	  int idx = this.list.indexOf( o );
	  if( idx >= 0 ) {
	    remove( idx );
	  }
	}
      }
    }
  }


  public E[] toArray( E[] a ) throws ArrayStoreException
  {
    return this.list.toArray( a );
  }


	/* --- ListModel --- */

  @Override
  public E getElementAt( int idx )
  {
    return this.list.get( idx );
  }


  @Override
  public int getSize()
  {
    return this.list.size();
  }
}
