package project.message;

import java.math.BigInteger;

public class NodeMessage extends BaseMessage {
    private final String address;
    private final int port;

    public NodeMessage(Message_Type type, BigInteger key, String address, int port) {
        super(type, key);
        this.address = address;
        this.port = port;
    }

    @Override
    public String getHeader() {
        return super.getHeader() + " " + address + " " + port;
    }

    public String getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }
}
