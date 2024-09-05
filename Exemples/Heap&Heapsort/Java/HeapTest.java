
import java.util.Scanner;

import AllSort.MaxHeap;

public class HeapTest {

  public static void main( String[] args ) {
    MaxHeap H = new MaxHeap(100);

    try (Scanner input = new Scanner( System.in )) {
      // 3 entradas possiveis:
      //
      // numero: coloca o numero na fila
      // quit: sai
      // get: retira elemento do inicio da fila
      //
      while ( input.hasNext() ) {
        String temp = input.next();

        if ( temp.equals( "quit" ) ) System.exit(0);
        if ( temp.equals( "get" ) )  System.out.println( H.get() );
        if ( temp.matches( "[0-9]+" ) ) H.put( Integer.parseInt( temp ) );

        H.print();
      }
    } catch (NumberFormatException e) {
      e.printStackTrace();
    }
  }
}
