#!/usr/bin/python3
#
# Bubblesort
#

import random
from time import process_time

def bubbleSort(alist):

    for i in range(len(alist)):
        trocou = False
        for j in range(len(alist)-i-1):
            if alist[j] > alist[j+1]:
                alist[j], alist[j+1] = alist[j+1], alist[j]
                trocou = True
        if not trocou:
           break

#alist = []
#for x in range(1000):
#    alist.append(random.randint(0,10000))

#alist = [54,26,93,17,77,31,44,55,20]

for max in range(10,50000,500):
  data = [random.randint(0,max*10) for x in range(max)]

  start = process_time()
  bubbleSort(data)
  end = process_time()
  t = (end-start) * 1000 # converte para ms

  print(f"{max} {t:.2f}")

