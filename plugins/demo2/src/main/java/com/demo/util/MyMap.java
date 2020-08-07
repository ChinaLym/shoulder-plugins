package com.demo.util;

public class MyMap<K, V> {

    static class Entry<K, V> {

        final int hashCode;
        final K key;
        V value;

        Entry<K, V> next;

        public Entry(int hash, K key, V value, Entry<K, V> next) {
            this.key = key;
            this.hashCode = hash;
            this.value = value;
            this.next = next;
        }

    }

    private int size;

    private Entry<K, V>[] table;


    @SuppressWarnings({"rawtypes", "unchecked"})
    public MyMap(int size) {
        this.size = size;
        table = (Entry<K, V>[]) new Entry[size];
    }

    private int indexOf(int hashCode) {
        return hashCode % (size);
    }

    private int hash(K k) {
        int h;
        return (h = k.hashCode()) ^ h >>> 16;
    }

    public V put(K key, V newValue) {
        if (key == null || newValue == null) {
            return null;
        }

        int hash = hash(key);
        int index = indexOf(hash);

        for (Entry<K, V> e = table[index]; e != null; e = e.next) {
            V oldValue;
            K k = e.key;
            if (k.hashCode() == e.hashCode && (k == key || k.equals(key))) {
                //若key存在 返回oldValue
                oldValue = e.value;
                e.value = newValue;
                return oldValue;
            }
        }
        //若key不存在，将新值插入Entry链表的最前端
        table[index] = new Entry<>(hash, key, newValue, table[index]);
        return null;
    }

    public V get(K key) {
        //key 为空直接返回
        if (key == null) {
            return null;
        }

        int hash = hash(key);
        int index = indexOf(hash);

        for (Entry<K, V> e = table[index]; e != null; e = e.next) {
            K k = e.key;
            if (k.hashCode() == e.hashCode && (k == key || k.equals(key))) {
                return e.value;
            }
        }

        return null;
    }

    public V remove(K key) {
        if (key == null) {
            return null;
        }

        int hash = hash(key);
        int index = indexOf(hash);

        Entry<K, V> e = table[index];
        Entry<K, V> prev = e;

        while (e != null) {
            Entry<K, V> next = e.next;
            if (hash == e.hashCode && (e.key == key || e.key.equals(key))) {
                if (prev == e) {
                    table[index] = next;
                } else {
                    prev.next = e.next;
                }
                return e.value;
            }
            prev = e;
            e = next;
        }
        return null;
    }

}