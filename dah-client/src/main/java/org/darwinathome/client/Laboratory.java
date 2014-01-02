// ========= Copyright (C) 2009, 2010 Gerald de Jong =================
// This file is part of the Darwin at Home project, distributed
// under the GNU General Public License, version 3.
// You should have received a copy of this license in "license.txt",
// but if not, see http://www.gnu.org/licenses/gpl-3.0.txt.
// ===================================================================
package org.darwinathome.client;

import com.sun.opengl.util.Animator;
import org.apache.log4j.Logger;
import org.darwinathome.body.Being;
import org.darwinathome.body.BeingFactory;
import org.darwinathome.body.Embryo;
import org.darwinathome.genetics.Genome;
import org.darwinathome.genetics.impl.PseudoNoise;
import org.darwinathome.geometry.jogl.FabricPainter;
import org.darwinathome.geometry.jogl.Floor;
import org.darwinathome.geometry.jogl.GLRenderer;
import org.darwinathome.geometry.jogl.GLViewPlatform;
import org.darwinathome.geometry.jogl.JointWhoPainter;
import org.darwinathome.geometry.jogl.PointOfView;
import org.darwinathome.geometry.navi.Positioner;
import org.darwinathome.geometry.structure.Joint;
import org.darwinathome.geometry.structure.Physics;
import org.darwinathome.geometry.structure.Span;
import org.darwinathome.geometry.transform.AboveFloor;
import org.darwinathome.universe.SphericalPhysics;

import javax.media.opengl.GL;
import javax.media.opengl.GLCanvas;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * todo: we now use spherical physics so camera posiitioning will have to be very different!!!!!
 *
 *
 * @author Gerald de Jong <geralddejong@gmail.com>
 */

public class Laboratory extends JFrame {
    private static final float LIGHT_POSITION[] = {1f, 0.1f, 2f, 0.5f};
    private static final Integer DEFAULT_LIFE = 20;
    private static final int ITERATIONS_PER_FRAME = 3;
    private static final int ROWS = 2;
    private static final int COLS = 3;
    private Logger log = Logger.getLogger(getClass());
    private List<View> views = new ArrayList<View>();
    private View selectedView;
    private Queue<Runnable> jobs = new ConcurrentLinkedQueue<Runnable>();
    private StressRange stressRange = new StressRange();
    private BeingFactory beingFactory = new BeingFactory(new PseudoNoise(), null, null);
    private double chanceOfMutation = 0.001;
    private Animator animator = new Animator();
    private SphericalPhysics spherical = new SphericalPhysics();
    private Physics physics = new Physics(spherical);

    public Laboratory() {
        super("Darwin at Home");
        JPanel grid = new JPanel(new GridLayout(ROWS, COLS));
        for (int walk = 0; walk < ROWS * COLS; walk++) {
            View view = new View();
            views.add(view);
            grid.add(view.getJPanel());
        }
        getContentPane().add(grid, BorderLayout.CENTER);
        getContentPane().add(createControls(), BorderLayout.SOUTH);
        PhysicsTweaker physicsTweaker = new PhysicsTweaker(spherical.getPhysicsValues());
        getContentPane().add(physicsTweaker.getPanel(), BorderLayout.NORTH);
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        setSize(screenSize.width, screenSize.height - 150);
        animator.setRunAsFastAsPossible(false);
        animator.start();
    }

    private class StressRange implements Span.StressRange {
        public double minimum() {
            return -0.005;
        }

        public double maximum() {
            return 0.005;
        }
    }

    void offspring() {
        jobs.add(new Runnable() {
            public void run() {
                if (selectedView == null) {
                    return;
                }
                Genome genome = selectedView.being.getGenome();
                log.info("Chosen:\n" + genome);
                for (View view : views) {
                    if (view == selectedView) {
                        continue;
                    }
                    Genome mutated = genome.copy().mutate(chanceOfMutation, "growth");
                    log.info("Mutated:\n" + mutated);
                    view.createBeing(mutated);
                }
            }
        });
    }

    class View implements GLRenderer {
        PointOfView pointOfView = new PointOfView(20);
        Positioner positioner = new Positioner(jobs, pointOfView);
        GLCanvas canvas = new GLCanvas();
        FabricPainter fabricPainter = new FabricPainter(stressRange);
        JointWhoPainter jointWhoPainter = new JointWhoPainter(pointOfView);
        GLViewPlatform viewPlatform = new GLViewPlatform(this, pointOfView, 1, 180);
        boolean born;
        Being being;
        Floor floor = new Floor();

        View() {
            pointOfView.getFocus().z = pointOfView.getEye().z = 0;
            canvas.setFocusable(true);
            canvas.addGLEventListener(viewPlatform);
            canvas.addKeyListener(positioner.getKeyListener());
            canvas.addMouseListener(positioner.getMouseListener());
            canvas.addMouseMotionListener(positioner.getMouseMotionListener());
            canvas.addMouseWheelListener(positioner.getMouseWheelListener());
            animator.add(canvas);
            createBeing();
        }

        JPanel getJPanel() {
            JButton choose = new JButton("choose");
            choose.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent actionEvent) {
//                    energyField.setText(String.valueOf(getEnergyValue()+3));
                    selectedView = View.this;
                    offspring();
                }
            });
            JPanel p = new JPanel(new BorderLayout());
            p.add(canvas, BorderLayout.CENTER);
            p.add(choose, BorderLayout.SOUTH);
            return p;
        }

        void createBeing() {
            jobs.add(new Runnable() {
                public void run() {
                    being = beingFactory.create("Gumby");
                    born = false;
                }
            });
        }

        void createBeing(Genome genome) {
            being = beingFactory.create(new Embryo("Gumby", "", null, genome, null));
            born = false;
        }

        public void init(GL gl) {
            physics.setIterations(ITERATIONS_PER_FRAME);
        }

        public void display(GL gl, int width, int height) {
            gl.glLightfv(GL.GL_LIGHT0, GL.GL_POSITION, LIGHT_POSITION, 0);
            if (being != null) {
                try {
                    being.getGeometry().refresh();
                    being.experienceTime(physics);
                    fabricPainter.paintTexture(being.getBody(), gl);
                    jointWhoPainter.preVisit(gl);
                    for (Joint joint : being.getBody().getJoints()) {
                        jointWhoPainter.visit(joint);
                    }
                    pointOfView.moveFocusTowardsIdeal(being.getGeometry().getBodyCenter(), 0.01);
                    if (!born && being.getShield() == null && !being.getBody().isAnySpanActive()) {
                        born = true;
                        being.getBody().addTransformation(new AboveFloor(0.01));
//                        being.getEnergy().add(1000); // todo:  where to get energy
                        pointOfView.getEye().z = 1;
                        pointOfView.getFocus().z = 1;
                    }
                    if (being.getPhase() == Being.Phase.ADULT_LIFE) {
                        floor.display(gl);
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                    log.info("oops", e);
                    being = null;
                }
            }
            while (!jobs.isEmpty()) {
                jobs.remove().run();
            }
            positioner.run();
        }
    }

    private Component createControls() {
        JPanel p = new JPanel();
        for (View view : views) {
            view.positioner.setSpin(true);
        }
        final JCheckBox spin = new JCheckBox("Spin", true);
        spin.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                for (View view : views) {
                    view.positioner.setSpin(spin.isSelected());
                }
            }
        });
        p.add(spin);
        JButton reboot = new JButton("Reboot All");
        reboot.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                for (View view : views) {
                    view.createBeing();
                }
            }
        });
        p.add(reboot);
        return p;
    }

    public void kill() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                Thread.currentThread().interrupt();
            }
        });
    }

    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                Laboratory laboratory = new Laboratory();
                laboratory.setVisible(true);
            }
        });
    }
//                            try {
//
//
//                                DecimalFormat form = new DecimalFormat("0.00000");
//                                PrintWriter out = new PrintWriter(new FileWriter("/tmp/darwinathome.obj"));
//                                out.println("# OBJ File from Darwin at Home");
//                                out.println("# first the joints");
//                                Map<Who, Integer> indexMap = new HashMap<Who, Integer>();
//                                int jointNumber = 1;
//                                for (Joint joint : being.getBody().getJoints()) {
//                                    Arrow loc = joint.getLocation();
//                                    out.println("v "+form.format(loc.x)+" "+form.format(loc.x)+" "+form.format(loc.x));
//                                    indexMap.put(joint.getWho(), jointNumber);
//                                    jointNumber++;
//                                }
//                                out.println("# then the face normals");
//                                for (Face face : being.getBody().getFaces()) {
//                                    Arrow normal = face.getNormal();
//                                    out.println("vn "+form.format(normal.x)+" "+form.format(normal.x)+" "+form.format(normal.x));
//                                }
//                                out.println("# finally the faces");
//                                int faceNumber = 1;
//                                for (Face face : being.getBody().getFaces()) {
//                                    out.print("f ");
//                                    for (Joint joint : face.getJoints()) {
//                                        Integer jointIndex = indexMap.get(joint.getWho());
//                                        out.print(jointIndex+"//"+faceNumber+" ");
//                                    }
//                                    out.println();
//                                    faceNumber++;
//                                }
//                                out.println("# that's it!");
//                                out.close();
//                            }
//                            catch (IOException e) {
//                                e.printStackTrace();
//                            }
}
