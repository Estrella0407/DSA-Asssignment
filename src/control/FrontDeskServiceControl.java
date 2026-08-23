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
