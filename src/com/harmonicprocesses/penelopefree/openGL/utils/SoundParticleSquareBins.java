package com.harmonicprocesses.penelopefree.openGL.utils;


public class SoundParticleSquareBins {
	
	float xRadius; //length of half a triangle
	float yRadius; //height of triangle
	float[] particleBins, normallizedBins; //place holder for the amplitude of each locations
	float[] latticeVBO;
	int numPerRow;
	
	public SoundParticleSquareBins(float Radius, int N){
		xRadius = Radius;
		yRadius = Radius;
		latticeVBO = new float[3];
		particleBins = new float[N*N];
		normallizedBins = new float[N*N];
		numPerRow = N;
		//TODO fix this
		//particleBins = calcNumBins();
	}
	
	public void add(SoundParticle particle){
		int ybin = (int) Math.floor((particle.location[1]+1)/yRadius);
		int xbin = (int) Math.floor((particle.location[0]+1)/xRadius);
		if (xbin > 0 && xbin < numPerRow && ybin > 0 && ybin < numPerRow){
			particleBins[ybin*numPerRow+xbin] += particle.amplitude*particle.amplitude;
		}
	}
	
	public void clear(){
		for (int i=0; i<particleBins.length;i++){
			particleBins[i] = 0;
		}
	}
	
	public void normallize(){
		float max = 0;
		for (int i=0; i<particleBins.length;i++){
			if (particleBins[i] > max){
				max = particleBins[i];
			}
		}
		for (int i=0; i<particleBins.length;i++){
			normallizedBins[i] = particleBins[i] / max;
		}
	}
	
	public float[] buildLattice(){
		float[] latticeVBO = new float[3 * calcNumBins()];
		// zero already equals zero, zero

		
		
		return latticeVBO;
	}
		
	private int calcNumBins(){
		return (int) (Math.floor(Math.PI/(xRadius*yRadius)));
	}
	
	private float midPoint(float x1, float x2){
		float output= (float) (Math.sqrt(x1*x1+x2*x2));
		return output;
	}
	
	private float[] drawHex(float r, float centerX, float centerY){
		float[] output = new float[7*3]; //7 vertices 3 coords each
		output[0]=centerX;
		output[1]=centerY;
		for (int i = 1; i<=6;i++){ //outside six points
			output[i*3] = (float) (centerX + r*Math.cos((i-1)*Math.PI/3));
			output[i*3+1] = (float) (centerY + r*Math.sin((i-1)*Math.PI/3));
		}
		return output;
	}
	
	private float[] trianglePoints(){
		return null;
	}
	
	private void latticeAppend(float[] latticePoints){
		int N =  latticeVBO.length;
		int len = N + latticePoints.length;
		float[] output = new float[len];
		for (int i=0; i<N; i++){
			output[i] = latticeVBO[i];
		}
		for (int i = N;i<len;i++){
			output[i] = latticePoints[i-N];
		}
		latticeVBO = output;
	}
}



