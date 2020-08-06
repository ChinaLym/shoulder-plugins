package com.demo.util.test;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class MyQueue<E> {

    class Node<E> {
        E data;
        Node<E> prev;
        Node<E> next;

        Node(E e){
            this.data = e;
        }
    }

    int count = 0;
    final int capacity;

    Node<E> first;
    Node<E> last;

    private Lock lock = new ReentrantLock();
    private Condition notEmpty = lock.newCondition();
    private Condition notFull = lock.newCondition();

    MyQueue(int capacity){
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
        assert Thread.holdsLock(lock);
        // special check 满了
        if(count >= capacity){
            return false;
        }

        // 完善 newNode
        Node<E> oldLast = this.last;
        newNode.prev = oldLast;

        // 维护两个指针
        last = newNode;
        if(first == null){
            // 第一个元素
            first = newNode;
        }else {
            oldLast.next = newNode;
        }

        // after add
        ++count;
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
        assert Thread.holdsLock(lock);
        // 为空
        if(first == null){
            return null;
        }

        // 完善 newNode
        E result = first.data;

        Node<E> second = first.next;


        first.data = null;
        // help gc
        first.next = first;

        first = second;

        // 维护两个指针
        if(second == null){
            // 取没了
            last = null;
        }else {
            second.prev = null;
        }

        // after take
        --count;
        notFull.signal();
        return result;
    }


}
