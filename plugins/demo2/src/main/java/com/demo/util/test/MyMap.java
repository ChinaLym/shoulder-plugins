package com.demo.util.test;

public class MyMap<K, V> {

    static class Entry<K, V> {
        final int hash;

        final K key;
        V value;
        long birth;

        Entry<K, V> next;

        public Entry(int hash, K key, V value, Entry<K, V> next) {
            this.key = key;
            this.hash = hash;
            this.value = value;
            this.next = next;
        }

    }

    private int size;
    //private int count;

    private Entry<K, V>[] table;


    @SuppressWarnings({"unchecked"})
    public MyMap(int size) {
        this.size = size;
        table = (Entry<K, V>[]) new Entry[size];
    }

    private int indexOf(int hashCode) {
        return hashCode % size;
    }

    private int hash(K k) {
        int h;
        return (h = k.hashCode()) ^ h >>> 16;
    }

    public V put(K key, V newValue) {
        // kv 不能为空
        if (key == null || newValue == null) {
            return null;
        }

        // 计算位置
        int hash = hash(key);
        int index = indexOf(hash);

        // 循环目标位置的链表
        for (Entry<K, V> e = table[index]; e != null; e = e.next) {
            K aimKey = e.key;
            if (hash == e.hash && (aimKey == key || aimKey.equals(key))) {
                //若key存在 返回oldValue
                V oldValue = e.value;
                e.value = newValue;
                return oldValue;
            }
        }
        // 若key不存在，将新值插入Entry链表的最前端
        table[index] = new Entry<K, V>(hash, key, newValue, null);
        return null;
    }

    public V get(K key) {
        // kv 不能为空
        if (key == null) {
            return null;
        }

        // 计算位置
        int hash = hash(key);
        int index = indexOf(hash);

        // 循环目标位置的链表
        for (Entry<K, V> e = table[index]; e != null; e = e.next) {
            K k = e.key;
            if (hash == e.hash && (k == key || k.equals(key))) {
                return e.value;
            }
        }

        // 没找到
        return null;
    }

    public V remove(K key) {
        if (key == null) {
            return null;
        }

        int hash = hash(key);
        int index = indexOf(hash);


        // 循环目标位置的链表
        for (Entry<K, V> e = table[index], prev = e; e != null; prev = e, e = e.next) {
            K k = e.key;
            if (hash == e.hash && (k == key || k.equals(key))) {
                if (e == table[index]) {
                    // 没有链表
                    table[index] = e.next;
                } else {
                    // 去除该链
                    prev.next = e.next;
                }
                e.next = null;
                return e.value;
            }
        }

        return null;
    }

}