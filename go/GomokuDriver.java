package go;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

public class GomokuDriver {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
            catch (Exception ignored) {}
            new IntroFrame().setVisible(true);
        });
    }
}


@SuppressWarnings("serial")
class IntroFrame extends JFrame {
    private JComboBox<String> sizeCombo;
    private JComboBox<String> difficultyCombo;
    private JComboBox<String> firstCombo;
    private JButton startBtn;

    public IntroFrame() {
        setTitle("Welcome to Gomoku");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(500, 400);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(15,15));
        getContentPane().setBackground(new Color(30, 87, 153));
        initComponents();
    }

    private void initComponents() {
        JLabel title = new JLabel("GOMOKU");
        title.setHorizontalAlignment(SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 36));
        title.setForeground(Color.WHITE);
        add(title, BorderLayout.NORTH);

        JPanel middle = new JPanel(new BorderLayout(10,10));
        middle.setBackground(new Color(30, 87, 153));
        middle.setBorder(new EmptyBorder(0,20,0,20));

        JTextArea instructions = new JTextArea(
            "Instructions:\n" +
            "Players alternate placing stones to get five in a row.\n" +
            "Black always moves first.\n" +
            "Select board size, difficulty, and who starts.\n"
        );
        instructions.setEditable(false);
        instructions.setLineWrap(true);
        instructions.setWrapStyleWord(true);
        instructions.setFont(new Font("SansSerif", Font.PLAIN, 14));
        instructions.setBackground(Color.WHITE);
        instructions.setForeground(Color.DARK_GRAY);
        instructions.setBorder(new EmptyBorder(10,10,10,10));
        middle.add(instructions, BorderLayout.NORTH);

        JPanel settings = new JPanel(new GridLayout(3, 2, 10, 10));
        settings.setBackground(new Color(30, 87, 153));
        settings.setBorder(new EmptyBorder(10,40,10,40));

        JLabel lblSize = new JLabel("Board Size:", SwingConstants.RIGHT);
        styleLabel(lblSize);
        settings.add(lblSize);
        sizeCombo = new JComboBox<>(new String[]{"15 × 15", "19 × 19"});
        styleCombo(sizeCombo);
        settings.add(sizeCombo);

        JLabel lblDiff = new JLabel("Difficulty:", SwingConstants.RIGHT);
        styleLabel(lblDiff);
        settings.add(lblDiff);
        difficultyCombo = new JComboBox<>(new String[]{"Normal", "Hard", "Impossible"});
        styleCombo(difficultyCombo);
        settings.add(difficultyCombo);

        JLabel lblFirst = new JLabel("First Move:", SwingConstants.RIGHT);
        styleLabel(lblFirst);
        settings.add(lblFirst);
        firstCombo = new JComboBox<>(new String[]{"AI", "Player"});
        styleCombo(firstCombo);
        settings.add(firstCombo);

        middle.add(settings, BorderLayout.CENTER);
        add(middle, BorderLayout.CENTER);

        startBtn = new JButton("Start Game");
        startBtn.setFont(new Font("SansSerif", Font.BOLD, 18));
        startBtn.setBackground(new Color(0, 51, 102));
        // Change text to BLACK for higher contrast against the dark‐blue background
        startBtn.setForeground(Color.BLACK);
        startBtn.setFocusPainted(false);
        startBtn.setPreferredSize(new Dimension(200, 50));
        startBtn.addActionListener(e -> launchGame());

        JPanel bottom = new JPanel();
        bottom.setBackground(new Color(30, 87, 153));
        bottom.add(startBtn);
        add(bottom, BorderLayout.SOUTH);
    }

    private void styleLabel(JLabel lbl) {
        lbl.setForeground(Color.WHITE);
        lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, 14f));
    }

    private void styleCombo(JComboBox<String> combo) {
        combo.setBackground(Color.WHITE);
        combo.setForeground(Color.BLACK);
        combo.setFont(combo.getFont().deriveFont(14f));
        combo.setOpaque(true);
    }

    private void launchGame() {
        int size = sizeCombo.getSelectedIndex() == 1 ? 19 : 15;
        int difficulty = difficultyCombo.getSelectedIndex();
        boolean aiFirst = firstCombo.getSelectedIndex() == 0;
        dispose();
        new GomokuUI(size, difficulty, aiFirst).setVisible(true);
    }
}


@SuppressWarnings("serial")
class GomokuUI extends JFrame {
    private Board board;
    private Minimax ai;
    private BoardPanel boardPanel;
    private JButton restartBtn;
    private JButton hintBtn;
    private JLabel statusLabel;
    private JProgressBar aiProgress;
    private final int difficulty;
    private final boolean aiFirst;
    private final boolean aiIsWhite;

    public GomokuUI(int size, int difficulty, boolean aiFirst) {
        this.difficulty = difficulty;
        this.aiFirst = aiFirst;
        this.aiIsWhite = !aiFirst; // AI is White if Player goes first

        setTitle("Gomoku");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(700, 800);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10,10));
        initMenu();
        initComponents();
        startNewGame(size);
    }

    private void initMenu() {
        JMenuBar menuBar = new JMenuBar();
        JMenu gameMenu = new JMenu("Game");
        JMenuItem newItem = new JMenuItem("New Intro");
        newItem.addActionListener(e -> {
            dispose();
            new IntroFrame().setVisible(true);
        });
        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> System.exit(0));
        gameMenu.add(newItem);
        gameMenu.addSeparator();
        gameMenu.add(exitItem);
        menuBar.add(gameMenu);
        setJMenuBar(menuBar);
    }

    private void initComponents() {
        // Top panel: Restart, Hint, and AI spinner
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        topPanel.setBackground(new Color(240, 248, 255));

        restartBtn = new JButton("Restart");
        styleActionButton(restartBtn);
        restartBtn.addActionListener(e -> {
            dispose();
            new IntroFrame().setVisible(true);
        });
        topPanel.add(restartBtn);

        hintBtn = new JButton("Hint");
        styleActionButton(hintBtn);
        hintBtn.addActionListener(e -> showHint());
        topPanel.add(hintBtn);

        aiProgress = new JProgressBar();
        aiProgress.setIndeterminate(true);
        aiProgress.setVisible(false);
        aiProgress.setPreferredSize(new Dimension(100, 20));
        topPanel.add(aiProgress);

        add(topPanel, BorderLayout.NORTH);

        boardPanel = new BoardPanel();
        boardPanel.setBorder(new EmptyBorder(0,20,0,20));
        add(boardPanel, BorderLayout.CENTER);

        statusLabel = new JLabel(" ");
        statusLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        statusLabel.setBorder(new EmptyBorder(5,10,5,10));
        add(statusLabel, BorderLayout.SOUTH);
    }

    private void styleActionButton(JButton b) {
        b.setFont(new Font("SansSerif", Font.BOLD, 16));
        b.setBackground(new Color(0, 51, 102));
        // Make the text BLACK for stronger contrast against the dark‐blue button
        b.setForeground(Color.BLACK);
        b.setFocusPainted(false);
        b.setPreferredSize(new Dimension(100, 40));
    }

    private void startNewGame(int size) {
        board = new Board(size);
        ai = new Minimax(board, aiIsWhite, difficulty);
        boardPanel.resetHint();
        boardPanel.setBoard(board);

        final int center = size / 2;

        if (aiFirst) {
            // AI (Black) places opening move at center
            board.addStone(center, center, false);
            boardPanel.repaint();
            statusLabel.setText("Your (White) turn");
        } else {
            // Player (Black) goes first—wait for click
            statusLabel.setText("Your (Black) turn");
        }

        boardPanel.repaint();
    }

    void handleClick(int x, int y) {
        if (board.checkWinLast()) return;
        int cell = boardPanel.pixelToCell(x, y);
        if (cell < 0) return;
        int row = cell / board.getSize(), col = cell % board.getSize();

        // Determine human color: if aiIsWhite, human is Black (white=false); else human is White
        boolean humanIsWhite = !aiIsWhite;
        if (!board.addStone(col, row, humanIsWhite)) return;

        boardPanel.resetHint();
        boardPanel.repaint();

        // Check for human win
        if (board.checkWin(col, row, humanIsWhite)) {
            statusLabel.setText("You win!");
            return;
        }

        // Now AI’s turn
        statusLabel.setText("AI is thinking...");
        aiProgress.setVisible(true);

        SwingUtilities.invokeLater(this::aiMove);
    }

    private void aiMove() {
        int depth;
        if (difficulty == 0) depth = 3;
        else if (difficulty == 1) depth = 4;
        else /* Impossible */ depth = 5;

        int[] move = ai.calculateNextMove(depth);
        if (move != null) {
            // move[0] = row, move[1] = col; AI color = aiIsWhite
            board.addStone(move[1], move[0], aiIsWhite);
        }
        boardPanel.repaint();

        aiProgress.setVisible(false);

        // Check for AI win
        if (board.checkWinLast()) {
            statusLabel.setText("AI wins!");
        } else {
            // Back to human turn
            if (aiIsWhite) {
                statusLabel.setText("Your (Black) turn");
            } else {
                statusLabel.setText("Your (White) turn");
            }
        }
    }

    private void showHint() {
        if (board.checkWinLast()) return;
        aiProgress.setVisible(true);
        SwingUtilities.invokeLater(() -> {
            int[] hint = ai.calculateNextMove(3);
            if (hint != null) {
                int row = hint[0], col = hint[1];
                if (board.getBoardMatrix()[row][col] != 0) {
                    List<int[]> mvs = board.generateMoves();
                    if (!mvs.isEmpty()) { row = mvs.get(0)[0]; col = mvs.get(0)[1]; }
                }
                boardPanel.setHintCell(row, col);
            }
            boardPanel.repaint();
            aiProgress.setVisible(false);
        });
    }

    class BoardPanel extends JPanel {
        private Board board;
        private final Color gridColor = new Color(105, 105, 105);
        private int hintRow = -1, hintCol = -1;

        BoardPanel() {
            setBackground(new Color(255, 248, 220));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    GomokuUI.this.handleClick(e.getX(), e.getY());
                }
            });
        }

        void setBoard(Board b) { board = b; repaint(); }
        void setHintCell(int row, int col) { hintRow = row; hintCol = col; }
        void resetHint() { hintRow = hintCol = -1; }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (board == null) return;
            int n = board.getSize();
            int w = getWidth(), h = getHeight();
            int cellSize = Math.min(w, h) / n;
            Graphics2D g2 = (Graphics2D) g;
            g2.setStroke(new BasicStroke(2));
            g2.setColor(gridColor);
            for (int i = 0; i <= n; i++) {
                g2.drawLine(10, 10 + i*cellSize, 10 + n*cellSize, 10 + i*cellSize);
                g2.drawLine(10 + i*cellSize, 10, 10 + i*cellSize, 10 + n*cellSize);
            }
            if (hintRow >= 0) {
                g2.setColor(new Color(255, 215, 0, 128));
                g2.fillRect(10 + hintCol*cellSize, 10 + hintRow*cellSize, cellSize, cellSize);
            }
            for (int r = 0; r < n; r++) {
                for (int c = 0; c < n; c++) {
                    int val = board.getBoardMatrix()[r][c];
                    if (val != 0) {
                        g2.setColor(val == 2 ? Color.BLACK : Color.WHITE);
                        Point p = cellToPixel(r, c);
                        int d = cellSize - 4;
                        g2.fillOval(p.x - d/2, p.y - d/2, d, d);
                        g2.setColor(Color.DARK_GRAY);
                        g2.drawOval(p.x - d/2, p.y - d/2, d, d);
                    }
                }
            }
        }

        int pixelToCell(int x, int y) {
            int n = board.getSize();
            int cs = Math.min(getWidth(), getHeight()) / n;
            int cx = (x - 10) / cs;
            int cy = (y - 10) / cs;
            return (cx >= 0 && cy >= 0 && cx < n && cy < n) ? cy*n + cx : -1;
        }

        Point cellToPixel(int row, int col) {
            int n = board.getSize();
            int cs = Math.min(getWidth(), getHeight()) / n;
            return new Point(10 + col*cs + cs/2, 10 + row*cs + cs/2);
        }
    }
}

/**
 * Manages board state and win logic
 */
class Board {
    private int size;
    private int[][] matrix;

    // initialize to “no move yet”
    private int lastX = -1, lastY = -1;

    public Board(int size) {
        this.size = size;
        matrix = new int[size][size];
    }

    public Board(Board other) {
        this.size = other.size;
        this.matrix = new int[size][size];
        for (int i = 0; i < size; i++)
            System.arraycopy(other.matrix[i], 0, this.matrix[i], 0, size);
        // copy lastX/lastY as well
        this.lastX = other.lastX;
        this.lastY = other.lastY;
    }

    public int getSize() { return size; }
    public int[][] getBoardMatrix() { return matrix; }

    public boolean addStone(int x, int y, boolean white) {
        if (x < 0 || y < 0 || x >= size || y >= size || matrix[y][x] != 0) return false;
        matrix[y][x] = white ? 1 : 2;
        lastX = x;
        lastY = y;
        return true;
    }

    public boolean checkWinLast() {
        if (lastX < 0 || lastY < 0) return false;
        int t = matrix[lastY][lastX];
        return Check.win(matrix, lastX, lastY, t);
    }

    public boolean checkWin(int x, int y, boolean white) {
        return Check.win(matrix, x, y, white ? 1 : 2);
    }

    public List<int[]> generateMoves() { return Check.generateMoves(matrix); }
    public void thinkingStarted() {}
    public void thinkingFinished() {}
    public void addStoneNoGUI(int x, int y, boolean black) { addStone(x, y, !black); }
    public void removeStoneNoGUI(int x, int y) { matrix[y][x] = 0; }
}

/**
 * Utility: win detection & move generation
 */
class Check {
    public static boolean win(int[][] m, int x, int y, int t) {
        int[][] dirs = {{1,0},{0,1},{1,1},{1,-1}};
        for (int[] d : dirs) {
            int cnt = 1 + count(m, x, y, d[0], d[1], t)
                       + count(m, x, y, -d[0], -d[1], t);
            if (cnt >= 5) return true;
        }
        return false;
    }
    private static int count(int[][] m, int x, int y, int dx, int dy, int t) {
        int c = 0;
        for (int i = 1; i < 5; i++) {
            int nx = x + dx*i, ny = y + dy*i;
            if (nx<0||ny<0||nx>=m.length||ny>=m.length||m[ny][nx]!=t) break;
            c++;
        }
        return c;
    }
    public static java.util.ArrayList<int[]> generateMoves(int[][] m) {
        java.util.ArrayList<int[]> moves = new java.util.ArrayList<>();
        int n = m.length;
        boolean[][] seen = new boolean[n][n];
        for (int r = 0; r < n; r++) for (int c = 0; c < n; c++) if (m[r][c] != 0) {
            for (int dr = -1; dr <= 1; dr++) for (int dc = -1; dc <= 1; dc++) {
                int nr = r+dr, nc = c+dc;
                if (nr>=0&&nc>=0&&nr<n&&nc<n&&m[nr][nc]==0&&!seen[nr][nc]){
                    seen[nr][nc]=true; moves.add(new int[]{nr,nc});
                }
            }
        }
        if (moves.isEmpty()) moves.add(new int[]{n/2,n/2});
        return moves;
    }
}
