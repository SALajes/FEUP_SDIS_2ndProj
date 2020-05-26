package project.protocols;

import project.chunk.StoredChunks;
import project.message.BaseMessage;
import project.message.DeleteReceivedMessage;
import project.message.NotifyStorage;
import project.peer.ChordNode;
import project.peer.Network;
import project.peer.NodeInfo;
import project.peer.Peer;
import project.store.Store;

import java.io.IOException;
import java.math.BigInteger;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static project.message.Message_Type.NOTIFY_STORAGE;

public class StorageInfo {

    public static void processNotifyStorage() {
        ConcurrentHashMap<String, StoredChunks> stored_chunks = Store.getInstance().getStoredChunks();

        for (String key: stored_chunks.keySet()) {
            StoredChunks storedChunks = stored_chunks.get(key);
            NotifyStorage notifyStorage = new NotifyStorage(NOTIFY_STORAGE, ChordNode.this_node.key, storedChunks.getChunkNumbers(), key);
            sendNotifyStorage(notifyStorage, storedChunks.getOwner(), 0);
        }

    }

    public static void sendNotifyStorage(NotifyStorage notifyStorage, BigInteger owner, int tries) {
        if(tries >= 10){
            System.out.println("Couldn't notify storage of chunks " + notifyStorage.getFile_id() + " of the owner " + owner);
            return;
        }

        NodeInfo nodeInfo = ChordNode.findSuccessor(owner);
        if(nodeInfo.key.equals(owner)) {
            try {
                BaseMessage response = Network.makeRequest(notifyStorage, nodeInfo.address, nodeInfo.port);
                return;
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        int n = tries + 1;
        Runnable task = ()->sendNotifyStorage(notifyStorage, owner, n);
        Peer.scheduled_executor.schedule(task, (int)Math.pow(3, n), TimeUnit.SECONDS);

    }

    public static void receiveStorageInfo() {

    }
}
