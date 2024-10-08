#!/usr/bin/python3
#
# Heapsort
#
import random
from time import process_time
from maxheap import MaxHeap

MAX = 100000
data = []

for max in range(10,50000,500):

    data = [random.randint(0,max*10) for x in range(max)]

    myheap = MaxHeap(data)
    start = process_time()
    myheap.sort()
    end = process_time()
    t = (end-start) * 1000 # converte para ms

    print(f"{max} {t:.2f}")
    #break

