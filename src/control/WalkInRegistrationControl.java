/*
 * Author: LAW QINQI
 * Control class - implements business logic for the Walk-In Registrations &
 * Standard Booking Procedure use case. Orchestrates the Guest queue (Linear
 * ADT) and the Room list, and produces management reports.
 *
 * Adapted to the shared entity.Guest / entity.Room definitions: unlike an
 * earlier draft, Guest has no Status enum or TYPE_/PREFIX_ constants of
 * its own (just a plain checkInStatus boolean), so those labels/prefixes
 * are tracked locally in this control class instead.
 */
package control;

import adt.ADT;
import adt.LinkedADT;
import entity.Guest;
import entity.Room;

public class WalkInRegistrationControl {

    // Guest "type" labels used consistently across this module.
    public static final String TYPE_WALKIN = "Walk-in";
    public static final String TYPE_BOOKED = "Booked";

    // Confirmation-number prefixes (WI- for walk-ins, SG- for standard/
    // pre-booked guests) so the two guest types stay distinguishable.
    private static final String PREFIX_WALKIN = "WI-";
    private static final String PREFIX_STANDARD = "SG-";

    // Cleaning status (from Room's own status vocabulary) that marks a
    // room as ready to receive a guest.
    private static final String STATUS_READY = "Ready for Check-In";

    // Chronological queue of guests awaiting check-in / room assignment.
    private ADT<Guest> guestQueue;
    // All guests ever registered (kept for reporting even after check-in).
    private ADT<Guest> guestRecords;
    // Room inventory (would normally be populated by the Room Management module).
    private ADT<Room> roomList;

    private int confirmationSeed = 1000;

    public WalkInRegistrationControl(ADT<Room> roomList) {
        this.guestQueue = new LinkedADT<>();
        this.guestRecords = new LinkedADT<>();
        this.roomList = roomList;
    }

    // ---------- Registration ----------

    /**
     * Register a new walk-in guest and add them to the back of the
     * chronological processing queue (Linear ADT - FIFO).
     * Pre-cond: name and billingDetails are non-blank.
     * Throws IllegalArgumentException if validation fails.
     */
    public Guest registerWalkIn(String name, String billingDetails) {
        String cleanName = requireNonBlank(name, "Guest name");
        String cleanBilling = requireNonBlank(billingDetails, "Billing details");

        String confirmationNumber = PREFIX_WALKIN + (confirmationSeed++);
        Guest guest = new Guest(confirmationNumber, cleanName, false, TYPE_WALKIN, null, cleanBilling, null);
        guestQueue.insertLast(guest);
        guestRecords.insertLast(guest);
        return guest;
    }

    /**
     * Register a new standard-booking guest and add them to the back of the
     * chronological processing queue (Linear ADT - FIFO).
     *
     * Pre-cond: name and billingDetails are non-blank.
     * Standard-booking confirmation numbers are always normalised to carry
     * the "SG-" prefix (mirroring the "WI-" prefix walk-ins get), and must
     * be unique among existing guest records.
     * Throws IllegalArgumentException if validation fails.
     */
    public Guest registerBooking(String confirmationNumber, String name, String billingDetails) {
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

        Guest guest = new Guest(normalizedConf, cleanName, false, TYPE_BOOKED, null, cleanBilling, null);
        guestQueue.insertLast(guest);
        guestRecords.insertLast(guest);
        return guest;
    }

    /** Trim and validate that a required field is not null/blank. */
    private String requireNonBlank(String value, String fieldLabel) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldLabel + " cannot be empty.");
        }
        return value.trim();
    }

    /** Linear search to check whether a confirmation number is already on record. */
    private boolean isConfirmationNumberTaken(String confirmationNumber) {
        for (int i = 0; i < guestRecords.getNumberOfEntries(); i++) {
            if (guestRecords.getEntry(i).getConfirmationNumber().equalsIgnoreCase(confirmationNumber)) {
                return true;
            }
        }
        return false;
    }

    // ---------- Queue processing ----------

    /**
     * Process the next guest in line: dequeue them, search for the first
     * available, ready-for-check-in room, assign it, and check them in.
     * Returns null if the queue is empty or no room is available.
     */
    public Guest processNextGuest() {
        if (guestQueue.isEmpty()) {
            return null;
        }
        Room room = findFirstAvailableCleanRoom();
        if (room == null) {
            return null; // leave the guest at the front of the queue
        }
        Guest guest = guestQueue.removeFirst();
        room.setAvailability(false);
        guest.assignRoom(room);
        guest.checkIn();
        return guest;
    }

    /** Linear search for the first available, ready-for-check-in room. */
    private Room findFirstAvailableCleanRoom() {
        for (int i = 0; i < roomList.getNumberOfEntries(); i++) {
            Room r = roomList.getEntry(i);
            if (r.isAvailable() && r.getCleaningStatus().equalsIgnoreCase(STATUS_READY)) {
                return r;
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

    /**
     * Check out a guest by confirmation number (linear search + removal).
     * Pre-cond: confirmationNumber is non-blank.
     * Throws IllegalArgumentException if no matching guest record is found,
     * and IllegalStateException if the matching guest is not currently
     * checked in.
     *
     * Note: Guest only tracks a plain checkInStatus boolean (no separate
     * PENDING/CHECKED_OUT states), so a guest who has already checked out
     * reads identically to one who never checked in - both are simply
     * "not currently checked in".
     */
    public boolean checkOutGuest(String confirmationNumber) {
        String cleanConf = requireNonBlank(confirmationNumber, "Confirmation number");

        for (int i = 0; i < guestRecords.getNumberOfEntries(); i++) {
            Guest g = guestRecords.getEntry(i);
            if (g.getConfirmationNumber().equalsIgnoreCase(cleanConf)) {
                if (!g.getCheckInStatus()) {
                    throw new IllegalStateException(
                            "Guest \"" + g.getName() + "\" is not currently checked in.");
                }
                g.checkOut();
                Room room = g.getAssignedRoom();
                if (room != null) {
                    room.setAvailability(true);
                    room.setCleaningStatus("Dirty");
                }
                return true;
            }
        }
        throw new IllegalArgumentException("No guest found with confirmation number \"" + cleanConf + "\".");
    }

    // ---------- Report: Guest Registration & Check-In Status Report ----------
    // Combines: linear search (filter by type/status) + insertion sort (by name)
    // Note: room-level reporting (occupancy, cleaning status) belongs to the
    // separate Room Management module, so this module only reports on guests.

    /**
     * @param typeFilter       null for all types, or "Walk-in"/"Booked"
     * @param checkedInFilter  null for all guests, TRUE for checked-in only,
     *                         FALSE for not-currently-checked-in only
     */
    public void printGuestCheckInReport(String typeFilter, Boolean checkedInFilter) {
        // Step 1: search/filter matching guests into a temporary array.
        Guest[] filtered = new Guest[guestRecords.getNumberOfEntries()];
        int count = 0;
        for (int i = 0; i < guestRecords.getNumberOfEntries(); i++) {
            Guest g = guestRecords.getEntry(i);
            boolean matchesType = (typeFilter == null) || g.getType().equalsIgnoreCase(typeFilter);
            boolean matchesStatus = (checkedInFilter == null) || g.getCheckInStatus() == checkedInFilter;
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
        System.out.println("=====================================================================");
        System.out.println(" GUEST REGISTRATION & CHECK-IN STATUS REPORT");
        System.out.println(" Filter -> Type: " + (typeFilter == null ? "ALL" : typeFilter)
                + " | Status: " + (checkedInFilter == null ? "ALL" : (checkedInFilter ? "Checked-In" : "Pending")));
        System.out.println("=====================================================================");
        System.out.printf("%-10s %-18s %-10s %-12s %-8s%n",
                "Conf. No", "Name", "Type", "Status", "Room");
        System.out.println("---------------------------------------------------------------------");
        for (int i = 0; i < count; i++) {
            Guest g = filtered[i];
            String room = (g.getAssignedRoom() == null) ? "-" : g.getAssignedRoom().getRoomNumber();
            System.out.printf("%-10s %-18s %-10s %-12s %-8s%n",
                    g.getConfirmationNumber(), g.getName(), g.getType(),
                    g.getCheckInStatus() ? "Checked-In" : "Pending", room);
        }
        System.out.println("---------------------------------------------------------------------");
        System.out.println(" Total matching guests: " + count);
        System.out.println("=====================================================================\n");
    }

    // report: queue summary
    public void printQueueSummaryReport() {

        System.out.println("=============================================================");
        System.out.println("              WALK-IN QUEUE SUMMARY REPORT");
        System.out.println("=============================================================");

        System.out.printf("%-5s %-10s %-20s %-10s\n",
                "No", "Conf No", "Guest Name", "Type");

        System.out.println("-------------------------------------------------------------");

        int walkIn = 0;
        int booked = 0;

        for (int i = 0; i < guestQueue.getNumberOfEntries(); i++) {

            Guest g = guestQueue.getEntry(i);

            System.out.printf("%-5d %-10s %-20s %-10s\n",
                    i + 1,
                    g.getConfirmationNumber(),
                    g.getName(),
                    g.getType());

            if (g.getType().equalsIgnoreCase(TYPE_WALKIN))
                walkIn++;
            else
                booked++;
        }

        System.out.println("-------------------------------------------------------------");
        System.out.println("Total Waiting Guests : " + guestQueue.getNumberOfEntries());
        System.out.println("Walk-in Guests       : " + walkIn);
        System.out.println("Booked Guests        : " + booked);
        System.out.println("=============================================================");
    }
}
