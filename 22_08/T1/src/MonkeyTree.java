// Mover alguns métodos para arquivos diferentes, como: 
    // WindowApp.java == Toda a interface Gráfica do aplicativo
    // Debugger.java == Toda a lógica do debugger
    // Só fazer isso quando todo o código estiver funcionando 100% 

// A partir do caso 150, ele está indo para a direita inesperadamente, mesmo que ele passou por um V anteriormente, isso acontece também no caso 90, quando tem dois números seguidos
// Fazer com que o ponteiro volte em um #, mesmo que tenha mais caminhos a frente
// Fazer alguma maneira de otimizar este código, remover métodos redundantes, melhorar a presição do algoritimo de busca de profundidade
// Fazer com que, quando a renderização termine, apareça um pop-up dizendo a soma máxima, e perguntando se quer fazer a renderização de uma nova árvore, se clicar em sim, aparece um novo dialog para escolher qual arquivo que será renderizado
// Arrumar o método para seguir a camera, isso será util em árvores maiores
package src;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.util.List;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
public class MonkeyTree extends JPanel {

  private char[][] tree;  // Representação da árvore
    private int[][] path;  // Caminho percorrido
    private static int CELL_SIZE = 20; // Tamanho da célula (controla o zoom)
    private String treeStatus = "Em Análise";  // Status atual da árvore
    private int height, width;  // Altura e largura da árvore
    private int[][] maxPathSums;  // Armazena as somas máximas dos caminhos
    private int maxSum;  // Armazena o valor da soma máxima
    private int startRow, startCol;  // Posição inicial da exploração
    private int currentRow, currentCol;  // Posição atual do explorador
    private int currentSum;  // Soma acumulada ao percorrer o caminho

    private javax.swing.Timer stepTimer;  // Timer para controlar os passos de exploração
    private Stack<int[]> dfsStack;  // Pilha usada no algoritmo de DFS (Depth-First Search)
    private Stack<int[]> maxPathStack;  // Pilha para armazenar o caminho máximo
    private boolean isReturning = false;  // Flag para indicar se está em modo de backtracking
    private boolean isPaused = false;  // Flag para pausar ou retomar a exploração

    private JCheckBox followPointerCheckBox;  // Checkbox para seguir o ponteiro na exploração
    private JLabel statusLabel;  // Label para exibir o status da exploração
    
    // Debug Log Components
    private JButton debugButton;  // Botão para abrir o log de depuração
    private JFrame debugFrame;  // Frame para mostrar o log de depuração
    private JTextArea debugTextArea;  // Área de texto para exibir o log de depuração
    private List<String> debugLogs;  // Lista que armazena o log de depuração

    public MonkeyTree(String filePath) {
        this.tree = readTreeFromFile(filePath); // Lê a árvore do arquivo
        this.path = new int[tree.length][tree[0].length]; // Inicializa a matriz de caminho
        this.height = tree.length; // Define a altura da árvore
        this.width = tree[0].length; // Define a largura da árvore
        this.currentSum = 0;
        this.maxSum = 0;

        this.dfsStack = new Stack<>();  // Inicializa a pilha de DFS
        this.maxPathStack = new Stack<>();  // Inicializa a pilha de caminho máximo

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
                buttonPanel.add(visualizer.debugButton); // Adiciona o botão Debug Log
                
                // Painel para a esquerda onde aparecerá o status e o checkbox
                JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
                statusPanel.add(visualizer.followPointerCheckBox);  // Adiciona o checkbox
                statusPanel.add(visualizer.statusLabel);  // Adiciona a label de status

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
        statusLabel.setText(String.format("Soma atual: %d | Soma maxima atual: %d | Status da arvore: %s", currentSum, maxSum, treeStatus));
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

    private boolean foundAnotherVW(String direction) {
        int newRow = currentRow - 1;
        int newCol = currentCol;
    
        if (direction.equals("left")) {
            newCol = currentCol - 1;
            while (newRow >= 0 && newCol >= 0 && tree[newRow][newCol] != ' ') {
                if (tree[newRow][newCol] == 'V' || tree[newRow][newCol] == 'W') {
                    return true;
                }
                newRow--;
                newCol--;
            }
        } else if (direction.equals("right")) {
            newCol = currentCol + 1;
            while (newRow >= 0 && newCol < width && tree[newRow][newCol] != ' ') {
                if (tree[newRow][newCol] == 'V' || tree[newRow][newCol] == 'W') {
                    return true;
                }
                newRow--;
                newCol++;
            }
        } else if (direction.equals("straight")) {
            while (newRow >= 0 && tree[newRow][newCol] != ' ') {
                if (tree[newRow][newCol] == 'V' || tree[newRow][newCol] == 'W') {
                    return true;
                }
                newRow--;
            }
        }
    
        return false;
    }
    
    private void animateStep() {
        if (!dfsStack.isEmpty()) {
            int[] pos = dfsStack.peek();
            currentRow = pos[0];
            currentCol = pos[1];
            int depth = pos[2];
    
            logDebug(String.format("Posição atual: (%d, %d), Soma atual: %d", currentRow, currentCol, currentSum));
    
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
    
                repaint();
    
                boolean moved = false;
                if (tree[currentRow][currentCol] == '#') {
                    logDebug("Encontrado '#', iniciando backtracking.");
                    isReturning = true; // Força o retorno ao encontrar '#'
                    processReturning();
                    return; // Não continue explorando
                }
                
                if (tree[currentRow][currentCol] == 'W') {
                    logDebug("Encontrado 'W', aplicando lógica de mudança.");
                    
                    // **Prioridade para 'W': Esquerda -> Meio -> Direita**
                    // Verifica se há outro 'V' ou 'W' no caminho antes de mudar
                    if (canMove(currentRow, currentCol, "left") && !foundAnotherVW("left")) {
                        move(currentRow, currentCol, depth, "left");
                        moved = true;
                    } else if (canMove(currentRow, currentCol, "straight") && !foundAnotherVW("straight")) {
                        move(currentRow, currentCol, depth, "straight");
                        moved = true;
                    } else if (canMove(currentRow, currentCol, "right") && !foundAnotherVW("right")) {
                        move(currentRow, currentCol, depth, "right");
                        moved = true;
                    }
                } else if (tree[currentRow][currentCol] == 'V') {
                    logDebug("Encontrado 'V', aplicando lógica de mudança.");
                    
                    // Prioridade para 'V': Esquerda -> Direita
                    if (canMove(currentRow, currentCol, "left") && !foundAnotherVW("left")) {
                        move(currentRow, currentCol, depth, "left");
                        moved = true;
                    } else if (canMove(currentRow, currentCol, "right") && !foundAnotherVW("right")) {
                        move(currentRow, currentCol, depth, "right");
                        moved = true;
                    }
                }

                // Primeiro tenta seguir em frente (explorar profundidade)
                if (tree[currentRow][currentCol] == 'W') {
                    logDebug("Encontrado 'W', aplicando lógica de mudança.");
    
                    // **Prioridade para 'W': Esquerda -> Meio -> Direita**
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
                    logDebug("Encontrado 'V', aplicando lógica de mudança.");
    
                    // Prioridade para 'V': Esquerda -> Direita
                    if (canMove(currentRow, currentCol, "left")) {
                        move(currentRow, currentCol, depth, "left");
                        moved = true;
                    } else if (canMove(currentRow, currentCol, "right")) {
                        move(currentRow, currentCol, depth, "right");
                        moved = true;
                    }
                } else if (canMove(currentRow, currentCol, "straight")) {
                    // Tenta seguir em frente
                    move(currentRow, currentCol, depth, "straight");
                    moved = true;
                } else if (canMove(currentRow, currentCol, "right")) {
                    // Tenta explorar a profundidade pela direita
                    move(currentRow, currentCol, depth, "right");
                    moved = true;
                } else if (canMove(currentRow, currentCol, "left")) {
                    // Tenta explorar a profundidade pela esquerda
                    move(currentRow, currentCol, depth, "left");
                    moved = true;
                }
    
                // Se não conseguiu se mover, começa a retornar
                if (!moved) {
                    isReturning = true;
                }
    
            } else {  // Processo de retorno
                treeStatus = "Em Análise";
                logDebug("Retornando...");
    
                if (tree[currentRow][currentCol] == 'W') {
                    logDebug(String.format("Verificando direção a partir de %c", tree[currentRow][currentCol]));
    
                    // **Prioridade para 'W': Esquerda -> Meio -> Direita**
                    if (canMove(currentRow, currentCol, "left")) {
                        logDebug("Movendo para a esquerda.");
                        isReturning = false;
                        move(currentRow, currentCol, depth, "left");
                    } else if (canMove(currentRow, currentCol, "straight")) {
                        logDebug("Movendo em linha reta.");
                        isReturning = false;
                        move(currentRow, currentCol, depth, "straight");
                    } else if (canMove(currentRow, currentCol, "right")) {
                        logDebug("Movendo para a direita.");
                        isReturning = false;
                        move(currentRow, currentCol, depth, "right");
                    } else {
                        logDebug("Sem movimento possível, retornando.");
                        processReturning();
                    }
    
                } else if (tree[currentRow][currentCol] == 'V') {
                    logDebug(String.format("Verificando direção a partir de %c", tree[currentRow][currentCol]));
    
                    // Prioridade para 'V': Esquerda -> Direita
                    if (canMove(currentRow, currentCol, "left")) {
                        logDebug("Movendo para a esquerda.");
                        isReturning = false;
                        move(currentRow, currentCol, depth, "left");
                    } else if (canMove(currentRow, currentCol, "right")) {
                        logDebug("Movendo para a direita.");
                        isReturning = false;
                        move(currentRow, currentCol, depth, "right");
                    } else {
                        logDebug("Sem movimento possível, retornando.");
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

private boolean canMove(int row, int col, String direction) {
    int newRow = row - 1;
    int newCol = col;

    if (direction.equals("left")) {
        newCol = col - 1;
        // Pula células já visitadas ou não válidas até encontrar um número ou uma célula válida
        while (newRow >= 0 && newCol >= 0 && (path[newRow][newCol] != 0 || tree[newRow][newCol] == '|' || tree[newRow][newCol] == '/')) {
            newRow--;
            newCol--;
        }
    } else if (direction.equals("right")) {
        newCol = col + 1;
        // Pula células já visitadas ou não válidas até encontrar um número ou uma célula válida
        while (newRow >= 0 && newCol < width && (path[newRow][newCol] != 0 || tree[newRow][newCol] == '|' || tree[newRow][newCol] == '\\')) {
            newRow--;
            newCol++;
        }
    } else if (direction.equals("straight")) {
        // Pula células já visitadas ou não válidas até encontrar um número ou uma célula válida
        while (newRow >= 0 && (path[newRow][newCol] != 0 || tree[newRow][newCol] == '\\' || tree[newRow][newCol] == '/')) {
            newRow--;
        }
    }

    // Verifica se a célula encontrada é válida para movimento
    return newCol >= 0 && newCol < width && newRow >= 0 && isValidMove(newRow, newCol) && path[newRow][newCol] == 0;
}

private void move(int row, int col, int depth, String direction) {
    int newRow = row - 1;
    int newCol = col;

    if (direction.equals("left")) {
        newCol = col - 1;
        // Pula células já visitadas ou não válidas até encontrar um número ou uma célula válida
        while (newRow >= 0 && newCol >= 0 && (path[newRow][newCol] != 0 || tree[newRow][newCol] == '|' || tree[newRow][newCol] == '/')) {
            newRow--;
            newCol--;
        }
    } else if (direction.equals("right")) {
        newCol = col + 1;
        // Pula células já visitadas ou não válidas até encontrar um número ou uma célula válida
        while (newRow >= 0 && newCol < width && (path[newRow][newCol] != 0 || tree[newRow][newCol] == '|' || tree[newRow][newCol] == '\\')) {
            newRow--;
            newCol++;
        }
    } else if (direction.equals("straight")) {
        // Pula células já visitadas ou não válidas até encontrar um número ou uma célula válida
        while (newRow >= 0 && (path[newRow][newCol] != 0 || tree[newRow][newCol] == '\\' || tree[newRow][newCol] == '/')) {
            newRow--;
        }
    }

    // Verifica se o novo caminho oferece um benefício (não pula números)
    if (newRow >= 0 && newCol >= 0 && newCol < width && (path[newRow][newCol] == 0 || Character.isDigit(tree[newRow][newCol]))) {
        dfsStack.push(new int[]{newRow, newCol, depth + 1});
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
                    g.drawString(Character.toString(tree[i][j]), xOffset + j * CELL_SIZE + 5, yOffset + i * CELL_SIZE + 15);
                }
            }
        }

        g.setColor(Color.WHITE);
    }
}