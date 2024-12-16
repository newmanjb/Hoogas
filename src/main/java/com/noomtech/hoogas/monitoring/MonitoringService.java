package com.noomtech.hoogas.monitoring;

import com.noomtech.hoogas.deployment.DeployedApplicationsUpdatedListener;
import com.noomtech.hoogas.datamodels.InternalMessageInbound;
import com.noomtech.hoogas.internal_messaging.StatsListener;

import java.util.List;


/**
 * Maintains an up-to-date store of information on the status of all applications and allows it to be queried over REST
 * @author Joshua Newman, December 2024
 */
public class MonitoringService implements StatsListener, DeployedApplicationsUpdatedListener {


    public MonitoringService() {
        //todo - Adds itself as listener to private ccnfig updates so as it can pick up when there's a change in the deployed application e.g. a new app has been deployed (do this in Main instead?)
        //todo - construct a cache that's refreshed as below and which can be queried over REST (stick a tiny web-server in here).
    }

    @Override
    public void onStatsMessageReceived(List<InternalMessageInbound> messages) {
        //todo - parse the stats and update the cache
    }

    @Override
    public void onApplicationsUpdated() {
        //todo - rebuild
    }
}
