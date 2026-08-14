package boundary;

import control.PriorityAllocationControl;
import entity.Guest;
import entity.Member;
import entity.Room;
import java.util.Scanner;

/**
 * Boundary class handling UI interaction for Module 2 and Report outputs.
 * 
 * @author Wei Xin
 */

public class PriorityAllocationUI {

    private final PriorityAllocationControl control;
    private final Scanner scanner;

    public PriorityAllocationUI() {
        this.control = new PriorityAllocationControl();
        this.scanner = new Scanner(System.in);
    }

    public void displayPriorityAllocationMenu() {
        int choice;
        do {
            System.out.println("\n------------------------------------------------");
            System.out.println("  MODULE 2: VIP & LOYALTY TIER PRIORITY ALLOCATION");
            System.out.println("------------------------------------------------");
            System.out.println("1. Add Priority VIP Reservation");
            System.out.println("2. Allocate Available Room to Highest VIP");
            System.out.println("3. View Pending Priority Queue");
            System.out.println("4. Generate Report 1: VIP Tier Demand Summary");
            System.out.println("5. Generate Report 2: Priority Waitlist Audit");
            System.out.println("0. Back to Main Menu");
            System.out.print("Enter choice: ");

            choice = getIntInput();

            switch (choice) {
                case 1:
                    addVIPReservation();
                    break;
                case 2:
                    allocateRoom();
                    break;
                case 3:
                    System.out.println("\n--- Current Pending VIP Queue ---");
                    System.out.println(control.getPendingPriorityQueueString());
                    break;
                case 4:
                    System.out.println(control.generateTierDistributionReport());
                    break;
                case 5:
                    System.out.println(control.generatePriorityWaitlistReport());
                    break;
                case 0:
                    break;
                default:
                    System.out.println("Invalid option. Please try again.");
            }
        } while (choice != 0);
    }

    private void addVIPReservation() {
        System.out.print("Enter 8-digit Confirmation Number: ");
        String conf = scanner.next();
        scanner.nextLine(); // clear buffer

        System.out.print("Enter Guest Name: ");
        String name = scanner.nextLine();

        System.out.print("Enter Member ID: ");
        String memberId = scanner.next();

        System.out.print("Select Tier (1. Diamond | 2. Platinum | 3. Elite): ");
        int tierChoice = getIntInput();
        String tier = "Gold";
        if (tierChoice == 1) tier = "Diamond";
        else if (tierChoice == 2) tier = "Platinum";

        Member member = new Member(memberId, tier, 1000);
        Guest newGuest = new Guest(conf, name, false, "Booked", null, "Paid", member);

        if (control.addPriorityGuest(newGuest)) {
            System.out.println(">> Guest successfully added to priority queue with auto-reordering!");
        } else {
            System.out.println(">> Failed to add guest.");
        }
    }

    private void allocateRoom() {
        System.out.print("Enter Available Room Number to Allocate: ");
        String roomNum = scanner.next();

        Room room = new Room(roomNum, "Ready for Check-In", true);
        Guest allocatedGuest = control.allocateRoomToNextGuest(room);

        if (allocatedGuest != null) {
            System.out.println("\n>> Success! Room allocated to top priority guest:");
            System.out.println("   Guest: " + allocatedGuest.getName() + " | Tier: " + allocatedGuest.getMemberProfile().getTierType());
        } else {
            System.out.println(">> Allocation failed. Priority queue is empty or room is unavailable.");
        }
    }

    private int getIntInput() {
        while (!scanner.hasNextInt()) {
            System.out.print("Invalid input! Enter a valid integer: ");
            scanner.next();
        }
        return scanner.nextInt();
    }
}