package hoogas_client.messaging;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

import static org.junit.jupiter.api.Assertions.*;
import static hoogas_client.Constants.MSG_SEPARATOR_CHAR;

public class MessageReceiverTest {


    /**
     * Send a string consisting of variable length messages, some larger than the buffer size, some the same and some
     * smaller, and make sure the {@link MessageReceiver#getReceivedMessages()} calls fetch the correct messages.
     */
    @Test
    public void test1() throws Exception {

        try(TestMessageSender messageSender = new TestMessageSender("localhost", 9000);
            MessageReceiver messageReceiver = new MessageReceiver(9000, 5);) {

            Thread t = new Thread(() -> {
                try {
                    messageReceiver.connect();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            t.start();
            Thread.sleep(200);
            messageSender.connect();
            t.join(1000);

            var messages = new String[]{"abcd" + MSG_SEPARATOR_CHAR + "abcd" + MSG_SEPARATOR_CHAR + "abcdefghij" + MSG_SEPARATOR_CHAR + "abc" + MSG_SEPARATOR_CHAR + "abcdefghi" + MSG_SEPARATOR_CHAR +
                    "abcd" + MSG_SEPARATOR_CHAR + "abcdefghij" + MSG_SEPARATOR_CHAR + "k" + MSG_SEPARATOR_CHAR};
            messageSender.send(messages[0]);

            Thread.sleep(200);

            var receivedMsgs = messageReceiver.getReceivedMessages();
            assertNotNull(receivedMsgs);
            assertEquals(receivedMsgs.size(), 1);
            assertEquals(receivedMsgs.getFirst(), "abcd");

            receivedMsgs = messageReceiver.getReceivedMessages();
            assertNotNull(receivedMsgs);
            assertEquals(receivedMsgs.size(), 1);
            assertEquals(receivedMsgs.getFirst(), "abcd");

            receivedMsgs = messageReceiver.getReceivedMessages();
            assertNotNull(receivedMsgs);
            assertEquals(receivedMsgs.size(), 0);

            receivedMsgs = messageReceiver.getReceivedMessages();
            assertNotNull(receivedMsgs);
            assertEquals(receivedMsgs.size(), 0);

            receivedMsgs = messageReceiver.getReceivedMessages();
            assertNotNull(receivedMsgs);
            assertEquals(receivedMsgs.size(), 2);
            assertEquals(receivedMsgs.getFirst(), "abcdefghij");
            assertEquals(receivedMsgs.get(1), "abc");

            receivedMsgs = messageReceiver.getReceivedMessages();
            assertNotNull(receivedMsgs);
            assertEquals(receivedMsgs.size(), 0);

            receivedMsgs = messageReceiver.getReceivedMessages();
            assertNotNull(receivedMsgs);
            assertEquals(receivedMsgs.size(), 1);
            assertEquals(receivedMsgs.getFirst(), "abcdefghi");

            receivedMsgs = messageReceiver.getReceivedMessages();
            assertNotNull(receivedMsgs);
            assertEquals(receivedMsgs.size(), 1);
            assertEquals(receivedMsgs.getFirst(), "abcd");

            receivedMsgs = messageReceiver.getReceivedMessages();
            assertNotNull(receivedMsgs);
            assertEquals(receivedMsgs.size(), 0);

            receivedMsgs = messageReceiver.getReceivedMessages();
            assertNotNull(receivedMsgs);
            assertEquals(receivedMsgs.size(), 0);

            receivedMsgs = messageReceiver.getReceivedMessages();
            assertNotNull(receivedMsgs);
            assertEquals(receivedMsgs.size(), 2);
            assertEquals(receivedMsgs.getFirst(), "abcdefghij");
            assertEquals(receivedMsgs.get(1), "k");
        }
    }


    /**
     * Send messages that are smaller than the receiver's buffer size and make sure that the {@link MessageReceiver#getReceivedMessages()} method
     * performs correctly.
     */
    @Test
    public void test2() throws Exception {

        try(TestMessageSender messageSender = new TestMessageSender("localhost", 9000);
            MessageReceiver messageReceiver = new MessageReceiver(9000, 10);) {

            Thread t = new Thread(() -> {
                try {
                    messageReceiver.connect();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            t.start();
            Thread.sleep(200);
            messageSender.connect();
            t.join(1000);

            var messages = new String[]{"abcd" + MSG_SEPARATOR_CHAR, "abcd" + MSG_SEPARATOR_CHAR + "ef" + MSG_SEPARATOR_CHAR};
            messageSender.send(messages[0]);

            Thread.sleep(200);

            var receivedMsgs = messageReceiver.getReceivedMessages();
            assertNotNull(receivedMsgs);
            assertEquals(receivedMsgs.size(), 1);
            assertEquals(receivedMsgs.getFirst(), "abcd");

            messageSender.send(messages[1]);

            receivedMsgs = messageReceiver.getReceivedMessages();
            assertNotNull(receivedMsgs);
            assertEquals(receivedMsgs.size(), 2);
            assertEquals(receivedMsgs.getFirst(), "abcd");
            assertEquals(receivedMsgs.get(1), "ef");
        }
    }

    /**
     * Check that the {@link MessageReceiver} is able to send messages to the sender
     */
    @Test
    public void test3() throws Exception {
        try(TestMessageSender messageSender = new TestMessageSender("localhost", 9000);
            MessageReceiver messageReceiver = new MessageReceiver(9000, 5);) {

            Thread t = new Thread(() -> {
                try {
                    messageReceiver.connect();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            t.start();
            Thread.sleep(200);
            messageSender.connect();
            t.join(1000);

            messageReceiver.sendMessage("Hello" + MSG_SEPARATOR_CHAR);
            Thread.sleep(200);
            var msg = messageSender.getReceivedMessage();
            assertNotNull(msg);
            assertEquals("Hello" + MSG_SEPARATOR_CHAR, msg);
            messageReceiver.sendMessage(" sender" + MSG_SEPARATOR_CHAR);
            Thread.sleep(200);
            msg = messageSender.getReceivedMessage();
            assertNotNull(msg);
            assertEquals(" sender" + MSG_SEPARATOR_CHAR, msg);
        }
    }

    /**
     * Check that any attempts to send or receive when not connected result in an exception.
     */
    @Test
    public void test4() throws Exception {
        try(TestMessageSender messageSender = new TestMessageSender("localhost", 9000);
            MessageReceiver messageReceiver = new MessageReceiver(9000, 5);) {

            checkExceptionThrown(() -> messageReceiver.sendMessage("Shouldn't work"), true);
            checkExceptionThrown(() -> messageReceiver.getReceivedMessages(), true);

            Thread t = new Thread(() -> {
                try {
                    messageReceiver.connect();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            t.start();
            Thread.sleep(200);
            messageSender.connect();
            t.join(1000);

            checkExceptionThrown(() -> messageReceiver.sendMessage("Shouldn't work"), false);
            checkExceptionThrown(() -> messageReceiver.getReceivedMessages(), false);
        }
    }


    private interface IOExceptionChucker {
        void run() throws IOException;
    }

    private void checkExceptionThrown(IOExceptionChucker ioExceptionChucker, boolean shouldBeThrown) {
        boolean exceptionThrown = false;
        try {
            ioExceptionChucker.run();
        }
        catch(Exception e) {
            exceptionThrown = true;
        }
        assertEquals(shouldBeThrown, exceptionThrown);
    }

    private class TestMessageSender implements AutoCloseable {
        private final String host;
        private final int port;
        private Socket socket;

        public TestMessageSender(String host, int port) {
            this.host = host;
            this.port = port;
        }

        private void connect() throws IOException {
            socket = new Socket(host, port);
        }

        private void disconnect() throws IOException {
            socket.shutdownInput();
            socket.shutdownOutput();
            socket.close();
        }

        private void send(String msg) throws IOException {
            socket.getOutputStream().write(msg.getBytes());
            socket.getOutputStream().flush();
        }

        private String getReceivedMessage() throws IOException {
            char[] c = new char[100];
            int numCharsRead = new BufferedReader(new InputStreamReader(socket.getInputStream())).read(c);
            var sb = new StringBuilder();
            for(int i = 0; i < numCharsRead; i++) {
                sb.append(c[i]);
            }
            return sb.toString();
        }

        @Override
        public void close() throws IOException {
            if(socket != null) {
                socket.close();
            }
        }
    }
}