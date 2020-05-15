package project.protocols;

import project.message.*;
import project.peer.ChordNode;
import project.peer.NodeInfo;
import project.peer.Peer;

import java.math.BigInteger;

public class ConnectionProtocol {
    public static void connectToNetwork(String neighbour_address, int neighbour_port) {
        ConnectionRequestMessage request = new ConnectionRequestMessage(Peer.id, ChordNode.this_node.key, ChordNode.this_node.address, ChordNode.this_node.port);

        ConnectionResponseMessage response = (ConnectionResponseMessage) ChordNode.makeRequest(request, neighbour_address, neighbour_port);
        ChordNode.setNumberOfPeers(response.getNumberOfPeers());
        ChordNode.setPredecessor(response.getPredecessor(), response.getAddress(), response.getPort());

        RequestPredecessorMessage contact_predecessor = new RequestPredecessorMessage(Peer.id, ChordNode.this_node.key, ChordNode.this_node.address, ChordNode.this_node.port);
        PredecessorResponseMessage predecessor_response = (PredecessorResponseMessage) ChordNode.makeRequest(contact_predecessor, ChordNode.predecessor.address, ChordNode.predecessor.port);

        if(predecessor_response.getChunk().length != 0){
            ChordNode.setFingerTable(new String(predecessor_response.getChunk()).trim());
        }
    }

    public static BaseMessage receiveRequest(ConnectionRequestMessage message) {
        NodeInfo predecessor = ChordNode.findPredecessor(message.getKey());
        if(ChordNode.number_of_peers == 1){
            ChordNode.setPredecessor(message.getKey(), message.getAddress(), message.getPort());
        }
        ChordNode.incrementNumberOfPeers();
        return new ConnectionResponseMessage(Peer.id, ChordNode.number_of_peers, predecessor.key, predecessor.address, predecessor.port);
    }

    public static BaseMessage receiveRequestPredecessor(RequestPredecessorMessage message) {
        NodeInfo successor = new NodeInfo(message.getKey(), message.getAddress(), message.getPort());
        PredecessorResponseMessage response = new PredecessorResponseMessage(Peer.id, ChordNode.convertFingerTable());

        ChordNode.addSuccessor(successor);

        return response;
    }

    public static NodeInfo findSuccessor(BigInteger key, NodeInfo node) {
        NodeMessage successor = (NodeMessage) ChordNode.makeRequest(new FindNodeMessage(Message_Type.FIND_SUCCESSOR, Peer.id, key), node.address, node.port);
        return new NodeInfo(successor.getKey(), successor.getAddress(), successor.getPort());
    }

    public static NodeInfo findPredecessor(BigInteger key, NodeInfo node) {
        NodeMessage predecessor = (NodeMessage) ChordNode.makeRequest(new FindNodeMessage(Message_Type.FIND_PREDECESSOR, Peer.id, key), node.address, node.port);
        System.out.println(predecessor.getKey() + " " + predecessor.getAddress() + " " + predecessor.getPort());
        return new NodeInfo(predecessor.getKey(), predecessor.getAddress(), predecessor.getPort());
    }

    public static BaseMessage receiveFindPredecessor(FindNodeMessage message) {
        NodeInfo predecessor = ChordNode.findPredecessor(message.getKey());
        return new NodeMessage(Message_Type.PREDECESSOR, Peer.id, predecessor.key, predecessor.address, predecessor.port);
    }

    public static BaseMessage receiveFindSuccessor(FindNodeMessage message) {
        NodeInfo successor = ChordNode.findPredecessor(message.getKey());
        return new NodeMessage(Message_Type.SUCCESSOR, Peer.id, successor.key, successor.address, successor.port);
    }
}
