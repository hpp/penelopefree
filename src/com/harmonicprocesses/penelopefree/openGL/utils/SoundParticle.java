package com.harmonicprocesses.penelopefree.openGL.utils;


public class SoundParticle { 
	float launchAngle; //angle of depature
	float theta; // current angle 0-2pi
	float furlong; //length of each stretch
	float distance2edge; //length left on current stretch
	float delta; //distance wave travels in medium per iteration
	public float[] location; //current location of particle
	float amplitude;
	int x,y;
	
	public SoundParticle(float Delta, float Amplitude){
		delta = Delta;
		float minRand = (float) (Math.acos(-delta/2)/Math.PI-0.5);
		int whileBreak = 0;
		while (whileBreak==0){
			float rand = (float) Math.random();
			if (rand>=minRand && rand<=1-minRand){
				launchAngle = (float) ((rand+0.5)*Math.PI);
				whileBreak=1;
			}
		}
		theta = launchAngle; // current angle 0-2pi
		furlong = Math.abs((float) (2.0f*Math.cos(launchAngle)));
		if (furlong == 0){
			furlong = 1;
		}
		distance2edge = furlong;
		location = new float[2];
		location[0]=1;
		location[1]=0;
		amplitude=Amplitude;
		x=0;y=1; //set location indexes
	}
	
	public float[] next(){
		if (distance2edge<delta){
			return turn(delta-distance2edge);
		}
		//update location
		location[x] += (float) (delta*Math.cos(theta));
		location[y] += (float) (delta*Math.sin(theta));
		distance2edge -= delta;
		return location;
	}
	
	private float[] turn(float distancePastEdge){
		// update location at edge
		location[x]+=(float) (distance2edge*Math.cos(theta));
		location[y]+=(float) (distance2edge*Math.sin(theta));
		
		// calc new theta
		theta += 2*launchAngle-Math.PI;
		/*if (location[x]>0 && location[y]>0){
			theta = (float) (theta - Math.PI + 2*launchAngle);	
		} else if (location[x]<0 && location[y]>0){
			// TODO finish quadrant turning
			theta = (float) (Math.PI + theta + 2*(Math.acos(location[x])));	
		} else if (location[x]<0 && location[y]<0){
			theta = (float) (Math.PI + theta + 2*(Math.acos(location[x])));	
		} else if (location[x]>0 && location[y]<0){
			theta = (float) (Math.PI + theta + 2*(Math.acos(location[x])));	
		}*/
		
		// update distance to edge
		distance2edge = furlong;
		
		// check if we need to turn again		
		if (distance2edge<distancePastEdge){
			return turn(distancePastEdge-distance2edge);
		}
		//update location
		location[x] += (float) (distancePastEdge*Math.cos(theta));
		location[y] += (float) (distancePastEdge*Math.sin(theta));
		distance2edge -= distancePastEdge;
		return location;
	}
	
}


