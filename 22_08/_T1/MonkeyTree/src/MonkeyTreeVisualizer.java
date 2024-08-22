import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;

public class MonkeyTreeVisualizer extends JPanel {

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

    public MonkeyTreeVisualizer(String filePath) {
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
                MonkeyTreeVisualizer visualizer = new MonkeyTreeVisualizer("lib/" + selectedFile);

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

    // Exibe um diálogo para seleção de arquivo com arquivos em ordem crescente
    private static String selectFileDialog() {
        File dir = new File("lib");
        String[] txtFiles = dir.list((d, name) -> name.endsWith(".txt"));

        if (txtFiles != null && txtFiles.length > 0) {
            // Ordena os arquivos em ordem crescente pelo nome
            Arrays.sort(txtFiles, Comparator.comparingInt(a -> Integer.parseInt(a.replaceAll("\\D", ""))));

            String selectedFile = (String) JOptionPane.showInputDialog(
                    null,
                    "Escolha um arquivo TXT para visualizar:",
                    "Seleção de Arquivo",
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    txtFiles,
                    txtFiles[0]);

            return selectedFile;
        } else {
            JOptionPane.showMessageDialog(null, "Nenhum arquivo TXT encontrado na pasta 'lib'.");
            return null;
        }
    }

    // Lê a árvore a partir de um arquivo
    private char[][] readTreeFromFile(String filePath) {
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

    // Método que inicia a visualização
    public void startVisualization() {
        findStartingPoint(); // Encontra a posição inicial correta
        calculateMaxPath();  // Calcula o caminho máximo primeiro
        Timer timer = new Timer(200, e -> animateStep());
        timer.start();
    }

    // Encontra o ponto inicial na base da árvore
    private void findStartingPoint() {
        startRow = height - 1; // Inicia na última linha
        for (int col = 0; col < width; col++) {
            if (tree[startRow][col] == '|') {
                startCol = col;
                break;
            }
        }
    }

    // Calcula o caminho máximo da raiz até a folha de maior valor
    private void calculateMaxPath() {
        maxPathSums = new int[height][width];
        path = new int[height][width];
        maxSum = 0;

        // Inicializa a última linha (raiz) com valores
        for (int j = 0; j < width; j++) {
            if (Character.isDigit(tree[height - 1][j])) {
                maxPathSums[height - 1][j] = Character.getNumericValue(tree[height - 1][j]);
            }
        }

        // Calcula a soma máxima de baixo para cima, incluindo a consideração dos nós V e W
        for (int i = height - 2; i >= 0; i--) {
            for (int j = 0; j < width; j++) {
                if (tree[i][j] == '/' || tree[i][j] == '\\' || tree[i][j] == '|' || tree[i][j] == 'V' || tree[i][j] == 'W') {
                    int left = (j > 0) ? maxPathSums[i + 1][j - 1] : Integer.MIN_VALUE;
                    int right = (j < width - 1) ? maxPathSums[i + 1][j + 1] : Integer.MIN_VALUE;
                    int straight = maxPathSums[i + 1][j];

                    maxPathSums[i][j] = Math.max(Math.max(left, right), straight);

                    // Considera os nós 'V' e 'W' como parte do caminho
                    if (tree[i][j] == 'V' || tree[i][j] == 'W') {
                        maxPathSums[i][j] += 1;
                    }

                    if (Character.isDigit(tree[i][j])) {
                        maxPathSums[i][j] += Character.getNumericValue(tree[i][j]);
                    }
                }
            }
        }

        // Determina o ponto inicial para a animação (início na base da árvore)
        currentRow = startRow;
        currentCol = startCol;
    }

    // Anima passo a passo da raiz até a folha de maior valor
    private void animateStep() {
        if (currentRow >= 0) {
            path[currentRow][currentCol] = 1;  // Caminho de ida em vermelho
            repaint();
            updateStatusMessage();  // Atualiza o status na parte inferior

            // Determina o próximo passo com base na soma máxima
            if (currentRow > 0) {
                int leftCol = currentCol - 1;
                int rightCol = currentCol + 1;

                // Verificar e corrigir possíveis índices fora dos limites
                int leftValue = (leftCol >= 0 && leftCol < width) ? maxPathSums[currentRow - 1][leftCol] : Integer.MIN_VALUE;
                int rightValue = (rightCol >= 0 && rightCol < width) ? maxPathSums[currentRow - 1][rightCol] : Integer.MIN_VALUE;
                int straightValue = maxPathSums[currentRow - 1][currentCol];

                if (leftValue >= rightValue && leftValue >= straightValue && leftCol >= 0) {
                    currentCol = leftCol;
                } else if (rightValue > leftValue && rightValue >= straightValue && rightCol < width) {
                    currentCol = rightCol;
                }
            }

            currentRow--;
        } else {
            calculateFinalSum();
            animateReturnPath();
        }
    }

    // Calcula a soma final do caminho
    private void calculateFinalSum() {
        int sum = 0;
        int col = currentCol;
        for (int i = 0; i < height; i++) {
            if (Character.isDigit(tree[i][col])) {
                sum += Character.getNumericValue(tree[i][col]);
            }
            if (i < height - 1) {
                int leftCol = col - 1;
                int rightCol = col + 1;

                int leftValue = (leftCol >= 0 && leftCol < width) ? maxPathSums[i + 1][leftCol] : Integer.MIN_VALUE;
                int rightValue = (rightCol >= 0 && rightCol < width) ? maxPathSums[i + 1][rightCol] : Integer.MIN_VALUE;
                int straightValue = maxPathSums[i + 1][col];

                if (leftValue >= rightValue && leftValue >= straightValue && leftCol >= 0) {
                    col = leftCol;
                } else if (rightValue >= leftValue && rightValue >= straightValue && rightCol < width) {
                    col = rightCol;
                } 
                // Se nem leftValue nem rightValue são melhores, col permanece o mesmo
            }
        }
        maxSum = sum;
        statusMessage = "Completo - Soma máxima: " + maxSum;
        repaint();
    }

    // Anima a volta do caminho (verde) para indicar que o macaquinho passou por ali
    private void animateReturnPath() {
        Timer returnTimer = new Timer(200, e -> {
            if (currentRow < height - 1) {
                currentRow++;
                path[currentRow][currentCol] = 2;  // Caminho de volta em verde
                repaint();
            } else {
                ((Timer) e.getSource()).stop();
                JOptionPane.showMessageDialog(this, "Soma máxima do caminho: " + maxSum);
            }
        });
        returnTimer.start();
    }

    // Atualiza a mensagem de status na parte inferior da tela
    private void updateStatusMessage() {
        statusMessage = "Parcial - Caminho percorrido até agora";
    }

    // Método de desenho da árvore
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Calcular o deslocamento para centralizar a árvore
        int xOffset = (getWidth() - tree[0].length * CELL_SIZE) / 2;
        int yOffset = (getHeight() - tree.length * CELL_SIZE) / 2;

        for (int i = 0; i < tree.length; i++) {
            for (int j = 0; j < tree[0].length; j++) {
                if (tree[i][j] != ' ') {
                    g.setColor(Color.WHITE);  // Cor da árvore em branco
                    g.drawRect(xOffset + j * CELL_SIZE, yOffset + i * CELL_SIZE, CELL_SIZE, CELL_SIZE);
                    g.drawString(Character.toString(tree[i][j]), xOffset + j * CELL_SIZE + 5, yOffset + i * CELL_SIZE + 15);

                    if (path[i][j] == 1) {
                        g.setColor(Color.RED);  // Caminho de ida em vermelho
                        g.fillRect(xOffset + j * CELL_SIZE, yOffset + i * CELL_SIZE, CELL_SIZE, CELL_SIZE);
                    } else if (path[i][j] == 2) {
                        g.setColor(Color.GREEN);  // Caminho de volta em verde
                        g.fillRect(xOffset + j * CELL_SIZE, yOffset + i * CELL_SIZE, CELL_SIZE, CELL_SIZE);
                    }
                }
            }
        }

        // Desenha a mensagem de status na parte inferior
        g.setColor(Color.WHITE);
        g.drawString(statusMessage, 10, getHeight() - 10);
    }
}
