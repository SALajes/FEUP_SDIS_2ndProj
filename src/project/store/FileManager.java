package project.store;

import project.Macros;
import project.chunk.Chunk;
import project.peer.Peer;
import project.protocols.ReclaimProtocol;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class FileManager {

    /**
     * Creates an empty file to start the restoring procedure or the backup
     * @return true if successful, and false otherwise
     */
    public static boolean createEmptyFileForRestoring(String file_name) {
        String restore_file_path = Store.getInstance().getRestoredDirectoryPath() + file_name;
        return createEmptyFile(restore_file_path);
    }

    /**
     * Creates an empty file to start the restoring procedure or the backup/info file
     * if exists return true but doesn't creates a new file
     * @param file_path path of the file that is being created
     */
    public static boolean createEmptyFile(String file_path) {

        try {
            File file = new File(file_path);
            if(file.exists())
                return true;

            if(file.createNewFile()) {
                System.out.println("File: " + file + " was created");
                return true;
            }
        } catch(Exception e) {
            e.printStackTrace();
            System.err.println("Couldn't create an empty file to start restoring");
            return false;
        }
        return false;
    }

    /**
     * Idempotent Method that creates a directory given the path
     * @return success if directory was or is created and false if not
     */
    public static boolean createDirectory(String directory_path) {

        File directory = new File(directory_path);
        if (!directory.exists()) {
            //mkdirs() returns true if created and false on failure or if exists
            if (directory.mkdir()) {
                return true;
            } else {
                return false;
            }
        }
        return true;
    }

    /**
     * encodes a file name with a sha-256 cryptographic hash function
     * @param file used to get name and lastModified date
     * @return file id
     */
    public static String createFileId(File file) {

        String file_name = file.getName();

        //encoded file name uses the file.lastModified() that ensures that a modified file has a different fileId
        String file_name_to_encode = file_name + file.lastModified();

        //identifier is obtained by applying SHA256, a cryptographic hash function, to some bit string.
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        byte[] hash = digest.digest(file_name_to_encode.getBytes(StandardCharsets.UTF_8));

        return String.valueOf(hash);
    }


    /**
     * This functions append the body of a chunk (file data) in the position desired ( calculated with chunk number)
     * Used for restoring a file with a certain given filename
     * @param file_name name of the file
     * @param chunk_data the array with the bytes to put
     * @param chunk_number number of the chunk
     * @return true if success and false otherwise
     */
    public static boolean writeChunkToRestoredFile(String file_name, byte[] chunk_data, int chunk_number) {

        String file_path = Store.getInstance().getRestoredDirectoryPath() + "/" + file_name;

        ByteBuffer buffer = ByteBuffer.wrap(chunk_data);
        Path path = Paths.get(file_path);
        AsynchronousFileChannel channel ;

        try {
            channel = AsynchronousFileChannel.open(path, StandardOpenOption.WRITE);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Couldn't open restore file");
            return false;
        }

        CompletionHandler handler = new CompletionHandler() {
            @Override
            public void completed(Object result, Object attachment) { }
            @Override
            public void failed(Throwable exc, Object attachment) {
                System.out.println("Restore failed with exception:");
                exc.printStackTrace();
            }
        };

        channel.write(buffer, chunk_number * Macros.CHUNK_MAX_SIZE, "", handler);

        /*try {
            channel.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Couldn't close restore file");
            return false;
        }*/

        return true;
    }


    public static void deleteFilesFolders(String file_id) {
        deleteFileFolder( Store.getInstance().getStoreDirectoryPath() + file_id );
        Store.getInstance().removeStoredChunks(file_id);

        String file_name = FilesListing.getInstance().getFileName(file_id);
        if( file_name != null) {
            deleteFileFolder( Store.getInstance().getRestoredDirectoryPath() + file_id );
            //You should not delete the original file, when you execute the Delete protocol
            //So the folder files isn't deleted
        }

    }

    /**
     * deletes folder with chunks of a file passed in the first argument
     * @param file_path directory file
     * @return true if successful, and false other wise
     */
    public static boolean deleteFileFolder(String file_path) {

        File file = new File(file_path);

        if(file == null){
            return false;
        }

        if (file.isFile()) {
            Store.getInstance().RemoveOccupiedStorage(file.length());
            return file.delete();
        }

        if (!file.isDirectory()) {
            return false;
        }

        File[] folder_files = file.listFiles();
        if (folder_files != null && folder_files.length > 0) {
            for (File f : folder_files) {
                if (f.isFile()) {
                    Store.getInstance().RemoveOccupiedStorage(f.length());
                    f.delete();
                }
                else if (!f.delete()) {
                    return false;
                }
            }
        }

        return file.delete();
    }

    /**
     * get chunk from stored directory
     * @param file_id encoded
     * @param chunk_no number of the chunk we want to retrieve
     * @return wanted chunk data
     */
    public static Chunk retrieveChunk(String file_id, int chunk_no){
        if(Store.getInstance().checkStoredChunk(file_id, chunk_no)) {
            Chunk chunk;
            //get the chunk information from the chunks saved file
            final String chunk_path = Store.getInstance().getStoreDirectoryPath() + "/" + file_id + "/" + chunk_no;
            File file = new File(chunk_path);
            int chunk_size = (int) file.length();

            Path path = Paths.get(chunk_path);

            AsynchronousFileChannel channel = null;
            try {
                channel = AsynchronousFileChannel.open(path, StandardOpenOption.READ);
            } catch (IOException e) {
                e.printStackTrace();
            }

            ByteBuffer buffer = ByteBuffer.allocate(chunk_size);

            Future result = channel.read(buffer, 0); // position = 0

            while (! result.isDone());

            try {
               result.get();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }

            buffer.flip();

            int i = 0;
            byte[] chunk_data = new byte[chunk_size];

            while (buffer.hasRemaining()) {
                chunk_data[i] = buffer.get();
                i++;
            }

            buffer.clear();

            /*try {
                channel.close();
            } catch (IOException e) {
                e.printStackTrace();
            }*/

            chunk = new Chunk(chunk_no, chunk_data, chunk_size);
            return chunk;

        }

        // Does not have the chunk
        return null;

    }

    public static long retrieveChunkSize(String file_id, int chunk_no){
        final String chunk_path = Store.getInstance().getStoreDirectoryPath() + "/" + file_id + "/" + chunk_no;
        File file = new File(chunk_path);
        return file.length();
    }

    /**
     * Deletes a chunk
     * @param file_id encoded
     * @param chunk_number number of the chunk
     * @return true if chunk was removed and false if it was
     */
    public static boolean removeChunk(String file_id, int chunk_number){
        return removeChunk(file_id, chunk_number, true);
    }
    public static boolean removeChunk(String file_id, int chunk_number, boolean reclaim_protocol) {

        //check if the chunk exists
        if (!Store.getInstance().checkStoredChunk(file_id, chunk_number)) {
            System.out.println("A chunk with number " + chunk_number + " and file_id " + file_id + " doesn't exists.");
            return true;
        }

        System.out.println("Deleting chunk "+ chunk_number + " with id:" + file_id);

        //chunk will be in stored_directory/file_id/chunk_no
        String chunk_dir = Store.getInstance().getStoreDirectoryPath() + file_id + "/"+ chunk_number;

        File chunk_file = new File(chunk_dir);

        if( !chunk_file.exists() ){
            System.out.println("File doesn't exist in the correct path ");
            return false;
        }

        Store.getInstance().RemoveOccupiedStorage((int) chunk_file.length());

        if (chunk_file.delete()) {
            //removes from stored chunks Hashtable
            Store.getInstance().removeStoredChunk(file_id, chunk_number);

            if(reclaim_protocol)
                ReclaimProtocol.sendRemoved(Peer.version, Peer.id, file_id, chunk_number);

            return true;
        }

        return false;
    }


    /**
     * Given the chunk_body ( the data ), stores it in a file
     * @param file_id encoded
     * @param chunk_number number of the chunk
     * @param chunk_body data
     * @param replicationDegree wanted replication degree
     * @return true if successful or false otherwise
     */
    public synchronized static boolean storeChunk(String file_id, int chunk_number, byte[] chunk_body, Integer replicationDegree){
        return storeChunk(file_id, chunk_number, chunk_body, replicationDegree, true);
    }
    public synchronized static boolean storeChunk(String file_id, int chunk_number, byte[] chunk_body, Integer replicationDegree, Boolean check_conditions) {
        if(check_conditions){
            Boolean x = checkConditionsForSTORED(file_id, chunk_number, chunk_body.length);
            if (x != null) return x;
        }

        String chunk_directory =  Store.getInstance().getStoreDirectoryPath() + file_id + "/";

        // Idempotent Method
        FileManager.createDirectory(chunk_directory);
        String chunk_path = chunk_directory + chunk_number;
        FileManager.createEmptyFile(chunk_path);

        ByteBuffer buffer = ByteBuffer.wrap(chunk_body);

        Path path = Paths.get(chunk_path);

        AsynchronousFileChannel channel = null;

        try {
            channel = AsynchronousFileChannel.open(path, StandardOpenOption.WRITE);
        } catch (IOException e) {
            e.printStackTrace();
        }

        CompletionHandler handler = new CompletionHandler() {
            @Override
            public void completed(Object result, Object attachment) {

            }
            @Override
            public void failed(Throwable exc, Object attachment) {
                System.out.println(attachment + " failed with exception:");
                exc.printStackTrace();
            }
        };

        channel.write(buffer, 0, "", handler);

//        try {
//            channel.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        Store.getInstance().addStoredChunk(file_id, chunk_number, replicationDegree, chunk_body.length);

        return true;
    }

    public static Boolean checkConditionsForSTORED(String file_id, int chunk_number, int chunk_body_length) {
        //check if the chunk already exists
        if (Store.getInstance().checkStoredChunk(file_id, chunk_number)) {
            return true;
        }

        //check if there is enough storage
        if (!Store.getInstance().hasSpace(chunk_body_length)) {
            System.out.println("A chunk with number " + chunk_number + " and file_id " + file_id + " can't be store because there isn't space left.");
            return false;
        }
        return null;
    }

}
