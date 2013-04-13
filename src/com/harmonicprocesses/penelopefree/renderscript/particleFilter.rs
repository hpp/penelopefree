/*
*
*/

#pragma version(1)
#pragma rs java_package_name(com.harmonicprocesses.penelopefree.renderscript)

typedef struct particle {
	float x;
	float y;
	float launchAngle;
	float theta;
	float furlong;
	float distance2edge;
	float delta;
	float amplitude;
} particle_t;

particle_t vin;
void getNextPosition();
void getTurnPosition(float distancePastEdge);



void root(const particle_t *v_in, particle_t *v_out) {
// the matrix must be included as a modifier of gl_Position
	vin = *v_in;
	getNextPosition();
	*v_out = vin;
}

void getNextPosition() {
	if (vin.distance2edge<vin.delta){
		return getTurnPosition(vin.delta-vin.distance2edge);
	}
	vin.x = vin.x + (vin.delta * cos(vin.theta));
	vin.y = vin.y + (vin.delta * sin(vin.theta));
	vin.distance2edge = vin.distance2edge - vin.delta; 

	//return location;
}

void getTurnPosition(float distancePastEdge) {
	// update location at edge
	vin.x = vin.x + (vin.distance2edge * cos(vin.theta));
	vin.y = sin(vin.theta) * vin.distance2edge + vin.y;

	// calc new theta
	vin.theta = 2.0 * vin.launchAngle + vin.theta - 3.14159265359;
	vin.distance2edge = vin.furlong;
	//if (distance2edge<distancePastEdge){
	//	return getTurnPosition(distancePastEdge-distance2edge);
	//}

	vin.x = cos(vin.theta) * distancePastEdge + vin.x;
	vin.y = sin(vin.theta) * distancePastEdge + vin.y;
	vin.distance2edge = vin.distance2edge - distancePastEdge;
	//return location;
}
