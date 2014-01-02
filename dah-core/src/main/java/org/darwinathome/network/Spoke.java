// ========= Copyright (C) 2009, 2010 Gerald de Jong =================
// This file is part of the Darwin at Home project, distributed
// under the GNU General Public License, version 3.
// You should have received a copy of this license in "license.txt",
// but if not, see http://www.gnu.org/licenses/gpl-3.0.txt.
// ===================================================================
package org.darwinathome.network;

import org.darwinathome.body.Speech;
import org.darwinathome.body.Target;
import org.darwinathome.universe.MultiverseEvolution;

/**
 * The universe on the client side
 *
 * @author Gerald de Jong, Beautiful Code BV, <geralddejong@gmail.com>
 */

public interface Spoke {

    /**
     * When things go wrong
     */

    public interface Notifier {
        void notify(String notification, int duration);
        void clearNotify();
        void fail(Failure fail);
    }

    /**
     * Who hears when things go wrong?
     * @param notifier they do
     */

    void setNotifier(Notifier notifier);

    /**
     * This must be called before things can proceed;
     *
     * @param bodyName who is logging in here
     * @param password the key
     * @param exchange report how it went
     */

    void authenticate(String bodyName, String password, Exchange exchange);

    /**
     * Asynchronously initiate
     *
     * @param whenReady what to run when it's initiated
     */

    void initiate(Runnable whenReady);

    /**
     * Set the state to something new.  Side effects happen.
     *
     * @param happy the next happy state or not?
     */

    void nextState(boolean happy);

    /**
     * Fetch a new world from the server and end up frozen
     */

    void reboot();

    /**
     * Fetch the speech above this critter's body
     *
     * @return the speech above
     */

    Speech getSpeech();

    /**
     * Set the sign above the body
     *
     * @param sign what will it hold
     */

    void setSpeech(String sign);

    /**
     * Set the target
     *
     * @param target what to focus on
     */
    
    void setTarget(Target target);

    /**
     * Get the current target.
     *
     * @return the current target
     */

    Target getTarget();

    /**
     * Access the existing evolving population of mutated clones of the
     * client being, or create that population if it doesn't exist yet.
     *
     * @return the evolving ones
     */

    MultiverseEvolution getEvolution();

    /**
     * Perform all the accumulated work which should be done in the thread that uses this interface
     */

    void runPendingJobs();
}
