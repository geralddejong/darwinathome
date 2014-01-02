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
import org.darwinathome.body.Direction;
import org.darwinathome.body.Target;
import org.darwinathome.genetics.Genome;
import org.darwinathome.geometry.math.Arrow;
import org.darwinathome.geometry.structure.Fablob;
import org.darwinathome.geometry.structure.Fabric;
import org.darwinathome.universe.MultiverseEvolution;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * The universe on the client side
 *
 * @author Gerald de Jong, Beautiful Code BV, <geralddejong@gmail.com>
 */

public class MultiverseEvolutionImpl implements MultiverseEvolution {
    private Logger log = Logger.getLogger(getClass());
    private Core core;
    private long lifespan = Constants.MIN_LIFESPAN;
    private Being ancestor;
    private Fablob ancestorBlob;
    private long ageFrozen;
    private List<CompetitorImpl> competitors = new ArrayList<CompetitorImpl>();
    private boolean terminated;
    private Arrow bodyCenter;
    private Arrow toGoal;
    private double distanceToGoal;
    private Target target;

    public MultiverseEvolutionImpl(Being ancestor, Core core) {
        this.toGoal = new Arrow().sub(ancestor.getGoal(), ancestor.getGeometry().getBodyCenter());
        this.distanceToGoal = toGoal.normalize();
        this.bodyCenter = ancestor.getGeometry().getBodyCenter();
        this.target = new Target(ancestor.getGoal(), ancestor.getPreyName());
        this.ancestor = ancestor;
        this.core = core;
        this.core.progressEvolution(Constants.MIN_LIFESPAN, 0, 0);
        freezeAncestor();
    }

    public boolean experienceTime(int iterations) {
        if (competitors.isEmpty()) {
            CompetitorImpl competitor = new CompetitorImpl((Being) ancestorBlob.createFabric(ancestor.getBody().getThingFactory()).getThing());
            competitors.add(competitor);
            createCompetitors(Constants.BIRTH_WAVE_SIZE);
        }
        core.getPhysics().setIterations(iterations);
        boolean finished = true;
        Iterator<CompetitorImpl> iterator = competitors.iterator();
        while (iterator.hasNext()) {
            CompetitorImpl competitor = iterator.next();
            if (!competitor.isFinished()) {
                try {
                    competitor.experienceTime();
                    finished = false;
                }
                catch (Exception e) {
                    iterator.remove();
                }
            }
        }
        if (finished && competitors.size() < Constants.POPULATION_SIZE) {
            createCompetitors(Constants.BIRTH_WAVE_SIZE);
            finished = false;
        }
        return !finished;
    }

    public Genome terminate() {
        core.terminateEvolution();
        terminated = true;
        return getBestGenome();
    }

    public Collection<? extends Competitor> getCompetitors() {
        return competitors;
    }

    public Competitor cull() {
        rankOnProximity();
        CompetitorImpl bestCompetitor = competitors.get(competitors.size() - 1);
        while (competitors.size() > Constants.POPULATION_SIZE - Constants.BIRTH_WAVE_SIZE) {
            competitors.remove(0);
        }
        return bestCompetitor;
    }

    @Override
    public double getDistanceToGoal() {
        return distanceToGoal;
    }

    public boolean advanceLifespan() {
        long longer = (long) (this.lifespan * 1.1);
        if (longer >= Constants.MAX_LIFESPAN) {
            return true;
        }
        setLifespan(longer);
        return false;
    }

    public void resetLifespan() {
        setLifespan(0);
    }

    public boolean isTerminated() {
        return terminated;
    }

    public void randomizeMovementGenes() {
        for (CompetitorImpl competitor : competitors) {
            competitor.randomizeMovementGenes();
        }
        resetLifespan();
    }

    // from here on private

    private Genome getBestGenome() {
        if (competitors.isEmpty()) {
            throw new RuntimeException("No competitors");
        }
        rankOnProximity();
        return competitors.get(competitors.size() - 1).getBeing().getGenome();
    }

    private void setLifespan(long lifespan) {
        double topSpeed = 0;
        double averageSpeed = 0;
        int competitorCount = 0;
        for (CompetitorImpl competitor : competitors) {
            if (competitor.isFinished()) {
                competitorCount++;
                double speed = competitor.getSpeed();
                averageSpeed += speed;
                if (speed > topSpeed) {
                    topSpeed = speed;
                }
            }
        }
        if (competitorCount > 0) {
            averageSpeed /= competitorCount;
        }
        if (lifespan < Constants.MIN_LIFESPAN) {
            lifespan = Constants.MIN_LIFESPAN;
        }
        else if (lifespan > Constants.MAX_LIFESPAN) {
            lifespan = Constants.MAX_LIFESPAN;
        }
        if (lifespan < this.lifespan || lifespan == Constants.MIN_LIFESPAN) {
            advanceAncestor();
            freezeAncestor();
            for (CompetitorImpl competitor : competitors) {
                competitor.reboot();
            }
            core.startEvolution();
        }
        this.lifespan = lifespan;
        core.progressEvolution(lifespan, averageSpeed, topSpeed);
    }

    private void advanceAncestor() {
        long startAge = ancestor.getBody().getAge();
        while (ancestor.getBody().getAge() < startAge + Constants.LIFESPAN_ADVANCE) {
            ancestor.getBody().executeTransformations(core.getPhysics());
            ancestor.getGeometry().refresh();
            ancestor.experienceTime(core.getPhysics());
        }
        bodyCenter = ancestor.getGeometry().getBodyCenter();
        target = new Target(ancestor.getGoal());
    }

    private void freezeAncestor() {
        this.ancestorBlob = new Fablob(ancestor.getBody());
        this.ageFrozen = ancestor.getBody().getAge();
    }

    private Being createAncestorClone() {
        Fabric cloneFabric = ancestorBlob.createFabric(ancestor.getBody().getThingFactory());
        Being being = (Being) cloneFabric.getThing();
        being.setVirtual(true);
        return being;
    }

    private void createCompetitors(int count) {
        while (count-- > 0) {
            int parent = (int) (Math.random() * competitors.size());
            CompetitorImpl mutatedClone = competitors.get(parent).createMutatedClone(Constants.CHANCE_OF_MUTATION);
            competitors.add(mutatedClone);
        }
    }

    private void rankOnProximity() {
        for (CompetitorImpl competitor : competitors) {
            competitor.calculateTripToGoal();
        }
        Collections.sort(competitors);
    }

    public class CompetitorImpl implements Comparable<CompetitorImpl>, Competitor {
        private Set<Direction> activeDirections = EnumSet.noneOf(Direction.class);
        private Arrow meToGoal = new Arrow();
        private Being being;
        private double travelToGoal;
        private double speed;

        public CompetitorImpl(Being being) {
            this.being = being;
            this.being.getGeometry().refresh();
            this.activeDirections.add(being.getGeometry().getDirection());
        }

        @Override
        public Being getBeing() {
            return being;
        }

        @Override
        public double getTravelToGoal() {
            return travelToGoal;
        }

        public void reboot() {
            being = createClone();
        }

        public boolean isFinished() {
            return getAge() > lifespan;
        }

        public void experienceTime() {
            being.getBody().executeTransformations(core.getPhysics());
            being.getGeometry().refresh();
            activeDirections.add(being.getGeometry().getDirection());
            being.experienceTime(core.getPhysics());
            being.setTarget(target);
        }

        public CompetitorImpl createMutatedClone(double chanceOfMutation) {
            Being clone = createClone();
            for (Direction direction : activeDirections) {
                clone.mutateDirectionGene(direction, chanceOfMutation);
            }
            return new CompetitorImpl(clone);
        }

        public void randomizeMovementGenes() {
            for (Direction direction : activeDirections) {
                being.randomizeMovementGene(direction);
            }
            reboot();
        }

        public long getAge() {
            return being.getBody().getAge() - ageFrozen;
        }

        public void calculateTripToGoal() {
            long age = getAge();
            if (age > 0) {
                speed = bodyCenter.distanceTo(being.getGeometry().getBodyCenter()) * Constants.ITERATIONS_PER_HOUR / age;
            }
            else {
                speed = 0;
            }
            double distance = meToGoal.sub(target.getLocation(), being.getGeometry().getBodyCenter()).normalize();
            double cos = meToGoal.dot(toGoal);
            this.travelToGoal = distance * (cos + Constants.SLOPE_TOWARDS_ORIGINAL_PATH * Math.sqrt(1 - cos * cos));
        }

        public int compareTo(CompetitorImpl competitor) {
            if (travelToGoal > competitor.travelToGoal) {
                return -1;
            }
            else if (travelToGoal < competitor.travelToGoal) {
                return 1;
            }
            else {
                return 0;
            }
        }

        public double getSpeed() {
            return speed;
        }

        private Being createClone() {
            Being cloneBeing = createAncestorClone();
            cloneBeing.setGenome(being.getGenome().copy());
            return cloneBeing;
        }

    }


}