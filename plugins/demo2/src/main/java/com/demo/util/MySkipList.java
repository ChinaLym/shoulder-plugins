package com.demo.util;

import java.util.*;

/**
 * MySkipList
 *
 * @author lym
 */
public class MySkipList<K extends Comparable, V> {

    static class Node<K, V> {
        final K key;
        V value;

        List<Node<K, V>> nexts = new LinkedList<>();

        Node(K key, V value){
            this.key = key;
            this.value = value;
        }
    }

    private Node header;

    private int maxLevel = 0;
    private int count = 0;
    private static final double PROBABILITY = 0.5;
    private static final int MAX_LEVEL = 32;

    MySkipList(){
        header = new Node<>(null, null);
        header.nexts.add(null);
    }

    public V put(K key, V value){
        if(key == null){
            // throws?
            return null;
        }

        int level = calculateLevel();
        Node<K, V> start = header;
        Node<K, V> current;
        Node<K, V> newNode = new Node<>(key, value);

        do {
            // 找位置
            current = findAtLevel(key, start, level);
            if(current != header && current.key.equals(key)){
                // 已有，修改
                V oldValue = current.value;
                current.value = value;
                return oldValue;
            } else {
                // 新加入，链接，插入目标后面
                Node<K, V> next = current.nexts == null ? null : current.nexts.get(level);
                Node<K, V> realNext = next;
                if(next != null && next.key.equals(key)){
                    realNext = next.nexts.get(level);
                }
                newNode.nexts.add(0, realNext);
                current.nexts.set(level, newNode);
            }

        } while (level-- > 0);
        this.count++;

        return null;

    }

    public V get(K key){
        if(key == null){
            // throws?
            return null;
        }

        int level = calculateLevel();
        Node<K, V> start = header;
        Node<K, V> current;

        do {
            // 找位置
            current = findAtLevel(key, start, level);
            if(current.key == null){
                return current.value;
            }

        } while (level-- > 0);
        return null;

    }

    public List<V> scan(K start, K end){
        Node<K, V> startNode = find(start);
        if(startNode == null || startNode.nexts == null){
            return Collections.emptyList();
        }
        List<V> result = new LinkedList<>();
        // 从最后一层扫
        Node<K, V> current = startNode.nexts.get(0);
        while (current != null && lessOrEqual(current.key, end)){
            result.add(current.value);
            current = current.nexts.get(0);
        }
        return result;
    }

    /*public List<Node<K, V>> scanNode(K start, K end){
        Node<K, V> startNode = find(start);
        if(startNode == null || startNode.nexts == null){
            return Collections.emptyList();
        }
        List<Node<K, V>> result = new LinkedList<>();
        // 从最后一层扫
        Node<K, V> current = startNode.nexts.get(0);
        while (current != null && lessOrEqual(current.key, end)){
            result.add(current);
            current = current.nexts.get(0);
        }
        return result;
    }*/

    public V remove(K key){
        Node<K, V> deleteNode = find(key);
        if (deleteNode == null || deleteNode.key == null || !deleteNode.key.equals(key)) {
            return null;
        }
        int toDeleteNodeMaxLevel = deleteNode.nexts.size();
        count--;
        int level = maxLevel;
        Node<K, V> start = header;
        Node<K, V> beforeAim;
        V oldValue = null;
        do {
            // 找到前一个位置
            beforeAim = findBeforeAtLevel(deleteNode.key, start, level);
            Node<K, V> aim = beforeAim.nexts.get(level);
            if(aim != null && aim.key.equals(key)){
                oldValue = aim.value;
                // 删链接
                if (level < toDeleteNodeMaxLevel) {
                    beforeAim.nexts.set(level, deleteNode.nexts.get(level));
                }
            }
        } while (level-- > 0);
        return oldValue;
    }



    private int calculateLevel(){
        int level = 0;
        while(Math.random() < PROBABILITY && level < MAX_LEVEL){
            ++ level;
        }
        while (level > maxLevel){
            header.nexts.add(null);
            maxLevel ++;
        }
        return level;
    }


    // Returns the skiplist node with greatest value <= e
    private Node<K, V> find(K k) {
        return findBelowLevel(k, header, maxLevel);
    }

    // Returns the skiplist node with greatest value <= key
    private Node<K, V> findBelowLevel(K key, Node<K, V> start, int level) {
        Node<K, V> current = start;
        do {
             current = findAtLevel(key, current, level);
        } while (level-- > 0);
        return current;
    }

    // Returns the node at a given level with highest value less than key
    private Node<K, V> findAtLevel(K key, Node<K, V> startNode, int level) {
        Node<K, V> last = startNode;
        Node<K, V> next = startNode.nexts.get(level);
        while (next != null) {
            K nodeKey = next.key;
            if (lessOrEqual(key, nodeKey)) {
                break;
            }
            last = next;
            next = next.nexts.get(level);
        }
        return last;
    }
    private Node<K, V> findBeforeAtLevel(K key, Node<K, V> startNode, int level) {
        Node<K, V> last = startNode;
        while (last.nexts.get(level) != null) {
            Node<K, V> next = startNode.nexts.get(level);
            K nodeKey = next.key;
            if (lessOrEqual(key, nodeKey)) {
                break;
            }
            last = next;
        }
        return last;
    }

    private boolean lessOrEqual(K key, K nodeKey) {
        return key.compareTo(nodeKey) <= 0;
    }

    private boolean lessThan(K key, K nodeKey) {
        return key.compareTo(nodeKey) < 0;
    }


    public static void main(String[] args) {
        MySkipList<Integer, String> skipList = new MySkipList<>();
        skipList.put(2123132, "xxx");
        skipList.put(123210, "xxx");
        Map<Integer, String> hashMap = new HashMap<>();
        for (int i = 0; i < 80; i++) {
            String value = UUID.randomUUID().toString();
            skipList.put(i, value);
            hashMap.put(i, value);
        }
        for (int i = 0; i < 80; i++) {
            String value = UUID.randomUUID().toString();
            skipList.put(i, value);
            hashMap.put(i, value);
        }
        for (int i = 0; i < 80; i++) {
            assert skipList.get(i) == hashMap.get(i);
        }
        for (int i = 40; i < 60; i++) {
            assert skipList.remove(i) == hashMap.remove(i);
        }
        assert skipList.remove(30) == hashMap.remove(30);
        List<String> range = skipList.scan(20, 50);
        System.out.println("ok");
    }
}
