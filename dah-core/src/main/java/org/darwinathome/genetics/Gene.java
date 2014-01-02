// ========= Copyright (C) 2009, 2010 Gerald de Jong =================
// This file is part of the Darwin at Home project, distributed
// under the GNU General Public License, version 3.
// You should have received a copy of this license in "license.txt",
// but if not, see http://www.gnu.org/licenses/gpl-3.0.txt.
// ===================================================================
package org.darwinathome.genetics;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.BitSet;

/**
 * A Gene is a source of bits, revealed by acquiring a scan of the gene.  A scan can
 * be acquired with a Noise source, in which case it will continue to grow when further scanned,
 * or without, in which case it ends when it ends, returning a Boolean null.
 * <p/>
 * Genes can also copy themselves with a given probability of a bit being flipped in the copy.
 *
 * @author Gerald de Jong <geralddejong@gmail.com>
 */

public interface Gene {

    /*\
     *
     * @return
     */
    String getName();

    /**
     * Get a gene reader that will stop when the gene is finished
     *
     * @return a reader that will read the gene once
     */

    Scan createScan();

    /**
     * Get a gene reader that will keep reading forever
     *
     * @param number which one, so it can be retrieved again
     * @return a reader that will read the gene once
     */

    Scan getScan(Integer number);

    /**
     * Make mutations in this gene
     *
     * @param chanceOfMutation what is the chance that a bit will be mutated?
     * @return how many bits were mutated
     */

    int mutate(double chanceOfMutation);

    /**
     * Serialize
     *
     * @param dos the stream
     * @throws java.io.IOException oops
     */

    void write(DataOutputStream dos) throws IOException;

    /**
     * Give up on this gene entirely
     */

    void forget();


    /**
     * A Stream of information that can be extracted from a gene
     */

    public interface Scan {

        Integer getNumber();

        double interpolate(double low, double high, int divisions);

        int choice(int options);

        BitSet choices(int options);

        void destroy();
    }
}