package project.peer;

import java.io.File;

import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.*;


import project.InvalidFileException;
import project.Macros;

import project.channel.*;
import project.chunk.ChunkFactory;
import project.message.InvalidMessageException;
import project.protocols.BackupProtocol;
import project.protocols.DeleteProtocol;
import project.protocols.RestoreProtocol;
import project.store.FileManager;
import project.store.FilesListing;
import project.store.Pair;
import project.store.Store;

public class Peer implements RemoteInterface {
    private static final int RegistryPort = 1099;
    public static double version;
    public static int id;

    private static String service_access_point;

    //Addresses, ports and InetAdress of each channel
    public static MulticastControlChannel MC;
    public static MulticastDataBackupChannel MDB;
    public static MulticastDataRecoveryChannel MDR;

    public static ExecutorService channel_executor;
    public static ScheduledThreadPoolExecutor scheduled_executor;

    public Peer(String MC_address, int MC_port, String MDB_address, int MDB_port, String MDR_address, int MDR_port)  {
        MC = new MulticastControlChannel(MC_address, MC_port);
        MDB = new MulticastDataBackupChannel(MDB_address, MDB_port);
        MDR = new MulticastDataRecoveryChannel(MDR_address, MDR_port);

        channel_executor = Executors.newCachedThreadPool();
        channel_executor.submit(MC);
        channel_executor.submit(MDB);
        channel_executor.submit(MDR);

        scheduled_executor = new ScheduledThreadPoolExecutor(0);
    }

    //class methods
    public static void main(String[] args){
        if(args.length != 9){
            System.out.println("Usage: [package]Peer <protocol_version> <peer_id> <service_access_point> " +
                    "<MC_address> <MC_port> <MDB_address> <MDB_port> <MDR_address> <MDR_port>");
            return;
        }

        try{
            version = Double.parseDouble(args[0]);

            if( version != Macros.BASIC_VERSION && version != Macros.ENHANCED_VERSION) {
                System.out.println("Not a recognizable version");
                System.exit(-1);
            }

            id = Integer.parseInt(args[1]);

            //since we are using RMI transport protocol, then the access_point is <remote_object_name>
            service_access_point = args[2];

            Peer object_peer = new Peer(args[3], Integer.parseInt(args[4]), args[5], Integer.parseInt(args[6]), args[7], Integer.parseInt(args[8]));
            RemoteInterface stub = (RemoteInterface) UnicastRemoteObject.exportObject(object_peer, 0);

            Registry registry;
            try {
                registry = LocateRegistry.createRegistry(RegistryPort);
            } catch (RemoteException e){
                registry = LocateRegistry.getRegistry(RegistryPort);
            }

            registry.rebind(service_access_point, stub);

            //creates folders
            Store.getInstance();
            FilesListing.getInstance();

            System.out.println("Peer " + id + " ready");

        } catch (Exception e) {
            System.err.println("Peer exception: " + e.toString());
            e.printStackTrace();
        }
    }

    public int backup(String file_path, int replication_degree) throws InvalidMessageException, InvalidFileException {

        if(replication_degree <= 0 || replication_degree > 9)
            throw new InvalidMessageException("Replication degree is invalid");

        System.out.println("Backup file: "+ file_path);

        File file = new File(file_path);

        if(file.length() >= Macros.MAX_FILE_SIZE) {
            throw new InvalidFileException("File is larger than accepted");
        }

        String file_id = FileManager.createFileId(file);
        int number_of_chunks = (int) Math.ceil((float) file.length() / Macros.CHUNK_MAX_SIZE );
        FilesListing.getInstance().add_file(file.getName(), file_id, number_of_chunks);

        ChunkFactory chunkFactory = new ChunkFactory(file, replication_degree);
        BackupProtocol.sendPutchunk(Peer.version, Peer.id, replication_degree, file_id, chunkFactory.getChunks());

        return 0;
    }

    /**
     *
     * @param file_path
     * The client shall specify the file to restore by its pathname.
     */
    @Override
    public int restore(String file_path) throws RemoteException, InvalidFileException {

        final String file_name = new File(file_path).getName();

        String file_id;
        try{
            file_id = FilesListing.getInstance().getFileId(file_name);
        }
        catch(Exception e){
            throw new InvalidFileException("File name not found");
        }

        FileManager.createEmptyFileForRestoring( file_name );

        int number_of_chunks = FilesListing.getInstance().get_number_of_chunks(file_name);

        RestoreProtocol.sendGetChunk(file_id, number_of_chunks);

        Store.getInstance().addRestoredFile(file_id, file_name);

        return 0;
    }

    /**
     * The client shall specify the file to delete by its pathname.
     * @param file_path of the file that is going to be deleted
     */
    @Override
    public int delete(String file_path) throws InvalidFileException {
        final String file_name = new File(file_path).getName();

        //gets the file_id from the entry with key file_name form allFiles
        String file_id;
        try{
            file_id = FilesListing.getInstance().getFileId(file_name);
        }
        catch(Exception e){
            throw new InvalidFileException("File name not found");
        }

        //sends message DELETE to all peers
        DeleteProtocol.sendDelete(file_id);

        if(Peer.version == Macros.BASIC_VERSION) {

            // Remove entry with the file_name and correspond file_id from allFiles
            FilesListing.getInstance().delete_file_records(file_name, file_id);

            System.out.println("Deleting chunks in all peers");

        }

        return 0;
    }


    /**
     *
     * @param max_disk_space
     * The client shall specify the maximum disk space in KBytes (1KByte = 1000 bytes) that can be used for storing chunks.
     * It must be possible to specify a value of 0, thus reclaiming all disk space previously allocated to the service.
     */
    @Override
    public int reclaim(int max_disk_space) throws RemoteException {
        if(max_disk_space < 0) {
            System.err.println("Invalid maximum disk space");
            System.exit(-1);
        }

        long max_disk_space_aux = 1000*(long)max_disk_space;
        Store.getInstance().setStorageCapacity(max_disk_space_aux);

        return 0;
    }

    @Override
    public String state() {
        String state = "\n------- THISISPEER " + Peer.id + " -------\n";
        state += "------- ( " + Peer.service_access_point + " ) -------\n";
        state += "------------------------------------\n\n";

        state += retrieveBackupState() + "\n";

        state += retrieveStoredChunksState() + "\n";

        state += retrieveStorageState();

        return state;
    }

    private String retrieveBackupState() {
        String state = "|--------- BACKUP --------|\n";
        ConcurrentHashMap<String, Pair<String, Integer>> files = FilesListing.get_files();

        Iterator it = files.entrySet().iterator();

        while(it.hasNext()){
            ConcurrentHashMap.Entry file = (ConcurrentHashMap.Entry)it.next();
            String file_name = (String) file.getKey();
            Pair<String, Integer> pair = (Pair<String, Integer>) file.getValue();// Pair( file_id , number_of_chunks )
            if(pair.first!=null){
                int replication_degree = Store.getInstance().getBackupChunkReplicationDegree(pair.first + "_0");

                state = state + "> path: " + file_name + "\n"
                        + "   id: " + pair.first + "\n"
                        + "   replication degree: " + replication_degree + "\n"
                        + "   > chunks:\n";

                for (int i = 0; i < pair.second; i++) {
                    state = state + "      id: " + i + "\n"
                            + "         perceived replication degree: " + Store.getInstance().checkBackupChunksOccurrences(pair.first + "_" + i) + "\n";
                }
            }
        }

        return state;
    }

    private String retrieveStoredChunksState() {
        String state = "|----- STORED CHUNKS -----|\n";

        ConcurrentHashMap<String, Pair<Integer, ArrayList<Integer>>> stored_chunks = Store.getInstance().getStoredChunks();
        Iterator it = stored_chunks.entrySet().iterator();

        while(it.hasNext()) {
            ConcurrentHashMap.Entry chunks = (ConcurrentHashMap.Entry) it.next();
            String file_id = (String) chunks.getKey();
            Pair<Integer,ArrayList<Integer>> pair = (Pair<Integer,ArrayList<Integer>>) chunks.getValue();// Pair( replication degree , chunks ids )

            state = state + "> file_id: " + file_id + "\n";

            for(Integer chunk_no : pair.second){
                state = state + "   id: " + chunk_no + "\n"
                        + "      size: " + FileManager.retrieveChunkSize(file_id, chunk_no) + "\n"
                        + "      perceived replication degree: " + Store.getInstance().checkStoredChunksOccurrences(file_id + "_" + chunk_no) + "\n";
            }
        }

        return state;
    }

    private String retrieveStorageState() {
        String state = "|----- STORAGE STATE -----|\n";

        state = state + "   Capacity: " + Store.getInstance().getStorageCapacity() + "\n"
                + "   Occupied: " + Store.getInstance().getOccupiedStorage() + "\n";

        return state + "---------------------------";
    }
}