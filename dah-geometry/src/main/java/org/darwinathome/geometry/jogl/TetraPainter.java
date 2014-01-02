/*
 * Copyright (C)2008 Gerald de Jong - GNU General Public License
 * please see the LICENSE.TXT in this distribution for more details.
 */
package org.darwinathome.geometry.jogl;

import org.darwinathome.geometry.math.Arrow;
import org.darwinathome.geometry.structure.Joint;
import org.darwinathome.geometry.structure.Tetra;

import javax.media.opengl.GL;
import java.util.List;

/**
 * Something to be seen in a GLViewPlatform will have to implement these functions.
 *
 * @author Gerald de Jong <geralddejong@gmail.com>
 */

public class TetraPainter {
    private static double PROPORTION = 0.7;
    private static float SHININESS = -1000.0f;
    private Tint AMBIENT_AND_DIFFUSE = new Tint(Tint.BLACK, Tint.WHITE,1f);
    private Tint SPECULAR = new Tint(Tint.BLACK, Tint.WHITE, 0.28f);
    private Arrow normal = new Arrow();
    private Arrow center = new Arrow();
    private Arrow a = new Arrow();
    private Arrow b = new Arrow();
    private Arrow c = new Arrow();
    private Arrow d = new Arrow();
    private GL gl;

    public void preVisit(GL gl) {
        this.gl = gl;
        gl.glMaterialfv(GL.GL_FRONT, GL.GL_SPECULAR, SPECULAR.getFloatArray(), 0);
        gl.glMaterialf(GL.GL_FRONT, GL.GL_SHININESS, SHININESS);
        gl.glEnable(GL.GL_LIGHTING);
        gl.glMaterialfv(GL.GL_FRONT, GL.GL_AMBIENT_AND_DIFFUSE, AMBIENT_AND_DIFFUSE.getFloatArray(), 0);
        gl.glBegin(GL.GL_TRIANGLES);
    }

    public void postVisit() {
        gl.glEnd();
    }

    public void visit(Tetra tetra) {
        List<Joint> j = tetra.getJoints();
        tetra.getLocation(center);
        a.interpolate(center, j.get(0).getLocation(), PROPORTION);
        b.interpolate(center, j.get(1).getLocation(), PROPORTION);
        c.interpolate(center, j.get(2).getLocation(), PROPORTION);
        d.interpolate(center, j.get(3).getLocation(), PROPORTION);
        if (!tetra.isClockwise()) {
            displayFace(c, b, a);
            displayFace(a, b, d);
            displayFace(c, d, b);
            displayFace(a, d, c);
        }
        else {
            displayFace(a, b, c);
            displayFace(d, b, a);
            displayFace(b, d, c);
            displayFace(c, d, a);
        }
    }

    private void displayFace(Arrow a, Arrow b, Arrow c) {
        normal.zero();
        normal.add(a);
        normal.add(b);
        normal.add(c);
        normal.scale(1/3.0);
        normal.sub(center);
        gl.glNormal3d(normal.x, normal.y, normal.z);
        gl.glVertex3d(a.x, a.y, a.z);
        gl.glVertex3d(b.x, b.y, b.z);
        gl.glVertex3d(c.x, c.y, c.z);
    }
}