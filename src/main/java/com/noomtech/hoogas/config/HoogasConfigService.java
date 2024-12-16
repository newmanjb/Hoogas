package com.noomtech.hoogas.config;

import com.noomtech.hoogas.constants.Constants;

import java.io.File;
import java.io.FileReader;
import java.util.*;


/**
 * Holds the config for the hoogas application.
 * @author Joshua Newman, Dec 2024
 */
public class HoogasConfigService {


    private final Properties config;

    private static volatile HoogasConfigService INSTANCE;


    //Only ever called synchronously by the start-up routine
    public static void init() throws Exception {
        if(INSTANCE != null) {
            throw new IllegalArgumentException(HoogasConfigService.class.getName() + " is already initialized");
        }

        INSTANCE = new HoogasConfigService();
    }


    private HoogasConfigService() throws Exception {
        var configDir = Constants.HoogasDirectory.CONFIG.getDirFile() + File.separator + Constants.HOOGAS_CONFIG_FILE_NAME;
        Properties properties = new Properties();
        properties.load(new FileReader(configDir));
        config = properties;
    }

    public static HoogasConfigService getInstance() {
        return INSTANCE;
    }

    public String getSetting(String key) {
        return Optional.of(config.getProperty(key)).orElseThrow(() -> new IllegalArgumentException("Could not find setting: " + key));
    }
}
