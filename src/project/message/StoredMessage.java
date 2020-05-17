package project.message;

import java.math.BigInteger;

public class StoredMessage extends ProtocolMessage {
    private final int chunk_no;

    public StoredMessage(BigInteger sender, String file_id, int chunk_no) {
        super(Message_Type.STORED, sender, file_id);

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
