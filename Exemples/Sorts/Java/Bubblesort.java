public class Bubblesort {

    public void sort(int[] values) {
        for(int i=0; i<values.length; i++) {
            boolean trocou = false;
            for(int j=0; j<values.length-i-1; j++) {
                if(values[j] > values[j+1]) {
                    int temp = values[j];
                    values[j] = values[j+1];
                    values[j+1] = temp;
                    trocou = true;
                }
            }
            if(!trocou) break;
        }
    }
}
