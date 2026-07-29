package il.cshaifasweng.OCSFMediatorExample.client;

import org.greenrobot.eventbus.EventBus;

import il.cshaifasweng.OCSFMediatorExample.client.ocsf.AbstractClient;
import il.cshaifasweng.OCSFMediatorExample.entities.GameMessage;
import il.cshaifasweng.OCSFMediatorExample.entities.Warning;

/**
 * Singleton Client instance managing socket connection and EventBus distribution.
 */
public class SimpleClient extends AbstractClient {

	private static SimpleClient client = null;

	private SimpleClient(String host, int port) {
		super(host, port);
	}

	@Override
	protected void handleMessageFromServer(Object msg) {
		// 1. Check if the message is our game message
		if (msg instanceof GameMessage) {
			// Wrap it in GameEvent and post it to the EventBus
			EventBus.getDefault().post(new GameEvent((GameMessage) msg));
		}
		// 2. Keep support for the original warning messages
		else if (msg instanceof Warning) {
			EventBus.getDefault().post(new WarningEvent((Warning) msg));
		}
		// 3. Any other general string/object messages
		else {
			String message = msg.toString();
			System.out.println("Received from server: " + message);
		}
	}

	/**
	 * Overloaded Singleton getter supporting custom target server host (IP or domain).
	 */
	public static SimpleClient getClient(String host) {
		if (client == null) {
			client = new SimpleClient(host, 3000);
		}
		return client;
	}

	/**
	 * Default Singleton getter fallback to localhost.
	 */
	public static SimpleClient getClient() {
		return getClient("localhost");
	}
}