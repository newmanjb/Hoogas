package com.noomtech.hoogas.deployment;

import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;


/**
 * Holds an up-to-date list on what applications are deployed in Hoogas and allows clients to receive notifications when its
 * updated.
 * @author Joshua Newman, December 2024
 */
public class DeployedApplicationsHolder {


    private static final List<DeployedApplicationsUpdatedListener> LISTENERS = new ArrayList<>();

    private static final Map<String,String> DEPLOYED_APPLICATIONS = new HashMap<>();
    private static final ReentrantReadWriteLock LOCK = new ReentrantReadWriteLock();

    static void addApplications(String[] appNames, String[] versions) {
        try {
            LOCK.writeLock().lock();
            if(appNames.length != versions.length) {
                throw new IllegalArgumentException("app names and app version lists should be equal in length but lengths were " +
                        appNames.length + " and " + versions.length + " respectively");
            }
            for(int i = 0; i < appNames.length; i++) {
                DEPLOYED_APPLICATIONS.put(appNames[i], versions[i]);
            }
            for(DeployedApplicationsUpdatedListener listener : LISTENERS) {
                listener.onApplicationsUpdated();
            }
        }
        finally {
            LOCK.writeLock().unlock();
        }
    }

    static void clearEverything() {
        try {
            LOCK.writeLock().lock();
            DEPLOYED_APPLICATIONS.clear();
            LISTENERS.clear();
        }
        finally {
            LOCK.writeLock().unlock();
        }
    }

    /**
     * Note that it isn't a good idea to call this once and cache the result because it won't be updated when another application update occurs.
     * So whenever a client needs to obtain the up-to-date set of deployed applications they should make a fresh
     * call to this method.
     * @return The list of deployed applications
     */
    public static Map<String,String> getDeployedApplications() {
        try {
            LOCK.readLock().lock();
            return Collections.unmodifiableMap(DEPLOYED_APPLICATIONS);
        }
        finally {
            LOCK.readLock().unlock();
        }
    }

    /**
     * Listeners added using this method will receive notifications when the deployed applications are updated e.g
     * after a deployment
     */
    public static void addApplicationUpdatedListener(DeployedApplicationsUpdatedListener deployedApplicationsUpdatedListener) {
        LISTENERS.add(deployedApplicationsUpdatedListener);
    }
}
