// ========= Copyright (C) 2009, 2010 Gerald de Jong =================
// This file is part of the Darwin at Home project, distributed
// under the GNU General Public License, version 3.
// You should have received a copy of this license in "license.txt",
// but if not, see http://www.gnu.org/licenses/gpl-3.0.txt.
// ===================================================================
package org.darwinathome.genetics;

/**
 * Provide random bits in chunks of bits, or in terms of choosing among a given number
 * of options.
 *
 * @author Gerald de Jong <geralddejong@gmail.com>
 */

public interface Noise {

    /**
     * Read the next 8 bits from the source
     *
     * @return the next byte
     */

    byte nextByte();

    /**
     * Read the next 8 bits from the source
     *
     * @return the next byte
     */

    double nextDouble();

    /**
     * Choose a number between 0 and count-1
     *
     * @param count max
     * @return an int
     */

    int choose(int count);

    /**
     * fetch the persistent seed so things can continue on
     *
     * @return the seed that will reboot this noise here again
     */

    Seed copySeed();

    /**
     * Where does noise come from?
     */

    public interface Source {
        Noise getNoise();
    }

    /**
     * a copyable seed
     */

    public interface Seed {
        Seed copy();
    }

}