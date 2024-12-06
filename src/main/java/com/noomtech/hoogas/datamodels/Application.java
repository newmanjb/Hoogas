package com.noomtech.hoogas.datamodels;


/**
 * Encapsulates an application that hoogas looks after
 * @author Joshua Newman, December 2024
 */
public record Application(String name, String version, String startCommand, String installationDirectory) {
}
