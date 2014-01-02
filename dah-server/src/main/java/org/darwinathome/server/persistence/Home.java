package org.darwinathome.server.persistence;

import com.thoughtworks.xstream.XStream;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Handle the home directory and the main configuration file
 *
 * @author Gerald de Jong <geralddejong@gmail.com>
 */

public class Home {
    private Logger log = Logger.getLogger(getClass());
    private File directory;
    private File configFile;
    private File oldConfigFile;
    private Configuration configuration;

    public Home() {
        String tetragotchiHome = System.getenv("TETRAGOTCHI_HOME");
        if (tetragotchiHome == null) {
            log.error("The environment variable TETRAGOTCHI_HOME must contain the path to the directory where the game is stored");
            System.exit(1);
        }
        directory = new File(tetragotchiHome);
        if (!directory.exists()) {
            log.warn("Directory does not exist: " + tetragotchiHome);
            if (directory.mkdirs()) {
                log.info("Created tetragotchi home: " + directory.getAbsolutePath());
            }
            else {
                log.error("Unable to create tetragotchi home: " + directory.getAbsolutePath());
                System.exit(1);
            }
        }
        configFile = new File(directory, "tetragotchi-configuration.xml");
        oldConfigFile = new File(directory, "tetragotchi-configuration.xml.old");
        log.info("Reading configuration from " + configFile.getAbsolutePath());
        configuration = readFrom(configFile);
        if (configuration == null) {
            log.info("Reading configuration from " + oldConfigFile.getAbsolutePath());
            configuration = readFrom(oldConfigFile);
            if (configuration == null) {
                configuration = Configuration.create();
                try {
                    saveConfiguration();
                }
                catch (IOException e) {
                    log.error("Unable to save configuration", e);
                    System.exit(1);
                }
            }
        }
        if (configuration.hasUnknowns()) {
            log.info("Please edit the unknown values in " + configFile.getAbsolutePath());
            System.exit(1);
        }
        new Motor().start();
    }

    public File getDirectory() {
        return directory;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    // ======

    private Configuration readFrom(File file) {
        try {
            if (!file.exists()) {
                log.error("File doesn't exist: " + file.getAbsolutePath());
                return null;
            }
            FileReader reader = new FileReader(file);
            XStream stream = new XStream();
            stream.processAnnotations(Configuration.class);
            Configuration config = (Configuration) stream.fromXML(reader);
            config.finish();
            reader.close();
            return config;
        }
        catch (IOException e) {
            log.error("Unable to read configuration from " + file.getAbsolutePath());
            return null;
        }
    }

    private synchronized void saveConfiguration() throws IOException {
        if (configuration.isDirty()) {
            if (oldConfigFile.exists()) {
                if (!oldConfigFile.delete()) {
                    log.warn("Unable to delete " + oldConfigFile.getAbsolutePath());
                }
            }
            if (configFile.exists()) {
                if (!configFile.renameTo(oldConfigFile)) {
                    log.warn("Unable to rename config to " + oldConfigFile.getAbsolutePath());
                }
            }
            log.info("Saving configuration to " + configFile.getAbsolutePath());
            FileWriter writer = new FileWriter(configFile);
            XStream stream = new XStream();
            stream.processAnnotations(Configuration.class);
            stream.toXML(configuration, writer);
            writer.close();
            configuration.clean();
        }
    }

    private class Motor implements Runnable {
        private static final long DELAY = 300;

        public void start() {
            Thread thread = new Thread(this);
            thread.setDaemon(true);
            thread.start();
        }

        public void run() {
            while (true) {
                try {
                    Thread.sleep(DELAY);
                }
                catch (InterruptedException e) {
                    break;
                }
                try {
                    saveConfiguration();
                }
                catch (IOException e) {
                    log.error("Unable to save", e);
                }
            }
        }
    }
}
