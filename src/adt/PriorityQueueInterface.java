/*
 * Module: Non-Linear ADT (Priority Queue Collection ADT Specification)
 * Author: WEI XIN
 * 
 * Description:
 * Interface for a Priority Queue Collection ADT. Entries are ordered based on
 * natural ordering or custom comparator priority levels (e.g. VIP loyalty tiers).
 */
package adt;

public interface PriorityQueueInterface<T extends Comparable<T>> {

    /**
     * Adds a new entry to the priority queue based on its priority level.
     *
     * @param newEntry The object to be added.
     * @return true if insertion was successful.
     */
    boolean enqueue(T newEntry);

    /**
     * Removes and returns the entry with the highest priority.
     *
     * @return The highest priority entry, or null if empty.
     */
    T dequeue();

    /**
     * Retrieves the entry with the highest priority without removing it.
     *
     * @return The highest priority entry, or null if empty.
     */
    T getMin(); // Or getMax depending on implementation

    /**
     * Retrieves the entry at the specified index without removing it.
     *
     * @param index The 0-based index in the queue.
     * @return The entry at index, or null if index is out of bounds.
     */
    T getEntry(int index);

    /**
     * Checks if the priority queue is empty.
     *
     * @return true if empty, false otherwise.
     */
    boolean isEmpty();

    /**
     * Gets the current number of entries in the priority queue.
     *
     * @return The number of entries.
     */
    int getSize();

    /**
     * Removes all entries from the priority queue.
     */
    void clear();

    /**
     * Removes and returns the entry at the specified index, shifting later
     * entries left to preserve priority order. Used when a lower-priority
     * entry needs to be serviced ahead of a higher-priority entry that is
     * currently blocked (e.g. its preferred room type isn't ready yet).
     *
     * @param index The 0-based index in the queue.
     * @return The entry at index, or null if index is out of bounds.
     */
    T dequeueAt(int index);
}
