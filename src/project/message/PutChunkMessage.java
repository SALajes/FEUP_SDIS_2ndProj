package project.message;

public class PutChunkMessage extends ProtocolMessage {
    private final int chunk_no;
    private final int replication_degree;

    public PutChunkMessage(int sender_id, String file_id, int chunk_no, int replication_degree, byte[] chunk) {
        super(Message_Type.PUTCHUNK, sender_id, file_id);

        this.chunk_no = chunk_no;
        //replication degree of the chunk  is a digit, thus allowing a replication degree of up to 9. It takes one byte, which is the ASCII code of that digit.

        this.replication_degree = replication_degree;

        this.chunk = chunk;
    }

    public int getChunkNo(){
        return chunk_no;
    }
    public int getReplicationDegree(){
        return replication_degree;
    }

    @Override
    public String getHeader(){
        return super.getHeader() + " " + chunk_no + " " + replication_degree;
    }
}
