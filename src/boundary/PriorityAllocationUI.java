/*
 * Module: VIP & Loyalty Tier Priority Room Allocation (Boundary UI Component)
 * Author: WEI XIN
 * 
 * Description:
 * Boundary class handling UI interaction for Module 2: VIP & Loyalty Tier Priority Room Allocation.
 * Strictly adheres to ECB constraints (communicates only with Actor and PriorityAllocationControl).
 */
package boundary;

import control.PriorityAllocationControl;
import entity.Guest;
import java.util.Scanner;

public class PriorityAllocationUI {

    private final PriorityAllocationControl control;
    private final Scanner scanner;

    public PriorityAllocationUI() {
        this(new PriorityAllocationControl(), new Scanner(System.in));
    }

    public PriorityAllocationUI(PriorityAllocationControl control, Scanner scanner) {
        this.control = control;
        this.scanner = scanner;
    }

    public void displayPriorityAllocationMenu() {
        int choice;
        do {
            System.out.println("\n============================================================");
            System.out.println("   MODULE 2: VIP & LOYALTY TIER PRIORITY ROOM ALLOCATION   ");
            System.out.println("============================================================");
            System.out.println("1. Add Priority VIP Reservation");
            System.out.println("2. Auto-Allocate Next Available Room to Highest-Tier VIP");
            System.out.println("3. Allocate Specific Room to Highest-Tier VIP");
            System.out.println("4. View Current Pending VIP Priority Queue");
            System.out.println("5. Generate Report 1: VIP Tier Allocation & Demand Summary");
            System.out.println("6. Generate Report 2: Active VIP Waitlist Real-Time Audit");
            System.out.println("0. Back to Main Menu");
            System.out.println("------------------------------------------------------------");
            System.out.print("Enter choice: ");

            choice = getIntInput();

            switch (choice) {
                case 1:
                    addVIPReservation();
                    break;
                case 2:
                    autoAllocateRoom();
                    break;
                case 3:
                    allocateSpecificRoom();
                    break;
                case 4:
                    viewPendingQueue();
                    break;
                case 5:
                    generateReport1();
                    break;
                case 6:
                    generateReport2();
                    break;
                case 0:
                    System.out.println("Returning to Main Menu...");
                    break;
                default:
                    System.out.println("Invalid option! Please enter a number between 0 and 6.");
            }
        } while (choice != 0);
    }

    private void addVIPReservation() {
        System.out.print("Enter 8-digit Confirmation Number: ");
        String conf = scanner.nextLine().trim();

        System.out.print("Enter Guest Name: ");
        String name = scanner.nextLine().trim();

        System.out.print("Enter Member ID: ");
        String memberId = scanner.nextLine().trim();

        System.out.println("Select Loyalty Tier:");
        System.out.println("1. Diamond");
        System.out.println("2. Platinum");
        System.out.println("3. Elite");
        System.out.println("4. Gold");
        System.out.println("5. Silver");
        System.out.print("Choose tier (1-5): ");
        int tierChoice = getIntInput();
        String tier = parseTier(tierChoice);

        System.out.print("Enter Member Loyalty Points: ");
        int points = getIntInput();

        System.out.print("Enter Billing Details (e.g., Credit Card / Corporate): ");
        String billing = scanner.nextLine().trim();

        try {
            Guest registered = control.registerVIPGuest(conf, name, memberId, tier, points, billing);
            if (registered != null) {
                System.out.println("\n>> Success! VIP Guest enqueued with automatic priority reordering:");
                System.out.println("   " + registered);
                System.out.println("   Current VIP Waitlist Size: " + control.getQueueSize());
            } else {
                System.out.println(">> Failed to add VIP guest.");
            }
        } catch (IllegalArgumentException ex) {
            System.out.println(">> Validation Error: " + ex.getMessage());
        }
    }

    private void autoAllocateRoom() {
        if (control.getQueueSize() == 0) {
            System.out.println("\n>> Priority queue is empty. No VIP guests awaiting room allocation.");
            return;
        }

        Guest nextInLine = control.peekNextVIP();
        System.out.println("\nNext VIP in line for room allocation: " + nextInLine.getName()
                + " [" + nextInLine.getMemberProfile().getTierType() + " | "
                + nextInLine.getMemberProfile().getPoints() + " pts]");

        Guest allocated = control.allocateFirstAvailableRoom();
        if (allocated != null) {
            System.out.println(">> Success! Room assigned and checked in:");
            System.out.println("   Guest : " + allocated.getName());
            System.out.println("   Tier  : " + allocated.getMemberProfile().getTierType());
            System.out.println("   Room  : " + allocated.getAssignedRoom().getRoomNumber()
                    + " (" + allocated.getAssignedRoom().getCleaningStatus() + ")");
        } else {
            System.out.println(">> Allocation failed: No vacant, 'Ready for Check-In' rooms found in inventory.");
            System.out.println("   Guest remains at the top of the priority waitlist.");
        }
    }

    private void allocateSpecificRoom() {
        if (control.getQueueSize() == 0) {
            System.out.println("\n>> Priority queue is empty. No VIP guests awaiting room allocation.");
            return;
        }

        System.out.print("Enter Target Available Room Number: ");
        String roomNumber = scanner.nextLine().trim();

        Guest allocated = control.allocateSpecificRoom(roomNumber);
        if (allocated != null) {
            System.out.println(">> Success! Room " + roomNumber + " allocated to top VIP guest: " + allocated.getName());
        } else {
            System.out.println(">> Allocation failed: Room " + roomNumber + " is either invalid, occupied, or not yet 'Ready for Check-In'.");
        }
    }

    private void viewPendingQueue() {
        System.out.println("\n----------------- CURRENT VIP PRIORITY QUEUE -----------------");
        System.out.println(control.getQueueSnapshot());
    }

    private void generateReport1() {
        System.out.println("\n--- Filter Options for VIP Demand Report ---");
        String tierFilter = promptTierFilter();

        System.out.print("Enter minimum loyalty points filter (0 for ALL): ");
        int minPts = getIntInput();
        Integer pointsFilter = (minPts > 0) ? minPts : null;

        System.out.println(control.generateTierDistributionReport(tierFilter, pointsFilter));
    }

    private void generateReport2() {
        System.out.println("\n--- Filter Options for Active VIP Waitlist Audit ---");
        String tierFilter = promptTierFilter();

        System.out.print("Enter minimum loyalty points filter (0 for ALL): ");
        int minPts = getIntInput();
        Integer pointsFilter = (minPts > 0) ? minPts : null;

        System.out.println(control.generatePriorityWaitlistReport(tierFilter, pointsFilter));
    }

    private String promptTierFilter() {
        System.out.println("Tier filter:");
        System.out.println("0. ALL Tiers");
        System.out.println("1. Diamond");
        System.out.println("2. Platinum");
        System.out.println("3. Elite");
        System.out.println("4. Gold");
        System.out.println("5. Silver");
        System.out.print("Choose option (0-5): ");
        int choice = getIntInput();
        if (choice == 0) {
            return null;
        }
        return parseTier(choice);
    }

    private String parseTier(int choice) {
        switch (choice) {
            case 1:
                return "DIAMOND";
            case 2:
                return "PLATINUM";
            case 3:
                return "ELITE";
            case 4:
                return "GOLD";
            case 5:
                return "SILVER";
            default:
                return "STANDARD";
        }
    }

    private int getIntInput() {
        while (!scanner.hasNextInt()) {
            System.out.print("Invalid input! Please enter a valid number: ");
            scanner.next();
        }
        int val = scanner.nextInt();
        scanner.nextLine(); // Clear newline
        return val;
    }
}
