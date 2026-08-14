/*
 * Course: BMCS2063 Data Structures and Algorithms
 * Module: VIP & Loyalty Tier Priority Room Allocation (Control Component)
 * Author: WEI XIN
 * 
 * Description:
 * Control class managing business logic for VIP Priority Room Allocation and Reporting.
 * Implements Non-Linear Priority Queue ADT operations, auto-reordering, room allocation
 * from shared room inventory, and analytical management reports combining searching,
 * multi-criteria filtering, and manual sorting (ECB Pattern, No JCF).
 */
package control;

import adt.ArrayPriorityQueue;
import adt.DoublyLinkedList;
import adt.DoublyLinkedListInterface;
import adt.PriorityQueueInterface;
import entity.Guest;
import entity.Member;
import entity.Room;

public class PriorityAllocationControl {

    private static final String STATUS_READY = "Ready for Check-In";

    private final PriorityQueueInterface<Guest> priorityQueue;
    private final DoublyLinkedListInterface<Guest> allocatedVipRecords;
    private DoublyLinkedListInterface<Room> roomList;

    private int totalPriorityProcessed;
    private int diamondCount;
    private int platinumCount;
    private int eliteCount;
    private int goldCount;
    private int silverCount;

    public PriorityAllocationControl() {
        this(new DoublyLinkedList<>());
    }

    public PriorityAllocationControl(DoublyLinkedListInterface<Room> roomList) {
        this.priorityQueue = new ArrayPriorityQueue<>();
        this.allocatedVipRecords = new DoublyLinkedList<>();
        this.roomList = roomList;
        this.totalPriorityProcessed = 0;
        this.diamondCount = 0;
        this.platinumCount = 0;
        this.eliteCount = 0;
        this.goldCount = 0;
        this.silverCount = 0;
        seedSampleData();
    }

    public void setRoomList(DoublyLinkedListInterface<Room> roomList) {
        this.roomList = roomList;
    }

    public DoublyLinkedListInterface<Room> getRoomList() {
        return roomList;
    }

    private void seedSampleData() {
        // Pre-populate with sample VIP guests to demonstrate auto-reordering
        Guest g1 = new Guest("VIP-1001", "Alice Tan", false, "Booked", null, "Credit Card", new Member("M101", "GOLD", 500));
        Guest g2 = new Guest("VIP-1002", "Dato Steven", false, "Booked", null, "Corporate Billing", new Member("M102", "DIAMOND", 2500));
        Guest g3 = new Guest("VIP-1003", "Bob Lee", false, "Booked", null, "Credit Card", new Member("M103", "PLATINUM", 1200));
        Guest g4 = new Guest("VIP-1004", "Dr. Clara", false, "Booked", null, "Direct Transfer", new Member("M104", "ELITE", 1800));

        priorityQueue.enqueue(g1);
        priorityQueue.enqueue(g2);
        priorityQueue.enqueue(g3);
        priorityQueue.enqueue(g4);
    }

    /**
     * Enqueue a new VIP guest. The priority queue automatically reorganizes
     * so that highest tier / loyalty points sits at index 0.
     */
    public boolean addPriorityGuest(Guest guest) {
        if (guest != null && guest.getMemberProfile() != null) {
            return priorityQueue.enqueue(guest);
        }
        return false;
    }

    /**
     * Create and enqueue a VIP guest from field inputs (ECB compliant).
     */
    public Guest registerVIPGuest(String confNumber, String name, String memberId, String tier, int points, String billing) {
        if (confNumber == null || confNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Confirmation number cannot be empty.");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Guest name cannot be empty.");
        }
        if (memberId == null || memberId.trim().isEmpty()) {
            throw new IllegalArgumentException("Member ID cannot be empty.");
        }

        Member member = new Member(memberId.trim(), tier.trim(), Math.max(0, points));
        Guest guest = new Guest(confNumber.trim(), name.trim(), false, "Booked", null,
                (billing == null || billing.trim().isEmpty()) ? "Paid" : billing.trim(), member);

        if (priorityQueue.enqueue(guest)) {
            return guest;
        }
        return null;
    }

    /**
     * Automatically search for the first available, clean room and allocate
     * to the highest-priority VIP guest waiting in the queue.
     */
    public Guest allocateFirstAvailableRoom() {
        if (priorityQueue.isEmpty()) {
            return null;
        }

        Room availableRoom = findFirstAvailableCleanRoom();
        if (availableRoom == null) {
            return null; // No clean vacant room available
        }

        return processAllocation(availableRoom);
    }

    /**
     * Allocate a specific room number to the highest-priority VIP guest.
     */
    public Guest allocateSpecificRoom(String roomNumber) {
        if (priorityQueue.isEmpty() || roomNumber == null) {
            return null;
        }

        Room targetRoom = findRoomByNumber(roomNumber);
        if (targetRoom == null || !targetRoom.isRoomAvailable()
                || !STATUS_READY.equalsIgnoreCase(targetRoom.getCleaningStatus())) {
            return null;
        }

        return processAllocation(targetRoom);
    }

    private Guest processAllocation(Room room) {
        Guest nextGuest = priorityQueue.dequeue();
        if (nextGuest == null) {
            return null;
        }

        nextGuest.assignRoom(room);
        nextGuest.checkIn();
        room.setAvailability(false);

        // Keep in allocated records for reporting
        allocatedVipRecords.insertLast(nextGuest);
        totalPriorityProcessed++;

        // Update tier distribution metrics
        if (nextGuest.getMemberProfile() != null) {
            String tier = nextGuest.getMemberProfile().getTierType().toUpperCase();
            switch (tier) {
                case "DIAMOND":
                    diamondCount++;
                    break;
                case "PLATINUM":
                    platinumCount++;
                    break;
                case "ELITE":
                    eliteCount++;
                    break;
                case "GOLD":
                    goldCount++;
                    break;
                case "SILVER":
                    silverCount++;
                    break;
                default:
                    break;
            }
        }

        return nextGuest;
    }

    /** Linear search for the first available, ready-for-check-in room. */
    private Room findFirstAvailableCleanRoom() {
        if (roomList == null) return null;
        for (int i = 0; i < roomList.getNumberOfEntries(); i++) {
            Room r = roomList.getEntry(i);
            if (r != null && r.isRoomAvailable() && STATUS_READY.equalsIgnoreCase(r.getCleaningStatus())) {
                return r;
            }
        }
        return null;
    }

    /** Linear search for room by number. */
    public Room findRoomByNumber(String roomNumber) {
        if (roomList == null || roomNumber == null) return null;
        String target = roomNumber.trim();
        for (int i = 0; i < roomList.getNumberOfEntries(); i++) {
            Room r = roomList.getEntry(i);
            if (r != null && r.getRoomNumber().equalsIgnoreCase(target)) {
                return r;
            }
        }
        return null;
    }

    public int getQueueSize() {
        return priorityQueue.getSize();
    }

    public Guest peekNextVIP() {
        return priorityQueue.getMin();
    }

    public PriorityQueueInterface<Guest> getPriorityQueue() {
        return priorityQueue;
    }

    // =========================================================================
    // REPORT 1: VIP Tier Allocation & Demand Performance Report
    // Combines: Linear Search + Multi-Criteria Filter (Tier & Min Points) +
    // Manual Insertion Sort (by Points descending / Name)
    // =========================================================================

    public String generateTierDistributionReport(String tierFilter, Integer minPointsFilter) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n=========================================================================================\n");
        sb.append("            REPORT 1: VIP & LOYALTY TIER ALLOCATION & DEMAND PERFORMANCE REPORT          \n");
        sb.append("=========================================================================================\n");
        sb.append(String.format("Filters -> Tier: %-10s | Min Points: %s\n",
                (tierFilter == null ? "ALL" : tierFilter),
                (minPointsFilter == null ? "0" : minPointsFilter)));
        sb.append("-----------------------------------------------------------------------------------------\n");

        // Step 1: Filter allocated records into temporary array
        int totalAllocated = allocatedVipRecords.getNumberOfEntries();
        Guest[] filtered = new Guest[totalAllocated];
        int count = 0;

        for (int i = 0; i < totalAllocated; i++) {
            Guest g = allocatedVipRecords.getEntry(i);
            if (g != null && g.getMemberProfile() != null) {
                Member m = g.getMemberProfile();
                boolean matchesTier = (tierFilter == null) || m.getTierType().equalsIgnoreCase(tierFilter);
                boolean matchesPoints = (minPointsFilter == null) || (m.getPoints() >= minPointsFilter);

                if (matchesTier && matchesPoints) {
                    filtered[count++] = g;
                }
            }
        }

        // Step 2: Manual Insertion Sort by Points (descending), then Name
        for (int i = 1; i < count; i++) {
            Guest key = filtered[i];
            int j = i - 1;
            while (j >= 0 && shouldSwapVipReport(filtered[j], key)) {
                filtered[j + 1] = filtered[j];
                j--;
            }
            filtered[j + 1] = key;
        }

        // Step 3: Format tabular output
        sb.append(String.format("%-10s | %-16s | %-10s | %-12s | %-8s | %-12s\n",
                "Conf. No", "Guest Name", "Tier", "Points", "Room", "Billing"));
        sb.append("-----------------------------------------------------------------------------------------\n");

        if (count == 0) {
            sb.append("                     No allocated VIP records matched the selected criteria.             \n");
        } else {
            for (int i = 0; i < count; i++) {
                Guest g = filtered[i];
                Member m = g.getMemberProfile();
                String room = (g.getAssignedRoom() != null) ? g.getAssignedRoom().getRoomNumber() : "-";
                sb.append(String.format("%-10s | %-16s | %-10s | %-12d | %-8s | %-12s\n",
                        g.getConfirmationNumber(), g.getName(), m.getTierType(), m.getPoints(), room, g.getBillingDetails()));
            }
        }

        sb.append("-----------------------------------------------------------------------------------------\n");
        sb.append(" Tier Breakdown Summary (All Time Allocations):\n");
        int total = (totalPriorityProcessed == 0) ? 1 : totalPriorityProcessed;
        sb.append(String.format("   Diamond  : %2d allocations (%5.1f%%)\n", diamondCount, (diamondCount * 100.0) / total));
        sb.append(String.format("   Platinum : %2d allocations (%5.1f%%)\n", platinumCount, (platinumCount * 100.0) / total));
        sb.append(String.format("   Elite    : %2d allocations (%5.1f%%)\n", eliteCount, (eliteCount * 100.0) / total));
        sb.append(String.format("   Gold     : %2d allocations (%5.1f%%)\n", goldCount, (goldCount * 100.0) / total));
        sb.append(String.format("   Silver   : %2d allocations (%5.1f%%)\n", silverCount, (silverCount * 100.0) / total));
        sb.append(String.format(" Total VIP Rooms Allocated: %d\n", totalPriorityProcessed));
        sb.append("=========================================================================================\n");

        return sb.toString();
    }

    // =========================================================================
    // REPORT 2: Active VIP Priority Waitlist & Real-Time Audit Report
    // Combines: Linear Search + Multi-Criteria Filter + Manual Insertion Sort
    // =========================================================================

    public String generatePriorityWaitlistReport(String tierFilter, Integer minPointsFilter) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n=========================================================================================\n");
        sb.append("              REPORT 2: ACTIVE VIP PRIORITY WAITLIST & REAL-TIME AUDIT                   \n");
        sb.append("=========================================================================================\n");
        sb.append(String.format("Filters -> Tier: %-10s | Min Points: %s\n",
                (tierFilter == null ? "ALL" : tierFilter),
                (minPointsFilter == null ? "0" : minPointsFilter)));
        sb.append("-----------------------------------------------------------------------------------------\n");
        sb.append(String.format("%-5s | %-10s | %-16s | %-10s | %-10s | %-15s\n",
                "Pos", "Conf. No", "Guest Name", "Member Tier", "Points", "Status"));
        sb.append("-----------------------------------------------------------------------------------------\n");

        int queueSize = priorityQueue.getSize();
        if (queueSize == 0) {
            sb.append("                         No VIP guests currently in waitlist.                            \n");
        } else {
            Guest[] temp = new Guest[queueSize];
            int count = 0;

            for (int i = 0; i < queueSize; i++) {
                Guest g = priorityQueue.getEntry(i);
                if (g != null && g.getMemberProfile() != null) {
                    Member m = g.getMemberProfile();
                    boolean matchesTier = (tierFilter == null) || m.getTierType().equalsIgnoreCase(tierFilter);
                    boolean matchesPoints = (minPointsFilter == null) || (m.getPoints() >= minPointsFilter);
                    if (matchesTier && matchesPoints) {
                        temp[count++] = g;
                    }
                }
            }

            if (count == 0) {
                sb.append("                     No waiting VIP guests match the filter criteria.                    \n");
            } else {
                for (int i = 0; i < count; i++) {
                    Guest g = temp[i];
                    Member m = g.getMemberProfile();
                    sb.append(String.format("%-5d | %-10s | %-16s | %-10s | %-10d | %-15s\n",
                            (i + 1), g.getConfirmationNumber(), g.getName(), m.getTierType(), m.getPoints(), "Awaiting Room"));
                }
            }
        }

        sb.append("-----------------------------------------------------------------------------------------\n");
        sb.append(String.format("Total Pending High-Tier Allocations: %d\n", priorityQueue.getSize()));
        sb.append("=========================================================================================\n");

        return sb.toString();
    }

    private boolean shouldSwapVipReport(Guest first, Guest second) {
        if (first == null || first.getMemberProfile() == null) return false;
        if (second == null || second.getMemberProfile() == null) return true;

        Member m1 = first.getMemberProfile();
        Member m2 = second.getMemberProfile();

        // Higher points first
        if (m1.getPoints() != m2.getPoints()) {
            return m1.getPoints() < m2.getPoints();
        }
        return first.getName().compareToIgnoreCase(second.getName()) > 0;
    }
}