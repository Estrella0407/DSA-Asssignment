package boundary;

import adt.ADT;
import adt.LinkedADT;
import control.WalkInRegistrationControl;
import control.HousekeepingControl;
import entity.Room;
import java.util.Scanner;

/**
 * Boundary class for displaying the main menu and handling navigation.
 * 
 * @author Wei Xin
 */

public class MainMenuUI {

    private final Scanner scanner;
    private final ADT<Room> roomList;
    private final WalkInRegistrationControl walkInControl;
    private final HousekeepingControl housekeepingControl;

    public MainMenuUI() {
        this.scanner = new Scanner(System.in);
        this.roomList = new LinkedADT<>();
        seedRooms();
        this.walkInControl = new WalkInRegistrationControl(roomList);
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
                    System.out.println("\nExiting system. Thank you!");
                    break;
                default:
                    System.out.println("Invalid choice! Please enter a number between 0 and 4.");
            }
        } while (choice != 0);
    }

    private void displayWalkInMenu() {
        // [Module 1] delegates straight to the dedicated Walk-In
        // Registrations & Standard Booking boundary/control pair, reusing
        // this menu's Scanner so input isn't split across two Scanners.
        new WalkInRegistrationUI(walkInControl, scanner).run();
    }

    private void displayPriorityAllocationMenu() {
        System.out.println("\n--- [Module 2] VIP & Priority Room Allocation ---");
        System.out.println("1. Report 1");
        System.out.println("2. Report 2");
        System.out.println("0. Back to Main Menu");
        System.out.print("Enter choice: ");
        int choice = getIntInput();

        if (choice != 0) {
            System.out.println(">> Feature selected. (Control logic integration goes here)");
        }
    }

    private void displayHousekeepingMenu() {
        new HousekeepingUI(housekeepingControl, scanner).run();
    }

    private void displayFrontDeskMenu() {
        System.out.println("\n--- [Module 4] Front-Desk Service ---");
        System.out.println("1. Report 1");
        System.out.println("2. Report 2");
        System.out.println("0. Back to Main Menu");
        System.out.print("Enter choice: ");
        int choice = getIntInput();

        if (choice != 0) {
            System.out.println(">> Feature selected. (Control logic integration goes here)");
        }
    }

    /**
     * Temporary sample data so the Walk-In module has rooms to allocate.
     * Replace/extend once the Room Management module owns this list.
     */
    private void seedRooms() {
        roomList.insertLast(new Room("101", "Ready for Check-In", true));
        roomList.insertLast(new Room("102", "Ready for Check-In", true));
        roomList.insertLast(new Room("103", "Dirty", true));
        roomList.insertLast(new Room("104", "Ready for Check-In", false));
        roomList.insertLast(new Room("105", "Ready for Check-In", true));
    }

    private int getIntInput() {
        while (!scanner.hasNextInt()) {
            System.out.print("Invalid input! Enter a valid number: ");
            scanner.next();
        }
        return scanner.nextInt();
    }
}
