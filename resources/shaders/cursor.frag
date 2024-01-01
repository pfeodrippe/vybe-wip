#ifdef GL_ES
precision mediump float;
#endif

#include "../com/pfeodrippe/vybe/shaders/lygia/draw/circle.glsl"

varying vec4 v_color;
varying vec2 v_texCoords;
uniform sampler2D u_texture;
uniform float u_time;
uniform vec2 u_cursor;

void main() {
    u_texture;
    vec2 st = v_texCoords.xy;
    vec2 cursor_pos = st - u_cursor;
    float cursor;

    cursor = circle(cursor_pos, .012);
    //cursor += circle(cursor_pos, .04, 0.007);

    if (cursor > 0.0) {
        gl_FragColor = vec4(0.0, 0.0, 0.0, 0.6);
    } else {
        gl_FragColor = vec4(0.0, 0.0, 0.0, .0);
    }
}
