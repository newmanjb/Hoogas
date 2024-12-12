package com.noomtech.hoogas.internal_messaging;

import com.noomtech.hoogas.constants.Constants;
import com.noomtech.hoogas.datamodels.InternalMessageOutbound;
import com.noomtech.hoogas.deployment.DeployedApplicationsHolder;
import com.noomtech.hoogas.put_in_shared_project.SharedConstants;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

public class OutboundMessagingServiceTest {


    private static MockedStatic<DeployedApplicationsHolder> mockedDeploymentApplicationsHolderHandle;
    private static File messageDirectoryApp1;
    private static File messageDirectoryApp2;
    private static File applicationDirectory;


    @BeforeAll
    public static void beforeAll() throws Exception {
        mockedDeploymentApplicationsHolderHandle = Mockito.mockStatic(DeployedApplicationsHolder.class);
        var mockedDeployments = new HashMap<String,String>();
        mockedDeployments.put("TestApp1", "1234");
        mockedDeployments.put("TestApp2", "5678");
        mockedDeploymentApplicationsHolderHandle.when(DeployedApplicationsHolder::getDeployedApplications).thenReturn(mockedDeployments);
        var workingDir = System.getProperty("user.dir");
        System.setProperty("installation_dir", workingDir);

        applicationDirectory = new File(workingDir + File.separator + Constants.HoogasDirectory.APPLICATIONS.getDirName());
        createDir(applicationDirectory);
        messageDirectoryApp1 = new File(applicationDirectory.getPath()  + File.separator + "TestApp1" +
                Constants.NAME_VERSION_SEPARATOR + "1234" + File.separator + Constants.HoogasDirectory.INTERNAL_MSGS_FROM_HOOGAS.getDirName());
        messageDirectoryApp2 = new File(applicationDirectory.getPath()  + File.separator + "TestApp2" +
                Constants.NAME_VERSION_SEPARATOR + "5678" + File.separator + Constants.HoogasDirectory.INTERNAL_MSGS_FROM_HOOGAS.getDirName());
    }

    @BeforeEach
    public void beforeEach() throws Exception {
        var files = applicationDirectory.listFiles();
        for(File file : files) {
            if(file.isDirectory()) {
                FileUtils.deleteDirectory(file);
            }
            else {
                deleteFile(file);
            }
        }

        createDir(messageDirectoryApp1);
        createDir(messageDirectoryApp2);
    }

    @AfterAll
    public static void afterAll() throws Exception {
        mockedDeploymentApplicationsHolderHandle.close();
        System.clearProperty("installation_dir");
        FileUtils.deleteDirectory(applicationDirectory);
    }


    //Send 2 messages of different types which will each go to the 2 apps we've set up, then check their messaging directories
    @Test
    public void test1() throws Exception {

        OutboundMessagingService outboundMessagingService = OutboundMessagingService.getInstance();
        var msg1 = new InternalMessageOutbound("test1 - 1", OutboundMessagingService.DataTypeOutbound.GLOBAL_CFG_RESPONSE);
        var msg2 = new InternalMessageOutbound("test1 - 2", OutboundMessagingService.DataTypeOutbound.DUMMY);
        assertNull(outboundMessagingService.send(msg1));
        assertNull(outboundMessagingService.send(msg2));

        var expectedContent = new HashMap<String,String>();
        expectedContent.put(OutboundMessagingService.DataTypeOutbound.GLOBAL_CFG_RESPONSE.name() + SharedConstants.HOOGAS_TO_APP_MSG_FILE_EXTENSION, msg1.text());
        expectedContent.put(OutboundMessagingService.DataTypeOutbound.DUMMY.name() + SharedConstants.HOOGAS_TO_APP_MSG_FILE_EXTENSION, msg2.text());
        checkMessagingDir(messageDirectoryApp1, expectedContent);
        checkMessagingDir(messageDirectoryApp2, expectedContent);
    }

    //As for the first test, but before sending the 2 messages send 1 beforehand.  This should get overwritten by one of the subsequent 2 that's of the same type
    @Test
    public void test2() throws Exception {

        OutboundMessagingService outboundMessagingService = OutboundMessagingService.getInstance();
        var msg0 = new InternalMessageOutbound("shouldn't see this", OutboundMessagingService.DataTypeOutbound.GLOBAL_CFG_RESPONSE);
        assertNull(outboundMessagingService.send(msg0));
        var expectedContent = new HashMap<String,String>();
        expectedContent.put(OutboundMessagingService.DataTypeOutbound.GLOBAL_CFG_RESPONSE.name() + SharedConstants.HOOGAS_TO_APP_MSG_FILE_EXTENSION, msg0.text());
        checkMessagingDir(messageDirectoryApp1, expectedContent);
        checkMessagingDir(messageDirectoryApp2, expectedContent);

        var msg1 = new InternalMessageOutbound("test1 - 1", OutboundMessagingService.DataTypeOutbound.GLOBAL_CFG_RESPONSE);
        var msg2 = new InternalMessageOutbound("test1 - 2", OutboundMessagingService.DataTypeOutbound.DUMMY);
        assertNull(outboundMessagingService.send(msg1));
        assertNull(outboundMessagingService.send(msg2));

        expectedContent.clear();
        expectedContent.put(OutboundMessagingService.DataTypeOutbound.GLOBAL_CFG_RESPONSE.name() + SharedConstants.HOOGAS_TO_APP_MSG_FILE_EXTENSION, msg1.text());
        expectedContent.put(OutboundMessagingService.DataTypeOutbound.DUMMY.name() + SharedConstants.HOOGAS_TO_APP_MSG_FILE_EXTENSION, msg2.text());
        checkMessagingDir(messageDirectoryApp1, expectedContent);
        checkMessagingDir(messageDirectoryApp2, expectedContent);
    }

    //as for 1 but delete one of the messaging directories beforehand so as the messages can't be delivered to that application, and make sure that this doesn't
    //affect the delivery of the messages to the other application
    @Test
    public void test3() throws Exception {

        FileUtils.deleteDirectory(messageDirectoryApp1);

        OutboundMessagingService outboundMessagingService = OutboundMessagingService.getInstance();

        var msg1 = new InternalMessageOutbound("test1 - 1", OutboundMessagingService.DataTypeOutbound.GLOBAL_CFG_RESPONSE);
        var msg2 = new InternalMessageOutbound("test1 - 2", OutboundMessagingService.DataTypeOutbound.DUMMY);
        var sendResult = outboundMessagingService.send(msg1);
        assertNotNull(sendResult);
        assertEquals(sendResult.size(), 1);
        assertEquals(sendResult.getFirst(), "TestApp1");

        sendResult = outboundMessagingService.send(msg2);
        assertNotNull(sendResult);
        assertEquals(sendResult.size(), 1);
        assertEquals(sendResult.getFirst(), "TestApp1");

        //Make sure there's still nothing in TestApp1's installation directory (because we deleted its messaging directory above)
        var shouldBeNothing = messageDirectoryApp1.getParentFile().list();
        assertNotNull(shouldBeNothing);
        assertEquals(shouldBeNothing.length, 0);

        var expectedContent = new HashMap<String,String>();
        expectedContent.put(OutboundMessagingService.DataTypeOutbound.GLOBAL_CFG_RESPONSE.name() + SharedConstants.HOOGAS_TO_APP_MSG_FILE_EXTENSION, msg1.text());
        expectedContent.put(OutboundMessagingService.DataTypeOutbound.DUMMY.name() + SharedConstants.HOOGAS_TO_APP_MSG_FILE_EXTENSION, msg2.text());
        checkMessagingDir(messageDirectoryApp2, expectedContent);
    }


    private static void deleteFile(File file) {
        if(!file.delete()) {
            throw new IllegalStateException("Could not delete file: " + file.getPath());
        }
    }

    private static void createDir(File file) {
        if(!file.mkdirs()) {
            throw new IllegalStateException("Could not create directory: " + file.getPath());
        }
    }

    private void checkMessagingDir(File dirToCheck, Map<String,String> expectedContent) throws Exception {
        var actualFiles = dirToCheck.listFiles();
        assertEquals(expectedContent.size(), actualFiles.length, "Actual number of files not equal to expected number of files");
        for(File actualFile : actualFiles) {
            assertTrue(actualFile.isFile(), "File: " + actualFile.getPath() + " is a directory!  Only files should be in a messaging directory");
            var expectedFileContent = expectedContent.get(actualFile.getName());
            assertNotNull(expectedFileContent, "Unexpected file '" + actualFile.getName() + "' in directory '" + dirToCheck.getPath() + "'");
            try(var reader = new BufferedReader(new FileReader(actualFile))) {
                var actualContent = reader.readLine();
                assertEquals(actualContent, expectedFileContent, "Content of file '" + actualFile.getPath() + " does not match expected content");
            }
        }
    }
}
