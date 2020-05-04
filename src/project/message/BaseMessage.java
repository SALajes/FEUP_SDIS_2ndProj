package project.message;

import project.Macros;

/**
 * fields common to all messages
 */
public abstract class BaseMessage {
    //Header
    private final double version;
    private final Message_Type message_type;
    private final int sender_id;
    private final String file_id;
    protected byte[] chunk;

    public BaseMessage(double version, Message_Type message_type, int sender_id, String file_id) {
        this.version = version;
        this.message_type = message_type;
        this.sender_id = sender_id;
        this.file_id = file_id;
        this.chunk = null;
    }

    public String getHeader(){
        return this.version + " " + this.message_type + " " + this.sender_id + " " + this.file_id;
    }

    public byte[] convertMessage(){
        String header = getHeader() + " " + ((char)Macros.CR) + ((char)Macros.LF) + ((char)Macros.CR) + ((char)Macros.LF);

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


    public double getVersion() {
        return version;
    }

    public Message_Type getMessageType() {
        return message_type;
    }

    public int getSenderId() {
        return sender_id;
    }

    public String getFileId() {
        return file_id;
    }

    public byte[] getChunk(){ return chunk; }
}