/**
 * Module: Front-Desk Service (Boundary UI Component)
 * Author: MELAINE YANG MEI
 * 
 * Description:
 * Boundary class handling console user interaction for Front-Desk Service inquiries,
 * room/billing lookups using 8-digit confirmation numbers, and management reporting.
 */

package boundary;
import entity.Guest;
import entity.Room;
import entity.Member;
import adt.DoublyLinkedList;
import control.FrontDeskServiceControl;
import java.util.Scanner;

public class FrontDeskServiceUI {
        private FrontDeskServiceControl controller;
        private Scanner scanner;
        
        public FrontDeskServiceUI(FrontDeskServiceControl controller, Scanner scanner){
            this.controller = controller;
            this.scanner = new Scanner(System.in);     
    }
        
        public void run(){
            int choice;
            do{
                System.out.println("\n==================================================");
            System.out.println("              FRONT DESK SERVICE                  ");
            System.out.println("==================================================");
            System.out.println("1. Search Guest Information (8-digit Confirmation)");
            System.out.println("2. Search Room Availability");
            System.out.println("3. Query Billing Details");
            System.out.println("4. Generate Guest Billing Summary Report");
            System.out.println("5. Generate Guest Occupancy Report");
            System.out.println("0. Back to Main Menu");
            System.out.println("--------------------------------------------------");
            System.out.print("Please select an option: ");
                
                choice = readInteger();
                
                switch(choice){
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
                        System.out.println("Okay bye");
                        break;
                    default:
                        System.out.println("Invalid choice! :(");
                }
            }while(choice != 0);
            
        }
        
        public void searchGuest(){
            System.out.print("Enter 8-digit confirmation number: ");
            String confirmationNumber = scanner.nextLine();
            
            Guest guest = controller.findGuest(confirmationNumber);
            if(guest == null){
                System.out.println("Guest not found!");
                return;
            }
            String roomNumber;
                
                if(guest.getAssignedRoom()!= null){
                    roomNumber = guest.getAssignedRoom().getRoomNumber();
                }else{
                    roomNumber = "Unassigned";
                }
            
            displayGuest(guest);          
        }
        
        private void displayGuest(Guest guest){
            System.out.println("\n==================================================");
            System.out.println("            GUEST INFORMATION");
            System.out.println("====================================================");
            
            System.out.println("Confirmation Number: " + guest.getConfirmationNumber());
            System.out.println("Name: " + guest.getName());
            System.out.println("Check-in status: " + 
                    (guest.getCheckInStatus() ? "Checked-In" : "Pending"));
            System.out.println("Type: " + guest.getType());
            
            String roomNumber = guest.getAssignedRoom() != null 
                    ? guest.getAssignedRoom().getRoomNumber() : "Unassigned";
            
            System.out.println("Assigned Room: " + roomNumber);
            
            if(guest.getMemberProfile() != null){
                System.out.println("Member ID: " + guest.getMemberProfile().getMemberID());
                System.out.println("Member Tier: " + guest.getMemberProfile().getTierType());
            }else{
                System.out.println("Member Status: Non-Member");
            }
            
            System.out.println("----------------------------------------------------");
        }
        
        public void checkBilling(){
            System.out.print("Enter confirmation number:");
            String confirmationNumber = scanner.nextLine();
            
            Guest guest = controller.findGuest(confirmationNumber);
            double roomCharge = 300.00;
            double tax = roomCharge *0.06;
            double total  = roomCharge + tax;
            
            String roomNumber = guest.getAssignedRoom() != null 
                    ? guest.getAssignedRoom().getRoomNumber() : "Unassigned";
            String billing = controller.getBillingDetails(confirmationNumber);
            
            System.out.println("\n===================================================");
            System.out.println("           QUERY BILLING DETAILS");
            System.out.println("=====================================================");
            System.out.println("Confirmation No: " + guest.getConfirmationNumber());
            System.out.println("Guest Name: " + guest.getName());
            System.out.println("Room Number: " + roomNumber);
            System.out.println("Billing Details: " + billing);
            System.out.println("------------------------------------------------------");
            
            System.out.printf("%-25s RM %8.2f%n", "Room Charges:", roomCharge);
            System.out.printf("%-25s RM %8.2f%n", "Tax (6%):", tax);
            
            System.out.println("------------------------------------------------------");
            System.out.printf("%-25s RM %8.2f%n", "Total:", total);
            System.out.println("======================================================");
            
            
        }
        
        
        public void checkRoom(){
            System.out.print("Enter room number:");
            String roomNumber = scanner.nextLine();
            
            Room room = controller.findRoom(roomNumber);
            
            if(room == null){
                System.out.println("\n=================================================");
                System.out.println("           ROOM NOT FOUND");
                System.out.println("===================================================");
                System.out.println("Room Number: " + roomNumber);
                System.out.println("Please check the room number and try again. :)");
                System.out.println("---------------------------------------------------");
                return; 
            }
            
            String availability = room.isAvailable()
                    ? "Available"
                    : "Not Available";
                System.out.println("\n=================================================");
                System.out.println("           ROOM INFORMATION");
                System.out.println("===================================================");
                System.out.println("Room Number:" + room.getRoomNumber());
                System.out.println("Cleaning Status:" + room.getCleaningStatus());
                System.out.println("Available:" + availability);
                System.out.println("===================================================");
        }
            
        public void frontDeskReport1(){
            DoublyLinkedList<Guest> guests = controller.getGuestList();
            System.out.println("\n==============================================================================");
            System.out.println("           GUEST BILLING SUMMARY REPORT");
            System.out.println("==============================================================================");           
            System.out.printf("%-12s %-20s %-25s %-12s%n", "Conf. No", "Guest Name", "Room", "Billing Details");
            System.out.println("------------------------------------------------------------------------------");
            
            for (int i = 0; i < guests.getNumberOfEntries(); i++){
                Guest guest = guests.getEntry(i);
                
                String roomNumber = (guest.getAssignedRoom()!= null)
                        ? guest.getAssignedRoom().getRoomNumber()
                        : "Unassigned";
                
                System.out.printf("%-12s %-20s %-25s %-12s%n", 
                        guest.getConfirmationNumber(), 
                        guest.getName(), 
                        roomNumber, 
                        guest.getBillingDetails());
            }
            System.out.println("===============================================================================");
        }
        
        public void frontDeskReport2(){
            DoublyLinkedList<Guest> guests = controller.getGuestList();
            System.out.println("\n===================================================================");
            System.out.println("                      GUEST OCCUPANCY REPORT");
            System.out.println("===================================================================");
            System.out.printf("%-12s %-18s %-10s %-12s %-10s%n", 
                    "Conf. No", "Guest Name", "Type", "Room", "Status");
            System.out.println("-------------------------------------------------------------------");           
            
            for (int i = 0; i < guests.getNumberOfEntries(); i++){
                Guest guest = guests.getEntry(i);
                
                String roomNumber = guest.getAssignedRoom() != null 
                        ? guest.getAssignedRoom().getRoomNumber()
                        : "Unassigned";
                
                String status = guest.getCheckInStatus() 
                        ? "Checked In" 
                        : "Pending";
                           
                System.out.printf("%-12s %-18s %-10s %-12s %-10s%n",
                        guest.getConfirmationNumber(),
                        guest.getName(),
                        guest.getType(),
                        roomNumber,
                        status);         
            }
            
            int totalGuest = 0;
            int checkInGuests = 0;
            int pendingGuests = 0;
            int occupiedRooms = 0;
            
            for (int i =0; i < guests.getNumberOfEntries(); i++){
                Guest guest = guests.getEntry(i);        
                totalGuest++;
                
                if(guest.getCheckInStatus()){
                    checkInGuests++;
                }else{
                    pendingGuests++;
                }
                
                if(guest.getAssignedRoom()!= null){
                    occupiedRooms++;
                }              
            }
            
            double occupancyRate = totalGuest > 0
                        ? (double) checkInGuests / totalGuest * 100
                        : 0;
            System.out.println("-------------------------------------------------------------------");
            System.out.println("Total Guests: " + totalGuest);
            System.out.println("Checked In Guests: " + checkInGuests);
            System.out.println("Pending Guests: " + pendingGuests);
            System.out.println("Occupied Rooms: " + occupiedRooms);
            System.out.printf("Occupancy Rate: %.2f%%%n", occupancyRate);
            System.out.println("====================================================================");
        }
        
        public int readInteger(){
            while(!scanner.hasNextInt()){
                System.out.println("Invalid input! Please enter a number");
                scanner.next();
            }
            int choice = scanner.nextInt();
            scanner.nextLine();
            return choice;
        }
}
