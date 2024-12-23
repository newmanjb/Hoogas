package com.noomtech.hoogas.internal_messaging;

import com.noomtech.hoogas.datamodels.InternalMessageInbound;

import java.util.List;

/**
 * Implemented by anything needs to process new stats requests when they are received
 * @author Joshua Newman, December 2024
 */
public interface StatsListener extends InboundInternalMessageListener {

    default void onMessageReceived(List<InternalMessageInbound> message) {
        onStatsMessageReceived(message);
    }

    void onStatsMessageReceived(List<InternalMessageInbound> message);
}
