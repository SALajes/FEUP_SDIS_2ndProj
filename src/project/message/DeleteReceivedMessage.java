package project.message;

public class DeleteReceivedMessage extends ProtocolMessage {
    public DeleteReceivedMessage(int sender_id, String file_id) {
        super(Message_Type.DELETE_RECEIVED, sender_id, file_id);
    }
}
