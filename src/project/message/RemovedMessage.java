package project.message;

public class RemovedMessage extends ProtocolMessage {
    private final Integer chunk_no;

    public RemovedMessage(int sender_id, String file_id, Integer chunk_no) {
        super(Message_Type.REMOVED, sender_id, file_id);

        this.chunk_no = chunk_no;
    }

    public Integer getChunkNo() {
        return chunk_no;
    }

    @Override
    public String getHeader(){
        return super.getHeader() + " " + chunk_no;
    }
}
