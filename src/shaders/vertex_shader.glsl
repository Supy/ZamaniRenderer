varying vec3 normal, light0Dir, light0hv, light1Dir, light1hv;

void main(void){
	vec4 vertCameraPos = gl_ModelViewMatrix * gl_Vertex;
	normal = gl_NormalMatrix * gl_Normal;

	// Lights direction and half vectors.
	light0Dir = (gl_LightSource[0].position-vertCameraPos).xyz;
	light0hv = gl_LightSource[0].halfVector.xyz;

	light1Dir = (gl_LightSource[1].position-vertCameraPos).xyz;
	light1hv = gl_LightSource[1].halfVector.xyz;

	gl_Position = ftransform();
}
