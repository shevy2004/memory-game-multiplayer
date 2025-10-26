
import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.Scanner;

/**
 * MemoryGameServer manages multiple concurrent memory game sessions.
 * It listens for client connections, pairs players, and starts games.
 */
public class MemoryGameServer {
    private ServerSocket serverSocket;
    private int port;
    private int boardRows;
    private int boardCols;
    private ExecutorService threadPool;
    private BlockingQueue<Socket> waitingClients;

    // Maximum number of images available
    private static final int MAX_IMAGES = 40;
    private static final int MAX_BOARD_SIZE = 6; // Maximum allowed board size is 6x6

    /** Constructor initializes port, board size, and thread pool. */
    public MemoryGameServer(int port, int boardRows, int boardCols) {
        this.port = port;
        this.boardRows = boardRows;
        this.boardCols = boardCols;
        this.threadPool = Executors.newCachedThreadPool();
        this.waitingClients = new LinkedBlockingQueue<>();
    }

    /** Starts the server and accepts new client connections.
     *  Also starts a background thread to match players into games.
     */
    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("שרת משחק הזיכרון הופעל על פורט " + port);
            System.out.println("גודל לוח: " + boardRows + "x" + boardCols);
            System.out.println("ממתין לשחקנים...");

            // Thread dedicated to pairing players
            threadPool.execute(this::matchPlayers);

            // Accepting new clients
            while (!serverSocket.isClosed()) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("שחקן התחבר");
                waitingClients.offer(clientSocket);
            }
        } catch (IOException e) {
            System.err.println("שגיאה בהפעלת השרת: " + e.getMessage());
        }
    }

    /** Matches two clients together and starts a new game session in a thread. */
    private void matchPlayers() {
        while (true) {
            try {
                Socket player1 = waitingClients.take();
                Socket player2 = waitingClients.take();

                System.out.println("משחק התחיל!");

                GameSession gameSession = new GameSession(player1, player2, boardRows, boardCols);
                threadPool.execute(gameSession);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    /** Stops the server and shuts down the thread pool. */
    public void stop() {
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            threadPool.shutdown();
        } catch (IOException e) {
            System.err.println("שגיאה בסגירת השרת: " + e.getMessage());
        }
    }

    /**
     * Checks if the chosen board size is valid.
     * Returns false if size is too big, too small, or requires too many images.
     */
    private static boolean isValidBoardSize(int rows, int cols) {
        if (rows <= 0 || cols <= 0) {
            System.err.println("שגיאה: מימדי הלוח חייבים להיות מספרים חיוביים");
            return false;
        }

        int totalCells = rows * cols;
        if (totalCells % 2 != 0) {
            System.err.println("שגיאה: המספר הכולל של תאים (" + totalCells + ") חייב להיות זוגי למשחק זיכרון");
            return false;
        }

        if (rows > MAX_BOARD_SIZE || cols > MAX_BOARD_SIZE) {
            System.err.println("שגיאה: גודל הלוח גדול מדי! מקסימום " + MAX_BOARD_SIZE + "x" + MAX_BOARD_SIZE);
            return false;
        }

        int pairsNeeded = totalCells / 2;
        if (pairsNeeded > MAX_IMAGES) {
            System.err.println("שגיאה: גודל הלוח דורש יותר מדי תמונות!");
            return false;
        }

        return true;
    }

    /**
     * Main function of the server. Starts the server with given arguments.
     * Usage: java MemoryGameServer <port> <N>
     * N is the size of the board (NxN)
     */
    public static void main(String[] args) {
        int port = 8080; // Default port
        int boardRows = 4; // Default board size
        int boardCols = 4;

        System.out.println("מפעיל שרת משחק הזיכרון...");

        // Read port number from command-line
        if (args.length >= 1) {
            try {
                port = Integer.parseInt(args[0]);
                if (port < 1 || port > 65535) {
                    System.err.println("פורט לא תקין, משתמש בברירת מחדל: 8080");
                    port = 8080;
                }
            } catch (NumberFormatException e) {
                System.err.println("פורט לא תקין, משתמש בברירת מחדל: 8080");
            }
        }

        // Read board size from command-line
        if (args.length >= 2) {
            try {
                int boardSize = Integer.parseInt(args[1]);
                boardRows = boardSize;
                boardCols = boardSize;
            } catch (NumberFormatException e) {
                System.err.println("גודל לוח לא תקין, משתמש בברירת מחדל: 4x4");
                boardRows = 4;
                boardCols = 4;
            }
        }

        // Validate the board size
        if (!isValidBoardSize(boardRows, boardCols)) {
            System.err.println("משתמש בגודל לוח ברירת מחדל: 4x4");

            try (Scanner scanner = new Scanner(System.in)) {
                System.out.print("הזן גודל לוח חדש N (ליצירת לוח NxN): ");
                try {
                    int newSize = Integer.parseInt(scanner.nextLine().trim());

                    if (isValidBoardSize(newSize, newSize)) {
                        boardRows = newSize;
                        boardCols = newSize;
                        System.out.println("גודל לוח עודכן ל: " + boardRows + "x" + boardCols);
                    } else {
                        System.err.println("גודל לא תקין. משתמש בברירת מחדל: 4x4");
                        boardRows = 4;
                        boardCols = 4;
                    }
                } catch (NumberFormatException e) {
                    System.err.println("קלט לא תקין. משתמש בברירת מחדל: 4x4");
                    boardRows = 4;
                    boardCols = 4;
                }
            }
        }

        // Show final settings
        System.out.println("הגדרות השרת:");
        System.out.println("פורט: " + port);
        System.out.println("לוח: " + boardRows + "x" + boardCols);

        // Start server
        MemoryGameServer server = new MemoryGameServer(port, boardRows, boardCols);

        // Add shutdown hook to stop server when the program exits
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));

        server.start();
    }
}
