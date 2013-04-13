package com.harmonicprocesses.penelopefree.openGL.utils;


public class SoundParticleHexBins {
	
	float xRadius; //length of half a triangle
	float yRadius; //height of triangle
	float[] particleBins, normallizedBins, rectBins;
	
	/**
	 * triagBins N+1 per row, N rows.
	 *  first indexes faces down
	 *  even entries face down, odd face up
	 */
	float[] triagBins; //place holder for the amplitude of each locations
	
	/**
	 * hexBins 
	 *  six triags per bin
	 *  first triag[0] falls off the map
	 *  triag{1,2,3} fall in hex[0], triag{456) fall in hex[1]
	 *  triag[row1][zero can fall off]
	 *  triag[row1]{123} goto hex[row1][0] whereas
	 *  triag[row1]{456} goto hex[1] whereas
	 *    odd 
	 *  even entries face down, odd face up
	 */
	float[] hexBins; //place holder for the amplitude of each locations
	
	
	float[] latticeVBO;
	int numRecPerRow, numRecRows, numTriagPerRow, numHexPerRow, 
		rectBinNum, triagBinNum, firstHexCount;
	
	/**
	 * Constructor for ParticleHexBins
	 * @param Radius, radius of outer circle, all particles within said radius
	 * @param N, atleast 8, must be a power of 2
	 */
	public SoundParticleHexBins(float Radius, int N){
		yRadius = (float) (Radius/Math.ceil((N/2)));
		xRadius = (float) (2*yRadius*Math.tan(Math.PI/6));
		latticeVBO = new float[3];
		rectBins = new float[2*N*N];
		triagBins = new float[2*N*(N+1)];
		particleBins = new float[N*N];
		normallizedBins = new float[N*N];
		numRecPerRow = 2*N;
		numRecRows = N;
		numTriagPerRow = 2*N+1;
		numHexPerRow = calcNumHexes(N);
		hexBins = new float[numHexPerRow*(N/2)];
		//TODO fix this
		//particleBins = calcNumBins();
	}
	
	private int calcNumHexes(int n) {
		// 
		int i = n-1;
		boolean hexDirectionUp = true;
		int hexCount = 0;
		while (i>3){
			i -= 3;
			hexDirectionUp = !hexDirectionUp; //3up 3down
			hexCount+=1;
		}
		//if (i==1) { hexCount-=1; hexDirectionUp = !hexDirectionUp;}
		firstHexCount = i;
		return 2*hexCount+1;//first half times two plus center hex
	}

	public void add(SoundParticle particle){
		
		float ydiff =  (particle.location[1]+1)/yRadius;
		int ybin = (int) Math.floor((particle.location[1]+1)/yRadius);
		ydiff -= ybin;
		
		float xdiff = (particle.location[0]+1)/xRadius;
		int xbin = (int) Math.floor((particle.location[0]+1)/xRadius);
		xdiff -= xbin;
		
		//find the rectangle the particle is in
		if (xbin > 0 && xbin < numRecPerRow && ybin > 0 && ybin < numRecRows){
			rectBinNum = ybin*numRecPerRow+xbin; //+= particle.amplitude;//magnitude of sums, not sum of mags
			// find the triangle the part is in
			if ( ((xbin+ybin)%2==0&&xdiff-ydiff<=0) || ((xbin+ybin)%2!=0&&xdiff+ydiff<=1) ){
				triagBinNum = ybin*numTriagPerRow+xbin;
			} else {
				xbin+=1;
				triagBinNum = ybin*numTriagPerRow+xbin;
			}
			
			// find hex the triag is in
			int hexRowNum = (int) Math.floor((xbin+3-firstHexCount)/3);
			if (ybin%2==0){ //even rows don't split
				hexBins[(ybin/2)*numHexPerRow+hexRowNum]=particle.amplitude;
			} else if (firstHexCount%2==0) {
				//first hex is down, odds hexrows zero indexes go up
				if (hexRowNum%2==0){
					hexBins[((ybin+1)/2)*numHexPerRow+hexRowNum]=particle.amplitude;
				} else {
					hexBins[((ybin-1)/2)*numHexPerRow+hexRowNum]=particle.amplitude;
				}
				
			} else {
				//first hex is up, odd hexrows first index gow down
				if (hexRowNum%2==0){
					hexBins[((ybin-1)/2)*numHexPerRow+hexRowNum]=particle.amplitude;
				} else {
					hexBins[((ybin+1)/2)*numHexPerRow+hexRowNum]=particle.amplitude;
				}
			}
				
			
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
	
	public static float distance(float x1, float y1, float x2, float y2){
		float ydis = y2-y1;
		float xdis = x2-x1;
		float dis = (float) Math.sqrt((xdis*xdis)+(ydis*ydis));
		return dis;
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



