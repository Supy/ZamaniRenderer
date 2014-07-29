package utils;

import javax.media.opengl.GL2;
import java.io.*;

/**
 * ShaderControl class taken from http://www.guyford.co.uk/showpage.php?id=50&page=How_to_setup_and_load_GLSL_Shaders_in_JOGL_2.0
 */
public class ShaderControl {
    private int vertexShaderProgram;
    private int fragmentShaderProgram;
    private int shaderprogram;
    public String[] vsrc = null;
    public String[] fsrc = null;

    public void init(GL2 gl) {
        try {
            attachShaders(gl);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Loads the shader in a file.
    public String[] loadShader(String name) throws IOException {
        StringBuilder sb = new StringBuilder();

        InputStream is = new FileInputStream(name);
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line);
            sb.append('\n');
        }
        is.close();
        System.out.println("Shader is " + sb.toString());
        return new String[]{sb.toString()};
    }

    private void attachShaders(GL2 gl) throws Exception {
        shaderprogram = gl.glCreateProgram();

        vertexShaderProgram = gl.glCreateShader(GL2.GL_VERTEX_SHADER);
        gl.glShaderSource(vertexShaderProgram, 1, vsrc, null, 0);
        gl.glCompileShader(vertexShaderProgram);
        gl.glAttachShader(shaderprogram, vertexShaderProgram);

        if(fsrc != null) {
            fragmentShaderProgram = gl.glCreateShader(GL2.GL_FRAGMENT_SHADER);
            gl.glShaderSource(fragmentShaderProgram, 1, fsrc, null, 0);
            gl.glCompileShader(fragmentShaderProgram);
            gl.glAttachShader(shaderprogram, fragmentShaderProgram);
        }

        gl.glLinkProgram(shaderprogram);
    }

    public int useShader(GL2 gl) {
        gl.glUseProgram(shaderprogram);
        return shaderprogram;
    }

    public void dontUseShader(GL2 gl) {
        gl.glUseProgram(0);
    }
}