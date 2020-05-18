package project.peer;

import project.Macros;
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
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ChordNode {
    private static final int m = 64;

    public static NodeInfo this_node;
    public static NodeInfo predecessor;

    public static ConcurrentHashMap<Integer, NodeInfo> finger_table = new ConcurrentHashMap<>();

    private static SSLServerSocket server_socket = null;
    private static SSLSocketFactory socket_factory = null;

    public ChordNode(int port) throws IOException, NoSuchAlgorithmException {
        predecessor = null;
        String address = InetAddress.getLocalHost().getHostAddress();
        this_node = new NodeInfo(generateKey(address + ":" + port), address, port);
        initiateServerSockets();
        initializeFingerTable();
        printStart();

        Peer.thread_executor.execute(this::run);
    }

    public ChordNode(int port, String neighbour_address, int neighbour_port) throws IOException, NoSuchAlgorithmException {
        predecessor = null;
        String address = InetAddress.getLocalHost().getHostAddress();
        this_node = new NodeInfo(generateKey(address + ":" + port), address, port);
        initiateServerSockets();
        initializeFingerTable();
        ConnectionProtocol.connectToNetwork(neighbour_address, neighbour_port);
        printStart();
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
        System.out.println("THIS NODE: " + this_node.key);
        System.out.println("SUCCESSOR: " + finger_table.get(1).key);
        if(predecessor!=null)
            System.out.println("PREDECESSOR: " + predecessor.key);
        System.out.println("MOCKITO");

        Peer.thread_executor.execute(this::stabilize);
        Peer.thread_executor.execute(this::verifyPredecessor);
        Peer.thread_executor.execute(this::updateFingerTable);
    }

    private void stabilize() {
        if(finger_table.get(1) != null){
            NodeInfo previous_successor = finger_table.get(1);
            StabilizeResponseMessage new_successor = ConnectionProtocol.stabilize(finger_table.get(1));

            if(new_successor == null)
                return;

            if (new_successor.getStatus().equals(Macros.SUCCESS) &&
                    (!this_node.key.equals(new_successor.getSender()) && isKeyBetween(new_successor.getSender(), this_node.key, finger_table.get(1).key))){
                finger_table.replace(1, new NodeInfo(new_successor.getSender(), new_successor.getAddress(), new_successor.getPort()));
            }

            if(!ConnectionProtocol.notifySuccessor())
                finger_table.replace(1, previous_successor);
        }
    }

    private void verifyPredecessor() {
        if(!ConnectionProtocol.checkPredecessor())
            predecessor = null;
    }

    private void initializeFingerTable(){
        for(int i=1; i <= m; i++)
            finger_table.put(1, this_node);
    }

    private void updateFingerTable() {
        for(int i=2; i <= m; i++){
            BigInteger key = this_node.key.add(new BigInteger("2").pow(i-1)).mod(new BigInteger("2").pow(m));
            int entry = i;
            Runnable task = ()->updateTableEntry(entry, key);
            Peer.thread_executor.execute(task);
        }
    }

    public static void fingerTableRecovery(BigInteger key) {
        if(finger_table.get(m).key.equals(key))
            finger_table.replace(m, this_node);

        for(int i=m-1; i > 0; i--){
            if(finger_table.get(i).key.equals(key)){
                finger_table.replace(i, finger_table.get(i+1));
            }
        }

        if(finger_table.get(1).equals(this_node)){
            System.out.println("There are no more nodes where this peer can grab on to");
            System.exit(1);
        }
    }

    private void updateTableEntry(int entry, BigInteger key){
        NodeInfo node = findSuccessor(key);

        finger_table.remove(entry);
        finger_table.put(entry, node);
    }

    private void run() {
        Peer.scheduled_executor.scheduleAtFixedRate(this::verifyState, 3, 10, TimeUnit.SECONDS);

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
            BaseMessage request = (BaseMessage) objectInputStream.readObject();

            BaseMessage response = MessageHandler.handleMessage(request);

            ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
            objectOutputStream.writeObject(response);

            socket.close();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static BaseMessage makeRequest(BaseMessage request, String address, Integer port) throws IOException, ClassNotFoundException {
        SSLSocket socket = (SSLSocket) socket_factory.createSocket(address, port);
        socket.setEnabledCipherSuites(socket.getSupportedCipherSuites());

        ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
        objectOutputStream.writeObject(request);

        ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
        BaseMessage raw_response = (BaseMessage) objectInputStream.readObject();

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

        return (node.key + ":" + node.address + ":" + node.port).getBytes();
    }

    public static void addSuccessor(NodeInfo successor) {
        finger_table.remove(1);
        finger_table.put(1, successor);
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
