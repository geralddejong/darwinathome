// ========= Copyright (C) 2009, 2010 Gerald de Jong =================
// This file is part of the Darwin at Home project, distributed
// under the GNU General Public License, version 3.
// You should have received a copy of this license in "license.txt",
// but if not, see http://www.gnu.org/licenses/gpl-3.0.txt.
// ===================================================================
package org.darwinathome.client;

import org.apache.log4j.Logger;
import org.darwinathome.Constants;
import org.darwinathome.body.Being;
import org.darwinathome.body.Speech;
import org.darwinathome.body.Target;
import org.darwinathome.genetics.Genome;
import org.darwinathome.geometry.jogl.PointOfView;
import org.darwinathome.geometry.math.Arrow;
import org.darwinathome.network.CargoCatcher;
import org.darwinathome.network.Exchange;
import org.darwinathome.network.Failure;
import org.darwinathome.network.Hub;
import org.darwinathome.network.Spoke;
import org.darwinathome.universe.MultiverseEvolution;
import org.darwinathome.universe.SpeechChange;
import org.darwinathome.universe.World;

import javax.swing.SwingUtilities;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * The world on the client side
 *
 * @author Gerald de Jong, Beautiful Code BV, <geralddejong@gmail.com>
 */

public class SpokeImpl implements Spoke {
    private static final long SPEECH_FETCH_INTERVAL = 30000;
    private static final long TARGET_SET_PATIENCE = 1000;
    private static final long TARGET_SET_TICK = 300;
    private Logger log = Logger.getLogger(getClass());
    private Queue<Runnable> jobs = new ConcurrentLinkedQueue<Runnable>();
    private SpeechFetcher speechFetcher = new SpeechFetcher();
    private TargetSetter targetSetter = new TargetSetter();
    private MultiverseEvolutionImpl evolution;
    private Notifier notifier;
    private boolean fetchingWorld;
    private PointOfView pointOfView;
    private Target target;
    private Hub hub;
    private Core core;

    public SpokeImpl(Hub hub, PointOfView pointOfView, Core core) {
        this.hub = hub;
        this.pointOfView = pointOfView;
        this.core = core;
    }

    public void setNotifier(Notifier notifier) {
        this.notifier = notifier;
    }

    public void authenticate(final String email, String password, final Exchange exchange) {
        notifier.notify("Authenticating...", 10000);
        hub.authenticate(email, password, new Exchange() {
            public void success() {
                core.setEmail(email);
                speechFetcher.start();
                targetSetter.start();
                notifier.clearNotify();
                exchange.success();
            }

            public void fail(Failure failure) {
                notifier.notify("Access Denied.  Please log in and create a new tetragotchi.jnlp", 5000);
                exchange.fail(Failure.AUTHENTICATION);
            }
        });
    }

    public void initiate(final Runnable whenReady) {
        fetchWorld(new Runnable() {
            @Override
            public void run() {
                whenReady.run();
            }
        });
    }

    public void nextState(final boolean happy) {
        jobs.add(new Runnable() {
            public void run() {
                switch (core.getSpokeState()) {
                    case FROZEN:
                        if (!happy) {
                            if (core.isCreationMode()) {
                                jobs.add(new Runnable() {
                                    public void run() {
                                        log.info("Creating being..");
                                        notifier.notify("Creating being...", 2000);
                                        Arrow location = new Arrow(pointOfView.getFocus());
                                        if (location.quadrance() < 0.001) {
                                            location.add(pointOfView.getEye());
                                        }
                                        hub.createBeing(location, new Exchange() {
                                            public void success() {
                                                fetchWorld(new Runnable() {
                                                    public void run() {
                                                        log.info("Being created");
                                                    }
                                                });
                                            }

                                            public void fail(Failure failure) {
                                                notifier.fail(failure);
                                            }
                                        });
                                    }
                                });
                            }
                            else {
                                reboot();
                            }
                        }
                        else {
                            core.setSpokeState(Core.SpokeState.TIME_ON);
                        }
                        break;
                    case TIME_ON:
                        final Being being = core.getBeing();
                        if (being == null || !happy || !core.isBeingEvolvable()) {
                            core.restoreWorld(getTarget());
                        }
                        else {
                            core.setSpokeState(Core.SpokeState.EVOLVING);
                        }
                        break;
                    case EVOLVING:
                        Genome genome = getEvolution().terminate();
                        if (happy) {
                            core.setSpokeState(Core.SpokeState.SAVE_GENOME);
                            core.getBeing().setGenome(genome);
                            log.info("Sending\n" + genome);
                            notifier.notify("Saving Genome", 5000);
                            hub.setGenome(genome, new Exchange() {
                                public void success() {
                                    fetchWorld(new Runnable() {
                                        public void run() {
                                            log.info("Sent genome, fetching new world");
                                        }
                                    });
                                }

                                public void fail(Failure failure) {
                                    notifier.fail(failure);
                                }
                            });
                        }
                        else {
                            core.restoreWorld(getTarget());
                        }
                        break;
                    case SAVE_GENOME:
                        break;
                }
            }
        });
    }

    public void reboot() {
        fetchWorld(null);
    }

    public Speech getSpeech() {
        Being being = core.getBeing();
        if (being != null) {
            return being.getSpeech();
        }
        else {
            return new Speech("?");
        }
    }

    public void setSpeech(final String speech) {
        final Being being = core.getBeing();
        if (being != null) {
            hub.setSpeech(speech, new Exchange() {
                public void success() {
                    being.setSpeech(speech);
                }

                public void fail(Failure failure) {
                    notifier.fail(failure);
                }
            });
        }
    }

    public void setTarget(Target target) {
        Being being = core.getBeing();
        if (being != null && !being.isTargetSame(target)) {
            Arrow toGoal = new Arrow().set(being.getGeometry().getBodyCenter());
            toGoal.setSpan(Constants.ROAM_RADIUS);
            toGoal.sub(target.getLocation()).scale(-1);
            double span = toGoal.span();
            if (span > 1e-6) {
                if (span < Constants.MIN_GOAL_DISTANCE + 1) {
                    toGoal.scale((Constants.MIN_GOAL_DISTANCE + 1) / span);
                    target.getLocation().set(being.getGeometry().getBodyCenter()).add(toGoal);
                    target.getLocation().setSpan(Constants.ROAM_RADIUS);
                }
            }
            else {
                return;
            }
            targetSetter.setTarget(target);
        }
        this.target = target;
    }

    public Target getTarget() {
        if (target == null && core.getBeing() != null) {
            target = new Target(core.getBeing().getGoal(), core.getBeing().getPreyName());
        }
        return target;
    }

    public MultiverseEvolution getEvolution() {
        if (evolution != null && evolution.isTerminated()) {
            evolution = null;
        }
        final Being being = core.getBeing();
        if (being != null) {
            if (evolution == null) {
                evolution = new MultiverseEvolutionImpl(being, core);
                core.startEvolution();
            }
        }
        return evolution;
    }

    public void runPendingJobs() {
        while (!jobs.isEmpty()) {
            jobs.remove().run();
        }
    }

    private class TargetSetter implements Runnable, Exchange {

        private Target target;
        private long triggerTime;

        public void start() {
            Thread thread = new Thread(this);
            thread.setDaemon(true);
            thread.start();
        }

        public void setTarget(Target target) {
            this.target = target;
            this.triggerTime = System.currentTimeMillis() + TARGET_SET_PATIENCE;
        }

        public void run() {
            while (true) {
                try {
                    Thread.sleep(TARGET_SET_TICK);
                }
                catch (InterruptedException e) {
                    break;
                }
                if (triggerTime > 0 && triggerTime < System.currentTimeMillis()) {
                    triggerTime = 0;
                    notifier.notify("Setting target, fetching world...", 5000);
                    hub.setTarget(target, getWorldCatcher(), this);
                }
            }
        }

        public void success() {
            notifier.clearNotify();
        }

        public void fail(Failure failure) {
            notifier.fail(failure);
        }
    }

    private class SpeechFetcher implements Runnable, CargoCatcher {
        private long time = 0;
        private List<SpeechChange> changes;
        private boolean fetching;

        public void start() {
            Thread thread = new Thread(this);
            thread.setDaemon(true);
            thread.start();
        }

        public void run() {
            while (true) {
                try {
                    Thread.sleep(SPEECH_FETCH_INTERVAL);
                }
                catch (InterruptedException e) {
                    break;
                }
                if (!fetching) {
                    fetching = true;
                    hub.getSpeechSince(time, this, new Exchange() {
                        public void success() {
                            if (changes != null) {
                                SwingUtilities.invokeLater(new Runnable() {
                                    @Override
                                    public void run() {
                                        for (SpeechChange change : changes) { // catchCargo has just caught new ones;
                                            Being being = core.getWorld().getBeing(change.getBodyName());
                                            if (being != null) {
                                                being.setSpeech(change.getSpeech());
                                            }
                                            time = change.getTime();
                                        }
                                        changes = null;
                                    }
                                });
                            }
                            fetching = false;
                        }

                        public void fail(Failure failure) {
                            log.warn(failure.toString());
                            fetching = false;
                        }
                    });
                }
            }
        }

        public void catchCargo(DataInputStream dis) throws IOException {
            changes = SpeechChange.read(dis);
        }
    }

    private void fetchWorld(final Runnable whenReady) {
        if (!fetchingWorld) {
            fetchingWorld = true;
            notifier.notify("Fetching World...", 5000);
            hub.getWorld(getWorldCatcher(), new Exchange() {
                public void success() {
                    notifier.clearNotify();
                    core.setSpokeState(Core.SpokeState.FROZEN);
                    target = null;
                    fetchingWorld = false;
                    if (whenReady != null) {
                        whenReady.run();
                    }
                }

                public void fail(Failure failure) {
                    notifier.fail(failure);
                }
            });
        }
    }

    private CargoCatcher getWorldCatcher() {
        return new CargoCatcher() {
            public void catchCargo(DataInputStream dis) throws IOException {
                World world = World.read(dis);
                core.setWorld(world);
            }
        };
    }
}