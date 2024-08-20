import java.util.Random;

public class App {

    public static void main(String[] args) {

        if(args.length==0)
            System.out.println("Nada foi informado");
        else
            for(String txt: args)
                System.out.println(txt);

        if(args.length!=1){
            System.out.println("É esperado o envio de um inteiro");
            System.exit(0);
        }

        

        // cria o vetor
        int tamVet = Integer.parseInt(args[0]);
        int vetor[] = new int[tamVet];
        Random r = new Random();

        for(int i=0; i<tamVet; i++)
            vetor[i]=r.nextInt(tamVet);

        //1)busca sequencial no vetor desordenado 
        //  contabilizem o nro de operacoes necessárias

        //2)ordenem/classifiquem o vetor
        //  contabilizem o nro de operacoes necessárias

        //3)busca sequencial no vetor ordenado
        //  contabilizem o nro de operacoes necessárias

        //4)busca binaria no vetor ordenado
        //  contabilizem o nro de operacoes necessárias
        
    }
    
}