package project.message;

import java.math.BigInteger;

public class DeleteMessage extends ProtocolMessage {
    public DeleteMessage(BigInteger key, String file_id) {
        super(Message_Type.DELETE, key, file_id);
    }

    public DeleteMessage(String file_id) {
        super(Message_Type.DELETE, file_id);
    }
}
