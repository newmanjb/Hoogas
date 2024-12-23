package com.noomtech.hoogas.app_management;


import com.noomtech.hoogas.deployment.DeployedApplicationsHolder;
import com.noomtech.hoogas.deployment.DeployedApplicationsUpdatedListener;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles user-requested operations for the applications e.g. starting and stopping.  Each operation
 * consists of running a command on the underlying OS, and each different application gets its own thread to
 * run these.  So when this service receives a request it's immediately passed off to another thread to be run
 * and so doesn't interfere with the main-thread that runs the Hoogas internals.
 *   The commands for each application are run one at a time and they aren't queued.
 * This prevents a user from accidentally issuing many commands if for example,they've run out of patience with waiting for an application to start or stop.
 * If a command is submitted for an application when another is already running a code is returned that can be used by the client inform the user of the
 * situation.
 * @author Joshua Newman, December 2024
 */
public class AppManagementService implements DeployedApplicationsUpdatedListener {


    private volatile boolean isShutDown = false;
    //Holds one task executor for each application
    private Map<String,AppManagementTaskExecutor> taskExecutorMap = new HashMap<>();



    public AppManagementService() {
        //todo - start a small web-server that takes requests for starting and stopping (this should be shared with the MonitoringService.  No point in running 2 web-servers)
        var deployedApplicationNames = DeployedApplicationsHolder.getDeployedApplications().keySet();
        for(String applicationName : deployedApplicationNames) {
            taskExecutorMap.put(applicationName, new AppManagementTaskExecutor(applicationName));
        }
    }

    private String onStartRequest(String applicationName) {
        if(!isShutDown) {
            //todo - use the application's start command to start it and return a status.  Return error if the application is already running or if it doesn't exist.  Cache the
            //application's process id
            var taskExecutor = taskExecutorMap.get(applicationName);
            if(taskExecutor != null) {
                Runnable task = null;
                if(!taskExecutor.submitTask(task)) {
                    //return something
                }
            }
        }
        return null;
    }

    private void onStopRequest(String applicationName) {
        if(!isShutDown) {
            //todo - use the application's stop command return a status.  Return error if the application is not running or if it doesn't exist.
            var taskExecutor = taskExecutorMap.get(applicationName);
            if(taskExecutor != null) {
                Runnable task = null;
                if(!taskExecutor.submitTask(task)) {
                    //return something
                }
            }
        }
    }

    private void kill(String applicationName) {

        if(!isShutDown) {
            //todo - use the application's process id to kill the process and return a status.  Return error if the application is not running or if it doesn't exist.
            var taskExecutor = taskExecutorMap.get(applicationName);
            if(taskExecutor != null) {
                Runnable task = null;
                if(!taskExecutor.submitTask(task)) {
                    //return something
                }
            }
        }
    }

    public void shutdown() throws Exception {
        isShutDown = true;
        for(AppManagementTaskExecutor appManagementTaskExecutor : taskExecutorMap.values()) {
            appManagementTaskExecutor.shutDown(5000);
        }
    }

    @Override
    public void onApplicationsUpdated() {
        //todo - In order to keep the main thread lock-free we may have to temporarily block all requests to this class via a boolean like the shut-down boolean and then
        //wait for each thread in each application's executor to finish its current task (use .join()) and then rebuild the application map.
        // We may need to make this class a PeriodicChecker, do the aforementioned ops in a Future and check it each time a check has been done.  Once it's done then we can
        //unset the boolean we set earlier.
    }
}