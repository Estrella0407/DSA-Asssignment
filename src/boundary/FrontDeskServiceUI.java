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
import entity.StayRecord;
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
            System.out.println("4. Transfer Guest Room");
            System.out.println("5. Generate Guest Billing Summary Report");
            System.out.println("6. Generate Guest Occupancy Report");
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
                        transferRoom();
                        break;
                    case 5:
                        frontDeskReport1();
                        break;
                    case 6:
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
            System.out.println("\n====================================================");
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
            
            System.out.println("\n=====================================================");
            System.out.println("           QUERY BILLING DETAILS");
            System.out.println("=====================================================");
            System.out.println("Confirmation No: " + guest.getConfirmationNumber());
            System.out.println("Guest Name:      " + guest.getName());
            System.out.println("Room:            " + roomNumber);
            System.out.println("Room Type Rate:  RM " + String.format("%.2f", bill.nightlyRate) + " / night (" + roomType + ")");
            System.out.println("Stay Duration:   " + bill.stayDays + " night(s)");
            System.out.println("Billing Note:    " + guest.getBillingDetails());
            System.out.println("-----------------------------------------------------");
            
            System.out.printf("%-28s RM %8.2f%n", "Base Room Charges:", bill.baseCharge);
            if (bill.pointDiscount > 0) {
                System.out.printf("%-28s -RM %7.2f (2-Day Free Stay)%n", "Point Redemption Credit:", bill.pointDiscount);
            }
            System.out.printf("%-28s RM %8.2f%n", "Subtotal:", bill.subtotal);
            System.out.printf("%-28s RM %8.2f%n", "Tax (6% SST):", bill.tax);
            
            System.out.println("-----------------------------------------------------");
            System.out.printf("%-28s RM %8.2f%n", "Grand Total:", bill.total);
            System.out.println("=====================================================");
        }
        
        public void checkRoom(){
            System.out.print("Enter room number (full or partial): ");
            String roomNumber = scanner.nextLine().trim();

            // Exact match first - keep the detailed single-room view.
            Room room = controller.findRoom(roomNumber);
            if(room != null){
                displayRoom(room);
                return;
            }

            // No exact match: fall back to a prefix search so "10" lists 101-105.
            DoublyLinkedList<Room> matches = controller.findRoomsByPrefix(roomNumber);
            if(matches.getNumberOfEntries() == 0){
                System.out.println("\n=================================================");
                System.out.println("                ROOM NOT FOUND");
                System.out.println("===================================================");
                System.out.println("Room Number: " + roomNumber);
                System.out.println("Please check the room number and try again. :)");
                System.out.println("---------------------------------------------------");
                return;
            }

            if(matches.getNumberOfEntries() == 1){
                displayRoom(matches.getEntry(0));
                return;
            }

            System.out.println("\n===========================================================================");
            System.out.println("                        ROOMS MATCHING \"" + roomNumber + "\"");
            System.out.println("===========================================================================");
            System.out.printf("%-8s %-10s %-24s %-14s %-12s%n",
                    "Room", "Type", "Cleaning Status", "Availability", "Rate/night");
            System.out.println("---------------------------------------------------------------------------");
            for(int i = 0; i < matches.getNumberOfEntries(); i++){
                Room r = matches.getEntry(i);
                System.out.printf("%-8s %-10s %-24s %-14s RM %8.2f%n",
                        r.getRoomNumber(),
                        r.getRoomType(),
                        r.getCleaningStatus(),
                        r.isAvailable() ? "Available" : "Not Available",
                        FrontDeskServiceControl.getRoomRate(r.getRoomType()));
            }
            System.out.println("---------------------------------------------------------------------------");
            System.out.println("Total: " + matches.getNumberOfEntries() + " room(s) matched");
            System.out.println("===========================================================================");
        }

        private void displayRoom(Room room){
            String availability = room.isAvailable() ? "Available" : "Not Available";
            System.out.println("\n===================================================");
            System.out.println("                    ROOM INFORMATION");
            System.out.println("===================================================");
            System.out.println("Room Number:     " + room.getRoomNumber());
            System.out.println("Room Type:       " + room.getRoomType() + " (RM " + String.format("%.2f", FrontDeskServiceControl.getRoomRate(room.getRoomType())) + "/night)");
            System.out.println("Cleaning Status: " + room.getCleaningStatus());
            System.out.println("Availability:    " + availability);
            System.out.println("===================================================");
        }
            
        public void frontDeskReport1(){
            DoublyLinkedList<Guest> guests = controller.getGuestList();
            
            double totalBaseCharges = 0;
            double totalDiscount = 0;
            double totalTax = 0;
            double totalRevenue = 0;
            
            int totalNights = 0;
            int totalGuests = guests.getNumberOfEntries();
            
            double highestBill = 0;
            double lowestBill = Double.MAX_VALUE;
            
            System.out.println("\n========================================================================================================================================================");
            System.out.println("                                                    GUEST BILLING SUMMARY REPORT");
            System.out.println("========================================================================================================================================================");           
            System.out.printf("%-11s %-16s %-6s %-14s %-14s %13s %13s       %-35s%n", "Conf. No", "Guest Name", "Stay", "Room", "Tier (Pts)", "Sub Total", "Total", "Billing Details");
            System.out.println("--------------------------------------------------------------------------------------------------------------------------------------------------------");
            
            for (int i = 0; i < guests.getNumberOfEntries(); i++){
                Guest guest = guests.getEntry(i);
                
                FrontDeskServiceControl.BillingBreakdown bill = controller.calculateBilling(guest.getConfirmationNumber());
                
                if(bill == null){
                    continue;
                }
                
                String roomNumber = (guest.getAssignedRoom()!= null)
                        ? guest.getAssignedRoom().getRoomNumber() + " (" + guest.getAssignedRoom().getRoomType() + ")"
                        : "Unassigned";
                String tier = (guest.getMemberProfile() != null)
                        ? guest.getMemberProfile().getTierType() + " (" + guest.getMemberProfile().getPoints() + ")"
                        : "Non-Member";
                
                System.out.printf("%-11s %-16s %-6s %-14s %-15s RM %10.2f RM %10.2f     %-30s%n", 
                        guest.getConfirmationNumber(), 
                        guest.getName(), 
                        guest.getStayDays() + "nts",
                        roomNumber, 
                        tier,
                        bill.subtotal,
                        bill.total,
                        guest.getBillingDetails());
                
                totalNights += bill.stayDays;
                totalBaseCharges += bill.baseCharge;
                totalDiscount += bill.pointDiscount;
                totalTax += bill.tax;
                totalRevenue += bill.total;
                
                if(bill.total > highestBill){
                    highestBill = bill.total;
                }
                
                if(bill.total < lowestBill){
                    lowestBill = bill.total;
                }
            }
            
            double averageStay = totalGuests > 0
                    ? (double) totalNights / totalGuests
                    : 0;
            
            double averageBill = totalGuests > 0 
                    ? totalRevenue/ totalGuests
                    : 0;
            
            System.out.println("--------------------------------------------------------------------------------------------------------------------------------------------------------");
            System.out.printf("%-28s : %d%n",
                    "Total Guests", totalGuests);
            System.out.printf("%-28s : %d nights%n",
                    "Total Nights", totalNights);
            System.out.printf("%-28s : RM %.2f%n", 
                    "Total Base Charges", totalBaseCharges);
            System.out.printf("%-28s : RM %.2f%n",
                    "Total Discounts", totalDiscount);
            System.out.printf("%-28s : RM %.2f%n",
                    "Total Tax (6% SST)", totalTax);
            System.out.printf("%-28s : RM %.2f%n",
                    "Total Revenue", totalRevenue);
            
            System.out.printf("%-28s : %.2f nights%n", 
                    "Average Stay Duration",averageStay);
            System.out.printf("%-28s : RM %.2f%n",
                    "Average Guest Bill", averageBill);
            
            if(totalGuests > 0){
                System.out.printf("%-28s : RM %.2f%n",
                        "Highest Guest Bill", highestBill);
                System.out.printf("%-28s : RM %.2f%n",
                        "Lowest Guest Bill", lowestBill);
            
            }
            System.out.println("========================================================================================================================================================");
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

            promptStayHistorySection();
        }

        /**
         * Optional add-on: asks whether staff want the stay-history / activity
         * timeline. Declining simply returns to the menu.
         */
        private void promptStayHistorySection(){
            System.out.print("\nView stay history / activity timeline? (1: Yes | 0: No): ");
            if(readInteger() != 1){
                return;
            }

            System.out.println("--- Stay History Timeline Filters ---");
            System.out.print("Confirmation number (blank = all guests): ");
            String histConf = scanner.nextLine().trim();
            if(histConf.isEmpty()){
                histConf = null;
            }
            System.out.print("From date dd/MM/yyyy (blank = no lower bound): ");
            java.time.LocalDate fromDate = StayRecord.parseDate(scanner.nextLine());
            System.out.print("To date dd/MM/yyyy (blank = no upper bound): ");
            java.time.LocalDate toDate = StayRecord.parseDate(scanner.nextLine());

            System.out.println(controller.getStayHistorySection(histConf, fromDate, toDate));
        }

        public void transferRoom(){
            System.out.print("Enter guest confirmation number: ");
            String conf = scanner.nextLine().trim();

            Guest guest = controller.findGuest(conf);
            if(guest == null){
                System.out.println("Guest with confirmation number \"" + conf + "\" was not found!");
                return;
            }
            String currentRoom = (guest.getAssignedRoom() != null)
                    ? guest.getAssignedRoom().getRoomNumber() + " (" + guest.getAssignedRoom().getRoomType() + ")"
                    : "Unassigned";
            System.out.println("Guest: " + guest.getName() + " | Current room: " + currentRoom
                    + " | Status: " + guest.getStatus());

            System.out.print("Enter new room number to transfer into: ");
            String newRoom = scanner.nextLine().trim();
            try{
                controller.transferRoom(conf, newRoom);
                Guest updated = controller.findGuest(conf);
                System.out.println(">> Transfer successful. " + updated.getName() + " is now in room "
                        + updated.getAssignedRoom().getRoomNumber()
                        + " (" + updated.getAssignedRoom().getRoomType() + "). Previous room released as Dirty.");
            } catch (IllegalArgumentException | IllegalStateException ex){
                System.out.println(">> Could not transfer room: " + ex.getMessage());
            }
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
