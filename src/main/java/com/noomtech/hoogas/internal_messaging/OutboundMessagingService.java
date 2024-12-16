package com.noomtech.hoogas.internal_messaging;


import com.noomtech.hoogas.constants.Constants;
import com.noomtech.hoogas.datamodels.InternalMessageOutbound;
import com.noomtech.hoogas.put_in_shared_project.SharedConstants;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Used by Hoogas to send messages to its applications.  The messaging protocol is just file transfer, as it doesn't have to fast or to be able to
 * handle a high frequency of transactions.
 * Messages are placed in the application's message folder as files with the same same as the message type.  The payload is in the file.
 * They are read by the hoogas client functionality which subsequently deletes them once they are processed.
 * Any existing messages in the application's message folders are overwritten if the new message is of the same type.
 * @author Joshua Newman, December 2024
 */
public class OutboundMessagingService {


    //Represents the different types of outbound (hoogas --> application) messages
    public enum DataTypeOutbound {
        GLOBAL_CFG_RESPONSE,
        //Just present so as we can write unit tests that put >1 message in each application's msg directory.
        //DELETE WHEN ANOTHER TYPE IS ADDED
        DUMMY
    }

    private static final class INSTANCE_HOLDER {
        private static OutboundMessagingService INSTANCE = new OutboundMessagingService();
    }

    private OutboundMessagingService() {
        //@todo - initialize using config holder
    }

    public static OutboundMessagingService getInstance() {
        return INSTANCE_HOLDER.INSTANCE;
    }

    /**
     * Sends the outbound message to all applications under hoogas
     * @return Null if all messages sent successfully, otherwise a list of the names of apps it failed to send to
     */
    public List<String> send(InternalMessageOutbound internalMessageOutbound, Map<String,String> destinationApps) {
        var couldntSendTo = new ArrayList<String>();
        for(Map.Entry<String,String> entry : destinationApps.entrySet()) {
            try {
                var file = new File(Constants.HoogasDirectory.APPLICATIONS.getDirFile().getPath() +
                        File.separator + entry.getKey() + Constants.NAME_VERSION_SEPARATOR + entry.getValue() + File.separator +
                        Constants.HoogasDirectory.INTERNAL_MSGS_FROM_HOOGAS.getDirName() + File.separator + internalMessageOutbound.type().name() + SharedConstants.HOOGAS_TO_APP_MSG_FILE_EXTENSION);

                if (file.exists()) {
                    System.out.println("INFO - Hoogas -> App message for app " + entry.getKey() + " of message type " +
                            internalMessageOutbound.type() + " with content '" + internalMessageOutbound.text() + "' will overwrite existing message with same type which hasn't been picked up yet");
                    if(!file.delete()) {
                        throw new IllegalStateException("Could not delete file: " + file.getPath());
                    }
                }

                if(!file.createNewFile()) {
                    throw new IllegalStateException("Could not create file: " + file.getPath());
                }

                try (var fileWriter = new BufferedWriter(new FileWriter(file))) {
                    fileWriter.write(internalMessageOutbound.text());
                }
            }
            catch(Exception e) {
                //todo - add logging and log a useful message
                e.printStackTrace();
                couldntSendTo.add(entry.getKey());
            }
        }

        return couldntSendTo.isEmpty() ? null : couldntSendTo;
    }
}
