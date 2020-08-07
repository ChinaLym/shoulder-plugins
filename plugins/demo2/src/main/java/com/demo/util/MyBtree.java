package com.demo.util;

import java.util.function.Consumer;

public class MyBtree<E extends Comparable> {
    private Node<E> root = new Node<>(null);

    private int count = 0;

    class Node<E extends Comparable> {
        private E data;
        private Node<E> parent;
        private Node<E> left;
        private Node<E> right;

        Node(E data){
            this.data = data;
        }

        public void add(E data) {
            if (this.data.compareTo(data) > 0){
                if(this.left == null){
                    this.left = new Node<>(data);
                }else{
                    this.left.add(data);
                }
            }else {
                if(this.right == null){
                    this.right = new Node<>(data);
                }else{
                    this.right.add(data);
                }
            }
        }
 
        public void midVisitor(Consumer<E> consumer) {
            if(this.left != null){
                this.left.midVisitor(consumer);
            }
            if(this.right != null){
                this.right.midVisitor(consumer);
            }
            consumer.accept(this.data);
        }

        public boolean contains(E data) {
            if(this.data == data){
                return true;
            }if(this.data.compareTo(data) > 0){
                return this.left.contains(data);
            }else {
                return this.right.contains(data);
            }
        }

    }
    public  void add(E data){
        if(root.data == null){
            root.data = data;
            return;
        }
        root.add(data);
        count ++;
    }

    public void midVisitor(Consumer<E> consumer) {
        if(root.data == null){
            return;
        }
        root.midVisitor(consumer);
    }

    boolean contains(E data){
        return root.data != null && root.contains(data);
    }

    public static void main(String[] args) {
        MyBtree<Integer> btree = new MyBtree<>();
        btree.add(50);
        for (int i = 0; i < 100; i++) {
            btree.add(i);
        }
        btree.midVisitor(System.out::println);

    }

}
