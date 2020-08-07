package com.demo.util.test;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class MyQueue<E> {


    static class Node<E> {
        E data;
        Node<E> prev;
        Node<E> next;

        public Node(E data) {
            this.data = data;
        }
    }

    private int count;
    private final int capacity;


    private Node<E> first;
    private Node<E> last;

    private Lock lock = new ReentrantLock();
    private Condition notEmpty = lock.newCondition();
    private Condition notFull = lock.newCondition();

    public MyQueue(int capacity) {
        this.capacity = capacity;
    }

    public void add(E data) throws InterruptedException {
        Node<E> newNode = new Node<>(data);
        lock.lock();
        try {
            while (!linkToLast(newNode)){
                notFull.await();
            }
        }finally {
            lock.unlock();
        }
    }

    private boolean linkToLast(Node<E> newNode) {
        // check if could do
        if(count >= capacity){
            return false;
        }

        // adjust new

        newNode.prev = this.last;

        // maintain 2 point
        Node<E> oldLast = this.last;
        this.last = newNode;

        if(first == null){
            // 第一次放
            first = newNode;
        }else {
            oldLast.next = newNode;
        }

        // after do
        count ++;
        notEmpty.signal();
        return true;
    }

    public E take() throws InterruptedException {
        lock.lock();
        try {
            E result;
            while ((result = unlinkFirst()) == null){
                notEmpty.await();
            }
            return result;
        }finally {
            lock.unlock();
        }
    }

    private E unlinkFirst() {
        // check if could do
        if(first == null){
            return null;
        }

        // get result
        E result = first.data;

        Node<E> second = first.next;
        first.data = null;
        first.next = first;

        first = second;

        // maintain 2 point

        if(second == null){
            // 拿没了
            last = null;
        }else {
            second.prev = null;
        }

        // after do
        count --;
        notFull.signal();
        return result;
    }

}
