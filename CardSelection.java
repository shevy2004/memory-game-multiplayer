
import java.io.Serializable;

/**
 * This class stores the positions of two cards selected by a player and the player's number.
 * Used to pass the selection info in the memory game.
 */
public class CardSelection implements Serializable {
    private static final long serialVersionUID = 1L;

    private int row1, col1;
    private int row2, col2;
    private int playerNumber;

    /**Creates a card selection with two card positions and a player number.*/
    public CardSelection(int row1, int col1, int row2, int col2, int playerNumber) {
        this.row1 = row1;
        this.col1 = col1;
        this.row2 = row2;
        this.col2 = col2;
        this.playerNumber = playerNumber;
    }

    /** Returns the row of the first card. */
    public int getRow1() { return row1; }

    /** Sets the row of the first card. */
    public void setRow1(int row1) { this.row1 = row1; }

    /** Returns the column of the first card. */
    public int getCol1() { return col1; }

    /** Sets the column of the first card. */
    public void setCol1(int col1) { this.col1 = col1; }

    /** Returns the row of the second card. */
    public int getRow2() { return row2; }

    /** Sets the row of the second card. */
    public void setRow2(int row2) { this.row2 = row2; }

    /** Returns the column of the second card. */
    public int getCol2() { return col2; }

    /** Sets the column of the second card. */
    public void setCol2(int col2) { this.col2 = col2; }

    /** Returns the player number. */
    public int getPlayerNumber() { return playerNumber; }

    /** Sets the player number. */
    public void setPlayerNumber(int playerNumber) { this.playerNumber = playerNumber; }
}
