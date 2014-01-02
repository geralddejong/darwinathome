package org.darwinathome.client;

import org.apache.log4j.Logger;
import org.darwinathome.Constants;
import org.darwinathome.body.Being;
import org.darwinathome.body.Target;
import org.darwinathome.genetics.Genome;
import org.darwinathome.geometry.math.Arrow;
import org.darwinathome.network.CargoCatcher;
import org.darwinathome.network.Exchange;
import org.darwinathome.network.Failure;
import org.darwinathome.network.Hub;
import org.darwinathome.universe.SpeechChange;
import org.darwinathome.universe.World;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Pretend to be a hub
 *
 * @author Gerald de Jong <geralddejong@gmail.com>
 */

public class MockHub extends JFrame implements Hub {
    private static final File WORLD_FILE = new File("Mock.world");
    private static final Dimension SIZE = new Dimension(600, 60);
    private Logger log = Logger.getLogger(getClass());
    private JLabel ageLabel = new JLabel("Age", JLabel.CENTER);
    private String email;
    private World world;
    private byte[] frozenWorld;
    private long frozenAge;
    private Executor executor = Executors.newSingleThreadExecutor();

    public MockHub() throws IOException {
        getContentPane().setLayout(new GridLayout(1, 0, 5, 5));
        getContentPane().add(ageLabel);
        addButton("10min", Constants.ITERATIONS_PER_HOUR / 6);
        addButton("1hr", Constants.ITERATIONS_PER_HOUR);
        addButton("6hrs", Constants.ITERATIONS_PER_HOUR * 6);
        setSize(SIZE);
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation((screen.width - SIZE.width)/2, screen.height - SIZE.height - 30);
        if (WORLD_FILE.exists()) {
            DataInputStream dis = new DataInputStream(new FileInputStream(WORLD_FILE));
            world = World.read(dis);
            dis.close();
        }
        else {
            world = World.create();
            for (int walk = 0; walk < Constants.ITERATIONS_PER_HOUR * 24 / Constants.ITERATIONS_PER_PATCH_LIFE; walk++) {
                this.world.experienceTime((int) Constants.ITERATIONS_PER_PATCH_LIFE);
            }
        }
        new AgeSetter().run();
        saveTheWorld();
    }

    private void addButton(String text, long time) {
        JButton button = new JButton(text);
        button.addActionListener(new TimeAdvancer((int) time));
        getContentPane().add(button);
    }

    @Override
    public void authenticate(String email, String password, final Exchange exchange) {
        this.email = email;
        delayedSuccess(exchange);
    }

    @Override
    public void getWorld(final CargoCatcher catcher, final Exchange exchange) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                DataInputStream dis = new DataInputStream(new ByteArrayInputStream(getFrozenWorld()));
                try {
                    catcher.catchCargo(dis);
                    delayedSuccess(exchange);
                }
                catch (IOException e) {
                    log.error("Problem sending world", e);
                    exchange.fail(Failure.NETWORK_CONNECTON);
                }
            }
        });
    }

    @Override
    public void createBeing(final Arrow location, final Exchange exchange) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                world.createBeing(location, new Arrow().random(), MockHub.this.email);
                frozenWorld = null;
                delayedSuccess(exchange);
                saveTheWorld();
            }
        });
    }

    @Override
    public void setSpeech(final String speech, final Exchange exchange) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                world.getBeing(email).setSpeech(speech);
                frozenWorld = null;
                delayedSuccess(exchange);
                saveTheWorld();
            }
        });
    }

    @Override
    public void getSpeechSince(long time, final CargoCatcher catcher, final Exchange exchange) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    List<SpeechChange> changes = new ArrayList<SpeechChange>();
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    DataOutputStream dos = new DataOutputStream(baos);
                    SpeechChange.write(dos, changes);
                    dos.close();
                    DataInputStream dis = new DataInputStream(new ByteArrayInputStream(baos.toByteArray()));
                    catcher.catchCargo(dis);
                    delayedSuccess(exchange);
                }
                catch (IOException e) {
                    log.error("Problem sending speech changes", e);
                    exchange.fail(Failure.NETWORK_CONNECTON);
                }
            }
        });
    }

    @Override
    public void setGenome(final Genome genome, final Exchange exchange) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                Being being = world.getBeing(email);
                being.setGenome(genome);
                frozenWorld = null;
                delayedSuccess(exchange);
                log.info("Genome set, now saving");
                saveTheWorld();
            }
        });
    }

    @Override
    public void setTarget(final Target target, final CargoCatcher catcher, final Exchange exchange) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                world.getBeing(email).setTarget(target);
                frozenWorld = null;
                DataInputStream dis = new DataInputStream(new ByteArrayInputStream(getFrozenWorld()));
                try {
                    catcher.catchCargo(dis);
                    delayedSuccess(exchange);
                }
                catch (IOException e) {
                    log.error("Problem sending world", e);
                    exchange.fail(Failure.NETWORK_CONNECTON);
                }
                log.info("Target set, now saving");
                saveTheWorld();
            }
        });
    }

    @Override
    public String getBaseUrl() {
        return "no url";
    }

    private byte[] getFrozenWorld() {
        if (frozenWorld == null || frozenAge < world.getAge()) {
            try {
                log.info("Freezing...");
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                world.write(new DataOutputStream(outputStream));
                frozenWorld = outputStream.toByteArray();
                frozenAge = world.getAge();
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return frozenWorld;
    }

    private void saveTheWorld() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    byte[] frozen = getFrozenWorld();
                    FileOutputStream fos = new FileOutputStream(WORLD_FILE);
                    fos.write(frozen);
                    fos.close();
                    log.info("Saved " + world.getBeings().size() + " beings to " + WORLD_FILE.getAbsolutePath());
                }
                catch (Exception e) {
                    log.error("could not save", e);
                }
            }
        });
    }

    private class AgeSetter implements Runnable {
        private static final String prompt = "Age: ";
        private static final String DY = "dy ";
        private static final String HR = "hr ";
        private static final String MIN = "min ";
        private static final String SEC = "sec";

        @Override
        public void run() {
            setAge(world.getAge());
        }

        public void setAge(long seconds) {
            if (seconds <= 60) {
                setAgeString(prompt + seconds + SEC);
            }
            else {
                long minutes = seconds / 60;
                if (minutes <= 60) {
                    seconds %= 60;
                    if (seconds < 10) {
                        setAgeString(prompt + minutes + MIN + "0" + seconds + SEC);
                    }
                    else {
                        setAgeString(prompt + minutes + MIN + seconds + SEC);
                    }
                }
                else {
                    long hours = minutes / 60;
                    minutes %= 60;
                    if (hours <= 24) {
                        setAgeString(prompt + hours + HR + minutes + MIN);
                    }
                    else {
                        long days = hours / 24;
                        minutes %= 60;
                        hours %= 24;
                        setAgeString(prompt + days + DY + hours + HR + minutes + MIN);
                    }
                }
            }
        }

        private void setAgeString(String string) {
            ageLabel.setText(string);
        }
    }

    private class TimeAdvancer implements ActionListener {
        private int iterations;

        private TimeAdvancer(int iterations) {
            this.iterations = iterations;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    frozenWorld = null;
                    for (int walk = 0; walk < iterations; walk++) {
                        world.experienceTime(1);
                    }
                    saveTheWorld();
                    SwingUtilities.invokeLater(new AgeSetter());
                }
            });
        }
    }

    private void delayedSuccess(final Exchange exchange) {
        new Timer(true).schedule(new TimerTask() {
            @Override
            public void run() {
                exchange.success();
            }
        }, 500);
    }
}
