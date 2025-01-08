package hoogas_client.messaging;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;


/**
 * Used for sending messages to a destination.  This is similar to the {@link MessageSender} except this class uses an
 * in-memory buffer to store the messages until they are sent, which will be immediately if there are no issues, but if
 * there are e.g. the connection has gone down, then it allows the messages to be buffered and sent once the connection comes back up.
 * It also means that messages can be submitted before the connection is established and will be sent once that happens.
 * This functionality allows for the toleration of downtime either during normal running or, for example, on application start-up in the case
 * where the application using this class starts up before the receiving application.
 * Classes can use the {@link ConnectionListener} instance they provide when creating this class to receive events on the connection.
 * @see MessageReceiver
 * @author Joshua Newman, January 2025
 */
public class BufferedMessageSender implements AutoCloseable, Runnable {


    private final ArrayBlockingQueue<String> buffer;
    private volatile boolean shutdown;
    private final Thread sendingRoutineThread;
    private final ConnectionListener connectionListener;
    private final int connectionId;
    private final String host;
    private final int port;
    private final long timeout;
    protected Socket socket;
    private HoogasMessageWriter hoogasMessageWriter;
    private static final int DEFAULT_QUEUE_SIZE = 1000;


    /**
     * @param host The address of the receiving application
     * @param port The port that the receiving application will be listening for connections on
     * @param timeout How long the connection can be down before a timeout event is published on the provided listener
     * @param connectionId Unique id for this connection
     * @param connectionListener Receives connection events
     */
    BufferedMessageSender(String host, int port, long timeout, int connectionId, ConnectionListener connectionListener) {
        buffer = new ArrayBlockingQueue<>(DEFAULT_QUEUE_SIZE);
        sendingRoutineThread = new Thread(this);
        sendingRoutineThread.setDaemon(false);
        sendingRoutineThread.setName("SendingRoutine_" + BufferedMessageSender.class.getName() + " " + connectionId);
        this.connectionListener = connectionListener;
        this.connectionId = connectionId;
        this.host = host;
        this.port = port;
        this.timeout = timeout;
    }


    /**
     * Initializes the instance.  Should be called before any sending operations are performed.
     * Classes should call this method and then wait for the associated {@link ConnectionListener#onConnectionEstablished(int)}
     * event or the {@link ConnectionListener#onConnectionTimedOut(int)} event on the {@link ConnectionListener} instance
     * they provided in the constructor before sending anything.  If the connection is lost then a {@link ConnectionListener#onConnectionLost(int)}
     * event will be received.
     * @throws IllegalArgumentException If this method is called more than once or if the instance has been shut-down
     */
    public void start() {
        if(sendingRoutineThread.isAlive()) {
            throw new IllegalArgumentException(this.getClass().getName() + " has already been started");
        }
        if(shutdown) {
            throw new IllegalArgumentException(this.getClass().getName() + " has been shut-down");
        }
        sendingRoutineThread.start();
    }

    /**
     * Sends the given message.  Messages are appended with {@link hoogas_client.Constants#MSG_SEPARATOR_CHAR} so as they
     * can be read by a {@link MessageReceiver} for example.
     * @see MessageReceiver
     */
    public void send(String message) {
        if(shutdown) {
            throw new IllegalArgumentException(this.getClass().getName() + " has been shut-down");
        }
        if(!sendingRoutineThread.isAlive()) {
            throw new IllegalArgumentException("Start the " + this.getClass().getName() + " first!");
        }
        try {
            buffer.put(message);
        }
        catch(InterruptedException e) {
            //¬log properly!
            System.out.println("Thread putting message in queue interrupted.  Message was: " + message);
        }
    }

    @Override
    public void run() {

        var connected = tryConnect();

        while(connected && !shutdown) {
            if(socketConnected()) {
                try {
                    var msg = buffer.peek();
                    if (msg != null) {
                        hoogasMessageWriter.doSend(msg);
                        buffer.remove();
                    }
                }
                catch(IOException e) {
                        connectionListener.onConnectionLost(connectionId);
                        connected = tryConnect();
                    }
            }
            else {
                connectionListener.onConnectionLost(connectionId);
                connected = tryConnect();
            }
        }
        if(socket != null) {
            try {
                socket.close();
            } catch(IOException e) {
                //¬log the exception
            }
        }
    }

    boolean socketConnected() {
        return socket.isConnected();
    }

    //Attempts to get the connection
    protected boolean tryConnect() {

        boolean timedout = false;
        long startedAt = System.currentTimeMillis();
        while (!shutdown && !timedout && (socket == null || !socketConnected())) {
            try {
                createConnectionObjects();
            } catch (Exception e) {}
            if(socket == null || !socketConnected()) {
                try {Thread.sleep(200);} catch(InterruptedException e) {}
                timedout = (System.currentTimeMillis() - startedAt) >= timeout;
            }
        }
        if(!shutdown) {
            if(!timedout) {
                connectionListener.onConnectionEstablished(connectionId);
            }
            else {
                connectionListener.onConnectionTimedOut(connectionId);
            }
            return !timedout;
        }
        return false;
    }

    protected void createConnectionObjects() throws IOException {
        socket = new Socket(host, port);
        hoogasMessageWriter = new HoogasMessageWriter(new OutputStreamWriter(socket.getOutputStream()));
    }

    /**
     * Should be called to shut-down this instance
     */
    public void close() throws Exception {
        shutdown = true;
        sendingRoutineThread.join(2000);
    }
}
