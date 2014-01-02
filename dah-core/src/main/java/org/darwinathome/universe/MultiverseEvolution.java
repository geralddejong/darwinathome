// ========= Copyright (C) 2009, 2010 Gerald de Jong =================
// This file is part of the Darwin at Home project, distributed
// under the GNU General Public License, version 3.
// You should have received a copy of this license in "license.txt",
// but if not, see http://www.gnu.org/licenses/gpl-3.0.txt.
// ===================================================================
package org.darwinathome.universe;

import org.darwinathome.body.Being;
import org.darwinathome.genetics.Genome;

import java.util.Collection;

/**
 * The access that the outside world has to the evolution process
 *
 * @author Gerald de Jong, Beautiful Code BV, <geralddejong@gmail.com>
 */

public interface MultiverseEvolution {

    /**
     * Give the population an amount of time in which to evolve, returning whether or not there
     * is more work to do before a culling can take place
     *
     * @param iterations how many time sweeps
     * @return true if there is still more work to do
     */

    boolean experienceTime(int iterations);

    /**
     * End this particular generation, culling Constants.BIRTH_WAVE_SIZE of the worst
     * performers, and returning the best performer.
     *
     * @return the best performer
     */

    Competitor cull();

    /**
     * Figure out the size of the challenge
     *
     * @return the distance from where we started to the goal
     */

    double getDistanceToGoal();


    /**
     * Shut down this evolution, returning the genome of the best performer
     * @return the best performer's genome
     */

    Genome terminate();

    /**
     * Access the list of competitors, each of them mutated clones of the ancestor
     *
     * @return the list of competitors
     */

    Collection<? extends Competitor> getCompetitors();

    /**
     * Randomize all the movement genes of all competitors
     */

    void randomizeMovementGenes();

    /**
     * Raise the lifespan a little bit
     *
     * @return true if it's over the limit
     */

    boolean advanceLifespan();

    /**
     * Start froms scratch
     */

    void resetLifespan();

    /**
     * Whether the evolution has been terminated and can be discarded
     *
     * @return true if it's over
     */

    boolean isTerminated();

    /**
     * One of the competitors in the evolution
     */

    public interface Competitor {
        Being getBeing();
        double getTravelToGoal();
    }
}