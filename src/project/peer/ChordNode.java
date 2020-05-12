package project.peer;

import project.message.*;
import project.protocols.ConnectionProtocol;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class ChordNode {
    public static int number_of_peers;

    private int m = 128;

    public static NodeInfo this_node;
    public static NodeInfo predecessor;

    private static ConcurrentHashMap<Integer, BigInteger> finger_table = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<BigInteger, NodeInfo> successors_info = new ConcurrentHashMap<>();

    private static SSLServerSocket server_socket = null;
    private static SSLServerSocketFactory server_socket_factory = null;
    private static SSLSocketFactory socket_factory = null;

    public ChordNode(int port) throws IOException, NoSuchAlgorithmException {
        number_of_peers = 1;
        String address = InetAddress.getLocalHost().getHostAddress();
        this_node = new NodeInfo(generateKey(address, port), address, port);
        initiateServerSockets();
        run();
    }

    public ChordNode(int port, String neighbour_address, int neighbour_port) throws IOException, NoSuchAlgorithmException {
        number_of_peers = 0;
        String address = InetAddress.getLocalHost().getHostAddress();
        this_node = new NodeInfo(generateKey(address, port), address, port);
        initiateServerSockets();
        ConnectionProtocol.connectToNetwork(neighbour_address, neighbour_port);
        run();
    }

    private BigInteger generateKey(String address, int port) throws NoSuchAlgorithmException {
        String unique_id = address + ":" + port;
        BigInteger maximum = new BigInteger("2").pow(m);

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(unique_id.getBytes(StandardCharsets.UTF_8));
        return new BigInteger(1, hash).mod(maximum);
    }

    private void initiateServerSockets() throws IOException {
        this.server_socket_factory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
        this.server_socket = (SSLServerSocket) server_socket_factory.createServerSocket(this_node.port);
        this.server_socket.setEnabledCipherSuites(this.server_socket.getSupportedCipherSuites());
        this.socket_factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
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
        updateFingerTable();

        Runnable task = ()-> stabilize();
        Peer.scheduled_executor.schedule(task, 60, TimeUnit.SECONDS);
    }

    private void updateFingerTable() {
        if(number_of_peers > 1){
            int num_entries = (int) Math.sqrt(number_of_peers);

            for(int i=1; i <= num_entries; i++){
                BigInteger lookup_key = this_node.key.add(new BigInteger("2").pow(i-1)).mod(new BigInteger("2").pow(m));
                System.out.println("lookupkey: " + lookup_key.toString());
            }
        }
    }

    private void receiveRequest(SSLSocket socket) {
        try {

            ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
            byte[] request = (byte[]) objectInputStream.readObject();

            System.out.println("RECEIVE REQUEST: " + new String(request));

            BaseMessage response = MessageHandler.handleMessage(request);

            ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
            objectOutputStream.writeObject(response.convertMessage());

            System.out.println("SEND RESPONSE: " + new String(response.convertMessage()));

            socket.close();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static BaseMessage makeRequest(BaseMessage request, String address, Integer port){
        try {
            SSLSocket socket = (SSLSocket) socket_factory.createSocket(address, port);
            socket.setEnabledCipherSuites(socket.getSupportedCipherSuites());

            ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
            objectOutputStream.writeObject(request.convertMessage());

            System.out.println("MAKE REQUEST: " + new String(request.convertMessage()));

            ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
            byte[] raw_response = (byte[]) objectInputStream.readObject();

            System.out.println("RECEIVE RESPONSE: " + new String(raw_response));

            socket.close();

            return MessageHandler.handleMessage(raw_response);

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void setFingerTable(String chunk) {
        List<String> chunk_bites = Arrays.asList(chunk.split(" "));

        for(int i=0; i < chunk_bites.size(); i++){
            List<String> node_info = Arrays.asList(chunk.split(":"));
            BigInteger key = new BigInteger(node_info.get(0));
            finger_table.put(i, key);
            successors_info.put(key, new NodeInfo(key, node_info.get(1), Integer.parseInt(node_info.get(2))));
        }
    }

    public static byte[] convertFingerTable() {
        String result = "";

        for(int i = finger_table.size(); i > 0; i++){
            NodeInfo node = successors_info.get(finger_table.get(i));
            result = result + node.key + ":" + node.address + ":" + node.port + " ";
        }

        return result.getBytes();
    }

    public static void addSuccessor(NodeInfo successor) {
        finger_table.remove(1);
        successors_info.remove(successor.key);

        finger_table.put(1, successor.key);
        successors_info.put(successor.key, successor);
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

    public static NodeInfo findPredecessor(BigInteger successor){
        if(this_node.key.equals(successor)) {
            return this_node;
        }
        else if(isKeyBetween(finger_table.get(0), this_node.key, successor)){
            return successors_info.get(finger_table.get(0));
        }
        else return successors_info.get(closestPrecedingNode(successor));
    }

    public static BigInteger closestPrecedingNode(BigInteger desired_key) {
        for (int n = finger_table.size(); n > 1; n--) {
            BigInteger aux = finger_table.get(n);
            if (isKeyBetween(aux, this_node.key, desired_key))
                return aux;
        }
        return null;
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
