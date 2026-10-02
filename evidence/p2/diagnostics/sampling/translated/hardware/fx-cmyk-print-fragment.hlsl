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
float2x2 mat2_ctor(float x0, float x1, float x2, float x3)
{
    return float2x2(x0, x1, x2, x3);
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

float dyn_index_vec3_int(in float3 base, in int index)
{
switch (index) {
case (0):
return base[0];
case (1):
return base[1];
case (2):
return base[2];
default:
break;
}
if ((index < 0))
{
return base[0];
}
{
return base[2];
}
}
void dyn_index_write_vec3_int(inout float3 base, in int index, in float value)
{
switch (index) {
case (0):
(base[0] = value);
return;
case (1):
(base[1] = value);
return;
case (2):
(base[2] = value);
return;
default:
break;
}
if ((index < 0))
{
(base[0] = value);
return;
}
{
(base[2] = value);
}
}
float f_hash(in float2 _p)
{
(_p = frac((_p * float2(123.34, 456.20999))));
(_p += dot(_p, (_p + 45.32)));
return frac((_p.x * _p.y));
}
float2x2 f_rot(in float _a)
{
float _c2587 = cos(_a);
float _s2588 = sin(_a);
return mat2_ctor(_c2587, (-_s2588), _s2588, _c2587);
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
float4 f_fx(in float2 _uv)
{
float _T2754 = (_uTime * f_spd(_uP0.y));
float _I2755 = _uP0.x;
float3 _c2756 = f_src(_uv).xyz;
float3 _col2757 = _c2756;
float2 _p2758 = f_asp(_uv);
float _grain2759 = (f_hash((floor(((_uv * _uSize) * 0.60000002)) + (frac((_T2754 * 13.0)) * 50.0))) - 0.5);
float _s2760 = {70.0};
float3 _inkc2763 = {0.0, 0.0, 0.0};
float3 _lv2764 = (float3(1.0, 1.0, 1.0) - _c2756);
{ for(int _k2765 = {0}; (_k2765 < 3); (_k2765++))
{
float2 _gg2766 = ((mul(transpose(f_rot(((float_ctor_int(_k2765) * 0.5) + 0.2))), _uv) * vec2_ctor(_uAspect, 1.0)) * _s2760);
float2 _ff2767 = (frac(_gg2766) - 0.5);
float sad3 = {0};
if ((_k2765 == 0))
{
(sad3 = _lv2764.x);
}
else
{
float sad4 = {0};
if ((_k2765 == 1))
{
(sad4 = _lv2764.y);
}
else
{
(sad4 = _lv2764.z);
}
(sad3 = sad4);
}
float _ch2768 = sad3;
int sadc = _k2765;
float sadd = dyn_index_vec3_int(_inkc2763, sadc);
(sadd = smoothstep(0.050000001, 0.0, (length(_ff2767) - (_ch2768 * 0.5))));
dyn_index_write_vec3_int(_inkc2763, sadc, sadd);
}
}
(_col2757 = lerp(_c2756, (float3(0.97000003, 0.94999999, 0.89999998) - (_inkc2763 * 0.85000002)), _I2755));
return vec4_ctor(_col2757, 1.0);
}
@@ PIXEL OUTPUT @@

PS_OUTPUT main(@@ PIXEL MAIN PARAMETERS @@){
@@ MAIN PROLOGUE @@
float4 _c2770 = f_fx(_vUv);
(gl_Color[0] = vec4_ctor(clamp(_c2770.xyz, 0.0, 1.0), 1.0));
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
float2x2 mat2_ctor(float x0, float x1, float x2, float x3)
{
    return float2x2(x0, x1, x2, x3);
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

float dyn_index_vec3_int(in float3 base, in int index)
{
switch (index) {
case (0):
return base[0];
case (1):
return base[1];
case (2):
return base[2];
default:
break;
}
if ((index < 0))
{
return base[0];
}
{
return base[2];
}
}
void dyn_index_write_vec3_int(inout float3 base, in int index, in float value)
{
switch (index) {
case (0):
(base[0] = value);
return;
case (1):
(base[1] = value);
return;
case (2):
(base[2] = value);
return;
default:
break;
}
if ((index < 0))
{
(base[0] = value);
return;
}
{
(base[2] = value);
}
}
float f_hash(in float2 _p)
{
(_p = frac((_p * float2(123.34, 456.20999))));
(_p += dot(_p, (_p + 45.32)));
return frac((_p.x * _p.y));
}
float2x2 f_rot(in float _a)
{
float _c2587 = cos(_a);
float _s2588 = sin(_a);
return mat2_ctor(_c2587, (-_s2588), _s2588, _c2587);
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
float4 f_fx(in float2 _uv)
{
float _T2754 = (_uTime * f_spd(_uP0.y));
float _I2755 = _uP0.x;
float3 _c2756 = f_src(_uv).xyz;
float3 _col2757 = _c2756;
float2 _p2758 = f_asp(_uv);
float _grain2759 = (f_hash((floor(((_uv * _uSize) * 0.60000002)) + (frac((_T2754 * 13.0)) * 50.0))) - 0.5);
float _s2760 = {70.0};
float3 _inkc2763 = {0.0, 0.0, 0.0};
float3 _lv2764 = (float3(1.0, 1.0, 1.0) - _c2756);
{ for(int _k2765 = {0}; (_k2765 < 3); (_k2765++))
{
float2 _gg2766 = ((mul(transpose(f_rot(((float_ctor_int(_k2765) * 0.5) + 0.2))), _uv) * vec2_ctor(_uAspect, 1.0)) * _s2760);
float2 _ff2767 = (frac(_gg2766) - 0.5);
float sad3 = {0};
if ((_k2765 == 0))
{
(sad3 = _lv2764.x);
}
else
{
float sad4 = {0};
if ((_k2765 == 1))
{
(sad4 = _lv2764.y);
}
else
{
(sad4 = _lv2764.z);
}
(sad3 = sad4);
}
float _ch2768 = sad3;
int sadc = _k2765;
float sadd = dyn_index_vec3_int(_inkc2763, sadc);
(sadd = smoothstep(0.050000001, 0.0, (length(_ff2767) - (_ch2768 * 0.5))));
dyn_index_write_vec3_int(_inkc2763, sadc, sadd);
}
}
(_col2757 = lerp(_c2756, (float3(0.97000003, 0.94999999, 0.89999998) - (_inkc2763 * 0.85000002)), _I2755));
return vec4_ctor(_col2757, 1.0);
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

float4 _c2770 = f_fx(_vUv);
(gl_Color[0] = vec4_ctor(clamp(_c2770.xyz, 0.0, 1.0), 1.0));
return generateOutput();
}

// COMPILER INPUT HLSL END

// FRAGMENT SHADER END
