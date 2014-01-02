// ========= Copyright (C) 2009, 2010 Gerald de Jong =================
// This file is part of the Darwin at Home project, distributed
// under the GNU General Public License, version 3.
// You should have received a copy of this license in "license.txt",
// but if not, see http://www.gnu.org/licenses/gpl-3.0.txt.
// ===================================================================
package org.darwinathome.client;

import org.darwinathome.Constants;
import org.darwinathome.geometry.jogl.PointOfView;
import org.darwinathome.geometry.jogl.TextureTrianglePainter;
import org.darwinathome.geometry.math.Arrow;
import org.darwinathome.universe.SurfacePatch;

import javax.media.opengl.GL;
import java.util.List;

/**
 * Render the planet
 *
 * @author Gerald de Jong, Beautiful Code BV, <geralddejong@gmail.com>
 */

public class WorldPainter {
    private Arrow toEye = new Arrow();
    private Arrow toMiddle = new Arrow();
    private TextureTrianglePainter painter = new TextureTrianglePainter("/PlanetTexture.png");

    public void display(GL gl, List<SurfacePatch> surfacePatches, PointOfView pointOfView) {
        double minDot = 0.3;
        if (pointOfView != null) {
            toEye.set(pointOfView.getEye());
            double altitude = toEye.normalize();
            if (!Constants.Calc.isAboveHover(altitude)) {
                minDot = 0.8;
            }
        }
        painter.prePaint(gl, 0);
        for (SurfacePatch surfacePatch : surfacePatches) {
            setColor(gl, surfacePatch);
            List<Arrow> corners = surfacePatch.getCorners();
            Arrow middle = surfacePatch.getMiddle();
            toMiddle.set(middle).normalize();
            if (toMiddle.dot(toEye) > minDot) {
//                painter.paint(corners.get(0), corners.get(1), corners.get(2));
                painter.paint(corners.get(1), middle, corners.get(0));
                painter.paint(corners.get(2), middle, corners.get(1));
                painter.paint(corners.get(0), middle, corners.get(2));
            }
        }
        painter.postPaint();
    }

    private void setColor(GL gl, SurfacePatch surfacePatch) {
        float water = surfacePatch.getWaterLevel();
        if (water < 0) {
            water = 0;
        }
        else if (water > 1) {
            water = 1;
        }
        gl.glColor3f(0.0f, (1 - water) * 0.7f + 0.3f, water * 0.7f + 0.3f);
    }
}
