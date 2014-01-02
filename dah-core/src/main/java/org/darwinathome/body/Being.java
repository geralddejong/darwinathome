// ========= Copyright (C) 2009, 2010 Gerald de Jong =================
// This file is part of the Darwin at Home project, distributed
// under the GNU General Public License, version 3.
// You should have received a copy of this license in "license.txt",
// but if not, see http://www.gnu.org/licenses/gpl-3.0.txt.
// ===================================================================
package org.darwinathome.body;

import org.apache.log4j.Logger;
import org.darwinathome.Constants;
import org.darwinathome.genetics.Gene;
import org.darwinathome.genetics.Genome;
import org.darwinathome.genetics.Noise;
import org.darwinathome.geometry.math.Arrow;
import org.darwinathome.geometry.structure.Fablob;
import org.darwinathome.geometry.structure.Fabric;
import org.darwinathome.geometry.structure.Face;
import org.darwinathome.geometry.structure.Interval;
import org.darwinathome.geometry.structure.Joint;
import org.darwinathome.geometry.structure.Physics;
import org.darwinathome.geometry.structure.TensegritySphereFactory;
import org.darwinathome.geometry.structure.Thing;
import org.darwinathome.geometry.transform.JointMerge;
import org.darwinathome.universe.SurfacePatch;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.LinkedList;
import java.util.List;

/**
 * The thing stored in a fabric which holds the blind watchmaker being state
 *
 * @author Gerald de Jong, Beautiful Code BV, <geralddejong@gmail.com>
 */

public class Being implements SurfacePatch.Holder, Thing {

    // always there stuff
    private Logger log = Logger.getLogger(getClass());
    private Fabric.PeriodicTransformation jointMerge = new JointMerge.Periodic(30);
    private Geometry geometry;

    // transient stuff
    private SurfacePatch surfacePatch; // transient
    private boolean drinking;

    // stored stuff
    private Fabric body;
    private String id;
    private String email;
    private Fabric shield;
    private Genome genome;
    private Energy energy;
    private Speech speech;
    private Phase phase = Phase.TRUNK_GROWTH;
    private Arrow goal = new Arrow();
    private String preyName = "";
    private LinkedList<Arrow> trail = new LinkedList<Arrow>();
    private long trailAge;
    private boolean virtual;

    public enum Phase {
        CONCEPTION,
        TRUNK_GROWTH,
        JOINT_MERGE,
        SELECT_LIMB_FACES,
        LIMB_GROWTH,
        SHIELD_COLLAPSE,
        BIRTH,
        ADULT_LIFE,
        KILLED,
        DYING,
        BIRTH_CANAL,
        UNDEATH,
        DEATH,
    }

    public interface Outside {
        Being getBeing(String bodyName);

        SurfacePatch getNearestPatch(Arrow location);
    }

    private Being(Fabric body, Outside outside) {
        this.body = body;
        this.geometry = new Geometry(this, outside);
    }

    public void setVirtual(boolean virtual) {
        this.virtual = virtual;
    }

    public Geometry getGeometry() {
        return geometry;
    }

    public LinkedList<Arrow> getTrail() {
        return trail;
    }

    public void interpolateTrail(Arrow point) {
        int index = (int) (trailAge / Constants.ITERATIONS_PER_TRAIL_POINT);
        if (index < trail.size()) {
            double proportion = ((double) trailAge) / (trail.size() * Constants.ITERATIONS_PER_TRAIL_POINT);
            double size = (1 - proportion) * Constants.MIN_SHIELD_RADIUS + proportion * Constants.MAX_SHIELD_RADIUS;
            Arrow shieldCenter = new Arrow();
            Arrow move = new Arrow();
            shield.getCenter(shieldCenter);
            for (Joint joint : shield.getJoints()) {
                Arrow loc = joint.getLocation();
                move.sub(loc, shieldCenter);
                move.setSpan(size);
                loc.add(shieldCenter, move);
            }
            if (index + 1 < trail.size()) {
                Arrow point0 = trail.get(index);
                Arrow point1 = trail.get(index + 1);
                proportion = ((double) (trailAge % Constants.ITERATIONS_PER_TRAIL_POINT)) / Constants.ITERATIONS_PER_TRAIL_POINT;
                point.interpolate(point0, point1, proportion);
            }
            else {
                point.set(trail.getLast());
            }
        }
    }

    public void addToTrail(Arrow arrow) {
        if (shield == null && body.getAge() - trailAge > Constants.ITERATIONS_PER_TRAIL_POINT) {
            trail.add(new Arrow(arrow));
            trailAge += Constants.ITERATIONS_PER_TRAIL_POINT;
            if (trail.size() > Constants.MAX_TRAIL_SIZE) {
                trail.remove();
            }
        }
    }

    public double getFitness() {
        if (trail.isEmpty()) {
            return 0;
        }
        else {
            double sum = 0;
            Arrow prevPoint = getGeometry().getBodyCenter();
            for (Arrow point : trail) {
                sum += prevPoint.distanceTo(point);
                prevPoint = point;
            }
            return sum;
        }
    }

    public boolean isDrinking() {
        return drinking;
    }

    public Fabric getBody() {
        return body;
    }

    public Fabric getShield() {
        return shield;
    }

    public Genome getGenome() {
        return genome;
    }

    public void setGenome(Genome genome) {
        this.genome = genome;
    }

    public Energy getEnergy() {
        return energy;
    }

    public String getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPreyName() {
        return preyName;
    }

    public void clearPreyName() {
        this.preyName = "";
    }

    public void bittenBy(Being maker) {
        // give the maker what he deserves
        maker.preyName = "";
        maker.geometry.releasePrey();
        // dramatically die
        phase = Phase.KILLED;
        body.addTransformation(new DeathTransformation());
        // take over the genome
        genome = maker.genome.copy();
    }

    public boolean mayHunt(Being other) {
        return !other.email.equals(email);
    }

    public boolean isTargetSame(Target target) {
        if (preyName.isEmpty() && target.getPreyName().isEmpty()) {
            Arrow goalAtTargetRadius = new Arrow(goal).setSpan(target.getLocation().span());
            return goalAtTargetRadius.distanceTo(target.getLocation()) < Constants.TARGET_TOLERANCE;
        }
        else {
            return target.getPreyName().equals(preyName);
        }
    }

    public void setTarget(Target target) {
        goal.set(target.getLocation());
        preyName = target.getPreyName();
    }

    public void handleHanging() {
        if (shield != null) {
            geometry.handleHanging();
        }
    }

    public Arrow getGoal() {
        return goal;
    }

    public void mutateDirectionGene(Direction direction, double chanceOfMutation) {
        getMovementGene(direction).mutate(chanceOfMutation);
    }

    public void randomizeMovementGene(Direction direction) {
        getMovementGene(direction).forget();
    }

    public Speech getSpeech() {
        if (speech == null) {
            speech = new Speech(toString());
        }
        return speech;
    }

    public void setSpeech(String signString) {
        this.speech = new Speech(speech == null ? "" : signString);
    }

    public Phase getPhase() {
        return phase;
    }

    public void experienceTime(Physics physics) {
        switch (phase) {
            case CONCEPTION:
                phase = Phase.TRUNK_GROWTH;
                break;
            case TRUNK_GROWTH:
                if (!body.isAnySpanActive()) {
                    if (!growFaces()) {
                        body.addTransformation(jointMerge);
                        phase = Phase.JOINT_MERGE;
                    }
                }
                break;
            case JOINT_MERGE:
                if (!body.isAnySpanActive()) {
                    if (jointMerge.isFinished()) {
                        phase = Phase.SELECT_LIMB_FACES;
                        log.info("joints merged\n" + genome);
                    }
                    else {
                        body.addTransformation(jointMerge);
                        log.info("added joint merge");
                    }
                }
                break;
            case SELECT_LIMB_FACES:
                addLimbBuds();
                phase = Phase.LIMB_GROWTH;
                break;
            case LIMB_GROWTH:
                if (!body.isAnySpanActive()) {
                    if (!growFaces()) {
                        phase = Phase.SHIELD_COLLAPSE;
                        shield.addTransformation(new Fabric.Transformation() {
                            @Override
                            public void transform(Fabric fabric) {
                                for (Interval interval : fabric.getIntervals()) {
                                    if (interval.getRole() == Interval.Role.CABLE) {
                                        interval.getSpan().setIdeal(interval.getSpan().getUltimateIdeal() * 2, 3600);
                                    }
                                }
                            }

                        });
                    }
                }
                break;
            case SHIELD_COLLAPSE:
                if (!shield.isAnySpanActive()) {
                    phase = Phase.BIRTH;
                }
                break;
            case BIRTH:
                shield = null;
                phase = Phase.ADULT_LIFE;
                energy = new Energy(0.5);
                break;
            case ADULT_LIFE:
                if (!body.isAnySpanActive()) {
                    drinking = false;
                    if (!virtual && energy.getAmount() < 1) {
                        for (Joint joint : body.getJoints()) {
                            JointSurface jointSurface = (JointSurface) joint.getThing();
                            if (jointSurface != null && joint.getLocation().span() < Constants.SURFACE_RADIUS) {
                                double energyNeeded = 1 - energy.getAmount();
                                if (energyNeeded > Constants.WATER_CONSUMPTION) {
                                    energyNeeded = Constants.WATER_CONSUMPTION;
                                }
                                else if (energyNeeded < 0) {
                                    energyNeeded = 0;
                                }
                                float consumed = jointSurface.getPatch(joint.getLocation()).consumeWater((float) energyNeeded);
                                if (consumed > 0) {
                                    drinking = true;
                                }
                                energy.add(consumed);
                            }
                        }
                    }
                    if (!drinking) {
                        takeAStep();
                    }
                    if (energy.isEmpty()) {
                        body.addTransformation(new DeathTransformation());
                        body.executeTransformations(null);
                        phase = Phase.KILLED;
                    }
                }
                break;
            case KILLED:
                if (!body.isAnySpanActive()) {
                    phase = Phase.DYING;
                }
                break;


            // todo: we need to handle birth like this, but not with a maker
//            Being maker = geometry.getMaker();
//            if (maker != null) {
//                trailAge = 0;
//                phase = Phase.BIRTH_CANAL;
//                shield = new TensegritySphereFactory(null).createSphere(3, Constants.MIN_SHIELD_RADIUS); // capsule
//                Arrow sheildCenter = new Arrow();
//                shield.getCenter(sheildCenter);
//                Arrow bodyCenter = new Arrow();
//                body.getCenter(bodyCenter);
//                Arrow move = new Arrow().sub(bodyCenter, sheildCenter);
//                for (Joint joint : shield.getJoints()) {
//                    joint.getLocation().add(move);
//                }
//                // take over maker's trail, reversed, as birth path
//                trail.clear();
//                for (Arrow point : maker.trail) {
//                    Arrow surfacePoint = new Arrow(point);
//                    surfacePoint.setSpan(Constants.ROAM_RADIUS);
//                    trail.addFirst(surfacePoint);
//                }
//                trail.addFirst(new Arrow(bodyCenter));
//                body.getFaces().clear();
//                body.getIntervals().clear();
//                body.getJoints().clear();
//            }
//            else {
//                phase = Phase.DEATH;
//            }

            case DYING:
                if (!body.isAnySpanActive()) {
                    phase = Phase.DEATH;
                }
                break;
            case DEATH:
                break;
            case BIRTH_CANAL:
                trailAge += physics.getIterations() * 5;
                if (trailAge / Constants.ITERATIONS_PER_TRAIL_POINT >= trail.size()) {
                    phase = Phase.UNDEATH;
                }
                break;
            case UNDEATH:
                break;
            default:
                throw new RuntimeException("Unhandled: " + phase);
        }
        if (body != null) {
            body.executeTransformations(physics);
        }
        if (shield != null && phase != Phase.BIRTH_CANAL && phase != Phase.UNDEATH) {
            shield.executeTransformations(physics);
        }
    }

    public SurfacePatch getSurfacePatch() {
        return surfacePatch;
    }

    public void setSurfacePatch(SurfacePatch surfacePatch) {
        this.surfacePatch = surfacePatch;
    }

    private void addLimbBuds() {
        List<Face.Pair> pairs = body.getFacePairs();
        Gene.Scan geneScan = genome.getGene("growth-buds").createScan();
        int limbSetCount = 2 + geneScan.choice(2);
        if (limbSetCount > pairs.size()) {
            limbSetCount = pairs.size();
        }
        LimbBud.Factory limbBudFactory = new LimbBud.Factory(energy.split(limbSetCount * 2));
        limbBudFactory.setFabric(body);
        for (int walk = 0; walk < limbSetCount; walk++) {
            int faceNumber = geneScan.choice(pairs.size());
            Face.Pair pair = pairs.get(faceNumber);
            pairs.remove(faceNumber);
            String geneName = "growth-limb-" + walk;
            pair.face0.setThing(limbBudFactory.createFresh(pair.face0, geneName));
            pair.face1.setThing(limbBudFactory.createFresh(pair.face1, geneName));
        }
        geneScan.destroy();
    }

    private void takeAStep() {
        List<Interval> muscles = new ArrayList<Interval>();
        for (Interval interval : body.getIntervals()) {
            if (interval.getRole() == Interval.Role.MUSCLE) {
                muscles.add(interval);
            }
        }
        Gene gene = getMovementGene(geometry.getDirection());
        Gene.Scan scan = gene.createScan();
        BitSet bits = scan.choices(muscles.size() * 3);
        int walk = 0;
        for (Interval muscle : muscles) {
            muscle.getSpan().perturbIdeal(
                    Constants.MUSCLE_DURATION,
                    contract(bits.get(walk * 3)),
                    contract(bits.get(walk * 3 + 1)),
                    contract(bits.get(walk * 3 + 2))
            );
            walk++;
        }
        scan.destroy();
    }

    private double contract(boolean contracted) {
        if (contracted) {
            consumeEnergy();
        }
        return contracted ? Constants.CONTRACTED : Constants.EXTENDED;
    }

    private void consumeEnergy() {
        energy.extract(Constants.CONTRACTION_ENERGY);
    }

    public void save(DataOutputStream dos) throws IOException {
        genome.write(dos);
        energy.write(dos);
        dos.writeUTF(id);
        dos.writeUTF(email);
        dos.writeUTF(getSpeech().getText());
        dos.writeByte(phase.ordinal());
        Fablob.packArrow(goal, dos);
        dos.writeUTF(preyName);
        if (shield != null) {
            Fablob shieldBlob = new Fablob(shield);
            dos.writeInt(shieldBlob.getBytes().length);
            dos.write(shieldBlob.getBytes());
        }
        else {
            dos.writeInt(-1);
        }
        dos.writeShort(trail.size());
        for (Arrow point : trail) {
            Fablob.packArrow(point, dos);
        }
        dos.writeLong(trailAge);
    }

    private void read(DataInputStream dis, Noise noise, Thing.Factory jointThingFactory) throws IOException {
        genome = Genome.read(dis, noise);
        energy = Energy.read(dis);
        id = dis.readUTF();
        email = dis.readUTF();
        speech = new Speech(dis.readUTF());
        phase = Phase.values()[dis.readByte()];
        Fablob.unpackArrow(dis, goal);
        preyName = dis.readUTF();
        int shieldLength = dis.readInt();
        if (shieldLength > 0) {
            byte[] blob = new byte[shieldLength];
            if (dis.read(blob) != shieldLength) {
                throw new IOException("Couldn't read shield blob");
            }
            Thing.Factory shieldFiller = jointThingFactory != null ? new ShieldFiller(jointThingFactory) : null;
            shield = new Fablob(blob).createFabric(shieldFiller);
        }
        int trailSize = dis.readShort();
        while (trailSize-- > 0) {
            Arrow point = new Arrow();
            Fablob.unpackArrow(dis, point);
            trail.add(point);
        }
        trailAge = dis.readLong();
    }

    public static Being create(Embryo embryo, Fabric body, Thing.Factory jointThingFactory, Outside outside) {
        Being being = new Being(body, outside);
        being.id = embryo.id;
        being.email = embryo.email;
        being.speech = embryo.speech;
        being.genome = embryo.genome;
        being.energy = embryo.limbEnergy;
        Thing.Factory shieldFiller = jointThingFactory != null ? new ShieldFiller(jointThingFactory) : null;
        being.shield = new TensegritySphereFactory(shieldFiller).setRelax(1.2).createSphere(3, Constants.MAX_SHIELD_RADIUS);
        being.trail = embryo.trail;
        return being;
    }

    public static Being restoreExisting(DataInputStream dis, Noise noise, Fabric fabric, Thing.Factory jointThingFactory, Outside outside) throws IOException {
        Being being = new Being(fabric, outside);
        being.read(dis, noise, jointThingFactory);
        return being;
    }

    Gene getMovementGene(Direction direction) {
        return genome.getGene("move-" + direction);
    }

    interface FaceGrowthBud {
        boolean run();

        void terminate();
    }

    private boolean growFaces() {
        boolean growing = false;
        for (Face face : body.getFaces()) {
            Thing thing = face.getThing();
            if (thing != null) {
                FaceGrowthBud faceGrowthBud = (FaceGrowthBud) thing;
                if (faceGrowthBud.run()) {
                    growing = true;
                }
                else {
                    faceGrowthBud.terminate();
                    face.setThing(null); // gone!
                }
            }
        }
        return growing;
    }

    public String toString() {
        return String.format("%s:%s", id, email);
    }

    public static String getId(String bodyName) {
        int colon = bodyName.indexOf(":");
        if (colon < 0) {
            throw new RuntimeException("Improper body name: " + bodyName);
        }
        return bodyName.substring(0, colon);
    }

    public static String getEmail(String bodyName) {
        int colon = bodyName.lastIndexOf(":");
        if (colon < 0) {
            throw new RuntimeException("Improper body name: " + bodyName);
        }
        return bodyName.substring(colon + 1);
    }
}