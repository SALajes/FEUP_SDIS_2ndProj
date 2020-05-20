package project.protocols;

import project.chunk.Chunk;
import project.message.*;
import project.peer.ChordNode;
import project.peer.NodeInfo;
import project.peer.Peer;
import project.store.FileManager;
import project.store.Store;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class ReclaimProtocol {
    private static int crashed = 0;

    // --------------------- peer initiator ------------------------------------------
    public static void sendRemoved( String file_id, Integer chunk_number) {
        String chunk_id = file_id + "_" + chunk_number;

        //finds a peer to keep the chunks
        Chunk chunk = FileManager.retrieveChunk(file_id, chunk_number);

        if (chunk != null) {
            Runnable task = () -> sendPutchunk(ChordNode.this_node.key, Store.getInstance().getReplicationDegree(chunk_id), file_id, chunk);

            //initiate the chunk backup subprotocol after a random delay uniformly distributed between 0 and 400 ms
            Peer.scheduled_executor.schedule(task, new Random().nextInt(401), TimeUnit.MILLISECONDS);
        }

        //sends remove with is own key
        RemovedMessage removedMessage = new RemovedMessage(ChordNode.this_node.key, file_id, chunk_number);
        Runnable task = () -> processRemoveMessage(removedMessage);
        new Thread(task).start();

    }

    public static void processRemoveMessage(RemovedMessage message) {
        //sends to the peer initiator
        BigInteger chunk_owner = Store.getInstance().getKeyOfStoredChunk(message.getFileId());
        NodeInfo nodeInfo = ChordNode.findSuccessor(chunk_owner);
        if(chunk_owner.equals(nodeInfo.key)) {
            try {
                ChordNode.makeRequest(message, nodeInfo.address, nodeInfo.port);
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    // -------------- peer not initiator

    public static BaseMessage receiveRemoved(RemovedMessage removedMessage) {

        String file_id = removedMessage.getFileId();
        Integer chunk_number = removedMessage.getChunkNo();
        String chunk_id = file_id + "_" + chunk_number;

        //update local count of this chunk replication degree
        Store.getInstance().removeBackupChunkOccurrence(chunk_id, removedMessage.getSender());
        //TODO: Change to new key
        Store.getInstance().addBackupChunksOccurrences(chunk_id, removedMessage.getSender());

        return new BaseMessage(Message_Type.REMOVED_RECEIVED, ChordNode.this_node.key);
    }

    // -- initiate another protocol
    public static StoredMessage sendPutchunk(BigInteger key, int replication_degree, String file_id, Chunk chunk) {
        //send put chunk

        PutChunkMessage putchunk = new PutChunkMessage(key, file_id, chunk.chunk_no, replication_degree, chunk.content);

        String chunk_id = file_id + "_" + chunk.chunk_no;

        crashed = 0;

        for(int j = 0; j < 5; j++) {
            int finalJ = j;
            Runnable task = () -> {
                if(finalJ == 0)
                    BackupProtocol.sendPutchunk(putchunk, putchunk.getReplicationDegree());
                else {
                    BackupProtocol.sendPutchunk(putchunk, putchunk.getReplicationDegree() + crashed);
                    crashed++;
                }
                return;
            };
            Peer.thread_executor.execute(task);

        }

        return null;
    }

}

