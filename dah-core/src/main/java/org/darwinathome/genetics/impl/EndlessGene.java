// ========= Copyright (C) 2009, 2010 Gerald de Jong =================
// This file is part of the Darwin at Home project, distributed
// under the GNU General Public License, version 3.
// You should have received a copy of this license in "license.txt",
// but if not, see http://www.gnu.org/licenses/gpl-3.0.txt.
// ===================================================================
package org.darwinathome.genetics.impl;

import org.darwinathome.genetics.Gene;
import org.darwinathome.genetics.Noise;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.BitSet;
import java.util.Map;
import java.util.TreeMap;

/**
 * This gene implementation stores its bits in a byte array, which automatically expands
 * with random bytes if more boolean bits are read.  It can only be created with a random
 * initial state, or by copying another existing one.
 *
 * @author Gerald de Jong <geralddejong@gmail.com>
 */

public class EndlessGene implements Gene {
//    private static Logger LOG = Logger.getLogger(EndlessGene.class);
    private Noise noise;
    private String name;
    private byte[] bytes;
    private Map<Integer, GeneScan> scans = new TreeMap<Integer, GeneScan>();

    public static EndlessGene read(DataInputStream dis, Noise noise) throws IOException {
        EndlessGene gene = new EndlessGene(dis.readUTF(), noise);
        int scanCount = dis.readByte();
        while (scanCount-- > 0) {
            gene.createScan(dis.readShort(), dis.readShort());
        }
        gene.bytes = new byte[dis.readShort()];
        if (gene.bytes.length > 0) {
            if (dis.read(gene.bytes) != gene.bytes.length) {
                throw new IOException("Missing bytes, expected " + gene.bytes.length);
            }
        }
        return gene;
    }

    public EndlessGene(String name, Noise noise) {
        this.name = name;
        this.noise = noise;
        this.bytes = new byte[0];
    }

    public String getName() {
        return name;
    }

    public GeneScan createScan() {
        int max = 0;
        for (Integer number : scans.keySet()) {
            if (max < number) {
                max = number;
            }
        }
        return getScan(max + 1);
    }

    public GeneScan getScan(Integer number) {
        GeneScan geneScan = scans.get(number);
        if (geneScan == null) {
            scans.put(number, geneScan = new GeneScan(number, -1));
        }
        return geneScan;
    }

    public int mutate(double chanceOfMutation) {
        if (bytes.length == 0) {
            return 0;
        }
        int mutationCount = (int) (chanceOfMutation * bytes.length * 8);
        if (mutationCount == 0) {
            mutationCount = 1; // gotta mutate something
        }
        int walk = mutationCount;
        while (walk-- > 0) {
            int position = noise.choose(bytes.length * 8);
            int bitPosition = position % 8;
            int bytePosition = position / 8;
            bytes[bytePosition] ^= (128 >> bitPosition);
        }
        return mutationCount;
    }

    public void write(DataOutputStream dos) throws IOException {
        dos.writeUTF(name);
        dos.writeByte(scans.size());
        for (GeneScan geneScan : scans.values()) {
            dos.writeShort(geneScan.number);
            dos.writeShort(geneScan.position);
        }
        dos.writeShort(bytes.length);
        dos.write(bytes);
    }

    public void forget() {
        this.bytes = new byte[0];
    }

    private boolean getBit(int bytePosition, int bitPosition) {
        return (bytes[bytePosition] & (128 >> bitPosition)) != 0;
    }

    public String toString() {
        StringBuffer out = new StringBuffer(bytes.length * 8);
        for (int walk = 0; walk < bytes.length; walk++) {
            for (int stroll = 0; stroll < 8; stroll++) {
                out.append(getBit(walk, stroll) ? "1" : "0");
            }
        }
        for (GeneScan geneScan : scans.values()) {
            out.append(' ').append(geneScan);
        }
        return out.toString();
    }

    void createScan(int number, int position) {
        GeneScan geneScan = new GeneScan(number, position);
        scans.put(geneScan.getNumber(), geneScan);
    }

    public void clearScans() {
        scans.clear();
    }

    private class GeneScan implements Scan {
        private Integer number;
        private int position = -1;

        private GeneScan(Integer number, int position) {
            this.number = number;
            this.position = position;
        }

        public Integer getNumber() {
            return number;
        }

        public double interpolate(double low, double high, int divisions) {
            int bits = divisionsToBits(divisions);
            double value = nextNuance(bits);
            return low * (1f - value) + high * value;
        }

        public int choice(int options) {
            int choice = (int) interpolate(0.0, (double) options, divisionsToBits(options));
            if (choice == options) choice--;
            return choice;
        }

        public BitSet choices(int options) {
            BitSet bitSet = new BitSet(options);
            for (int walk = 0; walk < options; walk++) {
                if (nextBit()) {
                    bitSet.set(walk);
                }
            }
            return bitSet;
        }

        public void destroy() {
            position = -1;
            EndlessGene.this.scans.remove(number);
        }

        private int divisionsToBits(int divisions) {
            int bits = 3;
            while (divisions > 0) {
                divisions >>= 1;
                bits++;
            }
            return bits;
        }

        private boolean nextBit() {
            position++;
            int bitPosition = position % 8;
            int bytePosition = position / 8;
            if (bytePosition >= bytes.length) {
                if (noise != null) {
                    byte[] newBytes = new byte[bytes.length + 1];
                    System.arraycopy(bytes, 0, newBytes, 0, bytes.length);
                    newBytes[bytes.length] = noise.nextByte();
                    bytes = newBytes;
//                    LOG.info(name+" made up a new byte, now "+bytes.length);
                }
                else {
                    throw new RuntimeException("Gene terminated unexpectedly");
                }
            }
            return getBit(bytePosition, bitPosition);
        }

        private double nextNuance(int bits) {
            long numerator = 0;
            long denominator = 0;
            while (bits-- > 0) {
                numerator <<= 1;
                denominator <<= 1;
                if (nextBit()) {
                    numerator++;
                }
                denominator++;
            }
            return (double) numerator / (double) denominator;
        }

        public String toString() {
            return "scan(" + number + "/" + position + ")";
        }
    }
}
