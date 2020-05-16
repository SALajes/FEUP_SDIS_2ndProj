package project.protocols;

import project.chunk.Chunk;
import project.message.PutChunkMessage;
import project.message.RemovedMessage;
import project.peer.ChordNode;
import project.peer.NodeInfo;
import project.peer.Peer;
import project.store.FileManager;
import project.store.Store;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class ReclaimProtocol {
    private static int crashed = 0;

    // --------------------- peer initiator
    public static void sendRemoved(Integer sender_id, String file_id, Integer chunk_number) {
        RemovedMessage removedMessage = new RemovedMessage(sender_id, file_id, chunk_number);
        Runnable task = () -> processRemoveMessage(removedMessage);
        new Thread(task).start();

    }

    public static void processRemoveMessage(RemovedMessage message) {
        NodeInfo nodeInfo = ChordNode.findPredecessor(ChordNode.this_node.key);
        try {
            ChordNode.makeRequest(message, nodeInfo.address, nodeInfo.port);
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    // -------------- peer not initiator

    public static void receiveRemoved(RemovedMessage removedMessage) {

        String file_id = removedMessage.getFileId();
        Integer chunk_number = removedMessage.getChunkNo();
        String chunk_id = file_id + "_" + chunk_number;

        //check if this is this peer with a file
        if (Store.getInstance().checkBackupChunksOccurrences(chunk_id) != -1) {

            //update local count of this chunk replication degree
            Store.getInstance().removeBackupChunkOccurrence(chunk_id, removedMessage.getSenderId());

        } else if (Store.getInstance().checkStoredChunksOccurrences(chunk_id) != -1) {

            Store.getInstance().removeStoredChunkOccurrence(chunk_id, removedMessage.getSenderId());

            //check if count drops below the desired replication degree of that chunk
            if (!Store.getInstance().hasReplicationDegree(chunk_id)) {

                Chunk chunk = FileManager.retrieveChunk(file_id, chunk_number);

                if (chunk != null) {
                    Runnable task = () -> sendPutchunk(Peer.id, Store.getInstance().getReplicationDegree(chunk_id), file_id, chunk);

                    //initiate the chunk backup subprotocol after a random delay uniformly distributed between 0 and 400 ms
                    Peer.scheduled_executor.schedule(task, new Random().nextInt(401), TimeUnit.MILLISECONDS);
                }
            }
        }
    }

    // -- initiate another protocol
    public static void sendPutchunk(int sender_id, int replication_degree, String file_id, Chunk chunk) {
        //send put chunk

        PutChunkMessage putchunk = new PutChunkMessage(sender_id, file_id, chunk.chunk_no, replication_degree, chunk.content);

        String chunk_id = file_id + "_" + chunk.chunk_no;

        crashed = 0;

        for(int j = 0; j < 5; j++) {
            int finalJ = j;
            Runnable task = () -> {
                try {
                    if(finalJ == 0)
                        BackupProtocol.sendPutchunk(putchunk, putchunk.getReplicationDegree());
                    else {
                        BackupProtocol.sendPutchunk(putchunk, putchunk.getReplicationDegree() + crashed);
                        crashed++;
                    }
                    return;
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
            };
            Peer.thread_executor.execute(task);

        }

    }

}

