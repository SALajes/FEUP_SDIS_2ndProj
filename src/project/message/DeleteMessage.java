package project.message;

public class DeleteMessage extends ProtocolMessage {
    public DeleteMessage(int sender_id, String file_id) {
        super(Message_Type.DELETE, sender_id, file_id);
    }

}
