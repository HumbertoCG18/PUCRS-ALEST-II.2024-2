import random

def generate_tree(height, width):
    tree = [[' ' for _ in range(width)] for _ in range(height)]
    
    # Inicializar a árvore no centro
    start = width // 2
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
        
        if random.random() < 0.3:
            for col in [start - 1, start + 1]:
                if 1 <= col < width - 1:
                    tree[i][col] = 'V'
                    next_positions.append((i - 1, col - 1))
                    next_positions.append((i - 1, col + 1))
        
        current_positions = next_positions
    
    # Adicionar folhas na parte superior
    for j in range(width):
        if tree[0][j] in ['/', '\\', 'V']:
            tree[0][j] = '#'
    
    return tree

def print_tree(tree):
    for row in tree:
        print("".join(row))

def extract_fruit_values(tree):
    values = []
    for row in tree:
        value_row = []
        for char in row:
            if char.isdigit():
                value_row.append(int(char))
            else:
                value_row.append(0)
        values.append(value_row)
    return values

def find_max_path(tree, values):
    rows = len(values)
    
    max_path_sums = [row[:] for row in values]
    
    for i in range(rows - 2, -1, -1):
        for j in range(len(tree[i])):
            if tree[i][j] in ['/', '\\', '|', 'V']:
                left = max_path_sums[i + 1][j - 1] if j > 0 else 0
                right = max_path_sums[i + 1][j + 1] if j + 1 < len(max_path_sums[i + 1]) else 0
                max_path_sums[i][j] += max(left, right)
    
    return max(max_path_sums[0])

# Configurações da árvore
height = 30  # Altura da árvore (número de linhas)
width = 30   # Largura da árvore (número de colunas)

# Gera a árvore
tree = generate_tree(height, width)

# Exibe a árvore gerada
print_tree(tree)

# Extrai os valores das frutas
values = extract_fruit_values(tree)

# Calcula o caminho máximo
max_sum = find_max_path(tree, values)

print(f"Soma máxima do caminho: {max_sum}")
