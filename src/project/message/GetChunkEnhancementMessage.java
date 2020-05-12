package project.message;


public class GetChunkEnhancementMessage extends ProtocolMessage {

    private final Integer chunk_no;
    private final Integer port;
    private final String address;

    public GetChunkEnhancementMessage(int sender_id, String file_id, int chunk_no, int port, String address) {
        super(Message_Type.GETCHUNK_ENHANCED, sender_id, file_id);

        this.chunk_no = chunk_no;
        this.port = port;
        this.address = address;

    }

    public Integer getChunkNo() {
        return chunk_no;
    }

    public Integer getPort() {
        return port;
    }

    public String getAddress() {
        return address;
    }

    @Override
    public String getHeader(){
        return super.getHeader() + " " + chunk_no + " " + port + " " + address;
    }
}
