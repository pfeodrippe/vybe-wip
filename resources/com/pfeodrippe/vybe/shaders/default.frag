#ifdef GL_ES
precision mediump float;
#endif

varying vec2 v_texCoords;
varying vec4 v_color;
uniform sampler2D u_texture;

void main() {
    gl_FragColor = v_color * texture2D(u_texture, v_texCoords);

    //gl_FragColor = vec4(v_texCoords.x/0.1, v_texCoords.y/0.05, 0.0, 1.0);
    //gl_FragColor = vec4(v_texCoords.x, v_texCoords.y, 0.0, 1.0);

    //gl_FragColor = vec4(gl_FragCoord.x*30., gl_FragCoord.y/200.0, 0.0, 1.0);
}
