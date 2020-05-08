package project.message;

public class ConnectionRequestMessage extends BaseMessage {
    private final String address;
    private final int port;

    public ConnectionRequestMessage(int sender_id, String address, int port) {
        super(Message_Type.CONNECTIONREQUEST, sender_id);
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
