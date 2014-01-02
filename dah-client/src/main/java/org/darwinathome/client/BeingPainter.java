// ========= Copyright (C) 2009, 2010 Gerald de Jong =================
// This file is part of the Darwin at Home project, distributed
// under the GNU General Public License, version 3.
// You should have received a copy of this license in "license.txt",
// but if not, see http://www.gnu.org/licenses/gpl-3.0.txt.
// ===================================================================
package org.darwinathome.client;

import org.darwinathome.Constants;
import org.darwinathome.body.Being;
import org.darwinathome.body.Direction;
import org.darwinathome.body.Target;
import org.darwinathome.geometry.jogl.FabricPainter;
import org.darwinathome.geometry.jogl.Font3d;
import org.darwinathome.geometry.jogl.PointOfView;
import org.darwinathome.geometry.jogl.TextureTrianglePainter;
import org.darwinathome.geometry.jogl.Tint;
import org.darwinathome.geometry.math.Arrow;
import org.darwinathome.geometry.structure.Span;

import javax.media.opengl.GL;

/**
 * Render the being in various situations correctly
 *
 * @author Gerald de Jong, Beautiful Code BV, <geralddejong@gmail.com>
 */

public class BeingPainter {
    private static final double CROSSHAIR_SIZE = 20;
    private static final Tint TRAIL_TINT_SUPERIOR = Tint.RED;
    private static final Tint TRAIL_TINT_NORMAL = new Tint(Tint.WHITE, Tint.BLACK, 0.3f);
    private static final Tint DRINKING_TINT = Tint.CYAN;
    private static final Tint PURSUE_TINT = Tint.YELLOW;
    private static final Tint PURSUE_TINT_TARGET = Tint.RED;

    private Arrow beingLocation = new Arrow();
    private Arrow closestLocation = new Arrow();
    private Arrow toEye = new Arrow();
    private Arrow end = new Arrow();

    private PointOfView pointOfView;
    private Core core;
    private Arrow textLocation = new Arrow();
    private Font3d font3d = new Font3d();
    private FramePainter framePainter = new FramePainter();
    private PointerPainter pointerPainter = new PointerPainter();
    private TrailPainter trailPainter = new TrailPainter();
    private FabricPainter fabricPainter;

    public BeingPainter(Span.StressRange stressRange, PointOfView pointOfView, Core core) {
        this.pointOfView = pointOfView;
        this.core = core;
        this.fabricPainter = new FabricPainter(stressRange);
        font3d.setScale(10f);
    }

    public void paintBeing(GL gl, Being being, Target target) {
        if (being.getBody().getJoints().isEmpty()) {
            trailPainter.paint(gl, being, true, false);
            fabricPainter.paintLines(being.getShield(), gl, null);
        }
        else {
            textLocation.set(being.getGeometry().getBodyCenter());
            textLocation.setSpan(textLocation.span() + 2);
            font3d.setLocation(textLocation);
            font3d.setOrientation(pointOfView.getGaze(), pointOfView.getUp());
            fabricPainter.paintTexture(being.getBody(), gl);
            if (being.getShield() != null) {
                fabricPainter.paintLines(being.getShield(), gl, null);
            }
            if (being == core.getBeing()) {
                if (target != null && !being.isTargetSame(target)) {
                    paintBeingPointer(gl, being, target.getLocation());
                }
                if (being.getShield() == null) {
                    framePainter.paint(gl, being);
                    Being prey = being.getGeometry().getPrey();
                    if (prey != null) {
                        framePainter.paint(gl, prey);
                        boolean playerFitter = core.getBeing().getFitness() > prey.getFitness();
                        trailPainter.paint(gl, core.getBeing(), playerFitter, true);
                        trailPainter.paint(gl, prey, !playerFitter, true);
                    }
                }
            }
            paintBeingPointer(gl, being, null);
            if (core.getBeing() != null && being != core.getBeing().getGeometry().getPrey()) {
                trailPainter.paint(gl, being, false, true);
            }
            int lineCount = being.getSpeech().getLines().size();
            for (int walk = 0; walk < lineCount; walk++) {
                font3d.setAnchor(0, walk * 3 - lineCount * 3 + 1);
                font3d.display(gl, being.getSpeech().getLines().get(walk), java.awt.Color.YELLOW);
            }
        }
    }

    public void paintGhost(GL gl, Being being, boolean leader) {
        if (leader) {
            fabricPainter.paintLines(being.getBody(), gl, Tint.WHITE);
            framePainter.paint(gl, being);
        }
        else {
            fabricPainter.paintLines(being.getBody(), gl, null);
        }
    }

    public void paintBeingsAsLines(GL gl) {
        gl.glDisable(GL.GL_TEXTURE_2D);
        gl.glDisable(GL.GL_LIGHTING);
        gl.glBegin(GL.GL_LINES);
        toEye.set(pointOfView.getEye()).normalize();
        double maxDot = -2;
        for (Being being : core.getBeings()) {
            being.getGeometry().refresh();
            beingLocation.set(being.getGeometry().getBodyCenter()).normalize();
            double dot = beingLocation.dot(toEye);
            if (dot > maxDot) {
                closestLocation.set(beingLocation);
                maxDot = dot;
            }
            if (dot < 0.98) {
                paintPin(gl);
            }
            else {
                gl.glColor3f(1f, 1f, 1f);
                paintCrosshairs(gl, false);
            }
        }
        beingLocation.set(closestLocation);
        gl.glColor3f(1f, 0f, 0f);
        paintCrosshairs(gl, true);
        // todo: paint his text
        gl.glEnd();
    }

    private void paintBeingPointer(GL gl, Being being, Arrow goal) {
        if (goal != null) {
            pointerPainter.display(gl, pointOfView, being.getGeometry().getBodyCenter(), goal, true);
        }
        else {
            pointerPainter.display(gl, pointOfView, being.getGeometry().getBodyCenter(), being.getGoal(), false);
        }
    }

    // ==== helpers

    private void paintPin(GL gl) {
        gl.glColor3f(0f, 0f, 0f);
        beingLocation.scale(Constants.SURFACE_RADIUS);
        gl.glVertex3d(beingLocation.x, beingLocation.y, beingLocation.z);
        gl.glColor3f(1f, 1f, 1f);
        beingLocation.setSpan(Constants.HOVER_TOP);
        gl.glVertex3d(beingLocation.x, beingLocation.y, beingLocation.z);
    }

    private void paintCrosshairs(GL gl, boolean higher) {
        beingLocation.scale(higher ? Constants.HOVER_TOP : Constants.HOVER_BOTTOM);
        end.set(beingLocation).add(pointOfView.getUp(), CROSSHAIR_SIZE);
        gl.glVertex3d(beingLocation.x, beingLocation.y, beingLocation.z);
        gl.glVertex3d(end.x, end.y, end.z);
        end.set(beingLocation).add(pointOfView.getUp(), -CROSSHAIR_SIZE / 2).add(pointOfView.getRight(), CROSSHAIR_SIZE / 2);
        gl.glVertex3d(beingLocation.x, beingLocation.y, beingLocation.z);
        gl.glVertex3d(end.x, end.y, end.z);
        end.set(beingLocation).add(pointOfView.getUp(), -CROSSHAIR_SIZE / 2).add(pointOfView.getRight(), -CROSSHAIR_SIZE / 2);
        gl.glVertex3d(beingLocation.x, beingLocation.y, beingLocation.z);
        gl.glVertex3d(end.x, end.y, end.z);
    }

    private static class PointerPainter {
        private static final double MIN_SIZE = 0.3;
        private static final double MAX_SIZE = 2.5;
        private static final double NOSHOW_THRESHOLD = 0.9999;
        private TextureTrianglePainter painter = new TextureTrianglePainter("/TriangleTexture.png");
        private Arrow purpose = new Arrow();
        private Arrow center = new Arrow();
        private Arrow toEye = new Arrow();
        private Arrow ortho = new Arrow();
        private Arrow left = new Arrow();
        private Arrow front = new Arrow();
        private Arrow right = new Arrow();

        public void display(GL gl, PointOfView pointOfView, Arrow bodyCenter, Arrow goal, boolean tentative) {
            center.set(bodyCenter);
            double sizeFactor = (pointOfView.getEye().span() - Constants.SURFACE_RADIUS) / (Constants.HOVER_TOP - Constants.SURFACE_RADIUS);
            toEye.sub(pointOfView.getEye(), center).normalize();
            purpose.sub(goal, center);
            double height = center.span();
            double distance = purpose.span();
            if (distance >= 0.01 && Math.abs(toEye.dot(purpose) / distance) < NOSHOW_THRESHOLD) {
                painter.prePaint(gl, 0);
                if (tentative) {
                    gl.glColor3f(0.5f, 0.5f, 0.5f);
                }
                else {
                    gl.glColor3f(1, 0.3f, 0.3f);
                }
                purpose.scale(1 / distance);
                ortho.cross(toEye, purpose).normalize();
                double size = MIN_SIZE * (1 - sizeFactor) + MAX_SIZE * sizeFactor;
                for (double rearPosition = 0; rearPosition < distance; rearPosition += size) {
                    left.set(center).add(purpose, rearPosition).setSpan(height).add(ortho, size / 2);
                    right.set(center).add(purpose, rearPosition).setSpan(height).sub(ortho, size / 2);
                    front.set(center).add(purpose, rearPosition + size).setSpan(height);
                    painter.paint(right, front, left);
                }
                painter.postPaint();
            }
        }
    }

    private static class FramePainter {
        private Arrow calc = new Arrow();

        public void paint(GL gl, Being being) {
            gl.glDisable(GL.GL_LIGHTING);
            gl.glDisable(GL.GL_TEXTURE_2D);
            gl.glBegin(GL.GL_LINES);
            Tint tint = being.isDrinking() ? DRINKING_TINT : PURSUE_TINT;
            for (int walk = 0; walk < Direction.values().length; walk++) {
                Direction direction = Direction.values()[walk];
                gl.glColor3fv(tint.getFloatArray(), 0);
                paintVertex(gl, being.getGeometry().getBodyCenter());
                paintDirectionVertex(gl, being, direction);
            }
            gl.glColor3fv(tint.getFloatArray(), 0);
            for (int walk = 0; walk < Direction.values().length; walk++) {
                Direction direction = Direction.values()[walk];
                Direction nextDirection = Direction.values()[(walk + 1) % Direction.values().length];
                paintDirectionVertex(gl, being, direction);
                paintDirectionVertex(gl, being, nextDirection);
            }
            gl.glEnd();
        }

        private void setDirectionColor(GL gl, boolean target, boolean drinking) {
            if (drinking) {
                gl.glColor3fv(DRINKING_TINT.getFloatArray(), 0);
            }
            else if (target) {
                gl.glColor3fv(PURSUE_TINT_TARGET.getFloatArray(), 0);
            }
            else {
                gl.glColor3fv(PURSUE_TINT.getFloatArray(), 0);
            }
        }

        private void paintDirectionVertex(GL gl, Being being, Direction direction) {
            setDirectionColor(gl, direction == being.getGeometry().getDirection(), being.isDrinking());
            Arrow center = being.getGeometry().getBodyCenter();
            double radius = being.getGeometry().getBodyRadius();
            if (being.isDrinking()) {
                double variation = Math.PI * 2 * (being.getBody().getAge() % 1000) / 1000.0;
                double sin = Math.sin(variation);
                radius += sin * (radius / 4);
            }
            paintVertex(
                    gl,
                    calc.set(center).add(
                            being.getGeometry().getForward(),
                            direction.getForwardness() * radius).add(being.getGeometry().getRight(),
                            direction.getRightness() * radius
                    )
            );
        }

    }

    private static class TrailPainter {
        public void paint(GL gl, Being being, boolean superior, boolean showBodyCenter) {
            gl.glDisable(GL.GL_LIGHTING);
            gl.glDisable(GL.GL_TEXTURE_2D);
            Tint tint = superior ? TRAIL_TINT_SUPERIOR : TRAIL_TINT_NORMAL;
            gl.glColor3fv(tint.getFloatArray(), 0);
            gl.glBegin(GL.GL_LINE_STRIP);
            for (Arrow point : being.getTrail()) {
                paintVertex(gl, point);
            }
            if (showBodyCenter) {
                paintVertex(gl, being.getGeometry().getBodyCenter());
            }
            gl.glEnd();
        }
    }

    private static void paintVertex(GL gl, Arrow location) {
        gl.glVertex3d(location.x, location.y, location.z);
    }
}