package hoogas_shared.constants;


import java.io.File;

/**
 * Constants used by both the hoogas server application and its clients.
 * @author Joshua Newman, December 2024
 */
public class SharedConstants {


    public static final File INSTALLATION_DIR = new File(System.getProperty("installation_dir"));
    public static final String NEWLINE = System.lineSeparator();
    public static final String APPLICATIONS_DIR_NAME = "applications";
    public static final String INTERNAL_MSGS_TO_HOOGAS_DIR_NAME = "to_hoogas_messages";
    public static final String INTERNAL_MSGS_FROM_HOOGAS_DIR_NAME = "from_hoogas_messages";

    public enum APPLICATION_STATE {
        STARTING,
        RUNNING,
        STOPPING,
        STOPPED
    }
}
