/*
 * Copyright (C)2008 Gerald de Jong - GNU General Public License
 * please see the LICENSE.TXT in this distribution for more details.
 */

package org.darwinathome.geometry.transform;

import org.darwinathome.geometry.math.Arrow;
import org.darwinathome.geometry.math.Space3;
import org.darwinathome.geometry.structure.Fabric;
import org.darwinathome.geometry.structure.Joint;

/**
 * @author Gerald de Jong <geralddejong@gmail.com>
 */

public class Relocator implements Fabric.Transformation {
    private Space3 matrix = new Space3();
    private Arrow origin;

    public Relocator(Arrow xAxis, Arrow yAxis, Arrow zAxis, Arrow origin) {
        matrix.set(xAxis.x, xAxis.y, xAxis.z, yAxis.x, yAxis.y, yAxis.z, zAxis.x, zAxis.y, zAxis.z);
        matrix.invert();
        this.origin = origin;
    }

    public void transform(Fabric fabric) {
        for (Joint joint : fabric.getJoints()) {
            matrix.transform(joint.getLocation());
            joint.getLocation().add(origin);
            joint.getVelocity().zero();
        }
    }
}