public class Compara {

    public static void main(String[] args) {

        In arq = new In("DomCasmurro_utf8.txt");
        String[] words = arq.readAllStrings();
        arq.close();

        for (String word : words) {
            System.out.println(word);
        }
    }
}
