package tools;

import server.LibraryEngine;
import server.ServerMetrics;
import model.Book;
import model.User;
import database.BookRepository;
import database.UserRepository;
import database.IssueRequestRepository;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public class ConcurrencyRaceTest {
    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("   MMCOE LIBRARY: 50-THREAD CONCURRENCY RACE TEST");
        System.out.println("==================================================");

        try {
            ServerMetrics metrics = new ServerMetrics();
            LibraryEngine engine = new LibraryEngine(metrics);
            engine.initialize();

            BookRepository bookRepo = new BookRepository();
            Book testBook = null;
            for (Book b : bookRepo.findAll()) {
                if ("Concurrency Race Test Book".equals(b.getTitle())) {
                    testBook = b;
                    break;
                }
            }
            if (testBook == null) {
                String randomIsbn = "978-" + (1000000000L + (long)(Math.random() * 8999999999L));
                bookRepo.insert(randomIsbn, "Concurrency Race Test Book", "Race Author", "Computer Engineering", 1);
                for (Book b : bookRepo.findAll()) {
                    if ("Concurrency Race Test Book".equals(b.getTitle())) {
                        testBook = b;
                        break;
                    }
                }
            }
            String testBookId = testBook != null ? testBook.getBookId() : "CS-101";
            if (testBook != null) {
                bookRepo.updateAvailable(testBookId, 1);
            }

            int threadCount = 50;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(threadCount);

            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failureCount = new AtomicInteger(0);

            for (int i = 0; i < threadCount; i++) {
                final String userEmail = "race_user_" + i + "@mmcoe.edu.in";
                executor.submit(() -> {
                    try {
                        startLatch.await(); // Synchronized start signal
                        Map<String, Object> reqRes = engine.requestIssue(userEmail, testBookId);
                        if (Boolean.TRUE.equals(reqRes.get("success"))) {
                            String msg = (String) reqRes.get("message");
                            int reqId = extractRequestId(msg);
                            if (reqId > 0) {
                                Map<String, Object> appRes = engine.approveIssue(reqId, "staff1@mmcoe.edu.in");
                                if (Boolean.TRUE.equals(appRes.get("success"))) {
                                    successCount.incrementAndGet();
                                } else {
                                    failureCount.incrementAndGet();
                                }
                            }
                        } else {
                            failureCount.incrementAndGet();
                        }
                    } catch (Exception e) {
                        failureCount.incrementAndGet();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            long startTime = System.currentTimeMillis();
            startLatch.countDown(); // Fire all 50 threads at once
            boolean finished = doneLatch.await(10, TimeUnit.SECONDS);
            executor.shutdown();

            long elapsed = System.currentTimeMillis() - startTime;
            Book finalBook = bookRepo.findByBookId(testBookId);
            int finalStock = finalBook == null ? 0 : finalBook.getAvailableQuantity();

            System.out.println("Threads Executed       : " + threadCount);
            System.out.println("Time Elapsed           : " + elapsed + " ms");
            System.out.println("Successful Approvals   : " + successCount.get());
            System.out.println("Rejected / Blocked     : " + failureCount.get());
            System.out.println("Final Book Copy Stock  : " + finalStock);

            boolean pass = finished && successCount.get() <= 1 && finalStock >= 0;
            System.out.println("--------------------------------------------------");
            if (pass) {
                System.out.println("RESULT: PASS — Zero stock underflow, atomic locks verified!");
                System.out.println("==================================================\n");
                System.exit(0);
            } else {
                System.out.println("RESULT: FAIL — Race condition detected or deadlock occurred!");
                System.exit(1);
            }

        } catch (Exception e) {
            System.err.println("Race test failed with exception: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static int extractRequestId(String msg) {
        if (msg == null) return -1;
        try {
            int idx = msg.indexOf("#");
            if (idx >= 0) {
                String num = msg.substring(idx + 1).replaceAll("[^0-9]", "");
                return Integer.parseInt(num);
            }
        } catch (Exception ignored) {}
        return -1;
    }
}
