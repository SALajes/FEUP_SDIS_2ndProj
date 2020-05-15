package project.message;

public class SuccessorResponseMessage extends BaseMessage{
    private final String status;

    public SuccessorResponseMessage(int sender_id, String status) {
        super(Message_Type.SUCCESSOR_RESPONSE, sender_id);
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
