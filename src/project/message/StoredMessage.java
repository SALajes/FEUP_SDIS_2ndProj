package project.message;

public class StoredMessage extends BaseMessage {
    private final int chunk_no;

    public StoredMessage(double version, int sender_id, String file_id, int chunk_no) {
        super(version, Message_Type.STORED, sender_id, file_id);

        this.chunk_no = chunk_no;
    }

    @Override
    public String getHeader(){
        return super.getHeader() + " " + chunk_no;
    }

    public int getChunkNo(){
        return chunk_no;
    }
}
