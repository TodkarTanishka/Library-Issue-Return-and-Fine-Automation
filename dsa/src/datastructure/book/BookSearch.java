package datastructure.book;

import model.Book;
import java.util.*;

public class BookSearch {
    private static final int TABLE_SIZE = 101;
    private final Trie titleTrie = new Trie();
    private final BookIdEntry[] bookIdTable = new BookIdEntry[TABLE_SIZE];
    private final AuthorEntry[] authorTable = new AuthorEntry[TABLE_SIZE];
    private final CategoryEntry[] categoryTable = new CategoryEntry[TABLE_SIZE];
    private int size;

    private static class BookIdEntry {
        String bookId; Book book; BookIdEntry next;
        BookIdEntry(String i, Book b) { bookId = i; book = b; }
    }

    private static class AuthorEntry {
        String author; List<Book> books = new ArrayList<>(); AuthorEntry next;
        AuthorEntry(String a) { author = a; }
    }

    private static class CategoryEntry {
        String category; List<Book> books = new ArrayList<>(); CategoryEntry next;
        CategoryEntry(String c) { category = c; }
    }

    private int hash(String v) {
        if (v == null) return 0;
        int h = 0;
        for (int i = 0; i < v.length(); i++) h = 31 * h + Character.toLowerCase(v.charAt(i));
        return Math.abs(h) % TABLE_SIZE;
    }

    public synchronized void addBook(Book book) {
        if (book == null || book.getBookId() == null) return;
        String key = book.getBookId().trim().toLowerCase();
        int idx = hash(key);
        BookIdEntry c = bookIdTable[idx];
        while (c != null) {
            if (c.bookId.equals(key)) {
                c.book = book;
                return;
            }
            c = c.next;
        }
        BookIdEntry n = new BookIdEntry(key, book);
        n.next = bookIdTable[idx];
        bookIdTable[idx] = n;

        if (book.getTitle() != null) titleTrie.insert(book.getTitle(), book.getBookId());
        if (book.getAuthor() != null) {
            titleTrie.insert(book.getAuthor(), book.getBookId());
            addAuthor(book.getAuthor(), book);
        }
        if (book.getCategory() != null) {
            titleTrie.insert(book.getCategory(), book.getBookId());
            addCategory(book.getCategory(), book);
        }
        size++;
    }

    public synchronized void removeBook(String bookId) {
        if (bookId == null) return;
        Book book = searchBookById(bookId);
        if (book == null) return;

        String key = bookId.trim().toLowerCase();
        int idx = hash(key);
        BookIdEntry cur = bookIdTable[idx], prev = null;
        while (cur != null) {
            if (cur.bookId.equals(key)) {
                if (prev == null) bookIdTable[idx] = cur.next;
                else prev.next = cur.next;
                break;
            }
            prev = cur; cur = cur.next;
        }

        if (book.getTitle() != null) titleTrie.remove(book.getTitle(), bookId);
        if (book.getAuthor() != null) titleTrie.remove(book.getAuthor(), bookId);
        if (book.getCategory() != null) titleTrie.remove(book.getCategory(), bookId);
        size = Math.max(0, size - 1);
    }

    private void addAuthor(String a, Book b) {
        String k = a.trim().toLowerCase();
        int i = hash(k);
        AuthorEntry c = authorTable[i];
        while (c != null) {
            if (c.author.equals(k)) {
                if (!c.books.contains(b)) c.books.add(b);
                return;
            }
            c = c.next;
        }
        AuthorEntry n = new AuthorEntry(k);
        n.books.add(b);
        n.next = authorTable[i];
        authorTable[i] = n;
    }

    private void addCategory(String a, Book b) {
        String k = a.trim().toLowerCase();
        int i = hash(k);
        CategoryEntry c = categoryTable[i];
        while (c != null) {
            if (c.category.equals(k)) {
                if (!c.books.contains(b)) c.books.add(b);
                return;
            }
            c = c.next;
        }
        CategoryEntry n = new CategoryEntry(k);
        n.books.add(b);
        n.next = categoryTable[i];
        categoryTable[i] = n;
    }

    public synchronized Book searchBookById(String id) {
        if (id == null || id.trim().isEmpty()) return null;
        String k = id.trim().toLowerCase();
        BookIdEntry c = bookIdTable[hash(k)];
        while (c != null) {
            if (c.bookId.equals(k)) return c.book;
            c = c.next;
        }
        return null;
    }

    public synchronized Set<String> searchByTitlePrefix(String prefix) {
        return titleTrie.searchByPrefix(prefix);
    }

    public synchronized List<Book> autocompleteBooks(String prefix, int limit, Map<String, Integer> popularityMap) {
        List<String> matchedIds = titleTrie.autocomplete(prefix, limit, popularityMap);
        List<Book> result = new ArrayList<>();
        for (String id : matchedIds) {
            Book b = searchBookById(id);
            if (b != null) result.add(b);
        }
        return result;
    }

    public synchronized List<Book> searchBooksWithMetadata(String query, String department, Map<String, Object> outMetadata) {
        long startTime = System.nanoTime();
        Set<Book> resultSet = new LinkedHashSet<>();
        String usedStructure = "Hash Table (O(1))";
        String complexity = "O(1)";
        int candidatesScanned = 0;

        if (query != null && !query.isBlank()) {
            String q = query.trim().toLowerCase();
            
            // 1. Direct Hash Table Match by Book ID (O(1))
            Book directIdMatch = searchBookById(q);
            if (directIdMatch != null) {
                resultSet.add(directIdMatch);
                candidatesScanned++;
                usedStructure = "Hash Table (Book ID)";
                complexity = "O(1)";
            } else {
                // 2. Trie Prefix Lookup (O(K))
                Set<String> trieBookIds = titleTrie.searchByPrefix(q);
                if (!trieBookIds.isEmpty()) {
                    usedStructure = "Trie + Hash Table";
                    complexity = "O(K)";
                    for (String bId : trieBookIds) {
                        Book b = searchBookById(bId);
                        if (b != null) resultSet.add(b);
                        candidatesScanned++;
                    }
                } else {
                    // 3. Chain fallback scanning
                    usedStructure = "Hash Chain / Linear Scan Fallback";
                    complexity = "O(N)";
                    for (Book b : allBooks()) {
                        candidatesScanned++;
                        boolean titleMatch = b.getTitle() != null && b.getTitle().toLowerCase().contains(q);
                        boolean authorMatch = b.getAuthor() != null && b.getAuthor().toLowerCase().contains(q);
                        boolean isbnMatch = b.getIsbn() != null && b.getIsbn().toLowerCase().contains(q);
                        if (titleMatch || authorMatch || isbnMatch) {
                            resultSet.add(b);
                        }
                    }
                }
            }
        } else {
            resultSet.addAll(allBooks());
            candidatesScanned = resultSet.size();
        }

        List<Book> resultList = new ArrayList<>(resultSet);
        if (department != null && !department.equalsIgnoreCase("all")) {
            resultList.removeIf(b -> !department.equalsIgnoreCase(b.getDepartment()));
        }

        long elapsedMicros = (System.nanoTime() - startTime) / 1000L;
        if (outMetadata != null) {
            outMetadata.put("structure", usedStructure);
            outMetadata.put("complexity", complexity);
            outMetadata.put("timeMicros", elapsedMicros);
            outMetadata.put("candidatesScanned", candidatesScanned);
            outMetadata.put("totalBooksIndexed", size);
            outMetadata.put("wordsIndexed", titleTrie.getTotalWordsIndexed());
        }

        return resultList;
    }

    public synchronized List<Book> allBooks() {
        List<Book> r = new ArrayList<>();
        for (BookIdEntry e : bookIdTable) {
            for (BookIdEntry c = e; c != null; c = c.next) {
                r.add(c.book);
            }
        }
        return r;
    }

    public synchronized void clear() {
        Arrays.fill(bookIdTable, null);
        Arrays.fill(authorTable, null);
        Arrays.fill(categoryTable, null);
        size = 0;
    }

    public int getSize() { return size; }
    public int getTrieWordCount() { return titleTrie.getTotalWordsIndexed(); }
}
