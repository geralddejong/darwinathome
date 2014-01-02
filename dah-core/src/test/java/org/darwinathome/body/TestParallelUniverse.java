// ========= Copyright (C) 2009, 2010 Gerald de Jong =================
// This file is part of the Darwin at Home project, distributed
// under the GNU General Public License, version 3.
// You should have received a copy of this license in "license.txt",
// but if not, see http://www.gnu.org/licenses/gpl-3.0.txt.
// ===================================================================
package org.darwinathome.body;

import org.apache.log4j.Logger;
import org.darwinathome.Constants;
import org.darwinathome.genetics.Noise;
import org.darwinathome.genetics.impl.PseudoNoise;
import org.darwinathome.geometry.math.Arrow;
import org.darwinathome.geometry.structure.Joint;
import org.darwinathome.universe.World;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.Iterator;

import static org.junit.Assert.assertEquals;

/**
 * Make sure beings are marshalled properly
 *
 * @author Gerald de Jong, Beautiful Code BV, <geralddejong@gmail.com>
 */

public class TestParallelUniverse {
    private static final byte[] SEED_BYTES = {
            (byte) 0x8A, (byte) 0x98, (byte) 0xAF, (byte) 0x9B, (byte) 0x63, (byte) 0xBB, (byte) 0x98, (byte) 0x31,
            (byte) 0x62, (byte) 0x11, (byte) 0x3A, (byte) 0x71, (byte) 0x7F, (byte) 0x7E, (byte) 0x45, (byte) 0xAE
    };
    private static final Logger LOG = Logger.getLogger(TestParallelUniverse.class);

    @Test
    public void noiseTest() {
        Noise.Seed seed = new PseudoNoise.ClonableSeed(SEED_BYTES, 0);
        Noise noise0 = new PseudoNoise(seed);
        for (int walk = 0; walk < 100; walk++) {
            noise0.nextByte();
        }
        Noise noise1 = new PseudoNoise(noise0.copySeed());
        for (int walk = 0; walk < 100; walk++) {
            assertEquals("next byte ", noise0.nextByte(), noise1.nextByte());
        }
    }

    @Test
    public void parallelUniverses() throws Exception {
        for (int count = 1000; count < 30000; count += 1000) {
            LOG.info("\n\n\nkickstart " + count);
            PseudoNoise noise0 = new PseudoNoise(new PseudoNoise.ClonableSeed(SEED_BYTES, 0));
            World universe0 = new ParallelUniverse(noise0);
            Arrow location = new Arrow(1, 2, 3);
            location.setSpan(Constants.ROAM_RADIUS);
            Being being0 = universe0.createBeing(location, new Arrow().random(), "Gumby");
            for (int walk = 0; walk < count; walk++) {
                universe0.experienceTime(1);
                assertEquals("dead beings at " + walk, 0, universe0.getDeadBeings().size());
            }
            LOG.info("going to replicate after " + count);
            PseudoNoise noise1 = new PseudoNoise(noise0.copySeed());
            World universe1 = new ParallelUniverse(noise1);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(bos);
//            while (being0.getBody().hasTransformations()) {
//                universe0.experienceTime(1);
//                LOG.info("extra time");
//            }
            universe0.getBeingFactory().save(being0, dos);
            DataInputStream dis = new DataInputStream(new ByteArrayInputStream(bos.toByteArray()));
            Being being1 = universe1.getBeingFactory().restore(dis);
            String genome0 = being0.getGenome().toString();
            String genome1 = being1.getGenome().toString();
            assertEquals("genome", genome0, genome1);
            universe1.add(being1);
            assertEquals(being0.getBody().getJoints().size(), being1.getBody().getJoints().size());
//            if (being0.getShield() != null) {
//                Fablob shieldblob0 = new Fablob(being0.getShield());
//                Fablob shieldblob1 = new Fablob(being1.getShield());
//                assertArrayEquals(shieldblob0.getBytes(), shieldblob1.getBytes());
//            }
            LOG.info("replacated " + count);
            for (int walk = 0; walk < 1100; walk++) {
                assertEquals(being0.getEnergy().getAmount(), being1.getEnergy().getAmount(), 0);
                assertEquals("genome difference at walk=" + walk, being0.getGenome().toString(), being1.getGenome().toString());
                assertEquals("joint list size at iteration " + walk, being0.getBody().getJoints().size(), being1.getBody().getJoints().size());
                Iterator<Joint> joints0 = being0.getBody().getJoints().iterator();
                Iterator<Joint> joints1 = being1.getBody().getJoints().iterator();
                while (joints0.hasNext()) {
                    Joint joint0 = joints0.next();
                    Joint joint1 = joints1.next();
                    assertEquals("Velocities unequal "+walk, joint0.getVelocity(), joint1.getVelocity());
                    assertEquals("Locations unequal "+walk, joint0.getLocation(), joint1.getLocation());
                    assertEquals("Joints unequal "+walk, joint0, joint1);
                }
//                assertEquals(being0.getPurpose(), being1.getPurpose()); // todo: why does only this fail?
                universe0.experienceTime(1);
                universe1.experienceTime(1);
            }
            LOG.info("successful "+count);
        }
    }

    private class ParallelUniverse extends World {
        public ParallelUniverse(Noise noise) {
            super(noise);
        }
    }
}
