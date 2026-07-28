package il.cshaifasweng.OCSFMediatorExample.entities;

import java.io.Serializable;

public class GameMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    // Message types to let the receiving party know what action to perform
    public enum MessageType {
        WAITING,      // The first client is waiting for a second player to connect
        START,        // The game is starting
        MOVE,         // A player is making a move
        UPDATE,       // Updating the board and the active turn
        GAME_OVER     // The game has ended (victory or draw)
    }

    private MessageType type;
    private char[][] board;      // The 3x3 game board
    private char playerSign;     // 'X' or 'O'
    private boolean isMyTurn;    // Indicates if it's currently this player's turn
    private String text;         // General message to display on the UI (e.g., "Your turn", "You lost")

    // Default constructor
    public GameMessage() {
        this.board = new char[3][3];
    }

    public GameMessage(MessageType type) {
        this();
        this.type = type;
    }

    // Getters and Setters
    public MessageType getType() { return type; }
    public void setType(MessageType type) { this.type = type; }

    public char[][] getBoard() { return board; }
    public void setBoard(char[][] board) { this.board = board; }

    public char getPlayerSign() { return playerSign; }
    public void setPlayerSign(char playerSign) { this.playerSign = playerSign; }

    public boolean isMyTurn() { return isMyTurn; }
    public void setMyTurn(boolean myTurn) { this.isMyTurn = myTurn; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
}