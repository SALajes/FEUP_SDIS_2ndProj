package project.message;

public enum Message_Type {
    PUTCHUNK,
    STORED,
    GETCHUNK,
    CHUNK,
    DELETE,
    REMOVED,
    REMOVED_RECEIVED,
    DELETE_RECEIVED,
    CONNECTION_REQUEST,
    CONNECTION_RESPONSE,
    REQUEST_PREDECESSOR,
    PREDECESSOR_RESPONSE,
    SUCCESSOR,
    FIND_SUCCESSOR,
    FIND_PREDECESSOR,
    PREDECESSOR,
    SUCCESSOR_RESPONSE,
    NOTIFY_SUCCESSOR, STABILIZE, STABILIZE_RESPONSE, DISCONNECT, MOCK
}