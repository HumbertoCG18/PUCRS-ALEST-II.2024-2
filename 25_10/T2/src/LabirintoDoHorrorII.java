package src;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import javax.swing.*;

public class LabirintoDoHorrorII {

    // Enum para os tipos de seres
    enum Ser {
        ANAO("Anao", 'A'),
        BRUXA("Bruxa", 'B'),
        CAVALEIRO("Cavaleiro", 'C'),
        DUENDE("Duende", 'D'),
        ELFO("Elfo", 'E'),
        FADA("Fada", 'F');

        private String nome;
        private char codigo;

        Ser(String nome, char codigo) {
            this.nome = nome;
            this.codigo = codigo;
        }

        public String getNome() {
            return nome;
        }

        public char getCodigo() {
            return codigo;
        }

        public static Ser fromCodigo(char codigo) {
            for (Ser ser : Ser.values()) {
                if (ser.codigo == codigo) {
                    return ser;
                }
            }
            return null;
        }
    }

    // Classe que representa uma célula no labirinto
    static class Celula {
        int x, y;
        boolean[] paredes = new boolean[4]; // 0: cima, 1: direita, 2: baixo, 3: esquerda
        boolean visitado = false;
        int regiao = -1;
        Ser ser = null;

        public Celula(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    // Classe que representa o labirinto
    static class Labirinto {
        int M, N;
        Celula[][] grid;
        int numRegioes = 0;
        Map<Integer, Map<Ser, Integer>> regioesSeres = new HashMap<>();

        // Método para ler o labirinto a partir de um arquivo
        public void readMazeFromFile(String filePath) throws IOException {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(filePath), StandardCharsets.UTF_8))) {
                String linha = br.readLine();
                if (linha == null || linha.trim().isEmpty()) {
                    throw new IOException("Arquivo inválido: a primeira linha deve conter M e N.");
                }

                // Ajuste para suportar dimensões maiores que 9
                String[] dimensions = linha.trim().split("\\s+");
                if (dimensions.length < 2) {
                    throw new IOException("Arquivo inválido: a primeira linha deve conter M e N separados por espaço.");
                }
                try {
                    M = Integer.parseInt(dimensions[0]);
                    N = Integer.parseInt(dimensions[1]);
                } catch (NumberFormatException e) {
                    throw new IOException("Arquivo inválido: M e N devem ser números inteiros.");
                }

                grid = new Celula[M][N];

                for (int i = 0; i < M; i++) {
                    linha = br.readLine();
                    if (linha == null || linha.length() < N) {
                        throw new IOException("Arquivo inválido: número de linhas ou colunas inconsistente.");
                    }
                    for (int j = 0; j < N; j++) {
                        char ch = linha.charAt(j);
                        Celula celula = new Celula(i, j);
                        grid[i][j] = celula;

                        // Verifica se é um ser (letra maiúscula)
                        if (Character.isLetter(ch) && Character.isUpperCase(ch)) {
                            celula.ser = Ser.fromCodigo(ch);
                            if (celula.ser == null) {
                                System.out.println("Aviso: Código de ser desconhecido '" + ch + "' na célula (" + i + "," + j + ").");
                            }
                        }

                        // Parse o valor hexadecimal para paredes
                        int valor = Character.digit(ch, 16);
                        if (valor == -1) {
                            // Tenta converter para minúsculo
                            valor = Character.digit(Character.toLowerCase(ch), 16);
                            if (valor == -1) {
                                throw new IOException("Arquivo inválido: caractere não hexadecimal '" + ch + "' na célula (" + i + "," + j + ").");
                            }
                        }

                        // Converte o valor hexadecimal para bits (paredes)
                        for (int k = 0; k < 4; k++) {
                            celula.paredes[k] = ((valor >> (3 - k)) & 1) == 1;
                        }
                    }
                }
            }
        }

        // Método para encontrar as regiões usando DFS
        public void encontrarRegioes() {
            int regiaoId = 0;
            for (int i = 0; i < M; i++) {
                for (int j = 0; j < N; j++) {
                    Celula celula = grid[i][j];
                    if (!celula.visitado) {
                        dfs(celula, regiaoId);
                        regiaoId++;
                    }
                }
            }
            numRegioes = regiaoId;
        }

        // Busca em profundidade para marcar as regiões
        private void dfs(Celula celula, int regiaoId) {
            celula.visitado = true;
            celula.regiao = regiaoId;
            int x = celula.x;
            int y = celula.y;

            // Movimentos: cima, direita, baixo, esquerda
            int[] dx = {-1, 0, 1, 0};
            int[] dy = {0, 1, 0, -1};

            for (int k = 0; k < 4; k++) {
                int nx = x + dx[k];
                int ny = y + dy[k];

                if (!celula.paredes[k]) { // Não há parede na direção k
                    if (nx >= 0 && nx < M && ny >= 0 && ny < N) {
                        Celula vizinho = grid[nx][ny];
                        if (!vizinho.visitado) {
                            dfs(vizinho, regiaoId);
                        }
                    }
                }
            }
        }

        // Método para contar os seres em cada região
        public void contarSeres() {
            for (int i = 0; i < M; i++) {
                for (int j = 0; j < N; j++) {
                    Celula celula = grid[i][j];
                    if (celula.ser != null) {
                        regioesSeres.putIfAbsent(celula.regiao, new HashMap<>());
                        Map<Ser, Integer> mapaSeres = regioesSeres.get(celula.regiao);
                        mapaSeres.put(celula.ser, mapaSeres.getOrDefault(celula.ser, 0) + 1);
                    }
                }
            }
        }

        // Método para imprimir os resultados
        public void imprimirResultados() {
            System.out.println("Número de regiões: " + numRegioes);
            for (int regiaoId = 0; regiaoId < numRegioes; regiaoId++) {
                Map<Ser, Integer> mapaSeres = regioesSeres.getOrDefault(regiaoId, new HashMap<>());
                if (!mapaSeres.isEmpty()) {
                    Ser serMaisFrequente = null;
                    int maxContagem = 0;
                    for (Map.Entry<Ser, Integer> entry : mapaSeres.entrySet()) {
                        if (entry.getValue() > maxContagem) {
                            maxContagem = entry.getValue();
                            serMaisFrequente = entry.getKey();
                        }
                    }
                    System.out.println("Região " + regiaoId + ": Ser mais frequente é " + serMaisFrequente.getNome());
                } else {
                    System.out.println("Região " + regiaoId + ": Nenhum ser encontrado");
                }
            }
        }

        // Método para renderizar o labirinto usando Swing
        public void renderizar() {
            JFrame frame = new JFrame("Labirinto do Horror II");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            // Painel principal
            JPanel mainPanel = new JPanel(new BorderLayout());

            LabirintoPanel labirintoPanel = new LabirintoPanel(grid, M, N);

            // Painel de legendas
            LegendPanel legendPanel = new LegendPanel();

            // Divisória entre os paineis
            JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, labirintoPanel, legendPanel);
            splitPane.setDividerLocation(0.75); // Define a posição inicial da divisória
            splitPane.setResizeWeight(0.75); // Prioriza o redimensionamento do labirinto
            mainPanel.add(splitPane, BorderLayout.CENTER);

            // Botão para selecionar outro arquivo
            JButton botaoSelecionarArquivo = new JButton("Selecionar Outro Labirinto");
            mainPanel.add(botaoSelecionarArquivo, BorderLayout.SOUTH);

            botaoSelecionarArquivo.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    frame.dispose();
                    SwingUtilities.invokeLater(() -> main(null));
                }
            });

            frame.setContentPane(mainPanel);
            frame.pack(); // Ajusta o tamanho da janela ao conteúdo
            frame.setLocationRelativeTo(null); // Centraliza a janela
            frame.setVisible(true);
        }
    }

    // Painel personalizado para desenhar o labirinto
        static class LabirintoPanel extends JPanel {
        Celula[][] grid;
        int M, N;
        int tamanhoCelula = 60;

        public LabirintoPanel(Celula[][] grid, int M, int N) {
            this.grid = grid;
            this.M = M;
            this.N = N;
            // Define o tamanho preferido
            setPreferredSize(new Dimension(N * tamanhoCelula, M * tamanhoCelula));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            // Preenche o fundo com cor branca
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, getWidth(), getHeight());

            // Usa Graphics2D para configurações avançadas
            Graphics2D g2 = (Graphics2D) g;
            g2.setColor(Color.BLACK);

            // Habilita anti-aliasing para melhor qualidade
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Define a espessura da linha
            float strokeWidth = 4.0f; // Espessura das paredes
            g2.setStroke(new BasicStroke(strokeWidth));

            // Calcula o tamanho total do labirinto
            int mazeWidth = N * tamanhoCelula;
            int mazeHeight = M * tamanhoCelula;

            // Calcula as margens para centralizar o labirinto
            int offsetX = (getWidth() - mazeWidth) / 2;
            int offsetY = (getHeight() - mazeHeight) / 2;

            // Garante que as margens não sejam negativas
            if (offsetX < 0) offsetX = 0;
            if (offsetY < 0) offsetY = 0;

            // Desenha as células
            for (int i = 0; i < M; i++) {
                for (int j = 0; j < N; j++) {
                    int x = offsetX + j * tamanhoCelula;
                    int y = offsetY + i * tamanhoCelula;
                    Celula celula = grid[i][j];

                    // Desenha as paredes
                    if (celula.paredes[0]) { // Cima
                        g2.drawLine(x, y, x + tamanhoCelula, y);
                    }
                    if (celula.paredes[1]) { // Direita
                        g2.drawLine(x + tamanhoCelula, y, x + tamanhoCelula, y + tamanhoCelula);
                    }
                    if (celula.paredes[2]) { // Baixo
                        g2.drawLine(x, y + tamanhoCelula, x + tamanhoCelula, y + tamanhoCelula);
                    }
                    if (celula.paredes[3]) { // Esquerda
                        g2.drawLine(x, y, x, y + tamanhoCelula);
                    }

                    // Desenha o ser, se houver
                    if (celula.ser != null) {
                        g2.setColor(Color.RED);
                        g2.setFont(new Font("Arial", Font.BOLD, 24)); // Aumenta o tamanho da fonte
                        String texto = String.valueOf(celula.ser.getCodigo());
                        FontMetrics fm = g2.getFontMetrics();
                        int textoWidth = fm.stringWidth(texto);
                        int textoHeight = fm.getAscent();
                        g2.drawString(texto, x + (tamanhoCelula - textoWidth) / 2, y + (tamanhoCelula + textoHeight) / 2);
                        g2.setColor(Color.BLACK);
                    }
                }
            }
        }
    }

    // Painel de legendas
    static class LegendPanel extends JPanel {
        public LegendPanel() {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBorder(BorderFactory.createTitledBorder("Legenda"));
            setPreferredSize(new Dimension(200, 0)); // Largura fixa

            for (Ser ser : Ser.values()) {
                JPanel itemPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
                JLabel iconLabel = new JLabel(String.valueOf(ser.getCodigo()));
                iconLabel.setFont(new Font("Arial", Font.BOLD, 24));
                iconLabel.setForeground(Color.RED);
                JLabel nameLabel = new JLabel("- " + ser.getNome());
                nameLabel.setFont(new Font("Arial", Font.PLAIN, 16));
                itemPanel.add(iconLabel);
                itemPanel.add(nameLabel);
                add(itemPanel);
            }
        }
    }

    // Método para selecionar o arquivo de labirinto
    private static String selectFileDialog() {
        String selectedFile = null;
        JFileChooser folderChooser = new JFileChooser();
        folderChooser.setDialogTitle("Selecione a pasta que contém os arquivos de labirinto");
        folderChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        folderChooser.setAcceptAllFileFilterUsed(false);

        int result = folderChooser.showOpenDialog(null);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFolder = folderChooser.getSelectedFile();
            File[] txtFiles = selectedFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".txt"));

            if (txtFiles != null && txtFiles.length > 0) {
                Arrays.sort(txtFiles, Comparator.comparing(File::getName));

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

    public static void main(String[] args) {
        // Definir o padrão de codificação para UTF-8
        System.setProperty("file.encoding", "UTF-8");

        // Executa o programa na thread da interface gráfica
        SwingUtilities.invokeLater(() -> {
            String caminhoArquivo = selectFileDialog();

            if (caminhoArquivo == null) {
                // Usuário cancelou a seleção
                System.exit(0);
            }

            // Cria o labirinto
            Labirinto labirinto = new Labirinto();

            try {
                labirinto.readMazeFromFile(caminhoArquivo);
                labirinto.encontrarRegioes();
                labirinto.contarSeres();
                labirinto.imprimirResultados();
                labirinto.renderizar();
            } catch (IOException e) {
                JOptionPane.showMessageDialog(null, "Erro ao ler o arquivo: " + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        });
    }
}
