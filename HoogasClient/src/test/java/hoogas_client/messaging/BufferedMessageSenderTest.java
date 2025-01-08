package hoogas_client.messaging;

import hoogas_client.Constants;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class BufferedMessageSenderTest {


    /**
     * Test the basics.  Send some high frequency bursts and make sure they come out the other end intact
     */
    @Test
    public void test1() throws Exception {
        var testMessageSenderListener = new TestConnectionListener();
        try(BufferedMessageSender bufferedMessageSender = new BufferedMessageSender("localhost", 9000, 1000, 1234, testMessageSenderListener);
            TestMessageReceiver testMessageReceiver = new TestMessageReceiver(9000)) {

            var connectionEstablishedCountDownLatch = new CountDownLatch(1);
            testMessageSenderListener.connectionEstablishedLatches.add(connectionEstablishedCountDownLatch);

            bufferedMessageSender.start();

            try(var singleThreadExecutor = Executors.newSingleThreadExecutor()) {
                singleThreadExecutor.submit(() -> {testMessageReceiver.connect();});

                assertTrue(connectionEstablishedCountDownLatch.await(1000, TimeUnit.MILLISECONDS), "Message sender didn't connect to the other side");

                sendTestMessages(bufferedMessageSender, 100, 8, 20);
                Thread.sleep(100);

                var received = testMessageReceiver.getReceived(2000);
                checkTestMessages(received, 100);
            }
        }
    }

    /**
     * Send some messages before the sender can connect to the other side and ensure that when the sender finally connects the messages are still sent.
     */
    @Test
    public void test2() throws Exception {
        var testMessageSenderListener = new TestConnectionListener();
        try(BufferedMessageSender bufferedMessageSender = new BufferedMessageSender("localhost", 9000, 10000, 1234, testMessageSenderListener);
            TestMessageReceiver testMessageReceiver = new TestMessageReceiver(9000)) {

            var connectionEstablishedCountDownLatch = new CountDownLatch(1);
            testMessageSenderListener.connectionEstablishedLatches.add(connectionEstablishedCountDownLatch);

            //Start the sender but delay the starting of the receiver and send some messages during that time

            bufferedMessageSender.start();
            try(var singleThreadExecutor = Executors.newSingleThreadExecutor()) {
                singleThreadExecutor.submit(() -> {
                    doSleepThatShouldntBeInterrupted(1000);
                    testMessageReceiver.connect();
                });

                sendTestMessages(bufferedMessageSender, 100, 0, 0);

                //Make sure that the connection still hasn't been made so the messages were all sent while the sender wasn't connected
                assertEquals(testMessageSenderListener.connectionEstablishedLatches.getFirst().getCount(),1);

                //Wait to be connected and then ensure that the messages have come through

                assertTrue(connectionEstablishedCountDownLatch.await(2000, TimeUnit.MILLISECONDS), "Message sender didn't connect to the other side");

                Thread.sleep(200);

                var received = testMessageReceiver.getReceived(2000);
                checkTestMessages(received, 100);
            }
        }
    }

    /**
     * Tests the close method making sure that everything that's supposed to be shut-down is shut-down and that the shut-down instance can't be used to send messages and that it cannot be
     * started again
     */
    @Test
    public void test3() throws Exception {
        var testMessageSenderListener = new TestConnectionListener();
        try(BufferedMessageSender bufferedMessageSender = new BufferedMessageSender("localhost", 9000, 1000, 1234, testMessageSenderListener);
            TestMessageReceiver testMessageReceiver = new TestMessageReceiver(9000)) {

            var connectionEstablishedCountDownLatch = new CountDownLatch(1);
            testMessageSenderListener.connectionEstablishedLatches.add(connectionEstablishedCountDownLatch);

            bufferedMessageSender.start();

            boolean exceptionCaught = false;
            try {
                bufferedMessageSender.start();
            }
            catch(IllegalArgumentException e) {
                exceptionCaught = true;
            }
            assertTrue(exceptionCaught);

            try(var singleThreadExecutor = Executors.newSingleThreadExecutor()) {
                singleThreadExecutor.submit(() -> {testMessageReceiver.connect();});

                assertTrue(connectionEstablishedCountDownLatch.await(1000, TimeUnit.MILLISECONDS), "Message sender didn't connect to the other side");

                Thread.sleep(1000);

                //shut it down
                bufferedMessageSender.close();

                assertTrue(bufferedMessageSender.socket.isClosed());

                exceptionCaught = false;
                try {
                    bufferedMessageSender.send("Shouldn't work");
                }
                catch(IllegalArgumentException e) {
                    exceptionCaught = true;
                }
                assertTrue(exceptionCaught);

                exceptionCaught = false;
                try {
                    bufferedMessageSender.start();
                }
                catch(IllegalArgumentException e) {
                    exceptionCaught = true;
                }
                assertTrue(exceptionCaught);
            }
        }
    }

    private static void sendTestMessages(
            BufferedMessageSender bufferedMessageSender,
            int howMany, int waitEveryXMessage,
            long timeBetweenBursts) throws InterruptedException {

        var messages = new String[howMany];
        var prefix = "message_";
        for(int i = 0 ; i < messages.length; i++) {
            var message = prefix + i;
            messages[i] = message;
        }

        for(int i = 0; i < messages.length; i++) {
            bufferedMessageSender.send(messages[i]);
            if(waitEveryXMessage > 0 && i % waitEveryXMessage == 0) {
                Thread.sleep(timeBetweenBursts);
            }
        }
    }

    private static void checkTestMessages(String received, int howMany) {
        assertNotNull(received);
        var split = received.split(new String(new char[]{Constants.MSG_SEPARATOR_CHAR}));
        assertEquals(split.length, howMany);
        var prefix = "message_";
        for(int i = 0; i < split.length; i++) {
            assertEquals(prefix + i, split[i]);
        }
    }

    //Saves all typing and confusing-looking code
    private static void doSleepThatShouldntBeInterrupted(long ms) {
        try {
            Thread.sleep(ms);
        }
        catch(InterruptedException e) {
            throw new IllegalStateException(e);
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

    private static class TestMessageReceiver implements AutoCloseable {

        private Socket socket;
        private ServerSocket serverSocket;
        private final int port;

        private TestMessageReceiver(int port) throws IOException {
            this.port = port;
        }

        private void connect() {
            try {
                serverSocket = new ServerSocket(port);
                socket = serverSocket.accept();
                socket.setSoLinger(true, 0);
            }
            catch(Exception e) {
                e.printStackTrace();
            }
        }

        private String getReceived(int chunkSize) throws IOException {
            char[] c = new char[chunkSize];
            int numCharsRead = new InputStreamReader(socket.getInputStream()).read(c);
            return new String(Arrays.copyOfRange(c, 0, numCharsRead));
        }

        @Override
        public void close() throws IOException {
            socket.close();
            serverSocket.close();
        }
    }
}
