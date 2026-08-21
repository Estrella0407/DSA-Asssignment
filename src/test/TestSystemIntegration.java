/*
 * Module: System Verification & Integration Test Suite
 * Author: WEI XIN
 *
 * Description:
 * Comprehensive automated test suite verifying all 4 hotel management subsystems:
 * - VIP Priority Queue Auto-Reordering (Heap ADT)
 * - Specific Room Type Request & Strict Matching / Waiting Logic
 * - Graduated Long-Stay Loyalty Milestone Promotions (Silver, Gold, Elite, Platinum, Diamond)
 * - Room-Type Specific Point Redemptions for 2-Day Free Stays
 * - Front-Desk O(1) Hash Table Lookup & Dynamic Billing Calculations
 * - Cross-Module Room Inventory & Housekeeping Sequential Workflow + LIFO Rollback
 * - Guest Lifecycle Tri-State Check-In/Check-Out
 * - DoublyLinkedList Linear ADT FIFO/LIFO operations
 * - Management Analytical Reports across all modules
 *
 * Run with: java -ea -cp bin test.TestSystemIntegration
 */
package test;

import adt.*;
import control.*;
import entity.*;

public class TestSystemIntegration {

    public static void main(String[] args) {
        System.out.println("======================================================================");
        System.out.println("     COMPREHENSIVE HOTEL MANAGEMENT AUTOMATED TEST SUITE             ");
        System.out.println("======================================================================");

        // Shared room inventory across subsystems
        DoublyLinkedListInterface<Room> roomList = new DoublyLinkedList<>();
        Room r101 = new Room("101", "Ready for Check-In", true, Room.TYPE_SINGLE);
        Room r102 = new Room("102", "Ready for Check-In", true, Room.TYPE_DOUBLE);
        Room r103 = new Room("103", "Dirty",              true, Room.TYPE_DELUXE);
        Room r201 = new Room("201", "Ready for Check-In", true, Room.TYPE_SUITE);

        roomList.insertLast(r101);
        roomList.insertLast(r102);
        roomList.insertLast(r103);
        roomList.insertLast(r201);

        Dictionary<String, Guest> guestTable = new HashTable<>();
        Dictionary<String, Room> roomTable = new HashTable<>();
        for (int i = 0; i < roomList.getNumberOfEntries(); i++) {
            Room r = roomList.getEntry(i);
            roomTable.add(r.getRoomNumber(), r);
        }

        WalkInRegistrationControl walkInControl   = new WalkInRegistrationControl(roomList);
        PriorityAllocationControl priorityControl = new PriorityAllocationControl(roomList);
        HousekeepingControl       hkControl       = new HousekeepingControl(roomList);
        FrontDeskServiceControl   frontDeskControl = new FrontDeskServiceControl(guestTable, roomTable);

        // =====================================================================
        // Test 1: VIP Priority Queue Auto-Reordering by Tier & Points
        // =====================================================================
        System.out.println("\n[Test 1] Priority Queue Auto-Reordering by VIP Tier:");
        priorityControl.getPriorityQueue().clear();

        priorityControl.registerVIPGuest("V001", "Gold Guest",     "M01", "Gold",     200,  "Card",      Room.TYPE_DOUBLE, 2);
        priorityControl.registerVIPGuest("V002", "Diamond Guest",  "M02", "Diamond",  1500, "Corporate", Room.TYPE_SUITE,  4);
        priorityControl.registerVIPGuest("V003", "Silver Guest",   "M03", "Silver",   100,  "Cash",      Room.TYPE_SINGLE, 1);
        priorityControl.registerVIPGuest("V004", "Elite Guest",    "M04", "Elite",    900,  "Card",      Room.TYPE_DOUBLE, 3);
        priorityControl.registerVIPGuest("V005", "Platinum Guest", "M05", "Platinum", 1200, "Card",      Room.TYPE_SINGLE, 2);

        Guest top = priorityControl.peekNextVIP();
        System.out.println("  Top of Priority Queue: " + top.getName() + " (" + top.getMemberProfile().getTierType() + ")");
        assert "Diamond Guest".equals(top.getName()) : "FAIL: Diamond should be at top!";
        System.out.println("  PASS: Diamond correctly at top of priority queue.");

        // =====================================================================
        // Test 2: Specific Room Type Request & Strict Matching / Waiting Logic
        // =====================================================================
        System.out.println("\n[Test 2] Specific Room Type Request & Strict Matching:");
        // Diamond requested SUITE -> Room 201 is Suite and Ready -> gets Room 201
        Guest allocDiamond = priorityControl.allocateFirstAvailableRoom();
        assert allocDiamond != null                                             : "FAIL: Diamond should be allocated!";
        assert "201".equals(allocDiamond.getAssignedRoom().getRoomNumber())     : "FAIL: Diamond must receive Suite 201!";
        assert Room.TYPE_SUITE.equals(allocDiamond.getAssignedRoom().getRoomType()) : "FAIL: Room type must be Suite!";
        System.out.println("  PASS: Diamond Guest received preferred Suite 201.");

        // Top is now Platinum Guest (requests SINGLE). Room 101 is Single & Ready -> gets Room 101
        Guest allocPlatinum = priorityControl.allocateFirstAvailableRoom();
        assert allocPlatinum != null                                            : "FAIL: Platinum should be allocated!";
        assert "101".equals(allocPlatinum.getAssignedRoom().getRoomNumber())    : "FAIL: Platinum must receive Single 101!";
        System.out.println("  PASS: Platinum Guest received preferred Single 101.");

        // Top is now Elite Guest (requests DOUBLE). Room 102 is Double & Ready -> gets Room 102
        Guest allocElite = priorityControl.allocateFirstAvailableRoom();
        assert allocElite != null                                               : "FAIL: Elite should be allocated!";
        assert "102".equals(allocElite.getAssignedRoom().getRoomNumber())       : "FAIL: Elite must receive Double 102!";
        System.out.println("  PASS: Elite Guest received preferred Double 102.");

        // Top is now Gold Guest (requests DOUBLE). All Double rooms (102) are occupied -> must wait!
        Guest allocGoldWait = priorityControl.allocateFirstAvailableRoom();
        assert allocGoldWait == null : "FAIL: Gold Guest requested Double but none ready, must return null!";
        System.out.println("  PASS: Gold Guest correctly waits because no clean Double room is available.");

        // Verify getAvailableCleanRoomsByType returns 0 for Double when occupied
        DoublyLinkedListInterface<Room> availDouble = priorityControl.getAvailableCleanRoomsByType(Room.TYPE_DOUBLE);
        assert availDouble.isEmpty() : "FAIL: Available Double rooms should be empty!";
        System.out.println("  PASS: getAvailableCleanRoomsByType correctly returns empty list for occupied Double type.");

        // =====================================================================
        // Test 3: Graduated Long-Stay Milestone Promotions
        // =====================================================================
        System.out.println("\n[Test 3] Graduated Long-Stay Loyalty Milestone Promotions:");
        // 1. > 14 days (15d) -> Silver (200 pts)
        Guest gSilver = walkInControl.registerWalkIn("Silver Stayer", "Card", Room.TYPE_DELUXE, 15);
        assert gSilver.getMemberProfile() != null                              : "FAIL: Should have member profile!";
        assert "SILVER".equalsIgnoreCase(gSilver.getMemberProfile().getTierType()) : "FAIL: 15 days must be Silver!";
        assert gSilver.getMemberProfile().getPoints() == 200                   : "FAIL: Silver must have 200 points!";
        System.out.println("  PASS: 15-day stay auto-enrolled as SILVER with 200 bonus points.");

        // 2. > 30 days (35d) -> Gold (500 pts)
        Guest gGold = walkInControl.registerBooking("BK100", "Gold Stayer", "Paid", Room.TYPE_DOUBLE, 35);
        assert "GOLD".equalsIgnoreCase(gGold.getMemberProfile().getTierType()) : "FAIL: 35 days must be Gold!";
        assert gGold.getMemberProfile().getPoints() == 500                    : "FAIL: Gold must have 500 points!";
        System.out.println("  PASS: 35-day stay auto-enrolled as GOLD with 500 bonus points.");

        // 3. > 60 days (70d) -> Elite (1000 pts)
        Guest gElite = walkInControl.registerWalkIn("Elite Stayer", "Card", Room.TYPE_DELUXE, 70);
        assert "ELITE".equalsIgnoreCase(gElite.getMemberProfile().getTierType()) : "FAIL: 70 days must be Elite!";
        assert gElite.getMemberProfile().getPoints() == 1000                    : "FAIL: Elite must have 1000 points!";
        System.out.println("  PASS: 70-day stay auto-enrolled as ELITE with 1000 bonus points.");

        // 4. > 90 days (100d) -> Platinum (1800 pts)
        Guest gPlat = walkInControl.registerWalkIn("Plat Stayer", "Card", Room.TYPE_SUITE, 100);
        assert "PLATINUM".equalsIgnoreCase(gPlat.getMemberProfile().getTierType()) : "FAIL: 100 days must be Platinum!";
        assert gPlat.getMemberProfile().getPoints() == 1800                     : "FAIL: Platinum must have 1800 points!";
        System.out.println("  PASS: 100-day stay auto-enrolled as PLATINUM with 1800 bonus points.");

        // 5. > 180 days (200d) -> Diamond (3000 pts)
        Guest gDiamond = walkInControl.registerWalkIn("Diamond Stayer", "Card", Room.TYPE_SUITE, 200);
        assert "DIAMOND".equalsIgnoreCase(gDiamond.getMemberProfile().getTierType()) : "FAIL: 200 days must be Diamond!";
        assert gDiamond.getMemberProfile().getPoints() == 3000                     : "FAIL: Diamond must have 3000 points!";
        System.out.println("  PASS: 200-day stay auto-enrolled as DIAMOND with 3000 bonus points.");

        // =====================================================================
        // Test 4: Room-Type Specific 2-Day Free Stay Point Redemption
        // =====================================================================
        System.out.println("\n[Test 4] Room-Type Specific 2-Day Free Stay Point Redemption:");
        assert Guest.getRedemptionCostForRoomType("Single") == 150 : "FAIL: Single redemption must be 150 pts!";
        assert Guest.getRedemptionCostForRoomType("Double") == 250 : "FAIL: Double redemption must be 250 pts!";
        assert Guest.getRedemptionCostForRoomType("Deluxe") == 400 : "FAIL: Deluxe redemption must be 400 pts!";
        assert Guest.getRedemptionCostForRoomType("Suite")  == 600 : "FAIL: Suite redemption must be 600 pts!";

        // Member with 1000 points redeems Suite (600 pts)
        Member testMember = new Member("M-TEST", "ELITE", 1000);
        Guest redeemGuest = new Guest("WI-RED01", "Redeemer John", false, "Walk-in", null, "Card", testMember, 5);
        redeemGuest.setPreferredRoomType(Room.TYPE_SUITE);
        boolean redeemed = redeemGuest.redeemPointsForStay(Room.TYPE_SUITE);

        assert redeemed                                    : "FAIL: Point redemption should succeed!";
        assert testMember.getPoints() == 400               : "FAIL: 1000 - 600 = 400 points remaining!";
        assert redeemGuest.isPointsRedeemed()              : "FAIL: isPointsRedeemed must be true!";
        assert redeemGuest.getRedeemedPoints() == 600      : "FAIL: redeemedPoints must be 600!";
        System.out.println("  PASS: Suite 2-day redemption deducted 600 points (Balance: 400 pts).");

        // Insufficient points test
        boolean failRedeem = redeemGuest.redeemPointsForStay(Room.TYPE_SUITE); // Needs 600, has 400
        assert !failRedeem : "FAIL: Should not allow redemption with insufficient points!";
        System.out.println("  PASS: Second redemption rejected due to insufficient points.");

        // =====================================================================
        // Test 5: Front-Desk O(1) Hash Table Lookup & Dynamic Billing
        // =====================================================================
        System.out.println("\n[Test 5] Front-Desk O(1) Hash Table Lookup & Billing:");
        frontDeskControl.addGuest(redeemGuest);
        Guest foundGuest = frontDeskControl.findGuest("WI-RED01");
        assert foundGuest != null && "Redeemer John".equals(foundGuest.getName()) : "FAIL: O(1) Guest lookup failed!";

        FrontDeskServiceControl.BillingBreakdown bill = frontDeskControl.calculateBilling("WI-RED01");
        assert bill != null                                : "FAIL: Billing breakdown returned null!";
        assert bill.nightlyRate == 500.00                  : "FAIL: Suite rate is RM 500.00!";
        assert bill.stayDays == 5                          : "FAIL: Stay days is 5!";
        assert bill.baseCharge == 2500.00                  : "FAIL: 5 * 500 = 2500!";
        assert bill.pointDiscount == 1000.00               : "FAIL: 2 free nights discount = 1000!";
        assert bill.subtotal == 1500.00                    : "FAIL: Subtotal = 1500!";
        assert Math.abs(bill.tax - 90.00) < 0.01           : "FAIL: 6% tax on 1500 = 90!";
        assert Math.abs(bill.total - 1590.00) < 0.01       : "FAIL: Total = 1590!";
        System.out.println("  PASS: Front-Desk calculated Suite billing: Base RM 2500 - Discount RM 1000 + Tax RM 90 = RM 1590.00.");

        // =====================================================================
        // Test 6: Housekeeping Sequential Cleaning & Room Preparation
        // =====================================================================
        System.out.println("\n[Test 6] Housekeeping Sequential Cleaning & Preparation:");
        // Room 103 is Dirty Deluxe -> Clean it to Ready
        hkControl.updateCleaningStatus("103", "Cleaning In Progress", "Staff Neo", "Started");
        hkControl.updateCleaningStatus("103", "Inspected",            "Super Neo", "Inspected");
        hkControl.updateCleaningStatus("103", "Ready for Check-In",   "Super Neo", "Ready");
        assert "Ready for Check-In".equals(r103.getCleaningStatus()) : "FAIL: Room 103 must be Ready!";
        System.out.println("  PASS: Room 103 successfully prepared to 'Ready for Check-In'.");

        // Silver Stayer (head of walk-in queue) requested DELUXE -> gets Room 103!
        Guest walkInAlloc = walkInControl.processNextGuest();
        assert walkInAlloc != null && "Silver Stayer".equals(walkInAlloc.getName()) : "FAIL: Silver Stayer expected!";
        assert "103".equals(walkInAlloc.getAssignedRoom().getRoomNumber())          : "FAIL: Must be assigned room 103!";
        assert Guest.STATUS_CHECKED_IN.equals(walkInAlloc.getStatus())              : "FAIL: Status must be Checked-In!";
        System.out.println("  PASS: Silver Stayer checked into preferred Deluxe room 103.");

        // =====================================================================
        // Test 7: Housekeeping LIFO Rollback & Late Check-Out Reset
        // =====================================================================
        System.out.println("\n[Test 7] Housekeeping LIFO Rollbacks:");
        // Prepare Room 101 for LIFO rollback testing by resetting to Dirty
        hkControl.correctCleaningStatus("101", "Dirty", "Super Neo", "Reset");
        hkControl.updateCleaningStatus("101", "Cleaning In Progress", "Staff Neo", "Cleaning");
        hkControl.updateCleaningStatus("101", "Inspected",            "Super Neo", "Inspected");

        HousekeepingTask popped = hkControl.rollbackLatestUpdate();
        assert "Cleaning In Progress".equals(r101.getCleaningStatus()) : "FAIL: Global rollback failed!";
        System.out.println("  PASS: Global LIFO rollback reverted room 101 to Cleaning In Progress.");

        HousekeepingTask roomPopped = hkControl.rollbackLatestUpdateForRoom("101");
        assert "Dirty".equals(r101.getCleaningStatus()) : "FAIL: Per-room rollback failed!";
        System.out.println("  PASS: Per-room LIFO rollback reverted room 101 to Dirty.");

        // =====================================================================
        // Test 8: Guest Check-Out & Room Release
        // =====================================================================
        System.out.println("\n[Test 8] Guest Check-Out & Room Release:");
        String silverConf = walkInAlloc.getConfirmationNumber();

        // Check out Silver Stayer -> Room 103 released to Available & Dirty
        boolean checkedOut = walkInControl.checkOutGuest(silverConf);
        assert checkedOut                                              : "FAIL: Checkout should return true!";
        assert Guest.STATUS_CHECKED_OUT.equals(walkInAlloc.getStatus()): "FAIL: Status must be Checked-Out!";
        assert r103.isRoomAvailable()                                  : "FAIL: Room 103 must be available!";
        assert "Dirty".equals(r103.getCleaningStatus())                : "FAIL: Room 103 must be Dirty!";
        System.out.println("  PASS: Silver Stayer checked out -> Room 103 released as Available & Dirty.");

        // =====================================================================
        // Test 9: DoublyLinkedList ADT Operations (FIFO & LIFO)
        // =====================================================================
        System.out.println("\n[Test 9] DoublyLinkedList ADT Operations:");
        DoublyLinkedListInterface<String> dll = new DoublyLinkedList<>();
        dll.insertLast("Item1");
        dll.insertLast("Item2");
        dll.insertLast("Item3");

        assert "Item1".equals(dll.removeFirst()) : "FAIL: FIFO removeFirst failed!";
        assert "Item3".equals(dll.removeLast())  : "FAIL: LIFO removeLast failed!";
        assert dll.getNumberOfEntries() == 1     : "FAIL: Size should be 1!";
        assert "Item2".equals(dll.getEntry(0))   : "FAIL: Remaining entry must be Item2!";
        System.out.println("  PASS: DoublyLinkedList FIFO removeFirst and LIFO removeLast verified.");

        // =====================================================================
        // Test 10: Management Analytical Reports Generation
        // =====================================================================
        System.out.println("\n[Test 10] Analytical Management Reports Verification:");
        System.out.println(priorityControl.generateTierDistributionReport(null, null));
        System.out.println(priorityControl.generatePriorityWaitlistReport(null, null));
        walkInControl.printGuestCheckInReport(null, null);
        walkInControl.printQueueSummaryReport(null);

        System.out.println("\n>> ALL 10 COMPREHENSIVE INTEGRATION & WORKFLOW TESTS PASSED SUCCESSFULLY! <<\n");
    }
}
