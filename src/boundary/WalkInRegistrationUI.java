/*
Module: Walk-In Registrations & Standard Booking Procedure (Boundary UI Component)
Author: LAW QINQI 

Description:
Boundary class handling console user interaction for Walk-In Registrations & Standard Booking.
Strictly adheres to ECB constraints (communicates only with Actor and WalkInRegistrationControl).
*/
package boundary;

import control.WalkInRegistrationControl;
import entity.Guest;
import java.util.Scanner;

public class WalkInRegistrationUI {

    private Scanner sc;
    private WalkInRegistrationControl control;

    // Standalone use: owns its own Scanner over System.in.
    public WalkInRegistrationUI(WalkInRegistrationControl control) {
        this(control, new Scanner(System.in));
    }

    /*
    Preferred when launched from another boundary class (e.g. MainMenuUI):
    reuses the caller's Scanner instead of opening a second one on System.in, which would otherwise silently drop buffered input.
    */
    public WalkInRegistrationUI(WalkInRegistrationControl control, Scanner sc) {
        this.control = control;
        this.sc = sc;
    }

    public void run() {
        int choice;
        do {
            printMenu();
            choice = readInt("Enter choice: ");
            switch (choice) {
                case 1:
                    registerWalkIn();
                    break;
                case 2:
                    registerBooking();
                    break;
                case 3:
                    processNextGuest();
                    break;
                case 4:
                    checkOutGuest();
                    break;
                case 5:
                    generateGuestCheckInReport();
                    break;
                case 6:
                    generateQueueSummaryReport();
                    break;
                case 0:
                    System.out.println("Exiting Walk-In Registration module. Goodbye!");
                    break;
                default:
                    System.out.println("Invalid choice, please try again.");
                    break;
            }
        } while (choice != 0);
    }

    private void printMenu() {
        System.out.println("\n===== WALK-IN REGISTRATIONS & STANDARD BOOKING PROCEDURE =====");
        System.out.println("1. Register Walk-In Guest");
        System.out.println("2. Register Standard Booking Guest");
        System.out.println("3. Process Next Guest in Queue (assign room + check-in)");
        System.out.println("4. Check-Out Guest");
        System.out.println("5. Print Guest Status Report");
        System.out.println("6. Print Guest Queue Summary Report");
        System.out.println("0. Exit");
    }

    private void registerWalkIn() {
        System.out.print("Enter guest name: ");
        String name = sc.nextLine().trim();
        System.out.print("Enter stay duration (in nights/days): ");
        int stayDays = readInt("");
        if (stayDays < 1) {
            stayDays = 1;
        }

        String preferredType = promptRoomType();
        System.out.print("Enter billing details (e.g. Cash / Credit Card): ");
        String billing = sc.nextLine().trim();

        try {
            Guest g = control.registerWalkIn(name, billing, preferredType, stayDays);
            System.out.println("\n==================================================");
            System.out.println("         WALK-IN REGISTRATION SUCCESSFUL         ");
            System.out.println("==================================================");
            System.out.println(g.toDetailedCard());
            System.out.println("Queue Position : " + control.getQueueSize());
            System.out.println("==================================================");

            if (g.getLastPromotionMessage() != null) {
                System.out.println("\n" + g.getLastPromotionMessage());
            }
        } catch (IllegalArgumentException ex) {
            System.out.println(">> Could not register walk-in guest: " + ex.getMessage());
        }
    }

    private void registerBooking() {
        System.out.print("Enter confirmation number ('SG-' is not needed): ");
        String conf = sc.nextLine().trim();
        System.out.print("Enter guest name: ");
        String name = sc.nextLine().trim();
        System.out.print("Enter stay duration (in nights/days): ");
        int stayDays = readInt("");
        if (stayDays < 1) {
            stayDays = 1;
        }

        String preferredType = promptRoomType();
        System.out.print("Enter billing details (e.g. Paid / Card): ");
        String billing = sc.nextLine().trim();

        try {
            Guest g = control.registerBooking(conf, name, billing, preferredType, stayDays);
            System.out.println("\n==================================================");
            System.out.println("         STANDARD BOOKING SUCCESSFUL              ");
            System.out.println("==================================================");
            System.out.println(g.toDetailedCard());
            System.out.println("Queue Position : " + control.getQueueSize());
            System.out.println("==================================================");

            if (g.getLastPromotionMessage() != null) {
                System.out.println("\n" + g.getLastPromotionMessage());
            }
        } catch (IllegalArgumentException ex) {
            System.out.println(">> Could not register booked guest: " + ex.getMessage());
        }
    }

    private String promptRoomType() {
        System.out.println("Preferred Room Type:");
        System.out.println("0. Any");
        System.out.println("1. Single");
        System.out.println("2. Double");
        System.out.println("3. Deluxe");
        System.out.println("4. Suite");
        int choice = readInt("Choose room type: ");
        switch (choice) {
            case 1: return "Single";
            case 2: return "Double";
            case 3: return "Deluxe";
            case 4: return "Suite";
            default: return null;
        }
    }

    private void processNextGuest() {
        if (control.getQueueSize() == 0) {
            System.out.println("The queue is empty - no guests to process.");
            return;
        }
        Guest next = control.peekNextInQueue();
        System.out.println("Next in line: " + next.getName() + " (" + next.getType() + ")");
        Guest processed = control.processNextGuest();
        if (processed == null) {
            System.out.println("No clean/available room found - guest remains at front of queue.");
        } else {
            System.out.println("Checked in " + processed.getName()
                    + " -> Room " + processed.getAssignedRoom().getRoomNumber()
                    + " (" + processed.getAssignedRoom().getRoomType() + ")");        }
    }

    private void checkOutGuest() {
        System.out.print("Enter confirmation number to check out: ");
        String conf = sc.nextLine();
        try {
            control.checkOutGuest(conf);
            System.out.println("Guest checked out successfully.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            System.out.println("Could not check out guest: " + ex.getMessage());
        }
    }

    private void generateGuestCheckInReport() {
        System.out.println("\n--- Guest Check-In Status Report Filters ---");
        System.out.println("Type filter: (0: ALL | 1: Walk-in | 2: Booked)");
        int typeChoice = readInt("Choose type filter: ");
        String typeFilter = null;
        if (typeChoice == 1) {
            typeFilter = "Walk-in";
        } else if (typeChoice == 2) {
            typeFilter = "Booked";
        }

        System.out.println("Status filter: (0: ALL | 1: Pending | 2: Checked-In | 3: Checked-Out)");
        int statusChoice = readInt("Choose status filter: ");
        String statusFilter = null;
        if (statusChoice == 1) {
            statusFilter = Guest.STATUS_PENDING;
        } else if (statusChoice == 2) {
            statusFilter = Guest.STATUS_CHECKED_IN;
        } else if (statusChoice == 3) {
            statusFilter = Guest.STATUS_CHECKED_OUT;
        }

        control.printGuestCheckInReport(typeFilter, statusFilter);
    }

    private void generateQueueSummaryReport() {
        System.out.println("\n--- Active Queue Audit Report Filters ---");
        System.out.println("Type filter: (0: ALL | 1: Walk-in | 2: Booked)");
        int typeChoice = readInt("Choose type filter: ");
        String typeFilter = null;
        if (typeChoice == 1) {
            typeFilter = "Walk-in";
        } else if (typeChoice == 2) {
            typeFilter = "Booked";
        }

        control.printQueueSummaryReport(typeFilter);
    }

    private int readInt(String prompt) {
        System.out.print(prompt);
        while (!sc.hasNextInt()) {
            System.out.print("Please enter a number: ");
            sc.next();
        }
        int value = sc.nextInt();
        sc.nextLine(); // consume newline
        return value;
    }
}
