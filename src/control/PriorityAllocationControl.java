package control;

import adt.ArrayPriorityQueue;
import adt.PriorityQueueInterface;
import entity.Guest;
import entity.Room;

/**
 * Control class managing business logic for VIP Priority Room Allocation and Reporting.
 * 
 * @author Wei Xin
 */

public class PriorityAllocationControl {

    private final PriorityQueueInterface<Guest> priorityQueue;
    private int totalPriorityProcessed;
    private int diamondCount;
    private int platinumCount;
    private int goldCount;

    public PriorityAllocationControl() {
        this.priorityQueue = new ArrayPriorityQueue<>();
        this.totalPriorityProcessed = 0;
        this.diamondCount = 0;
        this.platinumCount = 0;
        this.goldCount = 0;
        seedSampleData();
    }

    private void seedSampleData() {
        // Pre-populate with sample guests to demonstrate auto-reordering
        Guest g1 = new Guest("10000001", "Alice Tan", false, "Booked", null, "Paid", new entity.Member("M001", "GOLD", 500));
        Guest g2 = new Guest("10000002", "Dato Steven", false, "Booked", null, "Paid", new entity.Member("M002", "DIAMOND", 2500));
        Guest g3 = new Guest("10000003", "Bob Lee", false, "Booked", null, "Paid", new entity.Member("M003", "PLATINUM", 1200));

        priorityQueue.enqueue(g1);
        priorityQueue.enqueue(g2);
        priorityQueue.enqueue(g3);
    }

    public boolean addPriorityGuest(Guest guest) {
        if (guest != null && guest.getMemberProfile() != null) {
            return priorityQueue.enqueue(guest);
        }
        return false;
    }

    public Guest allocateRoomToNextGuest(Room room) {
        if (priorityQueue.isEmpty() || room == null || !room.isAvailable()) {
            return null;
        }

        Guest nextGuest = priorityQueue.dequeue();
        nextGuest.assignRoom(room);
        nextGuest.checkIn();
        room.setAvailability(false);

        // Update tracking statistics
        totalPriorityProcessed++;
        String tier = nextGuest.getMemberProfile().getTierType().toUpperCase();
        if (tier.equals("DIAMOND")) diamondCount++;
        else if (tier.equals("PLATINUM")) platinumCount++;
        else if (tier.equals("GOLD")) goldCount++;

        return nextGuest;
    }

    public String getPendingPriorityQueueString() {
        if (priorityQueue.isEmpty()) {
            return "No pending VIP guests in queue.";
        }
        return priorityQueue.toString();
    }

    // --- Report Generation Logic ---

    public String generateTierDistributionReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n=================================================================================\n");
        sb.append("                  REPORT 1: VIP & LOYALTY TIER DEMAND & ALLOCATION               \n");
        sb.append("=================================================================================\n");
        sb.append(String.format("%-15s | %-20s | %-20s\n", "Loyalty Tier", "Allocated Count", "Percentage Share"));
        sb.append("---------------------------------------------------------------------------------\n");

        int total = (totalPriorityProcessed == 0) ? 1 : totalPriorityProcessed; // Avoid divide-by-zero
        sb.append(String.format("%-15s | %-20d | %-19.1f%%\n", "Diamond", diamondCount, (diamondCount * 100.0) / total));
        sb.append(String.format("%-15s | %-20d | %-19.1f%%\n", "Platinum", platinumCount, (platinumCount * 100.0) / total));
        sb.append(String.format("%-15s | %-20d | %-19.1f%%\n", "Gold", goldCount, (goldCount * 100.0) / total));
        sb.append("---------------------------------------------------------------------------------\n");
        sb.append(String.format("Total VIP Rooms Allocated: %d\n", totalPriorityProcessed));
        sb.append("=================================================================================\n");

        return sb.toString();
    }

    public String generatePriorityWaitlistReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n=================================================================================\n");
        sb.append("               REPORT 2: ACTIVE VIP WAITLIST REAL-TIME AUDIT                     \n");
        sb.append("=================================================================================\n");
        sb.append(String.format("%-10s | %-15s | %-12s | %-10s | %-12s\n", "Conf. No", "Guest Name", "Member Tier", "Points", "Status"));
        sb.append("---------------------------------------------------------------------------------\n");

        if (priorityQueue.isEmpty()) {
            sb.append("                        No VIP guests currently in waitlist.                     \n");
        } else {
            sb.append(getPendingPriorityQueueString());
        }
        sb.append("---------------------------------------------------------------------------------\n");
        sb.append(String.format("Total Pending High-Tier Allocations: %d\n", priorityQueue.getSize()));
        sb.append("=================================================================================\n");

        return sb.toString();
    }
}