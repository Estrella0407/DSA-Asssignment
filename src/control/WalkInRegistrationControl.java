/*
 Module: Walk-In Registrations & Standard Booking Procedure (Control Component)
 Author: LAW QINQI
  
 Description:
 Control class implementing business logic for Walk-In Registrations & Standard Booking.
 Orchestrates the Guest FIFO queue (Doubly Linked List Linear ADT), shared room inventory,
 check-in/check-out lifecycle, and analytical management reports.
 */
package control;

import adt.DoublyLinkedList;
import adt.DoublyLinkedListInterface;
import entity.Guest;
import entity.Member;
import entity.Room;
import entity.StayRecord;
import java.time.LocalDate;

public class WalkInRegistrationControl {

    // Guest "type" labels used consistently across this module.
    public static final String TYPE_WALKIN = "Walk-in";
    public static final String TYPE_BOOKED = "Booked";

    // Confirmation-number prefixes (WI- for walk-ins, SG- for standard/pre-booked guests) so the two guest types stay distinguishable.
    private static final String PREFIX_WALKIN = "WI-";
    private static final String PREFIX_STANDARD = "SG-";

    // Cleaning status (from Room's own status vocabulary) that marks a room as ready to receive a guest.
    private static final String STATUS_READY = "Ready for Check-In";

    // Chronological queue of guests awaiting check-in / room assignment.
    private DoublyLinkedListInterface<Guest> guestQueue;
    // All guests ever registered (kept for reporting even after check-in).
    private DoublyLinkedListInterface<Guest> guestRecords;
    // Room inventory (would normally be populated by the Room Management module).
    private DoublyLinkedListInterface<Room> roomList;

    private final GuestDirectory guestDirectory; // nullable - shared "database"

    // nullable - shared VIP priority queue. When set, a walk-in / booking guest
    // who gains a loyalty Member profile (e.g. via a long-stay promotion) is also
    // enqueued here so the VIP & Loyalty module can see them.
    private PriorityAllocationControl priorityControl;

    private int confirmationSeed = 1000;

    public WalkInRegistrationControl(DoublyLinkedListInterface<Room> roomList) {
        this(roomList, null);
    }

    public WalkInRegistrationControl(DoublyLinkedListInterface<Room> roomList, GuestDirectory guestDirectory) {
        this.guestQueue = new DoublyLinkedList<>();
        this.guestRecords = new DoublyLinkedList<>();
        this.roomList = roomList;
        this.guestDirectory = guestDirectory;
    }

    /**
     * Wire in the shared VIP priority queue (set once during system start-up).
     * A separate setter avoids a circular constructor dependency between the
     * Walk-In and VIP controls.
     */
    public void setPriorityControl(PriorityAllocationControl priorityControl) {
        this.priorityControl = priorityControl;
    }

    /**
     * If the guest holds a loyalty Member profile, mirror them into the shared
     * VIP priority queue so the VIP & Loyalty Tier module lists them too. Safe to
     * call for every registration - non-members are ignored.
     */
    private void mirrorToVipQueueIfMember(Guest guest) {
        if (priorityControl != null && guest != null && guest.getMemberProfile() != null) {
            priorityControl.addPriorityGuest(guest);
        }
    }

    /** Null-safe stay-history event recording (the shared directory is optional in some contexts). */
    private void recordHistory(Guest guest, String eventType, String remarks) {
        if (guestDirectory != null) {
            guestDirectory.recordEvent(guest, eventType, remarks);
        }
    }

    /** Records the REGISTERED event plus a TIER-PROMOTED event if a long-stay milestone just fired. */
    private void recordRegistration(Guest guest, String remarks) {
        if (guestDirectory == null) {
            return;
        }
        guestDirectory.recordEvent(guest, StayRecord.EVENT_REGISTERED, remarks);
        if (guest.getMemberProfile() != null && guest.getLastPromotionMessage() != null) {
            guestDirectory.recordEvent(guest, StayRecord.EVENT_TIER_PROMOTED,
                    "Promoted to " + guest.getMemberProfile().getTierType()
                    + " tier (long-stay " + guest.getStayDays() + "d)");
        }
    }

    /**
     * Chronological stay-history timeline, optionally narrowed by confirmation
     * number and/or date range. Appended to the Guest Check-In Status Report.
     */
    public String getStayHistorySection(String confFilter, LocalDate fromDate, LocalDate toDate) {
        if (guestDirectory == null) {
            return "\n  (Stay history unavailable - no shared guest directory in this context.)\n";
        }
        return guestDirectory.buildStayHistorySection(confFilter, fromDate, toDate);
    }

    /*
    Registration:
    Register a new walk-in guest and add them to the back of the chronological processing queue (Linear ADT - FIFO). 
    Pre-cond: name and billingDetails are non-blank. 
    Throws IllegalArgumentException if validation fails.
    */
    public Guest registerWalkIn(String name, String billingDetails) {
        return registerWalkIn(name, billingDetails, null, 1);
    }

    public Guest registerWalkIn(String name, String billingDetails, String preferredRoomType) {
        return registerWalkIn(name, billingDetails, preferredRoomType, 1);
    }

    public Guest registerWalkIn(String name, String billingDetails, String preferredRoomType, int stayDays) {
        return registerWalkIn(name, billingDetails, preferredRoomType, stayDays, null, false);
    }

    public Guest registerWalkIn(String name, String billingDetails, String preferredRoomType, int stayDays, Member memberProfile, boolean redeemPoints) {
        String cleanName = requireNonBlank(name, "Guest name");
        String cleanBilling = requireNonBlank(billingDetails, "Billing details");

        String confirmationNumber = PREFIX_WALKIN + (confirmationSeed++);
        Guest guest = new Guest(confirmationNumber, cleanName, false, TYPE_WALKIN, null, cleanBilling, memberProfile, stayDays);
        guest.setPreferredRoomType(preferredRoomType == null ? null : Room.normalizeRoomType(preferredRoomType));

        // Evaluate graduated long-stay milestone promotions (>=14d Silver, >30d Gold, etc.)
        guest.applyLongStayPromotion();

        // If requested and eligible, redeem points for 2-day free stay
        if (redeemPoints && guest.getMemberProfile() != null) {
            guest.redeemPointsForStay(guest.getPreferredRoomType());
        }

        guest.setQueueEntryTime(System.currentTimeMillis());
        guestQueue.insertLast(guest);
        guestRecords.insertLast(guest);

        if (guestDirectory != null) {
            guestDirectory.add(guest);
        }
        mirrorToVipQueueIfMember(guest);
        recordRegistration(guest, "Walk-in registration");

        return guest;
    }

    /*
    Register a new standard-booking guest and add them to the back of the chronological processing queue (Linear ADT - FIFO).
     
    Pre-cond: name and billingDetails are non-blank. 
    Standard-booking confirmation numbers are always normalised to carry the "SG-" prefix (mirroring the "WI-" prefix walk-ins get), 
    and must be unique among existing guest records. 
    Throws IllegalArgumentException if validation fails.
    */
    public Guest registerBooking(String confirmationNumber, String name, String billingDetails) {
        return registerBooking(confirmationNumber, name, billingDetails, null, 1);
    }

    public Guest registerBooking(String confirmationNumber, String name, String billingDetails, String preferredRoomType) {
        return registerBooking(confirmationNumber, name, billingDetails, preferredRoomType, 1);
    }

    public Guest registerBooking(String confirmationNumber, String name, String billingDetails, String preferredRoomType, int stayDays) {
        return registerBooking(confirmationNumber, name, billingDetails, preferredRoomType, stayDays, null, false);
    }

    public Guest registerBooking(String confirmationNumber, String name, String billingDetails, String preferredRoomType, int stayDays, Member memberProfile, boolean redeemPoints) {
        String cleanName = requireNonBlank(name, "Guest name");
        String cleanBilling = requireNonBlank(billingDetails, "Billing details");
        String rawConf = requireNonBlank(confirmationNumber, "Confirmation number");

        String normalizedConf = rawConf.toUpperCase().startsWith(PREFIX_STANDARD)
                ? rawConf
                : PREFIX_STANDARD + rawConf;

        if (isConfirmationNumberTaken(normalizedConf)) {
            throw new IllegalArgumentException(
                    "Confirmation number \"" + normalizedConf + "\" is already registered.");
        }

        Guest guest = new Guest(normalizedConf, cleanName, false, TYPE_BOOKED, null, cleanBilling, memberProfile, stayDays);
        guest.setPreferredRoomType(preferredRoomType == null ? null : Room.normalizeRoomType(preferredRoomType));

        // Evaluate graduated long-stay milestone promotions
        guest.applyLongStayPromotion();

        // If requested and eligible, redeem points for 2-day free stay
        if (redeemPoints && guest.getMemberProfile() != null) {
            guest.redeemPointsForStay(guest.getPreferredRoomType());
        }

        guest.setQueueEntryTime(System.currentTimeMillis());
        guestQueue.insertLast(guest);
        guestRecords.insertLast(guest);

        if (guestDirectory != null) {
            guestDirectory.add(guest);
        }
        mirrorToVipQueueIfMember(guest);
        recordRegistration(guest, "Standard booking registration");

        return guest;
    }

    //Trim and validate that a required field is not null/blank.
    private String requireNonBlank(String value, String fieldLabel) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldLabel + " cannot be empty.");
        }
        return value.trim();
    }

    // Linear search to check whether a confirmation number is already on record.
    private boolean isConfirmationNumberTaken(String confirmationNumber) {
        for (int i = 0; i < guestRecords.getNumberOfEntries(); i++) {
            if (guestRecords.getEntry(i).getConfirmationNumber().equalsIgnoreCase(confirmationNumber)) {
                return true;
            }
        }
        if (guestDirectory != null && guestDirectory.contains(confirmationNumber)) {
            return true;
        }
        return false;
    }

    /*
    Queue processing:
    Process the next guest in line: dequeue them, search for the first available, ready-for-check-in room matching preferred room type, assign it, and check them in. 
    Returns null if the queue is empty or no matching clean room is available.
    */
    public Guest processNextGuest() {
        // Drop guests at the front who were already checked in elsewhere
        // (e.g. a promoted member serviced by the VIP module).
        while (!guestQueue.isEmpty() && guestQueue.retrieveFirst().getCheckInStatus()) {
            guestQueue.removeFirst();
        }
        if (guestQueue.isEmpty()) {
            return null;
        }
        Guest guest = guestQueue.retrieveFirst();
        Room room = findFirstAvailableCleanRoom(guest.getPreferredRoomType());
        if (room == null) {
            return null; // leave the guest at the front of the queue until their desired room is ready
        }
        guestQueue.removeFirst();
        room.setAvailability(false);
        guest.assignRoom(room);
        guest.checkIn();
        recordHistory(guest, StayRecord.EVENT_CHECKED_IN, "Checked in via walk-in queue");
        return guest;
    }

    /**
     * Linear search for the first available, ready-for-check-in room.
     * If preferredType is specified, only a room matching that exact type is returned.
     * If preferredType is null ("Any"), the first available ready room of any type is returned.
     */
    private Room findFirstAvailableCleanRoom(String preferredType) {
        for (int i = 0; i < roomList.getNumberOfEntries(); i++) {
            Room r = roomList.getEntry(i);
            if (r != null && r.isRoomAvailable() && r.getCleaningStatus().equalsIgnoreCase(STATUS_READY)) {
                if (preferredType == null || r.getRoomType().equalsIgnoreCase(preferredType)) {
                    return r;
                }
            }
        }
        return null;
    }

    public int getQueueSize() {
        return guestQueue.getNumberOfEntries();
    }

    public Guest peekNextInQueue() {
        return guestQueue.retrieveFirst();
    }

    /*
    Check out a guest by confirmation number (linear search + removal).
    Pre-cond: confirmationNumber is non-blank. 
    */
    public boolean checkOutGuest(String confirmationNumber) {
        String cleanConf = requireNonBlank(confirmationNumber, "Confirmation number");

        for (int i = 0; i < guestRecords.getNumberOfEntries(); i++) {
            Guest g = guestRecords.getEntry(i);
            if (g.getConfirmationNumber().equalsIgnoreCase(cleanConf)) {
                if (!g.getCheckInStatus()) {
                    String reason = Guest.STATUS_CHECKED_OUT.equals(g.getStatus())
                            ? "already checked out"
                            : "not currently checked in";
                    throw new IllegalStateException(
                            "Guest \"" + g.getName() + "\" is " + reason + ".");
                }
                Room room = g.getAssignedRoom();
                if (room != null) {
                    room.setAvailability(true);
                    room.setCleaningStatus("Dirty");
                }
                g.checkOut();
                recordHistory(g, StayRecord.EVENT_CHECKED_OUT, "Checked out at front desk");
                g.assignRoom(null); // guest no longer occupies this room
                return true;
            }
        }
        throw new IllegalArgumentException("No guest found with confirmation number \"" + cleanConf + "\".");
    }

    /* Report 1: Guest Registration & Check-In Status Report
    * Combines: linear search (filter by type/status) + insertion sort (by name) 
    */
    public void printGuestCheckInReport(String typeFilter, String statusFilter) {
        // Step 1: search/filter matching guests into a temporary array.
        Guest[] filtered = new Guest[guestRecords.getNumberOfEntries()];
        int count = 0;
        for (int i = 0; i < guestRecords.getNumberOfEntries(); i++) {
            Guest g = guestRecords.getEntry(i);
            boolean matchesType = (typeFilter == null) || g.getType().equalsIgnoreCase(typeFilter);
            boolean matchesStatus = (statusFilter == null) || g.getStatus().equalsIgnoreCase(statusFilter);
            if (matchesType && matchesStatus) {
                filtered[count++] = g;
            }
        }

        // Step 2: insertion sort filtered guests alphabetically by name.
        for (int i = 1; i < count; i++) {
            Guest key = filtered[i];
            int j = i - 1;
            while (j >= 0 && filtered[j].getName().compareToIgnoreCase(key.getName()) > 0) {
                filtered[j + 1] = filtered[j];
                j--;
            }
            filtered[j + 1] = key;
        }

        // Step 3: print structured report.
        System.out.println("\n=========================================================================================");
        System.out.println("                     GUEST REGISTRATION & CHECK-IN STATUS REPORT");
        System.out.println(" Filter -> Type: " + (typeFilter == null ? "ALL" : typeFilter)
                + " | Status: " + (statusFilter == null ? "ALL" : statusFilter));
        System.out.println("=========================================================================================");
        System.out.printf("%-10s %-16s %-9s %-7s %-12s %-12s %-10s%n",
                "Conf. No", "Name", "Type", "Stay", "Tier (Pts)", "Status", "Room");
        System.out.println("-----------------------------------------------------------------------------------------");
        for (int i = 0; i < count; i++) {
            Guest g = filtered[i];
            String room = (g.getAssignedRoom() == null) ? "-" : g.getAssignedRoom().getRoomNumber() + " (" + g.getAssignedRoom().getRoomType() + ")";
            String tierStr = (g.getMemberProfile() == null) ? "Non-Member" : g.getMemberProfile().getTierType() + " (" + g.getMemberProfile().getPoints() + ")";
            System.out.printf("%-10s %-16s %-9s %2d nts %-12s %-12s %-10s%n",
                    g.getConfirmationNumber(), g.getName(), g.getType(), g.getStayDays(), tierStr, g.getStatus(), room);
        }
        System.out.println("-----------------------------------------------------------------------------------------");
        System.out.println(" Total matching guests: " + count);
        System.out.println("=========================================================================================\n");
    }

    // Report 2: Active Waitlist & Queue Summary Report
    // Combines: search/filter by guest type + insertion sort (by name/pos)
    public void printQueueSummaryReport(String typeFilter) {
        int totalQueue = guestQueue.getNumberOfEntries();
        Guest[] filtered = new Guest[totalQueue];
        int count = 0;
        int walkInCount = 0;
        int bookedCount = 0;
        long now = System.currentTimeMillis();
        long totalWaitMillis = 0;      // across every guest currently in the queue
        long filteredWaitMillis = 0;   // across only the guests shown in the table

        for (int i = 0; i < totalQueue; i++) {
            Guest g = guestQueue.getEntry(i);
            if (g.getType().equalsIgnoreCase(TYPE_WALKIN)) {
                walkInCount++;
            } else {
                bookedCount++;
            }
            totalWaitMillis += waitingMillis(g, now);

            if (typeFilter == null || g.getType().equalsIgnoreCase(typeFilter)) {
                filtered[count++] = g;
            }
        }

        // Insertion sort by name
        for (int i = 1; i < count; i++) {
            Guest key = filtered[i];
            int j = i - 1;
            while (j >= 0 && filtered[j].getName().compareToIgnoreCase(key.getName()) > 0) {
                filtered[j + 1] = filtered[j];
                j--;
            }
            filtered[j + 1] = key;
        }

        System.out.println("\n=============================================================================================================");
        System.out.println("                                      ACTIVE QUEUE WAITLIST AUDIT REPORT");
        System.out.println(" Filter -> Type: " + (typeFilter == null ? "ALL" : typeFilter));
        System.out.println("=============================================================================================================");
        System.out.printf("%-4s %-10s %-16s %-9s %-6s %-10s %-15s %-12s %-18s\n",
                "No", "Conf No", "Guest Name", "Type", "Stay", "Pref Type", "Member Tier", "Waiting", "Billing");
        System.out.println("-------------------------------------------------------------------------------------------------------------");

        if (count == 0) {
            System.out.println("                  No waiting guests match the selected criteria.");
        } else {
            for (int i = 0; i < count; i++) {
                Guest g = filtered[i];
                String pref = (g.getPreferredRoomType() == null) ? "Any" : g.getPreferredRoomType();
                String tier = (g.getMemberProfile() == null) ? "-" : g.getMemberProfile().getTierType() + " (" + g.getMemberProfile().getPoints() + ")";
                long wait = waitingMillis(g, now);
                filteredWaitMillis += wait;
                System.out.printf("%-4d %-10s %-16s %-9s %2d nts %-10s %-15s %-12s %-18s\n",
                        (i + 1), g.getConfirmationNumber(), g.getName(), g.getType(), g.getStayDays(), pref, tier,
                        formatDuration(wait), g.getBillingDetails());
            }
        }

        System.out.println("-------------------------------------------------------------------------------------------------------------");
        System.out.println(" Total Matching Guests In Queue : " + count);
        System.out.println(" Overall Queue Stats -> Total: " + totalQueue + " | Walk-in: " + walkInCount + " | Booked: " + bookedCount);
        System.out.println(" Total Waiting Time (matching guests) : " + formatDuration(filteredWaitMillis));
        System.out.println(" Total Waiting Time (whole queue)     : " + formatDuration(totalWaitMillis));
        if (totalQueue > 0) {
            System.out.println(" Average Waiting Time per Guest       : " + formatDuration(totalWaitMillis / totalQueue));
        }
        System.out.println("=========================================================================================\n");
    }

    /** Milliseconds a guest has been waiting in the queue (0 if the entry time was never recorded). */
    private static long waitingMillis(Guest g, long now) {
        return (g.getQueueEntryTime() > 0) ? Math.max(0, now - g.getQueueEntryTime()) : 0L;
    }

    /** Formats a millisecond duration as a compact "1h 02m 03s" / "2m 05s" / "12s" string. */
    private static String formatDuration(long millis) {
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

    public void printQueueSummaryReport() {
        printQueueSummaryReport(null);
    }
}
