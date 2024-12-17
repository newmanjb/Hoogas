package com.noomtech.hoogas.public_config;

import com.noomtech.hoogas.constants.Constants;
import com.noomtech.hoogas.datamodels.InternalMessageInbound;
import com.noomtech.hoogas.datamodels.InternalMessageOutbound;
import com.noomtech.hoogas.deployment.DeployedApplicationsHolder;
import com.noomtech.hoogas.internal_messaging.OutboundMessagingService;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Arrays;
import java.util.Map;

import static com.noomtech.hoogas.put_in_shared_project.SharedConstants.NEWLINE;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;

public class PublicConfigServiceTest {


    private static MockedStatic<OutboundMessagingService> outboundMessagingServiceMockedStatic;
    private static MockedStatic<DeployedApplicationsHolder> deployedApplicationsHolderMockedStatic;



    @BeforeAll
    public static void beforeAll() throws Exception {
        outboundMessagingServiceMockedStatic = Mockito.mockStatic(OutboundMessagingService.class);
        deployedApplicationsHolderMockedStatic = Mockito.mockStatic(DeployedApplicationsHolder.class);
        //Set up 2 deployed apps
        deployedApplicationsHolderMockedStatic.when(DeployedApplicationsHolder::getDeployedApplications).thenReturn(Map.of("TestApp1", "1234", "TestApp2", "5678"));
        var workingDir = System.getProperty("user.dir");
        System.setProperty("installation_dir", workingDir);
        if(!Constants.HoogasDirectory.PUBLIC_CONFIG.getDirFile().mkdir()) {
            throw new IllegalStateException("Could not create directory: " + Constants.HoogasDirectory.PUBLIC_CONFIG.getDirFile().getPath());
        }
    }


    @AfterAll
    public static void afterAll() throws Exception {
        outboundMessagingServiceMockedStatic.close();
        deployedApplicationsHolderMockedStatic.close();
        FileUtils.deleteDirectory(Constants.HoogasDirectory.PUBLIC_CONFIG.getDirFile());
        System.clearProperty("installation_dir");
    }

    //Test that the correct config responses are sent in response to config requests from both of our test apps
    @Test
    public void test1() throws Exception {

        try {
            createPublicConfigFile(Map.of("testProp1", "testProp1Value", "testProp2", "testProp2Value"));

            var msg1 = new InternalMessageInbound("test1", "TestApp1");
            var msg2 = new InternalMessageInbound("test2", "TestApp2");

            OutboundMessagingService mockedOutboundMessagingService = Mockito.mock(OutboundMessagingService.class);
            outboundMessagingServiceMockedStatic.when(OutboundMessagingService::getInstance).thenReturn(mockedOutboundMessagingService);

            //This enables us to capture the responses that are sent and where they're being sent to
            ArgumentCaptor<InternalMessageOutbound> argumentCaptorMessages = ArgumentCaptor.forClass(InternalMessageOutbound.class);
            ArgumentCaptor<Map<String, String>> argumentCaptorDestinations = ArgumentCaptor.forClass(Map.class);

            var publicConfigService = new PublicConfigService(-1);
            //Simulate the requests being received and then run the processing routine that should send the responses and capture
            //what was sent
            publicConfigService.onMessageReceived(Arrays.asList(new InternalMessageInbound[]{msg1, msg2}));
            publicConfigService.doCheck();
            Mockito.verify(mockedOutboundMessagingService).send(argumentCaptorMessages.capture(), argumentCaptorDestinations.capture());

            var messageSent = argumentCaptorMessages.getValue();
            var destinationApps = argumentCaptorDestinations.getValue();

            assertNotNull(messageSent);
            assertNotNull(destinationApps);
            assert (messageSent.text().replace(NEWLINE, "").equals("testProp1=testProp1ValuetestProp2=testProp2Value"));
            assert (messageSent.type() == OutboundMessagingService.DataTypeOutbound.GLOBAL_CFG_RESPONSE);
            assert (destinationApps.size() == 2);
            assert (destinationApps.containsKey("TestApp1"));
            assert (destinationApps.containsKey("TestApp2"));
            assert (destinationApps.get("TestApp1").equals("1234"));
            assert (destinationApps.get("TestApp2").equals("5678"));
        }
        finally {
            deletePublicConfigFile();
        }
    }

    //Same as test1 except this tests the scenario where only one of the test apps sends a config request
    @Test
    public void test2() throws Exception {

        try {
            createPublicConfigFile(Map.of("testProp1", "testProp1Value", "testProp2", "testProp2Value"));
            var msg1 = new InternalMessageInbound("test1", "TestApp1");

            OutboundMessagingService mockedOutboundMessagingService = Mockito.mock(OutboundMessagingService.class);
            outboundMessagingServiceMockedStatic.when(OutboundMessagingService::getInstance).thenReturn(mockedOutboundMessagingService);

            ArgumentCaptor<InternalMessageOutbound> argumentCaptorMessages = ArgumentCaptor.forClass(InternalMessageOutbound.class);
            ArgumentCaptor<Map<String, String>> argumentCaptorDestinations = ArgumentCaptor.forClass(Map.class);

            var publicConfigService = new PublicConfigService(-1);
            publicConfigService.onMessageReceived(Arrays.asList(new InternalMessageInbound[]{msg1}));
            publicConfigService.doCheck();

            Mockito.verify(mockedOutboundMessagingService).send(argumentCaptorMessages.capture(), argumentCaptorDestinations.capture());

            var messageSent = argumentCaptorMessages.getValue();
            var destinationApps = argumentCaptorDestinations.getValue();

            assertNotNull(messageSent);
            assertNotNull(destinationApps);
            assert (messageSent.text().replace(NEWLINE, "").equals("testProp1=testProp1ValuetestProp2=testProp2Value"));
            assert (messageSent.type() == OutboundMessagingService.DataTypeOutbound.GLOBAL_CFG_RESPONSE);
            assert (destinationApps.size() == 1);
            assert (destinationApps.containsKey("TestApp1"));
            assert (destinationApps.get("TestApp1").equals("1234"));
        }
        finally {
            deletePublicConfigFile();
        }
    }

    //Checks that nothing is sent when the public config service has not received any config requests
    @Test
    public void test3() throws Exception {

        try {
            createPublicConfigFile(Map.of("testProp1", "testProp1Value", "testProp2", "testProp2Value"));
            OutboundMessagingService mockedOutboundMessagingService = Mockito.mock(OutboundMessagingService.class);
            outboundMessagingServiceMockedStatic.when(OutboundMessagingService::getInstance).thenReturn(mockedOutboundMessagingService);

            var publicConfigService = new PublicConfigService(-1);
            publicConfigService.doCheck();

            Mockito.verify(mockedOutboundMessagingService, never()).send(any(), any());
        }
        finally {
            deletePublicConfigFile();
        }
    }

    private void createPublicConfigFile(Map<String,String> properties) throws Exception {
        var configFile = new File(Constants.HoogasDirectory.PUBLIC_CONFIG.getDirFile().getPath() + File.separator + Constants.HOOGAS_PUBLIC_CONFIG_FILE_NAME);
        if(!configFile.createNewFile()) {
            throw new IllegalStateException("Could not create file: " + configFile.getPath());
        }
        try(var writer = new BufferedWriter(new FileWriter(configFile))) {
            for(Map.Entry<String,String> entry : properties.entrySet()) {
                writer.write(entry.getKey() + "=" + entry.getValue() + NEWLINE);
            }
        }
    }

    private void deletePublicConfigFile() throws Exception {
        var configFile = new File(Constants.HoogasDirectory.PUBLIC_CONFIG.getDirFile().getPath() + File.separator + Constants.HOOGAS_PUBLIC_CONFIG_FILE_NAME);
        if(!configFile.delete()) {
            throw new IllegalStateException("Could not delete file: " + configFile.getPath());
        }
    }
}
