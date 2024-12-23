package hoogas_client;

import com.noomtech.hoogas_shared.constants.SharedConstants;
import com.noomtech.hoogas_shared.internal_messaging.MessageTypeFromApplications;
import com.noomtech.hoogas_shared.internal_messaging.MessageTypeToApplications;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;


public class HoogasClientTest {


    private static String installationDir;


    @BeforeAll
    public static void beforeAll() throws Exception {
        installationDir = System.getProperty("user.dir");
        System.setProperty("installation_dir", installationDir);
    }

    @AfterAll
    public static void afterAll() throws Exception {
        System.clearProperty("installation_dir");
    }

    @BeforeEach
    public void beforeEach() throws Exception {
        var appDir = new File(installationDir + File.separator + SharedConstants.APPLICATIONS_DIR_NAME + File.separator + "TestApp1");
        if(!appDir.mkdirs()) {
            throw new IllegalStateException("Could not create directory: " + appDir.getPath());
        }
    }

    @AfterEach
    public void afterEach() throws Exception {
        var appDir = new File(installationDir + File.separator + SharedConstants.APPLICATIONS_DIR_NAME);
        FileUtils.deleteDirectory(appDir);
    }


    //Test that we can start the client and that it
    // 1: creates the messaging directories
    // 2: picks up the public properties and returns them from the start call
    // 3: subsequently provides notifications when a new message from hoogas comes in
    // 4: Throws an exception if another attempt at initialisation is made
    @Test
    public void test1() throws Exception {
        var countDownLatch1 = new CountDownLatch(1);
        var countDownLatch2 = new CountDownLatch(1);
        final Map<String,String>[] configUpdateArray = new Map[1];
        var hoogasMessageListener = new HoogasMessageListener(){
            @Override
            public void onPublicConfigUpdate(Map<String,String> configUpdate) {
                configUpdateArray[0] = configUpdate;
                countDownLatch1.countDown();
            }
            @Override
            public void onStop(){
                countDownLatch2.countDown();
            }
        };
        var messageSender = new MessageSender();
        var messageSenderThread = new Thread(messageSender);
        messageSenderThread.setDaemon(true);
        messageSenderThread.start();
        try {

            var publicConfig = HoogasClient.init("TestApp1", hoogasMessageListener);

            assertNotNull(publicConfig);
            assertEquals(2, publicConfig.size());
            var testing1 = publicConfig.get("testing1");
            assertNotNull(testing1);
            assertEquals("1234", testing1);
            var testing2 = publicConfig.get("testing2");
            assertNotNull(testing2);
            assertEquals( "5678", testing2);

            messageSender.sendConfigUpdate = true;
            assertTrue(countDownLatch1.await(3000, TimeUnit.MILLISECONDS));

            var configUpdate = configUpdateArray[0];
            assertNotNull(configUpdate);
            assertEquals(configUpdate.size(), 2);
            testing1 = configUpdate.get("testing1");
            assertNotNull(testing1);
            assertEquals("9101112", testing1);
            testing2 = configUpdate.get("testing2");
            assertNotNull(testing2);
            assertEquals("13141516", testing2);

            messageSender.sendStop = true;
            assertTrue(countDownLatch2.await(3000, TimeUnit.MILLISECONDS));

            var exceptionThrown = false;
            try {
                HoogasClient.init("TestApp1", hoogasMessageListener);
            }
            catch(Exception e) {
                exceptionThrown = true;
            }
            assertTrue(exceptionThrown);
        }
        finally {
            HoogasClient.shutdown();
            messageSender.shutdown = true;
            messageSenderThread.join();
        }
    }

    //Check that the Hoogas client initialization routine will time-out if it doesn't receive the public config response before the timeout
    @Test
    public void test2() throws Exception {
        File messageFromHoogasDir = new File(SharedConstants.INSTALLATION_DIR + File.separator + SharedConstants.APPLICATIONS_DIR_NAME +
                File.separator + "TestApp1" + File.separator + SharedConstants.INTERNAL_MSGS_FROM_HOOGAS_DIR_NAME);
        createDir(messageFromHoogasDir.getPath());

        boolean exceptionThrown = false;
        try {
            HoogasClient.waitForPublicConfig(messageFromHoogasDir);
        }
        catch(Exception e) {
            exceptionThrown = true;
        }

        assertTrue(exceptionThrown);
    }

    //Check that when the message receiver deletes message files after it's picked them up
    @Test
    public void test3() throws Exception {
        File messageFromHoogasDir = new File(SharedConstants.INSTALLATION_DIR + File.separator + SharedConstants.APPLICATIONS_DIR_NAME +
                File.separator + "TestApp1" + File.separator + SharedConstants.INTERNAL_MSGS_FROM_HOOGAS_DIR_NAME);
        createDir(messageFromHoogasDir.getPath());

        createFile(messageFromHoogasDir.getPath() + File.separator + MessageTypeToApplications.PUBLIC_CFG_RESPONSE.name());
        createFile(messageFromHoogasDir.getPath() + File.separator + MessageTypeToApplications.STOP);

        HoogasClient.checkForMsgsFromHoogas(messageFromHoogasDir);

        assertEquals(messageFromHoogasDir.listFiles().length, 0);
    }

    //Check that the Hoogas client initialization routine will clear the contents of the to-hoogas and from hoogas-msg directories if they're already present
    //when the initialisation routine is run
    @Test
    public void test4() throws Exception {
        String messageFromHoogasDir = SharedConstants.INSTALLATION_DIR + File.separator + SharedConstants.APPLICATIONS_DIR_NAME +
                File.separator + "TestApp1" + File.separator + SharedConstants.INTERNAL_MSGS_FROM_HOOGAS_DIR_NAME;
        String messageToHoogasDir = SharedConstants.INSTALLATION_DIR + File.separator + SharedConstants.APPLICATIONS_DIR_NAME +
                File.separator + "TestApp1" + File.separator + SharedConstants.INTERNAL_MSGS_TO_HOOGAS_DIR_NAME;

        createDir(messageFromHoogasDir);
        createDir(messageToHoogasDir);

        createFile(messageFromHoogasDir + File.separator + MessageTypeToApplications.PUBLIC_CFG_RESPONSE.name());
        createFile(messageFromHoogasDir + File.separator + MessageTypeToApplications.STOP);
        createFile(messageToHoogasDir + File.separator + MessageTypeFromApplications.PUBLIC_CFG_REQUEST);

        HoogasClient.setUpMessagingDir(new File(messageToHoogasDir));
        HoogasClient.setUpMessagingDir(new File(messageFromHoogasDir));

        assertEquals(0, new File(messageToHoogasDir).listFiles().length);
        assertEquals(0, new File(messageFromHoogasDir).listFiles().length);
    }

    //This can simulate the Hoogas server responding to public config requests from the application, and it can also send messages
    //to the application on command
    private class MessageSender implements Runnable {
        private volatile boolean shutdown;
        private volatile boolean sendConfigUpdate;
        private volatile boolean sendStop;
        private final String msgsFromHoogasDir = installationDir + File.separator + SharedConstants.APPLICATIONS_DIR_NAME +
                File.separator + "TestApp1" + File.separator + SharedConstants.INTERNAL_MSGS_FROM_HOOGAS_DIR_NAME;
        private final String msgsToHoogasDir =
                installationDir + File.separator + SharedConstants.APPLICATIONS_DIR_NAME +
                        File.separator + "TestApp1" + File.separator + SharedConstants.INTERNAL_MSGS_TO_HOOGAS_DIR_NAME;

        public void run() {

            var msgsToHoogasDirObject = new File(msgsToHoogasDir);
            try {
                while (!shutdown) {
                    if (sendConfigUpdate) {
                        String updateContent = "testing1=9101112" + SharedConstants.NEWLINE + "testing2=13141516";
                        createMessageFromHoogas(MessageTypeToApplications.PUBLIC_CFG_RESPONSE.name(), updateContent);
                        sendConfigUpdate = false;
                    } else if (sendStop) {
                        createMessageFromHoogas(MessageTypeToApplications.STOP.name(), null);
                        sendStop = false;
                    }
                    else if (msgsToHoogasDirObject.exists()) {
                        var files = msgsToHoogasDirObject.listFiles();
                        if (files.length == 1 && files[0].getName().equals(MessageTypeFromApplications.PUBLIC_CFG_REQUEST.name())) {
                            if (!files[0].delete()) {
                                throw new IllegalStateException("Could not delete file: " + files[0].getPath());
                            }
                            String publicConfig = "testing1=1234" + SharedConstants.NEWLINE + "testing2=5678";
                            createMessageFromHoogas(MessageTypeToApplications.PUBLIC_CFG_RESPONSE.name(), publicConfig);
                        }
                    }

                    Thread.sleep(200);
                }
            }
            catch(Exception e) {
                e.printStackTrace();
            }
        }

        private void createMessageFromHoogas(String fileName, String content) throws Exception {
            var messageFile = new File(msgsFromHoogasDir + File.separator + fileName);
            createFile(messageFile.getPath());
            if(content != null) {
                try (var writer = new BufferedWriter(new FileWriter(messageFile))) {
                    writer.write(content);
                }
            }
        }
    }

    private static void createFile(String file) throws Exception {
        if(!new File(file).createNewFile()) {
            throw new IllegalStateException("Could not create file: " + file);
        }
    }

    private static void createDir(String dir) {
        if(!new File(dir).mkdir()) {
            throw new IllegalStateException("Could not create directory: " + dir);
        }
    }
}
