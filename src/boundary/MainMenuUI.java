/*
 * Course: BMCS2063 Data Structures and Algorithms
 * Module: System Navigation & Subsystem Orchestration (Boundary UI Component)
 * Author: TARUMT Resorts Development Team
 * 
 * Description:
 * Boundary class for displaying the main menu, managing top-level navigation,
 * and initializing shared entity collections across integrated subsystems (ECB pattern).
 */
package boundary;

import adt.DoublyLinkedList;
import adt.DoublyLinkedListInterface;
import control.HousekeepingControl;
import control.PriorityAllocationControl;
import control.WalkInRegistrationControl;
import entity.Room;
import java.util.Scanner;

public class MainMenuUI {

    private final Scanner scanner;
    private final DoublyLinkedListInterface<Room> roomList;
    private final WalkInRegistrationControl walkInControl;
    private final PriorityAllocationControl priorityControl;
    private final HousekeepingControl housekeepingControl;

    public MainMenuUI() {
        this.scanner = new Scanner(System.in);
        this.roomList = new DoublyLinkedList<>();
        seedRooms();
        this.walkInControl = new WalkInRegistrationControl(roomList);
        this.priorityControl = new PriorityAllocationControl(roomList);
        this.housekeepingControl = new HousekeepingControl(roomList);
    }

    public void displayMainMenu() {
        int choice;
        do {
            System.out.println("\n==================================================");
            System.out.println("            TARUMT RESORTS SYSTEM                 ");
            System.out.println("==================================================");
            System.out.println("1. Walk-In Registrations & Standard Booking (Linear ADT)");
            System.out.println("2. VIP & Loyalty Tier Priority Room Allocation (Non-Linear ADT)");
            System.out.println("3. Housekeeping and Task Log (Linear ADT)");
            System.out.println("4. Front-Desk Service (Non-Linear ADT & Searching)");
            System.out.println("0. Exit");
            System.out.println("--------------------------------------------------");
            System.out.print("Enter choice: ");

            choice = getIntInput();

            switch (choice) {
                case 1:
                    displayWalkInMenu();
                    break;
                case 2:
                    displayPriorityAllocationMenu();
                    break;
                case 3:
                    displayHousekeepingMenu();
                    break;
                case 4:
                    displayFrontDeskMenu();
                    break;
                case 0:
                    System.out.println("\nExiting system. Thank you for using TARUMT Resorts Management System!");
                    break;
                default:
                    System.out.println("Invalid choice! Please enter a number between 0 and 4.");
            }
        } while (choice != 0);
    }

    private void displayWalkInMenu() {
        new WalkInRegistrationUI(walkInControl, scanner).run();
    }

    private void displayPriorityAllocationMenu() {
        new PriorityAllocationUI(priorityControl, scanner).displayPriorityAllocationMenu();
    }

    private void displayHousekeepingMenu() {
        new HousekeepingUI(housekeepingControl, scanner).run();
    }

    private void displayFrontDeskMenu() {
        System.out.println("\n--- [Module 4] Front-Desk Service (Pending Team Member Sync) ---");
        System.out.println("1. Query Guest Information (8-digit Confirmation Search)");
        System.out.println("2. Room Availability & Billing Query");
        System.out.println("0. Back to Main Menu");
        System.out.print("Enter choice: ");
        int choice = getIntInput();

        if (choice != 0) {
            System.out.println(">> Notice: Front-Desk module integration will be synced once your team member provides the sub-system.");
        }
    }

    /**
     * Initial resort room inventory shared across all subsystems.
     */
    private void seedRooms() {
        roomList.insertLast(new Room("101", "Ready for Check-In", true));
        roomList.insertLast(new Room("102", "Ready for Check-In", true));
        roomList.insertLast(new Room("103", "Dirty", true));
        roomList.insertLast(new Room("104", "Cleaning In Progress", false));
        roomList.insertLast(new Room("105", "Ready for Check-In", true));
        roomList.insertLast(new Room("201", "Ready for Check-In", true));
        roomList.insertLast(new Room("202", "Inspected", true));
    }

    private int getIntInput() {
        while (!scanner.hasNextInt()) {
            System.out.print("Invalid input! Enter a valid number: ");
            scanner.next();
        }
        int val = scanner.nextInt();
        scanner.nextLine(); // Clear buffer
        return val;
    }
}
