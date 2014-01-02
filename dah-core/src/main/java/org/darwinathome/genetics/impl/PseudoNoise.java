// ========= Copyright (C) 2009, 2010 Gerald de Jong =================
// This file is part of the Darwin at Home project, distributed
// under the GNU General Public License, version 3.
// You should have received a copy of this license in "license.txt",
// but if not, see http://www.gnu.org/licenses/gpl-3.0.txt.
// ===================================================================
package org.darwinathome.genetics.impl;

import org.darwinathome.genetics.Noise;
import org.uncommons.maths.random.MersenneTwisterRNG;

/**
 * An implementation of noise that uses the Mersenne Twister pseudorandom number.
 *
 * @author Gerald de Jong, Beautiful Code BV, <geralddejong@gmail.com>
 */

public class PseudoNoise implements Noise {
    private MersenneTwisterRNG random;
    private ClonableSeed seed;

    public PseudoNoise() {
        this.random = new MersenneTwisterRNG();
        this.seed = new ClonableSeed(random.getSeed(),0);
    }

    public PseudoNoise(Seed seed) {
        this.seed = (ClonableSeed)seed;
        this.random = new MersenneTwisterRNG(this.seed.bytes);
        int count = this.seed.count;
        this.seed.count = 0;
        while (this.seed.count < count) {
            nextDouble();
        }
    }

    public byte nextByte() {
        return (byte) ((int) (nextDouble() * 256) - 128);
    }

    public int choose(int count) {
        return (int) (nextDouble() * count);
    }

    public double nextDouble() {
        seed.count++;
        return random.nextDouble();
    }

    public Seed copySeed() {
        return seed.copy();
    }

    /**
     * Hold a seed so it can be persisted
     */

    public static class ClonableSeed implements Seed {
        public ClonableSeed(byte[] bytes, int count) {
            this.bytes = bytes;
            this.count = count;
        }
        public byte [] bytes;
        public int count;

        public Seed copy() {
            return new ClonableSeed(bytes, count);
        }
    }

}
