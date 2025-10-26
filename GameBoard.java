
import java.io.Serializable;
import java.util.*;

/**
 * This class represents the board of a memory game.
 * It keeps track of the cards, their values, and their states (revealed or matched).
 */
public class GameBoard implements Serializable {
    private static final long serialVersionUID = 1L;
    private int rows;
    private int cols;
    private int[][] board;           // card values
    private boolean[][] revealed;    // temporarily revealed cards
    private boolean[][] matched;     // permanently matched cards

    /** Creates the game board and fills it with shuffled pairs. */
    public GameBoard(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
        this.board = new int[rows][cols];
        this.revealed = new boolean[rows][cols];
        this.matched = new boolean[rows][cols];
        initializeBoard();
    }

    /** Prepares the board with shuffled matching pairs. */
    private void initializeBoard() {
        List<Integer> numbers = new ArrayList<>();
        int totalPairs = (rows * cols) / 2;

        for (int i = 1; i <= totalPairs; i++) {
            numbers.add(i);
            numbers.add(i);
        }

        Collections.shuffle(numbers);

        int index = 0;
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                board[row][col] = numbers.get(index++);
            }
        }
    }

    /** Makes a card at this position temporarily visible. */
    public void revealCard(int row, int col) {
        if (isValidPosition(row, col)) {
            revealed[row][col] = true;
        }
    }

    /** Hides a revealed card again. */
    public void hideCard(int row, int col) {
        if (isValidPosition(row, col)) {
            revealed[row][col] = false;
        }
    }

    /** Marks the card as matched and keeps it revealed. */
    public void markAsMatched(int row, int col) {
        if (isValidPosition(row, col)) {
            matched[row][col] = true;
            revealed[row][col] = true;
        }
    }

    /** Checks if two cards have the same value. */
    public boolean isMatch(int row1, int col1, int row2, int col2) {
        if (!isValidPosition(row1, col1) || !isValidPosition(row2, col2)) return false;
        if (row1 == row2 && col1 == col2) return false;
        if (matched[row1][col1] || matched[row2][col2]) return false;

        return board[row1][col1] == board[row2][col2];
    }

    /** Returns true if the card is not revealed or matched. */
    public boolean canSelectCard(int row, int col) {
        if (!isValidPosition(row, col)) return false;
        if (matched[row][col]) return false;
        if (revealed[row][col]) return false;
        return true;
    }

    /** Checks if all cards on the board are matched. */
    public boolean isGameFinished() {
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                if (!matched[row][col]) return false;
            }
        }
        return true;
    }

    /** Checks if the card is inside the board range. */
    private boolean isValidPosition(int row, int col) {
        return row >= 0 && row < rows && col >= 0 && col < cols;
    }

    /** Hides all revealed cards that are not matched. */
    public void resetRevealedCards() {
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                if (!matched[row][col]) {
                    revealed[row][col] = false;
                }
            }
        }
    }

    // -------- Getters --------

    /** Returns number of rows. */
    public int getRows() {
        return rows;
    }

    /** Returns number of columns. */
    public int getCols() {
        return cols;
    }

    /** Returns the value of the card in this position. */
    public int getCardValue(int row, int col) {
        if (!isValidPosition(row, col)) return -1;
        return board[row][col];
    }

    /** Returns true if the card is revealed. */
    public boolean isRevealed(int row, int col) {
        if (!isValidPosition(row, col)) return false;
        return revealed[row][col];
    }

    /** Returns true if the card is already matched. */
    public boolean isMatched(int row, int col) {
        if (!isValidPosition(row, col)) return false;
        return matched[row][col];
    }

    /** Checks if a card is available to be picked. */
    public boolean isCardAvailable(int row, int col) {
        return canSelectCard(row, col);
    }
}
