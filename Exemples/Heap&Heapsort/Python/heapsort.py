import random
from time import process_time
from maxheap import MaxHeap

def sort(h, size):
    # implemente heapsort!
    pass

MAX = 100000
data = []

for x in range(MAX):
    data.append(random.randint(1,MAX*10))

myheap = MaxHeap(data)
start = process_time()
myheap.sort()
end = process_time()
#print(myheap[1:])
t = end-start

print(myheap.getData())

print(f"Tempo para ordenar {MAX} elementos: {t} s")

