package com.merryblue.baseapplication.helpers.ripple

object Shaders {

    const val VS: String = """#version 300 es
layout(location=0) in vec2 aPos;
layout(location=1) in vec2 aUv;
out vec2 vUv;

void main() {
    vUv = aUv;
    gl_Position = vec4(aPos, 0.0, 1.0);
}
"""

    const val SIM_FS: String = """#version 300 es
precision highp float;

in vec2 vUv;
out vec2 outRG;

uniform sampler2D uHeightRG;
uniform vec2 uTexel;
uniform float uDamping;

uniform int uDropCount;
uniform vec4 uDrops[8]; // x,y,strength,radius (0..1)

float injectDrops(vec2 uv) {
    float s = 0.0;
    for (int i = 0; i < 8; i++) {
        if (i >= uDropCount) break;
        vec2 p = uDrops[i].xy;
        float strength = uDrops[i].z;
        float radius = uDrops[i].w;
        float d = distance(uv, p);
        float g = exp(-(d * d) / (radius * radius)); // gaussian-ish
        s += strength * g;
    }
    return s;
}

void main() {
    vec2 c = texture(uHeightRG, vUv).rg;
    float h  = c.r; // current
    float hp = c.g; // previous

    float hL = texture(uHeightRG, vUv - vec2(uTexel.x, 0.0)).r;
    float hR = texture(uHeightRG, vUv + vec2(uTexel.x, 0.0)).r;
    float hU = texture(uHeightRG, vUv + vec2(0.0, uTexel.y)).r;
    float hD = texture(uHeightRG, vUv - vec2(0.0, uTexel.y)).r;

    float lap = (hL + hR + hU + hD) * 0.5 - h;
    float newH = (h + lap) - hp;   // discrete wave equation
    newH *= uDamping;

    newH += injectDrops(vUv);

    // output: new current height, and store old current as previous
    outRG = vec2(newH, h);
}
"""

    const val RENDER_FS: String = """#version 300 es
precision highp float;

in vec2 vUv;
out vec4 outColor;

uniform sampler2D uBase;
uniform sampler2D uHeightRG;

uniform vec2 uTexel;       // 1/simSize
uniform float uRefract;    // 0.01..0.04
uniform float uSpecular;   // 0..0.15
uniform vec2 uCropScale;   // centerCrop scale (>=1)

vec2 centerCropUV(vec2 uv) {
    // uv in [0..1] -> apply crop scale around center
    return (uv - 0.5) / uCropScale + 0.5;
}

void main() {
    // Flip Y only for sampling base image (fix upside-down)
    vec2 uv = vec2(vUv.x, 1.0 - vUv.y);

    // Height gradient (use vUv in sim space, keep consistent)
    float hL = texture(uHeightRG, vUv - vec2(uTexel.x, 0.0)).r;
    float hR = texture(uHeightRG, vUv + vec2(uTexel.x, 0.0)).r;
    float hD = texture(uHeightRG, vUv - vec2(0.0, uTexel.y)).r;
    float hU = texture(uHeightRG, vUv + vec2(0.0, uTexel.y)).r;

    vec2 grad = vec2(hR - hL, hU - hD);

    // Refraction in screen UV; compensate crop scale so effect strength stays consistent
    vec2 offset = (grad * uRefract) / uCropScale;

    vec2 baseUv = centerCropUV(uv + offset);
    baseUv = clamp(baseUv, vec2(0.0), vec2(1.0));

    vec3 col = texture(uBase, baseUv).rgb;

    // subtle highlight
    float sp = clamp(length(grad) * 8.0, 0.0, 1.0);
    col += sp * uSpecular;

    outColor = vec4(col, 1.0);
}
"""
}
