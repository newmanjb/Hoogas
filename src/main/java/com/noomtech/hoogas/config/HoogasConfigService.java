package com.noomtech.hoogas.config;

import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;


/**
 * Holds the config for the hoogas application.
 * @author Joshua Newman, Dec 2024
 */
public class HoogasConfigService {


    private final Map<String,String> config = new HashMap<>();
    private final ReentrantReadWriteLock reentrantReadWriteLock = new ReentrantReadWriteLock();

    private static volatile HoogasConfigService INSTANCE;


    //Only ever called synchronously by the start-up routine
    public static void init() {
        if(INSTANCE != null) {
            throw new IllegalArgumentException(HoogasConfigService.class.getName() + " is already initialized");
        }

        INSTANCE = new HoogasConfigService();
    }


    private HoogasConfigService() {
        //todo - read environment variable for root dir and populate config map
    }

    public static HoogasConfigService getInstance() {
        return INSTANCE;
    }


    public String getSetting(String key) {
        try {
            reentrantReadWriteLock.readLock().lock();
            return config.get(key);
        }
        finally {
            reentrantReadWriteLock.readLock().unlock();
        }
    }
}
