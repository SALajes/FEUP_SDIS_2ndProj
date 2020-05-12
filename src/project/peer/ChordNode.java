package project.peer;

import project.message.*;

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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class ChordNode {
    public static int number_of_peers;

    private int m = 128;

    private static NodeInfo this_node;

    private static ConcurrentHashMap<Integer, BigInteger> finger_table = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<BigInteger, NodeInfo> successors_info = new ConcurrentHashMap<>();

    private static NodeInfo predecessor;

    private SSLServerSocket server_socket = null;
    private SSLServerSocketFactory server_socket_factory = null;
    private SSLSocketFactory socket_factory = null;

    public ChordNode(int port) throws IOException, NoSuchAlgorithmException {
        number_of_peers = 1;
        this_node = new NodeInfo(generateKey(), InetAddress.getLocalHost().getHostAddress(), port);
        initiateServerSockets();
        run();
    }

    public ChordNode(int port, String neighbour_address, int neighbour_port) throws IOException, NoSuchAlgorithmException {
        number_of_peers = 0;
        this_node = new NodeInfo(generateKey(), InetAddress.getLocalHost().getHostAddress(), port);
        initiateServerSockets();
        connectToNetwork(neighbour_address, neighbour_port);
        run();
    }

    private BigInteger generateKey() throws NoSuchAlgorithmException {
        String unique_id = this_node.address + ":" + this_node.port;
        BigInteger maximum = new BigInteger("2").pow(m);

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(unique_id.getBytes(StandardCharsets.UTF_8));
        return new BigInteger(1, hash).mod(maximum);
    }

    private void initiateServerSockets() throws IOException {
        this.server_socket_factory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
        this.server_socket = (SSLServerSocket) server_socket_factory.createServerSocket(this_node.port);
        //Sockets "created" by accept method inherit the cipher suite.
        this.server_socket.setEnabledCipherSuites(this.server_socket.getSupportedCipherSuites());
        this.socket_factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
    }

    private void connectToNetwork(String neighbour_address, int neighbour_port) {
        ConnectionRequestMessage request = new ConnectionRequestMessage(Peer.id, this_node.key, this_node.address, this_node.port);
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
                System.out.println("Failed to accept on port " + this_node.port);
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
        if(this_node.key.equals(desired_key)) {
            return this_node;
        }
        else if(isKeyBetween(desired_key, this_node.key, finger_table.get(0))){
            return successors_info.get(finger_table.get(0));
        }
        else return successors_info.get(closestPrecedingNode(desired_key));
    }

    public static BigInteger closestPrecedingNode(BigInteger desired_key) {
        for (int n = finger_table.size(); n > 0; n--) {
            BigInteger aux = finger_table.get(n);
            if (isKeyBetween(aux, this_node.key, desired_key))
                return aux;
        }
        return null;
    }

    public static NodeInfo findPredecessor(BigInteger successor){
        if(this_node.key.equals(successor)) {
            return this_node;
        }
        else return successors_info.get(closestPrecedingNode(successor));
    }

    //Returns true if key is between lowerBound and upperBound, taking into account chord nodes are in a circle (lowerBound can have a higher value than upperBound)
    public static boolean isKeyBetween(BigInteger key, BigInteger lowerBound, BigInteger upperBound){
        if(lowerBound.compareTo(upperBound) >= 0){
            return (key.compareTo(lowerBound) > 0) || (key.compareTo(upperBound) <= 0);
        }else{
            return (key.compareTo(lowerBound) > 0) && (key.compareTo(upperBound) <= 0);
        }
    }
}
