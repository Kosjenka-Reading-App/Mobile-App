#ifndef __AndroidCameraCapture_h__
#define __AndroidCameraCapture_h__

#include <pthread.h>
#include <cerrno>
#include "vs_main.h"

#define VISAGE_FRAMEGRABBER_FMT_RGB 0
#define VISAGE_FRAMEGRABBER_FMT_BGR 1 
#define VISAGE_FRAMEGRABBER_FMT_LUMINANCE 2
#define VISAGE_FRAMEGRABBER_FMT_RGBA 3
#define VISAGE_FRAMEGRABBER_FMT_BGRA 4

#define VISAGE_FRAMEGRABBER_ORIGIN_TL 0
#define VISAGE_FRAMEGRABBER_ORIGIN_BL 1

namespace VisageSDK
{

/** AndroidCameraCapture demonstrates use of raw camera image 
 * input to track from Android camera. 
 * @ref GrabFrame method will be periodically called to get new frame.
 * For inputing new frame, @ref WriteFrame should be used. This method expects frame in 
 * Android camera NV21 format (YUV420sp). YUV420sp to RGB converting, rotation and flipping
 * is done in @ref GrabFrame.
 */
class AndroidCameraCapture {
		
public:

	bool frameArrived;
		
	/** Constructor.
	 *	
	 */
	AndroidCameraCapture();
	
	/** Constructor.
	* 
	* @param width width of image
	* @param height height of image
	* @param orientation Orientation of image. Allowed values are 0, 90, 180, 270
	* @param flip Flip image horizontaly.
	*/
	AndroidCameraCapture(int width, int height, int orientation=0, int flip = 0);

	/** Destructor.
	 *	
	 */
	~AndroidCameraCapture(void);

	/**
	 * 
	 * This function is called periodically to get the new video frame to process.
	 * 
	 */
	VsImage *GrabFrame(long &timeStamp);
	
	/**
	* Method for writing imageData to buffer object.
	* @param imageData raw pixel data of image used for tracking
	*/
	void WriteFrame(unsigned char *imageData);

	void WriteFrameYUV(unsigned char* imageData);

	void YUV_NV21_TO_RGB(unsigned char* yuv, VsImage* buff, int width, int height);

	int clamp(int x);


private:

	/**
	* Convert default Android camera output format (YUV420sp) to RGB. 
	*/
	void YUV420toRGB(unsigned char* data, VsImage* buff, int width, int height);
	void convertYUVtoARGB(int y, int u, int v, char *r, char *g, char *b);

	VsImage* buffer;
	VsImage* bufferN;
	VsImage* bufferT;
	unsigned char *data;
	int orientation;
	int flip;
	int pts;
	int width, height;
	pthread_mutex_t mutex;
	pthread_cond_t cond;	
};

}
	
#endif // __AndroidCameraCapture_h__
