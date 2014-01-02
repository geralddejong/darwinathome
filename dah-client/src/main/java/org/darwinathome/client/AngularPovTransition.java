// ========= Copyright (C) 2009, 2010 Gerald de Jong =================
// This file is part of the Darwin at Home project, distributed
// under the GNU General Public License, version 3.
// You should have received a copy of this license in "license.txt",
// but if not, see http://www.gnu.org/licenses/gpl-3.0.txt.
// ===================================================================
package org.darwinathome.client;

import org.darwinathome.geometry.jogl.PointOfView;
import org.darwinathome.geometry.math.Arrow;
import org.darwinathome.geometry.math.Space3;

/**
 * Manage a transition of the point of view to a new one;
 *
 * @author Gerald de Jong <geralddejong@gmail.com>
 */

public class AngularPovTransition {
    private long durationPerRadian = 1000;
    private PointOfView pointOfView;
    private PointOfView original = new PointOfView(1);
    private long startTime, endTime;
    private Arrow eyeDestination = new Arrow();
    private Arrow axis = new Arrow();
    private Space3 rotation = new Space3();
    private double angle;

    public AngularPovTransition(PointOfView pointOfView) {
        this.pointOfView = pointOfView;
        this.original.getEye().set(pointOfView.getEye());
        this.original.getFocus().set(pointOfView.getFocus());
        this.original.getUp().set(pointOfView.getUp());
    }

    public void setDurationPerRadian(long durationPerRadian) {
        this.durationPerRadian = durationPerRadian;
    }

    public void setEyeDestination(Arrow eyeDestination) {
        this.eyeDestination.set(eyeDestination);
    }

    public void prepare() {
        axis.cross(pointOfView.getEye(), eyeDestination).normalize();
        angle = Math.acos(pointOfView.getEye().dot(eyeDestination) / pointOfView.getEye().span() / eyeDestination.span());
        startTime = System.currentTimeMillis();
        endTime = startTime + (long)(durationPerRadian * (Math.abs(angle) + 0.2));
    }

    public boolean step() {
        pointOfView.getEye().set(original.getEye());
        pointOfView.getFocus().set(original.getFocus());
        pointOfView.getUp().set(original.getUp());
        rotation.set(axis, angle * interpolate());
        rotation.transform(pointOfView.getEye());
        rotation.transform(pointOfView.getFocus());
        rotation.transform(pointOfView.getUp());
        pointOfView.update();
        return System.currentTimeMillis() < endTime;
    }

    private double interpolate() {
        long now = System.currentTimeMillis();
        double linear = ((double) (now - startTime)) / (endTime - startTime);
        return 0.5 * (1 - Math.cos(linear * Math.PI));
    }
}