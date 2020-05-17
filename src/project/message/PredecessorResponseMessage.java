package project.message;

import java.math.BigInteger;

public class PredecessorResponseMessage extends BaseMessage{

    public PredecessorResponseMessage(BigInteger key, byte[] finger_table) {
        super(Message_Type.PREDECESSOR_RESPONSE, key);
        this.chunk = finger_table;
    }
}
