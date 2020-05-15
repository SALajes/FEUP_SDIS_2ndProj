package project.message;

public class SuccessorResponseMessage extends BaseMessage{

    public SuccessorResponseMessage(int sender_id) {
        super(Message_Type.SUCCESSOR_RESPONSE, sender_id);
    }
}
