public class Compara {

    public static void main(String[] args) {

        In arq = new In("DomCasmurro_utf8.txt");
        String[] words = arq.readAllStrings();
        arq.close();

        //SeparateChainingHashST<String, Integer> hash = new SeparateChainingHashST<>();
        LinearProbingHashST<String, Integer> hash = new LinearProbingHashST<>();        

        for (String word : words) {
            Integer aux = hash.get(word.toLowerCase());
            if(aux==null) aux=1;
            else aux++;
            hash.put(word.toLowerCase(), aux);
        }

        for(String key : hash.keys())
            System.out.println("Chave ["+key+"] = "+hash.get(key));
    }
}
