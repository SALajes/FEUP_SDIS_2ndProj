package project.protocols;

import project.chunk.Chunk;
import project.message.BaseMessage;
import project.message.CancelBackupMessage;
import project.message.PutChunkMessage;
import project.message.StoredMessage;
import project.peer.ChordNode;
import project.peer.NodeInfo;
import project.peer.Peer;
import project.store.FileManager;
import project.store.FilesListing;
import project.store.Store;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class BackupProtocol  {

    //------------------------------- peer initiator  ---------------------------------------------------------------
    public static void sendPutchunk(int sender_id, int replication_degree, String file_id, ArrayList<Chunk> chunks) {

        //sends putchunks
        for (Chunk chunk : chunks) {
            PutChunkMessage putchunk = new PutChunkMessage(sender_id, file_id, chunk.chunk_no, replication_degree, chunk.content);

            String chunk_id = file_id + "_" + chunk.chunk_no;

            Store.getInstance().newBackupChunk(chunk_id, replication_degree);

            Runnable task = () -> processPutchunk(putchunk, putchunk.getReplicationDegree(), chunk_id, 0);
            Peer.scheduled_executor.execute(task);
        }
    }

    public static void processPutchunk(PutChunkMessage message, int replication_degree, String chunk_id, int tries) {
        if(tries >= 5){
            System.out.println("Putchunk failed desired replication degree: " + chunk_id);
            return;
        }

        if (Store.getInstance().checkBackupChunksOccurrences(chunk_id) >= replication_degree) {
            System.out.println("Backed up " + chunk_id + " with desired replication_degree");
            return;
        }

        //makeRequest(message, String address, Integer port)

        int try_aux = tries+1;
        long time = (long) Math.pow(2, try_aux-1);
        Runnable task = () -> processPutchunk(message, replication_degree, chunk_id, try_aux);
        Peer.scheduled_executor.schedule(task, time, TimeUnit.SECONDS);
    }

    public static CancelBackupMessage receiveStored(StoredMessage stored){
        String file_id = stored.getFileId();
        String chunk_id = file_id + "_" + stored.getChunkNo();
        int peer_id = stored.getSenderId();

        if(FilesListing.getInstance().getFileName(file_id) != null) {
            if(Store.getInstance().addBackupChunksOccurrences(chunk_id, peer_id)) {
                //condition is true is the replication degree has been accomplished
                return new CancelBackupMessage(Peer.id, stored.getFileId(), stored.getChunkNo(), stored.getSenderId());
            }
        } else {
            if(!Store.getInstance().hasReplicationDegree(chunk_id)){
                //adds to the replication degree of the stored file
                Store.getInstance().addReplicationDegree(chunk_id, peer_id);
            }
        }


        return null;
    }


    // ---------------------- Responses to Peer initiator -----------------------------------------



    public static void receivePutchunk(PutChunkMessage putchunk){

        String file_id = putchunk.getFileId();

        if(Store.getInstance().checkBackupChunksOccurrences(file_id + "_" + putchunk.getChunkNo()) != -1) {
            return;
        }

        Boolean x = FileManager.checkConditionsForSTORED(file_id, putchunk.getChunkNo(), putchunk.getChunk().length);
        if(x == null){
            Runnable task = ()-> sendStored(putchunk);
            Peer.scheduled_executor.schedule(task, new Random().nextInt(401), TimeUnit.MILLISECONDS);
        }

        if(putchunk.getReplicationDegree() > 0 ) {
            //each peer only send

            String chunk_id = file_id + "_" + putchunk.getChunkNo();

            Runnable task = () -> processPutchunk(putchunk, putchunk.getReplicationDegree() - 1, chunk_id, 0);
            Peer.scheduled_executor.execute(task);
        }

    }


    private static StoredMessage sendStored(PutChunkMessage putchunk) {
        int chunkNo = putchunk.getChunkNo();
        String fileId = putchunk.getFileId();
        String chunk_id = fileId + "_" + chunkNo ;

        if(Store.getInstance().checkAuxStoredOccurrences(chunk_id) < putchunk.getReplicationDegree()){
            FileManager.storeChunk(fileId, chunkNo, putchunk.getChunk(), putchunk.getReplicationDegree(), false);
            //Runnable task = () -> processStore(fileId, chunkNo);
           // new Thread(task).start();
            return processStore(fileId, chunkNo);

        }
        Store.getInstance().removeAuxStoredOccurrences(chunk_id);

        return null;
    }

    public static StoredMessage processStore(String fileId, int chunkNo) {
        return new StoredMessage(Peer.id, fileId,  chunkNo);
    }

    public static void receiveCancelBackup(CancelBackupMessage cancel_backup){
        if(Peer.id == cancel_backup.getReceiver_id()){
            FileManager.removeChunk(cancel_backup.getFileId(), cancel_backup.getChunkNo(), false);
        }
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
