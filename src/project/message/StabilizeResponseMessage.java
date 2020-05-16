package project.message;

import java.math.BigInteger;

public class StabilizeResponseMessage extends BaseMessage {
    private final BigInteger key;
    private final String address;
    private final int port;
    private final String status;
    private final int num_peers;

    public StabilizeResponseMessage(int sender_id, String status, BigInteger key, String address, int port, int num_peers) {
        super(Message_Type.STABILIZE_RESPONSE, sender_id);
        this.status = status;
        this.key = key;
        this.address = address;
        this.port = port;
        this.num_peers = num_peers;
    }

    @Override
    public String getHeader() {
        return super.getHeader() + " " + status + " " + key + " " + address + " " + port + " " + num_peers;
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

    public int getPeers() {
        return num_peers;
    }
}
