package datastructure.book;

import java.util.*;

public class Trie {
    private final TrieNode root = new TrieNode();
    private int totalWordsIndexed = 0;

    public synchronized void insert(String text, String bookId) {
        if (text == null || bookId == null || text.isBlank()) return;
        String[] words = text.toLowerCase().split("[^a-z0-9\\+\\-\\.]+");
        for (String word : words) {
            if (word.isBlank()) continue;
            insertWord(word, bookId);
            totalWordsIndexed++;
        }
    }

    private void insertWord(String word, String bookId) {
        TrieNode cur = root;
        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            cur = cur.children.computeIfAbsent(c, k -> new TrieNode());
            cur.bookIds.add(bookId);
        }
        cur.isWordEnd = true;
    }

    public synchronized void remove(String text, String bookId) {
        if (text == null || bookId == null || text.isBlank()) return;
        String[] words = text.toLowerCase().split("[^a-z0-9\\+\\-\\.]+");
        for (String word : words) {
            if (word.isBlank()) continue;
            removeWord(word, bookId);
        }
    }

    private void removeWord(String word, String bookId) {
        TrieNode cur = root;
        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            cur = cur.children.get(c);
            if (cur == null) return;
            cur.bookIds.remove(bookId);
        }
    }

    public synchronized Set<String> searchByPrefix(String prefix) {
        if (prefix == null || prefix.isBlank()) return Collections.emptySet();
        String p = prefix.toLowerCase().trim();
        TrieNode cur = root;
        for (int i = 0; i < p.length(); i++) {
            char c = p.charAt(i);
            cur = cur.children.get(c);
            if (cur == null) return Collections.emptySet();
        }
        return new HashSet<>(cur.bookIds);
    }

    public synchronized List<String> autocomplete(String prefix, int limit, Map<String, Integer> popularityMap) {
        Set<String> matches = searchByPrefix(prefix);
        List<String> list = new ArrayList<>(matches);
        if (popularityMap != null && !popularityMap.isEmpty()) {
            list.sort((a, b) -> Integer.compare(
                popularityMap.getOrDefault(b, 0),
                popularityMap.getOrDefault(a, 0)
            ));
        }
        if (limit > 0 && list.size() > limit) {
            return list.subList(0, limit);
        }
        return list;
    }

    public int getTotalWordsIndexed() {
        return totalWordsIndexed;
    }
}
