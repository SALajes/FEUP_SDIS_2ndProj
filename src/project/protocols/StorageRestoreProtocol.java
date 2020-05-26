package project.protocols;

import project.Macros;
import project.chunk.StoredChunks;
import project.message.BaseMessage;
import project.message.NotifyStorageMessage;
import project.message.StorageResponseMessage;
import project.peer.ChordNode;
import project.peer.Network;
import project.peer.NodeInfo;
import project.peer.Peer;
import project.store.Store;

import java.io.IOException;
import java.math.BigInteger;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class StorageRestoreProtocol {

    public static void processNotifyStorage() {
        ConcurrentHashMap<String, StoredChunks> stored_chunks = Store.getInstance().getStoredChunks();

        for (String key: stored_chunks.keySet()) {
            StoredChunks storedChunks = stored_chunks.get(key);
            NotifyStorageMessage notifyStorage = new NotifyStorageMessage(ChordNode.this_node.key, storedChunks.getChunkNumbers(), key);

            Runnable task = ()->sendNotifyStorage(notifyStorage, storedChunks.getOwner(), 0);
            Peer.thread_executor.execute(task);
        }

    }

    public static void sendNotifyStorage(NotifyStorageMessage notifyStorage, BigInteger owner, int tries) {
        if(tries >= 10){
            System.out.println("Couldn't notify storage of chunks " + notifyStorage.getFileId() + " of the owner " + owner);
            return;
        }

        NodeInfo nodeInfo = ChordNode.findSuccessor(owner);
        if(nodeInfo.key.equals(owner)) {
            try {
                BaseMessage response = Network.makeRequest(notifyStorage, nodeInfo.address, nodeInfo.port);
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

    private static void receiveStorageResponse(BaseMessage response) {

    }

    public static BaseMessage receiveNotifyStorage(NotifyStorageMessage notify){


        return new StorageResponseMessage(ChordNode.this_node.key);
    }
}
