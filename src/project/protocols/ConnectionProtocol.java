package project.protocols;

import project.message.*;
import project.peer.ChordNode;
import project.peer.NodeInfo;
import project.peer.Peer;

public class ConnectionProtocol {
    public static void connectToNetwork(String neighbour_address, int neighbour_port) {
        ConnectionRequestMessage request = new ConnectionRequestMessage(Peer.id, ChordNode.this_node.key, ChordNode.this_node.address, ChordNode.this_node.port);

        ConnectionResponseMessage response = (ConnectionResponseMessage) ChordNode.makeRequest(request, neighbour_address, neighbour_port);
        ChordNode.setNumberOfPeers(response.getNumberOfPeers());
        ChordNode.setPredecessor(response.getPredecessor(), response.getAddress(), response.getPort());

        RequestPredecessorMessage contact_predecessor = new RequestPredecessorMessage(Peer.id, ChordNode.this_node.key, ChordNode.this_node.address, ChordNode.this_node.port);
        PredecessorResponseMessage predecessor_response = (PredecessorResponseMessage) ChordNode.makeRequest(contact_predecessor, ChordNode.predecessor.address, ChordNode.predecessor.port);

        ChordNode.setFingerTable(new String(predecessor_response.getChunk()).trim());
    }

    public static BaseMessage receiveRequest(ConnectionRequestMessage message) {
        NodeInfo predecessor = ChordNode.findPredecessor(message.getKey());
        ChordNode.incrementNumberOfPeers();
        return new ConnectionResponseMessage(Peer.id, ChordNode.number_of_peers, predecessor.key, predecessor.address, predecessor.port);
    }

    public static BaseMessage receiveRequestPredecessor(RequestPredecessorMessage message) {
        NodeInfo successor = new NodeInfo(message.getKey(), message.getAddress(), message.getPort());
        PredecessorResponseMessage response = new PredecessorResponseMessage(Peer.id, ChordNode.convertFingerTable());

        ChordNode.addSuccessor(successor);

        return response;
    }
}
