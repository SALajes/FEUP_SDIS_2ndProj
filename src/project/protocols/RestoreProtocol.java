package project.protocols;

import project.Macros;
import project.chunk.Chunk;
import project.message.*;
import project.peer.Peer;
import project.store.FileManager;
import project.store.FilesListing;
import project.store.Store;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.*;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class RestoreProtocol {

    public static void sendGetChunk(String file_id, int number_of_chunks){
        for (int i = 0; i < number_of_chunks; i++) {
            int chunk_no = i;
            Runnable task = () -> processGetChunkEnhancement(file_id, chunk_no);
            Peer.scheduled_executor.execute(task);
        }
    }

    /**
     * a peer that has a copy of the specified chunk shall send it in the body of a CHUNK message via the MDR channel
     * @param  getChunkMessage message received
     */
    public static void receiveGetChunk(GetChunkMessage getChunkMessage ){
        String file_id = getChunkMessage.getFileId();

        Integer chunk_number = getChunkMessage.getChunkNo();
        Chunk chunk = FileManager.retrieveChunk(file_id, chunk_number);

        if (chunk == null)
           return;

        sendChunk(getChunkMessage.getVersion(), Peer.id, file_id, chunk_number, chunk.content);
    }

    public static void sendChunk(double version, Integer sender_id, String file_id, Integer chunk_no, byte[] chunk_data){
        ChunkMessage chunkMessage = new ChunkMessage(version, sender_id, file_id, chunk_no, chunk_data);

        String chunk_id = chunkMessage.getFileId() + "_" + chunkMessage.getChunkNo();
        Store.getInstance().addGetchunkReply(chunk_id);

        Runnable task = () -> processChunk(chunkMessage, chunk_id);
        Peer.scheduled_executor.schedule(task, new Random().nextInt(401), TimeUnit.MILLISECONDS);
    }

    public static void processChunk(ChunkMessage chunkMessage, String chunk_id){
        if(!Store.getInstance().getGetchunkReply(chunk_id))
            Peer.MDR.sendMessage(chunkMessage.convertMessage());
        Store.getInstance().removeGetchunkReply(chunk_id);
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


    //--------------------- ENHANCED VERSION ------------------
    public static void processGetChunkEnhancement(String file_id, int chunk_no){
        ServerSocket server_socket = null;

        try {
            server_socket = new ServerSocket(0);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Integer port = server_socket.getLocalPort();

        String address;

        try {
            address = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return;
        }

        GetChunkEnhancementMessage message = new GetChunkEnhancementMessage(Peer.version, Peer.id,
                file_id, chunk_no, port , address);

        Peer.MC.sendMessage(message.convertMessage());

        try {
            server_socket.setSoTimeout(10000);

            ServerSocket aux_server_socket = server_socket;
            Runnable task = ()-> {
                receiveChunkEnhancement(aux_server_socket);

                try {
                    aux_server_socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            };
            Peer.scheduled_executor.execute(task);
        } catch (SocketException e) {
            e.printStackTrace();
            System.err.println("No peer responded");
            return;
        }
    }

    public static void receiveGetChunkEnhancement(GetChunkEnhancementMessage message) {
        Integer chunk_no = message.getChunkNo();
        String file_id = message.getFileId();

        if(!Store.getInstance().checkStoredChunk(file_id, chunk_no)){
            return;
        }

        Chunk chunk = FileManager.retrieveChunk(file_id, chunk_no);

        if (chunk == null)
            return;

        ChunkMessage chunkMessage = new ChunkMessage(Peer.version, Peer.id, file_id, chunk_no, chunk.content);

        try {
            Socket socket = new Socket(InetAddress.getByName(message.getAddress()), message.getPort());

            ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
            objectOutputStream.writeObject(chunkMessage.convertMessage());
        } catch (IOException e) {
            return;
        }
    }

    public static void receiveChunkEnhancement(ServerSocket server_socket){
        try {
            final Socket socket = server_socket.accept();

            ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
            byte[] message = (byte[]) objectInputStream.readObject();
            // ObjectIntputStream is atomic
            ChunkMessage chunkMessage = (ChunkMessage) MessageParser.parseMessage(message, message.length);

            String file_name = FilesListing.getInstance().getFileName(chunkMessage.getFileId());
            if(FileManager.writeChunkToRestoredFile(file_name, chunkMessage.getChunk(), chunkMessage.getChunkNo())){
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InvalidMessageException e) {
            e.printStackTrace();
        }

    }
}
