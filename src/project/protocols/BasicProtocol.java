package project.protocols;

import project.message.BaseMessage;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.io.ObjectOutputStream;

import static project.Macros.checkPort;

public class BasicProtocol {

    protected static SSLSocket sslSocket = null;

    protected static boolean openSocket() {
        //TODO: change with chord
        String host = "this.peer";
        Integer port = 1025;

        if(checkPort(port)){
            return false;
        }

        try {
            sslSocket = (SSLSocket) SSLSocketFactory.getDefault().createSocket(host, port);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }


    protected static boolean sendWithTCP(BaseMessage message)  {

        try {
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(sslSocket.getOutputStream());
            objectOutputStream.writeObject(message.convertMessage());
        } catch (IOException e) {
            return false;
        }

        return true;
    }

    protected boolean closeSocket() {

        try {
            System.out.println("Client: Server shut down output: closing");
            sslSocket.close();
            sslSocket.shutdownOutput();
        } catch (IOException e){
            e.printStackTrace();
            return false;

        }
        return true;
    }
}
