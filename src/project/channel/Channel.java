package project.channel;

import project.Macros;
import project.peer.Peer;

import java.io.IOException;
import java.net.*;

public abstract class Channel implements Runnable {
    public String address;
    public int port;
    public InetAddress InetAddress;

    public Channel(String address, int port ) {
        this.address = address;
        this.port = port;

        try {
            InetAddress = InetAddress.getByName(address);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(byte[] message){
        try{
            MulticastSocket socket = new MulticastSocket(this.port);
            socket.setTimeToLive(Macros.TTL);

            socket.joinGroup(this.InetAddress);

            DatagramPacket packet = new DatagramPacket(message, message.length, this.InetAddress, this.port);

            socket.send(packet);

        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected abstract void readableMessage(DatagramPacket packet);

    @Override
    public void run(){
        try {
            MulticastSocket socket = new MulticastSocket(this.port);
            socket.setTimeToLive(Macros.TTL);

            socket.joinGroup(this.InetAddress);

            while(true) {
                byte[] buffer = new byte[Macros.MAX_MESSAGE_SIZE];

                DatagramPacket packet = new DatagramPacket(buffer, Macros.MAX_MESSAGE_SIZE);

                socket.receive(packet);

                Runnable read_message_task = () -> readableMessage(packet);

                Peer.channel_executor.submit(read_message_task);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
