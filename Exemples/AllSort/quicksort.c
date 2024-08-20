#include <stdio.h>
#include <stdlib.h>
#include <time.h>

void quicksort(int data[], int tam);
void _qsort(int data[], int p, int r);
int partition(int data[], int p, int r);
void swap(int data[], int p1, int p2);

void quicksort(int data[], int tam)
{
    _qsort(data, 0, tam-1);
}

void _qsort(int data[], int p, int r) {
    if(p < r) {
        int q = partition(data,p, r);
        _qsort(data, p, q-1);
        _qsort(data, q+1, r);
    }
}

int partition(int data[], int p, int r) {
    int q = p;
    for(int j=p; j<r; j++) {
        if(data[j] <= data[r]) {
            swap(data,j,q);
            q++;
        }
    }
    swap(data,r,q);
    return q;
}

void swap(int data[], int p1, int p2) {
    int tmp = data[p1];
    data[p1] = data[p2];
    data[p2] = tmp;
}

int main()
{
    int data[50000];
    for(int max=10; max<50000; max+=500)
    {
        for(int i=0; i<max; i++)
        data[i] = rand()%(max*10);

        long start = clock();
        quicksort(data, max);
        long end = clock();

        //for(int i=0; i<MAX; i++)
        //    printf("%5d", data[i]);
        //printf("\n");
        float tempo = (end-start)/1e3; // 1.000 (converte para ms)
        printf("%d %.2f\n", max, tempo);
    }
}
