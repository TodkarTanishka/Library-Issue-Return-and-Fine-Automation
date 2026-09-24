package datastructure.book;

import java.util.*;

public class TrieNode {
    public final Map<Character, TrieNode> children = new HashMap<>();
    public final Set<String> bookIds = new HashSet<>();
    public boolean isWordEnd = false;
}
