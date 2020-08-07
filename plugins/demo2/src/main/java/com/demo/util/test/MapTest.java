package com.demo.util.test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MapTest {



    public static void main(String[] args) {
        MyMap<Integer, String> myMap = new MyMap<>(32);
        Map<Integer, String> hashMap = new HashMap<>(256);
        for (int i = 0; i < 100; i++) {
            String value = UUID.randomUUID().toString();
            myMap.put(i, value);
            hashMap.put(i, value);
        }
        hashMap.forEach((k, v) -> {
            assert v == myMap.get(k);
        });
        hashMap.forEach((k, v) -> {
            assert v == myMap.remove(k);
        });
    }
}
