/**
 * Module: Front-Desk Service (Boundary UI Component)
 * Author: MELAINE YANG MEI
 * 
 * Description:
 * Boundary class handling console user interaction for Front-Desk Service inquiries,
 * room/billing lookups using 8-digit confirmation numbers, and management reporting.
 */

package boundary;
import adt.DoublyLinkedList;
import control.FrontDeskServiceControl;
import entity.Guest;
import entity.Room;
import java.util.Scanner;

public class FrontDeskServiceUI {
        private FrontDeskServiceControl controller;
        private Scanner scanner;
        
        public FrontDeskServiceUI(FrontDeskServiceControl controller, Scanner scanner){
            this.controller = controller;
            this.scanner = scanner;
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
            String confirmationNumber = scanner.nextLine().trim();
            
            Guest guest = controller.findGuest(confirmationNumber);
            if(guest == null){
                System.out.println("Guest not found!");
                return;
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
                    (guest.getCheckInStatus() ? "Checked-In" : (Guest.STATUS_CHECKED_OUT.equals(guest.getStatus()) ? "Checked-Out" : "Pending")));
            System.out.println("Type: " + guest.getType());
            System.out.println("Stay Duration: " + guest.getStayDays() + " night(s)");
            System.out.println("Preferred Room Type: " + (guest.getPreferredRoomType() == null ? "Any" : guest.getPreferredRoomType()));
            
            String roomNumber = guest.getAssignedRoom() != null 
                    ? guest.getAssignedRoom().getRoomNumber() + " (" + guest.getAssignedRoom().getRoomType() + ")" : "Unassigned";
            
            System.out.println("Assigned Room: " + roomNumber);
            
            if(guest.getMemberProfile() != null){
                System.out.println("Member ID: " + guest.getMemberProfile().getMemberID());
                System.out.println("Member Tier: " + guest.getMemberProfile().getTierType());
                System.out.println("Loyalty Points: " + guest.getMemberProfile().getPoints());
            } else {
                System.out.println("Member Status: Non-Member");
            }
            
            System.out.println("Billing Details: " + guest.getBillingDetails());
            if (guest.isPointsRedeemed()) {
                System.out.println("Point Redemption: 2-Day Free Stay Applied (-" + guest.getRedeemedPoints() + " pts)");
            }
            System.out.println("----------------------------------------------------");
        }
        
        public void checkBilling(){
            System.out.print("Enter confirmation number: ");
            String confirmationNumber = scanner.nextLine().trim();
            
            Guest guest = controller.findGuest(confirmationNumber);
            if(guest == null){
                System.out.println("Guest with confirmation number \"" + confirmationNumber + "\" was not found!");
                return;
            }

            FrontDeskServiceControl.BillingBreakdown bill = controller.calculateBilling(confirmationNumber);
            String roomNumber = guest.getAssignedRoom() != null 
                    ? guest.getAssignedRoom().getRoomNumber() + " (" + guest.getAssignedRoom().getRoomType() + ")" : "Unassigned";
            String roomType = (guest.getAssignedRoom() != null)
                    ? guest.getAssignedRoom().getRoomType()
                    : (guest.getPreferredRoomType() != null ? guest.getPreferredRoomType() : "Single");
            
            System.out.println("\n===================================================");
            System.out.println("           QUERY BILLING DETAILS");
            System.out.println("=====================================================");
            System.out.println("Confirmation No: " + guest.getConfirmationNumber());
            System.out.println("Guest Name:      " + guest.getName());
            System.out.println("Room:            " + roomNumber);
            System.out.println("Room Type Rate:  RM " + String.format("%.2f", bill.nightlyRate) + " / night (" + roomType + ")");
            System.out.println("Stay Duration:   " + bill.stayDays + " night(s)");
            System.out.println("Billing Note:    " + guest.getBillingDetails());
            System.out.println("------------------------------------------------------");
            
            System.out.printf("%-28s RM %8.2f%n", "Base Room Charges:", bill.baseCharge);
            if (bill.pointDiscount > 0) {
                System.out.printf("%-28s -RM %7.2f (2-Day Free Stay)%n", "Point Redemption Credit:", bill.pointDiscount);
            }
            System.out.printf("%-28s RM %8.2f%n", "Subtotal:", bill.subtotal);
            System.out.printf("%-28s RM %8.2f%n", "Tax (6% SST):", bill.tax);
            
            System.out.println("------------------------------------------------------");
            System.out.printf("%-28s RM %8.2f%n", "Grand Total:", bill.total);
            System.out.println("======================================================");
        }
        
        public void checkRoom(){
            System.out.print("Enter room number: ");
            String roomNumber = scanner.nextLine().trim();
            
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
            
            String availability = room.isAvailable() ? "Available" : "Not Available";
            System.out.println("\n=================================================");
            System.out.println("           ROOM INFORMATION");
            System.out.println("===================================================");
            System.out.println("Room Number:     " + room.getRoomNumber());
            System.out.println("Room Type:       " + room.getRoomType() + " (RM " + String.format("%.2f", FrontDeskServiceControl.getRoomRate(room.getRoomType())) + "/night)");
            System.out.println("Cleaning Status: " + room.getCleaningStatus());
            System.out.println("Availability:    " + availability);
            System.out.println("===================================================");
        }
            
        public void frontDeskReport1(){
            DoublyLinkedList<Guest> guests = controller.getGuestList();
            System.out.println("\n=========================================================================================");
            System.out.println("                              GUEST BILLING SUMMARY REPORT");
            System.out.println("=========================================================================================");           
            System.out.printf("%-10s %-16s %-6s %-12s %-15s %-25s%n", "Conf. No", "Guest Name", "Stay", "Room", "Tier (Pts)", "Billing Details");
            System.out.println("-----------------------------------------------------------------------------------------");
            
            for (int i = 0; i < guests.getNumberOfEntries(); i++){
                Guest guest = guests.getEntry(i);
                
                String roomNumber = (guest.getAssignedRoom()!= null)
                        ? guest.getAssignedRoom().getRoomNumber() + " (" + guest.getAssignedRoom().getRoomType() + ")"
                        : "Unassigned";
                String tier = (guest.getMemberProfile() != null)
                        ? guest.getMemberProfile().getTierType() + " (" + guest.getMemberProfile().getPoints() + ")"
                        : "Non-Member";
                
                System.out.printf("%-10s %-16s %2d nts %-12s %-15s %-25s%n", 
                        guest.getConfirmationNumber(), 
                        guest.getName(), 
                        guest.getStayDays(),
                        roomNumber, 
                        tier,
                        guest.getBillingDetails());
            }
            System.out.println("=========================================================================================");
        }
        
        public void frontDeskReport2(){
            DoublyLinkedList<Guest> guests = controller.getGuestList();
            System.out.println("\n=========================================================================================");
            System.out.println("                                GUEST OCCUPANCY REPORT");
            System.out.println("=========================================================================================");
            System.out.printf("%-10s %-16s %-8s %-6s %-12s %-12s %-15s%n", 
                    "Conf. No", "Guest Name", "Type", "Stay", "Room", "Status", "Member Tier");
            System.out.println("-----------------------------------------------------------------------------------------");           
            
            for (int i = 0; i < guests.getNumberOfEntries(); i++){
                Guest guest = guests.getEntry(i);
                
                String roomNumber = guest.getAssignedRoom() != null 
                        ? guest.getAssignedRoom().getRoomNumber() + " (" + guest.getAssignedRoom().getRoomType() + ")"
                        : "Unassigned";
                
                String status = guest.getCheckInStatus() 
                        ? "Checked-In" 
                        : (Guest.STATUS_CHECKED_OUT.equals(guest.getStatus()) ? "Checked-Out" : "Pending");
                String tier = (guest.getMemberProfile() != null)
                        ? guest.getMemberProfile().getTierType()
                        : "Non-Member";
                           
                System.out.printf("%-10s %-16s %-8s %2d nts %-12s %-12s %-15s%n",
                        guest.getConfirmationNumber(),
                        guest.getName(),
                        guest.getType(),
                        guest.getStayDays(),
                        roomNumber,
                        status,
                        tier);         
            }
            
            int totalGuest = 0;
            int checkInGuests = 0;
            int pendingGuests = 0;
            int occupiedRooms = 0;
            
            for (int i = 0; i < guests.getNumberOfEntries(); i++){
                Guest guest = guests.getEntry(i);        
                totalGuest++;
                
                if(guest.getCheckInStatus()){
                    checkInGuests++;
                } else if (Guest.STATUS_PENDING.equals(guest.getStatus())) {
                    pendingGuests++;
                }
                
                if(guest.getAssignedRoom()!= null){
                    occupiedRooms++;
                }              
            }
            
            double occupancyRate = totalGuest > 0
                        ? (double) checkInGuests / totalGuest * 100
                        : 0;
            System.out.println("-----------------------------------------------------------------------------------------");
            System.out.println("Total Guests: " + totalGuest);
            System.out.println("Checked In Guests: " + checkInGuests);
            System.out.println("Pending Guests: " + pendingGuests);
            System.out.println("Occupied Rooms: " + occupiedRooms);
            System.out.printf("Occupancy Rate: %.2f%%%n", occupancyRate);
            System.out.println("=========================================================================================");
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
