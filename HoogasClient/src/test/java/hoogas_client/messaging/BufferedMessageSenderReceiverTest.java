package hoogas_client.messaging;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;


/**
 * Tests for the {@link BufferedMessageSenderReceiver} class.  Currently very simple, as most of it is covered in
 * {@link BufferedMessageSenderTest} which is this classes superclass, and in {@link MessageReceiverTest} which tests
 * {@link MessageReceiver} that uses {@link HoogasMessageReader} to read messages in the same way {@link BufferedMessageSenderReceiver} does.
 * @author Joshua Newman, January 2024
 */
public class BufferedMessageSenderReceiverTest {


    /**
     * Send some messages and make sure they come out of the other side.
     */
    @Test
    public void test1() throws Exception {

        var testConnectionListener = new TestConnectionListener();
        try(BufferedMessageSenderReceiver bufferedMessageSenderReceiver = new BufferedMessageSenderReceiver("localhost", 9000, 10000, 1234, testConnectionListener, 2000);
            TestMessageSender testMessageSender = new TestMessageSender(9000);) {

            var connectionEstablishedLatch = new CountDownLatch(1);
            testConnectionListener.connectionEstablishedLatches.add(connectionEstablishedLatch);
            bufferedMessageSenderReceiver.start();

            try(var executorService = Executors.newSingleThreadExecutor()) {

                executorService.submit(() -> {
                    try {
                        testMessageSender.connect();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });

                assertTrue(connectionEstablishedLatch.await(1000, TimeUnit.MILLISECONDS), "Connection to the other side failed");

                var message = "Hello World¬";
                testMessageSender.send(message);

                Thread.sleep(200);

                var received = bufferedMessageSenderReceiver.getLatestMessagesReceived();
                assertNotNull(received);
                assertEquals(1, received.size());
                assertEquals(message.substring(0, message.length() - 1), received.getFirst());
            }
        }
    }

    private static class TestMessageSender implements AutoCloseable {

        private final int port;
        private ServerSocket serverSocket;
        private Socket socket;

        public TestMessageSender(int port) {
            this.port = port;
        }

        private void connect() throws IOException {
            serverSocket = new ServerSocket(port);
            socket = serverSocket.accept();
        }

        private void send(String msg) throws IOException {
            socket.getOutputStream().write(msg.getBytes());
            socket.getOutputStream().flush();
        }

        @Override
        public void close() throws IOException {
            socket.close();
            serverSocket.close();
        }
    }

    private static class TestConnectionListener implements ConnectionListener {


        private final List<CountDownLatch> connectionLostLatches = new ArrayList<>();
        private final List<CountDownLatch> connectionEstablishedLatches = new ArrayList<>();
        private final List<CountDownLatch> connectionTimedoutLatches = new ArrayList<>();


        @Override
        public void onConnectionLost(int connectionId) {
            for(var countdownLatch : connectionLostLatches) {
                countdownLatch.countDown();
            }
        }

        @Override
        public void onConnectionEstablished(int connectionId) {
            for(var countdownLatch : connectionEstablishedLatches) {
                countdownLatch.countDown();
            }
        }

        @Override
        public void onConnectionTimedOut(int connectionId) {
            for(var countdownLatch : connectionTimedoutLatches) {
                countdownLatch.countDown();
            }
        }
    }

}