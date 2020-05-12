package project.message;

public class PredecessorResponseMessage extends BaseMessage{

    public PredecessorResponseMessage(int sender_id, byte[] finger_table) {
        super(Message_Type.PREDECESSOR_RESPONSE, sender_id);
        this.chunk = finger_table;
    }
}
