package project.message;

import project.protocols.*;

public class MessageHandler {
    public static BaseMessage handleMessage(byte[] raw_message){
        try {
            BaseMessage message = MessageParser.parseMessage(raw_message, raw_message.length);
            switch (message.getMessageType()) {
                case STORED:
                    BackupProtocol.receiveStored((StoredMessage) message);
                    break;
                case GETCHUNK:
                    RestoreProtocol.receiveGetChunk((GetChunkMessage) message);
                    break;
                case DELETE:
                    return DeleteProtocol.receiveDelete((DeleteMessage) message);
                case DELETE_RECEIVED:
                    DeleteProtocol.receiveDeleteReceived((DeleteReceivedMessage) message);
                    break;
                case REMOVED:
                    ReclaimProtocol.receiveRemoved((RemovedMessage) message);
                    break;
                case PUTCHUNK:
                    return BackupProtocol.receivePutchunk((PutChunkMessage) message);
                case CHUNK:
                    RestoreProtocol.receiveChunk((ChunkMessage) message);
                    break;
                case CONNECTION_REQUEST:
                    return ConnectionProtocol.receiveRequest((ConnectionRequestMessage) message);
                case REQUEST_PREDECESSOR:
                    return ConnectionProtocol.receiveRequestPredecessor((RequestPredecessorMessage) message);
                case FIND_PREDECESSOR:
                    return ConnectionProtocol.receiveFindPredecessor((FindNodeMessage) message);
                case FIND_SUCCESSOR:
                    return ConnectionProtocol.receiveFindSuccessor((FindNodeMessage) message);
                case NOTIFY_SUCCESSOR:
                    return ConnectionProtocol.receiveNotifySuccessor((NotifySuccessorMessage) message);
                case STABILIZE:
                    return ConnectionProtocol.receivedStabilize((StabilizeMessage) message);
                case STABILIZE_RESPONSE:
                case SUCCESSOR_RESPONSE:
                case SUCCESSOR:
                case PREDECESSOR:
                case CONNECTION_RESPONSE:
                case PREDECESSOR_RESPONSE:
                    return message;
                default:
                    break;
            }
        } catch (InvalidMessageException e) {
            System.out.println(e.getMessage());
        }
        return null;
    }
}
