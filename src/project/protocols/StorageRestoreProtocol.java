package project.protocols;

import project.chunk.BackedupChunk;
import project.chunk.Chunk;
import project.chunk.StoredChunks;
import project.message.BaseMessage;
import project.message.NotifyStorageMessage;
import project.message.PutChunkMessage;
import project.message.StorageResponseMessage;
import project.peer.ChordNode;
import project.peer.Network;
import project.peer.NodeInfo;
import project.peer.Peer;
import project.store.FileManager;
import project.store.Store;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class StorageRestoreProtocol {

    public static void processNotifyStorage() {

        System.out.println("Here");
        //first send to the peer initiators a notification of the files saved of him
        ConcurrentHashMap<String, StoredChunks> stored_chunks = Store.getInstance().getStoredChunks();

        for (String key: stored_chunks.keySet()) {
            StoredChunks storedChunks = stored_chunks.get(key);
            NotifyStorageMessage notifyStorage = new NotifyStorageMessage(ChordNode.this_node.key, storedChunks.getChunkNumbers(), key, false);

            System.out.println("Notifying peers initiator we has chunks of "+ storedChunks.getOwner());
            Runnable task = ()->sendNotifyStorage(notifyStorage, storedChunks.getOwner(), 0);
            Peer.thread_executor.execute(task);
        }

        //second checks his own files and replies the ones that were deleted when he was down
        ConcurrentHashMap<String, BackedupChunk> backedUpChunk = Store.getInstance().getBacked();

        for (String key: backedUpChunk.keySet()) {
            BackedupChunk backedUpChunks = backedUpChunk.get(key);
            ArrayList<Integer> chunk_number = new ArrayList<>();
            chunk_number.add(backedUpChunks.getChunkNumber());
            NotifyStorageMessage notifyStorage = new NotifyStorageMessage(ChordNode.this_node.key, chunk_number, key, true);

            ArrayList<BigInteger> peers = backedUpChunks.getPeers();

            for(BigInteger peerKey: peers) {
                System.out.println("Sending notify backup 3 of " + peerKey);
                Runnable task = ()->sendNotifyStorage(notifyStorage, peerKey, 0);
                Peer.thread_executor.execute(task);
            }
        }

    }

    public static void sendNotifyStorage(NotifyStorageMessage notifyStorage, BigInteger owner, int tries) {
        if(tries >= 10){
            System.out.println("Couldn't notify storage of chunks " + notifyStorage.getFileId() + " of the owner " + owner);
            return;
        }

        NodeInfo nodeInfo = ChordNode.findSuccessor(owner);
        System.out.println("key " + nodeInfo.key);
        if(nodeInfo.key.equals(owner)) {
            try {
                System.out.println("Sending notify backup message to " + owner );
                StorageResponseMessage response = (StorageResponseMessage) Network.makeRequest(notifyStorage, nodeInfo.address, nodeInfo.port);
                receiveStorageResponse(response);
                return;
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        int n = tries + 1;
        Runnable task = ()->sendNotifyStorage(notifyStorage, owner, n);
        Peer.scheduled_executor.schedule(task, (int)Math.pow(2, n), TimeUnit.SECONDS);

    }

    private static void receiveStorageResponse(StorageResponseMessage response) {
        System.out.println("Receive notify storage response");

        if(!response.wasSuccessful()) {
            String file_id = response.getFile_id();
            Integer chunk_number = response.getChunk_number();
            String chunk_id = file_id + "_" + chunk_number;

            if(response.isStore()) {
                //file was deleted so deleting all files and records in stored
                FileManager.deleteFilesFolders(file_id);

            } else {
                //replication degree isn't the desire one
                Store.getInstance().removeStoredChunk(file_id, chunk_number);
                int rep_degree = Store.getInstance().getFileActualReplicationDegree(chunk_id);

                Chunk chunk = FileManager.retrieveChunk(file_id, chunk_number);
                if(chunk == null) {
                    PutChunkMessage putchunk = new PutChunkMessage(ChordNode.this_node.key, file_id, chunk_number, rep_degree, chunk.content);

                    //done chunk by chunk
                    Runnable task = () -> BackupProtocol.sendPutchunk(putchunk, rep_degree + 1, 0);
                    Peer.thread_executor.execute(task);
                }

            }
        }

        //records weren't deleted, so nothing to do were

    }

    public static BaseMessage receiveNotifyStorage(NotifyStorageMessage notify) {
        System.out.println("Receive notify store");

        ArrayList<Integer> chunk_numbers = notify.getChunk_numbers();
        String file_id = notify.getFileId();

        //checking if it is still store
        if(notify.isCheckStorage()) {
            // process was to be done chunk by chunk, was deletion could be just for some of the chunks
            for (Integer chunk_no : chunk_numbers) {
                if(Store.getInstance().checkStoredChunk(file_id, chunk_no))
                    return new StorageResponseMessage(ChordNode.this_node.key, chunk_no, file_id, notify.isCheckStorage(), true);
                else return new StorageResponseMessage(ChordNode.this_node.key, chunk_no, file_id, notify.isCheckStorage(), false);
            }

        } else {
            //add the peer to the list of Peers containing the file
            for (Integer chunk_no : chunk_numbers) {
                Store.getInstance().addBackupChunks(file_id + "_" + chunk_no, notify.getSender());
                return new StorageResponseMessage(ChordNode.this_node.key, chunk_no, file_id, notify.isCheckStorage(), true);
            }
        }
        return new StorageResponseMessage(ChordNode.this_node.key, chunk_numbers.get(0), file_id, notify.isCheckStorage(), true);

    }
}
