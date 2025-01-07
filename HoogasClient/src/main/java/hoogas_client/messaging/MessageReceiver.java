package hoogas_client.messaging;

import hoogas_client.Constants;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;


/**
 * Receives messages over a TCP connection.  Messages can also be sent back to the other side of the connection using this class e.g. messages acks.
 * In order to be read each message send to this connection needs to be terminated by a {@link Constants#MSG_SEPARATOR_CHAR} character.
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
    private volatile BufferedReader reader;
    private volatile OutputStreamWriter writer;
    private final char[] receivedMsgBuffer;
    private String partlyReadMessage = "";


    MessageReceiver(int port) {
        this(port, DEFAULT_RECEIVED_MSG_BUFFER_SIZE);
    }

    MessageReceiver(int port, int receivedMessageBufferSize) {
        this.port = port;
        this.receivedMsgBuffer = new char[receivedMessageBufferSize];
    }

    /**
     * Should be called in order to initiate the connection.  This will block until a connection from a client is
     * received.  An exception will be thrown from the send and receive methods if this method has not been called
     * successfully beforehand.
     */
    public void connect() throws IOException {
        serverSocket = new ServerSocket(port);
        socket = serverSocket.accept();
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        writer = new OutputStreamWriter(socket.getOutputStream());
    }

    /**
     * Returns a list of all the latest messages, in the order in which they were sent, that will completely fit into the
     * received messages character buffer.
     * For example, if the buffer size is 8 and the message termination character is '¬' then the following input stream would result in 2 messages being returned from the first call
     * to this method, the second and third call would return an empty list, and the fourth call would return the third message:
     * ab¬cd¬efghijklmnoprstuvwxyz¬
     * The message termination character is not included in the returned messages.
     * @throws IOException If the socket can't be read e.g. it's been closed unexpectedly
     */
    public List<String> getReceivedMessages() throws IOException {

        var messageList = new ArrayList<String>();
        if(reader.ready()) {
            int numCharsRead = reader.read(receivedMsgBuffer, 0, receivedMsgBuffer.length);
            var messageStringBuilder = new StringBuilder(partlyReadMessage);


            for (int i = 0; i < numCharsRead; i++) {
                char c1 = receivedMsgBuffer[i];
                if (i == numCharsRead - 1) {
                    if (c1 != Constants.MSG_SEPARATOR_CHAR) {
                        //There's no termination character, so we've got a partly read message
                        messageStringBuilder.append(c1);
                        partlyReadMessage = messageStringBuilder.toString();
                    } else {
                        partlyReadMessage = "";
                        messageList.add(messageStringBuilder.toString());
                    }
                } else if (c1 == Constants.MSG_SEPARATOR_CHAR) {
                    messageList.add(messageStringBuilder.toString());
                    messageStringBuilder = new StringBuilder();
                } else {
                    messageStringBuilder.append(c1);
                }

                receivedMsgBuffer[i] = '\u0000';
            }
        }
        return messageList;
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