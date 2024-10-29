import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.swing.*;

public class LabirintoDoHorrorII {

    // Enum para os tipos de seres
    enum Ser {
        ANAO("Anão", 'A'),
        BRUXA("Bruxa", 'B'),
        CAVALHEIRO("Cavalheiro", 'C'),
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
        public void lerArquivo(String nomeArquivo) throws IOException {
            BufferedReader br = new BufferedReader(new FileReader(nomeArquivo));
            String linha = br.readLine();
            if (linha == null || linha.length() < 2) {
                br.close();
                throw new IOException("Arquivo inválido: a primeira linha deve conter M e N.");
            }

            try {
                M = Integer.parseInt(String.valueOf(linha.charAt(0)));
                N = Integer.parseInt(String.valueOf(linha.charAt(1)));
            } catch (NumberFormatException e) {
                br.close();
                throw new IOException("Arquivo inválido: M e N devem ser dígitos.");
            }

            grid = new Celula[M][N];

            for (int i = 0; i < M; i++) {
                linha = br.readLine();
                if (linha == null || linha.length() < N) {
                    br.close();
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
                            br.close();
                            throw new IOException("Arquivo inválido: caractere não hexadecimal '" + ch + "' na célula (" + i + "," + j + ").");
                        }
                    }

                    // Converte o valor hexadecimal para bits (paredes)
                    for (int k = 0; k < 4; k++) {
                        celula.paredes[k] = ((valor >> (3 - k)) & 1) == 1;
                    }
                }
            }
            br.close();
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
            frame.setSize(N * 60 + 50, M * 60 + 70);
            frame.setLocationRelativeTo(null);

            LabirintoPanel panel = new LabirintoPanel(grid, M, N);
            frame.add(panel);
            frame.setVisible(true);
        }
    }

    // Painel personalizado para desenhar o labirinto
    static class LabirintoPanel extends JPanel {
        Celula[][] grid;
        int M, N;

        public LabirintoPanel(Celula[][] grid, int M, int N) {
            this.grid = grid;
            this.M = M;
            this.N = N;
            // Define o tamanho preferido
            setPreferredSize(new Dimension(N * 60, M * 60));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            int tamanhoCelula = 60;

            // Desenha as células
            for (int i = 0; i < M; i++) {
                for (int j = 0; j < N; j++) {
                    int x = j * tamanhoCelula;
                    int y = i * tamanhoCelula;
                    Celula celula = grid[i][j];

                    // Desenha as paredes
                    g.setColor(Color.BLACK);
                    if (celula.paredes[0]) { // Cima
                        g.drawLine(x, y, x + tamanhoCelula, y);
                    }
                    if (celula.paredes[1]) { // Direita
                        g.drawLine(x + tamanhoCelula, y, x + tamanhoCelula, y + tamanhoCelula);
                    }
                    if (celula.paredes[2]) { // Baixo
                        g.drawLine(x, y + tamanhoCelula, x + tamanhoCelula, y + tamanhoCelula);
                    }
                    if (celula.paredes[3]) { // Esquerda
                        g.drawLine(x, y, x, y + tamanhoCelula);
                    }

                    // Desenha o ser, se houver
                    if (celula.ser != null) {
                        g.setColor(Color.RED);
                        g.setFont(new Font("Arial", Font.BOLD, 14));
                        String texto = String.valueOf(celula.ser.getCodigo());
                        FontMetrics fm = g.getFontMetrics();
                        int textoWidth = fm.stringWidth(texto);
                        int textoHeight = fm.getHeight();
                        g.drawString(texto, x + (tamanhoCelula - textoWidth) / 2, y + (tamanhoCelula + textoHeight / 2) / 2);
                        g.setColor(Color.BLACK);
                    }
                }
            }
        }
    }

    public static void main(String[] args) {
        // Diretório onde estão os arquivos de labirintos
        String pastaLabirintos = "Labirintos";

        File dir = new File(pastaLabirintos);
        if (!dir.exists() || !dir.isDirectory()) {
            JOptionPane.showMessageDialog(null, "Pasta 'Labirintos' não encontrada no diretório atual.", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Lista todos os arquivos .txt na pasta
        File[] arquivos = dir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".txt");
            }
        });

        if (arquivos == null || arquivos.length == 0) {
            JOptionPane.showMessageDialog(null, "Nenhum arquivo .txt encontrado na pasta 'Labirintos'.", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Cria um array com os nomes dos arquivos
        String[] nomesArquivos = new String[arquivos.length];
        for (int i = 0; i < arquivos.length; i++) {
            nomesArquivos[i] = arquivos[i].getName();
        }

        // Cria o JComboBox para seleção dos arquivos
        JComboBox<String> comboBox = new JComboBox<>(nomesArquivos);
        comboBox.setSelectedIndex(0);

        // Mostra o diálogo de seleção
        int resultado = JOptionPane.showConfirmDialog(null, comboBox, "Selecione um arquivo de labirinto", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (resultado != JOptionPane.OK_OPTION) {
            // Usuário cancelou
            return;
        }

        String arquivoSelecionado = (String) comboBox.getSelectedItem();
        String caminhoArquivo = pastaLabirintos + File.separator + arquivoSelecionado;

        // Cria o labirinto
        Labirinto labirinto = new Labirinto();

        try {
            labirinto.lerArquivo(caminhoArquivo);
            labirinto.encontrarRegioes();
            labirinto.contarSeres();
            labirinto.imprimirResultados();
            labirinto.renderizar();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Erro ao ler o arquivo: " + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
}
