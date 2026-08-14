/*
 * Module: Front-Desk Service (Boundary UI Component)
 * Author: MELAINE YANG MEI
 * 
 * Description:
 * Boundary class handling console user interaction for Front-Desk Service inquiries,
 * room/billing lookups using 8-digit confirmation numbers, and management reporting.
 */
package boundary;

import control.FrontDeskServiceControl;
import entity.Guest;
import entity.Room;
import java.util.Scanner;

public class FrontDeskServiceUI {

    private FrontDeskServiceControl controller;
    private Scanner scanner;

    public FrontDeskServiceUI(FrontDeskServiceControl controller, Scanner scanner) {
        this.controller = controller;
        this.scanner = scanner;
    }

    public void run() {
        int choice;
        do {
            System.out.println("\n==================================================");
            System.out.println("              FRONT DESK SERVICE                  ");
            System.out.println("==================================================");
            System.out.println("1. Search Guest Information (8-digit Confirmation)");
            System.out.println("2. Search Room Availability");
            System.out.println("3. Query Billing Details");
            System.out.println("4. Generate Room Status Report");
            System.out.println("5. Generate Guest Occupancy Report");
            System.out.println("0. Back to Main Menu");
            System.out.println("--------------------------------------------------");
            System.out.print("Please select an option: ");

            choice = readInteger();

            switch (choice) {
                case 1:
                    searchGuest();
                    break;
                case 2:
                    checkRoom();
                    break;
                case 3:
                    checkBilling();
                    break;
                case 4:
                    frontDeskReport1();
                    break;
                case 5:
                    frontDeskReport2();
                    break;
                case 0:
                    System.out.println("Returning to Main Menu...");
                    break;
                default:
                    System.out.println("Invalid choice! Please enter a number between 0 and 5.");
                    break;
            }
        } while (choice != 0);
    }
        
        public void searchGuest(){
            System.out.print("Enter 8-digit confirmation number: ");
            String confirmationNumber = scanner.nextLine();
            
            Guest guest = controller.findGuest(confirmationNumber);
            if(guest == null){
                System.out.println("Guest not found!");
                return;
            }
            
            displayGuest(guest);          
        }
        
        private void displayGuest(Guest guest){
            System.out.println("~~~~~~~~Guest Information~~~~~~~~\n");
            System.out.println("Confirmation Number: " + 
                    guest.getConfirmationNumber());
            System.out.println("Name: " +
                    guest.getName());
            System.out.println("Check-in status: " +
                    guest.getCheckInStatus());
            System.out.println("Type: " +
                    guest.getType());
            System.out.println("Assigned Room:" +
                    guest.getAssignedRoom());
            System.out.println("Billing Details: "+
                    guest.getBillingDetails());
        }
        
        public void checkBilling(){
            System.out.print("Enter confirmation number:");
            String confirmationNumber = scanner.nextLine();
            
            String billing = controller.getBillingDetails(confirmationNumber);
            System.out.println("Billing Details:" + billing);
        }
        
        public void checkRoom(){
            System.out.print("Enter room number:");
            String roomNumber = scanner.nextLine();
            
            Room room = controller.findRoom(roomNumber);
            
            if(room == null){
                System.out.println("Room not found");
                return; 
            }
                System.out.println("Room:" + room.getRoomNumber());
                System.out.println("Cleaning Status:" + room.getCleaningStatus());
                System.out.println("Available:" + room.isAvailable());
        }
        
        
        public void frontDeskReport1(){
            Object[] rooms = controller.getRooms();
            System.out.println("\n~~~~~~~~Room Status Report~~~~~~~~");
            for (Object obj : rooms){
                Room room = (Room) obj;
                System.out.println(room);
            }
            System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~");
        }
        
        public void frontDeskReport2(){
            Object[] guests = controller.getGuests();
            System.out.println("\n~~~~~~~~Guest Occupancy Report~~~~~~~~");
            for(Object obj : guests){
                Guest guest = (Guest) obj;
                
                System.out.println("Confirmation Number: " 
                        + guest.getConfirmationNumber() + "| Name:" 
                        + guest.getName() + "| Room: "
                        + guest.getAssignedRoom() + "| Status" 
                        + guest.getCheckInStatus());
            }
            System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~");
        }
        
        public int readInteger(){
            while(!scanner.hasNextInt()){
                System.out.println("Invalid input! Please enter a number");
                scanner.next();
            }
            return scanner.nextInt();
        }
}
