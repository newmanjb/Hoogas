package com.noomtech.hoogas.deployment;

import com.noomtech.hoogas.constants.Constants;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import static com.noomtech.hoogas.constants.Constants.NAME_VERSION_SEPARATOR;


/**
 * Periodically checks the deployment directory for directories whos names have the format "app-name___version", and  will move these directories to that application's
 * installation folder under the root directory.  If there's an existing version deployed it's moved to the archive directory first.
 * @author Joshua Newman, December 2024
 */
public class DeploymentService implements PeriodicChecker {


    private static final File APPLICATION_DIR = Constants.HoogasDirectory.APPLICATIONS.getDirFile();
    private static final File DEPLOYMENT_DIR = Constants.HoogasDirectory.DEPLOYMENTS.getDirFile();
    private static final File ARCHIVE_DIR = Constants.HoogasDirectory.ARCHIVE.getDirFile();

    private final int checkingInterval;
    private long whenLastRunFinished;


    public DeploymentService(int checkingInterval) {
        this.checkingInterval = checkingInterval;
        //Build the initial list of deployed applications by looking at the file structure
        var deployedApps = APPLICATION_DIR.listFiles();
        var appNames = new ArrayList<String>();
        var appVersions = new ArrayList<String>();

        for (File deployedApp : deployedApps) {
            if (deployedApp.isDirectory() && deployedApp.getName().contains(NAME_VERSION_SEPARATOR)) {
                var split = deployedApp.getName().split(NAME_VERSION_SEPARATOR);
                appNames.add(split[0]);
                appVersions.add(split[1]);
            } else {
                System.out.println("WARNING - Invalid application present in '" + APPLICATION_DIR.getPath() + "' :" + deployedApp.getPath());
            }
        }

        DeployedApplicationsHolder.addApplications(appNames.toArray(new String[0]), appVersions.toArray(new String[0]));
    }

    void checkForNewDeployments() throws IOException {

        if(checkShouldRun(whenLastRunFinished)) {
            try {
                var applicationsAlreadyDeployed = DeployedApplicationsHolder.getDeployedApplications();
                var filesInDeploymentDir = DEPLOYMENT_DIR.listFiles();
                for (File fileInDestinationDirectory : filesInDeploymentDir) {

                    boolean invalidFile = true;
                    if (fileInDestinationDirectory.getName().contains(NAME_VERSION_SEPARATOR)) {

                        var split = fileInDestinationDirectory.getName().split(NAME_VERSION_SEPARATOR);
                        if (fileInDestinationDirectory.isDirectory() && split.length == 2) {

                            invalidFile = false;
                            System.out.println("Found deployed application at - " + fileInDestinationDirectory.getPath() + ".  Will archive existing application if there is one and deploy this new one");

                            var newAppName = split[0];
                            var newAppVersion = split[1];

                            var destinationForDeployment = new File(APPLICATION_DIR.getPath() + File.separator + newAppName + NAME_VERSION_SEPARATOR + newAppVersion);

                            var existingApplicationVersion = applicationsAlreadyDeployed.get(newAppName);
                            if (existingApplicationVersion != null) {

                                if (existingApplicationVersion.equals(newAppVersion)) {
                                    System.out.println("WARNING - Version " + newAppVersion + " of application '" + newAppName + "' is already deployed.");
                                }
                                var existingApplicationDir = APPLICATION_DIR.getPath() + File.separator + newAppName + NAME_VERSION_SEPARATOR + existingApplicationVersion;
                                var archivedApplicationDir = new File(ARCHIVE_DIR.getPath() + File.separator + newAppName + NAME_VERSION_SEPARATOR + existingApplicationVersion);

                                if (archivedApplicationDir.exists()) {
                                    System.out.println("Application with same version already exists in archive - '" + archivedApplicationDir.getPath() + "'.  Adding identifier to distinguish it from the one we're just about to archive");
                                    FileUtils.moveDirectory(archivedApplicationDir, new File(archivedApplicationDir.getPath() + "_old_" + System.currentTimeMillis()));
                                }

                                System.out.println("Moving '" + existingApplicationDir + "' to '" + archivedApplicationDir + "'");
                                FileUtils.moveDirectory(new File(existingApplicationDir), archivedApplicationDir);
                            }

                            System.out.println("Deploying '" + fileInDestinationDirectory.getPath() + "' to '" + destinationForDeployment.getPath() + "'");
                            FileUtils.moveDirectory(fileInDestinationDirectory, destinationForDeployment);
                            //Update the list of deployed applications
                            DeployedApplicationsHolder.addApplications(new String[]{newAppName}, new String[]{newAppVersion});
                            System.out.println("Deployment complete");
                        }
                    }

                    if (invalidFile) {
                        System.out.println("'" + fileInDestinationDirectory.getPath() + "' in deployment directory '" + DEPLOYMENT_DIR.getPath() + "' is invalid.  Ignoring");
                    }
                }
            }
            finally {
                whenLastRunFinished = System.currentTimeMillis();
            }
        }
    }

    @Override
    public void doCheck() throws Exception {
        checkForNewDeployments();
    }

    @Override
    public long getInterval() {
        return checkingInterval;
    }
}
