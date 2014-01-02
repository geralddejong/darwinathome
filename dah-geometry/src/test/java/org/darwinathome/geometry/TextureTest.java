package org.darwinathome.geometry;

import com.sun.opengl.util.j2d.TextureRenderer;
import javax.media.opengl.GL;
import javax.media.opengl.GLCanvas;
import org.darwinathome.geometry.jogl.GLRenderer;
import org.darwinathome.geometry.jogl.GLViewPlatform;
import org.darwinathome.geometry.jogl.PointOfView;
import org.darwinathome.geometry.jogl.TextureTrianglePainter;
import org.darwinathome.geometry.math.Arrow;

import java.awt.Frame;

/**
 * Try out texture tricks
 *
 * @author Gerald de Jong, Beautiful Code BV, <geralddejong@gmail.com>
 */

public class TextureTest extends Frame {
    private GLCanvas canvas = new GLCanvas();
    private PointOfView pointOfView = new PointOfView(5);
    private Arrow a = new Arrow(0, -1, 0);
    private Arrow b = new Arrow(0, 1, 0);
    private Arrow c = new Arrow(0, 0, Math.sqrt(3));
    private TextureTrianglePainter painter = new TextureTrianglePainter("/TriangleTexture.png");
    private TextureRenderer textureRenderer = new TextureRenderer(512, 512, false);

    public TextureTest() {
        super("Texture Test");
        GLViewPlatform viewPlatform = new GLViewPlatform(new Renderer(), pointOfView, 1, 180);
        canvas.addGLEventListener(viewPlatform);
        add(canvas);
        setSize(800, 800);
    }

    private class Renderer implements GLRenderer {

        public void init(GL gl) {
        }

        public void display(GL gl, int width, int height) {
            painter.prePaint(gl, 0);
            painter.paint(a, b, c);
            painter.postPaint();
        }
    }

    public static void main(String[] args) {
        TextureTest test = new TextureTest();
        test.setVisible(true);
    }

}
