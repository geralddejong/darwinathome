// ========= Copyright (C) 2009, 2010 Gerald de Jong =================
// This file is part of the Darwin at Home project, distributed
// under the GNU General Public License, version 3.
// You should have received a copy of this license in "license.txt",
// but if not, see http://www.gnu.org/licenses/gpl-3.0.txt.
// ===================================================================
package org.darwinathome.network;

import org.darwinathome.body.Target;
import org.darwinathome.genetics.Genome;
import org.darwinathome.geometry.math.Arrow;

/**
 * The communication that can happen with the server.
 *
 * @author Gerald de Jong, Beautiful Code BV, <geralddejong@gmail.com>
 */

public interface Hub {

    String SESSION = "/{session}";
    String NO_SESSION = "no-session";
    String AUTHENTICATE_SERVICE = "/authenticate.service";
    String GET_WORLD_SERVICE = SESSION + "/get-world.service";
    String CREATE_BEING_SERVICE = SESSION + "/create-being.service";
    String SET_SPEECH_SERVICE = SESSION + "/set-speech.service";
    String GET_SPEECH_SINCE_SERVICE = SESSION + "/get-speech-since.service";
    String SET_GENOME_SERVICE = SESSION + "/set-genome.service";
    String SET_TARGET_SERVICE = SESSION + "/set-target.service";
    String DOCUMENTATION_SERVICE = SESSION + "/documentation.service";

    String PARAM_BODY_NAME = "bodyName";
    String PARAM_PASSWORD = "password";
    String PARAM_LOCATION_X = "locationX";
    String PARAM_LOCATION_Y = "locationY";
    String PARAM_LOCATION_Z = "locationZ";
    String PARAM_PREY_NAME = "preyName";
    String PARAM_TIME = "time";
    String PARAM_SPEECH = "speech";
    String PARAM_TITLE = "title";

    String SUCCESS = "success";
    String FAILURE = "failure: ";

    /**
     * Get permission to communicate with the hub
     *
     * @param email who are you?
     * @param password the key
     * @param exchange report what happened
     */

    void authenticate(String email, String password, Exchange exchange);

    /**
     * Fetch the universe for this body
     *
     * @param catcher  who will receive it when we've got it
     * @param exchange reporting when things have happened
     */

    void getWorld(CargoCatcher catcher, Exchange exchange);

    /**
     * Create a new being at a given location.
     *
     * @param location where
     * @param exchange the exchange reporting if it was successful
     */

    void createBeing(Arrow location, Exchange exchange);

    /**
     * Set one of the tags of the being
     *
     * @param speech   what is above them
     * @param exchange the exchange reporting if it was successful
     */

    void setSpeech(String speech, Exchange exchange);

    /**
     * Get the changes in speech that have happened since the given time
     *
     * @param time     since which iteration do we want the new speech
     * @param catcher  who gets the recent changes
     * @param exchange reporting when successful
     */

    void getSpeechSince(long time, CargoCatcher catcher, Exchange exchange);

    /**
     * Set the genes on a body
     *
     * @param genome   what are they
     * @param exchange the exchange reporting if it was successful
     */

    void setGenome(Genome genome, Exchange exchange);

    /**
     * Set the body's goal, not prey
     *
     * @param target where to point, maybe another body
     * @param cargoCatcher catch the world where the target has been set
     * @param exchange the exchange reporting if it was successful
     */

    void setTarget(Target target, CargoCatcher cargoCatcher, Exchange exchange);

    String getBaseUrl();

}