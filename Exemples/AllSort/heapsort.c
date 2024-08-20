#include <stdio.h>
#include <stdlib.h>
#include <time.h>

#define MAX 1000000

void swim(int v[], int k);
void put(int v[], int *size, int data);
void sink(int v[], int size, int k);
int get(int v[], int *size);
void print(int v[], int size, int b, int elem, int sp);
void sort(int v[], int size);

void swim(int v[], int k)
{
    while (k > 1 && v[k / 2] < v[k])
    {
        //printf("Swap %d with %d\n",v[k], v[k/2]);
        int tmp = v[k];
        v[k] = v[k / 2];
        v[k / 2] = tmp;
        k = k / 2;
    }
}

void put(int v[], int *size, int data)
{
    v[*size] = data;
    swim(v, *size);
    (*size)++;
}

void sink(int v[], int size, int k)
{
    while (2 * k <= size)
    {
        int j = 2 * k;
        if (j < size && v[j] < v[j + 1])
            j++;
        if (v[k] >= v[j])
            break;
        int tmp = v[k];
        v[k] = v[j];
        v[j] = tmp;
        k = j;
    }
}

int get(int v[], int *size)
{
    int res = v[1];
    v[1] = v[--(*size)];
    sink(v, *size, 1);
    return res;
}

void print(int v[], int size, int b, int elem, int sp)
{
    int i, j;

    for (j = 1; j < size; j++)
        printf("%d ", v[j]);
    printf("\n");

    while (1)
    {
        for (j = 0; j <= sp / 2; j++)
            printf(" ");
        for (i = b; i < b + elem; i++)
        {
            if (i == size)
                return;
            printf("%d", v[i]);
            for (j = 0; j < sp; j++)
                printf(" ");
        }
        printf("\n");
        b = b + elem;
        elem = 2 * elem;
        sp = sp / 2;
    }
}

void sort(int v[], int size)
{
    // 1. bottom up build
    int n = size - 1;
    for (int k = n / 2; k >= 1; k--)
        sink(v, n, k);

    // 2. sortdown
    while (n > 1)
    {
        int tmp = v[n];
        v[n] = v[1];
        v[1] = tmp;
        n--;
        sink(v, n, 1);
    }
}

int main()
{
    int data[50000];
    for (int max = 10; max < 50000; max += 500)
    {
        for (int i = 0; i < max; i++)
            data[i] = rand() % (max * 10);

        long start = clock();
        sort(data, max);
        long end = clock();

        //for(int i=0; i<MAX; i++)
        //    printf("%5d", data[i]);
        //printf("\n");
        float tempo = (end - start) / 1e3; // 1.000 (converte para ms)
        printf("%d %.2f\n", max, tempo);
    }
}
