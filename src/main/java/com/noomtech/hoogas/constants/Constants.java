package com.noomtech.hoogas.constants;

import java.io.File;

public class Constants {

    public static final String NAME_VERSION_SEPARATOR = "___";
    public static final File INSTALLATION_DIR = new File(System.getProperty("installation_dir"));
    public static final String HOOGAS_CONFIG_FILE_NAME = "HoogasConfig.properties";
    //Represents each directory under the installation directory above.  Each directory has a different function.
    public enum HoogasDirectory {

        CONFIG("config"),
        PUBLIC_CONFIG("public_config"),
        LOGS("logs"),
        APPLICATIONS("applications"),
        INTERNAL_MSGS_TO_HOOGAS("to_hoogas_messages"),
        INTERNAL_MSGS_FROM_HOOGAS("from_hoogas_messages"),
        ARCHIVE("archive"),
        DEPLOYMENTS("deployments");

        private final String dirName;
        private final File theDirectory;
        HoogasDirectory(String dirName) {
            this.dirName = dirName;
            this.theDirectory = new File(INSTALLATION_DIR + File.separator + dirName);
        }

        public String getDirName() {
            return dirName;
        }

        public File getDirFile() {
            return theDirectory;
        }
    }
}
