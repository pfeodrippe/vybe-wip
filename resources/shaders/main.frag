#ifdef GL_ES
precision mediump float;
#endif

#include "lygia/space/ratio.glsl"
#include "lygia/math/absi.glsl"
#include "lygia/sdf/raysSDF.glsl"
#include "lygia/math/decimate.glsl"
#include "lygia/draw/circle.glsl"
#include "lygia/draw/rect.glsl"

varying vec4 v_color;
varying vec2 v_texCoords;
uniform sampler2D u_texture;
uniform float u_time;
uniform vec3 u_bg_color;
uniform vec2 u_cursor;

// TODO: Fix resolution

void main() {
     vec2 eita = vec2(600.0, 600.0);
     vec3 color = u_bg_color;
     float cursor;
     vec2 st = gl_FragCoord.xy/eita;
     vec2 cursor_pos = st - u_cursor;
     // st = ratio(st, eita);

     /* cursor = circle(cursor_pos, .02); */
     /* cursor += circle(cursor_pos, .1, 0.02); */

     // color += rect(st + vec2(-0.008, 0.008), vec2(0.5, 0.5)) * vec3(1.0, 1., 0.);
     // color += rect(st, vec2(0.5, 0.5));

     /* if (cursor > 0.0) { */
     /*     gl_FragColor = vec4(0.0, 0.0, 0.0, 1.0) * cursor; */
     /* } else { */
     /*     gl_FragColor = vec4(color, 1.0); */
     /* } */

     // gl_FragColor = texture2D(u_texture, v_texCoords);
     //gl_FragColor = vec4(v_texCoords, 1., 1.) * v_color;
     gl_FragColor = v_color;
}
