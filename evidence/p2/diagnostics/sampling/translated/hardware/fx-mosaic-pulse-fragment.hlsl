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
float _cells2758 = lerp(90.0, 18.0, _uP0.z);
float2 _grid2759 = vec2_ctor((_cells2758 * _uAspect), _cells2758);
float2 _id2760 = floor((_uv * _grid2759));
float3 _block2762 = f_src(((_id2760 + 0.5) / _grid2759)).xyz;
float _n2763 = floor(lerp(8.0, 120.0, (0.5 + (0.5 * cos((_T2754 * 1.5))))));
float2 _g2764 = vec2_ctor((_n2763 * _uAspect), _n2763);
(_col2757 = lerp(_c2756, f_src(((floor((_uv * _g2764)) + 0.5) / _g2764)).xyz, _I2755));
return vec4_ctor(_col2757, 1.0);
}
@@ PIXEL OUTPUT @@

PS_OUTPUT main(@@ PIXEL MAIN PARAMETERS @@){
@@ MAIN PROLOGUE @@
float4 _c2766 = f_fx(_vUv);
(gl_Color[0] = vec4_ctor(clamp(_c2766.xyz, 0.0, 1.0), 1.0));
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
float _cells2758 = lerp(90.0, 18.0, _uP0.z);
float2 _grid2759 = vec2_ctor((_cells2758 * _uAspect), _cells2758);
float2 _id2760 = floor((_uv * _grid2759));
float3 _block2762 = f_src(((_id2760 + 0.5) / _grid2759)).xyz;
float _n2763 = floor(lerp(8.0, 120.0, (0.5 + (0.5 * cos((_T2754 * 1.5))))));
float2 _g2764 = vec2_ctor((_n2763 * _uAspect), _n2763);
(_col2757 = lerp(_c2756, f_src(((floor((_uv * _g2764)) + 0.5) / _g2764)).xyz, _I2755));
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

float4 _c2766 = f_fx(_vUv);
(gl_Color[0] = vec4_ctor(clamp(_c2766.xyz, 0.0, 1.0), 1.0));
return generateOutput();
}

// COMPILER INPUT HLSL END

// FRAGMENT SHADER END
