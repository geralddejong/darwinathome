// ========= Copyright (C) 2009, 2010 Gerald de Jong =================
// This file is part of the Darwin at Home project, distributed
// under the GNU General Public License, version 3.
// You should have received a copy of this license in "license.txt",
// but if not, see http://www.gnu.org/licenses/gpl-3.0.txt.
// ===================================================================
package org.darwinathome.body;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;

/**
 * A place to store energy
 *
 * @author Gerald de Jong, Beautiful Code BV, <geralddejong@gmail.com>
 */

public class Energy {
    private double amount;

    public Energy(double amount) {
        this.amount = amount;
    }

    public void add(double energy) {
        amount += energy;
    }

    public double extract(double energy) {
        this.amount -= energy;
        return energy;
    }

    public double getAmount() {
        return amount;
    }

    public boolean isEmpty() {
        return amount < 0;
    }

    public void write(DataOutputStream dos) throws IOException {
        dos.writeDouble(amount);
    }

    public static Energy read(DataInputStream dis) throws IOException {
        return new Energy(dis.readDouble());
    }

    public String toString() {
        return "Energy("+amount+")";
    }

    public Queue<Energy> split(int pieces) {
        double part = amount / pieces;
        Queue<Energy> parts = new LinkedList<Energy>();
        amount = 0;
        for (int walk = 0; walk < pieces; walk++) {
            parts.add(new Energy(part));
        }
        return parts;
    }
}
