package hoogas_client;

import com.noomtech.hoogas_shared.constants.SharedConstants;
import com.noomtech.hoogas_shared.internal_messaging.MessageTypeFromApplications;
import com.noomtech.hoogas_shared.internal_messaging.MessageTypeToApplications;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HoogasClient {


    private static volatile boolean initialised;
    private static final Object CHECKER_ROUTINE_MUTEX = new Object();
    private static volatile boolean shutdown;


    private HoogasClient(){}

    /**
     * This should be the first call made when the application using Hoogas client starts up.  Start-up should NOT continue until the
     * call has returned.
     * The hoogas client uses files to communicate with the hoogas server, where each a file is a message. This class will create 2 folders for these files in the application's root directory:
     * 1 - {@link SharedConstants#INTERNAL_MSGS_TO_HOOGAS_DIR_NAME} for messages from the application to Hoogas e.g. a request for the public config.
     * 2 - {@link SharedConstants#INTERNAL_MSGS_FROM_HOOGAS_DIR_NAME} for messages from Hoogas to the application e.g. a stop message or a message containing the public config
     * When the receiver (Hoogas server or the application using Hoogas client) picks the message up it will delete the file.
     * @param appName The name that the application was deployed to hoogas under
     * @param listener This will be notified of any communications from hoogas after the initialisation i.e. during the running of the application that's using hoogas client
     * @return The current public config
     */
    public static Map<String,String> init(String appName, HoogasMessageListener listener) throws Exception {

        if(!initialised) {

            var appInstallationDir = SharedConstants.INSTALLATION_DIR.getPath() + File.separator +
                    SharedConstants.APPLICATIONS_DIR_NAME + File.separator + appName;
            var messagesToHoogasServerDir = new File(appInstallationDir + File.separator + SharedConstants.INTERNAL_MSGS_TO_HOOGAS_DIR_NAME);
            var messagesFromHoogasServerDir = new File(appInstallationDir + File.separator + SharedConstants.INTERNAL_MSGS_FROM_HOOGAS_DIR_NAME);

            //Create the messaging dirs if necessary.  If they already exist delete the contents
            setUpMessagingDir(messagesToHoogasServerDir);
            setUpMessagingDir(messagesFromHoogasServerDir);

            //Send a config request message to get the public config and wait until it has been received
            sendToHoogas(messagesToHoogasServerDir.getPath(),"", MessageTypeFromApplications.PUBLIC_CFG_REQUEST);
            var msgFromHoogasList = waitForPublicConfig(messagesFromHoogasServerDir);
            var propertiesMap = buildConfigMap(msgFromHoogasList.getFirst().text());

            //Start a thread that intermittently polls for messages from Hoogas
            Thread messageCheckerThread = new Thread(new CheckerRoutine(messagesFromHoogasServerDir, listener));
            messageCheckerThread.setName(appName + "_HoogasClient");
            messageCheckerThread.setDaemon(false);
            messageCheckerThread.start();

            initialised = true;

            return propertiesMap;
        }
        else {
            throw new IllegalArgumentException("Already initialised");
        }
    }

    public static void setStarting() {
        checkInitialised();
    }
    public static void setStopping() {
        checkInitialised();
    }
    public static void setRunning() {
        checkInitialised();
    }
    public static void setStopped() {
        checkInitialised();
    }

    /**
     * Should be called when the application that's using Hoogas client shuts down
     */
    public static void shutdown() {
        shutdown = true;
        synchronized (CHECKER_ROUTINE_MUTEX) {
            CHECKER_ROUTINE_MUTEX.notify();
        }
    }

    static List<MsgFromHoogas> waitForPublicConfig(File dir) throws Exception {
        var requestMadeTime = System.currentTimeMillis();
        List<MsgFromHoogas> msgFromHoogasList;
        long timeout = 4000;
        do {
            msgFromHoogasList = checkForMsgsFromHoogas(dir);
            Thread.sleep(250);
        }
        while(msgFromHoogasList.isEmpty() && (System.currentTimeMillis() - requestMadeTime) < timeout);

        if(msgFromHoogasList.isEmpty()) {
            throw new IllegalStateException("Timed-out out waiting for a response to the public config request.  Timeout was " + timeout);
        }
        if(msgFromHoogasList.size() != 1 || msgFromHoogasList.getFirst().type() != MessageTypeToApplications.PUBLIC_CFG_RESPONSE) {
            throw new IllegalStateException("Invalid response to config request from Hoogas.  Expected a single message of type " + MessageTypeToApplications.PUBLIC_CFG_RESPONSE.name());
        }

        return  msgFromHoogasList;
    }

    static void sendToHoogas(String msgToHoogasDir, String text, MessageTypeFromApplications type) throws Exception {
        var msgFile = new File(msgToHoogasDir + File.separator + type.name());
        if(msgFile.exists() && !msgFile.delete()) {
            throw new IllegalStateException("Cannot delete existing message file: " + msgFile.getPath());
        }
        if(!msgFile.createNewFile()) {
            throw new IllegalStateException("Cannot create message file: " + msgFile.getPath());
        }
        try(BufferedWriter writer = new BufferedWriter(new FileWriter(msgFile))) {
            writer.write(text);
        }
    }

    static List<MsgFromHoogas> checkForMsgsFromHoogas(File dir) throws Exception {

        var messagesFound = new ArrayList<MsgFromHoogas>();
        var msgFiles = dir.listFiles();
        for(var msgFile : msgFiles) {
            var type = MessageTypeToApplications.valueOf(msgFile.getName());
            var text = new String(Files.readAllBytes(Paths.get(msgFile.getPath())));
            messagesFound.add(new MsgFromHoogas(type, text));
            if(!msgFile.delete()) {
                throw new IllegalStateException("Could not delete file: " + msgFile.getPath());
            }
        }
        return messagesFound;
    }

    static void setUpMessagingDir(File dir) {
        if(!dir.exists()) {
            //todo - proper logging
            System.out.println("Creating messaging dir: " + dir.getPath());
            if(!dir.mkdir()) {
                throw new IllegalStateException("Could not create messaging dir: " + dir.getPath());
            }
        }
        else {
            for(File file : dir.listFiles()) {
                if(file.isFile() && !file.delete()) {
                    throw new IllegalStateException("Could not delete file '" + file.getPath() + "' when clearing out messaging dir");
                }
                else if(file.isDirectory()) {
                    throw new IllegalStateException("File in messaging dir is a directory: '" + file.getPath());
                }
            }
        }
    }

    private static void checkInitialised() {
        if(!initialised) {
            throw new IllegalArgumentException("Not initialised");
        }
    }

    private static Map<String,String> buildConfigMap(String text) {
        var split1 = text.split(SharedConstants.NEWLINE);
        var propertiesMap = new HashMap<String,String>();
        for(String line : split1) {
            var split2 = line.split("=");
            propertiesMap.put(split2[0], split2[1]);
        }
        return propertiesMap;
    }

    private static class CheckerRoutine implements Runnable {

        private final File msgsFromHoogasDir;
        private final HoogasMessageListener hoogasMessageListener;

        public CheckerRoutine(File msgsFromHoogasDir, HoogasMessageListener listener) {
            this.msgsFromHoogasDir = msgsFromHoogasDir;
            this.hoogasMessageListener = listener;
        }

        public void run() {
            synchronized (CHECKER_ROUTINE_MUTEX) {
                while (!shutdown) {
                    try {
                        var msgFromHoogasList = checkForMsgsFromHoogas(msgsFromHoogasDir);
                        if (!msgFromHoogasList.isEmpty()) {
                            for (MsgFromHoogas msgFromHoogas : msgFromHoogasList) {
                                switch (msgFromHoogas.type()) {
                                    case MessageTypeToApplications.PUBLIC_CFG_RESPONSE -> {
                                        hoogasMessageListener.onPublicConfigUpdate(buildConfigMap(msgFromHoogas.text()));
                                    }
                                    case MessageTypeToApplications.STOP -> {
                                        hoogasMessageListener.onStop();
                                    }
                                    default -> {
                                        throw new UnsupportedOperationException("Unsupported message type: " + msgFromHoogas.type());
                                    }

                                }
                            }
                        }
                    } catch (Exception e) {
                        System.out.println("Problem checking for messages from Hoogas: " + e);
                    }
                    try {CHECKER_ROUTINE_MUTEX.wait(2000);}catch(InterruptedException e){System.out.println("Checker routine interrupted.  That shouldn't have happened" + e);}
                }
            }
        }
    }
}
