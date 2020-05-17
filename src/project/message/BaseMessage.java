package project.message;

import project.Macros;

import java.math.BigInteger;

public class BaseMessage {
    private final Message_Type message_type;
    private BigInteger key;
    protected byte[] chunk;

    public BaseMessage(Message_Type message_type, BigInteger sender_id) {
        this.message_type = message_type;
        this.key = sender_id;
        this.chunk = null;
    }

    public BaseMessage(Message_Type message_type) {
        this.message_type = message_type;
        this.chunk = null;
    }

    public String getHeader(){
        return this.message_type + " " + this.key;
    }

    public byte[] convertMessage(){
        String header = getHeader() + " " + ((char) Macros.CR) + ((char)Macros.LF) + ((char)Macros.CR) + ((char)Macros.LF);

        if(this.chunk == null)
            return header.getBytes();
        else{
            byte[] header_bytes = header.getBytes();
            byte[] message = new byte[this.chunk.length + header_bytes.length];

            System.arraycopy(header_bytes, 0, message, 0, header_bytes.length);
            System.arraycopy(this.chunk, 0, message, header_bytes.length, this.chunk.length);
            return message;
        }
    }

    public Message_Type getMessageType() {
        return message_type;
    }

    public BigInteger getKey() {
        return key;
    }

    public void putKey(BigInteger key) {
        this.key = key;
    }

    public byte[] getChunk(){ return chunk; }
}
