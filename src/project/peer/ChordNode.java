package project.peer;

import project.message.*;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.concurrent.ConcurrentHashMap;

public class ChordNode {
    private int number_of_peers;

    public static int m = 128;

    private final ChordNodeInfo nodeInfo;

    private ConcurrentHashMap<Integer, ChordNodeInfo> finger_table = new ConcurrentHashMap<>();

    private SSLServerSocket server_socket = null;
    private SSLServerSocketFactory server_socket_factory = null;
    private SSLSocketFactory socket_factory = null;

    public ChordNode(int port) throws IOException {
        this.nodeInfo = new ChordNodeInfo(port);
        initiateServerSockets();
        run();
    }

    public ChordNode(int port, String neighbour_address, int neighbour_port) throws IOException {
        this.nodeInfo = new ChordNodeInfo(port);
        initiateServerSockets();
        connectToNetwork(neighbour_address, neighbour_port);
        run();
    }

    private void initiateServerSockets() throws IOException {
        this.server_socket_factory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
        this.server_socket = (SSLServerSocket) server_socket_factory.createServerSocket(this.nodeInfo.getPort());
        //Sockets "created" by accept method inherit the cipher suite.
        this.server_socket.setEnabledCipherSuites(this.server_socket.getSupportedCipherSuites());
        this.socket_factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
    }

    private void connectToNetwork(String neighbour_address, int neighbour_port) {
        ConnectionRequestMessage request = new ConnectionRequestMessage(Peer.id, this.nodeInfo.getIP(), this.nodeInfo.getPort());
        makeRequest(request, neighbour_address, neighbour_port);
    }

    private void run() {
        while(true){
            try{
                SSLSocket socket = (SSLSocket) server_socket.accept();

                Runnable task = () -> receiveRequest(socket);
                Peer.scheduled_executor.execute(task);

            } catch (IOException ioException) {
                System.out.println("Failed to accept on port " + this.nodeInfo.getPort());
                ioException.printStackTrace();
            }
        }
    }

    private void receiveRequest(SSLSocket socket) {
        try {

            ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
            byte[] request = (byte[]) objectInputStream.readObject();

            //TODO Isto retorna sempre null e para de funcionar ao fazer response.convertMessage() porque a response é null
            BaseMessage response = MessageHandler.handleMessage(request);
            System.out.println("RECEIVED REQUEST: " + new String(request));

            ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
            objectOutputStream.writeObject(response.convertMessage());
            System.out.println("SENT RESPONSE: " + new String(response.convertMessage()));


            socket.close();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void makeRequest(BaseMessage request, String address, Integer port){
        try {
            SSLSocket socket = (SSLSocket) socket_factory.createSocket(address, port);
            socket.setEnabledCipherSuites(socket.getSupportedCipherSuites());
            System.out.println("MADE REQUEST: " + new String(request.convertMessage()));

            ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
            objectOutputStream.writeObject(request.convertMessage());

            ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
            byte[] raw_response = (byte[]) objectInputStream.readObject();

            MessageHandler.handleMessage(raw_response);
            System.out.println("RECEIVED RESPONSE: " + new String(raw_response));

            socket.close();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public ChordNodeInfo findSuccessor(String desired_key){
        if(this.nodeInfo.getKey().equals(desired_key)) {
            return this.nodeInfo;
        }
        else if(isKeyBetween(desired_key, this.nodeInfo.getKey(), finger_table.get(0).getKey())){
            return finger_table.get(0);
        }
        else return closestPrecedingNode(desired_key);
        /*
        ChordNodeInfo precedingFinger = closestPrecedingNode(desired_key);

        //send message to preceding finger to find successor of desired_key (?)
        */
    }

    public ChordNodeInfo closestPrecedingNode(String desired_key){
        for(int n = finger_table.size()-1; n > 0; n--){
            ChordNodeInfo node = finger_table.get(n);
            if(node != null && isKeyBetween(node.getKey(), this.nodeInfo.getKey(), desired_key))
                return node;
        }
        return null;
    }

    //Returns true if key is between lowerBound and upperBound, taking into account chord nodes are in a circle (lowerBound can have a higher value than upperBound)
    public boolean isKeyBetween(String key, String lowerBound, String upperBound){
        if(lowerBound.compareTo(upperBound) >= 0){
            return (key.compareTo(lowerBound) > 0) || (key.compareTo(upperBound) <= 0);
        }else{
            return (key.compareTo(lowerBound) > 0) && (key.compareTo(upperBound) <= 0);
        }
    }

}
