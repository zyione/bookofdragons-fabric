#version 150

// The skinned body, instanced: one draw call per species per frame.

#define MINECRAFT_LIGHT_POWER   (0.6)
#define MINECRAFT_AMBIENT_LIGHT (0.4)

vec4 minecraft_mix_light(vec3 lightDir0, vec3 lightDir1, vec3 normal, vec4 color) {
    lightDir0 = normalize(lightDir0);
    lightDir1 = normalize(lightDir1);
    float light0 = max(0.0, dot(lightDir0, normal));
    float light1 = max(0.0, dot(lightDir1, normal));
    float lightAccum = min(1.0, (light0 + light1) * MINECRAFT_LIGHT_POWER + MINECRAFT_AMBIENT_LIGHT);
    return vec4(color.rgb * lightAccum, color.a);
}

float fog_distance(mat4 modelViewMat, vec3 pos, int shape) {
    if (shape == 0) {
        return length((modelViewMat * vec4(pos, 1.0)).xyz);
    } else {
        float distXZ = length((modelViewMat * vec4(pos.x, 0.0, pos.z, 1.0)).xyz);
        float distY = length((modelViewMat * vec4(0.0, pos.y, 0.0, 1.0)).xyz);
        return max(distXZ, distY);
    }
}

in vec3 Position;   // bone-local, the cube's own rotation already folded in
in vec2 UV0;
in vec3 Normal;     // bone-local, the cube's own rotation already folded in
in uvec2 Bone;      // x = index into the instance's palette, y = the flat-cube flip flags

uniform sampler2D Sampler1;   // overlay
uniform sampler2D Sampler2;   // lightmap
uniform sampler2D Sampler3;   // the palette: two header rows an instance, then 8 texels a bone, one bone a row

uniform mat4 ProjMat;
uniform mat3 IViewRotMat;
uniform int FogShape;
uniform int HeaderBase;       // the row the header block starts at, after every bone row

uniform vec3 Light0_Direction;
uniform vec3 Light1_Direction;

out float vertexDistance;
out vec4 vertexColor;
out vec4 lightMapColor;
out vec4 overlayColor;
out vec2 texCoord0;
out vec4 normal;
flat out float layer;

void main() {
    int h = HeaderBase + gl_InstanceID * 2;
    // Header row 0: the model-view (four columns), the root normal matrix (three).
    mat4 modelView = mat4(
        texelFetch(Sampler3, ivec2(0, h), 0),
        texelFetch(Sampler3, ivec2(1, h), 0),
        texelFetch(Sampler3, ivec2(2, h), 0),
        texelFetch(Sampler3, ivec2(3, h), 0));
    mat3 rootNormal = mat3(
        texelFetch(Sampler3, ivec2(4, h), 0).xyz,
        texelFetch(Sampler3, ivec2(5, h), 0).xyz,
        texelFetch(Sampler3, ivec2(6, h), 0).xyz);
    vec4 coords = texelFetch(Sampler3, ivec2(7, h), 0);
    vec4 color = texelFetch(Sampler3, ivec2(0, h + 1), 0);
    vec4 where = texelFetch(Sampler3, ivec2(1, h + 1), 0);

    int b = int(where.y) + int(Bone.x);
    mat4 pose = mat4(
        texelFetch(Sampler3, ivec2(0, b), 0),
        texelFetch(Sampler3, ivec2(1, b), 0),
        texelFetch(Sampler3, ivec2(2, b), 0),
        texelFetch(Sampler3, ivec2(3, b), 0));
    mat3 boneNormal = mat3(
        texelFetch(Sampler3, ivec2(4, b), 0).xyz,
        texelFetch(Sampler3, ivec2(5, b), 0).xyz,
        texelFetch(Sampler3, ivec2(6, b), 0).xyz);

    vec3 modelPos = (pose * vec4(Position, 1.0)).xyz;
    gl_Position = ProjMat * modelView * vec4(modelPos, 1.0);

    vec3 n = rootNormal * (boneNormal * Normal);
    uint flags = Bone.y;
    if (n.x < 0.0 && (flags & 1u) != 0u) n.x = -n.x;
    if (n.y < 0.0 && (flags & 2u) != 0u) n.y = -n.y;
    if (n.z < 0.0 && (flags & 4u) != 0u) n.z = -n.z;

    vertexDistance = fog_distance(modelView, IViewRotMat * modelPos, FogShape);
    vertexColor = minecraft_mix_light(Light0_Direction, Light1_Direction, n, color);
    lightMapColor = texelFetch(Sampler2, ivec2(coords.xy) / 16, 0);
    overlayColor = texelFetch(Sampler1, ivec2(coords.zw), 0);
    texCoord0 = UV0;
    normal = ProjMat * modelView * vec4(n, 0.0);
    layer = where.x;
}
