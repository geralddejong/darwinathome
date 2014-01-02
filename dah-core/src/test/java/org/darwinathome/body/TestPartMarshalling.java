// ========= Copyright (C) 2009, 2010 Gerald de Jong =================
// This file is part of the Darwin at Home project, distributed
// under the GNU General Public License, version 3.
// You should have received a copy of this license in "license.txt",
// but if not, see http://www.gnu.org/licenses/gpl-3.0.txt.
// ===================================================================
package org.darwinathome.body;

import junit.framework.Assert;
import org.darwinathome.genetics.Gene;
import org.darwinathome.genetics.Genome;
import org.darwinathome.genetics.Noise;
import org.darwinathome.genetics.impl.EndlessGene;
import org.darwinathome.genetics.impl.PseudoNoise;
import org.junit.Test;

import java.io.*;

/**
 * TODO: javadoc
 *
 * @author Gerald de Jong, Beautiful Code BV, <geralddejong@gmail.com>
 */
public class TestPartMarshalling {


    private static final int BITS = 1000;

    private enum Numbers {
        ONE, TWO, THREE
    }

    private enum Greek {
        ALPHA, OMEGA
    }

    @Test
    public void saveRestoreGenome() throws Exception {
        final Noise noise = new PseudoNoise();
        Genome genomeA = new Genome(noise);
        for (Numbers number : Numbers.values()) {
            Gene gene = genomeA.getGene(number.toString());
            Gene.Scan scan = gene.createScan();
            for (int walk = 0; walk < BITS; walk++) {
                scan.interpolate(0, 1, 1);
            }
            scan.destroy();
            for (Greek greek : Greek.values()) {
                gene = genomeA.getGene(number + ":" + greek);
                scan = gene.createScan();
                for (int walk = 0; walk < BITS; walk++) {
                    scan.interpolate(0, 1, 1);
                }
                scan.destroy();
            }
        }
        Assert.assertEquals(9, genomeA.size());
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        genomeA.write(new DataOutputStream(os));
        byte[] bytes = os.toByteArray();
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(bytes));
        Genome genomeB = Genome.read(dis, noise);
        for (Numbers number : Numbers.values()) {
            Gene geneA = genomeA.getGene(number.toString());
            Gene.Scan scanA = geneA.createScan();
            Gene geneB = genomeB.getGene(number.toString());
            Gene.Scan scanB = geneB.createScan();
            for (int walk = 0; walk < BITS; walk++) {
                Assert.assertEquals(scanA.interpolate(0, 1, 1), scanB.interpolate(0, 1, 1));
            }
            for (Greek greek : Greek.values()) {
                geneA = genomeA.getGene(number + ":" + greek);
                scanA = geneA.createScan();
                geneB = genomeB.getGene(number + ":" + greek);
                scanB = geneB.createScan();
                for (int walk = 0; walk < BITS; walk++) {
                    Assert.assertEquals(scanA.interpolate(0, 1, 1), scanB.interpolate(0, 1, 1));
                }
            }
        }

    }

    @Test
    public void saveRestoreGenomeALittleHarder() throws Exception {
        final Noise noise = new PseudoNoise();
        Genome genomeA = new Genome(noise);
        for (Numbers number : Numbers.values()) {
            Gene gene = genomeA.getGene(number.toString());
            Gene.Scan scan = gene.createScan();
            for (int walk = 0; walk < BITS; walk++) {
                scan.interpolate(0, 1, 1);
            }
            scan.destroy();
        }
        Assert.assertEquals(Numbers.values().length, genomeA.size());
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        genomeA.write(new DataOutputStream(os));
        byte[] bytes = os.toByteArray();
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(bytes));
        Genome genomeB = Genome.read(dis, noise);
        Assert.assertEquals(genomeA.size(), genomeB.size());
        for (Numbers number : Numbers.values()) {
            Gene geneA = genomeA.getGene(number.toString());
            Gene.Scan scanA = geneA.createScan();
            Gene geneB = genomeB.getGene(number.toString());
            Gene.Scan scanB = geneB.createScan();
            for (int walk = 0; walk < BITS; walk++) {
                Assert.assertEquals(scanA.interpolate(0, 1, 1), scanB.interpolate(0, 1, 1));
            }
        }
    }

    @Test
    public void saveRestoreGenomeSimple() throws Exception {
        final Noise noise = new PseudoNoise();
        Numbers number = Numbers.ONE;
        Genome genomeA = new Genome(noise);
        Gene gene = genomeA.getGene(number.toString());
        Gene.Scan scan = gene.createScan();
        for (int walk = 0; walk < BITS; walk++) {
            scan.interpolate(0, 1, 1);
        }
        scan.destroy();
        Assert.assertEquals(1, genomeA.size());
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        genomeA.write(new DataOutputStream(os));
        byte[] bytes = os.toByteArray();
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(bytes));
        Genome genomeB = Genome.read(dis, noise);
        Gene geneA = genomeA.getGene(number.toString());
        Gene.Scan scanA = geneA.createScan();
        Gene geneB = genomeB.getGene(number.toString());
        Gene.Scan scanB = geneB.createScan();
        for (int walk = 0; walk < BITS; walk++) {
            Assert.assertEquals(scanA.interpolate(0, 1, 1), scanB.interpolate(0, 1, 1));
        }
    }

    @Test
    public void saveRestoreGene() throws IOException {
        Gene geneA = new EndlessGene("Genie", new PseudoNoise());
        Gene.Scan scanA = geneA.createScan();
        for (int walk = 0; walk < BITS; walk++) {
            scanA.interpolate(0, 1, 1);
        }
        scanA.destroy();
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        geneA.write(new DataOutputStream(os));
        byte[] bytes = os.toByteArray();
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(bytes));
        Gene geneB = EndlessGene.read(dis, new PseudoNoise());
        Gene.Scan scanB = geneB.createScan();
        for (int walk = 0; walk < BITS; walk++) {
            Assert.assertEquals(scanA.interpolate(0, 1, 1), scanB.interpolate(0, 1, 1));
        }

    }
}
