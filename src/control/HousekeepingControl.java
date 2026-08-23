/*
 * Module: Housekeeping and Task Log (Control Component)
 * Author: NEO AI YIK
 * 
 * Description:
 * Control class implementing business logic for Housekeeping and Task Log.
 * Coordinates sequential room cleaning workflows, LIFO schedule undo / rollback using
 * the Linear ADT task log, late check-out resets, supervisor corrections, and management reports.
 */
package control;

import adt.DoublyLinkedList;
import adt.DoublyLinkedListInterface;
import entity.HousekeepingTask;
import entity.Room;
import java.time.LocalDateTime;

public class HousekeepingControl {

    public static final String STATUS_DIRTY = "Dirty";
    public static final String STATUS_CLEANING = "Cleaning In Progress";
    public static final String STATUS_INSPECTED = "Inspected";
    public static final String STATUS_READY = "Ready for Check-In";

    private final DoublyLinkedListInterface<Room> roomList;
    private final DoublyLinkedListInterface<HousekeepingTask> taskLog;
    private int taskSeed = 1;

    public HousekeepingControl(DoublyLinkedListInterface<Room> roomList) {
        this.roomList = roomList;
        this.taskLog = new DoublyLinkedList<>();
    }

    public DoublyLinkedListInterface<Room> getRoomList() {
        return roomList;
    }

    public DoublyLinkedListInterface<HousekeepingTask> getTaskLog() {
        return taskLog;
    }

    /* Boundary-safe views: the UI receives Control DTOs instead of Entity/ADT objects. */
    public RoomView getRoomView(String roomNumber) {
        Room room = findRoomByNumber(roomNumber);
        return room == null ? null : new RoomView(room, getNextExpectedStatus(room));
    }

    public RoomView[] getRoomViews() {
        RoomView[] views = new RoomView[roomList.getNumberOfEntries()];
        for (int i = 0; i < views.length; i++) {
            Room room = roomList.getEntry(i);
            views[i] = new RoomView(room, getNextExpectedStatus(room));
        }
        return views;
    }

    public TaskView[] getTaskViews() {
        TaskView[] views = new TaskView[taskLog.getNumberOfEntries()];
        for (int i = 0; i < views.length; i++) {
            views[i] = new TaskView(taskLog.getEntry(i));
        }
        return views;
    }

    public TaskView updateCleaningStatusForUI(String roomNumber, String newStatus,
            String staffName, String remarks) {
        return new TaskView(updateCleaningStatus(roomNumber, newStatus, staffName, remarks));
    }

    public TaskView correctCleaningStatusForUI(String roomNumber, String newStatus,
            String staffName, String remarks) {
        return new TaskView(correctCleaningStatus(roomNumber, newStatus, staffName, remarks));
    }

    public TaskView rollbackLatestUpdateForUI(String roomNumber) {
        HousekeepingTask task = (roomNumber == null || roomNumber.trim().isEmpty())
                ? rollbackLatestUpdate() : rollbackLatestUpdateForRoom(roomNumber);
        return task == null ? null : new TaskView(task);
    }

    public TaskView handleLateCheckoutForUI(String roomNumber, String staffName, String remarks) {
        return new TaskView(handleLateCheckout(roomNumber, staffName, remarks));
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
     * Updates cleaning status using the normal housekeeping sequence. Dirty ->
     * Cleaning In Progress -> Inspected -> Ready for Check-In.
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
     * Changes room availability separately from cleaning status. Unavailable is
     * therefore not treated as a cleaning status.
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
     * Rolls back the latest recorded task belonging to a selected room. The
     * search uses the ADT entries and removeAt() when the room's latest task is
     * not the overall last task.
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

    public String getNextExpectedStatus(String cleaningStatus) {
        if (cleaningStatus == null) {
            return null;
        }
        String status = normalizeStatus(cleaningStatus);
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
     * Report 1 processing: performs a linear scan using the custom ADT,
     * applies multiple criteria, manually sorts the matching rooms, and
     * computes management summary metrics. Display is intentionally left to
     * the Boundary class to follow the ECB architecture.
     */
    public RoomStatusReport generateRoomCleaningStatusReport(String statusFilter,
            Boolean availabilityFilter) {
        Room[] working = new Room[roomList.getNumberOfEntries()];
        int matchCount = 0;

        int dirtyCount = 0;
        int cleaningCount = 0;
        int inspectedCount = 0;
        int readyCount = 0;
        int availableCount = 0;
        int unavailableCount = 0;

        for (int i = 0; i < roomList.getNumberOfEntries(); i++) {
            Room room = roomList.getEntry(i);
            if (room == null) {
                continue;
            }

            String status = normalizeStatus(room.getCleaningStatus());
            if (status.equalsIgnoreCase(STATUS_DIRTY)) {
                dirtyCount++;
            } else if (status.equalsIgnoreCase(STATUS_CLEANING)) {
                cleaningCount++;
            } else if (status.equalsIgnoreCase(STATUS_INSPECTED)) {
                inspectedCount++;
            } else if (status.equalsIgnoreCase(STATUS_READY)) {
                readyCount++;
            }

            if (room.isAvailable()) {
                availableCount++;
            } else {
                unavailableCount++;
            }

            boolean matchesStatus = statusFilter == null
                    || status.equalsIgnoreCase(statusFilter);
            boolean matchesAvailability = availabilityFilter == null
                    || room.isAvailable() == availabilityFilter;

            if (matchesStatus && matchesAvailability) {
                working[matchCount++] = room;
            }
        }

        insertionSortRooms(working, matchCount);
        Room[] matches = new Room[matchCount];
        for (int i = 0; i < matchCount; i++) {
            matches[i] = working[i];
        }

        int totalRooms = roomList.getNumberOfEntries();
        double readyRate = totalRooms == 0 ? 0.0 : (readyCount * 100.0) / totalRooms;

        return new RoomStatusReport(statusFilter, availabilityFilter, matches,
                totalRooms, dirtyCount, cleaningCount, inspectedCount, readyCount,
                availableCount, unavailableCount, readyRate);
    }

    /**
     * Report 2 processing: performs a linear scan using the custom ADT,
     * applies staff + room + status filters, manually sorts the matching
     * tasks, and computes activity summary metrics.
     */
    public TaskActivityReport generateTaskActivityReport(String staffFilter,
            String roomFilter, String newStatusFilter) {
        HousekeepingTask[] working = new HousekeepingTask[taskLog.getNumberOfEntries()];
        int matchCount = 0;
        int completedToReady = 0;
        int resetToDirty = 0;

        String mostActiveStaff = "-";
        int mostActiveStaffCount = 0;

        for (int i = 0; i < taskLog.getNumberOfEntries(); i++) {
            HousekeepingTask task = taskLog.getEntry(i);
            if (task == null) {
                continue;
            }

            if (task.getNewStatus() != null
                    && task.getNewStatus().equalsIgnoreCase(STATUS_READY)) {
                completedToReady++;
            }
            if (task.getNewStatus() != null
                    && task.getNewStatus().equalsIgnoreCase(STATUS_DIRTY)
                    && task.getPreviousStatus() != null
                    && !task.getPreviousStatus().equalsIgnoreCase(STATUS_DIRTY)) {
                resetToDirty++;
            }

            String taskRoom = task.getRoom() == null ? "" : task.getRoom().getRoomNumber();
            boolean matchesStaff = staffFilter == null
                    || task.getStaffName().equalsIgnoreCase(staffFilter);
            boolean matchesRoom = roomFilter == null
                    || taskRoom.equalsIgnoreCase(roomFilter);
            boolean matchesStatus = newStatusFilter == null
                    || task.getNewStatus().equalsIgnoreCase(newStatusFilter);

            if (matchesStaff && matchesRoom && matchesStatus) {
                working[matchCount++] = task;
            }
        }

        // Determine the most active staff member without using Java Collections.
        for (int i = 0; i < taskLog.getNumberOfEntries(); i++) {
            HousekeepingTask candidate = taskLog.getEntry(i);
            if (candidate == null || candidate.getStaffName() == null
                    || candidate.getStaffName().trim().isEmpty()) {
                continue;
            }

            int candidateCount = 0;
            for (int j = 0; j < taskLog.getNumberOfEntries(); j++) {
                HousekeepingTask other = taskLog.getEntry(j);
                if (other != null && other.getStaffName() != null
                        && other.getStaffName().equalsIgnoreCase(candidate.getStaffName())) {
                    candidateCount++;
                }
            }

            if (candidateCount > mostActiveStaffCount) {
                mostActiveStaff = candidate.getStaffName();
                mostActiveStaffCount = candidateCount;
            }
        }

        insertionSortTasks(working, matchCount);
        HousekeepingTask[] matches = new HousekeepingTask[matchCount];
        for (int i = 0; i < matchCount; i++) {
            matches[i] = working[i];
        }

        return new TaskActivityReport(staffFilter, roomFilter, newStatusFilter,
                matches, taskLog.getNumberOfEntries(), completedToReady, resetToDirty,
                mostActiveStaff, mostActiveStaffCount);
    }

    /**
     * Read-only data returned by Report 1.
     */
    public static class RoomStatusReport {

        private final String statusFilter;
        private final Boolean availabilityFilter;
        private final Room[] rooms;
        private final int totalRooms;
        private final int dirtyCount;
        private final int cleaningCount;
        private final int inspectedCount;
        private final int readyCount;
        private final int availableCount;
        private final int unavailableCount;
        private final double readyRate;

        private RoomStatusReport(String statusFilter, Boolean availabilityFilter,
                Room[] rooms, int totalRooms, int dirtyCount, int cleaningCount,
                int inspectedCount, int readyCount, int availableCount,
                int unavailableCount, double readyRate) {
            this.statusFilter = statusFilter;
            this.availabilityFilter = availabilityFilter;
            this.rooms = rooms;
            this.totalRooms = totalRooms;
            this.dirtyCount = dirtyCount;
            this.cleaningCount = cleaningCount;
            this.inspectedCount = inspectedCount;
            this.readyCount = readyCount;
            this.availableCount = availableCount;
            this.unavailableCount = unavailableCount;
            this.readyRate = readyRate;
        }

        public String getStatusFilter() { return statusFilter; }
        public Boolean getAvailabilityFilter() { return availabilityFilter; }
        public Room[] getRooms() { return rooms; }
        public RoomView[] getRoomViews() {
            RoomView[] views = new RoomView[rooms.length];
            for (int i = 0; i < rooms.length; i++) {
                views[i] = new RoomView(rooms[i], null);
            }
            return views;
        }
        public int getMatchCount() { return rooms.length; }
        public int getTotalRooms() { return totalRooms; }
        public int getDirtyCount() { return dirtyCount; }
        public int getCleaningCount() { return cleaningCount; }
        public int getInspectedCount() { return inspectedCount; }
        public int getReadyCount() { return readyCount; }
        public int getAvailableCount() { return availableCount; }
        public int getUnavailableCount() { return unavailableCount; }
        public double getReadyRate() { return readyRate; }
    }

    /**
     * Read-only data returned by Report 2.
     */
    public static class TaskActivityReport {

        private final String staffFilter;
        private final String roomFilter;
        private final String newStatusFilter;
        private final HousekeepingTask[] tasks;
        private final int totalTasks;
        private final int completedToReady;
        private final int resetToDirty;
        private final String mostActiveStaff;
        private final int mostActiveStaffCount;

        private TaskActivityReport(String staffFilter, String roomFilter,
                String newStatusFilter, HousekeepingTask[] tasks, int totalTasks,
                int completedToReady, int resetToDirty, String mostActiveStaff,
                int mostActiveStaffCount) {
            this.staffFilter = staffFilter;
            this.roomFilter = roomFilter;
            this.newStatusFilter = newStatusFilter;
            this.tasks = tasks;
            this.totalTasks = totalTasks;
            this.completedToReady = completedToReady;
            this.resetToDirty = resetToDirty;
            this.mostActiveStaff = mostActiveStaff;
            this.mostActiveStaffCount = mostActiveStaffCount;
        }

        public String getStaffFilter() { return staffFilter; }
        public String getRoomFilter() { return roomFilter; }
        public String getNewStatusFilter() { return newStatusFilter; }
        public HousekeepingTask[] getTasks() { return tasks; }
        public TaskView[] getTaskViews() {
            TaskView[] views = new TaskView[tasks.length];
            for (int i = 0; i < tasks.length; i++) {
                views[i] = new TaskView(tasks[i]);
            }
            return views;
        }
        public int getMatchCount() { return tasks.length; }
        public int getTotalTasks() { return totalTasks; }
        public int getCompletedToReady() { return completedToReady; }
        public int getResetToDirty() { return resetToDirty; }
        public String getMostActiveStaff() { return mostActiveStaff; }
        public int getMostActiveStaffCount() { return mostActiveStaffCount; }
    }

    public static class RoomView {
        private final String roomNumber;
        private final String roomType;
        private final String cleaningStatus;
        private final boolean available;
        private final String nextStatus;

        private RoomView(Room room, String nextStatus) {
            this.roomNumber = room == null ? "-" : room.getRoomNumber();
            this.roomType = room == null ? "-" : room.getRoomType();
            this.cleaningStatus = room == null ? "-" : room.getCleaningStatus();
            this.available = room != null && room.isAvailable();
            this.nextStatus = nextStatus;
        }

        public String getRoomNumber() { return roomNumber; }
        public String getRoomType() { return roomType; }
        public String getCleaningStatus() { return cleaningStatus; }
        public boolean isAvailable() { return available; }
        public String getNextStatus() { return nextStatus; }
    }

    public static class TaskView {
        private final String taskId;
        private final String roomNumber;
        private final String staffName;
        private final String previousStatus;
        private final String newStatus;
        private final LocalDateTime taskDateTime;
        private final String remarks;

        private TaskView(HousekeepingTask task) {
            this.taskId = task == null ? "-" : task.getTaskId();
            this.roomNumber = task == null || task.getRoom() == null
                    ? "-" : task.getRoom().getRoomNumber();
            this.staffName = task == null ? "-" : task.getStaffName();
            this.previousStatus = task == null ? "-" : task.getPreviousStatus();
            this.newStatus = task == null ? "-" : task.getNewStatus();
            this.taskDateTime = task == null ? null : task.getTaskDateTime();
            this.remarks = task == null ? "" : task.getRemarks();
        }

        public String getTaskId() { return taskId; }
        public String getRoomNumber() { return roomNumber; }
        public String getStaffName() { return staffName; }
        public String getPreviousStatus() { return previousStatus; }
        public String getNewStatus() { return newStatus; }
        public LocalDateTime getTaskDateTime() { return taskDateTime; }
        public String getRemarks() { return remarks; }

        @Override
        public String toString() {
            String time = taskDateTime == null ? "-"
                    : taskDateTime.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
            return String.format("%-7s | Room %-4s | %-16s | %-22s -> %-22s | %s | %s",
                    taskId, roomNumber, staffName, previousStatus, newStatus, time, remarks);
        }
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
