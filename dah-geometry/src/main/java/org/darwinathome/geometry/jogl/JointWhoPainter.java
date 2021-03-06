package org.darwinathome.geometry.jogl;

import javax.media.opengl.GL;
import org.darwinathome.geometry.math.Arrow;
import org.darwinathome.geometry.structure.Joint;

/**
 * Show who is who in joints
 *
 * @author Gerald de Jong, Beautiful Code BV, <geralddejong@gmail.com>
 */

public class JointWhoPainter {
    private PointOfView pointOfView;
    private TextureFont textureFont = new TextureFont();
    private GL gl;
    private Arrow forward = new Arrow();

    public JointWhoPainter(PointOfView pointOfView) {
        this.pointOfView = pointOfView;
        textureFont.setAnchor(0,-1);
        textureFont.setScale(2f);
    }

    public void preVisit(GL gl) {
        this.gl = gl;
        textureFont.ensureInitialized(gl);
    }

    public void visit(Joint joint) {
        textureFont.setLocation(joint.getLocation());
        forward.sub(joint.getLocation(), pointOfView.getEye());
        forward.normalize();
        textureFont.setOrientation(forward, pointOfView.getUp());
        textureFont.display(gl, joint.getWho().toString(), java.awt.Color.WHITE);
    }
}