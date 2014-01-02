// ========= Copyright (C) 2009, 2010 Gerald de Jong =================
// This file is part of the Darwin at Home project, distributed
// under the GNU General Public License, version 3.
// You should have received a copy of this license in "license.txt",
// but if not, see http://www.gnu.org/licenses/gpl-3.0.txt.
// ===================================================================
package org.darwinathome.body;

import org.darwinathome.Constants;
import org.darwinathome.genetics.Gene;
import org.darwinathome.geometry.structure.Fabric;
import org.darwinathome.geometry.structure.Face;
import org.darwinathome.geometry.structure.Interval;
import org.darwinathome.geometry.structure.Thing;
import org.darwinathome.geometry.transform.OpenUp;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Queue;

/**
 * The watchmaker on a face that is executing a gene to build from it
 *
 * @author Gerald de Jong, Beautiful Code BV, <geralddejong@gmail.com>
 */

public class LimbBud implements Thing, Being.FaceGrowthBud {
    private static final int TICKS_TO_IDEAL = 1000;
    private Fabric fabric;
    private Face face;
    private String geneName;
    private Gene.Scan geneScan;
    private Energy energy;

    private LimbBud(Fabric fabric, Face face, String geneName, Gene.Scan geneScan, Energy energy) {
        this.fabric = fabric;
        this.face = face;
        this.geneName = geneName;
        this.geneScan = geneScan;
        this.energy = energy;
    }

    public boolean run() {
        boolean running = !energy.isEmpty();
        if (running) {
            switch (geneScan.choice(3)) {
                case 0:
                    face.twist(true);
                    break;
                case 1:
                    face.twist(false);
                    break;
            }
            OpenUp openUp = new OpenUp(face, 1, TICKS_TO_IDEAL, Interval.Role.MUSCLE);
            openUp.setUseChirality(true);
            int size = 3 + geneScan.choice(3);
            for (int walk = 0; walk < size; walk++) {
                fabric.addTransformation(openUp);
            }
            energy.extract(size * Constants.GROWTH_COST);
        }
        return running;
    }

    public void terminate() {
        geneScan.destroy();
    }

    public void save(DataOutputStream dos) throws IOException {
        dos.writeUTF("Limb");  // note: read by BudFactory
        dos.writeUTF(geneName);
        dos.writeShort(geneScan.getNumber());
        energy.write(dos);
    }

    public String toString() {
        return "LimbBud("+geneName+";"+ energy +")";
    }

    public static class Factory {
        private Queue<Energy> energies;
        private Fabric fabric;

        public Factory(Queue<Energy> energies) {
            this.energies = energies;
        }

        public void setFabric(Fabric fabric) {
            this.fabric = fabric;
        }

        public Thing createFresh(Object target, String geneName) {
            Face face = (Face) target;
            Being being = (Being) fabric.getThing();
            Gene gene = being.getGenome().getGene(geneName);
            return new LimbBud(
                    fabric,
                    face,
                    geneName,
                    gene.createScan(),
                    energies.remove()
            );
        }

        public Thing restoreExisting(DataInputStream dis, Object target) throws IOException {
            Face face = (Face) target;
            Being being = (Being) fabric.getThing();
            String geneName = dis.readUTF();
            Integer geneScan = (int)dis.readShort();
            Energy energy = Energy.read(dis);
            return new LimbBud(
                    fabric,
                    face,
                    geneName,
                    being.getGenome().getGene(geneName).getScan(geneScan),
                    energy
            );
        }
    }
}