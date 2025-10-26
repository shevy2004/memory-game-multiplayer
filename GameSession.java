
import java.io.*;
import java.net.Socket;

/** A session that handles the game between two players. */
public class GameSession implements Runnable {
    private Socket player1Socket;
    private Socket player2Socket;
    private ObjectInputStream player1Input;
    private ObjectOutputStream player1Output;
    private ObjectInputStream player2Input;
    private ObjectOutputStream player2Output;

    private GameBoard gameBoard;
    private int currentPlayer;
    private int player1Score;
    private int player2Score;
    private boolean gameActive;

    /** Sets up the game session, board, and connection with both players. */
    public GameSession(Socket player1, Socket player2, int rows, int cols) {
        this.player1Socket = player1;
        this.player2Socket = player2;
        this.gameBoard = new GameBoard(rows, cols);
        this.currentPlayer = 1;
        this.player1Score = 0;
        this.player2Score = 0;
        this.gameActive = true;

        try {
            player1Output = new ObjectOutputStream(player1.getOutputStream());
            player1Output.flush();
            player1Input = new ObjectInputStream(player1.getInputStream());

            player2Output = new ObjectOutputStream(player2.getOutputStream());
            player2Output.flush();
            player2Input = new ObjectInputStream(player2.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Starts the game and runs threads for each player. */
    @Override
    public void run() {
        try {
            startGame();
            Thread player1Thread = new Thread(() -> handlePlayer(1));
            Thread player2Thread = new Thread(() -> handlePlayer(2));
            player1Thread.start();
            player2Thread.start();
            player1Thread.join();
            player2Thread.join();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeConnections();
        }
    }

    /** Sends game start messages and board to both players. */
    private void startGame() {
        try {
            GameMessage startMsg1 = new GameMessage(GameMessage.MessageType.GAME_START);
            startMsg1.setPlayerNumber(1);
            player1Output.writeObject(startMsg1);
            player1Output.flush();

            GameMessage startMsg2 = new GameMessage(GameMessage.MessageType.GAME_START);
            startMsg2.setPlayerNumber(2);
            player2Output.writeObject(startMsg2);
            player2Output.flush();

            GameMessage boardMsg = new GameMessage(GameMessage.MessageType.BOARD_DATA, gameBoard);
            player1Output.writeObject(boardMsg);
            player1Output.flush();
            player2Output.writeObject(boardMsg);
            player2Output.flush();

            sendPlayerTurnMessage();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Handles messages from the player (card selection). */
    private void handlePlayer(int playerNumber) {
        ObjectInputStream input = (playerNumber == 1) ? player1Input : player2Input;
        try {
            while (gameActive) {
                Object obj = input.readObject();
                if (obj instanceof CardSelection) {
                    CardSelection selection = (CardSelection) obj;
                    if (selection.getPlayerNumber() == currentPlayer) {
                        processCardSelection(selection);
                    }
                }
            }
        } catch (Exception e) {
            if (gameActive) {
                e.printStackTrace();
            }
        }
    }

    /** Handles a player's turn, updates board and scores, and sends results. */
    private synchronized void processCardSelection(CardSelection selection) {
        try {
            int row1 = selection.getRow1();
            int col1 = selection.getCol1();
            int row2 = selection.getRow2();
            int col2 = selection.getCol2();

            gameBoard.revealCard(row1, col1);
            gameBoard.revealCard(row2, col2);

            boolean isMatch = gameBoard.isMatch(row1, col1, row2, col2);
            TurnResult result = new TurnResult(selection, isMatch, player1Score, player2Score, currentPlayer, gameBoard.isGameFinished());

            if (isMatch) {
                gameBoard.markAsMatched(row1, col1);
                gameBoard.markAsMatched(row2, col2);
                if (currentPlayer == 1) {
                    player1Score++;
                } else {
                    player2Score++;
                }
                result.setPlayer1Score(player1Score);
                result.setPlayer2Score(player2Score);
                result.setNextPlayer(currentPlayer);
            } else {
                currentPlayer = (currentPlayer == 1) ? 2 : 1;
                result.setNextPlayer(currentPlayer);
            }

            result.setGameFinished(gameBoard.isGameFinished());

            GameMessage resultMsg = new GameMessage(GameMessage.MessageType.TURN_RESULT, result);
            player1Output.writeObject(resultMsg);
            player1Output.flush();
            player2Output.writeObject(resultMsg);
            player2Output.flush();

            if (gameBoard.isGameFinished()) {
                gameActive = false;
                sendGameEndMessage();
            } else if (!isMatch) {
                Thread.sleep(2000);
                gameBoard.hideCard(row1, col1);
                gameBoard.hideCard(row2, col2);
                sendPlayerTurnMessage();
            } else {
                sendPlayerTurnMessage();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Sends message to show whose turn it is. */
    private void sendPlayerTurnMessage() {
        try {
            GameMessage turnMsg = new GameMessage(GameMessage.MessageType.PLAYER_TURN);
            turnMsg.setPlayerNumber(currentPlayer);
            player1Output.writeObject(turnMsg);
            player1Output.flush();
            player2Output.writeObject(turnMsg);
            player2Output.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Sends the final game result to both players. */
    private void sendGameEndMessage() {
        try {
            String endMessage;
            if (player1Score > player2Score) {
                endMessage = "שחקן 1 ניצח!";
            } else if (player2Score > player1Score) {
                endMessage = "שחקן 2 ניצח!";
            } else {
                endMessage = "תיקו!";
            }

            GameMessage endMsg = new GameMessage(GameMessage.MessageType.GAME_END, endMessage);
            player1Output.writeObject(endMsg);
            player1Output.flush();
            player2Output.writeObject(endMsg);
            player2Output.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Closes all sockets and streams of both players. */
    private void closeConnections() {
        try {
            if (player1Input != null) player1Input.close();
            if (player1Output != null) player1Output.close();
            if (player1Socket != null) player1Socket.close();

            if (player2Input != null) player2Input.close();
            if (player2Output != null) player2Output.close();
            if (player2Socket != null) player2Socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
