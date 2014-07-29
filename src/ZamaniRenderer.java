import com.jogamp.opengl.util.Animator;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import ply.PLYReader;
import utils.ByteSize;
import utils.NormalsCalculator;
import utils.ShaderControl;

import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.awt.GLCanvas;
import javax.media.opengl.glu.GLU;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.logging.Level.INFO;


class ZamaniRenderer implements GLEventListener, KeyListener, MouseWheelListener, MouseMotionListener {

    private static final Logger log = Logger.getLogger(ZamaniRenderer.class.getName());

    // Rendering settings for debugging purposes.
    private boolean drawNormals = false;
    private int glPolygonMode = GL2.GL_FILL;

    // Stores the list of currently pressed keys.
    private final HashMap<Integer, Boolean> keysDown = new HashMap<>();

    private final GLU glu = new GLU();
    private GL2 gl;
    private Camera camera;
    private int mouseCenterX, mouseCenterY;
    private static Cursor invisibleCursor;

    private PLYReader plyReader;
    private final IntBuffer buffers = IntBuffer.allocate(2);

    private ShaderControl shaderControl;
    private float[] data;

    public static void main(String[] args) {

        setupLogging();

        if (args.length != 1) {
            throw new IllegalArgumentException("must provide path to a PLY file");
        }

        try {
            ZamaniRenderer zamaniRenderer = new ZamaniRenderer(args[0]);



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
            animator.setUpdateFPSFrames(100, System.out);

        } catch (IOException e) {
            log.log(Level.SEVERE, "could not open file", e);
        }
    }

    private ZamaniRenderer(String fileName) throws IOException {
        this.plyReader = new PLYReader(fileName);
        this.data = NormalsCalculator.mergeWithVertices(this.plyReader.vertices, this.plyReader.indices);
    }

    public void init(GLAutoDrawable glDrawable) {
        gl = (GL2) glDrawable.getGL();

        gl.glShadeModel(GL2.GL_FLAT);
        gl.glClearColor(0.0f, 0.0f, 0.0f, 0.5f);                        // Clear the background colour to black.
        gl.glEnable(GL2.GL_DEPTH_TEST);                                 // Enable depth testing.
        gl.glDepthFunc(GL2.GL_LEQUAL);                                  // The type of depth test.
        gl.glClearDepth(1.0);
        gl.glHint(GL2.GL_PERSPECTIVE_CORRECTION_HINT, GL2.GL_NICEST);   // Quality of perspective calculations. Can possibly lower this.
        gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
        gl.glEnable(GL2.GL_CULL_FACE);
        gl.glFrontFace(GL2.GL_CW);

        setupLighting();

        //loadShaders();


        FloatBuffer vertexNormalData = FloatBuffer.wrap(data);

        gl.glGenBuffers(2, buffers);
        gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, buffers.get(0));
        gl.glBufferData(GL2.GL_ARRAY_BUFFER, this.plyReader.vertices.length * 2 * ByteSize.FLOAT, vertexNormalData, GL2.GL_STATIC_DRAW);
        gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, 0);

//        gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, buffers.get(0));
//        gl.glEnableVertexAttribArray(0);    //We like submitting vertices on stream 0 for no special reason
//        gl.glVertexAttribPointer(0, 3, GL2.GL_FLOAT, false, 24, 0);   //The starting point of the VBO, for the vertices
//        gl.glEnableVertexAttribArray(1);    //We like submitting normals on stream 1 for no special reason
//        gl.glVertexAttribPointer(1, 3, GL2.GL_FLOAT, false, 24, 12);     //The starting point of normals, 12 bytes away

        gl.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, buffers.get(1));
        gl.glBufferData(GL2.GL_ELEMENT_ARRAY_BUFFER, this.plyReader.indices.length * ByteSize.INT, IntBuffer.wrap(this.plyReader.indices), GL2.GL_STATIC_DRAW);
        gl.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, 0);



        camera = new Camera();

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                processInput();
            }
        }, 1, 10);
    }

    public void display(GLAutoDrawable glDrawable) {
        // Make sure we're in model view.
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadIdentity();

        gl.glPolygonMode(GL2.GL_FRONT_AND_BACK, glPolygonMode);

        // Clear the buffers.
        gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);

        Vector3D cameraPos = camera.getPosition();
        Vector3D lookAt = camera.getLookAt();
        Vector3D up = camera.getUp();

        // Set the camera's position and rotation. Always look at the origin.
        glu.gluLookAt(cameraPos.getX(), cameraPos.getY(), cameraPos.getZ(), lookAt.getX(), lookAt.getY(), lookAt.getZ(), up.getX(), up.getY(), up.getZ());


        gl.glEnable(GL2.GL_LIGHTING);
        //shaderControl.useShader(gl);

        // Draw the model.
        gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, buffers.get(0));
        gl.glVertexPointer(3, GL2.GL_FLOAT, 24, 0);
        gl.glNormalPointer(GL2.GL_FLOAT, 24, 12);


        gl.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, buffers.get(1));
        gl.glDrawElements(GL2.GL_TRIANGLES, this.plyReader.indices.length, GL2.GL_UNSIGNED_INT, 0);



        //shaderControl.dontUseShader(gl);

        gl.glDisable(GL2.GL_LIGHTING);

        if(drawNormals) {
            gl.glBegin(GL2.GL_LINES);
            gl.glColor3f(0.2f, 0.7f, 0);
            for (int i = 0; i < data.length; i += 6) {
                gl.glVertex3f(data[i], data[i + 1], data[i + 2]);
                gl.glVertex3f(data[i] + data[i + 3], data[i + 1] + data[i + 4], data[i + 2] + data[i + 5]);
            }
            gl.glEnd();
        }

        drawAxes();
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
        glu.gluPerspective(Camera.FOV, (float) windowWidth / (float) windowHeight, Camera.NEAR_PLANE, Camera.FAR_PLACE);
    }

    private void setupLighting() {
        float[] lightPosition = {0, 100, 0, 1};
        float[] ambientColor = {0.5f, 0.5f, 0.5f, 1f};
        float[] diffuseColor = {0.8f, 0.6f, 0.6f, 1f};
        float[] specularColor = {0.9f, 0.9f, 0.9f, 1f};

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
        gl.glVertex3f(10000f, 0f, 0f);
        gl.glVertex3f(-10000f, 0f, 0f);

        gl.glColor3f(0, 1f, 0);
        gl.glVertex3f(0, 10000f, 0);
        gl.glVertex3f(0, -10000f, 0);

        gl.glColor3f(0, 0, 1f);
        gl.glVertex3f(0, 0, 1000f);
        gl.glVertex3f(0, 0, -10000f);

        gl.glEnd();
    }

    private void loadShaders() {
        shaderControl = new ShaderControl();
        try {
            shaderControl.fsrc = shaderControl.loadShader("F:\\My Documents\\Workspace\\Zamani Renderer\\src\\shaders\\fragment_shader.glsl");
            shaderControl.vsrc = shaderControl.loadShader("F:\\My Documents\\Workspace\\Zamani Renderer\\src\\shaders\\vertex_shader.glsl");
            shaderControl.init(gl);
            //shaderControl.useShader(gl);
        } catch(IOException e) {
            log.log(Level.SEVERE, "failed to load shaders", e);
            System.exit(1);
        }
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

    private static void setupLogging() {
        // Set all logging.
        Logger root = Logger.getLogger("");
        root.setLevel(INFO);
        for (Handler handler : root.getHandlers()) {
            if (handler instanceof ConsoleHandler) {
                handler.setLevel(INFO);
            }
        }
    }
}