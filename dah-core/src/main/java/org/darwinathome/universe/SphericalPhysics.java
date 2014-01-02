// ========= Copyright (C) 2009, 2010 Gerald de Jong =================
// This file is part of the Darwin at Home project, distributed
// under the GNU General Public License, version 3.
// You should have received a copy of this license in "license.txt",
// but if not, see http://www.gnu.org/licenses/gpl-3.0.txt.
// ===================================================================
package org.darwinathome.universe;

import org.darwinathome.Constants;
import org.darwinathome.body.Being;
import org.darwinathome.body.JointSurface;
import org.darwinathome.geometry.math.Arrow;
import org.darwinathome.geometry.structure.Fabric;
import org.darwinathome.geometry.structure.Joint;
import org.darwinathome.geometry.structure.Physics;
import org.darwinathome.geometry.structure.PhysicsValue;

import java.util.ArrayList;
import java.util.List;

/**
 * A clever set of physics constraints which decides whether or not the triangle touched is land or water
 *
 * @author Gerald de Jong, Beautiful Code BV, <geralddejong@gmail.com>
 */

public class SphericalPhysics implements Physics.Constraints {
    private Arrow radial = new Arrow();
    private Arrow circumferencial = new Arrow();
    private PhysicsValue elasticFactor = new PhysicsValue("elasticFactor", 0.900000);
    private PhysicsValue radius = new PhysicsValue("radius", 0.100000);
    private PhysicsValue airGravity = new PhysicsValue("airGravity", -0.003500);
    private PhysicsValue airDamping = new PhysicsValue("airDamping", 0.000100);
    private PhysicsValue landGravity = new PhysicsValue("landGravity", 35.000000);
    private PhysicsValue waterGravity = new PhysicsValue("waterGravity", 3.000000);
    private PhysicsValue landDamping = new PhysicsValue("landDamping", 0.100000);
    private PhysicsValue waterDamping = new PhysicsValue("waterDamping", 0.001000);
    private PhysicsValue landFriction = new PhysicsValue("landFriction", 0.800000);
    private PhysicsValue waterFriction = new PhysicsValue("waterFriction", 0.010000);

    public List<PhysicsValue> getPhysicsValues() {
        List<PhysicsValue> values = new ArrayList<PhysicsValue>();

        values.add(elasticFactor);
        values.add(radius);

        values.add(airGravity);
        values.add(airDamping);

        values.add(landGravity);
        values.add(waterGravity);

        values.add(landDamping);
        values.add(waterDamping);

        values.add(landFriction);
        values.add(waterFriction);
        
        return values;
    }

    public PhysicsValue getElasticFactor() {
        return elasticFactor;
    }

    public void exertJointPhysics(Joint joint, Fabric fabric) {
        radial.set(joint.getLocation());
        double altitude = joint.setAltitude(radial.normalize() - Constants.SURFACE_RADIUS);
        Arrow velocity = joint.getVelocity();
        double rad = radius.get();
        if (altitude > rad) {
            joint.getGravity().set(radial, airGravity.get() * Constants.GRAVITY_FACTOR);
            velocity.scale(1 - airDamping.get());
        }
        else {
            SurfacePatch surfacePatch = getSurfacePatch(joint, fabric);
            double water = surfacePatch.getWaterLevel();
            circumferencial.set(velocity).sub(radial, velocity.dot(radial));
            double depth = (altitude < -rad) ? 1 : (altitude - rad) / (-2 * rad);
            velocity.scale(1 - interpolate(water, landDamping, waterDamping));
            double subsurfaceGravity = interpolate(water, landGravity, waterGravity);
            double gravityValue = interpolate(depth, airGravity.get() * Constants.GRAVITY_FACTOR, -airGravity.get() * Constants.GRAVITY_FACTOR * subsurfaceGravity);
            joint.getGravity().set(radial, gravityValue);
            velocity.sub(circumferencial, interpolate(water, landFriction, waterFriction) * depth);
        }
    }

    private SurfacePatch getSurfacePatch(Joint joint, Fabric fabric) {
        JointSurface jointSurface = (JointSurface) joint.getThing();
        if (jointSurface == null) {
            jointSurface = (JointSurface) fabric.getThingFactory().createFresh(joint, "SurfacePatch");
            joint.setThing(jointSurface);
        }
        SurfacePatch patch = jointSurface.getPatch(joint.getLocation());
        if (patch == null) {
            throw new RuntimeException("No patch found!");
        }
        return patch;
    }

    private static double interpolate(double water, Physics.Value landValue, Physics.Value waterValue) {
        return (1 - water) * landValue.get() + water * waterValue.get();
    }

    private static double interpolate(double proportion, double low, double high) {
        return (1 - proportion) * low + proportion * high;
    }

    public void postIterate(Fabric fabric) {
        Object thing = fabric.getThing();
        if (thing instanceof Being) {
            ((Being) thing).handleHanging();
        }
    }
}

