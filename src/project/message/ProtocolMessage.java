package project.message;

import project.Macros;

/**
 * fields common to all messages
 */
public abstract class ProtocolMessage extends BaseMessage {
    private final String file_id;

    public ProtocolMessage(Message_Type message_type, int sender_id, String file_id) {
        super(message_type, sender_id);
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