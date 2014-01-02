// ========= Copyright (C) 2009, 2010 Gerald de Jong =================
// This file is part of the Darwin at Home project, distributed
// under the GNU General Public License, version 3.
// You should have received a copy of this license in "license.txt",
// but if not, see http://www.gnu.org/licenses/gpl-3.0.txt.
// ===================================================================
package org.darwinathome.client;

import org.darwinathome.geometry.jogl.PointOfView;
import org.darwinathome.geometry.math.Arrow;

/**
 * Manage a transition of the point of view to a new one;
 *
 * @author Gerald de Jong <geralddejong@gmail.com>
 */

public class LinearPovTransition {
    private long duration = 1000;
    private LinearTransition eyeTransition;
    private LinearTransition focusTransition;
    private PointOfView pointOfView;
    private PointOfView target = new PointOfView(1);
    private double upness;
    private long startTime, endTime;

    public LinearPovTransition(PointOfView pointOfView) {
        this.pointOfView = pointOfView;
        target.getEye().set(pointOfView.getEye());
        target.getFocus().set(pointOfView.getFocus());
        target.getUp().set(pointOfView.getUp());
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public Arrow getEye() {
        return target.getEye();
    }

    public Arrow getFocus() {
        return target.getFocus();
    }

    public Arrow getUp() {
        return target.getUp();
    }

    public void setUpness(double upness) {
        this.upness = upness;
    }

    public void setDistanceFromFocus(double distance) {
        target.setDistanceFromFocus(distance);
    }

    public void prepare() {
        target.update();
        eyeTransition = new LinearTransition(pointOfView.getEye(), target.getEye());
        focusTransition = new LinearTransition(pointOfView.getFocus(), target.getFocus());
        startTime = System.currentTimeMillis();
        endTime = startTime + duration;
    }

    public boolean step() {
        eyeTransition.set(false);
        focusTransition.set(false);
        if (upness > 0) {
            pointOfView.getUp().add(target.getUp(), upness * interpolate()).normalize();
        }
        pointOfView.update();
        return System.currentTimeMillis() < endTime;
    }

    private double interpolate() {
        long now = System.currentTimeMillis();
        double linear = ((double) (now - startTime)) / (endTime - startTime);
        return 0.5 * (1 - Math.cos(linear * Math.PI));
    }

    class LinearTransition {
        private Arrow from, to, arrow;

        public LinearTransition(Arrow arrow, Arrow to) {
            this.from = new Arrow(arrow);
            this.arrow = arrow;
            this.to = to;
        }

        public void set(boolean normalize) {
            arrow.interpolate(from, to, interpolate());
            if (normalize) {
                arrow.normalize();
            }
        }
    }
}