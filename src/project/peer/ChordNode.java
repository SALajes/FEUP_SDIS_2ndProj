package project.peer;

import project.message.*;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

public class ChordNode {
    private final int m = 128;
    private int number_of_peers;

    private final String IP = InetAddress.getLocalHost().getHostAddress();
    private final int port;

    private String key;

    private ConcurrentHashMap<Integer, String> finger_table = new ConcurrentHashMap<>();

    private static SSLSocket socket = null;
    private static SSLServerSocket server_socket = null;
    private static SSLServerSocketFactory server_socket_factory = null;

    public ChordNode(int port) throws IOException {
        this.port = port;
        initiateServerSockets();
        run();
    }

    public ChordNode(int port, String neighbour_address, int neighbour_port) throws IOException {
        this.port = port;
        initiateServerSockets();
        connectToNetwork(neighbour_address, neighbour_port);
        run();
    }

    private void initiateServerSockets() throws IOException {
        this.server_socket_factory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
        this.server_socket = (SSLServerSocket) server_socket_factory.createServerSocket(this.port);
    }

    private void connectToNetwork(String neighbour_address, int neighbour_port) {
        try {
            ConnectionRequestMessage request = new ConnectionRequestMessage(Peer.id, this.IP, this.port);
            Socket connection_socket = new Socket(neighbour_address, neighbour_port);

            ObjectOutputStream objectOutputStream = new ObjectOutputStream(connection_socket.getOutputStream());
            objectOutputStream.writeObject(request.convertMessage());

            ObjectInputStream objectInputStream = new ObjectInputStream(connection_socket.getInputStream());
            byte[] response = (byte[]) objectInputStream.readObject();
            ConnectionResponseMessage response_message = (ConnectionResponseMessage) MessageParser.parseMessage(response, response.length);

            number_of_peers = response_message.getNumberOfPeers();
            key = response_message.getKey();

            connection_socket.close();
        } catch (IOException | InvalidMessageException | ClassNotFoundException e) {
            return;
        }
    }

    private void run() {
        try{
            socket = (SSLSocket) server_socket.accept();
        } catch (IOException ioException) {
            System.out.println("Failed to accept on port " + port);
            ioException.printStackTrace();
        }
        
        receiveRequest();
    }

    private void receiveRequest() {
        while(true){
            
        }
    }

    public void makeRequest(){

    }

    public String findSuccessor(String key){
        return null;
    }
}
