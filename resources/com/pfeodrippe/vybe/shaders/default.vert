attribute vec4 a_position;
attribute vec4 a_color;
attribute vec2 a_texCoord0;
attribute vec2 a_resolution;
attribute float a_stroke;
attribute float a_default;
uniform mat4 u_projTrans;
varying vec4 v_color;
varying vec2 v_texCoords;
varying vec2 v_resolution;
varying float v_stroke;
varying float v_default;
uniform float u_time;

void main() {
     v_color = a_color;
     v_color.a = v_color.a * (255.0/254.0);
     v_resolution = a_resolution;
     v_texCoords = a_texCoord0;
     v_stroke = a_stroke;

     v_default = a_default;

     gl_Position =  u_projTrans * a_position;
}
