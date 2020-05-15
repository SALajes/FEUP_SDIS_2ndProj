package project.protocols;

import project.chunk.Chunk;
import project.message.*;
import project.peer.ChordNode;
import project.peer.NodeInfo;
import project.peer.Peer;
import project.store.FileManager;
import project.store.FilesListing;
import project.store.Store;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.*;

public class RestoreProtocol {

    // --------- peer initiator
    public static void sendGetChunk(String file_id, int number_of_chunks){

        for (int i = 0; i < number_of_chunks; i++) {
            int chunk_no = i;
            Runnable task = () -> processGetChunk(file_id, chunk_no);
            Peer.scheduled_executor.execute(task);
        }
    }

    public static void processGetChunk(String file_id, int chunk_no){

        GetChunkMessage message = new GetChunkMessage( Peer.id, file_id, chunk_no);
        NodeInfo nodeInfo = ChordNode.findPredecessor(ChordNode.this_node.key);
        ChordNode.makeRequest(message, nodeInfo.address, nodeInfo.port);

    }

    public static void receiveChunk(ChunkMessage chunkMessage){
        String file_id = chunkMessage.getFileId();
        String file_name = FilesListing.getInstance().getFileName(file_id);

        String chunk_id = file_id + "_" + chunkMessage.getChunkNo();

        if (Store.getInstance().checkBackupChunksOccurrences(chunk_id) != -1) {
            FileManager.writeChunkToRestoredFile(file_name, chunkMessage.getChunk(), chunkMessage.getChunkNo());
        }

        Store.getInstance().checkGetchunkReply(chunk_id);

    }


    //---------------- peer not initiator

    /**
     * a peer that has a copy of the specified chunk shall send it in the body of a CHUNK message via the MDR channel
     * @param  getChunkMessage message received
     */
    public static ChunkMessage receiveGetChunk(GetChunkMessage getChunkMessage ){
        String file_id = getChunkMessage.getFileId();

        Integer chunk_number = getChunkMessage.getChunkNo();
        Chunk chunk = FileManager.retrieveChunk(file_id, chunk_number);

        if (chunk == null)
            return null;

        return sendChunk(Peer.id, file_id, chunk_number, chunk.content);
        //doesn't need to send the message to anyone was only one peer needs to answer
    }

    public static ChunkMessage sendChunk(Integer sender_id, String file_id, Integer chunk_no, byte[] chunk_data){
        ChunkMessage chunkMessage = new ChunkMessage(sender_id, file_id, chunk_no, chunk_data);

        String chunk_id = chunkMessage.getFileId() + "_" + chunkMessage.getChunkNo();
        Store.getInstance().addGetchunkReply(chunk_id);

        return processChunk(chunkMessage, chunk_id);
    }

    public static ChunkMessage processChunk(ChunkMessage chunkMessage, String chunk_id){
        if(!Store.getInstance().getGetchunkReply(chunk_id))
            return chunkMessage;
        Store.getInstance().removeGetchunkReply(chunk_id);
        return null;
    }

}
