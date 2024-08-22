import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;

public class MonkeyTree {

    public static void main(String[] args) {
        String filePath = "/mnt/data/casob30.txt";  // Substitua pelo caminho do arquivo correto
        char[][] tree = readTreeFromFile(filePath);
        
        if (tree != null) {
            int maxScore = findMaxPath(tree);
            System.out.println("Soma máxima do caminho: " + maxScore);
        } else {
            System.out.println("Erro ao ler o arquivo.");
        }
    }

    // Função para ler a árvore a partir de um arquivo de texto
    public static char[][] readTreeFromFile(String filePath) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String[] dimensions = reader.readLine().split(" ");
            int rows = Integer.parseInt(dimensions[0]);
            int cols = Integer.parseInt(dimensions[1]);

            char[][] tree = new char[rows][cols];

            String line;
            int row = 0;
            while ((line = reader.readLine()) != null) {
                for (int col = 0; col < line.length(); col++) {
                    tree[row][col] = line.charAt(col);
                }
                row++;
            }
            return tree;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    // Função para encontrar o caminho de maior soma
    public static int findMaxPath(char[][] tree) {
        int height = tree.length;
        int width = tree[0].length;
        int[][] maxPathSums = new int[height][width];

        // Inicializa o array maxPathSums com zeros
        for (int[] row : maxPathSums) {
            Arrays.fill(row, 0);
        }

        // Copia os valores da última linha da árvore (as folhas)
        for (int j = 0; j < width; j++) {
            if (Character.isDigit(tree[height - 1][j])) {
                maxPathSums[height - 1][j] = Character.getNumericValue(tree[height - 1][j]);
            }
        }

        // Calcula a soma máxima de baixo para cima
        for (int i = height - 2; i >= 0; i--) {
            for (int j = 0; j < width; j++) {
                if (tree[i][j] == '/' || tree[i][j] == '\\' || tree[i][j] == '|' || tree[i][j] == 'V' || tree[i][j] == 'W') {
                    int left = j > 0 ? maxPathSums[i + 1][j - 1] : 0;
                    int right = j < width - 1 ? maxPathSums[i + 1][j + 1] : 0;
                    maxPathSums[i][j] = Math.max(left, right);
                    if (Character.isDigit(tree[i][j])) {
                        maxPathSums[i][j] += Character.getNumericValue(tree[i][j]);
                    }
                }
            }
        }

        // A soma máxima estará na primeira linha
        int maxSum = 0;
        for (int j = 0; j < width; j++) {
            maxSum = Math.max(maxSum, maxPathSums[0][j]);
        }

        return maxSum;
    }
}
