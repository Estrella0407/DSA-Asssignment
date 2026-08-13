/*
 * Author: NEO AI YIK
 * Control class for the Housekeeping and Task Log module.
 * Uses the team's custom Linear ADT for room data and housekeeping history.
 */
package control;

import adt.ADT;
import adt.LinkedADT;
import entity.HousekeepingTask;
import entity.Room;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class HousekeepingControl {

    public static final String STATUS_DIRTY = "Dirty";
    public static final String STATUS_CLEANING = "Cleaning In Progress";
    public static final String STATUS_INSPECTED = "Inspected";
    public static final String STATUS_READY = "Ready for Check-In";

    private final ADT<Room> roomList;
    private final ADT<HousekeepingTask> taskLog;
    private int taskSeed = 1;

    public HousekeepingControl(ADT<Room> roomList) {
        this.roomList = roomList;
        this.taskLog = new LinkedADT<>();
    }

    public ADT<Room> getRoomList() {
        return roomList;
    }

    public ADT<HousekeepingTask> getTaskLog() {
        return taskLog;
    }

    /**
     * Linear search for a room by its room number.
     */
    public Room findRoomByNumber(String roomNumber) {
        if (roomNumber == null || roomNumber.trim().isEmpty()) {
            return null;
        }

        String target = roomNumber.trim();
        for (int i = 0; i < roomList.getNumberOfEntries(); i++) {
            Room room = roomList.getEntry(i);
            if (room != null && room.getRoomNumber().equalsIgnoreCase(target)) {
                return room;
            }
        }
        return null;
    }

    /**
     * Updates cleaning status using the normal housekeeping sequence.
     * Dirty -> Cleaning In Progress -> Inspected -> Ready for Check-In.
     */
    public HousekeepingTask updateCleaningStatus(String roomNumber, String newStatus,
            String staffName, String remarks) {
        Room room = findRoomByNumber(roomNumber);
        if (room == null) {
            throw new IllegalArgumentException("Room " + roomNumber + " was not found.");
        }

        String staff = requireNonBlank(staffName, "Staff name");
        String normalizedStatus = normalizeStatus(newStatus);
        String currentStatus = normalizeStatus(room.getCleaningStatus());

        if (!isValidTransition(currentStatus, normalizedStatus)) {
            throw new IllegalStateException("Invalid status transition: " + currentStatus
                    + " -> " + normalizedStatus + ". Follow the housekeeping sequence.");
        }

        return recordStatusChange(room, staff, currentStatus, normalizedStatus,
                remarks, false);
    }

    /**
     * Allows an authorised supervisor to correct an existing cleaning status.
     * The correction is still recorded in the task history so it can be audited
     * and rolled back.
     */
    public HousekeepingTask correctCleaningStatus(String roomNumber, String newStatus,
            String staffName, String remarks) {
        Room room = findRoomByNumber(roomNumber);
        if (room == null) {
            throw new IllegalArgumentException("Room " + roomNumber + " was not found.");
        }

        String staff = requireNonBlank(staffName, "Staff/supervisor name");
        String normalizedStatus = normalizeStatus(newStatus);
        String currentStatus = normalizeStatus(room.getCleaningStatus());

        if (currentStatus.equalsIgnoreCase(normalizedStatus)) {
            throw new IllegalStateException("The room is already in " + normalizedStatus + ".");
        }

        String cleanRemarks = (remarks == null || remarks.trim().isEmpty())
                ? "Supervisor status correction" : remarks.trim();

        return recordStatusChange(room, staff, currentStatus, normalizedStatus,
                cleanRemarks, true);
    }

    /**
     * Changes room availability separately from cleaning status.
     * Unavailable is therefore not treated as a cleaning status.
     */
    public void updateRoomAvailability(String roomNumber, boolean available) {
        Room room = findRoomByNumber(roomNumber);
        if (room == null) {
            throw new IllegalArgumentException("Room " + roomNumber + " was not found.");
        }
        room.setAvailability(available);
    }

    /**
     * LIFO rollback: removes the latest task from the custom Linear ADT and
     * restores that task's previous cleaning status.
     */
    public HousekeepingTask rollbackLatestUpdate() {
        HousekeepingTask latest = taskLog.removeLast();
        if (latest == null) {
            return null;
        }

        Room room = latest.getRoom();
        if (room != null) {
            room.setCleaningStatus(latest.getPreviousStatus());
        }
        return latest;
    }

    /**
     * Rolls back the latest recorded task belonging to a selected room.
     * The search uses the ADT entries and removeAt() when the room's latest
     * task is not the overall last task.
     */
    public HousekeepingTask rollbackLatestUpdateForRoom(String roomNumber) {
        Room room = findRoomByNumber(roomNumber);
        if (room == null) {
            throw new IllegalArgumentException("Room " + roomNumber + " was not found.");
        }

        int latestIndex = -1;
        for (int i = 0; i < taskLog.getNumberOfEntries(); i++) {
            HousekeepingTask task = taskLog.getEntry(i);
            if (task != null && task.getRoom() != null
                    && task.getRoom().getRoomNumber().equalsIgnoreCase(room.getRoomNumber())) {
                latestIndex = i;
            }
        }

        if (latestIndex == -1) {
            return null;
        }

        HousekeepingTask latest = taskLog.removeAt(latestIndex);
        if (latest != null) {
            room.setCleaningStatus(latest.getPreviousStatus());
        }
        return latest;
    }

    /**
     * Late check-out during cleaning resets the room to Dirty and records the
     * event in the same task history.
     */
    public HousekeepingTask handleLateCheckout(String roomNumber, String staffName, String remarks) {
        Room room = findRoomByNumber(roomNumber);
        if (room == null) {
            throw new IllegalArgumentException("Room " + roomNumber + " was not found.");
        }

        String current = normalizeStatus(room.getCleaningStatus());
        if (!current.equalsIgnoreCase(STATUS_CLEANING)
                && !current.equalsIgnoreCase(STATUS_INSPECTED)) {
            throw new IllegalStateException(
                    "Late check-out reset is only applicable while cleaning or inspection is in progress.");
        }

        String staff = requireNonBlank(staffName, "Staff/supervisor name");
        String cleanRemarks = (remarks == null || remarks.trim().isEmpty())
                ? "Late check-out: cleaning schedule reset to Dirty" : remarks.trim();

        return recordStatusChange(room, staff, current, STATUS_DIRTY, cleanRemarks, true);
    }

    /**
     * Checks the required sequential workflow.
     */
    public boolean isValidTransition(String currentStatus, String newStatus) {
        if (currentStatus == null || newStatus == null) {
            return false;
        }

        if (currentStatus.equalsIgnoreCase(STATUS_DIRTY)) {
            return newStatus.equalsIgnoreCase(STATUS_CLEANING);
        }
        if (currentStatus.equalsIgnoreCase(STATUS_CLEANING)) {
            return newStatus.equalsIgnoreCase(STATUS_INSPECTED);
        }
        if (currentStatus.equalsIgnoreCase(STATUS_INSPECTED)) {
            return newStatus.equalsIgnoreCase(STATUS_READY);
        }
        return false;
    }

    public String getNextExpectedStatus(Room room) {
        if (room == null || room.getCleaningStatus() == null) {
            return null;
        }

        String status = normalizeStatus(room.getCleaningStatus());
        if (status.equalsIgnoreCase(STATUS_DIRTY)) {
            return STATUS_CLEANING;
        }
        if (status.equalsIgnoreCase(STATUS_CLEANING)) {
            return STATUS_INSPECTED;
        }
        if (status.equalsIgnoreCase(STATUS_INSPECTED)) {
            return STATUS_READY;
        }
        return null;
    }

    /**
     * Report 1: multiple-criteria filtering followed by manual insertion sort.
     */
    public void printRoomCleaningStatusReport(String statusFilter, Boolean availabilityFilter) {
        Room[] filtered = new Room[roomList.getNumberOfEntries()];
        int count = 0;

        for (int i = 0; i < roomList.getNumberOfEntries(); i++) {
            Room room = roomList.getEntry(i);
            boolean matchesStatus = statusFilter == null
                    || room.getCleaningStatus().equalsIgnoreCase(statusFilter);
            boolean matchesAvailability = availabilityFilter == null
                    || room.isAvailable() == availabilityFilter;

            if (matchesStatus && matchesAvailability) {
                filtered[count++] = room;
            }
        }

        insertionSortRooms(filtered, count);

        System.out.println("\n=======================================================================");
        System.out.println("                    ROOM CLEANING STATUS REPORT");
        System.out.println("=======================================================================");
        System.out.println("Filter -> Status: " + (statusFilter == null ? "ALL" : statusFilter)
                + " | Availability: " + (availabilityFilter == null ? "ALL"
                : (availabilityFilter ? "Available" : "Unavailable")));
        System.out.println("-----------------------------------------------------------------------");
        System.out.printf("%-10s %-26s %-16s %-22s%n",
                "Room No.", "Cleaning Status", "Availability", "Next Status");
        System.out.println("-----------------------------------------------------------------------");

        for (int i = 0; i < count; i++) {
            Room room = filtered[i];
            String next = getNextExpectedStatus(room);
            System.out.printf("%-10s %-26s %-16s %-22s%n",
                    room.getRoomNumber(), room.getCleaningStatus(),
                    room.isAvailable() ? "Available" : "Unavailable",
                    next == null ? "-" : next);
        }

        System.out.println("-----------------------------------------------------------------------");
        System.out.println("Total matching rooms: " + count);
        System.out.println("=======================================================================\n");
    }

    /**
     * Report 2: multiple-criteria task filtering followed by manual insertion sort.
     * Criteria: staff + room + new status.
     */
    public void printTaskActivityReport(String staffFilter, String roomFilter,
            String newStatusFilter) {
        HousekeepingTask[] filtered = new HousekeepingTask[taskLog.getNumberOfEntries()];
        int count = 0;

        for (int i = 0; i < taskLog.getNumberOfEntries(); i++) {
            HousekeepingTask task = taskLog.getEntry(i);
            String taskRoom = task.getRoom() == null ? "" : task.getRoom().getRoomNumber();

            boolean matchesStaff = staffFilter == null
                    || task.getStaffName().equalsIgnoreCase(staffFilter);
            boolean matchesRoom = roomFilter == null
                    || taskRoom.equalsIgnoreCase(roomFilter);
            boolean matchesStatus = newStatusFilter == null
                    || task.getNewStatus().equalsIgnoreCase(newStatusFilter);

            if (matchesStaff && matchesRoom && matchesStatus) {
                filtered[count++] = task;
            }
        }

        insertionSortTasks(filtered, count);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        System.out.println("\n========================================================================================================");
        System.out.println("                              HOUSEKEEPING TASK ACTIVITY REPORT");
        System.out.println("========================================================================================================");
        System.out.println("Filters -> Staff: " + (staffFilter == null ? "ALL" : staffFilter)
                + " | Room: " + (roomFilter == null ? "ALL" : roomFilter)
                + " | New Status: " + (newStatusFilter == null ? "ALL" : newStatusFilter));
        System.out.println("--------------------------------------------------------------------------------------------------------");
        System.out.printf("%-7s %-7s %-16s %-22s %-22s %-17s%n",
                "Task", "Room", "Staff", "Previous", "New Status", "Date/Time");
        System.out.println("--------------------------------------------------------------------------------------------------------");

        for (int i = 0; i < count; i++) {
            HousekeepingTask task = filtered[i];
            String room = task.getRoom() == null ? "-" : task.getRoom().getRoomNumber();
            String time = task.getTaskDateTime() == null ? "-"
                    : task.getTaskDateTime().format(formatter);
            System.out.printf("%-7s %-7s %-16s %-22s %-22s %-17s%n",
                    task.getTaskId(), room, task.getStaffName(),
                    task.getPreviousStatus(), task.getNewStatus(), time);
        }

        System.out.println("--------------------------------------------------------------------------------------------------------");
        System.out.println("Total matching tasks: " + count);
        System.out.println("========================================================================================================\n");
    }

    private HousekeepingTask recordStatusChange(Room room, String staff,
            String previousStatus, String newStatus, String remarks, boolean specialEvent) {
        String cleanRemarks = remarks == null ? "" : remarks.trim();
        if (specialEvent && cleanRemarks.isEmpty()) {
            cleanRemarks = "Housekeeping status correction";
        }

        HousekeepingTask task = new HousekeepingTask(
                nextTaskId(), room, staff, previousStatus, newStatus,
                LocalDateTime.now(), cleanRemarks);

        room.setCleaningStatus(newStatus);
        taskLog.insertLast(task);
        return task;
    }

    private void insertionSortRooms(Room[] rooms, int count) {
        for (int i = 1; i < count; i++) {
            Room key = rooms[i];
            int j = i - 1;
            while (j >= 0 && compareRoomNumbers(rooms[j].getRoomNumber(), key.getRoomNumber()) > 0) {
                rooms[j + 1] = rooms[j];
                j--;
            }
            rooms[j + 1] = key;
        }
    }

    private void insertionSortTasks(HousekeepingTask[] tasks, int count) {
        for (int i = 1; i < count; i++) {
            HousekeepingTask key = tasks[i];
            int j = i - 1;
            while (j >= 0 && compareTasks(tasks[j], key) > 0) {
                tasks[j + 1] = tasks[j];
                j--;
            }
            tasks[j + 1] = key;
        }
    }

    private int compareRoomNumbers(String first, String second) {
        try {
            return Integer.compare(Integer.parseInt(first), Integer.parseInt(second));
        } catch (NumberFormatException ex) {
            return first.compareToIgnoreCase(second);
        }
    }

    private int compareTasks(HousekeepingTask first, HousekeepingTask second) {
        int roomCompare = compareRoomNumbers(
                first.getRoom() == null ? "" : first.getRoom().getRoomNumber(),
                second.getRoom() == null ? "" : second.getRoom().getRoomNumber());
        if (roomCompare != 0) {
            return roomCompare;
        }
        return first.getTaskId().compareToIgnoreCase(second.getTaskId());
    }

    private String normalizeStatus(String status) {
        if (status == null) {
            throw new IllegalArgumentException("Cleaning status cannot be empty.");
        }

        String value = status.trim();
        if (value.equalsIgnoreCase(STATUS_DIRTY)) {
            return STATUS_DIRTY;
        }
        if (value.equalsIgnoreCase(STATUS_CLEANING)) {
            return STATUS_CLEANING;
        }
        if (value.equalsIgnoreCase(STATUS_INSPECTED)) {
            return STATUS_INSPECTED;
        }
        if (value.equalsIgnoreCase(STATUS_READY)) {
            return STATUS_READY;
        }
        throw new IllegalArgumentException("Unknown housekeeping status: " + status);
    }

    private String requireNonBlank(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be empty.");
        }
        return value.trim();
    }

    private String nextTaskId() {
        return String.format("HK%03d", taskSeed++);
    }
}
