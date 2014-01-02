// ========= Copyright (C) 2009, 2010 Gerald de Jong =================
// This file is part of the Darwin at Home project, distributed
// under the GNU General Public License, version 3.
// You should have received a copy of this license in "license.txt",
// but if not, see http://www.gnu.org/licenses/gpl-3.0.txt.
// ===================================================================

package org.darwinathome.server.persistence.impl;

import org.apache.log4j.Logger;
import org.darwinathome.Constants;
import org.darwinathome.body.Being;
import org.darwinathome.body.Embryo;
import org.darwinathome.body.Target;
import org.darwinathome.genetics.Genome;
import org.darwinathome.genetics.Noise;
import org.darwinathome.geometry.math.Arrow;
import org.darwinathome.server.email.NotificationSender;
import org.darwinathome.server.persistence.Home;
import org.darwinathome.server.persistence.WorldHistory;
import org.darwinathome.universe.SpeechChange;
import org.darwinathome.universe.World;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * This class runs the universe by ticking it and saving every so often
 *
 * @author Gerald de Jong, Beautiful Code BV, <geralddejong@gmail.com>
 */

public class WorldHistoryImpl implements WorldHistory {
    private static final DecimalFormat HOUR_FORMATTER = new DecimalFormat("00");
    private static final DecimalFormat DAY_FORMATTER = new DecimalFormat("000");
    private static final DecimalFormat WORLD_NUMBER_FORMATTER = new DecimalFormat("000000000");
    private static final String EXTENSION = ".world";
    private Logger log = Logger.getLogger(getClass());
    private Frozen frozen;
    private Time time = new Time();
    private final Queue<SpeechChange> speechChanges = new ConcurrentLinkedQueue<SpeechChange>();

    @Autowired
    private Home home;

    @Autowired
    private NotificationSender notificationSender;

    public WorldHistoryImpl() {
        time.start();
    }

    public synchronized Frozen getFrozenWorld() {
        if (frozen == null || (frozen.getAge() < world().getAge())) {
            try {
                log.info("Freezing...");
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                for (Being being : world().getBeings()) {
                    being.getBody().executeTransformations(null);
                }
                world().write(new DataOutputStream(outputStream));
                frozen = new Frozen(outputStream.toByteArray(), world().getAge());
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return frozen;
    }

    public synchronized void createBeing(String bodyName, Arrow location, Arrow gaze) {
        world().createBeing(location, gaze, bodyName);
        frozen = null;
    }

    public synchronized void setSpeech(String bodyName, String speech) {
        world().getBeing(bodyName).setSpeech(speech);
        speechChanges.add(new SpeechChange(world().getAge(), bodyName, speech));
    }

    public synchronized List<SpeechChange> getSpeechSince(String bodyName, long time) {
        List<SpeechChange> changes = new ArrayList<SpeechChange>();
        Iterator<SpeechChange> walk = speechChanges.iterator();
        while (walk.hasNext()) {
            SpeechChange speechChange = walk.next();
            if (world().getAge() - speechChange.getTime() > Constants.SPEECH_TIME_TO_LIVE) {
                walk.remove();
            }
            else if (speechChange.getTime() > time && !speechChange.getBodyName().equals(bodyName)) {
                changes.add(speechChange);
            }
        }
        return changes;
    }

    public synchronized void setGenome(String bodyName, Genome genome) {
        Being being = world().getBeing(bodyName);
        if (being != null) {
            if (being.getShield() == null) {
                being.setGenome(genome);
            }
            else { // still premature.. make a new one
                log.info("Set genome inside premature tetragotchi causing rebirth");
                String id = being.getId();
                String email = being.getEmail();
                being = world().createBeing(
                        new Embryo(id, email, being.getSpeech(), genome, null),
                        being.getGeometry().getBodyCenter(),
                        being.getGeometry().getForward()
                );
                being.setTarget(new Target(being.getGoal(), being.getPreyName()));
            }
        }
        else {
            log.warn("Set genome of "+bodyName+" failed because the being couldn't be found");
        }
    }

    public synchronized Frozen setTarget(String bodyName, Target target) {
        world().getBeing(bodyName).setTarget(target);
        frozen = null;
        return getFrozenWorld();
    }

    @Override
    public boolean beingExists(String bodyName) {
        return world().getBeing(bodyName) != null;
    }

    public Noise getNoise() {
        return world().getNoise();
    }

    @Override
    public void advanceMinutes(int minutes) {
        time.minutesAdvance = minutes;
    }

    // must detect that this has happened and trigger it, with a message to the spoke
    // world.predatorConsumePrey(predatorName);

    private File fetchLatestFile(File directory) {
        File[] subdirectories = directory.listFiles(new FileFilter() {
            public boolean accept(File file) {
                return file.isDirectory() && file.getName().matches("[DH]\\d{2,3}");
            }
        });
        if (subdirectories == null || subdirectories.length == 0) {
            File[] worldFiles = directory.listFiles(new FileFilter() {
                public boolean accept(File file) {
                    return !file.isDirectory() && file.getName().endsWith(EXTENSION);
                }
            });
            if (worldFiles == null || worldFiles.length == 0) {
                return null;
            }
            else {
                Arrays.sort(worldFiles, new FileNameComparator());
                return worldFiles[worldFiles.length - 1];
            }
        }
        else {
            Arrays.sort(subdirectories, new FileNameComparator());
            return fetchLatestFile(subdirectories[subdirectories.length - 1]);
        }
    }

    private static class FileNameComparator implements Comparator<File> {
        public int compare(File fileA, File fileB) {
            return fileA.getName().compareTo(fileB.getName());
        }
    }

    private void saveTheWorld() {
        try {
            Frozen frozen = getFrozenWorld();
            File directory = new File(home.getDirectory(), pathFormat(frozen.getAge()));
            //noinspection ResultOfMethodCallIgnored
            directory.mkdirs();
            File worldFile = new File(directory, fileNameFormat(frozen.getAge()));
            FileOutputStream fos = new FileOutputStream(worldFile);
            fos.write(frozen.getWorld());
            fos.close();
            log.info("Saved " + world().getBeings().size() + " beings to " + worldFile.getAbsolutePath());
        }
        catch (Exception e) {
            log.error("could not save", e);
        }
    }

    private String pathFormat(long age) {
        int hour = (int) (age / (60 * 60) % 24);
        int day = (int) (age / (60 * 60 * 24));
        return "D" + DAY_FORMATTER.format(day) + File.separator + "H" + HOUR_FORMATTER.format(hour);
    }

    private String fileNameFormat(long age) {
        return WORLD_NUMBER_FORMATTER.format(age) + EXTENSION;
    }

    private World world() {
        if (time.world == null) {
            throw new RuntimeException("No world yet!");
        }
        return time.world;
    }

    private class Time implements Runnable {
        private World world;
        private int minutesAdvance;

        public void start() {
            Thread thread = new Thread(this);
            thread.setDaemon(true);
            thread.start();
        }

        public void run() {
            while (true) {
                try {
                    Thread.sleep(Constants.MILLIS_PER_ITERATION);
                }
                catch (InterruptedException e) {
                    break;
                }
                if (world == null) {
                    try {
                        File latestFile = fetchLatestFile(home.getDirectory());
                        if (latestFile == null) {
                            world = World.create();
                            world.addListener(notificationSender);
                            log.info("Created world");
                            for (int walk = 0; walk < Constants.ITERATIONS_PER_HOUR * 24 / Constants.ITERATIONS_PER_PATCH_LIFE; walk++) {
                                this.world.experienceTime((int) Constants.ITERATIONS_PER_PATCH_LIFE);
                            }
                        }
                        else {
                            DataInputStream dis = new DataInputStream(new FileInputStream(latestFile));
                            world = World.read(dis);
                            world.addListener(notificationSender);
                            log.info("Loaded " + world.getBeings().size() + " beings from " + latestFile.getAbsolutePath());
                            dis.close();
                        }
                    }
                    catch (IOException e) {
                        throw new RuntimeException("Unable to read latest world");
                    }
                }
                if (minutesAdvance > 0) {
                    for (int walk = 0; walk < 60; walk++) {
                        iterate();
                    }
                    minutesAdvance--;
                }
                else {
                    iterate();
                }
            }
        }

        private synchronized void iterate() {
            try {
                world.experienceTime(1);
                if (world.getAge() % Constants.ITERATIONS_PER_SAVE == 0) {
                    saveTheWorld();
                }
            }
            catch (Exception e) {
                log.error("## Problem Iterating !!", e);
            }
        }

    }
}