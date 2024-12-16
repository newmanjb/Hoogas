package com.noomtech.hoogas.monitoring;

import com.noomtech.hoogas.deployment.DeployedApplicationsUpdatedListener;
import com.noomtech.hoogas.datamodels.InternalMessageInbound;
import com.noomtech.hoogas.deployment.PeriodicChecker;
import com.noomtech.hoogas.internal_messaging.StatsListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Maintains an up-to-date store of information on the status of all applications and allows it to be queried over REST
 * @author Joshua Newman, December 2024
 */
public class MonitoringService implements StatsListener, DeployedApplicationsUpdatedListener, PeriodicChecker {


    //All operations in this class will be performed by the main thread via the routine where it runs every periodic checker's
    //checking functionality, so there's no need for any synchronization.

    private List<HoogasApplicationsStateListener> listenerList = new ArrayList<>();
    private Map<String,String> latestStatsMessagesPerApplication = new HashMap<>();
    private boolean applicationsUpdated;
    private long timeLastRun;
    private long checkingInterval;


    public MonitoringService(long checkingInterval) {
        this.checkingInterval = checkingInterval;

        //todo - Adds itself as listener to private ccnfig updates so as it can pick up when there's a change in the deployed application e.g. a new app has been deployed (do this in Main instead?)
        //todo - construct a cache that's refreshed as below and which can be queried over REST (stick a tiny web-server in here).
    }

    @Override
    public void onStatsMessageReceived(List<InternalMessageInbound> messages) {
        for(InternalMessageInbound msg : messages) {
            latestStatsMessagesPerApplication.put(msg.from(), msg.text());
        }
    }

    @Override
    public void onApplicationsUpdated() {
        applicationsUpdated = true;
    }

    public void addHoogasApplicationStateListener(HoogasApplicationsStateListener listener) {
        listenerList.add(listener);
    }

    public void removeHoogasApplicationStateListener(HoogasApplicationsStateListener listener) {
        listenerList.remove(listener);
    }

    @Override
    public void doCheck() {

        if(checkShouldRun(timeLastRun)) {
            try {
                String snapshot = rebuildState();
                publishState(snapshot);
            }
            finally {
                timeLastRun = System.currentTimeMillis();
            }
        }
    }

    @Override
    public long getInterval() {
        return checkingInterval;
    }


    private void publishState(String state) {
        for(HoogasApplicationsStateListener listener : listenerList) {
            listener.onStateChanged(state);
        }
    }

    private String rebuildState() {
        //todo rebuild based on the latest stats for each application and the onApplicationsUpdated boolean
        try {
            throw new UnsupportedOperationException();
        }
        finally {
            applicationsUpdated = false;
        }
    }
}
