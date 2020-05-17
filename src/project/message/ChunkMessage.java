package project.message;

import java.math.BigInteger;

public class ChunkMessage extends ProtocolMessage {
    private final Integer chunk_no;

    public ChunkMessage(BigInteger sender, String file_id, Integer chunk_no, byte[] chunk) {
        super(Message_Type.CHUNK, sender, file_id);

        this.chunk_no = chunk_no;
        this.chunk = chunk;
    }

    public Integer getChunkNo() {
        return chunk_no;
    }

    @Override
    public String getHeader(){
        return super.getHeader() + " " + chunk_no;
    }
}
