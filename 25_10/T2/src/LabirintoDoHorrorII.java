package src;

// Importações necessárias para funcionalidades gráficas, entrada/saída e estruturas de dados
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.FlowLayout;
import java.awt.AlphaComposite;
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
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Set;

// Interface para notificar sobre a seleção de regiões no labirinto
interface RegionSelectionListener {
    void regionSelected(Integer regionId); // Passa o ID da região selecionada ou null para deseleção
}

// Classe Edge para representar as arestas da borda de uma região
class Edge {
    int x1, y1, x2, y2;

    public Edge(int x1, int y1, int x2, int y2) {
        // Ordena os pontos para garantir consistência na representação da aresta
        if (x1 < x2 || (x1 == x2 && y1 <= y2)) {
            this.x1 = x1; this.y1 = y1; this.x2 = x2; this.y2 = y2;
        } else {
            this.x1 = x2; this.y1 = y2; this.x2 = x1; this.y2 = y1;
        }
    }

    @Override
    public boolean equals(Object o) {
        // Verifica se duas arestas são iguais com base em suas coordenadas
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Edge edge = (Edge) o;
        return x1 == edge.x1 && y1 == edge.y1 && x2 == edge.x2 && y2 == edge.y2;
    }

    @Override
    public int hashCode() {
        // Gera um código hash para a aresta baseado em suas coordenadas
        int result = x1;
        result = 31 * result + y1;
        result = 31 * result + x2;
        result = 31 * result + y2;
        return result;
    }
}

public class LabirintoDoHorrorII {

    // Enum para os tipos de seres presentes no labirinto
    enum Ser {
        ANAO("Anão", 'A'),
        BRUXA("Bruxa", 'B'),
        CAVALEIRO("Cavaleiro", 'C'),
        DUENDE("Duende", 'D'),
        ELFO("Elfo", 'E'),
        FEIJAO("Feijão", 'F');

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

        // Método para obter o tipo de ser a partir de seu código
        public static Ser fromCodigo(char codigo) {
            for (Ser ser : Ser.values()) {
                if (ser.codigo == codigo) {
                    return ser;
                }
            }
            return null; // Retorna null se o código não corresponder a nenhum ser conhecido
        }
    }

    // Classe que representa uma célula no labirinto
    static class Celula {
        int x, y; // Coordenadas da célula no grid
        boolean[] paredes = new boolean[4]; // Indica a presença de paredes: 0: cima, 1: direita, 2: baixo, 3: esquerda
        boolean visitado = false; // Flag para marcar se a célula já foi visitada durante a busca
        int regiao = -1; // ID da região a que a célula pertence
        Ser ser = null; // Tipo de ser presente na célula, se houver

        public Celula(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    // Classe que representa o labirinto e contém toda a lógica para processamento e visualização
    static class Labirinto {
        int M, N; // Dimensões do labirinto (linhas e colunas)
        Celula[][] grid; // Grid que representa as células do labirinto
        int numRegioes = 0; // Contador de regiões identificadas
        Map<Integer, Map<Ser, Integer>> regioesSeres = new HashMap<>(); // Mapeia cada região para a contagem de tipos de seres
        LabirintoPanel labirintoPanel; // Painel gráfico para renderizar o labirinto

        // Lista fixa de cores para as regiões, excluindo a cor vermelha
        private final List<Color> predefinedColors = Arrays.asList(
                Color.GREEN,
                Color.BLUE,
                Color.ORANGE,
                Color.LIGHT_GRAY,
                Color.CYAN,
                Color.GRAY,
                Color.DARK_GRAY,
                new Color(252, 144, 198),
                new Color(233, 60, 186), // Rosa Choque
                new Color(88, 110, 41), // Verde Militar Escuro
                new Color(45, 125, 252), // Azul Escuro
                new Color(128, 0, 128), // Roxo
                new Color(255, 165, 0), // Laranja
                new Color(0, 128, 127)  // Azul Esverdeado Escuro
        );

        // Mapa de cores para as regiões, associando cada ID de região a uma cor
        Map<Integer, Color> regionColors = new HashMap<>();

        // Mapa para associar cores aos seus nomes para exibição
        private static final Map<Integer, String> colorNameMap = new HashMap<>();

        // Inicialização estática do mapa de cores com seus respectivos nomes
        static {
            colorNameMap.put(Color.GREEN.getRGB(), "Verde");
            colorNameMap.put(Color.BLUE.getRGB(), "Azul");
            colorNameMap.put(Color.ORANGE.getRGB(), "Laranja");
            colorNameMap.put(Color.LIGHT_GRAY.getRGB(), "Cinza Claro");
            colorNameMap.put(Color.CYAN.getRGB(), "Ciano");
            colorNameMap.put(Color.GRAY.getRGB(), "Cinza");
            colorNameMap.put(Color.DARK_GRAY.getRGB(), "Cinza Escuro");
            colorNameMap.put(new Color(252, 144, 198).getRGB(), "Pink Claro");
            colorNameMap.put(new Color(233, 60, 186).getRGB(), "Rosa Choque");
            colorNameMap.put(new Color(88, 110, 41).getRGB(), "Verde Militar Escuro");
            colorNameMap.put(new Color(45, 125, 252).getRGB(), "Azul Escuro");
            colorNameMap.put(new Color(128, 0, 128).getRGB(), "Roxo");
            colorNameMap.put(new Color(255, 165, 0).getRGB(), "Laranja");
            colorNameMap.put(new Color(0, 128, 127).getRGB(), "Azul Esverdeado Escuro");
        }

        /**
         * Método para obter a cor associada a uma região.
         * Se a região já tiver uma cor atribuída, retorna essa cor.
         * Caso contrário, atribui uma cor da lista predefinedColors com base no ID da região.
         *
         * @param regionId ID da região
         * @return Cor associada à região
         */
        Color getColorForRegion(int regionId) {
            // Verifica se já existe uma cor atribuída para esta região
            if (regionColors.containsKey(regionId)) {
                return regionColors.get(regionId);
            }

            // Atribui uma cor da lista predefinedColors com base no ID da região
            Color color = predefinedColors.get(regionId % predefinedColors.size());
            regionColors.put(regionId, color);
            return color;
        }

        /**
         * Método para ler o labirinto a partir de um arquivo.
         * O arquivo deve estar no formato especificado, com M e N na primeira linha
         * e as linhas subsequentes representando as células com paredes e possíveis seres.
         *
         * @param filePath Caminho para o arquivo de labirinto
         * @throws IOException Se houver problemas na leitura ou formatação do arquivo
         */
        public void readMazeFromFile(String filePath) throws IOException {
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(new FileInputStream(filePath), "UTF-8"))) {
                String linha = br.readLine();
                if (linha == null || linha.trim().isEmpty()) {
                    throw new IOException("Arquivo invalido: a primeira linha deve conter M e N.");
                }

                // Leitura de M e N como inteiros separados por espaço
                String[] dimensoes = linha.trim().split("\\s+");
                if (dimensoes.length < 2) {
                    throw new IOException(
                            "Arquivo invalido: a primeira linha deve conter M e N separados por espaço.");
                }

                try {
                    M = Integer.parseInt(dimensoes[0]);
                    N = Integer.parseInt(dimensoes[1]);
                } catch (NumberFormatException e) {
                    throw new IOException("Arquivo inválido: M e N devem ser números inteiros.");
                }

                grid = new Celula[M][N];

                // Leitura das linhas que representam as células do labirinto
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

                        // Verifica se o caractere representa um ser (letra maiúscula)
                        if (Character.isLetter(ch) && Character.isUpperCase(ch)) {
                            celula.ser = Ser.fromCodigo(ch);
                            if (celula.ser == null) {
                                System.out.println("Aviso: Código de ser desconhecido '" + ch + "' na célula ("
                                        + i + "," + j + ").");
                            }
                        }

                        // Converte o caractere hexadecimal para representação de paredes
                        int valor = Character.digit(ch, 16);
                        if (valor == -1) {
                            throw new IOException("Arquivo inválido: caractere não hexadecimal '" + ch
                                    + "' na célula (" + i + "," + j + ").");
                        }

                        // Converte o valor hexadecimal para bits que representam as paredes
                        for (int k = 0; k < 4; k++) {
                            celula.paredes[k] = ((valor >> (3 - k)) & 1) == 1;
                        }
                    }
                }
            }
        }

        /**
         * Método para encontrar as regiões no labirinto usando busca em profundidade (DFS).
         * Cada célula conectada sem paredes forma uma região.
         */
        public void encontrarRegioes() {
            int regiaoId = 0; // Inicializa o ID da região
            for (int i = 0; i < M; i++) {
                for (int j = 0; j < N; j++) {
                    Celula celula = grid[i][j];
                    if (!celula.visitado) {
                        dfs(celula, regiaoId); // Executa DFS a partir desta célula
                        regiaoId++; // Incrementa o ID para a próxima região
                    }
                }
            }
            numRegioes = regiaoId; // Total de regiões identificadas

            System.out.println("Total de regioes identificadas: " + numRegioes);

            // Após identificar todas as regiões, atribuir cores fixas
            for (int id = 0; id < numRegioes; id++) {
                Color color = getColorForRegion(id);
                colorToString(color);

                // Debug para a atribuição de cores no terminal:
                // System.out.println("Região " + id + " atribuída à cor: " + colorToString(color));
            }
        }

        /**
         * Método auxiliar que implementa a busca em profundidade (DFS) para marcar as regiões.
         *
         * @param celula   Célula atual na busca
         * @param regiaoId ID da região sendo marcada
         */
        private void dfs(Celula celula, int regiaoId) {
            celula.visitado = true; // Marca a célula como visitada
            celula.regiao = regiaoId; // Atribui o ID da região à célula
            int x = celula.x;
            int y = celula.y;

            // Arrays para facilitar os movimentos nas 4 direções: cima, direita, baixo, esquerda
            int[] dx = { -1, 0, 1, 0 };
            int[] dy = { 0, 1, 0, -1 };

            for (int k = 0; k < 4; k++) {
                int nx = x + dx[k];
                int ny = y + dy[k];

                // Se não há parede na direção k e a célula vizinha está dentro dos limites
                if (!celula.paredes[k]) {
                    if (nx >= 0 && nx < M && ny >= 0 && ny < N) {
                        Celula vizinho = grid[nx][ny];
                        if (!vizinho.visitado) {
                            dfs(vizinho, regiaoId); // Continua a busca a partir da célula vizinha
                        }
                    }
                }
            }
        }

        /**
         * Método para contar os seres em cada região após a identificação das regiões.
         * Atualiza o mapa regioesSeres com a contagem de cada tipo de ser em cada região.
         */
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

        /**
         * Método para imprimir os resultados das regiões e os seres mais frequentes em cada uma.
         * Exibe no terminal o número total de regiões e o ser mais frequente em cada região.
         */
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

        /**
         * Método auxiliar para converter uma cor para uma string usando o mapa colorNameMap.
         * Se a cor não estiver no mapa, retorna "Personalizada".
         *
         * @param color Objeto Color a ser convertido
         * @return Nome da cor como string
         */
        private String colorToString(Color color) {
            String colorName = colorNameMap.get(color.getRGB());
            return colorName != null ? colorName : "Personalizada";
        }

        /**
         * Método para renderizar o labirinto usando Swing.
         * Cria a interface gráfica, incluindo o painel do labirinto, a legenda e os controles.
         */
        public void renderizar() {
            JFrame frame = new JFrame("Labirinto do Horror II");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            // Painel principal com layout BorderLayout
            JPanel mainPanel = new JPanel(new BorderLayout());

            // Inicializa o painel que desenha o labirinto
            labirintoPanel = new LabirintoPanel(grid, M, N, this);

            // Painel de legendas que mostra informações sobre os seres e regiões
            LegendPanel legendPanel = new LegendPanel(this);

            // Registrar o LegendPanel como ouvinte para seleção de regiões
            labirintoPanel.addRegionSelectionListener(legendPanel);

            // Adiciona o painel do labirinto a um JScrollPane para permitir rolagem
            JScrollPane scrollPane = new JScrollPane(labirintoPanel);
            scrollPane.getVerticalScrollBar().setUnitIncrement(16);
            scrollPane.getHorizontalScrollBar().setUnitIncrement(16);

            // Divisória entre os painéis do labirinto e da legenda
            JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scrollPane, legendPanel);
            splitPane.setDividerLocation(0.75); // Define a posição inicial da divisória
            splitPane.setResizeWeight(0.75); // Prioriza o redimensionamento do labirinto
            mainPanel.add(splitPane, BorderLayout.CENTER);

            // Botões de controle para zoom e seleção de outro arquivo de labirinto
            JButton zoomInButton = new JButton("Zoom In");
            JButton zoomOutButton = new JButton("Zoom Out");
            JButton botaoSelecionarArquivo = new JButton("Selecionar Outro Labirinto");

            // Painel de controles com layout FlowLayout centralizado
            JPanel controlsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
            controlsPanel.add(zoomInButton);
            controlsPanel.add(zoomOutButton);
            controlsPanel.add(botaoSelecionarArquivo);

            // Adiciona ActionListeners aos botões de zoom para ajustar o nível de zoom no painel do labirinto
            zoomInButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    labirintoPanel.zoomIn();
                }
            });

            zoomOutButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    labirintoPanel.zoomOut();
                }
            });

            // ActionListener para o botão de selecionar outro labirinto
            botaoSelecionarArquivo.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    frame.dispose(); // Fecha a janela atual
                    SwingUtilities.invokeLater(() -> main(null)); // Reinicia o método main para selecionar outro arquivo
                }
            });

            // Adiciona o painel de controles ao painel principal na região sul
            mainPanel.add(controlsPanel, BorderLayout.SOUTH);

            // Configura o frame principal
            frame.setContentPane(mainPanel);
            frame.setExtendedState(JFrame.MAXIMIZED_BOTH); // Abre a janela maximizada
            frame.setVisible(true);
        }
    }

    // Classe personalizada para desenhar o labirinto e lidar com interações de clique
    static class LabirintoPanel extends JPanel {
        Celula[][] grid; // Grid de células do labirinto
        int M, N; // Dimensões do labirinto
        int tamanhoCelula; // Tamanho de cada célula na renderização
        Labirinto labirinto; // Referência ao objeto Labirinto

        // ID da região atualmente destacada (-1 se nenhuma)
        private int highlightedRegionId = -1;

        // Arestas da região destacada para desenhar a borda
        private Set<Edge> highlightedRegionEdges = null;

        // Lista de ouvintes que serão notificados sobre a seleção de regiões
        private List<RegionSelectionListener> listeners = new ArrayList<>();

        // Variáveis para controle do zoom
        private final int minCellSize = 2;
        private int maxCellSize = 100;
        private int zoomStep = 5;

        /**
         * Construtor da classe LabirintoPanel.
         *
         * @param grid      Grid de células do labirinto
         * @param M         Número de linhas
         * @param N         Número de colunas
         * @param labirinto Referência ao objeto Labirinto
         */
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

            // Define o tamanho preferido do painel com base nas dimensões do labirinto
            updatePreferredSize();

            // Adiciona um MouseListener para detectar cliques nas células do labirinto
            addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    handleClick(e.getX(), e.getY()); // Processa o clique na posição (x, y)
                }
            });
        }

        /**
         * Método para atualizar o tamanho preferido do painel com base nas dimensões do labirinto e no tamanho das células.
         */
        private void updatePreferredSize() {
            setPreferredSize(new Dimension(N * tamanhoCelula, M * tamanhoCelula));
        }

        /**
         * Método para aumentar o nível de zoom, aumentando o tamanho das células.
         * Limita o tamanho máximo das células para evitar que o painel fique muito grande.
         */
        public void zoomIn() {
            if (tamanhoCelula + zoomStep <= maxCellSize) {
                tamanhoCelula += zoomStep;
                updatePreferredSize();
                if (highlightedRegionId != -1) {
                    highlightedRegionEdges = computeRegionBoundary(highlightedRegionId); // Recalcula as bordas da região destacada
                }
                revalidate(); // Revalida o layout do painel
                repaint(); // Redesenha o painel
            }
        }

        /**
         * Método para diminuir o nível de zoom, reduzindo o tamanho das células.
         * Limita o tamanho mínimo das células para evitar que o labirinto se torne ilegível.
         */
        public void zoomOut() {
            if (tamanhoCelula - zoomStep >= minCellSize) {
                tamanhoCelula -= zoomStep;
                updatePreferredSize();
                if (highlightedRegionId != -1) {
                    highlightedRegionEdges = computeRegionBoundary(highlightedRegionId); // Recalcula as bordas da região destacada
                }
                revalidate(); // Revalida o layout do painel
                repaint(); // Redesenha o painel
            }
        }

        /**
         * Método para adicionar ouvintes que serão notificados sobre a seleção de regiões.
         *
         * @param listener Ouvinte que implementa a interface RegionSelectionListener
         */
        public void addRegionSelectionListener(RegionSelectionListener listener) {
            listeners.add(listener);
        }

        /**
         * Método para lidar com cliques do mouse no painel do labirinto.
         * Determina qual célula foi clicada e destaca a região correspondente.
         *
         * @param mouseX Coordenada X do clique
         * @param mouseY Coordenada Y do clique
         */
        private void handleClick(int mouseX, int mouseY) {
            int j = mouseX / tamanhoCelula; // Calcula a coluna clicada
            int i = mouseY / tamanhoCelula; // Calcula a linha clicada

            if (i >= 0 && i < M && j >= 0 && j < N) {
                Celula clickedCell = grid[i][j];
                int clickedRegionId = clickedCell.regiao;

                if (highlightedRegionId == clickedRegionId) {
                    // Se a mesma região foi clicada novamente, deseleciona
                    highlightedRegionId = -1;
                    highlightedRegionEdges = null;
                    notifyRegionSelection(null);
                } else {
                    // Seleciona a nova região e calcula suas bordas
                    highlightedRegionId = clickedRegionId;
                    highlightedRegionEdges = computeRegionBoundary(highlightedRegionId);
                    notifyRegionSelection(highlightedRegionId);
                }
                repaint(); // Redesenha o painel para refletir as mudanças
            } else {
                // Clique fora de qualquer célula, deseleciona a região destacada
                if (highlightedRegionId != -1) {
                    highlightedRegionId = -1;
                    highlightedRegionEdges = null;
                    notifyRegionSelection(null);
                    repaint();
                }
            }
        }

        /**
         * Método para notificar todos os ouvintes sobre a seleção de uma região.
         *
         * @param regionId ID da região selecionada ou null se nenhuma estiver selecionada
         */
        private void notifyRegionSelection(Integer regionId) {
            for (RegionSelectionListener listener : listeners) {
                listener.regionSelected(regionId);
            }
        }

        /**
         * Método para computar as arestas de borda de uma região destacada.
         * Isso é usado para desenhar uma borda amarela ao redor da região selecionada.
         *
         * @param regionId ID da região a ser destacada
         * @return Conjunto de arestas que formam a borda da região
         */
        private Set<Edge> computeRegionBoundary(int regionId) {
            Set<Edge> boundaryEdges = new HashSet<>();
            for (int i = 0; i < M; i++) {
                for (int j = 0; j < N; j++) {
                    Celula celula = grid[i][j];
                    if (celula.regiao == regionId) {
                        int x = j;
                        int y = i;

                        // Arrays para facilitar os movimentos nas 4 direções: cima, direita, baixo, esquerda
                        int[] dx = {0, 1, 0, -1};
                        int[] dy = {-1, 0, 1, 0};

                        for (int k = 0; k < 4; k++) {
                            int nx = x + dx[k];
                            int ny = y + dy[k];

                            boolean isBoundary = false;

                            if (celula.paredes[k]) {
                                // Há uma parede; esta é uma aresta de borda
                                isBoundary = true;
                            } else {
                                // Não há parede; verificar se o vizinho está fora da região ou do labirinto
                                if (nx < 0 || nx >= N || ny < 0 || ny >= M) {
                                    isBoundary = true;
                                } else {
                                    Celula neighbor = grid[ny][nx];
                                    if (neighbor.regiao != regionId) {
                                        isBoundary = true;
                                    }
                                }
                            }

                            if (isBoundary) {
                                // Calcula as coordenadas dos pontos finais da aresta com base na direção
                                int x1 = x * tamanhoCelula;
                                int y1 = y * tamanhoCelula;
                                int x2 = x1;
                                int y2 = y1;

                                switch (k) {
                                    case 0: // Cima
                                        x2 = x1 + tamanhoCelula;
                                        y2 = y1;
                                        break;
                                    case 1: // Direita
                                        x1 = x1 + tamanhoCelula;
                                        x2 = x1;
                                        y2 = y1 + tamanhoCelula;
                                        break;
                                    case 2: // Baixo
                                        x2 = x1 + tamanhoCelula;
                                        y1 = y1 + tamanhoCelula;
                                        y2 = y1;
                                        break;
                                    case 3: // Esquerda
                                        y2 = y1 + tamanhoCelula;
                                        break;
                                }
                                Edge edge = new Edge(x1, y1, x2, y2);
                                boundaryEdges.add(edge); // Adiciona a aresta ao conjunto de bordas
                            }
                        }
                    }
                }
            }
            return boundaryEdges;
        }

        /**
         * Método sobrescrito para desenhar o labirinto no painel.
         * Utiliza Graphics2D para melhor controle sobre a renderização.
         *
         * @param g Objeto Graphics utilizado para desenhar
         */
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            // Preenche o fundo com cor branca
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, getWidth(), getHeight());

            // Cria uma cópia de Graphics para configurações avançadas
            Graphics2D g2 = (Graphics2D) g.create();

            // Habilita anti-aliasing para melhor qualidade de desenho
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Define a espessura das linhas com base no tamanho das células
            float strokeWidth = Math.max(1.0f, tamanhoCelula / 10.0f);
            g2.setStroke(new BasicStroke(strokeWidth));

            // Itera sobre todas as células para desenhá-las
            for (int i = 0; i < M; i++) {
                for (int j = 0; j < N; j++) {
                    int x = j * tamanhoCelula;
                    int y = i * tamanhoCelula;
                    Celula celula = grid[i][j];

                    // Determina a cor da célula com base na região
                    Color regionColor = labirinto.getColorForRegion(celula.regiao);

                    g2.setColor(regionColor);
                    g2.fillRect(x, y, tamanhoCelula, tamanhoCelula); // Preenche a célula com a cor da região

                    if (highlightedRegionId != -1 && celula.regiao != highlightedRegionId) {
                        // Preenche células que não estão na região destacada com preto semi-transparente
                        Composite originalComposite = g2.getComposite();
                        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.85f));
                        g2.setColor(Color.BLACK);
                        g2.fillRect(x, y, tamanhoCelula, tamanhoCelula);
                        g2.setComposite(originalComposite);
                    }

                    // Desenha as paredes da célula em preto
                    g2.setColor(Color.BLACK);
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

                    // Desenha o ser, se houver, apenas na região destacada ou se nenhuma região estiver destacada
                    if (celula.ser != null && (highlightedRegionId == -1 || celula.regiao == highlightedRegionId)) {
                        // Define a cor dos seres como vermelho
                        Color serColor = Color.RED;
                        if (tamanhoCelula >= 10) {
                            // Desenha o código do ser como uma letra centralizada na célula
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
                            // Desenha um pequeno quadrado vermelho para representar o ser
                            g2.setColor(serColor);
                            int size = Math.max(1, tamanhoCelula / 2);
                            int offset = (tamanhoCelula - size) / 2;
                            g2.fillRect(x + offset, y + offset, size, size);
                        }
                        g2.setColor(Color.BLACK); // Restaura a cor para desenhar outras paredes
                    }
                }
            }

            // Desenha a borda amarela ao redor da região destacada
            if (highlightedRegionId != -1 && highlightedRegionEdges != null) {
                g2.setColor(Color.YELLOW); // Cor da borda destacada
                g2.setStroke(new BasicStroke(2.0f)); // Espessura da borda
                for (Edge edge : highlightedRegionEdges) {
                    g2.drawLine(edge.x1, edge.y1, edge.x2, edge.y2); // Desenha cada aresta da borda
                }
            }

            g2.dispose(); // Libera os recursos utilizados pelo Graphics2D
        }
    }

    // Classe que representa o painel de legendas, exibindo informações sobre os seres e regiões
    static class LegendPanel extends JPanel implements RegionSelectionListener {
        Labirinto labirinto;

        // Componente para exibir informações sobre a região selecionada
        private JLabel infoLabel;

        /**
         * Construtor da classe LegendPanel.
         *
         * @param labirinto Referência ao objeto Labirinto para acessar informações sobre as regiões
         */
        public LegendPanel(Labirinto labirinto) {
            this.labirinto = labirinto;

            setLayout(new BorderLayout());
            setBorder(BorderFactory.createTitledBorder("Legenda"));
            setPreferredSize(new Dimension(250, 0)); // Define uma largura fixa para o painel de legendas

            // Painel para exibir a legenda dos seres
            JPanel seresPanel = new JPanel();
            seresPanel.setLayout(new BoxLayout(seresPanel, BoxLayout.Y_AXIS));

            // Cria uma legenda para cada tipo de ser, todos representados com cores vermelhas
            for (Ser ser : Ser.values()) {
                JPanel itemPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));

                // Pequeno quadrado vermelho para representar o ser na legenda
                JPanel colorPanel = new JPanel();
                colorPanel.setBackground(Color.RED);
                colorPanel.setPreferredSize(new Dimension(16, 16));
                colorPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));

                // Letra que representa o ser
                JLabel letraLabel = new JLabel(String.valueOf(ser.getCodigo()));
                letraLabel.setFont(new Font("Arial", Font.BOLD, 12));
                letraLabel.setForeground(Color.RED);

                // Nome do ser
                JLabel nomeLabel = new JLabel(" - " + ser.getNome());
                nomeLabel.setFont(new Font("Arial", Font.PLAIN, 14));

                // Adiciona os componentes ao itemPanel e, em seguida, ao seresPanel
                itemPanel.add(colorPanel);
                itemPanel.add(letraLabel);
                itemPanel.add(nomeLabel);
                seresPanel.add(itemPanel);
            }

            // Espaçamento entre a legenda dos seres e das regiões
            seresPanel.add(Box.createVerticalStrut(5));

            // Painel para exibir as regiões com suas cores correspondentes
            JPanel regioesPanel = new JPanel();
            regioesPanel.setLayout(new BorderLayout());

            JLabel regioesLabel = new JLabel("Regiões:");
            regioesLabel.setFont(new Font("Arial", Font.BOLD, 18));
            regioesPanel.add(regioesLabel, BorderLayout.NORTH);

            // Painel para listar as regiões em múltiplas colunas
            JPanel regioesListPanel = new JPanel();
            int numColunas = 3; // Define o número de colunas desejado para a lista de regiões
            regioesListPanel.setLayout(new GridLayout(0, numColunas, 10, 10)); // 0 linhas, 3 colunas, gaps de 10px

            // Ordena as regiões por ID para uma apresentação consistente
            List<Integer> regiaoIds = new ArrayList<>(labirinto.regionColors.keySet());
            Collections.sort(regiaoIds);

            // Cria um item de legenda para cada região
            for (Integer regiaoId : regiaoIds) {
                JPanel itemPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));

                // Pequeno quadrado colorido para representar a região na legenda
                JPanel colorPanel = new JPanel();
                colorPanel.setBackground(labirinto.getColorForRegion(regiaoId));
                colorPanel.setPreferredSize(new Dimension(16, 16));
                colorPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));

                // Número da região
                JLabel nomeLabel = new JLabel(" Regiao " + regiaoId);
                nomeLabel.setFont(new Font("Arial", Font.PLAIN, 14));

                // Adiciona os componentes ao itemPanel e, em seguida, ao regioesListPanel
                itemPanel.add(colorPanel);
                itemPanel.add(nomeLabel);
                regioesListPanel.add(itemPanel);
            }

            // Adiciona o painel de regiões a um JScrollPane para permitir rolagem se necessário
            JScrollPane regioesScrollPane = new JScrollPane(regioesListPanel);
            regioesScrollPane.setPreferredSize(new Dimension(250, 300)); // Define o tamanho preferido do scroll
            regioesScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
            regioesScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            regioesScrollPane.setBorder(BorderFactory.createEmptyBorder());

            regioesPanel.add(regioesScrollPane, BorderLayout.CENTER);

            // Painel principal que contém as legendas dos seres e das regiões
            JPanel legendasMainPanel = new JPanel();
            legendasMainPanel.setLayout(new BoxLayout(legendasMainPanel, BoxLayout.Y_AXIS));
            legendasMainPanel.add(seresPanel);
            legendasMainPanel.add(Box.createVerticalStrut(10));
            legendasMainPanel.add(regioesPanel);

            // Adiciona o painel de legendas principal a um JScrollPane
            JScrollPane legendasScrollPane = new JScrollPane(legendasMainPanel);
            legendasScrollPane.setBorder(BorderFactory.createEmptyBorder()); // Remove borda extra

            // Adiciona o JScrollPane ao LegendPanel
            add(legendasScrollPane, BorderLayout.CENTER);

            // Área para exibir informações sobre a região selecionada
            infoLabel = new JLabel("Clique em uma região para ver detalhes.");
            infoLabel.setFont(new Font("Arial", Font.PLAIN, 14));
            infoLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            add(infoLabel, BorderLayout.SOUTH);
        }

        /**
         * Implementação do método da interface RegionSelectionListener.
         * Atualiza o infoLabel com informações sobre a região selecionada.
         *
         * @param regionId ID da região selecionada ou null se nenhuma estiver selecionada
         */
        @Override
        public void regionSelected(Integer regionId) {
            if (regionId == null) {
                // Nenhuma região selecionada
                infoLabel.setText("Clique em uma região para ver detalhes.");
            } else {
                // Obtém o ser mais frequente na região selecionada
                Map<Ser, Integer> mapaSeres = labirinto.regioesSeres.getOrDefault(regionId, new HashMap<>());
                if (!mapaSeres.isEmpty()) {
                    Ser serMaisFrequente = null;
                    int maxContagem = 0;
                    for (Map.Entry<Ser, Integer> entry : mapaSeres.entrySet()) {
                        if (entry.getValue() > maxContagem) {
                            maxContagem = entry.getValue();
                            serMaisFrequente = entry.getKey();
                        }
                    }
                    // Atualiza o label com o ser mais frequente
                    infoLabel.setText("<html>Região " + regionId + ":<br>Ser mais frequente: " + serMaisFrequente.getNome() + "</html>");
                } else {
                    // Nenhum ser encontrado na região
                    infoLabel.setText("<html>Região " + regionId + ":<br>Nenhum Ser encontrado.</html>");
                }
            }
        }
    }

    /**
     * Método principal que inicia o programa.
     * Configura a codificação de caracteres, cria o labirinto, lê o arquivo, processa as regiões e renderiza a interface gráfica.
     *
     * @param args Argumentos de linha de comando (não utilizados)
     */
    public static void main(String[] args) {
        // Define o padrão de codificação para UTF-8 para evitar problemas com caracteres especiais
        System.setProperty("file.encoding", "UTF-8");

        // Executa o programa na thread da interface gráfica
        SwingUtilities.invokeLater(() -> {
            String caminhoArquivo = selectFileDialog(); // Abre um diálogo para selecionar o arquivo do labirinto

            if (caminhoArquivo == null) {
                // Usuário cancelou a seleção, encerra o programa
                System.exit(0);
            }

            // Cria o objeto Labirinto
            Labirinto labirinto = new Labirinto();

            try {
                labirinto.readMazeFromFile(caminhoArquivo); // Lê o arquivo do labirinto
                labirinto.encontrarRegioes(); // Identifica as regiões no labirinto
                labirinto.contarSeres(); // Conta os seres em cada região
                labirinto.imprimirResultados(); // Imprime os resultados no terminal
                labirinto.renderizar(); // Renderiza a interface gráfica do labirinto
            } catch (IOException e) {
                // Exibe uma mensagem de erro caso ocorra algum problema na leitura do arquivo
                JOptionPane.showMessageDialog(null, "Erro ao ler o arquivo: " + e.getMessage(), "Erro",
                        JOptionPane.ERROR_MESSAGE);
                e.printStackTrace(); // Imprime a pilha de exceções no terminal para depuração
            }
        });
    }

    /**
     * Método para abrir um diálogo de seleção de arquivo.
     * Permite ao usuário selecionar uma pasta contendo arquivos de labirinto e, em seguida, escolher um arquivo .txt específico.
     *
     * @return Caminho absoluto do arquivo selecionado ou null se o usuário cancelar a operação
     */
    private static String selectFileDialog() {
        String selectedFile = null;
        JFileChooser folderChooser = new JFileChooser();
        folderChooser.setDialogTitle("Selecione a pasta que contém os arquivos de labirinto");
        folderChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        folderChooser.setAcceptAllFileFilterUsed(false);

        int result = folderChooser.showOpenDialog(null);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFolder = folderChooser.getSelectedFile();
            // Filtra apenas os arquivos com extensão .txt na pasta selecionada
            File[] txtFiles = selectedFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".txt"));

            if (txtFiles != null && txtFiles.length > 0) {
                Arrays.sort(txtFiles, Comparator.comparing(File::getName)); // Ordena os arquivos por nome

                String[] fileNames = Arrays.stream(txtFiles)
                        .map(File::getName)
                        .toArray(String[]::new);

                // Abre um diálogo para o usuário escolher um dos arquivos .txt
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
                // Informa ao usuário que nenhum arquivo .txt foi encontrado na pasta selecionada
                JOptionPane.showMessageDialog(null, "Nenhum arquivo TXT encontrado na pasta selecionada.");
            }
        }
        return selectedFile;
    }
}