package com.hpp.dsp;

import com.harmonicprocesses.penelopefree.audio.AudioConstants;

public class Wavelet {
	double[] a = AudioConstants.quarterFilterACoeffs,
			b = AudioConstants.quarterFilterBCoeffs;
	double[][] noteA = AudioConstants.noteACoefs,
			noteB = AudioConstants.noteBCoefs;
	Filter[] filter;
	int noteInOctave = 12; //this could change if we could select notes in pitch control
	Filter[][] noteFilter;
	private float[] spectrum;
	private boolean pitchCorrect=true;
	
	public Wavelet(){
		filter = new Filter[16];
		noteFilter = new Filter[6][noteInOctave];
		spectrum = new float[96];
	}
	
	/**
	 * 
	 * @param timeSig, must be a power of 2
	 * @param rec, recursion level
	 * @return
	 */
	public float[] transform(float[] timeSig, int rec) {
		if (timeSig.length<=4) return timeSig;
		if (filter[rec]==null){filter[rec]=new Filter(a.length,timeSig.length);}
		
		float[] filterSig = filter[rec].filter(timeSig,a,b);
		float[] details = diff(timeSig,filterSig);
		float[] downSig=downSample(filterSig);
		
		float[] next = transform(downSig,rec+1);
		
		// assume lowest level apply effect
		next = applyFilter(next,rec-1);//minus 1 as we calaculated from a downsample in og data
		// build back out
		timeSig=combine(next,details);
		return timeSig;
	}
	
	private float[] diff(float[] timeSig, float[] filterSig) {
		for (int i=0;i<timeSig.length;i++){
			timeSig[i] -= filterSig[i];
		}
		return timeSig;
	}
	
	private float[] applyFilter(float[] details, int rec) {
		int l = details.length;
		if (l>=64){
			//This is to reduce processing high frequencies
			//64 was calculated to be about 2800 Hz, or First
			//resonate F on piano. based on sample rate 44100 
			// HZ and 1024 samples. this yields a recursion level
			// of {32,16,8,4,2}.length=5
			return details;
		}
		float[] output = new float[l];
		for (int note=0;note<12;note++){
			if (noteFilter[rec][note]==null){
				 noteFilter[rec][note] = new Filter(noteA[note].length, details.length);
			}
			float[] filteredDetails = noteFilter[rec][note].filter(details, noteB[note], noteA[note]);
			//32 = rec4, 16 = rec5 ... 64 is lev7
			spectrum[(96-12*(10-rec)) + note]=max(filteredDetails);
			if (pitchCorrect){
				for (int i=0;i<l;i++){
					output[i] += filteredDetails[i];
				}
			}
		}
		
		if (pitchCorrect){
			return output;
		} else
			return details;
	}

	private float max(float[] samples) {
		float max = 0;
		for (int i = 0; i<samples.length; i++){
			if (Math.abs(samples[i])>max){
				max=Math.abs(samples[i]);
			}
		}
		return max;
	}

	private float[] combine(float[] downAvg,float[] details) {
		int len = downAvg.length;
		float[] timeSignal = new float[len*2];
		for (int i=0; i<len; i++){
			
			timeSignal[2*i]=downAvg[i]+details[2*i+1];
			timeSignal[2*i+1]=downAvg[i]+details[2*i+1];
		}
		return timeSignal;
	}


	/**
	 * Takes a time signal and downsamples by half, 
	 * should be filtered below the new niquist first. 
	 * @param timeSignal, length must be even.
	 * @return downsampledSignal
	 */
	private float[] downSample(float[] timeSignal){
		int len = timeSignal.length/2;
		float[] downSignal = new float[len];
		for (int i=0; i<len; i++){
			downSignal[i]=timeSignal[2*i];
		}
		return downSignal;
	}

	public float[] getSpectrum() {
		return spectrum;
	}
	
}
