package com.harmonicprocesses.penelopefree.openGL.shapes;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import com.harmonicprocesses.penelopefree.openGL.MyGLRenderer;

import android.opengl.GLES20;

public class OuterCircle {

    private final String vertexShaderCode =
        // This matrix member variable provides a hook to manipulate
        // the coordinates of the objects that use this vertex shader
        "uniform mat4 uMVPMatrix;" +

        "attribute vec4 vPosition;" +
        "void main() {" +
        // the matrix must be included as a modifier of gl_Position
        "  gl_Position = vPosition * uMVPMatrix;" +
        "}";

    private final String fragmentShaderCode =
        "precision mediump float;" +
        "uniform vec4 vColor;" +
        "void main() {" +
        "  gl_FragColor = vColor;" +
        "}";

    private final FloatBuffer vertexBuffer;
    private final int mProgram;
    private int mPositionHandle;
    private int mColorHandle;
    private int mMVPMatrixHandle;
    private float[] radius = CalcRadius();

    // number of coordinates per vertex in this array
    static final int COORDS_PER_VERTEX = 3; //3d space
    private int mNote = 1;
    float outerCircleCoords[] = buildCircleVBO(radius[mNote], 64);
    		/*{ // in counterclockwise order:
         0.0f,  0.622008459f, 0.0f,   // top
        -0.5f, -0.311004243f, 0.0f,   // bottom left
         0.5f, -0.311004243f, 0.0f    // bottom right
    };*/
    private int vertexCount = outerCircleCoords.length / COORDS_PER_VERTEX;
    private final int vertexStride = COORDS_PER_VERTEX * 4; // 4 bytes per vertex

    // Set color with red, green, blue and alpha (opacity) values
    float color[] = { 0.63671875f, 0.76953125f, 0.22265625f, 1.0f };

    /**
     * The radius of the outer circle should be proportional 
     *  to the inverse of the fundamental frequency shifted to 
     *  the octave of mode I
     *   i.e A = 1, A# = 1/(2^13/12), B = 1/(2^7/6), C=1/(2^5/4), etc...
     * @param r, radius of outercircle
     * @param N, number of points in circle
     */
    public OuterCircle(int note, int N) {
        // initialize vertex byte buffer for shape coordinates
    	mNote = note;
    	outerCircleCoords = buildCircleVBO(radius[mNote], N);
    	vertexCount = outerCircleCoords.length / COORDS_PER_VERTEX;
    	
        ByteBuffer bb = ByteBuffer.allocateDirect(
                // (number of coordinate values * 4 bytes per float)
        		outerCircleCoords.length * 4);
        // use the device hardware's native byte order
        bb.order(ByteOrder.nativeOrder());

        // create a floating point buffer from the ByteBuffer
        vertexBuffer = bb.asFloatBuffer();
        // add the coordinates to the FloatBuffer
        vertexBuffer.put(outerCircleCoords);
        // set the buffer to read the first coordinate
        vertexBuffer.position(0);

        // prepare shaders and OpenGL program
        int vertexShader = MyGLRenderer.loadShader(GLES20.GL_VERTEX_SHADER,
                                                   vertexShaderCode);
        int fragmentShader = MyGLRenderer.loadShader(GLES20.GL_FRAGMENT_SHADER,
                                                     fragmentShaderCode);

        mProgram = GLES20.glCreateProgram();             // create empty OpenGL Program
        GLES20.glAttachShader(mProgram, vertexShader);   // add the vertex shader to program
        GLES20.glAttachShader(mProgram, fragmentShader); // add the fragment shader to program
        GLES20.glLinkProgram(mProgram);                  // create OpenGL program executables

    }

    private float[] CalcRadius() {
		float[] Radii = new float[12];
		int note = 0;
		for (float r : Radii){
			Radii[note] = (float) (1.0f/Math.pow(2.0, note++/12.0));
			// ~note r = (float)... does not change the Radii collection
		}
		return Radii;
	}

	private static float[] buildCircleVBO(float r, int N) {
		float[] output = new float[N*COORDS_PER_VERTEX];
		for (int i=0; i<N; i++){
			output[3*i]=(float) (r*Math.cos(2*Math.PI*i/N));
			output[3*i+1]=(float) (r*Math.sin(2*Math.PI*i/N));
			//stay in z plane
		}
		return output;
	}

	public void draw(float[] mvpMatrix, int note) {
        // if note has changed update outercircle coordinates
		if (note!=mNote){
			mNote = note;
			//clear out old coors
	        vertexBuffer.clear();
	        // calc new radius, TODO cache these values.
	        //float r = (float) Math.pow(2.0, (11.0+note)/12.0);
	        outerCircleCoords = buildCircleVBO(radius[mNote], vertexCount);
	        vertexBuffer.put(outerCircleCoords);
	        // set the buffer to read the first coordinate
	        vertexBuffer.position(0);
		}
		
        // Add program to OpenGL environment
        GLES20.glUseProgram(mProgram);

        // get handle to vertex shader's vPosition member
        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");

        // Enable a handle to the triangle vertices
        GLES20.glEnableVertexAttribArray(mPositionHandle);

        // Prepare the triangle coordinate data
        GLES20.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX,
                                     GLES20.GL_FLOAT, false,
                                     vertexStride, vertexBuffer);

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
        GLES20.glDrawArrays(GLES20.GL_LINE_LOOP, 0, vertexCount);

        // Disable vertex array
        GLES20.glDisableVertexAttribArray(mPositionHandle);
    }

	public float getRadius() {
		return radius[mNote];
	}
}