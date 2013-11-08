package com.hpp.dsp;

public class Filter{
	double[] filterPad, oldSamples;
	
	public Filter(int filterSize, int numSamples){
		filterPad = new double[filterSize];
		oldSamples = new double[filterSize];
	}
	
	/**
	 * Filters a samples stream
	 *  
	 * @param samples, input stream
	 * @param b, b filter coeffs
	 * @param a, a filter coeffs
	 * @return filtered samples
	 */
	public float[] filter(float[] samples, double[] b, double[] a) {
		int l = b.length, sl = samples.length;
		float[] filteredSamples = new float[sl];
		
		//initiallize filter (zero pad) or continue from stored, or "padded", values
		for (int i = 0; i < l; i ++){
			filterPad[i] = 0; //set new index back to tabula rasa, three day hunt for this bug...
			
			for (int j = 0; j<l; j++){ // add up b coeffs times stored data or new samples
				if (i-j<0){
					filterPad[i] += b[j]*oldSamples[j-i-1];
				} else {
					filterPad[i] += b[j]*samples[i-j];
				}
			}
			
			 
			for (int j = 1; j<l; j++){ // reduce by IIR stored data points
				if (i-j<0){
					filterPad[i] -= a[j]*filterPad[i+l-j];
				} else {
					filterPad[i] -= a[j]*filterPad[i-j];
				}
			}
			
			//if (abs(filterPad[i])>maxAmplitude){
			//	maxAmplitude = (float) abs(filterPad[k][0]);
			//}
		}
		
		// Main filtering algorythm
		for (int i = l; i<sl; i++){
			filterPad[i%l] = 0; //set new index back to tabula rasa
			
			for (int j = 0; j<l; j++){
				filterPad[i%l] += b[j]*samples[i-j];
			}
			for (int j = 1; j<l; j++){
				if (i%l-j<0){
					filterPad[i%l] -= a[j]*filterPad[i%l+l-j];
				} else {
					filterPad[i%l] -= a[j]*filterPad[i%l-j];
				}
			}
			
			filteredSamples[i]=(float) filterPad[i%l];
			//if (abs(filterPad[k][i%l]) > maxAmplitude) {
			//	maxAmplitude = (float) abs(filterPad[k][i%l]);
			//}
		}
		
		// reset filterPad to 0 index
		double[] temp = new double[l];
		for (int i = 0; i < l; i++){
			temp[i] = filterPad[(samples.length+i)%l];
		}
		filterPad = temp;
	
		// store off old samples
		for (int j = 0; j<l; j++){
			oldSamples[j] = samples[sl-j-1]; 
		}
		
		return filteredSamples;
	}
}

