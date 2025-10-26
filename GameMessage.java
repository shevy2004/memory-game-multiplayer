
import java.io.Serializable;

/** A message that is sent between the server and the client during the game. */
public class GameMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    /** Different types of messages that are used in the game. */
    public enum MessageType {
        JOIN_GAME,          // When a player joins the game
        GAME_START,         // When the game starts
        BOARD_DATA,         // Sends the game board
        PLAYER_TURN,        // Tells whose turn it is
        CARD_SELECTION,     // A card was selected
        TURN_RESULT,        // The result of the move
        GAME_END,           // When the game ends
        NEW_GAME_REQUEST    // Player wants to start a new game
    }

    private MessageType type;
    private Object data;
    private String message;
    private int playerNumber;

    /** Creates a message with only a type. */
    public GameMessage(MessageType type) {
        this.type = type;
    }

    /** Creates a message with type and data. */
    public GameMessage(MessageType type, Object data) {
        this.type = type;
        this.data = data;
    }

    /** Creates a message with type and text. */
    public GameMessage(MessageType type, String message) {
        this.type = type;
        this.message = message;
    }

    /** Returns the message type. */
    public MessageType getType() {
        return type;
    }

    /** Sets the message type. */
    public void setType(MessageType type) {
        this.type = type;
    }

    /** Returns the data in the message. */
    public Object getData() {
        return data;
    }

    /** Sets the data in the message. */
    public void setData(Object data) {
        this.data = data;
    }

    /** Returns the text message. */
    public String getMessage() {
        return message;
    }

    /** Sets the text message. */
    public void setMessage(String message) {
        this.message = message;
    }

    /** Returns the player number. */
    public int getPlayerNumber() {
        return playerNumber;
    }

    /** Sets the player number. */
    public void setPlayerNumber(int playerNumber) {
        this.playerNumber = playerNumber;
    }
}
