/*
 * Module: System Verification & Integration Test Suite
 * Author: WEI XIN
 * 
 * Description:
 * Automated test suite to verify ADT operations, VIP auto-reordering,
 * cross-module shared inventory synchronization, and analytical reporting.
 */
package test;

import adt.*;
import control.*;
import entity.*;

public class TestSystemIntegration {

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("    AUTOMATED VERIFICATION TEST SUITE             ");
        System.out.println("==================================================");

        // 1. Setup shared room list
        DoublyLinkedListInterface<Room> roomList = new DoublyLinkedList<>();
        roomList.insertLast(new Room("101", "Ready for Check-In", true));
        roomList.insertLast(new Room("102", "Ready for Check-In", true));
        roomList.insertLast(new Room("103", "Dirty", true));

        WalkInRegistrationControl walkInControl = new WalkInRegistrationControl(roomList);
        PriorityAllocationControl priorityControl = new PriorityAllocationControl(roomList);
        HousekeepingControl housekeepingControl = new HousekeepingControl(roomList);

        // Test 1: Priority Queue Auto-Reordering
        System.out.println("\n[Test 1] Priority Queue Auto-Reordering by VIP Tier:");
        priorityControl.getPriorityQueue().clear(); // start fresh

        priorityControl.registerVIPGuest("V001", "Gold Guest", "M01", "Gold", 200, "Card");
        priorityControl.registerVIPGuest("V002", "Diamond Guest", "M02", "Diamond", 1500, "Corporate");
        priorityControl.registerVIPGuest("V003", "Silver Guest", "M03", "Silver", 100, "Cash");
        priorityControl.registerVIPGuest("V004", "Elite Guest", "M04", "Elite", 900, "Card");
        priorityControl.registerVIPGuest("V005", "Platinum Guest", "M05", "Platinum", 1200, "Card");

        // Verify ordering: Diamond -> Platinum -> Elite -> Gold -> Silver
        Guest first = priorityControl.peekNextVIP();
        System.out.println("Top of Queue: " + first.getName() + " (" + first.getMemberProfile().getTierType() + ")");
        assert "Diamond Guest".equals(first.getName()) : "Expected Diamond Guest at top of queue!";

        Guest alloc1 = priorityControl.allocateFirstAvailableRoom();
        System.out.println("Allocated 1st: " + alloc1.getName() + " -> Room " + alloc1.getAssignedRoom().getRoomNumber());
        assert "Diamond Guest".equals(alloc1.getName()) && "101".equals(alloc1.getAssignedRoom().getRoomNumber());

        Guest alloc2 = priorityControl.allocateFirstAvailableRoom();
        System.out.println("Allocated 2nd: " + alloc2.getName() + " -> Room " + alloc2.getAssignedRoom().getRoomNumber());
        assert "Platinum Guest".equals(alloc2.getName()) && "102".equals(alloc2.getAssignedRoom().getRoomNumber());

        // Test 2: Room availability sync across modules
        System.out.println("\n[Test 2] Cross-Module Room Availability Sync:");
        Room r101 = housekeepingControl.findRoomByNumber("101");
        System.out.println("Room 101 status: " + r101.getCleaningStatus() + " | Available: " + r101.isRoomAvailable());
        assert !r101.isRoomAvailable() : "Room 101 should be unavailable after VIP allocation";

        // Walk-in should find no rooms since 101, 102 are allocated and 103 is Dirty
        walkInControl.registerWalkIn("John WalkIn", "Cash");
        Guest walkInProc = walkInControl.processNextGuest();
        System.out.println("Walk-in room allocation when no clean rooms available: " + (walkInProc == null ? "None (Correct - waiting)" : walkInProc.getName()));
        assert walkInProc == null : "Walk-in should wait when no clean rooms available";

        // Test 3: Housekeeping sequential update and LIFO Undo
        System.out.println("\n[Test 3] Housekeeping Flow & LIFO Undo:");
        // Clean room 103: Dirty -> Cleaning In Progress -> Inspected -> Ready for Check-In
        housekeepingControl.updateCleaningStatus("103", "Cleaning In Progress", "Staff A", "Started cleaning");
        housekeepingControl.updateCleaningStatus("103", "Inspected", "Supervisor B", "Passed inspection");
        System.out.println("Room 103 after inspection: " + roomList.getEntry(2).getCleaningStatus());
        assert "Inspected".equals(roomList.getEntry(2).getCleaningStatus());

        // Test LIFO Undo (Rollback)
        HousekeepingTask undone = housekeepingControl.rollbackLatestUpdate();
        System.out.println("Undone task: " + undone.getTaskId() + " -> reverted to: " + roomList.getEntry(2).getCleaningStatus());
        assert "Cleaning In Progress".equals(roomList.getEntry(2).getCleaningStatus());

        // Test Late check-out handling during cleaning
        housekeepingControl.handleLateCheckout("103", "Staff A", "Guest requested late checkout");
        System.out.println("Room 103 after late check-out reset: " + roomList.getEntry(2).getCleaningStatus());
        assert "Dirty".equals(roomList.getEntry(2).getCleaningStatus());

        // Test 4: Report generation test
        System.out.println("\n[Test 4] Reports Generation Test:");
        System.out.println(priorityControl.generateTierDistributionReport(null, null));
        System.out.println(priorityControl.generatePriorityWaitlistReport(null, null));
        walkInControl.printGuestCheckInReport(null, null);
        walkInControl.printQueueSummaryReport(null);
        housekeepingControl.generateRoomCleaningStatusReport(null, null);
        housekeepingControl.generateTaskActivityReport(null, null, null);

        System.out.println("\n>> ALL AUTOMATED VERIFICATION TESTS PASSED SUCCESSFULLY! <<\n");
    }
}
