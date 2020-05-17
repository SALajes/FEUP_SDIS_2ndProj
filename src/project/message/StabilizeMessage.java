package project.message;

import java.math.BigInteger;

public class StabilizeMessage extends BaseMessage{
    public StabilizeMessage(BigInteger key) {
        super(Message_Type.STABILIZE, key);
    }
}
