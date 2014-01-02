package org.darwinathome.body;

import org.darwinathome.Constants;
import org.darwinathome.geometry.math.Arrow;
import org.darwinathome.geometry.structure.Fabric;
import org.darwinathome.geometry.structure.Joint;

/**
 * The constantly recalculated orientation of the being
 *
 * @author Gerald de Jong <geralddejong@gmail.com>
 */

public class Geometry {
    private static final int SEED_SIZE = 3;
    private Being being;
    private Being.Outside outside;
    private long age = -1;
    private Arrow right = new Arrow(); // transient
    private Arrow arrow01 = new Arrow(); // transient
    private Arrow arrow12 = new Arrow(); // transient
    private Arrow outwards = new Arrow();
    private Arrow seedCenter = new Arrow();
    private Arrow bodyCenter = new Arrow();
    private Arrow toGoal = new Arrow();
    private Arrow shieldCenter = new Arrow();
    private Arrow forward = new Arrow();
    private Arrow toShieldCenter = new Arrow();
    private Direction direction = Direction.getClosest(1, 0);
    private double bodyRadius;
    private Being prey;

    public Geometry(Being being, Being.Outside outside) {
        this.being = being;
        this.outside = outside;
    }

    public Arrow getForward() {
        return forward;
    }

    public Arrow getRight() {
        return right;
    }

    public Direction getDirection() {
        return direction;
    }

    public Arrow getBodyCenter() {
        return bodyCenter;
    }

    public double getDistanceToGoal() {
        return bodyCenter.distanceTo(being.getGoal());
    }

    public double getBodyRadius() {
        return bodyRadius;
    }

    public Being getPrey() {
        return prey;
    }

    public void releasePrey() {
        prey = null;
    }

    public void invalidate() {
        this.age = 0;
    }

    public void findBodyCenter() {
        Fabric body = being.getBody();
        body.getCenter(bodyCenter);
    }

    public void refresh() {
        if (being.getBody() != null) {
            if (being.getBody().getAge() != age) {
                recalculate();
                age = being.getBody().getAge();
            }
        }
        else {
            recalculate();
        }
    }

    private void recalculate() {
        findBodyCenter();
        switch (being.getPhase()) {
            case KILLED:
                // joints are all screwed up so calculations will have to wait
                break;
            case BIRTH_CANAL:
                being.interpolateTrail(bodyCenter);
                Fabric shield = being.getShield();
                if (shield != null) {
                    shield.getCenter(seedCenter);
                    arrow01.sub(bodyCenter, seedCenter);
                    for (Joint joint : shield.getJoints()) {
                        joint.getLocation().add(arrow01);
                    }
                }
                bodyRadius = 0.1;
                break;
            default:
                double radius = outwards.set(bodyCenter).normalize();
                if (being.getSurfacePatch() == null || !being.getSurfacePatch().contains(bodyCenter)) {
                    being.setSurfacePatch(outside.getNearestPatch(bodyCenter));
                }
                being.addToTrail(bodyCenter);
                bodyRadius = being.getBody().getMaxDistanceFrom(bodyCenter);
                arrow01.sub(location(1), location(0));
                arrow12.sub(location(2), location(1));
                right.cross(arrow01, arrow12);
                right.sub(outwards, outwards.dot(right)).normalize(); // perpendicular to outwards
                forward.cross(right, outwards).normalize();
                double goalRadius = being.getGoal().span();
                if (goalRadius < Constants.SURFACE_RADIUS) {
                    toGoal.set(forward, Constants.MIN_GOAL_DISTANCE * 3);
                    being.getGoal().add(bodyCenter, toGoal);
                }
                being.getGoal().setSpan(radius);
                if (prey == null || !prey.toString().equals(being.getPreyName())) {
                    prey = outside.getBeing(being.getPreyName());
                }
                if (prey == null) {
                    toGoal.sub(being.getGoal(), bodyCenter);
                    double goalDistance = toGoal.span();
                    if (goalDistance < Constants.MIN_GOAL_DISTANCE) {
                        Arrow goal = null;
                        if (being.getEnergy().getAmount() > 0.5) {
                            goal = being.getSurfacePatch().getDriest(bodyCenter);
                        }
                        else {
                            goal = being.getSurfacePatch().getWettest(bodyCenter);
                        }
                        being.getGoal().set(goal);
                        being.getGoal().setSpan(radius);
                    }
                    else if (goalDistance > Constants.MAX_GOAL_DISTANCE) {
                        toGoal.scale(Constants.MAX_GOAL_DISTANCE / goalDistance);
                        being.getGoal().add(bodyCenter, toGoal);
                        being.clearPreyName();
                    }
                }
                else {
                    being.getGoal().set(prey.getGeometry().getBodyCenter());
                    toGoal.sub(prey.getGeometry().getBodyCenter(), bodyCenter);
                    double goalDistance = toGoal.span();
                    if (goalDistance > Constants.MAX_GOAL_DISTANCE) {
                        toGoal.scale(Constants.MAX_GOAL_DISTANCE / goalDistance);
                        being.getGoal().add(bodyCenter, toGoal);
                        being.clearPreyName();
                    }
                    if (prey.getShield() != null) {
                        being.clearPreyName();
                    }
                }
                toGoal.sub(being.getGoal(), bodyCenter);
                toGoal.normalize();
                toGoal.sub(outwards, toGoal.dot(outwards)); // perpendicular to outwards
                toGoal.normalize();
                direction = Direction.getClosest(toGoal.dot(forward), toGoal.dot(right));
                break;
        }
    }

    public void handlePrey() {
        if (being.getPreyName().isEmpty()) {
            prey = null;
        }
        else if (prey == null) {
            prey = outside.getBeing(being.getPreyName());
            if (prey == null) {
                being.clearPreyName(); // oh well, not found
            }
        }
        else if (!prey.toString().equals(being.getPreyName())) {
            prey = null;
        }
        else if (being.getPhase() == Being.Phase.ADULT_LIFE && prey.getPhase() == Being.Phase.ADULT_LIFE) {
            toGoal.sub(prey.getGeometry().getBodyCenter(), bodyCenter);
            double totalRadius = bodyRadius + prey.getGeometry().getBodyRadius();
            double preySeparation = (toGoal.span() - totalRadius) / totalRadius;
            if (preySeparation < 0) {
                if (being.getFitness() > prey.getFitness()) {
                    prey.bittenBy(being);
                }
                else {
                    being.bittenBy(prey);
                }
                prey = null;
            }
        }
    }

    public void handleHanging() {
        switch (being.getPhase()) {
            case CONCEPTION:
            case TRUNK_GROWTH:
            case JOINT_MERGE:
            case SELECT_LIMB_FACES:
            case LIMB_GROWTH:
                being.getShield().getCenter(shieldCenter);
                seedCenter.zero();
                for (int walk = 0; walk < SEED_SIZE; walk++) {
                    seedCenter.add(location(walk));
                }
                seedCenter.scale(1.0 / SEED_SIZE);
                toShieldCenter.sub(shieldCenter, seedCenter);
                outwards.set(shieldCenter).normalize();
                double upwards = outwards.dot(toShieldCenter);
                if (upwards < 0) { // don't push downwards ever
                    toShieldCenter.sub(outwards, upwards);
                }
                toShieldCenter.scale(0.05);
                for (int walk = 0; walk < SEED_SIZE; walk++) {
                    Joint joint = joint(walk);
                    joint.getLocation().add(toShieldCenter);
                    joint.getVelocity().scale(0.9);
                }
                break;
        }
    }

    private Arrow location(int index) {
        return joint(index).getLocation();
    }

    private Joint joint(int index) {
        return being.getBody().getJoints().get(index);
    }

    public String toString() {
        return being.toString();
    }
}
