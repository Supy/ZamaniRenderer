void main(void)
{
   gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;
   gl_FrontColor = vec4(0.8,0.2,0,0.2);
}