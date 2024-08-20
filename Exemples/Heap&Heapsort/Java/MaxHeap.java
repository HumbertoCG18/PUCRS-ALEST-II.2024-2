// MaxHeap.java
public class MaxHeap {

  private int v[];
  private int size;

  public MaxHeap(int tam) {
    size = 1;
    v = new int[tam];
    v[1] = 0;
  }

  public MaxHeap(int values[]) {
    v = values;
    size = values.length;
  }

  private int left ( int i )   { return 2 * i; }
  private int right ( int i )  { return 2 * i + 1; }
  private int parent ( int i ) { return i / 2; }

  private void swim ( int k ) {
      while (k>1 && v[k/2]<v[k]) {
         System.out.println("Swap "+v[k]+" with "+v[k/2]+" "+k);
         int tmp = v[k];
         v[k] = v[k/2];
         v[k/2] = tmp;
         k = k / 2;
      } 
  }

  public void put( int data ) {
    v[size] = data;
    swim( size );
    size++;
  }

  private void sink ( int k ) {
      while (2*k <= size) {
         int j = 2*k;
         if (j < size && v[j]<v[j+1]) j++;
         if (v[k] >= v[j]) break;
         int tmp = v[k];
         v[k] = v[j];
         v[j] = tmp;
         k = j;
      }
  }

  public int get( ) {
    if(size==1)
      throw new UnsupportedOperationException("MaxHeap vazio!");
    int res = v[1];
    v[1] = v[--size];
    sink( 1 );
    return res;
  }

  private void print( int b, int elem, int sp )  {
    int i, j;

    System.out.println( "" );
    for( j = 0; j < size; j++ ) System.out.print( v[j] + " " );
    System.out.println( "" );

    while ( true ) {
      for( j = 0; j <= sp / 2; j++ ) System.out.print( " " );
      for( i = b; i < b + elem; i++ ) {
        if ( i == size ) return;
        System.out.print( v[i] );
        for( j = 0; j < sp; j++ ) System.out.print( " " );
      }
      System.out.println( "" );
      b = b + elem;
      elem = 2 * elem;
      sp = sp / 2;
    }
  }

  public void print( )  {
    System.out.println( "" );
    print( 1, 1, 32 );
    System.out.println( "" );
  }

  public void printArray() {
    for(int i=0; i<size; i++)
       System.out.print(v[i]+" ");
    System.out.println();
  }

  public void sort() {
      // Implemente heapsort!
  }
}
