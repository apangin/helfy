package one.helfy;

public class BinarySearch {

    public static int binarySearch(int[] array, int value) {
        return binarySearch(array, value, 0, array.length);
    }

    public static int binarySearch(int[] array, int value, int left, int right) {
        if (left >= right) {
            return -1;
        }

        int mid = (left + right) / 2;

        try {
            if (array[mid] == value) {
                return mid;
            } else if (array[mid] > value) {
                return binarySearch(array, value, left, mid);
            } else  {
                return binarySearch(array, value, mid, right);
            }
        } catch (Exception e) {
            StackTrace.dumpCurrentThread();
            return -1;
        }
    }

    private static void fillArray(int[] array) {
        // ... fill somehow ...
    }

    public static void main(String[] args) {
        int[] array = new int[1_200_000_000];
        fillArray(array);

        int index = binarySearch(array, 777);
        System.out.println(index);
    }

}
