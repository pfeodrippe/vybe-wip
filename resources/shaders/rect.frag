#ifdef GL_ES
precision mediump float;
#endif

#include "../com/pfeodrippe/vybe/shaders/lygia/draw/rect.glsl"
#include "../com/pfeodrippe/vybe/shaders/lygia/space/ratio.glsl"

varying vec4 v_color;
varying vec2 v_texCoords;
varying vec2 v_resolution;
varying float v_stroke;

varying float v_default;

uniform sampler2D u_texture;

void main() {
    if (v_default == 1.0) {
        gl_FragColor = v_color * texture2D(u_texture, v_texCoords);
        return;
    }

    vec2 st = v_texCoords.xy;
    //vec2 st = gl_FragCoord.xy / v_resolution;
    //st = ratio(st, v_resolution);

    //gl_FragColor = vec4(1.0, 1.0, 0.0, 1.0) * rect(st, vec2(0.1, 0.4), 0.15);

    float x = st.x;
    float y = st.y;
    float stroke = v_stroke * 200. / v_resolution.x;
    float y_ratio = stroke * v_resolution.x/v_resolution.y;
    if (x <= stroke || x >= 1. - stroke || y <= y_ratio || y >= 1. - y_ratio) {
        gl_FragColor = v_color;
        //gl_FragColor = vec4(v_texCoords.x, v_texCoords.y, 0.0, 1.0);
    } else {
        gl_FragColor = vec4(0.0);

    }

    //gl_FragColor = vec4(st.x,st.y,0.0,1.0);

    //gl_FragColor = v_color * rect(st, vec2(1.0, 1.0), 0.05);
}
