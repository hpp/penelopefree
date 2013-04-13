package com.harmonicprocesses.penelopefree.openGL.shapes;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import com.harmonicprocesses.penelopefree.openGL.MyGLRenderer;

import android.opengl.GLES20;
import android.util.Log;

/**
 * OpenGL object
 * 
 *
 */
public class Particles {
	
    private final String vertexShaderCode2(float Delta) { return
            // This matrix member variable provides a hook to manipulate
            // the coordinates of the objects that use this vertex shader
            "uniform mat4 uMVPMatrix;\n" +
            //"attribute vec4 inits;\n" +
            "attribute vec4 vPosition;\n" +
            "void main() {\n" +
            // the matrix must be included as a modifier of gl_Position
            "  gl_Position = vPosition * uMVPMatrix;\n" +
            "  gl_PointSize=2.0;\n" +
            //"  float happy = inits[0];\n" +
            "}";}
    
    private String vertexShaderCode(float Delta) {
    	return "uniform mat4 uMVPMatrix;\n" +
	        "attribute vec4 vPosition;\n" + //[x,y,amplitude]
	        "attribute vec4 inits;" + //[lauchAngle, theta, distance2edge]
	        "vec4 location;\n" +
	        "float launchAngle;\n" +
	        "float theta;\n" +
	        "float furlong;\n" +
	        "float distance2edge;\n"  +
	        "float delta = " + Delta + ";\n"  +
	        "float amplitude;\n"  +
	        "bool initiallized = false;\n"  +
			"vec4 getNextPosition();\n" +
			"vec4 getTurnPosition(float distancePastEdge);\n" +
			"void init();\n" +
			"\n"  +
	        "void main() {\n" +
	        // the matrix must be included as a modifier of gl_Position
	        "	if (initiallized==false) { init(); initiallized=true; }\n" +
	        "	gl_Position = getNextPosition() * uMVPMatrix;\n" +
	        "	gl_PointSize = 2.0;\n" +
	        "}\n" +
	        "\n"  +
	        "vec4 getNextPosition() {\n" +
	        "	if (distance2edge<delta){\n" +
	        "		return getTurnPosition(delta-distance2edge);\n" +
	        "	}\n" +
	        "	location[0] = location[0] + (delta * cos(theta));\n" +
	        "	location[1] = location[1] + (delta * sin(theta));\n" +
	        "	distance2edge = distance2edge - delta; \n" +
	        "\n" +
	        "	return location;\n" +
	        "}\n" +
	        "\n" +
	        "vec4 getTurnPosition(float distancePastEdge) {\n" +
	        "	// update location at edge\n" +
	        "	location[0] = location[0] + (distance2edge * cos(theta));\n" +
	        "	location[1] = sin(theta) * distance2edge + location[1];\n" +
	        "\n" +
	        "	// calc new theta\n" +
	        "	theta = 2.0 * launchAngle + theta - 3.14159265359;\n" +
	        "	distance2edge = furlong;\n" +
	        "	//if (distance2edge<distancePastEdge){\n" +
	        "	//	return getTurnPosition(distancePastEdge-distance2edge);\n" +
	        "	//}\n" +
	        "\n" +
	        "	location[0] = cos(theta) * distancePastEdge + location[0];\n" +
	        "	location[1] = sin(theta) * distancePastEdge + location[1];\n" +
	        "	distance2edge = distance2edge - distancePastEdge;\n" +
	        "	return location;\n" +
	        "}\n" +
	        "\n" +
	        "void init() {\n" +
	        "	location[0] = vPosition[0];\n" +
	        "	location[1] = vPosition[1];\n" +
        	"	amplitude = vPosition[2];\n" +
        	"	launchAngle = inits[0];\n" +
        	"	theta = inits[1];\n" +
        	"	furlong = abs( 2.0 * cos(launchAngle));\n" +
        	"	distance2edge = inits[2];\n" +
        	"}";
    }
    
    private final String fragmentShaderCode =
        "precision mediump float;" +
        "uniform vec4 vColor;" +
        "void main() {" +
        "  gl_FragColor = vColor;" +
        "}";

    private volatile FloatBuffer vertexBuffer, initBuffer;
    private final int mProgram;
    private int mPositionHandle;
    private int mInitHandle;
    private int mColorHandle;
    private int mMVPMatrixHandle;

    // number of coordinates per vertex in this array
    static final int COORDS_PER_VERTEX = 3;
    //static float triangleCoords[] = { // in counterclockwise order:
    //     0.0f,  0.0f, 0.0f,   // center
    //};
    private final int vertexCount;
    private final int vertexStride = COORDS_PER_VERTEX * 4; // 4 bytes per vertex

    // Set color with red, green, blue and alpha (opacity) values
    float[] color; // = { 0.63671875f, 0.76953125f, 0.22265625f, 1.0f };
    ByteBuffer bb, bb2;

    public Particles(float[] Color, float[] particleVBO, float[] particleInits, 
    		float delta) {
    	
    	//vertexShaderCode = ReadFile("particleVertex.glsl");

    	
    	vertexCount = particleVBO.length / COORDS_PER_VERTEX;
    	color = Color;
        // initialize vertex byte buffer for shape coordinates
        bb = ByteBuffer.allocateDirect(
                // (number of coordinate values * 4 bytes per float)
        		particleVBO.length * 4);
        // use the device hardware's native byte order
        bb.order(ByteOrder.nativeOrder());

        // create a floating point buffer from the ByteBuffer
        vertexBuffer = bb.asFloatBuffer();
        // add the coordinates to the FloatBuffer
        vertexBuffer.put(particleVBO);
        // set the buffer to read the first coordinate
        vertexBuffer.position(0);
        
        /*
        bb2 = ByteBuffer.allocateDirect(
                // (number of coordinate values * 4 bytes per float)
        		particleInits.length * 4);
        // use the device hardware's native byte order
        bb2.order(ByteOrder.nativeOrder());
        // create a floating point buffer from the ByteBuffer
        initBuffer = bb2.asFloatBuffer();
        // add the coordinates to the FloatBuffer
        initBuffer.put(particleInits);
        // set the buffer to read the first coordinate
        initBuffer.position(0);
        */
              

        // prepare shaders and OpenGL program
        String vertexShaderCode = vertexShaderCode2(delta);
        int vertexShader = MyGLRenderer.loadShader(GLES20.GL_VERTEX_SHADER,
        											vertexShaderCode);
        int fragmentShader = MyGLRenderer.loadShader(GLES20.GL_FRAGMENT_SHADER,
                                                     fragmentShaderCode);

        // adapted from http://stackoverflow.com/questions/7629721/glsl-es-local-variables-crash
        /*
        GLES20.glCompileShader(vertexShader);

        // check whether compilation was successful; if not
        // then dump the log to the console
        IntBuffer status = null;
        GLES20.glGetShaderiv(vertexShader, GLES20.GL_COMPILE_STATUS, status);
        if(status!=null){
        	IntBuffer logLength = null;
        	GLES20.glGetShaderiv(vertexShader, GLES20.GL_INFO_LOG_LENGTH, logLength);
            if (logLength.limit() > 0)
            {
                String log = GLES20.glGetShaderInfoLog(vertexShader);
                Log.i("betarun.MyGLRenderer.Particles", "Shader compile log:\n%s" + log);
               
            }
        }
        */
        mProgram = GLES20.glCreateProgram();             // create empty OpenGL Program
        GLES20.glAttachShader(mProgram, vertexShader);   // add the vertex shader to program
        GLES20.glAttachShader(mProgram, fragmentShader); // add the fragment shader to program
        GLES20.glLinkProgram(mProgram);                  // create OpenGL program executable
  
    }

    
	public void draw(float[] mvpMatrix, float[] particleVBO) {
        // create a floating point buffer from the ByteBuffer
        //vertexBuffer = bb.asFloatBuffer();
        // add the coordinates to the FloatBuffer
        vertexBuffer.clear();
        vertexBuffer.put(particleVBO);
        // set the buffer to read the first coordinate
        vertexBuffer.position(0);
    	
    	// Add program to OpenGL environment
        GLES20.glUseProgram(mProgram);
        MyGLRenderer.checkGlError("glUseProgram");

        // get handle to vertex shader's vPosition member
        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
        mInitHandle = GLES20.glGetAttribLocation(mProgram, "inits");

        
        // Enable a handle to the triangle vertices
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        GLES20.glEnableVertexAttribArray(mInitHandle);
        
        // Prepare the triangle coordinate data
        GLES20.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX,
                                     GLES20.GL_FLOAT, false,
                                     vertexStride, vertexBuffer);
        // GLES20.glVertexAttribPointer(mInitHandle, COORDS_PER_VERTEX,
        //							 GLES20.GL_FLOAT, false,
        //							 vertexStride, initBuffer);
		

        // get handle to fragment shader's vColor member
        mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor");

        // Set color for drawing the triangle
        GLES20.glUniform4fv(mColorHandle, 1, color, 0);

        // get handle to shape's transformation matrix
        mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        MyGLRenderer.checkGlError("glGetUniformLocation");

        // Apply the projection and view transformation
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mvpMatrix, 0);
        MyGLRenderer.checkGlError("glUniformMatrix4fv");

        // Draw the triangle
        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, vertexCount);

        // Disable vertex array
        GLES20.glDisableVertexAttribArray(mPositionHandle);
        GLES20.glDisableVertexAttribArray(mInitHandle);
    }
	
	private String ReadFile(String fileName) {
    	String output = null;
    	// try opening the myfilename.txt
    	// from http://huuah.com/android-writing-and-reading-files/
    	try {
    	    // open the file for reading
    		BufferedReader buf = new BufferedReader(new FileReader(fileName));
    		
    	    //InputStream instream = openFileInput(fileName, Context.MODE_WORLD_WRITEABLE);
    	        	 
    	    // if file the available for reading
    	    if (buf.ready()) {
    	      // prepare the file for reading
    	      //InputStreamReader inputreader = new InputStreamReader(instream);
    	      //BufferedReader buffreader = new BufferedReader(inputreader);
    	                 
    	      String line = buf.readLine();
    	      
    	      // read every line of the file into the line-variable, on line at the time
    	      while(line!=null) {
    	    	// do something with the settings from the file
    	    	output+=line;
    	    	line = buf.readLine();
    	      }
    	 
    	    }
    	     
    	    // close the file again       
    	    buf.close();
    	    
    	  } catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return output;
	}

}