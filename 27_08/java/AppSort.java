import java.util.Random;

public class AppSort {

    public static void main(String args[]) {
        if(args.length!=2){
            System.out.println("Deve-se informar dois parametros na seguinte ordem");
            System.out.println("  Param 1: algoritmo de classificação (0 merge; 1 quick; 2 heap)");
            System.out.println("  Param 2: preenchimendo do vetor (0 crescente; 1 decrescente; 2 aleatorio)");
            System.out.println("Exemplo: AppSort 1 2");
            System.out.println("  Chama o programa solicitando a execução do algoritmo de ordenação");
            System.out.println("    quicksort com o vetor preenchido de forma aleatoria");
        }
        int algoritmo=Integer.parseInt(args[0]);
        int ordem=Integer.parseInt(args[1]);
        Random r = new Random();
        for (int max = 10; max < 10000; max += 500) {
            int[] data = new int[max];
            for (int i = 0; i < data.length; i++)
                switch (ordem) {
                    case 0:                        
                        data[i] = i;
                        break;
                    case 1:                        
                        data[i] = data.length-i-1;
                        break;                
                    default:
                        data[i] = r.nextInt(data.length * 10);
                        break;
                }
                

            long start = System.nanoTime();
            // Bubblesort s = new Bubblesort();
            switch (algoritmo) {
                case 0:                    
                    Mergesort m = new Mergesort();
                    m.sort(data);
                    break;
                case 1:                    
                    Quicksort q = new Quicksort();
                    q.sort(data);
                    break;
                default:
                    MaxHeap mh = new MaxHeap(data);
                    mh.sort();
                    break;
            }
            
            long end = System.nanoTime();

            double tempo = (end - start) / 1e6; // 1.000.000 (para transformar em ms)
            System.out.printf("%d %.2f\n", max, tempo);
        }
    }
}
