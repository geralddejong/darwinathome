// ========= Copyright (C) 2009, 2010 Gerald de Jong =================
// This file is part of the Darwin at Home project, distributed
// under the GNU General Public License, version 3.
// You should have received a copy of this license in "license.txt",
// but if not, see http://www.gnu.org/licenses/gpl-3.0.txt.
// ===================================================================
package org.darwinathome.genetics;

import org.apache.log4j.Logger;
import org.darwinathome.genetics.impl.EndlessGene;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * A genome holds a number of genes, logicaly one for each combination of enumeration objects given
 * in the fetch method.
 *
 * @author Gerald de Jong <geralddejong@gmail.com>
 */

public class Genome {
    private static Logger LOG = Logger.getLogger(Genome.class);
    private Noise noise;
    private Map<String, EndlessGene> geneMap = new TreeMap<String, EndlessGene>();

    public Genome(Noise noise) {
        this.noise = noise;
    }

    /**
     * Fetch the gene by this name
     *
     * @param key how to find the gene
     * @return a gene that can be read
     */

    public Gene getGene(String key) {
        EndlessGene gene = geneMap.get(key);
        if (gene == null) {
            geneMap.put(key, gene = new EndlessGene(key, noise));
        }
        return gene;
    }

    /**
     * Write the contents out to a stream
     *
     * @param dos where the data goes
     * @throws java.io.IOException oops
     */

    public void write(DataOutputStream dos) throws IOException {
        dos.writeShort(geneMap.size());
        for (EndlessGene gene : geneMap.values()) {
            gene.write(dos);
        }
    }

    public static Genome read(DataInputStream dis, Noise noise) throws IOException {
        Genome genome = new Genome(noise);
        int geneCount = dis.readShort();
        for (int walk = 0; walk < geneCount; walk++) {
            EndlessGene gene = EndlessGene.read(dis, noise);
            genome.geneMap.put(gene.getName(), gene);
        }
        return genome;
    }

    /**
     * Make one just like this one
     * @return the new one
     */

    public Genome copy() {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            write(new DataOutputStream(out));
            out.close();
            return read(new DataInputStream(new ByteArrayInputStream(out.toByteArray())), noise);
        }
        catch (IOException e) {
            throw new RuntimeException("Unable to copy genome", e);
        }
    }

    /**
     * How many genes in here?
     *
     * @return the count
     */

    public int size() {
        return geneMap.size();
    }

    /**
     * Mutate one of the genes with the given gene prefix with the chance given
     *
     * @param chanceOfMutation how much will change
     * @param genePrefix category of genes
     * @return this
     */

    public Genome mutate(double chanceOfMutation, String genePrefix) {
        List<EndlessGene> genes = new ArrayList<EndlessGene>();
        for (Map.Entry<String, EndlessGene> entry : geneMap.entrySet()) {
            if (entry.getKey().startsWith(genePrefix)) {
                genes.add(entry.getValue());
            }
        }
        if (genes.isEmpty()) {
            throw new RuntimeException("Cannot mutate");
        }
        int choice = (int)(noise.nextDouble() * genes.size());
        EndlessGene gene = genes.get(choice);
        LOG.info("mutating from:"+gene);
        gene.mutate(chanceOfMutation);
        LOG.info("mutating   to:"+gene);
        return this;
    }

    public String toString() {
         StringBuilder out = new StringBuilder("Genome {\n");
         for (Map.Entry<String, EndlessGene> entry : geneMap.entrySet()) {
             out.append("    ").append(entry.getKey()).append(" ==> ").append(entry.getValue()).append('\n');
         }
         out.append("}");
         return out.toString();
     }

}