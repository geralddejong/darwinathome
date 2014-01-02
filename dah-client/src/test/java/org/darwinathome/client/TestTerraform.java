package org.darwinathome.client;

import org.darwinathome.Constants;
import org.darwinathome.geometry.jogl.GLRenderer;
import org.darwinathome.geometry.jogl.GLViewPlatform;
import org.darwinathome.geometry.jogl.PointOfView;
import org.darwinathome.universe.World;

import javax.media.opengl.GL;
import javax.media.opengl.GLCanvas;
import javax.swing.JFrame;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;

/**
 * @author Gerald de Jong <geralddejong@gmail.com>
 */

public class TestTerraform extends JFrame {
    private World world = World.create();
    private PointOfView pointOfView = new PointOfView(Constants.ORBIT_RADIUS * 1.2);
    private View view = new View(null);

    public TestTerraform() throws HeadlessException {
        super("Terraform");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        getContentPane().add(view.canvas, BorderLayout.CENTER);
    }

    private class View implements GLRenderer {
        private GLCanvas canvas = new GLCanvas();
        private GLViewPlatform viewPlatform = new GLViewPlatform(this, pointOfView, 1, Constants.FRUSTUM_FAR);
        private WorldPainter worldPainter;

        private View(Boolean isRightEye) {
            worldPainter = new WorldPainter();
            viewPlatform.setRightEye(isRightEye);
            canvas.setFocusable(true);
            canvas.requestFocus();
            canvas.addGLEventListener(viewPlatform);
            canvas.addMouseMotionListener(new FocusClaimer(canvas));
        }

        public void init(GL gl) {
        }

        public void display(final GL gl, int width, int height) {
            worldPainter.display(gl, world.getSurfacePatches(), null);
        }
    }

    private static class FocusClaimer implements MouseMotionListener {
        private Component component;

        private FocusClaimer(Component component) {
            this.component = component;
        }

        private void claimFocus() {
            if (!component.hasFocus()) {
                component.requestFocus();
            }
        }

        public void mouseDragged(MouseEvent mouseEvent) {
            claimFocus();
        }

        public void mouseMoved(MouseEvent mouseEvent) {
            claimFocus();
        }
    }

    private void go() {
        setVisible(true);
        new Thread() {
            int count = 0;

            public void run() {
                for (int walk = 0; walk < Constants.ITERATIONS_PER_HOUR * 24 / Constants.ITERATIONS_PER_PATCH_LIFE; walk++) {
                    try {
                        Thread.sleep(100);
                    }
                    catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    world.experienceTime((int) Constants.ITERATIONS_PER_PATCH_LIFE);
                    count++;
                    if (count % 100 == 0) {
                        System.out.println(count + " patch iterations");
                    }
                    view.canvas.repaint();
                }
            }
        }.start();
    }

    public static void main(final String[] args) {
        final Dimension size = Toolkit.getDefaultToolkit().getScreenSize();
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                TestTerraform testTerraform = new TestTerraform();
                testTerraform.setSize(800, 800);
                testTerraform.setLocation(size.width - 800, 0);
                testTerraform.go();
            }
        });
    }
}