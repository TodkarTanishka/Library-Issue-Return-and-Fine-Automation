package datastructure.fine;

import model.OverdueRecord;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class FineManager {
    private static final double FINE_PER_DAY = 6.00;
    private OverdueRecord[] heap = new OverdueRecord[16];
    private final Map<Integer, Integer> loanIndexMap = new HashMap<>();
    private int size = 0;

    public double calculateFine(String dueDate, String returnDate) {
        if (dueDate == null || returnDate == null) return 0.0;
        try {
            long d = ChronoUnit.DAYS.between(LocalDate.parse(dueDate), LocalDate.parse(returnDate));
            return d <= 0 ? 0.0 : (double) d * FINE_PER_DAY;
        } catch (Exception e) {
            return 0.0;
        }
    }

    public double calculateCurrentFine(String dueDate) {
        if (dueDate == null) return 0.0;
        return calculateFine(dueDate, model.VirtualClock.getToday().toString());
    }

    public boolean isOverdue(String dueDate) {
        if (dueDate == null) return false;
        try {
            return model.VirtualClock.getToday().isAfter(LocalDate.parse(dueDate));
        } catch (Exception e) {
            return false;
        }
    }

    public synchronized void addOverdueRecord(OverdueRecord record) {
        if (record == null) return;
        Integer existingIdx = loanIndexMap.get(record.getLoanId());
        if (existingIdx != null) {
            heap[existingIdx] = record;
            heapifyUp(existingIdx);
            heapifyDown(existingIdx);
            return;
        }

        if (size == heap.length) {
            OverdueRecord[] next = new OverdueRecord[heap.length * 2];
            System.arraycopy(heap, 0, next, 0, size);
            heap = next;
        }
        heap[size] = record;
        loanIndexMap.put(record.getLoanId(), size);
        heapifyUp(size);
        size++;
    }

    public synchronized OverdueRecord getMostUrgentOverdue() {
        return size == 0 ? null : heap[0];
    }

    public synchronized OverdueRecord removeMostUrgentOverdue() {
        if (size == 0) return null;
        OverdueRecord root = heap[0];
        loanIndexMap.remove(root.getLoanId());
        
        size--;
        if (size > 0) {
            heap[0] = heap[size];
            loanIndexMap.put(heap[0].getLoanId(), 0);
            heap[size] = null;
            heapifyDown(0);
        } else {
            heap[0] = null;
        }
        return root;
    }

    public synchronized boolean removeByLoanId(int loanId) {
        Integer idx = loanIndexMap.get(loanId);
        if (idx == null) return false;

        loanIndexMap.remove(loanId);
        size--;
        if (idx == size) {
            heap[size] = null;
            return true;
        }

        heap[idx] = heap[size];
        loanIndexMap.put(heap[idx].getLoanId(), idx);
        heap[size] = null;

        heapifyUp(idx);
        heapifyDown(idx);
        return true;
    }

    public synchronized List<OverdueRecord> topK(int k) {
        List<OverdueRecord> result = new ArrayList<>();
        if (size == 0 || k <= 0) return result;

        // Clone heap array and map to do non-destructive extraction
        OverdueRecord[] copy = Arrays.copyOf(heap, size);
        int tempSize = size;
        
        for (int i = 0; i < k && tempSize > 0; i++) {
            OverdueRecord min = copy[0];
            result.add(min);
            tempSize--;
            if (tempSize > 0) {
                copy[0] = copy[tempSize];
                copy[tempSize] = null;
                heapifyDownArray(copy, tempSize, 0);
            }
        }
        return result;
    }

    private void heapifyUp(int i) {
        while (i > 0) {
            int p = (i - 1) / 2;
            if (compare(heap[i], heap[p]) >= 0) break;
            swap(i, p);
            i = p;
        }
    }

    private void heapifyDown(int i) {
        while (true) {
            int left = 2 * i + 1;
            int right = 2 * i + 2;
            int smallest = i;

            if (left < size && compare(heap[left], heap[smallest]) < 0) smallest = left;
            if (right < size && compare(heap[right], heap[smallest]) < 0) smallest = right;
            if (smallest == i) break;

            swap(i, smallest);
            i = smallest;
        }
    }

    private void heapifyDownArray(OverdueRecord[] arr, int n, int i) {
        while (true) {
            int left = 2 * i + 1;
            int right = 2 * i + 2;
            int smallest = i;

            if (left < n && compare(arr[left], arr[smallest]) < 0) smallest = left;
            if (right < n && compare(arr[right], arr[smallest]) < 0) smallest = right;
            if (smallest == i) break;

            OverdueRecord temp = arr[i];
            arr[i] = arr[smallest];
            arr[smallest] = temp;
            i = smallest;
        }
    }

    private int compare(OverdueRecord a, OverdueRecord b) {
        int dateCompare = a.getDueDate().compareTo(b.getDueDate());
        if (dateCompare != 0) return dateCompare;
        return Double.compare(b.getFine(), a.getFine()); // fine DESC
    }

    private void swap(int i, int j) {
        OverdueRecord temp = heap[i];
        heap[i] = heap[j];
        heap[j] = temp;
        loanIndexMap.put(heap[i].getLoanId(), i);
        loanIndexMap.put(heap[j].getLoanId(), j);
    }

    public synchronized int getOverdueCount() { return size; }
    public synchronized void clear() { size = 0; loanIndexMap.clear(); Arrays.fill(heap, null); }
}
