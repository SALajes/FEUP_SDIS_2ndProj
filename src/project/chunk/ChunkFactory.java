package project.chunk;

import project.Macros;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;

import static java.util.Arrays.copyOfRange;

public class ChunkFactory {
    private final File file;
    private final int replication_degree;
    private ArrayList<Chunk> chunks;

    public ChunkFactory(File file, int replication_degree) {
        this.file = file;
        this.replication_degree = replication_degree;

        chunks = new ArrayList<>();

        produceChunks();
    }

    private void produceChunks() {
        int chunk_no = 0;

        byte[] buffer = new byte[Macros.CHUNK_MAX_SIZE];

        try(BufferedInputStream stream = new BufferedInputStream(new FileInputStream(this.file))) {
            int size;
            while((size = stream.read(buffer)) > 0){
                Chunk chunk = new Chunk(chunk_no, Arrays.copyOf(buffer, size), size);

                this.chunks.add(chunk);

                chunk_no++;

                buffer = new byte[Macros.CHUNK_MAX_SIZE];
            }
            //check if needs 0 size chunk
            if(chunks.get(chunks.size() - 1).size == Macros.CHUNK_MAX_SIZE) {
                // If the file size is a multiple of the chunk size, the last chunk has size 0.
                this.chunks.add(new Chunk(chunks.size(), new byte[0], 0));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     *
     * @return
     * chunks
     */
    public ArrayList<Chunk> getChunks(){
        return chunks;
    }
}
