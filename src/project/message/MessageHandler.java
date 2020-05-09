package project.message;

import project.protocols.BackupProtocol;
import project.protocols.DeleteProtocol;
import project.protocols.ReclaimProtocol;
import project.protocols.RestoreProtocol;

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
                case GETCHUNKENHANCED:
                    RestoreProtocol.receiveGetChunkEnhancement((GetChunkEnhancementMessage) message);
                case DELETE:
                    DeleteProtocol.receiveDelete((DeleteMessage) message);
                    break;
                case DELETERECEIVED:
                    DeleteProtocol.receiveDeleteReceived((DeleteReceivedMessage) message);
                    break;
                case REMOVED:
                    ReclaimProtocol.receiveRemoved((RemovedMessage) message);
                    break;
                case PUTCHUNK:
                    BackupProtocol.receivePutchunk((PutChunkMessage) message);
                    break;
                case CANCELBACKUP:
                    BackupProtocol.receiveCancelBackup((CancelBackupMessage) message);
                    break;
                case CHUNK:
                    RestoreProtocol.receiveChunk((ChunkMessage) message);
                    break;
                default:
                    break;
            }
        } catch (InvalidMessageException e) {
            System.out.println(e.getMessage());
        }
        return null;
    }
}
