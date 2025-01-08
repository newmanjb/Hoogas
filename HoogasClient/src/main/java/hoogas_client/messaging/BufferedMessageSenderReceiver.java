package hoogas_client.messaging;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

/**
 * Extension of {@link BufferedMessageSender} that adds the ability to receive messages from the other side of the connection.
 * @author Joshua Newman, January 2025
 */
public class BufferedMessageSenderReceiver extends BufferedMessageSender {


    private HoogasMessageReader messageReader;
    private final int inputStreamReadingChunkSize;

    /***
     * @param host see {@link BufferedMessageSender}
     * @param port see {@link BufferedMessageSender}
     * @param timeout see {@link BufferedMessageSender}
     * @param connectionId see {@link BufferedMessageSender}
     * @param connectionListener see {@link BufferedMessageSender}
     * @param inputStreamReadingChunkSize see {@link HoogasMessageReader#HoogasMessageReader(InputStreamReader, int)}
     */
    public BufferedMessageSenderReceiver(String host, int port, long timeout, int connectionId, ConnectionListener connectionListener, int inputStreamReadingChunkSize) {
        super(host, port, timeout, connectionId, connectionListener);
        this.inputStreamReadingChunkSize = inputStreamReadingChunkSize;
    }

    @Override
    protected void createConnectionObjects() throws IOException {
        super.createConnectionObjects();
        messageReader = new HoogasMessageReader(new InputStreamReader(socket.getInputStream()), inputStreamReadingChunkSize);
    }

    /**
     * @see HoogasMessageReader#getReceivedMessages()
     */
    public List<String> getLatestMessagesReceived() throws IOException {
        return messageReader.getReceivedMessages();
    }
}
