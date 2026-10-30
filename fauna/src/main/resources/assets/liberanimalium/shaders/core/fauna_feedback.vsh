#version 150

// The skinning pass for a shader pack.

in vec3 Position;     // bone-local, the cube's own rotation folded in
in vec2 UV0;
in vec3 Normal;       // unused here: a pack replaces the vertex normal with the face normal
in uvec2 Bone;        // x = palette row
in vec3 FaceNormal;   // the winding's normal, bone-local (IrisQuad)
in vec4 Tangent;      // the tangent, bone-local, w = handedness
in vec2 MidTex;       // the quad's mean UV

uniform sampler2D Sampler3;   // the bone palette

uniform mat4 RootPose;        // the pose stack at the model root: bone space -> the batch's space
uniform mat3 RootNormal;      // its normal matrix
uniform vec4 VertexColor;
uniform ivec2 LightUV;
uniform ivec2 OverlayUV;
uniform ivec3 EntityInfo;     // iris_Entity: the entity, the block entity, the item, as Iris numbers them

out vec3 outPosition;
out vec4 outColor;
out vec2 outUV0;
flat out ivec2 outUV1;
flat out ivec2 outUV2;
out vec3 outNormal;
flat out ivec3 outEntity;
out vec2 outMidTex;
out vec4 outTangent;

void main() {
    int b = int(Bone.x);
    mat4 pose = mat4(
        texelFetch(Sampler3, ivec2(0, b), 0),
        texelFetch(Sampler3, ivec2(1, b), 0),
        texelFetch(Sampler3, ivec2(2, b), 0),
        texelFetch(Sampler3, ivec2(3, b), 0));
    mat3 boneNormal = mat3(
        texelFetch(Sampler3, ivec2(4, b), 0).xyz,
        texelFetch(Sampler3, ivec2(5, b), 0).xyz,
        texelFetch(Sampler3, ivec2(6, b), 0).xyz);

    outPosition = (RootPose * (pose * vec4(Position, 1.0))).xyz;

    // Iris computes the face normal from the four skinned positions.
    vec3 n = normalize(RootNormal * (boneNormal * FaceNormal));
    outNormal = trunc(n * 127.0) / 127.0;
    vec3 t = normalize(RootNormal * (boneNormal * Tangent.xyz));
    outTangent = vec4(trunc(t * 127.0), trunc(Tangent.w * 127.0));

    outColor = trunc(VertexColor * 255.0) / 255.0;
    outUV0 = UV0;
    outUV1 = OverlayUV;
    outUV2 = LightUV;
    outEntity = EntityInfo;
    outMidTex = MidTex;
    gl_Position = vec4(0.0, 0.0, 0.0, 1.0);
}
