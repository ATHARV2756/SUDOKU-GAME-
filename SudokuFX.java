import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.animation.PauseTransition;
import javafx.util.Duration;

import java.util.*;

public class SudokuFX extends Application {

    // Game state variables
    private int[][] solution = new int[9][9];
    private int[][] playerGrid = new int[9][9];
    private boolean[][] isInitial = new boolean[9][9];
    private TextField[][] textFields = new TextField[9][9];
    private ArrayList<int[]> history = new ArrayList<>();

    // UI Components
    private Label timeLabel, errorLabel, hintLabel;
    private Button hintButton;
    private Timeline gameTimer;
    private int secondsPlayed = 0;
    private int errorCount = 0;
    private long lastHintTime = 0;
    private static final int HINT_COOLDOWN_MS = 2000;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Sudoku FX");

        // Main container
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #667eea 0%, #764ba2 100%);");
        root.setPadding(new Insets(20));

        // Create top control panel
        HBox topPanel = createTopPanel();

        // Create the Sudoku board
        GridPane boardPanel = createBoard();

        // Create bottom status panel
        HBox bottomPanel = createBottomPanel();

        // Wrap board in a styled container
        StackPane boardContainer = new StackPane();
        boardContainer.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 20, 0, 0, 5);");
        boardContainer.setPadding(new Insets(25));
        boardContainer.getChildren().add(boardPanel);

        root.setTop(topPanel);
        root.setCenter(boardContainer);
        root.setBottom(bottomPanel);

        BorderPane.setMargin(topPanel, new Insets(0, 0, 20, 0));
        BorderPane.setMargin(boardContainer, new Insets(0, 0, 20, 0));

        Scene scene = new Scene(root, 650, 750);
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();

        startNewGame();
    }

    private HBox createTopPanel() {
        HBox topPanel = new HBox(15);
        topPanel.setAlignment(Pos.CENTER);

        Button newGameBtn = createStyledButton("New Game", "#4CAF50");
        Button undoBtn = createStyledButton("Undo", "#FF9800");
        hintButton = createStyledButton("Hint", "#2196F3");

        hintLabel = new Label("");
        hintLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        hintLabel.setTextFill(Color.WHITE);
        hintLabel.setStyle("-fx-background-color: rgba(255, 255, 255, 0.2); -fx-background-radius: 8; -fx-padding: 5 10;");
        hintLabel.setVisible(false);

        newGameBtn.setOnAction(e -> startNewGame());
        undoBtn.setOnAction(e -> undoMove());
        hintButton.setOnAction(e -> giveHint());

        topPanel.getChildren().addAll(newGameBtn, undoBtn, hintButton, hintLabel);
        return topPanel;
    }

    private Button createStyledButton(String text, String color) {
        Button btn = new Button(text);
        btn.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        btn.setTextFill(Color.WHITE);
        btn.setStyle(String.format(
                "-fx-background-color: %s; " +
                        "-fx-background-radius: 10; " +
                        "-fx-padding: 12 24; " +
                        "-fx-cursor: hand; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 5, 0, 0, 2);",
                color
        ));

        btn.setOnMouseEntered(e -> btn.setStyle(btn.getStyle() + "-fx-opacity: 0.9;"));
        btn.setOnMouseExited(e -> btn.setStyle(btn.getStyle().replace("-fx-opacity: 0.9;", "")));

        return btn;
    }

    private GridPane createBoard() {
        GridPane board = new GridPane();
        board.setAlignment(Pos.CENTER);
        board.setHgap(0);
        board.setVgap(0);

        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                StackPane cellContainer = new StackPane();

                // Create cell background with proper borders for 3x3 divisions
                Rectangle cellBg = new Rectangle(55, 55);
                cellBg.setFill(Color.WHITE);

                // Calculate border widths for clear 3x3 box separation
                double topWidth = (r % 3 == 0) ? 3 : 0.5;
                double leftWidth = (c % 3 == 0) ? 3 : 0.5;
                double bottomWidth = (r == 8) ? 3 : 0.5;
                double rightWidth = (c == 8) ? 3 : 0.5;

                // Apply borders using CSS-like styling
                cellContainer.setStyle(String.format(
                        "-fx-border-width: %fpx %fpx %fpx %fpx; " +
                                "-fx-border-color: " +
                                "%s %s %s %s;",
                        topWidth, rightWidth, bottomWidth, leftWidth,
                        (r % 3 == 0) ? "#333333" : "#DDDDDD",
                        (c == 8) ? "#333333" : ((c + 1) % 3 == 0) ? "#333333" : "#DDDDDD",
                        (r == 8) ? "#333333" : ((r + 1) % 3 == 0) ? "#333333" : "#DDDDDD",
                        (c % 3 == 0) ? "#333333" : "#DDDDDD"
                ));

                TextField tf = new TextField();
                tf.setAlignment(Pos.CENTER);
                tf.setFont(Font.font("Arial", FontWeight.BOLD, 24));
                tf.setPrefSize(55, 55);
                tf.setMaxSize(55, 55);
                tf.setStyle(
                        "-fx-background-color: transparent; " +
                                "-fx-border-color: transparent; " +
                                "-fx-text-fill: #1976D2; " +
                                "-fx-prompt-text-fill: transparent;"
                );

                // Limit input to single digit
                tf.textProperty().addListener((obs, oldVal, newVal) -> {
                    if (newVal.length() > 1) {
                        tf.setText(newVal.substring(0, 1));
                    }
                });

                final int row = r;
                final int col = c;

                tf.setOnKeyReleased(e -> handleInput(row, col));

                // Add hover effect
                cellContainer.setOnMouseEntered(e -> {
                    if (!isInitial[row][col]) {
                        cellBg.setFill(Color.rgb(240, 248, 255));
                    }
                });
                cellContainer.setOnMouseExited(e -> {
                    if (!isInitial[row][col]) {
                        cellBg.setFill(Color.WHITE);
                    }
                });

                textFields[r][c] = tf;
                cellContainer.getChildren().addAll(cellBg, tf);
                board.add(cellContainer, c, r);
            }
        }

        return board;
    }

    private HBox createBottomPanel() {
        HBox bottomPanel = new HBox(40);
        bottomPanel.setAlignment(Pos.CENTER);
        bottomPanel.setPadding(new Insets(15));
        bottomPanel.setStyle("-fx-background-color: rgba(255, 255, 255, 0.15); -fx-background-radius: 12;");

        timeLabel = createStatusLabel("⏱ Time: 00:00");
        errorLabel = createStatusLabel("❌ Mistakes: 0");

        bottomPanel.getChildren().addAll(timeLabel, errorLabel);
        return bottomPanel;
    }

    private Label createStatusLabel(String text) {
        Label label = new Label(text);
        label.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        label.setTextFill(Color.WHITE);
        label.setStyle("-fx-background-color: rgba(0, 0, 0, 0.2); -fx-background-radius: 8; -fx-padding: 8 16;");
        return label;
    }

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
                textFields[r][c].setStyle(textFields[r][c].getStyle() + "-fx-text-fill: #1976D2;");
                checkWin();
            } else {
                errorCount++;
                errorLabel.setText("❌ Mistakes: " + errorCount);
                textFields[r][c].setText("");
                playerGrid[r][c] = 0;

                // Visual feedback for wrong answer
                flashCell(r, c, Color.rgb(255, 82, 82));
                showTemporaryAlert("Invalid move!");
            }
        } catch (Exception ex) {
            textFields[r][c].setText("");
        }
    }

    private void flashCell(int row, int col, Color color) {
        String originalStyle = textFields[row][col].getStyle();
        textFields[row][col].setStyle(originalStyle + String.format("-fx-text-fill: rgb(%d, %d, %d);",
                (int)(color.getRed() * 255),
                (int)(color.getGreen() * 255),
                (int)(color.getBlue() * 255)));

        PauseTransition pause = new PauseTransition(Duration.millis(300));
        pause.setOnFinished(e -> textFields[row][col].setStyle(originalStyle));
        pause.play();
    }

    private void showTemporaryAlert(String message) {
        hintLabel.setText(message);
        hintLabel.setVisible(true);

        PauseTransition pause = new PauseTransition(Duration.seconds(2));
        pause.setOnFinished(e -> hintLabel.setVisible(false));
        pause.play();
    }

    private boolean isValid(int[][] grid, int row, int col, int num) {
        for (int i = 0; i < 9; i++) {
            if (i != col && grid[row][i] == num) return false;
            if (i != row && grid[i][col] == num) return false;
        }

        int boxRow = (row / 3) * 3;
        int boxCol = (col / 3) * 3;
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                int r = boxRow + i;
                int c = boxCol + j;
                if ((r != row || c != col) && grid[r][c] == num) return false;
            }
        }
        return true;
    }

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

    private void giveHint() {
        long currentTime = System.currentTimeMillis();
        long timeSinceLastHint = currentTime - lastHintTime;

        if (timeSinceLastHint < HINT_COOLDOWN_MS) {
            double secondsLeft = (HINT_COOLDOWN_MS - timeSinceLastHint) / 1000.0;
            showTemporaryAlert(String.format("Hint cooldown: %.1fs", secondsLeft));
            return;
        }

        // Find all empty cells
        ArrayList<int[]> emptyCells = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                if (!isInitial[i][j] && playerGrid[i][j] == 0) {
                    emptyCells.add(new int[]{i, j});
                }
            }
        }

        if (emptyCells.isEmpty()) {
            showTemporaryAlert("No empty cells!");
            return;
        }

        // Pick a random empty cell and fill it with the correct value
        Random rand = new Random();
        int[] cell = emptyCells.get(rand.nextInt(emptyCells.size()));
        int row = cell[0];
        int col = cell[1];

        history.add(new int[]{row, col, playerGrid[row][col]});
        playerGrid[row][col] = solution[row][col];
        textFields[row][col].setText(String.valueOf(solution[row][col]));
        textFields[row][col].setStyle(textFields[row][col].getStyle() + "-fx-text-fill: #4CAF50;");

        // Highlight the hint cell temporarily
        flashCell(row, col, Color.rgb(76, 175, 80));

        lastHintTime = currentTime;
        showTemporaryAlert("Hint provided!");

        checkWin();
    }

    private void startNewGame() {
        playerGrid = new int[9][9];
        solution = new int[9][9];
        history.clear();
        errorCount = 0;
        secondsPlayed = 0;
        lastHintTime = 0;
        errorLabel.setText("❌ Mistakes: 0");
        timeLabel.setText("⏱ Time: 00:00");

        if (gameTimer != null) {
            gameTimer.stop();
        }

        fillGrid(solution);

        Random rand = new Random();
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                if (rand.nextInt(100) < 35) { // 35% chance to show number
                    playerGrid[i][j] = solution[i][j];
                    isInitial[i][j] = true;
                    textFields[i][j].setText(String.valueOf(solution[i][j]));
                    textFields[i][j].setEditable(false);
                    textFields[i][j].setStyle(
                            "-fx-background-color: rgba(103, 126, 234, 0.1); " +
                                    "-fx-border-color: transparent; " +
                                    "-fx-text-fill: #333333; " +
                                    "-fx-font-weight: bold;"
                    );
                } else {
                    playerGrid[i][j] = 0;
                    isInitial[i][j] = false;
                    textFields[i][j].setText("");
                    textFields[i][j].setEditable(true);
                    textFields[i][j].setStyle(
                            "-fx-background-color: transparent; " +
                                    "-fx-border-color: transparent; " +
                                    "-fx-text-fill: #1976D2;"
                    );
                }
            }
        }

        gameTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            secondsPlayed++;
            int m = secondsPlayed / 60;
            int s = secondsPlayed % 60;
            timeLabel.setText(String.format("⏱ Time: %02d:%02d", m, s));
        }));
        gameTimer.setCycleCount(Timeline.INDEFINITE);
        gameTimer.play();
    }

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

    private void checkWin() {
        boolean full = true;
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 9; j++) {
                if (playerGrid[i][j] == 0) full = false;
            }
        }

        if (full) {
            gameTimer.stop();

            // Create victory alert
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Congratulations!");
            alert.setHeaderText("🎉 You Won! 🎉");
            alert.setContentText(String.format(
                    "Time: %02d:%02d\nMistakes: %d\n\nGreat job!",
                    secondsPlayed / 60,
                    secondsPlayed % 60,
                    errorCount
            ));

            DialogPane dialogPane = alert.getDialogPane();
            dialogPane.setStyle("-fx-background-color: white;");

            alert.showAndWait();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}