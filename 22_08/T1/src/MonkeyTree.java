package src;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.List;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class MonkeyTree extends JPanel {

    private char[][] tree;
    private int[][] path;
    private static int CELL_SIZE = 20;
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

    // Componentes para controle da câmera e status
    private JCheckBox followPointerCheckBox;
    private JLabel statusLabel;
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

                // Sub-painel para botões e status
                JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
                JButton resetButton = new JButton("Resetar");
                JButton pauseResumeButton = new JButton("Pausar");
                JButton zoomInButton = new JButton("+");
                JButton zoomOutButton = new JButton("-");

                buttonPanel.add(resetButton);
                buttonPanel.add(pauseResumeButton);
                buttonPanel.add(zoomInButton);
                buttonPanel.add(zoomOutButton);
                buttonPanel.add(visualizer.debugButton);

                // Painel para status e controle
                JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
                statusPanel.add(visualizer.followPointerCheckBox);
                statusPanel.add(visualizer.statusLabel);

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

                // Controle de botões
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
        stepTimer = new javax.swing.Timer(70, e -> animateStep());
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
        debugLogs.clear();
    }

    private void updateStatusMessage() {
        statusLabel.setText(String.format("Soma atual: %d | Soma maxima atual: %d | Status da arvore: %s", currentSum,
                maxSum, treeStatus));
    }

    private void calculateMaxPath() {
        maxPathSums = new int[height][width];
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

            if (!isReturning) {
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

                if (tree[currentRow][currentCol] == '#') {
                    logDebug("Encontrou o ponto final '#'. Iniciando backtracking.");
                    isReturning = true;
                } else {
                    repaint();
                    boolean moved = false;

                    if (tree[currentRow][currentCol] == 'W') {
                        logDebug("Encontrado 'W', reiniciando lógica de mudança.");
                        moved = resetDirectionPriority("W", currentRow, currentCol, depth);

                    } else if (tree[currentRow][currentCol] == 'V') {
                        logDebug("Encontrado 'V', reiniciando lógica de mudança.");
                        moved = resetDirectionPriority("V", currentRow, currentCol, depth);

                    } else if (canMove(currentRow, currentCol, "straight")) {
                        move(currentRow, currentCol, depth, "straight");
                        moved = true;

                    } else if (canMove(currentRow, currentCol, "right")) {
                        move(currentRow, currentCol, depth, "right");
                        moved = true;

                    } else if (canMove(currentRow, currentCol, "left")) {
                        move(currentRow, currentCol, depth, "left");
                        moved = true;
                    }

                    if (!moved) {
                        logDebug("Sem movimentos disponíveis, não há caminhos restantes nesta célula.");
                    }
                }

            } else {
                logDebug("Retornando...");
                treeStatus = "Em Analise";

                if (tree[currentRow][currentCol] == 'W' || tree[currentRow][currentCol] == 'V') {
                    logDebug(String.format("Retornando de %s na posicao (%d, %d)", tree[currentRow][currentCol],
                            currentRow, currentCol));

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
                } else {
                    processReturning();
                }
            }
        } else {
            paintMaxPath();
        }
        updateStatusMessage();
    }

    private void findStartingPoint() {
        // Percorre a última linha da árvore (altura - 1) para encontrar o ponto de
        // partida
        for (int col = 0; col < width; col++) {
            // Se encontrar o símbolo '|' na última linha, usa essa posição como ponto
            // inicial
            if (tree[height - 1][col] == '|') {
                startRow = height - 1;
                startCol = col;
                currentRow = startRow;
                currentCol = startCol;
                return; // Encerra a busca após encontrar o ponto inicial
            }
        }

        // Caso não encontre '|', procura por 'W' como ponto inicial
        for (int col = 0; col < width; col++) {
            if (tree[height - 1][col] == 'W') {
                startRow = height - 1;
                startCol = col;
                currentRow = startRow;
                currentCol = startCol;
                return; // Encerra a busca após encontrar o ponto inicial
            }
        }

        // Se nenhum ponto inicial for encontrado, exibe uma mensagem de erro no log
        logDebug("Nenhum ponto de partida encontrado na última linha da árvore.");
    }

    private boolean resetDirectionPriority(String letter, int row, int col, int depth) {
        boolean moved = false;
    
        if (letter.equals("W") || letter.equals("V")) {
            logDebug(String.format("Reiniciando prioridade em %s na posicao (%d, %d)", letter, row, col));
    
            // Prioridade: 1. Esquerda -> 2. Reto -> 3. Direita
            if (canMove(row, col, "left")) {
                logDebug(String.format("Movendo para a esquerda de %s na posicao (%d, %d)", letter, row, col));
                move(row, col, depth, "left");
                moved = true;
            } 
            // Só tenta seguir reto se não puder mover à esquerda
            else if (!moved && canMove(row, col, "straight")) {
                logDebug(String.format("Seguindo reto de %s na posicao (%d, %d)", letter, row, col));
                move(row, col, depth, "straight");
                moved = true;
            } 
            // Tenta mover para a direita apenas se esquerda e reto não funcionarem
            else if (!moved && canMove(row, col, "right")) {
                logDebug(String.format("Movendo para a direita de %s na posicao (%d, %d)", letter, row, col));
                move(row, col, depth, "right");
                moved = true;
            }
        }
    
        return moved;
    }
    

    private boolean canMove(int row, int col, String direction) {
        int newRow = row - 1;
        int newCol = col;
    
        if (direction.equals("left")) {
            newCol = col - 1;
            // Ignora '|', '/', e números já analisados (em verde)
            while (newRow >= 0 && newCol >= 0 &&
                   (tree[newRow][newCol] == '|' || tree[newRow][newCol] == '/' || path[newRow][newCol] == 2)) {
                newRow--;
                newCol--;
            }
            // Se houver um 'V' ou 'W', pode continuar movendo para a esquerda
            if (newRow >= 0 && newCol >= 0 && (tree[newRow][newCol] == 'V' || tree[newRow][newCol] == 'W')) {
                return true;
            }
        } else if (direction.equals("right")) {
            newCol = col + 1;
            // Ignora '|', '\' e números já analisados (em verde)
            while (newRow >= 0 && newCol < width &&
                   (tree[newRow][newCol] == '|' || tree[newRow][newCol] == '\\' || path[newRow][newCol] == 2)) {
                newRow--;
                newCol++;
            }
            // Se houver um 'V' ou 'W', pode continuar movendo para a direita
            if (newRow >= 0 && newCol < width && (tree[newRow][newCol] == 'V' || tree[newRow][newCol] == 'W')) {
                return true;
            }
        } else if (direction.equals("straight")) {
            // Ignora '\' e '/' e números já analisados (em verde)
            while (newRow >= 0 &&
                   (tree[newRow][newCol] == '\\' || tree[newRow][newCol] == '/' || path[newRow][newCol] == 2)) {
                newRow--;
            }
            // Se houver um 'V' ou 'W', pode continuar indo reto
            if (newRow >= 0 && (tree[newRow][newCol] == 'V' || tree[newRow][newCol] == 'W')) {
                return true;
            }
        }
    
        // Verifica se a célula é válida e não foi visitada
        return newRow >= 0 && newCol >= 0 && newCol < width && isValidMove(newRow, newCol);
    }
    
    private boolean isValidMove(int row, int col) {
        return tree[row][col] != ' ' && tree[row][col] != '\0' && path[row][col] != 2;
    }

    private void move(int row, int col, int depth, String direction) {
        int newRow = row - 1;
        int newCol = col;
    
        if (direction.equals("left")) {
            newCol = col - 1;
            newRow = row - 1;
            // Continua ignorando '|', '/', e números já analisados
            while (newRow >= 0 && newCol >= 0 &&
                   (tree[newRow][newCol] == '|' || tree[newRow][newCol] == '/' || path[newRow][newCol] == 2)) {
                newRow--;
                newCol--;
            }
            logDebug(String.format("Movendo para a esquerda: Novo row: %d, Novo col: %d", newRow, newCol));
        } else if (direction.equals("right")) {
            newCol = col + 1;
            newRow = row - 1;
            // Continua ignorando '|', '\' e números já analisados
            while (newRow >= 0 && newCol < width &&
                   (tree[newRow][newCol] == '|' || tree[newRow][newCol] == '\\' || path[newRow][newCol] == 2)) {
                newRow--;
                newCol++;
            }
            logDebug(String.format("Movendo para a direita: Novo row: %d, Novo col: %d", newRow, newCol));
        } else if (direction.equals("straight")) {
            newRow = row - 1;
            // Continua ignorando '\' e '/' e números já analisados
            while (newRow >= 0 &&
                   (tree[newRow][newCol] == '\\' || tree[newRow][newCol] == '/' || path[newRow][newCol] == 2)) {
                newRow--;
            }
            logDebug(String.format("Seguindo reto: Novo row: %d, Col permanece: %d", newRow, newCol));
        }
    
        // Se encontrar 'V' ou 'W', também considera o movimento válido
        if (newRow >= 0 && newCol >= 0 && (tree[newRow][newCol] == 'V' || tree[newRow][newCol] == 'W')) {
            dfsStack.push(new int[]{newRow, newCol, depth + 1});
        } else if (newRow >= 0 && newCol >= 0 && newCol < width && path[newRow][newCol] == 0) {
            dfsStack.push(new int[]{newRow, newCol, depth + 1});
        } else {
            logDebug(String.format("Movimento inválido: Row: %d, Col: %d", newRow, newCol));
        }
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
                if (tree[i][j] == ' ' || tree[i][j] == '\0') {
                    g.setColor(Color.GRAY);
                    g.fillRect(xOffset + j * CELL_SIZE, yOffset + i * CELL_SIZE, CELL_SIZE, CELL_SIZE);
                } else {
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
