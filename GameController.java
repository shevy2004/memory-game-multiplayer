
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** Controls the game screen, connects to the server, and manages game logic. */
public class GameController implements Initializable {

    // === UI Elements ===
    @FXML private Label titleLabel;
    @FXML private Label statusLabel;
    @FXML private Label scoreLabel;
    @FXML private Label playerLabel;
    @FXML private GridPane gameGrid;
    @FXML private Button newGameButton;
    @FXML private Button disconnectButton;

    // === Game State ===
    private boolean isMyTurn = false;
    private int selectedCards = 0;
    private int selectedRow1, selectedCol1, selectedRow2, selectedCol2;
    private int myScore = 0;
    private int opponentScore = 0;
    private int playerNumber;
    private boolean gameActive = true;
    private boolean waitingForCardsToClose = false;

    // === Connection ===
    private String serverHost = "localhost";
    private int serverPort = 8080;
    private Socket socket;
    private ObjectOutputStream output;
    private ObjectInputStream input;

    // === Other ===
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private Stage primaryStage;
    private GameBoard gameBoard;
    private Button[][] cardButtons;

    // === Images ===
    private Image[] cardImages;
    private Image cardBackImage;

    // === Responsive Design Variables ===
    private int cardSize = 100;
    private int imageSize = 80;
    private int fontSize = 18;

    /** Called automatically when the controller loads. Sets initial labels. */
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        statusLabel.setText("ממתין לחיבור לשרת...");
        scoreLabel.setText("הניקוד שלי: 0 | היריב: 0");
        playerLabel.setText("שחקן: -");
        loadImages();
    }

    /** Loads all the card images from the images folder. */
    private void loadImages() {
        try {
            cardImages = new Image[40];
            int loadedImages = 0;

            for (int i = 1; i <= 40; i++) {
                try {
                    String[] possiblePaths = {
                            "file:src/images/img" + i + ".jpg",
                            "file:src/images/img" + i + ".png",
                            "file:images/img" + i + ".jpg",
                            "file:images/img" + i + ".png",
                            "/images/img" + i + ".jpg",
                            "/images/img" + i + ".png"
                    };

                    Image image = null;
                    for (String path : possiblePaths) {
                        try {
                            if (path.startsWith("file:")) {
                                image = new Image(path);
                            } else {
                                image = new Image(getClass().getResourceAsStream(path));
                            }

                            if (image != null && !image.isError()) {
                                cardImages[i-1] = image;
                                loadedImages++;
                                break;
                            }
                        } catch (Exception ignored) {
                        }
                    }
                } catch (Exception e) {
                }
            }

            if (loadedImages == 0) {
                cardImages = null;
            }

        } catch (Exception e) {
            cardImages = null;
        }
    }

    /** Calculates card and image sizes based on board size. */
    private void calculateResponsiveSizes(int rows, int cols) {
        int maxBoardWidth = 700;
        int maxBoardHeight = 500;

        int maxCardWidth = maxBoardWidth / cols - 5;
        int maxCardHeight = maxBoardHeight / rows - 5;

        cardSize = Math.min(maxCardWidth, maxCardHeight);
        cardSize = Math.max(50, Math.min(cardSize, 120));

        imageSize = (int)(cardSize * 0.8);

        if (cardSize >= 100) {
            fontSize = 18;
        } else if (cardSize >= 80) {
            fontSize = 14;
        } else {
            fontSize = 12;
        }

        adjustWindowSize(rows, cols);
    }

    /** Changes the window size to fit the board. */
    private void adjustWindowSize(int rows, int cols) {
        if (primaryStage != null) {
            int windowWidth = cols * (cardSize + 5) + 100;
            int windowHeight = rows * (cardSize + 5) + 250;

            windowWidth = Math.max(600, Math.min(windowWidth, 1200));
            windowHeight = Math.max(500, Math.min(windowHeight, 900));

            primaryStage.setWidth(windowWidth);
            primaryStage.setHeight(windowHeight);
        }
    }

    /** Sets the server host and port for connecting later. */
    public void setConnectionParameters(String host, int port) {
        this.serverHost = host;
        this.serverPort = port;
    }

    /** Sets the main window and adjusts its size. */
    public void setPrimaryStage(Stage stage) {
        this.primaryStage = stage;
        stage.setMinWidth(500);
        stage.setMinHeight(400);
    }

    /** Connects to the server in a background thread. */
    public void connectToServer() {
        Thread connectionThread = new Thread(() -> {
            try {
                socket = new Socket(serverHost, serverPort);
                output = new ObjectOutputStream(socket.getOutputStream());
                output.flush();
                input = new ObjectInputStream(socket.getInputStream());

                Platform.runLater(() -> {
                    statusLabel.setText("מחובר לשרת! ממתין לשחקן נוסף...");
                });

                startMessageListener();

            } catch (IOException e) {
                Platform.runLater(() -> statusLabel.setText("שגיאה בחיבור: " + e.getMessage()));
            }
        });
        connectionThread.setDaemon(true);
        connectionThread.start();
    }

    /** Starts a thread to listen for messages from the server. */
    private void startMessageListener() {
        Thread listenerThread = new Thread(() -> {
            try {
                while (gameActive && socket != null && !socket.isClosed()) {
                    Object message = input.readObject();
                    if (message instanceof GameMessage) {
                        Platform.runLater(() -> handleServerMessage((GameMessage) message));
                    }
                }
            } catch (Exception e) {
                if (gameActive) {
                    Platform.runLater(() -> statusLabel.setText("החיבור לשרת נותק"));
                }
            }
        });
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    /** Handles messages received from the server. */
    private void handleServerMessage(GameMessage message) {
        switch (message.getType()) {
            case GAME_START:
                handleGameStart(message);
                break;
            case BOARD_DATA:
                handleBoardData(message);
                break;
            case PLAYER_TURN:
                handlePlayerTurn(message);
                break;
            case TURN_RESULT:
                handleTurnResult(message);
                break;
            case GAME_END:
                handleGameEnd(message);
                break;
        }
    }

    /** Handles starting the game and shows player number. */
    private void handleGameStart(GameMessage message) {
        playerNumber = message.getPlayerNumber();
        playerLabel.setText("שחקן: " + playerNumber);
        statusLabel.setText(message.getMessage());
        gameActive = true;
    }

    /** Creates the game board using data sent by the server. */
    private void handleBoardData(GameMessage message) {
        gameBoard = (GameBoard) message.getData();
        createGameBoard();
    }

    /** Updates the UI when it's this player's turn. */
    private void handlePlayerTurn(GameMessage message) {
        isMyTurn = (message.getPlayerNumber() == playerNumber);
        statusLabel.setText(isMyTurn && !waitingForCardsToClose ? "התור שלך! בחר שני קלפים." : "תור היריב, המתן...");
    }

    /** Handles the result of a turn and updates the board and scores. */
    private void handleTurnResult(GameMessage message) {
        TurnResult result = (TurnResult) message.getData();
        CardSelection selection = result.getSelection();

        updateBoardDisplay(selection, result.isMatch());

        if (playerNumber == 1) {
            myScore = result.getPlayer1Score();
            opponentScore = result.getPlayer2Score();
        } else {
            myScore = result.getPlayer2Score();
            opponentScore = result.getPlayer1Score();
        }

        scoreLabel.setText("הניקוד שלי: " + myScore + " | היריב: " + opponentScore);
        resetCardSelection();
        String statusMessage;
        if (result.isMatch()) {
            statusMessage = selection.getPlayerNumber() == playerNumber
                    ? "מצוין! מצאת זוג תואם. התור שלך שוב."
                    : "היריב מצא זוג תואם. " + (result.getNextPlayer() == playerNumber ? "התור שלך." : "המתן...");
            isMyTurn = result.getNextPlayer() == playerNumber;
            waitingForCardsToClose = false;
        } else {
            statusMessage = selection.getPlayerNumber() == playerNumber
                    ? "אין התאמה. "
                    : "היריב לא מצא התאמה. ";
            isMyTurn = result.getNextPlayer() == playerNumber;
            statusMessage += "ממתין לסגירת הקלפים...";
            waitingForCardsToClose = true;
        }

        statusLabel.setText(statusMessage);

        if (!result.isMatch()) {
            scheduler.schedule(() -> Platform.runLater(() -> {
                hideCards(selection);
                waitingForCardsToClose = false;
                if (isMyTurn) {
                    statusLabel.setText("התור שלך! בחר שני קלפים.");
                } else {
                    statusLabel.setText("תור היריב, המתן...");
                }
            }), 2, TimeUnit.SECONDS);
        }

        if (result.isGameFinished()) {
            gameActive = false;
            newGameButton.setVisible(true);
            String endMessage = myScore > opponentScore
                    ? "המשחק הסתיים! ניצחת! הניקוד הסופי: " + myScore + " - " + opponentScore
                    : myScore < opponentScore
                    ? "המשחק הסתיים! הפסדת! הניקוד הסופי: " + myScore + " - " + opponentScore
                    : "המשחק הסתיים! תיקו! הניקוד הסופי: " + myScore + " - " + opponentScore;
            showAlert("סיום המשחק", endMessage);
        }
    }

    /** Called when the game ends. Shows the final result. */
    private void handleGameEnd(GameMessage message) {
        gameActive = false;
        newGameButton.setVisible(true);
        showAlert("סיום המשחק", message.getMessage());
    }

    /** Resets the card selection state after each turn. */
    private void resetCardSelection() {
        selectedCards = 0;
        selectedRow1 = selectedCol1 = selectedRow2 = selectedCol2 = -1;
    }

    /** Builds the game board grid with buttons for each card. */
    private void createGameBoard() {
        gameGrid.getChildren().clear();
        int rows = gameBoard.getRows();
        int cols = gameBoard.getCols();

        calculateResponsiveSizes(rows, cols);

        cardButtons = new Button[rows][cols];

        double gap = Math.max(2, cardSize * 0.05);
        gameGrid.setHgap(gap);
        gameGrid.setVgap(gap);

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                Button cardButton = new Button();
                cardButton.setPrefSize(cardSize, cardSize);
                cardButton.setMinSize(cardSize, cardSize);
                cardButton.setMaxSize(cardSize, cardSize);

                cardButton.setStyle("-fx-background-color: #ff69b4; -fx-border-color: #2c5aa0; -fx-border-width: 2; -fx-font-size: " + fontSize + "px;");

                final int r = row, c = col;
                cardButton.setOnAction(e -> onCardClicked(r, c));
                cardButtons[row][col] = cardButton;
                gameGrid.add(cardButton, col, row);
            }
        }
    }

    /** Called when a card is clicked. Checks if it's a valid move and sends it to the server. */
    private void onCardClicked(int row, int col) {
        if (!gameActive || !isMyTurn || waitingForCardsToClose) {
            if (waitingForCardsToClose) {
                statusLabel.setText("המתן עד שהקלפים ייסגרו...");
            } else {
                statusLabel.setText("זה לא התור שלך!");
            }
            return;
        }
        if (!gameBoard.canSelectCard(row, col)) {
            statusLabel.setText("הקלף כבר נחשף או מותאם.");
            return;
        }
        if (selectedCards == 1 && row == selectedRow1 && col == selectedCol1) {
            statusLabel.setText("בחרת את אותו קלף פעמיים.");
            return;
        }

        selectedCards++;
        gameBoard.revealCard(row, col);

        showCardImage(cardButtons[row][col], gameBoard.getCardValue(row, col));
        cardButtons[row][col].setStyle("-fx-background-color: lightblue; -fx-border-color: #2c5aa0; -fx-border-width: 2; -fx-font-size: " + fontSize + "px;");

        if (selectedCards == 1) {
            selectedRow1 = row;
            selectedCol1 = col;
            statusLabel.setText("בחר קלף שני.");
        } else {
            selectedRow2 = row;
            selectedCol2 = col;
            CardSelection selection = new CardSelection(selectedRow1, selectedCol1, selectedRow2, selectedCol2, playerNumber);
            sendCardSelection(selection);
            statusLabel.setText("ממתין לתגובת השרת...");
            isMyTurn = false;
        }
    }

    /** Shows the image of a card on the button. */
    private void showCardImage(Button button, int cardValue) {
        if (cardImages != null && cardValue >= 1 && cardValue <= 40) {
            ImageView imageView = new ImageView(cardImages[cardValue - 1]);
            imageView.setFitWidth(imageSize);
            imageView.setFitHeight(imageSize);
            imageView.setPreserveRatio(true);
            imageView.setSmooth(true);
            button.setGraphic(imageView);
            button.setText("");
        } else {
            button.setText(String.valueOf(cardValue));
            button.setStyle(button.getStyle() + " -fx-font-weight: bold;");
        }
    }

    /** Hides the image from a card button. */
    private void hideCardImage(Button button) {
        button.setGraphic(null);
        button.setText("");
        button.setStyle("-fx-background-color: #ff69b4; -fx-border-color: #2c5aa0; -fx-border-width: 2; -fx-font-size: " + fontSize + "px;");
    }

    /** Sends the selected cards to the server using a new thread. */
    private void sendCardSelection(CardSelection selection) {
        Thread sendThread = new Thread(() -> {
            try {
                output.writeObject(selection);
                output.flush();
            } catch (IOException e) {
                Platform.runLater(() -> {
                    statusLabel.setText("שגיאה בשליחת הבחירה לשרת.");
                    isMyTurn = true;
                });
            }
        });
        sendThread.setDaemon(true);
        sendThread.start();
    }

    /** Shows the two selected cards and colors them by match result. */
    private void updateBoardDisplay(CardSelection selection, boolean isMatch) {
        int row1 = selection.getRow1();
        int col1 = selection.getCol1();
        int row2 = selection.getRow2();
        int col2 = selection.getCol2();

        showCardImage(cardButtons[row1][col1], gameBoard.getCardValue(row1, col1));
        showCardImage(cardButtons[row2][col2], gameBoard.getCardValue(row2, col2));

        String color = isMatch ? "lightgreen" : "lightcoral";
        cardButtons[row1][col1].setStyle("-fx-background-color: " + color + "; -fx-border-color: #2c5aa0; -fx-border-width: 2; -fx-font-size: " + fontSize + "px;");
        cardButtons[row2][col2].setStyle("-fx-background-color: " + color + "; -fx-border-color: #2c5aa0; -fx-border-width: 2; -fx-font-size: " + fontSize + "px;");

        if (isMatch) {
            gameBoard.revealCard(row1, col1);
            gameBoard.revealCard(row2, col2);
            gameBoard.markAsMatched(row1, col1);
            gameBoard.markAsMatched(row2, col2);
        }
    }

    /** Hides the selected cards if they do not match. */
    private void hideCards(CardSelection selection) {
        int row1 = selection.getRow1();
        int col1 = selection.getCol1();
        int row2 = selection.getRow2();
        int col2 = selection.getCol2();

        hideCardImage(cardButtons[row1][col1]);
        hideCardImage(cardButtons[row2][col2]);

        gameBoard.hideCard(row1, col1);
        gameBoard.hideCard(row2, col2);
    }

    /** Starts a new game and reconnects to the server. */
    @FXML
    private void onNewGameClicked() {
        if (socket != null && !socket.isClosed()) {
            disconnect();
        }
        resetGame();
        connectToServer();
    }

    /** Disconnects from server and closes the app. */
    @FXML
    private void onDisconnectClicked() {
        disconnect();
        Platform.exit();
    }

    /** Closes all network resources and stops the game. */
    private void disconnect() {
        try {
            gameActive = false;
            if (output != null) output.close();
            if (input != null) input.close();
            if (socket != null && !socket.isClosed()) socket.close();
            if (scheduler != null) scheduler.shutdown();
        } catch (IOException e) {
            System.err.println("שגיאה בניתוק: " + e.getMessage());
        }
    }

    /** Resets the game screen and variables for a new start. */
    private void resetGame() {
        gameActive = true;
        selectedCards = 0;
        myScore = opponentScore = 0;
        isMyTurn = false;
        waitingForCardsToClose = false;
        gameBoard = null;
        cardButtons = null;

        scoreLabel.setText("הניקוד שלי: 0 | היריב: 0");
        playerLabel.setText("שחקן: -");
        statusLabel.setText("ממתין לחיבור לשרת...");
        newGameButton.setVisible(false);
        if (gameGrid != null) gameGrid.getChildren().clear();

        if (scheduler != null) scheduler.shutdown();
        scheduler = Executors.newScheduledThreadPool(1);
        resetCardSelection();
    }

    /** Shows a simple message box with a title and message. */
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.show();
    }
}