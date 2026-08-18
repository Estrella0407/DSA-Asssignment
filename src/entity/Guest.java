/*
 * Module: Shared Entity Component (Guest & Reservation)
 * Author: WEI XIN
 * 
 * Description:
 * Entity class representing a Guest / Reservation profile in TARUMT Resorts.
 * Holds confirmation number, guest name, check-in status, guest type, assigned room,
 * billing details, and optional VIP loyalty member profile.
 */
package entity;

import java.io.Serializable;
import java.util.Objects;

public class Guest implements Serializable, Comparable<Guest> {

    // Tri-state lifecycle status - distinguishes "never checked in" from
    // "already checked out", which a plain boolean cannot express.
    public static final String STATUS_PENDING = "Pending";
    public static final String STATUS_CHECKED_IN = "Checked-In";
    public static final String STATUS_CHECKED_OUT = "Checked-Out";

    private String confirmationNumber; // Unique 8-digit confirmation number
    private String name;
    private String status; // one of STATUS_PENDING / STATUS_CHECKED_IN / STATUS_CHECKED_OUT
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

    /**
     * Kept for backward compatibility with existing call sites that pass a
     * boolean check-in flag. true maps to STATUS_CHECKED_IN, false maps to
     * STATUS_PENDING (use setStatus(...) directly if you need
     * STATUS_CHECKED_OUT at construction time).
     */
    public Guest(String confirmationNumber, String name, boolean checkInStatus,
            String type, Room assignedRoom, String billingDetails, Member memberProfile) {
        this.confirmationNumber = confirmationNumber;
        this.name = name;
        this.status = checkInStatus ? STATUS_CHECKED_IN : STATUS_PENDING;
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

    /**
     * Convenience boolean view of status, kept so existing code that only
     * cares "checked in or not" keeps working. Both STATUS_PENDING and
     * STATUS_CHECKED_OUT report false here - use getStatus() to tell them apart.
     */
    public boolean getCheckInStatus() {
        return STATUS_CHECKED_IN.equals(status);
    }

    /**
     * Kept for backward compatibility. true -> STATUS_CHECKED_IN,
     * false -> STATUS_PENDING. Prefer setStatus(...) if you need to set
     * STATUS_CHECKED_OUT explicitly.
     */
    public void setCheckInStatus(boolean checkInStatus) {
        this.status = checkInStatus ? STATUS_CHECKED_IN : STATUS_PENDING;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        if (!STATUS_PENDING.equals(status) && !STATUS_CHECKED_IN.equals(status)
                && !STATUS_CHECKED_OUT.equals(status)) {
            throw new IllegalArgumentException("Unknown guest status: " + status);
        }
        this.status = status;
    }

    public void checkIn() {
        this.status = STATUS_CHECKED_IN;
    }

    public void checkOut() {
        this.status = STATUS_CHECKED_OUT;
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
        if (other == null) {
            return -1;
        }

        // Priority Queue VIP Ranking: VIP members take priority over non-members
        if (this.memberProfile != null && other.memberProfile != null) {
            int memberComp = this.memberProfile.compareTo(other.memberProfile);
            if (memberComp != 0) {
                return memberComp;
            }
        } else if (this.memberProfile != null) {
            return -1; // this guest has VIP membership, goes first
        } else if (other.memberProfile != null) {
            return 1; // other guest has VIP membership, goes first
        }

        // Secondary fallback comparison by confirmation number
        if (this.confirmationNumber == null && other.confirmationNumber == null) {
            return 0;
        }
        if (this.confirmationNumber == null) {
            return 1;
        }
        if (other.confirmationNumber == null) {
            return -1;
        }
        return this.confirmationNumber.compareTo(other.getConfirmationNumber());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
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
        int pts = (memberProfile != null) ? memberProfile.getPoints() : 0;

        return String.format("Conf. No: %-8s | Name: %-15s | Type: %-7s | Tier: %-8s (%4d pts) | Room: %-6s | Status: %-10s | Billing: %s",
                confirmationNumber, name, type, tier, pts, roomNo, status, billingDetails);
    }
}
