/*
 * Module: Front-Desk Service (Control Component)
 * Author: MELAINE YANG MEI
 * 
 * Description:
 * Control class implementing business logic for Front-Desk Service inquiries.
 * Uses custom Dictionary / HashTable ADT and DoublyLinkedList to provide instant O(1) retrieval for guest identification,
 * room availability checks, and billing details.
 */
package control;

import adt.Dictionary;
import adt.DoublyLinkedList;
import entity.Guest;
import entity.Room;
import entity.StayRecord;
import java.time.LocalDate;

public class FrontDeskServiceControl {
    private GuestDirectory guestDirectory;
    private Dictionary<String, Room> roomTable;
    
    public FrontDeskServiceControl(GuestDirectory guestDirectory, Dictionary<String, Room> roomTable) {
        this.guestDirectory = guestDirectory;
        this.roomTable = roomTable;
    }
    
    public void addGuest(Guest guest) {
        guestDirectory.add(guest);
    }
    
    public void addRoom(Room room){
        roomTable.add(room.getRoomNumber(), room);
    }
    
    public Guest findGuest(String confirmationNumber) {
        return guestDirectory.find(confirmationNumber);
    }
    
    public Object[] getGuests() {
        return guestDirectory.getAllGuests();
    }
    
    public String getBillingDetails(String confirmationNumber){
        Guest guest = findGuest(confirmationNumber);
        
        if(guest == null){
            return "Guest not found";
        }
        return guest.getBillingDetails();
    } 
    
    public Room getGuestRoom(String confirmationNumber){
        Guest guest = findGuest(confirmationNumber);
        if(guest == null){
            return null;
        }
        return guest.getAssignedRoom();
    }
    
    public Room findRoom(String roomNumber){
        return roomTable.getValue(roomNumber);
    }

    /**
     * Partial / prefix room lookup: returns every room whose number starts with
     * the given text (case-insensitive), sorted ascending by room number.
     * e.g. "10" -> 101, 102, 103, 104, 105.
     */
    public DoublyLinkedList<Room> findRoomsByPrefix(String prefix){
        DoublyLinkedList<Room> matches = new DoublyLinkedList<>();
        if(prefix == null){
            return matches;
        }
        String needle = prefix.trim().toUpperCase();
        if(needle.isEmpty()){
            return matches;
        }

        Object[] all = roomTable.getValues();
        Room[] buffer = new Room[all.length];
        int found = 0;
        for(Object o : all){
            Room r = (Room) o;
            if(r != null && r.getRoomNumber() != null
                    && r.getRoomNumber().toUpperCase().startsWith(needle)){
                buffer[found++] = r;
            }
        }

        // Manual insertion sort by room number (ascending).
        for(int i = 1; i < found; i++){
            Room key = buffer[i];
            int j = i - 1;
            while(j >= 0 && buffer[j].getRoomNumber().compareToIgnoreCase(key.getRoomNumber()) > 0){
                buffer[j + 1] = buffer[j];
                j--;
            }
            buffer[j + 1] = key;
        }

        for(int i = 0; i < found; i++){
            matches.insertLast(buffer[i]);
        }
        return matches;
    }
    
    public Object[] getRooms(){
        return roomTable.getValues();
    }
    
    public boolean checkRoomAvailability(String roomNumber){
        Room room = roomTable.getValue(roomNumber);
        if(room == null){
            return false;
        }
        return room.isAvailable();
    }

    public DoublyLinkedList<Guest> getGuestList() {
        return guestDirectory.getGuestList();
    }

    /**
     * Moves a checked-in guest to a different (available, ready) room: releases
     * the old room (Available + Dirty), occupies the new one, and logs a
     * ROOM-CHANGED event so the transfer shows up in the stay-history timeline.
     */
    public boolean transferRoom(String confirmationNumber, String newRoomNumber) {
        Guest guest = findGuest(confirmationNumber);
        if (guest == null) {
            throw new IllegalArgumentException(
                    "No guest found with confirmation number \"" + confirmationNumber + "\".");
        }
        if (!guest.getCheckInStatus()) {
            throw new IllegalStateException(
                    "Guest \"" + guest.getName() + "\" is not currently checked in.");
        }
        Room newRoom = (newRoomNumber == null) ? null : roomTable.getValue(newRoomNumber.trim());
        if (newRoom == null) {
            throw new IllegalArgumentException("Room \"" + newRoomNumber + "\" does not exist.");
        }

        Room oldRoom = guest.getAssignedRoom();
        if (oldRoom != null && oldRoom.getRoomNumber().equalsIgnoreCase(newRoom.getRoomNumber())) {
            throw new IllegalStateException("Guest is already in room " + newRoom.getRoomNumber() + ".");
        }
        if (!newRoom.isAvailable() || !"Ready for Check-In".equalsIgnoreCase(newRoom.getCleaningStatus())) {
            throw new IllegalStateException(
                    "Room " + newRoom.getRoomNumber() + " is not 'Ready for Check-In' / available.");
        }

        String fromLabel = (oldRoom != null)
                ? oldRoom.getRoomNumber() + " (" + oldRoom.getRoomType() + ")"
                : "Unassigned";
        if (oldRoom != null) {
            oldRoom.setAvailability(true);
            oldRoom.setCleaningStatus("Dirty");
        }
        newRoom.setAvailability(false);
        guest.assignRoom(newRoom);

        guestDirectory.recordEvent(guest, StayRecord.EVENT_ROOM_CHANGED, "Transferred from " + fromLabel);
        return true;
    }

    /**
     * Chronological stay-history timeline, optionally narrowed by confirmation
     * number and/or date range. Appended to the Guest Occupancy Report.
     */
    public String getStayHistorySection(String confFilter, LocalDate fromDate, LocalDate toDate) {
        return guestDirectory.buildStayHistorySection(confFilter, fromDate, toDate);
    }

    public static double getRoomRate(String roomType) {
        String type = Room.normalizeRoomType(roomType);
        switch (type.toUpperCase()) {
            case "DOUBLE":
                return 220.00;
            case "DELUXE":
                return 350.00;
            case "SUITE":
                return 500.00;
            case "SINGLE":
            default:
                return 150.00;
        }
    }

    public static class BillingBreakdown {
        public double nightlyRate;
        public int stayDays;
        public double baseCharge;
        public double pointDiscount;
        public double subtotal;
        public double tax;
        public double total;

        public BillingBreakdown(double nightlyRate, int stayDays, double baseCharge, double pointDiscount, double subtotal, double tax, double total) {
            this.nightlyRate = nightlyRate;
            this.stayDays = stayDays;
            this.baseCharge = baseCharge;
            this.pointDiscount = pointDiscount;
            this.subtotal = subtotal;
            this.tax = tax;
            this.total = total;
        }
    }

    public BillingBreakdown calculateBilling(String confirmationNumber) {
        Guest guest = findGuest(confirmationNumber);
        if (guest == null) {
            return null;
        }

        String roomType = (guest.getAssignedRoom() != null)
                ? guest.getAssignedRoom().getRoomType()
                : (guest.getPreferredRoomType() != null ? guest.getPreferredRoomType() : Room.TYPE_SINGLE);

        double rate = getRoomRate(roomType);
        int days = Math.max(1, guest.getStayDays());
        double base = rate * days;
        double discount = 0.0;

        if (guest.isPointsRedeemed()) {
            // 2 days free stay credit
            discount = rate * Math.min(2, days);
        }

        double subtotal = Math.max(0.0, base - discount);
        double tax = subtotal * 0.06;
        double total = subtotal + tax;

        return new BillingBreakdown(rate, days, base, discount, subtotal, tax, total);
    }
}
