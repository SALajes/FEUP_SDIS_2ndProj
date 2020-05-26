package project.message;

import java.math.BigInteger;
import java.util.ArrayList;

public class NotifyStorage extends BaseMessage {
    private final ArrayList<Integer> chunk_numbers;
    private final String file_id;

    public NotifyStorage(Message_Type message_type, BigInteger sender, ArrayList<Integer> chunk_numbers, String file_id) {
        super(message_type, sender);
        this.chunk_numbers = chunk_numbers;
        this.file_id = file_id;
    }

    public String getFile_id() {
        return file_id;
    }
}
