package project.peer;

import project.message.*;
import project.Pair;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ChordNode {
    private int number_of_peers;

    private final String IP = InetAddress.getLocalHost().getHostAddress();
    private final int port;

    private String key = UUID.randomUUID().toString();

    private ConcurrentHashMap<Integer, String> finger_table = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, Pair<String, Integer>> successors_info = new ConcurrentHashMap<>();

    private SSLServerSocket server_socket = null;
    private SSLServerSocketFactory server_socket_factory = null;
    private SSLSocketFactory socket_factory = null;

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
        //Sockets "created" by accept method inherit the cipher suite.
        this.server_socket.setEnabledCipherSuites(this.server_socket.getSupportedCipherSuites());
        this.socket_factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
    }

    private void connectToNetwork(String neighbour_address, int neighbour_port) {
        ConnectionRequestMessage request = new ConnectionRequestMessage(Peer.id, neighbour_address, neighbour_port);
        makeRequest(request, neighbour_address, neighbour_port);
    }

    private void run() {
        while(true){
            try{
                SSLSocket socket = (SSLSocket) server_socket.accept();

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
            byte[] request = (byte[]) objectInputStream.readObject();

            //TODO Isto retorna sempre null e para de funcionar ao fazer response.convertMessage() porque a response é null
            BaseMessage response = MessageHandler.handleMessage(request);

            ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
            objectOutputStream.writeObject(response.convertMessage());


            socket.close();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void makeRequest(BaseMessage request, String address, Integer port){
        try {
            SSLSocket socket = (SSLSocket) socket_factory.createSocket(address, port);
            socket.setEnabledCipherSuites(socket.getSupportedCipherSuites());

            ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
            objectOutputStream.writeObject(request.convertMessage());

            ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
            byte[] raw_response = (byte[]) objectInputStream.readObject();

            MessageHandler.handleMessage(raw_response);

            socket.close();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private Pair<String, Integer> getDestinationNode() {
        return null;
    }

    public String findSuccessor(String desired_key){
        if(this.key.equals(desired_key)) {
            return this.key;
        }
        else if(desired_key.compareTo(finger_table.get(0)) <= 0){
            return finger_table.get(0);
        }
        else{
            String successor = closestPrecedingNode(desired_key);
            if(successor == null){
                //not sure what to do porque supostamente a key nao existe neste caso
            }
            else return successor;
        }
    }

    public String closestPrecedingNode(String desired_key){
        for(int n = finger_table.size(); n > 0; n--){
            String key = finger_table.get(n);
            if(key.compareTo(desired_key) <= 0)
                return key;
        }
        return null;
    }
}
