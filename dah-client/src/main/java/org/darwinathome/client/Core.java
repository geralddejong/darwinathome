package org.darwinathome.client;

import org.darwinathome.Constants;
import org.darwinathome.body.Being;
import org.darwinathome.body.Target;
import org.darwinathome.geometry.math.Arrow;
import org.darwinathome.geometry.structure.Physics;
import org.darwinathome.universe.SphericalPhysics;
import org.darwinathome.universe.SurfacePatch;
import org.darwinathome.universe.World;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The central location for the important state information, including the world itself
 *
 * @author Gerald de Jong <geralddejong@gmail.com>
 */

public class Core {
    public enum SpokeState {
        FROZEN,
        TIME_ON,
        EVOLVING,
        SAVE_GENOME,
    }

    public enum ShipLevel {
        TRANSITION,
        SPACE,
        ORBIT,
        HOVER,
        WATCH,
        ROAM
    }

    public enum ShipState {
        DORMANT,
        APPROACHING,
        RISING,
        FALLING,
        STEADY
    }

    public interface StateListener {
        void stateChanged(Core core);
    }

    public interface TimeListener {
        void snapshot(World world);

        void beingSet(Being being);

        void timeIs(long frozenTime, long frozenWorldAge, long currentWorldAge, Being being);
    }

    public interface EvolutionListener {
        void evolutionStarted(long frozenTime, long frozenBeingAge);

        void evolutionEnded();

        void evolutionProgress(long frozenTime, long frozenAge, long lifespan, double averageSpeed, double topSpeed);
    }

    private List<TimeListener> timeListeners = new CopyOnWriteArrayList<TimeListener>();
    private long nextTimeBroadcast;
    private List<EvolutionListener> evolutionListeners = new CopyOnWriteArrayList<EvolutionListener>();
    private List<StateListener> stateListeners = new CopyOnWriteArrayList<StateListener>();
    private String email;
    private World world;
    private byte[] frozenWorld;
    private long frozenTime;
    private long frozenWorldAge;
    private long evolutionStartTime;
    private long evolutionStartAge;
    private Being being;
    private SpokeState spokeState = SpokeState.FROZEN;
    private ShipLevel shipLevel = ShipLevel.SPACE;
    private ShipState shipState = ShipState.DORMANT;

    public void setEmail(String email) {
        this.email = email;
    }

    public String getEmail() {
        return email;
    }

    public void setBeing(Being being) {
        boolean change =
                (being == null && this.being != null) ||
                        (being != null && this.being == null) ||
                        (being != null && this.being != being);
        this.being = being;
        if (change) {
            for (TimeListener timeListener : timeListeners) {
                timeListener.beingSet(being);
            }
        }
    }

    public Being getBeing() {
        if (hasBeing() && being.getPhase() == Being.Phase.DEATH) {
            being = null;
        }
        return being;
    }

    public boolean hasBeing() {
        return being != null;
    }

    public boolean isBeingControllable() {
        return hasBeing() && being.getEmail().equals(email);
    }

    public boolean isBeingEvolvable() {
        return isBeingControllable() && being.getPhase() == Being.Phase.ADULT_LIFE;
    }

    public boolean isCreationMode() {
        return being == null || Constants.Calc.isCreationMode(being.getBody().getAge());
    }

    public boolean isReady() {
        return world != null;
    }

    public void addTimeListener(TimeListener timeListener) {
        timeListeners.add(timeListener);
    }

    public void addEvolutionListener(EvolutionListener evolutionListener) {
        evolutionListeners.add(evolutionListener);
    }

    public void addStateListener(StateListener stateListener) {
        stateListeners.add(stateListener);
        stateListener.stateChanged(this);
    }

    public SpokeState getSpokeState() {
        return spokeState;
    }

    public boolean mouseActivate() {
        return spokeState == SpokeState.FROZEN || spokeState == SpokeState.TIME_ON;
    }

    public void setSpokeState(SpokeState spokeState) {
        this.spokeState = spokeState;
        fireStateChange();
    }

    public ShipLevel getShipLevel() {
        return shipLevel;
    }

    public void setShipLevel(ShipLevel shipLevel) {
        this.shipLevel = shipLevel;
        fireStateChange();
    }

    public ShipState getShipState() {
        return shipState;
    }

    public void setShipState(ShipState shipState) {
        this.shipState = shipState;
        fireStateChange();
    }

    public boolean isOlderThanFrozen() {
        return getWorld().getAge() > frozenWorldAge;
    }

    public void setWorld(World world) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(out);
            world.write(dos);
            frozenWorld = out.toByteArray();
            frozenTime = System.currentTimeMillis();
            frozenWorldAge = world.getAge();
            setWorldInternal(world);
        }
        catch (IOException e) {
            throw new RuntimeException("Unable to freeze world", e);
        }
    }

    public World getWorld() {
        return world;
    }

    public void restoreWorld(Target target) {
        try {
            setWorldInternal(World.read(new DataInputStream(new ByteArrayInputStream(frozenWorld))));
            if (hasBeing()) {
                being.setTarget(target);
            }
        }
        catch (IOException e) {
            throw new RuntimeException("Cannot read world");
        }
    }

    public void experienceTime(int iterations) {
        if (spokeState != SpokeState.TIME_ON) {
            throw new RuntimeException("Time is off");
        }
        if (world != null) {
            world.experienceTime(iterations);
            if (hasBeing()) {
                if (being.getPhase() == Being.Phase.UNDEATH) {
                    being = world.getBeing(being.toString());
                }
            }
            if (System.currentTimeMillis() > nextTimeBroadcast) {
                for (TimeListener timeListener : timeListeners) {
                    timeListener.timeIs(frozenTime, frozenWorldAge, world.getAge(), being);
                }
                nextTimeBroadcast = System.currentTimeMillis() + Constants.TIME_UPDATE_MILLIS;
            }
        }
    }

    public void startEvolution() {
        if (hasBeing()) {
            long beingTimeSinceFreezing = being.getBody().getAge() - (world().getAge() - frozenWorldAge);
            evolutionStartTime = frozenTime + beingTimeSinceFreezing * Constants.MILLIS_PER_ITERATION;
            evolutionStartAge = being.getBody().getAge();
            for (EvolutionListener evolutionListener : evolutionListeners) {
                evolutionListener.evolutionStarted(evolutionStartTime, evolutionStartAge);
            }
        }
    }

    public void progressEvolution(long evolutionLifespan, double averageSpeed, double topSpeed) {
        for (EvolutionListener evolutionListener : evolutionListeners) {
            evolutionListener.evolutionProgress(evolutionStartTime, evolutionStartAge, evolutionLifespan, averageSpeed, topSpeed);
        }
    }

    public void terminateEvolution() {
        for (EvolutionListener evolutionListener : evolutionListeners) {
            evolutionListener.evolutionEnded();
        }
    }

    public Collection<Being> getBeings() {
        return world().getBeings();
    }

    public SphericalPhysics getSphericalPhysics() {
        return world().getSphericalPhysics();
    }

    public Physics getPhysics() {
        return world().getPhysics();
    }

    public List<SurfacePatch> getSurfacePatches() {
        return world().getSurfacePatches();
    }

    public Being getNearestBeing(Arrow surfaceLocation) {
        return world().getNearestBeing(surfaceLocation, being);
    }

    // the rest are private

    private void setWorldInternal(World world) {
        this.world = world;
        setSpokeState(SpokeState.FROZEN);
        for (TimeListener timeListener : timeListeners) {
            timeListener.snapshot(world);
            timeListener.timeIs(frozenTime, frozenWorldAge, world.getAge(), being);
        }
    }

    private World world() {
        if (world == null) {
            throw new RuntimeException("World is not ready yet.");
        }
        return world;
    }

    private void fireStateChange() {
        for (StateListener stateListener : stateListeners) {
            stateListener.stateChanged(this);
        }
    }

}
