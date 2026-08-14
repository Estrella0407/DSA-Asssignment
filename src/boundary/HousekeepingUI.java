/*
 * Module: Housekeeping and Task Log (Boundary UI Component)
 * Author: NEO AI YIK
 * 
 * Description:
 * Boundary class handling console user interaction for Housekeeping and Task Log.
 * Strictly adheres to ECB constraints (communicates only with Actor and HousekeepingControl).
 */
package boundary;

import adt.DoublyLinkedListInterface;
import control.HousekeepingControl;
import entity.HousekeepingTask;
import entity.Room;
import java.util.Scanner;

public class HousekeepingUI {

    private final HousekeepingControl control;
    private final Scanner scanner;

    public HousekeepingUI(HousekeepingControl control, Scanner scanner) {
        this.control = control;
        this.scanner = scanner;
    }

    public void run() {
        int choice;
        do {
            printMenu();
            choice = readInt("Enter choice: ");
            switch (choice) {
                case 1:
                    viewRoomCleaningStatus();
                    break;
                case 2:
                    updateCleaningStatus();
                    break;
                case 3:
                    updateRoomAvailability();
                    break;
                case 4:
                    rollbackLatestStatus();
                    break;
                case 5:
                    handleLateCheckout();
                    break;
                case 6:
                    viewTaskLog();
                    break;
                case 7:
                    generateRoomStatusReport();
                    break;
                case 8:
                    generateTaskActivityReport();
                    break;
                case 0:
                    System.out.println("Returning to Main Menu...");
                    break;
                default:
                    System.out.println("Invalid choice. Please enter 0 to 8.");
            }
        } while (choice != 0);
    }

    private void printMenu() {
        System.out.println("\n============================================================");
        System.out.println("               HOUSEKEEPING AND TASK LOG");
        System.out.println("============================================================");
        System.out.println("1. View Room Status");
        System.out.println("2. Update Cleaning Status");
        System.out.println("3. Update Room Availability");
        System.out.println("4. Undo Latest Cleaning Update");
        System.out.println("5. Handle Late Check-Out During Cleaning");
        System.out.println("6. View Housekeeping Task Log");
        System.out.println("7. Generate Room Cleaning Status Report");
        System.out.println("8. Generate Housekeeping Task Activity Report");
        System.out.println("0. Back to Main Menu");
        System.out.println("------------------------------------------------------------");
    }

    private void viewRoomCleaningStatus() {
        DoublyLinkedListInterface<Room> rooms = control.getRoomList();
        System.out.println("\n---------------- ROOM STATUS ----------------");
        if (rooms.isEmpty()) {
            System.out.println("No rooms available in the system.");
            return;
        }

        System.out.printf("%-8s %-24s %-15s %-24s%n",
                "Room", "Cleaning Status", "Availability", "Next Status");
        System.out.println("-----------------------------------------------------------------------");
        for (int i = 0; i < rooms.getNumberOfEntries(); i++) {
            Room room = rooms.getEntry(i);
            String next = control.getNextExpectedStatus(room);
            System.out.printf("%-8s %-24s %-15s %-24s%n",
                    room.getRoomNumber(), room.getCleaningStatus(),
                    room.isAvailable() ? "Available" : "Unavailable",
                    next == null ? "-" : next);
        }
    }

    private void updateCleaningStatus() {
        System.out.print("Enter room number: ");
        String roomNumber = scanner.nextLine().trim();
        Room room = control.findRoomByNumber(roomNumber);
        if (room == null) {
            System.out.println("Room not found.");
            return;
        }

        System.out.println("Current cleaning status: " + room.getCleaningStatus());
        System.out.println("1. Normal Sequential Update");
        System.out.println("2. Supervisor Status Correction");
        int mode = readInt("Choose update mode: ");

        if (mode != 1 && mode != 2) {
            System.out.println("Invalid update mode.");
            return;
        }

        String newStatus;
        if (mode == 1) {
            newStatus = control.getNextExpectedStatus(room);
            if (newStatus == null) {
                System.out.println("Room " + room.getRoomNumber()
                        + " is already Ready for Check-In.");
                return;
            }
            System.out.println("Next required status: " + newStatus);
        } else {
            System.out.println("Supervisor correction can change the cleaning status to any valid cleaning status.");
            System.out.println("1. Dirty");
            System.out.println("2. Cleaning In Progress");
            System.out.println("3. Inspected");
            System.out.println("4. Ready for Check-In");
            int statusChoice = readInt("Choose corrected status: ");
            newStatus = statusFromChoice(statusChoice, false);
            if (newStatus == null) {
                System.out.println("Invalid cleaning status.");
                return;
            }
        }

        System.out.print("Enter staff/supervisor name: ");
        String staffName = scanner.nextLine();
        System.out.print("Enter remarks (optional): ");
        String remarks = scanner.nextLine();

        try {
            HousekeepingTask task;
            if (mode == 1) {
                task = control.updateCleaningStatus(roomNumber, newStatus, staffName, remarks);
            } else {
                task = control.correctCleaningStatus(roomNumber, newStatus, staffName, remarks);
            }

            System.out.println("Status updated successfully.");
            System.out.println("Task ID: " + task.getTaskId());
            System.out.println("Room " + room.getRoomNumber() + " -> " + room.getCleaningStatus());
        } catch (IllegalArgumentException | IllegalStateException ex) {
            System.out.println("Update failed: " + ex.getMessage());
        }
    }

    private void updateRoomAvailability() {
        System.out.print("Enter room number: ");
        String roomNumber = scanner.nextLine().trim();
        Room room = control.findRoomByNumber(roomNumber);
        if (room == null) {
            System.out.println("Room not found.");
            return;
        }

        System.out.println("Current availability: " + (room.isAvailable() ? "Available" : "Unavailable"));
        System.out.println("1. Available");
        System.out.println("2. Unavailable");
        int choice = readInt("Choose availability: ");
        if (choice != 1 && choice != 2) {
            System.out.println("Invalid availability choice.");
            return;
        }

        try {
            control.updateRoomAvailability(roomNumber, choice == 1);
            System.out.println("Room " + roomNumber + " is now "
                    + (room.isAvailable() ? "Available" : "Unavailable") + ".");
            System.out.println("Note: availability is separate from cleaning status.");
        } catch (IllegalArgumentException ex) {
            System.out.println(ex.getMessage());
        }
    }

    private void rollbackLatestStatus() {
        System.out.print("Enter room number (or press Enter for the overall latest task): ");
        String roomNumber = scanner.nextLine().trim();

        try {
            HousekeepingTask task;
            if (roomNumber.isEmpty()) {
                task = control.rollbackLatestUpdate();
            } else {
                task = control.rollbackLatestUpdateForRoom(roomNumber);
            }

            if (task == null) {
                System.out.println("No housekeeping update is available to rollback.");
                return;
            }

            System.out.println("Rollback completed.");
            System.out.println("Removed task : " + task.getTaskId());
            System.out.println("Room         : "
                    + (task.getRoom() == null ? "-" : task.getRoom().getRoomNumber()));
            System.out.println("Changed back : " + task.getNewStatus() + " -> " + task.getPreviousStatus());
        } catch (IllegalArgumentException ex) {
            System.out.println("Rollback failed: " + ex.getMessage());
        }
    }

    private void handleLateCheckout() {
        System.out.print("Enter room number: ");
        String roomNumber = scanner.nextLine().trim();
        System.out.print("Enter staff/supervisor name: ");
        String staffName = scanner.nextLine();
        System.out.print("Enter remarks (optional): ");
        String remarks = scanner.nextLine();

        try {
            HousekeepingTask task = control.handleLateCheckout(roomNumber, staffName, remarks);
            System.out.println("Late check-out handled.");
            System.out.println("Cleaning schedule reset to Dirty.");
            System.out.println("Task ID: " + task.getTaskId());
        } catch (IllegalArgumentException | IllegalStateException ex) {
            System.out.println("Unable to handle late check-out: " + ex.getMessage());
        }
    }

    private void viewTaskLog() {
        DoublyLinkedListInterface<HousekeepingTask> tasks = control.getTaskLog();
        System.out.println("\n---------------- HOUSEKEEPING TASK LOG ----------------");
        if (tasks.isEmpty()) {
            System.out.println("No housekeeping tasks have been recorded yet.");
            return;
        }

        System.out.println("Latest task is kept at the end of the Linear ADT for LIFO rollback.");
        System.out.println("----------------------------------------------------------------------------------------------");
        for (int i = 0; i < tasks.getNumberOfEntries(); i++) {
            System.out.println(tasks.getEntry(i));
        }
        System.out.println("----------------------------------------------------------------------------------------------");
        System.out.println("Total tasks: " + tasks.getNumberOfEntries());
    }

    private void generateRoomStatusReport() {
        System.out.println("\nStatus filter:");
        System.out.println("0. ALL");
        System.out.println("1. Dirty");
        System.out.println("2. Cleaning In Progress");
        System.out.println("3. Inspected");
        System.out.println("4. Ready for Check-In");
        int statusChoice = readInt("Choose status filter: ");
        if (statusChoice < 0 || statusChoice > 4) {
            System.out.println("Invalid status filter.");
            return;
        }

        String statusFilter = statusFromChoice(statusChoice, true);

        System.out.println("Availability filter:");
        System.out.println("0. ALL");
        System.out.println("1. Available");
        System.out.println("2. Unavailable");
        int availabilityChoice = readInt("Choose availability filter: ");
        if (availabilityChoice < 0 || availabilityChoice > 2) {
            System.out.println("Invalid availability filter.");
            return;
        }

        Boolean availabilityFilter = availabilityChoice == 0
                ? null : availabilityChoice == 1;
        control.printRoomCleaningStatusReport(statusFilter, availabilityFilter);
    }

    private void generateTaskActivityReport() {
        System.out.print("Enter staff filter (press Enter for ALL): ");
        String staff = scanner.nextLine().trim();
        String staffFilter = staff.isEmpty() ? null : staff;

        System.out.print("Enter room number filter (press Enter for ALL): ");
        String room = scanner.nextLine().trim();
        String roomFilter = room.isEmpty() ? null : room;

        System.out.println("New-status filter:");
        System.out.println("0. ALL");
        System.out.println("1. Dirty");
        System.out.println("2. Cleaning In Progress");
        System.out.println("3. Inspected");
        System.out.println("4. Ready for Check-In");
        int statusChoice = readInt("Choose status filter: ");
        if (statusChoice < 0 || statusChoice > 4) {
            System.out.println("Invalid status filter.");
            return;
        }

        String statusFilter = statusFromChoice(statusChoice, true);
        control.printTaskActivityReport(staffFilter, roomFilter, statusFilter);
    }

    private String statusFromChoice(int choice, boolean allowAll) {
        if (allowAll && choice == 0) {
            return null;
        }
        switch (choice) {
            case 1:
                return HousekeepingControl.STATUS_DIRTY;
            case 2:
                return HousekeepingControl.STATUS_CLEANING;
            case 3:
                return HousekeepingControl.STATUS_INSPECTED;
            case 4:
                return HousekeepingControl.STATUS_READY;
            default:
                return null;
        }
    }

    private int readInt(String prompt) {
        System.out.print(prompt);
        while (!scanner.hasNextInt()) {
            System.out.print("Please enter a valid number: ");
            scanner.next();
        }
        int value = scanner.nextInt();
        scanner.nextLine();
        return value;
    }
}
