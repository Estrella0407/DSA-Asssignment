/*
 * Author: NEO AI YIK
 * Entity class representing one housekeeping status-change task/log entry.
 */
package entity;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class HousekeepingTask implements Serializable, Comparable<HousekeepingTask> {

    private String taskId;
    private Room room;
    private String staffName;
    private String previousStatus;
    private String newStatus;
    private LocalDateTime taskDateTime;
    private String remarks;

    public HousekeepingTask() {
        this("", null, "", "", "", LocalDateTime.now(), "");
    }

    public HousekeepingTask(String taskId, Room room, String staffName,
            String previousStatus, String newStatus,
            LocalDateTime taskDateTime, String remarks) {
        this.taskId = taskId;
        this.room = room;
        this.staffName = staffName;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.taskDateTime = taskDateTime;
        this.remarks = remarks;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        this.room = room;
    }

    public String getStaffName() {
        return staffName;
    }

    public void setStaffName(String staffName) {
        this.staffName = staffName;
    }

    public String getPreviousStatus() {
        return previousStatus;
    }

    public void setPreviousStatus(String previousStatus) {
        this.previousStatus = previousStatus;
    }

    public String getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(String newStatus) {
        this.newStatus = newStatus;
    }

    public LocalDateTime getTaskDateTime() {
        return taskDateTime;
    }

    public void setTaskDateTime(LocalDateTime taskDateTime) {
        this.taskDateTime = taskDateTime;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    @Override
    public int compareTo(HousekeepingTask other) {
        if (other == null) {
            return 1;
        }
        if (taskDateTime == null && other.taskDateTime == null) {
            return 0;
        }
        if (taskDateTime == null) {
            return -1;
        }
        if (other.taskDateTime == null) {
            return 1;
        }
        return taskDateTime.compareTo(other.taskDateTime);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        HousekeepingTask other = (HousekeepingTask) obj;
        return Objects.equals(taskId, other.taskId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(taskId);
    }

    @Override
    public String toString() {
        String roomNo = room == null ? "-" : room.getRoomNumber();
        String time = taskDateTime == null ? "-"
                : taskDateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        return String.format("%-7s | Room %-4s | %-16s | %-22s -> %-22s | %s | %s",
                taskId, roomNo, staffName, previousStatus, newStatus, time, remarks);
    }
}
