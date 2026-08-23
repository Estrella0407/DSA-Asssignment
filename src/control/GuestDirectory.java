/*
 * Module: Shared Control Component
 * Author: WEI XIN
 * 
 * Description:
 * Central shared "guest database" used by every module (Walk-In, VIP Allocation, Front-Desk). One instance is created in MainMenuUI and
 * injected into each control that needs to register or look up guests - so there is exactly ONE place that knows how a guest gets stored;
 * every other control just calls add()/find() on it.
 */
package control;

import adt.Dictionary;
import adt.DoublyLinkedList;
import adt.HashTable;
import entity.Guest;

public class GuestDirectory {

    private final Dictionary<String, Guest> guestTable;
    private final DoublyLinkedList<Guest> guestList;

    public GuestDirectory() {
        this.guestTable = new HashTable<>();
        this.guestList = new DoublyLinkedList<>();
    }

    public void add(Guest guest) {
        if (guest == null || guest.getConfirmationNumber() == null
                || guest.getConfirmationNumber().trim().isEmpty()) {
            return;
        }
        guestTable.add(normalize(guest.getConfirmationNumber()), guest);
        guestList.insertLast(guest);
    }

    public Guest find(String confirmationNumber) {
        if (confirmationNumber == null) {
            return null;
        }
        return guestTable.getValue(normalize(confirmationNumber));
    }

    public boolean contains(String confirmationNumber) {
        if (confirmationNumber == null) {
            return false;
        }
        return guestTable.contains(normalize(confirmationNumber));
    }

    /** For report modules that need to iterate every guest in registration order. */
    public DoublyLinkedList<Guest> getGuestList() {
        return guestList;
    }

    public Object[] getAllGuests() {
        return guestTable.getValues();
    }

    private String normalize(String confirmationNumber) {
        return confirmationNumber.trim().toUpperCase();
    }
}