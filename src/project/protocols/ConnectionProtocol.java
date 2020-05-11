package project.protocols;

import project.message.BaseMessage;
import project.message.ConnectionRequestMessage;
import project.message.ConnectionResponseMessage;
import project.peer.Peer;

public class ConnectionProtocol {
    public static BaseMessage receiveRequest(ConnectionRequestMessage message) {
        return new ConnectionResponseMessage(Peer.id, 2);
    }

    public static void receiveResponse(ConnectionResponseMessage message) {
    }
}
