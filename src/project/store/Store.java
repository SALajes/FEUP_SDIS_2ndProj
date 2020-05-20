package project.store;

import project.Macros;
import project.Pair;
import project.chunk.StoredChunks;
import project.peer.Peer;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Store implements Serializable {
    private static Store store = new Store();

    //state of others chunks
    private ConcurrentHashMap<String, StoredChunks> stored_chunks = new ConcurrentHashMap<>();

    //state of restored files - key file_id - value file_name
    private Hashtable<String, String> restored_files = new Hashtable<>();

    //state of our files - key file_id + chunk and value wanted_replication degree and list of peers
    private ConcurrentHashMap<String, Pair<Integer,ArrayList<BigInteger>>> backup_chunks_occurrences = new ConcurrentHashMap<>();

    //used for delete_enhancement, key file_id and value list of peers
    private ConcurrentHashMap<String, ArrayList<BigInteger>> not_deleted = new ConcurrentHashMap<>();

    private String peer_directory_path;
    private String files_directory_path;
    private String files_info_directory_path;
    private String store_directory_path;
    private String store_info_directory_path;
    private String restored_directory_path;

    private long occupied_storage = 0; //in bytes
    private long storage_capacity = Macros.INITIAL_STORAGE;


    /**
     * creates the four needed directory
     */
    private Store() { }

    /**
     * creates the four needed directory
     */
    public void initializeStore(){
        //setting the directory name
        peer_directory_path = Peer.id + "_directory/";
        files_directory_path = peer_directory_path + "files/";
        files_info_directory_path = peer_directory_path + "files.txt";
        store_directory_path = peer_directory_path + "stored/";
        store_info_directory_path = peer_directory_path + "stored.txt";
        restored_directory_path = peer_directory_path + "restored/";

        FileManager.createDirectory(peer_directory_path);
        FileManager.createDirectory(files_directory_path);
        FileManager.createEmptyFile(files_info_directory_path);
        FileManager.createDirectory(store_directory_path);
        //if exists return true but doesn't creates a new file
        FileManager.createEmptyFile(store_info_directory_path);
        FileManager.createDirectory(restored_directory_path);
    }

    public static Store getInstance(){
        return store;
    }

    public static void setInstance(Store storage) { store = storage; }


    // ---------------------------------------------- RECLAIM -----------------------------------

    /**
     * used in the reclaim sub protocol
     * It changes the storage_capacity variable and deletes the need files for the storage capacity be greater or
     * equal that the space with storage
     * @param new_capacity the new_storage_maximum_capacity
     */
    public void setStorageCapacity(long new_capacity) {
        this.storage_capacity = new_capacity;

        deleteOverReplicated();

        Set<String> keys = stored_chunks.keySet();

        //Obtaining iterator over set entries
        Iterator<String> itr = keys.iterator();
        String file_id;

        //deletes necessary chunk to have that space
        while((new_capacity < occupied_storage) && itr.hasNext()) {

            // Getting Key
            file_id = itr.next();

            ArrayList<Integer> chunks_nos = new ArrayList<>(stored_chunks.get(file_id).getChunkNumbers());

            for(Integer chunk_number : chunks_nos) {
                FileManager.removeChunk(file_id, chunk_number);
                //checks if can stop deleting files
                if(new_capacity >= occupied_storage){
                    return;
                }
            }
        }
    }

    public int getReplicationDegree(String file_id) {
        return stored_chunks.get(file_id).getReplication_degree();
    }

    /**
     * reclaim starts be deleting over replicated files.
     * If need, reclaims deletes replicated enough and under replicated
     */
    private void deleteOverReplicated() {
        Set<String> keys = stored_chunks.keySet();

        //Obtaining iterator over set entries
        Iterator<String> itr = keys.iterator();
        String file_id;

        //deletes necessary chunk to have that space
        while((this.storage_capacity < occupied_storage) && itr.hasNext()) {
            // Getting Key
            file_id = itr.next();

            ArrayList<Integer> chunks_nos = new ArrayList<>(stored_chunks.get(file_id).getChunkNumbers());

            for(Integer chunk_number : chunks_nos) {
                FileManager.removeChunk(file_id, chunk_number);
                if (this.storage_capacity >= occupied_storage) {
                    return;
                }
            }
        }
    }

    //-------------------- STORAGE ------------------
    public long getStorageCapacity() {
        return storage_capacity;
    }

    public long getOccupiedStorage() {
        return occupied_storage;
    }

    public boolean hasSpace(Integer space_wanted) {
        return (this.storage_capacity >= this.occupied_storage + space_wanted);
    }
    /**
     * used when a new chunk is store ( by backup )
     * @param space_wanted storage space added
     */
    public synchronized void AddOccupiedStorage(long space_wanted) {
        occupied_storage += space_wanted;
    }

    /**
     * used when a chunk is deleted
     * @param occupied_space the amount of space in bytes used for storage
     */
    public void RemoveOccupiedStorage(long occupied_space) {
        occupied_storage -= occupied_space;
        if(occupied_storage < 0)
            occupied_storage = 0;
    }


    // --------------------- STORED CHUNKS ----------------------------

    public ConcurrentHashMap<String, StoredChunks> getStoredChunks() {
        return stored_chunks;
    }

    public synchronized void addStoredChunk(BigInteger key, String file_id, int chunk_number, Integer replicationDegree, long chunk_length) {

        if(!stored_chunks.containsKey(file_id)) {
            ArrayList<Integer> chunks_stored = new ArrayList<>();
            chunks_stored.add(chunk_number);
            stored_chunks.put(file_id, new StoredChunks(key, replicationDegree, chunks_stored));

            //update the current space used for storage
            AddOccupiedStorage(chunk_length);
        }
        else if(!checkStoredChunk(file_id, chunk_number)) {
            StoredChunks chunks = stored_chunks.get(file_id);
            chunks.addChunkNumber(chunk_number);
            stored_chunks.replace(file_id, chunks);

            //update the current space used for storage
            AddOccupiedStorage(chunk_length);
        }
    }

    public BigInteger getKeyOfStoredChunk(String file_id) {
        return stored_chunks.get(file_id).getOwner();
    }

    /**
     * checks if the chunk exists in the stored_chunks
     * @param file_id encoded
     * @param chunk_no number of the chunk
     * @return true if exists and false otherwise
     */
    public boolean checkStoredChunk(String file_id, int chunk_no){
        if(stored_chunks.containsKey(file_id)) {
            return stored_chunks.get(file_id).getChunkNumbers().contains(chunk_no);
        }
        else return false;
    }

    public void removeStoredChunk(String file_id, Integer chunk_number) {
        if(stored_chunks.containsKey(file_id)) {
            StoredChunks chunks = stored_chunks.get(file_id);

            if(chunks.getChunkNumbers().size() == 1) {
                System.out.println("No more chunks of that file, removing folder of file " + file_id);
                stored_chunks.remove(file_id);
                FileManager.deleteFileFolder( this.getStoreDirectoryPath() + file_id);
            } else {
                chunks.removeChunkNumber(chunk_number);
                stored_chunks.replace(file_id, chunks);
            }
        }
    }

    public boolean removeStoredChunks(String file_id){
        if(!stored_chunks.containsKey(file_id)) {
            return false;
        }
        ArrayList<Integer> chunk_nos = stored_chunks.get(file_id).getChunkNumbers();

        if(chunk_nos.size() == 0) {
            return false;
        }

        stored_chunks.remove(file_id);
        return true;
    }

    public void removeFilePeerInfo(String file_id, int num_chunks){
        for(int i = 0; i < num_chunks; i++) {
            String chunk_id = file_id + "_" + i;
            backup_chunks_occurrences.remove(chunk_id);
        }
    }

    //---------------------------- BACKUP CHUNKS ----------------------------------

    public void newBackupChunk(String chunk_id, int replication_degree) {

        if(this.backup_chunks_occurrences.containsKey(chunk_id)){
            Pair<Integer, ArrayList<BigInteger>> pair = this.backup_chunks_occurrences.get(chunk_id);

            pair.first = replication_degree;

            this.backup_chunks_occurrences.replace(chunk_id, pair);
        }
        else this.backup_chunks_occurrences.put(chunk_id, new Pair<>(replication_degree, new ArrayList<>()));

    }
    //returns true in case there

    public boolean addBackupChunksOccurrences(String chunk_id, BigInteger key) {
        if(this.backup_chunks_occurrences.containsKey(chunk_id)){
            Pair<Integer, ArrayList<BigInteger>> pair = this.backup_chunks_occurrences.get(chunk_id);

            if(pair.second.contains(key))
                return false;

            if(checkBackupChunksOccurrences(chunk_id) >= getBackupChunkReplicationDegree(chunk_id))
                return true;

            pair.second.add(key);
            this.backup_chunks_occurrences.replace(chunk_id, pair);
        }
        return false;
    }

    public int checkBackupChunksOccurrences(String chunk_id) {
        if(backup_chunks_occurrences.containsKey(chunk_id))
            return backup_chunks_occurrences.get(chunk_id).second.size();
        return -1;
    }

    public int getBackupChunkReplicationDegree(String chunk_id) {
        return backup_chunks_occurrences.get(chunk_id).first;
    }

    public void removeBackupChunkOccurrence(String chunk_id, BigInteger key) {
        Pair<Integer,ArrayList<BigInteger>> value = this.backup_chunks_occurrences.get(chunk_id);

        if(value != null ){
            ArrayList<BigInteger> peersList = value.second;
            peersList.remove(key);

            if(peersList.size() > 0) {
                Pair<Integer, ArrayList<BigInteger>> pair = new Pair<>(value.first, peersList);
                this.backup_chunks_occurrences.replace(chunk_id, pair);
            }
            else this.backup_chunks_occurrences.remove(chunk_id);
        }

    }

    public void removeBackupChunksOccurrences(String chunk_id) {
        this.backup_chunks_occurrences.remove(chunk_id);
    }

    public ArrayList<BigInteger> getBackupChunksOccurrences(String chunk_id) {
        return backup_chunks_occurrences.get(chunk_id).second;
    }

    public void verifyBackupChunks(BigInteger key) {
        //TODO verify chunks that this peer backed up
    }

    //---------------------- DELETE ENHANCEMENT ------------------------

    public boolean checkIfAllDeleted(String file_id){
        String file_name = FilesListing.getInstance().getFileName(file_id);
        Integer number_of_chunks = FilesListing.getInstance().getNumberOfChunks(file_name);

        for(int i = 0; i < number_of_chunks; i++) {
            String chunk_id = file_id + "_" + i;
            Pair<Integer,ArrayList<BigInteger>> value = this.backup_chunks_occurrences.get(chunk_id);

            if(value.second.size() > 0) {
                return false;
            }
        }
        return true;
    }

    public void changeFromBackupToDelete(String file_id) {
        String file_name = FilesListing.getInstance().getFileName(file_id);
        Integer number_of_chunks = FilesListing.getInstance().getNumberOfChunks(file_name);

        for(int i = 0; i < number_of_chunks; i++) {
            String chunk_id = file_id + "_" + i;

            Pair<Integer,ArrayList<BigInteger>> value =  this.backup_chunks_occurrences.get(chunk_id);

            if(value.second.size() > 0) {

                ArrayList<BigInteger> copy_by_reference = new ArrayList<>(value.second);
                not_deleted.put(chunk_id, copy_by_reference);
            }

            removeBackupChunksOccurrences(chunk_id);

        }

        for(int i = 0; i < number_of_chunks; i++) {
            String chunk_id = file_id + "_" + i;

            ArrayList<BigInteger> peers_list =  not_deleted.get(chunk_id);
            if(peers_list != null) {
                System.out.println("File " + file_id + " still as " + peers_list.size() + " peers that didn't erase chunk " + i);
            }
        }
    }

    // ------------------------------- RESTORE ------------------------------------

    public void addRestoredFile(String file_id, String file_name){
        restored_files.put(file_id, file_name);
    }

    // ----------------------------------- GET PATHS ------------------------------------------------------

    public String getFilesInfoDirectoryPath() {
        return files_info_directory_path;
    }

    public String getRestoredDirectoryPath() {
        return restored_directory_path;
    }

    public String getStoreDirectoryPath() {
        return store_directory_path;
    }

    public String getFilesDirectoryPath() {
        return files_directory_path;
    }
}


