package com.demo.util.test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class QueueTest {


    static final CountDownLatch LATCH = new CountDownLatch(6);

    public static void main(String[] args) {
        Executor executor = Executors.newFixedThreadPool(10, Executors.defaultThreadFactory());
        MyQueue<Integer> queue = new MyQueue<>(2000);
        for (int i = 0; i < 5; i++) {
            executor.execute(new Producer(queue));
            executor.execute(new Consumer(queue));
            LATCH.countDown();
        }
        System.out.println("created");
        LATCH.countDown();
    }

    static class Producer implements Runnable {
        MyQueue<Integer> queue;
        Producer(MyQueue<Integer> queue){
            this.queue = queue;
        }
        @Override
        public void run() {
            try {
                LATCH.await();
                System.out.println("Producer start");
                for (int i = 0; i < 100; i++) {
                    queue.add(i);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    static class Consumer implements Runnable {
        MyQueue<Integer> queue;
        Consumer(MyQueue<Integer> queue){
            this.queue = queue;
        }
        @Override
        public void run() {
            try {
                LATCH.await();
                System.out.println("Consumer start");
                for (int i = 0; i < 100; i++) {
                    System.out.println(queue.take());
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}
