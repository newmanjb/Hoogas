package hoogas_client.messaging;

import hoogas_client.Constants;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;


/**
 * Can be used to send messages, either locally or across the network.
 * Each message is appended with the message separator character {@link Constants#MSG_SEPARATOR_CHAR}.
 * @see MessageReceiver
 * @author Joshua Newman, January 2025
 */
public class MessageSender implements AutoCloseable {


    private final String host;
    private final int port;
    protected volatile Socket socket;
    private volatile OutputStreamWriter outputStreamWriter;


    MessageSender(String host, int port) {
        this.host = host;
        this.port = port;
    }


    public void connect() throws IOException {
        socket = new Socket(host, port);
        outputStreamWriter = new OutputStreamWriter(socket.getOutputStream());
    }

    public void send(String message) throws IOException {
        doSend(message);
    }

    protected void doSend(String message) throws IOException {
        outputStreamWriter.write(message + Constants.MSG_SEPARATOR_CHAR);
        outputStreamWriter.flush();
    }

    @Override
    public void close() throws IOException {
        if(socket != null) {
            socket.close();
        }
    }
}
