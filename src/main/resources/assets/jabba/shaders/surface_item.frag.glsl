#version 120

uniform sampler2D u_Texture;
uniform sampler2D u_Lightmap;
uniform float u_AlphaCutoff;

varying vec2 v_TexCoord;
varying vec2 v_LightCoord;
varying vec4 v_Color;

void main() {
    vec4 color = texture2D(u_Texture, v_TexCoord) * v_Color;
    if (color.a <= u_AlphaCutoff) {
        discard;
    }
    color.rgb *= texture2D(u_Lightmap, v_LightCoord).rgb;

    float fog = clamp((gl_Fog.end - gl_FogFragCoord) * gl_Fog.scale, 0.0, 1.0);
    gl_FragColor = vec4(mix(gl_Fog.color.rgb, color.rgb, fog), color.a);
}
