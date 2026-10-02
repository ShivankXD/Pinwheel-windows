// INITIAL HLSL BEGIN

#pragma warning( disable: 3556 3571 )
float2 vec2_ctor(float x0, float x1)
{
    return float2(x0, x1);
}
float3 vec3_ctor(float x0)
{
    return float3(x0, x0, x0);
}
float4 vec4_ctor(float3 x0, float x1)
{
    return float4(x0, x1);
}
// Uniforms

uniform float2 _uSize : register(c0);
uniform float _uAspect : register(c1);
uniform float _uTime : register(c2);
uniform float4 _uP0 : register(c3);
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

float mod_emu(float x, float y)
{
    return x - y * floor(x / y);
}


float f_luma(in float3 _c)
{
return dot(_c, float3(0.21259999, 0.71520001, 0.0722));
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
float3 f_sharpen(in float2 _uv, in float _amt)
{
float2 _e2806 = vec2_ctor((1.5 / _uSize.x), (1.5 / _uSize.y));
float3 _c2807 = f_src(_uv).xyz;
float3 _n2808 = ((((f_src((_uv + vec2_ctor(_e2806.x, 0.0))).xyz + f_src((_uv - vec2_ctor(_e2806.x, 0.0))).xyz) + f_src((_uv + vec2_ctor(0.0, _e2806.y))).xyz) + f_src((_uv - vec2_ctor(0.0, _e2806.y))).xyz) * 0.25);
return (_c2807 + ((_c2807 - _n2808) * _amt));
}
float4 f_fx(in float2 _uv)
{
float _t2883 = _uTime;
float3 _base2884 = f_src(_uv).xyz;
float3 _c2885 = _base2884;
float2 _p2886 = f_asp(_uv);
float _s2887 = f_spd(_uP0.x);
float _I2888 = _uP0.y;
float _k2889 = floor(((_t2883 * _s2887) * 3.0));
float _b2890 = frac(((_t2883 * _s2887) * 3.0));
float _pale2891 = mod_emu(_k2889, 2.0);
float3 _sh2892 = f_sharpen(_uv, (1.0 + (3.0 * _uP0.z)));
float3 _crisp2893 = (((_sh2892 - 0.5) * 1.4) + 0.44999999);
(_crisp2893 = lerp(vec3_ctor(f_luma(_crisp2893)), _crisp2893, 1.35));
float3 _wash2894 = lerp(_base2884, float3(1.0, 1.0, 1.0), 0.55000001);
(_wash2894 = ((lerp(vec3_ctor(f_luma(_wash2894)), _wash2894, 0.44999999) * float3(0.95999998, 1.0, 1.04)) + 0.039999999));
float sb52 = {0};
if ((mod_emu(_k2889, 3.0) == 2.0))
{
float sb53 = {0};
if ((frac((_k2889 * 0.37)) > 0.5))
{
(sb53 = _uv.x);
}
else
{
(sb53 = (1.0 - _uv.x));
}
(sb52 = step(0.5, sb53));
}
else
{
(sb52 = 1.0);
}
float _halfM2895 = sb52;
(_c2885 = lerp(_crisp2893, _wash2894, (_pale2891 * _halfM2895)));
(_c2885 = lerp(_c2885, float3(1.0, 1.0, 1.0), ((exp(((-_b2890) * 18.0)) * 0.30000001) * _pale2891)));
(_c2885 = lerp(_base2884, _c2885, _I2888));
return vec4_ctor(_c2885, 1.0);
}
@@ PIXEL OUTPUT @@

PS_OUTPUT main(@@ PIXEL MAIN PARAMETERS @@){
@@ MAIN PROLOGUE @@
float4 _c2897 = f_fx(_vUv);
(gl_Color[0] = vec4_ctor(clamp(_c2897.xyz, 0.0, 1.0), 1.0));
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
float3 vec3_ctor(float x0)
{
    return float3(x0, x0, x0);
}
float4 vec4_ctor(float3 x0, float x1)
{
    return float4(x0, x1);
}
// Uniforms

uniform float2 _uSize : register(c0);
uniform float _uAspect : register(c1);
uniform float _uTime : register(c2);
uniform float4 _uP0 : register(c3);
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

float mod_emu(float x, float y)
{
    return x - y * floor(x / y);
}


float f_luma(in float3 _c)
{
return dot(_c, float3(0.21259999, 0.71520001, 0.0722));
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
float3 f_sharpen(in float2 _uv, in float _amt)
{
float2 _e2806 = vec2_ctor((1.5 / _uSize.x), (1.5 / _uSize.y));
float3 _c2807 = f_src(_uv).xyz;
float3 _n2808 = ((((f_src((_uv + vec2_ctor(_e2806.x, 0.0))).xyz + f_src((_uv - vec2_ctor(_e2806.x, 0.0))).xyz) + f_src((_uv + vec2_ctor(0.0, _e2806.y))).xyz) + f_src((_uv - vec2_ctor(0.0, _e2806.y))).xyz) * 0.25);
return (_c2807 + ((_c2807 - _n2808) * _amt));
}
float4 f_fx(in float2 _uv)
{
float _t2883 = _uTime;
float3 _base2884 = f_src(_uv).xyz;
float3 _c2885 = _base2884;
float2 _p2886 = f_asp(_uv);
float _s2887 = f_spd(_uP0.x);
float _I2888 = _uP0.y;
float _k2889 = floor(((_t2883 * _s2887) * 3.0));
float _b2890 = frac(((_t2883 * _s2887) * 3.0));
float _pale2891 = mod_emu(_k2889, 2.0);
float3 _sh2892 = f_sharpen(_uv, (1.0 + (3.0 * _uP0.z)));
float3 _crisp2893 = (((_sh2892 - 0.5) * 1.4) + 0.44999999);
(_crisp2893 = lerp(vec3_ctor(f_luma(_crisp2893)), _crisp2893, 1.35));
float3 _wash2894 = lerp(_base2884, float3(1.0, 1.0, 1.0), 0.55000001);
(_wash2894 = ((lerp(vec3_ctor(f_luma(_wash2894)), _wash2894, 0.44999999) * float3(0.95999998, 1.0, 1.04)) + 0.039999999));
float sb52 = {0};
if ((mod_emu(_k2889, 3.0) == 2.0))
{
float sb53 = {0};
if ((frac((_k2889 * 0.37)) > 0.5))
{
(sb53 = _uv.x);
}
else
{
(sb53 = (1.0 - _uv.x));
}
(sb52 = step(0.5, sb53));
}
else
{
(sb52 = 1.0);
}
float _halfM2895 = sb52;
(_c2885 = lerp(_crisp2893, _wash2894, (_pale2891 * _halfM2895)));
(_c2885 = lerp(_c2885, float3(1.0, 1.0, 1.0), ((exp(((-_b2890) * 18.0)) * 0.30000001) * _pale2891)));
(_c2885 = lerp(_base2884, _c2885, _I2888));
return vec4_ctor(_c2885, 1.0);
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

float4 _c2897 = f_fx(_vUv);
(gl_Color[0] = vec4_ctor(clamp(_c2897.xyz, 0.0, 1.0), 1.0));
return generateOutput();
}

// COMPILER INPUT HLSL END

// FRAGMENT SHADER END
