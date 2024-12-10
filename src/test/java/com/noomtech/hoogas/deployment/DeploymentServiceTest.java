package com.noomtech.hoogas.deployment;

import org.junit.jupiter.api.*;

import java.io.File;
import java.util.Collections;
import static com.noomtech.hoogas.constants.Constants.HoogasDirectory.DEPLOYMENTS;
import static com.noomtech.hoogas.constants.Constants.HoogasDirectory.ARCHIVE;
import static com.noomtech.hoogas.constants.Constants.HoogasDirectory.APPLICATIONS;
import static com.noomtech.hoogas.constants.Constants.NAME_VERSION_SEPARATOR;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;

import static org.junit.jupiter.api.Assertions.*;

public class DeploymentServiceTest {


    private static String workingDir;


    @BeforeAll
    public static void beforeAll() throws Exception {
        workingDir = System.getProperty("user.dir");
        System.setProperty("installation_dir", workingDir);
    }

    @BeforeEach
    public void beforeEach() throws Exception {
        DeployedApplicationsHolder.clearEverything();
        makeDirForTest(DEPLOYMENTS.getDirName());
        makeDirForTest(ARCHIVE.getDirName());
        makeDirForTest(APPLICATIONS.getDirName());
    }


    @AfterAll
    public static void afterAll() throws Exception {
        DeployedApplicationsHolder.clearEverything();
        System.clearProperty("installation_dir");
    }

    @AfterEach
    public void afterEach() throws Exception {
        rmDirForTest(DEPLOYMENTS.getDirName());
        rmDirForTest(APPLICATIONS.getDirName());
        rmDirForTest(ARCHIVE.getDirName());
    }

    //Make sure a check on an empty directory runs properly
    @Test
    public void test1() throws Exception {
        var deploymentService = new DeploymentService(-1);
        deploymentService.checkForNewDeployments();
        checkDirectoryFor(DEPLOYMENTS.getDirName(), Collections.emptyMap());
        checkDirectoryFor(ARCHIVE.getDirName(), Collections.emptyMap());
        checkDirectoryFor(APPLICATIONS.getDirName(), Collections.emptyMap());
    }

    //Add 2 valid deployments and a few invalid deployments to the deployment directory.  Taken together the invalid deployments
    // cover all ways that a deployment can be invalid
    @Test
    public void test2() throws Exception {

        var testApp1DeploymentDir = DEPLOYMENTS.getDirName() + File.separator + "TestApp1" + NAME_VERSION_SEPARATOR + "1234";
        makeDirForTest(testApp1DeploymentDir);
        makeDirForTest(testApp1DeploymentDir + File.separator + "testapp1subdir");
        makeFileForTest(testApp1DeploymentDir + File.separator + "testapp1file.txt");
        var testApp2DeploymentDir = DEPLOYMENTS.getDirName() + File.separator + "TestApp2" + NAME_VERSION_SEPARATOR + "1234";
        makeDirForTest(testApp2DeploymentDir);
        makeDirForTest(testApp2DeploymentDir + File.separator + "testapp2subdir");
        makeFileForTest(testApp2DeploymentDir + File.separator + "testapp2file.txt");

        var notDeployed1 = "ShouldNotBeDeployed1";
        var notDeployed2 = "ShouldNotBeDeployed2" + NAME_VERSION_SEPARATOR + "1234" + NAME_VERSION_SEPARATOR + "NOWAY";
        var notDeployed3 = "ShouldNotBeDeployed3" + NAME_VERSION_SEPARATOR + "_1234";
        makeDirForTest(DEPLOYMENTS.getDirName() + File.separator + notDeployed1);
        makeDirForTest(DEPLOYMENTS.getDirName() + File.separator + notDeployed2);
        makeFileForTest(DEPLOYMENTS.getDirName() + File.separator + notDeployed3);

        var deploymentService = new DeploymentService(-1);
        deploymentService.checkForNewDeployments();

        var expectedContent = new HashMap<String,Boolean>();
        expectedContent.put(notDeployed1, true);
        expectedContent.put(notDeployed2, true);
        expectedContent.put(notDeployed3, false);
        checkDirectoryFor(DEPLOYMENTS.getDirName(), expectedContent);
        expectedContent.clear();

        checkDirectoryFor(ARCHIVE.getDirName(), Collections.emptyMap());

        expectedContent.put("TestApp1" + NAME_VERSION_SEPARATOR + "1234", true);
        expectedContent.put("TestApp2" + NAME_VERSION_SEPARATOR + "1234", true);
        checkDirectoryFor(APPLICATIONS.getDirName(), expectedContent);
        expectedContent.clear();
        var testApp1ApplicationDir = APPLICATIONS.getDirName() + File.separator + "TestApp1" + NAME_VERSION_SEPARATOR + "1234";
        expectedContent.put("testapp1subdir", true);
        expectedContent.put("testapp1file.txt", false);
        checkDirectoryFor(testApp1ApplicationDir, expectedContent);
        expectedContent.clear();
        var testApp2ApplicationDir = APPLICATIONS.getDirName() + File.separator + "TestApp2" + NAME_VERSION_SEPARATOR + "1234";
        expectedContent.put("testapp2subdir", true);
        expectedContent.put("testapp2file.txt", false);
        checkDirectoryFor(testApp2ApplicationDir, expectedContent);
    }

    //Make sure existing deployments are archived when a new version is deployed
    @Test
    public void test3() throws Exception {

        var testApp1ApplicationDir = APPLICATIONS.getDirName() + File.separator + "TestApp1" + NAME_VERSION_SEPARATOR + "1234";
        makeDirForTest(testApp1ApplicationDir);
        makeDirForTest(testApp1ApplicationDir + File.separator + "1234");
        makeFileForTest(testApp1ApplicationDir + File.separator + "hello1234.txt");

        var testApp1NewDeploymentDir = DEPLOYMENTS.getDirName() + File.separator + "TestApp1" + NAME_VERSION_SEPARATOR + "12345";
        makeDirForTest(testApp1NewDeploymentDir);
        makeDirForTest(testApp1NewDeploymentDir + File.separator + "12345");
        makeFileForTest(testApp1NewDeploymentDir + File.separator + "hello12345.txt");

        var deploymentService = new DeploymentService(-1);
        deploymentService.checkForNewDeployments();

        checkDirectoryFor(DEPLOYMENTS.getDirName(), Collections.emptyMap());

        var newTestApp1ApplicationDir = APPLICATIONS.getDirName() + File.separator + "TestApp1" + NAME_VERSION_SEPARATOR + "12345";
        var expectedContents = new HashMap<String,Boolean>();
        expectedContents.put("12345", true);
        expectedContents.put("hello12345.txt", false);
        checkDirectoryFor(newTestApp1ApplicationDir, expectedContents);

        var archivedTestApp1Dir = ARCHIVE.getDirName() + File.separator + "TestApp1" + NAME_VERSION_SEPARATOR + "1234";
        expectedContents.clear();
        expectedContents.put("1234", true);
        expectedContents.put("hello1234.txt", false);
        checkDirectoryFor(archivedTestApp1Dir, expectedContents);
    }

    //As for test 3 but this tests is making sure the archiving takes place even if an identical version to the existing one is deployed, and also
    //checks that when existing applications are archived that they will overwrite anything in the archive with the same app name and version
    @Test
    public void test4() throws Exception {

        var testApp1ApplicationDir = APPLICATIONS.getDirName() + File.separator + "TestApp1" + NAME_VERSION_SEPARATOR + "1234";
        makeDirForTest(testApp1ApplicationDir);
        makeDirForTest(testApp1ApplicationDir + File.separator + "1234_1");
        makeFileForTest(testApp1ApplicationDir + File.separator + "hello1234_1.txt");

        var testApp1NewDeploymentDir = DEPLOYMENTS.getDirName() + File.separator + "TestApp1" + NAME_VERSION_SEPARATOR + "1234";
        makeDirForTest(testApp1NewDeploymentDir);
        makeDirForTest(testApp1NewDeploymentDir + File.separator + "1234_2");
        makeFileForTest(testApp1NewDeploymentDir + File.separator + "hello1234_2.txt");

        var testApp1ArchivedDir = ARCHIVE.getDirName() + File.separator + "TestApp1" + NAME_VERSION_SEPARATOR + "1234";
        makeDirForTest(testApp1ArchivedDir);
        makeDirForTest(testApp1ArchivedDir + File.separator + "1234_0");
        makeFileForTest(testApp1ArchivedDir + File.separator + "hello1234_0.txt");

        var deploymentService = new DeploymentService(-1);
        deploymentService.checkForNewDeployments();

        checkDirectoryFor(DEPLOYMENTS.getDirName(), Collections.emptyMap());

        var expectedContents = new HashMap<String,Boolean>();
        expectedContents.put("1234_2", true);
        expectedContents.put("hello1234_2.txt", false);
        checkDirectoryFor(testApp1ApplicationDir, expectedContents);

        expectedContents.clear();
        expectedContents.put("1234_1", true);
        expectedContents.put("hello1234_1.txt", false);
        checkDirectoryFor(testApp1ArchivedDir, expectedContents);
    }

    //Check that the map of deployed applications is correctly built when the deployment service is created and the application dir is empty
    @Test
    public void test5() throws Exception {
        //Shouldn't pick anything up
        var deploymentService = new DeploymentService(-1);
        assert (DeployedApplicationsHolder.getDeployedApplications().isEmpty());
    }

    //Check that the map of deployed applications is correctly built when the deployment service is created when there's one
    //deployment in the application dir along with some invalid files
    @Test
    public void test6() throws Exception {

        //Should pick up one
        var testApp1Dir = APPLICATIONS.getDirName() + File.separator + "TestApp1" + NAME_VERSION_SEPARATOR + "1234";
        makeDirForTest(testApp1Dir);
        var testAppInvalid1dir = APPLICATIONS.getDirName() + File.separator + "TestApp1" + "yyuu" + "1234";
        makeDirForTest(testAppInvalid1dir);
        var testAppInvalid1File = APPLICATIONS.getDirName() + File.separator + "TestApp1" + NAME_VERSION_SEPARATOR + "1234.txt";
        makeFileForTest(testAppInvalid1File);

        var deploymentService = new DeploymentService(-1);
        var version = DeployedApplicationsHolder.getDeployedApplications().get("TestApp1");
        assertEquals(DeployedApplicationsHolder.getDeployedApplications().size(), 1);
        assertNotNull(version);
        assertEquals("1234", version);
    }


    //Check that the map of deployed applications is correctly updated when deployments are made
    @Test
    public void test7() throws Exception {
        //Shouldn't pick anything up
        var deploymentService = new DeploymentService(-1);
        var testApp1NewDeploymentDir = DEPLOYMENTS.getDirName() + File.separator + "TestApp1" + NAME_VERSION_SEPARATOR + "1234";
        makeDirForTest(testApp1NewDeploymentDir);
        deploymentService.checkForNewDeployments();

        var version = DeployedApplicationsHolder.getDeployedApplications().get("TestApp1");
        assertEquals(DeployedApplicationsHolder.getDeployedApplications().size(), 1);
        assertNotNull(version);
        assertEquals("1234", version);

        var testApp2NewDeploymentDir = DEPLOYMENTS.getDirName() + File.separator + "TestApp2" + NAME_VERSION_SEPARATOR + "5678";
        makeDirForTest(testApp2NewDeploymentDir);
        deploymentService.checkForNewDeployments();

        assertEquals(DeployedApplicationsHolder.getDeployedApplications().size(), 2);
        version = DeployedApplicationsHolder.getDeployedApplications().get("TestApp1");
        assertNotNull(version);
        assertEquals("1234", version);
        version = DeployedApplicationsHolder.getDeployedApplications().get("TestApp2");
        assertNotNull(version);
        assertEquals("5678", version);
    }

    private static void makeDirForTest(String dir) throws Exception {
        var newDir = new File(workingDir + File.separator + dir);
        if(!newDir.mkdir()) {
            throw new IllegalStateException("Can't create " + newDir.getPath());
        }
    }

    private static void makeFileForTest(String filePath) throws Exception {
        var newFile = new File(workingDir + File.separator + filePath);
        if(!newFile.createNewFile()) {
            throw new IllegalStateException("Can't create " + newFile.getPath());
        }
    }

    private static void rmDirForTest(String dir) throws Exception {
        var newDir = new File(workingDir + File.separator + dir);
        FileUtils.deleteDirectory(newDir);
    }

    private void checkDirectoryFor(String dir, Map<String, Boolean> filesToLookFor) {
        var contents = new File(workingDir + File.separator + dir).listFiles();
        assert(contents.length == filesToLookFor.size());
        for(File f : contents) {
            var isDir = filesToLookFor.get(f.getName());
            assertNotNull(isDir, f.getName() + " not expected in " + dir);
            assertEquals(isDir, f.isDirectory());
        }
    }
}