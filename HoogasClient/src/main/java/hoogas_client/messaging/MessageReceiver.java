package hoogas_client.messaging;

import hoogas_client.Constants;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;


/**
 * Receives messages over a TCP connection.  Messages can also be sent back to the other side of the connection using this class e.g. message acks.
 * In order to be read each message sent to this connection needs to be terminated by a {@link Constants#MSG_SEPARATOR_CHAR} character.
 * For example, if the terminating character is '¬' then in order to provide 3 messages the receiver should read the following string from the socket's input stream:
 * \"msg1_characters¬msg2_characters¬msg3_characters¬\".
 * Messages are read from the stream into a fixed-size character buffer, the size of which can be provided in the constructor or else the default of {@link MessageReceiver#DEFAULT_RECEIVED_MSG_BUFFER_SIZE} is used.
 * @see #getReceivedMessages()
 * @author Joshua Newman, January 2025
 */
public class MessageReceiver implements AutoCloseable {


    private static final int DEFAULT_RECEIVED_MSG_BUFFER_SIZE = 1000;

    private final int port;
    protected volatile ServerSocket serverSocket;
    protected volatile Socket socket;
    private volatile OutputStreamWriter writer;
    private HoogasMessageReader messageReader;
    private final int inputStreamReadingChunkSize;


    MessageReceiver(int port) {
        this(port, DEFAULT_RECEIVED_MSG_BUFFER_SIZE);
    }

    MessageReceiver(int port, int inputStreamReadingChunkSize) {
        this.port = port;
        this.inputStreamReadingChunkSize = inputStreamReadingChunkSize;
    }

    /**
     * Should be called in order to initiate the connection.  This will block until a connection from a client is
     * received.  An exception will be thrown from the send and receive methods if this method has not been called
     * successfully beforehand.
     */
    public void connect() throws IOException {
        serverSocket = new ServerSocket(port);
        socket = serverSocket.accept();
        messageReader = new HoogasMessageReader(new InputStreamReader(socket.getInputStream()), inputStreamReadingChunkSize);
        writer = new OutputStreamWriter(socket.getOutputStream());
    }

    /**
     * @see HoogasMessageReader#getReceivedMessages()
     */
    public List<String> getReceivedMessages() throws IOException {
        return messageReader.getReceivedMessages();
    }

    /**
     * Sends the given message to the other side of this connection.
     * @throws IOException If the message cannot be sent e.g. if the connection on the other side has been lost.
     */
    public void sendMessage(String msg) throws IOException {
        writer.write(msg);
        writer.flush();
    }

    @Override
    public void close() throws IOException {
        if(socket != null) {
            socket.close();
        }
        if(serverSocket != null) {
            serverSocket.close();
        }
    }
}