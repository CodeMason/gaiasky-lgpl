#ifdef GL_ES
precision mediump float;
precision mediump int;
#endif

uniform float u_ar;

varying vec4 v_col;
void main() {
	vec2 uv = vec2(gl_PointCoord.s, gl_PointCoord.t);
	uv.y = uv.y / u_ar;
	float dist = distance(vec2(0.5), uv) * 2.0;
    gl_FragColor = vec4(v_col.rgb, v_col.a * pow(1.0 - dist, 4.0) * 1.5);
    
    // Prevent saturation
    gl_FragColor = clamp(gl_FragColor, 0.0, 1.0);
    gl_FragColor.rgb *= 0.98;
}