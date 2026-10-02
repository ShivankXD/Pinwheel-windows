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

float3 mod_emu(float3 x, float y)
{
    return x - y * floor(x / y);
}


float f_sat(in float _x)
{
return clamp(_x, 0.0, 1.0);
}
float f_luma(in float3 _c)
{
return dot(_c, float3(0.21259999, 0.71520001, 0.0722));
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
float f_spd(in float _s)
{
return lerp(0.25, 2.5999999, _s);
}
float f_sobel(in float2 _uv)
{
float2 _e2634 = (vec2_ctor((1.0 / _uSize.x), (1.0 / _uSize.y)) * 1.5);
float _tl2635 = f_luma(f_src((_uv + vec2_ctor((-_e2634.x), _e2634.y))).xyz);
float _t2636 = f_luma(f_src((_uv + vec2_ctor(0.0, _e2634.y))).xyz);
float _tr2637 = f_luma(f_src((_uv + _e2634)).xyz);
float _l2638 = f_luma(f_src((_uv - vec2_ctor(_e2634.x, 0.0))).xyz);
float _r2639 = f_luma(f_src((_uv + vec2_ctor(_e2634.x, 0.0))).xyz);
float _bl2640 = f_luma(f_src((_uv - _e2634)).xyz);
float _b2641 = f_luma(f_src((_uv - vec2_ctor(0.0, _e2634.y))).xyz);
float _br2642 = f_luma(f_src((_uv + vec2_ctor(_e2634.x, (-_e2634.y)))).xyz);
float _gx2643 = (((((_tr2637 + (2.0 * _r2639)) + _br2642) - _tl2635) - (2.0 * _l2638)) - _bl2640);
float _gy2644 = (((((_tl2635 + (2.0 * _t2636)) + _tr2637) - _bl2640) - (2.0 * _b2641)) - _br2642);
return sqrt(((_gx2643 * _gx2643) + (_gy2644 * _gy2644)));
}
float4 f_fx(in float2 _uv)
{
float _I2760 = _uP0.x;
float _t2761 = (_uTime * f_spd(_uP0.y));
float3 _base2762 = f_src(_uv).xyz;
float3 _c2763 = _base2762;
float2 _p2764 = f_asp(_uv);
float3 _col2765 = f_hsv(frac((_uP0.z + (_t2761 * 0.050000001))), 0.85000002, 1.0);
float _e2766 = f_sat((f_sobel(_uv) * 3.0));
float3 _ec2767 = lerp(float3(0.1, 0.89999998, 1.0), float3(1.0, 0.2, 0.80000001), (_uv.x + (0.2 * sin(_t2761))));
(_c2763 = lerp(_base2762, (((_base2762 * 0.25) + ((_ec2767 * _e2766) * 1.4)) + ((_ec2767 * f_sat((f_sobel((_uv + 0.0040000002)) * 3.0))) * 0.30000001)), _I2760));
return vec4_ctor(_c2763, 1.0);
}
@@ PIXEL OUTPUT @@

PS_OUTPUT main(@@ PIXEL MAIN PARAMETERS @@){
@@ MAIN PROLOGUE @@
float4 _c2769 = f_fx(_vUv);
(gl_Color[0] = vec4_ctor(clamp(_c2769.xyz, 0.0, 1.0), 1.0));
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

float3 mod_emu(float3 x, float y)
{
    return x - y * floor(x / y);
}


float f_sat(in float _x)
{
return clamp(_x, 0.0, 1.0);
}
float f_luma(in float3 _c)
{
return dot(_c, float3(0.21259999, 0.71520001, 0.0722));
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
float f_spd(in float _s)
{
return lerp(0.25, 2.5999999, _s);
}
float f_sobel(in float2 _uv)
{
float2 _e2634 = (vec2_ctor((1.0 / _uSize.x), (1.0 / _uSize.y)) * 1.5);
float _tl2635 = f_luma(f_src((_uv + vec2_ctor((-_e2634.x), _e2634.y))).xyz);
float _t2636 = f_luma(f_src((_uv + vec2_ctor(0.0, _e2634.y))).xyz);
float _tr2637 = f_luma(f_src((_uv + _e2634)).xyz);
float _l2638 = f_luma(f_src((_uv - vec2_ctor(_e2634.x, 0.0))).xyz);
float _r2639 = f_luma(f_src((_uv + vec2_ctor(_e2634.x, 0.0))).xyz);
float _bl2640 = f_luma(f_src((_uv - _e2634)).xyz);
float _b2641 = f_luma(f_src((_uv - vec2_ctor(0.0, _e2634.y))).xyz);
float _br2642 = f_luma(f_src((_uv + vec2_ctor(_e2634.x, (-_e2634.y)))).xyz);
float _gx2643 = (((((_tr2637 + (2.0 * _r2639)) + _br2642) - _tl2635) - (2.0 * _l2638)) - _bl2640);
float _gy2644 = (((((_tl2635 + (2.0 * _t2636)) + _tr2637) - _bl2640) - (2.0 * _b2641)) - _br2642);
return sqrt(((_gx2643 * _gx2643) + (_gy2644 * _gy2644)));
}
float4 f_fx(in float2 _uv)
{
float _I2760 = _uP0.x;
float _t2761 = (_uTime * f_spd(_uP0.y));
float3 _base2762 = f_src(_uv).xyz;
float3 _c2763 = _base2762;
float2 _p2764 = f_asp(_uv);
float3 _col2765 = f_hsv(frac((_uP0.z + (_t2761 * 0.050000001))), 0.85000002, 1.0);
float _e2766 = f_sat((f_sobel(_uv) * 3.0));
float3 _ec2767 = lerp(float3(0.1, 0.89999998, 1.0), float3(1.0, 0.2, 0.80000001), (_uv.x + (0.2 * sin(_t2761))));
(_c2763 = lerp(_base2762, (((_base2762 * 0.25) + ((_ec2767 * _e2766) * 1.4)) + ((_ec2767 * f_sat((f_sobel((_uv + 0.0040000002)) * 3.0))) * 0.30000001)), _I2760));
return vec4_ctor(_c2763, 1.0);
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

float4 _c2769 = f_fx(_vUv);
(gl_Color[0] = vec4_ctor(clamp(_c2769.xyz, 0.0, 1.0), 1.0));
return generateOutput();
}

// COMPILER INPUT HLSL END

// FRAGMENT SHADER END
