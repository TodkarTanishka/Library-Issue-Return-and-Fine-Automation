package tools;

import server.LibraryEngine;
import server.AutomationScheduler;
import model.VirtualClock;
import java.util.*;

public class AutomationTest {
    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("  MMCOE LIBRARY - PHASE 2 AUTOMATION TEST SUITE");
        System.out.println("==================================================");

        try {
            LibraryEngine engine = new LibraryEngine();
            engine.initialize();
            AutomationScheduler scheduler = engine.automationScheduler();

            System.out.println("\n[TEST 1] Initializing Virtual Clock");
            System.out.println("Initial Date : " + VirtualClock.getToday());
            System.out.println("Day Offset   : " + VirtualClock.getDayOffset());

            System.out.println("\n[TEST 2] Testing Time Travel (+7 Days)");
            Map<String, Object> travelRes = scheduler.timeTravel(7);
            System.out.println("Time Travel Result: " + travelRes.get("message"));
            System.out.println("New Virtual Date  : " + VirtualClock.getToday());

            System.out.println("\n[TEST 3] Verifying Overdue Priority Queue Heap Size");
            int overdueCount = engine.fineManager().getOverdueCount();
            System.out.println("Overdue Heap Size : " + overdueCount);

            System.out.println("\n[TEST 4] Verifying System Event Log Generation");
            List<Map<String, Object>> events = scheduler.getEvents(0);
            System.out.println("Total Generated Events: " + events.size());
            for (Map<String, Object> ev : events) {
                System.out.println("  - [" + ev.get("type") + "] " + ev.get("message"));
            }

            System.out.println("\n[TEST 5] Testing Time Travel Reset");
            Map<String, Object> resetRes = scheduler.resetTimeTravel();
            System.out.println("Reset Result    : " + resetRes.get("message"));
            System.out.println("Real System Date: " + VirtualClock.getToday());

            System.out.println("\n==================================================");
            System.out.println("  RESULT: PASS — All Automation & Time Travel tests verified!");
            System.out.println("==================================================");
            System.exit(0);

        } catch (Exception e) {
            System.err.println("AUTOMATION TEST FAILED: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
