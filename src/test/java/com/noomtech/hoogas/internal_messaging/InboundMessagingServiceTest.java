package com.noomtech.hoogas.internal_messaging;

import com.noomtech.hoogas.constants.Constants;
import com.noomtech.hoogas.datamodels.InternalMessageInbound;
import com.noomtech.hoogas.deployment.DeployedApplicationsHolder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.*;

import org.apache.commons.io.FileUtils;


public class InboundMessagingServiceTest {


    private static String workingDir;
    private static MockedStatic<DeployedApplicationsHolder> mockedDeploymentApplicationsHolderHandle;
    private static String messageDirectory;
    private static String applicationDirectory;

    @BeforeAll
    public static void beforeAll() throws Exception {
        mockedDeploymentApplicationsHolderHandle = Mockito.mockStatic(DeployedApplicationsHolder.class);
        var mockedDeployment = new HashMap<String,String>();
        mockedDeployment.put("TestApp1", "1234");
        mockedDeploymentApplicationsHolderHandle.when(DeployedApplicationsHolder::getDeployedApplications).thenReturn(mockedDeployment);
        workingDir = System.getProperty("user.dir");
        System.setProperty("installation_dir", workingDir);

        applicationDirectory = workingDir + File.separator + Constants.HoogasDirectory.APPLICATIONS.getDirName();
        messageDirectory = applicationDirectory  + File.separator + "TestApp1" +
            Constants.NAME_VERSION_SEPARATOR + "1234" + File.separator + Constants.HoogasDirectory.INTERNAL_MSGS_TO_HOOGAS.getDirName();
        var applicationMessagingDir = new File(messageDirectory);
        if(!applicationMessagingDir.mkdirs()) {
            throw new IllegalStateException("Cannot create " + applicationMessagingDir.getPath());
        }
    }

    @AfterAll
    public static void afterAll() throws Exception {
        mockedDeploymentApplicationsHolderHandle.close();
        System.clearProperty("installation_dir");
        FileUtils.deleteDirectory(new File(applicationDirectory));
    }


    //Run it on an empty messaging directory and check no listeners are fired and that the messaging directory is still empty
    @Test
    public void test1() throws Exception {
        var inboundMessagingService = new InboundMessagingService(-1);
        var firedConfigRequestListener = new boolean[]{false};
        var configRequestListener = new ConfigRequestListener() {
            public void onConfigRequestMessageReceived(List<InternalMessageInbound> message) {
                firedConfigRequestListener[0] = true;
            }
        };
        var firedStatsListener = new boolean[]{false};
        var statsListener = new StatsListener() {
            public void onStatsMessageReceived(List<InternalMessageInbound> message) {
                firedStatsListener[0] = true;
            }
        };

        inboundMessagingService.addConfigRequestListener(configRequestListener);
        inboundMessagingService.addStatsListener(statsListener);
        inboundMessagingService.collect();

        assertFalse(firedConfigRequestListener[0], "Config request listener fired when no config requests received");
        assertFalse(firedStatsListener[0], "Stats listener fired when no stats messages received");
        checkMessageDirectory(Collections.emptyMap());
    }

    //Send one message of each type, and also put invalid files in there.
    //Check that both messages are parsed correctly and their files deleted.  Also make sure that the invalid files
    //are still in the messaging directory.
    @Test
    public void test2() throws Exception {
        var inboundMessagingService = new InboundMessagingService(-1);

        var configRequestsReceived = new ArrayList<InternalMessageInbound>();
        var configRequestListener = new ConfigRequestListener() {
            public void onConfigRequestMessageReceived(List<InternalMessageInbound> messages) {
                configRequestsReceived.addAll(messages);
            }
        };
        var statsMessagesReceived = new ArrayList<InternalMessageInbound>();
        var statsListener = new StatsListener() {
            public void onStatsMessageReceived(List<InternalMessageInbound> messages) {
                statsMessagesReceived.addAll(messages);
            }
        };

        inboundMessagingService.addConfigRequestListener(configRequestListener);
        inboundMessagingService.addStatsListener(statsListener);

        sendInboundMessage(new InternalMessageInbound("config request 1", "TestApp1"), InboundMessagingService.DataTypeInbound.PUBLIC_CFG_REQUEST);
        sendInboundMessage(new InternalMessageInbound("stats 1", "TestApp1"), InboundMessagingService.DataTypeInbound.STATS);

        //Create some invalid message files
        var invalid1 = new File(messageDirectory + File.separator + "PLEASE_DONT");
        var invalid2 = new File(messageDirectory + File.separator + "RECEIVE_ME");
        //Add a directory, which should also be considered invalid and ignored
        var invalid3 = new File(messageDirectory + File.separator + "OR ME");

        createFile(invalid1);
        createFile(invalid2);
        createDir(invalid3);

        inboundMessagingService.collect();

        assert(configRequestsReceived.size() == 1);
        var configRequest = configRequestsReceived.getFirst();
        assert(configRequest.text() != null);
        assert(configRequest.text().equals("config request 1"));
        assert(configRequest.from() != null);
        assert(configRequest.from().equals("TestApp1"));

        assert(statsMessagesReceived.size() == 1);
        var statsMessage = statsMessagesReceived.getFirst();
        assert(statsMessage.text() != null);
        assert(statsMessage.text().equals("stats 1"));
        assert(statsMessage.from() != null);
        assert(statsMessage.from().equals("TestApp1"));

        var expectedContent = new HashMap<String,Boolean>();
        expectedContent.put("PLEASE_DONT", false);
        expectedContent.put("RECEIVE_ME", false);
        expectedContent.put("OR ME", true);
        checkMessageDirectory(expectedContent);
    }


    private void sendInboundMessage(InternalMessageInbound messageInbound, InboundMessagingService.DataTypeInbound type) throws Exception {
        var messageFile = new File(messageDirectory + File.separator + type);
        createFile(messageFile);
        try(BufferedWriter writer = new BufferedWriter(new FileWriter(messageFile))) {
            writer.write(messageInbound.text());
        }
    }

    private void createFile(File file) throws Exception {
        if(!file.createNewFile()) {
            throw new IllegalStateException("Cannot create file " + file.getPath());
        }
    }

    private void createDir(File file) {
        if(!file.mkdir()) {
            throw new IllegalStateException("Cannot create directory " + file.getPath());
        }
    }

    private void checkMessageDirectory(Map<String,Boolean> expectedContent) {
        var filesInMessageDirectory = new File(messageDirectory).listFiles();
        assertEquals(filesInMessageDirectory.length, expectedContent.size());
        for(File fileInMessageDirectory : filesInMessageDirectory) {
            var fileNameFromDirectory = fileInMessageDirectory.getName();
            var isDir = expectedContent.get(fileNameFromDirectory);
            assertNotNull(isDir, "Unexpected file in message directory: " + fileInMessageDirectory.getPath());
            assertEquals(isDir, fileInMessageDirectory.isDirectory());
        }
    }
}
