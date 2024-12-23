package com.noomtech.hoogas.constants;

import com.noomtech.hoogas_shared.constants.SharedConstants;

import java.io.File;

import static com.noomtech.hoogas_shared.constants.SharedConstants.INSTALLATION_DIR;

public class Constants {

    public static final String NAME_VERSION_SEPARATOR = "___";
    public static final String HOOGAS_CONFIG_FILE_NAME = "HoogasConfig.properties";
    public static final String HOOGAS_PUBLIC_CONFIG_FILE_NAME = "HoogasPublicConfig.properties";
    //Represents each directory under the installation directory above.  Each directory has a different function.
    public enum HoogasDirectory {

        CONFIG("config"),
        PUBLIC_CONFIG("public_config"),
        LOGS("logs"),
        APPLICATIONS(SharedConstants.APPLICATIONS_DIR_NAME),
        INTERNAL_MSGS_TO_HOOGAS(SharedConstants.INTERNAL_MSGS_TO_HOOGAS_DIR_NAME),
        INTERNAL_MSGS_FROM_HOOGAS(SharedConstants.INTERNAL_MSGS_FROM_HOOGAS_DIR_NAME),
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
