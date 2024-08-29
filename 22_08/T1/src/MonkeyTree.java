// Aperfeiçoar o algoritimo DFS, pois ele está se perdendo múito facilmente
// Arrumar o algoritimo para quando 
// 28.08 -- Consegui arrumar parcialmente, ele na volta esta pegando números que tem a direita e a esquerda


// Criar um botão para resetar a árvore (recomeçar a renderização)
// Criar um botão para pular a renderização, e retornar a árvoe completa e com a soma máxima correta e mostrando o caminho com maior soma de outra cor (laranja)
// Arrumar a questão do desvio do ponteiro durante uma linha reta (volta)
// Arrumar que a partir do "Casob60.txt" ele está criando quadradinhos vazios
// Mostrar status do andamentdo da árvore (Erro - Em Análise - Completo)
// Tem um botão que leve até o ponteiro, caso esteja em andamento


package src;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Stack;

public class MonkeyTree extends JPanel {

    private char[][] tree;
    private int[][] path;
    private static final int CELL_SIZE = 20;
    private String statusMessage = "Soma atual: 0 | Soma máxima atual: 0";
    private int height, width;
    private int[][] maxPathSums;
    private int maxSum;
    private int startRow, startCol;

    private int currentRow;
    private int currentCol;
    private int currentSum;

    private javax.swing.Timer stepTimer;
    private Stack<int[]> dfsStack;
    private Stack<int[]> maxPathStack;
    private boolean isReturning = false;

    public MonkeyTree(String filePath) {
        this.tree = readTreeFromFile(filePath);
        this.path = new int[tree.length][tree[0].length];
        this.height = tree.length;
        this.width = tree[0].length;
        this.currentSum = 0;
        this.maxSum = 0;
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

        dfsStack = new Stack<>();
        maxPathStack = new Stack<>();
        dfsStack.push(new int[]{startRow, startCol, 0}); // Inicializa a pilha com a raiz (row, col, depth)
        stepTimer = new javax.swing.Timer(200, e -> animateStep());
        stepTimer.start();
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
    }

    private void animateStep() {
        if (!dfsStack.isEmpty()) {
            int[] pos = dfsStack.peek(); // Não removemos ainda, pois podemos voltar a este nó
            currentRow = pos[0];
            currentCol = pos[1];
            int depth = pos[2];
    
            if (isReturning) {
                // Verifica se há caminhos não explorados ao retornar
                if (currentCol > 0 && isValidMove(currentRow - 1, currentCol - 1) && path[currentRow - 1][currentCol - 1] == 0) {
                    isReturning = false; // Encontrou um caminho à esquerda, para de retornar
                } else if (isValidMove(currentRow - 1, currentCol) && path[currentRow - 1][currentCol] == 0) {
                    isReturning = false; // Encontrou um caminho em frente, para de retornar
                } else if (currentCol < width - 1 && isValidMove(currentRow - 1, currentCol + 1) && path[currentRow - 1][currentCol + 1] == 0) {
                    isReturning = false; // Encontrou um caminho à direita, para de retornar
                } else {
                    // Continua a voltar, pinta de verde
                    if (Character.isDigit(tree[currentRow][currentCol])) {
                        currentSum -= Character.getNumericValue(tree[currentRow][currentCol]);
                    }
                    path[currentRow][currentCol] = 2;
                    dfsStack.pop(); // Volta ao nó anterior
                    repaint();
                }
            }
    
            if (!isReturning) {
                if (tree[currentRow][currentCol] == '#') {
                    // Quando atinge uma folha, pinta de verde e volta
                    path[currentRow][currentCol] = 2;
                    isReturning = true;
                    repaint();
                } else {
                    // Marcar o caminho na ida (vermelho)
                    path[currentRow][currentCol] = 1;
                    if (Character.isDigit(tree[currentRow][currentCol])) {
                        currentSum += Character.getNumericValue(tree[currentRow][currentCol]);
                        if (currentSum > maxSum) {
                            maxSum = currentSum;
                            saveMaxPath(); // Salva o caminho de maior soma
                        }
                    }
                    repaint();
    
                    // Adicionar os filhos na pilha (prioridade: esquerda, centro, direita)
                    boolean moved = false;
    
                    if (isValidMove(currentRow - 1, currentCol) && path[currentRow - 1][currentCol] != 2) {
                        // Continuar reto se encontrar '|'
                        dfsStack.push(new int[]{currentRow - 1, currentCol, depth + 1});
                        moved = true;
                    } else if (currentCol > 0 && isValidMove(currentRow - 1, currentCol - 1) && path[currentRow - 1][currentCol - 1] != 2) {
                        dfsStack.push(new int[]{currentRow - 1, currentCol - 1, depth + 1});
                        moved = true;
                    } else if (currentCol < width - 1 && isValidMove(currentRow - 1, currentCol + 1) && path[currentRow - 1][currentCol + 1] != 2) {
                        dfsStack.push(new int[]{currentRow - 1, currentCol + 1, depth + 1});
                        moved = true;
                    }
    
                    if (!moved) {
                        // Se não há movimento possível (todos os caminhos possíveis são verdes), volta
                        isReturning = true;
                    }
                }
            }
        } else {
            // Quando a pilha está vazia, para a animação
            stepTimer.stop();
            paintMaxPath(); // Pinta o caminho de maior valor de laranja
            JOptionPane.showMessageDialog(this, "Soma máxima do caminho: " + maxSum);
        }
    
        // Atualiza o status da soma
        statusMessage = "Soma atual: " + currentSum + " | Soma máxima atual: " + maxSum;
    }
    private void saveMaxPath() {
        maxPathStack.clear();
        for (int[] position : dfsStack) {
            maxPathStack.push(new int[]{position[0], position[1]});
        }
    }

    private void paintMaxPath() {
        while (!maxPathStack.isEmpty()) {
            int[] pos = maxPathStack.pop();
            path[pos[0]][pos[1]] = 3; // Usando 3 para representar o caminho de maior soma (laranja)
        }
        repaint();
    }

    private boolean isValidMove(int row, int col) {
        return row >= 0 && col >= 0 && col < width && tree[row][col] != ' ';
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

                    if (path[i][j] == 1) {
                        g.setColor(Color.RED);
                        g.fillRect(xOffset + j * CELL_SIZE, yOffset + i * CELL_SIZE, CELL_SIZE, CELL_SIZE);
                        g.setColor(Color.WHITE); // Pinta o caractere de branco quando em vermelho
                    } else if (path[i][j] == 2) {
                        g.setColor(Color.GREEN);
                        g.fillRect(xOffset + j * CELL_SIZE, yOffset + i * CELL_SIZE, CELL_SIZE, CELL_SIZE);
                        g.setColor(Color.BLACK); // Pinta o caractere de preto quando em verde
                    } else if (path[i][j] == 3) {
                        g.setColor(Color.ORANGE);
                        g.fillRect(xOffset + j * CELL_SIZE, yOffset + i * CELL_SIZE, CELL_SIZE, CELL_SIZE);
                        g.setColor(Color.BLACK); // Pinta o caractere de preto quando em laranja
                    }
                    g.drawString(Character.toString(tree[i][j]), xOffset + j * CELL_SIZE + 5, yOffset + i * CELL_SIZE + 15);
                }
            }
        }

        g.setColor(Color.WHITE);
        g.drawString(statusMessage, 10, getHeight() - 10);
    }
}