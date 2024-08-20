import java.util.Random;

public class Quicksort
{
    private int[] data;

    public void sort(int[] data) {
        this.data = data;
        sort(0, data.length-1);
    }

    public void sort(int p, int r) {
        if(p < r) {
           int q = partition(p, r);
           sort(p, q-1);
           sort(q+1, r);
        }
    }

    private int partition(int p, int r) {
        int q = p;
        for(int j=p; j<r; j++) {
            if(data[j] <= data[r]) {
                swap(j,q);
                q++;
            }
        }
        swap(r,q);
        return q;
    }

    private void swap(int p1, int p2) {
        int tmp = data[p1];
        data[p1] = data[p2];
        data[p2] = tmp;
    }
}

