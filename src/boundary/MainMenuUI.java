package boundary;

import java.util.Scanner;

/**
 * Boundary class for displaying the main menu and handling navigation.
 * 
 * @author Wei Xin
 */

public class MainMenuUI {

    private final Scanner scanner;

    public MainMenuUI() {
        this.scanner = new Scanner(System.in);
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
                    System.out.println("Invalid choice! Please enter a number between 0 and 5.");
            }
        } while (choice != 0);
    }

    private void displayWalkInMenu() {
        System.out.println("\n--- [Module 1] Walk-In & Standard Booking ---");
        System.out.println("1. Report 1");
        System.out.println("2. Report 2");
        System.out.println("0. Back to Main Menu");
        System.out.print("Enter choice: ");
        int choice = getIntInput();
        
        if (choice != 0) {
            System.out.println(">> Feature selected. (Control logic integration goes here)");
        }
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
        System.out.println("\n--- [Module 3] Housekeeping & Task Log ---");
        System.out.println("1. Report 1");
        System.out.println("2. Report 2");
        System.out.println("0. Back to Main Menu");
        System.out.print("Enter choice: ");
        int choice = getIntInput();

        if (choice != 0) {
            System.out.println(">> Feature selected. (Control logic integration goes here)");
        }
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

    private int getIntInput() {
        while (!scanner.hasNextInt()) {
            System.out.print("Invalid input! Enter a valid number: ");
            scanner.next();
        }
        return scanner.nextInt();
    }
}