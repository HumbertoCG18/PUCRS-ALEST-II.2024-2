// Mover alguns métodos para arquivos diferentes, como: 
    // WindowApp.java == Toda a interface Gráfica do aplicativo
    // Debugger.java == Toda a lógica do debugger
    // Só fazer isso quando todo o código estiver funcionando 100% 

// Está ocorrendo a mudança da direção por conda de que existem galhos muitos próximos uns dos outros, e isso não pode ser um problema, porque quanto maior a árvore, isso acontecerá com mais frequencia, e mais vezes. Tem que se pensar alguma maneira de arrumar este problema.

package src;

import javax.swing.*;

import java.awt.*;
import java.util.List;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class MonkeyTree extends JPanel {

    private char[][] tree;
    private int[][] path;
    private static int CELL_SIZE = 20; // Tornado variável para controle de zoom
    private String treeStatus = "Em Analise";
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

    // Novo componente para acompanhar a câmera
    private JCheckBox followPointerCheckBox;
    // Label para mostrar o status atualizado
    private JLabel statusLabel; 

    // Debug Log Components
    private JButton debugButton;
    private JFrame debugFrame;
    private JTextArea debugTextArea;
    private List<String> debugLogs;

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

        // Inicializa o checkbox e a label de status
        followPointerCheckBox = new JCheckBox("Seguir Ponteiro");
        statusLabel = new JLabel("Soma atual: 0 | Soma maxima atual: 0 | Status da arvore: Em Analise");

        // Debug log initialization
        debugLogs = new ArrayList<>();
        debugTextArea = new JTextArea(20, 50);
        debugTextArea.setEditable(false);
        debugButton = new JButton("Debug Log");
        debugButton.addActionListener(e -> showDebugLog());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            String selectedFile = selectFileDialog();
            if (selectedFile != null) {
                MonkeyTree visualizer = new MonkeyTree(selectedFile);

                JFrame frame = new JFrame("Monkey Tree Visualizer");
                JScrollPane scrollPane = new JScrollPane(visualizer);
                frame.add(scrollPane);

                JPanel panel = new JPanel(new BorderLayout());

                // Criando um sub-painel para os botões e o status
                JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
                JButton resetButton = new JButton("Resetar");
                JButton pauseResumeButton = new JButton("Pausar");
                JButton zoomInButton = new JButton("+");
                JButton zoomOutButton = new JButton("-");

                buttonPanel.add(resetButton);
                buttonPanel.add(pauseResumeButton);
                buttonPanel.add(zoomInButton);
                buttonPanel.add(zoomOutButton);
                // Adiciona o botão Debug Log
                buttonPanel.add(visualizer.debugButton); 

                // Painel para a esquerda onde aparecerá o status e o checkbox
                JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
                // Adiciona o checkbox
                statusPanel.add(visualizer.followPointerCheckBox);
                // Adiciona a label de status
                statusPanel.add(visualizer.statusLabel); 

                // Adiciona o painel de status à esquerda e os botões no centro
                panel.add(statusPanel, BorderLayout.WEST);
                panel.add(buttonPanel, BorderLayout.CENTER);

                frame.add(panel, BorderLayout.SOUTH);

                frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setVisible(true);

                visualizer.startVisualization();

                // Ações de zoom
                zoomInButton.addActionListener(e -> {
                    visualizer.zoomIn();
                    visualizer.revalidate();
                    visualizer.repaint();
                });
                zoomOutButton.addActionListener(e -> {
                    visualizer.zoomOut();
                    visualizer.revalidate();
                    visualizer.repaint();
                });

                // Botões de controle
                resetButton.addActionListener(e -> visualizer.resetVisualization(selectedFile));
                pauseResumeButton.addActionListener(e -> visualizer.togglePauseResume(pauseResumeButton));
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
                        "Selecao de Arquivo",
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

        dfsStack.push(new int[] { startRow, startCol, 0 });
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

    public void zoomIn() {
        CELL_SIZE += 5;
        setPreferredSize(new Dimension(width * CELL_SIZE, height * CELL_SIZE));
    }

    public void zoomOut() {
        if (CELL_SIZE > 5) {
            CELL_SIZE -= 5;
            setPreferredSize(new Dimension(width * CELL_SIZE, height * CELL_SIZE));
        }
    }

    private void resetState() {
        this.path = new int[height][width];
        this.currentSum = 0;
        this.maxSum = 0;
        this.treeStatus = "Em Analise";
        dfsStack.clear();
        maxPathStack.clear();
        isReturning = false;
        isPaused = false;
        debugLogs.clear(); // Limpa o log ao resetar
    }

    private void updateStatusMessage() {
        statusLabel.setText(String.format("Soma atual: %d | Soma maxima atual: %d | Status da arvore: %s", currentSum,
                maxSum, treeStatus));
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

            logDebug(String.format("Posicao atual: (%d, %d), Soma atual: %d", currentRow, currentCol, currentSum));

            // Explorando um novo caminho
            if (!isReturning) {
                path[currentRow][currentCol] = 1;

                // Atualiza a soma quando encontra um número
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

                // Resetando a prioridade se encontrar um 'W'
                if (tree[currentRow][currentCol] == 'W') {
                    logDebug("Encontrado 'W', reiniciando logica de mudanca.");
                    moved = resetDirectionPriority("W", currentRow, currentCol, depth);

                    // Resetando a prioridade se encontrar um 'V'
                } else if (tree[currentRow][currentCol] == 'V') {
                    logDebug("Encontrado 'V', reiniciando logica de mudanca.");
                    moved = resetDirectionPriority("V", currentRow, currentCol, depth);

                } else if (canMove(currentRow, currentCol, "straight")) {
                    // Tenta seguir em frente
                    move(currentRow, currentCol, depth, "straight");
                    moved = true;
                } else if (canMove(currentRow, currentCol, "right")) {
                    // Explora pela direita
                    move(currentRow, currentCol, depth, "right");
                    moved = true;
                } else if (canMove(currentRow, currentCol, "left")) {
                    // Explora pela esquerda
                    move(currentRow, currentCol, depth, "left");
                    moved = true;
                }

                // Se não conseguiu se mover, inicia o retorno
                if (!moved) {
                    isReturning = true;
                }

            } else {
                // Processo de retorno/backtracking (sem reset de direção)
                treeStatus = "Em Analise";
                logDebug("Retornando...");

                // Backtracking normal: não aplicar o reset aqui
                if (tree[currentRow][currentCol] == 'W') {
                    logDebug(String.format("Retornando de %c", tree[currentRow][currentCol]));

                    // Prioridade para 'W' no backtracking: segue o comportamento padrão sem reset
                    if (canMove(currentRow, currentCol, "left")) {
                        move(currentRow, currentCol, depth, "left");
                        isReturning = false;
                    } else if (canMove(currentRow, currentCol, "straight")) {
                        move(currentRow, currentCol, depth, "straight");
                        isReturning = false;
                    } else if (canMove(currentRow, currentCol, "right")) {
                        move(currentRow, currentCol, depth, "right");
                        isReturning = false;
                    } else {
                        processReturning();
                    }

                } else if (tree[currentRow][currentCol] == 'V') {
                    logDebug(String.format("Retornando de %c", tree[currentRow][currentCol]));

                    // Prioridade para 'V' no backtracking: segue o comportamento padrão sem reset
                    if (canMove(currentRow, currentCol, "left")) {
                        move(currentRow, currentCol, depth, "left");
                        isReturning = false;
                    } else if (canMove(currentRow, currentCol, "right")) {
                        move(currentRow, currentCol, depth, "right");
                        isReturning = false;
                    } else {
                        processReturning();
                    }

                } else {
                    processReturning();
                }
            }
        } else {
            paintMaxPath();
        }
        updateStatusMessage();
    }

    private boolean resetDirectionPriority(String letter, int row, int col, int depth) {
        boolean moved = false;

        if (letter.equals("W")) {
            // Reiniciando a prioridade de direção para 'W'
            logDebug(String.format("Reiniciando prioridade em W na posicao (%d, %d)", row, col));

            // Prioridade para 'W': Esquerda -> Reto -> Direita
            if (canMove(row, col, "left")) {
                logDebug(String.format("Movendo para a esquerda de W em (%d, %d)", row, col));
                move(row, col, depth, "left");
                moved = true;
            } else if (canMove(row, col, "straight")) {
                logDebug(String.format("Movendo para frente de W em (%d, %d)", row, col));
                move(row, col, depth, "straight");
                moved = true;
            } else if (canMove(row, col, "right")) {
                logDebug(String.format("Movendo para a direita de W em (%d, %d)", row, col));
                move(row, col, depth, "right");
                moved = true;
            }

        } else if (letter.equals("V")) {
            // Reiniciando a prioridade de direção para 'V'
            logDebug(String.format("Reiniciando prioridade em V na posicao (%d, %d)", row, col));

            // Prioridade para 'V': Esquerda -> Direita
            if (canMove(row, col, "left")) {
                logDebug(String.format("Movendo para a esquerda de V em (%d, %d)", row, col));
                move(row, col, depth, "left");
                moved = true;
            } else if (canMove(row, col, "right")) {
                logDebug(String.format("Movendo para a direita de V em (%d, %d)", row, col));
                move(row, col, depth, "right");
                moved = true;
            }
        }

        return moved;
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
        // Se começar com 'W' na raiz, use isso como ponto de partida
        for (int col = 0; col < width; col++) {
            if (tree[height - 1][col] == 'W') {
                startRow = height - 1;
                startCol = col;
                currentRow = startRow;
                currentCol = startCol;
                return;
            }
        }
    }

    private boolean canMove(int row, int col, String direction) {
        int newRow = row - 1;
        int newCol = col;

        if (direction.equals("left")) {
            newCol = col - 1;
            while (newRow >= 0 && newCol >= 0
                    && (path[newRow][newCol] != 0 || tree[newRow][newCol] == '|' || tree[newRow][newCol] == '/')) {
                newRow--;
                newCol--;
            }
        } else if (direction.equals("right")) {
            newCol = col + 1;
            while (newRow >= 0 && newCol < width
                    && (path[newRow][newCol] != 0 || tree[newRow][newCol] == '|' || tree[newRow][newCol] == '\\')) {
                newRow--;
                newCol++;
            }
        } else if (direction.equals("straight")) {
            while (newRow >= 0
                    && (path[newRow][newCol] != 0 || tree[newRow][newCol] == '\\' || tree[newRow][newCol] == '/')) {
                newRow--;
            }
        }

        return newCol >= 0 && newCol < width && newRow >= 0 && isValidMove(newRow, newCol) && path[newRow][newCol] == 0;
    }

    private void move(int row, int col, int depth, String direction) {
        int newRow = row - 1;
        int newCol = col;

        if (direction.equals("left")) {
            newCol = col - 1;
            // Pula células já visitadas ou não válidas até encontrar um número ou uma célula válida
            while (newRow >= 0 && newCol >= 0
                    && (path[newRow][newCol] != 0 || tree[newRow][newCol] == '|' || tree[newRow][newCol] == '/')) {
                newRow--;
                newCol--;
            }
        } else if (direction.equals("right")) {
            newCol = col + 1;
            // Pula células já visitadas ou não válidas até encontrar um número ou uma célula válida
            while (newRow >= 0 && newCol < width
                    && (path[newRow][newCol] != 0 || tree[newRow][newCol] == '|' || tree[newRow][newCol] == '\\')) {
                newRow--;
                newCol++;
            }
        } else if (direction.equals("straight")) {
            // Pula células já visitadas ou não válidas até encontrar um número ou uma célula válida
            while (newRow >= 0
                    && (path[newRow][newCol] != 0 || tree[newRow][newCol] == '\\' || tree[newRow][newCol] == '/')) {
                newRow--;
            }
        }

        // Verifica se o novo caminho oferece um benefício (não pula números)
        if (newRow >= 0 && newCol >= 0 && newCol < width
                && (path[newRow][newCol] == 0 || Character.isDigit(tree[newRow][newCol]))) {
            dfsStack.push(new int[] { newRow, newCol, depth + 1 });
        }
    }

    private boolean isValidMove(int row, int col) {
        return row >= 0 && col >= 0 && col < width && tree[row][col] != ' ';
    }

    private void processReturning() {
        if (Character.isDigit(tree[currentRow][currentCol])) {
            int value = Character.getNumericValue(tree[currentRow][currentCol]);
            if (value > 0 && currentSum > 0) {
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
            maxPathStack.push(new int[] { position[0], position[1] });
        }
    }

    private void paintMaxPath() {
        while (!maxPathStack.isEmpty()) {
            int[] pos = maxPathStack.pop();
            path[pos[0]][pos[1]] = 3;
        }
        repaint();
    }

    private void showDebugLog() {
        if (debugFrame == null) {
            debugFrame = new JFrame("Debug Log");
            debugFrame.setSize(600, 400);
            debugFrame.add(new JScrollPane(debugTextArea));
        }

        debugTextArea.setText(String.join("\n", debugLogs));
        debugFrame.setVisible(true);
    }

    private void logDebug(String message) {
        debugLogs.add(message);
        // Atualiza o debug log em tempo real
        debugTextArea.setText(String.join("\n", debugLogs));
        debugTextArea.revalidate();
        debugTextArea.repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        int xOffset = (getWidth() - tree[0].length * CELL_SIZE) / 2;
        int yOffset = (getHeight() - tree.length * CELL_SIZE) / 2;

        for (int i = 0; i < tree.length; i++) {
            for (int j = 0; j < tree[0].length; j++) {
                if (tree[i][j] != ' ' && tree[i][j] != '\0') {
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
                    g.drawString(Character.toString(tree[i][j]), xOffset + j * CELL_SIZE + 5,
                            yOffset + i * CELL_SIZE + 15);
                }
            }
        }

        g.setColor(Color.WHITE);
    }
}