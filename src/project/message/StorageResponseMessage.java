package project.message;

import java.math.BigInteger;

public class StorageResponseMessage extends BaseMessage {
    private final Integer chunk_number;
    private final String file_id;
    //if true the message is checking if the file is store, otherwise is notifying that is indeed storage
    private final boolean store;
    //can means the file exists in storage or the list was added
    private final boolean successful;

    public StorageResponseMessage(BigInteger sender, Integer chunk_number, String file_id, boolean store, boolean successful) {
        super(Message_Type.STORAGE_RESPONSE, sender);
        this.chunk_number = chunk_number;
        this.file_id = file_id;
        this.store = store;
        this.successful = successful;
    }

    public Integer getChunk_number() {
        return chunk_number;
    }

    public String getFile_id() {
        return file_id;
    }

    public boolean wasSuccessful() {
        return successful;
    }

    public boolean isStore() {
        return store;
    }
}
