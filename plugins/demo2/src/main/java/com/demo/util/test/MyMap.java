package com.demo.util.test;

public class MyMap<K, V> {

    static class Entry<K, V> {
        final int hash;

        final K key;
        V value;

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

    public V put(K key, V value){
        if(key == null || value == null){
            return null;
        }

        int hash = hash(key);
        int position = findPosition(hash);


        for (Entry<K, V> e = table[position]; e != null ; e = e.next) {
            K k = e.key;
            if(e.hash == hash && (e.key == key) || e.key.equals(key)){
                V oldValue = e.value;
                e.value = value;
                return oldValue;
            }
        }
        // 链表没有，加到最前
        table[position] = new Entry<>(hash, key, value, table[position]);
        return null;

    }

    public V get(K key){
        if(key == null){
            return null;
        }

        int hash = hash(key);
        int position = findPosition(hash);

        for (Entry<K, V> e = table[position]; e != null ; e = e.next) {
            K k = e.key;
            if(e.hash == hash && (e.key == key) || e.key.equals(key)){
                return e.value;
            }
        }
        return null;

    }

    public V remove(K key){
        if(key == null){
            return null;
        }

        int hash = hash(key);
        int position = findPosition(hash);

        Entry<K, V> e = table[position];
        Entry<K, V> prev = e;
        for (; e != null ; prev = e, e = e.next) {
            K k = e.key;
            if(e.hash == hash && (e.key == key) || e.key.equals(key)){
                //
                if(table[position] == e){
                    // 在table?
                    table[position] = null;
                }else {
                    prev.next = e.next;
                }

                return e.value;
            }
        }
        return null;

    }




    private int hash(K key){
        int h;
        return (h = key.hashCode()) ^ h;
    }

    private int findPosition(int hash){
        return hash % size;
    }
}