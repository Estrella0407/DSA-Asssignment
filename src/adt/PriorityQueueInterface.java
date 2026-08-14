package adt;

/**
 * Interface for a Priority Queue Collection ADT.
 * Entries are ordered based on their natural ordering or priority.
 * 
 * @author Wei Xin
 * @param <T>
 */

public interface PriorityQueueInterface<T extends Comparable<T>> {

    /**
     * Adds a new entry to the priority queue based on its priority level.
     * @param newEntry The object to be added.
     * @return true if insertion was successful.
     */
    boolean enqueue(T newEntry);

    /**
     * Removes and returns the entry with the highest priority.
     * @return The highest priority entry, or null if empty.
     */
    T dequeue();

    /**
     * Retrieves the entry with the highest priority without removing it.
     * @return The highest priority entry, or null if empty.
     */
    T getMin(); // Or getMax depending on implementation

    /**
     * Checks if the priority queue is empty.
     * @return true if empty, false otherwise.
     */
    boolean isEmpty();

    /**
     * Gets the current number of entries in the priority queue.
     * @return The number of entries.
     */
    int getSize();

    /**
     * Removes all entries from the priority queue.
     */
    void clear();
}