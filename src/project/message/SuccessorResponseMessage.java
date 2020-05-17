package project.message;

import java.math.BigInteger;

public class SuccessorResponseMessage extends BaseMessage{
    private final String status;

    public SuccessorResponseMessage(BigInteger key, String status) {
        super(Message_Type.SUCCESSOR_RESPONSE, key);
        this.status = status;
    }

    @Override
    public String getHeader() {
        return super.getHeader() + " " + status;
    }

    public String getStatus() {
        return status;
    }
}
