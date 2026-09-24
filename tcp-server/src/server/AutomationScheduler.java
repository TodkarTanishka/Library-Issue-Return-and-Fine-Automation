package server;

import database.*;
import datastructure.fine.FineManager;
import model.*;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public class AutomationScheduler {
    private final LibraryEngine engine;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final List<Map<String, Object>> eventBuffer = Collections.synchronizedList(new ArrayList<>());
    private final Deque<Map<String, Object>> kpiSnapshots = new ConcurrentLinkedDeque<>();
    private final AtomicLong eventIdGen = new AtomicLong(1);
    private static final int MAX_EVENTS = 100;
    private static final int MAX_KPI_SNAPSHOTS = 60;

    public AutomationScheduler(LibraryEngine engine) {
        this.engine = engine;
    }

    public void start() {
        // Run periodic background tasks
        scheduler.scheduleAtFixedRate(this::overdueSweep, 10, 60, TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(this::dueReminders, 20, 300, TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(this::waitlistReaper, 5, 60, TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(this::kpiSnapshot, 2, 15, TimeUnit.SECONDS);
        logEvent("SCHEDULER_STARTED", "Automation Scheduler active with 4 background daemons.", "SYSTEM");
    }

    public void stop() {
        scheduler.shutdown();
    }

    public synchronized Map<String, Object> timeTravel(int days) {
        VirtualClock.advanceDays(days);
        String newDate = VirtualClock.getToday().toString();
        logEvent("TIME_TRAVEL", "Time Machine advanced virtual clock by " + days + " days. Current virtual date: " + newDate, "ADMIN");
        
        // Execute all automation sweeps immediately for the new date
        runAllJobsNow();

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("success", true);
        res.put("daysAdvanced", days);
        res.put("virtualDate", newDate);
        res.put("dayOffset", VirtualClock.getDayOffset());
        res.put("message", "Time Travel successful! System now executing as of " + newDate);
        return res;
    }

    public synchronized Map<String, Object> resetTimeTravel() {
        VirtualClock.reset();
        String date = VirtualClock.getToday().toString();
        logEvent("TIME_TRAVEL_RESET", "Virtual clock reset to system real time: " + date, "ADMIN");
        runAllJobsNow();
        
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("success", true);
        res.put("virtualDate", date);
        res.put("dayOffset", 0);
        res.put("message", "Time Travel reset to real time.");
        return res;
    }

    public void runAllJobsNow() {
        try {
            overdueSweep();
            dueReminders();
            waitlistReaper();
            kpiSnapshot();
        } catch (Exception e) {
            System.err.println("Error running immediate automation jobs: " + e.getMessage());
        }
    }

    public void overdueSweep() {
        try {
            List<Issue> activeIssues = engine.issueRepo().findAll();
            LocalDate today = VirtualClock.getToday();
            int newOverdues = 0;
            double totalFineAdded = 0.0;

            for (Issue i : activeIssues) {
                if ("ISSUED".equalsIgnoreCase(i.getStatus()) || "OVERDUE".equalsIgnoreCase(i.getStatus())) {
                    LocalDate due = LocalDate.parse(i.getDueDate());
                    if (today.isAfter(due)) {
                        long daysOverdue = java.time.temporal.ChronoUnit.DAYS.between(due, today);
                        double fineAmt = daysOverdue * 6.00;
                        
                        if ("ISSUED".equalsIgnoreCase(i.getStatus())) {
                            engine.issueRepo().updateStatus(i.getId(), "OVERDUE");
                            newOverdues++;
                        }
                        
                        // Upsert fine in database and in-memory heap
                        engine.fineRepo().upsertFine(i.getId(), i.getStudentUsername(), fineAmt, "UNPAID");
                        OverdueRecord rec = new OverdueRecord(i.getId(), i.getStudentUsername(), i.getBookId(), i.getDueDate(), fineAmt);
                        engine.fineManager().addOverdueRecord(rec);
                        totalFineAdded += fineAmt;
                    }
                }
            }
            if (newOverdues > 0) {
                logEvent("OVERDUE_SWEEP", "Overdue sweep detected " + newOverdues + " newly overdue books. Updated min-heap priority queue.", "AUTOMATION");
            }
        } catch (Exception e) {
            System.err.println("Overdue sweep error: " + e.getMessage());
        }
    }

    public void dueReminders() {
        try {
            List<Issue> activeIssues = engine.issueRepo().findAll();
            LocalDate today = VirtualClock.getToday();
            int remindersSent = 0;

            for (Issue i : activeIssues) {
                if ("ISSUED".equalsIgnoreCase(i.getStatus())) {
                    LocalDate due = LocalDate.parse(i.getDueDate());
                    long daysRemaining = java.time.temporal.ChronoUnit.DAYS.between(today, due);
                    if (daysRemaining >= 0 && daysRemaining <= 2) {
                        Book b = engine.bookSearch().searchBookById(i.getBookId());
                        String title = b == null ? i.getBookId() : b.getTitle();
                        engine.notificationRepo().create(
                            i.getStudentUsername(), "user", "DUE_REMINDER",
                            "Book Due Soon",
                            "Reminder: '" + title + "' is due in " + daysRemaining + " day(s) on " + i.getDueDate() + ".",
                            i.getBookId(), i.getId(), null, null
                        );
                        remindersSent++;
                    }
                }
            }
            if (remindersSent > 0) {
                logEvent("DUE_REMINDERS", "Sent " + remindersSent + " due date reminders to students.", "AUTOMATION");
            }
        } catch (Exception e) {
            System.err.println("Due reminders error: " + e.getMessage());
        }
    }

    public void waitlistReaper() {
        try {
            // Process expirations
            List<WaitlistEntry> expired = engine.waitlistRepo().expirePastReservations();
            for (WaitlistEntry w : expired) {
                logEvent("WAITLIST_EXPIRED", "Waitlist reservation for user " + w.getStudentUsername() + " on book " + w.getBookId() + " expired.", "AUTOMATION");
                engine.waitingListManager().removeFromWaitingList(w.getStudentUsername(), w.getBookId());
            }

            // Check for available copies with waiting users
            List<Book> books = engine.bookSearch().allBooks();
            for (Book b : books) {
                if (b.getAvailableQuantity() > 0) {
                    List<WaitlistEntry> waiting = engine.waitlistRepo().findWaitingByBook(b.getBookId());
                    if (!waiting.isEmpty()) {
                        WaitlistEntry head = waiting.get(0);
                        if ("WAITING".equalsIgnoreCase(head.getStatus())) {
                            engine.waitlistRepo().notifyNextWaitingUser(head.getId());
                            engine.notificationRepo().create(
                                head.getStudentUsername(), "user", "WAITLIST_PROMOTED",
                                "Waitlist Reservation Ready",
                                "Book '" + b.getTitle() + "' is available! Your 24-hour reservation window has started.",
                                b.getBookId(), null, null, head.getId()
                            );
                            logEvent("WAITLIST_PROMOTED", "Auto-promoted " + head.getStudentUsername() + " to head reservation for '" + b.getTitle() + "'.", "AUTOMATION");
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Waitlist reaper error: " + e.getMessage());
        }
    }

    public void kpiSnapshot() {
        try {
            Map<String, Object> snap = new LinkedHashMap<>();
            snap.put("timestamp", VirtualClock.getNow().toString());
            snap.put("virtualDate", VirtualClock.getToday().toString());
            snap.put("activeLoans", engine.issueRepo().findAll().stream().filter(i -> "ISSUED".equalsIgnoreCase(i.getStatus()) || "OVERDUE".equalsIgnoreCase(i.getStatus())).count());
            snap.put("overdueCount", engine.fineManager().getOverdueCount());
            snap.put("waitlistCount", engine.waitlistRepo().findAll().size());
            snap.put("activeSessions", engine.sessionManager().getActiveSessionCount());
            snap.put("dbActiveConnections", DBConnection.getActiveConnections());
            snap.put("dbIdleConnections", DBConnection.getIdleConnections());
            snap.put("tcpMetrics", engine.metrics().getMetricsSummaryMap());

            kpiSnapshots.addLast(snap);
            while (kpiSnapshots.size() > MAX_KPI_SNAPSHOTS) {
                kpiSnapshots.removeFirst();
            }
        } catch (Exception e) {
            System.err.println("KPI snapshot error: " + e.getMessage());
        }
    }

    public void logEvent(String type, String message, String source) {
        Map<String, Object> ev = new LinkedHashMap<>();
        ev.put("id", eventIdGen.getAndIncrement());
        ev.put("type", type);
        ev.put("message", message);
        ev.put("source", source);
        ev.put("virtualDate", VirtualClock.getToday().toString());
        ev.put("timestamp", VirtualClock.getNow().toString());
        
        eventBuffer.add(ev);
        while (eventBuffer.size() > MAX_EVENTS) {
            eventBuffer.remove(0);
        }
    }

    public List<Map<String, Object>> getEvents(long sinceId) {
        synchronized (eventBuffer) {
            List<Map<String, Object>> res = new ArrayList<>();
            for (Map<String, Object> ev : eventBuffer) {
                long id = ((Number) ev.get("id")).longValue();
                if (id > sinceId) {
                    res.add(ev);
                }
            }
            return res;
        }
    }

    public List<Map<String, Object>> getKpiHistory() {
        return new ArrayList<>(kpiSnapshots);
    }
}
