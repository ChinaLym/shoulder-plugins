package com.demo.util;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class MyQueue<E> {

    static class Node<E> {
        E data;
        Node<E> prev;
        Node<E> next;

        Node(Node<E> prev, E data, Node<E> next) {
            this.prev = prev;
            this.data = data;
            this.next = next;
        }
        Node(E data) {
            this.data = data;
        }
    }


    private transient int count;

    private final int capacity;

    private final ReentrantLock lock = new ReentrantLock();

    private final Condition notEmpty = lock.newCondition();

    private final Condition notFull = lock.newCondition();

    /**
     * 头节点
     */
    private Node<E> first;

    /**
     * 尾节点
     */
    private Node<E> last;

    public MyQueue(int capacity){
        this.capacity = capacity;
    }

    /**
     * 入队列
     */
    public void add(E o) throws InterruptedException {
        Node<E> newNode = new Node<>(o);
        lock.lock();
        try{
            while (!linkLast(newNode)) {
                notFull.await();
            }
        }finally {
            lock.unlock();
        }

    }

    private boolean linkLast(Node<E> node) {
        if (count >= capacity) {
            return false;
        }
        Node<E> oldTail = this.last;
        node.prev = oldTail;
        this.last = node;
        if (first == null) {
            first = node;
        } else {
            oldTail.next = node;
        }
        ++count;
        notEmpty.signal();
        return true;
    }

    /**
     * 出队列
     */
    public E take() throws InterruptedException {
        lock.lock();
        try {
            E x;
            while ( (x = unlinkFirst()) == null) {
                notEmpty.await();
            }
            return x;
        } finally {
            lock.unlock();
        }
    }

    private E unlinkFirst() {
        if (first == null) {
            return null;
        }
        E element = first.data;
        first.data = null;

        Node<E> second = first.next;
        first = second;
        if (second == null) {
            last = null;
        } else {
            second.prev = null;
        }
        --count;
        notFull.signal();
        return element;
    }
}