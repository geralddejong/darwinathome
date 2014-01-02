package org.darwinathome.body;

import org.darwinathome.genetics.Gene;
import org.darwinathome.genetics.Genome;
import org.darwinathome.geometry.math.Arrow;

import java.util.LinkedList;
import java.util.List;

/**
 * All that you need to grow a new being for an existing genome
 *
 * @author Gerald de Jong <geralddejong@gmail.com>
 */

public class Embryo {
    public final String id;
    public final String email;
    public final Genome genome;
    public final Speech speech;
    public final LinkedList<Arrow> trail = new LinkedList<Arrow>();
    public final Energy trunkEnergy;
    public final Energy limbEnergy;

    public Embryo(String id, String email, Speech speech, Genome genome, List<Arrow> trail) {
        this.id = id;
        this.email = email;
        this.speech = speech;
        this.genome = genome;
        if (trail != null) {
            this.trail.addAll(trail);
        }
        Gene.Scan scan = genome.getGene("trunk-limb").createScan();
        double proportion = scan.interpolate(0.1, 0.4, 10);
        scan.destroy();
        this.trunkEnergy = new Energy(proportion);
        this.limbEnergy = new Energy(1-proportion);
    }

    public String toString() {
        return "Embryo("+ id +")";
    }
}
