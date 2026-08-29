/*
 * Module: Shared Entity Component (Guest & Reservation)
 * Author: WEI XIN & LAW QINQI
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
    private String preferredRoomType; // nullable - null means "Any" (no preference)
    private int stayDays = 1; // Length of stay in days (>= 1)
    private boolean isPointsRedeemed; // True if 2-day point redemption was applied
    private int redeemedPoints; // Points deducted for the 2-day redemption
    private String billingDetails;
    private Member memberProfile; // Nullable if guest is not a loyalty member
    private String lastPromotionMessage; // set by applyLongStayPromotion(); null if no milestone reached this call
    private long queueEntryTime = 0L; // epoch millis when the guest joined a processing queue; 0 = not queued / not tracked

    public Guest() {
        this("", "", false, "Walk-in", null, "", null, 1);
    }

    public Guest(String confirmationNumber, String name, String type) {
        this(confirmationNumber, name, false, type, null, "Pending Billing", null, 1);
    }

    /**
     * Kept for backward compatibility with existing call sites. Defaults stayDays to 1.
     */
    public Guest(String confirmationNumber, String name, boolean checkInStatus,
            String type, Room assignedRoom, String billingDetails, Member memberProfile) {
        this(confirmationNumber, name, checkInStatus, type, assignedRoom, billingDetails, memberProfile, 1);
    }

    public Guest(String confirmationNumber, String name, boolean checkInStatus,
            String type, Room assignedRoom, String billingDetails, Member memberProfile, int stayDays) {
        this.confirmationNumber = confirmationNumber;
        this.name = name;
        this.status = checkInStatus ? STATUS_CHECKED_IN : STATUS_PENDING;
        this.type = type;
        this.assignedRoom = assignedRoom;
        this.billingDetails = billingDetails;
        this.memberProfile = memberProfile;
        this.stayDays = Math.max(1, stayDays);
        this.isPointsRedeemed = false;
        this.redeemedPoints = 0;
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

    public String getPreferredRoomType() {
        return preferredRoomType;
    }

    public void setPreferredRoomType(String preferredRoomType) {
        this.preferredRoomType = preferredRoomType;
    }

    public int getStayDays() {
        return stayDays;
    }

    public void setStayDays(int stayDays) {
        this.stayDays = Math.max(1, stayDays);
    }

    public boolean isPointsRedeemed() {
        return isPointsRedeemed;
    }

    public int getRedeemedPoints() {
        return redeemedPoints;
    }
    
    public String getLastPromotionMessage() {
        return lastPromotionMessage;
    }

    /**
     * Time recorded when the guest was placed into a processing queue
     * (0 if the guest has never been queued). Used by the Walk-In queue report
     * to compute how long each guest has been waiting.
     */
    public long getQueueEntryTime() {
        return queueEntryTime;
    }

    public void setQueueEntryTime(long queueEntryTime) {
        this.queueEntryTime = queueEntryTime;
    }

    /** Milliseconds the guest has been waiting in the queue, or 0 if untracked. */
    public long getWaitingMillis() {
        return (queueEntryTime > 0) ? Math.max(0, System.currentTimeMillis() - queueEntryTime) : 0L;
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

    /**
     * Calculates the loyalty points required to redeem a 2-Day Free Stay
     * based on room type:
     * - Single: 250 pts
     * - Double: 400 pts
     * - Deluxe: 600 pts
     * - Suite:  850 pts
     */
    public static int getRedemptionCostForRoomType(String roomType) {
        String type = Room.normalizeRoomType(roomType);
        switch (type.toUpperCase()) {
            case "DOUBLE":
                return 400;
            case "DELUXE":
                return 600;
            case "SUITE":
                return 850;
            case "SINGLE":
            default:
                return 250;
        }
    }

    /**
     * Attempts to redeem points for a 2-day free stay.
     * Returns true if successful and points deducted, false otherwise.
     */
    public boolean redeemPointsForStay(String targetRoomType) {
        if (memberProfile == null) {
            return false;
        }
        int cost = getRedemptionCostForRoomType(targetRoomType);
        if (memberProfile.getPoints() >= cost) {
            memberProfile.deductPoints(cost);
            this.isPointsRedeemed = true;
            this.redeemedPoints = cost;
            this.billingDetails = (this.billingDetails == null || this.billingDetails.isEmpty())
                    ? "Points Redeemed (2-Day Free Stay - " + cost + " pts)"
                    : this.billingDetails + " [2-Day Free Stay Redeemed: -" + cost + " pts]";
            return true;
        }
        return false;
    }

    /**
     * Evaluates graduated long-stay loyalty milestone promotions:
     * - >= 180 days: DIAMOND Member (+ 3000 bonus points)
     * - >= 90 days:  PLATINUM Member (+ 1800 bonus points)
     * - >= 60 days:  ELITE Member    (+ 1000 bonus points)
     * - >= 30 days:  GOLD Member     (+ 500 bonus points)
     * - >= 14 days:  SILVER Member   (+ 200 bonus points)
     * 
     * Returns a celebratory message if a milestone promotion occurred, or null if no threshold met.
     */
    public String applyLongStayPromotion() {
        if (this.stayDays < 15) {
            return null;
        }

        String targetTier;
        int bonusPoints;

        if (this.stayDays >= 180) {
            targetTier = "DIAMOND";
            bonusPoints = 1000;
        } else if (this.stayDays >= 90) {
            targetTier = "PLATINUM";
            bonusPoints = 600;
        } else if (this.stayDays >= 60) {
            targetTier = "ELITE";
            bonusPoints = 400;
        } else if (this.stayDays >= 30) {
            targetTier = "GOLD";
            bonusPoints = 200;
        } else {
            targetTier = "SILVER";
            bonusPoints = 100;
        }

        if (this.memberProfile == null) {
            String autoMemberId = "M-" + (this.confirmationNumber != null ? this.confirmationNumber.replace("-", "") : "1000");
            this.memberProfile = new Member(autoMemberId, targetTier, bonusPoints);
            lastPromotionMessage = String.format(
                    ">> Long-Stay Reward (%d days): Automatically enrolled as %s Member (%s) with %d bonus loyalty points!",
                    this.stayDays, targetTier, autoMemberId, bonusPoints);
            return lastPromotionMessage;
        } else {
            boolean upgraded = this.memberProfile.upgradeTierIfHigher(targetTier, bonusPoints);
            if (upgraded) {
                lastPromotionMessage = String.format(
                        ">> Long-Stay Reward (%d days): Upgraded to %s Member with +%d bonus loyalty points! (Total Points: %d)",
                        this.stayDays, targetTier, bonusPoints, this.memberProfile.getPoints());
            } else {
                lastPromotionMessage = String.format(
                        ">> Long-Stay Reward (%d days): Retained %s tier and awarded +%d bonus loyalty points! (Total Points: %d)",
                        this.stayDays, this.memberProfile.getTierType(), bonusPoints, this.memberProfile.getPoints());
            }
            return lastPromotionMessage;
        }
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

    public String toDetailedCard() {
        String roomNo = (assignedRoom != null)
                ? assignedRoom.getRoomNumber() + " (" + assignedRoom.getRoomType() + ")"
                : "Unassigned";
        String tier = (memberProfile != null)
                ? memberProfile.getTierType() + " (" + memberProfile.getPoints() + " pts)"
                : "Non-Member (0 pts)";
        String prefType = (preferredRoomType == null) ? "Any" : preferredRoomType;

        StringBuilder sb = new StringBuilder();
        sb.append("--------------------------------------------------\n");
        sb.append(String.format("Confirmation No : %s%n", confirmationNumber));
        sb.append(String.format("Guest Name      : %s%n", name));
        sb.append(String.format("Guest Type      : %s%n", type));
        sb.append(String.format("Stay Duration   : %d night(s)%n", stayDays));
        sb.append(String.format("Preferred Room  : %s%n", prefType));
        sb.append(String.format("Assigned Room   : %s%n", roomNo));
        sb.append(String.format("Member Tier     : %s%n", tier));
        sb.append(String.format("Status          : %s%n", status));
        sb.append(String.format("Billing Details : %s%n", billingDetails));
        if (isPointsRedeemed) {
            sb.append(String.format("Point Discount  : 2-Day Free Stay (-%d pts)%n", redeemedPoints));
        }
        sb.append("--------------------------------------------------");
        return sb.toString();
    }

    @Override
    public String toString() {
        String roomNo = (assignedRoom != null) ? assignedRoom.getRoomNumber() : "Unassigned";
        String tier = (memberProfile != null) ? memberProfile.getTierType() : "Non-Member";
        int pts = (memberProfile != null) ? memberProfile.getPoints() : 0;
        String prefType = (preferredRoomType == null) ? "Any" : preferredRoomType;

        return String.format("Conf. No: %-8s | Name: %-15s | Type: %-7s | Stay: %2d nights | Tier: %-8s (%4d pts) | Pref: %-6s | Room: %-6s | Status: %-10s | Billing: %s",
                confirmationNumber, name, type, stayDays, tier, pts, prefType, roomNo, status, billingDetails);
    }
}
