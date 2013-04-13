package com.harmonicprocesses.penelopefree.openGL.shapes;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import com.harmonicprocesses.penelopefree.openGL.MyGLRenderer;
import com.harmonicprocesses.penelopefree.openGL.utils.ModeNodes;
import com.harmonicprocesses.penelopefree.openGL.utils.SoundParticleHexBins;

import android.opengl.GLES20;

public class DarkParticles extends BaseParticles {

	public DarkParticles(int numParticles) {
		super(numParticles);
		float darkColor[] = { 1.0f, 0.0f, 1.0f, 1.0f };
		color = darkColor;
		towardNodes=1; //head toward nodes
	}
	
}
