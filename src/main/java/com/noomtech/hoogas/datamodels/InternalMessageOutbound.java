package com.noomtech.hoogas.datamodels;


import com.noomtech.hoogas_shared.internal_messaging.MessageTypeToApplications;

/**
 * Represents an internal message sent from the hoogas server to an application
 * @author Joshua Newman, December 2024
 */
public record InternalMessageOutbound(String text, MessageTypeToApplications type) {
}
