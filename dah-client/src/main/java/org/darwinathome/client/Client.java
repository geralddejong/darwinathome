// ========= Copyright (C) 2009, 2010 Gerald de Jong =================
// This file is part of the Darwin at Home project, distributed
// under the GNU General Public License, version 3.
// You should have received a copy of this license in "license.txt",
// but if not, see http://www.gnu.org/licenses/gpl-3.0.txt.
// ===================================================================
package org.darwinathome.client;

import com.sun.opengl.util.FPSAnimator;
import org.darwinathome.Constants;
import org.darwinathome.body.Being;
import org.darwinathome.body.Target;
import org.darwinathome.geometry.jogl.GLRenderer;
import org.darwinathome.geometry.jogl.GLViewPlatform;
import org.darwinathome.geometry.jogl.HeadsUp;
import org.darwinathome.geometry.jogl.Mouse3d;
import org.darwinathome.geometry.jogl.PointOfView;
import org.darwinathome.geometry.math.Arrow;
import org.darwinathome.geometry.structure.Span;
import org.darwinathome.network.Exchange;
import org.darwinathome.network.Failure;
import org.darwinathome.network.Hub;
import org.darwinathome.network.Spoke;
import org.darwinathome.universe.MultiverseEvolution;
import org.darwinathome.universe.World;

import javax.media.opengl.GL;
import javax.media.opengl.GLCanvas;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Toolkit;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;

/**
 * @author Gerald de Jong <geralddejong@gmail.com>
 */

public class Client extends JFrame {
    private static final int LIFE_ITERATIONS = 10;
    private static final int MOVIE_ITERATIONS = 40;
    private static final int EVOLVE_ITERATIONS = 30;
    private Core core = new Core();
    private String bodyName, password;
    private Spoke spoke;
    private Ship ship;
    private PointOfView pointOfView = new PointOfView(7);
    private View view;
    private SpeechField speechField;
    private FPSAnimator fpsAnimator;

    public Client(Hub hub, String bodyName, String password) {
        this.bodyName = bodyName;
        this.password = password;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.spoke = new SpokeImpl(hub, pointOfView, core);
        setTitle(bodyName);
        ship = new Ship(pointOfView, core);
        this.view = new View(null);
        ship.attachTo(view.canvas);
        speechField = new SpeechField(spoke);
        getContentPane().add(speechField.getPanel(), BorderLayout.SOUTH);
        getContentPane().add(view.canvas, BorderLayout.CENTER);
        getContentPane().add(new Dashboard(core), BorderLayout.EAST);
        view.canvas.requestFocus();
    }

    public void kill() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if (fpsAnimator != null) {
                    fpsAnimator.stop();
                }
                Thread.currentThread().interrupt();
            }
        });
    }

    private static class StressRange implements Span.StressRange {
        public double minimum() {
            return -0.003;
        }

        public double maximum() {
            return 0.003;
        }
    }

    private void authenticate() {
        spoke.authenticate(bodyName, password, new Exchange() {
            public void success() {
                spoke.initiate(new Runnable() {
                    public void run() {
                        ship.launch();
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                speechField.refresh();
                                fpsAnimator = new FPSAnimator(view.canvas, 60, true);
                                fpsAnimator.start();
                            }
                        });
                    }
                });
            }

            public void fail(Failure failure) {
                view.canvas.repaint();
//                kill();
            }
        });

    }

    private class View implements GLRenderer {
        private GLCanvas canvas = new GLCanvas();
        private GLViewPlatform viewPlatform = new GLViewPlatform(this, pointOfView, 1, Constants.FRUSTUM_FAR);
        private InteractionFacts interactionFacts = new InteractionFacts(viewPlatform.getHeadsUp());
        private Mouse3d mouse3d = new Mouse3d(viewPlatform);
        private WorldPainter worldPainter;
        private HeadsUpNotifier headsUpNotifier = new HeadsUpNotifier();
        private BeingPainter beingPainter = new BeingPainter(new StressRange(), pointOfView, core);
        private MultiverseEvolution.Competitor bestCompetitor;
        private long lastInteraction = System.currentTimeMillis();

        private View(Boolean isRightEye) {
            spoke.setNotifier(headsUpNotifier);
            viewPlatform.getHeadsUp().set(HeadsUp.Pos.MIDDLE, headsUpNotifier);
            core.addTimeListener(new BeingFacts(viewPlatform.getHeadsUp()));
            core.addTimeListener(new WorldFacts(viewPlatform.getHeadsUp()));
            core.addTimeListener(new Core.TimeListener() {
                @Override
                public void snapshot(World world) {
                }

                @Override
                public void beingSet(Being being) {
                }

                @Override
                public void timeIs(long frozenTime, long frozenWorldAge, long currentWorldAge, Being being) {
                    if (currentWorldAge > frozenWorldAge + Constants.ITERATIONS_PER_HOUR * 24) {
                        spoke.nextState(false);
                    }
                }
            });
            core.addEvolutionListener(new EvolutionFacts(viewPlatform.getHeadsUp()));
            worldPainter = new WorldPainter();
            viewPlatform.setRightEye(isRightEye);
            core.addStateListener(interactionFacts);
            mouse3d.attachTo(canvas);
            canvas.setFocusable(true);
            canvas.requestFocus();
            canvas.addGLEventListener(viewPlatform);
            canvas.addMouseMotionListener(new FocusClaimer(canvas));
            canvas.addKeyListener(new KeyHandler());
        }

        public void init(GL gl) {
            authenticate();
        }

        public void display(final GL gl, int width, int height) {
            if (!core.isReady()) {
                return;
            }
            try {
                spoke.runPendingJobs();
                worldPainter.display(gl, core.getSurfacePatches(), pointOfView);
                Being me = core.getBeing();
                if (me != null) {
                    me.getGeometry().refresh();
                    me.getGeometry().handlePrey();
                    Being prey = me.getGeometry().getPrey();
                    if (prey != null) {
                        prey.getGeometry().refresh();
                    }
                }
                switch (core.getSpokeState()) {
                    case TIME_ON:
                        core.experienceTime(viewPlatform.isRecordingMovie() ? MOVIE_ITERATIONS : LIFE_ITERATIONS);
                        lastInteraction = System.currentTimeMillis();
                        mouse3d.clearEvents();
                    case FROZEN:
                        long lastActivity = Math.max(lastInteraction, ship.getLastGesture());
                        if (System.currentTimeMillis() - lastActivity > Constants.IDLE_MILLIS &&
                                Constants.Calc.isBelowOrbit(pointOfView.getEye().span())) {
                            core.setSpokeState(Core.SpokeState.TIME_ON);
                            spoke.nextState(true); // spontaneously start evolving
                        }
                    case SAVE_GENOME:
                        double altitude = pointOfView.getEye().span();
                        if (Constants.Calc.isAboveHover(altitude)) {
                            if (core.isOlderThanFrozen()) {
                                spoke.reboot();
                            }
                            beingPainter.paintBeingsAsLines(gl);
                            Being being = selectBeingFromOrbit(gl);
                            if (being != null) {
                                core.setBeing(being);
                            }
                        }
                        else {
                            for (Being being : core.getBeings()) {
                                switch (being.getPhase()) {
                                    case DYING:
                                    case KILLED:
                                        beingPainter.paintGhost(gl, being, false);
                                        break;
                                    default:
                                        beingPainter.paintBeing(gl, being, spoke.getTarget());
                                        break;
                                }
                            }
                            if (Constants.Calc.isHovering(altitude) && !core.isOlderThanFrozen()) {
                                Target target = createTargetFromHovering(gl);
                                if (target != null) {
                                    spoke.setTarget(target);
                                }
                            }
                            else { // down on the surface
                                mouse3d.clearEvents();
                            }
                        }
                        break;
                    case EVOLVING:
                        MultiverseEvolution evolution = spoke.getEvolution();
                        if (!evolution.experienceTime(EVOLVE_ITERATIONS)) {
                            bestCompetitor = evolution.cull();
                            if (bestCompetitor.getTravelToGoal() < evolution.getDistanceToGoal() / 2 || evolution.advanceLifespan()) {
                                spoke.nextState(true);
                            }
                        }
                        lastInteraction = System.currentTimeMillis();
                        for (MultiverseEvolution.Competitor competitor : evolution.getCompetitors()) {
                            beingPainter.paintGhost(gl, competitor.getBeing(), bestCompetitor == competitor);
                        }
                        beingPainter.paintBeing(gl, me, null);
                        mouse3d.clearEvents();
                        break;
                }
                ship.run();
            }
            catch (RuntimeException e) {
                e.printStackTrace();
                kill();
                throw e;
            }
        }

        private Target createTargetFromHovering(GL gl) {
            Mouse3d.Event event = mouse3d.getEvent(gl);
            if (event != null && core.getBeing() != null && core.mouseActivate()) {
                lastInteraction = System.currentTimeMillis();
                Arrow surfaceSpot = event.getIntersection(Constants.ROAM_RADIUS);
                if (surfaceSpot != null) {
                    Being nearestBeing = core.getNearestBeing(surfaceSpot);
                    if (nearestBeing == null) {
                        return new Target(surfaceSpot);
                    }
                    else {
                        core.getBeing().getGeometry().refresh();
                        nearestBeing.getGeometry().refresh();
                        Arrow centerSurface = new Arrow(nearestBeing.getGeometry().getBodyCenter()).setSpan(Constants.SURFACE_RADIUS);
                        double distance = centerSurface.distanceTo(surfaceSpot);
                        if (distance < nearestBeing.getGeometry().getBodyRadius() * 2 && core.getBeing().mayHunt(nearestBeing)) {
                            return new Target(surfaceSpot, nearestBeing.toString());
                        }
                        else {
                            return new Target(surfaceSpot);
                        }
                    }
                }
            }
            return null;
        }

        private Being selectBeingFromOrbit(GL gl) {
            Mouse3d.Event event = mouse3d.getEvent(gl);
            if (event != null && core.mouseActivate()) {
                Arrow surfaceSpot = event.getIntersection(Constants.HOVER_TOP);
                if (surfaceSpot != null) {
                    Being nearestBeing = core.getNearestBeing(surfaceSpot);
                    if (nearestBeing != null) {
                        return nearestBeing;
//                    ship.focusOn(nearestBeing);
                    }
                }
            }
            return null;
        }

        private class KeyHandler extends KeyAdapter {

            @Override
            public void keyPressed(KeyEvent event) {
                switch (event.getKeyCode()) {
                    case KeyEvent.VK_SPACE:
                        if (!Constants.Calc.isAboveHover(pointOfView.getEye().span())) {
                            if (!core.hasBeing() || core.isBeingControllable() || core.getSpokeState() == Core.SpokeState.FROZEN && !event.isShiftDown()) {
                                spoke.nextState(!event.isShiftDown());
                            }
                            else {
                                ship.backOff();
                            }
                        }
                        break;
//                    case KeyEvent.VK_I:
//                        fpsAnimator.stop();
//                        ship.setKeyStep(0.00001);
//                        fpsAnimator = new FPSAnimator(view.canvas, 5, true);
//                        fpsAnimator.start();
//                        viewPlatform.recordMovie(new File("/tmp"), "Tetragotchi", 1);
//                        break;
//                    case KeyEvent.VK_O:
//                        fpsAnimator.stop();
//                        fpsAnimator = new FPSAnimator(view.canvas, 60, true);
//                        fpsAnimator.start();
//                        viewPlatform.stopMovie();
//                        break;
                    case KeyEvent.VK_R:
                        spoke.getEvolution().randomizeMovementGenes();
                        break;
                    case KeyEvent.VK_S:
                        spoke.getEvolution().resetLifespan();
                        break;
                }
            }
        }

    }

    private static class FocusClaimer implements MouseMotionListener {
        private Component component;

        private FocusClaimer(Component component) {
            this.component = component;
        }

        private void claimFocus() {
            if (!component.hasFocus()) {
                component.requestFocus();
            }
        }

        public void mouseDragged(MouseEvent mouseEvent) {
            claimFocus();
        }

        public void mouseMoved(MouseEvent mouseEvent) {
            claimFocus();
        }
    }

    private class HeadsUpNotifier implements HeadsUp.Line, Spoke.Notifier {
        private long disappearTime;
        private String text;
        private String nextText;

        public boolean hasChanged() {
            if (System.currentTimeMillis() > disappearTime) {
                clearNotify();
            }
            return nextText != null;
        }

        public String getText() {
            if (nextText != null) {
                text = nextText;
                nextText = null;
            }
            return text;
        }

        public void notify(String notification, int duration) {
            nextText = notification;
            if (duration == 0) {
                disappearTime = 0;
            }
            else {
                disappearTime = System.currentTimeMillis() + duration;
            }
        }

        public void clearNotify() {
            notify("", 0);
        }

        public void fail(Failure fail) {
            notify("System problem:" + fail.toString(), 2000);
        }
    }

    private void go() {
        setVisible(true);
    }

    public static void main(final String[] args) throws Exception {
        final Dimension size = Toolkit.getDefaultToolkit().getScreenSize();
        if (args.length != 3) {
            if (args[0].contains("@")) {
                final MockHub mockHub = new MockHub();
                System.out.println("Mock Hub");
                EventQueue.invokeLater(new Runnable() {
                    public void run() {
                        mockHub.setVisible(true);
                        Client client = new Client(mockHub, args[0], "");
                        client.setSize(size);
                        client.go();
                    }
                });
            }
            else {
                throw new Exception("Expected arguments <server-url> <body-name> <password>");
            }
        }
        else {
            final String serverUrl = args[0];
            final String bodyName = args[1];
            final String password = args[2];
            if (args[0].startsWith("http://")) {
                System.out.println("Server '" + serverUrl + "'");
                System.out.println("Body '" + bodyName + "'");
                EventQueue.invokeLater(new Runnable() {
                    public void run() {
                        Client client = new Client(new HttpHub(serverUrl), bodyName, password);
                        client.setSize(size);
                        client.go();
                    }
                });
            }
            else {
                throw new RuntimeException("Argument not understood");
            }
        }
    }
}