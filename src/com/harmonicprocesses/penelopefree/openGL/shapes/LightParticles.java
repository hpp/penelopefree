package com.harmonicprocesses.penelopefree.openGL.shapes;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import com.harmonicprocesses.penelopefree.openGL.MyGLRenderer;
import com.harmonicprocesses.penelopefree.openGL.utils.ModeNodes;
import com.harmonicprocesses.penelopefree.openGL.utils.SoundParticleHexBins;

import android.opengl.GLES20;

public class LightParticles extends BaseParticles {

	public LightParticles(int numParticles) {
		super(numParticles);
		float lightColor[] = { 0.0f, 1.0f, 0.0f, 1.0f };
		color = lightColor;
		towardNodes=-1; //head away from nodes
	}
	
}