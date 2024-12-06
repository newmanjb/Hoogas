package com.noomtech.hoogas.datamodels;

/**
 * Represents an internal message sent from an application to the hoogas server
 * @author Joshua Newman, December 2024
 * @param text
 * @param from
 */
public record InternalMessageInbound(String text, Application from) {
}
