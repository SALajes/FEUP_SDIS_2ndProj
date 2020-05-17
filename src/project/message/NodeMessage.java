package project.message;

import java.math.BigInteger;

public class NodeMessage extends BaseMessage {
    private final BigInteger key;
    private final String address;
    private final int port;

    public NodeMessage(Message_Type type, BigInteger sender, BigInteger key, String address, int port) {
        super(type, sender);
        this.address = address;
        this.port = port;
        this.key = key;
    }

    @Override
    public String getHeader() {
        return super.getHeader() + " " + key.toString() + " " + address + " " + port;
    }

    public BigInteger getKey(){
        return key;
    }

    public String getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }
}
