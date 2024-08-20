import java.util.ArrayDeque;
import java.util.Queue;

public class CaminhamentoEmLargura{
    private int [] edgeTo;
    private int [] distTo;
    private boolean [] marked;
    private int s;

    public CaminhamentoEmLargura(Graph g, int ref) {
        this.s=ref;
        edgeTo=new int [g.V()];
        distTo=new int [g.V()];
        marked=new boolean [g.V()];
        bfs(g,s);
    }

    private void bfs(Graph g, int ref) {
        Queue<Integer> q = new ArrayDeque<Integer>();
        q.add(ref);        
        distTo[ref]=0;        
        marked[ref]=true;
        while(!q.isEmpty()){
            int aux = q.remove();            

            if(aux!=edgeTo[aux])
                distTo[aux]=distTo[edgeTo[aux]]+1;

            for(int adj: g.adj(aux))
                if(!marked[adj]){
                    marked[adj]=true;
                    edgeTo[adj]=aux;
                    q.add(adj);
                }
        }

    }

    public boolean hasPath(int v){
        return marked[v];
    }

    public Iterable<Integer> pathTo(int v){
        if(!marked[v]) return null;

        Bag b = new Bag();
        b.add(v);
        while(v!=edgeTo[v]){
            v=edgeTo[v];
            b.add(v);
        }
        return b;

    }

    public static void main(String[] args) {
        In in = new In(args[0]);
        Graph G = new Graph(in);
        StdOut.println(G);
        StdOut.println();
        StdOut.println(G.toDot());

        System.out.println("Estou na classe Caminhamento Em Profundidade...");

        CaminhamentoEmLargura cep = new CaminhamentoEmLargura(G, 0);
        System.out.println("Existe um caminho para o 3? "+(cep.hasPath(3)?"SIM":"NÃO"));
        if(cep.hasPath(3)){
            for(int p: cep.pathTo(3))
                System.out.print(p+"; ");
            System.out.println();
        }
            
        System.out.println("Existe um caminho para o 4? "+(cep.hasPath(4)?"SIM":"NÃO"));
        if(cep.hasPath(4)){
            for(int p: cep.pathTo(4))
                System.out.print(p+"; ");
            System.out.println();
        }
            

    }



}