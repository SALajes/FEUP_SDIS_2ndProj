package project.message;

import java.math.BigInteger;

public class ConnectionResponseMessage extends BaseMessage{
    private final int number_of_peers;
    private final BigInteger predecessor;
    private final String address;
    private final int port;

    public ConnectionResponseMessage(int sender_id, int number_of_peers, BigInteger predecessor, String address, int port) {
        super(Message_Type.CONNECTIONRESPONSE, sender_id);
        this.number_of_peers = number_of_peers;
        this.predecessor = predecessor;
        this.address = address;
        this.port = port;
    }

    @Override
    public String getHeader(){
        return super.getHeader() + " " + number_of_peers + " " + predecessor + " " + address + " " + port;
    }

    public int getNumberOfPeers() {
        return number_of_peers;
    }

    public BigInteger getPredecessor(){
        return predecessor;
    }

    public String getAddress(){
        return address;
    }

    public int getPort(){
        return port;
    }
}
