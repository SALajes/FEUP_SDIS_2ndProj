package project.message;

import java.math.BigInteger;

public class ConnectionRequestMessage extends BaseMessage {
    private final BigInteger key;
    private final String address;
    private final int port;

    public ConnectionRequestMessage(BigInteger sender, BigInteger key, String address, int port) {
        super(Message_Type.CONNECTION_REQUEST, sender);
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
