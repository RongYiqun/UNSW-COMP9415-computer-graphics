
out vec4 outputColor;


// Light properties
uniform vec3 lightDirection;
uniform vec3 torchPosition;
uniform vec3 torchDirection;
uniform vec3 daylightIntensity;
uniform vec3 nightlightIntensity;
uniform vec3 torchlightIntensity;
uniform vec3 ambientIntensity;
uniform vec3 viewPos;
uniform vec4 input_color;


uniform int mode;
uniform int enable;
uniform float cutoff;
uniform float lightConstant;
uniform float lightLinear;
uniform float lightQuadratic;

// Material properties
uniform vec3 ambientCoeff;
uniform vec3 diffuseCoeff;
uniform vec3 specularCoeff;
uniform float phongExp;
uniform sampler2D tex;
uniform sampler2D normalMap;


in vec4 FragPos;
in vec3 Normal;
in vec2 TexCoords;
in vec3 Tangent;


void main()
{
    // Compute the s, v and r vectors
    vec3 normal=normalize(Normal);
    vec3 viewpos=viewPos;
    vec3 torchposition=torchPosition;
    vec3 fragpos=FragPos.xyz;
    vec3 lightdirection=lightDirection;
    vec3 torchdirection=torchDirection;
    vec3 tangent=normalize(Tangent);

    if(enable==1){
        tangent=normalize(Tangent - dot(Tangent, normal) * normal);
        vec3 bitang=normalize(cross(normal,tangent));
        mat3 TBN=mat3(tangent, bitang,normal);
        vec3 rgbNormal = texture(normalMap, TexCoords).rgb;
        normal = normalize(rgbNormal * 2.0 - 1.0);
        normal = normalize(TBN*normal);
    }
    if(mode==0){
        vec3 s = normalize(lightdirection);
        vec3 v = normalize(viewpos-fragpos);
        vec3 r = normalize(-reflect(s,normal));

        vec3 ambient = daylightIntensity*ambientIntensity*ambientCoeff;
        vec3 diffuse = daylightIntensity*diffuseCoeff*max(dot(normal,s), 0.0);
        vec3 specular=vec3(0);

        // Only show specular reflections for the front face
        if (dot(Normal,s) > 0)
            specular = daylightIntensity*specularCoeff*max(pow(dot(r,v),phongExp), 0.0);

        vec3 intensity =ambient + diffuse + specular;
        outputColor = vec4(intensity,1)*(texture(tex, TexCoords));
    }else if(mode==1){
         vec3 s = normalize((torchposition-fragpos));
         float theta = dot(s, normalize(-torchdirection));
         vec3 v = normalize(viewpos - fragpos);
         vec3 r = normalize(-reflect(s,normal));
         vec3 torchIntensity = vec3(0);
         float distance    = length(torchposition - fragpos);
         float attenuation = 1.0 / (lightConstant + lightLinear * distance + lightQuadratic * (distance * distance));
         if(theta>cutoff){
            vec3 torchAmbient = torchlightIntensity*ambientIntensity*ambientCoeff;
            vec3 torchDiffuse = torchlightIntensity*diffuseCoeff*max(dot(normal,s), 0.0)*attenuation;
            vec3 torchSpecular=vec3(0);
                    // Only show specular reflections for the front face
            if (dot(normal,s) > 0)
                torchSpecular = torchlightIntensity*specularCoeff*max(pow(dot(r,v),phongExp), 0.0)*attenuation;

            torchIntensity =torchAmbient + torchDiffuse + torchSpecular;
         }

         vec3 sn = normalize(lightdirection);
         vec3 rn = normalize(-reflect(sn,normal));
         vec3 nightAmbient = nightlightIntensity*ambientIntensity*ambientCoeff;
         vec3 nightDiffuse = nightlightIntensity*diffuseCoeff*max(dot(normal,sn), 0.0);
         vec3 nightSpecular=vec3(0);

         // Only show specular reflections for the front face
         if (dot(normal,sn) > 0)
             nightSpecular = nightlightIntensity*specularCoeff*max(pow(dot(rn,v),phongExp), 0.0);

         vec3  nightIntensity =nightAmbient + nightDiffuse + nightSpecular;
         outputColor = vec4(torchIntensity+nightIntensity,1)*(texture(tex, TexCoords));
    }
}
