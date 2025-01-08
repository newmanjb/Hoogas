package hoogas_client.messaging;

import hoogas_client.Constants;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

public class MessageSenderTest {


    /**
     * Send several bursts through and make sure makes sure they make it to the other side
     */
    @Test
    public void test1() throws Exception {
        try(var testMessageReceiver = new TestMessageReceiver(9000);
            var messageSender = new MessageSender("localhost", 9000)) {

            var t = new Thread(() -> {testMessageReceiver.connect();});
            t.start();
            Thread.sleep(200);
            messageSender.connect();
            t.join(1000);

            var messages = new String[100];
            var prefix = "message_";
            for(int i = 0 ; i < messages.length; i++) {
                var message = prefix + i;
                messages[i] = message;
            }

            for(int i = 0; i < messages.length; i++) {
                messageSender.send(messages[i]);
                if(i % 8 == 0) {
                    Thread.sleep(20);
                }
            }

            var received = testMessageReceiver.getReceived(2000);
            assertNotNull(received);
            var split = received.split(new String(new char[]{Constants.MSG_SEPARATOR_CHAR}));
            assertEquals(split.length, 100);
            for(int i = 0; i < split.length; i++) {
                assertEquals(prefix + i, split[i]);
            }
        }
    }

    /**
     * Make sure attempts to send anything when not connected result in an exception
     */
    @Test
    public void test2() throws Exception {
        try (var messageSender = new MessageSender("localhost", 9000)) {

            boolean exceptionCaught = false;
            try {
                messageSender.send("Shouldn't work");
            }
            catch(Exception e) {
                exceptionCaught = true;
            }

            assertTrue(exceptionCaught);
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
