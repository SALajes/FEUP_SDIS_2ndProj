package project.message;

import java.math.BigInteger;

public class DeleteReceivedMessage extends ProtocolMessage {
        public DeleteReceivedMessage(BigInteger sender_id, String file_id) {
        super(Message_Type.DELETE_RECEIVED, sender_id, file_id);
    }
}
