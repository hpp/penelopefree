package com.harmonicprocesses.penelopefree.audio;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;

import com.harmonicprocesses.penelopefree.R;


import android.content.Context;

public class DSPEngine {
	private float[][] avgLadder, detLadder, twiddleFactor;
	private float[][] sampleCirBuffer, lastLPSample; 
	public int[] noteFactor;
	private final int real = 0, twidIdx = 0;
	private final int imag = 1, jIdx = 1;
	private float[] spectrum, freqSpec, imagSpectrum, realSpectrum;
	private int[] ladderIndex= {0,0,0,0,0,0,0,0}, partNoteFFTIndex;
	private int bufferSize, numCoeffs = 3, numFilters = 96;
	private int sampleFreq;
	private int[][] twiddleIdx = new int[12*8][2];
	private int[][] phaseCirBuffer;
	private float[] bCoeffs = {0.2928932188134524f, 0.5857864376269047f, 0.2928932188134524f};
	private float[] aCoeffs = {1.0f, -1.387778780781446e-16f, 0.1715728752538099f};
	private double[][] coeffsA, coeffsB, filterPad;
	private float[] oldSamples;
	private Context mContext;
	
	
	public DSPEngine(int BufferSize,int sampleRate, Context context){
		mContext = context;
		sampleFreq = sampleRate;
		bufferSize = BufferSize;
		detLadder = avgLadder = new float[8][bufferSize*2];
		sampleCirBuffer = new float[bufferSize][8];
		phaseCirBuffer= new int[96][bufferSize];
		realSpectrum = imagSpectrum = new float[96];
		twiddleFactor = calcTwiddleeDees(bufferSize);
		noteFactor = calcNoteBins(bufferSize, sampleRate); // set freqSpec in caclNoteBins
		spectrum = new float[96]; //base on 12 notes per 8 octaves
		partNoteFFTIndex = new int[8];
		lastLPSample = new float[8][4];
		coeffsA = readCoeffFile(R.raw.coeffs_a_36);
		coeffsB = readCoeffFile(R.raw.coeffs_b_36);
		filterPad = new double[numFilters][numCoeffs];
		oldSamples = new float[numCoeffs];
	}
	
	public float[] newSamples(float[] samples, int mode) throws Exception{
		
		//float[] sampleHP = new float[samples.length];
		
		//dual sum samples and add to ladder
		float[] sampleLP = LowPass(samples, mode);
		float[] sampleHP = DualSum(Subtract(samples,sampleLP));
		sampleLP = DualSum(sampleLP);
		
		int N = bufferSize>>1;
		for (int i = 0;i<N; i++){
			avgLadder[mode][i+(ladderIndex[mode]*N)] = sampleLP[i];
			detLadder[mode][i+(ladderIndex[mode]*N)] = sampleHP[i];
		}
		ladderIndex[mode]++; 
		if (ladderIndex[mode]>=4) {
			throw new Exception("Overran modeLadderIndexBuffer");
		}
	
		//Check OF
		if (mode==0){ //only once per cycle
			for (int rung = 0;rung<8;rung++){ 
				if (ladderIndex[rung] >= 2) { //Run the first ready buffer					 
					//calc part FFT 1/4 to 1/2 of samples
					// store return in proper place in spectrum
					PartFFT(detLadder[rung], rung);
					
					//use what was run to calculate lower freqs on next rung
					if (rung<=6){
						newSamples(avgLadder[rung], rung+1);
					}
					
					//we ran the ladder so shift it
					for (int j=0;j<bufferSize;j++){
						detLadder[rung][j]=detLadder[rung][j+bufferSize]; //shift cache down
						avgLadder[rung][j]=avgLadder[rung][j+bufferSize]; //shift cache down
					}
					ladderIndex[rung]-=2;
					break; //only once per cycle
				}
			}
		}
	
		//return spectrum
		return spectrum;
	}
	
	public float[] updateSpectrum(float[] samples, int mode) throws Exception{
		//float[] sampleHP = new float[samples.length];

		//dual sum samples and add to ladder
		float[] sampleLP = LowPass(samples, mode);
		PartNoteFFT(Subtract(samples,sampleLP), mode);
		float[] downSample = DualSum(sampleLP);
		
		/*
		int N = bufferSize>>1;
		for (int i = 0;i<N; i++){
			avgLadder[mode][i+(ladderIndex[mode]*N)] = sampleLP[i];
			detLadder[mode][i+(ladderIndex[mode]*N)] = sampleHP[i];
		}
		ladderIndex[mode]++; 
		if (ladderIndex[mode]>=4) {
			throw new Exception("Overran modeLadderIndexBuffer");
		}*/
			
		//Check OF
		if (mode<7){
			updateSpectrum(downSample, mode+1);
		}
		//PartNoteFFT(sampleHP, mode);
		
	
		//return spectrum
		return spectrum;
	}


	float[] Decibels(float[] sample) {
		for (float samples:sample){
			samples = (float) (Math.log10(samples/16.67047))/100;
		}
				
		return sample;
	}

	float[] DualSum(float[] samples){
		int N = samples.length>>1;
		float[] output = new float[N];
		for (int i = 0; i < N; i++){
			output[i] = (float) (samples[2*i] + samples[2*i+1]);
		}
		return output;
	}
	
	float[] DualDiff(float[] samples){
		for (int i = 0; i < samples.length>>1; i++){
			samples[i] = (float) (samples[2*i] - samples[2*i+1]);
		}
		return samples;
	}

	float[] FFT(float[] samples){
		int N = bufferSize;
		float[] spectrum = new float[N];
		int g = N>>1;
				
		//TODO widdle this down
		for (int i = 0; i < N; i++){
			float realSpectrum = 0;
			float imagSpectrum = 0;
			for (int j = 0; j < N; j++){
				//realSpectrum += samples[j] * twiddleFactor[i][j][real];
				//imagSpectrum += samples[j] * twiddleFactor[i][j][imag];
				realSpectrum += samples[j] * twiddleFactor[(int) ((i*j%N)/N)*360][real];
				imagSpectrum += samples[j] * twiddleFactor[(int) ((i*j%N)/N)*360][imag];
			}
			realSpectrum /= N;
			imagSpectrum /= N;
			spectrum[i] = (float) Math.sqrt(realSpectrum*realSpectrum + imagSpectrum*imagSpectrum);
		}
		return spectrum;
	}
	
	/**
	 * Calculate the FFT on samples from the 1/4(+1) mark to
	 * the 1/2 mark, which is a single octave.
	 * @param samples
	 * @return
	 */
	float[] PartFFT(float[] samples, int mode){
		int N = bufferSize, firstIdx = (bufferSize>>2)+1, lastIdx =bufferSize>>1;
		int aNote = noteFactor[firstIdx], num2Norm = 0, specIdx=0;
		float[] decayedSpectrum = new float[spectrum.length];
		float decayFactor = 0.0001f;
		
		//decay all
		for (int i = 0; i<spectrum.length; i++){
			if (spectrum[i]>decayFactor){
				spectrum[i]-=decayFactor;
			}
		}
		
		//set spectrum to zero, TODO addd a persist decay thing
		for (int i = 0; i<12; i++){
			if (Math.abs(spectrum[(7-mode)*12+i])>decayFactor);
				decayedSpectrum[(7-mode)*12+i] = spectrum[(7-mode)*12+i]; //decay by one...
			spectrum[(7-mode)*12+i] = 0;
		}
		
		//TODO twiddle this down sum more
		for (int i = firstIdx; i <= lastIdx; i++){
			float realSpectrum = 0;
			float imagSpectrum = 0;
			for (int j = 0; j < N; j++){
				//realSpectrum += samples[j] * twiddleFactor[i][j][real];
				//imagSpectrum += samples[j] * twiddleFactor[i][j][imag];
				int temp =(int) ((((float)i*j%N)/(float) N)*360.0);
				realSpectrum += samples[j] * twiddleFactor[temp][real];
				imagSpectrum += samples[j] * twiddleFactor[temp][imag];
			}
			realSpectrum /= N;
			imagSpectrum /= N;
			
			//store in spectrum
			if(aNote!=noteFactor[i]){
				if (num2Norm > 0){
					spectrum[(7-mode)*12+specIdx++]/=num2Norm;
					if (specIdx >= 12) break; //13th step (zero based)
				}
				aNote=noteFactor[i];
				num2Norm = 0;
			}
			spectrum[(7-mode)*12+specIdx]+=(float) Math.sqrt(realSpectrum*realSpectrum + imagSpectrum*imagSpectrum);
			num2Norm++;
		}
		
		for (int i = 0; i<12; i++){
			if (decayedSpectrum[(7-mode)*12+i]>spectrum[(7-mode)*12+i]){
				spectrum[(7-mode)*12+i] = decayedSpectrum[(7-mode)*12+i]; //decay by one..
			}
		}
		
		return spectrum;
	}
	
	/**
	 * Calculate the FFT on samples for a single octave
	 * the 1/4(+1) to 1/2 mark, at the 12 note frequencies.
	 * remove what comes out and add what comes in on each pass.
	 * @param samples is a float buffer with the new samples in it
	 * @param mode is the mode running size of samples is dependent on this 
	 * but a larger samples could be supplied and not used.
	 * @return
	 */
	float[] PartNoteFFT(float[] samples, int mode){
		int N = bufferSize, firstIdx = partNoteFFTIndex[mode], lastIdx = firstIdx + (bufferSize>>mode);
		//int aNote = noteFactor[firstIdx], num2Norm = 0, specIdx=0;
		
		//set spectrum to zero, TODO addd a persist decay thing
		/*for (int i = 0; i<12; i++){
			spectrum[(7-mode)*12+i]=0;
		}*/
		
		//TODO twiddle this down sum more
		for (int i = (7-mode)*12; i < (7-mode)*12+12; i++){ //i is note position 0f 96
			//float realSpectrum = 0;
			//float imagSpectrum = 0;
			
			int k = twiddleIdx[i][jIdx]; //k is j but continued around the clock the number of 
			float normNoteFreq = freqSpec[i]*((float) N / (float) (sampleFreq>>(mode)));
			for (int j = firstIdx; j < lastIdx; j++){ //j is sample position of bufferSize>>mode
				//realSpectrum += samples[j] * twiddleFactor[i][j][real];
				//imagSpectrum += samples[j] * twiddleFactor[i][j][imag];
				twiddleIdx[i][twidIdx] =(int) ((((normNoteFreq*(float)k++)%N)/(float) N)*360.0);
				if (twiddleIdx[i][twidIdx] == 0) {
					twiddleIdx[i][jIdx] = 0; //when returned to zero start over
				} else {
					twiddleIdx[i][jIdx]++;
				}
				realSpectrum[i] -= sampleCirBuffer[j%N][mode] * twiddleFactor[phaseCirBuffer[i][j%N]][real]; //out with the old
				imagSpectrum[i] -= sampleCirBuffer[j%N][mode] * twiddleFactor[phaseCirBuffer[i][j%N]][imag];
				sampleCirBuffer[j%N][mode] = samples[j-firstIdx];
				phaseCirBuffer[i][j%N] = twiddleIdx[i][twidIdx];
				realSpectrum[i] += sampleCirBuffer[j%N][mode] * twiddleFactor[phaseCirBuffer[i][j%N]][real]; //in with the new
				imagSpectrum[i] += sampleCirBuffer[j%N][mode] * twiddleFactor[phaseCirBuffer[i][j%N]][imag];
			}
			
			float rSpec = realSpectrum[i] / N;
			float iSpec = imagSpectrum[i] / N;
			
			spectrum[i] = (float) Math.sqrt(rSpec*rSpec + iSpec*iSpec);
		}
		partNoteFFTIndex[mode] = (lastIdx)%N; //index passed last Idx 
		return spectrum;
	}
	
	float[][] calcTwiddleeDees(int N){
		float[][] twiddleeDee = new float[360][2];
		//TODO widdle this down
		/*for (int i = 0; i < N; i++){
			for (int j = 0; j < N; j++){
				twiddleeDee[i][j][real] = (float) Math.cos(-2*Math.PI*i*j/N);
				twiddleeDee[i][j][imag] = (float) Math.sin(-2*Math.PI*i*j/N);
				//if (imag > real){
				//	twiddleeDee[i][j] = (float) -Math.sqrt(imag-real);
				//} else {
				//	twiddleeDee[i][j] = (float) Math.sqrt(real-imag);
				//}
			}
		}*/
		//new idea just do 360 calcs and use those rounded(remainder(i*j/N)*360)
		for (int i = 0; i < 360; i++){
			twiddleeDee[i][real] = (float) Math.cos(-2.0*Math.PI*(i/360.0));
			twiddleeDee[i][imag] = (float) Math.sin(-2.0*Math.PI*(i/360.0));
		}
		return twiddleeDee;
	}
	
	/**
	 * Calculate which samples go to which note.
	 * enumerate 1 through 12 Gb to F, 
	 * samples bufferSize/4+1 to bufferSize/2 
	 * 
	 * @param bufferSize
	 * @return
	 */
	private int[] calcNoteBins(int bufferSize, float sampleFreq) {
		// start at 440
		float aNote = 440;
		//create bins
		float lowNote = (float) (((bufferSize/4.0)+1.0)*(sampleFreq/bufferSize));
		float highNote = (float) ((bufferSize/2.0)*(sampleFreq/bufferSize));
		while (!(aNote<=highNote & aNote>=lowNote)){
			if (aNote<=lowNote){
				aNote*=2;
			}else if (aNote>=highNote){
				aNote/=2;
			}
		}
		aNote = (float) (aNote * Math.pow(2,-1/24));//shift to base of A
		int chromaticNumber = 0; //number associated with note starting A = 0, Ab=11
		//now aNote is in the right octave so scale down then up
		while (aNote>=lowNote){
			if (chromaticNumber <= 0) {chromaticNumber = 12;}
			chromaticNumber--;
			double temp = Math.pow(2,-1.0/12.0);
			aNote = (float) (aNote * temp);
		}
		
		freqSpec = new float[96];
		freqSpec[0] = (float) (aNote * Math.pow(2,1/24)); //shift up to middle of A
		freqSpec[0] /= (float) Math.pow(2.0,7.0); //shifted down 7 octaves
		//aNote is now the lowest note, so save frequency and scroll up
		for (int i = 1; i < 96; i++){
			freqSpec[i] = (float) (freqSpec[i-1] * Math.pow(2.0,1.0/12.0));
		}
		
		
		int[] noteBins = new int[bufferSize];
		for (int i = (bufferSize/4)+1; i<=bufferSize>>1; i++){
			if (i*(sampleFreq/bufferSize)>=aNote){
				aNote*=(float) Math.pow(2,1/12); //increment up
				chromaticNumber++;
				if (chromaticNumber >= 12) {chromaticNumber = 0;}
				
			}
			noteBins[i]=chromaticNumber;
		}
		
		return noteBins;
	}

	float[] LowPass(float[] samples, int mode){
		float[] output = new float[samples.length];
		
		float a0 = aCoeffs[0], a1 = aCoeffs[1], a2 = aCoeffs[2],
				b0 = bCoeffs[0], b1 = bCoeffs[1], b2 = bCoeffs[2];
		
	
		//zero pad
		output[0] = b0*samples[0]+b1*lastLPSample[mode][0]+b2*lastLPSample[mode][1]-
					a1*lastLPSample[mode][2]-a2*lastLPSample[mode][3];
		output[1] = b0*samples[1]+b1*samples[0]+b2*lastLPSample[mode][0]-
					a1*output[0]-a2*lastLPSample[mode][2];
		for (int i=2; i<samples.length; i++){
			output[i] = b0*samples[i]+b1*samples[i-1]+b2*samples[i-2]-
						a1*output[i-1]-a2*output[i-2];
		}
		lastLPSample[mode][0] = samples[samples.length-1];
		lastLPSample[mode][1] = samples[samples.length-2];
		lastLPSample[mode][2] = output[samples.length-1];
		lastLPSample[mode][3] = output[samples.length-2];
		return output;
	}
	
	private float[] Subtract(float[] LHArray, float[] RHArray) {
		int N = Math.min(LHArray.length,RHArray.length);
		float[] output  = new float[N];
		
		for (int i=0; i<N;i++){
			output[i] = LHArray[i] - RHArray[i];
		}
		return output;
	}
	
	/**
	 * Adapted from http://huuah.com/android-writing-and-reading-files/
	 * @param filename
	 */
	private double[][] readCoeffFile(int fileId){
		// try opening the myfilename.txt
		double[][] output = new double[numFilters][numCoeffs];
		String[][] StringArray = new String[numCoeffs][numFilters];
		try {
			// open the file for reading
			InputStream instream = mContext.getResources().openRawResource(fileId);
	 
			// 	if file the available for reading
			if (instream.available()>0) {
				// prepare the file for reading
				InputStreamReader inputreader = new InputStreamReader(instream);
				BufferedReader buffreader = new BufferedReader(inputreader);
	                 
				String line;
				
	 
				// read every line of the file into the line-variable, on line at the time
				for (int i = 0; i < numCoeffs; i++){
					line = buffreader.readLine();
					// do something with the settings from the file
					StringArray[i] = line.split(" ");
				} 
	 
			}
	     
			// close the file again       
			instream.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}  
		
		for (int i = 0; i<numCoeffs; i++) {
			for (int j = 0; j<numFilters; j++) {
				output[j][i] = Double.valueOf(StringArray[i][j]);
			}
		}
		
		return output;
	}

	/**
	 * takes an array of time domain samples runs them through
	 *  96 band pass filters (one per note, twelve notes per octave)
	 *  returns an array of amplitudes similar to spectrum
	 *  
	 * @param samples
	 * @return amplitudes
	 */
	public float[] filterSamples(float[] samples) {
		float[] output = new float[numFilters];
		double[] input = new double[samples.length];
		for (int i=0;i<samples.length;i++){
			input[i] = samples[i];
		}
		
		//filter buffered samples
		for (int i = 24; i < 72; i++){
			output[i] = filter(input, coeffsB[i], coeffsA[i], i);
		}
		
		//store off old samples for next pass
		for (int j = 0; j<numCoeffs; j++){
			oldSamples[j] = samples[samples.length-j-1]; 
		}
		
		return output;
	}

	/**
	 * Filters a samples stream and returns the largest amplitude based
	 * 
	 *  should work for anysize filter and anysize buffered samples
	 *  
	 * @param samples, input stream
	 * @param b, b filter coeffs
	 * @param a, a filter coeffs
	 * @param k, the key index of the filter
	 * @return amplitude of the filter
	 */
	private float filter(double[] samples, double[] b, double[] a, int k) {
		float maxAmplitude = 0;
		int l = b.length; 
		
		//initiallize filter (zero pad) or continue from stored, or "padded", values
		for (int i = 0; i < l; i ++){
			filterPad[k][i] = 0; //set new index back to tabula rasa, three day hunt for this bug...
			
			for (int j = 0; j<l; j++){ // add up b coeffs times stored data or new samples
				if (i-j<0){
					filterPad[k][i] += b[j]*oldSamples[j-i-1];
				} else {
					filterPad[k][i] += b[j]*samples[i-j];
				}
			}
			
			 
			for (int j = 1; j<l; j++){ // reduce by IIR stored data points
				if (i-j<0){
					filterPad[k][i] -= a[j]*filterPad[k][i+l-j];
				} else {
					filterPad[k][i] -= a[j]*filterPad[k][i-j];
				}
			}
			
			if (abs(filterPad[k][i])>maxAmplitude){
				maxAmplitude = (float) abs(filterPad[k][0]);
			}
		}
		
		// Main filtering algorythm
		for (int i = l; i<samples.length; i++){
			filterPad[k][i%l] = 0; //set new index back to tabula rasa
			
			for (int j = 0; j<l; j++){
				filterPad[k][i%l] += b[j]*samples[i-j];
			}
			for (int j = 1; j<l; j++){
				if (i%l-j<0){
					filterPad[k][i%l] -= a[j]*filterPad[k][i%l+l-j];
				} else {
					filterPad[k][i%l] -= a[j]*filterPad[k][i%l-j];
				}
			}
			
			if (abs(filterPad[k][i%l]) > maxAmplitude) {
				maxAmplitude = (float) abs(filterPad[k][i%l]);
			}
		}
		
		// reset filterPad to 0 index
		double[] temp = new double[l];
		for (int i = 0; i < l; i++){
			temp[i] = filterPad[k][(samples.length+i)%l];
		}
		filterPad[k] = temp;

		
		return maxAmplitude;
	}
	
	public double abs(double n){
		if (n>0.0){
			return n;
		} 
		return -n;
	}
	
}



