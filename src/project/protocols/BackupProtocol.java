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

import java.io.IOException;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

public class BackupProtocol  {

    //------------------------------- peer initiator  ---------------------------------------------------------------
    public static void processPutchunk(int replication_degree, String file_id, ArrayList<Chunk> chunks) {

        //sends putchunks
        for (Chunk chunk : chunks) {
            PutChunkMessage putchunk = new PutChunkMessage(file_id, chunk.chunk_no, replication_degree, chunk.content);

            String chunk_id = file_id + "_" + chunk.chunk_no;

            Store.getInstance().newBackupChunk(chunk_id, replication_degree);

            Runnable task = () -> intermediateProcessPutchunk(putchunk);
            Peer.thread_executor.execute(task);
        }
    }

    public static void intermediateProcessPutchunk(PutChunkMessage message) {
        for(int i = 1; i <= message.getReplicationDegree(); i++){
            int rep_degree = i;

            Peer.thread_executor.execute(()->sendPutchunk(message, rep_degree));
        }
    }

    public static void sendPutchunk(PutChunkMessage message, int rep_degree) {
        for(int tries = 0; tries < 5; tries++) {
            try {
                NodeInfo nodeInfo = getBackupPeer(message.getFileId(), message.getChunkNo(), rep_degree, tries);

                if(nodeInfo == null)
                    continue;
                message.setSender(nodeInfo.key);

                StoredMessage stored = (StoredMessage) ChordNode.makeRequest(message, nodeInfo.address, nodeInfo.port);
                if(stored.getStatus().equals(Macros.FAIL))
                    continue;
                else {
                    Peer.thread_executor.execute(()->receiveStored(stored));
                    return;
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        System.out.println("Could not backup chunk " + message.getFileId() + "_" + message.getChunkNo() + " (" + rep_degree + ")");
    }

    public static void receiveStored(StoredMessage stored){
        System.out.println("RECEIVE STORED: " + new String(stored.convertMessage()));
        String file_id = stored.getFileId();
        String chunk_id = file_id + "_" + stored.getChunkNo();
        BigInteger key = stored.getSender();
        System.out.println("STORED HAS: " + chunk_id + " " + key);

        Store.getInstance().addBackupChunksOccurrences(chunk_id, key);
    }

    // ---------------------- Responses to Peer initiator -----------------------------------------
    public static BaseMessage receivePutchunk(PutChunkMessage putchunk){

        String file_id = putchunk.getFileId();

        if(Store.getInstance().checkBackupChunksOccurrences(file_id + "_" + putchunk.getChunkNo()) != -1) {
            return sendStored(putchunk, Macros.FAIL);
        }

        Boolean x = FileManager.checkConditionsForSTORED(file_id, putchunk.getChunkNo(), putchunk.getChunk().length);
        if(x == null){
            return sendStored(putchunk, Macros.SUCCESS);
        }
        else return sendStored(putchunk, Macros.FAIL);
    }

    private static StoredMessage sendStored(PutChunkMessage putchunk, String status) {
        int chunkNo = putchunk.getChunkNo();
        String fileId = putchunk.getFileId();

        FileManager.storeChunk(fileId, chunkNo, putchunk.getChunk(), putchunk.getReplicationDegree(), false);
        return processStore(putchunk.getSender(), fileId, chunkNo, status);

    }

    public static StoredMessage processStore(BigInteger key, String fileId, int chunkNo, String status) {
        return new StoredMessage(key, fileId,  chunkNo, status);
    }


    //----------------------------------------------
    public static NodeInfo getBackupPeer(String file_id, int chunk_no, int rep_degree, int n_try){
        try {
            BigInteger key = ChordNode.generateKey(file_id + ":" + chunk_no + ":" + rep_degree + ":" + Peer.id + ":" + n_try);
            return ChordNode.findSuccessor(key);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }
}
