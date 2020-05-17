package project.message;

import project.Macros;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Arrays.copyOfRange;


public class MessageParser {

    /**
     *
     * @param message bytes of the message
     * @param message_length length of the received message
     * @return initial position of the CRLF or -1 if not found
     */
    private static int getCRLFPosition(byte[] message, int message_length) {

        for (int i = 0; i < message_length - 3; ++i) {
            if (message[i] == Macros.CR && message[i + 1] == Macros.LF && message[i + 2] == Macros.CR && message[i + 3] == Macros.LF) {
                return i;
            }
        }

        return -1;
    }

    /**
     *
     * @param header String with header
     * @return list with header fields
     * @throws InvalidMessageException when Received invalid message header
     */
    private static List<String> getMessageHeaderFields( String header) throws InvalidMessageException {
        List<String> header_fields = Arrays.asList(header.split(" "));

        if (header_fields.size() < 2) {
            throw new InvalidMessageException("Received invalid message header");
        }
        return header_fields;
    }

    /**
     *
     * @param message complete message received
     * @param message_length size of the message receive
     * @param first_CRLF_position index of the first CRLF CRLF
     * @return chunk data
     */
    private static byte[] getMessageBody(byte[] message, int message_length, int first_CRLF_position){
        return copyOfRange(message, first_CRLF_position + 4, message_length);
    }

    /**
     *
     * @param message complete message received
     * @param message_length size of the message receive
     * @return Message that extends from BaseMessage
     * @throws InvalidMessageException when message isn't a valid Message type or format
     */
    public static BaseMessage parseMessage(byte[] message, int message_length) throws InvalidMessageException {
        int first_CRLF_position = getCRLFPosition(message, message_length);

        if (first_CRLF_position < 0) {
            throw new InvalidMessageException("Received invalid message: no CRLF");
        }

        List<String> message_header = getMessageHeaderFields(new String(message, 0, first_CRLF_position));

        switch (Message_Type.valueOf(message_header.get(0))) {
            case PUTCHUNK:
                return new PutChunkMessage(
                        new BigInteger(message_header.get(1).trim()), //key
                        message_header.get(2).trim(), //file_id
                        Integer.parseInt(message_header.get(3).trim()), //chunk_no
                        Integer.parseInt(message_header.get(4).trim()), //replication_dregree
                        getMessageBody(message, message_length, first_CRLF_position) //only send body
                );
            case STORED:
                return new StoredMessage(
                        new BigInteger(message_header.get(1).trim()), //key
                        message_header.get(2), //file_id
                        Integer.parseInt(message_header.get(3).trim()) //chunk_no
                        //message without a body
                );
            case GETCHUNK:
                return new GetChunkMessage(
                        new BigInteger(message_header.get(1).trim()), //key
                        message_header.get(2).trim(), //file_id
                        Integer.parseInt(message_header.get(3).trim()) //chunk_no
                        //message without a body
                );
            case CHUNK:
                return new ChunkMessage(
                        new BigInteger(message_header.get(1).trim()), //sender_id
                        message_header.get(2).trim(), //file_id
                        Integer.parseInt(message_header.get(3).trim()), //chunk_no
                        getMessageBody(message, message_length, first_CRLF_position) //only send body
                );
            case DELETE:
                return new DeleteMessage(
                        new BigInteger(message_header.get(1).trim()), //key
                        message_header.get(2).trim() //file_id
                        //message without a body
                );
            case DELETE_RECEIVED:
                return new DeleteReceivedMessage(
                        new BigInteger(message_header.get(1).trim()), //key
                        message_header.get(2).trim() //file_id
                        //message without a body
                );
            case REMOVED:
                return new RemovedMessage(
                        new BigInteger(message_header.get(1).trim()), //key
                        message_header.get(2).trim(), //file_id
                        Integer.parseInt(message_header.get(3).trim()) //chunk_no
                        //message without a body
                );
            case CONNECTION_REQUEST:
                return new ConnectionRequestMessage(
                        new BigInteger(message_header.get(2).trim()),
                        message_header.get(3).trim(),
                        Integer.parseInt(message_header.get(4).trim())
                );
            case CONNECTION_RESPONSE:
                return new ConnectionResponseMessage(
                        new BigInteger(message_header.get(1).trim()),
                        new BigInteger(message_header.get(2).trim()),
                        message_header.get(3).trim(),
                        Integer.parseInt(message_header.get(4).trim())
                );
            case REQUEST_PREDECESSOR:
                return new RequestPredecessorMessage(
                        new BigInteger(message_header.get(2).trim()),
                        message_header.get(3).trim(),
                        Integer.parseInt(message_header.get(4).trim())
                );
            case PREDECESSOR_RESPONSE:
                return new PredecessorResponseMessage(
                        new BigInteger(message_header.get(1).trim()),
                        getMessageBody(message, message_length, first_CRLF_position)
                );
            case NOTIFY_SUCCESSOR:
                return new NotifySuccessorMessage(
                        new BigInteger(message_header.get(1).trim()),
                        message_header.get(2).trim(),
                        Integer.parseInt(message_header.get(3).trim())
                );
            case SUCCESSOR_RESPONSE:
                return new SuccessorResponseMessage(
                        new BigInteger(message_header.get(1).trim()),
                        message_header.get(2).trim()
                );
            case FIND_PREDECESSOR:
            case FIND_SUCCESSOR:
                return new FindNodeMessage(
                        Message_Type.valueOf(message_header.get(0)),
                        new BigInteger(message_header.get(1).trim())
                );
            case PREDECESSOR:
            case SUCCESSOR:
                return new NodeMessage(
                        Message_Type.valueOf(message_header.get(0)),
                        new BigInteger(message_header.get(1).trim()),
                        message_header.get(2).trim(),
                        Integer.parseInt(message_header.get(3).trim())
                );
            case STABILIZE:
                return new StabilizeMessage(
                        new BigInteger(message_header.get(1).trim())
                );
            case STABILIZE_RESPONSE:
                return new StabilizeResponseMessage(
                        message_header.get(1).trim(),
                        new BigInteger(message_header.get(2).trim()),
                        message_header.get(3).trim(),
                        Integer.parseInt(message_header.get(4).trim())
                );
            default:
                throw new InvalidMessageException("Received invalid message type");

        }

    }
}
