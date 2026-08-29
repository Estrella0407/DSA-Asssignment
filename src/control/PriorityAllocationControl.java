/*
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
import entity.StayRecord;
import java.time.LocalDate;

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

    private final GuestDirectory guestDirectory;

    public PriorityAllocationControl() {
        this(new DoublyLinkedList<>(), null);
    }

    public PriorityAllocationControl(DoublyLinkedListInterface<Room> roomList) {
        this(roomList, null);
    }

    public PriorityAllocationControl(DoublyLinkedListInterface<Room> roomList, GuestDirectory guestDirectory) {
        this.priorityQueue = new ArrayPriorityQueue<>();
        this.allocatedVipRecords = new DoublyLinkedList<>();
        this.roomList = roomList;
        this.guestDirectory = guestDirectory;
        this.totalPriorityProcessed = 0;
        this.diamondCount = 0;
        this.platinumCount = 0;
        this.eliteCount = 0;
        this.goldCount = 0;
        this.silverCount = 0;
    }

    public void setRoomList(DoublyLinkedListInterface<Room> roomList) {
        this.roomList = roomList;
    }

    public DoublyLinkedListInterface<Room> getRoomList() {
        return roomList;
    }

    /**
     * Enqueue a new VIP guest. The priority queue automatically reorganizes so
     * that highest tier / loyalty points sits at index 0.
     */
    public boolean addPriorityGuest(Guest guest) {
        if (guest != null && guest.getMemberProfile() != null) {
            if (guest.getQueueEntryTime() <= 0) {
                guest.setQueueEntryTime(System.currentTimeMillis());
            }
            return priorityQueue.enqueue(guest);
        }
        return false;
    }

    /**
     * Create and enqueue a VIP guest from field inputs (ECB compliant).
     */
    public Guest registerVIPGuest(String confNumber, String name, String memberId, String tier, int points, String billing) {
        return registerVIPGuest(confNumber, name, memberId, tier, points, billing, null, 1, false);
    }

    public Guest registerVIPGuest(String confNumber, String name, String memberId, String tier, int points, String billing, String preferredRoomType) {
        return registerVIPGuest(confNumber, name, memberId, tier, points, billing, preferredRoomType, 1, false);
    }

    public Guest registerVIPGuest(String confNumber, String name, String memberId, String tier, int points, String billing, String preferredRoomType, int stayDays) {
        return registerVIPGuest(confNumber, name, memberId, tier, points, billing, preferredRoomType, stayDays, false);
    }

    public Guest registerVIPGuest(String confNumber, String name, String memberId, String tier, int points, String billing, String preferredRoomType, int stayDays, boolean redeemPoints) {
        if (confNumber == null || confNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Confirmation number cannot be empty.");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Guest name cannot be empty.");
        }
        if (memberId == null || memberId.trim().isEmpty()) {
            throw new IllegalArgumentException("Member ID cannot be empty.");
        }

        String cleanConf = confNumber.trim();
        String cleanMemberId = memberId.trim();

        if (isConfirmationNumberTaken(cleanConf)) {
            throw new IllegalArgumentException(
                    "Confirmation number \"" + cleanConf + "\" is already registered.");
        }
        if (isMemberIdTaken(cleanMemberId)) {
            throw new IllegalArgumentException(
                    "Member ID \"" + cleanMemberId + "\" is already registered.");
        }

        String normalizedTier = (tier == null || tier.trim().isEmpty())
                ? "STANDARD" : tier.trim().toUpperCase();

        Member member = new Member(cleanMemberId, normalizedTier, Math.max(0, points));
        Guest guest = new Guest(cleanConf, name.trim(), false, "Booked", null,
                (billing == null || billing.trim().isEmpty()) ? "Paid" : billing.trim(), member, stayDays);
        guest.setPreferredRoomType(preferredRoomType == null ? null : Room.normalizeRoomType(preferredRoomType));

        // Evaluate graduated long-stay milestone promotions
        guest.applyLongStayPromotion();

        // If requested and eligible, redeem points for 2-day free stay
        if (redeemPoints) {
            guest.redeemPointsForStay(guest.getPreferredRoomType());
        }

        guest.setQueueEntryTime(System.currentTimeMillis());
        if (priorityQueue.enqueue(guest)) {
            if (guestDirectory != null) {
                guestDirectory.add(guest);
                guestDirectory.recordEvent(guest, StayRecord.EVENT_REGISTERED, "VIP reservation enqueued");
                if (guest.getMemberProfile() != null && guest.getLastPromotionMessage() != null) {
                    guestDirectory.recordEvent(guest, StayRecord.EVENT_TIER_PROMOTED,
                            "Promoted to " + guest.getMemberProfile().getTierType()
                            + " tier (long-stay " + guest.getStayDays() + "d)");
                }
            }
            return guest;
        }
        return null;
    }

    /** Null-safe stay-history event recording (the shared directory is optional in some contexts). */
    private void recordHistory(Guest guest, String eventType, String remarks) {
        if (guestDirectory != null) {
            guestDirectory.recordEvent(guest, eventType, remarks);
        }
    }

    /**
     * Chronological stay-history timeline, optionally narrowed by confirmation
     * number and/or date range. Appended to VIP Report 1.
     */
    public String getStayHistorySection(String confFilter, LocalDate fromDate, LocalDate toDate) {
        if (guestDirectory == null) {
            return "\n  (Stay history unavailable - no shared guest directory in this context.)\n";
        }
        return guestDirectory.buildStayHistorySection(confFilter, fromDate, toDate);
    }

    private boolean isConfirmationNumberTaken(String confirmationNumber) {
        for (int i = 0; i < priorityQueue.getSize(); i++) {
            Guest g = priorityQueue.getEntry(i);
            if (g != null && g.getConfirmationNumber().equalsIgnoreCase(confirmationNumber)) {
                return true;
            }
        }
        for (int i = 0; i < allocatedVipRecords.getNumberOfEntries(); i++) {
            Guest g = allocatedVipRecords.getEntry(i);
            if (g != null && g.getConfirmationNumber().equalsIgnoreCase(confirmationNumber)) {
                return true;
            }
        }
        if (guestDirectory != null && guestDirectory.contains(confirmationNumber)) {
            return true;
        }
        return false;
    }

    private boolean isMemberIdTaken(String memberId) {
        for (int i = 0; i < priorityQueue.getSize(); i++) {
            Guest g = priorityQueue.getEntry(i);
            if (g != null && g.getMemberProfile() != null
                    && g.getMemberProfile().getMemberID().equalsIgnoreCase(memberId)) {
                return true;
            }
        }
        for (int i = 0; i < allocatedVipRecords.getNumberOfEntries(); i++) {
            Guest g = allocatedVipRecords.getEntry(i);
            if (g != null && g.getMemberProfile() != null
                    && g.getMemberProfile().getMemberID().equalsIgnoreCase(memberId)) {
                return true;
            }
        }
        return false;
    }

    public boolean isConfirmationNumberRegistered(String confirmationNumber) {
        if (confirmationNumber == null || confirmationNumber.trim().isEmpty()) {
            return false;
        }
        return isConfirmationNumberTaken(confirmationNumber.trim());
    }

    public boolean isMemberIdRegistered(String memberId) {
        if (memberId == null || memberId.trim().isEmpty()) {
            return false;
        }
        return isMemberIdTaken(memberId.trim());
    }

    /**
     * Scans the priority queue in order and allocates a room to the first
     * (i.e. highest-priority) VIP guest whose preferred room type currently
     * has a ready, vacant room. If the top guest is blocked (their type
     * isn't ready), lower-priority guests are checked in turn rather than
     * leaving ready rooms empty - but a guest is only ever skipped by
     * someone with STRICTLY lower priority; earlier-queued guests are never
     * skipped over.
     */
    public Guest allocateFirstAvailableRoom() {
        purgeServedGuests();
        if (priorityQueue.isEmpty()) {
            return null;
        }

        for (int i = 0; i < priorityQueue.getSize(); i++) {
            Guest candidate = priorityQueue.getEntry(i);
            if (candidate == null) {
                continue;
            }
            Room availableRoom = findFirstAvailableCleanRoom(candidate.getPreferredRoomType());
            if (availableRoom != null) {
                Guest dequeuedGuest = priorityQueue.dequeueAt(i);
                return finalizeAllocation(dequeuedGuest, availableRoom);
            }
        }
        return null; // No ready room matches any waiting VIP's preferred type
    }

    /**
     * Linear search for the first available, ready-for-check-in room.
     * If preferredType is specified, returns only a room of that exact type.
     * If preferredType is null, returns the first available ready room.
     */
    private Room findFirstAvailableCleanRoom(String preferredType) {
        if (roomList == null) {
            return null;
        }
        for (int i = 0; i < roomList.getNumberOfEntries(); i++) {
            Room r = roomList.getEntry(i);
            if (r != null && r.isRoomAvailable() && STATUS_READY.equalsIgnoreCase(r.getCleaningStatus())) {
                if (preferredType == null || r.getRoomType().equalsIgnoreCase(preferredType)) {
                    return r;
                }
            }
        }
        return null;
    }

    /**
     * Retrieves all available, clean rooms matching a specific room type
     * (or all ready rooms if roomType is null or "Any").
     */
    public DoublyLinkedListInterface<Room> getAvailableCleanRoomsByType(String roomType) {
        DoublyLinkedListInterface<Room> availableList = new DoublyLinkedList<>();
        if (roomList == null) {
            return availableList;
        }
        for (int i = 0; i < roomList.getNumberOfEntries(); i++) {
            Room r = roomList.getEntry(i);
            if (r != null && r.isRoomAvailable() && STATUS_READY.equalsIgnoreCase(r.getCleaningStatus())) {
                if (roomType == null || r.getRoomType().equalsIgnoreCase(roomType)) {
                    availableList.insertLast(r);
                }
            }
        }
        return availableList;
    }

    /**
     * Allocate a specific room number to the highest-priority VIP guest.
     */
    public Guest allocateSpecificRoom(String roomNumber) {
        purgeServedGuests();
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
        return finalizeAllocation(nextGuest, room);
    }

    /**
     * Shared finalization logic: assigns the room, checks the guest in,
     * records the allocation for reporting, and updates tier metrics.
     */
    private Guest finalizeAllocation(Guest nextGuest, Room room) {
        nextGuest.assignRoom(room);
        nextGuest.checkIn();
        room.setAvailability(false);
        recordHistory(nextGuest, StayRecord.EVENT_CHECKED_IN,
                "VIP priority allocation to room " + room.getRoomNumber());

        allocatedVipRecords.insertLast(nextGuest);
        totalPriorityProcessed++;

        if (nextGuest.getMemberProfile() != null) {
            String tier = nextGuest.getMemberProfile().getTierType().toUpperCase();
            switch (tier) {
                case "DIAMOND": diamondCount++; break;
                case "PLATINUM": platinumCount++; break;
                case "ELITE": eliteCount++; break;
                case "GOLD": goldCount++; break;
                case "SILVER": silverCount++; break;
                default: break;
            }
        }

        return nextGuest;
    }

    /**
     * Linear search for room by number.
     */
    public Room findRoomByNumber(String roomNumber) {
        if (roomList == null || roomNumber == null) {
            return null;
        }
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
        purgeServedGuests();
        return priorityQueue.getSize();
    }

    public Guest peekNextVIP() {
        purgeServedGuests();
        return priorityQueue.getMin();
    }

    /**
     * Drops any guest from the pending priority queue who has already been
     * checked in / given a room by another module (e.g. a walk-in guest who was
     * mirrored here after a loyalty promotion and then serviced at the front
     * desk). Keeps the pending waitlist and its reports honest and prevents a
     * second room from being allocated to the same guest.
     */
    private void purgeServedGuests() {
        for (int i = 0; i < priorityQueue.getSize(); i++) {
            Guest g = priorityQueue.getEntry(i);
            if (g != null && (g.getCheckInStatus() || g.getAssignedRoom() != null)) {
                priorityQueue.dequeueAt(i);
                i--;
            }
        }
    }

    /**
     * Check out an allocated VIP guest by confirmation number: releases their
     * room (Available + Dirty) and flips the guest to Checked-Out.
     */
    public boolean checkOutVIP(String confirmationNumber) {
        if (confirmationNumber == null || confirmationNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Confirmation number cannot be empty.");
        }
        String cleanConf = confirmationNumber.trim();

        for (int i = 0; i < allocatedVipRecords.getNumberOfEntries(); i++) {
            Guest g = allocatedVipRecords.getEntry(i);
            if (g != null && g.getConfirmationNumber().equalsIgnoreCase(cleanConf)) {
                if (!g.getCheckInStatus()) {
                    String reason = Guest.STATUS_CHECKED_OUT.equals(g.getStatus())
                            ? "already checked out"
                            : "not currently checked in";
                    throw new IllegalStateException(
                            "VIP guest \"" + g.getName() + "\" is " + reason + ".");
                }
                Room room = g.getAssignedRoom();
                if (room != null) {
                    room.setAvailability(true);
                    room.setCleaningStatus("Dirty");
                }
                g.checkOut();
                recordHistory(g, StayRecord.EVENT_CHECKED_OUT, "VIP checked out at front desk");
                g.assignRoom(null);
                return true;
            }
        }
        throw new IllegalArgumentException(
                "No allocated VIP guest found with confirmation number \"" + cleanConf + "\".");
    }

    /** Allocated (checked-in / checked-out) VIP records, for the check-out picker and reports. */
    public DoublyLinkedListInterface<Guest> getAllocatedVipRecords() {
        return allocatedVipRecords;
    }

    public PriorityQueueInterface<Guest> getPriorityQueue() {
        return priorityQueue;
    }

    /**
     * Quick unformatted view of the pending VIP queue in priority order.
     */
    public String getQueueSnapshot() {
        purgeServedGuests();
        StringBuilder sb = new StringBuilder();
        int size = priorityQueue.getSize();
        if (size == 0) {
            return "No VIP guests currently in the priority queue.\n";
        }
        for (int i = 0; i < size; i++) {
            Guest g = priorityQueue.getEntry(i);
            sb.append(String.format("%d. %s [%s | %d pts]%n",
                    i + 1, g.getName(),
                    g.getMemberProfile().getTierType(),
                    g.getMemberProfile().getPoints()));
        }
        return sb.toString();
    }

    // =========================================================================
    // REPORT 1: VIP Tier Allocation & Demand Performance Report
    // Combines: Linear Search + Multi-Criteria Filter (Tier & Min Points) +
    // Manual Insertion Sort (by Points descending / Name)
    // =========================================================================
    public String generateTierDistributionReport(String tierFilter, Integer minPointsFilter) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n=======================================================================================================\n");
        sb.append("                    REPORT 1: VIP & LOYALTY TIER ALLOCATION & DEMAND PERFORMANCE REPORT                  \n");
        sb.append("=========================================================================================================\n");
        sb.append(String.format("Filters -> Tier: %-10s | Min Points: %s\n",
                (tierFilter == null ? "ALL" : tierFilter),
                (minPointsFilter == null ? "0" : minPointsFilter)));
        sb.append("---------------------------------------------------------------------------------------------------------\n");

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
        sb.append(String.format("%-10s | %-16s | %-10s | %-6s | %-12s | %-12s | %-18s\n",
                "Conf. No", "Guest Name", "Tier", "Stay", "Points", "Room (Type)", "Billing"));
        sb.append("---------------------------------------------------------------------------------------------------------\n");

        if (count == 0) {
            sb.append("                               No allocated VIP records matched the selected criteria.                  \n");
        } else {
            for (int i = 0; i < count; i++) {
                Guest g = filtered[i];
                Member m = g.getMemberProfile();
                String room = (g.getAssignedRoom() != null)
                        ? g.getAssignedRoom().getRoomNumber() + " (" + g.getAssignedRoom().getRoomType() + ")"
                        : "-";
                sb.append(String.format("%-10s | %-16s | %-10s | %2d nts | %-12d | %-12s | %-18s\n",
                        g.getConfirmationNumber(), g.getName(), m.getTierType(), g.getStayDays(), m.getPoints(), room, g.getBillingDetails()));
            }
        }

        sb.append("---------------------------------------------------------------------------------------------------------\n");
        sb.append(" Tier Breakdown Summary (All Time Allocations):\n");
        int total = (totalPriorityProcessed == 0) ? 1 : totalPriorityProcessed;
        sb.append(String.format("   Diamond  : %2d allocations (%5.1f%%)\n", diamondCount, (diamondCount * 100.0) / total));
        sb.append(String.format("   Platinum : %2d allocations (%5.1f%%)\n", platinumCount, (platinumCount * 100.0) / total));
        sb.append(String.format("   Elite    : %2d allocations (%5.1f%%)\n", eliteCount, (eliteCount * 100.0) / total));
        sb.append(String.format("   Gold     : %2d allocations (%5.1f%%)\n", goldCount, (goldCount * 100.0) / total));
        sb.append(String.format("   Silver   : %2d allocations (%5.1f%%)\n", silverCount, (silverCount * 100.0) / total));
        sb.append(String.format(" Total VIP Rooms Allocated: %d\n", totalPriorityProcessed));
        sb.append("=========================================================================================================\n");

        return sb.toString();
    }

    // =========================================================================
    // REPORT 2: Active VIP Priority Waitlist & Real-Time Audit Report
    // Combines: Linear Search + Multi-Criteria Filter + Manual Insertion Sort
    // =========================================================================
    public String generatePriorityWaitlistReport(String tierFilter, Integer minPointsFilter) {
        purgeServedGuests();
        StringBuilder sb = new StringBuilder();
        sb.append("\n=========================================================================================================\n");
        sb.append("                               REPORT 2: ACTIVE VIP PRIORITY WAITLIST & REAL-TIME AUDIT                  \n");
        sb.append("=========================================================================================================\n");
        sb.append(String.format("Filters -> Tier: %-10s | Min Points: %s\n",
                (tierFilter == null ? "ALL" : tierFilter),
                (minPointsFilter == null ? "0" : minPointsFilter)));
        sb.append("--------------------------------------------------------------------------------------------------------------------\n");
        sb.append(String.format("%-4s | %-10s | %-16s | %-11s | %-6s | %-10s | %-8s | %-15s | %-12s\n",
                "Pos", "Conf. No", "Guest Name", "Member Tier", "Stay", "Points", "Pref Type", "Status", "Waited"));
        sb.append("--------------------------------------------------------------------------------------------------------------------\n");

        long now = System.currentTimeMillis();
        long totalWaitMillis = 0;     // across every guest currently in the priority queue
        long matchingWaitMillis = 0;  // across only the guests shown in the table

        int queueSize = priorityQueue.getSize();
        if (queueSize == 0) {
            sb.append("                                   No VIP guests currently in waitlist.                                  \n");
        } else {
            Guest[] temp = new Guest[queueSize];
            int count = 0;

            for (int i = 0; i < queueSize; i++) {
                Guest g = priorityQueue.getEntry(i);
                if (g != null && g.getMemberProfile() != null) {
                    totalWaitMillis += waitedMillis(g, now);
                    Member m = g.getMemberProfile();
                    boolean matchesTier = (tierFilter == null) || m.getTierType().equalsIgnoreCase(tierFilter);
                    boolean matchesPoints = (minPointsFilter == null) || (m.getPoints() >= minPointsFilter);
                    if (matchesTier && matchesPoints) {
                        temp[count++] = g;
                    }
                }
            }

            if (count == 0) {
                sb.append("                               No waiting VIP guests match the filter criteria.                          \n");
            } else {
                for (int i = 0; i < count; i++) {
                    Guest g = temp[i];
                    Member m = g.getMemberProfile();
                    String pref = (g.getPreferredRoomType() == null) ? "Any" : g.getPreferredRoomType();
                    long waited = waitedMillis(g, now);
                    matchingWaitMillis += waited;
                    sb.append(String.format("%-4d | %-10s | %-16s | %-11s | %2d nts | %-10d | %-8s | %-15s | %-12s\n",
                            (i + 1), g.getConfirmationNumber(), g.getName(), m.getTierType(), g.getStayDays(),
                            m.getPoints(), pref, "Awaiting Room", formatWaiting(waited)));
                }
            }
        }

        sb.append("--------------------------------------------------------------------------------------------------------------------\n");
        sb.append(String.format("Total Pending High-Tier Allocations : %d\n", priorityQueue.getSize()));
        sb.append(String.format("Total Time Waited (matching guests) : %s\n", formatWaiting(matchingWaitMillis)));
        sb.append(String.format("Total Time Waited (whole waitlist)  : %s\n", formatWaiting(totalWaitMillis)));
        if (priorityQueue.getSize() > 0) {
            sb.append(String.format("Average Wait per Waiting Guest      : %s\n",
                    formatWaiting(totalWaitMillis / priorityQueue.getSize())));
        }
        sb.append("=========================================================================================================\n");

        return sb.toString();
    }

    /** Milliseconds a VIP guest has been waiting in the priority queue (0 if the entry time was never recorded). */
    private static long waitedMillis(Guest g, long now) {
        return (g.getQueueEntryTime() > 0) ? Math.max(0, now - g.getQueueEntryTime()) : 0L;
    }

    /** Formats a millisecond duration as a compact "1h 02m 03s" / "2m 05s" / "12s" string (matches the Walk-In report). */
    private static String formatWaiting(long millis) {
        long totalSeconds = millis / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        if (hours > 0) {
            return String.format("%dh %02dm %02ds", hours, minutes, seconds);
        }
        if (minutes > 0) {
            return String.format("%dm %02ds", minutes, seconds);
        }
        return seconds + "s";
    }

    private boolean shouldSwapVipReport(Guest first, Guest second) {
        if (first == null || first.getMemberProfile() == null) {
            return false;
        }
        if (second == null || second.getMemberProfile() == null) {
            return true;
        }

        Member m1 = first.getMemberProfile();
        Member m2 = second.getMemberProfile();

        // Higher points first
        if (m1.getPoints() != m2.getPoints()) {
            return m1.getPoints() < m2.getPoints();
        }
        return first.getName().compareToIgnoreCase(second.getName()) > 0;
    }
}
