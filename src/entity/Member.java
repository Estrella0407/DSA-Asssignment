package entity;

import java.io.Serializable;
import java.util.Objects;

/**
 * Entity class representing a Loyalty Member.
 * 
 * @author Wei Xin
 */

public class Member implements Serializable, Comparable<Member> {

    private String memberID;
    private String tierType; //  Diamond, Platinum, Gold, Silver, Standard
    private int points;

    public Member() {
        this("", "Standard", 0);
    }

    public Member(String memberID, String tierType) {
        this(memberID, tierType, 0);
    }

    public Member(String memberID, String tierType, int points) {
        this.memberID = memberID;
        this.tierType = tierType;
        this.points = points;
    }

    public String getMemberID() {
        return memberID;
    }

    public void setMemberID(String memberID) {
        this.memberID = memberID;
    }

    public String getTierType() {
        return tierType;
    }

    public void setTierType(String tierType) {
        this.tierType = tierType;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public void addPoints(int points) {
        if (points > 0) {
            this.points += points;
        }
    }

    public void deductPoints(int points) {
        if (points > 0 && this.points >= points) {
            this.points -= points;
        }
    }

    /**
     * Helper priority weight for non-linear priority queue room allocation.
     */
    public int getTierPriorityWeight() {
        if (tierType == null) return 0;
        switch (tierType.toUpperCase()) {
            case "DIAMOND":
                return 4;
            case "PLATINUM":
                return 3;
            case "GOLD":
                return 2;
            case "SILVER":
                return 1;
            default:
                return 0;
        }
    }

    @Override
    public int compareTo(Member other) {
        if (other == null) return 1;
        // Higher tier priority takes precedence
        int priorityCompare = Integer.compare(other.getTierPriorityWeight(), this.getTierPriorityWeight());
        if (priorityCompare != 0) {
            return priorityCompare;
        }
        // Secondary sort by member ID
        return this.memberID.compareTo(other.memberID);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Member member = (Member) obj;
        return Objects.equals(memberID, member.memberID);
    }

    @Override
    public int hashCode() {
        return Objects.hash(memberID);
    }

    @Override
    public String toString() {
        return String.format("Member ID: %-8s | Tier: %-10s | Points: %d",
                memberID, tierType, points);
    }
}