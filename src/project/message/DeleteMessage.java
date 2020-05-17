package project.message;

import java.math.BigInteger;

public class DeleteMessage extends ProtocolMessage {
    public DeleteMessage(BigInteger sender_id, String file_id) {
        super(Message_Type.DELETE, sender_id, file_id);
    }

    public DeleteMessage(String file_id) {
        super(Message_Type.DELETE, file_id);
    }
}
