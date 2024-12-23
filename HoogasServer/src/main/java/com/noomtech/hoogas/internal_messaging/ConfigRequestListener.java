package com.noomtech.hoogas.internal_messaging;

import com.noomtech.hoogas.datamodels.InternalMessageInbound;

import java.util.List;


/**
 * Implemented by anything that needs process new config requests when they are received
 * @author Joshua Newman, December 2024
 */
public interface ConfigRequestListener extends InboundInternalMessageListener {

    default void onMessageReceived(List<InternalMessageInbound> message) {
        onConfigRequestMessageReceived(message);
    }

    void onConfigRequestMessageReceived(List<InternalMessageInbound> message);
}
