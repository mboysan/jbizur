package protocol.commands.ping;

import protocol.commands.NetworkCommand;

/**
 * Pong message
 */
public class Pong_NC extends NetworkCommand {

    @Override
    public String toString() {
        return "Pong_NC{} " + super.toString();
    }
}
