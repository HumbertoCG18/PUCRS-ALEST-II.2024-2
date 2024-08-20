#include <stdio.h>
#include <stdlib.h>
#include <time.h>

#define MAX 1000000

void swim (int v[], int k );
void put(int v[], int* size, int data );
void sink (int v[], int size, int k );
int get( int v[], int* size );
void print( int v[], int size, int b, int elem, int sp );
void sort(int v[], int size);

void swim (int v[], int k ) {
    while (k>1 && v[k/2]<v[k]) {
        //printf("Swap %d with %d\n",v[k], v[k/2]);
        int tmp = v[k];
        v[k] = v[k/2];
        v[k/2] = tmp;
        k = k / 2;
    }
}

void put(int v[], int* size, int data ) {
    v[*size] = data;
    swim( v, *size );
    (*size)++;
}

void sink (int v[], int size, int k ) {
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

int get( int v[], int* size ) {
    int res = v[1];
    v[1] = v[--(*size)];
    sink( v, *size, 1 );
    return res;
}

void print( int v[], int size, int b, int elem, int sp )  {
    int i, j;

    for( j = 1; j < size; j++ ) printf("%d ", v[j]);
    printf("\n");

    while ( 1 ) {
        for( j = 0; j <= sp / 2; j++ ) printf(" ");
        for( i = b; i < b + elem; i++ ) {
            if ( i == size ) return;
            printf("%d", v[i] );
            for( j = 0; j < sp; j++ ) printf(" ");
        }
        printf("\n");
        b = b + elem;
        elem = 2 * elem;
        sp = sp / 2;
    }
}

void sort(int v[], int size)
{
    // Implemente heapsort!
}

int main()
{
   // Inicializa gerador aleatorio
   srand(time(0));

   int v[MAX+1];
   v[1] = 0;  // posicao 0 nao e' usada
   int size = 1;

   for(int i=0; i<MAX; i++)
     put(v, &size, rand()%(MAX*10)); // preenche aleatoriamente

//  print(v, size, 1, 1, 64);
//  printf("\n");

   // Ordena
   long start = clock();
   sort(v, size);
   long end = clock();

//   printf("\nOrdenado:\n");

//   for(int i=1; i<size; i++)
//     printf("%d ", v[i]);
//   printf("\n");

   printf("Tempo para %d elementos: %ld ns\n", MAX,(end-start));
}

