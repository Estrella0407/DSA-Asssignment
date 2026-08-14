/*Front-desk agents handle hundreds of simultaneous calls and walk-in inquiries. 
Query types involve room availability, billing details, or guest identification 
using a unique 8-digit confirmation number. Implement an efficient search algorithm 
capable of instantly retrieving complete guest information. 
*/

package control;
import adt.Dictionary;
import adt.HashTable;
import entity.Guest;
import entity.Room;

/**
 *
 * @author Melaine Yang Mei
 */
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
