package project.protocols;

import project.Macros;
import project.chunk.Chunk;
import project.message.BaseMessage;
import project.message.PutChunkMessage;
import project.message.StoredMessage;
import project.peer.ChordNode;
import project.peer.NodeInfo;
import project.peer.Peer;
import project.store.FileManager;
import project.store.Store;

import java.awt.desktop.SystemSleepEvent;
import java.io.IOException;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class BackupProtocol  {

    //------------------------------- peer initiator  ---------------------------------------------------------------
    public static void processPutchunk(int replication_degree, String file_id, ArrayList<Chunk> chunks) {

        //sends putchunks
        for (Chunk chunk : chunks) {
            PutChunkMessage putchunk = new PutChunkMessage(file_id, chunk.chunk_no, replication_degree, chunk.content);

            String chunk_id = file_id + "_" + chunk.chunk_no;

            Store.getInstance().newBackupChunk(chunk_id, replication_degree);

            Runnable task = () -> intermediateProcessPutchunk(putchunk, replication_degree);
            Peer.thread_executor.execute(task);
        }
    }

    public static void intermediateProcessPutchunk(PutChunkMessage message, int rep_degree) {
        if(rep_degree > 0){
            Peer.thread_executor.execute(() -> sendPutchunk(message, rep_degree));

            int i = rep_degree - 1;
            Runnable task = () -> intermediateProcessPutchunk(message, i);
            Peer.scheduled_executor.schedule(task, 400, TimeUnit.MILLISECONDS);
            //TODO WRAP CRITICAL PATH IN SYNCHRONIZED
        }
    }

    public static StoredMessage sendPutchunk(PutChunkMessage message, int rep_degree) {
        for(int tries = 0; tries < 5; tries++) {
            try {
                NodeInfo nodeInfo = getBackupPeer(message.getFileId(), message.getChunkNo(), rep_degree, tries);
                System.out.println("GOT KEY: " + nodeInfo.key);

                if(nodeInfo == null)
                    continue;
                message.setSender(nodeInfo.key);

                StoredMessage stored = (StoredMessage) ChordNode.makeRequest(message, nodeInfo.address, nodeInfo.port);

                if(stored.getStatus().equals(Macros.FAIL))
                    continue;
                else {
                    receiveStored(stored);
                    return stored;
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        System.out.println("Could not backup chunk " + message.getFileId() + "_" + message.getChunkNo() + " (" + rep_degree + ")");
        return null;
    }

    public static void receiveStored(StoredMessage stored){
        String chunk_id = stored.getFileId() + "_" + stored.getChunkNo();
        BigInteger key = stored.getSender();

        Store.getInstance().addBackupChunksOccurrences(chunk_id, key);
    }

    // ---------------------- Responses to Peer initiator -----------------------------------------
    public static BaseMessage receivePutchunk(PutChunkMessage putchunk){

        String file_id = putchunk.getFileId();

        //check if this is this peer with a file
        if(Store.getInstance().checkBackupChunksOccurrences(file_id + "_" + putchunk.getChunkNo()) != -1) {
            return sendStored(putchunk, Macros.FAIL, ChordNode.this_node.key);
        }

        Boolean x = FileManager.checkConditionsForSTORED(file_id, putchunk.getChunkNo(), putchunk.getChunk().length);
        if(x == null){
            return sendStored(putchunk, Macros.SUCCESS, ChordNode.this_node.key);
        }
        else return sendStored(putchunk, Macros.FAIL, ChordNode.this_node.key);
    }

    private static StoredMessage sendStored(PutChunkMessage putchunk, String status, BigInteger key) {
        int chunkNo = putchunk.getChunkNo();
        String fileId = putchunk.getFileId();

        FileManager.storeChunk(fileId, chunkNo, putchunk.getChunk(), putchunk.getReplicationDegree(), false, key);
        StoredMessage message = new StoredMessage(ChordNode.this_node.key, fileId,  chunkNo, status);
        return message;
    }

    //----------------------------------------------
    public static NodeInfo getBackupPeer(String file_id, int chunk_no, int rep_degree, int n_try){
        try {
            BigInteger key = ChordNode.generateKey(file_id + ":" + chunk_no + ":" + rep_degree + ":" + Peer.id + ":" + n_try);
            System.out.println("looking  for key: " + key.toString());
            return ChordNode.findSuccessor(key);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }
}
