import random

def generate_tree(height, width):
    tree = [[' ' for _ in range(width)] for _ in range(height)]
    
    # Iniciar a árvore em um ponto na parte inferior
    start = random.randint(1, width - 2)
    tree[height - 1][start] = '|'
    current_positions = [(height - 1, start)]
    
    for i in range(height - 2, -1, -1):
        next_positions = []
        for row, col in current_positions:
            if col > 1 and col < width - 2:
                direction = random.choice(['left', 'right'])
                if direction == 'left':
                    tree[row-1][col-1] = '\\'
                    tree[row-2][col-2] = str(random.randint(0, 9))
                    next_positions.append((row-2, col-2))
                else:
                    tree[row-1][col+1] = '/'
                    tree[row-2][col+2] = str(random.randint(0, 9))
                    next_positions.append((row-2, col+2))
            tree[row-1][col] = '|'
            next_positions.append((row-1, col))
        
        if random.random() < 0.2:
            for col in [start - 1, start + 1]:
                if 1 <= col < width - 1:
                    tree[i][col] = 'V'
                    next_positions.append((i - 1, col - 1))
                    next_positions.append((i - 1, col + 1))
        
        current_positions = next_positions
    
    # Adiciona folhas
    for j in range(width):
        if tree[0][j] in ['/', '\\', 'V']:
            tree[0][j] = '#'
    
    return tree

def print_tree(tree):
    for row in tree:
        print("".join(row))

# Configurações da árvore
height = 30  # Altura da árvore (número de linhas)
width = 30   # Largura da árvore (número de colunas)

# Gera a árvore
tree = generate_tree(height, width)

# Exibe a árvore gerada
print_tree(tree)
