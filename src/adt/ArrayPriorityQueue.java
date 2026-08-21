/*
 * Module: Non-Linear ADT (Priority Queue Collection ADT Implementation)
 * Author: WEI XIN
 * 
 * Description:
 * Priority Queue ADT implementation maintaining a sorted array structure.
 * Insertion places elements in priority order (highest tier/priority first at index 0).
 */
package adt;

import java.io.Serializable;

public class ArrayPriorityQueue<T extends Comparable<T>> implements PriorityQueueInterface<T>, Serializable {

    private T[] array;
    private int size;
    private static final int DEFAULT_CAPACITY = 25;

    @SuppressWarnings("unchecked")
    public ArrayPriorityQueue() {
        this(DEFAULT_CAPACITY);
    }

    @SuppressWarnings("unchecked")
    public ArrayPriorityQueue(int initialCapacity) {
        size = 0;
        array = (T[]) new Comparable[initialCapacity];
    }

    @Override
    public boolean enqueue(T newEntry) {
        if (newEntry == null) {
            return false;
        }
        if (isArrayFull()) {
            doubleArray();
        }

        // Insert in sorted order (highest priority towards index 0)
        int i = size - 1;
        while (i >= 0 && newEntry.compareTo(array[i]) < 0) {
            array[i + 1] = array[i]; // Shift lower priority items back
            i--;
        }
        array[i + 1] = newEntry;
        size++;
        return true;
    }

    @Override
    public T dequeue() {
        if (isEmpty()) {
            return null;
        }
        T highestPriority = array[0];
        // Shift remaining items left
        for (int i = 0; i < size - 1; i++) {
            array[i] = array[i + 1];
        }
        array[size - 1] = null;
        size--;
        return highestPriority;
    }

    @Override
    public T getMin() {
        if (isEmpty()) {
            return null;
        }
        return array[0];
    }

    @Override
    public T getEntry(int index) {
        if (index < 0 || index >= size) {
            return null;
        }
        return array[index];
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public int getSize() {
        return size;
    }

    @Override
    public void clear() {
        for (int i = 0; i < size; i++) {
            array[i] = null;
        }
        size = 0;
    }

    private boolean isArrayFull() {
        return size == array.length;
    }

    @SuppressWarnings("unchecked")
    private void doubleArray() {
        T[] oldArray = array;
        array = (T[]) new Comparable[oldArray.length * 2];
        System.arraycopy(oldArray, 0, array, 0, oldArray.length);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < size; i++) {
            sb.append(array[i]).append("\n");
        }
        return sb.toString();
    }

    @Override
    public T dequeueAt(int index) {
        if (index < 0 || index >= size) {
            return null;
        }
        T removed = array[index];
        for (int i = index; i < size - 1; i++) {
            array[i] = array[i + 1];
        }
        array[size - 1] = null;
        size--;
        return removed;
    }
}
