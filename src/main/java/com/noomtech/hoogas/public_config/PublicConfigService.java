package com.noomtech.hoogas.public_config;


import com.noomtech.hoogas.constants.Constants;
import com.noomtech.hoogas.datamodels.InternalMessageInbound;
import com.noomtech.hoogas.datamodels.InternalMessageOutbound;
import com.noomtech.hoogas.deployment.DeployedApplicationsHolder;
import com.noomtech.hoogas.deployment.PeriodicChecker;
import com.noomtech.hoogas.internal_messaging.ConfigRequestListener;
import com.noomtech.hoogas.internal_messaging.OutboundMessagingService;
import com.noomtech.hoogas.put_in_shared_project.SharedConstants;

import java.io.File;
import java.io.FileReader;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Handles requests from the applications for the public configuration.
 * @author Joshua Newman, December 2024
 */
public class PublicConfigService implements ConfigRequestListener, PeriodicChecker {

    //No synchronization is necessary in this class as it's all run from the main thread


    private final Properties publicConfig;
    private final Set<String> configRequests = new HashSet<>();
    private final long checkingInterval;
    private long timeLastRun;


    public PublicConfigService(long checkingInterval) throws Exception {
        this.checkingInterval = checkingInterval;
        var configDir = Constants.HoogasDirectory.PUBLIC_CONFIG.getDirFile().getPath() + File.separator + Constants.HOOGAS_PUBLIC_CONFIG_FILE_NAME;
        Properties properties = new Properties();
        try(var reader = new FileReader(configDir)) {
            properties.load(reader);
        }
        publicConfig = properties;
    }

    //todo - add webserver endpoints that edit the config and then publish out config updates


    //Cache the config requests.  They're then sent below.
    @Override
    public void onConfigRequestMessageReceived(List<InternalMessageInbound> messages) {
        for(InternalMessageInbound msg : messages) {
            configRequests.add(msg.from());
        }
    }

    @Override
    public void doCheck() throws Exception {
        if(checkShouldRun(timeLastRun)) {
            try {
                if (!configRequests.isEmpty()) {
                    StringBuilder propertiesStringBuilder = new StringBuilder();
                    for (Map.Entry<Object, Object> property : publicConfig.entrySet()) {
                        propertiesStringBuilder.append(property.getKey()).append("=").append(property.getValue()).append(SharedConstants.NEWLINE);
                    }
                    var propertiesString = propertiesStringBuilder.toString();

                    var destinations = DeployedApplicationsHolder.getDeployedApplications().entrySet().stream().filter(
                            e -> configRequests.contains(e.getKey())).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

                    OutboundMessagingService.getInstance().send(new InternalMessageOutbound(propertiesString, OutboundMessagingService.DataTypeOutbound.GLOBAL_CFG_RESPONSE), destinations);
                }
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
}