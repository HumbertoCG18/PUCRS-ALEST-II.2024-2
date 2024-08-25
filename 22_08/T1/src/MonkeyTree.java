package src;

//TODO IMPLEMENTAR A ANIMAÇÃO DE LEITURA DA ÁRVORE DE IDA E DE VOLTA

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;

public class MonkeyTree extends JPanel {

    private char[][] tree;
    private int[][] path;
    private static final int CELL_SIZE = 20;
    private String statusMessage = "Parcial - ";
    private int height, width;
    private int[][] maxPathSums;
    private int maxSum;
    private int startRow, startCol;

    private int currentRow;
    private int currentCol;

    public MonkeyTree(String filePath) {
        this.tree = readTreeFromFile(filePath);
        this.path = new int[tree.length][tree[0].length];
        this.height = tree.length;
        this.width = tree[0].length;
        setPreferredSize(new Dimension(tree[0].length * CELL_SIZE, tree.length * CELL_SIZE));
        setBackground(Color.BLACK);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            String selectedFile = selectFileDialog();
            if (selectedFile != null) {
                MonkeyTree visualizer = new MonkeyTree(selectedFile);

                JFrame frame = new JFrame("Monkey Tree Visualizer");
                JScrollPane scrollPane = new JScrollPane(visualizer);
                frame.add(scrollPane);

                frame.setExtendedState(JFrame.MAXIMIZED_BOTH); // Janela maximizada
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setVisible(true);

                visualizer.startVisualization();
            }
        });
    }

    private static String selectFileDialog() {
        String selectedFile = null;
        try {
            File dir = new File("../Casos").getCanonicalFile(); // Caminho absoluto relativo ao diretório atual
            String[] txtFiles = dir.list((d, name) -> name.endsWith(".txt"));

            if (txtFiles != null && txtFiles.length > 0) {
                Arrays.sort(txtFiles, Comparator.comparingInt(a -> Integer.parseInt(a.replaceAll("\\D", ""))));

                selectedFile = (String) JOptionPane.showInputDialog(
                        null,
                        "Escolha um arquivo TXT para visualizar:",
                        "Seleção de Arquivo",
                        JOptionPane.PLAIN_MESSAGE,
                        null,
                        txtFiles,
                        txtFiles[0]);

                if (selectedFile != null) {
                    return new File(dir, selectedFile).getAbsolutePath();
                }
            } else {
                JOptionPane.showMessageDialog(null, "Nenhum arquivo TXT encontrado na pasta 'Casos'.");
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Erro ao acessar a pasta: " + e.getMessage());
            e.printStackTrace();
        }
        return selectedFile;
    }

    private char[][] readTreeFromFile(String filePath) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath, StandardCharsets.UTF_8))) {
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

    public void startVisualization() {
        findStartingPoint();
        calculateMaxPath(); // Preprocessa a árvore para calcular os valores máximos
        maxSum = dfs(startRow, startCol); // Inicia a DFS a partir da raiz usando os valores precomputados
        animateReturnPath(); // Anima a volta pelo melhor caminho encontrado
    }

    private void findStartingPoint() {
        for (int col = 0; col < width; col++) {
            // Procura pela raiz na última linha (deve ser um tronco '|')
            if (tree[height - 1][col] == '|') {
                startRow = height - 1;
                startCol = col;
                currentRow = startRow;
                currentCol = startCol;
                return;
            }
        }
    }

    private void calculateMaxPath() {
        maxPathSums = new int[height][width];
        path = new int[height][width];
        maxSum = 0;

        for (int j = 0; j < width; j++) {
            if (Character.isDigit(tree[height - 1][j])) {
                maxPathSums[height - 1][j] = Character.getNumericValue(tree[height - 1][j]);
            }
        }

        for (int i = height - 2; i >= 0; i--) {
            for (int j = 0; j < width; j++) {
                if (tree[i][j] == '/' || tree[i][j] == '\\' || tree[i][j] == '|' || tree[i][j] == 'V'
                        || tree[i][j] == 'W') {
                    int left = (j > 0) ? maxPathSums[i + 1][j - 1] : Integer.MIN_VALUE;
                    int right = (j < width - 1) ? maxPathSums[i + 1][j + 1] : Integer.MIN_VALUE;
                    int straight = maxPathSums[i + 1][j];

                    maxPathSums[i][j] = Math.max(Math.max(left, right), straight);

                    if (tree[i][j] == 'V' || tree[i][j] == 'W') {
                        maxPathSums[i][j] += 1;
                    }

                    if (Character.isDigit(tree[i][j])) {
                        maxPathSums[i][j] += Character.getNumericValue(tree[i][j]);
                    }
                }
            }
        }

        currentRow = startRow;
        currentCol = startCol;
    }

    private int dfs(int row, int col) {
        if (row < 0 || col < 0 || col >= width || tree[row][col] == ' ') {
            return 0; // Fora dos limites ou caminho inválido
        }

        if (tree[row][col] == '#') {
            return 0; // Alcançou uma folha
        }

        // Marcar o caminho na ida
        path[row][col] = 1;
        repaint();

        int leftCol = col - 1;
        int rightCol = col + 1;

        // Explora os três possíveis caminhos usando maxPathSums como referência
        int leftSum = (leftCol >= 0) ? dfs(row - 1, leftCol) + maxPathSums[row][col] : Integer.MIN_VALUE;
        int rightSum = (rightCol < width) ? dfs(row - 1, rightCol) + maxPathSums[row][col] : Integer.MIN_VALUE;
        int straightSum = dfs(row - 1, col) + maxPathSums[row][col];

        // Encontra o maior valor entre os caminhos
        int maxSumPath = Math.max(Math.max(leftSum, rightSum), straightSum);

        // Desmarcar o caminho após voltar (para explorar outras possibilidades)
        path[row][col] = 0;

        return maxSumPath;
    }

    private void animateReturnPath() {
        Timer returnTimer = new Timer(200, e -> {
            if (currentRow < height - 1) {
                // Segue o caminho marcado na ida, ao contrário, para retornar
                if (currentRow < height - 1 && currentCol > 0 && path[currentRow + 1][currentCol - 1] == 1) {
                    currentCol--;  // Vai para a esquerda
                } else if (currentRow < height - 1 && currentCol < width - 1 && path[currentRow + 1][currentCol + 1] == 1) {
                    currentCol++;  // Vai para a direita
                }
                currentRow++;
                path[currentRow][currentCol] = 2;  // Marca o caminho de volta em verde
                repaint();
            } else {
                ((Timer) e.getSource()).stop();
                JOptionPane.showMessageDialog(this, "Soma máxima do caminho: " + maxSum);
            }
        });
        returnTimer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        int xOffset = (getWidth() - tree[0].length * CELL_SIZE) / 2;
        int yOffset = (getHeight() - tree.length * CELL_SIZE) / 2;

        for (int i = 0; i < tree.length; i++) {
            for (int j = 0; j < tree[0].length; j++) {
                if (tree[i][j] != ' ') {
                    g.setColor(Color.WHITE);
                    g.drawRect(xOffset + j * CELL_SIZE, yOffset + i * CELL_SIZE, CELL_SIZE, CELL_SIZE);
                    g.drawString(Character.toString(tree[i][j]), xOffset + j * CELL_SIZE + 5, yOffset + i * CELL_SIZE + 15);

                    if (path[i][j] == 1) {
                        g.setColor(Color.RED);
                        g.fillRect(xOffset + j * CELL_SIZE, yOffset + i * CELL_SIZE, CELL_SIZE, CELL_SIZE);
                    } else if (path[i][j] == 2) {
                        g.setColor(Color.GREEN);
                        g.fillRect(xOffset + j * CELL_SIZE, yOffset + i * CELL_SIZE, CELL_SIZE, CELL_SIZE);
                    }
                }
            }
        }

        g.setColor(Color.WHITE);
        g.drawString(statusMessage, 10, getHeight() - 10);
    }
}
