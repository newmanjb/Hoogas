package com.noomtech.hoogas.config;

import com.noomtech.hoogas.datamodels.Application;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantReadWriteLock;


/**
 * Holds the config for the hoogas application.  Alows reads and also specific modifications e.g. adding an application.
 * All modifications will write-through to the underlying file as well as to memory.  All operations are thread-safe.
 * @author Joshua Newman, Dec 2024
 */
public class HoogasConfigService {


    private final Map<String,String> config = new HashMap<>();
    private final ReentrantReadWriteLock reentrantReadWriteLock = new ReentrantReadWriteLock();
    //A cache of the deployed applications which is kept up to date.  This information is frequently read, so it makes sense to cache it
    //rather than read the config every time
    private final Map<String, Application> applicationsMap;
    private final List<ConfiguredApplicationsUpdatedListener> applicationUpdatedListeners = new CopyOnWriteArrayList<>();

    private static volatile HoogasConfigService INSTANCE;


    //Only ever called synchronously by the start-up routine
    public static void init() {
        if(INSTANCE != null) {
            throw new IllegalArgumentException(HoogasConfigService.class.getName() + " is already initialized");
        }

        INSTANCE = new HoogasConfigService();
    }


    private HoogasConfigService() {
        //todo - read environment variable for root dir and populate config map
        //todo - build applications map (use the validateDeployedApplicationConfig method), make it unmodifiable, and make sure it gets rebuilt when a new application is added)
        applicationsMap = buildApplicationsMap();
    }

    public static HoogasConfigService getInstance() {
        return INSTANCE;
    }

    /**
     * Listeners added using this method will receive notifications when the configured list of applications is updated e.g
     * after a deployment
     * @param applicationUpdatedListener
     */
    public void addApplicationUpdatedListener(ConfiguredApplicationsUpdatedListener applicationUpdatedListener) {
        applicationUpdatedListeners.add(applicationUpdatedListener);
    }

    /**
     * Convenience method.  Note that it isn't a good idea to call this once and cache the result because it can change e.g. if a
     * new application is deployed a new map will be created.  So whenever a client needs to obtain the up-to-date set of deployed applications they should make a fresh
     * call to this method.
     * @return The list of deployed applications
     * @see Application
     */
    public Map<String, Application> getDeployedApplications() {
        return applicationsMap;
    }

    public String getSetting(String key) {
        try {
            reentrantReadWriteLock.readLock().lock();
            return config.get(key);
        }
        finally {
            reentrantReadWriteLock.readLock().unlock();
        }
    }

    /**
     * Add a new application to the config, or modify an existing one e.g. if a deployment's been done and the version's
     * been updated.
     * @param application
     */
    public void updateApplications(Application application) {
        try {
            reentrantReadWriteLock.writeLock().lock();
            //todo - update the config file and rebuild the applications map
            for(ConfiguredApplicationsUpdatedListener applicationUpdatedListener : applicationUpdatedListeners) {
                applicationUpdatedListener.onApplicationUpdated();
            }
        }
        finally {
            reentrantReadWriteLock.writeLock().unlock();
        }
    }

    //-----------------------------------------------
    private Map<String,Application> buildApplicationsMap() {
        throw new UnsupportedOperationException();
    }

    //@todo - fix this method and use it to validate the application config on start-up
//    private Map<String,String> validateDeployedApplicationConfig() {
//
//        //Check the structure of the application directory and make sure it matches what's in the config.
//        // This ensures the integrity of the config and makes it less likely that Hoogas will run in a broken state e.g.
//        //if someone's pasted a directory into the application directory or manually updated a version then this application could end up in a semi-deployed state
//        var deployedApplications = new HashMap<String,String>();
//        var applicationDir = Main.HoogasDirectory.APPLICATIONS.getDirFile();
//        for(File deployment : applicationDir.listFiles()) {
//            var split = deployment.getName().split(NAME_VERSION_SEPARATOR);
//            if(split.length != 2) {
//                throw new IllegalStateException("Invalid deployment name found: " + applicationDir.getPath());
//            }
//
//            var appName = split[0];
//            var appVersion = split[1];
//            if(deployedApplications.containsKey(appName)) {
//                throw new IllegalStateException("Application " + appName + " deployed more than once.  Please check " + applicationDir.getPath() + " for two copies of a " +
//                        appName + " deployment, delete one, and update the config manually so as it ONLY contains the remaining deployment.  You can then restart Hoogas.");
//            }
//
//            deployedApplications.put(appName, appVersion);
//        }
//
//        var deployedApplicationNamesFromConfig = HoogasConfigService.getInstance().get("applications.name");
//        var deployedApplicationVersionsFromConfig = HoogasConfigService.getInstance().getPrivateConfig().get("applications.versions");
//        if((deployedApplicationNamesFromConfig == null && deployedApplicationVersionsFromConfig != null) ||
//                (deployedApplicationVersionsFromConfig == null && deployedApplicationNamesFromConfig != null)) {
//            throw new IllegalArgumentException("Application versions don't match application names");
//        }
//        else if(deployedApplicationVersionsFromConfig != null && deployedApplicationNamesFromConfig != null) {
//            var splitNames = deployedApplicationNamesFromConfig.split(NAME_VERSION_SEPARATOR);
//            var splitVersions = deployedApplicationVersionsFromConfig.split(NAME_VERSION_SEPARATOR);
//            if(splitNames.length != splitVersions.length) {
//                throw new IllegalArgumentException("Application versions don't match application names");
//            }
//            if(deployedApplications.size() != splitNames.length) {
//                throw new IllegalArgumentException("Mismatch between versions deployed and what's deployed according to the private config");
//            }
//
//            for(int i = 0; i < splitNames.length; i++) {
//                var name = splitNames[i];
//                var version = splitVersions[i];
//                var deployedVersion = deployedApplications.get(name);
//                if(deployedVersion == null){
//                    throw new IllegalStateException("Config says that application: " + name + " is deployed but it isn't (check in " + applicationDir.getPath() + ")");
//                }
//                else if(!deployedVersion.equals(version)){
//                    throw new IllegalStateException("Config says that application: " + name + " has version " + version + " but it's " + deployedVersion + " (check in " + applicationDir.getPath() + ")");
//                }
//            }
//        }
//        else {
//            if(!deployedApplications.isEmpty()) {
//                throw new IllegalStateException("Config says that no applications are deployed but there ARE applications deployed (check in " + applicationDir.getPath() + ")");
//            }
//        }
//
//        return deployedApplications;
//    }
}
