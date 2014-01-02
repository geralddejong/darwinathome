package org.darwinathome.body;

import org.darwinathome.Constants;
import org.darwinathome.geometry.structure.Fabric;
import org.darwinathome.geometry.structure.Interval;
import org.darwinathome.geometry.structure.Joint;

import java.util.ArrayList;
import java.util.List;

/**
 * Transform this body into a dying version
 *
 * @author Gerald de Jong <geralddejong@gmail.com>
 */

public class DeathTransformation implements Fabric.Transformation {

    public void transform(Fabric fabric) {
        List<Joint> toRemove = new ArrayList<Joint>();
        for (Interval interval : fabric.getIntervals()) {
            toRemove.add(makeNewJoint(interval, fabric, false));
            toRemove.add(makeNewJoint(interval, fabric, true));
            interval.getSpan().setIdeal(interval.getSpan().getUltimateIdeal()/2, Constants.DYING_TIME);
        }
        for (Joint joint : toRemove) {
            fabric.getMods().getJointMod().remove(joint);
        }
    }

    private Joint makeNewJoint(Interval interval, Fabric fabric, boolean alpha) {
        Joint oldJoint = interval.get(alpha);
        Joint newJoint = fabric.createJoint(fabric.who().createAnotherLike(oldJoint.getWho()), oldJoint.getLocation());
        if (interval.replace(oldJoint, newJoint)) {
            throw new RuntimeException("No reason to be unable to replace");
        }
        fabric.getMods().getJointMod().add(newJoint);
        return oldJoint;
    }
}
