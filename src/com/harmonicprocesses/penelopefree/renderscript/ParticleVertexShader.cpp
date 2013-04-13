// This matrix member variable provides a hook to manipulate
// the coordinates of the objects that use this vertex shader
float uMVPMatrix;
float vPosition;
float inits;
float theta;
float furlong;
float distance2edge;
float delta = " + Delta + ";
float amplitude;
bool initiallized = false;

void main() {
// the matrix must be included as a modifier of gl_Position
	if (initiallized==false) { init(); initiallized=true; };
	gl_Position = getNextPosition() * uMVPMatrix;
	gl_PointSize = 2.0;
}

float getNextPosition() {
	if (distance2edge<delta){
		return getTurnPosition(delta-distance2edge);
	};
	vPosition[0] = vPosition[0] + (delta * cos(theta));
	vPosition[1] = vPosition[1] + (delta * sin(theta));
	distance2edge = distance2edge - delta;

	return vPosition;
}

void getTurnPosition(float distancePastEdge) {
	// update location at edge
	vPosition[0] = vPosition[0] + (distance2edge * cos(theta));
	vPosition[1] = sin(theta) * distance2edge + vPosition[1];

	// calc new theta
	theta = 2 * launchAngle + theta -3.14159265359;
	distance2edge = furlong;
	if (distance2edge<distancePastEdge){
		return getTurnPosition(distancePastEdge-distance2edge);
	};

	vPosition[0] = cos(theta) * distancePastedge + vPosition[0];
	vPosition[1] = sin(theta) * distancePastedge + vPosition[1];
	distance2edge = distance2edge - distancePastEdge;
	return vPosition;
}

void init() {
	amplitude = vPosition[2];
	launchAngle = inits[0];
	theta = inits[1];
	furlong = abs( 2.0 * cos(launchAngle));
	distance2edge = inits[2];
}
