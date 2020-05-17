package project.message;

import java.math.BigInteger;

public class NotifySuccessorMessage  extends BaseMessage{
    private final String address;
    private final int port;

    public NotifySuccessorMessage(BigInteger key, String address, int port) {
        super(Message_Type.NOTIFY_SUCCESSOR, key);
        this.address = address;
        this.port = port;
    }

    public String getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }
}
