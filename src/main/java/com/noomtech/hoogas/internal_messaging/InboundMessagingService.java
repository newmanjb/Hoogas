package com.noomtech.hoogas.internal_messaging;


import com.noomtech.hoogas.config.HoogasConfigService;
import com.noomtech.hoogas.constants.Constants;
import com.noomtech.hoogas.datamodels.Application;
import com.noomtech.hoogas.datamodels.InternalMessageInbound;
import com.noomtech.hoogas.deployment.PeriodicChecker;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Used by Hoogas to receive messages from its applications.  The messaging protocol is just file transfer, as it doesn't have to fast or to be able to
 * handle a high frequency of transactions.
 * Messages are placed in the application's outbound message folder as files by the applications using the hoogas client functionality.
 * The files have the same name as the type of the message.  The payload is in the file.
 * The files are picked up by this class which subsequently deletes them once they are processed.
 * Existing messages of the same type in an application's outbound message folder are overwritten by the new messages.
 * @author Joshua Newman, December 2024
 */
public class InboundMessagingService implements PeriodicChecker {


    private static final List<StatsListener> statsListeners = new CopyOnWriteArrayList<>();
    private static final List<ConfigRequestListener> configRequestListeners = new CopyOnWriteArrayList<>();
    private long whenLastRunFinished;

    private final long CHECKING_INTERVAL = Long.parseLong(HoogasConfigService.getInstance().getSetting("inbound_msg_service.checking.interval"));

    //enum that represents the different types of message that can be received.  It's also also to segregate the types of messages received
    // and their different listener lists and to fire those listeners.
    public enum DataTypeInbound {
        STATS(statsListeners),
        PUBLIC_CFG_REQUEST(configRequestListeners);

        private final List<InternalMessageInbound> receivedMessages = new ArrayList<>();
        private final List<? extends InboundInternalMessageListener> listeners;
        DataTypeInbound(List<? extends InboundInternalMessageListener> listeners) {
            this.listeners = listeners;
        }

        void addReceivedMessage(InternalMessageInbound message) {
            receivedMessages.add(message);
        }

        void fireListeners() {
            for(InboundInternalMessageListener listener : listeners) {
                listener.onMessageReceived(receivedMessages);
            }
            receivedMessages.clear();
        }
    }

    public InboundMessagingService() {
        //@todo - initialize using config holder
    }

    @Override
    public void doCheck() throws Exception {
        if(checkShouldRun(whenLastRunFinished)) {
            collect();
            whenLastRunFinished = System.currentTimeMillis();
        }
    }

    @Override
    public long getInterval() {
        return CHECKING_INTERVAL;
    }

    /**
     * Scans each application's message directory for inbound messages and fires the listeners added to this class.
     */
    private void collect() throws Exception {
        var apps = HoogasConfigService.getInstance().getDeployedApplications();
        //Messages are collected first during the scanning routine and then sent in bulk.  It's more efficient
        //than firing all the listeners for each message from each application.
        for(Application app : apps.values()) {
            try {
                var internalMessagesDir = new File(app.installationDirectory() + File.pathSeparator + Constants.HoogasDirectory.INTERNAL_MSGS_TO_HOOGAS.getDirName());
                var msgFiles = internalMessagesDir.listFiles();
                for (File msgFile : msgFiles) {
                    try {
                        var dataType = DataTypeInbound.valueOf(msgFile.getName());
                        try (var reader = new BufferedReader(new FileReader(msgFile))) {
                            var message = new InternalMessageInbound(reader.readLine(), app);
                            dataType.addReceivedMessage(message);
                            if(!msgFile.delete()) {
                                throw new IllegalStateException("Could not delete message file: " + msgFile.getPath());
                            }
                        }
                    } catch (Exception e) {
                        //todo - add proper logging and an informative message
                        e.printStackTrace();
                    }
                }
            }
            catch(Exception e) {
                //todo - add proper logging and an informative message
                e.printStackTrace();
            }
        }
        for(DataTypeInbound dataTypeInbound : DataTypeInbound.values()) {
            dataTypeInbound.fireListeners();
        }
    }

    public void addConfigRequestListener(ConfigRequestListener listener) {
        configRequestListeners.add(listener);
    }

    public void removeConfigRequestListener(ConfigRequestListener listener) {
        configRequestListeners.remove(listener);
    }

    public void addStatsListener(StatsListener listener) {
        statsListeners.add(listener);
    }

    public void removeStatsListener(StatsListener listener) {
        statsListeners.remove(listener);
    }
}
