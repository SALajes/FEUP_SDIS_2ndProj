package project.message;

import java.math.BigInteger;

public class StabilizeResponseMessage extends BaseMessage {
    private final String address;
    private final int port;
    private final String status;

    public StabilizeResponseMessage(BigInteger sender, String status, BigInteger key, String address, int port) {
        super(Message_Type.STABILIZE_RESPONSE, sender);
        this.status = status;
        this.address = address;
        this.port = port;
    }

    @Override
    public String getHeader() {
        return super.getHeader() + " " + status + " " + address + " " + port;
    }

    public String getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }

    public String getStatus() {
        return status;
    }
}
