/*
 * Author: LAW QINQI
 * Custom Linear ADT interface (linked-list based, FIFO/LIFO capable).
 * This is an original implementation written for this assignment and does
 * NOT use any interface or class from the Java Collections Framework.
 */
package adt;

public interface ADT<T> {

    /** Insert an item at the front of the list. */
    boolean insertFirst(T obj);

    /** Insert an item at the end of the list. */
    boolean insertLast(T obj);

    /** Remove and return the item at the front of the list. */
    T removeFirst();

    /** Remove and return the item at the end of the list. */
    T removeLast();

    /** Return (without removing) the item at the front of the list. */
    T retrieveFirst();

    /** Return (without removing) the item at the end of the list. */
    T retrieveLast();

    /** Return the item at the given 0-based index. */
    T getEntry(int index);

    /** Replace the item at the given 0-based index with a new value. */
    boolean replace(int index, T newObj);

    /** Remove the item at the given 0-based index. */
    T removeAt(int index);

    /** Remove all items from the list. */
    void clear();

    /** Whether the list currently has no items. */
    boolean isEmpty();

    /** Whether the list has reached its maximum capacity (if bounded). */
    boolean isFull();

    /** Current number of items stored. */
    int getNumberOfEntries();
}
