/*
 * Module: Front-Desk Service (Control Component)
 * Author: MELAINE YANG MEI
 * 
 * Description:
 * Control class implementing business logic for Front-Desk Service inquiries.
 * Uses custom Dictionary / HashTable ADT to provide instant O(1) retrieval for guest identification,
 * room availability checks, and billing details.
 */
package control;

import adt.Dictionary;
import entity.Guest;
import entity.Room;

public class FrontDeskServiceControl {
    private Dictionary<String, Guest> guestTable;
    private Dictionary<String, Room> roomTable;
    
    public FrontDeskServiceControl(Dictionary<String, Guest> guestTable,Dictionary<String, Room> roomTable){
        this.guestTable = guestTable;
        this.roomTable = roomTable;
    }
    
    public void addGuest(Guest guest){
        guestTable.add(guest.getConfirmationNumber(), guest);
    }
    
    public void addRoom(Room room){
        roomTable.add(room.getRoomNumber(), room);
    }
    
    public Guest findGuest(String confirmationNumber){
        return guestTable.getValue(confirmationNumber);
    }
    
    public Object[] getGuests(){
        return guestTable.getValues();
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
}
