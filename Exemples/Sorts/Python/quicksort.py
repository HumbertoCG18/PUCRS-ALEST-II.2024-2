#!/usr/bin/python3
#
# Quicksort
#

import random
from time import process_time

def partition(data, p, r):
    q = p;
    for j in range(p,r):
        if data[j] <= data[r]:
            data[j], data[q] = data[q], data[j]
            q += 1
    data[r], data[q] = data[q], data[r]
    return q

def quicksort(data, p, r):
    if p < r:
        q = partition(data, p, r);
        quicksort(data, p, q-1);
        quicksort(data, q+1, r);

for max in range(10,50000,500):

    data = [random.randint(0,max*10) for x in range(max)]

    start = process_time()    
    quicksort(data, 0, len(data)-1)
    end = process_time()
    t = (end-start) * 1000 # converte para ms

    print(f"{max} {t:.2f}")
