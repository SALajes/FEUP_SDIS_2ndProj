package project.protocols;

import project.chunk.Chunk;
import project.message.BaseMessage;
import project.message.PutChunkMessage;
import project.message.StoredMessage;
import project.peer.ChordNode;
import project.peer.NodeInfo;
import project.peer.Peer;
import project.store.FileManager;
import project.store.FilesListing;
import project.store.Store;

import java.io.IOException;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

public class BackupProtocol  {
    private static int crashed = 0;

    //------------------------------- peer initiator  ---------------------------------------------------------------
    public static void processPutchunk(int sender_id, int replication_degree, String file_id, ArrayList<Chunk> chunks) {

        //sends putchunks
        for (Chunk chunk : chunks) {
            PutChunkMessage putchunk = new PutChunkMessage(sender_id, file_id, chunk.chunk_no, replication_degree, chunk.content);

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
            int n_try = tries;
            try {
                NodeInfo nodeInfo = getBackupPeer(message.getFileId(), message.getChunkNo(), rep_degree, n_try);
                StoredMessage stored = (StoredMessage) ChordNode.makeRequest(message, nodeInfo.address, nodeInfo.port);
                Peer.thread_executor.execute(()->receiveStored(stored));
                return;
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        System.out.println("Could not backup chunk " + message.getFileId() + "_" + message.getChunkNo() + " (" + rep_degree + ")");
    }

    public static void receiveStored(StoredMessage stored){
        String file_id = stored.getFileId();
        String chunk_id = file_id + "_" + stored.getChunkNo();
        int peer_id = stored.getSenderId();

        if(FilesListing.getInstance().getFileName(file_id) != null) {
            if(Store.getInstance().addBackupChunksOccurrences(chunk_id, peer_id)) {
                //condition is true is the replication degree has been accomplished
              //  return new CancelBackupMessage(Peer.id, stored.getFileId(), stored.getChunkNo(), stored.getSenderId());
            }
        } else {
            if(!Store.getInstance().hasReplicationDegree(chunk_id)){
                //adds to the replication degree of the stored file
                Store.getInstance().addReplicationDegree(chunk_id, peer_id);
            }
        }

    }

    // ---------------------- Responses to Peer initiator -----------------------------------------
    public static BaseMessage receivePutchunk(PutChunkMessage putchunk){

        String file_id = putchunk.getFileId();

        if(Store.getInstance().checkBackupChunksOccurrences(file_id + "_" + putchunk.getChunkNo()) != -1) {
            return null;
        }

        Boolean x = FileManager.checkConditionsForSTORED(file_id, putchunk.getChunkNo(), putchunk.getChunk().length);
        if(x == null){
            return sendStored(putchunk);
        }
        else{
            //caso ele já tenha guardado o chunk ou nao tenha espaço, temos de pedir ao seu sucessor para a guardar
            NodeInfo nodeInfo = ChordNode.findPredecessor(ChordNode.this_node.key);
            try {
                ChordNode.makeRequest(putchunk, nodeInfo.address, nodeInfo.port);
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    private static StoredMessage sendStored(PutChunkMessage putchunk) {
        int chunkNo = putchunk.getChunkNo();
        String fileId = putchunk.getFileId();
        String chunk_id = fileId + "_" + chunkNo ;

        FileManager.storeChunk(fileId, chunkNo, putchunk.getChunk(), putchunk.getReplicationDegree(), false);
        return processStore(fileId, chunkNo);

    }

    public static StoredMessage processStore(String fileId, int chunkNo) {
        return new StoredMessage(Peer.id, fileId,  chunkNo);
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
