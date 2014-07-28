import com.jogamp.opengl.util.Animator;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.awt.GLCanvas;
import javax.media.opengl.glu.GLU;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;


class ZamaniRenderer implements GLEventListener, KeyListener, MouseWheelListener, MouseMotionListener {

    // Rendering settings for debugging purposes.
    private boolean drawNormals = true;
    private int glPolygonMode = GL2.GL_FILL;

    // Stores the list of currently pressed keys.
    private final HashMap<Integer, Boolean> keysDown = new HashMap<Integer, Boolean>();

    // The Array For The Points On The Grid Of Our "Wave"
    private final double[][][] points = new double[45][45][3];

    private final GLU glu = new GLU();
    private GL2 gl;
    private Camera camera;
    private int mouseCenterX, mouseCenterY;
    private static Cursor invisibleCursor;

    public static void main(String[] args) {

        ZamaniRenderer zamaniRenderer = new ZamaniRenderer();

        GLCanvas canvas = new GLCanvas();
        canvas.addGLEventListener(zamaniRenderer);
        canvas.addKeyListener(zamaniRenderer);
        canvas.addMouseWheelListener(zamaniRenderer);
        canvas.addMouseMotionListener(zamaniRenderer);

        makeInvisibleCursor();

        Frame frame = new Frame("Zamani Renderer");
        frame.add(canvas);
        frame.setSize(1200, 880);
        frame.setUndecorated(false);
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
        frame.setVisible(true);
        frame.requestFocus();
        frame.setCursor(invisibleCursor);

        // Repeatedly calls the canvas's display() method.
        Animator animator = new Animator(canvas);
        animator.start();
    }

    public void init(GLAutoDrawable glDrawable) {
        gl = (GL2) glDrawable.getGL();

        gl.glShadeModel(GL2.GL_SMOOTH);
        gl.glClearColor(0.0f, 0.0f, 0.0f, 0.5f);                        // Clear the background colour to black.
        gl.glEnable(GL2.GL_DEPTH_TEST);                                 // Enable depth testing.
        gl.glDepthFunc(GL2.GL_LEQUAL);                                  // The type of depth test.
        gl.glClearDepth(1.0);
        gl.glHint(GL2.GL_PERSPECTIVE_CORRECTION_HINT, GL2.GL_NICEST);   // Quality of perspective calculations. Can possibly lower this.
        gl.glEnable(GL2.GL_NORMALIZE);

        setupLighting();

        camera = new Camera();

        // Create our sample points.
        for (int x = 0; x < 45; x++) {
            for (int y = 0; y < 45; y++) {
                points[x][y][0] = ((x / 10.0f) - 2.25f);
                points[x][y][2] = ((y / 10.0f) - 2.25f);
                double x2 = (x / 22.5) - 1;
                double y2 = (y / 22.5f) - 1;
                points[x][y][1] = x2 * x2 * x2 - 3 * x2 + y2 * y2 * y2 - 3 * y2;
            }
        }

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                processInput();
            }
        }, 1, 10);
    }

    public void display(GLAutoDrawable glDrawable) {
        gl.glPolygonMode(GL2.GL_FRONT_AND_BACK, glPolygonMode);           // Render our points as lines.

        // Clear the buffers.
        gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);

        // Make sure we're in model view.
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadIdentity();

        Vector3D cameraPos = camera.getPosition();
        Vector3D lookAt = cameraPos.add(camera.getDirection());
        Vector3D up = camera.getUp();

        // Set the camera's position and rotation. Always look at the origin.
        glu.gluLookAt(cameraPos.getX(), cameraPos.getY(), cameraPos.getZ(), lookAt.getX(), lookAt.getY(), lookAt.getZ(), up.getX(), up.getY(), up.getZ());


        gl.glPushMatrix();

        gl.glEnable(GL2.GL_LIGHTING);

        gl.glBegin(GL2.GL_QUADS);
        gl.glColor3f(1, 1, 1);

        for (int x = 0; x < 44; x++) {
            for (int y = 0; y < 44; y++) {
                Vector3D quadNormal = Vector3D.crossProduct(new Vector3D(points[x][y]), new Vector3D(points[x][y + 1])).normalize();

                gl.glNormal3d(quadNormal.getX(), quadNormal.getY(), quadNormal.getZ());

                gl.glVertex3d(points[x][y][0], points[x][y][1], points[x][y][2]);
                gl.glVertex3d(points[x][y + 1][0], points[x][y + 1][1], points[x][y + 1][2]);
                gl.glVertex3d(points[x + 1][y + 1][0], points[x + 1][y + 1][1], points[x + 1][y + 1][2]);
                gl.glVertex3d(points[x + 1][y][0], points[x + 1][y][1], points[x + 1][y][2]);
            }
        }

        gl.glEnd();

        gl.glDisable(GL2.GL_LIGHTING);

        if (drawNormals) {
            gl.glBegin(GL2.GL_LINES);
            gl.glColor3f(0, 0.6f, 0);

            for (int x = 0; x < 44; x++) {
                for (int y = 0; y < 44; y++) {
                    Vector3D quadNormal = Vector3D.crossProduct(new Vector3D(points[x][y]), new Vector3D(points[x][y + 1])).normalize();

                    gl.glVertex3d(points[x][y][0], points[x][y][1], points[x][y][2]);
                    gl.glVertex3d(points[x][y][0] + quadNormal.getX(), points[x][y][1] + quadNormal.getY(), points[x][y][2] + quadNormal.getZ());


                    gl.glVertex3d(points[x][y + 1][0], points[x][y + 1][1], points[x][y + 1][2]);
                    gl.glVertex3d(points[x][y + 1][0] + quadNormal.getX(), points[x][y + 1][1] + quadNormal.getY(), points[x][y + 1][2] + quadNormal.getZ());

                    gl.glVertex3d(points[x + 1][y + 1][0], points[x + 1][y + 1][1], points[x + 1][y + 1][2]);
                    gl.glVertex3d(points[x + 1][y + 1][0] + quadNormal.getX(), points[x + 1][y + 1][1] + quadNormal.getY(), points[x + 1][y + 1][2] + quadNormal.getZ());


                    gl.glVertex3d(points[x + 1][y][0], points[x + 1][y][1], points[x + 1][y][2]);
                    gl.glVertex3d(points[x + 1][y][0] + quadNormal.getX(), points[x + 1][y][1] + quadNormal.getY(), points[x + 1][y][2] + quadNormal.getZ());
                }
            }
            gl.glEnd();
        }

        drawAxes();

        gl.glPopMatrix();
    }

    public void reshape(GLAutoDrawable glDrawable, int x, int y, int windowWidth, int windowHeight) {
        if (windowHeight == 0)
            windowHeight = 1;

        // Calculate position mouse will be reset to.
        mouseCenterX = x + windowWidth / 2;
        mouseCenterY = y + windowHeight / 2;

        // Setup our projection matrix.
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();
        glu.gluPerspective(45.0f, (float) windowWidth / (float) windowHeight, 0.1f, 100.0f);
    }

    private void setupLighting() {
        float[] lightPosition = {0, 100, 100, 1};
        float[] ambientColor = {0.8f, 0.8f, 0.8f, 1f};
        float[] diffuseColor = {1f, 0.6f, 0.6f, 1f};
        float[] specularColor = {1f, 1f, 1f, 1f};

        gl.glEnable(GL2.GL_LIGHTING);
        gl.glEnable(GL2.GL_LIGHT0);
        gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_AMBIENT, ambientColor, 0);
        gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_DIFFUSE, diffuseColor, 0);
        gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_POSITION, lightPosition, 0);
        gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_SPECULAR, specularColor, 0);
    }

    private void drawAxes() {
        gl.glBegin(GL2.GL_LINES);
        gl.glColor3f(1f, 0f, 0f);
        gl.glVertex3f(100f, 0f, 0f);
        gl.glVertex3f(-100f, 0f, 0f);

        gl.glColor3f(0, 1f, 0);
        gl.glVertex3f(0, 100f, 0);
        gl.glVertex3f(0, -100f, 0);

        gl.glColor3f(0, 0, 1f);
        gl.glVertex3f(0, 0, 100f);
        gl.glVertex3f(0, 0, -100f);

        gl.glEnd();
    }


    @Override
    public void dispose(GLAutoDrawable arg0) {

    }

    @Override
    public void keyPressed(KeyEvent keyEvent) {
        keysDown.put(keyEvent.getKeyCode(), true);
    }

    @Override
    public void keyReleased(KeyEvent keyEvent) {
        keysDown.put(keyEvent.getKeyCode(), false);
    }

    @Override
    public void keyTyped(KeyEvent keyEvent) {
        if (keyEvent.getKeyChar() == 'f') {
            glPolygonMode = (glPolygonMode == GL2.GL_FILL) ? GL2.GL_LINE : GL2.GL_FILL;
        }

        if (keyEvent.getKeyChar() == 'n') {
            drawNormals = !drawNormals;
        }
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        if (e.getPreciseWheelRotation() > 0) {
            camera.moveForward();
        } else {
            camera.moveBackward();
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {

    }

    @Override
    public void mouseMoved(MouseEvent e) {
        camera.addYaw((e.getXOnScreen() - mouseCenterX) * -0.03);
        camera.addPitch((e.getYOnScreen() - mouseCenterY) * -0.03);

        try {
            Robot robot = new Robot();
            robot.mouseMove(mouseCenterX, mouseCenterY);
        } catch (Exception ignored) {
        }
    }

    private void processInput() {
        if (isKeyDown(KeyEvent.VK_UP)) {
            camera.addPitch(1.5);
        }

        if (isKeyDown(KeyEvent.VK_DOWN)) {
            camera.addPitch(-1.5);
        }

        if (isKeyDown(KeyEvent.VK_LEFT)) {
            camera.addYaw(1.5);
        }

        if (isKeyDown(KeyEvent.VK_RIGHT)) {
            camera.addYaw(-1.5);
        }

        if (isKeyDown(KeyEvent.VK_W)) {
            camera.moveForward();
        }

        if (isKeyDown(KeyEvent.VK_S)) {
            camera.moveBackward();
        }

        if (isKeyDown(KeyEvent.VK_A)) {
            camera.moveLeft();
        }

        if (isKeyDown(KeyEvent.VK_D)) {
            camera.moveRight();
        }

        if (isKeyDown(KeyEvent.VK_SPACE)) {
            camera.moveUp();
        }

        if (isKeyDown(KeyEvent.VK_ESCAPE)) {
            System.exit(0);
        }
    }

    boolean isKeyDown(int e) {
        Boolean down = keysDown.get(e);
        return down == null ? false : down;
    }

    /*
     * Java hacks for hiding a cursor - making a transparent one.
     */
    private static void makeInvisibleCursor() {
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Point hotSpot = new Point(0, 0);
        BufferedImage cursorImage = new BufferedImage(1, 1, BufferedImage.TRANSLUCENT);
        invisibleCursor = toolkit.createCustomCursor(cursorImage, hotSpot, "InvisibleCursor");
    }
}