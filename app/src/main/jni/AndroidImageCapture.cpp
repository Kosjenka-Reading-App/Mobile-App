#include "AndroidImageCapture.h"
#include <android/log.h>

#define  LOG_TAG    "AndroidImageCapture"
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)

namespace VisageSDK
{

AndroidImageCapture::AndroidImageCapture()
{
	pts = 0;
}

AndroidImageCapture::AndroidImageCapture(int width, int height, int format)
{
	int nChannels;
	switch (format) {
		case VISAGE_FRAMEGRABBER_FMT_RGB:
			// fall-through
		case VISAGE_FRAMEGRABBER_FMT_BGR:
			nChannels = 3;
			break;
		case VISAGE_FRAMEGRABBER_FMT_LUMINANCE:
			// fall-through
		default:
			nChannels = 1;
			break;
	}
	buffer = vsCreateImage(vsSize(width, height), VS_DEPTH_8U,nChannels);
	pts = 0;
	this->width = width;
	this->height = height;	
}

AndroidImageCapture::~AndroidImageCapture(void)
{
	// cleaning up
	    vsReleaseImage(&buffer);
}
 
void AndroidImageCapture::WriteFrame(unsigned char* imageData, int width, int height)
{
	this->width = width;
	this->height = height;
	memcpy(buffer->imageData, imageData, buffer->imageSize);
}

VsImage *AndroidImageCapture::GrabFrame(long &timeStamp)
{
	timeStamp = pts++;
	return buffer;
}
}
