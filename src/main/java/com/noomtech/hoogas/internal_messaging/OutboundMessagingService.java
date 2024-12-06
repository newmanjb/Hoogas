package com.noomtech.hoogas.internal_messaging;


import com.noomtech.hoogas.config.HoogasConfigService;
import com.noomtech.hoogas.constants.Constants;
import com.noomtech.hoogas.datamodels.Application;
import com.noomtech.hoogas.datamodels.InternalMessageOutbound;

import java.io.*;

/**
 * Used by Hoogas to send messages to its applications.  The messaging protocol is just file transfer, as it doesn't have to fast or to be able to
 * handle a high frequency of transactions.
 * Messages are placed in the application's message folder as files with the same same as the message type.  The payload is in the file.
 * They are read by the hoogas client functionality which subsequently deletes them once they are processed.
 * Any existing messages in the application's message folders are overwritten if the new message is of the same type.
 * @author Joshua Newman, December 2024
 */
public class OutboundMessagingService {


    private static volatile boolean initialized;
    private static volatile OutboundMessagingService INSTANCE;


    //Represents the different types of outbound (hoogas --> application) messages
    public enum DataTypeOutbound {
        GLOBAL_CFG_RESPONSE
    }

    //Only ever called by the start-up routine, which calls it synchronously
    public static void init() {
        if(INSTANCE != null) {
            throw new IllegalArgumentException(OutboundMessagingService.class.getName() + " is already initialized");
        }

        INSTANCE = new OutboundMessagingService();
        initialized = true;
    }

    private OutboundMessagingService() {
        //@todo - initialize using config holder
    }

    public static OutboundMessagingService getInstance() {
        return INSTANCE;
    }

    /**
     * Sends the outbound message to all applications under hoogas
     * @throws Exception
     */
    public void send(InternalMessageOutbound internalMessageOutbound) throws Exception {
        var apps = HoogasConfigService.getInstance().getDeployedApplications();
        for(Application app : apps.values()) {
            try {
                var file = new File(app.installationDirectory() + File.pathSeparator +
                        Constants.HoogasDirectory.INTERNAL_MSGS_FROM_HOOGAS.getDirName() + File.pathSeparator + internalMessageOutbound.type().name() + ".hoogas_msg");
                if (file.exists() && !file.delete()) {
                    throw new IllegalStateException("Previous message file: " + file.getPath() + " could not be deleted");
                }

                if (!file.createNewFile()) {
                    throw new IllegalStateException("Could not create message file: " + file.getPath());
                }

                try (var fileWriter = new BufferedWriter(new FileWriter(file))) {
                    fileWriter.write(internalMessageOutbound.text());
                }
            }
            catch(Exception e) {
                //todo - add logging and log a useful message
                e.printStackTrace();
            }
        }
    }
}
