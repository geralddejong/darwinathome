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
import org.darwinathome.body.Geometry;
import org.darwinathome.geometry.jogl.PointOfView;
import org.darwinathome.geometry.math.Arrow;
import org.darwinathome.geometry.math.Space3;
import org.darwinathome.geometry.navi.DirectionKeys;
import org.darwinathome.universe.World;

import java.awt.Component;
import java.awt.event.KeyEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

/**
 * @author Gerald de Jong <geralddejong@gmail.com>
 */

public class Ship {
    private static final long MAX_INTERACTIVE_MILLIS = 100;
    private Logger log = Logger.getLogger(getClass());
    private KeyHandler keyHandler = new KeyHandler();
    private WheelHandler wheelHandler = new WheelHandler();
    private PointOfView pov;
    private Arrow targetFocus = new Arrow();
    private Level level;
    private Core core;
    private Boolean forward;
    private long lastInteractive, lastGesture;

    public Ship(PointOfView pov, final Core core) {
        this.pov = pov;
        this.core = core;
        core.addTimeListener(new Core.TimeListener() {
            @Override
            public void snapshot(World world) {
            }

            @Override
            public void beingSet(Being being) {
                if (being != null) {
                    forward = Boolean.TRUE;
                }
                else switch (core.getShipLevel()) {
                    case HOVER:
                    case ROAM:
                    case WATCH:
                        forward = Boolean.FALSE;
                }
            }

            @Override
            public void timeIs(long frozenTime, long frozenWorldAge, long currentWorldAge, Being being) {
            }
        });
        this.level = space;
    }

    public void setKeyStep(double step) {
        keyHandler.setStep(step);
    }

    public void attachTo(Component component) {
        component.addKeyListener(keyHandler);
        component.addMouseWheelListener(wheelHandler);
    }

    public void launch() {
        level.prepareLower();
        core.setShipState(Core.ShipState.FALLING);
    }

    private Arrow center() {
        if (core.hasBeing()) {
            Geometry geometry = core.getBeing().getGeometry();
            geometry.refresh();
            return geometry.getBodyCenter();
        }
        else {
            throw new RuntimeException("No center available!");
        }
    }

    public long getLastGesture() {
        return lastGesture;
    }

    public void backOff() {
        forward = Boolean.FALSE;
    }

    public void run() {
        Level wasLevel = level;
        switch (core.getShipState()) {
            case DORMANT:
                break;
            case APPROACHING:
                if (!level.moveCloser()) {
                    core.setShipState(Core.ShipState.STEADY);
                }
                break;
            case RISING:
                if (!level.moveHigher()) {
                    level = level.next(false);
                    core.setShipState(Core.ShipState.STEADY);
                }
                break;
            case FALLING:
                if (!level.moveLower()) {
                    level = level.next(true);
                    core.setShipState(Core.ShipState.STEADY);
                }
                break;
            case STEADY:
                keyHandler.run();
                Boolean forward = keyHandler.getForward();
                if (forward != null) {
                    if (forward) {
                        if (level.prepareLower()) {
                            log.info(level + " falling");
                            core.setShipState(Core.ShipState.FALLING);
                        }
                        else if (level.prepareCloser()) {
                            log.info(level + " approaching");
                            core.setShipState(Core.ShipState.APPROACHING);
                        }
                        else {
                            log.info(level + " steady");
                        }
                    }
                    else if (level.prepareHigher()) {
                        log.info(level + " rising");
                        core.setShipState(Core.ShipState.RISING);
                    }
                    lastInteractive = 0;
                }
                else if (lastInteractive > 0) {
                    long now = System.currentTimeMillis();
                    long since = now - lastInteractive;
                    if (since > MAX_INTERACTIVE_MILLIS) {
                        since = MAX_INTERACTIVE_MILLIS;
                    }
                    double intensity = ((double) since) / MAX_INTERACTIVE_MILLIS;
                    level.interactive(intensity);
                    lastInteractive = now;
                }
                else {
                    lastInteractive = System.currentTimeMillis();
                }
                break;
        }
        if (wasLevel != level) {
            core.setShipLevel(level.getShipLevel());
            keyHandler.clear();
        }
    }

    private interface Level {

        Core.ShipLevel getShipLevel();

        Level next(boolean lower);

        boolean prepareCloser();

        boolean moveCloser();

        boolean prepareLower();

        boolean moveLower();

        void interactive(double intensity);

        boolean prepareHigher();

        boolean moveHigher();
    }

    private Level space = new Level() {
        private LinearPovTransition transition;
        private Arrow toGoal = new Arrow();

        public Core.ShipLevel getShipLevel() {
            return Core.ShipLevel.SPACE;
        }

        public Level next(boolean lower) {
            if (!lower) {
                return null;
            }
            return core.hasBeing() ? hover : orbit;
        }

        public boolean prepareCloser() {
            return true;
        }

        public boolean moveCloser() {
            return true;
        }

        public boolean prepareLower() {
            SoundEffect.APPROACH.play();
            if (core.hasBeing()) {
                pov.getEye().set(center()).setSpan(Constants.REMOTE_RADIUS);
                toGoal.sub(core.getBeing().getGoal(), center());
                double distance = toGoal.span();
                if (distance > 0.1) {
                    toGoal.scale(1 / distance);
                    pov.getUp().set(toGoal);
                }
                else {
                    pov.getUp().random();
                }
            }
            else {
                pov.getEye().random().setSpan(Constants.REMOTE_RADIUS);
                pov.getUp().random();
            }
            pov.getFocus().set(pov.getEye()).setSpan(Constants.SURFACE_RADIUS);
            pov.getGaze().sub(pov.getFocus(), pov.getEye()).normalize();
            pov.perpendicular(pov.getGaze(), pov.getUp());
            pov.getRight().cross(pov.getGaze(), pov.getUp()).normalize();
            pov.update();
            transition = new LinearPovTransition(pov);
            transition.setDuration(Constants.SPACE_TO_LANDING);
            if (next(true) == hover) {
                transition.getEye().setSpan(Constants.HOVER_MIDDLE);
                transition.getFocus().setSpan(Constants.SURFACE_RADIUS);
            }
            else {
                transition.getEye().setSpan(Constants.ORBIT_RADIUS);
                transition.getFocus().zero();
            }
            transition.prepare();
            return true;
        }

        public boolean moveLower() {
            return transition.step();
        }

        public void interactive(double intensity) {
            System.exit(0); // todo: more graceful
        }

        public boolean prepareHigher() {
            return true;
        }

        public boolean moveHigher() {
            return false;
        }

        public String toString() {
            return "Space";
        }
    };

    private Level orbit = new Level() {
        private LinearPovTransition linear;
        private AngularPovTransition angular;
        private Space3 rotation = new Space3();

        public Core.ShipLevel getShipLevel() {
            return Core.ShipLevel.ORBIT;
        }

        public Level next(boolean lower) {
            return lower ? hover : space;
        }

        public boolean prepareCloser() {
            if (!core.hasBeing()) {
                return false;
            }
            SoundEffect.MOVE.play();
            angular = new AngularPovTransition(pov);
            angular.setEyeDestination(center());
            angular.setDurationPerRadian(Constants.ORBIT_ROTATE);
            angular.prepare();
            return true;
        }

        public boolean moveCloser() {
            boolean step = angular.step();
            if (!step) {
                SoundEffect.MOVE.stop();
            }
            return step;
        }

        public boolean prepareLower() {
            if (isFarAway()) {
                return false;
            }
            else {
                SoundEffect.DESCEND.play();
                linear = new LinearPovTransition(pov);
                linear.setDuration(Constants.ORBIT_TO_HOVER);
                if (core.hasBeing()) {
                    linear.getFocus().set(center()).setSpan(Constants.SURFACE_RADIUS);
                    linear.getEye().set(center()).setSpan(Constants.HOVER_BOTTOM);
                }
                else {
                    linear.getFocus().set(pov.getEye()).setSpan(Constants.SURFACE_RADIUS);
                    linear.getEye().setSpan(Constants.HOVER_TOP);
                }
                linear.prepare();
                return true;
            }
        }

        private boolean isFarAway() {
            if (!core.hasBeing()) {
                return false;
            }
            Arrow destination = center();
            double dot = destination.dot(pov.getEye()) / destination.span() / pov.getEye().span();
            return dot < Constants.ORBIT_CLOSE_DOT;
        }

        public boolean moveLower() {
            return linear.step();
        }

        public void interactive(double intensity) {
            rotation.set(pov.getRight(), Constants.ORBIT_NAVI_SPEED_FACTOR * getDownUp() * intensity);
            rotation.transform(pov.getEye());
            rotation.transform(pov.getUp());
            rotation.set(pov.getUp(), Constants.ORBIT_NAVI_SPEED_FACTOR * getLeftRight() * intensity);
            rotation.transform(pov.getEye());
            pov.getEye().setSpan(Constants.ORBIT_RADIUS);
            pov.update();
        }

        public boolean prepareHigher() {
            linear = new LinearPovTransition(pov);
            linear.setDuration(Constants.ORBIT_TO_SPACE);
            linear.getEye().setSpan(Constants.REMOTE_RADIUS);
            linear.prepare();
            return true;
        }

        public boolean moveHigher() {
            return linear.step();
        }

        public String toString() {
            return "Orbit";
        }
    };

    private Level hover = new Level() {
        private LinearPovTransition linear;
        private AngularPovTransition angular;
        private Space3 rotation = new Space3();

        public Core.ShipLevel getShipLevel() {
            return Core.ShipLevel.HOVER;
        }

        public Level next(boolean lower) {
            if (lower) {
                return core.hasBeing() ? watch : roam;
            }
            else {
                core.setBeing(null);
                return orbit;
            }
        }

        public boolean prepareCloser() {
            if (!core.hasBeing()) {
                return false;
            }
            SoundEffect.MOVE.play();
            angular = new AngularPovTransition(pov);
            angular.setEyeDestination(center());
            angular.setDurationPerRadian(Constants.HOVER_ROTATE);
            angular.prepare();
            return true;
        }

        public boolean moveCloser() {
            boolean step = angular.step();
            if (!step) {
                SoundEffect.MOVE.stop();
            }
            return step;
        }

        public boolean prepareLower() { // to roam
            if (isFarAway()) {
                return false;
            }
            else {
                SoundEffect.DESCEND.play();
                linearDown();
                return true;
            }
        }

        private boolean isFarAway() {
            if (!core.hasBeing()) {
                return false;
            }
            Arrow destination = center();
            double dot = destination.dot(pov.getEye()) / destination.span() / pov.getEye().span();
            return dot < Constants.HOVER_CLOSE_DOT;
        }

        private void linearDown() {
            linear = new LinearPovTransition(pov);
            linear.setDuration(Constants.HOVER_TO_ROAM);
            linear.getFocus().set(pov.getEye()).setSpan(Constants.ROAM_RADIUS);
            linear.getEye().setSpan(Constants.ROAM_RADIUS).sub(pov.getUp(), Constants.ROAM_DISTANCE);
            linear.prepare();
            setTargetFocus();
        }

        public boolean moveLower() {
            return linear.step();
        }

        public void interactive(double intensity) {
            double altitude = pov.getEye().span();
            double proportion = (altitude - Constants.HOVER_BOTTOM) / (Constants.HOVER_TOP - Constants.HOVER_BOTTOM);
            double factor = Constants.HOVER_KEY_FACTOR * Constants.SURFACE_ANGLE * (1 + proportion * 4);
            rotation.set(pov.getRight(), factor * getDownUp() * intensity);
            rotation.transform(pov.getFocus());
            rotation.transform(pov.getEye());
            rotation.transform(pov.getUp());
            rotation.set(pov.getUp(), factor * getLeftRight() * intensity);
            rotation.transform(pov.getFocus());
            rotation.transform(pov.getEye());
            rotation.transform(pov.getUp());
            double forwardBackward = wheelHandler.isActive() ? wheelHandler.getForwardBackward() : getForwardBackward();
            double span = pov.getEye().span() - (2 + 3 * proportion) * Constants.HOVER_KEY_FACTOR * forwardBackward * intensity;
            if (span < Constants.HOVER_BOTTOM) {
                span = Constants.HOVER_BOTTOM;
            }
            else if (span > Constants.HOVER_TOP) {
                span = Constants.HOVER_TOP;
            }
            pov.getEye().setSpan(span);
            pov.update();
        }

        public boolean prepareHigher() {
            SoundEffect.ASCEND.play();
            linear = new LinearPovTransition(pov);
            linear.setDuration(Constants.HOVER_TO_ORBIT);
            linear.getEye().setSpan(Constants.ORBIT_RADIUS);
            linear.getFocus().zero();
            linear.prepare();
            return true;
        }

        public boolean moveHigher() {
            return linear.step();
        }

        public String toString() {
            return "Hover";
        }
    };

    private Level roam = new Level() {
        private LinearPovTransition transition;
        private Arrow upwards = new Arrow();
        private Space3 rotation = new Space3();
        private Arrow focusToSurfaceFocus = new Arrow();

        public Core.ShipLevel getShipLevel() {
            return Core.ShipLevel.ROAM;
        }

        public Level next(boolean lower) {
            return lower ? watch : hover;
        }

        public boolean isClose() {
            return true;
        }

        public boolean prepareCloser() {
            return false;
        }

        public boolean moveCloser() {
            return false;
        }

        public boolean prepareLower() {
            if (core.hasBeing()) {
                setTargetFocus();
                return true;
            }
            else {
                return false;
            }
        }

        public boolean moveLower() {
            return false;
        }

        public void interactive(double intensity) {
            upwards.set(pov.getEye()).normalize();
            striveForRadius(pov.getFocus(), Constants.ROAM_RADIUS, upwards, 0.5); // todo: ignores frame rate
            striveForRadius(pov.getEye(), Constants.ROAM_RADIUS, upwards, 0.5);
            pov.adjustUp(upwards, Constants.ADJUST_UPWARDS);
            double downUp = getDownUp();
            if (downUp > 0) {
                downUp /= 3;
            }
            rotation.set(pov.getRight(), Constants.ROAM_KEY_FACTOR * Constants.SURFACE_ANGLE * downUp * intensity);
            rotation.transform(targetFocus);
            rotation.set(upwards, -12 * Constants.ROAM_KEY_FACTOR * Constants.SURFACE_ANGLE * getLeftRight() * intensity);
            rotation.transform(targetFocus);
            // chase the focus
            targetFocus.setSpan(Constants.ROAM_RADIUS);
            double focusDistance = targetFocus.distanceTo(pov.getFocus());
            if (focusDistance > 0.01) {
                focusToSurfaceFocus.sub(targetFocus, pov.getFocus()).normalize();
                pov.getFocus().add(focusToSurfaceFocus, Math.min(Constants.ROAM_FOCUS_CHASE_FACTOR, focusDistance));
            }
            // hold distance
            double distance = pov.getDistance();
            double newDistance = Constants.ROAM_DISTANCE * Constants.ROAM_INERTIA + distance * (1 - Constants.ROAM_INERTIA);
            pov.setDistanceFromFocus(newDistance);
        }

        public boolean prepareHigher() {
            SoundEffect.ASCEND.play();
            transition = new LinearPovTransition(pov);
            transition.setDuration(Constants.ROAM_TO_HOVER);
            transition.getFocus().setSpan(Constants.SURFACE_RADIUS);
            transition.getEye().set(transition.getFocus()).setSpan(Constants.HOVER_BOTTOM);
            transition.prepare();
            return true;
        }

        public boolean moveHigher() {
            return transition.step();
        }

        public String toString() {
            return "Roam";
        }
    };

    private Level watch = new Level() {
        private LinearPovTransition transition;
        private Arrow upwards = new Arrow();
        private Arrow focusToSurfaceFocus = new Arrow();

        public Core.ShipLevel getShipLevel() {
            return Core.ShipLevel.WATCH;
        }

        public Level next(boolean lower) {
            return lower ? roam : hover;
        }

        public boolean isClose() {
            return true;
        }

        public boolean prepareCloser() {
            return false;
        }

        public boolean moveCloser() {
            return false;
        }

        public boolean prepareLower() {
            double distance = pov.getDistance();
            if (distance > Constants.ROAM_DISTANCE * 1.1) {
                transition = new LinearPovTransition(pov);
                transition.getFocus().set(pov.getEye()).add(pov.getGaze(), Constants.ROAM_DISTANCE).setSpan(Constants.ROAM_RADIUS);
                transition.setDuration((long) (Constants.WATCH_TO_ROAM * (distance - Constants.ROAM_DISTANCE) / Constants.WATCH_FAR));
                transition.prepare();
            }
            else {
                transition = null;
            }
            return true;
        }

        public boolean moveLower() {
            if (transition != null && transition.step()) {
                return true;
            }
            else {
                setTargetFocus();
                return false;
            }
        }

        public void interactive(double intensity) {
            if (!core.hasBeing()) {
                backOff();
                return;
            }
            targetFocus.set(center()).setSpan(Constants.ROAM_RADIUS);
            // radius adjustments
            upwards.set(pov.getEye()).normalize();
            striveForRadius(pov.getFocus(), Constants.ROAM_RADIUS, upwards, 0.2);
            striveForRadius(pov.getEye(), Constants.ROAM_RADIUS, upwards, 0.5);
            pov.adjustUp(upwards, Constants.ADJUST_UPWARDS);
            // focus movement
            double focusDistance = targetFocus.distanceTo(pov.getFocus());
            if (focusDistance > 0.01) {
                focusToSurfaceFocus.sub(targetFocus, pov.getFocus()).normalize();
                double distance = pov.getDistance();
                pov.getFocus().add(focusToSurfaceFocus, Math.min(Constants.ROAM_FOCUS_CHASE_FACTOR, focusDistance));
                pov.setDistanceFromFocus(distance);
            }
            // up-down control
            double move = getDownUp() * Constants.WATCH_APPROACH_FACTOR * intensity;
            double distance = pov.getDistance() - move;
            if (distance < Constants.ROAM_DISTANCE) {
                distance = Constants.ROAM_DISTANCE;
            }
            else if (distance > Constants.WATCH_FAR) {
                distance = Constants.WATCH_FAR;
            }
            pov.setDistanceFromFocus(distance);
            // left-right control
            pov.focusRotateX(getLeftRight() * Constants.WATCH_ROTATE_FACTOR * intensity);
        }

        public boolean prepareHigher() {
            SoundEffect.ASCEND.play();
            transition = new LinearPovTransition(pov);
            transition.setDuration(Constants.ROAM_TO_HOVER);
            transition.getFocus().setSpan(Constants.SURFACE_RADIUS);
            transition.getEye().set(transition.getFocus()).setSpan(Constants.HOVER_BOTTOM);
            transition.prepare();
            return true;
        }

        public boolean moveHigher() {
            return transition.step();
        }

        public String toString() {
            return "Watch";
        }
    };

    private void setTargetFocus() {
        targetFocus.set(pov.getFocus());
        targetFocus.setSpan(Constants.ROAM_RADIUS);
    }

    private static void striveForRadius(Arrow arrow, double radius, Arrow upwards, double degree) {
        double currentRadius = upwards.set(arrow).normalize();
        arrow.add(upwards, (radius - currentRadius) * degree);
    }

    private double getLeftRight() {
        return keyHandler.getLeftRight().getValue();
    }

    private double getDownUp() {
        return keyHandler.getDownUp().getValue();
    }

    private double getForwardBackward() {
        return keyHandler.getForwardBackward().getValue();
    }

    private class WheelHandler implements MouseWheelListener {
        private int totalChangeSince;

        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {
            int change = e.getWheelRotation();
            totalChangeSince += change;
            lastGesture = System.currentTimeMillis();
        }

        public boolean isActive() {
            return totalChangeSince != 0;
        }

        public double getForwardBackward() {
            double forwardBackward = totalChangeSince / 5.0;
            totalChangeSince = 0;
            if (forwardBackward > 1) {
                forwardBackward = 1;
            }
            else if (forwardBackward < -1) {
                forwardBackward = -1;
            }
            return forwardBackward;
        }
    }

    private class KeyHandler extends DirectionKeys {
        public void clear() {
        }

        public Boolean getForward() {
            Boolean was = forward;
            forward = null;
            return was;
        }

        @Override
        public void keyPressed(KeyEvent event) {
            switch (event.getKeyCode()) {
                case KeyEvent.VK_ESCAPE:
                    forward = Boolean.FALSE;
                    break;
                case KeyEvent.VK_ENTER:
                    forward = Boolean.TRUE;
                    break;
            }
            super.keyPressed(event);
            lastGesture = System.currentTimeMillis();
        }
    }
}