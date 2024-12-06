package com.noomtech.hoogas.config.public_config;


import com.noomtech.hoogas.datamodels.InternalMessageInbound;
import com.noomtech.hoogas.internal_messaging.ConfigRequestListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles requests from the applications for the public configuration and also publishes notification messages when the public configuration is updated.
 * @author Joshua Newman, December 2024
 */
public class PublicConfigService implements ConfigRequestListener {


    private final Map<String,String> publicConfig = new HashMap<>();


    public PublicConfigService() {
        //todo - populate the public config using the root dir setting
    }

    //todo - add webserver endpoints that edit the config and then publish out config updates


    @Override
    public void onConfigRequestMessageReceived(List<InternalMessageInbound> message) {
        //todo - reply to the apps that have requested it using the outbound messaging service
    }
}
