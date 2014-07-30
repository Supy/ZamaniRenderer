package utils;

import javax.media.opengl.GL2;
import java.io.*;

/**
 * ShaderControl class adapted from http://www.guyford.co.uk/showpage.php?id=50&page=How_to_setup_and_load_GLSL_Shaders_in_JOGL_2.0
 */
public class ShaderControl {

    private int vertexShaderProgram;
    private int fragmentShaderProgram;
    private int shaderProgram;
    public String[] vertexShaderSrc = null;
    public String[] fragmentShaderSrc = null;

    private GL2 gl;

    public ShaderControl(GL2 gl){
        this.gl = gl;
    }

    public void init() {
        try {
            attachShaders();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Loads the shader in a file.
    public void loadShader(String name, ShaderType shaderType) throws IOException {
        StringBuilder sb = new StringBuilder();

        InputStream is = new FileInputStream(name);
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line);
            sb.append('\n');
        }
        is.close();

        String[] shader = new String[]{sb.toString()};

        switch(shaderType) {
            case VERTEX:
                this.vertexShaderSrc = shader;
                break;
            case FRAGMENT:
                this.fragmentShaderSrc = shader;
                break;
        }
    }

    private void attachShaders() throws Exception {
        shaderProgram = gl.glCreateProgram();

        if(vertexShaderSrc == null){
            throw new Exception("vertex shader not loaded");
        }
        vertexShaderProgram = gl.glCreateShader(GL2.GL_VERTEX_SHADER);
        gl.glShaderSource(vertexShaderProgram, 1, vertexShaderSrc, null, 0);
        gl.glCompileShader(vertexShaderProgram);
        gl.glAttachShader(shaderProgram, vertexShaderProgram);

        if(fragmentShaderSrc != null) {
            fragmentShaderProgram = gl.glCreateShader(GL2.GL_FRAGMENT_SHADER);
            gl.glShaderSource(fragmentShaderProgram, 1, fragmentShaderSrc, null, 0);
            gl.glCompileShader(fragmentShaderProgram);
            gl.glAttachShader(shaderProgram, fragmentShaderProgram);
        }

        gl.glLinkProgram(shaderProgram);
    }

    public int useShader() {
        gl.glUseProgram(shaderProgram);
        return shaderProgram;
    }

    public void dontUseShader() {
        gl.glUseProgram(0);
    }
}