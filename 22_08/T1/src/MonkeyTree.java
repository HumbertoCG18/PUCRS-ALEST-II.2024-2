// Tem que arrumar novamente o algoritimo de exploração, pois ele está mudando de direção mesmo sem um W ou V, uma possivel solução é que, ele só pode mudar de direção caso encontre um W ou um V, 
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
    private String treeStatus = "Em Análise";
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
    private boolean isPaused = false;

    public MonkeyTree(String filePath) {
        this.tree = readTreeFromFile(filePath);
        this.path = new int[tree.length][tree[0].length];
        this.height = tree.length;
        this.width = tree[0].length;
        this.currentSum = 0;
        this.maxSum = 0;

        this.dfsStack = new Stack<>();
        this.maxPathStack = new Stack<>();

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

                JPanel panel = new JPanel();
                JButton resetButton = new JButton("Resetar");
                JButton pauseResumeButton = new JButton("Pausar");
                JButton centerButton = new JButton("Centralizar no Ponteiro");

                resetButton.addActionListener(e -> visualizer.resetVisualization(selectedFile));
                pauseResumeButton.addActionListener(e -> visualizer.togglePauseResume(pauseResumeButton));
                centerButton.addActionListener(e -> visualizer.centerOnPointer(scrollPane));

                panel.add(resetButton);
                panel.add(pauseResumeButton);
                panel.add(centerButton);
                frame.add(panel, BorderLayout.SOUTH);

                frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setVisible(true);

                visualizer.startVisualization();
            }
        });
    }

    private static String selectFileDialog() {
        String selectedFile = null;
        try {
            File dir = new File("../Casos").getCanonicalFile();
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
        resetState();
        findStartingPoint();
        calculateMaxPath();

        dfsStack.push(new int[]{startRow, startCol, 0});
        stepTimer = new javax.swing.Timer(200, e -> animateStep());
        stepTimer.start();
    }

    public void resetVisualization(String filePath) {
        if (stepTimer != null) {
            stepTimer.stop();
        }
        this.tree = readTreeFromFile(filePath);
        resetState();
        startVisualization();
    }

    public void togglePauseResume(JButton button) {
        if (isPaused) {
            stepTimer.start();
            button.setText("Pausar");
        } else {
            stepTimer.stop();
            button.setText("Retomar");
        }
        isPaused = !isPaused;
    }

    public void centerOnPointer(JScrollPane scrollPane) {
        if (!isPaused && !dfsStack.isEmpty()) {
            int[] pos = dfsStack.peek();
            Rectangle rect = new Rectangle(pos[1] * CELL_SIZE, pos[0] * CELL_SIZE, CELL_SIZE, CELL_SIZE);
            scrollPane.getViewport().scrollRectToVisible(rect);
        }
    }

    private void resetState() {
        this.path = new int[height][width];
        this.currentSum = 0;
        this.maxSum = 0;
        this.treeStatus = "Em Análise";
        dfsStack.clear();
        maxPathStack.clear();
        isReturning = false;
        isPaused = false;
    }

    private void findStartingPoint() {
        for (int col = 0; col < width; col++) {
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
            int[] pos = dfsStack.peek();
            currentRow = pos[0];
            currentCol = pos[1];
            int depth = pos[2];

            if (isReturning) {
                treeStatus = "Em Análise";  // Muda o status para "Em Análise" durante o retorno

                if (tree[currentRow][currentCol] == 'V') {
                    if (canMove(currentRow, currentCol, "left")) {
                        isReturning = false;
                        move(currentRow, currentCol, depth, "left");
                    } else if (canMove(currentRow, currentCol, "right")) {
                        isReturning = false;
                        move(currentRow, currentCol, depth, "right");
                    } else {
                        processReturning();
                    }
                } else if (tree[currentRow][currentCol] == 'W') {
                    if (canMove(currentRow, currentCol, "left")) {
                        isReturning = false;
                        move(currentRow, currentCol, depth, "left");
                    } else if (canMove(currentRow, currentCol, "straight")) {
                        isReturning = false;
                        move(currentRow, currentCol, depth, "straight");
                    }                     else if (canMove(currentRow, currentCol, "right")) {
                        isReturning = false;
                        move(currentRow, currentCol, depth, "right");
                    } else {
                        processReturning();
                    }
                } else {
                    processReturning();
                }
            }

            if (!isReturning) {
                if (tree[currentRow][currentCol] == '#') {
                    path[currentRow][currentCol] = 2;
                    isReturning = true;
                    repaint();
                } else {
                    path[currentRow][currentCol] = 1;
                    if (Character.isDigit(tree[currentRow][currentCol])) {
                        int value = Character.getNumericValue(tree[currentRow][currentCol]);
                        if (value > 0) {
                            currentSum += value;
                        }
                        if (currentSum > maxSum) {
                            maxSum = currentSum;
                            saveMaxPath();
                        }
                    }
                    repaint();

                    boolean moved = false;

                    // Ajusta para aceitar dois números em sequência
                    if (tree[currentRow][currentCol] == 'W') {
                        if (canMove(currentRow, currentCol, "left")) {
                            move(currentRow, currentCol, depth, "left");
                            moved = true;
                        } else if (canMove(currentRow, currentCol, "straight")) {
                            move(currentRow, currentCol, depth, "straight");
                            moved = true;
                        } else if (canMove(currentRow, currentCol, "right")) {
                            move(currentRow, currentCol, depth, "right");
                            moved = true;
                        }
                    } else if (tree[currentRow][currentCol] == 'V') {
                        if (canMove(currentRow, currentCol, "left")) {
                            move(currentRow, currentCol, depth, "left");
                            moved = true;
                        } else if (canMove(currentRow, currentCol, "right")) {
                            move(currentRow, currentCol, depth, "right");
                            moved = true;
                        }
                    } else {
                        if (canMove(currentRow, currentCol, "straight")) {
                            move(currentRow, currentCol, depth, "straight");
                            moved = true;
                        } else if (canMove(currentRow, currentCol, "left")) {
                            move(currentRow, currentCol, depth, "left");
                            moved = true;
                        } else if (canMove(currentRow, currentCol, "right")) {
                            move(currentRow, currentCol, depth, "right");
                            moved = true;
                        }
                    }

                    if (!moved) {
                        isReturning = true;
                    }
                }
            }
        } else {
            stepTimer.stop();
            paintMaxPath();
            treeStatus = "Completo";
            JOptionPane.showMessageDialog(this, "Soma máxima do caminho: " + maxSum);
        }

        statusMessage = String.format("Soma atual: %d | Soma máxima atual: %d", currentSum, maxSum);
    }

    private void processReturning() {
        if (Character.isDigit(tree[currentRow][currentCol])) {
            int value = Character.getNumericValue(tree[currentRow][currentCol]);
            if (value > 0 && currentSum > 0) {  // Evitar que a soma vá para valores negativos
                currentSum -= value;
            }
        }
        path[currentRow][currentCol] = 2;
        dfsStack.pop();
        repaint();
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
            path[pos[0]][pos[1]] = 3;
        }
        repaint();
    }

    private boolean canMove(int row, int col, String direction) {
        int newRow = row - 1;
        int newCol = col;
        if (direction.equals("left")) {
            newCol = col - 1;
            while (newRow >= 0 && (tree[newRow][newCol] == '|' || tree[newRow][newCol] == '/')) {
                newRow--;
                newCol--;
            }
        } else if (direction.equals("right")) {
            newCol = col + 1;
            while (newRow >= 0 && (tree[newRow][newCol] == '|' || tree[newRow][newCol] == '\\')) {
                newRow--;
                newCol++;
            }
        } else if (direction.equals("straight")) {
            while (newRow >= 0 && (tree[newRow][newCol] == '\\' || tree[newRow][newCol] == '/')) {
                newRow--;
            }
        }
        return newCol >= 0 && newCol < width && isValidMove(newRow, newCol) && path[newRow][newCol] == 0;
    }

    private void move(int row, int col, int depth, String direction) {
        int newRow = row - 1;
        int newCol = col;
        if (direction.equals("left")) {
            newCol = col - 1;
            while (newRow >= 0 && (tree[newRow][newCol] == '|' || tree[newRow][newCol] == '/')) {
                newRow--;
                newCol--;
            }
        } else if (direction.equals("right")) {
            newCol = col + 1;
            while (newRow >= 0 && (tree[newRow][newCol] == '|' || tree[newRow][newCol] == '\\')) {
                newRow--;
                newCol++;
            }
        } else if (direction.equals("straight")) {
            while (newRow >= 0 && (tree[newRow][newCol] == '\\' || tree[newRow][newCol] == '/')) {
                newRow--;
            }
        }
        dfsStack.push(new int[]{newRow, newCol, depth + 1});
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
                if (tree[i][j] != ' ' && tree[i][j] != '\0') { // Evitar quadrados vazios
                    g.setColor(Color.WHITE);
                    g.drawRect(xOffset + j * CELL_SIZE, yOffset + i * CELL_SIZE, CELL_SIZE, CELL_SIZE);

                    if (path[i][j] == 1) {
                        g.setColor(Color.RED);
                        g.fillRect(xOffset + j * CELL_SIZE, yOffset + i * CELL_SIZE, CELL_SIZE, CELL_SIZE);
                        g.setColor(Color.WHITE);
                    } else if (path[i][j] == 2) {
                        g.setColor(Color.GREEN);
                        g.fillRect(xOffset + j * CELL_SIZE, yOffset + i * CELL_SIZE, CELL_SIZE, CELL_SIZE);
                        g.setColor(Color.BLACK);
                    } else if (path[i][j] == 3) {
                        g.setColor(Color.ORANGE);
                        g.fillRect(xOffset + j * CELL_SIZE, yOffset + i * CELL_SIZE, CELL_SIZE, CELL_SIZE);
                        g.setColor(Color.BLACK);
                    }
                    g.drawString(Character.toString(tree[i][j]), xOffset + j * CELL_SIZE + 5, yOffset + i * CELL_SIZE + 15);
                }
            }
        }

        g.setColor(Color.WHITE);
        g.drawString(statusMessage + " | Status da árvore: " + treeStatus, 10, getHeight() - 10);
    }
}