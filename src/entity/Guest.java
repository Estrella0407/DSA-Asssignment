package entity;

import java.io.Serializable;
import java.util.Objects;

/**
 * Entity class representing a Guest/Reservation.
 * 
 * @author -
 */

public class Guest implements Serializable, Comparable<Guest> {

    private String confirmationNumber; // Unique 8-digit confirmation number
    private String name;
    private boolean checkInStatus; // true = checked in, false = checked out / pending
    private String type; // Walk-in / Booked
    private Room assignedRoom;
    private String billingDetails;
    private Member memberProfile; // Nullable if guest is not a loyalty member

    public Guest() {
        this("", "", false, "Walk-in", null, "", null);
    }

    public Guest(String confirmationNumber, String name, String type) {
        this(confirmationNumber, name, false, type, null, "Pending Billing", null);
    }

    public Guest(String confirmationNumber, String name, boolean checkInStatus, 
                 String type, Room assignedRoom, String billingDetails, Member memberProfile) {
        this.confirmationNumber = confirmationNumber;
        this.name = name;
        this.checkInStatus = checkInStatus;
        this.type = type;
        this.assignedRoom = assignedRoom;
        this.billingDetails = billingDetails;
        this.memberProfile = memberProfile;
    }

    public String getConfirmationNumber() {
        return confirmationNumber;
    }

    public void setConfirmationNumber(String confirmationNumber) {
        this.confirmationNumber = confirmationNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean getCheckInStatus() {
        return checkInStatus;
    }

    public void setCheckInStatus(boolean checkInStatus) {
        this.checkInStatus = checkInStatus;
    }

    public void checkIn() {
        this.checkInStatus = true;
    }

    public void checkOut() {
        this.checkInStatus = false;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Room getAssignedRoom() {
        return assignedRoom;
    }

    public void assignRoom(Room room) {
        this.assignedRoom = room;
    }

    public String getBillingDetails() {
        return billingDetails;
    }

    public void updateBillingDetails(String details) {
        this.billingDetails = details;
    }

    public Member getMemberProfile() {
        return memberProfile;
    }

    public void setMemberProfile(Member memberProfile) {
        this.memberProfile = memberProfile;
    }

    @Override
    public int compareTo(Guest other) {
        if (other == null || this.confirmationNumber == null) {
            return 0;
        }
        return this.confirmationNumber.compareTo(other.getConfirmationNumber());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Guest guest = (Guest) obj;
        return Objects.equals(confirmationNumber, guest.confirmationNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(confirmationNumber);
    }

    @Override
    public String toString() {
        String roomNo = (assignedRoom != null) ? assignedRoom.getRoomNumber() : "Unassigned";
        String tier = (memberProfile != null) ? memberProfile.getTierType() : "Non-Member";
        
        return String.format("Conf. No: %-8s | Name: %-15s | Type: %-7s | Tier: %-8s | Room: %-6s | Status: %s | Billing: %s",
                confirmationNumber, name, type, tier, roomNo, (checkInStatus ? "Checked-In" : "Pending"), billingDetails);
    }
}