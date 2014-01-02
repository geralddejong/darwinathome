// ========= Copyright (C) 2009, 2010 Gerald de Jong =================
// This file is part of the Darwin at Home project, distributed
// under the GNU General Public License, version 3.
// You should have received a copy of this license in "license.txt",
// but if not, see http://www.gnu.org/licenses/gpl-3.0.txt.
// ===================================================================

package org.darwinathome.server.persistence;

import org.darwinathome.body.Target;
import org.darwinathome.genetics.Genome;
import org.darwinathome.genetics.Noise;
import org.darwinathome.geometry.math.Arrow;
import org.darwinathome.universe.SpeechChange;

import java.util.List;

/**
 * We deal with the world history via this interface
 *
 * @author Gerald de Jong, Beautiful Code BV, <geralddejong@gmail.com>
 */

public interface WorldHistory {

    Frozen getFrozenWorld();

    void createBeing(String bodyName, Arrow location, Arrow gaze);

    void setSpeech(String bodyName, String speech);

    List<SpeechChange> getSpeechSince(String bodyName, long time);

    void setGenome(String bodyName, Genome genome);

    Frozen setTarget(String bodyName, Target target);

    boolean beingExists(String bodyName);

    Noise getNoise();

    void advanceMinutes(int hours);

    public class Frozen {
        private byte[] world;
        private long age;

        public Frozen(byte[] world, long age) {
            this.world = world;
            this.age = age;
        }

        public byte[] getWorld() {
            return world;
        }

        public long getAge() {
            return age;
        }
    }
}
