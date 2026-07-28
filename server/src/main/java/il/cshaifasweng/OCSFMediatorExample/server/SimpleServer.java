package il.cshaifasweng.OCSFMediatorExample.server;

import il.cshaifasweng.OCSFMediatorExample.entities.GameMessage;
import il.cshaifasweng.OCSFMediatorExample.server.ocsf.AbstractServer;
import il.cshaifasweng.OCSFMediatorExample.server.ocsf.ConnectionToClient;

import java.io.IOException;
import java.util.ArrayList;

import il.cshaifasweng.OCSFMediatorExample.entities.Warning;
import il.cshaifasweng.OCSFMediatorExample.server.ocsf.SubscribedClient;

public class SimpleServer extends AbstractServer {

	// Keep the list to manage our connected players
	private static ArrayList<SubscribedClient> SubscribersList = new ArrayList<>();

	// The shared game board for Tic-Tac-Toe
	private char[][] gameBoard = new char[3][3];

	// Keeps track of whose turn it currently is ('X' or 'O')
	private char currentTurn;

	// Keep track of the signs assigned to each player
	private char player1Sign;
	private char player2Sign;

	// Keep track of active connection status for reconnection logic
	private boolean isPlayer1Connected = true;
	private boolean isPlayer2Connected = true;

	public SimpleServer(int port) {
		super(port);
	}

	@Override
	protected void handleMessageFromClient(Object msg, ConnectionToClient client) {
		// Check if the received message is our game message type
		if (msg instanceof GameMessage) {
			GameMessage message = (GameMessage) msg;

			// Scenario 1: A player is trying to connect (or reconnect!)
			if (message.getType() == GameMessage.MessageType.WAITING) {
				char reconnectSign = message.getPlayerSign();

				// A. Check if this is a reconnecting player ('X' or 'O')
				if (reconnectSign == 'X' || reconnectSign == 'O') {
					System.out.println("Player " + reconnectSign + " is trying to reconnect...");

					// If Player 1 (X) disconnected and is now trying to return
					if (reconnectSign == player1Sign && !isPlayer1Connected) {
						SubscribersList.get(0).setClient(client); // Update the client reference to the new connection
						isPlayer1Connected = true;
						System.out.println("Player 1 (X) successfully reconnected!");
						broadcastUpdate(); // Resume game and update both players!
						return;
					}
					// If Player 2 (O) disconnected and is now trying to return
					else if (reconnectSign == player2Sign && !isPlayer2Connected) {
						SubscribersList.get(1).setClient(client); // Update the client reference to the new connection
						isPlayer2Connected = true;
						System.out.println("Player 2 (O) successfully reconnected!");
						broadcastUpdate(); // Resume game and update both players!
						return;
					}
				}

				// B. Check if the game is already full (Reject 3rd player)
				if (SubscribersList.size() >= 2) {
					System.out.println("A 3rd player tried to join but was rejected.");
					GameMessage rejectMsg = new GameMessage(GameMessage.MessageType.GAME_OVER);

					rejectMsg.setText("The game is already full. Please try again later!");
					try {
						client.sendToClient(rejectMsg);
					} catch (IOException e) {
						e.printStackTrace();
					}
					return; // Exit early to ignore this connection
				}

				// C. If the game is not full, handle a brand new player connection (Original logic)
				SubscribedClient connection = new SubscribedClient(client);
				SubscribersList.add(connection);

				System.out.println("A player connected. Total players: " + SubscribersList.size());

				// If this is the first player, tell them to wait
				if (SubscribersList.size() == 1) {
					GameMessage waitingMsg = new GameMessage(GameMessage.MessageType.WAITING);
					waitingMsg.setText("Waiting for another player to join...");
					try {
						client.sendToClient(waitingMsg);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				// If this is the second player, start the game!
				else if (SubscribersList.size() == 2) {
					// Reset connection status variables for a fresh start
					isPlayer1Connected = true;
					isPlayer2Connected = true;
					startGame();
				}
			}
			// Scenario 2: A player made a move
			else if (message.getType() == GameMessage.MessageType.MOVE) {
				char playerSign = message.getPlayerSign();

				// 1. Verify it is actually this player's turn
				if (playerSign == currentTurn) {
					char[][] clientBoard = message.getBoard();

					// 2. Update our master board in the server
					gameBoard = clientBoard;

					// 3. Check if this move won the game
					if (checkWin(playerSign)) {
						endGame(playerSign + " won the game!", playerSign);
					}
					// 4. Check if the board is full (Draw)
					else if (isBoardFull()) {
						endGame("The game ended in a draw!", ' ');
					}
					// 5. If no one won and no draw, switch turns!
					else {
						currentTurn = (currentTurn == 'X') ? 'O' : 'X';
						broadcastUpdate();
					}
				}
			}
		}
	}

	private void startGame() {
		// 1. Initialize the board with empty spaces
		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 3; j++) {
				gameBoard[i][j] = ' ';
			}
		}

		// 2. Randomly decide who gets 'X' and who gets 'O'
		boolean firstIsX = Math.random() < 0.5;
		player1Sign = firstIsX ? 'X' : 'O';
		player2Sign = firstIsX ? 'O' : 'X';

		// 3. Randomly decide who goes first ('X' or 'O')
		currentTurn = Math.random() < 0.5 ? 'X' : 'O';

		System.out.println("Game started! Player 1 is " + player1Sign + ", Player 2 is " + player2Sign + ". " + currentTurn + " starts.");

		// 4. Create and send start messages to both clients
		ConnectionToClient player1 = SubscribersList.get(0).getClient();
		ConnectionToClient player2 = SubscribersList.get(1).getClient();

		// Message for Player 1
		GameMessage msg1 = new GameMessage(GameMessage.MessageType.START);
		msg1.setBoard(gameBoard);
		msg1.setPlayerSign(player1Sign);
		msg1.setMyTurn(player1Sign == currentTurn);
		msg1.setText(player1Sign == currentTurn ? "Game started! Your turn (" + player1Sign + ")." : "Game started! Opponent's turn (" + currentTurn + ").");

		// Message for Player 2
		GameMessage msg2 = new GameMessage(GameMessage.MessageType.START);
		msg2.setBoard(gameBoard);
		msg2.setPlayerSign(player2Sign);
		msg2.setMyTurn(player2Sign == currentTurn);
		msg2.setText(player2Sign == currentTurn ? "Game started! Your turn (" + player2Sign + ")." : "Game started! Opponent's turn (" + currentTurn + ").");

		try {
			player1.sendToClient(msg1);
			player2.sendToClient(msg2);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// Checks if a player has won the game
	private boolean checkWin(char player) {
		// Check rows and columns
		for (int i = 0; i < 3; i++) {
			if ((gameBoard[i][0] == player && gameBoard[i][1] == player && gameBoard[i][2] == player) ||
					(gameBoard[0][i] == player && gameBoard[1][i] == player && gameBoard[2][i] == player)) {
				return true;
			}
		}
		// Check diagonals
		if ((gameBoard[0][0] == player && gameBoard[1][1] == player && gameBoard[2][2] == player) ||
				(gameBoard[0][2] == player && gameBoard[1][1] == player && gameBoard[2][0] == player)) {
			return true;
		}
		return false;
	}

	// Checks if the board is completely full (Draw scenario)
	private boolean isBoardFull() {
		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 3; j++) {
				if (gameBoard[i][j] == ' ') {
					return false;
				}
			}
		}
		return true;
	}

	// Ends the game and notifies both players of the result
	private void endGame(String messageText, char winner) {
		System.out.println("Game Over: " + messageText);

		ConnectionToClient player1 = SubscribersList.get(0).getClient();
		ConnectionToClient player2 = SubscribersList.get(1).getClient();

		GameMessage gameOverMsg = new GameMessage(GameMessage.MessageType.GAME_OVER);
		gameOverMsg.setBoard(gameBoard);
		gameOverMsg.setText(messageText);

		try {
			player1.sendToClient(gameOverMsg);
			player2.sendToClient(gameOverMsg);
		} catch (IOException e) {
			e.printStackTrace();
		}

		// Clear the subscribers list so a new game can start when clients reconnect
		SubscribersList.clear();
	}

	// Broadcasts the updated board and turn to both players
	private void broadcastUpdate() {
		ConnectionToClient player1 = SubscribersList.get(0).getClient();
		ConnectionToClient player2 = SubscribersList.get(1).getClient();

		// Message for Player 1
		GameMessage msg1 = new GameMessage(GameMessage.MessageType.UPDATE);
		msg1.setBoard(gameBoard);
		msg1.setPlayerSign(player1Sign);
		msg1.setMyTurn(player1Sign == currentTurn);
		msg1.setText(player1Sign == currentTurn ? "Your turn!" : "Opponent's turn (" + currentTurn + ")...");

		// Message for Player 2
		GameMessage msg2 = new GameMessage(GameMessage.MessageType.UPDATE);
		msg2.setBoard(gameBoard);
		msg2.setPlayerSign(player2Sign);
		msg2.setMyTurn(player2Sign == currentTurn);
		msg2.setText(player2Sign == currentTurn ? "Your turn!" : "Opponent's turn (" + currentTurn + ")...");

		try {
			player1.sendToClient(msg1);
			player2.sendToClient(msg2);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void clientDisconnected(ConnectionToClient client) {
		System.out.println("A client disconnected. Checking game state...");

		// 1. Identify which player disconnected
		boolean p1Disconnected = false;
		boolean p2Disconnected = false;

		if (SubscribersList.size() > 0 && SubscribersList.get(0).getClient().equals(client)) {
			p1Disconnected = true;
			isPlayer1Connected = false;
		} else if (SubscribersList.size() > 1 && SubscribersList.get(1).getClient().equals(client)) {
			p2Disconnected = true;
			isPlayer2Connected = false;
		}

		// If it wasn't an active player (e.g., game didn't start yet), just clean up
		if (!p1Disconnected && !p2Disconnected) {
			SubscribersList.removeIf(sub -> sub.getClient().equals(client));
			return;
		}

		// 2. Identify the remaining player
		final boolean finalP1Disconnected = p1Disconnected;
		final boolean finalP2Disconnected = p2Disconnected;

		ConnectionToClient remainingClient = finalP1Disconnected ?
				SubscribersList.get(1).getClient() : SubscribersList.get(0).getClient();

		// 3. Notify the remaining player to wait for reconnection
		GameMessage pauseMsg = new GameMessage(GameMessage.MessageType.WAITING);
		pauseMsg.setBoard(gameBoard);
		pauseMsg.setText("Opponent disconnected! Waiting 15 seconds for them to reconnect...");
		try {
			remainingClient.sendToClient(pauseMsg);
		} catch (IOException e) {
			e.printStackTrace();
		}

		// 4. Start a 15-second timer in the background
		new Thread(() -> {
			try {
				Thread.sleep(15000); // Wait for 15 seconds

				// Check if the disconnected player is still offline after 15 seconds
				if ((finalP1Disconnected && !isPlayer1Connected) || (finalP2Disconnected && !isPlayer2Connected)) {
					System.out.println("Grace period expired. Remaining player wins!");

					GameMessage forfeitMsg = new GameMessage(GameMessage.MessageType.GAME_OVER);
					forfeitMsg.setBoard(gameBoard);
					forfeitMsg.setText("Opponent failed to reconnect within 15 seconds. You win by forfeit! 🎉");

					try {
						remainingClient.sendToClient(forfeitMsg);
					} catch (IOException e) {
						e.printStackTrace();
					}

					// Clear the list so a new game can start freshly
					SubscribersList.clear();
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}).start();
	}

	public void sendToAllClients(String message) {
		try {
			for (SubscribedClient subscribedClient : SubscribersList) {
				subscribedClient.getClient().sendToClient(message);
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}
}