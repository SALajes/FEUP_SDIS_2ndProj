package project.store;

import project.Pair;
import project.protocols.DeleteProtocol;

import java.io.*;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

/**
 * class that keeps record of the conversion of file_name to file_id
 */
public class FilesListing implements Serializable {

    private static FilesListing filesListing = new FilesListing();
    private ConcurrentHashMap<String, Pair<String, Integer>> files = new ConcurrentHashMap<>();

    //singleton
    private FilesListing() {  }

    /**
     * get all files listed
     * @return an instance FilesListing
     */
    public static FilesListing getInstance() {
        return filesListing;
    }

    public static void setInstance(FilesListing fl) { filesListing = fl; }

    public String getFileId(String file_name) {
        return files.get(file_name).first;
    }

    public int getNumberOfChunks(String file_name) {
        return files.get(file_name).second;
    }

    public String getFileName(String file_id) {
        Iterator it = files.entrySet().iterator();

        while(it.hasNext()){
            ConcurrentHashMap.Entry file = (ConcurrentHashMap.Entry) it.next();
            Pair<String, Integer> pair = (Pair<String, Integer>) file.getValue();
            if(file_id.equals(pair.first)){
                return (String) file.getKey();
            }
        }
        return null;
    }

    public void addFile(String file_name, String file_id, Integer number_of_chunks) {

        //put returns the previous value associated with key, or null if there was no mapping for key
        Pair<String, Integer> pair = files.put(file_name, new Pair<>(file_id, number_of_chunks));

        if (pair != null) {
            //backing up a file with the same name that wasn't change
            if(!pair.first.equals(file_id)) {
                System.out.println("This file_name already exists, the content will be updated.");

                //deletes the older file
                System.out.println("Deleting " + pair.second + " chunks from the out of date file");

                //deletes file from network storage
                DeleteProtocol.processDelete(file_id);

                //deletes own files with chunks of the file in the 3 folders ( files, stored, restored)
                FileManager.deleteFilesFolders(pair.first);

                //old file is ours so unregister chunks of the file
                Store.getInstance().removeStoredChunks(pair.first);
            }
        }

        setFilesDiskInfo();
    }

    public void deleteFileRecords(String file_name) {
        files.remove(file_name);
        setFilesDiskInfo();
    }


    /**
     * changes the content of the file to contain this current object
     * @return true if successful and false otherwise
     */
    public boolean setFilesDiskInfo() {
        try {
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(Store.getInstance().getFilesInfoDirectoryPath()));
            objectOutputStream.writeObject(this);

        } catch (Exception e) {
            e.getStackTrace();
            return false;
        }
        return true;
    }

    /**
     *
     * @return true if successful and false otherwise
     */
    public boolean getFilesDiskInfo() {
        //if file is empty there is nothing to have in the concurrentMap
        if (new File(Store.getInstance().getFilesInfoDirectoryPath()).length() == 0) {
            files = new ConcurrentHashMap<>();
            return true;
        }

        try {
            ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(Store.getInstance().getFilesInfoDirectoryPath()));
            filesListing = (FilesListing) objectInputStream.readObject();
            System.out.println("Files Concurrent map is updated according to disk info");

        } catch (Exception ignored) {
            System.out.println("Couldn't put files info into the disk");
            return false;
        }
        return true;
    }

    public ConcurrentHashMap<String, Pair<String, Integer>> getFiles() {
        return files;
    }
}
