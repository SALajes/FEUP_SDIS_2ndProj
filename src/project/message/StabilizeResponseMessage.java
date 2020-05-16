package project.message;

import java.math.BigInteger;

public class StabilizeResponseMessage extends BaseMessage {
    private final BigInteger key;
    private final String address;
    private final int port;
    private final String status;

    public StabilizeResponseMessage(int sender_id, String status, BigInteger key, String address, int port) {
        super(Message_Type.STABILIZE_RESPONSE, sender_id);
        this.status = status;
        this.key = key;
        this.address = address;
        this.port = port;
    }

    @Override
    public String getHeader() {
        return super.getHeader() + " " + status + " " + key + " " + address + " " + port;
    }

    public BigInteger getKey() {
        return key;
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
