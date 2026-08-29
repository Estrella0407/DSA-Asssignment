/*
 * Module: Shared Control Component
 * Author: WEI XIN
 * 
 * Description:
 * Central shared "guest database" used by every module (Walk-In, VIP Allocation, Front-Desk).
 * One instance is created in MainMenuUI and injected into each control that needs to register or
 * look up guests - so there is exactly ONE place that knows how a guest gets stored;
 * every other control just calls add()/find() on it.
 */
package control;

import adt.Dictionary;
import adt.DoublyLinkedList;
import adt.HashTable;
import entity.Guest;
import entity.StayRecord;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class GuestDirectory {

    private final Dictionary<String, Guest> guestTable;
    private final DoublyLinkedList<Guest> guestList;

    // Append-only chronological log of guest-stay events (Linear ADT).
    private final DoublyLinkedList<StayRecord> stayHistory;
    private int historySeed = 1000;

    public GuestDirectory() {
        this.guestTable = new HashTable<>();
        this.guestList = new DoublyLinkedList<>();
        this.stayHistory = new DoublyLinkedList<>();
    }

    public void add(Guest guest) {
        if (guest == null || guest.getConfirmationNumber() == null
                || guest.getConfirmationNumber().trim().isEmpty()) {
            return;
        }
        guestTable.add(normalize(guest.getConfirmationNumber()), guest);
        guestList.insertLast(guest);
    }

    public Guest find(String confirmationNumber) {
        if (confirmationNumber == null) {
            return null;
        }
        return guestTable.getValue(normalize(confirmationNumber));
    }

    public boolean contains(String confirmationNumber) {
        if (confirmationNumber == null) {
            return false;
        }
        return guestTable.contains(normalize(confirmationNumber));
    }

    /** For report modules that need to iterate every guest in registration order. */
    public DoublyLinkedList<Guest> getGuestList() {
        return guestList;
    }

    public Object[] getAllGuests() {
        return guestTable.getValues();
    }

    // =========================================================================
    // Guest stay history / activity timeline
    // =========================================================================

    /** Raw append of a fully-formed history record (used for seeding back-dated events). */
    public void addHistory(StayRecord record) {
        if (record != null) {
            stayHistory.insertLast(record);
        }
    }

    /**
     * Records one lifecycle event for a guest, stamping it with the current time
     * and a snapshot of the guest's room / tier. Returns the created record
     * (null if the guest is null).
     */
    public StayRecord recordEvent(Guest guest, String eventType, String remarks) {
        if (guest == null) {
            return null;
        }
        String roomNo = (guest.getAssignedRoom() != null) ? guest.getAssignedRoom().getRoomNumber() : null;
        String roomType = (guest.getAssignedRoom() != null) ? guest.getAssignedRoom().getRoomType() : null;
        String tier = (guest.getMemberProfile() != null) ? guest.getMemberProfile().getTierType() : null;

        StayRecord record = new StayRecord(
                "SR-" + (historySeed++),
                guest.getConfirmationNumber(),
                guest.getName(),
                eventType,
                roomNo,
                roomType,
                tier,
                LocalDateTime.now(),
                remarks);
        stayHistory.insertLast(record);
        return record;
    }

    public DoublyLinkedList<StayRecord> getStayHistory() {
        return stayHistory;
    }

    /**
     * Builds a chronological "Stay History & Activity Timeline" block, optionally
     * narrowed to a single confirmation number and/or a date range (inclusive).
     */
    public String buildStayHistorySection(String confFilter, LocalDate fromDate, LocalDate toDate) {
        String cleanConf = (confFilter == null || confFilter.trim().isEmpty())
                ? null : confFilter.trim();

        int total = stayHistory.getNumberOfEntries();
        StayRecord[] filtered = new StayRecord[total];
        int count = 0;
        for (int i = 0; i < total; i++) {
            StayRecord r = stayHistory.getEntry(i);
            if (r == null) {
                continue;
            }
            if (cleanConf != null && !cleanConf.equalsIgnoreCase(r.getConfirmationNumber())) {
                continue;
            }
            LocalDate d = (r.getEventDateTime() == null) ? null : r.getEventDateTime().toLocalDate();
            if (fromDate != null && (d == null || d.isBefore(fromDate))) {
                continue;
            }
            if (toDate != null && (d == null || d.isAfter(toDate))) {
                continue;
            }
            filtered[count++] = r;
        }

        // Manual insertion sort by event timestamp (oldest first).
        for (int i = 1; i < count; i++) {
            StayRecord key = filtered[i];
            int j = i - 1;
            while (j >= 0 && filtered[j].compareTo(key) > 0) {
                filtered[j + 1] = filtered[j];
                j--;
            }
            filtered[j + 1] = key;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("\n--------------------------------------------------------------------------------------------------------------\n");
        sb.append("  STAY HISTORY & ACTIVITY TIMELINE   (Filter -> Conf: ")
                .append(cleanConf == null ? "ALL" : cleanConf)
                .append(" | From: ").append(fromDate == null ? "ALL" : fromDate.format(StayRecord.DATE_FORMAT))
                .append(" | To: ").append(toDate == null ? "ALL" : toDate.format(StayRecord.DATE_FORMAT))
                .append(")\n");
        sb.append("--------------------------------------------------------------------------------------------------------------\n");
        sb.append(String.format("  %-18s  %-10s  %-16s  %-13s  %-14s  %-10s  %s%n",
                "Date/Time", "Conf. No", "Guest Name", "Event", "Room", "Tier", "Remarks"));
        sb.append("--------------------------------------------------------------------------------------------------------------\n");

        if (count == 0) {
            sb.append("  No stay-history events match the selected criteria.\n");
        } else {
            for (int i = 0; i < count; i++) {
                StayRecord r = filtered[i];
                sb.append(String.format("  %-18s  %-10s  %-16s  %-13s  %-14s  %-10s  %s%n",
                        r.getFormattedTimestamp(), r.getConfirmationNumber(), r.getGuestName(),
                        r.getEventType(), r.getRoomLabel(), r.getTier(), r.getRemarks()));
            }
        }
        sb.append("--------------------------------------------------------------------------------------------------------------\n");
        sb.append("  Total history events: ").append(count).append('\n');
        sb.append("--------------------------------------------------------------------------------------------------------------\n");
        return sb.toString();
    }

    private String normalize(String confirmationNumber) {
        return confirmationNumber.trim().toUpperCase();
    }
}