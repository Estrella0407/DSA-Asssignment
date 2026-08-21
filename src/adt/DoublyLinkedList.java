/*
Module: Linear ADT (Collection ADT Implementation)
Author: LAW QINQI, Neo Ai Yik

Description:
Doubly linked-list based implementation of the custom Linear ADT.
Used to manage walk-in / standard booking guests chronologically (insertLast to register a new guest, removeFirst to process the 
next guest in line - i.e. FIFO queue behaviour), housekeeping task logs (insertLast / removeLast for LIFO rollback), and shared room inventory.
*/
package adt;

import java.io.Serializable;

public class DoublyLinkedList<T> implements DoublyLinkedListInterface<T>, Serializable {

    // Private inner node class - not exposed outside this ADT implementation.
    private class Node implements Serializable {

        private T data;
        private Node prev;
        private Node next;

        private Node(T data) {
            this.data = data;
        }
    }

    private Node head;
    private Node tail;
    private int numberOfEntries;

    public DoublyLinkedList() {
        head = null;
        tail = null;
        numberOfEntries = 0;
    }

    @Override
    public boolean insertFirst(T obj) {
        if (obj == null) {
            return false;
        }
        Node newNode = new Node(obj);
        if (isEmpty()) {
            head = newNode;
            tail = newNode;
        } else {
            newNode.next = head;
            head.prev = newNode;
            head = newNode;
        }
        numberOfEntries++;
        return true;
    }

    @Override
    public boolean insertLast(T obj) {
        if (obj == null) {
            return false;
        }
        Node newNode = new Node(obj);
        if (isEmpty()) {
            head = newNode;
            tail = newNode;
        } else {
            tail.next = newNode;
            newNode.prev = tail;
            tail = newNode;
        }
        numberOfEntries++;
        return true;
    }

    @Override
    public T removeFirst() {
        if (isEmpty()) {
            return null;
        }
        T data = head.data;
        head = head.next;
        if (head == null) {
            tail = null;
        } else {
            head.prev = null;
        }
        numberOfEntries--;
        return data;
    }

    @Override
    public T removeLast() {
        if (isEmpty()) {
            return null;
        }
        T data = tail.data;
        tail = tail.prev;
        if (tail == null) {
            head = null;
        } else {
            tail.next = null;
        }
        numberOfEntries--;
        return data;
    }

    @Override
    public T retrieveFirst() {
        return isEmpty() ? null : head.data;
    }

    @Override
    public T retrieveLast() {
        return isEmpty() ? null : tail.data;
    }

    private Node getNode(int index) {
        if (index < 0 || index >= numberOfEntries) {
            return null;
        }
        Node current = head;
        for (int i = 0; i < index; i++) {
            current = current.next;
        }
        return current;
    }

    @Override
    public T getEntry(int index) {
        Node node = getNode(index);
        return node == null ? null : node.data;
    }

    @Override
    public boolean replace(int index, T newObj) {
        Node node = getNode(index);
        if (node == null || newObj == null) {
            return false;
        }
        node.data = newObj;
        return true;
    }

    @Override
    public T removeAt(int index) {
        Node node = getNode(index);
        if (node == null) {
            return null;
        }
        if (node == head) {
            return removeFirst();
        }
        if (node == tail) {
            return removeLast();
        }
        node.prev.next = node.next;
        node.next.prev = node.prev;
        numberOfEntries--;
        return node.data;
    }

    @Override
    public void clear() {
        head = null;
        tail = null;
        numberOfEntries = 0;
    }

    @Override
    public boolean isEmpty() {
        return numberOfEntries == 0;
    }

    @Override
    public boolean isFull() {
        // Unbounded linked structure - never full.
        return false;
    }

    @Override
    public int getNumberOfEntries() {
        return numberOfEntries;
    }
}
