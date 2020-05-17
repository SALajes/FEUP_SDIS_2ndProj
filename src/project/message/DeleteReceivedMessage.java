package project.message;

import java.math.BigInteger;

public class DeleteReceivedMessage extends ProtocolMessage {
    public DeleteReceivedMessage(BigInteger key, String file_id) {
        super(Message_Type.DELETE_RECEIVED, key, file_id);
    }
}
