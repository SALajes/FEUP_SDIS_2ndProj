package project.message;

import java.math.BigInteger;

public class RemovedMessage extends ProtocolMessage {
    private final Integer chunk_no;

    public RemovedMessage(BigInteger key, String file_id, Integer chunk_no) {
        super(Message_Type.REMOVED, key, file_id);

        this.chunk_no = chunk_no;
    }

    public RemovedMessage(String file_id, Integer chunk_number) {
        super(Message_Type.REMOVED, file_id);
        this.chunk_no = chunk_number;
    }

    public Integer getChunkNo() {
        return chunk_no;
    }

    @Override
    public String getHeader(){
        return super.getHeader() + " " + chunk_no;
    }
}
