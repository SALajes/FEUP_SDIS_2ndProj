package project.protocols;

import project.Macros;
import project.chunk.Chunk;
import project.message.CancelBackupMessage;
import project.message.PutChunkMessage;
import project.message.StoredMessage;
import project.peer.Peer;
import project.store.FileManager;
import project.store.FilesListing;
import project.store.Store;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class BackupProtocol {
    public static void sendPutchunk(double version, int sender_id, int replication_degree, String file_id, ArrayList<Chunk> chunks) {
        //sends putchunks
        for (Chunk chunk : chunks) {
            PutChunkMessage putchunk = new PutChunkMessage(version, sender_id, file_id, chunk.chunk_no, replication_degree, chunk.content);

            String chunk_id = file_id + "_" + chunk.chunk_no;

            Store.getInstance().newBackupChunk(chunk_id, replication_degree);

            Runnable task = () -> processPutchunk(putchunk.convertMessage(), putchunk.getReplicationDegree(), chunk_id, 0);
            Peer.scheduled_executor.execute(task);
        }
    }

    private static void processPutchunk(byte[] message, int replication_degree, String chunk_id, int tries) {
        if(tries >= 5){
            System.out.println("Putchunk failed desired replication degree: " + chunk_id);
            return;
        }

        if (Store.getInstance().checkBackupChunksOccurrences(chunk_id) >= replication_degree) {
            System.out.println("Backed up " + chunk_id + " with desired replication_degree");
            return;
        }

        Peer.MDB.sendMessage(message);

        int try_aux = tries+1;
        long time = (long) Math.pow(2, try_aux-1);
        Runnable task = () -> processPutchunk(message, replication_degree, chunk_id, try_aux);
        Peer.scheduled_executor.schedule(task, time, TimeUnit.SECONDS);
    }

    public static void receivePutchunk(PutChunkMessage putchunk){

        String file_id = putchunk.getFileId();

        if(Store.getInstance().checkBackupChunksOccurrences(file_id + "_" + putchunk.getChunkNo()) != -1) {
            return;
        }

        if(putchunk.getVersion() == Macros.VERSION) {
            Boolean x = FileManager.checkConditionsForSTORED(file_id, putchunk.getChunkNo(), putchunk.getChunk().length);
            if(x == null){
                Runnable task = ()-> sendStoredEnhanced(putchunk);
                Peer.scheduled_executor.schedule(task, new Random().nextInt(401), TimeUnit.MILLISECONDS);
            }
        }

    }

    private static void sendStored(byte[] message){
        Peer.MC.sendMessage(message);
    }

    private static void sendStoredEnhanced(PutChunkMessage putchunk) {
        String chunk_id = putchunk.getFileId() + "_" + putchunk.getChunkNo();
        if(Store.getInstance().checkAuxStoredOccurrences(chunk_id) < putchunk.getReplicationDegree()){
            FileManager.storeChunk(putchunk.getFileId(), putchunk.getChunkNo(), putchunk.getChunk(), putchunk.getReplicationDegree(), false);
            StoredMessage stored = new StoredMessage(putchunk.getVersion(), Peer.id, putchunk.getFileId(), putchunk.getChunkNo());
            sendStored(stored.convertMessage());
        }
        Store.getInstance().removeAuxStoredOccurrences(chunk_id);
    }

    public static void receiveStored(StoredMessage stored){
        String file_id = stored.getFileId();
        String chunk_id = file_id + "_" + stored.getChunkNo();
        int peer_id = stored.getSenderId();

        if(FilesListing.getInstance().getFileName(file_id) != null) {
            if(Store.getInstance().addBackupChunksOccurrences(chunk_id, peer_id, Peer.version == Macros.VERSION && stored.getVersion() == Macros.VERSION)) {
                //condition is true is the replication degree has been accomplished
                Runnable task = ()-> sendCancelBackup(stored);
                Peer.scheduled_executor.execute(task);
            }
        } else {
            if(Peer.version == Macros.VERSION && stored.getVersion() == Macros.VERSION) {
                if(!Store.getInstance().hasReplicationDegree(chunk_id)){
                    //adds to the replication degree of the stored file
                    Store.getInstance().addReplicationDegree(chunk_id, peer_id);
                }
            }
        }
    }

    private static void sendCancelBackup(StoredMessage stored) {
        CancelBackupMessage message = new CancelBackupMessage(Peer.version, Peer.id, stored.getFileId(), stored.getChunkNo(), stored.getSenderId());
        Peer.MDB.sendMessage(message.convertMessage());
    }

    public static void receiveCancelBackup(CancelBackupMessage cancel_backup){
        if(Peer.id == cancel_backup.getReceiver_id()){
            FileManager.removeChunk(cancel_backup.getFileId(), cancel_backup.getChunkNo(), false);
        }
    }
}
