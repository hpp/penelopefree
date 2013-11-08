package com.hpp.dsp;

import com.harmonicprocesses.penelopefree.audio.AudioConstants;

public class PitchCorrection {

	public float[][] timeSignals;
	private double[] phase, notes, phaseInc;
	private float[] lastAmplitude;
	public int numSamples, numFilters;
	private double threshHold;
	

	public PitchCorrection(int NumFilters, double BaseNote, double ThreshHold) {
		numFilters = NumFilters;
		phase = new double[numFilters];
		notes = new double[numFilters];
		phaseInc = new double[numFilters];
		numSamples = 0;
		threshHold=ThreshHold;
		calcNotes(BaseNote);
		lastAmplitude = new float[numFilters];
	}

	private void calcNotes(double baseNote) {
		for (int i=0; i<numFilters; i++){
			notes[i] = baseNote * Math.pow(2.0, (double) i / 12.0 );
			phaseInc[i] =  2.0 * Math.PI * notes[i] / (double) AudioConstants.sampleRate;
		}
	}

	public void next(int NumSamples, float[] amplitude) {
		double ampDiff, timeDiff, time, amp;
		if (NumSamples != numSamples){
			numSamples = NumSamples;
			timeSignals = new float[numFilters][numSamples];
		}
		for (int i=0; i<numFilters; i++){
			if (amplitude[i]<threshHold) {
				lastAmplitude[i] = 0;
				timeSignals[i] = new float[numSamples];
				continue;
			}
			ampDiff = amplitude[i]-lastAmplitude[i];
			timeDiff = (double) i / (double) numSamples;
			time = 0;
			for (int j=0; j<numSamples; j++){
				phase[i] += phaseInc[i];
		    	if (phase[i] >= 2.0 * Math.PI) {phase[i] %= 2.0 * Math.PI;}
		    	amp = (ampDiff*time) + amplitude[i];
		    	time += timeDiff;
				timeSignals[i][j]=(float) (amp * Math.sin(phase[i]));
			}
			lastAmplitude[i] = amplitude[i];
		}
	}
	
}
