package project.protocols;

import project.message.*;
import project.peer.ChordNode;
import project.peer.NodeInfo;
import project.peer.Peer;

import java.math.BigInteger;
import java.sql.SQLClientInfoException;

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

            NodeInfo successor = ChordNode.finger_table.get(1);

            NotifySuccessorMessage contact_successor = new NotifySuccessorMessage(Peer.id, ChordNode.this_node.key, ChordNode.this_node.address, ChordNode.this_node.port);
            //Response is kind of redundant
            SuccessorResponseMessage successor_response = (SuccessorResponseMessage) ChordNode.makeRequest(contact_successor, successor.address, successor.port);

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

    public static BaseMessage receiveNotifySuccessor(NotifySuccessorMessage message) {
        SuccessorResponseMessage response = new SuccessorResponseMessage(Peer.id);

        ChordNode.setPredecessor(message.getKey(), message.getAddress(), message.getPort());

        return response;
    }

    public static NodeInfo findSuccessor(BigInteger key, NodeInfo node) {
        NodeMessage successor = (NodeMessage) ChordNode.makeRequest(new FindNodeMessage(Message_Type.FIND_SUCCESSOR, Peer.id, key), node.address, node.port);
        return new NodeInfo(successor.getKey(), successor.getAddress(), successor.getPort());
    }

    public static NodeInfo findPredecessor(BigInteger key, NodeInfo node) {
        NodeMessage predecessor = (NodeMessage) ChordNode.makeRequest(new FindNodeMessage(Message_Type.FIND_PREDECESSOR, Peer.id, key), node.address, node.port);
        return new NodeInfo(predecessor.getKey(), predecessor.getAddress(), predecessor.getPort());
    }

    public static BaseMessage receiveFindPredecessor(FindNodeMessage message) {
        NodeInfo predecessor = ChordNode.findPredecessor(message.getKey());
        return new NodeMessage(Message_Type.PREDECESSOR, Peer.id, predecessor.key, predecessor.address, predecessor.port);
    }

    public static BaseMessage receiveFindSuccessor(FindNodeMessage message) {
        NodeInfo successor = ChordNode.findSuccessor(message.getKey());
        return new NodeMessage(Message_Type.SUCCESSOR, Peer.id, successor.key, successor.address, successor.port);
    }
}
