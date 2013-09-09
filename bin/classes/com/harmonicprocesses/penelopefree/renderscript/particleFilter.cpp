// This matrix member variable provides a hook to manipulate
// the coordinates of the objects that use this vertex shader
uniform mat4 uMVPMatrix;
attribute vec4 vPosition; //[x,y,amplitude]
attribute vec4 inits; //[lauchAngle, theta, distance2edge]
vec4 location;
float launchAngle;
float theta;
float furlong;
float distance2edge;
float delta = " + Delta + ";
float amplitude;
bool initiallized = false;
vec4 getNextPosition();
vec4 getTurnPosition(float distancePastEdge);
void init();

void main() {
// the matrix must be included as a modifier of gl_Position
	if (initiallized==false) { init(); initiallized=true; }\
	gl_Position = getNextPosition() * uMVPMatrix;
	gl_PointSize = 2.0;
}

vec4 getNextPosition() {
	if (distance2edge<delta){
		return getTurnPosition(delta-distance2edge);
	}
	location[0] = location[0] + (delta * cos(theta));
	location[1] = location[1] + (delta * sin(theta));
	distance2edge = distance2edge - delta; 

	return location;
}

vec4 getTurnPosition(float distancePastEdge) {
	// update location at edge
	location[0] = location[0] + (distance2edge * cos(theta));
	location[1] = sin(theta) * distance2edge + location[1];

	// calc new theta
	theta = 2.0 * launchAngle + theta - 3.14159265359;
	distance2edge = furlong;
	//if (distance2edge<distancePastEdge){
	//	return getTurnPosition(distancePastEdge-distance2edge);
	//}
	
	location[0] = cos(theta) * distancePastEdge + location[0];
	location[1] = sin(theta) * distancePastEdge + location[1];
	distance2edge = distance2edge - distancePastEdge;
	return location;
}

void init() {
	location[0] = vPosition[0];
	location[1] = vPosition[1];
	amplitude = vPosition[2];
	launchAngle = inits[0];
	theta = inits[1];
	furlong = abs( 2.0 * cos(launchAngle));
	distance2edge = inits[2];
}