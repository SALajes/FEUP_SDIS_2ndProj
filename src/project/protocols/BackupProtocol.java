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
            crashed = 0;

            for(int j = 0; j < 5; j++) {
                int finalJ = j;
                Runnable task = () -> {
                    try {
                        if(finalJ == 0)
                            sendPutchunk(message, message.getReplicationDegree());
                        else {
                            sendPutchunk(message, message.getReplicationDegree() + crashed);
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

    public static void sendPutchunk(PutChunkMessage message, int rep_degree) throws IOException, ClassNotFoundException {

        NodeInfo nodeInfo = getBackupPeer(message.getFileId(), message.getChunkNo(), rep_degree);
        ChordNode.makeRequest(message, nodeInfo.address, nodeInfo.port);
        //throws exception when peer being access is down

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
    public static NodeInfo getBackupPeer(String file_id, int chunk_no, int rep_degree){
        try {
            BigInteger key = ChordNode.generateKey(file_id + ":" + chunk_no + ":" + rep_degree + ":" + Peer.id);
            return ChordNode.findSuccessor(key);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }
}
