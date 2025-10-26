
import java.io.Serializable;


/**
 * This class stores the result of one turn in the memory game.
 * It has the cards chosen, if it was a match, scores, next player, and if the game ended.
 */
public class TurnResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private CardSelection selection;
    private boolean isMatch;
    private int player1Score;
    private int player2Score;
    private int nextPlayer;
    private boolean gameFinished;

    /** Create a turn result with all details including if game finished. */
    public TurnResult(CardSelection selection, boolean isMatch, int player1Score, int player2Score, int nextPlayer, boolean gameFinished) {
        this.selection = selection;
        this.isMatch = isMatch;
        this.player1Score = player1Score;
        this.player2Score = player2Score;
        this.nextPlayer = nextPlayer;
        this.gameFinished = gameFinished;
    }

    /** Create a turn result assuming the game is not finished. */
    public TurnResult(CardSelection selection, boolean isMatch, int player1Score, int player2Score, int nextPlayer) {
        this(selection, isMatch, player1Score, player2Score, nextPlayer, false);
    }

    // Getters and setters follow

    public CardSelection getSelection() {
        return selection;
    }

    public void setSelection(CardSelection selection) {
        this.selection = selection;
    }

    public boolean isMatch() {
        return isMatch;
    }

    public void setMatch(boolean match) {
        isMatch = match;
    }

    public int getPlayer1Score() {
        return player1Score;
    }

    public void setPlayer1Score(int player1Score) {
        this.player1Score = player1Score;
    }

    public int getPlayer2Score() {
        return player2Score;
    }

    public void setPlayer2Score(int player2Score) {
        this.player2Score = player2Score;
    }

    public int getNextPlayer() {
        return nextPlayer;
    }

    public void setNextPlayer(int nextPlayer) {
        this.nextPlayer = nextPlayer;
    }

    public boolean isGameFinished() {
        return gameFinished;
    }

    public void setGameFinished(boolean gameFinished) {
        this.gameFinished = gameFinished;
    }

    /** Get score of a player by number (1 or 2). */
    public int getPlayerScore(int playerNumber) {
        return (playerNumber == 1) ? player1Score : player2Score;
    }

    /** Set score of a player by number (1 or 2). */
    public void setPlayerScore(int playerNumber, int score) {
        if (playerNumber == 1) {
            player1Score = score;
        } else if (playerNumber == 2) {
            player2Score = score;
        }
    }

    /** Returns text showing the turn result details. */
    @Override
    public String toString() {
        return String.format(
                "TurnResult{selection=%s, isMatch=%b, scores=%d-%d, nextPlayer=%d, gameFinished=%b}",
                selection, isMatch, player1Score, player2Score, nextPlayer, gameFinished);
    }
}
