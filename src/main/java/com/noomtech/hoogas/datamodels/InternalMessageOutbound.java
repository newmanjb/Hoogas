package com.noomtech.hoogas.datamodels;


import com.noomtech.hoogas.internal_messaging.OutboundMessagingService;

/**
 * Represents an internal message sent from the hoogas server to an application
 * @author Joshua Newman, December 2024
 */
public record InternalMessageOutbound(String text, OutboundMessagingService.DataTypeOutbound type) {
}
