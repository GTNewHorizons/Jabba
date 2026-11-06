#version 120

// Vanilla inventory item lighting.
const float MODEL_AMBIENT = 0.4;
const float LIGHT_DIFFUSE = 0.6;

uniform vec3 u_Light0;
uniform vec3 u_Light1;
uniform mat3 u_FaceInverse;
uniform mat4 u_BlockFrame;
uniform vec3 u_TangentU;
uniform vec3 u_TangentV;
uniform vec4 u_CornerAo;
uniform vec4 u_CornerBlockLight;
uniform vec4 u_CornerSkyLight;

varying vec2 v_TexCoord;
varying vec2 v_LightCoord;
varying vec4 v_Color;

float bilinear(vec4 corners, vec2 position) {
    return mix(mix(corners.x, corners.y, position.x), mix(corners.z, corners.w, position.x), position.y);
}

void main() {
    gl_Position = ftransform();
    vec4 eyePosition = gl_ModelViewMatrix * gl_Vertex;

    vec3 itemNormal = normalize(u_FaceInverse * (mat3(gl_ModelViewMatrix) * gl_Normal));
    float light = MODEL_AMBIENT
            + LIGHT_DIFFUSE * (max(dot(itemNormal, u_Light0), 0.0) + max(dot(itemNormal, u_Light1), 0.0));

    vec3 offset = eyePosition.xyz - u_BlockFrame[3].xyz;
    vec3 axisU = (u_BlockFrame * vec4(u_TangentU, 0.0)).xyz;
    vec3 axisV = (u_BlockFrame * vec4(u_TangentV, 0.0)).xyz;
    vec2 facePosition = clamp(vec2(dot(offset, axisU) / dot(axisU, axisU), dot(offset, axisV) / dot(axisV, axisV)),
            0.0, 1.0);

    float ao = bilinear(u_CornerAo, facePosition);
    vec2 brightness = vec2(bilinear(u_CornerBlockLight, facePosition), bilinear(u_CornerSkyLight, facePosition));

    v_Color = vec4(gl_Color.rgb * clamp(light, 0.0, 1.0) * ao, gl_Color.a);
    v_TexCoord = gl_MultiTexCoord0.st;
    v_LightCoord = (gl_TextureMatrix[1] * vec4(brightness, 0.0, 1.0)).st;
    gl_FogFragCoord = length(eyePosition.xyz);
}
