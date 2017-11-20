#ifndef __GLES__H__
#define __GLES__H__

#include <vector>

int gles_type = 0;
std::vector<float> gles_vertices;
std::vector<float> gles_texcoord;


//shader stuff
GLuint model_program_ = UINT_MAX;
GLint model_position_param_;
GLint model_texture_param_;
GLint model_uv_param_;
GLint model_modelview_param_;
GLint model_projection_param_;


//shader
const char* kTextureShader[] = {R"glsl(
    #version 100
    precision highp float;
    uniform mat4 u_P;
    uniform mat4 u_MV;
    attribute vec4 a_Position;
    attribute vec2 a_UV;
    varying vec2 v_UV;

    void main() {
      v_UV = a_UV;
      gl_Position = u_P * u_MV * a_Position;
      //gl_Position = a_Position;
    })glsl",

    R"glsl(
    #version 100
    precision highp float;
    uniform sampler2D u_color_texture;
    varying vec2 v_UV;

    void main() {
      gl_FragColor = texture2D(u_color_texture, v_UV);
      if (gl_FragColor.a < 0.5)
        discard;
      gl_FragColor.a = 1.0;
    })glsl"
};

void glesBegin(int type)
{
    gles_vertices.clear();
    gles_texcoord.clear();
    gles_type = type;
}

void glesVertex2d(double x, double y)
{
    gles_vertices.push_back((float)x);
    gles_vertices.push_back((float)y);
    gles_vertices.push_back(0);
}

void glesVertex2f(float x, float y)
{
    gles_vertices.push_back(x);
    gles_vertices.push_back(y);
    gles_vertices.push_back(0);
}

void glesVertex3f(float x, float y, float z)
{
    gles_vertices.push_back(x);
    gles_vertices.push_back(y);
    gles_vertices.push_back(z);
}

void glesTexCoord2f(float u, float v)
{
    gles_texcoord.push_back(u);
    gles_texcoord.push_back(v);
}

void glesEnd()
{
    //get matrices
    float projection[16];
    float modelview[16];
    glGetFloatv(GL_PROJECTION_MATRIX, projection);
    glGetFloatv(GL_MODELVIEW_MATRIX, modelview);

    //compile shader
    if (model_program_ == UINT_MAX)
    {
        GLuint vertex_shader = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(vertex_shader, 1, &kTextureShader[0], nullptr);
        glCompileShader(vertex_shader);

        GLuint fragment_shader = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(fragment_shader, 1, &kTextureShader[1], nullptr);
        glCompileShader(fragment_shader);

        model_program_ = glCreateProgram();
        glAttachShader(model_program_, vertex_shader);
        glAttachShader(model_program_, fragment_shader);
        glLinkProgram(model_program_);
        glUseProgram(model_program_);

        model_position_param_ = glGetAttribLocation(model_program_, "a_Position");
        model_uv_param_ = glGetAttribLocation(model_program_, "a_UV");
        model_modelview_param_ = glGetUniformLocation(model_program_, "u_MV");
        model_projection_param_ = glGetUniformLocation(model_program_, "u_P");
        model_texture_param_ = glGetUniformLocation(model_program_, "u_color_texture");
    }

    //bind shader
    glUseProgram(model_program_);
    glUniform1i(model_texture_param_, 0);
    glUniformMatrix4fv(model_modelview_param_, 1, GL_FALSE, modelview);
    glUniformMatrix4fv(model_projection_param_, 1, GL_FALSE, projection);


    //render data
    glEnableVertexAttribArray((GLuint) model_position_param_);
    glEnableVertexAttribArray((GLuint) model_uv_param_);
    glVertexAttribPointer((GLuint) model_position_param_, 3, GL_FLOAT, GL_FALSE, 0, gles_vertices.data());
    glVertexAttribPointer((GLuint) model_uv_param_, 2, GL_FLOAT, GL_FALSE, 0, gles_texcoord.data());
    glDrawArrays(gles_type, 0, (GLsizei) gles_vertices.size());
    glUseProgram(0);
}

#endif

