package project.message;

import java.math.BigInteger;

public class RequestPredecessorMessage extends BaseMessage{
    private final String address;
    private final int port;

    public RequestPredecessorMessage( BigInteger key, String address, int port) {
        super(Message_Type.REQUEST_PREDECESSOR, key);
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
