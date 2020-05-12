package project.peer;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;

public class ChordNodeInfo {
    private final String IP = InetAddress.getLocalHost().getHostAddress();
    private final String key = UUID.randomUUID().toString();
    private final int port;

    public ChordNodeInfo(int port) throws UnknownHostException {
        this.port = port;
    }

    public int getPort() {
        return port;
    }

    public String getIP() {
        return IP;
    }

    public String getKey() {
        return key;
    }
}
