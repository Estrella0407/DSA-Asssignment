/*
 * Module: Shared Entity Component (Guest Stay History)
 * Author: WEI XIN & LAW QINQI
 *
 * Description:
 * Entity class representing ONE point-in-time event in a guest's stay lifecycle
 * (registration, check-in, room transfer, tier promotion, check-out). Records are
 * append-only and hold a snapshot of the guest's room / tier AT THE TIME of the
 * event, so the history stays accurate even after the live Guest object changes.
 * Ordered chronologically for timeline reporting.
 */
package entity;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class StayRecord implements Serializable, Comparable<StayRecord> {

    // Canonical event types.
    public static final String EVENT_REGISTERED = "REGISTERED";
    public static final String EVENT_CHECKED_IN = "CHECKED-IN";
    public static final String EVENT_ROOM_CHANGED = "ROOM-CHANGED";
    public static final String EVENT_TIER_PROMOTED = "TIER-PROMOTED";
    public static final String EVENT_CHECKED_OUT = "CHECKED-OUT";

    public static final DateTimeFormatter TS_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    public static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private String recordId;
    private String confirmationNumber;
    private String guestName;
    private String eventType;
    private String roomNumber; // snapshot at event time; "-" if none
    private String roomType;   // snapshot at event time; "-" if none
    private String tier;       // snapshot at event time; "Non-Member" if none
    private LocalDateTime eventDateTime;
    private String remarks;

    public StayRecord() {
        this("", "", "", EVENT_REGISTERED, "-", "-", "Non-Member", LocalDateTime.now(), "");
    }

    public StayRecord(String recordId, String confirmationNumber, String guestName,
            String eventType, String roomNumber, String roomType, String tier,
            LocalDateTime eventDateTime, String remarks) {
        this.recordId = recordId;
        this.confirmationNumber = confirmationNumber;
        this.guestName = guestName;
        this.eventType = eventType;
        this.roomNumber = blankToDash(roomNumber);
        this.roomType = blankToDash(roomType);
        this.tier = (tier == null || tier.trim().isEmpty()) ? "Non-Member" : tier;
        this.eventDateTime = (eventDateTime == null) ? LocalDateTime.now() : eventDateTime;
        this.remarks = (remarks == null) ? "" : remarks;
    }

    private static String blankToDash(String value) {
        return (value == null || value.trim().isEmpty()) ? "-" : value;
    }

    public String getRecordId() {
        return recordId;
    }

    public void setRecordId(String recordId) {
        this.recordId = recordId;
    }

    public String getConfirmationNumber() {
        return confirmationNumber;
    }

    public void setConfirmationNumber(String confirmationNumber) {
        this.confirmationNumber = confirmationNumber;
    }

    public String getGuestName() {
        return guestName;
    }

    public void setGuestName(String guestName) {
        this.guestName = guestName;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = blankToDash(roomNumber);
    }

    public String getRoomType() {
        return roomType;
    }

    public void setRoomType(String roomType) {
        this.roomType = blankToDash(roomType);
    }

    public String getTier() {
        return tier;
    }

    public void setTier(String tier) {
        this.tier = (tier == null || tier.trim().isEmpty()) ? "Non-Member" : tier;
    }

    public LocalDateTime getEventDateTime() {
        return eventDateTime;
    }

    public void setEventDateTime(LocalDateTime eventDateTime) {
        this.eventDateTime = eventDateTime;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = (remarks == null) ? "" : remarks;
    }

    /** "201 (Suite)" style label, or "-" when no room was attached to the event. */
    public String getRoomLabel() {
        if ("-".equals(roomNumber)) {
            return "-";
        }
        return roomNumber + " (" + roomType + ")";
    }

    public String getFormattedTimestamp() {
        return eventDateTime == null ? "-" : eventDateTime.format(TS_FORMAT);
    }

    /** Parses "dd/MM/yyyy" leniently; returns null for blank or unparseable input. */
    public static LocalDate parseDate(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(text.trim(), DATE_FORMAT);
        } catch (Exception ex) {
            return null;
        }
    }

    @Override
    public int compareTo(StayRecord other) {
        if (other == null) {
            return 1;
        }
        if (eventDateTime == null && other.eventDateTime == null) {
            return 0;
        }
        if (eventDateTime == null) {
            return -1;
        }
        if (other.eventDateTime == null) {
            return 1;
        }
        return eventDateTime.compareTo(other.eventDateTime);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        StayRecord other = (StayRecord) obj;
        return Objects.equals(recordId, other.recordId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(recordId);
    }

    @Override
    public String toString() {
        return String.format("%-16s | %-10s | %-16s | %-13s | %-14s | %-10s | %s",
                getFormattedTimestamp(), confirmationNumber, guestName, eventType,
                getRoomLabel(), tier, remarks);
    }
}
