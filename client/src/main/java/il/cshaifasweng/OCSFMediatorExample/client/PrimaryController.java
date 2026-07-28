package il.cshaifasweng.OCSFMediatorExample.client;

import il.cshaifasweng.OCSFMediatorExample.entities.GameMessage;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Font;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.IOException;

public class PrimaryController {

	@FXML
	private Label statusLabel; // Displays game status (e.g., "Your turn", "Waiting...")

	@FXML
	private GridPane gameGrid; // The JavaFX layout grid where we dynamically place our 9 buttons

	private Button[][] buttons = new Button[3][3]; // Keeps physical button references in memory
	private char[][] board = new char[3][3];       // Stores the local game board state
	private char mySign = ' ';                     // The sign assigned by the server ('X' or 'O')
	private boolean isMyTurn = false;              // Flags whether it's currently our turn to play

	@FXML
	void initialize() {
		// 1. Register to the EventBus to listen for server messages in real-time
		EventBus.getDefault().register(this);

		// 2. Programmatically generate and arrange the 3x3 grid of buttons
		setupBoard();

		// 3. Notify the server that we connected and are waiting to join a game
		try {
			GameMessage joinMsg = new GameMessage(GameMessage.MessageType.WAITING);
			SimpleClient.getClient().sendToServer(joinMsg);
			statusLabel.setText("Connecting to server...");
		} catch (IOException e) {
			statusLabel.setText("Error connecting to server.");
			e.printStackTrace();
		}
	}

	// Programmatically builds the 3x3 tic-tac-toe visual grid
	private void setupBoard() {
		gameGrid.getChildren().clear(); // Clean up any existing elements in the grid

		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 3; col++) {
				Button btn = new Button("");
				btn.setPrefSize(100, 100); // Standardize button dimensions
				btn.setFont(new Font("Arial", 36)); // Clear, large font for X and O

				final int r = row;
				final int c = col;

				// Handle clicking a board cell
				btn.setOnAction(event -> handleButtonClick(r, c));

				buttons[row][col] = btn;
				gameGrid.add(btn, col, row); // Place the button on the JavaFX GridPane
			}
		}
		disableBoard(true); // Lock the board initially until the game officially starts
	}

	// Handles logic whenever a board button is clicked
	private void handleButtonClick(int row, int col) {
		// Moves are allowed only if it is our turn and the target cell is empty
		if (isMyTurn && board[row][col] == ' ') {
			board[row][col] = mySign; // Update the local board state
			buttons[row][col].setText(String.valueOf(mySign));
			isMyTurn = false; // Temporarily end local turn
			disableBoard(true); // Lock buttons to prevent multiple clicks

			// Send the updated board and move details to the server
			try {
				GameMessage moveMsg = new GameMessage(GameMessage.MessageType.MOVE);
				moveMsg.setBoard(board);
				moveMsg.setPlayerSign(mySign);
				SimpleClient.getClient().sendToServer(moveMsg);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	// Message handler invoked by EventBus whenever a server broadcast arrives
	@Subscribe
	public void onGameEvent(GameEvent event) {
		// Network messages arrive on a separate thread, UI updates must run on JavaFX Application Thread
		Platform.runLater(() -> {
			GameMessage message = event.getMessage();
			GameMessage.MessageType type = message.getType();

			// Synch local board with the server master board state
			board = message.getBoard();
			updateBoardUI();

			if (type == GameMessage.MessageType.WAITING) {
				statusLabel.setText(message.getText());
				disableBoard(true);
			}
			else if (type == GameMessage.MessageType.START) {
				mySign = message.getPlayerSign();
				isMyTurn = message.isMyTurn();
				statusLabel.setText(message.getText() + " You are: " + mySign);
				disableBoard(!isMyTurn);
			}
			else if (type == GameMessage.MessageType.UPDATE) {
				isMyTurn = message.isMyTurn();
				statusLabel.setText(message.getText());
				disableBoard(!isMyTurn);
			}
			else if (type == GameMessage.MessageType.GAME_OVER) {
				statusLabel.setText(message.getText());
				disableBoard(true); // Lock the board permanently since game has ended

				// If the message indicates the game is full, hide the game board entirely for better UX
				if (message.getText().contains("full")) {
					gameGrid.setVisible(false);
				}

				// Unregister to prevent memory leaks and handle future connections freshly
				EventBus.getDefault().unregister(this);
			}
		});
	}

	// Synchronizes the text displayed on the 3x3 visual buttons with our actual memory board
	private void updateBoardUI() {
		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 3; j++) {
				char cell = board[i][j];
				if (cell == ' ') {
					buttons[i][j].setText("");
				} else {
					buttons[i][j].setText(String.valueOf(cell));
				}
			}
		}
	}

	// Utility method to lock or unlock the interactability of board buttons
	private void disableBoard(boolean disable) {
		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 3; j++) {
				// If a spot is already marked, keep it disabled regardless
				if (board[i][j] != ' ') {
					buttons[i][j].setDisable(true);
				} else {
					buttons[i][j].setDisable(disable);
				}
			}
		}
	}
}