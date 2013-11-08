package com.harmonicprocesses.penelopefree.audio;

public class AudioConstants {
	public final static String AudioThruThreadName = "AudioThru";
	public static final String AudioProcessorThreadName = "AudioProcessor";
	
	public final static int sampleRate = 44100;
	public final static int AudioSpectrumWhat = 16;
	

	public final static int numBytePerFrame = 2; //two bytes per frame, pcm16 and monoChannel
	public final static int numChannels = 1; //set for mono both record and track for now
	
	public static int defaultBufferSize = 512;
	public float wetDry = 0.5f;
	
	public final static double AMPLITUDE_THRESHOLD = 0.001; //used in audio processing 
	public final static double[] quarterFilterACoeffs = {1,-1.79378,0.813395},
			 quarterFilterBCoeffs= {0.0191969,-0.0187761,0.0191969};
	public final static double[][] noteACoefs = {{1, -1.424159446112778, 0.9971662912449828},
				 {1, -1.357852072965285, 0.9969979814465472},
				 {1, -1.284452441645767, 0.9968196827113388},
				 {1, -1.203345398277623, 0.9966308026461319},
				 {1, -1.11390164731306, 0.9964307136781321},
				 {1, -1.015490131751931, 0.9962187509408957},
				 {1, -0.9074948982733048, 0.9959942100265745},
				 {1, -0.7893374991218262, 0.9957563445945627},
				 {1, -0.6605061415599149, 0.9955043638256239},
				 {1, -0.5205929463396142, 0.9952374297093814},
				 {1, -0.3693407992191619, 0.9949546541517433},
				 {1, -0.2067013439934402, 0.994655095887252}},
				 noteBCoefs = {{0.001416854377508723, 0, -0.001416854377508723},
					 {0.001501009276726475, 0, -0.001501009276726475},
					 {0.001590158644330594, 0, -0.001590158644330594},
					 {0.001684598676934067, 0, -0.001684598676934067},
					 {0.001784643160934056, 0, -0.001784643160934056},
					 {0.001890624529552116, 0, -0.001890624529552116},
					 {0.002002894986712778, 0, -0.002002894986712778},
					 {0.002121827702718511, 0, -0.002121827702718511},
					 {0.002247818087188022, 0, -0.002247818087188022},
					 {0.002381285145309144, 0, -0.002381285145309144},
					 {0.002522672924128256, 0, -0.002522672924128256},
					 {0.002672452056374079, 0, -0.002672452056374079}};
	
	public static int getFloatBufferSize(int buffSize) {
		int floatBufferSize = (defaultBufferSize*buffSize)/(numBytePerFrame*numChannels);
		return floatBufferSize;
	}
	
	public static int getBufferSize(int buffSize) {
		int bufferSize = (defaultBufferSize*buffSize);
		return bufferSize;
	}
}
