package project.protocols;

import project.Macros;
import project.message.*;
import project.peer.ChordNode;
import project.peer.NodeInfo;
import project.peer.Peer;

import java.io.IOException;
import java.math.BigInteger;

public class ConnectionProtocol {
    public static void connectToNetwork(String neighbour_address, int neighbour_port) {
        ConnectionRequestMessage request = new ConnectionRequestMessage(Peer.id, ChordNode.this_node.key, ChordNode.this_node.address, ChordNode.this_node.port);

        try {
            ConnectionResponseMessage response = (ConnectionResponseMessage) ChordNode.makeRequest(request, neighbour_address, neighbour_port);
            ChordNode.setNumberOfPeers(response.getNumberOfPeers());
            ChordNode.setPredecessor(response.getPredecessor(), response.getAddress(), response.getPort());
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }

        try {
            RequestPredecessorMessage contact_predecessor = new RequestPredecessorMessage(Peer.id, ChordNode.this_node.key, ChordNode.this_node.address, ChordNode.this_node.port);
            PredecessorResponseMessage predecessor_response = (PredecessorResponseMessage) ChordNode.makeRequest(contact_predecessor, ChordNode.predecessor.address, ChordNode.predecessor.port);
            if(predecessor_response.getChunk().length != 0){
                ChordNode.setSuccessor(new String(predecessor_response.getChunk()).trim());
                notifySuccessor();
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
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
        PredecessorResponseMessage response = new PredecessorResponseMessage(Peer.id, ChordNode.getSuccessor());

        ChordNode.addSuccessor(successor);

        return response;
    }

    public static boolean notifySuccessor() {
        try {
            NodeInfo successor = ChordNode.finger_table.get(1);
            NotifySuccessorMessage contact_successor = new NotifySuccessorMessage(Peer.id, ChordNode.this_node.key, ChordNode.this_node.address, ChordNode.this_node.port);
            ChordNode.makeRequest(contact_successor, successor.address, successor.port);
            return true;
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static BaseMessage receiveNotifySuccessor(NotifySuccessorMessage message) {
        String status = ChordNode.setPredecessor(message.getKey(), message.getAddress(), message.getPort());
        SuccessorResponseMessage response = new SuccessorResponseMessage(Peer.id, status);

        return response;
    }

    public static NodeInfo findSuccessor(BigInteger key, NodeInfo node) {
        try {
            NodeMessage successor = (NodeMessage) ChordNode.makeRequest(new FindNodeMessage(Message_Type.FIND_SUCCESSOR, Peer.id, key), node.address, node.port);
            return new NodeInfo(successor.getKey(), successor.getAddress(), successor.getPort());
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static NodeInfo findPredecessor(BigInteger key, NodeInfo node) {
        try {
            NodeMessage predecessor = (NodeMessage) ChordNode.makeRequest(new FindNodeMessage(Message_Type.FIND_PREDECESSOR, Peer.id, key), node.address, node.port);
            return new NodeInfo(predecessor.getKey(), predecessor.getAddress(), predecessor.getPort());
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static BaseMessage receiveFindPredecessor(FindNodeMessage message) {
        NodeInfo predecessor = ChordNode.findPredecessor(message.getKey());
        return new NodeMessage(Message_Type.PREDECESSOR, Peer.id, predecessor.key, predecessor.address, predecessor.port);
    }

    public static BaseMessage receiveFindSuccessor(FindNodeMessage message) {
        NodeInfo successor = ChordNode.findSuccessor(message.getKey());
        return new NodeMessage(Message_Type.SUCCESSOR, Peer.id, successor.key, successor.address, successor.port);
    }

    public static BaseMessage stabilize(String address, int port) {
        try {
            return ChordNode.makeRequest(new StabilizeMessage(Peer.id), address, port);
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            //TODO THIS MEANS SUCCESSOR IS DOWN
        }
        return null;
    }

    public static boolean checkPredecessor() {
        try {
            ChordNode.makeRequest(new StabilizeMessage(Peer.id), ChordNode.predecessor.address, ChordNode.predecessor.port);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static BaseMessage receivedStabilize(StabilizeMessage message) {
        if(ChordNode.predecessor == null)
            return new StabilizeResponseMessage(Peer.id, Macros.FAIL, BigInteger.ZERO, "0", 0);
        else return new StabilizeResponseMessage(Peer.id, Macros.SUCCESS, ChordNode.predecessor.key, ChordNode.predecessor.address, ChordNode.predecessor.port);
    }
}
