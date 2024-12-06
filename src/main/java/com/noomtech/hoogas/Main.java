package com.noomtech.hoogas;

import com.noomtech.hoogas.app_management.AppManagementService;
import com.noomtech.hoogas.config.HoogasConfigService;
import com.noomtech.hoogas.config.public_config.PublicConfigService;
import com.noomtech.hoogas.constants.Constants;
import com.noomtech.hoogas.deployment.PeriodicChecker;
import com.noomtech.hoogas.internal_messaging.InboundMessagingService;
import com.noomtech.hoogas.deployment.DeploymentService;
import com.noomtech.hoogas.internal_messaging.OutboundMessagingService;
import com.noomtech.hoogas.monitoring.MonitoringService;
import static com.noomtech.hoogas.constants.Constants.INSTALLATION_DIR;


/**
 * Start-up class.  Starts the services and registers a shut-down hook that ensures services shut-down cleanly.
 * @author Joshua Newman, December 2024
 */
public class Main {


    //todo - write the client-side functionality
    //todo - add a webserver that can be used by the services that interact with the users

    private volatile boolean shutdown;


    public static void main(String[] args) throws Exception {

        if(!INSTALLATION_DIR.exists() || !INSTALLATION_DIR.isDirectory()) {
            throw new IllegalArgumentException(
                    "Invalid installationDirectory '" + INSTALLATION_DIR +
                            "'.  Please specify installation directory in java command using -Dinstallation_dir=...");
        }

        //Check if all the required subdirectories are present.  They may not be if this is a new installation
        for(Constants.HoogasDirectory hoogasDirectory : Constants.HoogasDirectory.values()) {
            if(!hoogasDirectory.getDirFile().exists()) {
                if(!hoogasDirectory.getDirFile().mkdir()) {
                    throw new IllegalStateException("Could not create directory: " + hoogasDirectory.getDirFile().getPath());
                }
            }
            else if(hoogasDirectory.getDirFile().isFile()){
                throw new IllegalStateException("Directory '" + hoogasDirectory.getDirFile().getPath() + " is a file!");
            }
        }

        new Main();
    }

    private Main() throws Exception {

        //Build these first as they are used by the other services
        HoogasConfigService.init();
        OutboundMessagingService.init();

        var inboundMessageService = new InboundMessagingService();
        var deploymentService = new DeploymentService();
        var monitoringService = new MonitoringService();
        var publicConfigService = new PublicConfigService();
        var appManagementService = new AppManagementService();

        //Set up the connections between these difference services
        inboundMessageService.addStatsListener(monitoringService);
        inboundMessageService.addConfigRequestListener(publicConfigService);
        HoogasConfigService.getInstance().addApplicationUpdatedListener(monitoringService);
        HoogasConfigService.getInstance().addApplicationUpdatedListener(appManagementService);

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                shutdown();
            }
        }));

        //These services have routines which need to be run at set intervals e.g. checking for new messages or new deployments.
        //I don't see the point in running them in a separate thread, as they are not very demanding routines
        var periodicCheckers = new PeriodicChecker[]{deploymentService, inboundMessageService};
        while(!shutdown) {
            for(PeriodicChecker periodicChecker : periodicCheckers) {
                try {
                    periodicChecker.doCheck();
                }
                catch(Exception e) {
                    //todo - add a logger to the code
                    e.printStackTrace();
                }
            }
        }

        //todo - shut-down whatever web-server you've set up

        //So far this is the only service that needs to be explicitly shut-down, as all the other ones depend on the ones that are
        //run periodically above i.e. they just depend on the main thread.  This service is different because it takes requests
        // that come from a web-server potentially on different threads, and these requests result in processing that shouldn't be interrupted
        appManagementService.shutdown();
    }

    private void shutdown() {
        shutdown = true;
    }
}
