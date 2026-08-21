/*
 * Module: Shared Entity Component
 * Author: WEI XIN
 * 
 * Description:
 * Entity class representing a Hotel Room in the resort inventory.
 * Tracks room number, cleaning status, and room availability.
 */
package entity;

import java.io.Serializable;
import java.util.Objects;

public class Room implements Serializable, Comparable<Room> {

    public static final String TYPE_SINGLE = "Single";
    public static final String TYPE_DOUBLE = "Double";
    public static final String TYPE_DELUXE = "Deluxe";
    public static final String TYPE_SUITE = "Suite";

    private String roomNumber;
    private String cleaningStatus; // Dirty, Cleaning In Progress, Inspected, Ready for Check-In
    private boolean isAvailable;
    private String roomType; // Single, Double, Deluxe, Suite

    public Room() {
        this("", "Ready for Check-In", true, TYPE_SINGLE);
    }

    /**
     * Backward-compatible constructor for existing call sites created before
     * room types were introduced. Defaults roomType to Single.
     */
    public Room(String roomNumber, String cleaningStatus, boolean isAvailable) {
        this(roomNumber, cleaningStatus, isAvailable, TYPE_SINGLE);
    }

    public Room(String roomNumber, String cleaningStatus, boolean isAvailable, String roomType) {
        this.roomNumber = roomNumber;
        this.cleaningStatus = cleaningStatus;
        this.isAvailable = isAvailable;
        this.roomType = normalizeRoomType(roomType);
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public String getCleaningStatus() {
        return cleaningStatus;
    }

    public void setCleaningStatus(String cleaningStatus) {
        if (cleaningStatus != null && !cleaningStatus.trim().isEmpty()) {
            this.cleaningStatus = cleaningStatus.trim();
        }
    }

    public boolean isRoomAvailable() {
        return isAvailable;
    }

    public boolean isAvailable() {
        return isAvailable;
    }

    public void setAvailability(boolean available) {
        this.isAvailable = available;
    }

    public String getRoomType() {
        return roomType;
    }

    public void setRoomType(String roomType) {
        this.roomType = normalizeRoomType(roomType);
    }

    /**
     * Normalizes free-text room type input into one of the four canonical
     * constants. Unknown or blank input defaults to Single. Public + static
     * so Control classes can reuse the same normalization when validating
     * guest room-type preferences.
     */
    public static String normalizeRoomType(String type) {
        if (type == null) {
            return TYPE_SINGLE;
        }
        switch (type.trim().toUpperCase()) {
            case "SINGLE":
                return TYPE_SINGLE;
            case "DOUBLE":
                return TYPE_DOUBLE;
            case "DELUXE":
                return TYPE_DELUXE;
            case "SUITE":
                return TYPE_SUITE;
            default:
                return TYPE_SINGLE;
        }
    }

    @Override
    public int compareTo(Room other) {
        if (other == null || this.roomNumber == null) {
            return 0;
        }
        return this.roomNumber.compareTo(other.roomNumber);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        Room room = (Room) obj;
        return Objects.equals(roomNumber, room.roomNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(roomNumber);
    }

    @Override
    public String toString() {
        return String.format("Room No: %-6s | Type: %-8s | Status: %-22s | Available: %s",
                roomNumber, roomType, cleaningStatus, isAvailable ? "Yes" : "No");
    }
}