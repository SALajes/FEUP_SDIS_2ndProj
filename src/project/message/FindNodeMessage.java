package project.message;

import java.math.BigInteger;

public class FindNodeMessage extends BaseMessage{

    public FindNodeMessage(Message_Type type, BigInteger key) {
        super(type, key);
    }

}
