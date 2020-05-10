package project.peer;

import project.message.*;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
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

    private static SSLServerSocket server_socket = null;
    private static SSLSocketFactory socket_factory = null;
    private static SSLSocket socket = null;

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
        this.server_socket = (SSLServerSocket) SSLServerSocketFactory.getDefault().createServerSocket(this.port);
        this.socket_factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
    }

    private void connectToNetwork(String neighbour_address, int neighbour_port) {
        try {
            ConnectionRequestMessage request = new ConnectionRequestMessage(Peer.id, this.IP, this.port);
            SSLSocket connection_socket = (SSLSocket) socket_factory.createSocket(neighbour_address, neighbour_port);

            ObjectOutputStream objectOutputStream = new ObjectOutputStream(connection_socket.getOutputStream());
            objectOutputStream.writeObject(request.convertMessage());

            ObjectInputStream objectInputStream = new ObjectInputStream(connection_socket.getInputStream());
            byte[] response = (byte[]) objectInputStream.readObject();
            ConnectionResponseMessage response_message = (ConnectionResponseMessage) MessageParser.parseMessage(response, response.length);

            number_of_peers = response_message.getNumberOfPeers();
            key = response_message.getKey();

            connection_socket.close();
        } catch (IOException | InvalidMessageException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void run() {
        while(true){
            try{
                socket = (SSLSocket) server_socket.accept();

                Runnable task = () -> receiveRequest(socket);
                Peer.scheduled_executor.execute(task);

            } catch (IOException ioException) {
                System.out.println("Failed to accept on port " + port);
                ioException.printStackTrace();
            }
        }
    }

    private void receiveRequest(SSLSocket socket) {
        try {
            ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
            byte[] response = (byte[]) objectInputStream.readObject();
            //gives response
            MessageHandler.handleMessage(response);

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void makeRequest(){

    }

    public String findSuccessor(String key){
        return null;
    }

    public boolean respond(BaseMessage message) {
        try {
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
            objectOutputStream.writeObject(message.convertMessage());
        } catch (IOException e) {
            return false;
        }

        return true;
    }
}
