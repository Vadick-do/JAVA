import java.util.Scanner;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.List;

public class lab1 {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        int[] arr1 = new int[4];
        int[] arr2 = new int[3];
        int[] arr3 = new int[4];
        int[] arr4 = new int[5];

        System.out.println("Часть 1: Статические массивы");
        scan(arr1, scanner);
        scan(arr2, scanner);
        scan(arr3, scanner);
        scan(arr4, scanner);

        int minIndex1 = min(arr1);
        int minIndex2 = min(arr2);
        int minIndex3 = min(arr3);
        int minIndex4 = min(arr4);

        int maxIndex1 = max(arr1);
        int maxIndex2 = max(arr2);
        int maxIndex3 = max(arr3);
        int maxIndex4 = max(arr4);

        change(arr1, minIndex1, maxIndex1);
        change(arr2, minIndex2, maxIndex2);
        change(arr3, minIndex3, maxIndex3);
        change(arr4, minIndex4, maxIndex4);

        write(arr1);
        write(arr2);
        write(arr3);
        write(arr4);

        System.out.println("Часть 2: Динамические массивы");
        ArrayList<Integer> arrDinamic = new ArrayList<>();
        LinkedList<Integer> arrLinked = new LinkedList<>();
        scanDinamic(arrDinamic, arrLinked, scanner);

        int minArray = minDinamic(arrDinamic);
        int minLinked = minDinamic(arrLinked);
        int maxArray = maxDinamic(arrDinamic);
        int maxLinked = maxDinamic(arrLinked);

        changeDinamic(arrDinamic, minArray, maxArray);
        changeDinamic(arrLinked, minLinked, maxLinked);

        System.out.println("Динамический массив:");
        writeDinamic(arrDinamic);
        System.out.println("Двусвязанный список:");
        writeDinamic(arrLinked);

        scanner.close();
    }

    public static void scan(int[] array, Scanner scanner) {
        System.out.println("Введите элементы массива: ");
        for (int i = 0; i < array.length; i++) {
            array[i] = scanner.nextInt();
        }
    }

    public static int min(int[] array) {
        int minIndex = 0;
        for (int i = 1; i < array.length; i++) {
            if (array[i] < array[minIndex]) {
                minIndex = i;
            }
        }
        
        return minIndex;
    }

    public static int max(int[] array) {
        int maxIndex = 0;
        for (int i = 1; i < array.length; i++) {
            if (array[i] > array[maxIndex]) {
                maxIndex = i;
            }
        }
        
        return maxIndex;
    }

    public static void change(int[] array, int minIndex, int maxIndex) {
        int minValue = array[minIndex];
        array[minIndex] = array[maxIndex];
        array[maxIndex] = minValue;
    }

    public static void write(int[] array) {
        System.out.println(java.util.Arrays.toString(array));
    }

    public static void scanDinamic(List<Integer> arrayDinamic, List<Integer> arrayLinked, Scanner scanner) {
        System.out.println("Введите элементы динамического массива: ");
        for (int i = 0; i < 5; i++) {
            arrayDinamic.add(scanner.nextInt());
        }

        System.out.println("Введите элементы связанного массива: ");
        for (int i = 0; i < 4; i++) {
            arrayLinked.add(scanner.nextInt());
        }
    }

    public static int minDinamic(List<Integer> array) {
        int minIndex = 0;
        for (int i = 1; i < array.size(); i++) {
            if (array.get(i) < array.get(minIndex)) {
                minIndex = i;
            }
        }

        return minIndex;
    }

    public static int maxDinamic(List<Integer> array) {
        int maxIndex = 0;
        for (int i = 1; i < array.size(); i++) {
            if (array.get(i) > array.get(maxIndex)) {
                maxIndex = i;
            }
        }

        return maxIndex;
    }

    public static void changeDinamic(List<Integer> array, int minIndex, int maxIndex) {
        int minValue = array.get(minIndex);
        array.set(minIndex, array.get(maxIndex));
        array.set(maxIndex, minValue);
    }

    public static void writeDinamic(List<Integer> array) {
        System.out.println(array);
    }
}