package src;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.List;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.prefs.Preferences; // Import adicionado

public class MonkeyTree extends JPanel {

    private String lastDirection = "straight";
    private char[][] tree;
    private int[][] path;
    private static int CELL_SIZE = 30;
    private String treeStatus = "Em Analise";
    private int height, width;
    private int[][] maxPathSums;
    private int maxSum;
    private int startRow, startCol;

    private int currentRow;
    private int currentCol;
    private int currentSum;

    private javax.swing.Timer stepTimer;
    private Stack<PathNode> dfsStack;
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

    // Constantes para preferências
    private static final String PREFS_NODE = "MonkeyTreePrefs";
    private static final String LAST_CASES_PATH_KEY = "lastCasesPath";

    // Classe interna para representar um nó no caminho
    private static class PathNode {
        int row;
        int col;
        int depth;
        Set<String> fruitsInPath;

        PathNode(int row, int col, int depth, Set<String> fruitsInPath) {
            this.row = row;
            this.col = col;
            this.depth = depth;
            // Criar uma cópia do conjunto para cada novo nó
            this.fruitsInPath = new HashSet<>(fruitsInPath);
        }
    }

    public MonkeyTree(String filePath) {
        this.tree = readTreeFromFile(filePath);
        if (this.tree == null) {
            JOptionPane.showMessageDialog(this, "Erro ao ler a árvore do arquivo.", "Erro", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
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
            Preferences prefs = Preferences.userRoot().node(PREFS_NODE);
            String savedPath = prefs.get(LAST_CASES_PATH_KEY, null);
            final String selectedFile; // Declarar como final

            if (savedPath != null) {
                File savedFolder = new File(savedPath);
                if (savedFolder.exists() && savedFolder.isDirectory()) {
                    selectedFile = savedPath;
                } else {
                    // Se o caminho salvo não for válido, solicita novamente
                    selectedFile = selectFileDialog();
                }
            } else {
                selectedFile = selectFileDialog();
            }

            if (selectedFile != null) {
                // Salva o caminho selecionado nas preferências
                prefs.put(LAST_CASES_PATH_KEY, selectedFile);

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
                JButton changePathButton = new JButton("Alterar Caminho"); // Botão para alterar o caminho

                buttonPanel.add(resetButton);
                buttonPanel.add(pauseResumeButton);
                buttonPanel.add(zoomInButton);
                buttonPanel.add(zoomOutButton);
                buttonPanel.add(visualizer.debugButton);
                buttonPanel.add(changePathButton); // Adiciona o botão ao painel

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

                // Listener para o botão de alterar caminho
                changePathButton.addActionListener(e -> {
                    int confirm = JOptionPane.showConfirmDialog(frame, "Deseja alterar o caminho da pasta 'Casos'?", "Confirmar", JOptionPane.YES_NO_OPTION);
                    if (confirm == JOptionPane.YES_OPTION) {
                        prefs.remove(LAST_CASES_PATH_KEY); // Remove a preferência salva
                        String newSelectedFile = selectFileDialog();
                        if (newSelectedFile != null) {
                            prefs.put(LAST_CASES_PATH_KEY, newSelectedFile);
                            visualizer.resetVisualization(newSelectedFile);
                        } else {
                            JOptionPane.showMessageDialog(frame, "Nenhuma pasta selecionada. O caminho anterior será mantido.");
                        }
                    }
                });
            } else {
                JOptionPane.showMessageDialog(null, "Nenhuma pasta selecionada. O programa será encerrado.");
                System.exit(0); // Sai se nenhum arquivo for selecionado
            }
        });
    }

    private static String selectFileDialog() {
        String selectedFile = null;
        JFileChooser folderChooser = new JFileChooser();
        folderChooser.setDialogTitle("Selecione a pasta 'Casos'");
        folderChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        folderChooser.setAcceptAllFileFilterUsed(false);

        int result = folderChooser.showOpenDialog(null);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFolder = folderChooser.getSelectedFile();
            File[] txtFiles = selectedFolder.listFiles((dir, name) -> name.endsWith(".txt"));

            if (txtFiles != null && txtFiles.length > 0) {
                Arrays.sort(txtFiles, Comparator.comparingInt(a -> {
                    String num = a.getName().replaceAll("\\D", "");
                    return num.isEmpty() ? 0 : Integer.parseInt(num);
                }));

                String[] fileNames = Arrays.stream(txtFiles)
                        .map(File::getName)
                        .toArray(String[]::new);

                String chosenFile = (String) JOptionPane.showInputDialog(
                        null,
                        "Escolha um arquivo TXT para visualizar:",
                        "Seleção de Arquivo",
                        JOptionPane.PLAIN_MESSAGE,
                        null,
                        fileNames,
                        fileNames[0]);

                if (chosenFile != null) {
                    selectedFile = new File(selectedFolder, chosenFile).getAbsolutePath();
                }
            } else {
                JOptionPane.showMessageDialog(null, "Nenhum arquivo TXT encontrado na pasta selecionada.");
            }
        }
        return selectedFile;
    }

    private char[][] readTreeFromFile(String filePath) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath, StandardCharsets.UTF_8))) {
            String[] dimensions = reader.readLine().split(" ");
            if (dimensions.length < 2) {
                JOptionPane.showMessageDialog(this, "Formato do arquivo inválido. Primeira linha deve conter dimensões.", "Erro", JOptionPane.ERROR_MESSAGE);
                return null;
            }
            int rows = Integer.parseInt(dimensions[0]);
            int cols = Integer.parseInt(dimensions[1]);

            char[][] tree = new char[rows][cols];

            String line;
            int row = 0;
            while ((line = reader.readLine()) != null && row < rows) {
                for (int col = 0; col < Math.min(line.length(), cols); col++) {
                    tree[row][col] = line.charAt(col);
                }
                row++;
            }
            return tree;
        } catch (IOException | NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Erro ao ler o arquivo: " + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            return null;
        }
    }

    public void startVisualization() {
        resetState();
        findStartingPoint();
        calculateMaxPath();

        if (treeStatus.equals("Em Analise")) {
            // Inicializar com um PathNode vazio (sem frutas contabilizadas)
            Set<String> initialFruits = new HashSet<>();
            PathNode startNode = new PathNode(startRow, startCol, 0, initialFruits);
            dfsStack.push(startNode);
            stepTimer = new javax.swing.Timer(30, e -> animateStep()); // Ajustado para 30ms
            stepTimer.start();
        }
    }

    public void resetVisualization(String filePath) {
        if (stepTimer != null) {
            stepTimer.stop();
        }
        this.tree = readTreeFromFile(filePath);
        if (this.tree == null) {
            JOptionPane.showMessageDialog(this, "Erro ao ler a árvore do arquivo.", "Erro", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
        this.height = tree.length;
        this.width = tree[0].length;
        resetState();
        setPreferredSize(new Dimension(tree[0].length * CELL_SIZE, tree.length * CELL_SIZE));
        revalidate();
        repaint();
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
            PathNode pos = dfsStack.peek();
            Rectangle rect = new Rectangle(pos.col * CELL_SIZE, pos.row * CELL_SIZE, CELL_SIZE, CELL_SIZE);
            scrollPane.getViewport().scrollRectToVisible(rect);
        }
    }

    public void zoomIn() {
        CELL_SIZE += 5;
        setPreferredSize(new Dimension(width * CELL_SIZE, height * CELL_SIZE));
        revalidate();
        repaint();
    }

    public void zoomOut() {
        if (CELL_SIZE > 5) {
            CELL_SIZE -= 5;
            setPreferredSize(new Dimension(width * CELL_SIZE, height * CELL_SIZE));
            revalidate();
            repaint();
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
        updateStatusMessage();
    }

    private void updateStatusMessage() {
        statusLabel.setText(String.format("Soma atual: %d | Soma maxima atual: %d | Status da arvore: %s", currentSum,
                maxSum, treeStatus));
    }

    // Calcula o caminho máximo possível na árvore
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

                    if (maxPathSums[i][j] > maxSum) {
                        maxSum = maxPathSums[i][j];
                    }
                }
            }
        }
    }

    private void animateStep() {
        if (!dfsStack.isEmpty()) {
            PathNode currentNode = dfsStack.peek();
            currentRow = currentNode.row;
            currentCol = currentNode.col;
            int depth = currentNode.depth;

            logDebug(String.format("Posicao atual: (%d, %d), Soma atual: %d", currentRow, currentCol, currentSum));

            if (!isReturning) {
                path[currentRow][currentCol] = 1;

                if (Character.isDigit(tree[currentRow][currentCol])) {
                    int value = Character.getNumericValue(tree[currentRow][currentCol]);
                    if (value > 0) {
                        // currentSum já é incrementada no attemptMove
                    }
                    if (currentSum > maxSum) {
                        maxSum = currentSum;
                        saveMaxPath();
                        logDebug(String.format("Updated maxSum to %d", maxSum));
                    }
                }

                if (tree[currentRow][currentCol] == '#') {
                    logDebug("Encontrou o ponto final '#'. Iniciando backtracking.");
                    isReturning = true;
                } else {
                    repaint();
                    boolean moved = false;

                    if (tree[currentRow][currentCol] == 'W' || tree[currentRow][currentCol] == 'V') {
                        moved = resetDirectionPriority(Character.toString(tree[currentRow][currentCol]), currentRow, currentCol, depth);
                    } else {
                        switch (lastDirection) {
                            case "left":
                                moved = attemptMove(currentRow, currentCol, "left", depth) ||
                                         attemptMove(currentRow, currentCol, "straight", depth) ||
                                         attemptMove(currentRow, currentCol, "right", depth);
                                break;
                            case "right":
                                moved = attemptMove(currentRow, currentCol, "right", depth) ||
                                         attemptMove(currentRow, currentCol, "straight", depth) ||
                                         attemptMove(currentRow, currentCol, "left", depth);
                                break;
                            default:
                                moved = attemptMove(currentRow, currentCol, "straight", depth) ||
                                         attemptMove(currentRow, currentCol, "left", depth) ||
                                         attemptMove(currentRow, currentCol, "right", depth);
                                break;
                        }
                    }

                    if (!moved) {
                        logDebug("Sem movimentos disponíveis, iniciando backtracking.");
                        isReturning = true;
                    }
                }
            } else {
                logDebug("Retornando...");
                treeStatus = "Em Analise";

                if ((tree[currentRow][currentCol] == 'W' || tree[currentRow][currentCol] == 'V') && path[currentRow][currentCol] == 1) {
                    logDebug(String.format("Retornando de %s na posicao (%d, %d)", tree[currentRow][currentCol],
                            currentRow, currentCol));

                    boolean moved = resetDirectionPriority(Character.toString(tree[currentRow][currentCol]), currentRow, currentCol, depth);

                    if (!moved) {
                        processReturning();
                    } else {
                        isReturning = false;
                    }
                } else {
                    processReturning();
                }
            }
        } else {
            // Se a pilha está vazia, a análise está concluída
            paintMaxPath();
            treeStatus = "Concluido"; // Atualiza o status para "Concluido"
            logDebug("Análise concluída. Soma máxima encontrada: " + maxSum);
        }
        updateStatusMessage();
    }

    private boolean resetDirectionPriority(String letter, int row, int col, int depth) {
        boolean moved = false;

        logDebug(String.format("Reiniciando prioridade em %s na posicao (%d, %d)", letter, row, col));

        if (letter.equals("W")) {
            // Alterando a prioridade de movimento para Esquerda -> Direita -> Meio
            moved = attemptMove(row, col, "left", depth) ||
                    attemptMove(row, col, "straight", depth)||
                    attemptMove(row, col, "right", depth);

        } else if (letter.equals("V")) {
            // Mantém a prioridade existente para 'V'
            moved = attemptMove(row, col, "left", depth) ||
                    attemptMove(row, col, "right", depth);
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
                logDebug(String.format("Ponto de partida encontrado em (%d, %d)", startRow, startCol));
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
                logDebug(String.format("Ponto de partida alternativo encontrado em (%d, %d)", startRow, startCol));
                return;
            }
        }
        // Se não encontrar nenhum ponto de partida, encerra o programa
        JOptionPane.showMessageDialog(this, "Nenhum ponto de partida encontrado na árvore.", "Erro", JOptionPane.ERROR_MESSAGE);
        System.exit(1);
    }

    private boolean attemptMove(int row, int col, String direction, int depth) {
        int newRow = row - 1;
        int newCol = col;

        if (direction.equals("left")) {
            newCol = col - 1;
            logDebug(String.format("Attempting to move LEFT from (%d, %d)", row, col));
            while (newRow >= 0 && newCol >= 0
                    && (path[newRow][newCol] != 0 || tree[newRow][newCol] == '|' || tree[newRow][newCol] == '/')) {
                newRow--;
                newCol--;
            }
        } else if (direction.equals("right")) {
            newCol = col + 1;
            logDebug(String.format("Attempting to move RIGHT from (%d, %d)", row, col));
            while (newRow >= 0 && newCol < width
                    && (path[newRow][newCol] != 0 || tree[newRow][newCol] == '|' || tree[newRow][newCol] == '\\')) {
                newRow--;
                newCol++;
            }
        } else if (direction.equals("straight")) {
            logDebug(String.format("Attempting to move STRAIGHT from (%d, %d)", row, col));
            while (newRow >= 0
                    && (path[newRow][newCol] != 0 || tree[newRow][newCol] == '\\' || tree[newRow][newCol] == '/')) {
                newRow--;
            }
        }

        if (newRow >= 0 && newCol >= 0 && newCol < width && isValidMove(newRow, newCol)) {
            Set<String> newFruitsInPath = new HashSet<>(dfsStack.peek().fruitsInPath); // Copiar o conjunto atual

            if (Character.isDigit(tree[newRow][newCol])) {
                int value = Character.getNumericValue(tree[newRow][newCol]);
                String posKey = newRow + "," + newCol;

                if (newFruitsInPath.contains(posKey)) {
                    logDebug(String.format("Shared fruit at (%d, %d) already counted in this path.", newRow, newCol));
                    // NÃO incrementa currentSum novamente
                } else {
                    newFruitsInPath.add(posKey);
                    currentSum += value;
                    if (currentSum > maxSum) {
                        maxSum = currentSum;
                        saveMaxPath();
                        logDebug(String.format("Updated maxSum to %d", maxSum));
                    }
                }
            }

            // Criar um novo PathNode com o conjunto atualizado
            PathNode newNode = new PathNode(newRow, newCol, depth + 1, newFruitsInPath);
            dfsStack.push(newNode);
            lastDirection = direction;
            logDebug(String.format("Moved to (%d, %d) via %s", newRow, newCol, direction));
            return true;
        } else {
            logDebug(String.format("Invalid move to (%d, %d) via %s", newRow, newCol, direction));
            return false;
        }
    }

    // Função para verificar se o movimento é válido
    private boolean isValidMove(int row, int col) {
        // Adiciona verificação para ignorar células vazias
        return row >= 0 && col >= 0 && col < width && tree[row][col] != ' ' && tree[row][col] != '\0';
    }

    private void processReturning() {
        PathNode currentNode = dfsStack.peek(); // Obter o nó atual antes de pop

        if (Character.isDigit(tree[currentNode.row][currentNode.col])) {
            int value = Character.getNumericValue(tree[currentNode.row][currentNode.col]);
            String posKey = currentNode.row + "," + currentNode.col;

            // Criar um novo conjunto sem a fruta atual
            Set<String> updatedFruitsInPath = new HashSet<>(currentNode.fruitsInPath);
            if (updatedFruitsInPath.contains(posKey)) {
                updatedFruitsInPath.remove(posKey);
                currentSum -= value; // Decrementa apenas quando a fruta é removida do caminho
                logDebug(String.format("Removed fruit at (%d, %d) from path. New currentSum: %d", currentNode.row, currentNode.col, currentSum));
            } else {
                if (value > 0 && currentSum > 0) {
                    currentSum -= value;
                    logDebug(String.format("Decremented currentSum by %d. New currentSum: %d", value, currentSum));
                }
            }

            // Atualizar o PathNode atual com o conjunto atualizado
            currentNode.fruitsInPath = updatedFruitsInPath;
        }

        path[currentRow][currentCol] = 2; // Marca como backtracked
        dfsStack.pop();
        repaint();
    }

    private void saveMaxPath() {
        maxPathStack.clear();
        for (PathNode position : dfsStack) {
            maxPathStack.push(new int[] { position.row, position.col });
        }
    }

    private void paintMaxPath() {
        while (!maxPathStack.isEmpty()) {
            int[] pos = maxPathStack.pop();
            path[pos[0]][pos[1]] = 3; // Corrigido para 3
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
        debugTextArea.revalidate();
        debugTextArea.repaint();
        debugFrame.setVisible(true); // Mova setVisible para depois de setText
    }

    private void logDebug(String message) {
        debugLogs.add(message);
        // Evitar chamar setText a cada log para melhorar desempenho
        // Atualize apenas quando o debugFrame estiver visível
        if (debugFrame != null && debugFrame.isVisible()) {
            debugTextArea.append(message + "\n");
            debugTextArea.revalidate();
            debugTextArea.repaint();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        int xOffset = (getWidth() - tree[0].length * CELL_SIZE) / 2;
        int yOffset = (getHeight() - tree.length * CELL_SIZE) / 2;

        for (int i = 0; i < tree.length; i++) {
            for (int j = 0; j < tree[0].length; j++) {
                if (tree[i][j] == ' ' || tree[i][j] == '\0') {
                    // Pintar as células vazias de cinza
                    g.setColor(Color.GRAY);
                    g.fillRect(xOffset + j * CELL_SIZE, yOffset + i * CELL_SIZE, CELL_SIZE, CELL_SIZE);
                } else {
                    // Determina a cor com base no estado da célula
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
                    } else {
                        g.setColor(Color.WHITE);
                    }

                    g.drawRect(xOffset + j * CELL_SIZE, yOffset + i * CELL_SIZE, CELL_SIZE, CELL_SIZE);
                    g.drawString(Character.toString(tree[i][j]), xOffset + j * CELL_SIZE + 5,
                            yOffset + i * CELL_SIZE + 15);
                }
            }
        }
    }
}
