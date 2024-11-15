package src;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

public class LabirintoDoHorrorII {

    // Enum para os tipos de seres
    enum Ser {
        ANAO("Anao", 'A'),
        BRUXA("Bruxa", 'B'),
        CAVALEIRO("Cavaleiro", 'C'),
        DUENDE("Duende", 'D'),
        ELFO("Elfo", 'E'),
        FEIJAO("Feijao", 'F');

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

        // Lista fixa de cores para as regiões, excluindo a cor vermelha
        private final List<Color> predefinedColors = Arrays.asList(
                Color.GREEN,
                Color.BLUE,
                Color.ORANGE,
                new Color(252, 144, 198),
                Color.CYAN,
                new Color(179, 52, 115),
                new Color(88, 110, 41),
                Color.LIGHT_GRAY,
                Color.GRAY,
                Color.DARK_GRAY,
                new Color(45, 125, 252),
                new Color(128, 0, 128), // Roxo
                new Color(255, 165, 0), // Laranja
                new Color(0, 128, 128)  // Teal
        );

        // Mapa de cores para as regiões
        Map<Integer, Color> regionColors = new HashMap<>();

        // Método para obter a cor associada a uma região
        Color getColorForRegion(int regionId) {
            // Se já temos uma cor para esta região, retornamos
            if (regionColors.containsKey(regionId)) {
                return regionColors.get(regionId);
            }

            // Atribui uma cor fixa baseada no ID da região
            Color color = predefinedColors.get(regionId % predefinedColors.size());
            regionColors.put(regionId, color);
            return color;
        }

        // Método para ler o labirinto a partir de um arquivo
        public void readMazeFromFile(String filePath) throws IOException {
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(new FileInputStream(filePath), "UTF-8"))) {
                String linha = br.readLine();
                if (linha == null || linha.trim().isEmpty()) {
                    throw new IOException("Arquivo inválido: a primeira linha deve conter M e N.");
                }

                // Leitura de M e N como inteiros separados por espaço
                String[] dimensoes = linha.trim().split("\\s+");
                if (dimensoes.length < 2) {
                    throw new IOException(
                            "Arquivo inválido: a primeira linha deve conter M e N separados por espaço.");
                }

                try {
                    M = Integer.parseInt(dimensoes[0]);
                    N = Integer.parseInt(dimensoes[1]);
                } catch (NumberFormatException e) {
                    throw new IOException("Arquivo inválido: M e N devem ser números inteiros.");
                }

                grid = new Celula[M][N];

                for (int i = 0; i < M; i++) {
                    linha = br.readLine();
                    if (linha == null) {
                        throw new IOException("Arquivo inválido: número de linhas inconsistente.");
                    }

                    // Dividir a linha em tokens usando espaços (inclui múltiplos espaços)
                    String[] tokens = linha.trim().split("\\s+");
                    if (tokens.length < N) {
                        throw new IOException(
                                "Arquivo inválido: número de colunas inconsistente na linha " + (i + 1));
                    }

                    for (int j = 0; j < N; j++) {
                        String token = tokens[j];
                        if (token.length() != 1) {
                            throw new IOException("Arquivo inválido: token inválido '" + token + "' na célula ("
                                    + i + "," + j + ").");
                        }
                        char ch = token.charAt(0);
                        Celula celula = new Celula(i, j);
                        grid[i][j] = celula;

                        // Verifica se é um ser (letra maiúscula)
                        if (Character.isLetter(ch) && Character.isUpperCase(ch)) {
                            celula.ser = Ser.fromCodigo(ch);
                            if (celula.ser == null) {
                                System.out.println("Aviso: Código de ser desconhecido '" + ch + "' na célula ("
                                        + i + "," + j + ").");
                            }
                        }

                        // Parse o valor hexadecimal para paredes
                        int valor = Character.digit(ch, 16);
                        if (valor == -1) {
                            throw new IOException("Arquivo inválido: caractere não hexadecimal '" + ch
                                    + "' na célula (" + i + "," + j + ").");
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
                        //System.out.println("Regiao " + regiaoId + " identificada.");
                        regiaoId++;
                    }
                }
            }
            numRegioes = regiaoId;

            System.out.println("Total de regioes identificadas: " + numRegioes);

            // Após identificar todas as regiões, atribuir cores fixas
            for (int id = 0; id < numRegioes; id++) {
                Color color = getColorForRegion(id);
                colorToString(color);

                //Debug para o atribuição de cores no terminal
                //System.out.println("Região " + id + " atribuída à cor: " + colorToString(color));
            }
        }

        // Busca em profundidade para marcar as regiões
        private void dfs(Celula celula, int regiaoId) {
            celula.visitado = true;
            celula.regiao = regiaoId;
            int x = celula.x;
            int y = celula.y;

            // Movimentos: cima, direita, baixo, esquerda
            int[] dx = { -1, 0, 1, 0 };
            int[] dy = { 0, 1, 0, -1 };

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
            System.out.println("Numero de regioes: " + numRegioes);
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
                    System.out.println("Regiao " + regiaoId + ": Ser mais frequente eh "
                            + serMaisFrequente.getNome());
                } else {
                    System.out.println("Regiao " + regiaoId + ": Nenhum ser encontrado");
                }
            }
        }

        // Método para renderizar o labirinto usando Swing
        public void renderizar() {
            JFrame frame = new JFrame("Labirinto do Horror II");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            // Painel principal
            JPanel mainPanel = new JPanel(new BorderLayout());

            LabirintoPanel labirintoPanel = new LabirintoPanel(grid, M, N, this);

            // Adiciona o labirinto a um JScrollPane
            JScrollPane scrollPane = new JScrollPane(labirintoPanel);
            scrollPane.getVerticalScrollBar().setUnitIncrement(16);
            scrollPane.getHorizontalScrollBar().setUnitIncrement(16);

            // Painel de legendas
            LegendPanel legendPanel = new LegendPanel(this);

            // Divisória entre os paineis
            JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scrollPane, legendPanel);
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
            frame.setExtendedState(JFrame.MAXIMIZED_BOTH); // Abre a janela maximizada
            frame.setVisible(true);
        }

        // Método auxiliar para converter cor para string
        private String colorToString(Color color) {
            if (color.equals(Color.GREEN)) return "Verde";
            if (color.equals(Color.BLUE)) return "Azul";
            if (color.equals(Color.ORANGE)) return "Laranja";
            if (color.equals(new Color(252, 144, 198))) return "Pink Claro";
            if (color.equals(Color.CYAN)) return "Ciano";
            if (color.equals(new Color(179, 52, 115))) return "Rosa Escuro";
            if (color.equals(new Color(88, 110, 41))) return "Verde Escuro";
            if (color.equals(Color.LIGHT_GRAY)) return "Cinza Claro";
            if (color.equals(Color.GRAY)) return "Cinza";
            if (color.equals(Color.DARK_GRAY)) return "Cinza Escuro";
            if (color.equals(new Color(45, 125, 252))) return "Azul Claro";
            if (color.equals(new Color(128, 0, 128))) return "Roxo";
            if (color.equals(new Color(255, 165, 0))) return "Laranja";
            if (color.equals(new Color(0, 128, 128))) return "Teal";
            return "Personalizada";
        }
    }

    // Painel personalizado para desenhar o labirinto
    static class LabirintoPanel extends JPanel {
        Celula[][] grid;
        int M, N;
        int tamanhoCelula;
        Labirinto labirinto;

        public LabirintoPanel(Celula[][] grid, int M, int N, Labirinto labirinto) {
            this.grid = grid;
            this.M = M;
            this.N = N;
            this.labirinto = labirinto;

            // Ajusta o tamanho da célula com base no tamanho do labirinto
            int maxDimension = Math.max(M, N);
            if (maxDimension <= 20) {
                tamanhoCelula = 40;
            } else if (maxDimension <= 50) {
                tamanhoCelula = 20;
            } else if (maxDimension <= 100) {
                tamanhoCelula = 10;
            } else if (maxDimension <= 200) {
                tamanhoCelula = 5;
            } else {
                tamanhoCelula = 2;
            }

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

            // Habilita anti-aliasing para melhor qualidade
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Define a espessura da linha
            float strokeWidth = Math.max(1.0f, tamanhoCelula / 10.0f); // Ajusta a espessura com base no tamanho da célula
            g2.setStroke(new BasicStroke(strokeWidth));

            // Desenha as células
            for (int i = 0; i < M; i++) {
                for (int j = 0; j < N; j++) {
                    int x = j * tamanhoCelula;
                    int y = i * tamanhoCelula;
                    Celula celula = grid[i][j];

                    // Preenche a célula com a cor da região
                    Color regionColor = labirinto.getColorForRegion(celula.regiao);
                    g2.setColor(regionColor);
                    g2.fillRect(x, y, tamanhoCelula, tamanhoCelula);

                    // Desenha as paredes em preto
                    g2.setColor(Color.BLACK);
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
                        // Todos os seres agora são vermelhos
                        Color serColor = Color.RED;
                        if (tamanhoCelula >= 10) {
                            int fontSize = tamanhoCelula - 2;
                            g2.setColor(serColor);
                            g2.setFont(new Font("Arial", Font.BOLD, fontSize));
                            String texto = String.valueOf(celula.ser.getCodigo());
                            FontMetrics fm = g2.getFontMetrics();
                            int textoWidth = fm.stringWidth(texto);
                            int textoHeight = fm.getAscent();

                            int textX = x + (tamanhoCelula - textoWidth) / 2;
                            int textY = y + (tamanhoCelula + textoHeight) / 2 - fm.getDescent();

                            g2.drawString(texto, textX, textY);
                        } else {
                            // Desenha um quadrado colorido
                            g2.setColor(serColor);
                            int size = Math.max(1, tamanhoCelula / 2);
                            int offset = (tamanhoCelula - size) / 2;
                            g2.fillRect(x + offset, y + offset, size, size);
                        }
                        g2.setColor(Color.BLACK);
                    }
                }
            }
        }
    }

    // Painel de legendas
    static class LegendPanel extends JPanel {
        Labirinto labirinto;

        public LegendPanel(Labirinto labirinto) {
            this.labirinto = labirinto;

            setLayout(new BorderLayout());
            setBorder(BorderFactory.createTitledBorder("Legenda"));
            setPreferredSize(new Dimension(250, 0)); // Largura fixa

            // Painel para os seres
            JPanel seresPanel = new JPanel();
            seresPanel.setLayout(new BoxLayout(seresPanel, BoxLayout.Y_AXIS));

            // Legenda para os seres (todos em vermelho)
            for (Ser ser : Ser.values()) {
                JPanel itemPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));

                // Pequeno quadrado vermelho para representar o Ser
                JPanel colorPanel = new JPanel();
                colorPanel.setBackground(Color.RED);
                colorPanel.setPreferredSize(new Dimension(16, 16));
                colorPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));

                // Letra do Ser (A, B, etc.)
                JLabel letraLabel = new JLabel(String.valueOf(ser.getCodigo()));
                letraLabel.setFont(new Font("Arial", Font.BOLD, 14));
                letraLabel.setForeground(Color.RED);

                // Nome do Ser
                JLabel nomeLabel = new JLabel(" - " + ser.getNome());
                nomeLabel.setFont(new Font("Arial", Font.PLAIN, 14));

                itemPanel.add(colorPanel);
                itemPanel.add(letraLabel);
                itemPanel.add(nomeLabel);
                seresPanel.add(itemPanel);
            }

            // Espaçamento
            seresPanel.add(Box.createVerticalStrut(20));

            // Painel para as regiões
            JPanel regioesPanel = new JPanel();
            regioesPanel.setLayout(new BorderLayout());

            JLabel regioesLabel = new JLabel("Regiões:");
            regioesLabel.setFont(new Font("Arial", Font.BOLD, 18));
            regioesPanel.add(regioesLabel, BorderLayout.NORTH);

            // Painel para listar as regiões com múltiplas colunas
            JPanel regioesListPanel = new JPanel();
            int numColunas = 3; // Defina o número de colunas desejado
            regioesListPanel.setLayout(new GridLayout(0, numColunas, 10, 10)); // 0 linhas, 3 colunas, 10px gaps

            // Ordenar as regiões por ID
            List<Integer> regiaoIds = new ArrayList<>(labirinto.regionColors.keySet());
            Collections.sort(regiaoIds);

            for (Integer regiaoId : regiaoIds) {
                JPanel itemPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));

                // Pequeno quadrado colorido para representar a Região
                JPanel colorPanel = new JPanel();
                colorPanel.setBackground(labirinto.getColorForRegion(regiaoId));
                colorPanel.setPreferredSize(new Dimension(16, 16));
                colorPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));

                // Número da região
                JLabel nomeLabel = new JLabel(" Região " + regiaoId);
                nomeLabel.setFont(new Font("Arial", Font.PLAIN, 14));

                itemPanel.add(colorPanel);
                itemPanel.add(nomeLabel);
                regioesListPanel.add(itemPanel);
            }

            // Adicionar regioesListPanel a um JScrollPane
            JScrollPane regioesScrollPane = new JScrollPane(regioesListPanel);
            regioesScrollPane.setPreferredSize(new Dimension(250, 300)); // Ajuste conforme necessário
            regioesScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
            regioesScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            regioesScrollPane.setBorder(BorderFactory.createEmptyBorder());

            regioesPanel.add(regioesScrollPane, BorderLayout.CENTER);

            // Painel principal de legendas
            JPanel legendasMainPanel = new JPanel();
            legendasMainPanel.setLayout(new BoxLayout(legendasMainPanel, BoxLayout.Y_AXIS));
            legendasMainPanel.add(seresPanel);
            legendasMainPanel.add(Box.createVerticalStrut(10));
            legendasMainPanel.add(regioesPanel);

            // Adicionar legendasMainPanel a um JScrollPane
            JScrollPane legendasScrollPane = new JScrollPane(legendasMainPanel);
            legendasScrollPane.setBorder(BorderFactory.createEmptyBorder()); // Remove borda extra

            add(legendasScrollPane, BorderLayout.CENTER);
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
            @SuppressWarnings("unused")
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
                JOptionPane.showMessageDialog(null, "Erro ao ler o arquivo: " + e.getMessage(), "Erro",
                        JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        });
    }
}
