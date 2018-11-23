in vec3 position;

in vec3 normal;

in vec2 texCoord;

in vec3 tangent;


uniform mat4 model_matrix;

uniform mat4 view_matrix;

uniform mat4 proj_matrix;

out vec4 FragPos;
out vec3 Normal;
out vec2 TexCoords;
out vec3 Tangent;

void main() {

    FragPos = model_matrix * vec4(position, 1);
    gl_Position = proj_matrix * view_matrix * FragPos;

    Normal = mat3(transpose(inverse(model_matrix)))*normal;
    Tangent= mat3(transpose(inverse(model_matrix)))*tangent;
    TexCoords = texCoord;
}
