#version 150


float fog_distance(mat4 modelViewMat, vec3 pos, int shape) {
    if (shape == 0) {
        return length((modelViewMat * vec4(pos, 1.0)).xyz);
    } else {
        float distXZ = length((modelViewMat * vec4(pos.x, 0.0, pos.z, 1.0)).xyz);
        float distY = length((modelViewMat * vec4(0.0, pos.y, 0.0, 1.0)).xyz);
        return max(distXZ, distY);
    }
}

in vec3 Position;
in vec2 UV0;
in uvec2 Bone;

uniform sampler2D Sampler3;   // the bone palette: 8 texels a bone, one bone a row

uniform mat4 ModelViewMat;    // the render system's model-view times the pose stack at the model root
uniform mat4 ProjMat;
uniform mat4 TextureMat;
uniform int FogShape;
uniform int BoneBase;         // the row this body's bones start at (0 alone; its block in an instanced batch)
uniform vec4 VertexColor;     // the render context's colour

out float vertexDistance;
out vec4 vertexColor;
out vec2 texCoord0;

void main() {
    int b = BoneBase + int(Bone.x);
    mat4 pose = mat4(
        texelFetch(Sampler3, ivec2(0, b), 0),
        texelFetch(Sampler3, ivec2(1, b), 0),
        texelFetch(Sampler3, ivec2(2, b), 0),
        texelFetch(Sampler3, ivec2(3, b), 0));
    vec3 modelPos = (pose * vec4(Position, 1.0)).xyz;
    gl_Position = ProjMat * ModelViewMat * vec4(modelPos, 1.0);
    // The swirl program measures fog on the position itself, not through IViewRotMat: mirrored.
    vertexDistance = fog_distance(ModelViewMat, modelPos, FogShape);
    vertexColor = VertexColor;
    texCoord0 = (TextureMat * vec4(UV0, 0.0, 1.0)).xy;
}
