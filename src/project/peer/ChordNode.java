package project.peer;

import project.Macros;
import project.message.*;
import project.protocols.ConnectionProtocol;

import javax.crypto.Mac;
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
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ChordNode {
    public static int number_of_peers;

    private static int m = 128;

    public static NodeInfo this_node;
    public static NodeInfo predecessor;

    public static ConcurrentHashMap<Integer, NodeInfo> finger_table = new ConcurrentHashMap<>();

    private static SSLServerSocket server_socket = null;
    private static SSLSocketFactory socket_factory = null;

    public static ScheduledThreadPoolExecutor chord_executor = new ScheduledThreadPoolExecutor(4);

    public ChordNode(int port) throws IOException, NoSuchAlgorithmException {
        number_of_peers = 1;
        predecessor = null;
        String address = InetAddress.getLocalHost().getHostAddress();
        this_node = new NodeInfo(generateKey(address + ":" + port), address, port);
        initiateServerSockets();
        printStart();

        Peer.thread_executor.execute(this::run);
    }

    public ChordNode(int port, String neighbour_address, int neighbour_port) throws IOException, NoSuchAlgorithmException {
        number_of_peers = 0;
        predecessor = null;
        String address = InetAddress.getLocalHost().getHostAddress();
        this_node = new NodeInfo(generateKey(address + ":" + port), address, port);
        initiateServerSockets();
        ConnectionProtocol.connectToNetwork(neighbour_address, neighbour_port);
        printStart();

        updateFingerTable();

        Peer.thread_executor.execute(this::run);
    }

    private void printStart() {
        System.out.println("Peer " + Peer.id + " running in address " + this_node.address + " and port " + this_node.port +
                "\n( key: " + this_node.key.toString() + " )");
    }

    public static BigInteger generateKey(String data) throws NoSuchAlgorithmException {
        BigInteger maximum = new BigInteger("2").pow(m);

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
        return new BigInteger(1, hash).mod(maximum);
    }

    private void initiateServerSockets() throws IOException {
        server_socket = (SSLServerSocket) SSLServerSocketFactory.getDefault().createServerSocket(this_node.port);
        server_socket.setEnabledCipherSuites(server_socket.getSupportedCipherSuites());
        socket_factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
    }

    private void verifyState(){
        Peer.thread_executor.execute(()->stabilize());
        Peer.thread_executor.execute(()->verifyPredecessor());
        Peer.thread_executor.execute(()->updateFingerTable());
    }

    private void stabilize() {
        NodeInfo previous_successor = finger_table.get(1);
        StabilizeResponseMessage new_successor = (StabilizeResponseMessage) ConnectionProtocol.stabilize(finger_table.get(1).address, finger_table.get(1).port);

        if(new_successor.getStatus()== Macros.SUCCESS &&
                (!this_node.equals(new_successor) && isKeyBetween(new_successor.getKey(), this_node.key, finger_table.get(1).key)))
            finger_table.replace(1, new NodeInfo(new_successor.getKey(), new_successor.getAddress(), new_successor.getPort()));

        if(!ConnectionProtocol.notifySuccessor())
            finger_table.replace(1, previous_successor);
    }

    private void verifyPredecessor() {
        if(!ConnectionProtocol.checkPredecessor())
            predecessor = null;
    }

    private void updateFingerTable() {
        if(number_of_peers > 1){
            int num_entries = Math.min((int) Math.sqrt(number_of_peers), m);
            System.out.println("_______________________________________________________________");
            System.out.println("Num peers: " + number_of_peers);
            System.out.println(finger_table.get(1).key);
            System.out.println(this_node.key);
            if(predecessor != null)
                System.out.println("Predecessor: " + predecessor.key);

            for(int i=2; i <= num_entries; i++){
                BigInteger lookup_key = this_node.key.add(new BigInteger("2").pow(i-1)).mod(new BigInteger("2").pow(m));
                int entry = i;
                Runnable task = ()->updateTableEntry(entry, lookup_key);
                Peer.thread_executor.execute(task);
            }
        }
    }

    private void updateTableEntry(int entry, BigInteger lookup_key){
        NodeInfo new_node = findSuccessor(lookup_key);

        if(finger_table.containsKey(entry)){
            finger_table.remove(entry);
            finger_table.put(entry, new_node);
        }
    }

    private void run() {
        Runnable stabilize_task = ()-> verifyState();
        ChordNode.chord_executor.scheduleAtFixedRate(stabilize_task, 60, 60, TimeUnit.SECONDS);

        while(true){
            try{
                SSLSocket socket = (SSLSocket) server_socket.accept();

                Peer.thread_executor.execute(() -> receiveRequest(socket));

            } catch (IOException ioException) {
                System.out.println("Failed to accept on port " + this_node.port);
                ioException.printStackTrace();
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
            if (response != null) {
                objectOutputStream.writeObject(response.convertMessage());

            System.out.println("SEND RESPONSE: " + new String(response.convertMessage()));
            }else{
                System.out.println("Response was null");
            }

            socket.close();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static BaseMessage makeRequest(BaseMessage request, String address, Integer port) throws IOException, ClassNotFoundException {
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
    }

    public static void setSuccessor(String chunk) {
        List<String> node_info = Arrays.asList(chunk.split(":"));
        finger_table.put(1, new NodeInfo(new BigInteger(node_info.get(0).trim()), node_info.get(1).trim(), Integer.parseInt(node_info.get(2).trim())));
    }

    public static byte[] getSuccessor() {
        NodeInfo node = this_node;

        if(finger_table.size() > 0){
            node = finger_table.get(1);
        }

        StringBuilder result = new StringBuilder(node.key + ":" + node.address + ":" + node.port);

        return result.toString().getBytes();
    }

    public static void addSuccessor(NodeInfo successor) {
        finger_table.remove(1);
        finger_table.put(1, successor);
    }

    public static void incrementNumberOfPeers() {
        number_of_peers++;
    }

    public static void setNumberOfPeers(int n) {
        if(n > number_of_peers)
            number_of_peers = n;
    }

    public static String setPredecessor(BigInteger key, String address, int port) {
        if(predecessor == null || key.equals(predecessor.key) || isKeyBetween(key, predecessor.key, this_node.key)) {
            predecessor = new NodeInfo(key, address, port);
            return Macros.SUCCESS;
        }
        else return Macros.FAIL;
    }

    public static NodeInfo findSuccessor(BigInteger successor){
        if(this_node.key.equals(successor)) {
            return this_node;
        }
        else if(isKeyBetween(successor, this_node.key, finger_table.get(1).key)){
            return finger_table.get(1);
        }

        NodeInfo preceding_finger = closestPrecedingNode(successor);

        if(preceding_finger.key.equals(this_node.key)){
            return this_node;
        }

        return ConnectionProtocol.findSuccessor(successor, preceding_finger);
    }

    public static NodeInfo findPredecessor(BigInteger successor){
        if(finger_table.size() == 0) {
            return this_node;
        }
        else if(isKeyBetween(successor, this_node.key, finger_table.get(1).key)){
            return this_node;
        }

        NodeInfo preceding_finger = closestPrecedingNode(successor);

        if(preceding_finger.key.equals(this_node.key)){
            return this_node;
        }

        return ConnectionProtocol.findPredecessor(successor, preceding_finger);
    }

    public static NodeInfo closestPrecedingNode(BigInteger key) {
        for (int n = finger_table.size(); n >= 1; n--) {
            if (isKeyBetween(finger_table.get(n).key, this_node.key, key))
                return finger_table.get(n);
        }
        return this_node;
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
