/*
 * Module: VIP & Loyalty Tier Priority Room Allocation (Boundary UI Component)
 * Author: WEI XIN
 * 
 * Description:
 * Boundary class handling UI interaction for Module 2: VIP & Loyalty Tier Priority Room Allocation.
 * Strictly adheres to ECB constraints (communicates only with Actor and PriorityAllocationControl).
 */
package boundary;

import adt.DoublyLinkedListInterface;
import control.PriorityAllocationControl;
import entity.Guest;
import entity.Room;
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
        if (conf.isEmpty()) {
            System.out.println(">> Validation Error: Confirmation number cannot be empty.");
            return;
        }
        if (control.isConfirmationNumberRegistered(conf)) {
            System.out.println(">> Validation Error: Confirmation number \"" + conf + "\" is already registered.");
            return;
        }

        System.out.print("Enter Guest Name: ");
        String name = scanner.nextLine().trim();
        if (name.isEmpty()) {
            System.out.println(">> Validation Error: Guest name cannot be empty.");
            return;
        }

        System.out.print("Enter Member ID: ");
        String memberId = scanner.nextLine().trim();
        if (memberId.isEmpty()) {
            System.out.println(">> Validation Error: Member ID cannot be empty.");
            return;
        }
        if (control.isMemberIdRegistered(memberId)) {
            System.out.println(">> Validation Error: Member ID \"" + memberId + "\" is already registered.");
            return;
        }

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

        System.out.print("Enter stay duration (in nights/days): ");
        int stayDays = getIntInput();
        if (stayDays < 1) {
            stayDays = 1;
        }

        String preferredType = promptRoomType();

        int redeemCost = Guest.getRedemptionCostForRoomType(preferredType);
        boolean redeemPoints = false;
        if (points >= redeemCost) {
            System.out.printf("You have %d loyalty points! Redeem a 2-Day Free Stay (%s: %d pts)? (1: Yes | 2: No): ",
                    points, (preferredType == null ? "Single" : preferredType), redeemCost);
            int redeemChoice = getIntInput();
            redeemPoints = (redeemChoice == 1);
        }

        System.out.print("Enter Billing Details (e.g., Credit Card / Corporate): ");
        String billing = scanner.nextLine().trim();

        try {
            Guest registered = control.registerVIPGuest(conf, name, memberId, tier, points, billing, preferredType, stayDays, redeemPoints);
            if (registered != null) {
                System.out.println("\n==================================================");
                System.out.println("         VIP RESERVATION ENQUEUED SUCCESSFUL      ");
                System.out.println("==================================================");
                System.out.println(registered.toDetailedCard());
                System.out.println("Current VIP Waitlist Size: " + control.getQueueSize());
                System.out.println("==================================================");

                if (stayDays > 14) {
                    System.out.println("\n" + registered.applyLongStayPromotion());
                }
            } else {
                System.out.println(">> Failed to add VIP guest.");
            }
        } catch (IllegalArgumentException ex) {
            System.out.println(">> Validation Error: " + ex.getMessage());
        }
    }

    private String promptRoomType() {
        System.out.println("Preferred Room Type:");
        System.out.println("0. Any");
        System.out.println("1. Single");
        System.out.println("2. Double");
        System.out.println("3. Deluxe");
        System.out.println("4. Suite");
        int choice = getIntInput();
        switch (choice) {
            case 1: return "Single";
            case 2: return "Double";
            case 3: return "Deluxe";
            case 4: return "Suite";
            default: return null;
        }
    }

    private void autoAllocateRoom() {
        if (control.getQueueSize() == 0) {
            System.out.println("\n>> Priority queue is empty. No VIP guests awaiting room allocation.");
            return;
        }

        Guest topGuest = control.peekNextVIP();
        System.out.println("\nTop of Priority Queue: " + topGuest.getName()
                + " [" + topGuest.getMemberProfile().getTierType() + " | "
                + topGuest.getMemberProfile().getPoints() + " pts | Pref: "
                + (topGuest.getPreferredRoomType() == null ? "Any" : topGuest.getPreferredRoomType()) + "]");

        Guest allocated = control.allocateFirstAvailableRoom();
        if (allocated != null) {
            if (allocated.equals(topGuest)) {
                System.out.println(">> Success! Top-priority VIP allocated:");
            } else {
                System.out.println(">> Top-priority guest's preferred room type isn't ready yet - skipped to:");
            }
            System.out.println("   Guest : " + allocated.getName());
            System.out.println("   Tier  : " + allocated.getMemberProfile().getTierType());
            System.out.println("   Room  : " + allocated.getAssignedRoom().getRoomNumber()
                    + " (" + allocated.getAssignedRoom().getRoomType() + ", "
                    + allocated.getAssignedRoom().getCleaningStatus() + ")");
        } else {
            System.out.println(">> Allocation failed: No 'Ready for Check-In' room matches any waiting VIP's preferred type.");
            System.out.println("   All VIP guests remain in the priority waitlist.");
        }
    }

    private void allocateSpecificRoom() {
        if (control.getQueueSize() == 0) {
            System.out.println("\n>> Priority queue is empty. No VIP guests awaiting room allocation.");
            return;
        }

        Guest nextInLine = control.peekNextVIP();
        String pref = nextInLine.getPreferredRoomType();
        String displayPref = (pref == null) ? "Any" : pref;

        System.out.println("\nNext VIP in line for room allocation:");
        System.out.println("   Guest Name     : " + nextInLine.getName());
        System.out.println("   Loyalty Tier   : " + nextInLine.getMemberProfile().getTierType()
                + " (" + nextInLine.getMemberProfile().getPoints() + " pts)");
        System.out.println("   Preferred Type : " + displayPref);
        System.out.println("   Stay Duration  : " + nextInLine.getStayDays() + " night(s)");

        // 1. SMART FIX: Automatically fetch rooms matching the guest's preference!
        DoublyLinkedListInterface<Room> availableRooms = control.getAvailableCleanRoomsByType(pref);

        // 2. Fallback: If their preferred type isn't available, ask staff if they want to override
        if (availableRooms.isEmpty()) {
            System.out.println("\n>> No available 'Ready for Check-In' rooms found for preferred type: " + displayPref);
            System.out.println("   Would you like to search for a different room type? (1: Yes | 2: No, keep in waitlist)");
            int choice = getIntInput();
            if (choice == 1) {
                System.out.println("\nSelect Alternative Room Type to Allocate:");
                pref = promptRoomType();
                availableRooms = control.getAvailableCleanRoomsByType(pref);
                if (availableRooms.isEmpty()) {
                    System.out.println("\n>> Still no rooms available for that type. Guest remains in waitlist.");
                    return;
                }
            } else {
                return;
            }
        }

        System.out.println("\n--------------------------------------------------");
        System.out.println(" Available " + (pref == null ? "All" : pref) + " Rooms (Ready for Check-In):");
        System.out.println("--------------------------------------------------");
        for (int i = 0; i < availableRooms.getNumberOfEntries(); i++) {
            Room r = availableRooms.getEntry(i);
            // UX FIX: Removed the "1.", "2." numbering so staff doesn't mistakenly type the list index
            System.out.printf(" Room %-5s | Type: %-8s | Status: %s%n",
                    r.getRoomNumber(), r.getRoomType(), r.getCleaningStatus());
        }
        System.out.println("--------------------------------------------------");

        // UX FIX: Give a literal example in the prompt so the user knows exactly what to type
        System.out.print("Enter exact Target Room Number (e.g., " + availableRooms.getEntry(0).getRoomNumber() + "): ");
        String roomNumber = scanner.nextLine().trim();

        Guest allocated = control.allocateSpecificRoom(roomNumber);
        if (allocated != null) {
            System.out.println("\n==================================================");
            System.out.println("        VIP ROOM ALLOCATION SUCCESSFUL            ");
            System.out.println("==================================================");
            System.out.println("Guest Name     : " + allocated.getName());
            System.out.println("Member Tier    : " + allocated.getMemberProfile().getTierType());
            System.out.println("Assigned Room  : " + allocated.getAssignedRoom().getRoomNumber()
                    + " (" + allocated.getAssignedRoom().getRoomType() + ")");
            System.out.println("Stay Duration  : " + allocated.getStayDays() + " night(s)");
            System.out.println("Status         : " + allocated.getStatus());
            System.out.println("Billing Note   : " + allocated.getBillingDetails());
            System.out.println("==================================================");
        } else {
            System.out.println("\n>> Allocation failed: Room " + roomNumber + " is either invalid, occupied, or not 'Ready for Check-In'.");
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