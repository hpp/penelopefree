package com.harmonicprocesses.penelopefree.openGL.utils;

public class ModeNodes {
	
	final static int x=0, y=1;
	
	public ModeNodes(){
		
	}
	
	/**
	 * Get Node locations based on mode
	 *  currently set for 1 through 4
	 *  where number of nodes = 2^(mode-1)
	 * @param mode, the octave of resonation
	 * @return Node array of x y pairs, 
	 */
	public static float[][] GetNodes(int mode){
		if (mode>4 || mode<1) {return null;} //having a wierd zero value passed
		
		float[][] Nodes = new float[(int) Math.pow(2, mode-1)][2];
		if (mode<=1){
			Nodes[0][x]=0; Nodes[0][y]=0;
		} else if(mode == 2) {
			Nodes[0][x]=-1.0f/3.0f; Nodes[0][y]=0;
			Nodes[1][x]=1.0f/3.0f; Nodes[1][y]=0;
		} else if(mode == 3) {
			float temp = (float) (1.0f/(2.0f*Math.cos(Math.PI/4.0f)+1.0f));
			Nodes[0][x]=temp; Nodes[0][y]=0;
			Nodes[1][x]=-temp; Nodes[1][y]=0;
			Nodes[2][x]=0; Nodes[2][y]=temp;
			Nodes[3][x]=0; Nodes[3][y]=-temp;
		} else if(mode == 4) {
			float temp0 = (float) (1.0f/
					(Math.cos(3.0f*Math.PI/8.0f)+Math.sin(3.0f*Math.PI/8.0f)+1.0f));
			float temp1 = (float) (temp0*Math.cos(3.0f*Math.PI/8.0f));
			float temp2 = (float) (temp0*Math.sin(3.0f*Math.PI/8.0f));
			Nodes[0][x]=-temp1-temp2; Nodes[0][y]=0;
			Nodes[1][x]=-temp2; Nodes[1][y]=temp2;
			Nodes[2][x]=0; Nodes[2][y]=temp1+temp2;
			Nodes[3][x]=temp2; Nodes[3][y]=temp2;
			Nodes[4][x]=temp1+temp2; Nodes[4][y]=0;
			Nodes[5][x]=temp2; Nodes[5][y]=-temp2;
			Nodes[6][x]=0; Nodes[6][y]=-temp1-temp2;
			Nodes[7][x]=-temp2; Nodes[7][y]=-temp2;
		}	
		
		return Nodes;
		
	}
	
	static public float GetNodeRadius(int mode){
		float radius = 1.0f;
		
		if (mode>4 || mode<1) {return 0.0f;} //error checker
		
		if (mode<=2){
			radius = 2.0f/3.0f;
		} else if (mode<=3){
			radius = (float) (2.0f/
					(2.0f*Math.cos(Math.PI/4)+1));
		} else if (mode<=4){
			radius = (float) (1.0f/
					(Math.cos(3.0f*Math.PI/8.0f)+Math.sin(3.0f*Math.PI/8.0f)+1.0f));
		}
		return radius;
	}
}
