import java.util.Random;

public class AppSort {

    public static void main(String args[]) {
        Random r = new Random();
        for(int max=10; max<50000; max+=500)
        {
            int[] data = new int[max];
            for(int i=0; i<data.length; i++)
                data[i] = r.nextInt(data.length*10);

            long start = System.nanoTime();
            // Bubblesort s = new Bubblesort();
            // Mergesort s = new Mergesort();
            Quicksort s = new Quicksort();
            s.sort(data);
            long end = System.nanoTime();
            // for(int i=0; i<data.length; i++)
            //    System.out.print(data[i]+" ");
            // System.out.println();
            double tempo = (end-start)/1e6; // 1.000.000 (para transformar em ms)
            System.out.printf("%d %.2f\n",max,tempo);
        }
    }
}
