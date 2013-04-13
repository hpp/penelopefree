package com.harmonicprocesses.penelopefree.openGL.utils;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class Accelmeter {
	private SensorManager mSensorManager;
	private Sensor accelSensor;
	private float[] gravity;
	public float[] linear_acceleration;

	public Accelmeter(Context context){
		mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
		accelSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		gravity = new float[3];
		linear_acceleration = new float[3];
	}
	
	public void start(){
		mSensorManager.registerListener(accelListener, accelSensor, SensorManager.SENSOR_DELAY_UI);
	}
	
	public void stop(){
		mSensorManager.unregisterListener(accelListener, accelSensor);
	}
	
	private SensorEventListener accelListener = new SensorEventListener(){

		@Override
		public void onSensorChanged(SensorEvent event) {
			  // In this example, alpha is calculated as t / (t + dT),
			  // where t is the low-pass filter's time-constant and
			  // dT is the event delivery rate.

			  final float alpha = 0.8f;

			  // Isolate the force of gravity with the low-pass filter.
			  gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
			  gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
			  gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];

			  // Remove the gravity contribution with the high-pass filter.
			  linear_acceleration[0] = event.values[0] - gravity[0];
			  linear_acceleration[1] = event.values[1] - gravity[1];
			  linear_acceleration[2] = event.values[2] - gravity[2];
			
		}

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
			// TODO Auto-generated method stub
			
		}
		
	};
	
	
}
