///////////////////////////////////////////////////////////////////////////////
// 
// (c) Visage Technologies AB 2002 - 2023  All rights reserved. 
// 
// This file is part of visage|SDK(tm). 
// Unauthorized copying of this file, via any medium is strictly prohibited. 
// 
// No warranty, explicit or implicit, provided. 
// 
// This is proprietary software. No part of this software may be used or 
// reproduced in any form or by any means otherwise than in accordance with
// any written license granted by Visage Technologies AB. 
// 
/////////////////////////////////////////////////////////////////////////////


#include "VisageRendering.h"
#include "MathMacros.h"

namespace VisageSDK
{

#if defined (IOS) || defined(ANDROID)
void gluLookAt(GLfloat eyex, GLfloat eyey, GLfloat eyez,
    GLfloat centerx, GLfloat centery, GLfloat centerz,
    GLfloat upx, GLfloat upy, GLfloat upz)
{
    GLfloat m[16];
    GLfloat x[3], y[3], z[3];
    GLfloat mag;
    GLfloat invmag;

    /* Make rotation matrix */

    /* Z vector */
    z[0] = eyex - centerx;
    z[1] = eyey - centery;
    z[2] = eyez - centerz;
    mag = sqrt(z[0] * z[0] + z[1] * z[1] + z[2] * z[2]);
    if (mag) {          /* mpichler, 19950515 */
        invmag = 1.0f / mag;
        z[0] *= invmag;
        z[1] *= invmag;
        z[2] *= invmag;
    }

    /* Y vector */
    y[0] = upx;
    y[1] = upy;
    y[2] = upz;

    /* X vector = Y cross Z */
    x[0] = y[1] * z[2] - y[2] * z[1];
    x[1] = -y[0] * z[2] + y[2] * z[0];
    x[2] = y[0] * z[1] - y[1] * z[0];

    /* Recompute Y = Z cross X */
    y[0] = z[1] * x[2] - z[2] * x[1];
    y[1] = -z[0] * x[2] + z[2] * x[0];
    y[2] = z[0] * x[1] - z[1] * x[0];

    /* mpichler, 19950515 */
    /* cross product gives area of parallelogram, which is < 1.0 for
    * non-perpendicular unit-length vectors; so normalize x, y here
    */

    mag = sqrt(x[0] * x[0] + x[1] * x[1] + x[2] * x[2]);
    if (mag) {
        invmag = 1.0f / mag;
        x[0] *= invmag;
        x[1] *= invmag;
        x[2] *= invmag;
    }

    mag = sqrt(y[0] * y[0] + y[1] * y[1] + y[2] * y[2]);
    if (mag) {
        invmag = 1.0f / mag;
        y[0] *= invmag;
        y[1] *= invmag;
        y[2] *= invmag;
    }

#define M(row,col)  m[col*4+row]
    M(0, 0) = x[0];
    M(0, 1) = x[1];
    M(0, 2) = x[2];
    M(0, 3) = 0.0;
    M(1, 0) = y[0];
    M(1, 1) = y[1];
    M(1, 2) = y[2];
    M(1, 3) = 0.0;
    M(2, 0) = z[0];
    M(2, 1) = z[1];
    M(2, 2) = z[2];
    M(2, 3) = 0.0;
    M(3, 0) = 0.0;
    M(3, 1) = 0.0;
    M(3, 2) = 0.0;
    M(3, 3) = 1.0;
#undef M
    glMultMatrixf(m);

    /* Translate Eye to Origin */
    glTranslatef(-eyex, -eyey, -eyez);
}

#endif

static GLuint frame_tex_id = 0;
static GLuint logo_tex_id = -1;
static GLuint img_tex_id = -1;
static GLuint font_tex_id = -1;
static float tex_x_coord = 0;
static float tex_y_coord = 0;
static bool video_texture_inited = false;
static int video_texture_width = 0;
static int video_texture_height = 0;
static int numberOfVertices = 0;
static float m_fontUV[256][12];
static int font_width = 0;
static int font_height = 0;
static const VsImage* font_tex = NULL;

static int winWidth;
static int winHeight;

static int frameWidth;
static int frameHeight;

static std::vector<GLushort> output;

typedef struct CubicPoly
{
    float c0, c1, c2, c3;
    float eval(float t)
    {
        float t2 = t*t;
        float t3 = t2 * t;
        return c0 + c1*t + c2*t2 + c3*t3;
    }
} CubicPoly;

typedef struct Vec2D
{
    Vec2D(float _x, float _y) : x(_x), y(_y) {}
    float x, y;
} Vec2D;

static void InitCubicPoly(float x0, float x1, float t0, float t1, CubicPoly &p)
{
    p.c0 = x0;
    p.c1 = t0;
    p.c2 = -3 * x0 + 3 * x1 - 2 * t0 - t1;
    p.c3 = 2 * x0 - 2 * x1 + t0 + t1;
}

static void InitCatmullRom(float x0, float x1, float x2, float x3, CubicPoly &p)
{
    InitCubicPoly(x1, x2, 0.5f*(x2 - x0), 0.5f*(x3 - x1), p);
}

static void InitNonuniformCatmullRom(float x0, float x1, float x2, float x3, float dt0, float dt1, float dt2, CubicPoly &p)
{
    float t1 = (x1 - x0) / dt0 - (x2 - x0) / (dt0 + dt1) + (x2 - x1) / dt1;
    float t2 = (x2 - x1) / dt1 - (x3 - x1) / (dt1 + dt2) + (x3 - x2) / dt2;

    t1 *= dt1;
    t2 *= dt1;

    InitCubicPoly(x1, x2, t1, t2, p);
}

static float VecDistSquared(const Vec2D& p, const Vec2D& q)
{
    float dx = q.x - p.x;
    float dy = q.y - p.y;
    return dx*dx + dy*dy;
}

static void InitCentripetalCR(const Vec2D& p0, const Vec2D& p1, const Vec2D& p2, const Vec2D& p3, CubicPoly &px, CubicPoly &py)
{
    float dt0 = powf(VecDistSquared(p0, p1), 0.25f);
    float dt1 = powf(VecDistSquared(p1, p2), 0.25f);
    float dt2 = powf(VecDistSquared(p2, p3), 0.25f);

    if (dt1 < 1e-4f)    dt1 = 1.0f;
    if (dt0 < 1e-4f)    dt0 = dt1;
    if (dt2 < 1e-4f)    dt2 = dt1;

    InitNonuniformCatmullRom(p0.x, p1.x, p2.x, p3.x, dt0, dt1, dt2, px);
    InitNonuniformCatmullRom(p0.y, p1.y, p2.y, p3.y, dt0, dt1, dt2, py);
}

static void SetupCamera(int width, int height, float f)
{
    GLfloat x_offset = 1;
    GLfloat y_offset = 1;
    if (width > height)
        x_offset = ((GLfloat)width) / ((GLfloat)height);
    else if (width < height)
        y_offset = ((GLfloat)height) / ((GLfloat)width);

    //Note:
    // FOV in radians is: fov*0.5 = arctan ((top-bottom)*0.5 / near)
    // In this case: FOV = 2 * arctan(frustum_y / frustum_near)
    //set frustum specs
    GLfloat frustum_near = 0.001f;
    GLfloat frustum_far = 30; //hard to estimate face too far away
    GLfloat frustum_x = x_offset*frustum_near / f;
    GLfloat frustum_y = y_offset*frustum_near / f;
    //set frustum
    glMatrixMode(GL_PROJECTION);
    glLoadIdentity();
#if defined(WIN32) || defined(LINUX) || defined(MAC_OS_X)
    glFrustum(-frustum_x, frustum_x, -frustum_y, frustum_y, frustum_near, frustum_far);
#else
    glFrustumf(-frustum_x, frustum_x, -frustum_y, frustum_y, frustum_near, frustum_far);
#endif
    glMatrixMode(GL_MODELVIEW);
    //clear matrix
    glLoadIdentity();
    //camera in (0,0,0) looking at (0,0,1) up vector (0,1,0)
    gluLookAt(0, 0, 0, 0, 0, 1, 0, 1, 0);

}

static void ClearGL()
{
    glClearColor(0, 0, 0, 0);
    glClear(GL_COLOR_BUFFER_BIT);
}

static void DrawSpline2D(int *points, int num, FaceData* trackingData, bool useAlpha = false)
{
    if (num < 2)
        return;

    std::vector<float> pointCoords;
    std::vector<float> pointCoordsQuality;

    int n = 0;
    FDP *fdp = trackingData->featurePoints2D;

    for (int i = 0; i < num; i++)
    {
        const FeaturePoint &fp = fdp->getFP(points[2 * i], points[2 * i + 1]);

        if (fp.defined && fp.detected && fp.pos[0] != 0 && fp.pos[1] != 0)
        {
            //vector of points position
            pointCoords.push_back(fp.pos[0]);
            pointCoords.push_back(fp.pos[1]);

            float alphaChannel = (fp.quality > 0 && useAlpha) ? std::max(fp.quality*0.75f, 0.2f) : 0.75f;

            pointCoordsQuality.push_back(alphaChannel);
            pointCoordsQuality.push_back(alphaChannel);

            n++;
        }
    }

    if (pointCoords.size() == 0 || n <= 2)
        return;

    int factor = 10;
    std::vector<float> pointsToDraw;
    std::vector<float> qualityToDraw;
    //
    // Interpolate between points position and points quality
    // Poistion is used to determin the spline shape
    // Quality is used to determin the color of the spline
    VisageRendering::CalcSpline(pointCoords, factor, pointsToDraw);
    VisageRendering::CalcSpline(pointCoordsQuality, factor, qualityToDraw);
    //
    int nVert = (int)pointsToDraw.size() / 2;
    float *vertices = new float[nVert * 3];
    float *colors = new float[nVert * 4];

    float rChannel = 0.69f;
    float gChannel = 0.77f;
    float bChannel = 0.87f;

    int cnt = 0;
    for (int i = 0; i < nVert; ++i)
    {
        vertices[3 * i + 0] = pointsToDraw.at(cnt++);
        vertices[3 * i + 1] = pointsToDraw.at(cnt++);
        vertices[3 * i + 2] = 0.0f;
        //
        colors[4 * i + 0] = rChannel;
        colors[4 * i + 1] = gChannel;
        colors[4 * i + 2] = bChannel;
        colors[4 * i + 3] = qualityToDraw[i * 2];
    }


    glEnable(GL_BLEND);
    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
    glEnableClientState(GL_VERTEX_ARRAY);
    glEnableClientState(GL_COLOR_ARRAY);
    glVertexPointer(3, GL_FLOAT, 0, vertices);
    glColorPointer(4, GL_FLOAT, 0, colors);
    glDrawArrays(GL_LINE_STRIP, 0, nVert);
    glDisableClientState(GL_COLOR_ARRAY);
    glDisableClientState(GL_VERTEX_ARRAY);
    glDisable(GL_BLEND);


    //clean-up
    delete[] vertices;
    delete[] colors;
}

static void DrawElipse(float x, float y, float radiusX, float radiusY, bool filled = true)
{
    static const int circle_points = 100;

    float step = 2 * V_PI / circle_points;
    float theta = 0.0;

    float *vertices = new float[circle_points * 2];
    for (int i = 0; i < circle_points; i++)
    {
        vertices[2 * i] = x + radiusX * cos(theta);
        vertices[2 * i + 1] = y + radiusY * sin(theta);
        theta += step;
    }

    glEnableClientState(GL_VERTEX_ARRAY);
    glVertexPointer(2, GL_FLOAT, 0, vertices);

    if (filled)
        glDrawArrays(GL_TRIANGLE_FAN, 0, circle_points);
    else
        glDrawArrays(GL_LINE_LOOP, 0, circle_points);

    glDisableClientState(GL_VERTEX_ARRAY);

    delete[] vertices;
}

static void DrawPoints2D(int *points, int num, bool singleColor, FaceData* trackingData, VsImage* frame, bool drawQuality = true, bool useAlpha = false)
{
#ifdef IOS
    float radius = (trackingData->faceScale / (float)frame->width) * 10;
#elif defined(MAC_OS_X) || defined(WIN32)
    float faceScaleNormalized = (frame->width > frame->height) ? trackingData->faceScale / (float)frame->width : trackingData->faceScale / (float)frame->height;
    float radius = ((1 + (faceScaleNormalized * 2.0)) * (1 + (winHeight / 250.0)));
#else
    float radius = (trackingData->faceScale / (float)frame->width) * 30;
#endif

    FDP *fdp = trackingData->featurePoints2D;

    float *vertices = new float[num * 2];
    float *colorsQuality = new float[num * 4];
    float *colorsCircle = new float[num * 4];
    int n = 0;
    for (int i = 0; i < num; i++) {
        const FeaturePoint &fp = fdp->getFP(points[2 * i], points[2 * i + 1]);
        if (fp.detected && fp.defined && fp.pos[0] != 0 && fp.pos[1] != 0) {
            vertices[2 * n + 0] = fp.pos[0];
            vertices[2 * n + 1] = fp.pos[1];
            if (drawQuality && fp.quality >= 0) {
                colorsQuality[4 * n + 0] = 1.0f - fp.quality;
                colorsQuality[4 * n + 1] = fp.quality;
                colorsQuality[4 * n + 2] = 0;
                colorsQuality[4 * n + 3] = useAlpha ? std::max(fp.quality, 0.5f) : 1.0f;
                //
                colorsCircle[4 * n + 0] = 0.0f;
                colorsCircle[4 * n + 1] = 0.0f;
                colorsCircle[4 * n + 2] = 0.0f;
                colorsCircle[4 * n + 3] = useAlpha ? std::max(fp.quality, 0.3f) : 1.0f;
            }
            else {
                colorsQuality[4 * n + 0] = 0;
                colorsQuality[4 * n + 1] = 1.0f;
                colorsQuality[4 * n + 2] = 1.0f;
                colorsQuality[4 * n + 3] = 1.0f;
                //
                colorsCircle[4 * n + 0] = 0.0f;
                colorsCircle[4 * n + 1] = 0.0f;
                colorsCircle[4 * n + 2] = 0.0f;
                colorsCircle[4 * n + 3] = 1.0f;
            }
            n++;
        }
    }

    glEnable(GL_BLEND);
    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
    glEnable(GL_POINT_SMOOTH);
    glEnableClientState(GL_VERTEX_ARRAY);
    glVertexPointer(2, GL_FLOAT, 0, vertices);

    glPointSize(radius);
    glEnableClientState(GL_COLOR_ARRAY);
    glColorPointer(4, GL_FLOAT, 0, colorsCircle);
    glDrawArrays(GL_POINTS, 0, n);
    glDisableClientState(GL_COLOR_ARRAY);

    if (!singleColor)
    {
        glPointSize(0.65f * radius);
        glEnableClientState(GL_COLOR_ARRAY);
        glColorPointer(4, GL_FLOAT, 0, colorsQuality);
        glDrawArrays(GL_POINTS, 0, n);
        glDisableClientState(GL_COLOR_ARRAY);
    }
    glDisableClientState(GL_VERTEX_ARRAY);
    glDisable(GL_BLEND);
    //clean-up
    delete[] vertices;
    delete[] colorsQuality;
    delete[] colorsCircle;
}

static void DrawPoints3D(int *points, int num, bool singleColor, FaceData* trackingData, VsImage* frame, bool relative)
{

#ifdef IOS
    float radius = (trackingData->faceScale / (float)frame->width) * 10;
#elif MAC_OS_X
    float radius = (trackingData->faceScale / (float)frame->width) * 20;
#else
    float faceScaleNormalized = (frame->width > frame->height) ? trackingData->faceScale / (float)frame->width : trackingData->faceScale / (float)frame->height;
    float radius = ((1 + (faceScaleNormalized * 2.0)) * (1 + (winHeight / 250.0)));
#endif

    FDP *fdp = relative ? trackingData->featurePoints3DRelative : trackingData->featurePoints3D;

    float *vertices = new float[num * 3];
    int n = 0;
    for (int i = 0; i < num; i++)
    {
        const FeaturePoint &fp = fdp->getFP(points[2 * i], points[2 * i + 1]);
        if (fp.defined && fp.pos[0] != 0 && fp.pos[1] != 0)
        {
            vertices[3 * n + 0] = fp.pos[0];
            vertices[3 * n + 1] = fp.pos[1];
            vertices[3 * n + 2] = fp.pos[2];
            n++;
        }
    }

    glEnable(GL_POINT_SMOOTH);
    glEnableClientState(GL_VERTEX_ARRAY);
    glVertexPointer(3, GL_FLOAT, 0, vertices);
    glPointSize(radius);
    glColor4ub(0, 0, 0, 255);
    glDrawArrays(GL_POINTS, 0, n);
    if (!singleColor)
    {
        glPointSize(0.8f * radius);
        glColor4ub(0, 255, 255, 255);
        glDrawArrays(GL_POINTS, 0, n);
    }
    glDisableClientState(GL_VERTEX_ARRAY);

    //clean-up
    delete[] vertices;
}

static int NearestPow2(int n)
{
    unsigned int v; // compute the next highest power of 2 of 32-bit v

    v = n;
    v--;
    v |= v >> 1;
    v |= v >> 2;
    v |= v >> 4;
    v |= v >> 8;
    v |= v >> 16;
    v++;

    return v;
}

static void InitFrameTex(int x_size, int y_size, const VsImage *image)
{
    //Create The Texture
    glGenTextures(1, &frame_tex_id);

    //Typical Texture Generation Using Data From The Bitmap
    glBindTexture(GL_TEXTURE_2D, frame_tex_id);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);

    //Creating pow2 texture
#ifdef IOS
    switch (image->nChannels) {
    case 1:
        glTexImage2D(GL_TEXTURE_2D, 0, GL_LUMINANCE, x_size, y_size, 0, GL_LUMINANCE, GL_UNSIGNED_BYTE, 0);
        break;
    case 3: glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, x_size, y_size, 0, GL_RGB, GL_UNSIGNED_BYTE, 0);
        break;
    case 4:
    default:
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, x_size, y_size, 0, GL_BGRA, GL_UNSIGNED_BYTE, 0);
        break;
    }
#elif ANDROID
    switch (image->nChannels)
    {
    case 1:
        glTexImage2D(GL_TEXTURE_2D, 0, GL_LUMINANCE, x_size, y_size, 0, GL_LUMINANCE, GL_UNSIGNED_BYTE, 0);
        break;
    case 3:
    case 4:
    default:
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, x_size, y_size, 0, GL_RGB, GL_UNSIGNED_BYTE, 0);
        break;
    }
#else
    glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, x_size, y_size, 0, GL_LUMINANCE, GL_UNSIGNED_BYTE, 0);
#endif

    tex_x_coord = (float)image->width / (float)x_size;
    tex_y_coord = (float)image->height / (float)y_size;
}

static void GenerateImgTex(GLuint &tex_id)
{
    glGenTextures(1, &tex_id);

    //Bind the newly created texture
    glBindTexture(GL_TEXTURE_2D, tex_id);

    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);

}

void VisageRendering::DisplayLogo(const VsImage *logo, int width, int height)
{
    //Create the texture if not inited
    if (logo_tex_id == -1)
    {
        glGenTextures(1, &logo_tex_id);

        //Bind the newly created texture
        glBindTexture(GL_TEXTURE_2D, logo_tex_id);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);

        //Creating texture
#ifdef IOS
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, logo->width, logo->height, 0, GL_BGRA, GL_UNSIGNED_BYTE, logo->imageData);
#elif ANDROID
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, logo->width, logo->height, 0, GL_RGBA, GL_UNSIGNED_BYTE, logo->imageData);
#else
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, logo->width, logo->height, 0, GL_BGRA_EXT, GL_UNSIGNED_BYTE, logo->imageData);
#endif
    }

    glBindTexture(GL_TEXTURE_2D, logo_tex_id);

#if defined(WIN32) || defined(LINUX)
    glPushAttrib(GL_DEPTH_BUFFER_BIT | GL_VIEWPORT_BIT | GL_ENABLE_BIT | GL_FOG_BIT | GL_STENCIL_BUFFER_BIT | GL_TRANSFORM_BIT | GL_TEXTURE_BIT);
#endif

    glDisable(GL_ALPHA_TEST);
    glDisable(GL_DEPTH_TEST);
    glDisable(GL_DITHER);
    glDisable(GL_FOG);
    glDisable(GL_SCISSOR_TEST);
    glDisable(GL_STENCIL_TEST);
    glDisable(GL_LIGHTING);
    glDisable(GL_LIGHT0);

    //transparency
    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
    glEnable(GL_BLEND);

    //use texture
    glEnable(GL_TEXTURE_2D);

    glMatrixMode(GL_MODELVIEW);
    glPushMatrix();
    glLoadIdentity();

    glMatrixMode(GL_PROJECTION);
    glPushMatrix();
    glLoadIdentity();

#if defined(WIN32) || defined(LINUX) || defined(MAC_OS_X)
    glOrtho(0.0f, 1.0f, 0.0f, 1.0f, -10.0f, 10.0f);
#else
    glOrthof(0.0f, 1.0f, 0.0f, 1.0f, -10.0f, 10.0f);
#endif

    //logo aspect
    float logoAspect = logo->width / (float)logo->height;
    //viewport aspect
    float viewportAspect = width / (float)height;
    //set logo position to upper right corner, maintain logo aspect relative
    float x = 0.75f;
    float y = 1 - ((1 - x) * viewportAspect / logoAspect);

    GLfloat vertices[] = {
        x,y,-5.0f,
        1.0f,y,-5.0f,
        x,1.0f,-5.0f,
        1.0f,1.0f,-5.0f,
    };

    //tex coords are flipped upside down instead of an image
    GLfloat texcoords[] = {
        0.0f,1.0f,
        1.0f,1.0f,
        0.0f,0.0f,
        1.0f,0.0f,
    };

    glColor4f(1.0f, 1.0f, 1.0f, 1.0f);

    glEnableClientState(GL_TEXTURE_COORD_ARRAY);
    glEnableClientState(GL_VERTEX_ARRAY);

    glVertexPointer(3, GL_FLOAT, 0, vertices);
    glTexCoordPointer(2, GL_FLOAT, 0, texcoords);

    glViewport(0, 0, width, height);

    //drawing vertices and texcoords
    glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);

    glDisableClientState(GL_TEXTURE_COORD_ARRAY);
    glDisableClientState(GL_VERTEX_ARRAY);

    glDisable(GL_TEXTURE_2D);
    glDisable(GL_BLEND);

    glPopMatrix();

    glMatrixMode(GL_MODELVIEW);
    glPopMatrix();

    //disable logo texture
    glBindTexture(GL_TEXTURE_2D, 0);

#if defined(WIN32) || defined(LINUX)
    glPopAttrib();
#endif

    //glClear(GL_DEPTH_BUFFER_BIT);
}

void VisageRendering::DisplayFrame(const VsImage *image, int width, int height)
{
    glPixelStorei(GL_UNPACK_ALIGNMENT, (image->widthStep & 3) ? 1 : 4);

    if (video_texture_inited && (video_texture_width != image->width || video_texture_height != image->height))
    {
        glDeleteTextures(1, &frame_tex_id);
        video_texture_inited = false;
    }

    if (!video_texture_inited)
    {
        InitFrameTex(NearestPow2(image->width), NearestPow2(image->height), image);
        video_texture_width = image->width;
        video_texture_height = image->height;
        video_texture_inited = true;
    }

    glBindTexture(GL_TEXTURE_2D, frame_tex_id);

    switch (image->nChannels) {
    case 3:
#if defined (IOS) || defined (ANDROID)
        glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, image->width, image->height, GL_RGB, GL_UNSIGNED_BYTE, image->imageData);
#else
        glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, image->width, image->height, GL_BGR, GL_UNSIGNED_BYTE, image->imageData);
#endif
        break;
    case 4:
#if defined(IOS) || defined(MAC_OS_X)
        glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, image->width, image->height, GL_BGRA, GL_UNSIGNED_BYTE, image->imageData);
#else
        glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, image->width, image->height, GL_RGBA, GL_UNSIGNED_BYTE, image->imageData);
#endif
        break;
    case 1:
        glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, image->width, image->height, GL_LUMINANCE, GL_UNSIGNED_BYTE, image->imageData);
        break;
    default:
        return;
    }

#if defined(WIN32) || defined(LINUX)
    glPushAttrib(GL_DEPTH_BUFFER_BIT | GL_VIEWPORT_BIT | GL_ENABLE_BIT | GL_FOG_BIT | GL_STENCIL_BUFFER_BIT | GL_TRANSFORM_BIT | GL_TEXTURE_BIT);
#endif

    glDisable(GL_ALPHA_TEST);
    glDisable(GL_DEPTH_TEST);
    glDisable(GL_BLEND);
    glDisable(GL_DITHER);
    glDisable(GL_FOG);
    glDisable(GL_SCISSOR_TEST);
    glDisable(GL_STENCIL_TEST);
    glDisable(GL_LIGHTING);
    glDisable(GL_LIGHT0);
    glEnable(GL_TEXTURE_2D);

    glMatrixMode(GL_MODELVIEW);
    glPushMatrix();
    glLoadIdentity();

    glMatrixMode(GL_PROJECTION);
    glPushMatrix();
    glLoadIdentity();

#if defined(WIN32) || defined(LINUX) || defined(MAC_OS_X)
    glOrtho(0.0f, 1.0f, 0.0f, 1.0f, -10.0f, 10.0f);
#else
    glOrthof(0.0f, 1.0f, 0.0f, 1.0f, -10.0f, 10.0f);
#endif

    static GLfloat vertices[] = {
        0.0f,0.0f,-5.0f,
        1.0f,0.0f,-5.0f,
        0.0f,1.0f,-5.0f,
        1.0f,1.0f,-5.0f,
    };

    // tex coords are flipped upside down instead of an image
    GLfloat texcoords[] = {
        0.0f,           tex_y_coord,
        tex_x_coord,    tex_y_coord,
        0.0f,           0.0f,
        tex_x_coord,    0.0f,
    };

    glColor4f(1.0f, 1.0f, 1.0f, 1.0f);

    glEnableClientState(GL_TEXTURE_COORD_ARRAY);
    glEnableClientState(GL_VERTEX_ARRAY);

    glVertexPointer(3, GL_FLOAT, 0, vertices);
    glTexCoordPointer(2, GL_FLOAT, 0, texcoords);

    glViewport(0, 0, width, height);
    glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);

    glDisableClientState(GL_TEXTURE_COORD_ARRAY);
    glDisableClientState(GL_VERTEX_ARRAY);

    glDisable(GL_TEXTURE_2D);

    glPopMatrix();
    glMatrixMode(GL_MODELVIEW);
    glPopMatrix();

    glBindTexture(GL_TEXTURE_2D, 0);

#if defined(WIN32) || defined(LINUX)
    glPopAttrib();
#endif

    glClear(GL_DEPTH_BUFFER_BIT);
}

void VisageRendering::DisplayFeaturePoints(FaceData* trackingData, int width, int height, VsImage* frame, bool _3D, bool relative, bool drawQuality)
{
    glViewport(0, 0, width, height);

#if defined(WIN32) || defined(LINUX) || defined(MAC_OS_X)
    glPushAttrib(GL_DEPTH_BUFFER_BIT | GL_VIEWPORT_BIT | GL_ENABLE_BIT | GL_FOG_BIT | GL_STENCIL_BUFFER_BIT | GL_TRANSFORM_BIT | GL_TEXTURE_BIT);
#endif

    glDisable(GL_ALPHA_TEST);
    glDisable(GL_DEPTH_TEST);
    glDisable(GL_DITHER);
    glDisable(GL_FOG);
    glDisable(GL_SCISSOR_TEST);
    glDisable(GL_STENCIL_TEST);
    glDisable(GL_BLEND);

    glMatrixMode(GL_MODELVIEW);
    glPushMatrix();
    glLoadIdentity();

    glMatrixMode(GL_PROJECTION);
    glPushMatrix();
    glLoadIdentity();

#if defined(WIN32) || defined(LINUX) || defined(MAC_OS_X)
    glOrtho(0.0f, 1.0f, 0.0f, 1.0f, -10.0f, 10.0f);
#else
    glOrthof(0.0f, 1.0f, 0.0f, 1.0f, -10.0f, 10.0f);
#endif

    if (_3D) {
        SetupCamera(width, height, trackingData->cameraFocus);

        if (relative) {
            const float *r = trackingData->faceRotation;
            const float *t = trackingData->faceTranslation;

            glTranslatef(t[0], t[1], t[2]);
            glRotatef(V_RAD2DEG(r[1] + V_PI), 0.0f, 1.0f, 0.0f);
            glRotatef(V_RAD2DEG(r[0]), 1.0f, 0.0f, 0.0f);
            glRotatef(V_RAD2DEG(r[2]), 0.0f, 0.0f, 1.0f);
        }
    }

    static int chinPoints[] = {
        2,  1
    };

    if (_3D) DrawPoints3D(chinPoints, 1, false, trackingData, frame, relative);
    else DrawPoints2D(chinPoints, 1, false, trackingData, frame, drawQuality);

    static int innerLipPoints[] = {
        2,  2,
        17, 18,
        2,  6,
        17, 14,
        2,  4,
        17, 16,
        2,  8,
        17, 20,
        2,  3,
        17, 19,
        2,  9,
        17, 15,
        2,  5,
        17, 13,
        2,  7,
        17, 17,
    };
    if (_3D) DrawPoints3D(innerLipPoints, 16, false, trackingData, frame, relative);
    else DrawPoints2D(innerLipPoints, 16, false, trackingData, frame, drawQuality);

    static int outerLipPoints[] = {
        8,  1,
        8,  10,
        17, 10,
        8,  5,
        17, 5,
        8,  3,
        17, 7,
        8,  7,
        17, 11,
        8,  2,
        17, 12,
        8,  8,
        17, 8,
        8,  4,
        17, 6,
        8,  6,
        17, 9,
        8,  9,
    };
    if (_3D) DrawPoints3D(outerLipPoints, 18, false, trackingData, frame, relative);
    else DrawPoints2D(outerLipPoints, 18, false, trackingData, frame, drawQuality);

    static int nosePoints[] = {
        9,  5,
        9,  4,
        9,  3,
        9,  15,
        14, 22,
        14, 23,
        14, 24,
        14, 25
    };
    if (_3D) DrawPoints3D(nosePoints, 8, false, trackingData, frame, relative);
    else DrawPoints2D(nosePoints, 8, false, trackingData, frame, drawQuality);

    if (trackingData->eyeClosure[1] > 0.5f)
    {
        //if eye is open, draw the pupil
        glColor4ub(200, 80, 0, 255);
        static int pupilPoints[] = {
            3,  6,
        };
        if (_3D) DrawPoints3D(pupilPoints, 1, false, trackingData, frame, relative);
        else DrawPoints2D(pupilPoints, 1, false, trackingData, frame, drawQuality);
    }

    if (trackingData->eyeClosure[0] > 0.5f)
    {
        glColor4ub(200, 80, 0, 255);
        static int pupilPoints[] = {
            3,  5,
        };
        if (_3D) DrawPoints3D(pupilPoints, 1, false, trackingData, frame, relative);
        else DrawPoints2D(pupilPoints, 1, false, trackingData, frame, drawQuality);
    }

    static int eyesPointsR[] = {
        3,  2,
        3,  4,
        3,  8,
        3,  10,
        3,  12,
        3,  14,
        12, 6,
        12, 8,
        12, 10,
        12, 12,
        16, 2,
        16, 4,
        16, 6,
        16, 8,
        16, 10,
        16, 12,
        16, 14,
        16, 16,
        16, 18,
        16, 20,
        16, 22,
        16, 24,
        16, 26,
        16, 28,
    };
    if (_3D) DrawPoints3D(eyesPointsR, 24, trackingData->eyeClosure[1] <= 0.5f, trackingData, frame, relative);
    else DrawPoints2D(eyesPointsR, 24, trackingData->eyeClosure[1] <= 0.5f, trackingData, frame, drawQuality);

    static int eyesPointsL[] = {
        3,  1,
        3,  3,
        3,  7,
        3,  9,
        3,  11,
        3,  13,
        12, 5,
        12, 7,
        12, 9,
        12, 11,
        16, 1,
        16, 3,
        16, 5,
        16, 7,
        16, 9,
        16, 11,
        16, 13,
        16, 15,
        16, 17,
        16, 19,
        16, 21,
        16, 23,
        16, 25,
        16, 27,
    };
    if (_3D) DrawPoints3D(eyesPointsL, 24, trackingData->eyeClosure[0] <= 0.5f, trackingData, frame, relative);
    else DrawPoints2D(eyesPointsL, 24, trackingData->eyeClosure[0] <= 0.5f, trackingData, frame, drawQuality);

    static int eyebrowPoints[] = {
        4,  1,
        4,  2,
        4,  3,
        4,  4,
        4,  5,
        4,  6,
        14, 1,
        14, 2,
        14, 3,
        14, 4,
        14, 5,
        14, 6,
        14, 7,
        14, 8,
        14, 9,
        14, 10,
        14, 11,
        14, 12
    };
    if (_3D) DrawPoints3D(eyebrowPoints, 18, false, trackingData, frame, relative);
    else DrawPoints2D(eyebrowPoints, 18, false, trackingData, frame, drawQuality);


    // physical contour
    static int contourPointsPhysical[] = {
        15, 1,
        15, 3,
        15, 5,
        15, 7,
        15, 9,
        15, 11,
        15, 13,
        15, 15,
        15, 17,
        15, 16,
        15, 14,
        15, 12,
        15, 10,
        15, 8,
        15, 6,
        15, 4,
        15, 2
    };
    if (_3D) DrawPoints3D(contourPointsPhysical, 17, false, trackingData, frame, relative);
    else DrawPoints2D(contourPointsPhysical, 17, false, trackingData, frame, drawQuality, true);

    static int leftEarPoints[] = {
        10, 1,
        10, 3,
        10, 5,
        10, 7,
        10, 9,
        10, 11,
        10, 13,
        10, 15,
        10, 17,
        10, 19,
        10, 21,
        10, 23,
    };
    if (_3D) DrawPoints3D(leftEarPoints, 12, false, trackingData, frame, relative);
    else DrawPoints2D(leftEarPoints, 12, false, trackingData, frame, drawQuality);

    static int rightEarPoints[] = {
        10, 2,
        10, 4,
        10, 6,
        10, 8,
        10, 10,
        10, 12,
        10, 14,
        10, 16,
        10, 18,
        10, 20,
        10, 22,
        10, 24,
    };

    if (_3D) DrawPoints3D(rightEarPoints, 12, false, trackingData, frame, relative);
    else DrawPoints2D(rightEarPoints, 12, false, trackingData, frame, drawQuality);

    glMatrixMode(GL_PROJECTION);
    glPopMatrix();
    glMatrixMode(GL_MODELVIEW);
    glPopMatrix();
#if defined(WIN32) || defined(LINUX) || defined(MAC_OS_X)
    glPopAttrib();
#endif
}

void VisageRendering::DisplaySplines(FaceData* trackingData, int width, int height)
{
    glViewport(0, 0, width, height);

#if defined(WIN32) || defined(LINUX) || defined(MAC_OS_X)
    glPushAttrib(GL_DEPTH_BUFFER_BIT | GL_VIEWPORT_BIT | GL_ENABLE_BIT | GL_FOG_BIT | GL_STENCIL_BUFFER_BIT | GL_TRANSFORM_BIT | GL_TEXTURE_BIT);
#endif

    glDisable(GL_ALPHA_TEST);
    glDisable(GL_DEPTH_TEST);
    glDisable(GL_DITHER);
    glDisable(GL_FOG);
    glDisable(GL_SCISSOR_TEST);
    glDisable(GL_STENCIL_TEST);

    glMatrixMode(GL_MODELVIEW);
    glPushMatrix();
    glLoadIdentity();

    glMatrixMode(GL_PROJECTION);
    glPushMatrix();
    glLoadIdentity();

#if defined(WIN32) || defined(LINUX) || defined(MAC_OS_X)
    glOrtho(0.0f, 1.0f, 0.0f, 1.0f, -10.0f, 10.0f);
#else
    glOrthof(0.0f, 1.0f, 0.0f, 1.0f, -10.0f, 10.0f);
#endif

    glPointSize(3);
    glLineWidth(2);

    static int outerUpperLipPoints[] = {
        8, 4,
        17, 6,
        8, 6,
        17, 9,
        8, 9,
        8, 1,
        8, 10,
        17, 10,
        8, 5,
        17, 5,
        8, 3,
    };
    DrawSpline2D(outerUpperLipPoints, 11, trackingData);

    static int outerLowerLipPoints[] = {
        8, 4,
        17, 8,
        8, 8,
        17, 12,
        8, 2,
        17, 11,
        8, 7,
        17, 7,
        8, 3,
    };
    DrawSpline2D(outerLowerLipPoints, 9, trackingData);

    static int innerUpperLipPoints[] = {
        2, 5,
        17, 13,
        2, 7,
        17, 17,
        2, 2,
        17, 18,
        2, 6,
        17, 14,
        2, 4,
    };
    DrawSpline2D(innerUpperLipPoints, 9, trackingData);

    static int innerLowerLipPoints[] = {
        2, 5,
        17, 15,
        2, 9,
        17, 19,
        2, 3,
        17, 20,
        2, 8,
        17, 16,
        2, 4,
    };
    DrawSpline2D(innerLowerLipPoints, 9, trackingData);

    static int noseLinePoints[] = {
        9,  5,
        9,  3,
        9,  4
    };
    DrawSpline2D(noseLinePoints, 3, trackingData);

    static int noseLinePoints2[] = {
        9,  3,
        14, 22,
        14, 23,
        14, 24,
        14, 25
    };
    DrawSpline2D(noseLinePoints2, 5, trackingData);

    static int outerUpperEyePointsR[] = {
        16,  28,
        16,  6,
        3,   14,
        16,  2,
        16,  26,
    };
    DrawSpline2D(outerUpperEyePointsR, 5, trackingData);

    static int outerLowerEyePointsR[] = {
        16,  26,
        16,  4,
        3,   10,
        16,  8,
        16,  28,
    };
    DrawSpline2D(outerLowerEyePointsR, 5, trackingData);

    static int innerUpperEyePointsR[] = {
        3,  12,
        16, 22,
        12, 10,
        16, 18,
        3,  2,
        16, 14,
        12, 6,
        16, 10,
        3,  8
    };
    DrawSpline2D(innerUpperEyePointsR, 9, trackingData);

    static int innerLowerEyePointsR[] = {
        3,  8,
        16, 12,
        12, 8,
        16, 16,
        3,  4,
        16, 20,
        12, 12,
        16, 24,
        3,  12,
    };
    DrawSpline2D(innerLowerEyePointsR, 9, trackingData);

    static int outerUpperEyePointsL[] = {
        16,  27,
        16,  5,
        3,   13,
        16,  1,
        16,  25,
    };
    DrawSpline2D(outerUpperEyePointsL, 5, trackingData);

    static int outerLowerEyePointsL[] = {
        16,  25,
        16,  3,
        3,   9,
        16,  7,
        16,  27,
    };
    DrawSpline2D(outerLowerEyePointsL, 5, trackingData);

    static int innerUpperEyePointsL[] = {
        3,   11,
        16,  21,
        12,  9,
        16,  17,
        3,   1,
        16,  13,
        12,  5,
        16,  9,
        3,   7,
    };
    DrawSpline2D(innerUpperEyePointsL, 9, trackingData);

    static int innerLowerEyePointsL[] = {
        3,   7,
        16,  11,
        12,  7,
        16,  15,
        3,   3,
        16,  19,
        12,  11,
        16,  23,
        3,   11
    };
    DrawSpline2D(innerLowerEyePointsL, 9, trackingData);

    static int eyebrowUpperPointsR[] = {
        14, 6,
        4,  2,
        14, 2,
        4,  4,
        14, 4,
        4,  6
    };
    DrawSpline2D(eyebrowUpperPointsR, 6, trackingData);

    static int eyebrowLowerPointsR[] = {
        4,  6,
        14, 12,
        14, 10,
        14, 8,
        14, 6
    };
    DrawSpline2D(eyebrowLowerPointsR, 5, trackingData);

    static int eyebrowUpperPointsL[] = {
        14, 5,
        4,  1,
        14, 1,
        4,  3,
        14, 3,
        4,  5
    };
    DrawSpline2D(eyebrowUpperPointsL, 6, trackingData);

    static int eyebrowLowerPointsL[] = {
        4,  5,
        14, 11,
        14, 9,
        14, 7,
        14, 5
    };
    DrawSpline2D(eyebrowLowerPointsL, 5, trackingData);

    static int contourLinesPointsLPhysical[] = {
        15, 1,
        15, 3,
        15, 5,
        15, 7,
        15, 9,
        15, 11,
        15, 13,
        15, 15,
        15, 17,
        15, 16,
        15, 14,
        15, 12,
        15, 10,
        15, 8,
        15, 6,
        15, 4,
        15, 2
    };
    DrawSpline2D(contourLinesPointsLPhysical, 17, trackingData, true);

    static int leftEarPoints[] = {
        10, 11,
        10, 1,
        10, 13,
        10, 3,
        10, 15,
        10, 5,
        10, 19,
    };

    DrawSpline2D(leftEarPoints, 7, trackingData);

    static int rightEarPoints[] = {
        10, 12,
        10, 2,
        10, 14,
        10, 4,
        10, 16,
        10, 6,
        10, 20
    };

    DrawSpline2D(rightEarPoints, 7, trackingData);

    glDisable(GL_BLEND);

    glMatrixMode(GL_PROJECTION);
    glPopMatrix();
    glMatrixMode(GL_MODELVIEW);
    glPopMatrix();

#if defined(WIN32) || defined(LINUX) || defined(MAC_OS_X)
    glPopAttrib();
#endif
}

void VisageRendering::DisplayGaze(FaceData* trackingData, int width, int height)
{
    glViewport(0, 0, width, height);

    glMatrixMode(GL_MODELVIEW);
    glPushMatrix();
    glLoadIdentity();

    glMatrixMode(GL_PROJECTION);
    glPushMatrix();
    glLoadIdentity();

    SetupCamera(width, height, trackingData->cameraFocus);

    glShadeModel(GL_FLAT);

    static float vertices[] = {
        0.0f, 0.0f, 0.0f,
        0.0f, 0.0f, 0.04f
    };

    float tr[6] = { 0, 0, 0, 0, 0, 0 };

    FDP *fdp = trackingData->featurePoints3D;

    if (fdp == NULL)
        return;

    const FeaturePoint &leye = fdp->getFP(3, 5);
    const FeaturePoint &reye = fdp->getFP(3, 6);

    if (leye.defined && reye.defined)
    {
        tr[0] = leye.pos[0];
        tr[1] = leye.pos[1];
        tr[2] = leye.pos[2];
        tr[3] = reye.pos[0];
        tr[4] = reye.pos[1];
        tr[5] = reye.pos[2];
    }

    float h_rot = V_RAD2DEG(trackingData->gazeDirectionGlobal[1] + V_PI);
    float v_rot = V_RAD2DEG(trackingData->gazeDirectionGlobal[0]);
    float roll = V_RAD2DEG(trackingData->gazeDirectionGlobal[2]);

    glEnableClientState(GL_VERTEX_ARRAY);

    glMatrixMode(GL_MODELVIEW);
    glPushMatrix();

    glTranslatef(tr[0], tr[1], tr[2]);
    glRotatef(h_rot, 0.0f, 1.0f, 0.0f);
    glRotatef(v_rot, 1.0f, 0.0f, 0.0f);
    glRotatef(roll, 0.0f, 0.0f, 1.0f);

    glLineWidth(2);

    glColor4ub(240, 96, 0, 255);

    if (trackingData->eyeClosure[0] > 0.5f)
    {
        glVertexPointer(3, GL_FLOAT, 0, vertices);
        glDrawArrays(GL_LINES, 0, 2);
    }

    glPopMatrix();

    glPushMatrix();

    glTranslatef(tr[3], tr[4], tr[5]);
    glRotatef(h_rot, 0.0f, 1.0f, 0.0f);
    glRotatef(v_rot, 1.0f, 0.0f, 0.0f);
    glRotatef(roll, 0.0f, 0.0f, 1.0f);

    if (trackingData->eyeClosure[1] > 0.5f)
    {
        glVertexPointer(3, GL_FLOAT, 0, vertices);
        glDrawArrays(GL_LINES, 0, 2);
    }

    glDisableClientState(GL_VERTEX_ARRAY);

    glPopMatrix();

    glMatrixMode(GL_PROJECTION);
    glPopMatrix();

    glMatrixMode(GL_MODELVIEW);
    glPopMatrix();
}

void VisageRendering::DisplayIrises(FaceData* trackingData, int width, int height, VsImage* frame)
{

    glViewport(0, 0, width, height);

#if defined(WIN32) || defined(LINUX) || defined(MAC_OS_X)
    glPushAttrib(GL_DEPTH_BUFFER_BIT | GL_VIEWPORT_BIT | GL_ENABLE_BIT | GL_FOG_BIT | GL_STENCIL_BUFFER_BIT | GL_TRANSFORM_BIT | GL_TEXTURE_BIT);
#endif

    glDisable(GL_ALPHA_TEST);
    glDisable(GL_DEPTH_TEST);
    glDisable(GL_DITHER);
    glDisable(GL_FOG);
    glDisable(GL_SCISSOR_TEST);
    glDisable(GL_STENCIL_TEST);

    glMatrixMode(GL_MODELVIEW);
    glPushMatrix();
    glLoadIdentity();

    glMatrixMode(GL_PROJECTION);
    glPushMatrix();
    glLoadIdentity();

#if defined(WIN32) || defined(LINUX) || defined(MAC_OS_X)
    glOrtho(0.0f, 1.0f, 0.0f, 1.0f, -10.0f, 10.0f);
#else
    glOrthof(0.0f, 1.0f, 0.0f, 1.0f, -10.0f, 10.0f);
#endif

    glPointSize(3);
    glLineWidth(2);

    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
    glEnable(GL_BLEND);

    FDP *fdp = trackingData->featurePoints3D;

    if (fdp == NULL)
        return;

    const FeaturePoint &leye = fdp->getFP(3, 5);
    const FeaturePoint &reye = fdp->getFP(3, 6);

    if (trackingData->irisRadius[0] > 0)
    {
        FeaturePoint fp = trackingData->featurePoints2D->getFP(3, 5);
        float rx = trackingData->irisRadius[0] / float(frame->width);
        float ry = trackingData->irisRadius[0] / float(frame->height);
        glColor4ub(255, 255, 255, 20);
        DrawElipse(fp.pos[0], fp.pos[1], rx, ry);
        glColor4ub(255, 255, 255, 255);
        DrawElipse(fp.pos[0], fp.pos[1], rx, ry, false);
    }

    if (trackingData->irisRadius[1] > 0)
    {
        FeaturePoint fp = trackingData->featurePoints2D->getFP(3, 6);
        float rx = trackingData->irisRadius[1] / float(frame->width);
        float ry = trackingData->irisRadius[1] / float(frame->height);
        glColor4ub(255, 255, 255, 20);
        DrawElipse(fp.pos[0], fp.pos[1], rx, ry);
        glColor4ub(255, 255, 255, 255);
        DrawElipse(fp.pos[0], fp.pos[1], rx, ry, false);
    }

    glDisable(GL_BLEND);

    glMatrixMode(GL_PROJECTION);
    glPopMatrix();
    glMatrixMode(GL_MODELVIEW);
    glPopMatrix();

#if defined(WIN32) || defined(LINUX) || defined(MAC_OS_X)
    glPopAttrib();
#endif

}

void VisageRendering::DisplayModelAxes(FaceData* trackingData, int width, int height)
{
    glViewport(0, 0, width, height);

    glMatrixMode(GL_MODELVIEW);
    glPushMatrix();
    glLoadIdentity();

    glMatrixMode(GL_PROJECTION);
    glPushMatrix();
    glLoadIdentity();

    SetupCamera(width, height, trackingData->cameraFocus);

    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
    glEnable(GL_BLEND);

    glShadeModel(GL_FLAT);

    //rotate and translate into the current coordinate system of the head
    const float *r = trackingData->faceRotation;
    //const float *t = trackingData->faceTranslation;
    FDP *fdp = trackingData->featurePoints3D;

    if (fdp == NULL)
        return;

    const FeaturePoint &fp1 = fdp->getFP(4, 1);
    const FeaturePoint &fp2 = fdp->getFP(4, 2);

    glTranslatef((fp1.pos[0] + fp2.pos[0]) / 2.0f, (fp1.pos[1] + fp2.pos[1]) / 2.0f, (fp1.pos[2] + fp2.pos[2]) / 2.0f);
    glRotatef(V_RAD2DEG(r[1] + V_PI), 0.0f, 1.0f, 0.0f);
    glRotatef(V_RAD2DEG(r[0]), 1.0f, 0.0f, 0.0f);
    glRotatef(V_RAD2DEG(r[2]), 0.0f, 0.0f, 1.0f);

    static const float coordVertices[] = {
        0.0f,   0.0f,   0.0f,
        0.07f,  0.0f,   0.0f,
        0.0f,   0.0f,   0.0f,
        0.0f,   0.07f,  0.0f,
        0.0f,   0.0f,   0.0f,
        0.0f,   0.0f,   0.07f,
    };

    static const float coordColors[] = {
        1.0f, 0.0f, 0.0f, 0.25f,
        1.0f, 0.0f, 0.0f, 0.25f,
        0.0f, 0.0f, 1.0f, 0.25f,
        0.0f, 0.0f, 1.0f, 0.25f,
        0.0f, 1.0f, 0.0f, 0.25f,
        0.0f, 1.0f, 0.0f, 0.25f,
    };

    glLineWidth(2);

    glEnableClientState(GL_VERTEX_ARRAY);
    glEnableClientState(GL_COLOR_ARRAY);
    glVertexPointer(3, GL_FLOAT, 0, coordVertices);
    glColorPointer(4, GL_FLOAT, 0, coordColors);
    glDrawArrays(GL_LINES, 0, 6);
    glDisableClientState(GL_VERTEX_ARRAY);
    glDisableClientState(GL_COLOR_ARRAY);

    glDisable(GL_BLEND);

    glMatrixMode(GL_PROJECTION);
    glPopMatrix();

    glMatrixMode(GL_MODELVIEW);
    glPopMatrix();

#if defined(WIN32) || defined(LINUX) || defined(MAC_OS_X)
    glPopAttrib();
#endif
}

void VisageRendering::DisplayWireFrame(FaceData* trackingData, int width, int height, float alpha)
{
    //set image specs
    glViewport(0, 0, width, height);

    glMatrixMode(GL_MODELVIEW);
    glPushMatrix();
    glLoadIdentity();

    glMatrixMode(GL_PROJECTION);
    glPushMatrix();
    glLoadIdentity();

    SetupCamera(width, height, trackingData->cameraFocus);

    glEnableClientState(GL_VERTEX_ARRAY);
    glShadeModel(GL_FLAT);

    //transparency
    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
    glEnable(GL_BLEND);

    //set the color for the wireframe
    glColor4f(0.0f, 1.0f, 0.0f, alpha);
    //vertex list
    glVertexPointer(3, GL_FLOAT, 0, trackingData->faceModelVertices);

    glLineWidth(1);

    const float *r = trackingData->faceRotation;
    const float *t = trackingData->faceTranslation;

    glTranslatef(t[0], t[1], t[2]);
    glRotatef(V_RAD2DEG(r[1] + V_PI), 0.0f, 1.0f, 0.0f);
    glRotatef(V_RAD2DEG(r[0]), 1.0f, 0.0f, 0.0f);
    glRotatef(V_RAD2DEG(r[2]), 0.0f, 0.0f, 1.0f);

    //draw the wireframe
    //initialize indexes for drawing wireframe (once per model)
    if (numberOfVertices != trackingData->faceModelVertexCount)
    {
        std::set<std::pair<GLushort, GLushort> > indexList;

        for (int i = 0; i < trackingData->faceModelTriangleCount; i++) {
            GLushort triangle[] = {
                static_cast<GLushort>(trackingData->faceModelTriangles[3 * i + 0]),
                static_cast<GLushort>(trackingData->faceModelTriangles[3 * i + 1]),
                static_cast<GLushort>(trackingData->faceModelTriangles[3 * i + 2]),
            };
            if (triangle[0] > triangle[1])
                std::swap(triangle[0], triangle[1]);
            if (triangle[0] > triangle[2])
                std::swap(triangle[0], triangle[2]);
            if (triangle[1] > triangle[2])
                std::swap(triangle[1], triangle[2]);

            indexList.insert(std::make_pair(triangle[0], triangle[1]));
            indexList.insert(std::make_pair(triangle[1], triangle[2]));
            indexList.insert(std::make_pair(triangle[0], triangle[2]));
        }

        output.clear();
        for (std::set<std::pair<GLushort, GLushort> >::iterator it = indexList.begin(); it != indexList.end(); ++it)
        {
            output.push_back((*it).first);
            output.push_back((*it).second);
        }
    }

    numberOfVertices = trackingData->faceModelVertexCount;

    glDrawElements(GL_LINES, (int)output.size(), GL_UNSIGNED_SHORT, &output[0]);

    glDisable(GL_BLEND);
    glDisableClientState(GL_VERTEX_ARRAY);

    glMatrixMode(GL_PROJECTION);
    glPopMatrix();

    glMatrixMode(GL_MODELVIEW);
    glPopMatrix();
}

void VisageRendering::CalcSpline(std::vector <float>& inputPoints, int ratio, std::vector <float>& outputPoints) {

    int nPoints, nPointsToDraw, nLines;

    nPoints = (int)inputPoints.size() / 2 + 2;
    nPointsToDraw = (int)inputPoints.size() / 2 + ((int)inputPoints.size() / 2 - 1) * ratio;
    nLines = nPoints - 1 - 2;

    inputPoints.insert(inputPoints.begin(), inputPoints[1] + (inputPoints[1] - inputPoints[3]));
    inputPoints.insert(inputPoints.begin(), inputPoints[1] + (inputPoints[1] - inputPoints[3]));
    inputPoints.insert(inputPoints.end(), inputPoints[inputPoints.size() / 2 - 2] + (inputPoints[inputPoints.size() / 2 - 2] - inputPoints[inputPoints.size() / 2 - 4]));
    inputPoints.insert(inputPoints.end(), inputPoints[inputPoints.size() / 2 - 1] + (inputPoints[inputPoints.size() / 2 - 1] - inputPoints[inputPoints.size() / 2 - 3]));

    Vec2D p0(0, 0), p1(0, 0), p2(0, 0), p3(0, 0);
    CubicPoly px, py;

    outputPoints.resize(2 * nPointsToDraw);

    for (int i = 0; i < nPoints - 2; i++) {
        outputPoints[i * 2 * (ratio + 1)] = inputPoints[2 * i + 2];
        outputPoints[i * 2 * (ratio + 1) + 1] = inputPoints[2 * i + 1 + 2];
    }

    for (int i = 0; i < 2 * nLines; i = i + 2) {
        p0.x = inputPoints[i];
        p0.y = inputPoints[i + 1];
        p1.x = inputPoints[i + 2];
        p1.y = inputPoints[i + 3];
        p2.x = inputPoints[i + 4];
        p2.y = inputPoints[i + 5];
        p3.x = inputPoints[i + 6];
        p3.y = inputPoints[i + 7];

        InitCentripetalCR(p0, p1, p2, p3, px, py);

        for (int j = 1; j <= ratio; j++) {
            outputPoints[i*(ratio + 1) + 2 * j] = (px.eval(1.00f / (ratio + 1)*(j)));
            outputPoints[i*(ratio + 1) + 2 * j + 1] = (py.eval(1.00f / (ratio + 1)*(j)));
        }

    }

    inputPoints.erase(inputPoints.begin(), inputPoints.begin() + 2);
    inputPoints.erase(inputPoints.end() - 2, inputPoints.end());
}

void VisageRendering::DisplayTrackingQualityBar(FaceData* trackingData)
{
    glMatrixMode(GL_MODELVIEW);
    glPushMatrix();
    glLoadIdentity();

    glMatrixMode(GL_PROJECTION);
    glPushMatrix();
    glLoadIdentity();

#if defined(WIN32) || defined(LINUX) || defined(MAC_OS_X)
    glOrtho(0.0f, 1.0f, 0.0f, 1.0f, -10.0f, 10.0f);
#else
    glOrthof(0.0f, 1.0f, 0.0f, 1.0f, -10.0f, 10.0f);
#endif

    int points_to_draw = 2;
    float vertices[6];
    char tmpbuff[200];
    glLineWidth(10);

    vertices[0] = 0.1f;
    vertices[1] = 0.9f;
    vertices[2] = 0.0f;
    vertices[3] = 0.25f;
    vertices[4] = 0.9f;
    vertices[5] = 0.0f;
    glColor4f(0.5, 0.5, 0.5, 1);
    glEnableClientState(GL_VERTEX_ARRAY);
    glVertexPointer(3, GL_FLOAT, 0, vertices);
    glDrawArrays(GL_LINES, 0, points_to_draw);
    glDisableClientState(GL_VERTEX_ARRAY);

    vertices[0] = 0.1f;
    vertices[1] = 0.9f;
    vertices[2] = 0.0f;
    vertices[3] = 0.1 + trackingData->trackingQuality * 0.15f;
    vertices[4] = 0.9f;
    vertices[5] = 0.0f;
    glColor4f((1 - trackingData->trackingQuality), trackingData->trackingQuality, 0, 1);
    glEnableClientState(GL_VERTEX_ARRAY);
    glVertexPointer(3, GL_FLOAT, 0, vertices);
    glDrawArrays(GL_LINES, 0, points_to_draw);
    glDisableClientState(GL_VERTEX_ARRAY);
    glLineWidth(1);

    glMatrixMode(GL_PROJECTION);
    glPopMatrix();

    glMatrixMode(GL_MODELVIEW);
    glPopMatrix();
}

void VisageRendering::Reset()
{
    video_texture_inited = false;
    logo_tex_id = -1;
    img_tex_id = -1;
    font_tex_id = -1;
}

void VisageRendering::DisplayImage(VsImage *image, float effectValue, bool imageChanged)
{
    if (img_tex_id == -1)
        GenerateImgTex(img_tex_id);

    glBindTexture(GL_TEXTURE_2D, img_tex_id);

    if (imageChanged)
    {
        //Creating texture
#ifdef IOS
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, image->width, image->height, 0, GL_BGRA, GL_UNSIGNED_BYTE, image->imageData);
#elif ANDROID
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, image->width, image->height, 0, GL_RGBA, GL_UNSIGNED_BYTE, image->imageData);
#else
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, image->width, image->height, 0, GL_BGRA_EXT, GL_UNSIGNED_BYTE, image->imageData);
#endif
    }

#if defined(WIN32) || defined(LINUX)
    glPushAttrib(GL_DEPTH_BUFFER_BIT | GL_VIEWPORT_BIT | GL_ENABLE_BIT | GL_FOG_BIT | GL_STENCIL_BUFFER_BIT | GL_TRANSFORM_BIT | GL_TEXTURE_BIT);
#endif

    glDisable(GL_ALPHA_TEST);
    glDisable(GL_DEPTH_TEST);
    glDisable(GL_DITHER);
    glDisable(GL_FOG);
    glDisable(GL_SCISSOR_TEST);
    glDisable(GL_STENCIL_TEST);
    glDisable(GL_LIGHTING);
    glDisable(GL_LIGHT0);

    //transparency
    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
    glEnable(GL_BLEND);

    //use texture
    glEnable(GL_TEXTURE_2D);

    glMatrixMode(GL_MODELVIEW);
    glPushMatrix();
    glLoadIdentity();

    glMatrixMode(GL_PROJECTION);
    glPushMatrix();
    glLoadIdentity();

#if defined(WIN32) || defined(LINUX) || defined(MAC_OS_X)
    glOrtho(0.0f, 1.0f, 0.0f, 1.0f, -10.0f, 10.0f);
#else
    glOrthof(0.0f, 1.0f, 0.0f, 1.0f, -10.0f, 10.0f);
#endif

    //image aspect
    float imageAspect = image->width / (float)image->height;
    //viewport aspect
    float viewportAspect = winWidth / (float)winHeight;
    //set image position to center, maintain image aspect relative
    float y1 = 0.15f;
    float x1 = (1 - (imageAspect*(1 - 2 * y1) / viewportAspect)) / 2.0f;
    float x2 = 1.0f - x1;
    float y2 = 1.0f - y1;

    GLfloat vertices[] = {
        x1,y1,-5.0f,
        x2,y1,-5.0f,
        x1,y2,-5.0f,
        x2,y2,-5.0f,
    };

    //tex coords are flipped upside down instead of an image
    GLfloat texcoords[] = {
        0.0f,1.0f,
        1.0f,1.0f,
        0.0f,0.0f,
        1.0f,0.0f,
    };

    glColor4f(1.0f, 1.0f, 1.0f, effectValue);

    glEnableClientState(GL_TEXTURE_COORD_ARRAY);
    glEnableClientState(GL_VERTEX_ARRAY);

    glVertexPointer(3, GL_FLOAT, 0, vertices);
    glTexCoordPointer(2, GL_FLOAT, 0, texcoords);

    glViewport(0, 0, winWidth, winHeight);

    //drawing vertices and texcoords
    glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);

    glDisableClientState(GL_TEXTURE_COORD_ARRAY);
    glDisableClientState(GL_VERTEX_ARRAY);

    glDisable(GL_TEXTURE_2D);

    glDisable(GL_BLEND);

    glPopMatrix();

    glMatrixMode(GL_MODELVIEW);
    glPopMatrix();

    //disable texture
    glBindTexture(GL_TEXTURE_2D, 0);

#if defined(WIN32) || defined(LINUX)
    glPopAttrib();
#endif

    //glClear(GL_DEPTH_BUFFER_BIT);
}

void VisageRendering::SetFontTexture(const VsImage *font) {
    font_tex = font;
}

static void InitFontTexture(const VsImage *font_tex)
{
    font_width = font_tex->width / 16;
    font_height = font_tex->height / 16;
    int tex_width = font_tex->width;
    int tex_height = font_tex->height;

    // Generate font UV coordinates
    for (int i = 0; i < 256; i++) {
        float u1 = (((i)* font_width) % tex_width) / static_cast<float>(tex_width);
        float u2 = (((i)* font_width) % tex_width + font_width) / static_cast<float>(tex_width);
        float v1 = (((i)* font_width) / tex_width * font_height) / static_cast<float>(tex_height);
        float v2 = (((i)* font_width) / tex_width * font_height + font_height) / static_cast<float>(tex_height);

        m_fontUV[i][0] = u1;
        m_fontUV[i][1] = v2;

        m_fontUV[i][2] = u2;
        m_fontUV[i][3] = v2;

        m_fontUV[i][4] = u2;
        m_fontUV[i][5] = v1;

        m_fontUV[i][6] = u1;
        m_fontUV[i][7] = v1;

        m_fontUV[i][8] = u1;
        m_fontUV[i][9] = v2;

        m_fontUV[i][10] = u2;
        m_fontUV[i][11] = v1;

    }

    // Load The Texture Into OpenGL
    glGenTextures(1, &font_tex_id);                         // Get An Open ID
    glBindTexture(GL_TEXTURE_2D, font_tex_id);              // Bind The Texture
    glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, tex_width, tex_height, 0, GL_RGBA, GL_UNSIGNED_BYTE, font_tex->imageData);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
}

static void DrawString(const char* buffer, float xc, float yc, int width, int height, float scale = 1.0f, bool centerText = false)
{
    if (font_tex == NULL)
        return;

    if (font_tex_id == -1) {
        InitFontTexture(font_tex);
    }

    float f_w = font_width / (float)width * scale;
    float f_h = font_height / (float)height * scale;

    int len = strlen(buffer);

    float *vertices = new float[6 * 3 * len];
    float *tex_coords = new float[6 * 2 * len];

    float y = yc;
    float x = xc;
    if (centerText)
        x = xc - len*f_w / 2;

    // generate quad array
    for (int i = 0; i < len; i++) {
        int n = 0;
        vertices[18 * i + n++] = x + i * f_w;
        vertices[18 * i + n++] = y + 0.0f;
        vertices[18 * i + n++] = 0.0f;

        vertices[18 * i + n++] = x + (i + 1) * f_w;
        vertices[18 * i + n++] = y + 0.0f;
        vertices[18 * i + n++] = 0.0f;

        vertices[18 * i + n++] = x + (i + 1) * f_w;
        vertices[18 * i + n++] = y + f_h;
        vertices[18 * i + n++] = 0.0f;

        vertices[18 * i + n++] = x + i * f_w;
        vertices[18 * i + n++] = y + f_h;
        vertices[18 * i + n++] = 0.0f;

        vertices[18 * i + n++] = x + i * f_w;
        vertices[18 * i + n++] = y + 0.0f;
        vertices[18 * i + n++] = 0.0f;

        vertices[18 * i + n++] = x + (i + 1) * f_w;
        vertices[18 * i + n++] = y + f_h;
        vertices[18 * i + n++] = 0.0f;
    }

    // copy texture coordinate for each letter
    for (int i = 0; i < len; i++) {
        memcpy(&tex_coords[12 * i], m_fontUV[buffer[i]], 12 * sizeof(float));
    }

    //  glPushClientAttrib(GL_CLIENT_ALL_ATTRIB_BITS);

    //glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
    //glEnable(GL_BLEND);

    glEnable(GL_TEXTURE_2D);

    glBindTexture(GL_TEXTURE_2D, font_tex_id);

    //  glEnableClientState(GL_VERTEX_ARRAY);
    glEnableClientState(GL_TEXTURE_COORD_ARRAY);

    glVertexPointer(3, GL_FLOAT, 0, vertices);
    glTexCoordPointer(2, GL_FLOAT, 0, tex_coords);
    glDrawArrays(GL_TRIANGLES, 0, 6 * len);

    //  glDisableClientState(GL_VERTEX_ARRAY);
    glDisableClientState(GL_TEXTURE_COORD_ARRAY);

    glDisable(GL_TEXTURE_2D);

    //glDisable(GL_BLEND);

    //  glPopClientAttrib();

    delete[] vertices;
    delete[] tex_coords;
}

void VisageRendering::DisplayText(const char* displayText, float effectValue, float scale)
{
    float ev = effectValue * 255;

    glColor4ub(255, 0, 0, ev);

    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
    glEnable(GL_BLEND);

    glEnableClientState(GL_VERTEX_ARRAY);
    DrawString(displayText, 0, -0.7f, winWidth, winHeight, scale, true);
    glDisableClientState(GL_VERTEX_ARRAY);
    glDisable(GL_BLEND);

}

void VisageRendering::DisplayActionUnits(FaceData* trackingData, int width, int height)
{
    glMatrixMode(GL_MODELVIEW);
    glPushMatrix();
    glLoadIdentity();

    glMatrixMode(GL_PROJECTION);
    glPushMatrix();
    glLoadIdentity();

#if defined(WIN32) || defined(LINUX) || defined(MAC_OS_X)
    glOrtho(0.0f, 1.0f, 0.0f, 1.0f, -10.0f, 10.0f);
#else
    glOrthof(0.0f, 1.0f, 0.0f, 1.0f, -10.0f, 10.0f);
#endif

    float auVis[] = {
        0.5f, 0.0f, 0.0f,
        1.0f, 0.0f, 0.0f,
        1.0f, 0.1f, 0.0f,
        0.5f, 0.1f, 0.0f
    };

    glColor4ub(0, 255, 0, 255);

    glEnableClientState(GL_VERTEX_ARRAY);
    glVertexPointer(3, GL_FLOAT, 0, auVis);

    char tmpbuff[200];
    /* default AUs
    0 au_nose_wrinkler
    1 au_jaw_z_push
    2 au_jaw_x_push
    3 au_jaw_drop
    4 au_lower_lip_drop
    5 au_upper_lip_raiser
    6 au_lip_stretcher_left
    7 au_lip_corner_depressor
    8 au_lip_presser
    9 au_left_outer_brow_raiser
    10 au_left_inner_brow_raiser
    11 au_left_brow_lowerer
    12 au_leye_closed
    13 au_lid_tightener
    14 au_upper_lid_raiser
    15 au_rotate_eyes_left
    16 au_rotate_eyes_down
    17 au_lower_lip_x_push
    18 au_lip_stretcher_right
    19  au_right_outer_brow_raiser
    20 au_right_inner_brow_raiser
    21 au_right_brow_lowerer
    22 au_reye_closed
    */
    int cnt = 0;

    //int au_order[] = {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23};
    int au_order[] = { 3, 4, 9, 10, 19, 20, 12, 22 };

    int actionUnitCount = sizeof(au_order) / sizeof(au_order[0]);

    const float vis_scale = 16.0f / height;

    glLineWidth(1);

    for (int i = 0; i<actionUnitCount; i++) {
        glVertexPointer(3, GL_FLOAT, 0, auVis);
        auVis[3] = auVis[6] = 0.5f + trackingData->actionUnits[au_order[i]] * 0.5f;
        auVis[1] = auVis[4] = 1.0f - (2 * i + 2)*vis_scale;
        auVis[7] = auVis[10] = 1.0f - (2 * i + 1)*vis_scale;

        glColor4ub(0, 255, 0, 128);
        //  glDrawArrays(GL_POLYGON, 0, 4);

        glDrawArrays(GL_TRIANGLE_FAN, 0, 4);

        glColor4ub(0, 0, 0, 128);
        glDrawArrays(GL_LINE_LOOP, 0, 4);

        sprintf(tmpbuff, "%+6.2f %s", trackingData->actionUnits[au_order[i]], trackingData->actionUnitsNames[au_order[i]]);

        glColor4ub(0, 0, 0, 255);
        DrawString(tmpbuff, 0.5f, auVis[1] + 0.000f, width, height, 1.0f);

        cnt++;
    }

    glDisableClientState(GL_VERTEX_ARRAY);

    glMatrixMode(GL_PROJECTION);
    glPopMatrix();

    glMatrixMode(GL_MODELVIEW);
    glPopMatrix();
}

void VisageRendering::DisplayResults(FaceData* trackingData, int trackStat, int width, int height, VsImage* frame, int drawingOptions)
{
    winWidth = width;
    winHeight = height;

    frameWidth = frame->width;
    frameHeight = frame->height;

    glViewport(0, 0, width, height);

    if (frame != NULL && (drawingOptions & DISPLAY_FRAME))
    {
        ClearGL();
        DisplayFrame(frame, width, height);
    }

    if (trackStat == TRACK_STAT_OK)
    {
        if (drawingOptions & DISPLAY_SPLINES)
        {
            DisplaySplines(trackingData, width, height);
        }

        if (drawingOptions & DISPLAY_FEATURE_POINTS)
        {
            bool drawQuality = drawingOptions & DISPLAY_POINT_QUALITY;
            DisplayFeaturePoints(trackingData, width, height, frame, false, false, drawQuality); // draw 2D feature points
                                                                                                 //DisplayFeaturePoints(trackingData, width, height, frame, true); // draw 3D feature points
                                                                                                 //DisplayFeaturePoints(trackingData, width, height, frame, true, true); // draw relative 3D feature points
        }

        if (drawingOptions & DISPLAY_GAZE)
        {
            DisplayGaze(trackingData, width, height);
        }

        if (drawingOptions & DISPLAY_IRIS)
        {
            DisplayIrises(trackingData, width, height, frame);
        }

        if (drawingOptions & DISPLAY_AXES)
        {
            DisplayModelAxes(trackingData, width, height);
        }

        if (drawingOptions & DISPLAY_WIRE_FRAME)
        {
            DisplayWireFrame(trackingData, width, height);
        }

        if (drawingOptions & DISPLAY_ACTION_UNITS)
        {
            DisplayActionUnits(trackingData, width, height);
        }

        if (drawingOptions & DISPLAY_TRACKING_QUALITY)
        {
            DisplayTrackingQualityBar(trackingData);
        }
    }
}

}
