package com.noomtech.hoogas.config;


/**
 * Used to listen for whenever the set of applications that Hoogas looks after is updated e.g. if a deployment has been done
 * @author Joshua Newman, December 2024
 */
public interface DeployedApplicationsUpdatedListener {


    void onApplicationsUpdated();
}
