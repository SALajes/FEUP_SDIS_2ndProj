package project.message;

public class DeleteReceivedMessage extends BaseMessage {
    public DeleteReceivedMessage(double version, int sender_id, String file_id) {
        super(version, Message_Type.DELETERECEIVED, sender_id, file_id);
    }
}
