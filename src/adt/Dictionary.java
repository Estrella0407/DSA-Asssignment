/*
 * Module: Non-Linear ADT (Dictionary / Map ADT Specification)
 * Author: MELAINE YANG MEI
 * 
 * Description:
 * Interface for a Dictionary / Key-Value Map ADT.
 * Defines operations for key-value association, fast lookup, removal, and value retrieval.
 */
package adt;

public interface Dictionary<K, V> {
    public V add(K key, V value);
    
    public V remove(K key);
    
    public V getValue(K key);
    
    public boolean contains(K key);
    
    public boolean isEmpty();
    
    public boolean isFull();
    
    public int getSize();
    
    public void clear();
    
    public Object[] getValues();
}
