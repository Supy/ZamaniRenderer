varying vec3 normal, light0Dir, light0hv, light1Dir, light1hv;

void main(void)
{

	// World ambience
	vec4 color = gl_FrontMaterial.ambient * gl_LightModel.ambient;

	vec3 normalizedN = normalize(normal);

	/*	Light 0	*/
	// Diffuse angle
	float diffAngle = max(dot(normalizedN, normalize(light0Dir)), 0.0);

	if(diffAngle > 0.0){
		// Diffuse colour
		color += (diffAngle * gl_LightSource[0].diffuse);

		// Specular angle
		float specAngle = max(dot(normalizedN, normalize(light0hv)), 0.0);
		color += gl_LightSource[0].specular * pow(specAngle, 20);
	}

	/*	Light 1	*/
	diffAngle = max(dot(normalizedN, normalize(light1Dir)), 0.0);

	if(diffAngle > 0.0){
		color += (diffAngle * gl_LightSource[1].diffuse);

		float specAngle = max(dot(normalizedN, normalize(light1hv)), 0.0);
		color += gl_LightSource[1].specular * pow(specAngle, 20);
	}


	gl_FragColor = color;
}
