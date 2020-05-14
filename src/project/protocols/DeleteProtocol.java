package project.protocols;

import project.message.*;
import project.peer.Peer;
import project.store.FileManager;
import project.store.FilesListing;
import project.store.Store;

import java.util.concurrent.TimeUnit;

public class DeleteProtocol {

    // --------------------   peer initiator
    public static void sendDelete(String file_id){

        DeleteMessage deleteMessage = new DeleteMessage(Peer.id, file_id);
        Runnable task = () -> processDelete(deleteMessage, file_id, 0);
        Peer.scheduled_executor.execute(task);
    }

    public static void processDelete(DeleteMessage message, String file_id, int tries){

        if(tries >= 10){
            System.out.println("Couldn't delete all chunks of the file " + file_id);
            Store.getInstance().changeFromBackupToDelete(file_id);
            return;
        }

        if (Store.getInstance().checkIfAllDeleted(file_id)) {
            System.out.println("Delete all chunks of the file " + file_id);

            String file_name = FilesListing.getInstance().getFileName(file_id);
            Integer number_of_chunks = FilesListing.getInstance().get_number_of_chunks(file_name);

            for(int i = 0; i< number_of_chunks; i++ ) {
                String chunk_id = file_id + "_" + i;
                Store.getInstance().removeBackupChunksOccurrences( chunk_id);
            }

            // Remove entry with the file_name and correspond file_id from allFiles
            FilesListing.getInstance().delete_file_records(file_name, file_id); //no reason to keep them

            return;
        }

        //makeRequest(message, String address, Integer port)

        int try_aux = tries + 1;

        long time = (long) Math.pow(3, try_aux-1);
        Runnable task = () -> processDelete(message, file_id, try_aux);
        Peer.scheduled_executor.schedule(task, time, TimeUnit.SECONDS);
    }

    public static void receiveDeleteReceived(DeleteReceivedMessage message) {

        Integer peer_id = message.getSenderId();
        String file_id = message.getFileId();

        String file_name = FilesListing.getInstance().getFileName(file_id);
        Integer number_of_chunks = FilesListing.getInstance().get_number_of_chunks(file_name);

        for(int i = 0; i < number_of_chunks; i++ ) {
            String chunk_id = file_id + "_" + i;
            //remove peer from the list of chunks backup, if chunk doesn't exists it's fine
            Store.getInstance().removeBackupChunkOccurrence(chunk_id, peer_id);

        }
        System.out.println("Confirm deletion all chunks of file " + file_id + " on peer " + message.getSenderId());


    }


    // ------------------------- peer not initiator --------------------

    public static DeleteReceivedMessage receiveDelete(DeleteMessage deleteMessage){
        String file_id = deleteMessage.getFileId();

        //delete all files and records in stored
        FileManager.deleteFileFolder(Store.getInstance().getStoreDirectoryPath() + file_id);

        if (Store.getInstance().removeStoredChunks(file_id)) {
            //Sends to other peers the delete message
            Runnable task = () -> processDelete(deleteMessage, file_id, 0);
            Peer.scheduled_executor.execute(task);
        }

        return new DeleteReceivedMessage(Peer.id, file_id);

    }


}


