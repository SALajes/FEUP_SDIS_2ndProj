package project.message;

public class ConnectionResponseMessage extends BaseMessage{
    private final String key;
    private final int number_of_peers;

    public ConnectionResponseMessage(int sender_id, String key, int number_of_peers) {
        super(Message_Type.CONNECTIONRESPONSE, sender_id);
        this.key = key;
        this.number_of_peers = number_of_peers;
    }

    @Override
    public String getHeader(){
        return super.getHeader() + " " + key + " " + number_of_peers;
    }

    public int getNumberOfPeers() {
        return number_of_peers;
    }

    public String getKey() {
        return key;
    }
}
