// INITIAL HLSL BEGIN

#pragma warning( disable: 3556 3571 )
float2 vec2_ctor(float x0, float x1)
{
    return float2(x0, x1);
}
float4 vec4_ctor(float3 x0, float x1)
{
    return float4(x0, x1);
}
// Uniforms

uniform float _uAspect : register(c0);
uniform float _uTime : register(c1);
uniform float4 _uP0 : register(c2);
static const uint _uTexture = 0;
uniform Texture2D<float4> textures2D[1] : register(t0);
uniform SamplerState samplers2D[1] : register(s0);
#ifdef ANGLE_ENABLE_LOOP_FLATTEN
#define LOOP [loop]
#define FLATTEN [flatten]
#else
#define LOOP
#define FLATTEN
#endif

#define ATOMIC_COUNTER_ARRAY_STRIDE 4

// Varyings
static  float2 _vUv = {0, 0};

static float4 gl_Color[1] =
{
    float4(0, 0, 0, 0)
};

cbuffer DriverConstants : register(b1)
{
    uint dx_Misc : packoffset(c2.w);
    struct SamplerMetadata
    {
        int baseLevel;
        int wrapModes;
        int2 padding;
        int4 intBorderColor;
    };
    SamplerMetadata samplerMetadata[1] : packoffset(c4);
};

#define GL_USES_FRAG_COLOR
float4 gl_texture2D(uint samplerIndex, float2 t)
{
    return textures2D[samplerIndex].Sample(samplers2D[samplerIndex], float2(t.x, t.y));
}

float4 f_src(in float2 _uv)
{
return gl_texture2D(_uTexture, clamp(_uv, 0.0, 1.0));
}
float2 f_asp(in float2 _uv)
{
return ((_uv - 0.5) * vec2_ctor(_uAspect, 1.0));
}
float f_spd(in float _s)
{
return lerp(0.25, 2.5999999, _s);
}
float3 f_pixelate(in float2 _uv, in float _n)
{
float2 _g2777 = vec2_ctor((_n * _uAspect), _n);
return f_src(((floor((_uv * _g2777)) + 0.5) / _g2777)).xyz;
}
float4 f_fx(in float2 _uv)
{
float _I2780 = _uP0.x;
float _t2781 = (_uTime * f_spd(_uP0.y));
float3 _base2782 = f_src(_uv).xyz;
float3 _c2783 = _base2782;
float2 _p2784 = f_asp(_uv);
float _cyc2785 = frac((_t2781 * 0.38));
float _k2786 = floor((_cyc2785 * 8.0));
float _n2787 = exp2((_k2786 + 1.0));
float3 sae7 = {0, 0, 0};
if ((_k2786 >= 7.0))
{
(sae7 = _base2782);
}
else
{
(sae7 = f_pixelate(_uv, _n2787));
}
float3 _pc2788 = sae7;
(_c2783 = lerp(_base2782, _pc2788, _I2780));
return vec4_ctor(_c2783, 1.0);
}
@@ PIXEL OUTPUT @@

PS_OUTPUT main(@@ PIXEL MAIN PARAMETERS @@){
@@ MAIN PROLOGUE @@
float4 _c2790 = f_fx(_vUv);
(gl_Color[0] = vec4_ctor(clamp(_c2790.xyz, 0.0, 1.0), 1.0));
return generateOutput();
}

// INITIAL HLSL END


// COMPILER INPUT HLSL BEGIN

struct PS_INPUT
{
    float4 dx_Position : SV_Position;
    float4 gl_Position : TEXCOORD1;
    float2 v0 : TEXCOORD0;
};

#pragma warning( disable: 3556 3571 )
float2 vec2_ctor(float x0, float x1)
{
    return float2(x0, x1);
}
float4 vec4_ctor(float3 x0, float x1)
{
    return float4(x0, x1);
}
// Uniforms

uniform float _uAspect : register(c0);
uniform float _uTime : register(c1);
uniform float4 _uP0 : register(c2);
static const uint _uTexture = 0;
uniform Texture2D<float4> textures2D[1] : register(t0);
uniform SamplerState samplers2D[1] : register(s0);
#ifdef ANGLE_ENABLE_LOOP_FLATTEN
#define LOOP [loop]
#define FLATTEN [flatten]
#else
#define LOOP
#define FLATTEN
#endif

#define ATOMIC_COUNTER_ARRAY_STRIDE 4

// Varyings
static  float2 _vUv = {0, 0};

static float4 gl_Color[1] =
{
    float4(0, 0, 0, 0)
};

cbuffer DriverConstants : register(b1)
{
    uint dx_Misc : packoffset(c2.w);
    struct SamplerMetadata
    {
        int baseLevel;
        int wrapModes;
        int2 padding;
        int4 intBorderColor;
    };
    SamplerMetadata samplerMetadata[1] : packoffset(c4);
};

#define GL_USES_FRAG_COLOR
float4 gl_texture2D(uint samplerIndex, float2 t)
{
    return textures2D[samplerIndex].Sample(samplers2D[samplerIndex], float2(t.x, t.y));
}

float4 f_src(in float2 _uv)
{
return gl_texture2D(_uTexture, clamp(_uv, 0.0, 1.0));
}
float2 f_asp(in float2 _uv)
{
return ((_uv - 0.5) * vec2_ctor(_uAspect, 1.0));
}
float f_spd(in float _s)
{
return lerp(0.25, 2.5999999, _s);
}
float3 f_pixelate(in float2 _uv, in float _n)
{
float2 _g2777 = vec2_ctor((_n * _uAspect), _n);
return f_src(((floor((_uv * _g2777)) + 0.5) / _g2777)).xyz;
}
float4 f_fx(in float2 _uv)
{
float _I2780 = _uP0.x;
float _t2781 = (_uTime * f_spd(_uP0.y));
float3 _base2782 = f_src(_uv).xyz;
float3 _c2783 = _base2782;
float2 _p2784 = f_asp(_uv);
float _cyc2785 = frac((_t2781 * 0.38));
float _k2786 = floor((_cyc2785 * 8.0));
float _n2787 = exp2((_k2786 + 1.0));
float3 sae7 = {0, 0, 0};
if ((_k2786 >= 7.0))
{
(sae7 = _base2782);
}
else
{
(sae7 = f_pixelate(_uv, _n2787));
}
float3 _pc2788 = sae7;
(_c2783 = lerp(_base2782, _pc2788, _I2780));
return vec4_ctor(_c2783, 1.0);
}
struct PS_OUTPUT
{
    float4 gl_Color0 : SV_TARGET0;
};

PS_OUTPUT generateOutput()
{
    PS_OUTPUT output;
    output.gl_Color0 = gl_Color[0];
    return output;
}


PS_OUTPUT main(PS_INPUT input){
    _vUv = input.v0.xy;

float4 _c2790 = f_fx(_vUv);
(gl_Color[0] = vec4_ctor(clamp(_c2790.xyz, 0.0, 1.0), 1.0));
return generateOutput();
}

// COMPILER INPUT HLSL END

// FRAGMENT SHADER END
