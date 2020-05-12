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
                case GETCHUNK_ENHANCED:
                    RestoreProtocol.receiveGetChunkEnhancement((GetChunkEnhancementMessage) message);
                case DELETE:
                    DeleteProtocol.receiveDelete((DeleteMessage) message);
                    break;
                case DELETE_RECEIVED:
                    DeleteProtocol.receiveDeleteReceived((DeleteReceivedMessage) message);
                    break;
                case REMOVED:
                    ReclaimProtocol.receiveRemoved((RemovedMessage) message);
                    break;
                case PUTCHUNK:
                    BackupProtocol.receivePutchunk((PutChunkMessage) message);
                    break;
                case CANCEL_BACKUP:
                    BackupProtocol.receiveCancelBackup((CancelBackupMessage) message);
                    break;
                case CHUNK:
                    RestoreProtocol.receiveChunk((ChunkMessage) message);
                    break;
                case CONNECTION_REQUEST:
                    return ConnectionProtocol.receiveRequest((ConnectionRequestMessage) message);
                case CONNECTION_RESPONSE:
                    return message;
                case REQUEST_PREDECESSOR:
                    return ConnectionProtocol.receiveRequestPredecessor((RequestPredecessorMessage) message);
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
