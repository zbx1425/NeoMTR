#version 330

uniform isamplerBuffer InstanceBuffer;

vec4 instanceColorVec4() {
    int byteOffset = gl_InstanceID * 72 + 0;

    float c0 = float(texelFetch(InstanceBuffer, byteOffset + 0).r & 0xFF) / 255.0;
    float c1 = float(texelFetch(InstanceBuffer, byteOffset + 1).r & 0xFF) / 255.0;
    float c2 = float(texelFetch(InstanceBuffer, byteOffset + 2).r & 0xFF) / 255.0;
    float c3 = float(texelFetch(InstanceBuffer, byteOffset + 3).r & 0xFF) / 255.0;

    return vec4(c0, c1, c2, c3);
}

ivec2 instanceLightIVec2() {
    int byteOffset = gl_InstanceID * 72 + 4;

    int b0 = texelFetch(InstanceBuffer, byteOffset + 0).r;
    int b1 = texelFetch(InstanceBuffer, byteOffset + 1).r;
    int b2 = texelFetch(InstanceBuffer, byteOffset + 2).r;
    int b3 = texelFetch(InstanceBuffer, byteOffset + 3).r;

    int s0 = ((b1 & 0xFF) << 8) | ((b0 & 0xFF) << 0);
    int s1 = ((b3 & 0xFF) << 8) | ((b2 & 0xFF) << 0);

    return ivec2(s0, s1);
}

float instanceFloat(int byteOffset) {
    int b0 = texelFetch(InstanceBuffer, byteOffset + 0).r;
    int b1 = texelFetch(InstanceBuffer, byteOffset + 1).r;
    int b2 = texelFetch(InstanceBuffer, byteOffset + 2).r;
    int b3 = texelFetch(InstanceBuffer, byteOffset + 3).r;

    return intBitsToFloat(  ((b3 & 0xFF) << 24)
                          | ((b2 & 0xFF) << 16)
                          | ((b1 & 0xFF) << 8)
                          | ((b0 & 0xFF) << 0)
    );
}

vec4 instanceVec4(int byteOffset) {
    float f0 = instanceFloat(byteOffset + 0 * 4);
    float f1 = instanceFloat(byteOffset + 1 * 4);
    float f2 = instanceFloat(byteOffset + 2 * 4);
    float f3 = instanceFloat(byteOffset + 3 * 4);

    return vec4(f0, f1, f2, f3);
}

mat4 instanceModelViewMat() {
    mat4 modelView = mat4(0.0);

    int byteOffset = gl_InstanceID * 72 + 4 + 4;

    modelView[0] = instanceVec4(byteOffset + 0 * 16);
    modelView[1] = instanceVec4(byteOffset + 1 * 16);
    modelView[2] = instanceVec4(byteOffset + 2 * 16);
    modelView[3] = instanceVec4(byteOffset + 3 * 16);

    return modelView;
}