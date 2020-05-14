package project.protocols;

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

public class RestoreProtocol {

    // --------- peer initiator
    public static void sendGetChunk(String file_id, int number_of_chunks){

        for (int i = 0; i < number_of_chunks; i++) {
            int chunk_no = i;
            Runnable task = () -> processGetChunk(file_id, chunk_no);
            Peer.scheduled_executor.execute(task);
        }
    }

    public static void processGetChunk(String file_id, int chunk_no){

        GetChunkMessage message = new GetChunkMessage( Peer.id, file_id, chunk_no);
        //makeRequest(message, String address, Integer port)

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


    //---------------- peer not initiator

    /**
     * a peer that has a copy of the specified chunk shall send it in the body of a CHUNK message via the MDR channel
     * @param  getChunkMessage message received
     */
    public static ChunkMessage receiveGetChunk(GetChunkMessage getChunkMessage ){
        String file_id = getChunkMessage.getFileId();

        Integer chunk_number = getChunkMessage.getChunkNo();
        Chunk chunk = FileManager.retrieveChunk(file_id, chunk_number);

        if (chunk == null)
            return null;

        return sendChunk(Peer.id, file_id, chunk_number, chunk.content);
        //doesn't need to send the message to anyone was only one peer needs to answer
    }

    public static ChunkMessage sendChunk(Integer sender_id, String file_id, Integer chunk_no, byte[] chunk_data){
        ChunkMessage chunkMessage = new ChunkMessage(sender_id, file_id, chunk_no, chunk_data);

        String chunk_id = chunkMessage.getFileId() + "_" + chunkMessage.getChunkNo();
        Store.getInstance().addGetchunkReply(chunk_id);

        return processChunk(chunkMessage, chunk_id);
    }

    public static ChunkMessage processChunk(ChunkMessage chunkMessage, String chunk_id){
        if(!Store.getInstance().getGetchunkReply(chunk_id))
            return chunkMessage;
        Store.getInstance().removeGetchunkReply(chunk_id);
        return null;
    }


    //--------------------- ENHANCED VERSION ------------------
    public static void processGetChunkEnhancement(String file_id, int chunk_no){
        ServerSocket server_socket = null;

        try {
            server_socket = new ServerSocket(0);
        } catch (IOException e) {
            e.printStackTrace();
        }

        int port = server_socket.getLocalPort();

        String address;

        try {
            address = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return;
        }

        GetChunkEnhancementMessage message = new GetChunkEnhancementMessage(Peer.id,
                file_id, chunk_no, port , address);

      //  Peer.MC.sendMessage(message.convertMessage());

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

        ChunkMessage chunkMessage = new ChunkMessage(Peer.id, file_id, chunk_no, chunk.content);

        try {
            Socket socket = new Socket(InetAddress.getByName(message.getAddress()), message.getPort());

            ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
            objectOutputStream.writeObject(chunkMessage.convertMessage());
        } catch (IOException e) {
            e.printStackTrace();
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
        } catch (IOException | ClassNotFoundException | InvalidMessageException  e) {
            e.printStackTrace();

        }

    }
}
