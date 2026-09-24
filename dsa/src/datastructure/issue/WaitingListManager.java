package datastructure.issue;

import java.util.*;

public class WaitingListManager {
    private static class QueueNode {
        String email;
        QueueNode next;
        QueueNode(String e) { email = e; }
    }

    private static class BookQueue {
        String bookId;
        QueueNode front, rear;
        BookQueue(String id) { bookId = id; }
    }

    private BookQueue[] queues = new BookQueue[16];
    private int queueCount = 0;

    private BookQueue findQueue(String id) {
        if (id == null) return null;
        for (int i = 0; i < queueCount; i++) {
            if (queues[i].bookId.equalsIgnoreCase(id)) return queues[i];
        }
        return null;
    }

    private BookQueue createQueue(String id) {
        if (queueCount == queues.length) {
            queues = Arrays.copyOf(queues, queues.length * 2);
        }
        return queues[queueCount++] = new BookQueue(id);
    }

    public synchronized void addToWaitingList(String email, String bookId) {
        if (email == null || bookId == null) return;
        BookQueue q = findQueue(bookId);
        if (q == null) q = createQueue(bookId);

        for (QueueNode c = q.front; c != null; c = c.next) {
            if (c.email.equalsIgnoreCase(email)) return;
        }

        QueueNode n = new QueueNode(email);
        if (q.front == null) {
            q.front = q.rear = n;
        } else {
            q.rear.next = n;
            q.rear = n;
        }
    }

    public synchronized boolean removeFromMiddle(String email, String bookId) {
        BookQueue q = findQueue(bookId);
        if (q == null || q.front == null || email == null) return false;

        QueueNode cur = q.front, prev = null;
        while (cur != null) {
            if (cur.email.equalsIgnoreCase(email)) {
                if (prev == null) {
                    q.front = cur.next;
                    if (q.front == null) q.rear = null;
                } else {
                    prev.next = cur.next;
                    if (cur == q.rear) q.rear = prev;
                }
                return true;
            }
            prev = cur;
            cur = cur.next;
        }
        return false;
    }

    public synchronized String getNextWaitingUser(String bookId) {
        BookQueue q = findQueue(bookId);
        if (q == null || q.front == null) return null;
        String e = q.front.email;
        q.front = q.front.next;
        if (q.front == null) q.rear = null;
        return e;
    }

    public synchronized String peekNextWaitingUser(String bookId) {
        BookQueue q = findQueue(bookId);
        return (q == null || q.front == null) ? null : q.front.email;
    }

    public synchronized boolean isUserWaiting(String email, String bookId) {
        BookQueue q = findQueue(bookId);
        if (q == null || email == null) return false;
        for (QueueNode c = q.front; c != null; c = c.next) {
            if (c.email.equalsIgnoreCase(email)) return true;
        }
        return false;
    }

    public synchronized int getUserPosition(String email, String bookId) {
        BookQueue q = findQueue(bookId);
        if (q == null || email == null) return -1;
        int pos = 1;
        for (QueueNode c = q.front; c != null; c = c.next) {
            if (c.email.equalsIgnoreCase(email)) return pos;
            pos++;
        }
        return -1;
    }

    public synchronized int getWaitingCount(String bookId) {
        BookQueue q = findQueue(bookId);
        int n = 0;
        for (QueueNode c = (q == null ? null : q.front); c != null; c = c.next) n++;
        return n;
    }

    public synchronized List<String> getWaitingUsers(String bookId) {
        List<String> r = new ArrayList<>();
        BookQueue q = findQueue(bookId);
        for (QueueNode c = (q == null ? null : q.front); c != null; c = c.next) r.add(c.email);
        return r;
    }

    public synchronized void clear() {
        Arrays.fill(queues, null);
        queueCount = 0;
    }

    public synchronized boolean removeFromWaitingList(String email, String bookId) {
        return removeFromMiddle(email, bookId);
    }

    public synchronized int getWaitlistCount() {
        int total = 0;
        for (int i = 0; i < queueCount; i++) {
            if (queues[i] != null) {
                for (QueueNode c = queues[i].front; c != null; c = c.next) total++;
            }
        }
        return total;
    }
}
