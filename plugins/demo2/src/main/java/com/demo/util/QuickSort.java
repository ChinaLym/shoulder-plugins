package com.demo.util;

public class QuickSort {
    private static int count;
    private static int chg;

    public static void main(String[] args) {
		int total = 100_00;
		int[] num = new int[total];
		for (int i = 0; i < total; i++) {
			num[i] = total - i;
		}
        quickSort(num, 0, num.length - 1);

		System.out.println("数组个数：" + num.length);
		System.out.println("循环次数：" + count);

		for (int i = 0; i < total; i++) {
			System.out.print(i + ", ");
			if(i % 50 == 0){
				System.out.println();
			}
		}
    }

    /**
     * 快速排序
     *
     * @param num   排序的数组
     * @param left  数组的前针
     * @param right 数组后针
     */
    private static void quickSort(int[] num, int left, int right) {
        //如果left等于right，即数组只有一个元素，直接返回
        if (left >= right) {
            return;
        }
        //设置最左边的元素为基准值
        int base = num[left];

        //数组中比key小的放在左边，比key大的放在右边，key值下标为i
        int i = left;
        int j = right;
        while (i < j) {
            //j向左移，直到遇到比key小的值
            while (num[j] >= base && i < j) {
                j--;
            }
			//i和j指向的元素交换
			if (i < j) {
				int temp = num[i];
				num[i] = num[j];
				num[j] = temp;
				chg++;
			}
            //i向右移，直到遇到比key大的值
            while (num[i] <= base && i < j) {
                i++;
            }
            //i和j指向的元素交换
            if (i < j) {
                num[i] ^= num[j];
                num[j] ^= num[i];
				num[i] ^= num[j];
				chg++;
            }
        }

        num[left] = num[i];
        num[i] = base;
        count++;

        // left
        quickSort(num, left, i - 1);
        // right
        quickSort(num, i + 1, right);
    }


}