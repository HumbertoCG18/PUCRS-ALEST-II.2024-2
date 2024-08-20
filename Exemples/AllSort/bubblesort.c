#include <stdio.h>
#include <stdlib.h>
#include <time.h>

void sort(int values[], int total);

void sort(int values[], int total)
{
    for(int i=0; i<total; i++) {
        char trocou = 0;
        for(int j=0; j<total-i-1; j++) {
            if(values[j] > values[j+1]) {
               int temp = values[j];
               values[j] = values[j+1];
               values[j+1] = temp;
               trocou = 1;
            }
        }
        if(!trocou) break;
    }
}

int main()
{
    int data[50000];
    for(int max=10; max<50000; max+=500)
    {
        //int data[max];
        for(int i=0; i<max; i++)
            data[i] = rand()%(max*10);
        long start = clock();
        sort(data, max);
        long end = clock();
        //for(int i=0; i<MAX; i++)
        //    printf("%d ", data[i]);
        //printf("\n");
        float tempo = (end-start)/1e3; // 1.000 (converte para ms)
        printf("%d %.2f\n", max, tempo);
    }  
}
