// INITIAL HLSL BEGIN

#pragma warning( disable: 3556 3571 )
float2 vec2_ctor(float x0, float x1)
{
    return float2(x0, x1);
}
float2x2 mat2_ctor(float x0, float x1, float x2, float x3)
{
    return float2x2(x0, x1, x2, x3);
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

float3 mod_emu(float3 x, float y)
{
    return x - y * floor(x / y);
}


float f_sat(in float _x)
{
return clamp(_x, 0.0, 1.0);
}
float2x2 f_rot(in float _a)
{
float _c2587 = cos(_a);
float _s2588 = sin(_a);
return mat2_ctor(_c2587, (-_s2588), _s2588, _c2587);
}
float3 f_hsv(in float _h, in float _s, in float _v)
{
float3 _k2595 = clamp((abs((mod_emu(((_h * 6.0) + float3(0.0, 4.0, 2.0)), 6.0) - 3.0)) - 1.0), 0.0, 1.0);
return (_v * lerp(float3(1.0, 1.0, 1.0), _k2595, _s));
}
float4 f_src(in float2 _uv)
{
return gl_texture2D(_uTexture, clamp(_uv, 0.0, 1.0));
}
float2 f_asp(in float2 _uv)
{
return ((_uv - 0.5) * vec2_ctor(_uAspect, 1.0));
}
float f_ease(in float _x)
{
(_x = f_sat(_x));
return ((_x * _x) * (3.0 - (2.0 * _x)));
}
float f_easeOut(in float _x)
{
(_x = f_sat(_x));
return (1.0 - (((1.0 - _x) * (1.0 - _x)) * (1.0 - _x)));
}
float f_spd(in float _s)
{
return lerp(0.25, 2.5999999, _s);
}
float4 f_fx(in float2 _uv)
{
float _I2782 = _uP0.x;
float _t2783 = (_uTime * f_spd(_uP0.y));
float3 _base2784 = f_src(_uv).xyz;
float3 _c2785 = _base2784;
float2 _p2786 = f_asp(_uv);
float3 _tint2787 = f_hsv(_uP0.z, 0.75, 1.0);
float _ph2788 = frac((_t2783 * 0.41999999));
float _r2789 = length(_p2786);
float _open2790 = (f_easeOut(f_sat((_ph2788 / 0.25))) * 1.4);
float _close2791 = (f_ease(f_sat(((_ph2788 - 0.64999998) / 0.30000001))) * 1.4);
float _field2792 = (step(_r2789, _open2790) * step(_close2791, _r2789));
float2 _q2793 = mul(transpose(f_rot((_ph2788 * 4.0))), _p2786);
float _sz2794 = (lerp(0.039999999, 0.36000001, f_easeOut(f_sat((_ph2788 / 0.55000001)))) * (1.0 + (_close2791 * 2.0)));
float _st2795 = (pow((abs(_q2793.x) / _sz2794), 0.55000001) + pow((abs(_q2793.y) / _sz2794), 0.55000001));
float3 _col2796 = {0.92000002, 0.25, 0.5};
(_col2796 = lerp(_col2796, float3(0.44999999, 0.2, 0.64999998), step(_st2795, 1.0)));
(_col2796 = lerp(_col2796, float3(0.97000003, 0.93000001, 1.0), step(_st2795, 0.60000002)));
(_c2785 = lerp(_base2784, _col2796, (_field2792 * _I2782)));
return vec4_ctor(_c2785, 1.0);
}
@@ PIXEL OUTPUT @@

PS_OUTPUT main(@@ PIXEL MAIN PARAMETERS @@){
@@ MAIN PROLOGUE @@
float4 _c2798 = f_fx(_vUv);
(gl_Color[0] = vec4_ctor(clamp(_c2798.xyz, 0.0, 1.0), 1.0));
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
float2x2 mat2_ctor(float x0, float x1, float x2, float x3)
{
    return float2x2(x0, x1, x2, x3);
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

float3 mod_emu(float3 x, float y)
{
    return x - y * floor(x / y);
}


float f_sat(in float _x)
{
return clamp(_x, 0.0, 1.0);
}
float2x2 f_rot(in float _a)
{
float _c2587 = cos(_a);
float _s2588 = sin(_a);
return mat2_ctor(_c2587, (-_s2588), _s2588, _c2587);
}
float3 f_hsv(in float _h, in float _s, in float _v)
{
float3 _k2595 = clamp((abs((mod_emu(((_h * 6.0) + float3(0.0, 4.0, 2.0)), 6.0) - 3.0)) - 1.0), 0.0, 1.0);
return (_v * lerp(float3(1.0, 1.0, 1.0), _k2595, _s));
}
float4 f_src(in float2 _uv)
{
return gl_texture2D(_uTexture, clamp(_uv, 0.0, 1.0));
}
float2 f_asp(in float2 _uv)
{
return ((_uv - 0.5) * vec2_ctor(_uAspect, 1.0));
}
float f_ease(in float _x)
{
(_x = f_sat(_x));
return ((_x * _x) * (3.0 - (2.0 * _x)));
}
float f_easeOut(in float _x)
{
(_x = f_sat(_x));
return (1.0 - (((1.0 - _x) * (1.0 - _x)) * (1.0 - _x)));
}
float f_spd(in float _s)
{
return lerp(0.25, 2.5999999, _s);
}
float4 f_fx(in float2 _uv)
{
float _I2782 = _uP0.x;
float _t2783 = (_uTime * f_spd(_uP0.y));
float3 _base2784 = f_src(_uv).xyz;
float3 _c2785 = _base2784;
float2 _p2786 = f_asp(_uv);
float3 _tint2787 = f_hsv(_uP0.z, 0.75, 1.0);
float _ph2788 = frac((_t2783 * 0.41999999));
float _r2789 = length(_p2786);
float _open2790 = (f_easeOut(f_sat((_ph2788 / 0.25))) * 1.4);
float _close2791 = (f_ease(f_sat(((_ph2788 - 0.64999998) / 0.30000001))) * 1.4);
float _field2792 = (step(_r2789, _open2790) * step(_close2791, _r2789));
float2 _q2793 = mul(transpose(f_rot((_ph2788 * 4.0))), _p2786);
float _sz2794 = (lerp(0.039999999, 0.36000001, f_easeOut(f_sat((_ph2788 / 0.55000001)))) * (1.0 + (_close2791 * 2.0)));
float _st2795 = (pow((abs(_q2793.x) / _sz2794), 0.55000001) + pow((abs(_q2793.y) / _sz2794), 0.55000001));
float3 _col2796 = {0.92000002, 0.25, 0.5};
(_col2796 = lerp(_col2796, float3(0.44999999, 0.2, 0.64999998), step(_st2795, 1.0)));
(_col2796 = lerp(_col2796, float3(0.97000003, 0.93000001, 1.0), step(_st2795, 0.60000002)));
(_c2785 = lerp(_base2784, _col2796, (_field2792 * _I2782)));
return vec4_ctor(_c2785, 1.0);
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

float4 _c2798 = f_fx(_vUv);
(gl_Color[0] = vec4_ctor(clamp(_c2798.xyz, 0.0, 1.0), 1.0));
return generateOutput();
}

// COMPILER INPUT HLSL END

// FRAGMENT SHADER END
