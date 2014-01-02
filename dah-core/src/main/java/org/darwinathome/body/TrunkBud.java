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
import java.util.BitSet;
import java.util.Queue;

/**
 * The watchmaker on a face that is executing a gene to build from it
 *
 * @author Gerald de Jong, Beautiful Code BV, <geralddejong@gmail.com>
 */

public class TrunkBud implements Thing, Being.FaceGrowthBud {
    private static final int TICKS_TO_IDEAL = 500;
    private Fabric fabric;
    private Face face;
    private String geneName;
    private Gene.Scan geneScan;
    private Energy energy;

    private TrunkBud(Fabric fabric, Face face, String geneName, Gene.Scan geneScan, Energy energy) {
        this.fabric = fabric;
        this.face = face;
        this.geneName = geneName;
        this.geneScan = geneScan;
        this.energy = energy;
    }

    public Fabric getFabric() {
        return fabric;
    }

    public boolean run() {
        boolean running = !energy.isEmpty();
        if (running) {
            BitSet bits = geneScan.choices(3);
            if (!(bits.get(0) || bits.get(1) || bits.get(2))) {
                bits.set(0, true);
            }
            boolean stayAlive = bits.get(0);
            boolean expandFace01 = bits.get(1);
            boolean expandFace20 = bits.get(2);
            OpenUp openUp = new OpenUp(face, 1, TICKS_TO_IDEAL, Interval.Role.SPRING);
            openUp.setCallback(new FacesMade(stayAlive, expandFace01, expandFace20));
            getFabric().addTransformation(openUp);
            energy.extract(Constants.GROWTH_COST);
        }
        return running;
    }

    public void terminate() {
        geneScan.destroy();
    }

    public void save(DataOutputStream dos) throws IOException {
        dos.writeUTF("Trunk"); // note: read by BudFactory
        dos.writeUTF(geneName);
        dos.writeShort(geneScan.getNumber());
        energy.write(dos);
    }

    public String toString() {
        return "TrunkBud("+geneName+";"+ energy +")";
    }

    public static class Factory implements Thing.Factory {
        private Queue<Energy> energies;
        private Fabric fabric;

        public Factory(Energy energy) {
            if (energy != null) {
                this.energies = energy.split(2);
            }
        }

        public void setFabric(Fabric fabric) {
            this.fabric = fabric;
        }

        public Thing createFresh(Object target, String geneName) {
            Face face = (Face)target;
            Being being = (Being) fabric.getThing();
            Gene gene = being.getGenome().getGene(geneName);
            return new TrunkBud(
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
            Integer scanNumber = (int) dis.readShort();
            Gene gene = being.getGenome().getGene(geneName);
            Energy energy = Energy.read(dis);
            return new TrunkBud(
                    fabric,
                    face,
                    gene.getName(),
                    gene.getScan(scanNumber),
                    energy
            );
        }
    }

    private class FacesMade implements OpenUp.Callback {
        private boolean stayAlive, expandFace01, expandFace20;

        private FacesMade(boolean stayAlive, boolean expandFace01, boolean expandFace20) {
            this.stayAlive = stayAlive;
            this.expandFace01 = expandFace01;
            this.expandFace20 = expandFace20;
        }

        public void faces(Face face01, Face face12, Face face20) {
            int budCount = (stayAlive?1:0) + (expandFace01?1:0) +(expandFace20?1:0);
            Queue<Energy> parts = energy.split(budCount);
            if (expandFace01) {
                face01.setThing(createChild(face01, geneName+"-01", parts.remove()));
            }
            if (expandFace20) {
                face20.setThing(createChild(face20, geneName+"-20", parts.remove()));
            }
            if (stayAlive) {
                energy = parts.remove();
            }
            else {
                terminate();
                face12.setThing(null); // that was us, but we're gone now
            }
            if (!parts.isEmpty()) {
                throw new RuntimeException("parts not empty");
            }
        }
    }

    private TrunkBud createChild(Face face, String geneName, Energy energy) {
        Being being = (Being) fabric.getThing();
        Gene gene = being.getGenome().getGene(geneName);
        return new TrunkBud(
                fabric,
                face,
                gene.getName(),
                gene.createScan(),
                energy
        );
    }

}