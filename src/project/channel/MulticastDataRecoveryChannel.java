package project.channel;

import project.message.*;
import project.peer.Peer;
import project.protocols.RestoreProtocol;

import java.net.DatagramPacket;

public class MulticastDataRecoveryChannel extends Channel {

    public MulticastDataRecoveryChannel(String address, int port) {
        super(address, port);
    }

    @Override
    protected void readableMessage(DatagramPacket packet) {
        try {
            byte [] raw_message = packet.getData();
            BaseMessage message = MessageParser.parseMessage(raw_message, packet.getLength());

            if(message.getSenderId() == Peer.id)
                return;

            if(message.getMessageType() == Message_Type.CHUNK)
                RestoreProtocol.receiveChunk((ChunkMessage) message);
            else System.out.println("Invalid message type for Data Recovery Channel: " + message.getMessageType());


        } catch (InvalidMessageException e) {
            System.out.println(e.getMessage());
        }
    }
}