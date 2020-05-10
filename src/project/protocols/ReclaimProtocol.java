package project.protocols;

import project.chunk.Chunk;
import project.message.PutChunkMessage;
import project.message.RemovedMessage;
import project.peer.Peer;
import project.store.FileManager;
import project.store.Store;

import java.util.Random;
import java.util.concurrent.TimeUnit;

public class ReclaimProtocol extends BasicProtocol{

    public static void sendRemoved(Integer sender_id, String file_id, Integer chunk_number){
        openSocket();
        RemovedMessage removedMessage = new RemovedMessage( sender_id, file_id, chunk_number);
        Runnable task = () -> processRemoveMessage(removedMessage);
        new Thread(task).start();
    }

    public static void processRemoveMessage(RemovedMessage removedMessage){
        sendWithTCP(removedMessage);
    }


    public static void receiveRemoved(RemovedMessage removedMessage ){
        String file_id = removedMessage.getFileId();
        Integer chunk_number = removedMessage.getChunkNo();

        String chunk_id = file_id + "_" + chunk_number;

        //check if this is this peer with a file
        if(Store.getInstance().checkBackupChunksOccurrences(chunk_id) != -1) {

            //update local count of this chunk replication degree
            Store.getInstance().removeBackupChunkOccurrence(chunk_id, removedMessage.getSenderId());

        } else if (Store.getInstance().checkStoredChunksOccurrences(chunk_id) != -1 ){

            Store.getInstance().removeStoredChunkOccurrence(chunk_id, removedMessage.getSenderId() );

            //check if count drops below the desired replication degree of that chunk
            if(!Store.getInstance().hasReplicationDegree(chunk_id)) {

                Chunk chunk = FileManager.retrieveChunk(file_id, chunk_number);

                if(chunk != null) {
                    Runnable task = () -> sendPutchunk( Peer.id, Store.getInstance().getReplicationDegree(chunk_id), file_id, chunk);

                    //initiate the chunk backup subprotocol after a random delay uniformly distributed between 0 and 400 ms
                    Peer.scheduled_executor.schedule(task, new Random().nextInt(401), TimeUnit.MILLISECONDS);
                }
            }
        }
    }

    public static void sendPutchunk(int sender_id, int replication_degree, String file_id, Chunk chunk) {
        //send put chunk

        PutChunkMessage putchunk = new PutChunkMessage( sender_id, file_id, chunk.chunk_no, replication_degree, chunk.content);

        String chunk_id = file_id + "_" + chunk.chunk_no;

        processPutchunk(putchunk, putchunk.getReplicationDegree(), chunk_id, 0);

    }

    private static void processPutchunk(PutChunkMessage message, int replication_degree, String chunk_id, int tries) {

        if(tries >= 5){
            System.out.println("Put chunk failed desired replication degree: " + chunk_id);
            return;
        }

        if ( Store.getInstance().hasReplicationDegree(chunk_id)) {
            System.out.println("Backed up " + chunk_id + " with desired replication_degree");
            return;
        }

       // Peer.MDB.sendMessage(message);
        sendWithTCP(message);

        int try_aux = tries + 1;
        long time = (long) Math.pow(2, try_aux-1);
        Runnable task = () -> processPutchunk(message, replication_degree, chunk_id, try_aux);
        Peer.scheduled_executor.schedule(task, time, TimeUnit.SECONDS);
    }
}