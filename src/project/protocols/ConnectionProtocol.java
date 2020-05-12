package project.protocols;

import project.message.BaseMessage;
import project.message.ConnectionRequestMessage;
import project.message.ConnectionResponseMessage;
import project.peer.ChordNode;
import project.peer.NodeInfo;
import project.peer.Peer;

import java.math.BigInteger;

public class ConnectionProtocol {
    public static BaseMessage receiveRequest(ConnectionRequestMessage message) {
        NodeInfo predecessor = ChordNode.findPredecessor(message.getKey());
        ChordNode.incrementNumberOfPeers();
        return new ConnectionResponseMessage(Peer.id, ChordNode.number_of_peers, predecessor.key, predecessor.address, predecessor.port);
    }

    public static void receiveResponse(ConnectionResponseMessage message) {
        ChordNode.setNumberOfPeers(message.getNumberOfPeers());
        ChordNode.setPredecessor(message.getPredecessor(), message.getAddress(), message.getPort());
    }
}
