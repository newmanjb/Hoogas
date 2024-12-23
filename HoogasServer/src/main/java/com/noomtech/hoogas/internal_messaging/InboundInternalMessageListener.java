package com.noomtech.hoogas.internal_messaging;


import com.noomtech.hoogas.datamodels.InternalMessageInbound;

import java.util.List;


/**
 * Super-interface for the inbound message listeners
 * @author Joshua Newman, Dec 2024
 */
public interface InboundInternalMessageListener {
    void onMessageReceived(List<InternalMessageInbound> message);
}
