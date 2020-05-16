package project.message;

import java.math.BigInteger;

public class ConnectionResponseMessage extends BaseMessage{
    private final BigInteger predecessor;
    private final String address;
    private final int port;

    public ConnectionResponseMessage(int sender_id, BigInteger predecessor, String address, int port) {
        super(Message_Type.CONNECTION_RESPONSE, sender_id);
        this.predecessor = predecessor;
        this.address = address;
        this.port = port;
    }

    @Override
    public String getHeader(){
        return super.getHeader() + " " + predecessor + " " + address + " " + port;
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
