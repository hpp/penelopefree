package com.hpp.dsp;

public class Hilbert {
	
	public Hilbert(){
		
	}
	
	public float[][] splitProc(float[] timeSig){
		float[][] splitSig=split(timeSig);
		if (splitSig[0].length>=2){
			float[][] reduction = splitProc(splitSig[0]);
			// assume lowest level apply effect
			reduction[1] = applyFilter(reduction[1]);
			// build back out
			splitSig[0]=combine(reduction);
			return splitSig;
		} 
		return splitSig;
		
	}
	
	private float[] applyFilter(float[] details) {
		if (details.length>64){
			//This is to reduce processing high frequencies
			//64 was calculated to be about 2800 Hz, or First
			//resonate F on piano. based on sample rate 44100 
			// HZ and 1024 samples
			return details;
		}
		for (int i=0;i<12;i++){
			
		}
		return null;
	}





	private float[] combine(float[][] splitSignal) {
		int len = splitSignal.length;
		float[] timeSignal = new float[len*2];
		for (int i=0; i<len; i++){
			timeSignal[2*i]=(splitSignal[0][i]+splitSignal[1][i])/2.0f;
			timeSignal[2*i+1]=(splitSignal[0][i]-splitSignal[1][i])/2.0f;
		}
		return timeSignal;
	}


	/**
	 * Takes a time signal and splits it into averages 
	 * and details. 
	 * @param timeSignal, length must be even.
	 * @return (0,:) is averages, (1,:) is details
	 */
	private float[][] split(float[] timeSignal){
		int len = timeSignal.length/2;
		float[][] splitSignal = new float[2][len];
		for (int i=0; i<len; i++){
			splitSignal[0][i]=timeSignal[2*i]+timeSignal[2*i+1];
			splitSignal[1][i]=timeSignal[2*i]-timeSignal[2*i+1];
		}
		return splitSignal;
	}
}
