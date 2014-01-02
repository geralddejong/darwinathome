// ========= Copyright (C) 2009, 2010 Gerald de Jong =================
// This file is part of the Darwin at Home project, distributed
// under the GNU General Public License, version 3.
// You should have received a copy of this license in "license.txt",
// but if not, see http://www.gnu.org/licenses/gpl-3.0.txt.
// ===================================================================
package org.darwinathome.universe;

import org.apache.log4j.Logger;
import org.darwinathome.Constants;
import org.darwinathome.body.Being;
import org.darwinathome.body.BeingFactory;
import org.darwinathome.body.Embryo;
import org.darwinathome.body.JointSurface;
import org.darwinathome.genetics.Noise;
import org.darwinathome.genetics.impl.PseudoNoise;
import org.darwinathome.geometry.math.Arrow;
import org.darwinathome.geometry.structure.Physics;
import org.darwinathome.geometry.transform.Relocator;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The entire universe where it all happens
 *
 * @author Gerald de Jong, Beautiful Code BV, <geralddejong@gmail.com>
 */

public class World {
    private static final String MAGIC_STRING = "Tetragotchi";
    private Logger log = Logger.getLogger(getClass());
    private List<Listener> listeners = new CopyOnWriteArrayList<Listener>();
    private Noise noise;
    private BeingFactory beingFactory;
    private Map<String, Being> beings = new TreeMap<String, Being>();
    private List<Being> deadBeings = new ArrayList<Being>();
    private List<Being> rebornBeings = new ArrayList<Being>();
    private List<SurfacePatch> surfacePatches;
    private float[] water;
    private Physics physics;
    private Physics.Constraints constraints;
    private long patchAge, age;
    private transient double totalWaterLastTime;

    public interface Listener {

        void beingBorn(Being being);

        void beingDied(Being being);
    }

    protected World(Noise noise) {
        this(noise, SurfacePatch.createWater(noise));
    }

    private World(Noise noise, float[] water) {
        this.noise = noise;
        this.water = water;
        surfacePatches = SurfacePatch.createSurfacePatches(water);
        this.beingFactory = new BeingFactory(noise, new JointSurface.Factory(surfacePatches), new OutsideImpl());
        constraints = new SphericalPhysics();
        physics = new Physics(constraints);
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public BeingFactory getBeingFactory() {
        return beingFactory;
    }

    public long getAge() {
        return age;
    }

    public Being getBeing(String bodyName) {
        return beings.get(bodyName);
    }

    public Being getNearestBeing(Arrow arrow, Being avoid) {
        double bestDot = Double.NEGATIVE_INFINITY;
        Being nearest = null;
        for (Being being : getBeings()) {
            if (being == avoid) continue;
            double dot = arrow.dot(being.getGeometry().getBodyCenter());
            if (dot > bestDot) {
                bestDot = dot;
                nearest = being;
            }
        }
        return nearest;
    }

    public void write(DataOutputStream dos) throws IOException {
        dos.write(MAGIC_STRING.getBytes("ASCII"));
        dos.write(0);
        dos.writeShort(water.length);
        for (float a : water) {
            dos.writeFloat(a);
        }
        dos.writeLong(patchAge);
        dos.writeLong(age);
        dos.writeInt(beings.size());
        for (Being being : beings.values()) {
            beingFactory.save(being, dos);
        }
        log.info("saved " + beings.size() + " beings");
    }

    public static World create() {
        return new World(new PseudoNoise());
    }

    public static World read(DataInputStream dis) throws IOException {
        byte[] magic = new byte[MAGIC_STRING.length()];
        if (dis.read(magic) != magic.length) {
            throw new IOException("Couldn't read magic string.  This is not a World file!");
        }
        String magicString = new String(magic, "ASCII");
        if (!MAGIC_STRING.equals(magicString)) {
            throw new IOException("Couldn't find magic string, found [" + magicString + "].  This is not a World file!");
        }
        dis.readByte(); // ignore;
        int altitudeSize = dis.readShort();
        float[] altitude = new float[altitudeSize];
        for (int walk = 0; walk < altitude.length; walk++) {
            altitude[walk] = dis.readFloat();
        }
        World world = new World(new PseudoNoise(), altitude);
        world.patchAge = dis.readLong();
        world.age = dis.readLong();
        int beingCount = dis.readInt();
        while (beingCount-- > 0) {
            Being being = world.getBeingFactory().restore(dis);
            world.log.info(String.format("Loaded %s (age %d), prey: %s", being, being.getBody().getAge(), being.getPreyName()));
            world.beings.put(being.toString(), being);
        }
        for (Being being : world.beings.values()) {
            being.getGeometry().findBodyCenter();
        }
        for (Being being : world.beings.values()) {
            being.getGeometry().refresh();
        }
        return world;
    }

    public void experienceTime(int duration) {
        age += duration;
        if (age - patchAge > Constants.ITERATIONS_PER_PATCH_LIFE) {
            patchLife();
            patchAge += Constants.ITERATIONS_PER_PATCH_LIFE;
        }
        physics.setIterations(duration);
        for (Being being : beings.values()) {
            being.getGeometry().refresh();
        }
        for (Being being : beings.values()) {
            if (!being.getPreyName().isEmpty()) {
                being.getGeometry().handlePrey();
            }
        }
        Iterator<Being> beingIterator = beings.values().iterator();
        while (beingIterator.hasNext()) {
            Being being = beingIterator.next();
            try {
                being.experienceTime(physics);
                switch (being.getPhase()) {
                    case BIRTH:
                        log.info("Birth of " + being);
                        for (Listener listener : listeners) {
                            listener.beingBorn(being);
                        }
                        break;
                    case UNDEATH:
                        rebornBeings.add(being);
                        beingIterator.remove();
                        break;
                    case DEATH:
                        log.info("Death of " + being);
                        beingIterator.remove();
                        for (Listener listener : listeners) {
                            listener.beingDied(being);
                        }
                        break;
                }
            }
            catch (Exception e) {
                log.warn("Boom!", e);
                deadBeings.add(being);
                beingIterator.remove();
            }
        }
        if (!rebornBeings.isEmpty()) {
            for (Being being : rebornBeings) {
                Arrow center = new Arrow();
                being.getShield().getCenter(center);
                Arrow forward = new Arrow();
                forward.sub(being.getTrail().getLast(), being.getTrail().getFirst());
                if (forward.span() > 0.1) {
                    forward.normalize();
                }
                else {
                    forward.random();
                }
                Being fresh = createBeing(
                        new Embryo(
                                being.getId(),
                                being.getEmail(),
                                being.getSpeech(),
                                being.getGenome(),
                                being.getTrail()
                        ),
                        center,
                        forward
                );
                fresh.getGeometry().refresh();
            }
            rebornBeings.clear();
        }
    }

    public SphericalPhysics getSphericalPhysics() {
        return (SphericalPhysics) constraints;
    }

    public Collection<Being> getBeings() {
        return beings.values();
    }

    public Physics getPhysics() {
        return physics;
    }

    public Noise getNoise() {
        return noise;
    }

    public List<Being> getDeadBeings() {
        return deadBeings;
    }

    public List<SurfacePatch> getSurfacePatches() {
        return surfacePatches;
    }

    public Being createBeing(Arrow location, Arrow gaze, String bodyName) {
        return putBeing(beingFactory.create(bodyName), location, gaze);
    }

    public Being createBeing(Embryo embryo, Arrow location, Arrow gaze) {
        return putBeing(beingFactory.create(embryo), location, gaze);
    }

    private Being putBeing(Being being, Arrow location, Arrow gaze) {
        if (location != null) {
            Relocator relocator = createRelocator(being, location, gaze);
            being.getBody().addTransformation(relocator);
            being.getBody().executeTransformations(null);
            being.getShield().addTransformation(relocator);
            being.getShield().executeTransformations(null);
        }
        beings.put(being.toString(), being);
        log.info("Being created: " + being);
        return being;
    }

    private Relocator createRelocator(Being being, Arrow location, Arrow gaze) {
        Arrow x = new Arrow(gaze).scale(-1);
        Arrow center = new Arrow(location);
        Arrow shieldCenter = new Arrow();
        being.getShield().getCenter(shieldCenter);
        double shieldRadius = being.getShield().getRadiusFrom(shieldCenter);
        center.setSpan(Constants.SURFACE_RADIUS + shieldRadius);
        Arrow z = new Arrow(location);
        z.normalize();
        x.sub(z, z.dot(x));
        x.normalize();
        Arrow y = new Arrow().cross(gaze, z);
        y.normalize();
        return new Relocator(x, y, z, center);
    }

    public void add(Being being) {
        beings.put(being.toString(), being);
    }

    private void patchLife() {
        float totalWater = 0f;
        for (SurfacePatch patch : surfacePatches) {
            totalWater += patch.prepare();
        }
        if (Math.abs(totalWaterLastTime - totalWater) > 10) {
            log.info("Total water: " + totalWater);
            totalWaterLastTime = totalWater;
        }
        for (SurfacePatch patch : surfacePatches) {
            patch.distribute();
        }
        double totalEnergy = 0;
        for (Being being : beings.values()) {
            totalEnergy += being.getEnergy().getAmount();
        }
        double averageEnergy = totalEnergy / beings.size();
        if (averageEnergy < 0.5) {
            double averageAmount = 2.0 / Constants.RAIN_LOCATIONS;
            for (int walk = 0; walk < Constants.RAIN_LOCATIONS; walk++) {
                int index = noise.choose(surfacePatches.size());
                surfacePatches.get(index).rain(averageAmount * noise.nextDouble());
            }
        }
        for (SurfacePatch patch : surfacePatches) {
            patch.commit();
        }
    }

    private class OutsideImpl implements Being.Outside {
        @Override
        public Being getBeing(String bodyName) {
            return beings.get(bodyName);
        }

        @Override
        public SurfacePatch getNearestPatch(Arrow location) {
            return SurfacePatch.fetchPatch(location, surfacePatches);
        }
    }
}
