// INITIAL HLSL BEGIN

#pragma warning( disable: 3556 3571 )
float4 vec4_ctor(float x0, float x1, float x2, float x3)
{
    return float4(x0, x1, x2, x3);
}
// Uniforms

uniform float _uFlipY : register(c1);
#ifdef ANGLE_ENABLE_LOOP_FLATTEN
#define LOOP [loop]
#define FLATTEN [flatten]
#else
#define LOOP
#define FLATTEN
#endif

#define ATOMIC_COUNTER_ARRAY_STRIDE 4

// Attributes
static float4 _aPosition = {0, 0, 0, 0};

static float4 gl_Position = float4(0, 0, 0, 0);

// Varyings
static  float2 _vUv = {0, 0};

cbuffer DriverConstants : register(b1)
{
    float4 dx_ViewAdjust : packoffset(c1);
    float2 dx_ViewCoords : packoffset(c2);
    float2 dx_ViewScale  : packoffset(c3);
    float clipControlOrigin : packoffset(c3.z);
    float clipControlZeroToOne : packoffset(c3.w);
};

@@ VERTEX ATTRIBUTES @@

@@ VERTEX OUTPUT @@

VS_OUTPUT main(VS_INPUT input){
@@ MAIN PROLOGUE @@
float sa01 = {0};
if ((_uFlipY == 0.0))
{
(sa01 = 1.0);
}
else
{
(sa01 = _uFlipY);
}
(gl_Position = vec4_ctor(_aPosition.x, (_aPosition.y * sa01), 0.0, 1.0));
(_vUv = ((_aPosition.xy * 0.5) + 0.5));
return generateOutput(input);
}

// INITIAL HLSL END


// COMPILER INPUT HLSL BEGIN

struct VS_OUTPUT
{
    float4 dx_Position : SV_Position;
    float4 gl_Position : TEXCOORD1;
    float2 v0 : TEXCOORD0;
};
#pragma warning( disable: 3556 3571 )
float4 vec4_ctor(float x0, float x1, float x2, float x3)
{
    return float4(x0, x1, x2, x3);
}
// Uniforms

uniform float _uFlipY : register(c1);
#ifdef ANGLE_ENABLE_LOOP_FLATTEN
#define LOOP [loop]
#define FLATTEN [flatten]
#else
#define LOOP
#define FLATTEN
#endif

#define ATOMIC_COUNTER_ARRAY_STRIDE 4

// Attributes
static float4 _aPosition = {0, 0, 0, 0};

static float4 gl_Position = float4(0, 0, 0, 0);

// Varyings
static  float2 _vUv = {0, 0};

cbuffer DriverConstants : register(b1)
{
    float4 dx_ViewAdjust : packoffset(c1);
    float2 dx_ViewCoords : packoffset(c2);
    float2 dx_ViewScale  : packoffset(c3);
    float clipControlOrigin : packoffset(c3.z);
    float clipControlZeroToOne : packoffset(c3.w);
};

struct VS_INPUT
{
    float4 _aPosition : TEXCOORD0;
};

void initAttributes(VS_INPUT input)
{
    _aPosition = input._aPosition;
}


VS_OUTPUT generateOutput(VS_INPUT input)
{
    VS_OUTPUT output;
    output.gl_Position = gl_Position;
    output.dx_Position.x = gl_Position.x;
    output.dx_Position.y = clipControlOrigin * gl_Position.y;
    if (clipControlZeroToOne)
    {
        output.dx_Position.z = gl_Position.z;
    } else {
        output.dx_Position.z = (gl_Position.z + gl_Position.w) * 0.5;
    }
    output.dx_Position.w = gl_Position.w;
    output.v0 = _vUv;

    return output;
}

VS_OUTPUT main(VS_INPUT input){
    initAttributes(input);

float sa01 = {0};
if ((_uFlipY == 0.0))
{
(sa01 = 1.0);
}
else
{
(sa01 = _uFlipY);
}
(gl_Position = vec4_ctor(_aPosition.x, (_aPosition.y * sa01), 0.0, 1.0));
(_vUv = ((_aPosition.xy * 0.5) + 0.5));
return generateOutput(input);
}

// COMPILER INPUT HLSL END

// VERTEX SHADER END
