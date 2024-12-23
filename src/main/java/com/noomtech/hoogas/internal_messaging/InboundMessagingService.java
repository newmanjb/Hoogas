package com.noomtech.hoogas.internal_messaging;


import com.noomtech.hoogas.constants.Constants;
import com.noomtech.hoogas.datamodels.InternalMessageInbound;
import com.noomtech.hoogas.deployment.DeployedApplicationsHolder;
import com.noomtech.hoogas.deployment.PeriodicChecker;
import com.noomtech.hoogas_shared.internal_messaging.MessageTypeFromApplications;

import java.io.*;
import java.util.*;

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


    private Map<String, MessageProcessor> messageProcessors = new HashMap<>();
    private final long checkingInterval;
    private long whenLastRunFinished;


    public InboundMessagingService(long checkingInterval) {
        this.checkingInterval = checkingInterval;

        for(MessageTypeFromApplications type : MessageTypeFromApplications.values()) {
            messageProcessors.put(type.name(), new MessageProcessor());
        }
    }

    @Override
    public void doCheck() throws Exception {
        if(checkShouldRun(whenLastRunFinished)) {
            try {
                collect();
            }
            finally {
                whenLastRunFinished = System.currentTimeMillis();
            }
        }
    }

    @Override
    public long getInterval() {
        return checkingInterval;
    }

    /**
     * Scans each application's message directory for inbound messages and fires the listeners added to this class.
     */
    void collect() throws Exception {
        var messageProcessorsToInvoke = new HashSet<MessageProcessor>();
        var apps = DeployedApplicationsHolder.getDeployedApplications();
        //Messages are collected first during the scanning routine and then sent in bulk.  It's more efficient
        //than firing all the listeners for each message from each application.
        for(Map.Entry<String,String> entry : apps.entrySet()) {
            try {
                var internalMessagesDir = new File(Constants.HoogasDirectory.APPLICATIONS.getDirFile().getPath() +
                        File.separator + entry.getKey() + Constants.NAME_VERSION_SEPARATOR + entry.getValue() +
                        File.separator + Constants.HoogasDirectory.INTERNAL_MSGS_TO_HOOGAS.getDirName());
                var msgFiles = internalMessagesDir.listFiles();
                for (File msgFile : msgFiles) {
                    try {
                        if(!msgFile.isDirectory()) {
                            var messageProcessor = Objects.requireNonNull(messageProcessors.get(msgFile.getName()));
                            try (var reader = new BufferedReader(new FileReader(msgFile))) {
                                InternalMessageInbound message = new InternalMessageInbound(reader.readLine(), entry.getKey());
                                messageProcessor.addReceivedMessage(message);
                                messageProcessorsToInvoke.add(messageProcessor);
                            }
                            if (!msgFile.delete()) {
                                throw new IllegalStateException("Could not delete message file: " + msgFile.getPath());
                            }
                        }
                        else {
                            System.out.println("Found a directory in " + internalMessagesDir.getPath() + ": '" + msgFile.getPath() + "'.  Ignoring this.");
                        }
                    }
                    catch (IllegalArgumentException e) {
                        //todo - add proper logging
                       System.out.println("Invalid inbound message file in " + internalMessagesDir.getPath() + ": " + msgFile.getPath() + "  " + e);
                    }
                    catch(Exception e) {
                        System.out.println("Problem with message file : " + msgFile.getPath() + "  " + e);
                    }
                }
            }
            catch(Exception e) {
                //todo - add proper logging and an informative message
                e.printStackTrace();
            }
        }
        for(MessageProcessor messageProcessor : messageProcessorsToInvoke) {
            messageProcessor.processMessagesReceived();
        }
    }

    private static class MessageProcessor {
        private final List<InboundInternalMessageListener> listeners = new ArrayList<>();
        private final List<InternalMessageInbound> messagesReceived = new ArrayList<>();
        private void addReceivedMessage(InternalMessageInbound message){
            messagesReceived.add(message);
        }
        private void addListener(InboundInternalMessageListener listener){
            listeners.add(listener);
        }
        private void removeListener(InboundInternalMessageListener listener) {
            listeners.remove(listener);
        }
        private void processMessagesReceived(){
            if(!messagesReceived.isEmpty()) {
                var readOnlyMessagesReceived = Collections.unmodifiableList(messagesReceived);
                for (InboundInternalMessageListener listener : listeners) {
                    listener.onMessageReceived(readOnlyMessagesReceived);
                }
                messagesReceived.clear();
            }
        }
    }

    public void addConfigRequestListener(ConfigRequestListener listener) {
        addMessageListener(MessageTypeFromApplications.PUBLIC_CFG_REQUEST, listener);
    }

    public void removeConfigRequestListener(ConfigRequestListener listener) {
        removeMessageListener(MessageTypeFromApplications.PUBLIC_CFG_REQUEST, listener);
    }

    public void addStatsListener(StatsListener listener) {
        addMessageListener(MessageTypeFromApplications.STATS, listener);
    }

    public void removeStatsListener(StatsListener listener) {
        removeMessageListener(MessageTypeFromApplications.STATS, listener);
    }

    private void addMessageListener(MessageTypeFromApplications messageType, InboundInternalMessageListener listener) {
        messageProcessors.get(messageType.name()).addListener(listener);
    }

    private void removeMessageListener(MessageTypeFromApplications messageType, InboundInternalMessageListener listener) {
        messageProcessors.get(messageType.name()).removeListener(listener);
    }
}
