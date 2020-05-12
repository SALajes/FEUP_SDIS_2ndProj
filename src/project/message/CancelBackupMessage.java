package project.message;

public class CancelBackupMessage extends ProtocolMessage {
    private final int receiver_id;
    private final int chunk_no;

    public CancelBackupMessage(int sender_id, String file_id, int chunk_no, int receiver_id) {
        super(Message_Type.CANCEL_BACKUP, sender_id, file_id);
        this.receiver_id = receiver_id;
        this.chunk_no = chunk_no;
    }

    @Override
    public String getHeader(){
        return super.getHeader() + " " + chunk_no + " " + receiver_id;
    }

    public int getReceiver_id() {
        return receiver_id;
    }

    public int getChunkNo() {
        return chunk_no;
    }
}
