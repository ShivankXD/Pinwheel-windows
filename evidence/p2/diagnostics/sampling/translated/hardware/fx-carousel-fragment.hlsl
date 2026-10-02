// INITIAL HLSL BEGIN

#pragma warning( disable: 3556 3571 )
float float_ctor_int(int x0)
{
    return float(x0);
}
float2 vec2_ctor(float x0, float x1)
{
    return float2(x0, x1);
}
float3 vec3_ctor(float x0, float x1, float x2)
{
    return float3(x0, x1, x2);
}
float3 vec3_ctor(float2 x0, float x1)
{
    return float3(x0, x1);
}
float3x3 mat3_ctor(float x0, float x1, float x2, float x3, float x4, float x5, float x6, float x7, float x8)
{
    return float3x3(x0, x1, x2, x3, x4, x5, x6, x7, x8);
}
float4 vec4_ctor(float2 x0, float x1, float x2)
{
    return float4(x0, x1, x2);
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

float f_sat(in float _x)
{
return clamp(_x, 0.0, 1.0);
}
float4 f_src(in float2 _uv)
{
return gl_texture2D(_uTexture, clamp(_uv, 0.0, 1.0));
}
float f_inside(in float2 _uv)
{
return (((step(0.0, _uv.x) * step(_uv.x, 1.0)) * step(0.0, _uv.y)) * step(_uv.y, 1.0));
}
float2 f_asp(in float2 _uv)
{
return ((_uv - 0.5) * vec2_ctor(_uAspect, 1.0));
}
float f_spd(in float _s)
{
return lerp(0.25, 2.5999999, _s);
}
float3 f_blur9(in float2 _uv, in float _r)
{
float3 _c2620 = {0.0, 0.0, 0.0};
float2 _d2621 = vec2_ctor((_r / _uAspect), _r);
{LOOP for(int _x2622 = {-1}; (_x2622 <= 1); (_x2622++))
{
{LOOP for(int _y2623 = {-1}; (_y2623 <= 1); (_y2623++))
{
(_c2620 += (f_src((_uv + (vec2_ctor(float_ctor_int(_x2622), float_ctor_int(_y2623)) * _d2621))).xyz + float3(0.0, 0.0, 0.0)));
}
}
}
}
return (_c2620 / 9.0);
}
float3x3 f_rotY(in float _a)
{
float _c2726 = cos(_a);
float _s2727 = sin(_a);
return mat3_ctor(_c2726, 0.0, (-_s2727), 0.0, 1.0, 0.0, _s2727, 0.0, _c2726);
}
float4 f_card2(in float2 _uv, in float3x3 _m, in float3 _c, in float _s)
{
float3 _rd2737 = vec3_ctor((f_asp(_uv) * 2.0), 2.2);
float3 _n2738 = mul(transpose(_m), float3(0.0, 0.0, -1.0));
float _den2739 = dot(_rd2737, _n2738);
if ((abs(_den2739) < 9.9999997e-05))
{
return float4(0.0, 0.0, 0.0, 0.0);
}
float _t2740 = (dot(_c, _n2738) / _den2739);
if ((_t2740 <= 0.0))
{
return float4(0.0, 0.0, 0.0, 0.0);
}
float3 _local2741 = ((_rd2737 * _t2740) - _c);
float2 _q2742 = ((vec2_ctor((dot(_local2741, mul(transpose(_m), float3(1.0, 0.0, 0.0))) / (_uAspect * _s)), (dot(_local2741, mul(transpose(_m), float3(0.0, 1.0, 0.0))) / _s)) * 0.5) + 0.5);
return vec4_ctor(_q2742, f_inside(_q2742), _t2740);
}
float3 f_blurredBg(in float2 _uv, in float _dim)
{
return (f_blur9((((_uv - 0.5) * 0.80000001) + 0.5), 0.029999999) * _dim);
}
float3 f_bg(in float2 _uv)
{
return lerp(float3(0.02, 0.02, 0.039999999), float3(0.079999998, 0.07, 0.12), _uv.y);
}
float4 f_fx(in float2 _uv)
{
float _T2754 = (_uTime * f_spd(_uP0.y));
float3 _col2755 = {0.0, 0.0, 0.0};
(_col2755 = lerp(f_bg(_uv), f_blurredBg(_uv, 0.30000001), step(0.5, _uP0.w)));
float _bestT2756 = {1000000000.0};
float _R2757 = (1.25 * max(_uAspect, 0.80000001));
float _s2758 = lerp(0.62, 0.44999999, _uP0.z);
{LOOP for(int _i2759 = {0}; (_i2759 < 6); (_i2759++))
{
float _th2760 = (((float_ctor_int(_i2759) / 6.0) * 6.2831855) + (_T2754 * 0.5));
float3 _c2761 = vec3_ctor((sin(_th2760) * _R2757), 0.0, (3.0 + (cos(_th2760) * _R2757)));
float4 _q2762 = f_card2(_uv, f_rotY(_th2760), _c2761, _s2758);
if (((_q2762.z > 0.5) && (_q2762.w < _bestT2756)))
{
(_bestT2756 = _q2762.w);
(_col2755 = (f_src(_q2762.xy).xyz * (0.44999999 + (0.55000001 * f_sat((-cos(_th2760)))))));
}
}
}
return vec4_ctor(_col2755, 1.0);
}
@@ PIXEL OUTPUT @@

PS_OUTPUT main(@@ PIXEL MAIN PARAMETERS @@){
@@ MAIN PROLOGUE @@
float4 _c2764 = f_fx(_vUv);
(gl_Color[0] = vec4_ctor(clamp(_c2764.xyz, 0.0, 1.0), 1.0));
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
float float_ctor_int(int x0)
{
    return float(x0);
}
float2 vec2_ctor(float x0, float x1)
{
    return float2(x0, x1);
}
float3 vec3_ctor(float x0, float x1, float x2)
{
    return float3(x0, x1, x2);
}
float3 vec3_ctor(float2 x0, float x1)
{
    return float3(x0, x1);
}
float3x3 mat3_ctor(float x0, float x1, float x2, float x3, float x4, float x5, float x6, float x7, float x8)
{
    return float3x3(x0, x1, x2, x3, x4, x5, x6, x7, x8);
}
float4 vec4_ctor(float2 x0, float x1, float x2)
{
    return float4(x0, x1, x2);
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

float f_sat(in float _x)
{
return clamp(_x, 0.0, 1.0);
}
float4 f_src(in float2 _uv)
{
return gl_texture2D(_uTexture, clamp(_uv, 0.0, 1.0));
}
float f_inside(in float2 _uv)
{
return (((step(0.0, _uv.x) * step(_uv.x, 1.0)) * step(0.0, _uv.y)) * step(_uv.y, 1.0));
}
float2 f_asp(in float2 _uv)
{
return ((_uv - 0.5) * vec2_ctor(_uAspect, 1.0));
}
float f_spd(in float _s)
{
return lerp(0.25, 2.5999999, _s);
}
float3 f_blur9(in float2 _uv, in float _r)
{
float3 _c2620 = {0.0, 0.0, 0.0};
float2 _d2621 = vec2_ctor((_r / _uAspect), _r);
{LOOP for(int _x2622 = {-1}; (_x2622 <= 1); (_x2622++))
{
{LOOP for(int _y2623 = {-1}; (_y2623 <= 1); (_y2623++))
{
(_c2620 += (f_src((_uv + (vec2_ctor(float_ctor_int(_x2622), float_ctor_int(_y2623)) * _d2621))).xyz + float3(0.0, 0.0, 0.0)));
}
}
}
}
return (_c2620 / 9.0);
}
float3x3 f_rotY(in float _a)
{
float _c2726 = cos(_a);
float _s2727 = sin(_a);
return mat3_ctor(_c2726, 0.0, (-_s2727), 0.0, 1.0, 0.0, _s2727, 0.0, _c2726);
}
float4 f_card2(in float2 _uv, in float3x3 _m, in float3 _c, in float _s)
{
float3 _rd2737 = vec3_ctor((f_asp(_uv) * 2.0), 2.2);
float3 _n2738 = mul(transpose(_m), float3(0.0, 0.0, -1.0));
float _den2739 = dot(_rd2737, _n2738);
if ((abs(_den2739) < 9.9999997e-05))
{
return float4(0.0, 0.0, 0.0, 0.0);
}
float _t2740 = (dot(_c, _n2738) / _den2739);
if ((_t2740 <= 0.0))
{
return float4(0.0, 0.0, 0.0, 0.0);
}
float3 _local2741 = ((_rd2737 * _t2740) - _c);
float2 _q2742 = ((vec2_ctor((dot(_local2741, mul(transpose(_m), float3(1.0, 0.0, 0.0))) / (_uAspect * _s)), (dot(_local2741, mul(transpose(_m), float3(0.0, 1.0, 0.0))) / _s)) * 0.5) + 0.5);
return vec4_ctor(_q2742, f_inside(_q2742), _t2740);
}
float3 f_blurredBg(in float2 _uv, in float _dim)
{
return (f_blur9((((_uv - 0.5) * 0.80000001) + 0.5), 0.029999999) * _dim);
}
float3 f_bg(in float2 _uv)
{
return lerp(float3(0.02, 0.02, 0.039999999), float3(0.079999998, 0.07, 0.12), _uv.y);
}
float4 f_fx(in float2 _uv)
{
float _T2754 = (_uTime * f_spd(_uP0.y));
float3 _col2755 = {0.0, 0.0, 0.0};
(_col2755 = lerp(f_bg(_uv), f_blurredBg(_uv, 0.30000001), step(0.5, _uP0.w)));
float _bestT2756 = {1000000000.0};
float _R2757 = (1.25 * max(_uAspect, 0.80000001));
float _s2758 = lerp(0.62, 0.44999999, _uP0.z);
{LOOP for(int _i2759 = {0}; (_i2759 < 6); (_i2759++))
{
float _th2760 = (((float_ctor_int(_i2759) / 6.0) * 6.2831855) + (_T2754 * 0.5));
float3 _c2761 = vec3_ctor((sin(_th2760) * _R2757), 0.0, (3.0 + (cos(_th2760) * _R2757)));
float4 _q2762 = f_card2(_uv, f_rotY(_th2760), _c2761, _s2758);
if (((_q2762.z > 0.5) && (_q2762.w < _bestT2756)))
{
(_bestT2756 = _q2762.w);
(_col2755 = (f_src(_q2762.xy).xyz * (0.44999999 + (0.55000001 * f_sat((-cos(_th2760)))))));
}
}
}
return vec4_ctor(_col2755, 1.0);
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

float4 _c2764 = f_fx(_vUv);
(gl_Color[0] = vec4_ctor(clamp(_c2764.xyz, 0.0, 1.0), 1.0));
return generateOutput();
}

// COMPILER INPUT HLSL END

// FRAGMENT SHADER END
