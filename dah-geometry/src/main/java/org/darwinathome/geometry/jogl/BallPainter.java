/*
 * Copyright (C)2008 Gerald de Jong - GNU General Public License
 * please see the LICENSE.TXT in this distribution for more details.
 */
package org.darwinathome.geometry.jogl;

import org.darwinathome.geometry.math.Arrow;

import javax.media.opengl.GL;
import javax.media.opengl.glu.GLU;
import javax.media.opengl.glu.GLUquadric;

/**
 * Paint a location
 *
 * @author Gerald de Jong <geralddejong@gmail.com>
 */

public class BallPainter {
    public static final double DEFAULT_WIDTH = 0.2;
    private static float SHININESS = -1000.0f;
    private Tint GREENISH = new Tint(Tint.BLACK, Tint.GREEN, 0.4f);
    private Tint REDDISH = new Tint(Tint.BLACK, Tint.RED, 0.6f);
    private Tint SPECULAR = new Tint(Tint.BLACK, Tint.WHITE, 0.28f);
    private double width = DEFAULT_WIDTH;
    private GLU glu = new GLU();
    private GLUquadric quadric = glu.gluNewQuadric();
    private boolean hilighted;

    public void setRadius(double width) {
        this.width = width;
    }

    public void setHilighted(boolean hilighted) {
        this.hilighted = hilighted;
    }

    public void paint(GL gl, Arrow arrow) {
        gl.glEnable(GL.GL_LIGHTING);
        gl.glMaterialfv(GL.GL_FRONT, GL.GL_SPECULAR, SPECULAR.getFloatArray(), 0);
        gl.glMaterialf(GL.GL_FRONT, GL.GL_SHININESS, SHININESS);
        if (hilighted) {
            gl.glMaterialfv(GL.GL_FRONT, GL.GL_AMBIENT_AND_DIFFUSE, REDDISH.getFloatArray(), 0);
        }
        else {
            gl.glMaterialfv(GL.GL_FRONT, GL.GL_AMBIENT_AND_DIFFUSE, GREENISH.getFloatArray(), 0);
        }
        gl.glPushMatrix();
        gl.glTranslated(arrow.x, arrow.y, arrow.z);
        gl.glScaled(width,width,width);
        glu.gluSphere(quadric,1,15,15);
        gl.glPopMatrix();
    }
}