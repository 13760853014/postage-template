package com.jianke.service;

public class TestMaxWater {

    public static void main(String[] args) {
        int[] arr = {0,1,0,2,1,0,1,3,2,0,1,1};
        int water = 0;
        if (arr.length == 0) {
            System.out.println(water);
        }
        System.out.println(maxWater(arr, 0));
    }

    public static int maxWater(int[] arr, int index) {
        int water = 0;
        int size = arr.length;
        int currentVolume = index;
        for (; index < size; index ++) {
            int leftVolume = findLeftVolume(arr, index);
            currentVolume = findRightLowVolume(arr, leftVolume);
            if (currentVolume == 0) {
                return water;
            }

            int rightVolume = findNextHighThanLeftVolume(arr, leftVolume);
            if (rightVolume != 0) {
                while (currentVolume < rightVolume) {
                    index = currentVolume;
                    water += arr[leftVolume] - arr[currentVolume];
                    currentVolume ++;
                }
            } else {
                rightVolume = findNextLowThanLeftHighThanCurrentVolume(arr, leftVolume, currentVolume);
                if (rightVolume != 0) {
                    while (currentVolume < rightVolume) {
                        index = currentVolume;
                        water += arr[rightVolume] - arr[currentVolume];
                        currentVolume ++;
                    }
                }
            }
        }
        System.out.println(index);
        return water;
    }

    private static int findNextLowThanLeftHighThanCurrentVolume(int[] arr, int leftVolume, int currentVolume) {
        int size = arr.length;
        int rightVolume = 0;
        for (int i = leftVolume + 1; i < size - 1; i ++) {
            if (arr[i] <= arr[leftVolume] && arr[i] > arr[currentVolume]) {
                return i;
            }
        }
        return rightVolume;
    }

    private static int findNextHighThanLeftVolume(int[] arr, int leftVolume) {
        int size = arr.length;
        for (int i = leftVolume; i < size - 1; i ++) {
            int rightVolume = i + 1;
            if (arr[rightVolume] >= arr[leftVolume]) {
                return rightVolume;
            }
        }
        return 0;
    }

    private static int findRightLowVolume(int[] arr, int leftVolume) {
        int size = arr.length;
        int rightVolume = 0;
        for (; leftVolume < size - 1; leftVolume ++) {
            if (leftVolume < size -1) {
                rightVolume = leftVolume + 1;
            }
            if (arr[rightVolume] >= arr[leftVolume]) {
                continue;
            } else {
                return rightVolume;
            }
        }
        return rightVolume;
    }

    private static int findLeftVolume(int[] arr, int index) {
        int leftVolume = 0;
        int size = arr.length;
        for (; index < size - 1; index ++) {
            if (arr[index] != 0 && arr[index] > arr[index + 1]) {
                leftVolume = index;
                break;
            }
        }
        return leftVolume;
    }
}
