package project.protocols;

import project.Macros;
import project.message.*;
import project.peer.ChordNode;
import project.peer.NodeInfo;
import project.peer.Peer;
import project.store.Store;

import java.io.IOException;
import java.math.BigInteger;

public class ConnectionProtocol {
    public static void connectToNetwork(String neighbour_address, int neighbour_port) {
        ConnectionRequestMessage request = new ConnectionRequestMessage(ChordNode.this_node.key, ChordNode.this_node.address, ChordNode.this_node.port);

        try {
            ConnectionResponseMessage response = (ConnectionResponseMessage) ChordNode.makeRequest(request, neighbour_address, neighbour_port);
            ChordNode.setPredecessor(response.getPredecessor(), response.getAddress(), response.getPort());
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }

        try {
            RequestPredecessorMessage contact_predecessor = new RequestPredecessorMessage(ChordNode.this_node.key, ChordNode.this_node.address, ChordNode.this_node.port);

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
        NodeInfo predecessor = ChordNode.findPredecessor(message.getSender());
        ChordNode.setPredecessor(message.getSender(), message.getAddress(), message.getPort());
        return new ConnectionResponseMessage(ChordNode.this_node.key, predecessor.key, predecessor.address, predecessor.port);
    }

    public static BaseMessage receiveRequestPredecessor(RequestPredecessorMessage message) {
        NodeInfo successor = new NodeInfo(message.getSender(), message.getAddress(), message.getPort());
        PredecessorResponseMessage response = new PredecessorResponseMessage(ChordNode.this_node.key, ChordNode.getSuccessor());

        ChordNode.addSuccessor(successor);

        return response;
    }

    public static boolean notifySuccessor() {
        try {
            NodeInfo successor = ChordNode.getSuccessorNode();
            NotifySuccessorMessage contact_successor = new NotifySuccessorMessage(ChordNode.this_node.key, ChordNode.this_node.address, ChordNode.this_node.port);
            ChordNode.makeRequest(contact_successor, successor.address, successor.port);
            return true;
        } catch (IOException | ClassNotFoundException e) {
            return false;
        }
    }

    public static BaseMessage receiveNotifySuccessor(NotifySuccessorMessage message) {
        String status = ChordNode.setPredecessor(message.getSender(), message.getAddress(), message.getPort());
        SuccessorResponseMessage response = new SuccessorResponseMessage(ChordNode.this_node.key, status);

        return response;
    }

    public static NodeInfo findSuccessor(BigInteger key, NodeInfo node) {
        try {
            NodeMessage successor = (NodeMessage) ChordNode.makeRequest(new FindNodeMessage(Message_Type.FIND_SUCCESSOR, ChordNode.this_node.key, key), node.address, node.port);
            return new NodeInfo(successor.getKey(), successor.getAddress(), successor.getPort());
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return null; //TODO SUCCESSOR IS DOWN
        }
    }

    public static NodeInfo findPredecessor(BigInteger key, NodeInfo node) {
        try {
            NodeMessage predecessor = (NodeMessage) ChordNode.makeRequest(new FindNodeMessage(Message_Type.FIND_PREDECESSOR, ChordNode.this_node.key, key), node.address, node.port);

            return new NodeInfo(predecessor.getKey(), predecessor.getAddress(), predecessor.getPort());
        } catch (IOException | ClassNotFoundException e) {
            return null;
        }
    }

    public static BaseMessage receiveFindPredecessor(FindNodeMessage message) {
        NodeInfo predecessor = ChordNode.findPredecessor(message.getKey());
        return new NodeMessage(Message_Type.PREDECESSOR, ChordNode.this_node.key, predecessor.key, predecessor.address, predecessor.port);
    }

    public static BaseMessage receiveFindSuccessor(FindNodeMessage message) {
        NodeInfo successor = ChordNode.findSuccessor(message.getKey());
        return new NodeMessage(Message_Type.SUCCESSOR, ChordNode.this_node.key, successor.key, successor.address, successor.port);
    }


    public static StabilizeResponseMessage stabilize(NodeInfo node) {
        try {
            return (StabilizeResponseMessage) ChordNode.makeRequest(new StabilizeMessage(ChordNode.this_node.key), node.address, node.port);
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Successor is down");
            ChordNode.fingerTableRecovery(node.key);
            Peer.thread_executor.execute(()->Store.getInstance().verifyBackupChunks(node.key));
            Peer.thread_executor.execute(()->sendDisconnectMessage(new DisconnectMessage(ChordNode.this_node.key, node.key, node.address, node.port)));

            return null;
        }
    }

    public static boolean checkPredecessor() {
        if(ChordNode.predecessor != null){
            try {
                ChordNode.makeRequest(new StabilizeMessage(ChordNode.this_node.key), ChordNode.predecessor.address, ChordNode.predecessor.port);
                return true;
            } catch (IOException | ClassNotFoundException e) {
            }
        }
        return false;
    }

    public static BaseMessage receivedStabilize() {
        if(ChordNode.predecessor == null)
            return new StabilizeResponseMessage(ChordNode.this_node.key, Macros.FAIL, BigInteger.ZERO, "0", 0);
        else return new StabilizeResponseMessage(ChordNode.this_node.key, Macros.SUCCESS, ChordNode.predecessor.key, ChordNode.predecessor.address, ChordNode.predecessor.port);
    }

    private static void sendDisconnectMessage(DisconnectMessage message) {
        try {
            ChordNode.makeRequest(message, ChordNode.getSuccessorNode().address, ChordNode.getSuccessorNode().port);
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static BaseMessage receivedDisconnectMessage(DisconnectMessage message) {
        if(!message.getSender().equals(ChordNode.this_node.key)){
            ChordNode.fingerTableRecovery(message.getKey());
            Peer.thread_executor.execute(()->Store.getInstance().verifyBackupChunks(message.getKey()));
            Peer.thread_executor.execute(()->sendDisconnectMessage(message));
        }
        return new MockMessage(ChordNode.this_node.key);
    }
}
