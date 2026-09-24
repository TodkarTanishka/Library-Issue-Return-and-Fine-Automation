package server;

import database.*;
import datastructure.book.BookSearch;
import datastructure.user.UserSearch;
import datastructure.issue.IssueManager;
import datastructure.fine.FineManager;
import kpi.KPIManager;
import model.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class LibraryEngine {
    private static final int MAX_ACTIVE_BOOKS = 2;
    private static final double FINE_RATE_PER_DAY = 6.00;

    private final UserRepository userRepo = new UserRepository();
    private final BookRepository bookRepo = new BookRepository();
    private final IssueRepository issueRepo = new IssueRepository();
    private final IssueRequestRepository requestRepo = new IssueRequestRepository();
    private final FineRepository fineRepo = new FineRepository();
    private final WaitlistRepository waitlistRepo = new WaitlistRepository();
    private final NotificationRepository notificationRepo = new NotificationRepository();
    private final AuditLogRepository auditRepo = new AuditLogRepository();

    private final UserSearch userSearch = new UserSearch();
    private final BookSearch bookSearch = new BookSearch();
    private final IssueManager issueManager = new IssueManager();
    private final FineManager fineManager = new FineManager();
    private final KPIManager kpiManager = new KPIManager();
    private final ServerMetrics metrics;
    private final SessionManager sessionManager = new SessionManager();
    private final AutomationScheduler automationScheduler = new AutomationScheduler(this);
    private final Map<String, User> specialUsers = new HashMap<>();

    private final java.util.concurrent.locks.ReentrantReadWriteLock rwLock = new java.util.concurrent.locks.ReentrantReadWriteLock();
    private final java.util.concurrent.ConcurrentHashMap<String, java.util.concurrent.locks.ReentrantLock> bookLocks = new java.util.concurrent.ConcurrentHashMap<>();

    private java.util.concurrent.locks.ReentrantLock getBookLock(String bookId) {
        return bookLocks.computeIfAbsent(bookId == null ? "GLOBAL" : bookId.toLowerCase(), k -> new java.util.concurrent.locks.ReentrantLock());
    }

    public SessionManager sessionManager() { return sessionManager; }
    public AuditLogRepository auditRepo() { return auditRepo; }
    public UserRepository userRepo() { return userRepo; }
    public BookRepository bookRepo() { return bookRepo; }
    public IssueRepository issueRepo() { return issueRepo; }
    public FineRepository fineRepo() { return fineRepo; }
    public WaitlistRepository waitlistRepo() { return waitlistRepo; }
    public NotificationRepository notificationRepo() { return notificationRepo; }
    public FineManager fineManager() { return fineManager; }
    public datastructure.issue.WaitingListManager waitingListManager() { return issueManager.getWaitingListManager(); }
    public BookSearch bookSearch() { return bookSearch; }
    public AutomationScheduler automationScheduler() { return automationScheduler; }
    public ServerMetrics metrics() { return metrics; }

    public LibraryEngine() { this(new ServerMetrics()); }
    public LibraryEngine(ServerMetrics metrics) { this.metrics = metrics; }

    public synchronized void initialize() throws Exception {
        DBConnection.ensureSchema();
        userRepo.migratePlaintextPasswords();
        userSearch.clearIfSupported();
        List<User> users = userRepo.findAll();
        for (User u : users) {
            userSearch.addUser(u);
            issueManager.addUser(u);
        }
        User admin = userRepo.findAdmin("MMADMIN01", "admin123");
        if (admin != null) specialUsers.put(admin.getEmail(), admin);

        List<Book> books = bookRepo.findAll();
        for (Book b : books) {
            bookSearch.addBook(b);
            issueManager.addBook(b);
        }
        issueManager.setIssues(issueRepo.findAll());
        
        // Sync waitlists from database into DSA queue
        List<WaitlistEntry> activeWait = waitlistRepo.findAll();
        for (WaitlistEntry w : activeWait) {
            if ("WAITING".equalsIgnoreCase(w.getStatus()) || "NOTIFIED".equalsIgnoreCase(w.getStatus())) {
                issueManager.getWaitingListManager().addToWaitingList(w.getStudentUsername(), w.getBookId());
            }
        }
        
        processWaitlistExpirations();
        refreshOverdueStatus();
        automationScheduler.start();
    }

    private void createNotification(String recipientEmail, String recipientRole, String type, String title, String message,
                                    String bookId, Integer loanId, Integer requestId, Integer waitlistId) {
        try {
            notificationRepo.create(recipientEmail, recipientRole, type, title, message, bookId, loanId, requestId, waitlistId);
        } catch (Exception e) {
            System.err.println("Failed to create notification: " + e.getMessage());
        }
    }

    private synchronized void processWaitlistExpirations() {
        try {
            List<WaitlistEntry> expiredList = waitlistRepo.expirePastReservations();
            for (WaitlistEntry w : expiredList) {
                Book b = bookSearch.searchBookById(w.getBookId());
                String title = b == null ? w.getBookId() : b.getTitle();
                createNotification(w.getStudentUsername(), "user", "WAITLIST_EXPIRED",
                        "Waitlist Reservation Expired",
                        "Your reservation window for '" + title + "' has expired.",
                        w.getBookId(), null, null, w.getId());

                // Notify next waiting user if available
                List<WaitlistEntry> nextWaiting = waitlistRepo.findWaitingByBook(w.getBookId());
                if (!nextWaiting.isEmpty()) {
                    WaitlistEntry next = nextWaiting.get(0);
                    if ("WAITING".equalsIgnoreCase(next.getStatus())) {
                        waitlistRepo.notifyNextWaitingUser(next.getId());
                        createNotification(next.getStudentUsername(), "user", "BOOK_AVAILABLE",
                                "Book Available - Claim Your Reservation",
                                "Good news! The book '" + title + "' is now available. You are next in the waitlist. Submit your issue request within 24 hours.",
                                next.getBookId(), null, null, next.getId());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error processing waitlist expirations: " + e.getMessage());
        }
    }

    public synchronized User login(String id, String password, String role, String loginType) throws Exception {
        if ("admin".equalsIgnoreCase(loginType)) {
            User u = "admin".equalsIgnoreCase(role) ? userRepo.findAdmin(id, password) : userRepo.findStaff(id, password, "librarian");
            return u;
        }
        User u = userSearch.login(id, password);
        if (u != null && u.getPassword() != null && u.getPassword().startsWith("$pbkdf2$")) {
            userRepo.updateUserPassword(u.getEmail(), u.getPassword());
        }
        return u;
    }

    public synchronized List<Book> searchBooks(String search, String department) {
        processWaitlistExpirations();
        List<Book> all = bookSearch.allBooks();
        if (search != null && !search.isBlank()) {
            String q = search.trim().toLowerCase();
            List<Book> result = new ArrayList<>();
            for (Book b : all) {
                boolean titleMatch = b.getTitle() != null && b.getTitle().toLowerCase().contains(q);
                boolean authorMatch = b.getAuthor() != null && b.getAuthor().toLowerCase().contains(q);
                boolean isbnMatch = b.getIsbn() != null && b.getIsbn().toLowerCase().contains(q);
                boolean idMatch = b.getBookId() != null && b.getBookId().toLowerCase().contains(q);
                boolean catMatch = b.getCategory() != null && b.getCategory().toLowerCase().contains(q);
                boolean deptMatch = b.getDepartment() != null && b.getDepartment().toLowerCase().contains(q);
                if (titleMatch || authorMatch || isbnMatch || idMatch || catMatch || deptMatch) {
                    result.add(b);
                }
            }
            all = result;
        }
        if (department != null && !department.equalsIgnoreCase("all"))
            all.removeIf(b -> !department.equalsIgnoreCase(b.getDepartment()));
        return all;
    }

    public synchronized List<Map<String, Object>> loans(String email) throws Exception {
        refreshOverdueStatus();
        List<Issue> issues = (email == null || email.isBlank()) ? issueManager.getIssues() : issueRepo.findByUser(email);
        Map<String, Book> books = new HashMap<>();
        for (Book b : bookSearch.allBooks()) books.put(b.getBookId(), b);
        List<Map<String, Object>> out = new ArrayList<>();
        for (Issue i : issues) {
            Book b = books.get(i.getBookId());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", i.getId());
            m.put("user_id", i.getStudentUsername());
            m.put("book_id", i.getBookId());
            m.put("book_title", b == null ? "Engineering Textbook" : b.getTitle());
            m.put("book_author", b == null ? "Author" : b.getAuthor());
            m.put("issue_date", i.getIssueDate());
            m.put("due_date", i.getDueDate());
            m.put("status", i.getStatus().toLowerCase());
            m.put("fine_amount", fine(i.getDueDate()));
            out.add(m);
        }
        return out;
    }

    private double fine(String due) {
        return fineManager.calculateFine(due, LocalDate.now().toString());
    }

    private void refreshOverdueStatus() throws Exception {
        fineRepo.processOverdueFinesProcedure();
        issueManager.setIssues(issueRepo.findAll());
    }

    // Direct issue endpoint is disabled to enforce strict librarian approval workflow
    public synchronized Map<String, Object> directIssue(String email, String bookId) throws Exception {
        return response(false, "Direct book issuance is disabled. Please submit an issue request for librarian approval.");
    }

    // USER operation: creates a request only.
    public synchronized Map<String, Object> requestIssue(String email, String bookId) throws Exception {
        processWaitlistExpirations();
        User u = userSearch.searchUser(email);
        if (u == null) return response(false, "User not found.");
        Book b = bookSearch.searchBookById(bookId);
        if (b == null) return response(false, "Book not found.");
        int active = issueManager.getUserActiveIssueCount(email);
        int pending = requestRepo.countActiveOrPending(email);
        if (active + pending >= MAX_ACTIVE_BOOKS)
            return response(false, "Borrowing limit reached: a user may have at most 2 active/pending books.");
        if (issueManager.findActiveIssue(email, bookId) != null)
            return response(false, "This book is already issued to you.");
        if (requestRepo.hasPending(email, bookId))
            return response(false, "A request for this book is already pending.");

        int id = requestRepo.create(email, bookId);
        createNotification(email, "user", "ISSUE_REQUESTED", "Issue Request Submitted",
                "Your request for '" + b.getTitle() + "' has been submitted for librarian approval.",
                bookId, null, id, null);
        createNotification(null, "librarian", "NEW_ISSUE_REQUEST", "New Issue Request Received",
                "New book request received from " + email + " for '" + b.getTitle() + "'.",
                bookId, null, id, null);

        return response(true, "Issue request submitted. Waiting for librarian approval. Request #" + id);
    }

    // USER operation: submits multiple requests
    public synchronized Map<String, Object> requestIssues(String email, List<String> bookIds) throws Exception {
        processWaitlistExpirations();
        User u = userSearch.searchUser(email);
        if (u == null) return response(false, "User not found.");
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        if (bookIds != null) for (String id : bookIds) if (id != null && !id.isBlank()) unique.add(id.trim());
        if (unique.isEmpty()) return response(false, "Select at least one book.");
        if (unique.size() > MAX_ACTIVE_BOOKS) return response(false, "You can request a maximum of 2 books at a time.");

        int active = issueManager.getUserActiveIssueCount(email);
        int pending = requestRepo.countActiveOrPending(email);
        if (active + pending + unique.size() > MAX_ACTIVE_BOOKS)
            return response(false, "Book limit exceeded. You can have at most 2 active/pending books.");

        for (String bookId : unique) {
            Book b = bookSearch.searchBookById(bookId);
            if (b == null) return response(false, "Book not found: " + bookId);
            if (issueManager.findActiveIssue(email, bookId) != null) return response(false, "Already issued: " + b.getTitle());
            if (requestRepo.hasPending(email, bookId)) return response(false, "Request already pending: " + b.getTitle());
        }
        List<Integer> ids = requestRepo.createBatch(email, new ArrayList<>(unique));
        for (int i = 0; i < ids.size(); i++) {
            String bId = new ArrayList<>(unique).get(i);
            Book b = bookSearch.searchBookById(bId);
            String title = b == null ? bId : b.getTitle();
            createNotification(email, "user", "ISSUE_REQUESTED", "Issue Request Submitted",
                    "Your request for '" + title + "' has been submitted.", bId, null, ids.get(i), null);
            createNotification(null, "librarian", "NEW_ISSUE_REQUEST", "New Issue Request Received",
                    "New request from " + email + " for '" + title + "'.", bId, null, ids.get(i), null);
        }
        return response(true, ids.size() + " issue request(s) submitted to the librarian. Request IDs: " + ids);
    }

    // LIBRARIAN operation: approves a pending request and creates the actual loan.
    public synchronized Map<String, Object> approveIssue(int requestId, String librarianId) throws Exception {
        User librarian = userRepo.findLibrarianByEmail(librarianId);
        if (librarian == null) return response(false, "Only an authenticated librarian can approve requests.");
        IssueRequest r = requestRepo.find(requestId);
        if (r == null) return response(false, "Request not found.");
        if (!"PENDING".equalsIgnoreCase(r.getStatus())) return response(false, "Request is already decided.");
        User u = userSearch.searchUser(r.getUserEmail());
        Book b = bookSearch.searchBookById(r.getBookId());
        if (u == null || b == null) {
            requestRepo.decide(requestId, "REJECTED", librarianId, "User or book no longer exists.");
            return response(false, "Request rejected: user/book not found.");
        }
        if (issueManager.getUserActiveIssueCount(r.getUserEmail()) >= MAX_ACTIVE_BOOKS) {
            requestRepo.decide(requestId, "REJECTED", librarianId, "Two-book active limit reached.");
            return response(false, "Request rejected: user already has 2 active books.");
        }
        if (issueManager.findActiveIssue(r.getUserEmail(), r.getBookId()) != null) {
            requestRepo.decide(requestId, "REJECTED", librarianId, "Duplicate active issue.");
            return response(false, "Request rejected: book already issued to this user.");
        }
        
        // Check if copy is available or reserved for this user via waitlist
        boolean isNotifiedForUser = false;
        List<WaitlistEntry> userWait = waitlistRepo.findByUser(r.getUserEmail());
        for (WaitlistEntry w : userWait) {
            if (w.getBookId().equalsIgnoreCase(r.getBookId()) && "NOTIFIED".equalsIgnoreCase(w.getStatus())) {
                isNotifiedForUser = true;
                break;
            }
        }

        if (b.getAvailableQuantity() <= 0 && !isNotifiedForUser) {
            requestRepo.decide(requestId, "REJECTED", librarianId, "No copy available.");
            return response(false, "Request rejected: no available copy.");
        }

        LocalDate d = LocalDate.now();
        Issue temp = new Issue(0, r.getUserEmail(), r.getBookId(), d.toString(), d.plusDays(14).toString(), "Issued");
        issueManager.addIssue(temp);
        b.setAvailableQuantity(Math.max(0, b.getAvailableQuantity() - 1));
        int dbId = issueRepo.insert(r.getUserEmail(), r.getBookId(), temp.getIssueDate(), temp.getDueDate());
        if (dbId < 0) {
            b.setAvailableQuantity(b.getAvailableQuantity() + 1);
            issueManager.removeLastIssue();
            requestRepo.decide(requestId, "REJECTED", librarianId, "Database insert failed; transaction rolled back.");
            return response(false, "Issue failed: database insert error. No stock was consumed.");
        }
        requestRepo.decide(requestId, "APPROVED", librarianId, "Approved and issued.");
        bookRepo.updateAvailable(r.getBookId(), b.getAvailableQuantity());
        issueManager.setIssues(issueRepo.findAll());

        // Update waitlist entry status to FULFILLED if user was waiting
        for (WaitlistEntry w : userWait) {
            if (w.getBookId().equalsIgnoreCase(r.getBookId()) && ("WAITING".equalsIgnoreCase(w.getStatus()) || "NOTIFIED".equalsIgnoreCase(w.getStatus()))) {
                waitlistRepo.updateStatus(w.getId(), "FULFILLED");
                waitlistRepo.recalculatePositionsProcedure(r.getBookId());
            }
        }

        createNotification(r.getUserEmail(), "user", "ISSUE_APPROVED", "Issue Request Approved",
                "Your request for '" + b.getTitle() + "' has been approved! Return Due Date: " + temp.getDueDate() + ".",
                r.getBookId(), dbId, requestId, null);

        return response(true, "Request #" + requestId + " approved. Book issued to " + r.getUserEmail() + ".");
    }

    public synchronized Map<String, Object> rejectIssue(int requestId, String librarianId, String reason) throws Exception {
        User librarian = userRepo.findLibrarianByEmail(librarianId);
        if (librarian == null) return response(false, "Only an authenticated librarian can reject requests.");
        IssueRequest r = requestRepo.find(requestId);
        if (r == null) return response(false, "Request not found.");
        if (!"PENDING".equalsIgnoreCase(r.getStatus())) return response(false, "Request is already decided.");
        String rejReason = (reason == null || reason.isBlank()) ? "Rejected by librarian." : reason;
        requestRepo.decide(requestId, "REJECTED", librarianId, rejReason);

        Book b = bookSearch.searchBookById(r.getBookId());
        String title = b == null ? r.getBookId() : b.getTitle();
        createNotification(r.getUserEmail(), "user", "ISSUE_REJECTED", "Issue Request Rejected",
                "Your request for '" + title + "' was rejected. Reason: " + rejReason,
                r.getBookId(), null, requestId, null);

        return response(true, "Request #" + requestId + " rejected.");
    }

    public synchronized List<Map<String, Object>> pendingRequests(String librarianId) throws Exception {
        if (userRepo.findLibrarianByEmail(librarianId) == null)
            throw new SecurityException("Only an authenticated librarian can view pending issue requests.");
        List<Map<String, Object>> out = new ArrayList<>();
        Map<String, Book> books = new HashMap<>();
        for (Book b : bookSearch.allBooks()) books.put(b.getBookId(), b);
        for (IssueRequest r : requestRepo.findPending()) {
            Book b = books.get(r.getBookId());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.getId());
            m.put("user_id", r.getUserEmail());
            m.put("book_id", r.getBookId());
            m.put("book_title", b == null ? "Unknown" : b.getTitle());
            m.put("requested_at", r.getRequestedAt());
            m.put("status", r.getStatus());
            out.add(m);
        }
        return out;
    }

    public synchronized List<Map<String, Object>> myRequests(String email) throws Exception {
        List<Map<String, Object>> out = new ArrayList<>();
        Map<String, Book> books = new HashMap<>();
        for (Book b : bookSearch.allBooks()) books.put(b.getBookId(), b);
        for (IssueRequest r : requestRepo.findAll()) {
            if (email != null && !email.equalsIgnoreCase(r.getUserEmail())) continue;
            Book b = books.get(r.getBookId());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.getId());
            m.put("book_id", r.getBookId());
            m.put("book_title", b == null ? "Unknown" : b.getTitle());
            m.put("requested_at", r.getRequestedAt());
            m.put("status", r.getStatus());
            m.put("decided_at", r.getDecidedAt());
            m.put("reason", r.getDecisionReason());
            out.add(m);
        }
        return out;
    }

    // STEP 1: USER initiates Return Request (status becomes RETURN_REQUESTED, awaiting librarian approval)
    public synchronized Map<String, Object> returnBook(int loanId, String email) throws Exception {
        Issue dbIssue = issueRepo.find(loanId);
        if (dbIssue == null) return response(false, "Loan not found.");
        
        // Strict Security Check: Only the borrower can request return for their own loan
        if (email == null || email.isBlank() || !dbIssue.getStudentUsername().equalsIgnoreCase(email.trim())) {
            return response(false, "Access Denied: Only the borrowing student can submit a return request for their loan.");
        }

        if ("Returned".equalsIgnoreCase(dbIssue.getStatus())) {
            return response(false, "This book has already been returned.");
        }
        if ("RETURN_REQUESTED".equalsIgnoreCase(dbIssue.getStatus()) || "Return_Requested".equalsIgnoreCase(dbIssue.getStatus())) {
            return response(false, "A return request for this loan is already pending librarian approval.");
        }

        // Set status to RETURN_REQUESTED in DB
        issueRepo.updateStatus(loanId, "RETURN_REQUESTED");
        Book b = bookSearch.searchBookById(dbIssue.getBookId());
        String title = (b == null ? dbIssue.getBookId() : b.getTitle());

        createNotification(email, "user", "RETURN_REQUESTED", "Return Request Submitted",
                "Your return request for '" + title + "' has been submitted. Awaiting librarian verification and approval.",
                dbIssue.getBookId(), loanId, null, null);
        createNotification(null, "librarian", "NEW_RETURN_REQUEST", "New Return Request Received",
                "Student " + email + " submitted a return request for '" + title + "' (Loan #" + loanId + ").",
                dbIssue.getBookId(), loanId, null, null);

        return response(true, "Return request submitted successfully. Awaiting librarian verification.");
    }

    // STEP 2: LIBRARIAN Approves Return Request
    public synchronized Map<String, Object> approveReturn(int loanId, String librarianId) throws Exception {
        User librarian = userRepo.findLibrarianByEmail(librarianId);
        if (librarian == null) return response(false, "Only an authenticated librarian can approve return requests.");
        Issue dbIssue = issueRepo.find(loanId);
        if (dbIssue == null) return response(false, "Loan not found.");
        if (!"RETURN_REQUESTED".equalsIgnoreCase(dbIssue.getStatus()) && !"Return_Requested".equalsIgnoreCase(dbIssue.getStatus())) {
            return response(false, "This loan does not have a pending return request.");
        }

        Book b = bookSearch.searchBookById(dbIssue.getBookId());
        if (!issueManager.returnBook(dbIssue.getStudentUsername(), dbIssue.getBookId())) {
            // Retry syncing if state mismatch
            issueManager.setIssues(issueRepo.findAll());
            issueManager.returnBook(dbIssue.getStudentUsername(), dbIssue.getBookId());
        }

        issueRepo.updateStatus(loanId, "Returned");

        // Restore copy stock (+1)
        if (b != null) {
            b.setAvailableQuantity(b.getAvailableQuantity() + 1);
            bookRepo.updateAvailable(b.getBookId(), b.getAvailableQuantity());
        }

        // Calculate final fine if overdue
        LocalDate due = LocalDate.parse(dbIssue.getDueDate());
        LocalDate now = LocalDate.now();
        int overdueDays = (int) Math.max(0, ChronoUnit.DAYS.between(due, now));
        double fineAmount = overdueDays * FINE_RATE_PER_DAY;

        if (overdueDays > 0) {
            fineRepo.createOrUpdateFine(loanId, dbIssue.getStudentUsername(), dbIssue.getBookId(), dbIssue.getDueDate(), now.toString(), overdueDays, fineAmount);
            createNotification(dbIssue.getStudentUsername(), "user", "FINE_GENERATED", "Fine Accrued",
                    "A fine of ₹" + String.format("%.2f", fineAmount) + " has been generated for overdue return of '" + (b == null ? dbIssue.getBookId() : b.getTitle()) + "'.",
                    dbIssue.getBookId(), loanId, null, null);
        }

        issueManager.setIssues(issueRepo.findAll());

        createNotification(dbIssue.getStudentUsername(), "user", "RETURN_APPROVED", "Book Return Approved",
                "Your return request for '" + (b == null ? dbIssue.getBookId() : b.getTitle()) + "' has been verified and approved by the librarian.",
                dbIssue.getBookId(), loanId, null, null);

        // Process waitlist notification: notify earliest WAITING user
        List<WaitlistEntry> waiting = waitlistRepo.findWaitingByBook(dbIssue.getBookId());
        String waitlistMsg = "";
        if (!waiting.isEmpty()) {
            WaitlistEntry next = waiting.get(0);
            waitlistRepo.notifyNextWaitingUser(next.getId());
            createNotification(next.getStudentUsername(), "user", "BOOK_AVAILABLE", "Book Available - Claim Reservation",
                    "Good news! The book '" + (b == null ? dbIssue.getBookId() : b.getTitle()) + "' is now available. You are next in the waitlist. Submit your issue request within 24 hours.",
                    dbIssue.getBookId(), null, null, next.getId());
            waitlistMsg = " Next waiting user (" + next.getStudentUsername() + ") has been notified.";
        }

        return response(true, "Return request #" + loanId + " approved successfully. Book stock updated." + (overdueDays > 0 ? " Fine accrued: ₹" + String.format("%.2f", fineAmount) + "." : "") + waitlistMsg);
    }

    // STEP 3: LIBRARIAN Rejects Return Request
    public synchronized Map<String, Object> rejectReturn(int loanId, String librarianId, String reason) throws Exception {
        User librarian = userRepo.findLibrarianByEmail(librarianId);
        if (librarian == null) return response(false, "Only an authenticated librarian can reject return requests.");
        Issue dbIssue = issueRepo.find(loanId);
        if (dbIssue == null) return response(false, "Loan not found.");
        if (!"RETURN_REQUESTED".equalsIgnoreCase(dbIssue.getStatus()) && !"Return_Requested".equalsIgnoreCase(dbIssue.getStatus())) {
            return response(false, "This loan does not have a pending return request.");
        }

        LocalDate due = LocalDate.parse(dbIssue.getDueDate());
        boolean isOverdue = LocalDate.now().isAfter(due);
        String restoredStatus = isOverdue ? "Overdue" : "Issued";

        issueRepo.updateStatus(loanId, restoredStatus);

        Book b = bookSearch.searchBookById(dbIssue.getBookId());
        String title = (b == null ? dbIssue.getBookId() : b.getTitle());
        String rejReason = (reason == null || reason.isBlank()) ? "Rejected by librarian." : reason;

        createNotification(dbIssue.getStudentUsername(), "user", "RETURN_REJECTED", "Return Request Rejected",
                "Your return request for '" + title + "' was rejected. Reason: " + rejReason,
                dbIssue.getBookId(), loanId, null, null);

        return response(true, "Return request #" + loanId + " rejected. Loan status restored to " + restoredStatus + ".");
    }

    // Fetch Pending Return Requests for Librarian
    public synchronized List<Map<String, Object>> pendingReturns(String librarianId) throws Exception {
        if (userRepo.findLibrarianByEmail(librarianId) == null)
            throw new SecurityException("Only an authenticated librarian can view pending return requests.");
        List<Map<String, Object>> out = new ArrayList<>();
        Map<String, Book> books = new HashMap<>();
        for (Book b : bookSearch.allBooks()) books.put(b.getBookId(), b);
        for (Issue i : issueRepo.findAll()) {
            if ("RETURN_REQUESTED".equalsIgnoreCase(i.getStatus()) || "Return_Requested".equalsIgnoreCase(i.getStatus())) {
                Book b = books.get(i.getBookId());
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", i.getId());
                m.put("user_id", i.getStudentUsername());
                m.put("book_id", i.getBookId());
                m.put("book_title", b == null ? "Unknown" : b.getTitle());
                m.put("issue_date", i.getIssueDate());
                m.put("due_date", i.getDueDate());
                m.put("status", i.getStatus());
                out.add(m);
            }
        }
        return out;
    }

    // WAITLIST MANAGEMENT
    public synchronized Map<String, Object> joinWaitlist(String email, String bookId) throws Exception {
        processWaitlistExpirations();
        User u = userSearch.searchUser(email);
        if (u == null) return response(false, "User not found.");
        Book b = bookSearch.searchBookById(bookId);
        if (b == null) return response(false, "Book not found.");
        
        // Check if user is notified for this book
        boolean isNotified = false;
        for (WaitlistEntry w : waitlistRepo.findByUser(email)) {
            if (w.getBookId().equalsIgnoreCase(bookId) && "NOTIFIED".equalsIgnoreCase(w.getStatus())) {
                isNotified = true;
                break;
            }
        }
        
        if (b.getAvailableQuantity() > 0 && !isNotified)
            return response(false, "Book has available copies. You can submit an issue request directly.");
        if (issueManager.findActiveIssue(email, bookId) != null)
            return response(false, "You already have an active loan for this book.");
        if (waitlistRepo.hasActiveWaitlist(email, bookId))
            return response(false, "You are already on the waitlist for this book.");

        int id = waitlistRepo.add(email, bookId);
        if (id > 0) {
            issueManager.getWaitingListManager().addToWaitingList(email, bookId);
            List<WaitlistEntry> list = waitlistRepo.findWaitingByBook(bookId);
            int pos = list.size();
            for (WaitlistEntry w : list) {
                if (w.getStudentUsername().equalsIgnoreCase(email)) {
                    pos = w.getPosition();
                    break;
                }
            }
            createNotification(email, "user", "WAITLIST_JOINED", "Joined Waitlist",
                    "You joined the waitlist for '" + b.getTitle() + "'. Current position: #" + pos + ".",
                    bookId, null, null, id);

            return response(true, "Successfully joined waitlist for '" + b.getTitle() + "'. Your queue position is #" + pos);
        }
        return response(false, "Unable to join waitlist.");
    }

    public synchronized Map<String, Object> leaveWaitlist(int waitlistId, String email) throws Exception {
        WaitlistEntry target = null;
        for (WaitlistEntry w : waitlistRepo.findByUser(email)) {
            if (w.getId() == waitlistId) {
                target = w;
                break;
            }
        }
        if (target == null) return response(false, "Waitlist entry not found.");
        if (!"WAITING".equalsIgnoreCase(target.getStatus()) && !"NOTIFIED".equalsIgnoreCase(target.getStatus())) {
            return response(false, "Cannot cancel a waitlist entry with status: " + target.getStatus());
        }

        String bookId = target.getBookId();
        boolean ok = waitlistRepo.cancelWaitlist(waitlistId, email);
        if (ok) {
            Book b = bookSearch.searchBookById(bookId);
            String title = b == null ? bookId : b.getTitle();
            createNotification(email, "user", "WAITLIST_CANCELLED", "Waitlist Cancelled",
                    "You have left the waitlist for '" + title + "'.", bookId, null, null, waitlistId);

            // Check if position changed for remaining waiting users
            List<WaitlistEntry> remaining = waitlistRepo.findWaitingByBook(bookId);
            for (WaitlistEntry r : remaining) {
                if ("WAITING".equalsIgnoreCase(r.getStatus())) {
                    createNotification(r.getStudentUsername(), "user", "POSITION_CHANGED", "Waitlist Position Updated",
                            "Your position for '" + title + "' updated to #" + r.getPosition() + ".",
                            bookId, null, null, r.getId());
                }
            }
            return response(true, "You have left the waitlist for '" + title + "'.");
        }
        return response(false, "Failed to cancel waitlist entry.");
    }

    public synchronized List<Map<String, Object>> myWaitlists(String email) throws Exception {
        processWaitlistExpirations();
        List<Map<String, Object>> out = new ArrayList<>();
        Map<String, Book> books = new HashMap<>();
        for (Book b : bookSearch.allBooks()) books.put(b.getBookId(), b);
        for (WaitlistEntry w : waitlistRepo.findByUser(email)) {
            Book b = books.get(w.getBookId());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", w.getId());
            m.put("book_id", w.getBookId());
            m.put("book_title", b == null ? "Unknown" : b.getTitle());
            m.put("joined_at", w.getJoinedAt());
            m.put("position", w.getPosition());
            m.put("status", w.getStatus());
            m.put("expires_at", w.getExpiresAt());
            out.add(m);
        }
        return out;
    }

    public synchronized List<Map<String, Object>> waitlists(String bookId) throws Exception {
        processWaitlistExpirations();
        List<Map<String, Object>> out = new ArrayList<>();
        Map<String, Book> books = new HashMap<>();
        for (Book b : bookSearch.allBooks()) books.put(b.getBookId(), b);
        List<WaitlistEntry> list = (bookId != null && !bookId.isBlank()) ? waitlistRepo.findWaitingByBook(bookId) : waitlistRepo.findAll();
        for (WaitlistEntry w : list) {
            Book b = books.get(w.getBookId());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", w.getId());
            m.put("user_id", w.getStudentUsername());
            m.put("book_id", w.getBookId());
            m.put("book_title", b == null ? "Unknown" : b.getTitle());
            m.put("position", w.getPosition());
            m.put("status", w.getStatus());
            m.put("joined_at", w.getJoinedAt());
            m.put("expires_at", w.getExpiresAt());
            out.add(m);
        }
        return out;
    }

    // NOTIFICATIONS API
    public synchronized List<Map<String, Object>> getNotifications(String email, String role) throws Exception {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Notification n : notificationRepo.findByUser(email, role)) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", n.getId());
            m.put("type", n.getType());
            m.put("title", n.getTitle());
            m.put("message", n.getMessage());
            m.put("book_id", n.getRelatedBookId());
            m.put("is_read", n.isRead());
            m.put("created_at", n.getCreatedAt());
            out.add(m);
        }
        return out;
    }

    public synchronized Map<String, Object> markNotificationRead(int id, String email) throws Exception {
        boolean ok = notificationRepo.markAsRead(id, email);
        return response(ok, ok ? "Notification marked as read." : "Notification not found.");
    }

    public synchronized Map<String, Object> markAllNotificationsRead(String email, String role) throws Exception {
        boolean ok = notificationRepo.markAllAsRead(email, role);
        return response(ok, ok ? "All notifications marked as read." : "No unread notifications.");
    }

    public synchronized Map<String, Object> countUnreadNotifications(String email, String role) throws Exception {
        int count = notificationRepo.countUnread(email, role);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("success", true);
        m.put("unreadCount", count);
        return m;
    }

    // FINE SETTLEMENT & MANAGEMENT
    public synchronized List<Map<String, Object>> myFines(String email) throws Exception {
        List<Map<String, Object>> out = new ArrayList<>();
        Map<String, Book> books = new HashMap<>();
        for (Book b : bookSearch.allBooks()) books.put(b.getBookId(), b);
        for (FineRecord f : fineRepo.findByUser(email)) {
            Book b = books.get(f.getBookId());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", f.getId());
            m.put("loan_id", f.getLoanId());
            m.put("book_id", f.getBookId());
            m.put("book_title", b == null ? "Unknown" : b.getTitle());
            m.put("due_date", f.getDueDate());
            m.put("return_date", f.getReturnDate());
            m.put("overdue_days", f.getOverdueDays());
            m.put("fine_amount", f.getFineAmount());
            m.put("paid_amount", f.getPaidAmount());
            m.put("remaining_amount", f.getRemainingAmount());
            m.put("status", f.getStatus());
            out.add(m);
        }
        return out;
    }

    public synchronized List<Map<String, Object>> allFines(String librarianId) throws Exception {
        if (userRepo.findLibrarianByEmail(librarianId) == null && userRepo.findAdminByEmail(librarianId) == null)
            throw new SecurityException("Only authorized personnel can view fine records.");
        List<Map<String, Object>> out = new ArrayList<>();
        Map<String, Book> books = new HashMap<>();
        for (Book b : bookSearch.allBooks()) books.put(b.getBookId(), b);
        for (FineRecord f : fineRepo.findAll()) {
            Book b = books.get(f.getBookId());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", f.getId());
            m.put("loan_id", f.getLoanId());
            m.put("user_id", f.getStudentUsername());
            m.put("book_id", f.getBookId());
            m.put("book_title", b == null ? "Unknown" : b.getTitle());
            m.put("due_date", f.getDueDate());
            m.put("return_date", f.getReturnDate());
            m.put("overdue_days", f.getOverdueDays());
            m.put("fine_amount", f.getFineAmount());
            m.put("paid_amount", f.getPaidAmount());
            m.put("remaining_amount", f.getRemainingAmount());
            m.put("status", f.getStatus());
            m.put("payment_method", f.getPaymentMethod());
            m.put("payment_reference", f.getPaymentReference());
            out.add(m);
        }
        return out;
    }

    public synchronized Map<String, Object> settleFine(int fineId, double amountPaid, String paymentMethod, String paymentRef, String librarianId) throws Exception {
        User librarian = userRepo.findLibrarianByEmail(librarianId);
        if (librarian == null) return response(false, "Only an authenticated librarian can settle fines.");
        if (amountPaid <= 0) return response(false, "Payment amount must be greater than zero.");
        FineRecord f = fineRepo.findById(fineId);
        if (f == null) return response(false, "Fine record not found.");
        if ("PAID".equalsIgnoreCase(f.getStatus()) || f.getRemainingAmount() <= 0)
            return response(false, "This fine has already been fully paid.");

        boolean ok = fineRepo.recordPayment(fineId, amountPaid, paymentMethod, paymentRef, librarianId);
        if (ok) {
            FineRecord updated = fineRepo.findById(fineId);
            createNotification(f.getStudentUsername(), "user", "FINE_PAID", "Fine Payment Recorded",
                    "Payment of ₹" + String.format("%.2f", amountPaid) + " recorded. Remaining balance: ₹" + String.format("%.2f", updated.getRemainingAmount()),
                    f.getBookId(), f.getLoanId(), null, null);
            return response(true, "Payment of ₹" + String.format("%.2f", amountPaid) + " recorded successfully. Remaining balance: ₹" + String.format("%.2f", updated.getRemainingAmount()));
        }
        return response(false, "Failed to record payment.");
    }

    public synchronized List<Map<String, Object>> usersDirectory(String adminEmail) throws Exception {
        if (userRepo.findAdminByEmail(adminEmail) == null)
            throw new SecurityException("Only an authenticated admin can view the member directory.");
        return userRepo.findAllDirectory();
    }

    public synchronized Map<String, Object> stats() throws Exception {
        refreshOverdueStatus();
        List<Issue> issues = issueManager.getIssues();
        List<Book> books = bookSearch.allBooks();
        List<User> users = userRepo.findAll();
        kpiManager.setData(issues, books, users);

        double fines = 0;
        for (Issue i : issues) fines += fine(i.getDueDate());
        for (FineRecord f : fineRepo.findAll()) fines += f.getRemainingAmount();

        Map<String, Object> s = new LinkedHashMap<>();
        s.put("totalTitles", books.size());
        int copies = 0;
        for (Book b : books) copies += b.getTotalQuantity();
        s.put("totalCopies", copies);
        s.put("totalMembers", users.size());
        s.put("activeLoans", kpiManager.getActiveIssues());
        s.put("overdueCount", kpiManager.getOverdueBooks());
        s.put("totalFines", fines);
        s.put("totalBooksIssued", kpiManager.getTotalBooksIssued());
        s.put("zeroCopyBooks", kpiManager.getZeroCopyBooks());
        s.put("activeConnections", metrics.getActiveConnections());
        s.put("peakConcurrentUsers", metrics.getPeakConcurrentUsers());
        s.put("totalTcpRequests", metrics.getTotalRequests());
        s.put("failedTcpRequests", metrics.getFailedRequests());
        s.put("averageResponseMs", metrics.getAverageLatencyMs());
        s.put("maxResponseMs", metrics.getMaxLatencyMs());
        
        List<IssueRequest> req = requestRepo.findAll();
        int pending = 0, approved = 0, rejected = 0;
        long decisionMs = 0;
        int decided = 0;
        for (IssueRequest r : req) {
            if ("PENDING".equals(r.getStatus())) pending++;
            else if ("APPROVED".equals(r.getStatus())) approved++;
            else if ("REJECTED".equals(r.getStatus())) rejected++;
            if (r.getDecidedAt() != null && r.getRequestedAt() != null) {
                try {
                    decisionMs += Duration.between(Instant.parse(r.getRequestedAt()), Instant.parse(r.getDecidedAt())).toMillis();
                    decided++;
                } catch (Exception ignored) {}
            }
        }
        int totalDecided = approved + rejected;
        s.put("issueRequests", req.size());
        s.put("pendingIssueRequests", pending);
        s.put("approvedRequests", approved);
        s.put("rejectedRequests", rejected);
        s.put("approvalRatePercent", totalDecided == 0 ? 0.0 : (approved * 100.0 / totalDecided));
        s.put("rejectionRatePercent", totalDecided == 0 ? 0.0 : (rejected * 100.0 / totalDecided));
        s.put("averageApprovalDecisionMs", decided == 0 ? 0.0 : (double) decisionMs / decided);
        s.put("bookLimitPerUser", MAX_ACTIVE_BOOKS);
        return s;
    }

    public synchronized Map<String, Object> addBook(String role, String librarianId, String isbn, String title, String author, String category, String department, int copies) throws Exception {
        if (!"librarian".equalsIgnoreCase(role) && !"staff".equalsIgnoreCase(role)) {
            return response(false, "Access Denied: Only authenticated librarians are permitted to add new books.");
        }
        if (title == null || title.isBlank() || isbn == null || isbn.isBlank()) {
            return response(false, "Title and ISBN are required fields.");
        }
        if (copies <= 0) {
            return response(false, "Total quantity must be at least 1.");
        }
        String[] res = bookRepo.addBookProcedure(isbn, title, author, category, department, copies);
        boolean success = Boolean.parseBoolean(res[0]);
        String msg = res[1];
        if (success) {
            List<Book> all = bookRepo.findAll();
            bookSearch.clear();
            for (Book b : all) {
                bookSearch.addBook(b);
                issueManager.addBook(b);
            }
            createNotification(null, "librarian", "INVENTORY_ADDED", "New Book Added",
                    "Book '" + title + "' (ISBN: " + isbn + ") added to inventory.", null, null, null, null);
        }
        return response(success, msg);
    }

    public synchronized Map<String, Object> updateBook(String role, String librarianId, String bookId, String title, String author, String isbn, String category, String department, int newTotal) throws Exception {
        if (!"librarian".equalsIgnoreCase(role) && !"staff".equalsIgnoreCase(role)) {
            return response(false, "Access Denied: Only authenticated librarians are permitted to edit book details.");
        }
        if (bookId == null || bookId.isBlank() || title == null || title.isBlank()) {
            return response(false, "Book ID and Title are required.");
        }
        if (newTotal <= 0) {
            return response(false, "Total quantity must be at least 1.");
        }
        String[] res = bookRepo.updateBookProcedure(bookId, title, author, isbn, category, department, newTotal);
        boolean success = Boolean.parseBoolean(res[0]);
        String msg = res[1];
        if (success) {
            List<Book> all = bookRepo.findAll();
            bookSearch.clear();
            for (Book b : all) {
                bookSearch.addBook(b);
                issueManager.addBook(b);
            }
        }
        return response(success, msg);
    }

    public synchronized Map<String, Object> deleteBook(String role, String librarianId, String bookId) throws Exception {
        if (!"librarian".equalsIgnoreCase(role) && !"staff".equalsIgnoreCase(role)) {
            return response(false, "Access Denied: Only authenticated librarians are permitted to delete books.");
        }
        if (bookId == null || bookId.isBlank()) {
            return response(false, "Book ID is required for deletion.");
        }
        String[] res = bookRepo.deleteBookProcedure(bookId);
        boolean success = Boolean.parseBoolean(res[0]);
        String msg = res[1];
        if (success) {
            List<Book> all = bookRepo.findAll();
            bookSearch.clear();
            for (Book b : all) {
                bookSearch.addBook(b);
                issueManager.addBook(b);
            }
        }
        return response(success, msg);
    }

    public Map<String, Object> autocompleteSuggestions(String query, int limit) {
        rwLock.readLock().lock();
        try {
            Map<String, Integer> popularity = new HashMap<>();
            for (Book b : bookSearch.allBooks()) {
                popularity.put(b.getBookId(), kpiManager.getBookIssueCount(b.getBookId()));
            }
            List<Book> matches = bookSearch.autocompleteBooks(query, limit <= 0 ? 8 : limit, popularity);
            List<Map<String, Object>> suggestions = new ArrayList<>();
            for (Book b : matches) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", b.getId());
                m.put("book_id", b.getBookId());
                m.put("title", b.getTitle());
                m.put("author", b.getAuthor());
                m.put("category", b.getCategory());
                m.put("department", b.getDepartment());
                m.put("available_copies", b.getAvailableQuantity());
                m.put("popularity", popularity.getOrDefault(b.getBookId(), 0));
                suggestions.add(m);
            }
            Map<String, Object> res = new LinkedHashMap<>();
            res.put("success", true);
            res.put("query", query == null ? "" : query);
            res.put("structure", "Trie");
            res.put("complexity", "O(K)");
            res.put("suggestions", suggestions);
            return res;
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public Map<String, Object> searchBooksDetailed(String search, String department) {
        rwLock.readLock().lock();
        try {
            Map<String, Object> meta = new LinkedHashMap<>();
            List<Book> books = bookSearch.searchBooksWithMetadata(search, department, meta);
            List<Map<String, Object>> list = new ArrayList<>();
            for (Book b : books) {
                Map<String, Object> x = new LinkedHashMap<>();
                x.put("id", b.getId());
                x.put("book_id", b.getBookId());
                x.put("title", b.getTitle());
                x.put("author", b.getAuthor());
                x.put("isbn", b.getIsbn());
                x.put("category", b.getCategory());
                x.put("department", b.getDepartment());
                x.put("rack_number", b.getRackNumber());
                x.put("total_copies", b.getTotalQuantity());
                x.put("available_copies", b.getAvailableQuantity());
                list.add(x);
            }
            Map<String, Object> res = new LinkedHashMap<>();
            res.put("success", true);
            res.put("books", list);
            res.put("meta", meta);
            return res;
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public List<Map<String, Object>> urgentOverdue(int limit) {
        rwLock.readLock().lock();
        try {
            List<OverdueRecord> top = fineManager.topK(limit <= 0 ? 10 : limit);
            List<Map<String, Object>> list = new ArrayList<>();
            for (OverdueRecord r : top) {
                Book b = bookSearch.searchBookById(r.getBookId());
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("loan_id", r.getLoanId());
                m.put("student_username", r.getStudentUsername());
                m.put("book_id", r.getBookId());
                m.put("book_title", b == null ? r.getBookId() : b.getTitle());
                m.put("due_date", r.getDueDate());
                m.put("fine_amount", r.getFine());
                list.add(m);
            }
            return list;
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public Map<String, Object> failureSimulation(String librarianId) throws Exception { return new FailureModeSimulator(this).run(librarianId); }
    
    public Map<String, Object> getDsaState() {
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("success", true);
        
        Map<String, Object> trieState = new LinkedHashMap<>();
        trieState.put("structure", "Hand-Written Trie (Map<Character, TrieNode>)");
        trieState.put("totalWords", bookSearch.getTrieWordCount());
        trieState.put("totalBooksIndexed", bookSearch.allBooks().size());
        trieState.put("timeComplexity", "O(K) where K is prefix length");
        res.put("trie", trieState);

        Map<String, Object> heapState = new LinkedHashMap<>();
        heapState.put("structure", "Hand-Written Binary Min-Heap Priority Queue");
        heapState.put("size", fineManager.getOverdueCount());
        heapState.put("topK", fineManager.topK(5));
        heapState.put("timeComplexity", "O(1) peek, O(log N) insert/remove/decreaseKey");
        res.put("minHeap", heapState);

        Map<String, Object> queueState = new LinkedHashMap<>();
        queueState.put("structure", "Hand-Written Doubly Linked List FIFO Queue");
        queueState.put("totalWaiting", waitingListManager().getWaitlistCount());
        queueState.put("timeComplexity", "O(1) enqueue/dequeue, O(N) middle removal");
        res.put("waitlistQueue", queueState);

        Map<String, Object> userSearchState = new LinkedHashMap<>();
        userSearchState.put("structure", "Hand-Written Hash Table with Chaining");
        userSearchState.put("totalUsers", userSearch.getUserCount());
        userSearchState.put("timeComplexity", "O(1) average lookup");
        res.put("userHashTable", userSearchState);

        Map<String, Object> lockState = new LinkedHashMap<>();
        lockState.put("readWriteLock", rwLock.toString());
        lockState.put("perBookLockCount", bookLocks.size());
        lockState.put("timeComplexity", "Fine-grained per-entity synchronization");
        res.put("concurrencyLocks", lockState);

        return res;
    }

    public Map<String, Object> runBenchmarks() {
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("success", true);

        // Benchmark 1: Trie Search O(K) vs Table Scan
        long t1 = System.nanoTime();
        for (int i = 0; i < 500; i++) {
            autocompleteSuggestions("struct", 8);
        }
        long t2 = System.nanoTime();
        double trieAvgMicros = (t2 - t1) / 500.0 / 1000.0;

        long t3 = System.nanoTime();
        for (int i = 0; i < 500; i++) {
            try {
                List<Book> all = bookRepo.findAll();
                all.removeIf(b -> !b.getTitle().toLowerCase().contains("struct"));
            } catch (Exception ignored) {}
        }
        long t4 = System.nanoTime();
        double scanAvgMicros = (t4 - t3) / 500.0 / 1000.0;

        Map<String, Object> trieBench = new LinkedHashMap<>();
        trieBench.put("trieMicrosPerOp", Math.round(trieAvgMicros * 100.0) / 100.0);
        trieBench.put("linearScanMicrosPerOp", Math.round(scanAvgMicros * 100.0) / 100.0);
        trieBench.put("speedupFactor", Math.round((scanAvgMicros / Math.max(trieAvgMicros, 0.001)) * 10.0) / 10.0 + "x");
        res.put("searchBenchmark", trieBench);

        // Benchmark 2: Min-Heap Top Overdue vs SQL fetch
        long h1 = System.nanoTime();
        for (int i = 0; i < 500; i++) {
            fineManager.getMostUrgentOverdue();
        }
        long h2 = System.nanoTime();
        double heapAvgMicros = (h2 - h1) / 500.0 / 1000.0;

        long s1 = System.nanoTime();
        for (int i = 0; i < 500; i++) {
            try {
                fineRepo.findAll();
            } catch (Exception ignored) {}
        }
        long s2 = System.nanoTime();
        double dbAvgMicros = (s2 - s1) / 500.0 / 1000.0;

        Map<String, Object> heapBench = new LinkedHashMap<>();
        heapBench.put("heapMicrosPerOp", Math.round(heapAvgMicros * 100.0) / 100.0);
        heapBench.put("sqlOrderByMicrosPerOp", Math.round(dbAvgMicros * 100.0) / 100.0);
        heapBench.put("speedupFactor", Math.round((dbAvgMicros / Math.max(heapAvgMicros, 0.001)) * 10.0) / 10.0 + "x");
        res.put("heapBenchmark", heapBench);

        return res;
    }

    public Map<String, Object> generateReceipt(int loanId) {
        Map<String, Object> res = new LinkedHashMap<>();
        try {
            Issue issue = issueRepo.findById(loanId);
            if (issue == null) {
                res.put("success", false);
                res.put("message", "Loan record not found.");
                return res;
            }
            Book b = bookSearch.searchBookById(issue.getBookId());
            User u = userSearch.searchUser(issue.getStudentUsername());
            FineRecord fine = fineRepo.findByLoanId(loanId);

            res.put("success", true);
            res.put("receiptId", "REC-" + String.format("%06d", loanId));
            res.put("loanId", loanId);
            res.put("studentEmail", issue.getStudentUsername());
            res.put("studentName", u != null ? u.getFullName() : issue.getStudentUsername());
            res.put("bookId", issue.getBookId());
            res.put("bookTitle", b != null ? b.getTitle() : issue.getBookId());
            res.put("issueDate", issue.getIssueDate());
            res.put("dueDate", issue.getDueDate());
            res.put("returnDate", VirtualClock.getToday().toString());
            res.put("fineAmount", fine != null ? fine.getAmount() : fineManager.calculateCurrentFine(issue.getDueDate()));
            res.put("fineStatus", fine != null ? fine.getStatus() : "PAID");
            res.put("paymentMethod", fine != null && fine.getPaymentMethod() != null ? fine.getPaymentMethod() : "CASH");
            res.put("issuedBy", "MMCOE Library Operations");
            res.put("timestamp", VirtualClock.getNow().toString());
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", e.getMessage());
        }
        return res;
    }

    private Map<String, Object> response(boolean ok, String msg) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("success", ok);
        m.put("message", msg);
        return m;
    }
}
