import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

public class SudokuGame extends JFrame {

    // Global variables for the game state
    private int[][] solution = new int[9][9];
    private int[][] playerGrid = new int[9][9];
    private boolean[][] isInitial = new boolean[9][9];
    private JTextField[][] textFields = new JTextField[9][9];
    private ArrayList<int[]> history = new ArrayList<>();
    private JLabel timeLabel, errorLabel;
    private javax.swing.Timer gameTimer;
    private int secondsPlayed = 0;
    private int errorCount = 0;

    public SudokuGame() {
        // Setup the basic window settings
        setTitle("My Sudoku Game");
        setSize(550, 650);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel boardPanel = new JPanel(new GridLayout(9, 9));
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                textFields[r][c] = new JTextField();
                textFields[r][c].setHorizontalAlignment(JTextField.CENTER);
                textFields[r][c].setFont(new Font("SansSerif", Font.BOLD, 18));

                // Styling the 3x3 boxes visually
                int top = (r % 3 == 0) ? 3 : 1;
                int left = (c % 3 == 0) ? 3 : 1;
                textFields[r][c].setBorder(BorderFactory.createMatteBorder(top, left, 1, 1, Color.BLACK));

                final int row = r;
                final int col = c;
                textFields[r][c].addKeyListener(new KeyAdapter() {
                    public void keyReleased(KeyEvent e) {
                        handleInput(row, col);
                    }
                });
                boardPanel.add(textFields[r][c]);
            }
        }

        // Top menu buttons
        JPanel topPanel = new JPanel();
        JButton newGameBtn = new JButton("New Game");
        JButton undoBtn = new JButton("Undo Last");
        newGameBtn.addActionListener(e -> startNewGame());
        undoBtn.addActionListener(e -> undoMove());
        topPanel.add(newGameBtn);
        topPanel.add(undoBtn);

        // Bottom status bar
        JPanel bottomPanel = new JPanel(new GridLayout(1, 2));
        timeLabel = new JLabel("Time: 00:00");
        errorLabel = new JLabel("Mistakes: 0");
        bottomPanel.add(timeLabel);
        bottomPanel.add(errorLabel);

        add(topPanel, BorderLayout.NORTH);
        add(boardPanel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        // Simple timer logic
        gameTimer = new javax.swing.Timer(1000, e -> {
            secondsPlayed++;
            int m = secondsPlayed / 60;
            int s = secondsPlayed % 60;
            timeLabel.setText(String.format("Time: %02d:%02d", m, s));
        });

        startNewGame();
    }

    // Logic to handle user typing into the cells
    private void handleInput(int r, int c) {
        if (isInitial[r][c]) return;

        String text = textFields[r][c].getText();
        if (text.equals("")) {
            playerGrid[r][c] = 0;
            return;
        }

        try {
            int val = Integer.parseInt(text);
            if (val < 1 || val > 9) throw new Exception();

            if (isValid(playerGrid, r, c, val)) {
                history.add(new int[]{r, c, playerGrid[r][c]});
                playerGrid[r][c] = val;
                textFields[r][c].setForeground(Color.BLACK);
                checkWin();
            } else {
                errorCount++;
                errorLabel.setText("Mistakes: " + errorCount);
                textFields[r][c].setText("");
                playerGrid[r][c] = 0;
                JOptionPane.showMessageDialog(this, "That number doesn't fit there!");
            }
        } catch (Exception ex) {
            textFields[r][c].setText("");
        }
    }

    // Basic Sudoku rule checking
    private boolean isValid(int[][] grid, int row, int col, int num) {
        for (int i = 0; i < 9; i++) {
            if (grid[row][i] == num || grid[i][col] == num) return false;
        }
        int boxRow = (row / 3) * 3;
        int boxCol = (col / 3) * 3;
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (grid[boxRow + i][boxCol + j] == num) return false;
            }
        }
        return true;
    }

    // Reverting the last move stored in the list
    private void undoMove() {
        if (history.size() > 0) {
            int[] last = history.remove(history.size() - 1);
            int r = last[0];
            int c = last[1];
            int oldVal = last[2];
            playerGrid[r][c] = oldVal;
            textFields[r][c].setText(oldVal == 0 ? "" : String.valueOf(oldVal));
        }
    }

    // Simple puzzle generation by filling and then hiding cells
    private void startNewGame() {
        playerGrid = new int[9][9];
        solution = new int[9][9];
        history.clear();
        errorCount = 0;
        secondsPlayed = 0;
        errorLabel.setText("Mistakes: 0");

        fillGrid(solution);

        Random rand = new Random();
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                if (rand.nextInt(100) < 40) { // 40% chance to show number
                    playerGrid[i][j] = solution[i][j];
                    isInitial[i][j] = true;
                    textFields[i][j].setText(String.valueOf(solution[i][j]));
                    textFields[i][j].setEditable(false);
                    textFields[i][j].setBackground(new Color(235, 235, 235));
                } else {
                    playerGrid[i][j] = 0;
                    isInitial[i][j] = false;
                    textFields[i][j].setText("");
                    textFields[i][j].setEditable(true);
                    textFields[i][j].setBackground(Color.WHITE);
                }
            }
        }
        gameTimer.start();
    }

    // Recursive helper to create a valid board
    private boolean fillGrid(int[][] grid) {
        for (int row = 0; row < 9; row++) {
            for (int col = 0; col < 9; col++) {
                if (grid[row][col] == 0) {
                    Integer[] nums = {1, 2, 3, 4, 5, 6, 7, 8, 9};
                    Collections.shuffle(Arrays.asList(nums));
                    for (int n : nums) {
                        if (isValid(grid, row, col, n)) {
                            grid[row][col] = n;
                            if (fillGrid(grid)) return true;
                            grid[row][col] = 0;
                        }
                    }
                    return false;
                }
            }
        }
        return true;
    }

    // Checking if the board is full to end the game
    private void checkWin() {
        boolean full = true;
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                if (playerGrid[i][j] == 0) full = false;
            }
        }
        if (full) {
            gameTimer.stop();
            JOptionPane.showMessageDialog(this, "You Won!");
        }
    }

    // Standard main method to launch the app
    public static void main(String[] args) {
        new SudokuGame().setVisible(true);
    }
}