package com.noomtech.hoogas.app_management;


import com.noomtech.hoogas.config.DeployedApplicationsUpdatedListener;

/**
 * Handles user-requested operations for the applications e.g. starting and stopping
 * @author Joshua Newman, December 2024
 */
public class AppManagementService implements DeployedApplicationsUpdatedListener {


    private volatile boolean isShutDown = false;
    //We don't want any commands to be run at the same time
    private final Object mutex = new Object();


    public AppManagementService() {
        //todo - start a small web-server that takes requests for starting and stopping (this should be shared with the MonitoringService.  No point in running 2 web-servers)
    }

    private void onStartRequest(String applicationName) {
        synchronized (mutex) {
            if(!isShutDown) {
                //todo - use the application's start command to start it and return a status.  Return error if the application is already running or if it doesn't exist.  Cache the
                //application's process id
            }
        }
    }

    private void onStopRequest(String applicationName) {
        synchronized (mutex) {
            if(!isShutDown) {
                //todo - use the application's stop command return a status.  Return error if the application is not running or if it doesn't exist.
            }
        }
    }

    private void kill(String applicationName) {
        synchronized(mutex) {
            if(!isShutDown) {
                //todo - use the application's process id to kill the process and return a status.  Return error if the application is not running or if it doesn't exist.
            }
        }
    }

    @Override
    public void onApplicationsUpdated() {
        synchronized(mutex) {

        }
    }

    public void shutdown() {
        synchronized (mutex) {
            isShutDown = true;
        }
    }
}