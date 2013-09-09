
	private void MoveParticiles(int mode, float radius, float amplitude) {
		int k = 0;
		if (mode==0) {Shake(radius); return;}
		
		float[][] nodes = ModeNodes.GetNodes(mode); 
		for (int i = 0; i<particleCoords.length;){
			float x = particleCoords[i];
			float y = particleCoords[i+1];
			double angle;
			float force;
			float damping; 
			
			// move particle inside the circle if out
			float dist2center = SoundParticleHexBins.distance(0.0f, 0.0f, x,y);
			if (dist2center>radius) {
				angle = Math.atan2(y,x);
				x = (float) (radius*Math.cos(angle));
				y = (float) (radius*Math.sin(angle));
			}
			
			// find the closest node
			float dist = 100; 
			int node = 0;
			float nodeX = 0;
			float nodeY = 0;
			if (nodes==null){
				angle = Math.atan2(y,x);
				int happy = (int) angle;
			}
			for (int j = 0; j<nodes.length; j++){
				nodeX = nodes[j][0]*radius;
				nodeY = nodes[j][1]*radius;
				float newDis = SoundParticleHexBins.distance(nodeX,nodeY,x,y);
				if (newDis < dist) { dist = newDis; node = j;}
			}
			angle = Math.atan2((y-(nodes[node][1])),(x-(nodes[node][0])));
			
			float nodeRadius = radius*ModeNodes.GetNodeRadius(mode);
			/* calculate force from node
			nodeRadius = radius/mode;
			if (dist>=nodeRadius) {
				force = 1.0f;
				angle = 2.0f*Math.PI*Math.random();
				damping = 0.98f; // no damping
			} else {
				force = (dist/nodeRadius);
				damping = 0.98f;
			}//*/
			
			//* calculate distance from node
			force = amplitude;
	
			//Log.d("com.hpp.penny.openGL.shapes.BaseParticles","amplitude="+amplitude );
			
			angle = 2.0f*Math.PI*Math.random(); //new random angle
			if (force>MyGLSurfaceView.AMPLITUDE_THRESHOLD){
				if (towardNodes<0){
					damping = (float) (1 - Math.pow(Math.cos(2.0f*Math.PI*dist/nodeRadius),3));
				} else {
					damping = (float) (1 - Math.pow(Math.sin(2.0f*Math.PI*dist/nodeRadius),3));
				}
			} else {
				damping = 0;
			}
			
			//*/
			
			// update particleMomentum
			particleMomentums[k] = (float) (towardNodes*force*Math.cos(angle)+
					damping*particleMomentums[k]);
			particleMomentums[k+1] = (float) (towardNodes*force*Math.sin(angle)+
					damping*particleMomentums[k+1]);
			
			// update particlePosition
			x+=particleMomentums[k++]/100;
			y+=particleMomentums[k++]/100;
			
			// check if particle is outside of circle
			if (SoundParticleHexBins.distance(0.0f, 0.0f, x,y)>radius){
				angle = Math.atan2(y,x);
				x = (float) (radius*Math.cos(angle));
				y = (float) (radius*Math.sin(angle));
				particleMomentums[k-2]=0;
				particleMomentums[k-1]=0;
			}
			
			// push position to particleCoods array
			particleCoords[i++] = x; // x iter to y
			particleCoords[i++] = y; // y iter to z
			i++; // skip z
			
		}
		
		// clear certexBuffer
		vertexBuffer.clear();
		// add the coordinates to the FloatBuffer
	    vertexBuffer.put(particleCoords);
	    // set the buffer to read the first coordinate
	    vertexBuffer.position(0);
		
	}
