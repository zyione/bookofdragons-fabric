#version 150

// The particle pass.

uniform samplerBuffer Records;
uniform vec3 Offset;        // the batch origin less the camera
uniform mat3 Billboard;     // the camera-facing rotation
uniform float PartialTick;

out vec3 outPosition;
out vec2 outUV0;
flat out uint outColor;
flat out uint outUV2;

const vec2 CORNERS[4] = vec2[4](vec2(-1.0, -1.0), vec2(-1.0, 1.0), vec2(1.0, 1.0), vec2(1.0, -1.0));
const mat3 LEVEL = mat3(1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, -1.0, 0.0);

void main() {
    int record = gl_VertexID / 4;
    int corner = gl_VertexID - record * 4;
    int base = record * 6;
    vec4 from = texelFetch(Records, base);
    vec4 to = texelFetch(Records, base + 1);
    vec4 colour = texelFetch(Records, base + 2);
    vec4 uv = texelFetch(Records, base + 3);
    vec4 turn = texelFetch(Records, base + 4);
    vec4 extra = texelFetch(Records, base + 5);

    int flags = int(extra.x);
    if ((flags & 2) != 0) corner = 3 - corner;

    float extent = PartialTick < extra.y ? 0.0 : mix(from.w, to.w, PartialTick);
    float spin = mix(turn.x, turn.y, PartialTick);
    float c = cos(spin);
    float s = sin(spin);
    vec2 local = mat2(c, s, -s, c) * (CORNERS[corner] * extent);
    mat3 facing = (flags & 1) != 0 ? LEVEL : Billboard;

    outPosition = mix(from.xyz, to.xyz, PartialTick) + Offset + facing * vec3(local, 0.0);
    outUV0 = vec2(corner < 2 ? uv.x : uv.z, (corner == 1 || corner == 2) ? uv.y : uv.w);
    uvec4 bytes = uvec4(clamp(colour, 0.0, 1.0) * 255.0);
    outColor = bytes.r | (bytes.g << 8) | (bytes.b << 16) | (bytes.a << 24);
    outUV2 = uint(turn.z) | (uint(turn.w) << 16);
    gl_Position = vec4(0.0, 0.0, 0.0, 1.0);
}
