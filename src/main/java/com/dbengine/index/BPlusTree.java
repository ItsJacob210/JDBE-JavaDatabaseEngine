package com.dbengine.index;

import com.dbengine.storage.RecordId;

import java.util.*;

/**
 * In-memory B+ tree index for fast lookups.
 * For simplicity, this implementation keeps the tree in memory.
 * A production implementation would be page-backed on larger datasets.
 */
public class BPlusTree {
    private static final int ORDER = 4;  // Max number of keys per node
    
    private BPlusTreeNode root;
    private final String columnName;
    
    public BPlusTree(String columnName) {
        this.columnName = columnName;
        this.root = new BPlusTreeLeafNode();
    }
    
    /**
     * Insert a key-value pair into the tree.
     */
    public void insert(Comparable<?> key, RecordId rid) {
        if (key == null) {
            return;  // Don't index null values
        }
        
        BPlusTreeNode newRoot = root.insert(key, rid);
        if (newRoot != null) {
            root = newRoot;
        }
    }
    
    /**
     * Search for a specific key.
     */
    public List<RecordId> search(Comparable<?> key) {
        if (key == null) {
            return new ArrayList<>();
        }
        return root.search(key);
    }
    
    /**
     * Range search: find all keys >= minKey and <= maxKey.
     */
    public List<RecordId> rangeSearch(Comparable<?> minKey, Comparable<?> maxKey) {
        List<RecordId> results = new ArrayList<>();
        root.rangeSearch(minKey, maxKey, results);
        return results;
    }
    
    /**
     * Delete a key-value pair.
     */
    public void delete(Comparable<?> key, RecordId rid) {
        if (key == null) {
            return;
        }
        root.delete(key, rid);
    }
    
    public String getColumnName() {
        return columnName;
    }
    
    /**
     * Base class for B+ tree nodes.
     */
    private abstract static class BPlusTreeNode {
        abstract BPlusTreeNode insert(Comparable<?> key, RecordId rid);
        abstract List<RecordId> search(Comparable<?> key);
        abstract void rangeSearch(Comparable<?> minKey, Comparable<?> maxKey, List<RecordId> results);
        abstract void delete(Comparable<?> key, RecordId rid);
        abstract boolean isLeaf();
    }
    
    /**
     * Internal node in the B+ tree.
     */
    private static class BPlusTreeInternalNode extends BPlusTreeNode {
        private List<Comparable<?>> keys;
        private List<BPlusTreeNode> children;
        
        public BPlusTreeInternalNode() {
            this.keys = new ArrayList<>();
            this.children = new ArrayList<>();
        }
        
        @Override
        BPlusTreeNode insert(Comparable<?> key, RecordId rid) {
            int index = findChildIndex(key);
            BPlusTreeNode child = children.get(index);
            BPlusTreeNode newChild = child.insert(key, rid);
            
            if (newChild != null) {
                //child was split
                Comparable<?> splitKey = getSplitKey(newChild);
                keys.add(index, splitKey);
                children.add(index + 1, newChild);
                
                if (keys.size() >= ORDER) {
                    return split();
                }
            }
            
            return null;
        }
        
        @Override
        List<RecordId> search(Comparable<?> key) {
            int index = findChildIndex(key);
            return children.get(index).search(key);
        }
        
        @Override
        void rangeSearch(Comparable<?> minKey, Comparable<?> maxKey, List<RecordId> results) {
            for (BPlusTreeNode child : children) {
                child.rangeSearch(minKey, maxKey, results);
            }
        }
        
        @Override
        void delete(Comparable<?> key, RecordId rid) {
            int index = findChildIndex(key);
            children.get(index).delete(key, rid);
        }
        
        @Override
        boolean isLeaf() {
            return false;
        }
        
        @SuppressWarnings("unchecked")
        private int findChildIndex(Comparable<?> key) {
            for (int i = 0; i < keys.size(); i++) {
                if (((Comparable<Object>) key).compareTo(keys.get(i)) < 0) {
                    return i;
                }
            }
            return keys.size();
        }
        
        private Comparable<?> getSplitKey(BPlusTreeNode node) {
            if (node instanceof BPlusTreeLeafNode) {
                return ((BPlusTreeLeafNode) node).keys.get(0);
            } else {
                return ((BPlusTreeInternalNode) node).keys.get(0);
            }
        }
        
        private BPlusTreeNode split() {
            BPlusTreeInternalNode newRoot = new BPlusTreeInternalNode();
            BPlusTreeInternalNode newNode = new BPlusTreeInternalNode();
            
            int mid = keys.size() / 2;
            
            newNode.keys.addAll(keys.subList(mid + 1, keys.size()));
            newNode.children.addAll(children.subList(mid + 1, children.size()));
            
            keys.subList(mid, keys.size()).clear();
            children.subList(mid + 1, children.size()).clear();
            
            newRoot.keys.add(keys.get(keys.size() - 1));
            newRoot.children.add(this);
            newRoot.children.add(newNode);
            
            return newRoot;
        }
    }
    
    /**
     * Leaf node in the B+ tree.
     */
    private static class BPlusTreeLeafNode extends BPlusTreeNode {
        private List<Comparable<?>> keys;
        private List<List<RecordId>> values;
        private BPlusTreeLeafNode next;  // For range scans
        
        public BPlusTreeLeafNode() {
            this.keys = new ArrayList<>();
            this.values = new ArrayList<>();
            this.next = null;
        }
        
        @Override
        BPlusTreeNode insert(Comparable<?> key, RecordId rid) {
            int index = findInsertIndex(key);
            
            if (index < keys.size() && keys.get(index).equals(key)) {
                //key already exists, add to value list
                values.get(index).add(rid);
            } else {
                //insert new key
                keys.add(index, key);
                List<RecordId> valueList = new ArrayList<>();
                valueList.add(rid);
                values.add(index, valueList);
            }
            
            if (keys.size() >= ORDER) {
                return split();
            }
            
            return null;
        }
        
        @Override
        List<RecordId> search(Comparable<?> key) {
            for (int i = 0; i < keys.size(); i++) {
                if (keys.get(i).equals(key)) {
                    return new ArrayList<>(values.get(i));
                }
            }
            return new ArrayList<>();
        }
        
        @Override
        @SuppressWarnings("unchecked")
        void rangeSearch(Comparable<?> minKey, Comparable<?> maxKey, List<RecordId> results) {
            for (int i = 0; i < keys.size(); i++) {
                Comparable<Object> key = (Comparable<Object>) keys.get(i);
                if (key.compareTo(minKey) >= 0 && key.compareTo(maxKey) <= 0) {
                    results.addAll(values.get(i));
                }
            }
            
            if (next != null) {
                next.rangeSearch(minKey, maxKey, results);
            }
        }
        
        @Override
        void delete(Comparable<?> key, RecordId rid) {
            for (int i = 0; i < keys.size(); i++) {
                if (keys.get(i).equals(key)) {
                    values.get(i).remove(rid);
                    if (values.get(i).isEmpty()) {
                        keys.remove(i);
                        values.remove(i);
                    }
                    return;
                }
            }
        }
        
        @Override
        boolean isLeaf() {
            return true;
        }
        
        @SuppressWarnings("unchecked")
        private int findInsertIndex(Comparable<?> key) {
            for (int i = 0; i < keys.size(); i++) {
                if (((Comparable<Object>) key).compareTo(keys.get(i)) < 0) {
                    return i;
                }
            }
            return keys.size();
        }
        
        private BPlusTreeNode split() {
            BPlusTreeInternalNode newRoot = new BPlusTreeInternalNode();
            BPlusTreeLeafNode newLeaf = new BPlusTreeLeafNode();
            
            int mid = keys.size() / 2;
            
            newLeaf.keys.addAll(keys.subList(mid, keys.size()));
            newLeaf.values.addAll(values.subList(mid, values.size()));
            
            keys.subList(mid, keys.size()).clear();
            values.subList(mid, values.size()).clear();
            
            newLeaf.next = this.next;
            this.next = newLeaf;
            
            newRoot.keys.add(newLeaf.keys.get(0));
            newRoot.children.add(this);
            newRoot.children.add(newLeaf);
            
            return newRoot;
        }
    }
}
