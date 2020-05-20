package project.protocols;

import project.message.*;
import project.peer.ChordNode;
import project.peer.NodeInfo;
import project.peer.Peer;
import project.store.FileManager;
import project.store.FilesListing;
import project.store.Store;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class DeleteProtocol {

    // --------------------   peer initiator
    public static void sendDelete(String file_id) {

        //get file_name
        String file_name = FilesListing.getInstance().getFileName(file_id);
        int number = FilesListing.getInstance().get_number_of_chunks(file_name);

        // Remove entry with the file_name and correspond file_id from allFiles
        FilesListing.getInstance().delete_file_records(file_name, file_id); //no reason to keep them

        DeleteMessage deleteMessage = new DeleteMessage(ChordNode.this_node.key, file_id);
        Runnable task = () -> processDelete(deleteMessage, number,0);
        Peer.scheduled_executor.execute(task);
    }

    /**
     * sends delete message to all peers register as peers who backup the file
     * @param message delete message
     * @param number_chunks number of chunks that the file have
     * @param tries current number of files
     */
    public static void processDelete(DeleteMessage message, int number_chunks, int tries) {
        String file_id = message.getFileId();

        if(tries >= 10){
            System.out.println("Couldn't delete all chunks of the file " + file_id);
            //keeps track of the un-deleted files
            Store.getInstance().changeFromBackupToDelete(file_id);
            return;
        }

        if (Store.getInstance().checkIfAllDeleted(file_id)) {
            System.out.println("All chunks of the file " + file_id + " were deleted");

            for(int i = 0; i < number_chunks; i++ ) {
                String chunk_id = file_id + "_" + i;
                Store.getInstance().removeBackupChunksOccurrences(chunk_id);
            }
            return;
        }

        for(int i = 0; i < number_chunks; i++) {
            String chunk_id = file_id + "_" + i;
            ArrayList<BigInteger> keys = Store.getInstance().get_backup_chunks_occurrences(chunk_id);
            for(int j= 0; j < keys.size(); j++) {
                NodeInfo nodeInfo = ChordNode.findSuccessor(keys.get(j));
                message.setSender(nodeInfo.key);
                try {
                    ChordNode.makeRequest(message, nodeInfo.address, nodeInfo.port);
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }

        int try_aux = tries + 1;

        long time = (long) Math.pow(3, try_aux-1);
        Runnable task = () -> processDelete(message, number_chunks, try_aux);
        Peer.scheduled_executor.schedule(task, time, TimeUnit.SECONDS);
    }

    public static void receiveDeleteReceived(DeleteReceivedMessage message) {

        BigInteger key = message.getSender();
        String file_id = message.getFileId();

        String file_name = FilesListing.getInstance().getFileName(file_id);
        Integer number_of_chunks = FilesListing.getInstance().get_number_of_chunks(file_name);

        for(int i = 0; i < number_of_chunks; i++ ) {
            String chunk_id = file_id + "_" + i;
            //remove peer from the list of chunks backup, if chunk doesn't exists it's fine
            Store.getInstance().removeBackupChunkOccurrence(chunk_id, key);

        }
        System.out.println("Confirm deletion all chunks of file " + file_id + " on peer " + key);
    }


    // ------------------------- peer not initiator --------------------

    public static DeleteReceivedMessage receiveDelete(DeleteMessage deleteMessage){
        String file_id = deleteMessage.getFileId();

        //delete all files and records in stored
        FileManager.deleteFileFolder(Store.getInstance().getStoreDirectoryPath() + file_id);

        if (Store.getInstance().removeStoredChunks(file_id)) {
            System.out.println("Remove file records");

        } else {
            //sends successor the delete message
            Runnable task = () -> sendDelete(deleteMessage);
            Peer.scheduled_executor.execute(task);
        }
        //sends with is key of the chord
        return new DeleteReceivedMessage(deleteMessage.getSender(), file_id);

    }

    /**
     * sends delete message to the successor
     * @param deleteMessage delete message received
     */
    public static void sendDelete(DeleteMessage deleteMessage) {
        String file_id = deleteMessage.getFileId();
        int number_chunks = Store.getInstance().number_of_chunks(file_id);
        deleteMessage.setSender(ChordNode.this_node.key);

        for(int i = 0; i < number_chunks; i++) {
            NodeInfo nodeInfo = ChordNode.findSuccessor(ChordNode.this_node.key);
            try {
                ChordNode.makeRequest(deleteMessage, nodeInfo.address, nodeInfo.port);
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }

        }
    }


}


