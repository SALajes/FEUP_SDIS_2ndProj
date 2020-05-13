package project.message;

import project.protocols.BackupProtocol;
import project.protocols.DeleteProtocol;
import project.protocols.ReclaimProtocol;
import project.protocols.RestoreProtocol;

import static project.protocols.DeleteProtocol.receiveDeleteReceived;

public class MessageHandler {
    public static BaseMessage handleMessage(byte[] raw_message){
        try {
            BaseMessage message = MessageParser.parseMessage(raw_message, raw_message.length);

            switch (message.getMessageType()) {
                case STORED:
                    //response to Putchunk
                    return BackupProtocol.receiveStored((StoredMessage) message);
                case GETCHUNK:
                    RestoreProtocol.receiveGetChunk((GetChunkMessage) message);
                    break;
                case DELETE:
                    return DeleteProtocol.receiveDelete((DeleteMessage) message);
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
                    //response to getchunk
                    RestoreProtocol.receiveChunk((ChunkMessage) message);
                    break;
                case CONNECTIONREQUEST:
                    //TODO parse conection request
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
