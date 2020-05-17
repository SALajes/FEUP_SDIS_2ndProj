package project.message;

import java.math.BigInteger;

/**
 * fields common to all messages
 */
public abstract class ProtocolMessage extends BaseMessage {
    private final String file_id;

    public ProtocolMessage(Message_Type message_type, BigInteger key, String file_id) {
        super(message_type, key);
        this.file_id = file_id;
    }

    public ProtocolMessage(Message_Type message_type, String file_id) {
        super(message_type);
        this.file_id = file_id;
    }

    @Override
    public String getHeader(){
        return super.getHeader() + this.file_id;
    }

    public String getFileId() {
        return file_id;
    }
}