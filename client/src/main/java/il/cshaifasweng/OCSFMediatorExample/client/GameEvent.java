package il.cshaifasweng.OCSFMediatorExample.client;

import il.cshaifasweng.OCSFMediatorExample.entities.GameMessage;

public class GameEvent {
    private GameMessage message;

    // Constructor to wrap the incoming GameMessage
    public GameEvent(GameMessage message) {
        this.message = message;
    }

    // Getter to retrieve the wrapped message inside the controller
    public GameMessage getMessage() {
        return message;
    }
}