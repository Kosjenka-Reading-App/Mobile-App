#include "AndroidStreamCapture.h"
#include <android/log.h>

#define  LOG_TAG    "AndroidCameraCapture"
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)

namespace VisageSDK
{
/**
 * Simple timer function
 */
long getTimeNsec() {
    struct timespec now;
    clock_gettime(CLOCK_REALTIME, &now);
    return (long) ((now.tv_sec*1000000000LL + now.tv_nsec)/1000000LL);
}

AndroidStreamCapture::AndroidStreamCapture(int width, int height, int orientation, int flip)
{
    for(int i = 0; i < 3; i++){
        _buffers.push_back(make_pair(vsCreateImage(vsSize(width, height), VS_DEPTH_8U, 3), 0));
        _buffers[i].first->widthStep = 3 * width; //removing padding because OpenGL cannot handle image data with additional padding
    }

	bufferN = vsCreateImage(vsSize(width, height),VS_DEPTH_8U,3);
	bufferN->widthStep = 3 * width; 
	bufferT = vsCreateImage(vsSize(height, width),VS_DEPTH_8U,3);
	bufferT->widthStep = 3 * height;

	pthread_mutex_init(&mutex, NULL);
    pthread_mutex_init(&paramMutex, NULL);
	pthread_cond_init(&cond, NULL);

	frameArrived = false;
	grabCount = 0;
    writeCount = 0;

    this->orientation = orientation;
	this->flip = flip;
	this->width = width;
	this->height = height;

    _wb = 0;
    _rb = 1;
    _ub = 2;

}

AndroidStreamCapture::~AndroidStreamCapture(void)
{
    for(int i = 0; i < 3; i++)
    {
        vsReleaseImage(&_buffers[i].first);
    }

    _buffers.clear();

    // cleaning up
	vsReleaseImage(&bufferN);
	vsReleaseImage(&bufferT);

	pthread_mutex_destroy(&mutex);
    pthread_mutex_destroy(&paramMutex);
	pthread_cond_destroy(&cond);
}

void AndroidStreamCapture::WriteFrameYUV420(unsigned char* imageDataChannel0, unsigned char* imageDataChannel1,
                                         unsigned char* imageDataChannel2, long timestamp_A, int pixelStride)
{
	pthread_mutex_lock(&paramMutex);
    _buffers[_wb].second = timestamp_A;

    YUV420toRGB(imageDataChannel0, imageDataChannel1, imageDataChannel2, _buffers[_wb].first, width, height, pixelStride);

    pthread_mutex_unlock(&paramMutex);

    int tmp;

    pthread_mutex_lock(&mutex);
    tmp = _wb;
    _wb = _rb;
    _rb = tmp;

	frameArrived = true;
    writeCount++;

	pthread_cond_signal(&cond);
	pthread_mutex_unlock(&mutex);
}

VsImage *AndroidStreamCapture::GrabFrame(long &timeStamp)
{
    struct timespec   ts;
    struct timeval    tp;
    VsImage* ret;
    timeStamp = _buffers[_ub].second;
    int rc = gettimeofday(&tp, NULL);
    ts.tv_sec  = tp.tv_sec + 2;
    ts.tv_nsec = 0;
	pthread_mutex_lock(&mutex);
	while (!frameArrived){
		int ret_cond = pthread_cond_timedwait(&cond, &mutex, &ts);
        if (ret_cond == ETIMEDOUT){
            pthread_mutex_unlock(&mutex);
            return 0;
    	}
    }

    int tmp = _ub;
    _ub = _rb;
    _rb = tmp;

    frameArrived = false;
    pthread_mutex_unlock(&mutex);

    long startTime2 = getTimeNsec();

    pthread_mutex_lock(&paramMutex);
	switch(orientation){

	case 0: case 360:
				if (flip)
					vsFlip(_buffers[_ub].first, bufferN, 1);
				else
					vsCopy(_buffers[_ub].first, bufferN);
				ret = bufferN;
				break;
			case 90:
				vsTranspose(_buffers[_ub].first, bufferT);
				if (!flip)
					vsFlip(bufferT, bufferT, 1);
				ret = bufferT;
				break;
			case 180:
				if (flip)
					vsFlip(_buffers[_ub].first, bufferN, 0);
				else
					vsFlip(_buffers[_ub].first, bufferN, -1);
				ret = bufferN;
				break;
			case 270:
				vsTranspose(_buffers[_ub].first, bufferT);
				if (flip)
					vsFlip(bufferT, bufferT, -1);
				else
					vsFlip(bufferT, bufferT, 0);
				ret = bufferT;
				break;
	}
    pthread_mutex_unlock(&paramMutex);
    int time2 = (int) (getTimeNsec() - startTime2);
    grabCount++;
	return ret;
}
void AndroidStreamCapture::YUV420toRGB(unsigned char* dataChannel0, unsigned char* dataChannel1, unsigned char* dataChannel2, VsImage* buff, int width, int height, int pixelStride){
    int size = width*height;
    int u, v, y1, y2, y3, y4;
    const int widthStep = buff->widthStep;
    // i along Y and the final pixels
    // k along pixels U and V
    for(int i=0, k=0; i < size; i+=2, k+=pixelStride) {
        y1 = dataChannel0[i  ]&0xff;
        y2 = dataChannel0[i+1]&0xff;
        y3 = dataChannel0[width+i  ]&0xff;
        y4 = dataChannel0[width+i+1]&0xff;

        u = dataChannel1[k]&0xff;
        v = dataChannel2[k]&0xff;

		convertYUV420toARGB(y1,u,v, &buff->imageData[i*3], &buff->imageData[i*3+1], &buff->imageData[i*3+2]);
		convertYUV420toARGB(y2,u,v, &buff->imageData[(i+1)*3], &buff->imageData[(i+1)*3+1], &buff->imageData[(i+1)*3+2]);
		convertYUV420toARGB(y3,u,v, &buff->imageData[widthStep+i*3], &buff->imageData[widthStep+i*3+1], &buff->imageData[widthStep+i*3+2]);
		convertYUV420toARGB(y4,u,v, &buff->imageData[widthStep+(i+1)*3], &buff->imageData[widthStep+(i+1)*3+1], &buff->imageData[widthStep+(i+1)*3+2]);
        if (i!=0 && (i+2)%width==0)
            i+=width;
    }
}
 
int AndroidStreamCapture::clamp(int x) {
    unsigned y;
    return !(y=x>>8) ? x : (0xff ^ (y>>24));
}

void AndroidStreamCapture::YUV_NV21_TO_RGB(unsigned char* yuv, VsImage* buff, int width, int height)
{
    const int frameSize = width * height;

    const int ii = 0;
    const int ij = 0;
    const int di = +1;
    const int dj = +1;

    unsigned char* rgb = (unsigned char*)buff->imageData;

    int a = 0;
    for (int i = 0, ci = ii; i < height; ++i, ci += di)
    {
        for (int j = 0, cj = ij; j < width; ++j, cj += dj)
        {
            int y = (0xff & ((int) yuv[ci * width + cj]));
            int v = (0xff & ((int) yuv[frameSize + (ci >> 1) * width + (cj & ~1) + 0]));
            int u = (0xff & ((int) yuv[frameSize + (ci >> 1) * width + (cj & ~1) + 1]));
            y = y < 16 ? 16 : y;

            //int r = (int) (1.164f * (y - 16) + 1.596f * (v - 128));
            //int g = (int) (1.164f * (y - 16) - 0.813f * (v - 128) - 0.391f * (u - 128));
            //int b = (int) (1.164f * (y - 16) + 2.018f * (u - 128));

            int a0 = 1192 * (y -  16);
            int a1 = 1634 * (v - 128);
            int a2 =  832 * (v - 128);
            int a3 =  400 * (u - 128);
            int a4 = 2066 * (u - 128);

            int r = (a0 + a1) >> 10;
            int g = (a0 - a2 - a3) >> 10;
            int b = (a0 + a4) >> 10;

            *rgb++ = clamp(r);
            *rgb++ = clamp(g);
            *rgb++ = clamp(b);
        }
    }
}

void AndroidStreamCapture::convertYUV420toARGB(int y, int u, int v, char *r, char *g, char *b) {

    int rTmp = y + (int)(1.370705 * (v-128));
    int gTmp = y - (int)(0.698001 * (v-128)) - (int)(0.337633 * (u-128));
    int bTmp = y + (int)(1.732446 * (u-128));

    *r = (char) ((rTmp) > 255 ? 255 : (rTmp) < 0 ? 0 : (rTmp));
    *g = (char) ((gTmp) > 255 ? 255 : (gTmp) < 0 ? 0 : (gTmp));
    *b = (char) ((bTmp) > 255 ? 255 : (bTmp) < 0 ? 0 : (bTmp));
}


void AndroidStreamCapture::rotateYUV(const unsigned char* input, unsigned char* output, int width, int height, int rotation, bool flip)
{
	const bool swap = (rotation == 90 || rotation == 270);

	//if flip is true, toggle flipX or flipY. flip is another way to specify that mirroring of the input is needed.
	//different axis needs to be flipped depending on input rotation 90/270 ->flipY, 0/180 flipY
	bool flipAxisX = (rotation == 0 || rotation == 180) && flip;
	bool flipAxisY = (rotation == 90 || rotation == 270) && flip;

	const bool flipY = (rotation == 90 || rotation == 180) != flipAxisY;
	const bool flipX = (rotation == 180 || rotation == 270) != flipAxisX;

	//const bool flipY = (rotation == 90 || rotation == 180) != flip;
	//const bool flipX = (rotation == 180 || rotation == 270);

	for (int x = 0; x < width; x++) {
		for (int y = 0; y < height; y++) {
			int xo = x, yo = y;
			int w = width, h = height;
			int wo = w, ho = h;
			int xi = xo, yi = yo;
			if (swap) {
				xo = y;
				yo = x;
				wo = h;
				ho = w;
			}
			if (flipX) {
				xi = w - xi - 1;
			}
			if (flipY) {
				yi = h - yi - 1;
			}
			output[wo * yo + xo] = input[w * yi + xi];

//            int fs = w * h; //full size
//            int qs = (fs >> 2); // chroma size
//            xi = (xi >> 1);
//            yi = (yi >> 1);
//            xo = (xo >> 1);
//            yo = (yo >> 1);
//            w = (w >> 1);
//            wo = (wo >> 1);
//            int ui = fs + w * yi + xi;
//            int uo = fs + wo * yo + xo;
//            int vi = qs + ui;
//            int vo = qs + uo;
//            output[uo] = input[ui];
//            output[vo] = input[vi];
		}
	}
}
}
