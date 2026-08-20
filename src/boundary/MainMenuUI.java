/*
 * Module: System Navigation & Subsystem Orchestration (Boundary UI Component)
 * Author: TARUMT Resorts Development Team
 * 
 * Description:
 * Boundary class for displaying the main menu, managing top-level navigation,
 * and initializing shared entity collections across all 4 integrated subsystems (ECB pattern).
 */
package boundary;

import adt.Dictionary;
import adt.DoublyLinkedList;
import adt.DoublyLinkedListInterface;
import adt.HashTable;
import control.FrontDeskServiceControl;
import control.HousekeepingControl;
import control.PriorityAllocationControl;
import control.WalkInRegistrationControl;
import entity.Guest;
import entity.Member;
import entity.Room;
import java.util.Scanner;

public class MainMenuUI {

    private final Scanner scanner;
    private final DoublyLinkedListInterface<Room> roomList;
    private final Dictionary<String, Guest> guestTable;
    private final Dictionary<String, Room> roomTable;
    private final WalkInRegistrationControl walkInControl;
    private final PriorityAllocationControl priorityControl;
    private final HousekeepingControl housekeepingControl;
    private final FrontDeskServiceControl frontDeskControl;

    public MainMenuUI() {
        this.scanner = new Scanner(System.in);
        this.roomList = new DoublyLinkedList<>();
        this.guestTable = new HashTable<>();
        this.roomTable = new HashTable<>();

        this.walkInControl = new WalkInRegistrationControl(roomList);
        this.priorityControl = new PriorityAllocationControl(roomList);
        this.housekeepingControl = new HousekeepingControl(roomList);
        this.frontDeskControl = new FrontDeskServiceControl(guestTable, roomTable);
        
        seedRoomsAndGuests();
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
        new FrontDeskServiceUI(frontDeskControl, scanner).run();
    }

    /**
     * Initial resort room inventory and sample guests shared across subsystems.
     */
    private void seedRoomsAndGuests() {
        Room r101 = new Room("101", "Ready for Check-In", true);
        Room r102 = new Room("102", "Ready for Check-In", true);
        Room r103 = new Room("103", "Dirty", true);
        Room r104 = new Room("104", "Cleaning In Progress", false);
        Room r105 = new Room("105", "Ready for Check-In", true);
        Room r201 = new Room("201", "Ready for Check-In", true);
        Room r202 = new Room("202", "Inspected", true);

        // Add to Linear ADT room list
        roomList.insertLast(r101);
        roomList.insertLast(r102);
        roomList.insertLast(r103);
        roomList.insertLast(r104);
        roomList.insertLast(r105);
        roomList.insertLast(r201);
        roomList.insertLast(r202);

        // Add to Front-Desk Hash Table room dictionary for fast lookup
        roomTable.add(r101.getRoomNumber(), r101);
        roomTable.add(r102.getRoomNumber(), r102);
        roomTable.add(r103.getRoomNumber(), r103);
        roomTable.add(r104.getRoomNumber(), r104);
        roomTable.add(r105.getRoomNumber(), r105);
        roomTable.add(r201.getRoomNumber(), r201);
        roomTable.add(r202.getRoomNumber(), r202);

        // Seed sample guests into Front-Desk Hash Table guest dictionary
        Guest g1 = new Guest("10001001", "Alice Tan", true, "Booked", r101, "Credit Card - Paid", new Member("M101", "GOLD", 500));
        Guest g2 = new Guest("10001002", "Dato Steven", true, "Booked", r102, "Corporate Billing", new Member("M102", "DIAMOND", 2500));
        Guest g3 = new Guest("10001003", "Bob Lee", false, "Walk-in", null, "Cash - Pending", null);
        Guest g4 = new Guest("10001004", "Dr. Clara", false, "Booked", null, "Direct Transfer", new Member("M104", "ELITE", 1800));

        
        frontDeskControl.addGuest(g1);
        frontDeskControl.addGuest(g2);
        frontDeskControl.addGuest(g3);
        frontDeskControl.addGuest(g4);
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
