package project;

public class Macros {
    public final static byte CR = 0xD;
    public final static byte LF = 0xA;

    public static final Double VERSION = 1.0;

    public static final Integer TTL = 1;

    public static final Integer CHUNK_MAX_SIZE = 64000; //in bytes
    public static final Integer MAX_NUMBER_CHUNKS = 1000000;
    public static final long MAX_FILE_SIZE = ((long) Macros.MAX_NUMBER_CHUNKS) * ((long) Macros.CHUNK_MAX_SIZE);

    public static final Integer MAX_MESSAGE_SIZE = 64200;

    public static final long INITIAL_STORAGE = 1000000000;
}
