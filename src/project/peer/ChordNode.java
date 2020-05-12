package project.peer;

import org.w3c.dom.Node;
import project.message.*;
import project.Pair;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class ChordNode {
    public static int number_of_peers;

    private String IP;
    private int port;

    private int m = 128;
    private static BigInteger key;

    private static NodeInfo node;

    private static ConcurrentHashMap<Integer, BigInteger> finger_table = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<BigInteger, NodeInfo> successors_info = new ConcurrentHashMap<>();

    private static NodeInfo predecessor;

    private SSLServerSocket server_socket = null;
    private SSLServerSocketFactory server_socket_factory = null;
    private SSLSocketFactory socket_factory = null;

    public ChordNode(int port) throws IOException {
        number_of_peers = 1;
        initiateInfo(port);
        initiateServerSockets();
        run();
    }

    public ChordNode(int port, String neighbour_address, int neighbour_port) throws IOException {
        number_of_peers = 0;
        initiateInfo(port);
        initiateServerSockets();
        connectToNetwork(neighbour_address, neighbour_port);
        run();
    }

    private void initiateInfo(int port) throws UnknownHostException {
        this.IP = InetAddress.getLocalHost().getHostAddress();
        this.port = port;
        generateKey();
        this.node = new NodeInfo(key, IP, port);
    }

    private void generateKey() {
        String unique_id = IP + ":" + port;
        BigInteger maximum = new BigInteger("2").pow(m);

        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(unique_id.getBytes(StandardCharsets.UTF_8));
            this.key = new BigInteger(1, hash).mod(maximum);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    private void initiateServerSockets() throws IOException {
        this.server_socket_factory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
        this.server_socket = (SSLServerSocket) server_socket_factory.createServerSocket(this.port);
        //Sockets "created" by accept method inherit the cipher suite.
        this.server_socket.setEnabledCipherSuites(this.server_socket.getSupportedCipherSuites());
        this.socket_factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
    }

    private void connectToNetwork(String neighbour_address, int neighbour_port) {
        ConnectionRequestMessage request = new ConnectionRequestMessage(Peer.id, this.key, this.IP, this.port);
        makeRequest(request, neighbour_address, neighbour_port);
    }

    private void run() {
        Runnable stabilize_task = ()-> stabilize();
        Peer.scheduled_executor.schedule(stabilize_task, 60, TimeUnit.SECONDS);

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

    private void stabilize(){


        Runnable task = ()-> stabilize();
        Peer.scheduled_executor.schedule(task, 60, TimeUnit.SECONDS);
    }

    private void receiveRequest(SSLSocket socket) {
        try {

            ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
            byte[] request = (byte[]) objectInputStream.readObject();

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

    public static void incrementNumberOfPeers() {
        number_of_peers++;
    }

    public static void setNumberOfPeers(int n) {
        if(n > number_of_peers)
            number_of_peers = n;
    }

    public static void setPredecessor(BigInteger key, String address, int port) {
        predecessor = new NodeInfo(key, address, port);
    }

    public static NodeInfo findSuccessor(BigInteger desired_key){
        if(key.equals(desired_key)) {
            return node;
        }
        else if(desired_key.compareTo(finger_table.get(0)) <= 0){
            return successors_info.get(finger_table.get(0));
        }
        else return successors_info.get(closestPrecedingNode(desired_key));
    }

    public static BigInteger closestPrecedingNode(BigInteger desired_key){
        for(int n = finger_table.size(); n > 0; n--){
            BigInteger aux = finger_table.get(n);
            if(aux.compareTo(desired_key) <= 0)
                return aux;
        }
        return null;
    }

    public static NodeInfo findPredecessor(BigInteger successor){
        if(key.equals(successor)) {
            return node;
        }
        else return successors_info.get(closestPrecedingNode(successor));
    }
}
