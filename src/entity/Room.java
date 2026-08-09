package entity;

import java.io.Serializable;
import java.util.Objects;

/**
 * Entity class representing a Hotel Room.
 * 
 * @author -
 */

public class Room implements Serializable, Comparable<Room> {

    private String roomNumber;
    private String cleaningStatus; // Dirty, Cleaning In Progress, Inspected, Ready for Check-In
    private boolean isAvailable;

    public Room() {
        this("", "Ready for Check-In", true);
    }

    public Room(String roomNumber, String cleaningStatus, boolean isAvailable) {
        this.roomNumber = roomNumber;
        this.cleaningStatus = cleaningStatus;
        this.isAvailable = isAvailable;
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
        this.cleaningStatus = cleaningStatus;
    }

    public boolean isAvailable() {
        return isAvailable;
    }

    public void setAvailability(boolean available) {
        this.isAvailable = available;
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
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Room room = (Room) obj;
        return Objects.equals(roomNumber, room.roomNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(roomNumber);
    }

    @Override
    public String toString() {
        return String.format("Room No: %-6s | Status: %-22s | Available: %s",
                roomNumber, cleaningStatus, isAvailable ? "Yes" : "No");
    }
}