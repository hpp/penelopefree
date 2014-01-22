package com.harmonicprocesses.penelopefree.openGL.shapes;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;

import com.harmonicprocesses.penelopefree.R;
import com.harmonicprocesses.penelopefree.openGL.MyGLRenderer;

public class NoteBillboard {
	
	/** How many bytes per float. */
	private final int mBytesPerFloat = 4;	

    private final String vertexShaderCode =
        // This matrix member variable provides a hook to manipulate
        // the coordinates of the objects that use this vertex shader
        "uniform mat4 uMVPMatrix;\n" +
        "attribute vec4 vPosition;\n" +
        "attribute vec2 a_TexCoordinate;\n" +
        "varying vec2 v_TexCoordinate;\n" +
        "void main() {\n" +
        "  gl_Position = uMVPMatrix * vPosition;\n" +
        "  v_TexCoordinate = a_TexCoordinate;\n" +
        "}\n";
    		
    private final String fragmentShaderCode =
        "precision mediump float;\n" +
        "varying vec2 v_TexCoordinate;\n" +
        "uniform sampler2D u_Texture;\n" +
        "void main() {\n" +
        //"  gl_FragColor = vec4(1.0,1.0,1.0,1.0);\n" +
        "  gl_FragColor = texture2D(u_Texture, v_TexCoordinate);\n" +
        "}\n";

    private final FloatBuffer vertexBuffer;
    private final ShortBuffer drawListBuffer;
    private final int mProgram;
    private int mPositionHandle;
    //private int mColorHandle;
    private int mMVPMatrixHandle;
    /** Store our model data in a float buffer. */
    private final FloatBuffer mCubeTextureCoordinates;
     
    /** This will be used to pass in the texture. */
    private int mTextureUniformHandle;
     
    /** This will be used to pass in model texture coordinate information. */
    private int mTextureCoordinateHandle;
     
    /** Size of the texture coordinate data in elements. */
    private final int mTextureCoordinateDataSize = 2;
     
    /** This is a handle to our texture data. */
    private int[] mTextureDataHandles = new int[12];

    // number of coordinates per vertex in this array
    static final int COORDS_PER_VERTEX = 3;
    static float squareCoords[];

    private final short drawOrder[] = { 0, 1, 2, 0, 2, 3 }; // order to draw vertices

    private final int vertexStride = COORDS_PER_VERTEX * 4; // 4 bytes per vertex

    // Set color with red, green, blue and alpha (opacity) values
    float color[] = { 0.2f, 0.709803922f, 0.898039216f, 1.0f };

    public NoteBillboard(Context context) {
    	float[] Coords = {-0.1f, 0.8f, 0.0f,   // top left
    					   -0.1f, 0.6f, 0.0f,   // bottom left
    					    0.1f, 0.6f, 0.0f,   // bottom right
    					    0.1f, 0.8f, 0.0f }; // top right
    	squareCoords = Coords;
        // initialize vertex byte buffer for shape coordinates
        ByteBuffer bb = ByteBuffer.allocateDirect(
        // (# of coordinate values * 4 bytes per float)
                squareCoords.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(squareCoords);
        vertexBuffer.position(0);

        
        final float[] cubeTextureCoordinateData =	{
        		1.0f, 0.0f,
        		1.0f, 1.0f,
        		0.0f, 1.0f,
        		0.0f, 0.0f   	        
    	        };
		mCubeTextureCoordinates = ByteBuffer.allocateDirect(cubeTextureCoordinateData.length * mBytesPerFloat)
		.order(ByteOrder.nativeOrder()).asFloatBuffer();
		mCubeTextureCoordinates.put(cubeTextureCoordinateData).position(0);
         
        
        // initialize byte buffer for the draw list
        ByteBuffer dlb = ByteBuffer.allocateDirect(
        // (# of coordinate values * 2 bytes per short)
                drawOrder.length * 2);
        dlb.order(ByteOrder.nativeOrder());
        drawListBuffer = dlb.asShortBuffer();
        drawListBuffer.put(drawOrder);
        drawListBuffer.position(0);

        // prepare shaders and OpenGL program
        int vertexShader = MyGLRenderer.loadShader(GLES20.GL_VERTEX_SHADER,
                                                   vertexShaderCode);
        int fragmentShader = MyGLRenderer.loadShader(GLES20.GL_FRAGMENT_SHADER,
                                                     fragmentShaderCode);

        mProgram = GLES20.glCreateProgram();             // create empty OpenGL Program
        GLES20.glAttachShader(mProgram, vertexShader);   // add the vertex shader to program
        GLES20.glAttachShader(mProgram, fragmentShader); // add the fragment shader to program
       
        GLES20.glLinkProgram(mProgram);                  // create OpenGL program executables

             
        // Load the texture
        mTextureDataHandles = loadTexture(context);
    }
    
    
    public static int[] loadTexture(final Context context)
    {
    	
        final int[] textureHandles = new int[12];
     
        GLES20.glGenTextures(12, textureHandles, 0);
        
        int[] note = {R.drawable.note1, R.drawable.note2,
        		R.drawable.note3, R.drawable.note4, R.drawable.note5,
        		R.drawable.note6, R.drawable.note7, R.drawable.note8,
        		R.drawable.note9, R.drawable.note10, R.drawable.note11, R.drawable.note0
        };
     
        int i = 0;
        for (int textureHandle:textureHandles){
        	if (textureHandle != 0){
        		
        		
        		int resourceId = note[i++];
        	
        		final BitmapFactory.Options options = new BitmapFactory.Options();
	            //options.inScaled = false;   // No pre-scaling
	     
	            // Read in the resource
	            final Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), resourceId, options);
	     
	            // Bind to the texture in OpenGL
	            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle);
	     
	            // Set filtering
	            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
	            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
	     
	            // Load the bitmap into the bound texture.
	            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
	     
	            // Recycle the bitmap, since its data has been loaded into OpenGL.
	            bitmap.recycle();
	        }
	     
	        if (textureHandle == 0)
	        {
	            throw new RuntimeException("Error loading texture.");
	        }
        }
	     
        return textureHandles;
    }
    
    public void draw(float[] mvpMatrix, int note) {
        // Add program to OpenGL environment
        GLES20.glUseProgram(mProgram);
        MyGLRenderer.checkGlError("glUseProgram");

        // get handle to vertex shader's vPosition member
        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");

        // Enable a handle to the triangle vertices
        GLES20.glEnableVertexAttribArray(mPositionHandle);

        // Prepare the triangle coordinate data
        //Matrix.multiplyMV(vertexBuffer, 0, scaleMatrix, 0, vertexBuffer, 0);
        GLES20.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX,
                                     GLES20.GL_FLOAT, false,
                                     vertexStride, vertexBuffer);
        
        // get handle to fragment shader's vColor member
        //mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor");

        mTextureUniformHandle = GLES20.glGetUniformLocation(mProgram, "u_Texture");
        mTextureCoordinateHandle = GLES20.glGetAttribLocation(mProgram, "a_TexCoordinate");
     
        // Set the active texture unit to texture unit 1. (0 reserved for FBO?)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
     
        // Bind the texture to this unit.
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureDataHandles[note]);
     
        // Tell the texture uniform sampler to use this texture in the shader by binding to texture unit 0.
        GLES20.glUniform1i(mTextureUniformHandle, 0);
        
        // Set color for drawing the triangle
        //GLES20.glUniform4fv(mColorHandle, 1, color, 0);

        // Pass in the texture coordinate information
        mCubeTextureCoordinates.position(0);
        GLES20.glVertexAttribPointer(mTextureCoordinateHandle, mTextureCoordinateDataSize, GLES20.GL_FLOAT, false, 
        		0, mCubeTextureCoordinates);
        
        GLES20.glEnableVertexAttribArray(mTextureCoordinateHandle);
        
        
        // get handle to shape's transformation matrix
        mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        MyGLRenderer.checkGlError("glGetUniformLocation");

        // Apply the projection and view transformation
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mvpMatrix, 0);
        MyGLRenderer.checkGlError("glUniformMatrix4fv");

        // Draw the square
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, drawOrder.length,
                              GLES20.GL_UNSIGNED_SHORT, drawListBuffer);

        // Disable vertex array
        GLES20.glDisableVertexAttribArray(mPositionHandle);
    }
}




