package project.message;

public class RemovedMessage extends BaseMessage {
    private final Integer chunk_no;

    public RemovedMessage(double version, int sender_id, String file_id, Integer chunk_no) {
        super(version, Message_Type.REMOVED, sender_id, file_id);

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
