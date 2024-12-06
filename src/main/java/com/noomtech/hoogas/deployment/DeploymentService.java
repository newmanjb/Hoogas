package com.noomtech.hoogas.deployment;

import com.noomtech.hoogas.config.HoogasConfigService;
import com.noomtech.hoogas.constants.Constants;
import com.noomtech.hoogas.datamodels.Application;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

import static com.noomtech.hoogas.constants.Constants.NAME_VERSION_SEPARATOR;


/**
 * Periodically checks the deployment directory for directories whos names have the format "app-name___version", and  will move these directories to that application's
 * installation folder under the root directory.  If there's an existing version deployed it's moved to the archive directory first.
 * It will then update the config for the applications deployed on hoogas.
 * @author Joshua Newman, December 2024
 */
public class DeploymentService implements PeriodicChecker {


    private static final File APPLICATION_DIR = Constants.HoogasDirectory.APPLICATIONS.getDirFile();
    private static final File DEPLOYMENT_DIR = Constants.HoogasDirectory.DEPLOYMENTS.getDirFile();
    private static final File ARCHIVE_DIR = Constants.HoogasDirectory.ARCHIVE.getDirFile();
    private final long CHECKING_INTERVAL;

    private long whenLastRunFinished;


    public DeploymentService() {
        CHECKING_INTERVAL = Long.parseLong(HoogasConfigService.getInstance().getSetting("deployment_service.checking.interval"));
    }

    @Override
    public void doCheck() throws IOException {

        if(checkShouldRun(whenLastRunFinished)) {

            var filesInDeploymentDir = DEPLOYMENT_DIR.listFiles();
            for (File fileInDestinationDirectory : filesInDeploymentDir) {

                boolean invalidFile = true;
                if(fileInDestinationDirectory.getName().contains(NAME_VERSION_SEPARATOR)) {

                    var split = fileInDestinationDirectory.getName().split(NAME_VERSION_SEPARATOR);
                    if (fileInDestinationDirectory.isDirectory() && split.length == 2) {

                        invalidFile = false;
                        System.out.println("Found deployed application at - " + fileInDestinationDirectory.getPath() + ".  Will archive existing application if there is one and deploy this new one");

                        var newAppName = split[0];
                        var newAppVersion = split[1];

                        var destinationForDeployment = new File(APPLICATION_DIR.getPath() + File.pathSeparator + newAppName + NAME_VERSION_SEPARATOR + newAppVersion);

                        var deployedApplications = HoogasConfigService.getInstance().getDeployedApplications();
                        var existingApplication = deployedApplications.get(newAppName);
                        var newApplication = false;
                        if (existingApplication != null) {

                            var existingVersion = existingApplication.version();
                            if (existingVersion.equals(newAppVersion)) {
                                System.out.println("WARNING - Version " + newAppVersion + " of application '" + newAppName + "' is already deployed.");
                            }
                            var existingApplicationDir = existingApplication.installationDirectory();
                            var archivedApplicationDir = new File(ARCHIVE_DIR.getPath() + File.pathSeparator + newAppName + NAME_VERSION_SEPARATOR + existingVersion);

                            if (archivedApplicationDir.exists()) {
                                System.out.println("Application with same version already exists in archive - '" + archivedApplicationDir.getPath() + "'.  Adding identifier to distinguish it from the one we're just about to archive");
                                FileUtils.moveDirectory(archivedApplicationDir, new File(archivedApplicationDir.getPath() + "_old_" + System.currentTimeMillis()));
                            }

                            System.out.println("Moving '" + existingApplicationDir + "' to '" + archivedApplicationDir + "'");
                            FileUtils.moveDirectory(new File(existingApplicationDir), archivedApplicationDir);
                        } else {
                            newApplication = true;
                        }

                        System.out.println("Deploying '" + fileInDestinationDirectory.getPath() + "' to '" + destinationForDeployment.getPath() + "'");
                        FileUtils.moveDirectory(fileInDestinationDirectory, destinationForDeployment);
                        System.out.println("Deployment complete");

                        //Start command will be added via a config update bu a user
                        HoogasConfigService.getInstance().updateApplications(new Application(newAppName, newAppVersion, null, destinationForDeployment.getPath()));
                    }
                }

                if(invalidFile) {
                    System.out.println("'" + fileInDestinationDirectory.getPath() + "' in deployment directory '" + DEPLOYMENT_DIR.getPath() + "' is invalid.  Ignoring");
                }
            }
            whenLastRunFinished = System.currentTimeMillis();
        }
    }

    @Override
    public long getInterval() {
        return CHECKING_INTERVAL;
    }

    //todo - unit test
}
