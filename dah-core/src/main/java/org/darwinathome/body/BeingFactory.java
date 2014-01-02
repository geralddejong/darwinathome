// ========= Copyright (C) 2009, 2010 Gerald de Jong =================
// This file is part of the Darwin at Home project, distributed
// under the GNU General Public License, version 3.
// You should have received a copy of this license in "license.txt",
// but if not, see http://www.gnu.org/licenses/gpl-3.0.txt.
// ===================================================================
package org.darwinathome.body;

import org.apache.log4j.Logger;
import org.darwinathome.genetics.Genome;
import org.darwinathome.genetics.Noise;
import org.darwinathome.geometry.math.Arrow;
import org.darwinathome.geometry.structure.Fablob;
import org.darwinathome.geometry.structure.Fabric;
import org.darwinathome.geometry.structure.Face;
import org.darwinathome.geometry.structure.Interval;
import org.darwinathome.geometry.structure.Thing;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * The entire universe where it all happens
 *
 * @author Gerald de Jong, Beautiful Code BV, <geralddejong@gmail.com>
 */

public class BeingFactory {
    private Logger logger = Logger.getLogger(getClass());
    private Thing.Factory jointThingFactory;
    private Noise noise;
    private Being.Outside outside;

    public BeingFactory(Noise noise, Thing.Factory jointThingFactory, Being.Outside outside) {
        this.noise = noise;
        this.jointThingFactory = jointThingFactory;
        this.outside = outside;
    }

    public Being create(String email) {
        Speech speech = new Speech(email);
        StringBuilder id = new StringBuilder();
        for (int walk = 0; walk < 4; walk++) {
            id.append((char) ('A' + (int) (Math.random() * 26)));
        }
        return create(new Embryo(id.toString(), email, speech, new Genome(noise), null));
    }

    public Being create(Embryo embryo) {
        logger.info("Creating a being with "+ embryo);
        Fabric body = createFabric(new BodyFiller(embryo, noise, jointThingFactory, outside));
        return (Being) body.getThing();
    }

    public Being restore(DataInputStream dis) throws IOException {
        Fablob fablob = Fablob.read(dis);
        return (Being) fablob.createFabric(new BodyFiller(null, noise, jointThingFactory, outside)).getThing();
    }

    public void save(Being being, DataOutputStream dos) throws IOException {
        Fablob fablob = new Fablob(being.getBody());
        fablob.write(dos);
    }

    private Fabric createFabric(Thing.Factory thingFactory) {
        Fabric fabric = new Fabric(thingFactory);
        double radius = Math.sqrt(3.0 / 4.0) * 2 / 3;
        for (int walk = 0; walk < 3; walk++) {
            double angle = walk * Math.PI * 2 / 3;
            fabric.getJoints().add(
                    fabric.createJoint(
                            fabric.who().createMiddle(),
                            new Arrow(radius * Math.cos(angle), 0, radius + radius * Math.sin(angle))
                    )
            );
        }
        for (int walk = 0; walk < 3; walk++) {
            Interval interval = fabric.createInterval(fabric.getJoints().get(walk), fabric.getJoints().get((walk + 1) % 3), Interval.Role.SPRING);
            interval.getSpan().setIdeal(1, 0);
            fabric.getIntervals().add(interval);
        }
        for (Face.Order order : Face.Order.values()) {
            Face face = new Face(order);
            face.getJoints().addAll(fabric.getJoints());
            fabric.getFaces().add(face);
            if (fabric.getThingFactory() != null) {
                face.setThing(fabric.getThingFactory().createFresh(face, "growth-trunk"));
            }
        }
        return fabric;
    }

}