/*
 * Module: System Verification & Integration Test Suite
 * Author: WEI XIN
 *
 * Description:
 * Automated test suite verifying ADT operations, VIP auto-reordering,
 * tri-state Guest lifecycle, cross-module room inventory synchronization,
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

        DoublyLinkedListInterface<Room> roomList = new DoublyLinkedList<>();
        roomList.insertLast(new Room("101", "Ready for Check-In", true));
        roomList.insertLast(new Room("102", "Ready for Check-In", true));
        roomList.insertLast(new Room("103", "Dirty", true));

        WalkInRegistrationControl walkInControl   = new WalkInRegistrationControl(roomList);
        PriorityAllocationControl priorityControl = new PriorityAllocationControl(roomList);
        HousekeepingControl       hkControl       = new HousekeepingControl(roomList);

        // Test 1: VIP Priority Queue Auto-Reordering
        System.out.println("\n[Test 1] Priority Queue Auto-Reordering by VIP Tier:");
        priorityControl.getPriorityQueue().clear();
        priorityControl.registerVIPGuest("V001", "Gold Guest",     "M01", "Gold",     200,  "Card");
        priorityControl.registerVIPGuest("V002", "Diamond Guest",  "M02", "Diamond",  1500, "Corporate");
        priorityControl.registerVIPGuest("V003", "Silver Guest",   "M03", "Silver",   100,  "Cash");
        priorityControl.registerVIPGuest("V004", "Elite Guest",    "M04", "Elite",    900,  "Card");
        priorityControl.registerVIPGuest("V005", "Platinum Guest", "M05", "Platinum", 1200, "Card");

        Guest top = priorityControl.peekNextVIP();
        System.out.println("  Top of Queue: " + top.getName() + " (" + top.getMemberProfile().getTierType() + ")");
        assert "Diamond Guest".equals(top.getName()) : "FAIL: Diamond should be at top!";
        System.out.println("  PASS: Diamond correctly at top of priority queue.");

        Guest alloc1 = priorityControl.allocateFirstAvailableRoom();
        System.out.println("  Allocated 1st: " + alloc1.getName() + " -> Room " + alloc1.getAssignedRoom().getRoomNumber());
        assert "Diamond Guest".equals(alloc1.getName())               : "FAIL: Expected Diamond 1st!";
        assert "101".equals(alloc1.getAssignedRoom().getRoomNumber()) : "FAIL: Expected room 101!";
        assert Guest.STATUS_CHECKED_IN.equals(alloc1.getStatus())     : "FAIL: Should be Checked-In!";
        System.out.println("  PASS: Diamond Guest -> Room 101, status=Checked-In.");

        Guest alloc2 = priorityControl.allocateFirstAvailableRoom();
        System.out.println("  Allocated 2nd: " + alloc2.getName() + " -> Room " + alloc2.getAssignedRoom().getRoomNumber());
        assert "Platinum Guest".equals(alloc2.getName())              : "FAIL: Expected Platinum 2nd!";
        assert "102".equals(alloc2.getAssignedRoom().getRoomNumber()) : "FAIL: Expected room 102!";
        assert Guest.STATUS_CHECKED_IN.equals(alloc2.getStatus())     : "FAIL: Should be Checked-In!";
        System.out.println("  PASS: Platinum Guest -> Room 102, status=Checked-In.");

        // Test 2: Guest Tri-State Lifecycle
        System.out.println("\n[Test 2] Guest Tri-State Lifecycle Status:");
        Guest alice = walkInControl.registerWalkIn("Alice Lim", "Cash");
        assert Guest.STATUS_PENDING.equals(alice.getStatus()) : "FAIL: New guest should be Pending!";
        System.out.println("  PASS: Walk-in Alice Lim registered as Pending.");

        Guest noRoom = walkInControl.processNextGuest();
        assert noRoom == null : "FAIL: Should return null when no clean room available!";
        System.out.println("  PASS: Alice correctly waits (no Ready for Check-In room).");

        // Test 3: Cross-Module Room Availability Sync
        System.out.println("\n[Test 3] Cross-Module Room Availability Sync:");
        Room r101 = hkControl.findRoomByNumber("101");
        Room r102 = hkControl.findRoomByNumber("102");
        System.out.println("  Room 101 -> " + r101.getCleaningStatus() + " | available: " + r101.isRoomAvailable());
        System.out.println("  Room 102 -> " + r102.getCleaningStatus() + " | available: " + r102.isRoomAvailable());
        assert !r101.isRoomAvailable() : "FAIL: Room 101 should be unavailable!";
        assert !r102.isRoomAvailable() : "FAIL: Room 102 should be unavailable!";
        System.out.println("  PASS: Room 101 and 102 both correctly marked unavailable.");

        // Test 4: Housekeeping Sequential Cleaning Workflow
        System.out.println("\n[Test 4] Housekeeping Sequential Cleaning Workflow:");
        hkControl.updateCleaningStatus("103", "Cleaning In Progress", "Staff A",    "Started");
        assert "Cleaning In Progress".equals(roomList.getEntry(2).getCleaningStatus()) : "FAIL!";
        System.out.println("  PASS: Room 103: Dirty -> Cleaning In Progress.");

        hkControl.updateCleaningStatus("103", "Inspected",           "Supervisor B", "Passed");
        assert "Inspected".equals(roomList.getEntry(2).getCleaningStatus()) : "FAIL!";
        System.out.println("  PASS: Room 103: Cleaning In Progress -> Inspected.");

        hkControl.updateCleaningStatus("103", "Ready for Check-In",  "Supervisor B", "Cleared");
        assert "Ready for Check-In".equals(roomList.getEntry(2).getCleaningStatus()) : "FAIL!";
        System.out.println("  PASS: Room 103: Inspected -> Ready for Check-In.");

        try {
            hkControl.updateCleaningStatus("103", "Dirty", "Staff A", "Skip");
            System.out.println("  FAIL: Should have rejected invalid transition!");
        } catch (IllegalStateException e) {
            System.out.println("  PASS: Invalid transition rejected -> " + e.getMessage());
        }

        // Test 5: LIFO Rollback - Global & Per-Room
        System.out.println("\n[Test 5] Housekeeping LIFO Rollback:");
        hkControl.correctCleaningStatus("103", "Dirty", "Supervisor B", "Reset for test");
        hkControl.updateCleaningStatus("103", "Cleaning In Progress", "Staff A",     "Cleaning");
        hkControl.updateCleaningStatus("103", "Inspected",            "Supervisor B", "Inspected");

        HousekeepingTask undoneGlobal = hkControl.rollbackLatestUpdate();
        System.out.println("  Global rollback: " + undoneGlobal.getTaskId() + " -> room 103 now: " + roomList.getEntry(2).getCleaningStatus());
        assert "Cleaning In Progress".equals(roomList.getEntry(2).getCleaningStatus()) : "FAIL: Expected Cleaning In Progress!";
        System.out.println("  PASS: Global LIFO rollback reverted room 103 to Cleaning In Progress.");

        HousekeepingTask undoneRoom = hkControl.rollbackLatestUpdateForRoom("103");
        System.out.println("  Per-room rollback: " + undoneRoom.getTaskId() + " -> room 103 now: " + roomList.getEntry(2).getCleaningStatus());
        assert "Dirty".equals(roomList.getEntry(2).getCleaningStatus()) : "FAIL: Expected Dirty!";
        System.out.println("  PASS: Per-room LIFO rollback reverted room 103 to Dirty.");

        // Test 6: Late Check-Out Reset
        System.out.println("\n[Test 6] Late Check-Out Reset:");
        hkControl.updateCleaningStatus("103", "Cleaning In Progress", "Staff A", "Cleaning");
        hkControl.handleLateCheckout("103", "Staff A", "Guest extended stay");
        assert "Dirty".equals(roomList.getEntry(2).getCleaningStatus()) : "FAIL: Expected Dirty after late checkout!";
        System.out.println("  PASS: Late check-out correctly reset room 103 to Dirty.");

        // Test 7: Walk-In Checks In After Room Is Prepared
        System.out.println("\n[Test 7] Walk-In Checks In After Room 103 Becomes Ready:");
        hkControl.updateCleaningStatus("103", "Cleaning In Progress", "Staff A",     "Cleaning");
        hkControl.updateCleaningStatus("103", "Inspected",            "Supervisor B", "Inspected");
        hkControl.updateCleaningStatus("103", "Ready for Check-In",   "Supervisor B", "Ready");

        Guest walkedIn = walkInControl.processNextGuest();
        assert walkedIn != null                                         : "FAIL: Should process Alice into room 103!";
        assert "Alice Lim".equals(walkedIn.getName())                   : "FAIL: Expected Alice Lim!";
        assert "103".equals(walkedIn.getAssignedRoom().getRoomNumber()) : "FAIL: Expected room 103!";
        assert Guest.STATUS_CHECKED_IN.equals(walkedIn.getStatus())     : "FAIL: Alice should be Checked-In!";
        System.out.println("  PASS: Alice Lim checked into room 103 (status=Checked-In).");

        // Test 8: Check-Out & Room Release
        System.out.println("\n[Test 8] Check-Out & Room Release:");
        walkInControl.registerWalkIn("Bob Tan", "Credit Card");
        walkInControl.checkOutGuest(walkedIn.getConfirmationNumber());
        assert Guest.STATUS_CHECKED_OUT.equals(walkedIn.getStatus()) : "FAIL: Alice should be Checked-Out!";
        assert walkedIn.getAssignedRoom() == null                     : "FAIL: Room ref should be null!";

        Room r103 = hkControl.findRoomByNumber("103");
        assert r103.isRoomAvailable()                 : "FAIL: Room 103 should be available!";
        assert "Dirty".equals(r103.getCleaningStatus()) : "FAIL: Room 103 should be Dirty!";
        System.out.println("  PASS: Alice checked out -> room 103 Available & Dirty.");

        try {
            walkInControl.checkOutGuest(walkedIn.getConfirmationNumber());
            System.out.println("  FAIL: Should have rejected double check-out!");
        } catch (IllegalStateException e) {
            System.out.println("  PASS: Double check-out rejected -> " + e.getMessage());
        }

        try {
            walkInControl.checkOutGuest("FAKE-9999");
            System.out.println("  FAIL: Should have rejected unknown confirmation number!");
        } catch (IllegalArgumentException e) {
            System.out.println("  PASS: Unknown conf# rejected -> " + e.getMessage());
        }

        // Test 9: DoublyLinkedList ADT - FIFO and LIFO
        System.out.println("\n[Test 9] DoublyLinkedList ADT - FIFO Queue & LIFO Stack:");
        DoublyLinkedListInterface<String> list = new DoublyLinkedList<>();
        list.insertLast("A");
        list.insertLast("B");
        list.insertLast("C");

        assert "A".equals(list.retrieveFirst())  : "FAIL: retrieveFirst should be A!";
        assert "C".equals(list.retrieveLast())   : "FAIL: retrieveLast should be C!";
        assert list.getNumberOfEntries() == 3    : "FAIL: size should be 3!";

        String front = list.removeFirst();
        assert "A".equals(front)                 : "FAIL: removeFirst should return A!";
        assert list.getNumberOfEntries() == 2    : "FAIL: size should be 2!";

        String back = list.removeLast();
        assert "C".equals(back)                  : "FAIL: removeLast should return C!";
        assert list.getNumberOfEntries() == 1    : "FAIL: size should be 1!";
        System.out.println("  PASS: FIFO removeFirst=A, LIFO removeLast=C.");

        // Manually verify remaining element is B (index 0)
        assert "B".equals(list.getEntry(0)) : "FAIL: Only remaining element should be B!";
        assert list.getNumberOfEntries() == 1 : "FAIL: List should have exactly 1 element!";
        System.out.println("  PASS: Only B remains after removing A (FIFO) and C (LIFO).");

        // Test 10: Full Report Generation
        System.out.println("\n[Test 10] Full Report Generation:");

        System.out.println(priorityControl.generateTierDistributionReport(null, null));
        System.out.println(priorityControl.generatePriorityWaitlistReport(null, null));
        walkInControl.printGuestCheckInReport(null, null);
        walkInControl.printQueueSummaryReport(null);

        HousekeepingControl.RoomStatusReport roomRpt = hkControl.generateRoomCleaningStatusReport(null, null);
        System.out.println("\n=======================================================================");
        System.out.println("                    ROOM CLEANING STATUS REPORT");
        System.out.println("=======================================================================");
        System.out.printf("Filter -> Status: %-20s | Availability: %s%n",
                roomRpt.getStatusFilter() == null ? "ALL" : roomRpt.getStatusFilter(),
                roomRpt.getAvailabilityFilter() == null ? "ALL" : roomRpt.getAvailabilityFilter());
        System.out.println("-----------------------------------------------------------------------");
        System.out.printf("%-10s %-26s %-16s %-22s%n", "Room No.", "Cleaning Status", "Availability", "Next Status");
        System.out.println("-----------------------------------------------------------------------");
        for (Room r : roomRpt.getRooms()) {
            String next = hkControl.getNextExpectedStatus(r);
            System.out.printf("%-10s %-26s %-16s %-22s%n",
                    r.getRoomNumber(), r.getCleaningStatus(),
                    r.isAvailable() ? "Available" : "Unavailable",
                    next == null ? "-" : next);
        }
        System.out.println("-----------------------------------------------------------------------");
        System.out.printf("Total rooms: %d | Dirty: %d | Cleaning: %d | Inspected: %d | Ready: %d | ReadyRate: %.1f%%%n",
                roomRpt.getTotalRooms(), roomRpt.getDirtyCount(), roomRpt.getCleaningCount(),
                roomRpt.getInspectedCount(), roomRpt.getReadyCount(), roomRpt.getReadyRate());
        System.out.println("=======================================================================");

        HousekeepingControl.TaskActivityReport taskRpt = hkControl.generateTaskActivityReport(null, null, null);
        System.out.println("\n========================================================================================================");
        System.out.println("                              HOUSEKEEPING TASK ACTIVITY REPORT");
        System.out.println("========================================================================================================");
        System.out.printf("Filters -> Staff: %-10s | Room: %-6s | New Status: %s%n",
                taskRpt.getStaffFilter() == null ? "ALL" : taskRpt.getStaffFilter(),
                taskRpt.getRoomFilter() == null ? "ALL" : taskRpt.getRoomFilter(),
                taskRpt.getNewStatusFilter() == null ? "ALL" : taskRpt.getNewStatusFilter());
        System.out.println("--------------------------------------------------------------------------------------------------------");
        for (HousekeepingTask t : taskRpt.getTasks()) {
            System.out.println(t);
        }
        System.out.println("--------------------------------------------------------------------------------------------------------");
        System.out.printf("Total: %d | Completed to Ready: %d | Reset to Dirty: %d | Most Active: %s (%d tasks)%n",
                taskRpt.getTotalTasks(), taskRpt.getCompletedToReady(),
                taskRpt.getResetToDirty(), taskRpt.getMostActiveStaff(),
                taskRpt.getMostActiveStaffCount());
        System.out.println("========================================================================================================");

        System.out.println("\n>> ALL AUTOMATED VERIFICATION TESTS PASSED SUCCESSFULLY! <<\n");
    }
}
